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
| TZ                              | America/Vancouver           | Required in containers: date-only fields are sent to VIPS as midnight in this zone. See *Dates and timestamps*. A local run uses the operating system's zone |
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
them equal to the YAML. This configuration also corrects the generator's response
annotations, which repeat an operation's combined media types on every status:
Problem Details errors advertise only `application/problem+json`, and successful
responses do not advertise that error media type. For the generated controllers,
the configuration also removes the error media type from MVC route negotiation:
successful JSON/PDF responses keep their own content type even when a caller
prefers Problem Details, and bodyless 204 responses do not require a particular
Accept header. Exception handlers still explicitly select `application/problem+json`.
The docs describe the same API as the YAML but are springdoc's
rendering of it: OpenAPI 3.0.1 like the YAML, with an absolute server URL taken
from the request, and without the `X-Correlation-ID` response header (the
generator's annotations omit response headers). `springdoc.api-docs.version` is
set to `openapi_3_0` because springdoc's 3.1 default deprecates the schema
`example` keyword used by the generated models, which editor.swagger.io reports
as warnings. `MultipartOpenApiConfig` explicitly restores the required `Location`
header on 201 responses and `Retry-After` on business-operation 503 responses in the runtime docs.
Moving the contract to 3.1 waits for an OpenAPI Generator release
that keeps schema examples ([#24755](https://github.com/OpenAPITools/openapi-generator/issues/24755));
today it drops them. Share the YAML itself with API consumers.

| Variable | Default | Effect |
|---|---|---|
| `DIGITALFORMS_SWAGGER_ENABLED` | `false` | `true` enables Swagger UI at `/swagger-ui/index.html`. |
| `SPRINGDOC_APIDOCS_ENABLED` | `true` | `false` (PROD) turns off `/v3/api-docs`, `/v3/api-docs.yaml` and Swagger UI, which then return 404. The API itself is unaffected. |

| Response | Meaning |
|---|---|
| 200 | Successful GET; notices are resources without a `result` wrapper, and document collections are arrays (`[]` when empty). |
| 201 | Successful prohibition, impoundment or document POST, with a `Location` header and the saved resource ID. |
| 400 | Missing/malformed JSON or multipart data, invalid parameter types or failed request constraints. |
| 401 | Missing, invalid or expired bearer token, with a `WWW-Authenticate: Bearer` challenge. |
| 404 | Unknown route or missing requested notice/document. |
| 405 | Unsupported method, with an `Allow` header. |
| 406 / 415 | Unsupported response/request media type. |
| 409 | Confirmed duplicate prohibition or impoundment notice number. |
| 413 | Document exceeds 10 MiB or the multipart request exceeds 11 MiB. |
| 500 | VI/IRP processing/configuration failure, including unchanged handling when DIAM's signing keys are unreachable. |
| 502 | Invalid, malformed, empty or failed VIPS WS/ORDS response, including unexpected upstream HTTP errors. |
| 503 | VIPS WS/ORDS is unreachable or returns HTTP 429/503; includes `Retry-After`. |
| 504 | VIPS WS/ORDS timeout, exhausted VIPS GET retry time budget, or upstream HTTP 408/504. |

Errors use [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457),
rendered with Spring `ProblemDetail` and `Content-Type: application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Request validation failed.",
  "instance": "/digitalforms-viirp/v1/prohibitions",
  "errors": [
    {
      "field": "prohibition.prohibitionNoticeNo",
      "message": "must not be null"
    }
  ]
}
```

`type: about:blank` uses the HTTP status meaning; `title` is its reason phrase and
`status` matches the response status. `instance` is the original request path, without
the host or query parameters. Validation failures add an `errors` array instead of a
joined message: fields use JSON paths with array indexes, public parameter/header/part
names, or `body` for an unreadable or invalid whole request. Validation feedback does not
include rejected values or controller signatures. Local failures return `Internal Server Error`.
Upstream failures use the safe details `Invalid response from VIPS.`, `VIPS is temporarily unavailable.`,
or `VIPS did not respond in time.`; upstream bodies, URLs and diagnostics stay in server logs.

**Caller migration:** the legacy `{"status_message":"..."}` error envelope is removed.
Read `detail` for the summary and `errors[].field` / `errors[].message` for validation
feedback. Accept the success media type and `application/problem+json` (for example,
`Accept: application/json, application/problem+json`, or
`Accept: application/pdf, application/problem+json` for downloads).
Errors can return Problem Details even when the caller only accepts the success media
type. VIPS WS/ORDS failures previously reported as 500 now use 502/503/504 as above.
The `X-Correlation-ID` echo, Bearer `WWW-Authenticate` challenge, `Allow` header,
client-error statuses and binary PDF downloads are unchanged. JSON success responses follow
the resource contract below. Actuator
health retains its standard health payload, including `{"status":"DOWN"}` with 503.

#### Success responses and caller migration

`GET /prohibitions/{noticeNo}` and `GET /impoundments/{noticeNo}` return the existing
notice fields directly, without `{"result": ...}`. The two typed document collection
GETs return a JSON array rather than `{"results": [...]}`; a known notice with no
documents returns `[]`. Nested resources use the names and types in
[Consistent names and types](#consistent-names-and-types).
`GET /codetables` returns only active entries and active groups in `configuration`.
Configuration entries with a missing or null activity flag are excluded. Activity flags are omitted
from the response; callers should stop reading `active`, `noticeActive`,
`documentActive` and `reviewAppActive`. VIPS `respMsg` is not exposed.

For an impoundment, `judicialOrder` is a JSON boolean and is required when any
original cause has `objectType: "FVPA"` in the `IMP` group of
`configuration.originalCauses`. Either `true` or `false` satisfies the requirement.
It is optional for other causes. When omitted or null, the API reads the VIPS
configuration to classify the causes before creating the notice; if a cause cannot
be classified, correct it or supply `judicialOrder`. A failed or invalid
configuration response prevents creation rather than bypassing validation.
If `judicialOrder` is supplied, no configuration lookup is needed.

`configuration.impoundLotOperators` contains active operators with `identityId`,
`legalNm`, `businessNm`, address lines, `cityNm`, `provinceCd`, `postalCodeTxt`,
and `phoneNumbers` and `faxNumbers` arrays. Missing name/address fields are null;
the number arrays contain all available active values, or `[]`. These structured
fields replace the former combined `identityNm` label. The API retrieves them
using the VIPS WS `GET /configuration?includeImpoundLotOperatorDetails=true`
opt-in, with an 8 MiB response limit. VIPS WS uses its existing operator
lookup and authorization internally; an upstream failure, oversized response
or invalid result fails the entire code-table request rather than returning a
partial list. Deploy the VIPS WS extension first and verify the service
account's operator lookup access.

All three create operations return **201 Created** and **Location**. The header is a
root-relative URI that includes the servlet context path, so it works on the caller's
origin without depending on an internal host, scheme or proxy-forwarded headers:

| Create operation | Example `Location` |
|---|---|
| `POST /prohibitions` | `/digitalforms-viirp/v1/prohibitions/21900601` |
| `POST /impoundments` | `/digitalforms-viirp/v1/impoundments/00123456` |
| `POST /prohibitions/{noticeNo}/documents` or `POST /impoundments/{noticeNo}/documents` | `/digitalforms-viirp/v1/documents/102220` |

Resolve `Location` against the request URL and GET that URI using the same bearer
authentication. Notice resources use the **notice number**, not the internal ID.
Leading zeroes are preserved. Document resources use the document ID. An invalid
upstream document ID produces 502 rather than a 201 with an unusable link.

Notice-create bodies contain only `prohibitionId` or `impoundmentId`, with no
`warnings` or `respMsg`:

```json
{"prohibitionId":123456789012345}
```

**201** means the submission was saved; unsuccessful submissions use the documented
error responses. VIPS codes 11 and 29 also represent saved notices, so they retain
201, the saved ID and `Location`. Their diagnostics remain in server logs, not in the
public response. Callers must not resubmit these saved notices.

Manual vetting, approval and subsequent ICBC submission are separate from this API's
create contract. Neither manual-driver nor ICBC processing produces a caller-facing
warning in a successful create response.

These response changes do not alter approval rules, upstream calls or retry policies.
Uploads return the integer `{"documentId":102220}`.

#### Dates and timestamps

Date-only fields (`*Dt`, including `birthDt`) use OpenAPI `format: date`, for example
`"2026-11-05"`. `incidentDtm` and the document `*Dtm` fields use `format: date-time`:
an RFC 3339 timestamp with seconds and `Z` or a numeric offset, for example
`"2026-11-05T01:30:00-07:00"`.

| Direction | Dates | Timestamps |
|---|---|---|
| Request | Strict `YYYY-MM-DD`; impossible dates, timestamps, blanks and non-strings are rejected. | Seconds and an offset are required; fractions beyond milliseconds are truncated. |
| To VIPS WS | Midnight in the JVM default zone, e.g. `2025-12-15T00:00:00.000-08:00`, `2026-11-05T00:00:00.000-07:00`. | The caller's instant and offset, with milliseconds. |
| Response | The calendar date VIPS returned, without a zone conversion. | The instant and offset VIPS returned, with milliseconds; UTC is `Z`. |

VIPS WS binds every date as a timestamp and the create procedures truncate date-only
fields to the day, so the deployed container must run with `TZ=America/Vancouver`:
under UTC, a date would arrive as UTC midnight and be stored as the previous day. The
zone is a deployment setting rather than application code, so B.C.'s permanent UTC-7
and historical `-08:00` winter dates follow the JDK's timezone data. Reading only the
calendar date keeps historical winter dates correct even if VIPS WS reports them with
a fixed `-07:00`. An invalid request value returns 400 with the field path and the
expected format, without echoing the value, and VIPS is not called. A malformed date or
timestamp from VIPS returns 502 `Invalid response from VIPS.`

**Caller migration:** send dates such as `"birthDt": "1974-05-25"` instead of
`"1974-05-25T00:00:00.000-07:00"`; the old form now returns 400. Timestamps no longer
need exactly three fraction digits or a negative offset. Responses return dates as
`YYYY-MM-DD` and timestamps with milliseconds and an offset.

#### Consistent names and types

The public contract uses camelCase names without VIPS prefixes or `Obj`/`YN` suffixes,
JSON booleans for flags and an integer document ID. The private VIPS request adapters
and the client-only `VipsJson` module translate these to the historical VIPS WS
property names and `"Y"`/`"N"` strings. The VIPS WS contract, ORDS client and database
are unchanged.

| Before | Now |
|---|---|
| `vipsDocumentIdArray` | No public field; create the notice before uploading its documents |
| `vipsProhibitionCreate`, `vipsRegistrationCreate` | `prohibition`, `registration` |
| `vipsImpoundCreate`, `vipsRegistrationCreateArray`, `vipsVehicleCreate` | `impoundment`, `registrations`, `vehicle` |
| `vipsImpoundmentArray` (later `priorImpoundmentIds`) | Removed from create requests; the API supplies `[]` to VIPS |
| `vipsAddressArray`, `vipsLicenceObj` | `addresses`, `licence` |
| `policeDetatchmentId` | `policeDetachmentId` |
| `driverLicenceSeizedYN: "Y"` / `"N"` | `driverLicenceSeized: true` / `false` |
| `disclosureDocumentYN` | `disclosureDocument` (boolean) |
| Code tables `data_sources`, `registration_roles` | `dataSources`, `registrationRoles` |
| Code-table `internalYN` | `internal` (boolean) |
| Upload parts `type_code`, `notice_type_code`, `notice_subject_code` | `documentTypeCd`, `noticeTypeCd`, `noticeSubjectCd` |
| Upload response `{"document_id":"102220"}` | `{"documentId":102220}` (int64) |
| Impoundment `driverLicenceNo` maximum 8; licence `driverLicenceNo` maximum 10 | Both maximum 20 |
| Impoundment `policeOfficerNm` maximum 100 | Maximum 60, matching the VIPS column |

OpenAPI schema names also drop the VIPS naming, which changes generated client type
names:

| Before | Now |
|---|---|
| `VipsProhibitionCreate`, `VipsProhibitionObj` | `ProhibitionCreate`, `Prohibition` |
| `VipsImpoundmentCreate`, `VipsImpoundObj` | `ImpoundmentCreate`, `Impoundment` |
| `VipsRegistrationCreate`, `VipsRegistrationObj` | `RegistrationCreate`, `Registration` |
| `VipsAddressCreateObj`, `VipsAddressObj` | `AddressCreate`, `Address` |
| `VipsLicenceCreate`, `VipsLicenceObj` | `LicenceCreate`, `Licence` |
| `VipsVehicleCreate`, `VipsVehicleObj` | `VehicleCreate`, `Vehicle` |
| `VipsDocumentObj`, `storeVIPSDocument`, `vipsDocumentResponse` | `Document`, `DocumentUpload`, `CreateDocumentResponse` |
| `CreateProhibitionServiceResponse`, `CreateImpoundmentServiceResponse` | `CreateProhibitionResponse`, `CreateImpoundmentResponse` |
| `GetCodetablesServiceResponse`, `VipsConfigurationObj` | `CodeTablesResponse`, `CodeTables` |
| `VipsCodeDescriptionObj`, `VipsCodeDescAbbrevObj`, `VipsIdNameObj` | `CodeDescription`, `AbbreviatedCodeDescription`, `IdentityName` |
| `VipsCodeDescByNoticeObj`, `VipsCodeDescAbbrByNoticeObj` | `NoticeTypeCodes`, `NoticeTypeAbbreviatedCodes` |
| `VipsCodeDescByDocObj`, `VipsCodeDescByRVWAppObj` | `DocumentTypeCodes`, `ReviewApplicationTypeCodes` |

Request booleans must be JSON `true` or `false`; `"Y"`, `"true"`, `1` and other values
return 400 with the field path and `Must be true or false.` A required boolean that is
missing or `null` returns the usual required-field error. Validation field paths use
the new names, for example `registrations[0].licence.driverLicenceNo`. A VIPS WS flag
other than `"Y"`, `"N"` or `null` returns 502 `Invalid response from VIPS.`

**Caller migration:** rename request and response properties, multipart parts and
generated client types as above; send booleans instead of `"Y"`/`"N"`, and read the
upload `documentId` as a number. Old names are not accepted as aliases: an unknown
property is ignored, so a required field sent only under its old name returns 400.

#### Server-owned inputs and request enums

Callers no longer send these fields when creating a notice:

| Removed public input | Value supplied to VIPS WS by the API |
|---|---|
| `impoundment.noticeTypeCd` | `"IMP"` |
| `impoundment.noticeSubjectCd` | `"VEHI"` |
| `priorImpoundmentIds` | `"vipsImpoundmentArray": []` |
| `prohibition.noticeSubjectCd` | `"PERS"` |

These are server-owned values, not overridable defaults. If a caller still supplies
a removed field, it is ignored like any other unknown JSON property, even if it is
`null` or has the wrong type. It cannot change the downstream values. Private create
request adapters supply the VIPS-only fields without adding them to the public
models. Notice GET responses still include `noticeTypeCd` and `noticeSubjectCd`.

The remaining documented fixed code sets are enforced as OpenAPI enums:

| Request field | Allowed values |
|---|---|
| `prohibition.noticeTypeCd` | `UL`, `IRP`, `ADP` |
| Each create address's `registrationAddressTypeCd` | `RES`, `MAIL`, `BUSI` |

Codes are case-sensitive and are not trimmed. Unsupported codes, blank values,
and non-string JSON values return 400 Problem Details with the public field path
and the allowed values, without echoing the rejected value or calling VIPS/ORDS.
Required JSON enums still reject missing/null values.

`dataSourceCd` remains a required registration field with the existing four-character
limit; the separate Digital Forms source-lifecycle change is not included here.
Dynamic code-table values and all GET response types are unchanged.

**Caller migration:** remove the four server-owned fields, use the exact enum
values above, and regenerate clients from the updated specification.

#### Optional prohibition fields

`cityNm`, `provinceCd`, `streetDetailsTxt`, `policeOfficerNm`, `policeOfficerNo`,
`expiryDt`, `tempDlEffectiveDt`, and `tempDlExpiryDt` are optional on prohibition
creation and returned on retrieval. Dates use `YYYY-MM-DD`.

The VIPS UI does not currently display these fields. A subsequent edit in VIPS
may clear them; coordinate preservation with the VIPS team before relying on
their values after an edit.

#### Upstream failures and retries

All business operations distinguish dependency failures from local VI/IRP errors.
Classification is confined to the VIPS WS/ORDS call and response-decoding boundaries;
an arbitrary application exception does not become a gateway error. Failed VIPS
business response codes are 502 unless an existing rule establishes 404/409, or a
saved-with-warning create (codes 11/29). Existing empty document lists remain successful.

A business-operation 503 always includes `Retry-After`: a valid upstream delay in
seconds or HTTP date is retained; missing, invalid or repeated values fall back to
`30` seconds. This is retry advice, not a guarantee of recovery or proof that a write
was not committed. No new environment variables are required.

The VIPS WebClient still retries only GET/HEAD, subject to its existing attempt and
time limits. VIPS POSTs are not re-sent. The shared ORDS client's transport/retry
policy is unchanged, including its existing handling of HTTP 408; the VI/IRP adapter
adds no retries. After an ambiguous create failure, retrieve the notice before
resubmitting. Do not blindly repeat a document upload, which can create a duplicate.

#### Uploading a document

Create the notice first, without `documentIds`, then upload each PDF using
`POST /prohibitions/{noticeNo}/documents` or `POST /impoundments/{noticeNo}/documents`.
These replace the standalone `POST /documents` and both document-association PUTs.
The new POSTs accept **multipart/form-data only**, with exactly one PDF file.
When importing the OpenAPI YAML into Postman, generate request parameters from
**Examples**, not the schema. This uses the sample `noticeNo` and `documentTypeCd`
instead of generating text from their validation patterns.
In Postman, choose **Body > form-data**, set `file` to **File**, and use **Text**
for the other parts. Let Postman set the Content-Type header and multipart boundary.
Send the OAuth2 bearer token and the optional `X-Correlation-ID` header as for other operations.

| Part | Required | Value |
|---|---|---|
| `file` | Yes | A non-empty, readable PDF with 1 to 99,999 pages; at most 10 MiB (10,485,760 bytes). |
| `documentTypeCd` | Yes | Document classification, e.g. `MV2723`; nonblank, at most 10 characters. |

After both upload and association succeed, the response is **HTTP 201** with a `Location` header pointing to the PDF download
(for example, `/digitalforms-viirp/v1/documents/102220`) and an integer (int64) document ID:

```json
{"documentId":102220}
```

The API validates the PDF contents with PDFBox; it does not trust the filename or
part Content-Type. Files requiring a password to open, malformed PDFs, PDFs without
pages or with more than 99,999 pages, missing/empty files, and multiple file parts are
rejected with 400 before calling ORDS. File parts under other names are not silently
ignored. This is format validation, not malware scanning or a guarantee that every
page can be rendered.

The page count is read from the PDF and forwarded to ORDS as `page_count`; VIPS stores
it in `VIPS_DOCUMENTS.PAGE_COUNT_QTY` (`NUMBER(5)`, hence the 99,999-page limit) and
shows it in the document collections returned by `GET /prohibitions/{noticeNo}/documents`
and `GET /impoundments/{noticeNo}/documents`.
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

The API looks up the notice before uploading. It obtains the notice type from VIPS
and derives the subject from the route (`PERS` for prohibitions; `VEHI` for impoundments).
The ORDS upload and VIPS WS association are separate writes until an atomic database
operation is available. If association fails, the API returns the error, not a document
ID or `Location`, and logs the uploaded ID with the correlation ID for reconciliation.
The PDF may already be stored, and a timed-out association may have succeeded. Check
the notice's document list and contact support with the correlation ID before retrying;
repeating the POST can create a duplicate.

`GET /documents/{documentId}` downloads the original document as `application/pdf`
with an attachment filename. VI/IRP requests the raw PDF from VIPS and forwards its
data buffers as a streaming resource; it does not create an expanded Base64 string or
hold a second full-document copy in memory. It checks the five-byte PDF signature
before committing a download, so a non-PDF error page becomes 502 and an initial
body timeout becomes 504. If the transfer fails after the HTTP response has already
been committed, the transfer terminates; its status cannot be replaced with Problem
Details. Callers must treat an interrupted or truncated download as a failure.

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
Only a confirmed existing notice becomes 409; failed upstream lookups use 502/503/504
and unrelated VI/IRP errors remain 500. Oracle uniqueness constraints remain the final guard.
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

Both Spring test starters exclude JSONAssert's transitive `android-json` dependency.
Tests retain JSONAssert but use the same `org.json:json` library supplied by the
ORDS client in production, avoiding duplicate `org.json.JSONObject` implementations
and classpath-order-dependent behavior. `JsonClasspathTest` guards against duplicates.
After changing these dependencies, reload the Maven project in your IDE before
rerunning JUnit tests.

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
- All public operations, including notice-scoped document uploads, retrieval, and listing,
  code tables and ORDS ping.
- Single-PDF multipart upload, optional metadata and exact binary forwarding to ORDS;
  rejection of multiple files, invalid PDFs, malformed multipart data and legacy JSON.
  The 10 MiB acceptance boundary and one-byte-over rejection are exercised over HTTP.
- Missing/malformed bodies, required fields, parameter constraints, media types,
  duplicates/concurrent creates and unexpected downstream response codes.
- VIPS WS/ORDS 502/503/504 Problem Details, safe `Retry-After` handling, malformed
  response JSON, unavailable connections, connect/read/write timeout classification,
  GET retry exhaustion, POST read timeouts and failures before PDF streaming commits.
  Local processing/configuration errors and DIAM signing-key failures remain 500.
- Every required field in both create requests, with cases derived from the YAML:
  missing/null values, empty/space/tab/newline-only required strings, empty required
  objects and collections with `minItems`. Multiple registrations and addresses
  exercise nested validation beyond the first array element. Invalid requests must
  return 400 identifying the field without reaching VIPS. Positive controls preserve
  optional licence objects and allowed empty arrays; separate cases cover the
  impoundment-only requirement for a licence number when a licence object is supplied.
- Oracle collection boundaries: at most five addresses per registration on either
  create endpoint, and at most ten registrations per impoundment. Requests at the
  limits reach VIPS intact; one-over requests return field-specific 400 responses
  without reaching VIPS.
- Every date and timestamp field in both create requests, found from the YAML's
  formats: impossible, partial, legacy-timestamp, blank and non-string values return a
  field-specific 400 without reaching VIPS. Dates reach VIPS as B.C. midnight, including
  `-08:00` before 2026-11-01 and `-07:00` afterwards (the tests set and restore
  `America/Vancouver`); timestamps keep their offset with milliseconds. Reads return
  VIPS dates without a zone shift, timestamps with their VIPS offset, and 502 for a
  malformed VIPS date. Runtime OpenAPI keeps the contract's 21 formats.
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

CSRF protection stays enabled. Spring Security's OAuth2 resource-server support
automatically exempts requests carrying a bearer token in the `Authorization`
header; those tokens are still validated before access is granted. API callers
do not need a CSRF token. Cookie authentication is not configured, and access
tokens in query strings or form bodies are not accepted. Do not globally disable
CSRF protection.

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
