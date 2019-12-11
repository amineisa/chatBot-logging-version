/** copyright Etisalat social team. To present All rights reserved
*/
/**
 * 
 */
package com.chatbot.services;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chatbot.entity.CustomerProfile;
import com.chatbot.entity.UserSelection;
import com.chatbot.repo.BotConfigurationRepo;
import com.chatbot.util.Constants;
import com.github.messenger4j.send.message.template.GenericTemplate;
import com.github.messenger4j.send.message.template.Template;
import com.github.messenger4j.send.message.template.button.Button;
import com.github.messenger4j.send.message.template.button.PostbackButton;
import com.github.messenger4j.send.message.template.common.Element;


/**
 * @author A.Eissa 
 */
@Service
public class EmeraldService {

	@Autowired
	private BotConfigurationRepo botConfigurationRepo;
	@Autowired
	private UtilService utilService;
	@Autowired
	private ChatBotService chatBotService;
	
	//private static final Logger logger = LoggerFactory.getLogger(EmeraldService.class);
	
	boolean checkAddChildEligibility(JSONObject object,String[] keys){
		return object.getBoolean(keys[0]);
	}

	/**
	 * @param locale
	 * @return 
	 */
	public String sendNotEligibleTOAddChildMsg(String locale) {
		return locale.contains(Constants.LOCALE_AR) ? 
				botConfigurationRepo.findBotConfigurationByKey(Constants.EMERALD_NOT_ELIGIBLE_ADD_MEMBER_AR).getValue():
					botConfigurationRepo.findBotConfigurationByKey(Constants.EMERALD_NOT_ELIGIBLE_ADD_MEMBER_EN).getValue();
		
	}

	/**
	 * @param locale
	 * @return
	 */
	public String sendEligibleTOAddChildMsg(String locale) {
		return locale.contains(Constants.LOCALE_AR) ? 
				botConfigurationRepo.findBotConfigurationByKey(Constants.EMERALD_ELIGIBLE_ADD_MEMBER_AR).getValue():
					botConfigurationRepo.findBotConfigurationByKey(Constants.EMERALD_ELIGIBLE_ADD_MEMBER_EN).getValue();
		

		
	}

	/**
	 * @param paramNames
	 * @param userSelections
	 * @return
	 */
	public JSONObject addChildRequestBody(ArrayList<String> paramNames, UserSelection userSelections) {
		JSONObject body = new JSONObject();
		body.put(paramNames.get(0),userSelections.getEmeraldRateplanProductName());
		body.put(paramNames.get(1), userSelections.getEmeraldChildDial());
		return body;
	}
	// Emerald Remove Child request body  
	public JSONObject removeChildRequestBody(UserSelection userSelection) {
		JSONObject body = new JSONObject(); 
		JSONObject orderLineRequest = new JSONObject();
		orderLineRequest.put("operation", "REMOVE_CHILD");
		orderLineRequest.put("productName", userSelection.getEmeraldRateplanProductName());
		JSONArray parameterListRequest = new JSONArray();
		JSONObject childDetail = new JSONObject();
		childDetail.put("name", "CHILD_MSISDN");
		childDetail.put("value", userSelection.getEmeraldChildDialToRemove());
		parameterListRequest.put(childDetail);
		orderLineRequest.put("parameterListRequest", parameterListRequest);
		body.put("orderLineRequest", orderLineRequest);
		return body ;
	}

	/**
	 * @param jsonObject
	 * @param userlocale
	 * @param elements
	 */
	public Template getAllEmeraldTraficCases(JSONObject jsonObject, String userlocale) {
		List<Element> elements = new ArrayList<>();
		JSONArray bundleResponses = jsonObject.getJSONArray(Constants.JSON_KEY_BUNDLE_RESPONSE);
		for(int i = 0 ; i < bundleResponses.length() ; i++) {
			List<Button> buttons = new ArrayList<>();
			JSONObject object = bundleResponses.getJSONObject(i);
			String payload = object.getString(Constants.FB_JSON_KEY_ID);
			String label = userlocale.contains(Constants.LOCALE_AR) ? object.getString(Constants.JSON_KEY_NAME_AR):object.getString(Constants.JSON_KEY_NAME_EN);
			Button button = PostbackButton.create(label, Constants.EMERALD_GET_DIALS_FOR_DISTRIBUTE_PAYLOAD+payload);
			String title = userlocale.contains(Constants.LOCALE_AR) ? object.getString(Constants.JSON_KEY_ARABIC_DESCRIPTION_KEY): object.getString(Constants.JSON_KEY_FOR_ENGLISH_DESCRIPTION);
			buttons.add(button);
			Element element = Element.create(title, Optional.of("---"), Optional.empty(),Optional.empty(), Optional.of(buttons) );
			elements.add(element);
		}
		return GenericTemplate.create(elements);
		
	}
	
