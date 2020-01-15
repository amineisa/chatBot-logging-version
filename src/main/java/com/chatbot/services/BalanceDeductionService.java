/** copyright Etisalat social team. To present All rights reserved
*/
/**
 * 
 */
package com.chatbot.services;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.chatbot.util.Constants;


/**
 * @author A.Eissa 
 */
@Service
public class BalanceDeductionService {
	
	private static final Logger logger = LoggerFactory.getLogger(BalanceDeductionService.class);
	
	
	public JSONObject setRequestBody() {
		Map<String ,String> dateValues = lastDayes(120);
		JSONObject body = new JSONObject();
		for(Entry<String, String> row : dateValues.entrySet()){
			body.put(row.getKey(), row.getValue());
		}
		return body;
	}
	
	private Map<String ,String> lastDayes(int days) {
		Map<String,String> dates = new HashMap<>();
		LocalDate toDate = LocalDate.now();
		LocalDate  fromDate = toDate.minusDays(days);
		dates.put("dateTo",setDatePattern(toDate));
		logger.debug(Constants.LOGGER_INFO_PREFIX+"Balance Deduction time is From "+setDatePattern(fromDate) +" TO "+setDatePattern(toDate));
		dates.put("dateFrom", setDatePattern(fromDate));
		dates.put("otherTransaction","");
        return dates;
	}
	
	
	String setDatePattern(LocalDate date){
		return DateTimeFormatter.ofPattern("yyyy-MM-dd").format(date);
	}
	

	

	
}
