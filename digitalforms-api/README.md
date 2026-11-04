## Digital Forms VI IRP API

This is the API level of this of this project.

## Technical Overview

| Layer              | Technology            |
| ------------------ | --------------------- |
| Service            | Java 21, Spring Framework |
| Application Server | Spring Boot 4 / Tomcat 11 |
| Runtime            | BC DevOps OpenShift   |

### Environmental Variables

These values must be set as environmental variables if running the API locally (As run configuration in STS4 / Eclipse)

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

2) Set the compiler version to java 21

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

### Regression tests

With `JAVA_HOME` pointing to JDK 21, run from the repository root:

```
mvn -B -ntp clean verify
```

The HTTP regression suites in [src/test/java/ca/bc/gov/open/digitalformsapi/viirp/controller](src/test/java/ca/bc/gov/open/digitalformsapi/viirp/controller)
start the application on a random port with the real security filters, controllers,
JSON codecs, and VIPS WebClient. A loopback HTTP stub replaces only the remote VIPS
service; no real credentials, database writes, or Docker daemon are required.
They cover:

- All four retired `/dfpayloads/{noticeNo}/{correlationId}` operations returning 404
  after authentication, and their absence from generated OpenAPI.
- Authenticated prohibition/impoundment creation followed by search and retrieval,
  including forwarded authentication/audit headers and JSON field preservation.
- Missing/invalid credentials, missing notices, VIPS failures, and licence validation.
- Null or absent search pagination metadata, which must not break Jackson 3 decoding.

These are HTTP contract tests, not proof of persistence in Oracle. Live VIPS/ORDS
connectivity, database persistence and container deployment require environment validation.
The shared client's existing test runner currently discovers zero tests; its sources
and test configuration are maintained in that separate repository.

### RSVIPS-889 cleanup

The `/dfpayloads/*` API, its local delegate/models/helpers, and the
`jag-digitalforms-client` dependency/submodule reference have been removed from VI/IRP.
`DIGITALFORMS_ORDS_*` variables are no longer used by this application; remove them
only from VI/IRP-specific deployment secrets, not secrets shared by other applications.
The ORDS client repositories and database packages have not been changed.
`/utility/ords/ping/{correlationId}` now checks VIPS ORDS only.

The retained `/prohibitions/*` and `/impoundments/*` routes still call the existing
VIPS WS `/cases/...` APIs through `VipsRestServiceImpl`. This cleanup/Java 21/Spring
Boot 4 upgrade does not implement the later AOT source/verification/idempotency
rules or change that downstream write path.

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
