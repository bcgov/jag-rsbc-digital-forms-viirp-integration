package ca.bc.gov.open.digitalformsapi.viirp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import ca.bc.gov.open.digitalformsapi.viirp.model.GetCodetablesServiceResponse;
import ca.bc.gov.open.digitalformsapi.viirp.model.VipsConfigurationObj;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;
import reactor.core.publisher.Mono;

@RunWith(SpringRunner.class)
@SpringBootTest
public class VipsRestServiceTest {
	
	@MockBean 
	WebClient webClient;
	
	@SuppressWarnings("rawtypes")
	@Mock WebClient.RequestHeadersSpec requestHeadersMock;
    @SuppressWarnings("rawtypes")
	@Mock WebClient.RequestHeadersUriSpec requestHeadersUriMock;
    @Mock WebClient.RequestBodySpec requestBodyMock;
    @Mock WebClient.RequestBodyUriSpec requestBodyUriMock;
    @Mock WebClient.ResponseSpec responseMock;
    
    @Autowired
	VipsRestService service;
    
    @Before
    public void setUp() {
        openMocks(this);
    }

    @After
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
        when(this.responseMock.bodyToMono(GetCodetablesServiceResponse.class)).thenReturn(Mono.just(codeTablesServiceResponse));
        
        var result = service.getCodeTableValues(correlationId);
        
        assertThat(result).isNotNull();
        assertThat(result.getRespMsg()).isEqualTo("Success");
        assertThat(result.getConfiguration()).usingRecursiveComparison().isEqualTo(configuration);
	}
}
