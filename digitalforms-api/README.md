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
| DIGITALFORMS_OAUTH2_ALLOWED_CLIENT_IDS | vips-vi-irp-dev         | DIAM client IDs allowed to call the API (comma-separated); startup fails if empty. See *Security* |
| SPLUNK_URL					  | url							| 'splunk' profile only|
| SPLUNK_TOKEN_VIPS				  | splunk token				| 'splunk' profile only|   

### Optional Environmental Variables

These values can be set as environmental variables to overwrite the default values if running the API locally (As run configuration in STS4 or Eclipse)

| Name                            | Default Value               |
| ------------------------------- | --------------------------- |
| DIGITALFORMS_VIPSAPI_TIMEOUT 	  | 10000 (ms)                  |
| DIGITALFORMS_VIPSAPI_RETRY_COUNT| 3 (GET/HEAD only; POSTs are never retried) |
| DIGITALFORMS_VIPSAPI_RETRY_DELAY| 5 (seconds)                 |
| DIGITALFORMS_VIPSAPI_RETRY_MAX_DURATION | 15000 (ms; total time for a GET including retries, 0 = no limit) |
| DIGITALFORMS_SWAGGER_ENABLED    | false (Swagger UI off)      |
| SPRINGDOC_APIDOCS_ENABLED       | true (`false` turns off the API docs and Swagger UI) |
| DIGITALFORMS_OAUTH2_AUDIENCE    | empty (audience not checked); see *Security* |
| DIGITALFORMS_OAUTH2_ISSUER_URI  | https://common-sso.justice.gov.bc.ca/auth/realms/Services |

## API Services

After running the `digitalforms-api` from the directory `/digitalforms-api/`, these services should be available:

| Name                  | URL                                          						| Notes
| --------------------- | ----------------------------------------------------------------- | --------------------------------------------
| digitalforms-api      | http://localhost:8082/digitalforms-viirp/v1/ 						| The root URL for the API service endpoints
| digitalforms-api      | http://localhost:8082/digitalforms-viirp/v1/swagger-ui/index.html | The Swagger-UI tool to query the API
| digitalforms-api      | http://localhost:8082/digitalforms-viirp/v1/actuator/health		| Public health status; no component details
| digitalforms-api      | http://localhost:8082/digitalforms-viirp/v1/actuator/info | Application build and running Java versions; OAuth2 bearer token required

### Actuator health and version information

Only `health` and `info` are exposed over HTTP, using standard Spring Boot Actuator
endpoints (not generated business API operations).

Springdoc's standard `springdoc.show-actuator=true` setting adds the exposed `health`,
`info` and actuator-discovery (`/actuator`) operations to the runtime `/v3/api-docs`
document and Swagger UI. Postman can import the runtime document at
`/digitalforms-viirp/v1/v3/api-docs`. The health operation is public; info and
discovery require an OAuth2 bearer token, which the OpenAPI document advertises.
Disabled Actuator operations are not added. The canonical `VI_IRP_V1.yaml` remains
the business API contract and intentionally contains no Actuator paths; import the
runtime document when you want an operational Postman collection.

- **`GET /actuator/health` is public.** It returns the aggregate `status` without
  `components` or `details`, even for authenticated requests. Spring Boot may also
  include probe group names such as `liveness` and `readiness`. No disk-space values,
  filesystem paths, or other component details are returned.
- **`GET /actuator/info` requires the same OAuth2 bearer token as the business API.** The relevant fields are:

  | Field | Source |
  |---|---|
  | `build.version` | Application version from [pom.xml](pom.xml), currently `1.0.2` (not the Spring Boot parent version). |
  | `build.artifact`, `build.group`, `build.name`, `build.time` | Maven-generated application build metadata. |
  | `java.version` | Java version of the running process. |
  | `java.runtime.version`, `java.runtime.name` | Hosted JRE build/version and runtime name. |
  | `java.jvm.version`, `java.jvm.name`, `java.jvm.vendor` | Running JVM build, implementation and vendor. |

  In Postman, use **GET** with the OAuth 2.0 authorization described under *Security*.
  Missing/invalid tokens return 401 with a Bearer challenge. Java information comes from the live JVM, not the compiler
  target, Maven build JDK or a hard-coded Docker image version.
