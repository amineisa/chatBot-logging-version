package com.chatbot.services;

import static java.util.Optional.empty;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.chatbot.entity.BotButton;
import com.chatbot.entity.BotButtonTemplateMSG;
import com.chatbot.entity.BotConfiguration;
import com.chatbot.entity.BotGTemplateMessage;
import com.chatbot.entity.BotInteractionMessage;
import com.chatbot.entity.BotQuickReplyMessage;
import com.chatbot.entity.BotTemplateElement;
import com.chatbot.entity.BotTextMessage;
import com.chatbot.entity.BotTextResponseMapping;
import com.chatbot.entity.BotWebserviceMessage;
import com.chatbot.entity.CustomerProfile;
import com.chatbot.util.Constants;
import com.chatbot.util.Utils;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.MessagingType;
import com.github.messenger4j.send.message.TemplateMessage;
import com.github.messenger4j.send.message.TextMessage;
import com.github.messenger4j.send.message.quickreply.QuickReply;
import com.github.messenger4j.send.message.quickreply.TextQuickReply;
import com.github.messenger4j.send.message.template.ButtonTemplate;
import com.github.messenger4j.send.message.template.GenericTemplate;
import com.github.messenger4j.send.message.template.Template;
import com.github.messenger4j.send.message.template.button.Button;
import com.github.messenger4j.send.message.template.button.LogInButton;
import com.github.messenger4j.send.message.template.button.LogOutButton;
import com.github.messenger4j.send.message.template.button.PostbackButton;
import com.github.messenger4j.send.message.template.button.UrlButton;
import com.github.messenger4j.send.message.template.common.Element;

@Service
public class UtilService {

	@Autowired
	private JsonUtilsService jsonUtilsService;
	
	@Autowired
	private ChatBotService chatBotService;

	private static final Logger logger = LoggerFactory.getLogger(UtilService.class);

	// Method to get Path Details
	public String[] getPaths(String path) {
		if(path != null) {
		String[] paths = new String[0];
		if (path.contains(Constants.COMMA_CHAR)) {
			paths = path.split(Constants.COMMA_CHAR);
		} else if (path.length() > 1 && !path.contains(Constants.COMMA_CHAR)) {
			paths = new String[] { path };
		}
		return paths;
		}else {
			return new String[0];
		}
	}

	// Method to get keys
	public String[] getKeys(String key) {
		if(key != null) {
		String[] keys = new String[0];
		if (key.contains(Constants.COMMA_CHAR)) {
			keys = key.split(Constants.COMMA_CHAR);
		} else {
			keys = new String[] { key };
		}
		return keys;
		}else {
			return new String [0];
		}
	}

	// Switcher for JSONObject
	public Map<String, ArrayList<String>> switchToObjectMode(JSONObject jsonResponse, String[] paths, String[] keys, String msg, String locale) {
		Map<String, ArrayList<String>> mapValues = new HashMap<String, ArrayList<String>>();
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
		// PostBack
		if (botButton.getButtonType().getId() == Utils.ButtonTypeEnum.POSTBACK.getValue()) {
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
			// URl
		} else if (botButton.getButtonType().getId() == Utils.ButtonTypeEnum.URL.getValue()) {
			URL url = Utils.createUrl(botButton.getButtonUrl());
			if (locale.contains(Constants.ARABIC_LOCAL)) {
				try {
					if (botButton.getBotText().getArabicText().equals(Constants.BUTTON_LABEL_CHARGE_AR)) {
						String par = Utils.encryptDPIParam(Constants.URL_USER_AND_TIME_KEY + dialNumber + Constants.URL_PAY_BILL_AND_RECHARGE_CHANEL);
						String stringUrl = url + Constants.URL_LOCALE_AR + par;
						URL newUrl = Utils.createUrl(stringUrl);
						return UrlButton.create(Utils.getTextValueForButtonLabel(locale, botButton), newUrl);
					} else if (botButton.getBotText().getArabicText().equals(Constants.BUTTON_LABEL_CHARGE_EN)) {
						String par = Utils.encryptDPIParam(Constants.URL_USER_AND_TIME_KEY + dialNumber + Constants.URL_PAY_BILL_AND_RECHARGE_CHANEL);
						String stringUrl = url + Constants.URL_LOCALE_EN + par;
						URL newUrl = Utils.createUrl(stringUrl);
						return UrlButton.create(Utils.getTextValueForButtonLabel(locale, botButton), newUrl);
					}
				} catch (Exception e) {
					logger.error(Constants.LOGGER_EXCEPTION_MESSAGE + e);
					e.printStackTrace();
				}
			} else {
				try {
					if (botButton.getBotText().getEnglishText().equalsIgnoreCase("ادفع فاتورتك الان ")) {
						String par = Utils.encryptDPIParam(Constants.URL_USER_AND_TIME_KEY + dialNumber + Constants.URL_PAY_BILL_AND_RECHARGE_CHANEL);
						String stringUrl = url + Constants.URL_LOCALE_AR + par;
						URL newUrl = Utils.createUrl(stringUrl);
						return UrlButton.create(Utils.getTextValueForButtonLabel(locale, botButton), newUrl);

					} else if (botButton.getBotText().getEnglishText().equalsIgnoreCase("Pay Now")) {
						String par = Utils.encryptDPIParam(Constants.URL_USER_AND_TIME_KEY + dialNumber + Constants.URL_PAY_BILL_AND_RECHARGE_CHANEL);
						String stringUrl = url+ Constants.URL_LOCALE_EN + par;
						URL newUrl = Utils.createUrl(stringUrl);
						return UrlButton.create(Utils.getTextValueForButtonLabel(locale, botButton), newUrl);
					}
				} catch (Exception e) {
					logger.error(Constants.LOGGER_EXCEPTION_MESSAGE + e);
					e.printStackTrace();
				}
			}
			return UrlButton.create(Utils.getTextValueForButtonLabel(locale, botButton), url);
			// Login
		} else if (botButton.getButtonType().getId() == Utils.ButtonTypeEnum.LOGIN.getValue()) {
			URL url = Utils.createUrl(botButton.getButtonUrl());
			return LogInButton.create(url);
			// Logout
		} else if (botButton.getButtonType().getId() == Utils.ButtonTypeEnum.LOGOUT.getValue()) {
			return LogOutButton.create();
		}
		return null;
	}

