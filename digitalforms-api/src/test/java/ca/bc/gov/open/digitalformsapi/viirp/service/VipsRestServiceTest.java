package ca.bc.gov.open.digitalformsapi.viirp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Base64;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import ca.bc.gov.open.digitalformsapi.viirp.model.GetCodetablesServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsConfigurationObj;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsGetDocumentByIdResponse;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;
import reactor.core.publisher.Mono;

//@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles(value = "test")
public class VipsRestServiceTest {

	@MockBean
	WebClient webClient;

	@SuppressWarnings("rawtypes")
	@Mock
	WebClient.RequestHeadersSpec requestHeadersMock;
	@SuppressWarnings("rawtypes")
	@Mock
	WebClient.RequestHeadersUriSpec requestHeadersUriMock;
	@Mock
	WebClient.RequestBodySpec requestBodyMock;
	@Mock
	WebClient.RequestBodyUriSpec requestBodyUriMock;
	@Mock
	WebClient.ResponseSpec responseMock;

	@Autowired
	VipsRestService service;

	@BeforeEach
	public void setUp() {
		openMocks(this);
	}

	@AfterEach
	public void tearDown() {

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetCodeTableValues() {

		String correlationId = DigitalFormsConstants.REQUEST_CORRELATION_ID;

		GetCodetablesServiceResponse codeTablesServiceResponse = new GetCodetablesServiceResponse();
		VipsConfigurationObj configuration = new VipsConfigurationObj();

		codeTablesServiceResponse.setRespMsg("Success");
		codeTablesServiceResponse.setConfiguration(configuration);

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri("/configuration")).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(GetCodetablesServiceResponse.class))
				.thenReturn(Mono.just(codeTablesServiceResponse));

		var result = service.getCodeTableValues(correlationId);

		assertThat(result).isNotNull();
		assertThat(result.getRespMsg()).isEqualTo("Success");
		assertThat(result.getConfiguration()).usingRecursiveComparison().isEqualTo(configuration);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getDocumentAsBase64() {

		String correlationId = DigitalFormsConstants.UNIT_TEST_CORRELATION_ID;
		Long documentId = DigitalFormsConstants.UNIT_TEST_DOCUMENT_ID;

		VipsGetDocumentByIdResponse vipsDocumentResponse = new VipsGetDocumentByIdResponse();

		Base64.Encoder enc = Base64.getEncoder();
		String testStr = "77+9x6s=";
		// encode data using BASE64
		String encodedBase64 = enc.encodeToString(testStr.getBytes());
		vipsDocumentResponse.setDocument(encodedBase64);

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri("/documents/" + documentId + "?b64=true&url=false"))
				.thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.header(any(String.class), any(String.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.header(any(String.class), any(String.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.header(any(String.class), any(String.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just(encodedBase64));

		var result = service.getDocumentAsBase64(correlationId, documentId);

		assertThat(result).isNotNull();
		assertThat(result.getDocument()).isEqualTo(encodedBase64);
		assertThat(result).usingRecursiveComparison().isEqualTo(vipsDocumentResponse);
	}
}
