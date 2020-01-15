/** copyright Etisalat social team. To present All rights reserved
*/
/**
 * 
 */
package com.chatbot.services;

import static java.util.Optional.empty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chatbot.entity.UserSelection;
import com.chatbot.util.Constants;
import com.chatbot.util.Utils;
import com.github.messenger4j.send.message.template.GenericTemplate;
import com.github.messenger4j.send.message.template.Template;
import com.github.messenger4j.send.message.template.button.Button;
import com.github.messenger4j.send.message.template.button.PostbackButton;
import com.github.messenger4j.send.message.template.common.Element;

/**
 * @author A.Eissa 
 */
@Service
public class HarleyService {

	@Autowired
	private UtilService utilService;

	private static final Logger logger = LoggerFactory.getLogger(HarleyService.class);
	/**
	 * @param response
	 * @param userLocale
	 * @return
	 */
	public GenericTemplate harleyAddonRateplansByCategoryId(String senderId,String response, String userLocale) {
		logger.debug(Constants.LOGGER_INFO_PREFIX + " Harley rateplans by its categoryId ");
		UserSelection userSelection = utilService.getUserSelectionsFromCache(senderId);
		String addonCategoryId = userSelection.getHarleyAddonCategory();
		JSONArray addonsArray = new JSONArray(response);
		List<Element> elements = new ArrayList<>();
		for (int i = 0; i < addonsArray.length(); i++) {
			String categoryId = addonsArray.getJSONObject(i).getJSONObject(Constants.JSON_KEY_CATEGORY_KEY).getString(Constants.JSON_KEY_CATEGORY_ID);
			if(addonCategoryId.equalsIgnoreCase(categoryId)) {
				JSONArray products = addonsArray.getJSONObject(i).getJSONArray(Constants.JSON_KEY_FOR_PRODUCT);
				if(products.length() < 10 ) {
				for (int j = 0; j < products.length(); j++) {
					List<Button> buttons = new ArrayList<>();
					JSONObject product = products.getJSONObject(j);
					String productName = product.getString("name");
					String title = userLocale.contains(Constants.LOCALE_AR) ? product.getString(Constants.JSON_KEY_NAME_AR):product.getString(Constants.JSON_KEY_NAME_EN);
					String subtitle = userLocale.contains(Constants.LOCALE_AR) ? product.getString(Constants.JSON_KEY_ARABIC_DESCRIPTION_KEY) : product.getString(Constants.JSON_KEY_FOR_ENGLISH_DESCRIPTION);
					String label = userLocale.contains(Constants.LOCALE_AR) ? Constants.BUTTON_LABEL_SUBSCRIBE_AR : Constants.BUTTON_LABEL_SUBSCRIBE_EN;
					PostbackButton productButton = PostbackButton.create(label , Constants.HARLEY_ADDON_PRODUCT_PREFEX+Constants.COMMA_CHAR+productName);
					PostbackButton backButton = PostbackButton.create(Utils.getLabelForBackButton(userLocale),Constants.PAYLOAD_RATEPLAN_DETAILS);
					buttons.add(productButton);
					buttons.add(backButton);
					Element productElement =Element.create(title, Optional.of(subtitle), empty(), empty(), Optional.of(buttons));
					elements.add(productElement);
				}
				}
			}
		}
		return  GenericTemplate.create(elements);		
	}
	
	// Get All Harley Addons Categories 
	public List<Element> getHarleyAddonBundlePlansCategories(String locale,ChatBotService chatBotService) {
		logger.debug(Constants.LOGGER_INFO_PREFIX + "Harley Addon Rateplans ");
		String[] categoriesArray = chatBotService.getEnabledCategoryConfigurationDaoById(3l).getEnglishCategories().split(Constants.COMMA_CHAR);
		String [] categoryNames = locale.contains(Constants.LOCALE_AR) ? chatBotService.getEnabledCategoryConfigurationDaoById(3l).getCategoryLabel().getArabicText().split(Constants.COMMA_CHAR):
											   chatBotService.getEnabledCategoryConfigurationDaoById(3l).getCategoryLabel().getEnglishText().split(Constants.COMMA_CHAR);
		List<Element> elements = new ArrayList<>();
		if(categoriesArray.length == categoryNames.length) {
			for(int i=0 ; i<categoriesArray.length ; i++) {
				PostbackButton categoryButton = PostbackButton.create(harleyCategoryButtonLabel(locale), Constants.HARLEY_ADDON_CATEGORY_PREFEX+Constants.COMMA_CHAR+ categoriesArray[i]);
				List<Button> buttons = new ArrayList<>();
				buttons.add(categoryButton);
				Element categoryElement = Element.create(categoryNames[i], Optional.of("---"), empty(), empty(),Optional.of(buttons));
				elements.add(categoryElement);
			}
		}
		return elements;
	}

