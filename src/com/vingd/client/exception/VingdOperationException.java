package com.vingd.client.exception;

public class VingdOperationException extends VingdException {
	private static final long serialVersionUID = -1025693035911840338L;

	public VingdOperationException(String message, Throwable cause) {
		super(message, cause);
	}

	public VingdOperationException(String message, String context) {
		super(message, context);
	}

	public VingdOperationException(String message, String context, int code) {
		super(message, context, code);
	}

	public VingdOperationException(String message, String context, int code, Throwable cause) {
		super(message, context, code, cause);
	}
}