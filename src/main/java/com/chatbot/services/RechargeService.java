/** copyright Etisalat social team. To present All rights reserved
*/
/**
 * 
 */
package com.chatbot.services;

import java.util.ArrayList;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chatbot.entity.CustomerProfile;
import com.chatbot.entity.UserSelection;
import com.chatbot.util.Constants;

/**
 * @author A.Eissa 
 */
@Service
public class RechargeService {
	
	// For Native recharge update button type to be postback button which its id is 156

	@Autowired
	private UtilService utilService;
	/**
	 * @param userSelections
	 * @param customerProfile
	 * @param paramNames
	 * @param paramValuesList
	 * @return
	 */
	public JSONObject normalRecharge(UserSelection userSelections, CustomerProfile customerProfile, ArrayList<String> paramNames, ArrayList<String> paramValuesList) {
		if(userSelections.getScratcheddNumberForRecharge() != null && customerProfile.getLocale() !=null && customerProfile.getMsisdn() != null) {
			String locale = customerProfile.getLocale();
			String dial = customerProfile.getMsisdn();
			String scratchedNumber = userSelections.getScratcheddNumberForRecharge();
			paramValuesList.add(dial);
			paramValuesList.add(scratchedNumber);
			paramValuesList.add(locale);
			if(userSelections.getAkwaKartCategoryName() !=null) {
				paramNames.add(Constants.AKWAKART_CATEGORY_KEY);
				paramValuesList.add(userSelections.getAkwaKartCategoryName());
			}
			return utilService.setRequestBodyValueForPostCalling(paramValuesList, paramNames);	
		} else {
			return new JSONObject();
		}
		
	}

	
}
