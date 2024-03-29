## Digital Forms VI IRP VIPS Integration [![Lifecycle:Maturing](https://img.shields.io/badge/Lifecycle-Maturing-007EC6)](<Redirect-URL>) [![CI Checks for API](https://github.com/bcgov/jag-rsbc-digital-forms-viirp-integration/actions/workflows/build_check.yml/badge.svg)](https://github.com/bcgov/jag-rsbc-digital-forms-viirp-integration/actions/workflows/build_check.yml)

This is the digitalforms level (root) of this project.

### Built With

- [Maven](https://maven.apache.org/) - Dependency Management
- [JDK](https://www.oracle.com/java/technologies/javase/jdk11-archive-downloads.html) - Java Platform, Standard Edition 11 Development Kit
- [Spring Boot](https://spring.io/projects/spring-boot) - Framework to ease the bootstrapping and development of new Spring Applications
- [git](https://git-scm.com/) - Free and Open-Source distributed version control system
- [Swagger](https://swagger.io/) - Open-Source software framework backed by a large ecosystem of tools that helps developers design, build, document, and consume RESTful Web services.

### External Tools Used

- [Postman](https://www.getpostman.com/) - API Development Environment (Testing Documentation)
 
## Technical Overview

| Layer              | Technology                |
| ------------------ | ------------------------- |
| Service            | Java, SpringFramework     |
| Test framework     | TBD						 |
| Application Server | Spring Boot / Tomcat      |
| Runtime            | BC DevOps OpenShift      |

### Running the application


**1. Using Eclipse or Spring Tool Suite 4 (STS4)**

- Import as a Maven project from digitalforms (parent) level. Dependent API and Specification modules should load automatically.

- Create a run configuration for the parent POM:  

	- clean install

- Launch the **digitalforms-api** from the Boot Dashboard or from a new run configuration with target of:

	- spring-boot:run 

**2. Using command line**

From the **digitalforms** (parent) level, run: 
```
mvn clean install
```

Then, from the **digitalforms-api** level, run:
```
mvn spring-boot:run
```

Note: This API uses submodules which should be updated before running the API: 

```
 git submodule update --init 
```

### Security

API is protected by basic auth which must be configured using environmental variables. 

See digitalforms-api README.md, *Environmental Variables*

### Actuator

See digitalforms-api README.md, *API Services*. 

## Splunk

| Environment variable | Value     |
| ---------- | --------- |
| `SPLUNK_URL` | Splunk HEC url |
| `SPLUNK_TOKEN_VIPS` | token |

### Remote dependency 

This API contains two shared dependencies, jag-vips-client, and jag-digitalforms-client. Both are used by other projects including DPS and Digital Forms, Reviews.
Endpoint and user / password data must be set as environmental variables if running locally or set as Openshift secrets when in deployed in DEV/TEST/PROD environments. 
  
| jag-vips-client Environment Variables | Value     |
| ---------- | --------- |
| `DIGITALFORMS_VIPSORDS_BASEPATH` | ORDS base endpoint |
| `DIGITALFORMS_VIPSORDS_PASSWORD` | ORDS password |
| `DIGITALFORMS_VIPSORDS_USER` | ORDS user |
  
| jag-digitalforms-client Environment Variables | Value     |
| ---------- | --------- |
| `DIGITALFORMS_ORDS_BASEPATH` | ORDS base endpoint |
| `DIGITALFORMS_ORDS_PASSWORD` | ORDS password |
| `DIGITALFORMS_ORDS_USER` | ORDS user |

For a complete listing of environmental variables see the API, README.md (https://github.com/bcgov/jag-rsbc-digital-forms-viirp-integration/blob/release/1.0/digitalforms-api/README.md)




