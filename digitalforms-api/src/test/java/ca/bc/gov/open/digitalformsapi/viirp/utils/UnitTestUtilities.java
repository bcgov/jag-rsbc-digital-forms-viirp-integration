package ca.bc.gov.open.digitalformsapi.viirp.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import tools.jackson.databind.json.JsonMapper;

/**
 * 
 * @author 176899
 *
 */
public class UnitTestUtilities {

    private static final JsonMapper JSON = JsonMapper.builder().addModule(TemporalJson.publicModule()).build();

    public static byte[] samplePdf() throws IOException {
        return samplePdf(1);
    }

    public static byte[] samplePdf(int pages) throws IOException {
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
            for (int i = 0; i < pages; i++) {
                document.addPage(new PDPage());
            }
            document.save(output);
            return output.toByteArray();
        }
    }
	
	/**
     * Utility method to convert JSOB object to string. 
     * @param obj
     * @return
     */
    public static String asJsonString(final Object obj) {
        try {
            return JSON.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
