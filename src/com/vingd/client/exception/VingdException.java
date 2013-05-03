package com.vingd.client.exception;

import org.apache.commons.httpclient.HttpStatus;

public class VingdException extends Exception {
	private static final long serialVersionUID = 1754779891169930445L;

	private String context;
	private int code;

	public VingdException(String message, Throwable cause) {
		this(message, null, HttpStatus.SC_CONFLICT, cause);
	}

	public VingdException(String message, String context) {
		this(message, context, HttpStatus.SC_CONFLICT, null);
	}

	public VingdException(String message, String context, int code) {
		this(message, context, code, null);
	}

	public VingdException(String message, String context, int code, Throwable cause) {
		super(message, cause);
		this.context = context;
		this.code = code;
	}

	public String toString() {
		return this.getClass().getName() + ": " + (context != null ? "[" + context + "]: " : "") + getMessage() + " (" + code + " " + HttpStatus.getStatusText(code) + ")";
	}
}
