package ca.bc.gov.open.digitalformsapi.viirp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import ca.bc.gov.open.digitalformsapi.viirp.model.CodeTablesResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.CodeTables;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

//@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles(value = "test")
public class VipsRestServiceTest {

	@MockitoBean
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
	@Mock
	WebClient.Builder webClientBuilderMock;

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

		CodeTablesResponse codeTablesServiceResponse = new CodeTablesResponse();
		CodeTables configuration = new CodeTables();
		configuration.setImpoundLotOperators(java.util.List.of());

		codeTablesServiceResponse.setConfiguration(configuration);

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri("/configuration?includeImpoundLotOperatorDetails=true"))
				.thenReturn(this.requestHeadersMock);
		when(this.webClient.mutate()).thenReturn(this.webClientBuilderMock);
		when(this.webClientBuilderMock.codecs(any())).thenReturn(this.webClientBuilderMock);
		when(this.webClientBuilderMock.build()).thenReturn(this.webClient);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.header(any(String.class), any(String.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(JsonNode.class))
				.thenReturn(Mono.just(JsonMapper.builder().build().readTree(
						"{\"respCd\":0,\"configuration\":{\"impoundLotOperatorDetails\":[]}}")));

		var result = service.getCodeTableValues();

		assertThat(result).isNotNull();
		assertThat(result.getConfiguration()).usingRecursiveComparison().isEqualTo(configuration);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getDocumentStreamsPdfBytes() {

		Long documentId = DigitalFormsConstants.UNIT_TEST_DOCUMENT_ID;
		byte[] pdf = "pdf bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		ResponseEntity<Flux<DataBuffer>> expected = ResponseEntity.ok()
				.body(Flux.<DataBuffer>just(DefaultDataBufferFactory.sharedInstance.wrap(pdf)));

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri("/documents/" + documentId + "?b64=false&url=false"))
				.thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.header(any(String.class), any(String.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.header(any(String.class), any(String.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.header(any(String.class), any(String.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
		when(this.responseMock.toEntityFlux(DataBuffer.class)).thenReturn(Mono.just(expected));

		var result = service.getDocument(documentId);

		assertThat(result).isNotNull();
		var buffer = result.getBody().blockFirst();
		byte[] actual = new byte[buffer.readableByteCount()];
		buffer.read(actual);
		DataBufferUtils.release(buffer);
		assertThat(actual).containsExactly(pdf);
	}
}
