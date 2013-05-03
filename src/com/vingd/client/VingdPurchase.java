package com.vingd.client;

public class VingdPurchase {
	private String huid;
	private long oid;
	private long orderid;
	private long purchaseid;
	private long transferid;
	private String context;

	public VingdPurchase(String huid, long oid, long orderid, long purchaseid, long transferid, String context) {
		this.huid = huid;
		this.oid = oid;
		this.orderid = orderid;
		this.purchaseid = purchaseid;
		this.transferid = transferid;
		this.context = context;
	}

	public String getHUID() {
		return huid;
	}

	public long getObjectID() {
		return oid;
	}

	public long getOrderID() {
		return orderid;
	}

	public long getPurchaseID() {
		return purchaseid;
	}

	public long getTransferID() {
		return transferid;
	}

	public String getContext() {
		return context;
	}
}
