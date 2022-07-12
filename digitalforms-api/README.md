## Digital Forms API

This is the API level of this of this project.

## Technical Overview

| Layer              | Technology            |
| ------------------ | --------------------- |
| Service            | Java, SpringFramework |
| Application Server | Spring Boot / Tomcat  |
| Runtime            | BC DevOps OpenShift  |

### Environmental Variables

These values must be set as environmental variables if running the API locally (As run configuration in STS4 or Eclipse)

| Name                            | Example Value    |
| ------------------------------- | ---------------- |
| VAR1 							  | value1	         |
| VAR2                            | value2           |

## API Services

After running the `digitalforms-api` from the directory `/digitalforms-api/`, these services should be available:

| Name                  | URL                                          						| Notes
| --------------------- | ----------------------------------------------------------------- | --------------------------------------------
| digitalforms-api      | http://localhost:8082/digitalforms-viirp/v1/ 						| The root URL for the API service endpoints
| digitalforms-api      | http://localhost:8082/digitalforms-viirp/v1/swagger-ui/index.html | The Swagger-UI tool to query the API
| digitalforms-api      | http://localhost:8082/digitalforms-viirp/v1/actuator/health		| Health check actuator endpoint

### Building, installing and running the application locally

1) After clone run:  
 
```
 git submodule update --init 
```
this will pull the last working version of jag-vips-client (submodule).

2) Set the compiler version to java 11

3) From the digitalforms (parent) level, run: 

```
mvn clean install
```

4) From the digitalforms-api level, run:

```
mvn spring-boot:run
```

### Security

TODO

### Swagger2

TODO

### Actuator

See above, API Services. 

### Files and Directories

```
digitalforms-api/
    └── src/../
	 └── digitalforms
	 └── viirp
		├── config 	# Contains API Configuration files
		├── controller 	# Contains API controller files
		├── exception 	# Contains API Exception files
		├── model 	# Contains API Model files
		├── security 	# Contains API Security files
		├── service 	# Contains API Service files
		├── swagger2 	# Contains API Swagger files
		└── util 	# Contains API Util files
```

### Remote dependency 

This API contains a shared dependency, jag-vips-client, used by other projects including DPS and Digital Forms, Reviews.
Endpoint and user / password data must be set as environmental variables if running locally or set as Openshift 
secrets when in the DEV/TEST/PROD environments.

### API Operations categories.

| Category            | Summary                              |
| ------------------- | ------------------------------------ |
| `impoundment`    | Impoundment operations               |
| `prohibition`    | Prohibition operations               |
| `documents`       | Document operations                 |
| `df payloads`    | Digital Forms Payload operations     |
| `code tables`    | Code Table value operations          |
| `utility`          | Utility operations  	                 |

- Note: Please see Swagger2 endpoints for a complete breakdown of API Operations and
  parameters

## DevOps Processes

TODO

### DEV builds

TODO

## Promotion to TEST

TODO

## Promotion to PROD

TODO

### License

Apache license 2.0
