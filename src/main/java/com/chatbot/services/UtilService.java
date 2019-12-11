package com.chatbot.services;

import static java.util.Optional.empty;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.chatbot.entity.AddonCategory;
import com.chatbot.entity.BotButton;
import com.chatbot.entity.BotButtonTemplateMSG;
import com.chatbot.entity.BotConfiguration;
import com.chatbot.entity.BotInteractionMessage;
import com.chatbot.entity.BotTextMessage;
import com.chatbot.entity.BotTextResponseMapping;
import com.chatbot.entity.BotWebserviceMessage;
import com.chatbot.entity.CustomerProfile;
import com.chatbot.entity.InteractionLogging;
import com.chatbot.entity.UserSelection;
import com.chatbot.util.Constants;
import com.chatbot.util.Utils;
import com.github.messenger4j.common.WebviewHeightRatio;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.MessagingType;
import com.github.messenger4j.send.message.TemplateMessage;
import com.github.messenger4j.send.message.TextMessage;
import com.github.messenger4j.send.message.template.ButtonTemplate;
import com.github.messenger4j.send.message.template.GenericTemplate;
import com.github.messenger4j.send.message.template.button.Button;
import com.github.messenger4j.send.message.template.button.LogInButton;
import com.github.messenger4j.send.message.template.button.LogOutButton;
import com.github.messenger4j.send.message.template.button.PostbackButton;
import com.github.messenger4j.send.message.template.button.UrlButton;
import com.github.messenger4j.send.message.template.common.Element;
import com.hazelcast.core.IMap;

/**
 * @author Amin Eisa
 */
@Service
public class UtilService {

	@Autowired
	private JSONUtilsService jsonUtilsService;
	@Autowired
	private ChatBotService chatBotService;
	@Autowired
	private IMap<String, Object> usersSelectionCache;
	@Autowired
	private EmeraldService emeraldService;

	private static final Logger logger = LoggerFactory.getLogger(UtilService.class);

	public UserSelection getUserSelectionsFromCache(String senderId) {
		return usersSelectionCache.get(senderId) == null ? new UserSelection() : (UserSelection) usersSelectionCache.get(senderId);
	}

	public void updateUserSelectionsInCache(String senderId, UserSelection userSelection) {
		usersSelectionCache.put(senderId, userSelection);
	}

	// Method to get Path Details
	public String[] getPaths(String path) {
		if (path != null) {
			String[] paths = new String[0];
			if (path.contains(Constants.COMMA_CHAR)) {
				paths = path.split(Constants.COMMA_CHAR);
			} else if (path.length() > 1 && !path.contains(Constants.COMMA_CHAR)) {
				paths = new String[] { path };
			}
			return paths;
		} else {
			return new String[0];
		}
	}

	// Method to get keys
	public String[] getKeys(String key) {
		if (key != null) {
			String[] keys = new String[0];
			if (key.contains(Constants.COMMA_CHAR)) {
				keys = key.split(Constants.COMMA_CHAR);
			} else {
				keys = new String[] { key };
			}
			return keys;
		} else {
			return new String[0];
		}
	}

	// Switcher for JSONObject
	public Map<String, ArrayList<String>> switchToObjectMode(JSONObject jsonResponse, String[] paths, String[] keys, String msg, String locale) {
		Map<String, ArrayList<String>> mapValues = new HashMap<>();
		int length = paths.length;
		if (length == 0) {
			mapValues = jsonUtilsService.inCaseZeroLevelJsonObject(keys, jsonResponse, msg, locale);
		} else if (length == 1) {
			mapValues = jsonUtilsService.inCaseOneLevelJsonObject(paths, keys, jsonResponse, msg, locale);
		} else if (length == 2) {
			mapValues = jsonUtilsService.inCaseTwoLevelJsonObject(jsonResponse, paths, keys, msg, locale);
		}
		return mapValues;
	}

	// Switcher for JSONArray
	public Map<String, ArrayList<String>> switchToArrayMode(JSONArray jsonResponse, String[] paths, String[] keys, String msg, String locale) {
		Map<String, ArrayList<String>> mapValues = new HashMap<>();
		int length = paths.length;
		if (length == 0) {
			mapValues = jsonUtilsService.inCaseZeroLevelJsonArray(keys, jsonResponse, msg, locale);
		} else if (length == 1) {
			mapValues = jsonUtilsService.inCaseOneLevelJsonArrayForTextMessage(paths, keys, jsonResponse, msg, locale);
		} else if (length == 2) {
			mapValues = jsonUtilsService.inCaseTwoLevelJsonArrayForTextMessage(jsonResponse, paths, keys, msg, locale);
		}
		return mapValues;
	}

	// Create Button
	public Button createButton(BotButton botButton, String locale, JSONObject jsonObject, String dialNumber) {
		if (botButton.getButtonType().getId() == Utils.ButtonTypeEnum.POSTBACK.getValue()) {// PostBack
			String payload = botButton.getButtonPayload();
			String lable = Utils.getTextValueForButtonLabel(locale, botButton);
			if (payload.contains(Constants.IS_KEY_HAS_DOT) && lable.contains(Constants.IS_KEY_HAS_DOT)) {
				String finalPayLoad = jsonUtilsService.getValuesFromJson(jsonObject, new String[] { payload }).get(0);
				String finalText = jsonUtilsService.getValuesFromJson(jsonObject, new String[] { lable }).get(0);
				if (finalPayLoad.contains(Constants.UNDERSCORE)) {
					finalPayLoad = Constants.PREFIX_RATEPLAN_SUBSCRIPTION + finalPayLoad;
				}
				return PostbackButton.create(finalText, finalPayLoad);
			}
			return PostbackButton.create(Utils.getTextValueForButtonLabel(locale, botButton), botButton.getButtonPayload());
		} else if (botButton.getButtonType().getId() == Utils.ButtonTypeEnum.URL.getValue()) {// URl
			URL url = Utils.createUrl(botButton.getButtonUrl());
			String localeParamValue = locale.contains(Constants.LOCALE_AR) ? Constants.URL_LOCALE_AR : Constants.URL_LOCALE_EN;
			if (botButton.getButtonUrl().contains("recharge") || botButton.getButtonUrl().contains("balanceDeduction") || botButton.getButtonUrl().contains("payment") || botButton.getButtonUrl().contains("distribute")) {
				URL realURL = null;
				try {
					String stringUrl = url + localeParamValue + paramAndParamChannelEncryptionForWebView(dialNumber);
					realURL = Utils.createUrl(stringUrl);
					logger.debug(Constants.LOGGER_INFO_PREFIX + "Button template button url " + realURL.toString());
				} catch (Exception e) {
					e.printStackTrace();
				}
				return UrlButton.create(Utils.getTextValueForButtonLabel(locale, botButton), realURL,Optional.of(WebviewHeightRatio.TALL));
			}
				return UrlButton.create(Utils.getTextValueForButtonLabel(locale, botButton), url);
		} else if (botButton.getButtonType().getId() == Utils.ButtonTypeEnum.LOGIN.getValue()) {// Login
			URL url = Utils.createUrl(botButton.getButtonUrl());
			return LogInButton.create(url);
		} else if (botButton.getButtonType().getId() == Utils.ButtonTypeEnum.LOGOUT.getValue()) {// Logout
			return LogOutButton.create();
		}
		return null;
	}

