## Digital Forms VI IRP VIPS Integration [![Lifecycle:Experimental](https://img.shields.io/badge/Lifecycle-Experimental-339999)](<Redirect-URL>)

This is the root level of this project.

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

### Building the application

TBD

### Running the application


**1. Using Eclipse or Spring Tool Suite 4 (STS4)**

- Import as a Maven project from the root level. Dependent API should load automatically.

- Create a run configuration for the POM:  

	- clean install package

- Launch the **digitalforms-api** from the Boot Dashboard.

**2. Using command line**

- From the digitalforms-api level,  Run in order:

```
mvn clean install -Pdefault-profile
mvn spring-boot:run
```

### Security

TBD

### Actuator

TBD


### Files and Directories

TBD






