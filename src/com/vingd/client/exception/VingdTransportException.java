package com.vingd.client.exception;

public class VingdTransportException extends VingdException {
	private static final long serialVersionUID = -840964751431872479L;

	public VingdTransportException(String message, Throwable cause) {
		super(message, cause);
	}

	public VingdTransportException(String message, String context) {
		super(message, context);
	}

	public VingdTransportException(String message, String context, int code) {
		super(message, context, code);
	}

	public VingdTransportException(String message, String context, int code, Throwable cause) {
		super(message, context, code, cause);
	}
}