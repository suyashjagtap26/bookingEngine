package com.coxandkings.travel.bookingengine.orchestrator.accoV2.enums;

public enum Status {
	CONF("Confirmed"), RESERVED("On Request") ,NOTCONF("On Request") ,CANCONF("Cancelled"), CON("Confirmed"),NOTCON("On Request");

	private String mStatus;
	
	private Status(String status) {
		this.mStatus=status;
	}

	private String getStatus() {
		return mStatus;
	}
	
	public static String getValue(Status status) {
		if(status==null)
			return "";
		return status.getStatus()==null?"":status.getStatus();	
	}
	
}