	public Template getAllEmeraldTraficCasesByChildDial(JSONObject jsonObject, CustomerProfile customerProfile,UserSelection userSelection) {
		List<Element> elements = new ArrayList<>();
		String userLocale = customerProfile.getLocale();
		String childDial = userSelection.getEmeraldTransferFromDial();
		JSONObject details = jsonObject.getJSONObject(Constants.EMERALD_DETAILS_KEY);
		//String realDial = childDial.substring(1,childDial.length());
		JSONArray traficCase = details.getJSONArray(childDial);
		for(int i = 0 ; i < traficCase.length() ; i++) {
			List<Button> buttons = new ArrayList<>();
			JSONObject object = traficCase.getJSONObject(i);
			if(object.get(Constants.FB_JSON_KEY_ID) != null) {
		    boolean distbuteAfterTrans = object.getBoolean(Constants.DISTRIBUTE_AFTER_TRANSFER);
			String payload = object.getString(Constants.FB_JSON_KEY_ID);
			String label = userLocale.contains(Constants.LOCALE_AR) ? object.getString(Constants.JSON_KEY_NAME_AR):object.getString(Constants.JSON_KEY_NAME_EN);
			Button button = PostbackButton.create(label, Constants.EMERALD_CHILD_TRANSFER_TO_PAYLOAD +Constants.COMMA_CHAR+payload+Constants.COMMA_CHAR+distbuteAfterTrans);
			String title = userLocale.contains(Constants.LOCALE_AR) ? object.getString(Constants.JSON_KEY_ARABIC_DESCRIPTION_KEY): object.getString(Constants.JSON_KEY_FOR_ENGLISH_DESCRIPTION);
			buttons.add(button);
			Element element = Element.create(title, Optional.of("---"), Optional.empty(),Optional.empty(), Optional.of(buttons) );
			elements.add(element);
			}
		}
		return GenericTemplate.create(elements);
		
	}

	
	/**
	 * @param paramNames 
	 * @param userSelections
	 * @return
	 */
	public JSONObject distributeRequestBody(ArrayList<String> paramNames, UserSelection userSelections) {
		JSONObject body = new JSONObject();
		List<String> values = new ArrayList<>();
		String childDial = encryptDPIParam(userSelections.getEmeraldDialForDistribute());
		String emeraldSeviceId = userSelections.getEmeraldTraficCaseID();
		String rateplanId = userSelections.getEmeraldRateplanProductName();
		String qouta = userSelections.getEmeraldDistributeAmount();
		values.add(rateplanId);
		values.add(childDial);
		values.add(emeraldSeviceId);
		values.add(qouta);
		for (int i = 0; i < paramNames.size(); i++) {
			body.put(paramNames.get(i), values.get(i));
		}
		return body ;
	}
	
	public JSONObject transferRequestBody(ArrayList<String> paramNames , UserSelection userSelection) {
		JSONObject body = new JSONObject();
		List<String> values = new ArrayList<>();
		String productName = userSelection.getEmeraldRateplanProductName();
		String trafficCase = userSelection.getEmeraldTraficCaseID();
		String source = encryptDPIParam(userSelection.getEmeraldTransferFromDial());
		String target = encryptDPIParam(userSelection.getEmeraldTransferToDial());
		String quota = userSelection.getEmeraldDistributeAmount();
		String distrbuteAfterTransfare = userSelection.getEmeraldDistbuteAfterTrans().equals("true")?"Y":"N";
		values.add(productName);
		values.add(trafficCase);
		values.add(source);
		values.add(target);
		values.add(quota);
		values.add(distrbuteAfterTransfare);
		for (int i = 0; i < paramNames.size(); i++) {
			body.put(paramNames.get(i), values.get(i));
		}
		return body ;
	}
	
	
	
	public String encryptDPIParam(String encryptedString) {
		String dbEncryptionKey = chatBotService.getBotConfigurationByKey(Constants.DPI_ENCRYPTION_KEY).getValue();
		String encryptionDial = "";
		try {
		byte[] decryptionKey = new byte[dbEncryptionKey.length()];
		for (int i = 0; i < dbEncryptionKey.length(); i++) {
			decryptionKey[i] = (byte) dbEncryptionKey.charAt(i);
		}
			Cipher c = Cipher.getInstance("AES");
			SecretKeySpec k = new SecretKeySpec(decryptionKey, "AES");
			c.init(Cipher.ENCRYPT_MODE, k);
			byte[] utf8 = encryptedString.getBytes("UTF8");
			byte[] enc = c.doFinal(utf8);
			encryptionDial = URLEncoder.encode(DatatypeConverter.printBase64Binary(enc), "UTF-8"); 
		} catch (Exception e) {
			e.printStackTrace();
		}
		return encryptionDial;
	}

	

}
