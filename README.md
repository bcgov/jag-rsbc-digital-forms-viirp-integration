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

	- clean install package

- Launch the **digitalforms-api** from the Boot Dashboard.

**2. Using command line**

From the **digitalforms** (parent) level, run: 
```
mvn clean install
```

Then, from the **digitalforms-api** level, run:
```
mvn spring-boot:run
```

### Security

TBD

### Actuator

See digitalforms-api README.md, API Services. 

### Files and Directories

TBD

### Remote dependency 

This API contains a shared dependency, jag-vips-client, used by other projects including DPS and Digital Forms, Reviews.
Endpoint and user / password data must be set as environmental variables if running locally or set as Openshift 
secrets when in the DEV/TEST/PROD environments.
  