	/**
	 * @param messageId
	 * @param chatBotService
	 * @return
	 */
	public Template createGenericTemplate(Long messageId, ChatBotService chatBotService, String userlocale, BotWebserviceMessage botWebserviceMessage, JSONObject jsonObject, String dialNumber,
			List<String> consumptionNames, String payload) {
		BotGTemplateMessage botGTemplateMessage = chatBotService.findGTemplateMessageByMessageId(messageId);
		List<BotTemplateElement> botTemplateElementList = chatBotService.findTemplateElementsByGTMsgId(botGTemplateMessage.getGTMsgId());
		List<Element> elements = new ArrayList<>();
		for (BotTemplateElement botTemplateElement : botTemplateElementList) {
			List<BotButton> elementButtonList = chatBotService.findButtonsByTemplateElementId(botTemplateElement.getElementId());
			List<Button> buttonsList = new ArrayList<>();
			for (BotButton botButton : elementButtonList) {
				Button button = createButton(botButton, userlocale, new JSONObject(), dialNumber);
				buttonsList.add(button);
			}

			String elemnetImageUrl = Constants.EMPTY_STRING;
			Element element = null;
			elemnetImageUrl = botTemplateElement.getImageUrl();
			String title = getTitletBotTemplateElement(botTemplateElement, userlocale);
			String subtitle = getSubTitletBotTemplateElement(botTemplateElement, userlocale);
			String msg = Utils.informUserThatHeDoesnotSubscribeAtAnyMIBundle(userlocale);
			if (subtitle.contains("?") && botWebserviceMessage != null) {
				Long wsId = botWebserviceMessage.getWsMsgId();
				BotTextResponseMapping botTextResponseMapping = chatBotService.findTextResponseMappingByWsId(wsId).get(0);
				String[] keys = getKeysString(botTextResponseMapping, userlocale).split(Constants.COMMA_CHAR);
				String[] paths = getPaths(botTextResponseMapping.getCommonPath());
				Map<String, ArrayList<String>> mapValues = switchToObjectMode(jsonObject, paths, keys, subtitle, userlocale);
				ArrayList<String> values = mapValues.get(Constants.RESPONSE_MAP_MESSAGE_KEY);
				ArrayList<String> titles = mapValues.get(Constants.RESPONSE_MAP_TITLE_KEY);
				ArrayList<String> percentageList = mapValues.get(Constants.RESPONSE_PERCENTAGE_KEY);
				if (values == null || values.isEmpty()) {
					createNoConsumptionTemplate(userlocale, elements, botTemplateElement, buttonsList, msg);
				} else {
					for (int i = 0; i < values.size(); i++) {
						 if(payload.equalsIgnoreCase(Constants.PAYLOAD_RATEPLAN_ADDONS_CONSUMPTION)) {
							 title = consumptionNames.get(i);
						 }else {
							 title = titles.get(i);
						}
						if ((title.equalsIgnoreCase(Constants.MOBILE_INTERNET_CONSUMPTION_NAME_EN) || title.equalsIgnoreCase(Constants.MOBILE_INTERNET_CONSUMPTION_NAME_AR))
								&& !payload.equalsIgnoreCase("rateplan details")) {
							title = consumptionNames.size() > i ? consumptionNames.get(i) : title;
						}
						String perc = String.valueOf(percentageList.get(i));
						if (perc.contains(Constants.IS_KEY_HAS_DOT)) {
							perc = perc.substring(0, perc.indexOf('.'));
						}
						elemnetImageUrl = botTemplateElement.getImageUrl() + perc + ".png?version=1";
						element = createElement(buttonsList, elemnetImageUrl, title, values.get(i));
						elements.add(element);
					}
				}
			} else {
				if (elemnetImageUrl.length() > 1) {
					URL imageUrl = Utils.createUrl(elemnetImageUrl);
					element = Element.create(title, Optional.of(subtitle), Optional.of(imageUrl), empty(), Optional.of(buttonsList));
				} else {
					element = Element.create(title, Optional.of(botTemplateElement.getSubTitle().getEnglishText()), empty(), empty(), Optional.of(buttonsList));
				}
				elements.add(element);
			}
		}
		return GenericTemplate.create(elements);
	}

