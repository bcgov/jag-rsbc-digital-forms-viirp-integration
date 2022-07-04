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


### Building, installing and running the application locally

From the digitalforms (parent) level, run: 

```
mvn clean install
```

Then, from the digitalforms-api level, run:

```
mvn spring-boot:run
```

### Security

TODO

### Swagger2

TODO

### Actuator

TODO

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

### API Operations categories.

| Category            | Summary                              |
| ------------------- | ------------------------------------ |
| `impoundment`    | Impoundment operations               |
| `prohibition`    | Prohibition operations               |
| `documents`       | Document operations                 |
| `df payloads`    | Digital Forms Payload operations     |
| `code tables`    | Code Table value operations          |
| `search`          | Search operations  	                 |

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
