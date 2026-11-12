package ca.bc.gov.open.digitalformsapi.viirp.exception;

/**
 * 
 * Returns HTTP Status code 401 and 'The requester is unauthorized' string. 
 * 
 * @author 176899
 *
 */
public class UnauthorizedException extends RuntimeException {

	private static final long serialVersionUID = 8111046986001984816L;

	public UnauthorizedException(String msg) {
		super(msg);
	}
}