	/**
	 * @param userlocale
	 * @param elements
	 * @param botTemplateElement
	 * @param buttonsList
	 * @param msg
	 */
	public void createNoConsumptionTemplate(String userlocale, List<Element> elements, BotTemplateElement botTemplateElement, List<Button> buttonsList, String msg) {
		String elemnetImageUrl;
		Element element;
		String errorTitle = Constants.EMPTY_STRING;
		elemnetImageUrl = botTemplateElement.getImageUrl() + Constants.URL_WARNING_IMAGE;
		if (userlocale.contains(Constants.ARABIC_LOCAL)) {
			errorTitle = Constants.NOTELIGIBLE_ELEMENT_TITLE_AR;
		} else {
			errorTitle = Constants.NOTELIGIBLE_ELEMENT_TITLE_EN;
		}
		element = createElement(buttonsList, elemnetImageUrl, errorTitle, msg);
		elements.add(element);
	}

	/**
	 * @param buttonsList
	 * @param elemnetImageUrl
	 * @param title
	 * @param msg
	 * @return
	 */
	public Element createElement(List<Button> buttonsList, String elemnetImageUrl, String title, String msg) {
		Element element;
		if (elemnetImageUrl.length() > 1) {
			URL imageUrl = Utils.createUrl(elemnetImageUrl);
			element = Element.create(title, Optional.of(msg), Optional.of(imageUrl), empty(), Optional.of(buttonsList));
		} else {
			element = Element.create(title, Optional.of(msg), empty(), empty(), Optional.of(buttonsList));
		}
		return element;
	}

	public String getTitletBotTemplateElement(BotTemplateElement botTemplateElement, String local) {
		String title = Constants.EMPTY_STRING;
		if (local.equalsIgnoreCase(Constants.ARABIC_LOCAL)) {
			title = botTemplateElement.getTitle().getArabicText();
		} else {
			title = botTemplateElement.getTitle().getEnglishText();
		}
		return title;
	}

	public String getSubTitletBotTemplateElement(BotTemplateElement botTemplateElement, String local) {
		String title = Constants.EMPTY_STRING;
		if (local.equalsIgnoreCase(Constants.ARABIC_LOCAL)) {
			title = botTemplateElement.getSubTitle().getArabicText();
		} else {
			title = botTemplateElement.getSubTitle().getEnglishText();
		}
		return title;
	}

