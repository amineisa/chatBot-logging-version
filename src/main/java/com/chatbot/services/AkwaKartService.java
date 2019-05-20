/** copyright Etisalat social team. To present All rights reserved
*/
/**
 * 
 */
package com.chatbot.services;

import static java.util.Optional.empty;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chatbot.util.Constants;
import com.chatbot.util.Utils;
import com.github.messenger4j.send.message.template.GenericTemplate;
import com.github.messenger4j.send.message.template.Template;
import com.github.messenger4j.send.message.template.button.Button;
import com.github.messenger4j.send.message.template.button.PostbackButton;
import com.github.messenger4j.send.message.template.common.Element;
import com.google.gson.JsonObject;

/**
 * @author A.Eissa 
 */
@Service
public class AkwaKartService {

	
	@Autowired
	private ChatBotService chatBotService;
	
	
	Logger logger = LoggerFactory.getLogger(AkwaKartService.class);
	/**
	 * @param response
	 * @param userLocale
	 * @return
	 */
	public Template checkEligibilityAndReturnProducts(String response, String userLocale) {
		JSONObject jsonRes = new JSONObject(response);
		
		JSONArray products = jsonRes.getJSONArray("akwaKartAllProducts"); 
				//new JSONArray(response);
		if(products.length() >0) {
			List<Element> elements = new ArrayList<>();
			for (int i = 0; i < products.length(); i++) {
				JSONObject product = products.getJSONObject(i);
				logger.debug("Deduct from balance product # "+i+" "+product );
				List<Button> buttons = new ArrayList<>();
				String label = getElementInfo(userLocale, product).get(Constants.RESPONSE_MAP_LABEL_KEY);
				String title = getElementInfo(userLocale, product).get(Constants.RESPONSE_MAP_TITLE_KEY);
				String subtitle = getElementInfo(userLocale, product).get(Constants.RESPONSE_MAP_SUBTITLE_KEY);
				PostbackButton button = PostbackButton.create(label,product.getString(Constants.AKWAKART_PRODUCT_NAME));
				URL imageUrl = Utils.createUrl(chatBotService.getBotConfigurationByKey(Constants.IMAGES_URL_KEY).getValue()+product.getString(Constants.AKWAKART_PRODUCT_IMAGE_NAME));
				logger.debug("Product image url "+imageUrl);
				buttons.add(button);
				Element element = Element.create(title, Optional.of(subtitle), Optional.of(imageUrl), empty(), Optional.of(buttons));
				elements.add(element);
			}
			return GenericTemplate.create(elements); 
		}else {
			logger.debug("AkwaKart No eligible deduct from balance products ");
			return  noEligibleForDeductTemplate(userLocale);
		}
	}

	
	
	/**
	 * @param userLocale
	 * @return
	 */
	private Map<String,String> notEligibleToborrowElementInformation(String userLocale) {
		Map<String,String> values = new HashMap<>();
		if(userLocale.contains(Constants.LOCALE_AR)) {
			values.put(Constants.RESPONSE_MAP_TITLE_KEY, Constants.NOTELIGIBLE_ELEMENT_TITLE_AR);
			values.put(Constants.RESPONSE_MAP_SUBTITLE_KEY, chatBotService.getBotConfigurationByKey(Constants.NOT_ELIGIBLE_SALLEFNY_SUBTITLE_AR).getValue());
			values.put(Constants.RESPONSE_MAP_LABEL_KEY, Constants.BUTTON_LABEL_BACK_AR);
		}else {
			values.put(Constants.RESPONSE_MAP_TITLE_KEY,Constants.NOTELIGIBLE_ELEMENT_TITLE_EN );
			values.put(Constants.RESPONSE_MAP_SUBTITLE_KEY, chatBotService.getBotConfigurationByKey(Constants.NOT_ELIGIBLE_SALLEFNY_SUBTITLE_EN).getValue());
			values.put(Constants.RESPONSE_MAP_LABEL_KEY, Constants.BUTTON_LABEL_BACK_EN);
		}
		return values;
	
	}

	
	private GenericTemplate noEligibleForDeductTemplate(String userLocale) {
		Map<String , String > values = notEligibleToborrowElementInformation(userLocale);
		List<Button> buttons = new ArrayList<>();
		List<Element> elements = new ArrayList<>();
		String stringUrl = chatBotService.getBotConfigurationByKey(Constants.CONFIGURATION_TABLE_WARNING_IMAGE_URL).getValue();
		URL url = Utils.createUrl(stringUrl);
		PostbackButton backButton = PostbackButton.create(values.get(Constants.RESPONSE_MAP_LABEL_KEY), Constants.PAYLOAD_ACCOUNT_DETAILS);
		buttons.add(backButton);
		Element element = Element.create(values.get(Constants.RESPONSE_MAP_TITLE_KEY), Optional.of(values.get(Constants.RESPONSE_MAP_SUBTITLE_KEY)), Optional.of(url), empty(), Optional.of(buttons));
		elements.add(element);
		return GenericTemplate.create(elements);
	}


	private Map<String,String> getElementInfo(String userLocale , JSONObject product){
		Map<String,String> values = new HashMap<>();
		if(userLocale.contains(Constants.LOCALE_AR)) {
		values.put(Constants.RESPONSE_MAP_TITLE_KEY, product.getString(Constants.AKWAKART_PRODUCT_AR_NAME));
		values.put(Constants.RESPONSE_MAP_SUBTITLE_KEY ,product.getString(Constants.AKWAKART_PRODUCT_AR_DESCRITION));
		values.put(Constants.RESPONSE_MAP_LABEL_KEY, Constants.BUTTON_LABEL_SUBSCRIBE_AR);
		return values;
	}else {
		values.put(Constants.RESPONSE_MAP_TITLE_KEY, product.getString(Constants.AKWAKART_PRODUCT_EN_NAME));
		values.put(Constants.RESPONSE_MAP_SUBTITLE_KEY ,product.getString(Constants.AKWAKART_PRODUCT_EN_DESCRITION));
		values.put(Constants.RESPONSE_MAP_LABEL_KEY, Constants.BUTTON_LABEL_BACK_SUBSCRIBE_EN);
		return values;
	}
	}



	/**
	 * @param paramNames
	 * @param paramValuesList
	 * @return
	 */
	public JSONObject deductFromBalanceRequestBody(ArrayList<String> paramNames, ArrayList<String> paramValuesList) {
		JSONObject body = new JSONObject();
		logger.debug("Deduct from balance request body size "+paramNames.size());
		for(int i = 0 ; i < paramNames.size() ;i++) {
			logger.debug(" Deduct from balance  param key "+paramNames.get(i) +" Deduct from balance param value "+paramValuesList.get(i));
			body.put(paramNames.get(i), paramValuesList.get(i));
		}
		return body;
	}



	
	
}
