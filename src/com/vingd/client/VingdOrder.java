package com.vingd.client;

public class VingdOrder {
	private long id;
	private long oid;
	private int price;
	private String context;
	private String expiresTimestamp;
	private String frontendURL;

	public VingdOrder(long id, long oid, int price, String context, String expiresTimestamp, String frontendURL) {
		this.id = id;
		this.oid = oid;
		this.price = price;
		this.context = context;
		this.expiresTimestamp = expiresTimestamp;
		this.frontendURL = frontendURL;
	}

	public long getOrderID() {
		return id;
	}

	public long getObjectID() {
		return oid;
	}

	public int getPrice() {
		return price;
	}

	public String getContext() {
		return context;
	}

	public String getExpiresTimestamp() {
		return expiresTimestamp;
	}

	public String getFrontendURL() {
		return frontendURL;
	}

	public String getRedirectURL() {
		return frontendURL + "/orders/" + id + "/add/";
	}
	
	public String getPopupURL() {
		return frontendURL + "/popup/orders/" + id + "/add/";
	}
	
	public String toString() {
		return "Order (ID = "+id+") for object with OID = "+oid+", with price of "+price+" vingd cents that expires on "+expiresTimestamp;
	}
}