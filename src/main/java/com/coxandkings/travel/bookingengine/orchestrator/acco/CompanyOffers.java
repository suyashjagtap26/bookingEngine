package com.coxandkings.travel.bookingengine.orchestrator.acco;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONPointer;
import org.json.JSONTokener;

import com.coxandkings.travel.bookingengine.config.ServiceConfig;
import com.coxandkings.travel.bookingengine.config.acco.AccoConfig;
import com.coxandkings.travel.bookingengine.enums.CommercialsOperation;
import com.coxandkings.travel.bookingengine.enums.OffersType;
import com.coxandkings.travel.bookingengine.enums.PassengerType;
import com.coxandkings.travel.bookingengine.userctx.ClientInfo;
import com.coxandkings.travel.bookingengine.userctx.OrgHierarchy;
import com.coxandkings.travel.bookingengine.userctx.UserContext;
import com.coxandkings.travel.bookingengine.utils.HTTPServiceConsumer;
import com.coxandkings.travel.bookingengine.utils.redis.RedisCityData;
import com.coxandkings.travel.bookingengine.utils.redis.RedisHotelData;
import com.coxandkings.travel.bookingengine.utils.redis.RedisRoEData;

public class CompanyOffers implements AccoConstants {

	private static final Logger logger = LogManager.getLogger(CompanyOffers.class);

	public static void getCompanyOffers(CommercialsOperation op,JSONObject req, JSONObject res, OffersType invocationType) {
		JSONObject companyDtlsJson, clientDtlsjson, commonElemJson, productDtlObj = null, subReqBody,
				 comOfferReqJson, briJsonObj,roomDtlsjsonObj,passDtlObj,accoInfoJson;
		JSONArray  productDtlsArr;
		
		//OffersConfig offConfig = AccoConfig.getOffersConfig();
		//CommercialTypeConfig commTypeConfig = offConfig.getOfferTypeConfig(invocationType);
		ServiceConfig commTypeConfig = AccoConfig.getOffersTypeConfig(invocationType);
		comOfferReqJson = new JSONObject(new JSONTokener(commTypeConfig.getRequestJSONShell()));

		JSONObject reqHeader = req.getJSONObject(JSON_PROP_REQHEADER);
		JSONObject reqBody = req.getJSONObject(JSON_PROP_REQBODY);
		JSONObject resHeader = res.getJSONObject(JSON_PROP_RESHEADER);
		JSONObject resBody = res.getJSONObject(JSON_PROP_RESBODY);
		JSONObject clientCtxJson = reqHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT);
		 
		JSONObject breHdrJson = new JSONObject();
		breHdrJson.put(JSON_PROP_SESSIONID, resHeader.getString(JSON_PROP_SESSIONID));
		breHdrJson.put(JSON_PROP_TRANSACTID, resHeader.getString(JSON_PROP_TRANSACTID));
		breHdrJson.put(JSON_PROP_USERID, resHeader.getString(JSON_PROP_USERID));
		breHdrJson.put(JSON_PROP_OPERATIONNAME, op.toString());
		
		JSONObject rootJson = comOfferReqJson.getJSONArray("commands").getJSONObject(0).getJSONObject("insert")
				.getJSONObject("object").getJSONObject("cnk.accomodation_companyoffers.withoutredemption.Root");
		
		UserContext usrCtx = UserContext.getUserContextForSession(reqHeader);
		List<ClientInfo> clientCommHierarchyList = usrCtx.getClientHierarchy();
		String clientGroup = "";
		if (clientCommHierarchyList != null && clientCommHierarchyList.size() > 0) {
			ClientInfo clInfo = clientCommHierarchyList.get(clientCommHierarchyList.size() - 1);
			if (clInfo.getCommercialsEntityType() == ClientInfo.CommercialsEntityType.ClientGroup) {
				clientGroup = clInfo.getCommercialsEntityId();
			}
		}
		OrgHierarchy orgHier = usrCtx.getOrganizationHierarchy();