- **`/actuator` discovery and health subpaths also require authentication.**
  Other endpoints, including `env`, `beans`, `metrics`, `configprops` and `heapdump`,
  are not exposed, even to authenticated callers. Only the build and Java info
  contributors are enabled; environment, Git and OS information are not published.

Disk-space checks remain active internally: a failing check still changes the
aggregate health status to `DOWN` with HTTP 503. Existing OpenShift probes use
`/digitalforms-viirp/v1/actuator/health` and do not need authentication or path changes.
The Actuator endpoints remain available when `SPRINGDOC_APIDOCS_ENABLED=false`.

The Spring Boot Maven `build-info` goal generates `META-INF/build-info.properties`
during the normal Maven lifecycle and includes it in the deployed JAR. Maven also
filters `digitalforms.app.version` from `@project.version@` for the startup log,
so that value no longer needs a separate manual update. Before launching from Eclipse,
run a Maven build (or at least `mvn process-classes` in the application module with
its reactor dependencies installed) and refresh the project. A direct IDE launch
without generated build metadata will omit the `build` section from `/actuator/info`.

No new environment variables are required. Do not override
`management.endpoint.health.show-details` / `show-components` to `always`, or widen
`management.endpoints.web.exposure.include` beyond `health,info`.

### HTTP contract

[VI_IRP_V1.yaml](../digitalforms-api-specification/src/main/resources/VI_IRP_V1.yaml)
is the authoritative public contract. OpenAPI Generator produces the interfaces,
controllers and models; do not edit generated files. Change the YAML first, then
update the delegates and run the contract tests.

