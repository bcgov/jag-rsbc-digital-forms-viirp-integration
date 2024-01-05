##############################################################################################
#### Stage where the git submodules are updated                                            ###
##############################################################################################

FROM alpine/git as libraries

WORKDIR /libs

COPY . .

RUN git submodule update --remote --merge

##############################################################################################
#### Stage where the maven dependencies are cached                                         ###
##############################################################################################
FROM maven:3.8.2-eclipse-temurin-17 as dependencies-cache

WORKDIR /build

## for the lack at a COPY --patern */pom.xml, we have to declare all the pom files manually
COPY pom.xml pom.xml
COPY digitalforms-api/pom.xml digitalforms-api/pom.xml
COPY digitalforms-api-specification/pom.xml digitalforms-api-specification/pom.xml

COPY --from=libraries /libs/jag-digitalforms-client/pom.xml jag-digitalforms-client/pom.xml
COPY --from=libraries /libs/jag-vips-client/src/jag-vips-client/pom.xml jag-vips-client/src/jag-vips-client/pom.xml

RUN  mvn dependency:go-offline \
    -DskipTests \
    --no-transfer-progress \
    --batch-mode \
    --fail-never

##############################################################################################
#### Stage where the application is built                                                  ###
##############################################################################################
FROM dependencies-cache as build

WORKDIR /build

COPY digitalforms-api/src digitalforms-api/src
COPY digitalforms-api-specification/src digitalforms-api-specification/src

COPY --from=libraries /libs/jag-digitalforms-client/src jag-digitalforms-client/src
COPY --from=libraries /libs/jag-digitalforms-client/digitalformsords.yaml jag-digitalforms-client/digitalformsords.yaml
COPY --from=libraries /libs/jag-vips-client/src/jag-vips-client/src jag-vips-client/src/jag-vips-client/src
COPY --from=libraries /libs/jag-vips-client/src/jag-vips-client/vipsords.yaml jag-vips-client/src/jag-vips-client/vipsords.yaml

RUN  mvn clean package \
    -DskipTests \
    --no-transfer-progress \
    --batch-mode

##############################################################################################
#### Stage where Docker is running a java process to run a service built in previous stage ###
##############################################################################################
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /build/digitalforms-api/target/digitalforms-api-*.jar digitalforms-api.jar

CMD ["java", "-jar", "digitalforms-api.jar"]