package ca.bc.gov.open.digitalformsapi.viirp.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

import ca.bc.gov.open.digitalformsapi.viirp.service.VipsOrdsService;

@Component
public class VipsOrdsHealthIndicator extends AbstractHealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(VipsOrdsHealthIndicator.class);
    private final VipsOrdsService ords;

    public VipsOrdsHealthIndicator(VipsOrdsService ords) {
        super("VIPS ORDS health check failed");
        this.ords = ords;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        var response = ords.health();
        // VIPS_REST.health_check returns "success", not Actuator's "UP".
        if ("success".equals(response.getStatus())) {
            builder.up();
        } else {
            logger.warn("VIPS ORDS health check did not report success");
            builder.down();
        }
    }
}
