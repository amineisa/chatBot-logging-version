/** copyright Etisalat social team. To present All rights reserved
*/
/**
 * 
 */
package com.chatbot.services;

import static java.util.Optional.empty;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	private ChatBotService chatBotService;

	private Logger logger = LoggerFactory.getLogger(EmeraldService.class);

	 boolean checkAddChildEligibility(JSONObject object, String[] keys) {
		return object.getBoolean(keys[0]);
	}

	/**
	 * @param locale
	 * @return
	 */
	public String sendNotEligibleTOAddChildMsg(String locale) {
		return locale.contains(Constants.LOCALE_AR) ? botConfigurationRepo.findBotConfigurationByKey(Constants.EMERALD_NOT_ELIGIBLE_ADD_MEMBER_AR).getValue()
				: botConfigurationRepo.findBotConfigurationByKey(Constants.EMERALD_NOT_ELIGIBLE_ADD_MEMBER_EN).getValue();

	}

	/**
	 * @param locale
	 * @return
	 */
	public String sendEligibleTOAddChildMsg(String locale) {
		return locale.contains(Constants.LOCALE_AR) ? botConfigurationRepo.findBotConfigurationByKey(Constants.EMERALD_ELIGIBLE_ADD_MEMBER_AR).getValue()
				: botConfigurationRepo.findBotConfigurationByKey(Constants.EMERALD_ELIGIBLE_ADD_MEMBER_EN).getValue();

	}

	/**
	 * @param paramNames
	 * @param userSelections
	 * @return
	 */
	public JSONObject addChildRequestBody(ArrayList<String> paramNames, UserSelection userSelections) {
		JSONObject body = new JSONObject();
		body.put(paramNames.get(0), userSelections.getEmeraldRateplanProductName());
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
		return body;
	}

	/**
	 * @param jsonObject
	 * @param userlocale
	 * @param elements
	 */
	public Template getAllEmeraldTraficCases(JSONObject jsonObject, String userlocale) {
		List<Element> elements = new ArrayList<>();
		JSONArray bundleResponses = jsonObject.getJSONArray(Constants.JSON_KEY_BUNDLE_RESPONSE);
		for (int i = 0; i < bundleResponses.length(); i++) {
			List<Button> buttons = new ArrayList<>();
			JSONObject object = bundleResponses.getJSONObject(i);
			String payload = object.getString(Constants.FB_JSON_KEY_ID);
			String label = userlocale.contains(Constants.LOCALE_AR) ? object.getString(Constants.JSON_KEY_NAME_AR) : object.getString(Constants.JSON_KEY_NAME_EN);
			Button button = PostbackButton.create(label, Constants.EMERALD_GET_DIALS_FOR_DISTRIBUTE_PAYLOAD + payload);
			String title = userlocale.contains(Constants.LOCALE_AR) ? object.getString(Constants.JSON_KEY_ARABIC_DESCRIPTION_KEY) : object.getString(Constants.JSON_KEY_FOR_ENGLISH_DESCRIPTION);
			buttons.add(button);
			Element element = Element.create(title, Optional.of("---"), Optional.empty(), Optional.empty(), Optional.of(buttons));
			elements.add(element);
		}
		return GenericTemplate.create(elements);

	}

	public Template getAllEmeraldTraficCasesByChildDial(JSONObject jsonObject, CustomerProfile customerProfile, UserSelection userSelection) {
		List<Element> elements = new ArrayList<>();
		String userLocale = customerProfile.getLocale();
		String childDial = userSelection.getEmeraldTransferFromDial();
		JSONObject details = jsonObject.getJSONObject(Constants.EMERALD_DETAILS_KEY);
		JSONArray traficCase = details.getJSONArray(childDial);
		for (int i = 0; i < traficCase.length(); i++) {
			List<Button> buttons = new ArrayList<>();
			JSONObject object = traficCase.getJSONObject(i);
			if (object.get(Constants.FB_JSON_KEY_ID) != null) {
				boolean distbuteAfterTrans = object.getBoolean(Constants.DISTRIBUTE_AFTER_TRANSFER);
				String payload = object.getString(Constants.FB_JSON_KEY_ID);
				String label = userLocale.contains(Constants.LOCALE_AR) ? object.getString(Constants.JSON_KEY_NAME_AR) : object.getString(Constants.JSON_KEY_NAME_EN);
				Button button = PostbackButton.create(label, Constants.EMERALD_CHILD_TRANSFER_TO_PAYLOAD + Constants.COMMA_CHAR + payload + Constants.COMMA_CHAR + distbuteAfterTrans);
				String title = userLocale.contains(Constants.LOCALE_AR) ? object.getString(Constants.JSON_KEY_ARABIC_DESCRIPTION_KEY) : object.getString(Constants.JSON_KEY_FOR_ENGLISH_DESCRIPTION);
				buttons.add(button);
				Element element = Element.create(title, Optional.of("---"), Optional.empty(), Optional.empty(), Optional.of(buttons));
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
		return body;
	}

	public JSONObject transferRequestBody(ArrayList<String> paramNames, UserSelection userSelection) {
		JSONObject body = new JSONObject();
		List<String> values = new ArrayList<>();
		String productName = userSelection.getEmeraldRateplanProductName();
		String trafficCase = userSelection.getEmeraldTraficCaseID();
		String source = encryptDPIParam(userSelection.getEmeraldTransferFromDial());
		String target = encryptDPIParam(userSelection.getEmeraldTransferToDial());
		String quota = userSelection.getEmeraldDistributeAmount();
		String distrbuteAfterTransfare = userSelection.getEmeraldDistbuteAfterTrans().equals("true") ? "Y" : "N";
		values.add(productName);
		values.add(trafficCase);
		values.add(source);
		values.add(target);
		values.add(quota);
		values.add(distrbuteAfterTransfare);
		for (int i = 0; i < paramNames.size(); i++) {
			body.put(paramNames.get(i), values.get(i));
		}
		return body;
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

	// Get All Emerald Addons Categories
	public List<Element> getEmeraldAddonBundlePlansCategories(String locale, ChatBotService chatBotService) {
		logger.debug(Constants.LOGGER_INFO_PREFIX + "Emerald Addon Rateplans ");
		String[] categoriesArray = chatBotService.getEnabledCategoryConfigurationDaoById(5l).getEnglishCategories().split(Constants.COMMA_CHAR);
		String[] categoryNames = locale.contains(Constants.LOCALE_AR) ? chatBotService.getEnabledCategoryConfigurationDaoById(5l).getCategoryLabel().getArabicText().split(Constants.COMMA_CHAR)
				: chatBotService.getEnabledCategoryConfigurationDaoById(3l).getCategoryLabel().getEnglishText().split(Constants.COMMA_CHAR);
		List<Element> elements = new ArrayList<>();
		if (categoriesArray.length == categoryNames.length) {
			for (int i = 0; i < categoriesArray.length; i++) {
				PostbackButton categoryButton = PostbackButton.create(emeraldCategoryButtonLabel(locale), Constants.EMERALD_ADDON_CATEGORY_PREFEX + Constants.COMMA_CHAR + categoriesArray[i]);
				List<Button> buttons = new ArrayList<>();
				buttons.add(categoryButton);
				Element categoryElement = Element.create(categoryNames[i], Optional.of("---"), empty(), empty(), Optional.of(buttons));
				elements.add(categoryElement);
			}
		}
		return elements;
	}

	/**
	 * @param locale
	 * @return
	 */
	private String emeraldCategoryButtonLabel(String locale) {
		return locale.contains(Constants.LOCALE_AR) ? "عرض الباقات " : "Show Bundles";
	}

	public  GenericTemplate getEmeraldRateplanRateplanAddosAccordingToCategoryId(String response, UserSelection userSelection, String userLocale) {
		JSONArray jsonResponse = new JSONArray(response);
		JSONArray products = new JSONArray();
		for (int i = 0; i < jsonResponse.length(); i++) {
			JSONObject object = jsonResponse.getJSONObject(i);
			String categoryID = object.getJSONObject(Constants.JSON_KEY_CATEGORY_KEY).getString(Constants.JSON_KEY_CATEGORY_ID);
			if (categoryID.equalsIgnoreCase(userSelection.getEmearldAddonCategoryId())) {
				products = object.getJSONArray(Constants.JSON_KEY_FOR_PRODUCT);
			}
		}
		return createProductTemplate(products, userLocale);

	}

	/**
	 * @param products
	 * @param userLocale
	 * @return
	 */
	private GenericTemplate createProductTemplate(JSONArray products, String userLocale) {
		List<Element> elements = new ArrayList<>();
		if (products.length() > 0) {
			for (int i = 0; i < products.length(); i++) {
				JSONObject product = products.getJSONObject(i);
				List<Button> buttons = new ArrayList<>();
				String buttonLabel = userLocale.contains(Constants.LOCALE_AR) ? Constants.BUTTON_LABEL_SUBSCRIBE_AR : Constants.BUTTON_LABEL_SUBSCRIBE_EN;
				String productID = product.getString(Constants.JSON_KEY_FOR_NAME);
				PostbackButton button = PostbackButton.create(buttonLabel, Constants.EMERALD_ADDON_SUBMIT_ORDER + Constants.COMMA_CHAR + productID);
				String title = getCategoryName(userLocale, product);
				String subtitle = getCategoryDescription(userLocale, product);
				buttons.add(button);
				Element element = Element.create(title, Optional.of(subtitle), empty(), empty(), Optional.of(buttons));
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
	private String getCategoryName(String userLocale, JSONObject jsonObject) {
		return userLocale.contains(Constants.LOCALE_AR) ? jsonObject.getString(Constants.JSON_KEY_NAME_AR) : jsonObject.getString(Constants.JSON_KEY_NAME_EN);
	}

	/**
	 * @param userLocale
	 * @param jsonObject
	 * @return
	 */
	private String getCategoryDescription(String userLocale, JSONObject jsonObject) {
		return userLocale.contains(Constants.LOCALE_AR) ? jsonObject.getString(Constants.JSON_KEY_ARABIC_DESCRIPTION_KEY) : jsonObject.getString(Constants.JSON_KEY_FOR_ENGLISH_DESCRIPTION);
	}

	/**
	 * @param paramNames
	 * @param userSelections
	 * @param senderId
	 * @return
	 */
	public JSONObject setBuyAddonRequestBodyValue(ArrayList<String> paramNames, UserSelection userSelections) {
		JSONObject body = new JSONObject();
		body.put(paramNames.get(0), Constants.ACTIVATE_OPERATION);
		body.put(paramNames.get(1), userSelections.getEmearldAddonId());
		return body;
	}

}
