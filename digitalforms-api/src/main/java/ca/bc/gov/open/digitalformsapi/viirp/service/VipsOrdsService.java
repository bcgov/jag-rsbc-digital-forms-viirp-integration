package ca.bc.gov.open.digitalformsapi.viirp.service;

import java.io.IOException;

import org.springframework.stereotype.Service;

import com.google.gson.JsonParseException;

import ca.bc.gov.open.digitalformsapi.viirp.config.ConfigProperties;
import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.UpstreamServiceException;
import ca.bc.gov.open.jag.ordsvipsclient.api.DocumentApi;
import ca.bc.gov.open.jag.ordsvipsclient.api.HealthApi;
import ca.bc.gov.open.jag.ordsvipsclient.api.handler.ApiClient;
import ca.bc.gov.open.jag.ordsvipsclient.api.handler.ApiException;
import ca.bc.gov.open.jag.ordsvipsclient.api.model.HealthOrdsResponse;
import ca.bc.gov.open.jag.ordsvipsclient.api.model.VipsDocumentOrdsResponse;
import okhttp3.Call;
import okhttp3.Response;

@Service
public class VipsOrdsService {

    private final DocumentApi documents;
    private final HealthApi health;
    private final ConfigProperties properties;

    public VipsOrdsService(DocumentApi documents, HealthApi health, ConfigProperties properties) {
        this.documents = documents;
        this.health = health;
        this.properties = properties;
    }

    public VipsDocumentOrdsResponse storeDocument(String typeCode, byte[] content,
            String noticeTypeCode, String noticeSubjectCode, int pageCount) {
        String authGuid = properties.getVipsRestApiCredentialsGuid();
        // Preserve the generated convenience method's required-argument checks when building the call directly.
        if (typeCode == null || content == null || authGuid == null) {
            throw new DigitalFormsException("Required VIPS ORDS upload parameters are missing.");
        }
        try {
            return execute(documents.getApiClient(), documents.storeDocumentPostCall(typeCode, "application", "pdf",
                    authGuid, content, noticeTypeCode, noticeSubjectCode, pageCount, null), VipsDocumentOrdsResponse.class);
        } catch (ApiException ex) {
            throw UpstreamServiceException.from(ex);
        }
    }

    public HealthOrdsResponse health() {
        try {
            return execute(health.getApiClient(), health.healthCall(null), HealthOrdsResponse.class);
        } catch (ApiException ex) {
            throw UpstreamServiceException.from(ex);
        }
    }

    private <T> T execute(ApiClient client, Call call, Class<T> responseType) throws ApiException {
        try (Response response = call.execute()) {
            // The generated decoder also throws IllegalArgumentException/IllegalStateException for bad JSON shapes.
            // Keep that classification here, separate from local request construction and execution errors.
            try {
                T body = client.handleResponse(response, responseType);
                if (body == null) {
                    throw UpstreamServiceException.invalidResponse("VIPS ORDS returned an empty response.");
                }
                return body;
            } catch (JsonParseException | IllegalArgumentException | IllegalStateException ex) {
                throw UpstreamServiceException.invalidResponse("Invalid VIPS ORDS response.", ex);
            }
        } catch (IOException ex) {
            throw UpstreamServiceException.from(ex);
        }
    }
}