	/**
	 * @param botInteractionMessage
	 * 
	 * @param chatBotService
	 * @return
	 */
	public ButtonTemplate createButtonTemplateInScenario(BotInteractionMessage botInteractionMessage, String local, String dialNumber) {
		String title = Constants.EMPTY_STRING;
		ArrayList<Button> buttons = new ArrayList<>();
		List<BotButtonTemplateMSG> botButtonTemplateMSGs = chatBotService.findBotButtonTemplateMSGByBotInteractionMessage(botInteractionMessage);
		for (BotButtonTemplateMSG botButtonTemplateMSG : botButtonTemplateMSGs) {
			title = getTextForButtonTemplate(local, botButtonTemplateMSG);
			List<BotButton> botButtons = chatBotService.findAllByBotButtonTemplateMSGId(botButtonTemplateMSG);
			for (BotButton botButton : botButtons) {
				Button button = createButton(botButton, local, new JSONObject(), dialNumber);
				buttons.add(button);
			}
		}
		return ButtonTemplate.create(title, buttons);

	}

	public String getTextForButtonTemplate(String locale, BotButtonTemplateMSG botButtonTemplateMSG) {
		if (locale == null) {
			locale = Constants.LOCALE_EN;
		}
		return locale.contains(Constants.LOCALE_AR) ? botButtonTemplateMSG.getBotText().getArabicText() : botButtonTemplateMSG.getBotText().getEnglishText();
	}

	/**
	 * @param senderId
	 * @param messagePayloadList
	 * @param botWebserviceMessage
	 * @param jsonBodyString
	 * @throws JSONException
	 */

