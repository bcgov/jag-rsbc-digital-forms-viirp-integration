package ca.bc.gov.open.digitalformsapi.viirp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "management.health.diskspace.threshold=9223372036854775807B")
class ActuatorHealthDownHttpTests extends HttpApiTestSupport {

    @Test
    void failedDiskSpaceCheckStillReturns503WithoutExposingDetails() throws Exception {
        for (String authorization : new String[] {null, AUTH}) {
            vips.enqueue(200, "{\"status\":\"success\"}");
            var response = request("GET", "/actuator/health", null, authorization);
            assertEquals(503, response.statusCode());
            assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
            var health = json.readTree(response.body());
            assertEquals("DOWN", health.path("status").asString());
            assertTrue(Set.of("status", "groups").containsAll(health.propertyNames()), health.toString());
            assertTrue(vips.requests.poll(2, TimeUnit.SECONDS) != null);
        }
    }
}