		JSONArray briJsonArr = new JSONArray();
		rootJson.put(JSON_PROP_HEADER, breHdrJson);
		rootJson.put(JSON_PROP_BUSSRULEINTAKE, briJsonArr);
		int briIndex=0,roomIdx=0,hotelIndex = 0;
		Map<Integer,String> SI2BRMSRoomMap=new HashMap<>();
		Map<String, JSONObject> hotelMap = null;
		Map<String, Integer> hotelIndexMap=new HashMap<>();
		JSONArray multiResArr = resBody.getJSONArray(JSON_PROP_ACCOMODATIONARR);
		for (int i = 0; i < multiResArr.length(); i++) {
			accoInfoJson = (JSONObject) multiResArr.get(i);
            subReqBody = reqBody.has(JSON_PROP_ACCOMODATIONARR)? (JSONObject) reqBody.getJSONArray(JSON_PROP_ACCOMODATIONARR).get(i): reqBody;
			String prevSuppId = DEF_SUPPID;	
			productDtlsArr = new JSONArray();
			briJsonObj = new JSONObject();
			for(Object roomStayJson: accoInfoJson.getJSONArray(JSON_PROP_ROOMSTAYARR)) {
				String suppId = ((JSONObject)roomStayJson).getString(JSON_PROP_SUPPREF);
				String hotelCode = ((JSONObject) ((JSONObject) ((JSONObject)roomStayJson).get(JSON_PROP_ROOMINFO)).get(JSON_PROP_HOTELINFO)).getString(JSON_PROP_HOTELCODE);
				
				if(!prevSuppId.equals(suppId)) {
					hotelMap = new HashMap<String,JSONObject>();
					prevSuppId = suppId;
					companyDtlsJson = new JSONObject();
					companyDtlsJson.put(JSON_PROP_SBU, orgHier.getSBU().isEmpty()?null:orgHier.getSBU());
					companyDtlsJson.put(JSON_PROP_BU, orgHier.getBU().isEmpty()?null:orgHier.getBU());
					companyDtlsJson.put(JSON_PROP_DIVISION, orgHier.getDivision().isEmpty()?null:orgHier.getDivision());
					companyDtlsJson.put(JSON_PROP_SALESOFFICELOC, orgHier.getSalesOfficeLoc().isEmpty()?null:orgHier.getSalesOfficeLoc());
					companyDtlsJson.put(JSON_PROP_SALESOFFICE, orgHier.getSalesOfficeName().isEmpty()?null:orgHier.getSalesOfficeName());
					companyDtlsJson.put(JSON_PROP_COMPANYMKT,clientCtxJson.getString(JSON_PROP_CLIENTMARKET));

					clientDtlsjson = new JSONObject();
					clientDtlsjson.put(JSON_PROP_CLIENTCAT, usrCtx.getClientCategory().isEmpty()?null:usrCtx.getClientCategory());
					clientDtlsjson.put(JSON_PROP_CLIENTSUBCAT, usrCtx.getClientSubCategory().isEmpty()?null:usrCtx.getClientSubCategory());
					clientDtlsjson.put(JSON_PROP_CLIENTNAME, usrCtx.getClientName().isEmpty()?null:usrCtx.getClientName());
					clientDtlsjson.put(JSON_PROP_CLIENTGROUP, clientGroup.isEmpty()?null:clientGroup);
					clientDtlsjson.put(JSON_PROP_CLIENTTYPE, clientCtxJson.getString(JSON_PROP_CLIENTTYPE));
					clientDtlsjson.put(JSON_PROP_NATIONALITY,clientCtxJson.getString(JSON_PROP_CLIENTMARKET));
					clientDtlsjson.put(JSON_PROP_POS, clientCtxJson.optString(JSON_PROP_POS, null));

					commonElemJson = new JSONObject();
					commonElemJson.put(JSON_PROP_BOOKINGDATE, DATE_FORMAT.format(new Date()));
					commonElemJson.put("checkInDate", subReqBody.getString(JSON_PROP_CHKIN).concat("T00:00:00"));
					commonElemJson.put("checkOutDate", subReqBody.getString(JSON_PROP_CHKOUT).concat("T00:00:00"));
					commonElemJson.put("firstBookingOnSite", true);
					
					briJsonObj.put("companyDetails", companyDtlsJson);
					briJsonObj.put("clientDetails", clientDtlsjson);
					briJsonObj.put("commonElements", commonElemJson);
					briJsonObj.put("productDetails", productDtlsArr);
					
					briJsonArr.put(briIndex++,briJsonObj);
				}
				productDtlObj = hotelMap.get(hotelCode);
				
				String cityName;
				roomDtlsjsonObj=new JSONObject();
				Map<String, Object> cityAttrs;
				Map<String, Object> hotelAttrs;
				if (productDtlObj == null) {
					hotelAttrs = RedisHotelData.getHotelInfo(hotelCode);
					cityName = RedisHotelData.getHotelInfo(hotelCode, "city");
					cityAttrs = RedisCityData.getCityInfo(cityName);
					productDtlObj = new JSONObject();
					productDtlObj.put("productCategory", "Accommodation");
					productDtlObj.put("productCategorySubType",((JSONObject) roomStayJson).getString(JSON_PROP_ACCOSUBTYPE));
					productDtlObj.put("brand", hotelAttrs.getOrDefault("brand", null));
					if(productDtlObj.optString("brand").isEmpty()) {
						productDtlObj.remove("brand");
					}
					productDtlObj.put("selectedOfferList", subReqBody.optJSONArray("selectedOfferList"));
					productDtlObj.put("chain", hotelAttrs.getOrDefault("chain", ""));
					productDtlObj.put("city", cityName);
					productDtlObj.put("country", cityAttrs.getOrDefault("country", ""));
					productDtlObj.put("productName", hotelAttrs.getOrDefault("name", ""));
					productDtlObj.put("noOfNights", ((JSONObject) roomStayJson).getJSONArray(JSON_PROP_NIGHTLYPRICEARR).length());
					productDtlsArr.put(productDtlObj);
					hotelMap.put(hotelCode, productDtlObj);
					hotelIndexMap.put(hotelCode, hotelIndex++);
			}
				
				
				roomDtlsjsonObj = getBRMSRoomDetailsJSON((JSONObject) roomStayJson,subReqBody);
				
			/*	JSONObject totalPriceInfoJson = ((JSONObject) roomStayJson).getJSONObject(JSON_PROP_ROOMPRICE);
				JSONObject taxesJson = totalPriceInfoJson.getJSONObject(JSON_PROP_TOTALTAX);
				JSONArray taxesJsonArr = taxesJson.optJSONArray(JSON_PROP_TAXBRKPARR);
				BigDecimal roomPrice = totalPriceInfoJson.getBigDecimal(JSON_PROP_AMOUNT);
			   
				
				roomDtlsjsonObj.put("roomCategory", ((JSONObject) roomStayJson).getJSONObject("roomInfo").getJSONObject("roomTypeInfo").getString("roomCategoryName"));
				roomDtlsjsonObj.put("roomType", ((JSONObject) roomStayJson).getJSONObject("roomInfo").getJSONObject("roomTypeInfo").getString("roomTypeName"));
				roomDtlsjsonObj.put("totalFare",roomPrice);
				JSONObject fareBrkpJson = new JSONObject();
				fareBrkpJson.put("fareName", JSON_VAL_BASE);
				fareBrkpJson.put("fareValue", roomPrice.subtract(taxesJson.optBigDecimal(JSON_PROP_AMOUNT, BigDecimal.ZERO)));
				roomDtlsjsonObj.append("fareDetails", fareBrkpJson);

				//add taxes
				if(taxesJsonArr!=null) {
					for(Object taxJson:taxesJsonArr) {
						addTaxesInFareDtls(taxJson,roomDtlsjsonObj);
					}
				}
				
				//nightly price 
				JSONArray nightlyPriceInfoArr = ((JSONObject) roomStayJson).getJSONArray(JSON_PROP_NIGHTLYPRICEARR);
				JSONArray nightlyprcArr=new JSONArray();
				for(Object nightprc:nightlyPriceInfoArr) {
					JSONObject nightlyObj=new JSONObject();
					BigDecimal nightPrice = ((JSONObject) nightprc).getBigDecimal("amount");
					nightlyObj.put("effectiveDate", ((JSONObject) nightprc).getString("effectiveDate").concat("T00:00:00"));
					nightlyObj.put("totalFare",nightPrice);
					
					JSONObject nightfareBrkpJson = new JSONObject();
					JSONArray nighttaxesJsonArr = ((JSONObject) nightprc).optJSONObject("taxes").optJSONArray(JSON_PROP_TAXBRKPARR);
					nightfareBrkpJson.put("fareName", JSON_VAL_BASE);
					nightfareBrkpJson.put("fareValue", nightPrice.subtract(((JSONObject) nightprc).optJSONObject("taxes").optBigDecimal(JSON_PROP_AMOUNT, BigDecimal.ZERO)));
					nightlyObj.append("fareDetails", nightfareBrkpJson);
					
					if(nighttaxesJsonArr!=null) {
						for(Object nightTaxJson:nighttaxesJsonArr) {
							addTaxesInFareDtls(nightTaxJson,nightlyObj);
						}
					}
					
					nightlyprcArr.put(nightlyObj);
				}
				roomDtlsjsonObj.put("nightDetails", nightlyprcArr);
				JSONArray passDtlsArr=new JSONArray();
				int roomNo=((JSONObject) roomStayJson).getJSONObject(JSON_PROP_ROOMINFO).getInt(JSON_PROP_ROOMINDEX);
				JSONObject room=new JSONObject();
			//	if(roomNo>0) {
			    room = (JSONObject) reqBody.getJSONArray("roomConfig").get(roomNo-1);
				
				for(int h=0;h<room.getInt(JSON_PROP_ADTCNT);h++) {
					passDtlObj=new JSONObject();
					passDtlObj.put("passengerType", PassengerType.ADULT.toString());	
					passDtlsArr.put(passDtlObj);
				}
				JSONArray roomChld = room.getJSONArray("childAges");
				for(int t=0;t<roomChld.length();t++) {
					passDtlObj=new JSONObject();
					passDtlObj.put("passengerType", PassengerType.CHILD.toString());
					passDtlObj.put("age", roomChld.get(t));
					passDtlsArr.put(passDtlObj);
				}
				//}
				roomDtlsjsonObj.put("passengerDetails", passDtlsArr);*/
				productDtlObj.append("roomDetails", roomDtlsjsonObj);
				
				//this is done so that while appending the offers to the respective rooms it becomes easy 
				SI2BRMSRoomMap.put(roomIdx++,String.format("%s%c%s%c%s",briIndex-1,KEYSEPARATOR,hotelIndexMap.get(hotelCode),KEYSEPARATOR,productDtlObj.getJSONArray("roomDetails").length()-1));
			}
			
		}
		rootJson.put(JSON_PROP_BUSSRULEINTAKE, briJsonArr);
		
