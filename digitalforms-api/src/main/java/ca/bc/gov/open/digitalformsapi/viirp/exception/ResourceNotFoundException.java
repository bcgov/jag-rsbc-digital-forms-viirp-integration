package ca.bc.gov.open.digitalformsapi.viirp.exception;

/**
 * 
 * Returns HTTP Status code 404 and 'Not Found' string.  
 * 
 * @author 176899
 *
 */
public class ResourceNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 8111046986001984816L;

	public ResourceNotFoundException(String msg) {
		super(msg);
	}
}