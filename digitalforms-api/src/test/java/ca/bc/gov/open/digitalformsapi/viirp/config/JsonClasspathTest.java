package ca.bc.gov.open.digitalformsapi.viirp.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class JsonClasspathTest {

    @Test
    void jsonObjectHasOnlyOneImplementationOnTheTestClasspath() throws Exception {
        var implementations = Collections.list(JSONObject.class.getClassLoader()
                .getResources("org/json/JSONObject.class"));

        assertThat(implementations)
                .as("Tests must not shadow the production org.json library with android-json")
                .hasSize(1);
    }
}
