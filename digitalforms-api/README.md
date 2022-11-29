## Digital Forms VI IRP API

This is the API level of this of this project.

## Technical Overview

| Layer              | Technology            |
| ------------------ | --------------------- |
| Service            | Java, SpringFramework |
| Application Server | Spring Boot / Tomcat  |
| Runtime            | BC DevOps OpenShift  |

### Environmental Variables

These values must be set as environmental variables if running the API locally (As run configuration in STS4 or Eclipse)

| Name                            | Example Value               | Comments	       |
| ------------------------------- | --------------------------- |------------------|
| DIGITALFORMS_VIPSAPI_URL 	      | http://localhost:8080/api/  |                  |
| DIGITALFORMS_VIPSAPI_USERNAME   | username                    |                  |
| DIGITALFORMS_VIPSAPI_PASSWORD   | password                    |                  |
| DIGITALFORMS_VIPSAPI_CREDS_DISPLAYNAME| display name			|				   |
| DIGITALFORMS_VIPSAPI_CREDS_GUID |                             |                  |
| DIGITALFORMS_VIPSAPI_CREDS_USER |                             |                  |
| DIGITALFORMS_VIPSORDS_BASEPATH  | http://localhost:8080/api/  |                  |
| DIGITALFORMS_VIPSORDS_USER      | user                        |                  |
| DIGITALFORMS_VIPSORDS_PASSWORD  | password                    |                  |
| DIGITALFORMS_ORDS_BASEPATH      | http://localhost:8080/api/  |                  |
| DIGITALFORMS_ORDS_USER          | user						|                  |
| DIGITALFORMS_ORDS_PASSWORD      | password					|                  |
| DIGITALFORMS_BASICAUTH_USER          | user					|                  |
| DIGITALFORMS_BASICAUTH_PASSWORD      | password				|                  |
| DIGITALFORMS_SWAGGER_ENABLED      | true					|                  |
| SPLUNK_URL					  | url							| 'splunk' profile only|               
| SPLUNK_TOKEN_VIPS				  | splunk token				| 'splunk' profile only|   

### Optional Environmental Variables

These values can be set as environmental variables to overwrite the default values if running the API locally (As run configuration in STS4 or Eclipse)

| Name                            | Default Value               |
| ------------------------------- | --------------------------- |
| DIGITALFORMS_VIPSAPI_TIMEOUT 	  | 2000                        |
| DIGITALFORMS_VIPSAPI_RETRY_COUNT| 3                           |
| DIGITALFORMS_VIPSAPI_RETRY_DELAY| 5                           |

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

5.) To pull in changes to a submodule, from the digitalforms (parent) level, run  

```
git submodule update --remote --merge
```

### Security

API is protected by basic auth which must be configured using environmental variables. 

See *Environmental Variables*

### Files and Directories

```
digitalforms-api/
    └── src/../
	 └── digitalformsapi/viirp
		├── config 	# Contains API Configuration files
		├── controller 	# Contains API controller files
		├── exception 	# Contains API Exception files
		├── interceptor 	# Contains MDC interceptor files
		├── model 	# Contains VIPS specific POJOs
		├── security 	# Contains API Security files
		├── service 	# Contains API Service files
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

All changes to the application must be made via branches off of the current release branch.

Once a feature has been completed, a PR must be created and sent for review. 

On acceptance of the PR, and after the change has been merged into the current release branch, GIT actions are 
triggered which perform application unit tests and push a new image to OpenShift, DEV.

**Workflows**

| Workflow            | Action                              |
| ------------------- | ------------------------------------ |
| CI Checks for API    | Triggers application unit tests               |
| Deploy    | Retags DEV image to TEST, or PROD               |
| Main    | Build Image and Push to Openshift Registry for Dev Deployment AND runs Trivy vulnerability scanner              |

To push a DEV image in the OpenShift Registry to TEST or TEST to PROD, use the Deploy workflow. 

### License

Apache license 2.0
