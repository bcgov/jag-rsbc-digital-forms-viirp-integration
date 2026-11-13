package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "management.health.diskspace.threshold=9223372036854775807B")
class ActuatorHealthDownHttpTests extends HttpApiTestSupport {

    @Test
    void failedDiskSpaceCheckStillReturns503WithoutExposingDetails() throws Exception {
        for (String authorization : new String[] {null, AUTH}) {
            var health = responseJson(request("GET", "/actuator/health", null, authorization), 503);
            assertEquals("DOWN", health.path("status").asString());
            assertTrue(Set.of("status", "groups").containsAll(health.propertyNames()), health.toString());
        }
    }
}
