package com.coxandkings.travel.bookingengine.orchestrator.air.enums;

public enum TripType {
	ONEWAY("OneWay"), RETURN("Return"), MULTICITY("Multicity");
	
	private String mTripTypeCode;
	private TripType(String tripTypeCode) {
		mTripTypeCode = tripTypeCode;
	}
	
	public String toString() {
		return mTripTypeCode;
	}
	
	public static TripType forString(String tripTypeStr) {
		if (tripTypeStr == null || tripTypeStr.isEmpty()) {
			return null;
		}
		
		TripType[] tripTypes = TripType.values();
		for (TripType tripType : tripTypes) {
			if (tripType.toString().equalsIgnoreCase(tripTypeStr)) {
				return tripType;
			}
		}
		
		return null;
	}
}