	/**
	 * @param botInteractionMessage
	 * 
	 * @param chatBotService
	 * @return
	 */
	public ButtonTemplate createButtonTemplateInScenario(BotInteractionMessage botInteractionMessage, ChatBotService chatBotService, String local, String dialNumber) {
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

	public String getTextForButtonTemplate(String local, BotButtonTemplateMSG botButtonTemplateMSG) {
		String text = Constants.EMPTY_STRING;
		if(local==null) {
			local ="en_Us";
		}
		if (local.contains(Constants.ARABIC_LOCAL)) {
			text = botButtonTemplateMSG.getBotText().getArabicText();
		} else {
			text = botButtonTemplateMSG.getBotText().getEnglishText();
		}
		return text;
	}

	public List<QuickReply> createQuickReply(ChatBotService chatBotService, Long messageId, String local) {
		BotQuickReplyMessage botQuickReplyMessage = chatBotService.findQuickReplyMessageByMessageId(messageId);
		List<QuickReply> quickReplies = new ArrayList<>();
		List<BotButton> quickReplyButtonList = chatBotService.findButtonsByQuickReplyMessageId(botQuickReplyMessage.getQuickMsgId());
		QuickReply quickReply = null;
		for (BotButton botButton : quickReplyButtonList) {
			String label = Utils.getTextValueForButtonLabel(local, botButton);
			if (botButton.getButtonImageUrl() != null) {
				if (botButton.getButtonImageUrl().length() > 0) {
					URL url = Utils.createUrl(botButton.getButtonImageUrl());
					quickReply = TextQuickReply.create(label, botButton.getButtonPayload(), Optional.of(url));
				} else {
					quickReply = TextQuickReply.create(label, botButton.getButtonPayload(), empty());
				}
			} else {
				quickReply = TextQuickReply.create(label, botButton.getButtonPayload());
			}
			quickReplies.add(quickReply);
		}
		return quickReplies;
	}

	/**
	 * @param senderId
	 * @param messagePayloadList
	 * @param botWebserviceMessage
	 * @param jsonBodyString
	 * @throws JSONException
	 */

	public void getTextMessageIfResponseIsArray(String senderId, List<MessagePayload> messagePayloadList, BotWebserviceMessage botWebserviceMessage, String jsonBodyString,
			ChatBotService chatBotService, String local) {
		MessagePayload messagePayload;
		List<BotTextResponseMapping> botTextResponseMappings = chatBotService.findTextResponseMappingByWsId(botWebserviceMessage.getWsMsgId());
		JSONArray rootArray = new JSONArray(jsonBodyString);
		for (BotTextResponseMapping botTextResponseMapping : botTextResponseMappings) {
			String msg = getTextForBotTextResponseMapping(local, botTextResponseMapping);
			String path = botTextResponseMapping.getCommonPath();
			String[] paths = getPaths(path);
			String keys = getKeysString(botTextResponseMapping, local);

			String[] keysArray = getKeys(keys);
			ArrayList<String> values = switchToArrayMode(rootArray, paths, keysArray, msg, local).get(msg);
			for (String val : values) {
				messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(val));
				messagePayloadList.add(messagePayload);
			}
		}
	}