		JSONObject comOfferResJson = null;
		try {
			logger.info("Before opening HttpURLConnection to BRMS for Supplier Commercials");
			//comOfferResJson = HTTPServiceConsumer.consumeJSONService("BRMS_COMPANY_OFFERS", commTypeConfig.getServiceURL(), offConfig.getHttpHeaders(), comOfferReqJson);
			comOfferResJson = HTTPServiceConsumer.consumeJSONService("BRMS_COMPANY_OFFERS", commTypeConfig.getServiceURL(), commTypeConfig.getHttpHeaders(), comOfferReqJson);
			logger.info("HttpURLConnection to BRMS closed");
		} 
		
		catch (Exception x) {
			logger.warn("An exception occurred when calling supplier commercials", x);
		}
		if(comOfferResJson==null) {
			logger.info("CompanyOffers not applied");
			
		}
		if (BRMS_STATUS_TYPE_FAILURE.equals(comOfferResJson.getString(JSON_PROP_TYPE))) {
	        	logger.warn(String.format("A failure response was received from Company Offers calculation engine: %s", comOfferResJson.toString()));
	        	return;
	        }
		
        if (OffersType.COMPANY_SEARCH_TIME == invocationType) {
        	appendOffersToResults(reqHeader,resBody, comOfferResJson,SI2BRMSRoomMap,op);
        }
	}

	static JSONObject getBRMSRoomDetailsJSON(JSONObject roomStayJson, JSONObject reqBody) {
		JSONObject roomDtlsjsonObj=new JSONObject();
		JSONObject totalPriceInfoJson = ((JSONObject) roomStayJson).getJSONObject(JSON_PROP_ROOMPRICE);
		JSONObject taxesJson = totalPriceInfoJson.getJSONObject(JSON_PROP_TOTALTAX);
		JSONArray taxesJsonArr = taxesJson.optJSONArray(JSON_PROP_TAXBRKPARR);
		BigDecimal roomPrice = totalPriceInfoJson.getBigDecimal(JSON_PROP_AMOUNT);
	   
		
		roomDtlsjsonObj.put("roomCategory", ((JSONObject) roomStayJson).getJSONObject("roomInfo").getJSONObject("roomTypeInfo").getString("roomCategoryName"));
		roomDtlsjsonObj.put("roomType", ((JSONObject) roomStayJson).getJSONObject("roomInfo").getJSONObject("roomTypeInfo").getString("roomTypeName"));
		roomDtlsjsonObj.put("totalFare",roomPrice);
		JSONObject fareBrkpJson = new JSONObject();
		fareBrkpJson.put("fareName", JSON_VAL_BASE);
		fareBrkpJson.put("fareValue", roomPrice.subtract(taxesJson.optBigDecimal(JSON_PROP_AMOUNT, BigDecimal.ZERO)));
		roomDtlsjsonObj.append("fareDetails", fareBrkpJson);

		//add taxes
		if(taxesJsonArr!=null) {
			for(Object taxJson:taxesJsonArr) {
				addTaxesInFareDtls(taxJson,roomDtlsjsonObj);
			}
		}
		
		//nightly price 
		JSONArray nightlyPriceInfoArr = ((JSONObject) roomStayJson).getJSONArray(JSON_PROP_NIGHTLYPRICEARR);
		JSONArray nightlyprcArr=new JSONArray();
		for(Object nightprc:nightlyPriceInfoArr) {
			JSONObject nightlyObj=new JSONObject();
			BigDecimal nightPrice = ((JSONObject) nightprc).getBigDecimal("amount");
			nightlyObj.put("effectiveDate", ((JSONObject) nightprc).getString("effectiveDate").concat("T00:00:00"));
			nightlyObj.put("totalFare",nightPrice);
			
			JSONObject nightfareBrkpJson = new JSONObject();
			JSONArray nighttaxesJsonArr = ((JSONObject) nightprc).optJSONObject("taxes").optJSONArray(JSON_PROP_TAXBRKPARR);
			nightfareBrkpJson.put("fareName", JSON_VAL_BASE);
			nightfareBrkpJson.put("fareValue", nightPrice.subtract(((JSONObject) nightprc).optJSONObject("taxes").optBigDecimal(JSON_PROP_AMOUNT, BigDecimal.ZERO)));
			nightlyObj.append("fareDetails", nightfareBrkpJson);
			
			if(nighttaxesJsonArr!=null) {
				for(Object nightTaxJson:nighttaxesJsonArr) {
					addTaxesInFareDtls(nightTaxJson,nightlyObj);
				}
			}
			
			nightlyprcArr.put(nightlyObj);
		}
		roomDtlsjsonObj.put("nightDetails", nightlyprcArr);
		JSONArray passDtlsArr=new JSONArray();
		int roomNo=((JSONObject) roomStayJson).getJSONObject(JSON_PROP_ROOMINFO).optInt(JSON_PROP_ROOMINDEX,1);
		if(roomNo==-1)
			roomNo=1;
		JSONObject room=new JSONObject();
	//	if(roomNo>0) {
	    room = (JSONObject) reqBody.getJSONArray("roomConfig").get(roomNo-1);
		
		for(int h=0;h<room.getInt(JSON_PROP_ADTCNT);h++) {
			JSONObject passDtlObj=new JSONObject();
			passDtlObj.put("passengerType", PassengerType.ADULT.toString());	
			passDtlsArr.put(passDtlObj);
		}
		JSONArray roomChld = room.getJSONArray("childAges");
		for(int t=0;t<roomChld.length();t++) {
			JSONObject passDtlObj=new JSONObject();
			passDtlObj.put("passengerType", PassengerType.CHILD.toString());
			passDtlObj.put("age", roomChld.get(t));
			passDtlsArr.put(passDtlObj);
		}
		//}
		roomDtlsjsonObj.put("passengerDetails", passDtlsArr);
		return roomDtlsjsonObj;
	}

	private static void appendOffersToResults(JSONObject reqHeader,JSONObject resBody, JSONObject comOfferResJson, Map<Integer, String> SI2BRMSRoomMap,CommercialsOperation op) {
		JSONArray briArr = getCompanyOffersBusinessRuleIntakeJSONArray(comOfferResJson);
		JSONArray resAccoInfoJsonArr = resBody.getJSONArray(JSON_PROP_ACCOMODATIONARR);
		int roomIdx=0;
		String clientCurrency = reqHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCCY);
		String clientMrkt =reqHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTMARKET);
		for(int i=0;i<resAccoInfoJsonArr.length();i++) {
			JSONArray resRoomJsonArr = resAccoInfoJsonArr.getJSONObject(i).getJSONArray(JSON_PROP_ROOMSTAYARR);
			for(int j=0;j<resRoomJsonArr.length();j++) {
				 JSONObject resRoomObj = resRoomJsonArr.getJSONObject(j);
				JSONObject comOffRoomJson = getCompanyOffersRoomDetailJson(SI2BRMSRoomMap.get(roomIdx++),briArr);
				JSONArray offDtlsSetArr = comOffRoomJson.optJSONArray("offerDetailsSet");
				resRoomObj.put("offerDetailsSet", offDtlsSetArr);
				if(offDtlsSetArr!=null) {
					for(int k=0;k<offDtlsSetArr.length();k++) {
						JSONObject offerObj = offDtlsSetArr.getJSONObject(k);
						BigDecimal percentage = offerObj.optBigDecimal("percentage", BigDecimal.ZERO);
						if(percentage.equals(BigDecimal.ZERO)) {
							BigDecimal roeAmt = RedisRoEData.getRateOfExchange(offerObj.optString("currency",""),clientCurrency, clientMrkt);
							BigDecimal offerAmt = offerObj.optBigDecimal("offerAmount",new BigDecimal(0)).multiply(roeAmt);
							offerObj.put("offerAmount", offerAmt);
						}
						else {
							offerObj.put("currency", clientCurrency);
						}
					}
				}
				if(CommercialsOperation.Reprice.equals(op)) {
					if(resRoomObj.has("offerDetailsSet")) {
						BigDecimal totalOfferAmt=new BigDecimal(0);
						JSONObject totalPriceObj = resRoomObj.getJSONObject(JSON_PROP_ROOMPRICE);
						totalPriceObj.put("amountBeforeDiscount", totalPriceObj.getBigDecimal(JSON_PROP_AMOUNT));
						JSONObject offerObj=new JSONObject();
						offerObj.put("discount", new JSONArray());
						totalPriceObj.put("discounts", offerObj);
						for(int k=0;k<offDtlsSetArr.length();k++) {
							JSONObject discount=new JSONObject();
							JSONObject offDtlsSetObj = offDtlsSetArr.getJSONObject(k);
							/*BigDecimal roeAmt = RedisRoEData.getRateOfExchange(offDtlsSetObj.optString("currency",""),clientCurrency, clientMrkt);
							BigDecimal offerAmt = offDtlsSetObj.optBigDecimal("offerAmount",new BigDecimal(0)).multiply(roeAmt);
							discount.put(JSON_PROP_AMOUNT,offerAmt.negate());*/
							discount.put(JSON_PROP_AMOUNT,offDtlsSetObj.optBigDecimal("offerAmount",new BigDecimal(0)).negate());
							discount.put("discountCode",offDtlsSetObj.optString("offerID",""));
							discount.put("discountType",offDtlsSetObj.optString("offerType",""));
							//for now set the currency code to client currency code
							discount.put(JSON_PROP_CCYCODE,clientCurrency);
							offerObj.append("discount", discount);
							totalOfferAmt=totalOfferAmt.add(offDtlsSetObj.optBigDecimal("offerAmount",new BigDecimal(0)));
							if(!("cashback".equalsIgnoreCase(offDtlsSetObj.optString("offerType","")))) {
							totalPriceObj.put(JSON_PROP_AMOUNT, totalPriceObj.getBigDecimal(JSON_PROP_AMOUNT).subtract(offDtlsSetObj.optBigDecimal("offerAmount",new BigDecimal(0))));
						   }
						}
						offerObj.put(JSON_PROP_AMOUNT, totalOfferAmt.negate());
					}
				}
			}
		}
		
		
	}

	private static JSONObject getCompanyOffersRoomDetailJson(String briKey, JSONArray briArr) {
		JSONObject roomJson = null;
		if(briKey==null)
			return roomJson;
		String[] briIdxs = briKey.split(Pattern.quote(String.valueOf(KEYSEPARATOR)));//to escape this pipe
		//Key has been made using bri index,hotel index and room index in supplier commercials. Therefore this below restriction.
		if(briIdxs.length!=3)
			return roomJson;
		try {
			roomJson =(JSONObject) new JSONPointer(String.format("/%s/%s/%s/%s/%s",briIdxs[0],"productDetails",briIdxs[1],JSON_PROP_ROOMDETAILS,briIdxs[2])).queryFrom(briArr);
		}
		catch(Exception e) {}

		return roomJson;
	}

	static JSONArray getCompanyOffersBusinessRuleIntakeJSONArray(JSONObject comOfferResJson) {
		JSONArray briJsonArray = new JSONArray();

		try{
			briJsonArray = comOfferResJson.getJSONObject(JSON_PROP_RESULT).getJSONObject(JSON_PROP_EXECUTIONRES).getJSONArray(JSON_PROP_RESULTS).getJSONObject(0).getJSONObject(JSON_PROP_VALUE).getJSONObject("cnk.accomodation_companyoffers.withoutredemption.Root").getJSONArray(JSON_PROP_BUSSRULEINTAKE);
		}
		catch(Exception e){
			logger.warn("Company Offers \"businessRuleIntake\" evaluated to be null");
		}
		if(briJsonArray==null) {
			logger.warn("Company Offers\"businessRuleIntake\" evaluated to be null");
			briJsonArray = new JSONArray();
		}

		return briJsonArray;
	}

	private static void addTaxesInFareDtls(Object taxJson, JSONObject addObj) {
		JSONObject fareBrkpJson = new JSONObject();
		fareBrkpJson.put("fareName", ((JSONObject) taxJson).getString(JSON_PROP_TAXCODE));
		fareBrkpJson.put("fareValue",((JSONObject) taxJson).getBigDecimal(JSON_PROP_AMOUNT));
		addObj.append("fareDetails", fareBrkpJson);
    }

}