	public void getTextMessageIfResponseIsArray(String senderId, List<MessagePayload> messagePayloadList, BotWebserviceMessage botWebserviceMessage, JSONArray rootArray, String locale) {
		MessagePayload messagePayload;
		List<BotTextResponseMapping> botTextResponseMappings = chatBotService.findTextResponseMappingByWsId(botWebserviceMessage.getWsMsgId());
		BotTextResponseMapping text = botTextResponseMappings.get(0);
		String msg = getTextForBotTextResponseMapping(locale, text);
		String path = text.getCommonPath();
		String[] paths = getPaths(path);
		String keys = getKeysString(text, locale);
		String[] keysArray = getKeys(keys);
		ArrayList<String> values = switchToArrayMode(rootArray, paths, keysArray, msg, locale).get(Constants.RESPONSE_MAP_MESSAGE_KEY);
		for (String val : values) {
			messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(val));
			messagePayloadList.add(messagePayload);
		}
		logger.debug(Constants.LOGGER_INFO_PREFIX + "Messages list Size : " + messagePayloadList.size());
	}

	public String getTextForBotTextResponseMapping(String local, BotTextResponseMapping botTextResponseMapping) {
		String text = Constants.EMPTY_STRING;
		if (local.equalsIgnoreCase(Constants.LOCALE_AR)) {
			text = botTextResponseMapping.getBotText().getArabicText();
		} else {
			text = botTextResponseMapping.getBotText().getEnglishText();
		}
		return text;
	}

	/**
	 * @param senderId
	 * @param messagePayloadList
	 * @param botWebserviceMessage
	 * @param jsonBodyString
	 * @throws JSONException
	 */

	public void getTextMessageIfResponseIsObject(String senderId, List<MessagePayload> messagePayloadList, BotWebserviceMessage botWebserviceMessage, String jsonBodyString, String locale) {
		MessagePayload messagePayload;
		List<BotTextResponseMapping> botTextResponseMappings = chatBotService.findTextResponseMappingByWsId(botWebserviceMessage.getWsMsgId());
		JSONObject rootObject = new JSONObject(jsonBodyString);
		for (BotTextResponseMapping botTextResponseMapping : botTextResponseMappings) {
			String msg = getTextForBotTextResponseMapping(locale, botTextResponseMapping);
			String path = botTextResponseMapping.getCommonPath();
			String[] paths = getPaths(path);
			String keys = Constants.EMPTY_STRING;
			if (locale.equalsIgnoreCase(Constants.LOCALE_AR)) {
				keys = botTextResponseMapping.getArParams();
			} else {
				keys = botTextResponseMapping.getEnParams();
			}
			String[] keysArray = getKeys(keys);
			if (botWebserviceMessage.getWsUrl().contains("family/childSetting") && emeraldService.checkAddChildEligibility(rootObject, keysArray)) {
				logger.debug(Constants.LOGGER_INFO_PREFIX + " Emerald Eligible TO Add Child ");
				msg = emeraldService.sendEligibleTOAddChildMsg(locale);
				messagePayloadList.add(MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(msg)));
			} else if (botWebserviceMessage.getWsUrl().contains("family/childSetting") && !emeraldService.checkAddChildEligibility(rootObject, keysArray)) {
				logger.debug(Constants.LOGGER_INFO_PREFIX + " Emerald Not Eligible TO Add Child");
				msg = emeraldService.sendNotEligibleTOAddChildMsg(locale);
				messagePayloadList.add(MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(msg)));	
			} else {
				logger.debug(Constants.LOGGER_INFO_PREFIX + " Text Message Creation");
				ArrayList<String> values = switchToObjectMode(rootObject, paths, keysArray, msg, locale).get(Constants.RESPONSE_MAP_MESSAGE_KEY);
				for (String val : values) {
					messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(val));
					messagePayloadList.add(messagePayload);
				}
			}
		}
	}

	public void getTextMessageIfResponseIsString(String senderId, List<MessagePayload> messagePayloadList, BotWebserviceMessage botWebserviceMessage, String local) {
		String text = Constants.EMPTY_STRING;
		if (local.equalsIgnoreCase(Constants.LOCALE_AR)) {
			text = botWebserviceMessage.getTitle().getArabicText();
		} else {
			text = botWebserviceMessage.getTitle().getEnglishText();
		}

		TextMessage textMSG = TextMessage.create(text);
		MessagePayload msgPayLoad = MessagePayload.create(senderId, MessagingType.RESPONSE, textMSG);
		messagePayloadList.add(msgPayLoad);
	}

	// Return key As one String according to customer local
	public String getKeysString(BotTextResponseMapping botTextResponseMapping, String local) {
		String keys = Constants.EMPTY_STRING;
		if (local.equalsIgnoreCase(Constants.LOCALE_AR)) {
			keys = botTextResponseMapping.getArParams();
		} else {
			keys = botTextResponseMapping.getEnParams();
		}
		return keys;
	}

	// get text for BotTextMessage according to local
	public String getTextValueForBotTextMessage(BotTextMessage botTextMessage, String locale, String userFirstName, String phoneNumber) {
		String text = locale.equalsIgnoreCase(Constants.LOCALE_AR) ? botTextMessage.getBotText().getArabicText() : botTextMessage.getBotText().getEnglishText();
		return Utils.replacePlaceholderByNameValue(text, userFirstName, phoneNumber);

	}

	/**
	 * call webService
	 * 
	 * @return String response
	 * @param botWebserviceMessage
	 */

	public Map<String, String> postWSCalling(BotWebserviceMessage botWebserviceMessage, String jsonParam, String senderId) {
		CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		String dialNumber = customerProfile.getMsisdn();
		Map<String, String> mapResponse = new HashMap<>();
		int responseStatusId = 0;
		String stringResponse = Constants.EMPTY_STRING;
		try {
			String paramChannel = paramAndParamChannelEncryption(dialNumber);
			CloseableHttpClient client = createClient();
			HttpPost httpPost = new HttpPost(botWebserviceMessage.getWsUrl());
			String wsUrl = botWebserviceMessage.getWsUrl() +Constants.URL_DIAL_PARAM_KEY + paramChannel;
			URI uri = new URI(wsUrl);
			httpPost.setURI(uri);
			logger.debug(Constants.LOGGER_INFO_PREFIX + "Post Service URL IS " + uri);
			logger.debug(Constants.LOGGER_BUNDLE_SUPSCRIPTION + jsonParam);
			StringEntity entity = new StringEntity(jsonParam);
			httpPost.setEntity(entity);
			httpPost.setHeader("Accept", "application/json");
			httpPost.setHeader("Content-type", "application/json");
			CloseableHttpResponse response = client.execute(httpPost);
			responseStatusId = response.getStatusLine().getStatusCode();
			org.apache.http.HttpEntity responseEntity = response.getEntity();
			stringResponse = EntityUtils.toString(responseEntity, "UTF-8");
			client.close();
		} catch (Exception e) {
			logger.error(Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_EXCEPTION_MESSAGE + e);
			e.printStackTrace();
		} finally {
			mapResponse.put(Constants.RESPONSE_STATUS_KEY, String.valueOf(responseStatusId));
			mapResponse.put(Constants.RESPONSE_KEY, stringResponse);
		}
		return mapResponse;
	}

	
	public ResponseEntity<String> postWSCallingRT(BotWebserviceMessage botWebserviceMessage, JSONObject jsonParam, String senderId){
		CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		String dialNumber = customerProfile.getMsisdn();
		URI uri = null;
		HttpEntity requestEntity = null;
		RestTemplate postRestTemplate = new RestTemplate();
			try {
				String paramChannel = paramAndParamChannelEncryption(dialNumber);
				String wsUrl = botWebserviceMessage.getWsUrl() +Constants.URL_DIAL_PARAM_KEY + paramChannel;
				uri = new URI(wsUrl);
				HttpHeaders headers = new HttpHeaders();
				headers.set("Content-type","application/json");
				requestEntity = new HttpEntity<>(new JSONObject(jsonParam),headers);			
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return postRestTemplate.exchange(uri, HttpMethod.POST, requestEntity, String.class);
			
	}
	
	
	
	
	
	
	
	
	
	
	private CloseableHttpClient createClient() {
		RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(100000).setConnectTimeout(5 * 1000).setConnectionRequestTimeout(5 * 1000).setSocketTimeout(5 * 1000).build();
		return HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
	}

	/**
	 * @param senderId
	 * @param messagePayloadList
	 * @param botWebserviceMessage
	 * @param jsonBodyString
	 * @throws JSONException
	 */

	public void createTextMessageInDynamicScenario(String payload, String senderId, List<MessagePayload> messagePayloadList, BotWebserviceMessage botWebserviceMessage, String jsonBodyString,
			String locale) {
		// string
		if (botWebserviceMessage.getOutType().getInOutTypeId() == 1) {
			getTextMessageIfResponseIsString(senderId, messagePayloadList, botWebserviceMessage, locale);
			// Object
		} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 2) {
			getTextMessageIfResponseIsObject(senderId, messagePayloadList, botWebserviceMessage, jsonBodyString, locale);
			// Array
		} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 3) {
			UserSelection userSelections = getUserSelectionsFromCache(senderId);
			int maxSize = userSelections.getAccountDeductionHistory();
			JSONArray jsonArray = new JSONArray(jsonBodyString);
			if (payload.equalsIgnoreCase(Constants.PAYLOAD_BALANCE_DEDUCTION) && jsonArray.length() > 0) {
				logger.debug(Constants.LOGGER_INFO_PREFIX + "Balance deduction transactions number " + maxSize);
				userSelections.setParentPayLoad("FOUND_ANSWER");
				updateUserSelectionsInCache(senderId, userSelections);
				List<JSONObject> lastTransactions = new ArrayList<>();
				for (int i = 0; i < maxSize; i++) {
					lastTransactions.add(jsonArray.getJSONObject(i));
				}
				jsonArray = new JSONArray(lastTransactions);
			}
			logger.debug(Constants.LOGGER_INFO_PREFIX + "Values list size " + jsonArray.length());
			getTextMessageIfResponseIsArray(senderId, messagePayloadList, botWebserviceMessage, jsonArray, locale);
		}
	}

	public CustomerProfile setLinkingInfoForCustomer(String senderId, String customerDial) {
		CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		CustomerProfile newCustomerProfile = new CustomerProfile();
		Date date = new Date();
		Timestamp timestamp = new Timestamp(date.getTime());
		newCustomerProfile.setCustomerLastSeen(timestamp);
		newCustomerProfile.setFirstInsertion(customerProfile.getFirstInsertion());
		newCustomerProfile.setLinkingDate(timestamp);
		newCustomerProfile.setLocale(customerProfile.getLocale().substring(0,2));
		newCustomerProfile.setFirstName(customerProfile.getFirstName());
		newCustomerProfile.setLastName(customerProfile.getLastName());
		if (customerDial.startsWith("0") && customerDial.length() > 10) {
			newCustomerProfile.setMsisdn(customerDial);
		} else {
			newCustomerProfile.setMsisdn(Constants.EMPTY_STRING);
		}
		newCustomerProfile.setSenderID(senderId);
		return chatBotService.saveCustomerProfile(newCustomerProfile);
	}
	
	public void setCustomerProfileLocal(CustomerProfile customerProfile, String locale) {
		CustomerProfile customerProfileForLocal = new CustomerProfile();
		Date date = new Date();
		Timestamp timestamp = new Timestamp(date.getTime());
		customerProfileForLocal.setCustomerLastSeen(timestamp);
		customerProfileForLocal.setFirstInsertion(customerProfile.getFirstInsertion());
		customerProfileForLocal.setLastGetProfileWSCall(customerProfile.getLastGetProfileWSCall());
		customerProfileForLocal.setLinkingDate(customerProfile.getLinkingDate());
		customerProfileForLocal.setLocale(locale);
		customerProfileForLocal.setMsisdn(customerProfile.getMsisdn());
		customerProfileForLocal.setSenderID(customerProfile.getSenderID());
		customerProfileForLocal.setFirstName(customerProfile.getFirstName());
		customerProfileForLocal.setLastName(customerProfile.getLastName());
		chatBotService.saveCustomerProfile(customerProfileForLocal);
	}

	public MessagePayload getProductsFromJsonByCategory(JSONArray arrayResponse, String category, String senderId, String locale) {
		MessagePayload fmsg = null;
		logger.debug(Constants.LOGGER_ELIGIBLE_PRODUCT + arrayResponse);
		for (int i = 0; i < arrayResponse.length(); i++) {
			try {
				JSONObject object = arrayResponse.getJSONObject(i);
				String categoryId = object.getJSONObject(Constants.JSON_KEY_CATEGORY_KEY).getString(Constants.JSON_KEY_CATEGORY_ID);
				if (category.startsWith(categoryId)) {
					JSONArray childEligibleProductModels = object.getJSONArray(Constants.JSON_KEY_CHILED_ELIGIBLE_PRODUCTS);
					if (category.equals(Constants.JSON_KEY_LEGO_HIGH) || category.equals(Constants.JSON_KEY_LEGO_LOW)) {
						fmsg = getProductsByCategoryIfLego(senderId, locale, childEligibleProductModels, category);
					} else if (categoryId.equalsIgnoreCase(Constants.JSON_KEY_LEGO)) {
						List<Element> elements = new ArrayList<>();
						for (int o = 0; o < childEligibleProductModels.length(); o++) {
							List<Button> buttons = new ArrayList<>();
							JSONObject childEligibleProduct = childEligibleProductModels.getJSONObject(o);
							JSONObject categoryObject = childEligibleProduct.getJSONObject(Constants.JSON_KEY_CATEGORY_KEY);
							String label = jsonUtilsService.getCategoryNameValue(categoryObject, locale);
							String payload = categoryObject.getString(Constants.JSON_KEY_CATEGORY_ID);
							String subTitle = Constants.SUBTITLE_VALUE;
							PostbackButton button = PostbackButton.create(Utils.getLabelForViewButton(locale), Constants.PREFIX_RATEPLAN_SUBSCRIPTION + payload);
							Button backButton = PostbackButton.create(Utils.getLabelForBackButton(locale), Constants.PAYLOAD_CHANGE_BUNDLE);
							buttons.add(button);
							buttons.add(backButton);
							Element element = Element.create(label, Optional.of(subTitle), empty(), empty(), Optional.of(buttons));
							elements.add(element);
						}
						GenericTemplate gTemplate = GenericTemplate.create(elements);
						fmsg = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));
					} else {
						JSONArray products = object.getJSONArray(Constants.JSON_KEY_FOR_PRODUCT);
						fmsg = getProductsByCategoryNotLego(senderId, locale, products);
					}

				}
			} catch (JSONException e) {
				logger.error(Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_EXCEPTION_MESSAGE + e);
				e.printStackTrace();
			}
		}
		return fmsg;

	}

	public MessagePayload getProductsByCategoryNotLego(String senderId, String locale, JSONArray products) {
		MessagePayload fmsg;
		List<Element> elements = new ArrayList<>();
		for (int j = 0; j < products.length(); j++) {
			List<Button> buttons = new ArrayList<>();
			JSONObject childObject = products.getJSONObject(j);
			String name = childObject.getString(Constants.JSON_KEY_FOR_NAME);
			String operationName = childObject.getJSONArray(Constants.JSON_KEY_OPERATION_RESPONSE).getJSONObject(0).getString("operationName");
			JSONArray relatedProducts = childObject.getJSONArray(Constants.JSON_KEY_FOR_RELATED_PRODUCT);
			String subtitle, title;
			subtitle = title = Constants.EMPTY_STRING;
			subtitle = jsonUtilsService.getDescriptionValue(childObject, locale);
			title = jsonUtilsService.getNameValue(childObject, locale);
			if (relatedProducts.length() > 0) {
				Button button = PostbackButton.create(Utils.subscribeButtonLabel(locale), Constants.PREFIX_RELATED_PRODUCTS + Constants.COMMA_CHAR + name + "," + operationName);
				buttons.add(button);
			} else {
				Button button = PostbackButton.create(Utils.subscribeButtonLabel(locale), name + Constants.COMMA_CHAR + operationName);
				buttons.add(button);
			}
			Button backButton = PostbackButton.create(Utils.getLabelForBackButton(locale), Constants.PAYLOAD_CHANGE_BUNDLE);
			buttons.add(backButton);
			Element element = Element.create(title, Optional.of(subtitle), empty(), empty(), Optional.of(buttons));
			elements.add(element);
		}
		GenericTemplate gTemplate = GenericTemplate.create(elements);
		fmsg = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));
		return fmsg;
	}

	public MessagePayload getProductsByCategoryIfLego(String senderId, String locale, JSONArray childEligibleProductModels, String categoryId) {
		ArrayList<Element> elements = new ArrayList<>();
		for (int j = 0; j < childEligibleProductModels.length(); j++) {
			JSONObject childObject = childEligibleProductModels.getJSONObject(j);
			String requiredCategory = childObject.getJSONObject(Constants.JSON_KEY_CATEGORY_KEY).getString(Constants.JSON_KEY_CATEGORY_ID);
			String name, title, subtitle, operationName;
			title = subtitle = operationName = Constants.EMPTY_STRING;
			if (requiredCategory.equals(categoryId)) {
				JSONArray products = childObject.getJSONArray(Constants.JSON_KEY_FOR_PRODUCT);
				for (int p = 0; p < products.length(); p++) {
					List<Button> buttons = new ArrayList<>();
					JSONObject product = products.getJSONObject(p);
					operationName = product.getJSONArray(Constants.JSON_KEY_OPERATION_RESPONSE).getJSONObject(0).getString("operationName");
					name = product.getString(Constants.JSON_KEY_FOR_NAME);
					Button button = PostbackButton.create(Utils.subscribeButtonLabel(locale), name + Constants.COMMA_CHAR + operationName);
					Button backButton = PostbackButton.create(Utils.getLabelForBackButton(locale), Constants.PAYLOAD_CHANGE_BUNDLE);
					title = jsonUtilsService.getNameValue(product, locale);
					// product.getString(Constants.JSON_KEY_NAME_EN);
					subtitle = jsonUtilsService.getDescriptionValue(product, locale);
					// product.getString(Constants.JSON_KEY_FOR_ENGLISH_DESCRIPTION);
					buttons.add(button);
					buttons.add(backButton);
					Element element = Element.create(title, Optional.of(subtitle), empty(), empty(), Optional.of(buttons));
					elements.add(element);
				}
			}
		}

		GenericTemplate gTemplate = GenericTemplate.create(elements);
		return MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));
	}

	public MessagePayload getRelatedProductFromJsonByBundleId(JSONArray arrayResponse, String productId, String senderId, String locale) {
		MessagePayload fmsg = null;
		for (int i = 0; i < arrayResponse.length(); i++) {
			JSONObject object = new JSONObject();
			try {
				object = arrayResponse.getJSONObject(i);
				if (!object.get(Constants.JSON_KEY_FOR_PRODUCT).equals(null)) {
					JSONArray products = object.getJSONArray(Constants.JSON_KEY_FOR_PRODUCT);
					for (int j = 0; j < products.length(); j++) {
						JSONObject childObject = products.getJSONObject(j);
						String name = childObject.getString(Constants.JSON_KEY_FOR_NAME);
						JSONArray relatedProducts = childObject.getJSONArray(Constants.JSON_KEY_FOR_RELATED_PRODUCT);
						if (name.equals(productId)) {
							List<Element> elements = new ArrayList<>();
							for (int r = 0; r < relatedProducts.length(); r++) {
								List<Button> buttons = new ArrayList<>();
								JSONObject relatedObject = relatedProducts.getJSONObject(r);
								String relatedProductName = relatedObject.getString(Constants.JSON_KEY_FOR_NAME);
								String title = jsonUtilsService.getNameValue(relatedObject, locale);
								// relatedObject.getString(Constants.JSON_KEY_NAME_EN);
								String subtitle = jsonUtilsService.getDescriptionValue(relatedObject, locale);
								// relatedObject.getString(Constants.JSON_KEY_FOR_ENGLISH_DESCRIPTION);
								Button button = PostbackButton.create(Utils.subscribeButtonLabel(locale), relatedProductName + Constants.COMMA_CHAR + Constants.PREFIX_RELATED_PRODUCTS_SUBSCRIPTION);
								buttons.add(button);
								Button backButton = PostbackButton.create(Utils.getLabelForBackButton(locale), Constants.PAYLOAD_CHANGE_BUNDLE);
								buttons.add(backButton);
								Element element = Element.create(title, Optional.of(subtitle), empty(), empty(), Optional.of(buttons));
								elements.add(element);
							}
							GenericTemplate gTemplate = GenericTemplate.create(elements);
							return MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));
						}
					}
				}
			} catch (JSONException e) {
				logger.error(Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_EXCEPTION_MESSAGE + e);
				e.printStackTrace();
			}
		}
		return fmsg;
	}

	public MessagePayload getExtraMobileInternetAddonsByCategory(JSONArray arrayResponse, String senderId, String locale, String addonId) {
		ArrayList<Element> elements = new ArrayList<>();
		String title = Constants.EMPTY_STRING;
		String subTitle = Constants.SUBTITLE_VALUE;
		String buttonLabel = Utils.subscribeButtonLabel(locale);
		for (int i = 0; i < arrayResponse.length(); i++) {
			try {
				List<Button> buttonsList = new ArrayList<>();
				JSONObject object = arrayResponse.getJSONObject(i);
				JSONArray retrievedCategoriesArray = object.getJSONArray(Constants.JSON_KEY_FOR_PRODUCT_CATEGORY);
				String id = object.getString("id");
				for (int o = 0; o < retrievedCategoriesArray.length(); o++) {
					if (retrievedCategoriesArray.getString(o).equalsIgnoreCase(addonId)) {
						title = jsonUtilsService.getNameValue(object, locale);
						String payLoad = Constants.PREFIX_ADDONSUBSCRIPE + id;
						PostbackButton bundleButton = PostbackButton.create(buttonLabel, payLoad);
						PostbackButton backButton = PostbackButton.create(Utils.getLabelForBackButton(locale), Constants.PAYLOAD_BUY_ADDONS_ROOT);
						buttonsList.add(bundleButton);
						buttonsList.add(backButton);
						Element element = Element.create(title, Optional.of(subTitle), empty(), empty(), Optional.of(buttonsList));
						elements.add(element);
					}
				}
			} catch (JSONException e) {
				logger.error(Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_EXCEPTION_MESSAGE + e);
				e.printStackTrace();
			}

		}
		GenericTemplate gTemplate = GenericTemplate.create(elements);
		return MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));

	}

	public MessagePayload getCategoryForMobileInternetAddons(JSONArray arrayResponse, String senderId, String locale) {
		logger.debug(Constants.LOGGER_INFO_PREFIX + Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_ELIGIBLE_PRODUCT + arrayResponse.toString());
		// Map<String, String> categoriesMap = new HashMap<>();
		Map<String, AddonCategory> categoriesMap = new HashMap<>();
		ArrayList<Element> elements = new ArrayList<>();
		String[] categoriesIDS = chatBotService.getEnabledCategoryConfigurationDaoById(2l).getEnglishCategories().split(Constants.COMMA_CHAR);
		String[] categoriesLabels = null;
		String[] categoriesDescription = null;
		if (locale.contains(Constants.LOCALE_AR)) {
			categoriesLabels = chatBotService.getEnabledCategoryConfigurationDaoById(2l).getCategoryLabel().getArabicText().split(Constants.COMMA_CHAR);
			// categoriesDescription =
			// chatBotService.getEnabledCategoryConfigurationDaoById(2l).getCategoryDescriptionId().getArabicText().split(Constants.COMMA_CHAR);
		} else {
			categoriesLabels = chatBotService.getEnabledCategoryConfigurationDaoById(2l).getCategoryLabel().getEnglishText().split(Constants.COMMA_CHAR);
			categoriesDescription = chatBotService.getEnabledCategoryConfigurationDaoById(2l).getCategoryDescriptionId().getEnglishText().split(Constants.COMMA_CHAR);
		}

		// String subtitle = Constants.SUBTITLE_VALUE;
		String buttonLabel = Constants.EMPTY_STRING;
		for (int i = 0; i < arrayResponse.length(); i++) {
			try {
				JSONObject object = arrayResponse.getJSONObject(i);
				JSONArray jsonCategoriesArray = object.getJSONArray(Constants.JSON_KEY_FOR_PRODUCT_CATEGORY);
				ArrayList<String> categoriesLabelsList = new ArrayList<>(Arrays.asList(categoriesLabels));
				ArrayList<String> categoriesDescriptionList = new ArrayList<>(Arrays.asList(categoriesDescription));
				ArrayList<String> enabledCategoriesList = new ArrayList<>(Arrays.asList(categoriesIDS));
				for (int o = 0; jsonCategoriesArray.length() > o; o++) {
					String category = jsonCategoriesArray.getString(o);
					if (enabledCategoriesList.contains(category)) {
						int index = enabledCategoriesList.indexOf(category);
						AddonCategory addonCategory = new AddonCategory();
						addonCategory.setTitle(jsonCategoriesArray.getString(o));
						addonCategory.setDescription(categoriesDescriptionList.get(o));
						// categoriesMap.put(categoriesLabelsList.get(index),
						// jsonCategoriesArray.getString(o));
						categoriesMap.put(categoriesLabelsList.get(index), addonCategory);
					}
				}
			} catch (JSONException e) {
				logger.error(Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_EXCEPTION_MESSAGE + e);
				e.printStackTrace();
			}

		}
		if (categoriesMap.size() > 0) {
			for (String key : categoriesMap.keySet()) {
				buttonLabel = Utils.getLabelForViewButton(locale);
				List<Button> buttonsList = new ArrayList<>();
				PostbackButton bundleButton = PostbackButton.create(buttonLabel, Constants.PREFIX_MOBILEINTERNET_ADDON + categoriesMap.get(key));
				buttonsList.add(bundleButton);
				Element element = Element.create(key, Optional.of(categoriesMap.get(key).getDescription()), empty(), empty(), Optional.of(buttonsList));
				elements.add(element);
			}
			GenericTemplate gTemplate = GenericTemplate.create(elements);
			return MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));

		} else {
			return createNotEligibleGenericMSG(senderId, locale, Constants.PAYLOAD_VIEW_CONNECT_DETAILS);
		}

	}

