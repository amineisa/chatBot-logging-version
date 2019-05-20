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
public class AuthService {
	
	@Autowired
	private UtilService utilService;

	/**
	 * @param userSelections
	 * @param customerProfile
	 * @param paramNames
	 * @param paramValuesList
	 * @return
	 */
	public JSONObject checkDialValidity(UserSelection userSelections, CustomerProfile customerProfile, ArrayList<String> paramNames, ArrayList<String> paramValuesList) {
		if(userSelections.getUserDialForAuth() !=null) {
			String locale = customerProfile.getLocale();
			String dial = userSelections.getUserDialForAuth();
			String timeStamp = Constants.AUTH_TIME_STAMP_VALUE;
			paramValuesList.add(dial);
			paramValuesList.add(locale);
			paramValuesList.add(timeStamp);
			
			return utilService.setRequestBodyValueForPostCalling(paramValuesList, paramNames);	
		} else {
			return null;
		}
		
	}

	/**
	 * @param userSelections
	 * @param customerProfile
	 * @param paramNames
	 * @param paramValuesList
	 */
	public JSONObject checkVerificationCode(UserSelection userSelections, ArrayList<String> paramNames, ArrayList<String> paramValuesList) {
		if(userSelections.getActivationCode() != null && userSelections.getUserDialForAuth() !=null ) {
			String code = userSelections.getActivationCode();
			String dial = userSelections.getUserDialForAuth();
			paramValuesList.add(dial);
			paramValuesList.add(code);
			return utilService.setRequestBodyValueForPostCalling(paramValuesList, paramNames);
		}else {
			return null;
		}

	}
	

}
	

