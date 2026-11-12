package ca.bc.gov.open.digitalformsapi.viirp.exception;

/**
 * 
 * Returns HTTP Status code 400. 
 * 
 * @author 176899
 *
 */
public class BadRequestException extends RuntimeException {

	private static final long serialVersionUID = 8111046986001984816L;

	public BadRequestException(String msg) {
		super(msg);
	}
}