	/**
	 * @param locale
	 * @return
	 */
	private String harleyCategoryButtonLabel(String locale) {
		return locale.contains(Constants.LOCALE_AR)? "عرض الباقات " : "Show Bundles";
	}
	/**
	 * @param paramNames
	 * @param userSelections
	 * @return
	 */
	public JSONObject setBuyAddonRequestBodyValue(ArrayList<String> paramNames, UserSelection userSelections) {
		JSONObject jsonParam = new JSONObject();
		jsonParam.put(paramNames.get(0), userSelections.getHarleyProductName());
		jsonParam.put(paramNames.get(1), Constants.ACTIVATE_OPERATION);
		return jsonParam;
	}

	
	/**
	 * @param paramNames
	 * @param userSelections
	 * @return
	 */
	public JSONObject setRenewBundleRequestBodyValue(ArrayList<String> paramNames) {
		JSONObject jsonParam = new JSONObject();
		jsonParam.put(paramNames.get(0), Constants.HARLEY_PRODUCT_NAME);
		jsonParam.put(paramNames.get(1), Constants.RENEW_OPERATION_VALUE);
		return jsonParam;
	}
	/**
	 * @param paramNames
	 * @param userSelections
	 * @return
	 */
	public JSONObject setAddFafNumRequestBodyValue(ArrayList<String> paramNames, UserSelection userSelections, String senderId) {
		JSONObject jsonParam = new JSONObject();
		jsonParam.put(paramNames.get(0), Constants.HARLEY_PRODUCT_NAME);
		jsonParam.put(paramNames.get(1), userSelections.getHarleyFafNumberValue());
		jsonParam.put(paramNames.get(2), userSelections.getHarleyFafNumbRenewelMode());
		jsonParam.put(paramNames.get(3), Constants.ACTIVATE_OPERATION);
		jsonParam.put(paramNames.get(4), Constants.HARLEY_ADD_FAF_SERVICE_NAME);
		userSelections.setHarleyFafNumberValue("");
		userSelections.setHarleyFafNumbRenewelMode("");
		utilService.updateUserSelectionsInCache(senderId, userSelections);
		return jsonParam;
	}
	
	
	public Template getEtisalatFreeServicesForHarleyRateplan(String response ,ChatBotService chatBotService , String userLocale){
		JSONArray productRelationshipsList = new JSONObject(response).getJSONArray("productRelationshipsList");
		List<Element> elements = new ArrayList<>();
		List<String> categories = Arrays.asList(chatBotService.getEnabledCategoryConfigurationDaoById(4l).getEnglishCategories().split(Constants.COMMA_CHAR));
		for(int i = 0 ; i < productRelationshipsList.length() ; i++) {
			String productId = productRelationshipsList.getJSONObject(i).getString(Constants.JSON_PRODUCT_NAME_KEY);
			if(categories.contains(productId)) {
				String productName =  getFreeServiceName(userLocale,productRelationshipsList.getJSONObject(i));
				String title = userLocale.contains(Constants.LOCALE_AR) ? Constants.BUTTON_LABEL_SUBSCRIBE_AR : Constants.BUTTON_LABEL_SUBSCRIBE_EN;
				PostbackButton productButton = PostbackButton.create(title , Constants.FREE_ETISALAT_SERVICES + Constants.COMMA_CHAR +productId);
				List<Button> buttons = new ArrayList<>();
				buttons.add(productButton);
				Element element = Element.create(productName, Optional.of("---"), empty(), empty(), Optional.of(buttons));
				elements.add(element);
			}
		}
		return GenericTemplate.create(elements);
	}
	/**
	 * @param userLocale
	 * @param jsonObject
	 * @return
	 */
	private String getFreeServiceName(String userLocale, JSONObject jsonObject) {
		return userLocale.contains(Constants.LOCALE_AR) ? jsonObject.getString(Constants.JSON_PRODUCT_AR_NAME) : jsonObject.getString(Constants.JSON_PRODUCT_EN_NAME); 
	}
	/**
	 * @param paramNames
	 * @param userSelections
	 * @param senderId
	 * @return
	 */
	public JSONObject setSubscribeFreeServicesRequestBody(UserSelection userSelections) {
		JSONObject body = new JSONObject(); 
		JSONObject orderLineRequest = new JSONObject();
		orderLineRequest.put("operation", "CHANGE_FREE_SERVICE");
		orderLineRequest.put("productName", userSelections.getFreeServiceName());
		JSONArray parameterListRequest = new JSONArray();
		JSONObject childDetail = new JSONObject();
		childDetail.put("name", "PROMO_CODE");
		childDetail.put("value", "1");
		parameterListRequest.put(childDetail);
		orderLineRequest.put("parameterListRequest", parameterListRequest);
		body.put("orderLineRequest", orderLineRequest);
		return body ;
	}
	
	
	
	
}
