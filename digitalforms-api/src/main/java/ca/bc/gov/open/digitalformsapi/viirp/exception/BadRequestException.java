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

	private final String field;

	public BadRequestException(String msg) {
		this("body", msg);
	}

	public BadRequestException(String field, String msg) {
		super(msg);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}