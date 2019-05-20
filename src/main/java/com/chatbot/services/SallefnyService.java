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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chatbot.util.Constants;
import com.chatbot.util.Utils;
import com.github.messenger4j.send.message.template.GenericTemplate;
import com.github.messenger4j.send.message.template.button.Button;
import com.github.messenger4j.send.message.template.button.PostbackButton;
import com.github.messenger4j.send.message.template.common.Element;

/**
 * @author A.Eissa 
 */
@Service
public class SallefnyService {
	
	@Autowired
	private ChatBotService chatBotService;

	
	public GenericTemplate sallefnyHandling(JSONArray products , String userLocale){
		if(products.length() > 0) {
			return eligibleSallefnyProducts(products,userLocale);
		}else {
			return notElegibleForSallefnyService(userLocale);
		}
	}

	/**
	 * @param products
	 * @return
	 */
	private GenericTemplate notElegibleForSallefnyService(String userLocale) {
		Map<String , String > values = noTEligibleToborrowElementInformation(userLocale);
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

	
	private GenericTemplate eligibleSallefnyProducts(JSONArray products , String userLocale) {
		List<Element> elements = new ArrayList<>();
		for (int i = 0; i < products.length(); i++) {
			JSONObject product = products.getJSONObject(i);
			List<Button> buttons = new ArrayList<>();
			String label = sallefnyProductElement(userLocale, product).get(Constants.RESPONSE_MAP_LABEL_KEY);
			String title = sallefnyProductElement(userLocale, product).get(Constants.RESPONSE_MAP_TITLE_KEY);
			String subtitle = sallefnyProductElement(userLocale, product).get(Constants.RESPONSE_MAP_SUBTITLE_KEY);
			PostbackButton button = PostbackButton.create(label, Constants.PREFIX_SALLEFNY_INTERACTION + product.getString(Constants.SALLEFNY_PRODUCT_NAME_KEY));
			buttons.add(button);
			Element element = Element.create(title, Optional.of(subtitle), empty(), empty(), Optional.of(buttons));
			elements.add(element);
		}
		return GenericTemplate.create(elements);
	}
	
	
	private  Map<String , String > sallefnyProductElement(String userLocale , JSONObject product) {
		Map<String , String > values = new HashMap<>();
		if(userLocale.contains("ar")) {
			values.put(Constants.RESPONSE_MAP_TITLE_KEY, product.getString(Constants.SALLEFNY_ELEMENT_TITLE_KEY_AR));
			values.put(Constants.RESPONSE_MAP_SUBTITLE_KEY, product.getString(Constants.SALLEFNY_ELEMENT_SUBTITLE_KEY_AR));
			values.put(Constants.RESPONSE_MAP_LABEL_KEY, Constants.SALLEFNY_BUTTON_LABEL_AR);
			return values;
		}else {
			values.put(Constants.RESPONSE_MAP_TITLE_KEY, product.getString(Constants.SALLEFNY_ELEMENT_TITLE_KEY_EN));
			values.put(Constants.RESPONSE_MAP_SUBTITLE_KEY, product.getString(Constants.SALLEFNY_ELEMENT_SUBTITLE_KEY_EN));
			values.put(Constants.RESPONSE_MAP_LABEL_KEY, Constants.SALLEFNY_BUTTON_LABEL_EN);
			return values;
		}
	}
	
	
	private Map<String , String > noTEligibleToborrowElementInformation(String userLocale){
		Map<String,String> values = new HashMap<>();
		if(userLocale.contains("ar")) {
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
}