springdoc publishes API docs built from the generated controllers at `/v3/api-docs`
(JSON) and `/v3/api-docs.yaml`, beneath the context path, for example
`http://localhost:8082/digitalforms-viirp/v1/v3/api-docs`. `/digitalforms-viirp/v1`
is the context path, which carries the API version; `v3` is springdoc's default
path and refers to OpenAPI 3. The generator also produces springdoc's title,
description, version and `oauth2` scheme from the YAML. The generator emits only
the scheme's type, so `MultipartOpenApiConfig` adds its description and
client-credentials token URL (from `DIGITALFORMS_OAUTH2_ISSUER_URI`); a test keeps
them equal to the YAML. The docs describe the same API as the YAML but are springdoc's
rendering of it: OpenAPI 3.0.1 like the YAML, with an absolute server URL taken
from the request, and without the `X-Correlation-ID` response header (the
generator's annotations omit response headers). `springdoc.api-docs.version` is
set to `openapi_3_0` because springdoc's 3.1 default deprecates the schema
`example` keyword used by the generated models, which editor.swagger.io reports
as warnings. Moving the contract to 3.1 waits for an OpenAPI Generator release
that keeps schema examples ([#24755](https://github.com/OpenAPITools/openapi-generator/issues/24755));
today it drops them. Share the YAML itself with API consumers.

| Variable | Default | Effect |
|---|---|---|
| `DIGITALFORMS_SWAGGER_ENABLED` | `false` | `true` enables Swagger UI at `/swagger-ui/index.html`. |
| `SPRINGDOC_APIDOCS_ENABLED` | `true` | `false` (PROD) turns off `/v3/api-docs`, `/v3/api-docs.yaml` and Swagger UI, which then return 404. The API itself is unaffected. |

| Response | Meaning |
|---|---|
| 200 | Successful GET; an existing notice with no documents returns `{"results":[]}`. |
| 201 | Successful prohibition, impoundment, document or document-association POST; includes saved-with-warning codes 11 and 29. |
| 400 | Missing/malformed JSON or multipart data, invalid parameter types or failed request constraints. |
| 401 | Missing, invalid or expired bearer token, with a `WWW-Authenticate: Bearer` challenge. |
| 404 | Unknown route or missing requested notice/document. |
| 405 | Unsupported method, with an `Allow` header. |
| 406 / 415 | Unsupported response/request media type. |
| 409 | Confirmed duplicate prohibition or impoundment notice number. |
| 413 | Document exceeds 10 MiB or the multipart request exceeds 11 MiB. |
| 500 | Internal/downstream failure, including DIAM's signing keys being unreachable, with a safe generic message. |

Errors use `{"status_message":"..."}`. Validation errors identify fields without
exposing controller signatures; unexpected failures return `Internal Server Error`
and retain diagnostic details in server logs.

#### Uploading a document

`POST /documents` accepts **multipart/form-data only**, with exactly one PDF file.
In Postman, choose **Body > form-data**, set `file` to **File**, and use **Text**
for the other parts. Let Postman set the Content-Type header and multipart boundary.
Send the OAuth2 bearer token and the optional `X-Correlation-ID` header as for other operations.

| Part | Required | Value |
|---|---|---|
| `file` | Yes | A non-empty, readable PDF with 1 to 99,999 pages; at most 10 MiB (10,485,760 bytes). |
| `type_code` | Yes | Document classification, e.g. `MV2723`; nonblank, at most 10 characters. |
| `notice_type_code` | No | Notice classification, e.g. `IRP`; at most 3 characters. |
| `notice_subject_code` | No | Subject classification, e.g. `PERS`; at most 4 characters. |

The response remains **HTTP 201** with a string document ID:

```json
{"document_id":"102220"}
```

The API validates the PDF contents with PDFBox; it does not trust the filename or
part Content-Type. Files requiring a password to open, malformed PDFs, PDFs without
pages or with more than 99,999 pages, missing/empty files, and multiple file parts are
rejected with 400 before calling ORDS. File parts under other names are not silently
ignored. This is format validation, not malware scanning or a guarantee that every
page can be rendered.

The page count is read from the PDF and forwarded to ORDS as `page_count`; VIPS stores
it in `VIPS_DOCUMENTS.PAGE_COUNT_QTY` (`NUMBER(5)`, hence the 99,999-page limit) and
shows it in document lists, including `pageCount` in `GET /documents/list/{noticeNo}`.
Clients do not send a page count; a `pageCount` text part is ignored.

The API forwards the original bytes unchanged to the existing ORDS client as
`application/pdf`. `mime_type`, `mime_sub_type`, `pageCount`, and the JSON/Base64
`file_object` are no longer request fields. JSON and raw binary uploads return 415.

Standard springdoc still publishes the API docs. `springdoc.default-support-form-data`
keeps the text parts in the multipart body, and `MultipartOpenApiConfig` restores
required flags from the generated `@RequestParam` annotations (a springdoc 3.2 gap).
It does not serve a separate spec or duplicate field definitions from the YAML.

The parser limits in `application.yml` are `spring.servlet.multipart.max-file-size=10MB`
and `spring.servlet.multipart.max-request-size=11MB` (Spring's MB units are binary MiB).
The delegate also enforces the 10 MiB file cap. Tomcat's bounded
`server.tomcat.max-swallow-size=12MB` allows ordinary oversized requests to receive
the JSON 413 response; much larger rejected uploads may have their connection closed.
Deployment overrides must not lower these limits unintentionally.

Uploading still **only stores a document**: the notice classifications do not associate
it with a notice. The association endpoint, create-request `vipsDocumentIdArray`
properties, and Base64 document retrieval are unchanged.

#### Correlation ID

Every operation accepts an optional `X-Correlation-ID` request header (1-64
characters from `A-Z`, `a-z`, `0-9`, `.`, `_` and `-`; a UUID is recommended).
When the header is omitted, the API generates a UUID. The effective value is always
returned in the `X-Correlation-ID` response header, including on error responses,
and is logged as `correlationId` for every log line of the request. It is used only
for tracing; it is not forwarded to VIPS and is not an idempotency key. An invalid
value, or more than one `X-Correlation-ID` header, is rejected with 400 before
authentication. `CorrelationIdFilter` enforces this; its pattern must match the
`CorrelationId` parameter in the YAML (a test guards against drift).

Creates check for an exact existing notice number before submitting. If VIPS WS
returns its generic failure code 1, the API checks again for a concurrent create.
Only a confirmed existing notice becomes 409; an unavailable lookup or unrelated
failure remains 500. Oracle uniqueness constraints remain the final guard.
This is not idempotency: after an ambiguous failure, retrieve by notice number
before resubmitting. A dedicated downstream duplicate code would remove reliance
on the follow-up lookup. No database or shared-client changes are required.

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
JSON codecs, and VIPS WebClient/ORDS client. A loopback HTTP stub replaces the remote
VIPS WS and ORDS services, and a loopback identity provider replaces DIAM: it serves a
signing key set and mints RS256 tokens shaped like DIAM's client-credentials tokens.
No real credentials, database writes, or Docker daemon are required.
They cover:

- All four retired `/dfpayloads/*` operations returning 404
  after authentication, and their absence from the canonical OpenAPI contract.
- Authenticated prohibition/impoundment creation followed by search and retrieval,
  including forwarded authentication/audit headers and JSON field preservation.
- Missing/invalid tokens, missing notices, VIPS failures, and licence validation.
- OAuth2: expired, other-realm, unlisted or missing `azp`, ID (`typ`) and untyped,
  forged, unknown-key, unsigned and malformed tokens, and Basic credentials, all
  rejected with a Bearer challenge before reaching VIPS; the optional audience check;
  a 500 when DIAM's keys cannot be downloaded; the protected-resource metadata; the
  client allow-list parsing; and the published `oauth2` scheme matching the YAML.
- Null or absent search pagination metadata, which must not break Jackson 3 decoding.
- All ten public operations, including document upload/retrieval/association/listing,
  code tables and ORDS ping.
- Single-PDF multipart upload, optional metadata and exact binary forwarding to ORDS;
  rejection of multiple files, invalid PDFs, malformed multipart data and legacy JSON.
  The 10 MiB acceptance boundary and one-byte-over rejection are exercised over HTTP.
- Missing/malformed bodies, required fields, parameter constraints, media types,
  duplicates/concurrent creates and unexpected downstream response codes.
- Every required field in both create requests, with cases derived from the YAML:
  missing/null values, empty/space/tab/newline-only required strings, empty required
  objects and collections with `minItems`. Multiple registrations and addresses
  exercise nested validation beyond the first array element. Invalid requests must
  return 400 identifying the field without reaching VIPS. Positive controls preserve
  optional licence objects and allowed empty arrays; separate cases cover the
  impoundment-only requirement for a licence number when a licence object is supplied.
- Response status, content type, `X-Correlation-ID` header and body-schema validation
  against the YAML for every tested public operation. Guard tests deliberately inject
  wrong statuses, shapes and headers to prove drift fails the tests.
- `X-Correlation-ID` generation, echo on every response (including 401/404/405/406/415/500),
  logging, and 400 rejection of invalid or repeated headers before authentication.
- Published JSON/YAML contract equality and Swagger UI's canonical document URL.
- Minimal public health output for both UP/200 and disk-space DOWN/503, protected
  build/runtime information, and non-exposure of other Actuator endpoints.

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
`/utility/ords/ping` now checks VIPS ORDS only.

The retained `/prohibitions/*` and `/impoundments/*` routes still call the existing
VIPS WS `/cases/...` APIs through `VipsRestServiceImpl`. This cleanup/Java 21/Spring
Boot 4 upgrade does not implement the later AOT source/verification/idempotency
rules or change that downstream write path.

### Security

The API is an OAuth 2.0 resource server. Callers use the client credentials grant to
get an access token from DIAM (the Keycloak `Services` realm on
`common-sso.justice.gov.bc.ca`) and send it on every request as
`Authorization: Bearer <access_token>`. Basic authentication is no longer accepted.

The API validates each token itself; it does not call DIAM per request. Spring
Security checks:

- the RS256 signature against DIAM's published signing keys (JWKS). The keys are
  downloaded on first use and cached; a token signed with an unknown key ID triggers
  a refresh, so DIAM key rotation needs no restart;
- the issuer (`iss`) equals `DIGITALFORMS_OAUTH2_ISSUER_URI`;
- expiry and not-before times, allowing 60 seconds of clock skew;
- the authorized party (`azp`, the calling client) is in
  `DIGITALFORMS_OAUTH2_ALLOWED_CLIENT_IDS`. The realm issues tokens to many unrelated
  applications, so a valid realm token is not enough;
- the `typ` claim is `Bearer`, which rejects Keycloak ID tokens;
- the audience (`aud`) contains `DIGITALFORMS_OAUTH2_AUDIENCE`, when that is set.

`/actuator/health`, the API docs, Swagger UI and the RFC 9728 metadata at
`/.well-known/oauth-protected-resource` are public; every other path needs a token.
DIAM issues no API-specific scopes for this client, so every allowed client may call
every operation.

| Variable | Required | Default | Notes |
|---|---|---|---|
| `DIGITALFORMS_OAUTH2_ALLOWED_CLIENT_IDS` | Yes | none | Comma-separated DIAM client IDs, e.g. `vips-vi-irp-dev`. Startup fails if empty. Not a secret. |
| `DIGITALFORMS_OAUTH2_AUDIENCE` | Recommended | empty (not checked) | Identifies this API as an intended token recipient; it need not equal the calling client ID. DEV is explicitly set to `vips-irp-dev`, which DIAM currently issues. After DIAM corrects the stale audience and a fresh token is verified, change this to `vips-vi-irp-dev`. Do not disable validation during the cutover. |
| `DIGITALFORMS_OAUTH2_ISSUER_URI` | No | `https://common-sso.justice.gov.bc.ca/auth/realms/Services` | Keys are read from `<issuer>/protocol/openid-connect/certs`; the published OpenAPI token URL is `<issuer>/protocol/openid-connect/token`. |

The API holds no client secret; only callers do. It needs outbound HTTPS to
`common-sso.justice.gov.bc.ca`. For a local run (for example in Eclipse) set at least
`DIGITALFORMS_OAUTH2_ALLOWED_CLIENT_IDS=vips-vi-irp-dev` and
`DIGITALFORMS_OAUTH2_AUDIENCE=vips-irp-dev` while DIAM still issues the old audience.
After DIAM confirms the correction and a fresh token contains `vips-vi-irp-dev` in
`aud`, change the audience setting to `vips-vi-irp-dev` and restart the API.
Coordinate the change: a token without the configured audience returns 401.

#### Getting and using a token

```
curl -X POST https://common-sso.justice.gov.bc.ca/auth/realms/Services/protocol/openid-connect/token \
  -d grant_type=client_credentials -d client_id=vips-vi-irp-dev -d client_secret=<secret>
```

DIAM tokens are valid for 5 minutes and come without a refresh token; request a new
one when the API answers 401 with `error="invalid_token"` and a `Jwt expired` description.

In Postman, set the collection's **Authorization** to **OAuth 2.0** with grant type
**Client Credentials**, the token URL above, the client ID and the client secret, then
choose **Get New Access Token** and **Use Token**. A collection imported from the
runtime `/v3/api-docs` already carries the token URL from the `oauth2` scheme. Keep the
secret in a Postman secret variable or the Postman Vault, not in a shared collection.

| Situation | Response |
|---|---|
| No token, or non-Bearer (e.g. Basic) credentials | 401, `WWW-Authenticate: Bearer realm="DigitalForms - VI IRP API", resource_metadata="..."` |
| Invalid, expired, other-client or other-realm token | 401, the challenge adds `error="invalid_token"` and an `error_description` naming the failed check |
| DIAM's signing keys cannot be downloaded | 500 `Internal Server Error`, logged as an error |

Rejected tokens are logged at WARN with the failed check; tokens are never logged.

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
