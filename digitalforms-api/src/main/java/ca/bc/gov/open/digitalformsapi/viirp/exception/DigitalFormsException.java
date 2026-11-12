package ca.bc.gov.open.digitalformsapi.viirp.exception;

/**
 * 
 * Generic Exception for Digital Forms
 * 
 * Returns HTTP Status code 500 and message string. 
 * 
 * @author 176899
 *
 */
public class DigitalFormsException extends RuntimeException {

	private static final long serialVersionUID = 5873442413088571528L;

	public DigitalFormsException(String message) {
		super(message);
	}

	public DigitalFormsException(String message, Throwable cause) {
		super(message, cause);
	}

}