/*	public Map<String, String> getCalling(BotWebserviceMessage botWebserviceMessage, String senderId, String phoneNumber) {
		URI uri = createURI(botWebserviceMessage, senderId, chatBotService, phoneNumber);
		logger.debug(Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_DIAL_IS + phoneNumber + Constants.LOGGER_SERVICE_URL + uri);
		return callGetWebServiceByRestTemplate(uri);
	}*/
	
	public ResponseEntity<String> getCalling(BotWebserviceMessage botWebserviceMessage, String senderId, String phoneNumber){
		URI uri = createURI(botWebserviceMessage, senderId, chatBotService, phoneNumber);
		logger.debug(Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_DIAL_IS + phoneNumber + Constants.LOGGER_SERVICE_URL + uri);
		return callGetWebServiceByRestTemplate(uri);
	}

	/**
	 * Get WebService Calling
	 * 
	 * @param uri
	 */
/*	public Map<String, String> callGetWebServiceByRestTemplate(URI uri) {
		Map<String, String> responseMap = new HashMap<>();
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.APPLICATION_JSON_UTF8_VALUE);
		int statusId = 0;
		try {
			HttpEntity entity = new HttpEntity<>(headers);
			RestTemplate restTemplate = customRestTemplate();
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			statusId = response.getStatusCodeValue();
			responseMap.put(Constants.RESPONSE_STATUS_KEY, String.valueOf(statusId));
			responseMap.put(Constants.RESPONSE_KEY, response.getBody());
		} catch (Exception e) {
			responseMap.put(Constants.RESPONSE_STATUS_KEY, String.valueOf(statusId));
			logger.error(Constants.LOGGER_EXCEPTION_MESSAGE + e);
			e.printStackTrace();
		}
		return responseMap;
	}*/
	
	
	public ResponseEntity<String> callGetWebServiceByRestTemplate(URI uri) {
		ResponseEntity<String> response = new ResponseEntity<>(HttpStatus.NO_CONTENT);
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.APPLICATION_JSON_UTF8_VALUE);
		try {
			HttpEntity entity = new HttpEntity<>(headers);
			RestTemplate restTemplate = customRestTemplate();
			response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
		} catch (Exception e) {
			logger.error(Constants.LOGGER_EXCEPTION_MESSAGE + e);
			e.printStackTrace();
		}
		return response;
	}

	private RestTemplate customRestTemplate() {
		HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
		String timeOutValue = chatBotService.getBotConfigurationByKey(Constants.REQUEST_TIME_OUT_VALUE) == null ? "10000"
				: chatBotService.getBotConfigurationByKey(Constants.REQUEST_TIME_OUT_VALUE).getValue();
		int inttimeOut = Integer.parseInt(timeOutValue);
		httpRequestFactory.setConnectionRequestTimeout(inttimeOut);
		httpRequestFactory.setConnectTimeout(inttimeOut);
		httpRequestFactory.setReadTimeout(inttimeOut);
		return new RestTemplate(httpRequestFactory);
	}

	public MessagePayload changeLanguageResponse(String locale, String senderId) {
		String text = Constants.EMPTY_STRING;
		if (locale.contains(Constants.LOCALE_AR)) {
			text = Constants.RESPOSE_MSG_CHANGE_LOCALE_AR;
		} else {
			text = Constants.RESPOSE_MSG_CHANGE_LOCALE_EN;
		}
		TextMessage textMSG = TextMessage.create(text);
		return MessagePayload.create(senderId, MessagingType.RESPONSE, textMSG);
	}

	// Create Generic Not Eligible Generic Template
	public MessagePayload createNotEligibleGenericMSG(String senderId, String locale, String payload) {
		List<Element> elementsList = new ArrayList<>();
		List<Button> buttons = new ArrayList<>();
		String title;
		String subtitle;
		String buttonLabel = Utils.getLabelForBackButton(locale);
		if (locale.contains(Constants.LOCALE_AR)) {
			title = Constants.NOTELIGIBLE_ELEMENT_TITLE_AR;
			subtitle = Constants.NOTELIGIBLE_ELEMENT_SUBTITLE_AR;

		} else {
			title = Constants.NOTELIGIBLE_ELEMENT_TITLE_EN;
			subtitle = Constants.NOTELIGIBLE_ELEMENT_SUBTITLE_EN;

		}
		Button button = PostbackButton.create(buttonLabel, payload);
		buttons.add(button);
		BotConfiguration warningImageRaw = chatBotService.getBotConfigurationByKey(Constants.CONFIGURATION_TABLE_WARNING_IMAGE_URL);

		URL url = Utils.createUrl(warningImageRaw.getValue());
		Element element = Element.create(title, Optional.of(subtitle), Optional.of(url), empty(), Optional.of(buttons));
		elementsList.add(element);
		GenericTemplate gTemplate = GenericTemplate.create(elementsList);
		return MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));
	}

	public MessagePayload createErrorTemplateForNoConsumptionDetails(String senderId, String locale, String payload) {
		List<Element> elementsList = new ArrayList<>();
		List<Button> buttons = new ArrayList<>();
		String title;
		String subtitle;
		String buttonLabel = Utils.getLabelForBackButton(locale);
		if (locale.contains(Constants.LOCALE_AR)) {
			title = Constants.NOTELIGIBLE_ELEMENT_TITLE_AR;
			subtitle = Constants.NOCONSUMPTION_ELEMENT_SUBTITLE_EN;

		} else {
			title = Constants.NOTELIGIBLE_ELEMENT_TITLE_EN;
			subtitle = Constants.NOCONSUMPTION_ELEMENT_SUBTITLE_EN;

		}
		Button button = PostbackButton.create(buttonLabel, payload);
		buttons.add(button);
		BotConfiguration warningImageRaw = chatBotService.getBotConfigurationByKey(Constants.CONFIGURATION_TABLE_WARNING_IMAGE_URL);
		URL url = Utils.createUrl(warningImageRaw.getValue());
		Element element = Element.create(title, Optional.of(subtitle), Optional.of(url), empty(), Optional.of(buttons));
		elementsList.add(element);
		GenericTemplate gTemplate = GenericTemplate.create(elementsList);
		return MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));
	}

	/**
	 * @param jsonParam
	 * @param paramValuesList
	 * @param paramNames
	 */
	public JSONObject setRequestBodyValueForPostCalling(ArrayList<String> paramValuesList, ArrayList<String> paramNames) {
		JSONObject jsonParam = new JSONObject();
		for (int p = 0; p < paramNames.size(); p++) {
			jsonParam.put(paramNames.get(p), paramValuesList.get(p));
		}
		return jsonParam;
	}

	public void interactionLoggingUpdateResponseTime(InteractionLogging interactionLogging) {
		InteractionLogging updatedInteractionLogging = new InteractionLogging();
		updatedInteractionLogging.setBotInteraction(interactionLogging.getBotInteraction());
		updatedInteractionLogging.setAdditionalParam(interactionLogging.getAdditionalParam());
		updatedInteractionLogging.setCustomerProfile(interactionLogging.getCustomerProfile());
		updatedInteractionLogging.setInteractionCallingDate(interactionLogging.getInteractionCallingDate());
		updatedInteractionLogging.setInteractionLoggingId(interactionLogging.getInteractionLoggingId());
		updatedInteractionLogging.setBotResponseDate(new Timestamp(new Date().getTime()));
		chatBotService.saveInteractionLogging(updatedInteractionLogging);

	}

	/**
	 * user locale setting
	 * 
	 * @param customerProfile
	 *            used for update user locale in database
	 * @param messenger
	 * @param senderId
	 * @param payload
	 */
	public ArrayList<MessagePayload> userlocaleSetting(CustomerProfile customerProfile, String senderId, String payload) {
		ArrayList<MessagePayload> messagePayloadList = new ArrayList<>();
		if (payload.equalsIgnoreCase(Constants.LOCALE_EN)) {
			setCustomerProfileLocal(customerProfile, Constants.LOCALE_EN);
			messagePayloadList.add(changeLanguageResponse(Constants.LOCALE_EN, senderId));
			// interactionHandlingService.sendMultipleMessages(messagePayloadList, senderId,
			// messenger, null);

		} else if (payload.equalsIgnoreCase(Constants.LOCALE_AR)) {
			setCustomerProfileLocal(customerProfile, Constants.LOCALE_AR);
			messagePayloadList.add(changeLanguageResponse(Constants.LOCALE_AR, senderId));
			// interactionHandlingService.sendMultipleMessages(messagePayloadList, senderId,
			// messenger, null);

		}
		return messagePayloadList;
	}

	/**
	 * DashBoard encryption for channel param
	 * 
	 */
	public String encryptChannelParam(String url)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
		BotConfiguration encryptionRaw = chatBotService.getBotConfigurationByKey(Constants.CHANNEL_ENCRYPTION_KEY);
		String key = encryptionRaw.getValue();
		byte[] keyBytes = key.getBytes();
		SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
		Cipher ecipher = Cipher.getInstance("AES");
		ecipher.init(Cipher.ENCRYPT_MODE, secretKey);
		byte[] utf8 = url.getBytes("UTF8");
		byte[] enc = ecipher.doFinal(utf8);
		String encryptedParams = new sun.misc.BASE64Encoder().encode(enc);
		byte[] encryptedBytes = encryptedParams.getBytes();
		StringBuilder strbuf = new StringBuilder(encryptedBytes.length * 2);
		for (int i = 0; i < encryptedBytes.length; i++) {
			if (((int) encryptedBytes[i] & 0xff) < 0x10) {
				strbuf.append("0");
			}
			strbuf.append(Long.toString((int) encryptedBytes[i] & 0xff, 16));
		}
		String toBeSentParams = strbuf.toString();
		return toBeSentParams;
	}

	// DashBoard encryption Utils
	public String encryptDPIParam(String url) throws Exception {
		BotConfiguration encryptionRaw = chatBotService.getBotConfigurationByKey(Constants.DPI_ENCRYPTION_KEY);
		String encryptionKey = encryptionRaw.getValue();
		logger.debug(Constants.LOGGER_INFO_PREFIX + "Encryption Key " + encryptionKey);
		byte[] decryptionKey = new byte[encryptionKey.length()];
		for (int i = 0; i < encryptionKey.length(); i++) {
			decryptionKey[i] = (byte) encryptionKey.charAt(i);
		}
		Cipher c = Cipher.getInstance("AES");
		SecretKeySpec k = new SecretKeySpec(decryptionKey, "AES");
		c.init(Cipher.ENCRYPT_MODE, k);
		byte[] utf8 = url.getBytes("UTF8");
		byte[] enc = c.doFinal(utf8);
		return URLEncoder.encode(DatatypeConverter.printBase64Binary(enc), "UTF-8");
	}

	/*
	 * public static String encryptDPIParam(String encryptedString) throws Exception
	 * { byte[] decryptionKey = new byte[] { '0', '1', '2', '3', '4', '5', '6', '7',
	 * '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' }; Cipher c =
	 * Cipher.getInstance("AES"); SecretKeySpec k = new SecretKeySpec(decryptionKey,
	 * "AES"); c.init(Cipher.ENCRYPT_MODE, k); byte[] utf8 =
	 * encryptedString.getBytes("UTF8"); byte[] enc = c.doFinal(utf8); return
	 * URLEncoder.encode(DatatypeConverter.printBase64Binary(enc),"UTF-8"); }
	 */
	/**
	 * URI creation for Get Webservice Calling
	 * 
	 */
	public URI createURI(BotWebserviceMessage botWebserviceMessage, String senderId, ChatBotService chatBotService, String phoneNumber) {
		URI uri = null;
		try {
			CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
			String dialNumber = customerProfile.getMsisdn();
			String realParameter = paramAndParamChannelEncryption(dialNumber);
			if (botWebserviceMessage.getWsUrl().contains("allowedMoves")) {
				String[] params = botWebserviceMessage.getListParamName().split(Constants.COMMA_CHAR);
				String requiredParam = "&" + params[0] + "=&" + params[1] + "=";
				uri = new URI(botWebserviceMessage.getWsUrl() + Constants.URL_DIAL_PARAM_KEY + realParameter + requiredParam);
			} else {
				uri = new URI(botWebserviceMessage.getWsUrl() + Constants.URL_DIAL_PARAM_KEY + realParameter);
			}

		} catch (URISyntaxException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException e1) {
			logger.error(Constants.LOGGER_DIAL_IS + phoneNumber + Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_EXCEPTION_MESSAGE + e1);
			e1.printStackTrace();
		} catch (Exception e) {
			logger.error(Constants.LOGGER_DIAL_IS + phoneNumber + Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_EXCEPTION_MESSAGE + e);
			e.printStackTrace();
		}
		return uri;
	}

	/**
	 * @param senderId
	 * @param chatBotService
	 * @return
	 * @throws Exception
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws UnsupportedEncodingException
	 */
	public String paramAndParamChannelEncryption(String dialNumber) throws Exception {
		long currentTimeInSecond = System.currentTimeMillis() / 1000;
		String param = encryptDPIParam(Constants.URL_TIME_KEY + currentTimeInSecond + Constants.URL_PARAM_MSISDN_KEY + dialNumber + Constants.URL_ETISALAT_VALUE);
		String paramChannel = encryptChannelParam(Constants.URL_CHANNEL_KEY + Constants.CHANEL_PARAM);
		return "param:" + param + "&paramChannel:" + paramChannel;

	}
	
	
	public String paramAndParamChannelEncryptionForWebView(String dialNumber) throws Exception {
		long currentTimeInSecond = System.currentTimeMillis() / 1000;
		String param = encryptDPIParam(Constants.URL_TIME_KEY + currentTimeInSecond + Constants.URL_PARAM_MSISDN_KEY + dialNumber + Constants.URL_ETISALAT_VALUE);
		String paramChannel = encryptChannelParam(Constants.URL_CHANNEL_KEY + Constants.CHANEL_PARAM);
		return "param=" + param + "&paramChannel:" + paramChannel;
	}

}