	public String getTextForBotTextResponseMapping(String local, BotTextResponseMapping botTextResponseMapping) {
		String text = Constants.EMPTY_STRING;
		if (local.equalsIgnoreCase(Constants.ARABIC_LOCAL)) {
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

	public void getTextMessageIfResponseIsObject(String senderId, List<MessagePayload> messagePayloadList, BotWebserviceMessage botWebserviceMessage, String jsonBodyString,
			ChatBotService chatBotService, String local) {
		MessagePayload messagePayload;
		List<BotTextResponseMapping> botTextResponseMappings = chatBotService.findTextResponseMappingByWsId(botWebserviceMessage.getWsMsgId());
		JSONObject rootObject = new JSONObject(jsonBodyString);
		for (BotTextResponseMapping botTextResponseMapping : botTextResponseMappings) {
			String msg = getTextForBotTextResponseMapping(local, botTextResponseMapping);
			String path = botTextResponseMapping.getCommonPath();
			String[] paths = getPaths(path);
			String keys = Constants.EMPTY_STRING;
			if (local.equalsIgnoreCase(Constants.ARABIC_LOCAL)) {
				keys = botTextResponseMapping.getArParams();
			} else {
				keys = botTextResponseMapping.getEnParams();
			}
			String[] keysArray = getKeys(keys);
			ArrayList<String> values = switchToObjectMode(rootObject, paths, keysArray, msg, local).get(Constants.RESPONSE_MAP_MESSAGE_KEY);
			for (String val : values) {
				messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(val));
				messagePayloadList.add(messagePayload);
			}
		}
	}

	public MessagePayload getBundleCategories(JSONArray arrayResponse, String senderId, ChatBotService chatBotService, String locale, String dial) {
		ArrayList<Element> elements = new ArrayList<>();
		ArrayList<String> categories = new ArrayList<>();
		String[] categoriesArray = chatBotService.getEnabledCategoryConfigurationDaoById(1l).getEnglishCategories().split(Constants.COMMA_CHAR);
		categories = new ArrayList<>(Arrays.asList(categoriesArray));
		for (int i = 0; i < arrayResponse.length(); i++) {
			try {
				JSONObject object = arrayResponse.getJSONObject(i);
				JSONObject categoryObject = object.getJSONObject(Constants.JSON_KEY_CATEGORY_KEY);
				String title, subTitle, buttonLabel;
				title = subTitle = buttonLabel = Constants.EMPTY_STRING;
				String name = Utils.getLabelForViewButton(locale);
				if (categories.contains(categoryObject.get(Constants.JSON_KEY_CATEGORY_ID))) {
					if (locale.equalsIgnoreCase(Constants.ARABIC_LOCAL)) {
						buttonLabel = categoryObject.getString(Constants.JSON_KEY_CATEGORY_NAME_AR);
					} else {
						buttonLabel = categoryObject.getString(Constants.JSON_KEY_CATEGORY_NAME_EN);
					}
					String payLoad = Constants.PREFIX_RATEPLAN_SUBSCRIPTION + categoryObject.getString(Constants.JSON_KEY_CATEGORY_ID);

					title = buttonLabel;
					subTitle = Constants.SUBTITLE_VALUE;
					List<Button> buttonsList = new ArrayList<>();
					PostbackButton bundleButton = PostbackButton.create(name, payLoad);
					PostbackButton backButton = PostbackButton.create(Utils.getLabelForBackButton(locale), Constants.PAYLOAD_CONSUMPTION);
					buttonsList.add(bundleButton);
					buttonsList.add(backButton);
					Element element = Element.create(title, Optional.of(subTitle), empty(), empty(), Optional.of(buttonsList));
					elements.add(element);
				}

			} catch (JSONException e) {
				logger.error(Constants.LOGGER_EXCEPTION_MESSAGE + e);
				e.printStackTrace();
			}
		}
		if (!elements.isEmpty()) {
			GenericTemplate gTemplate = GenericTemplate.create(elements);
			return MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));
		} else {
			List<Button> buttonsList = new ArrayList<>();
			GenericTemplate gTemplate = createGenericTemplateForNotEligiblBundleDials(locale, buttonsList, elements, dial);

			return MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));
		}
	}

	public void getTextMessageIfResponseIsString(String senderId, List<MessagePayload> messagePayloadList, BotWebserviceMessage botWebserviceMessage, String local) {
		String text = Constants.EMPTY_STRING;
		if (local.equalsIgnoreCase(Constants.ARABIC_LOCAL)) {
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
		if (local.equalsIgnoreCase(Constants.ARABIC_LOCAL)) {
			keys = botTextResponseMapping.getArParams();
		} else {
			keys = botTextResponseMapping.getEnParams();
		}
		return keys;
	}

	/**
	 * @param payload
	 * @param senderId
	 * @param userFirstName
	 * @param botInteraction
	 * @param messagePayloadList
	 * @param botInteractionMessage
	 * @param messageTypeId
	 * @param messageId
	 */

	public MessagePayload responseInCaseStaticScenario(String payload, String senderId, String userFirstName, BotInteractionMessage botInteractionMessage, Long messageTypeId, Long messageId,
			ChatBotService chatBotService, String locale, String userDial) {
		String text;
		MessagePayload messagePayload = null;
		logger.debug(Constants.LOGGER_DIAL_IS + userDial + " Method Name is Static Scenario Message is " + botInteractionMessage.toString());
		// text message
		if (messageTypeId == Utils.MessageTypeEnum.TEXTMESSAGE.getValue()) {
			BotTextMessage botTextMessage = chatBotService.findTextMessageByMessageId(messageId);
			text = getTextValueForBotTextMessage(botTextMessage, locale);
			text = text + " " + userFirstName;
			messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(text));

		}
		// quick reply
		else if (messageTypeId == Utils.MessageTypeEnum.QUICKREPLYMESSAGE.getValue()) {
			BotQuickReplyMessage botQuickReplyMessage = chatBotService.findQuickReplyMessageByMessageId(messageId);
			text = getTextForQuickReply(locale, botQuickReplyMessage, userDial);
			List<QuickReply> quickReplies = createQuickReply(chatBotService, messageId, locale);
			Optional<List<QuickReply>> quickRepliesOp = Optional.of(quickReplies);
			messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(text, quickRepliesOp, empty()));
		}
		// generic template
		else if (messageTypeId == Utils.MessageTypeEnum.GENERICTEMPLATEMESSAGE.getValue()) {
			Template template = createGenericTemplate(messageId, chatBotService, locale, new BotWebserviceMessage(), new JSONObject(), userDial, new ArrayList<String>(), payload);
			messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(template));
			// ButtonTemplate
		} else if (messageTypeId == Utils.MessageTypeEnum.ButtonTemplate.getValue()) {
			ButtonTemplate buttonTemplate = createButtonTemplateInScenario(botInteractionMessage, chatBotService, locale, userDial);
			messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(buttonTemplate));
		}
		return messagePayload;
	}

	// get text for BotTextMessage according to local
	public String getTextValueForBotTextMessage(BotTextMessage botTextMessage, String local) {
		String text = Constants.EMPTY_STRING;
		if (local.equalsIgnoreCase(Constants.ARABIC_LOCAL)) {
			text = botTextMessage.getBotText().getArabicText();
		} else {
			text = botTextMessage.getBotText().getEnglishText();
		}
		return text;
	}

	/**
	 * call webService
	 * 
	 * @return String response
	 * @param botWebserviceMessage
	 */

	/*public Map<String, String> callGetWebService(BotWebserviceMessage botWebserviceMessage, String senderId, ChatBotService chatBotService, String phoneNumber) {
		logger.debug(Constants.LOGGER_DIAL_IS + phoneNumber + Constants.LOGGER_METHOD_NAME + "callGetWebService");
		return getCalling(botWebserviceMessage, senderId, chatBotService, phoneNumber);
	}*/

	public Map<String, String> buyProductOrAddon(BotWebserviceMessage botWebserviceMessage, String jsonParam, ChatBotService chatBotService, String senderId) {
		CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		String dialNumber = customerProfile.getMsisdn();
		Map<String, String> mapResponse = new HashMap<>();
		String paramChannel = Constants.EMPTY_STRING;
		int responseStatusId = 0;
		try {
			paramChannel = Utils.encryptChannelParam(Constants.URL_PARAM_MSISDN_KEY + dialNumber + Constants.URL_TIME_CHANNEL_KEY + Constants.CHANEL_PARAM);
			String realParameter = Constants.URL_PARAM_CHANNEL_KEY + paramChannel;
			CloseableHttpClient client = HttpClients.createDefault();
			HttpPost httpPost = new HttpPost(botWebserviceMessage.getWsUrl());
			URI uri = new URIBuilder(httpPost.getURI()).addParameter("dial", realParameter).build();
			httpPost.setURI(uri);
			logger.debug(Constants.LOGGER_BUNDLE_SUPSCRIPTION + jsonParam);
			StringEntity entity = new StringEntity(jsonParam);
			httpPost.setEntity(entity);
			httpPost.setHeader("Accept", "application/json");
			httpPost.setHeader("Content-type", "application/json");
			CloseableHttpResponse response = client.execute(httpPost);
			responseStatusId = response.getStatusLine().getStatusCode();
			client.close();
		} catch (IOException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | URISyntaxException e) {
			logger.error(Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_EXCEPTION_MESSAGE + e);
			e.printStackTrace();
		}
		if (responseStatusId == 200) {
			mapResponse.put(Constants.RESPONSE_STATUS_KEY, String.valueOf(responseStatusId));
			mapResponse.put(Constants.RESPONSE_KEY, Constants.EMPTY_STRING);
		} else {
			mapResponse.put(Constants.RESPONSE_STATUS_KEY, String.valueOf(responseStatusId));
			mapResponse.put(Constants.RESPONSE_KEY, Constants.EMPTY_STRING);
		}
		return mapResponse;
	}

	/**
	 * @param senderId
	 * @param messagePayloadList
	 * @param botWebserviceMessage
	 * @param jsonBodyString
	 * @throws JSONException
	 */

	public void createTextMessageInDynamicScenario(String senderId, List<MessagePayload> messagePayloadList, BotWebserviceMessage botWebserviceMessage, String jsonBodyString,
			ChatBotService chatBotService, String local) {
		// string
		if (botWebserviceMessage.getOutType().getInOutTypeId() == 1) {
			getTextMessageIfResponseIsString(senderId, messagePayloadList, botWebserviceMessage, local);
			// Object
		} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 2) {
			getTextMessageIfResponseIsObject(senderId, messagePayloadList, botWebserviceMessage, jsonBodyString, chatBotService, local);
			// Array
		} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 3) {
			getTextMessageIfResponseIsArray(senderId, messagePayloadList, botWebserviceMessage, jsonBodyString, chatBotService, local);
		}
	}

	public String getTextForQuickReply(String local, BotQuickReplyMessage botQuickReplyMessage, String userDial) {
		String text = Constants.EMPTY_STRING;
		if (local.equalsIgnoreCase(Constants.ARABIC_LOCAL)) {
			text = botQuickReplyMessage.getBotText().getArabicText() + " " + userDial + " ?";
		} else {
			text = botQuickReplyMessage.getBotText().getEnglishText() + " " + userDial + " ?";
		}
		return text;
	}

	public CustomerProfile setLinkingInfoForCustomer(String senderId, String customerDial, ChatBotService chatBotService) {
		CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		CustomerProfile newCustomerProfile = new CustomerProfile();
		Date date = new Date();
		Timestamp timestamp = new Timestamp(date.getTime());
		newCustomerProfile.setCustomerLastSeen(timestamp);
		newCustomerProfile.setFirstInsertion(customerProfile.getFirstInsertion());
		newCustomerProfile.setLinkingDate(timestamp);
		newCustomerProfile.setLocale(customerProfile.getLocale());
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

	public void setCustomerProfileLocal(CustomerProfile customerProfile, ChatBotService chatBotService,String locale) {
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

	
	public MessagePayload getProductsFromJsonByCategory(JSONArray arrayResponse, String category, String senderId, ChatBotService chatBotService, String locale) {
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
							String label = Constants.EMPTY_STRING;
							if (locale.contains(Constants.ARABIC_LOCAL)) {
								label = categoryObject.getString(Constants.JSON_KEY_CATEGORY_NAME_AR);
							} else {
								label = categoryObject.getString(Constants.JSON_KEY_CATEGORY_NAME_EN);
							}
							String payload = categoryObject.getString(Constants.JSON_KEY_CATEGORY_ID);
							String subTitle = Constants.SUBTITLE_VALUE;
							PostbackButton button = PostbackButton.create(Utils.getLabelForViewButton(locale), Constants.PREFIX_RATEPLAN_SUBSCRIPTION + payload);
							Button backButton = PostbackButton.create(Utils.getLabelForBackButton(locale), Constants.PAYLOAD_CHANGE_BUNDLE);
							buttons.add(backButton);
							buttons.add(button);
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
			if (locale.equalsIgnoreCase(Constants.ARABIC_LOCAL)) {
				title = childObject.getString(Constants.JSON_KEY_NAME_AR);
				subtitle = childObject.getString(Constants.JSON_KEY_ARABIC_DESCRIPTION_KEY);
			} else {
				title = childObject.getString(Constants.JSON_KEY_NAME_EN);
				subtitle = childObject.getString(Constants.JSON_KEY_FOR_ENGLISH_DESCRIPTION);
			}
			if (relatedProducts.length() > 0) {
				Button button = PostbackButton.create(Utils.getLabelForٍSubscribeButton(locale), Constants.PREFIX_RELATED_PRODUCTS + Constants.COMMA_CHAR + name + "," + operationName);
				buttons.add(button);
			} else {
				Button button = PostbackButton.create(Utils.getLabelForٍSubscribeButton(locale), name + Constants.COMMA_CHAR + operationName);
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
					Button button = PostbackButton.create(Utils.getLabelForٍSubscribeButton(locale), name + Constants.COMMA_CHAR + operationName);
					Button backButton = PostbackButton.create(Utils.getLabelForBackButton(locale), Constants.PAYLOAD_CHANGE_BUNDLE);
					title = product.getString(Constants.JSON_KEY_NAME_EN);
					subtitle = product.getString(Constants.JSON_KEY_FOR_ENGLISH_DESCRIPTION);
					buttons.add(backButton);
					buttons.add(button);
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
				JSONArray products = new JSONArray();
				products = object.getJSONArray(Constants.JSON_KEY_FOR_PRODUCT);
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
							String title = relatedObject.getString(Constants.JSON_KEY_NAME_EN);
							String subtitle = relatedObject.getString(Constants.JSON_KEY_FOR_ENGLISH_DESCRIPTION);
							Button button = PostbackButton.create(Utils.getLabelForٍSubscribeButton(locale),
									relatedProductName + Constants.COMMA_CHAR + Constants.PREFIX_RELATED_PRODUCTS_SUBSCRIPTION);
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
		String buttonLabel = Utils.getLabelForٍSubscribeButton(locale);
		for (int i = 0; i < arrayResponse.length(); i++) {
			try {
				List<Button> buttonsList = new ArrayList<>();
				JSONObject object = arrayResponse.getJSONObject(i);
				JSONArray retrievedCategoriesArray = object.getJSONArray(Constants.JSON_KEY_FOR_PRODUCT_CATEGORY);
				String id = object.getString("id");
				for (int o = 0; o < retrievedCategoriesArray.length(); o++) {
					if (retrievedCategoriesArray.getString(o).equalsIgnoreCase(addonId)) {
						if (locale.equalsIgnoreCase(Constants.ARABIC_LOCAL)) {
							title = object.getString(Constants.JSON_KEY_NAME_AR);
						} else {
							title = object.getString(Constants.JSON_KEY_NAME_EN);
						}
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

	public MessagePayload getCategoryForMobileInternetAddons(JSONArray arrayResponse, String senderId, ChatBotService chatBotService, String locale) {
		logger.debug(Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_ELIGIBLE_PRODUCT + arrayResponse.toString());
		Map<String, String> categoriesMap = new HashMap<>();
		ArrayList<Element> elements = new ArrayList<>();
		String[] categoriesIDS = chatBotService.getEnabledCategoryConfigurationDaoById(2l).getEnglishCategories().split(Constants.COMMA_CHAR);
		String[] categoriesLabels = null;
		if (locale.equalsIgnoreCase(Constants.ARABIC_LOCAL)) {
			categoriesLabels = chatBotService.getEnabledCategoryConfigurationDaoById(2l).getCategoryLabel().getArabicText().split(Constants.COMMA_CHAR);
		} else {
			categoriesLabels = chatBotService.getEnabledCategoryConfigurationDaoById(2l).getCategoryLabel().getEnglishText().split(Constants.COMMA_CHAR);
		}

		String subTitle = Constants.SUBTITLE_VALUE;
		String buttonLabel = Constants.EMPTY_STRING;
		for (int i = 0; i < arrayResponse.length(); i++) {
			try {
				JSONObject object = arrayResponse.getJSONObject(i);
				JSONArray jsonCategoriesArray = object.getJSONArray(Constants.JSON_KEY_FOR_PRODUCT_CATEGORY);
				ArrayList<String> categoriesLabelsList = new ArrayList<>(Arrays.asList(categoriesLabels));
				ArrayList<String> enabledCategoriesList = new ArrayList<>(Arrays.asList(categoriesIDS));
				for (int o = 0; jsonCategoriesArray.length() > o; o++) {
					String category = jsonCategoriesArray.getString(o);
					if (enabledCategoriesList.contains(category)) {
						int index = enabledCategoriesList.indexOf(category);
						categoriesMap.put(categoriesLabelsList.get(index), jsonCategoriesArray.getString(o));
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
				Element element = Element.create(key, Optional.of(subTitle), empty(), empty(), Optional.of(buttonsList));
				elements.add(element);
			}
			GenericTemplate gTemplate = GenericTemplate.create(elements);
			return MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));

		} else {
			return createNotEligibleGenericMSG(senderId, locale, Constants.PAYLOAD_VIEW_CONNECT_DETAILS);
		}

	}

	public GenericTemplate createGenericTemplateForNotEligiblBundleDials(String locale, List<Button> buttonsList, List<Element> elements, String dial) {
		if (locale.contains(Constants.ARABIC_LOCAL)) {
			PostbackButton bundleButton = PostbackButton.create(Utils.getLabelForBackButton(locale), Constants.PAYLOAD_MI_CONSUMTION_PARENT);
			buttonsList.add(bundleButton);
			Element element = Element.create(Constants.INTERNET_BUNDLE_AR, Optional.of("نأسف هذا الرقم" + " ' " + dial + " ' " + "لا توجد باقات صالحة له"), empty(), empty(), Optional.of(buttonsList));
			elements.add(element);
			return GenericTemplate.create(elements);
		} else {
			PostbackButton bundleButton = PostbackButton.create(Utils.getLabelForBackButton(locale), Constants.PAYLOAD_MI_CONSUMTION_PARENT);
			buttonsList.add(bundleButton);
			Element element = Element.create(Constants.MOBILE_INTERNET_CONSUMPTION_NAME_EN, Optional.of("Your dial" + " ' " + dial + " ' " + "is not eligible to any bundle"), empty(), empty(),
					Optional.of(buttonsList));
			elements.add(element);
			return GenericTemplate.create(elements);
		}
	}

	public Map<String, String> getCalling(BotWebserviceMessage botWebserviceMessage, String senderId, ChatBotService chatBotService, String phoneNumber) {
		URI uri = Utils.createURI(botWebserviceMessage, senderId, chatBotService, phoneNumber);
		logger.debug(Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_DIAL_IS + phoneNumber +Constants.LOGGER_SERVICE_URL + uri);
		return Utils.callGetWebServiceByRestTemplate(uri,chatBotService);
	}

	public MessagePayload changeLanguageResponse(String locale, String senderId) {
		String text = Constants.EMPTY_STRING;
		if (locale.contains(Constants.ARABIC_LOCAL)) {
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
		if (locale.contains(Constants.ARABIC_LOCAL)) {
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

	public JSONObject rasaIntegration(String url, String payload, String paramKey) {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		JSONObject requestBody = new JSONObject();
		requestBody.put(paramKey, payload);
		HttpEntity<String> request = new HttpEntity<>(requestBody.toString());
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
		if (response.getStatusCodeValue() != 200) {
			return new JSONObject();
		}
		return new JSONObject(response.getBody());
	}
	
	
	
	public MessagePayload createErrorTemplateForNoConsumptionDetails(String senderId, String locale, String payload) {
		List<Element> elementsList = new ArrayList<>();
		List<Button> buttons = new ArrayList<>();
		String title;
		String subtitle;
		String buttonLabel = Utils.getLabelForBackButton(locale);
		if (locale.contains(Constants.ARABIC_LOCAL)) {
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
}
