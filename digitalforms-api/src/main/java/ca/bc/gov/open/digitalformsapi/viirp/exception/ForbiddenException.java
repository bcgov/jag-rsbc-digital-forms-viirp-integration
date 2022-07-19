package ca.bc.gov.open.digitalformsapi.viirp.exception;

/**
 * 
 * Forbidden Exception
 * 
 * Returns HTTP Status code 403 and 'Forbidden' string. 
 * 
 * @author 176899
 *
 */
public class ForbiddenException extends RuntimeException {

	private static final long serialVersionUID = 8111046986001984816L;

	public ForbiddenException(String msg) {
		super(msg);
	}
}