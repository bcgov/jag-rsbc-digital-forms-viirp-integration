package ca.bc.gov.open.digitalformsapi.viirp.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {
	
	static final Logger log = LoggerFactory.getLogger(ApplicationStartup.class);

	/**
	 * This event is executed as late as conceivably possible to indicate that the
	 * application is ready to service requests.
	 */
	@Override
	public void onApplicationEvent(final ApplicationReadyEvent event) {
		
		String splunk_url = System.getenv("SPLUNK_URL");
		log.info("Splunk URL is: " + splunk_url);
		return;
	}
}
