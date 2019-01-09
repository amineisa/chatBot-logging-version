package com.chatbot.services;

import static java.util.Optional.empty;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chatbot.controller.ChatBotController;
import com.chatbot.entity.BotButton;
import com.chatbot.entity.BotButtonTemplateMSG;
import com.chatbot.entity.BotConfiguration;
import com.chatbot.entity.BotInteraction;
import com.chatbot.entity.BotInteractionMessage;
import com.chatbot.entity.BotQuickReplyMessage;
import com.chatbot.entity.BotTextMessage;
import com.chatbot.entity.BotWebserviceMessage;
import com.chatbot.entity.CustomerProfile;
import com.chatbot.entity.InteractionLogging;
import com.chatbot.entity.UserSelection;
import com.chatbot.util.CacheHelper;
import com.chatbot.util.Constants;
import com.chatbot.util.Utils;
import com.github.messenger4j.Messenger;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerIOException;
import com.github.messenger4j.send.HandoverAction;
import com.github.messenger4j.send.HandoverPayload;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.MessagingType;
import com.github.messenger4j.send.message.TemplateMessage;
import com.github.messenger4j.send.message.TextMessage;
import com.github.messenger4j.send.message.quickreply.QuickReply;
import com.github.messenger4j.send.message.template.ButtonTemplate;
import com.github.messenger4j.send.message.template.GenericTemplate;
import com.github.messenger4j.send.message.template.Template;
import com.github.messenger4j.send.message.template.button.Button;
import com.github.messenger4j.send.message.template.button.PostbackButton;
import com.github.messenger4j.send.message.template.button.UrlButton;
import com.github.messenger4j.send.message.template.common.Element;
import com.github.messenger4j.userprofile.UserProfile;
import com.hazelcast.core.IMap;

@Service
public class InteractionHandlingService {

	@Autowired
	private UtilService utilService;

	@Autowired
	private ChatBotService chatBotService;

	@Autowired
	IMap<String, Object> usersSelectionCache;

	@Autowired
	public MigrationService migrationService;

	@Autowired
	public GenericTemplateService genericTemplateService;

	private static CacheHelper<String, Object> wsResponseCache = new CacheHelper<>("usersResponses");

	private static final Logger logger = LoggerFactory.getLogger(ChatBotController.class);

	public String payloadSetting(String payload, String senderId) {
		UserSelection userSelections = getUserSelectionsFromCache(senderId);
		logger.debug(" User Selections " + userSelections.toString());
		if (payload.startsWith(Constants.PREFIX_ADDONSUBSCRIPE)) {// addonId
			userSelections.setAddonId(payload.substring(9, payload.length()) + Constants.COMMA_CHAR + "ACTIVATE");
			payload = Constants.PAYLOAD_ADDON_SUBSCRIPTION;
			updateUserSelectionsInCache(senderId, userSelections);
		}
		if (payload.startsWith(Constants.PREFIX_RATEPLAN_SUBSCRIPTION)) {// productIdAndOperationName
			userSelections.setProductIdAndOperationName(payload.substring(4, payload.length()));
			updateUserSelectionsInCache(senderId, userSelections);
			payload = payload.substring(0, 4);
		} else if (payload.startsWith(Constants.PREFIX_MOBILEINTERNET_ADDON)) {// addonId for MI
			userSelections.setAddonId(payload.substring(7, payload.length()));
			updateUserSelectionsInCache(senderId, userSelections);
			payload = Constants.PAYLOAD_BUY_ADDONS;
		} else if (payload.startsWith(Constants.PREFIX_RELATED_PRODUCTS)) {// productIdAndOperationName which has related products
			String[] params = payload.split(Constants.COMMA_CHAR);
			String allParameters = params[1] + "," + params[2];
			userSelections.setParametersListForRelatedProducts(allParameters);
			updateUserSelectionsInCache(senderId, userSelections);
			payload = Constants.PAYLOAD_RELATED_PRODUCTS;
		} else if (payload.equalsIgnoreCase(Constants.PREFIX_CONFIRM_MOBILEINTERNET_SUBSCRIPTION)) {
			String stringParametersListForRelatedProducts = userSelections.getParametersListForRelatedProducts();
			if (stringParametersListForRelatedProducts != null) {
				String[] parametersListForRelatedProducts = stringParametersListForRelatedProducts.split(",");
				logger.debug("User Selection" + userSelections.toString());
				if (parametersListForRelatedProducts != null && parametersListForRelatedProducts.length == 3) {
					payload = Constants.PREFIX_CONFIRM_MOBILEINTERNET_SUBSCRIPTION + "_related_product";
				} else if (parametersListForRelatedProducts != null && parametersListForRelatedProducts.length == 2) {
					payload = Constants.PREFIX_CONFIRM_MOBILEINTERNET_SUBSCRIPTION;
				}
			} // MObile Internet subscription in case not has related products
		} else if (payload.contains(Constants.COMMA_CHAR) && !payload.contains(Constants.PREFIX_RELATED_PRODUCTS_SUBSCRIPTION) && !payload.contains(Constants.PREFIX_MIGRATE_NAME)
				&& !payload.contains(Constants.PREFIX_MIGRATE_ID)) {// productIdAndOperationName subscription
			userSelections.setProductIdAndOperationName(payload);
			String[] parameters = userSelections.getProductIdAndOperationName().split(Constants.COMMA_CHAR);
			String parametersListForRelatedProducts = "";
			for (int i = 0; i < parameters.length; i++) {
				parametersListForRelatedProducts = parameters[i] + Constants.COMMA_CHAR;
			}
			userSelections.setParametersListForRelatedProducts(parametersListForRelatedProducts);
			payload = Constants.PAYLOAD_MOBILE_INTERNET_CONFIRMATION_MSG;
			updateUserSelectionsInCache(senderId, userSelections);
		} else if (payload.contains(Constants.PREFIX_RELATED_PRODUCTS_SUBSCRIPTION)) {
			String relatedId = payload.substring(0, payload.indexOf(','));
			String[] parameterArry = userSelections.getParametersListForRelatedProducts().split(Constants.COMMA_CHAR);
			if (parameterArry.length < 3) {
				String stingParametersListForRelatedProducts = userSelections.getParametersListForRelatedProducts() + Constants.COMMA_CHAR + relatedId;
				userSelections.setParametersListForRelatedProducts(stingParametersListForRelatedProducts);
				updateUserSelectionsInCache(senderId, userSelections);
			}
			logger.debug(" User Selections " + userSelections.toString());
			payload = Constants.PAYLOAD_MOBILE_INTERNET_CONFIRMATION_MSG;
		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_MOBILE_INTERNET_SUBSCRIPTION_CANCEL)) {
			payload = Constants.PAYLOAD_CHANGE_BUNDLE;
		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_CANCEL_RECHARGING) || payload.equalsIgnoreCase(Constants.PAYLOAD_CANCEL_BILL_PAYMENT)) {
			payload = Constants.PAYLOAD_CANCEL_PAY_OR_RECHARGE;
		} else if (payload.contains(Constants.PREFIX_MIGRATE_ID)) {
			userSelections.setRateplanIdForMigration(Integer.parseInt(payload.split(Constants.COMMA_CHAR)[1]));
			updateUserSelectionsInCache(senderId, userSelections);
			payload = Constants.PAYLOAD_CONFIRMATION_MIGRATE;
		} else if (payload.contains(Constants.PREFIX_MIGRATE_NAME)) {
			userSelections.setRateplanNameForMigration(payload.split(Constants.COMMA_CHAR)[1]);
			updateUserSelectionsInCache(senderId, userSelections);
			payload = Constants.PAYLOAD_CONFIRMATION_MIGRATE;
		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_MIGRATE_CONFIRM)) {
			if (userSelections.getRateplanIdForMigration() != 0) {
				payload = Constants.PAYLOAD_MIGRATE_BY_ID;
			} else if (userSelections.getRateplanNameForMigration() != null) {
				payload = Constants.PAYLOAD_MIGRATE_BY_NAME;
			}
		}
		return payload;
	}

	public void handlePayload(String payload, Messenger messenger, String senderId) {
		String userFirstName, userLocale, lastName;
		userFirstName = userLocale = lastName = "";
		Utils.markAsTypingOn(messenger, senderId);
		if (payload != null && payload.equalsIgnoreCase("welcome")) {
			UserProfile userProfile = Utils.getUserProfile(senderId, messenger);
			userFirstName = userProfile.firstName();
			lastName = userProfile.lastName();
			if (userProfile.locale() == null) {
				userLocale = Constants.LOCALE_EN;
			}
		} else if (payload != null) {
			payload = payloadSetting(payload, senderId);
		}
		CustomerProfile customerProfile = Utils.saveCustomerInformation(chatBotService, senderId, userLocale, userFirstName, lastName);
		String phoneNumber = Constants.EMPTY_STRING;
		userLocale = customerProfile.getLocale() == null ? Constants.LOCALE_EN : customerProfile.getLocale();
		logger.debug("Handle payload for customer " + customerProfile.toString());
		try {
			ArrayList<MessagePayload> messagePayloadList = new ArrayList<>();
			if (payload.equalsIgnoreCase(Constants.LOCALE_EN)) {
				utilService.setCustomerProfileLocal(customerProfile, chatBotService, Constants.LOCALE_EN);
				messagePayloadList.add(utilService.changeLanguageResponse(Constants.LOCALE_EN, senderId));
				if (!messagePayloadList.isEmpty()) {
					sendMultipleMessages(messagePayloadList, senderId, messenger);
				}
			} else if (payload.equalsIgnoreCase(Constants.LOCALE_AR)) {
				utilService.setCustomerProfileLocal(customerProfile, chatBotService, Constants.LOCALE_AR);
				messagePayloadList.add(utilService.changeLanguageResponse(Constants.LOCALE_AR, senderId));
				if (!messagePayloadList.isEmpty()) {
					sendMultipleMessages(messagePayloadList, senderId, messenger);
				}
			} else {
				BotInteraction botInteraction = chatBotService.findInteractionByPayload(payload);
				if (botInteraction == null && !payload.equals("Migrate More")) {
					UserSelection userSelections = getUserSelectionsFromCache(senderId);
					userSelections.setFreeText(payload);
					updateUserSelectionsInCache(senderId, userSelections);
					handlePayload("rasa", messenger, senderId);
				} else if (payload.equals("Migrate More")) {
					Template template = migrationService.displayMigrationRatePlans(customerProfile.getMsisdn(), userLocale, new JSONArray());
					MessagePayload messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(template));
					messagePayloadList.add(messagePayload);
					sendMultipleMessages(messagePayloadList, senderId, messenger);

				} else {
					InteractionLogging interactionLogging = Utils.interactionLogginghandling(customerProfile, botInteraction, chatBotService);
					phoneNumber = customerProfile.getMsisdn() != null ? customerProfile.getMsisdn() : Constants.EMPTY_STRING;
					if (!botInteraction.getIsSecure() || phoneNumber.length() > 0) {
						UserSelection userSelections = getUserSelectionsFromCache(senderId);
						if (botInteraction.getParentPayLoad() != null && userSelections != null) {
							userSelections.setParentPayLoad(botInteraction.getParentPayLoad());
							updateUserSelectionsInCache(senderId, userSelections);
						}
						List<BotInteractionMessage> interactionMessageList = chatBotService.findInteractionMessagesByInteractionId(botInteraction.getInteractionId());
						MessagePayload messagePayload = null;
						for (BotInteractionMessage botInteractionMessage : interactionMessageList) {
							Long messageTypeId = botInteractionMessage.getBotMessageType().getMessageTypeId();
							Long messageId = botInteractionMessage.getMessageId();

							if (botInteractionMessage.getIsStatic()) {
								messagePayload = responseInCaseStaticScenario(payload, senderId, userFirstName, botInteractionMessage, messageTypeId, messageId, chatBotService, userLocale,
										phoneNumber);
								messagePayloadList.add(messagePayload);
							} else {// Dynamic Scenario
								dynamicScenarioController(payload, messenger, senderId, userLocale, botInteraction, messagePayloadList, messageTypeId, messageId, phoneNumber);
							}
						}
						if (!messagePayloadList.isEmpty()) {
							sendMultipleMessages(messagePayloadList, senderId, messenger);
						}

						String parentPayLoad = Constants.EMPTY_STRING;
						userSelections = getUserSelectionsFromCache(senderId);
						if (userSelections != null && !payload.equalsIgnoreCase(Constants.PAYLOAD_CHANGE_BUNDLE)) {
							parentPayLoad = userSelections.getParentPayLoad() == null ? Constants.EMPTY_STRING : userSelections.getParentPayLoad();
						}

						if (parentPayLoad.length() > 0) {
							userSelections.setParentPayLoad(null);
							updateUserSelectionsInCache(senderId, userSelections);
							handlePayload(parentPayLoad, messenger, senderId);
						}
					} else {
						UserSelection userSelections = getUserSelectionsFromCache(senderId);
						userSelections.setOriginalPayLoad(payload);
						updateUserSelectionsInCache(senderId, userSelections);
						BotInteraction loginInteraction = chatBotService.findInteractionByPayload("login");
						String title = Constants.EMPTY_STRING;
						BotInteractionMessage interactionMSG = chatBotService.findMessageByInteraction(loginInteraction);
						BotButtonTemplateMSG botButtonTemplate = chatBotService.findBotButtonTemplateByMessageId(interactionMSG);
						title = utilService.getTextForButtonTemplate(userLocale, botButtonTemplate);
						List<BotButton> botButtons = chatBotService.findAllByBotButtonTemplateMSGId(botButtonTemplate);

						// messenger4j
						ArrayList<Button> buttons = new ArrayList<>();
						for (BotButton botButton : botButtons) {
							Button button = utilService.createButton(botButton, userLocale, new JSONObject(), phoneNumber);
							buttons.add(button);
						}

						ButtonTemplate buttonTemplate = ButtonTemplate.create(title, buttons);
						MessagePayload messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(buttonTemplate));
						messagePayloadList.add(messagePayload);
						if (!messagePayloadList.isEmpty()) {
							sendMultipleMessages(messagePayloadList, senderId, messenger);
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error(Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_EXCEPTION_MESSAGE + e);
			e.printStackTrace();
			// handlePayload(Constants.PAYLOAD_FAULT_MSG, messenger, senderId);
		}

	}

	private void dynamicScenarioController(String payload, Messenger messenger, String senderId, String userLocale, BotInteraction botInteraction, ArrayList<MessagePayload> messagePayloadList,
			Long messageTypeId, Long messageId, String phoneNumber) {
		logger.debug(Constants.LOGGER_DIAL_IS + phoneNumber + Constants.LOGGER_METHOD_NAME + " dynamicScenario and Interaction is " + botInteraction.toString());
		UserSelection userSelections = getUserSelectionsFromCache(senderId);
		BotWebserviceMessage botWebserviceMessage = chatBotService.findWebserviceMessageByMessageId(messageId);
		String response = Constants.EMPTY_STRING;
		Map<String, String> mapResponse = new HashMap<>();
		String productIdAndOperationName = Constants.EMPTY_STRING;
		if (userSelections.getProductIdAndOperationName() != null && payload != "related product") {
			productIdAndOperationName = userSelections.getProductIdAndOperationName() == null ? Constants.EMPTY_STRING : userSelections.getProductIdAndOperationName();
		}
		if (botWebserviceMessage.getBotMethodType().getMethodTypeId() == 1) { // GET
			String url = botWebserviceMessage.getWsUrl();
			boolean cacheValue = false;
			Map<String, String> cachedMAp = getFromCachDependOnWebService(url, phoneNumber);
			if (cachedMAp == null || cachedMAp.size() == 0) {
				mapResponse = utilService.getCalling(botWebserviceMessage, senderId, chatBotService, phoneNumber);
				cacheValue = true;
				logger.debug(Constants.LOGGER_SERVER_RESPONSE);
			} else {
				mapResponse = cachedMAp;
				logger.debug(Constants.LOGGER_CACHED_RESPONSE);
			}
			if (mapResponse.get(Constants.RESPONSE_STATUS_KEY).equals("200")) {
				if (cacheValue) {
					if (payload.equalsIgnoreCase(Constants.PAYLOAD_POSTPAID_DIAL)) {
						// lastBill
						logger.debug("Billing profile Object ");

						JSONObject billProfile = new JSONObject(mapResponse.get(Constants.RESPONSE_KEY));
						String billAmount = "";
						if (!billProfile.get(Constants.JSON_KEY_BIILAMOUNT).equals(null)) {
							billAmount = billProfile.getString(Constants.JSON_KEY_BIILAMOUNT) == null || billProfile.getString(Constants.JSON_KEY_BIILAMOUNT) == "" ? ""
									: billProfile.getString(Constants.JSON_KEY_BIILAMOUNT);
						}
						logger.debug("Billing Amount " + billAmount);
						if (billAmount == "") {// post paid without bill to paid
							postPaidNoBillToPaid(senderId, userLocale, messagePayloadList, messageTypeId);
						} else {// post paid with bill to paid
							postPaidbillingPaymentHandling(senderId, userLocale, messagePayloadList, messageTypeId, phoneNumber, billProfile, billAmount);
						}
					}
					putInCachDependOnWebService(url, mapResponse, phoneNumber);
				}
				response = mapResponse.get(Constants.RESPONSE_KEY);
			} else {
				messageTypeId = 0L;
				handlePayload(Constants.PAYLOAD_FAULT_MSG, messenger, senderId);
			}
		} else if (botWebserviceMessage.getBotMethodType().getMethodTypeId() == 2) {
			response = postWebServicehandling(payload, messenger, senderId, userSelections, botWebserviceMessage, response, productIdAndOperationName);
		}
		if (messageTypeId != 0 && messageTypeId == Utils.MessageTypeEnum.TEXTMESSAGE.getValue()) {// Text Message
			utilService.createTextMessageInDynamicScenario(senderId, messagePayloadList, botWebserviceMessage, response, chatBotService, userLocale);
		} else if (messageTypeId != 0 && messageTypeId == Utils.MessageTypeEnum.ButtonTemplate.getValue()) {// Button Template
			if (botWebserviceMessage.getOutType().getInOutTypeId() == 1) {// String
			} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 2) {// Object
			} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 3) {// Array
				buttonTemplateHandling(senderId, userLocale, messagePayloadList, phoneNumber, botWebserviceMessage, response);
			}
		} else if (messageTypeId != 0 && messageTypeId == Utils.MessageTypeEnum.GENERICTEMPLATEMESSAGE.getValue()) {// Generic Template
			if (payload.equals("rateplan actions")) {
				JSONObject jsonObject = new JSONObject(response);
				JSONArray ratePlan = jsonObject.getJSONArray("rateplan");
				if (ratePlan.length() == 0) {
					handlePayload("rateplan without consumption", messenger, senderId);
				} else {
					genericTemplateHandling(payload, messenger, senderId, userLocale, messagePayloadList, messageId, phoneNumber, userSelections, botWebserviceMessage, response);
				}
			} else {
					genericTemplateHandling(payload, messenger, senderId, userLocale, messagePayloadList, messageId, phoneNumber, userSelections, botWebserviceMessage, response);
			}
		}

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
			text = utilService.getTextValueForBotTextMessage(botTextMessage, locale);
			text = text + " " + userFirstName;
			messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(text));

		}
		// quick reply
		else if (messageTypeId == Utils.MessageTypeEnum.QUICKREPLYMESSAGE.getValue()) {
			BotQuickReplyMessage botQuickReplyMessage = chatBotService.findQuickReplyMessageByMessageId(messageId);
			text = utilService.getTextForQuickReply(locale, botQuickReplyMessage, userDial);
			List<QuickReply> quickReplies = utilService.createQuickReply(chatBotService, messageId, locale);
			Optional<List<QuickReply>> quickRepliesOp = Optional.of(quickReplies);
			messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(text, quickRepliesOp, empty()));
		}
		// generic template
		else if (messageTypeId == Utils.MessageTypeEnum.GENERICTEMPLATEMESSAGE.getValue()) {
			Template template = genericTemplateService.createGenericTemplate(messageId, chatBotService, locale, new BotWebserviceMessage(), new JSONObject(), userDial, new ArrayList<String>(),
					payload);

			messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(template));
			// ButtonTemplate
		} else if (messageTypeId == Utils.MessageTypeEnum.ButtonTemplate.getValue()) {
			ButtonTemplate buttonTemplate = utilService.createButtonTemplateInScenario(botInteractionMessage, chatBotService, locale, userDial);
			messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(buttonTemplate));
		}
		return messagePayload;
	}

	/**
	 * @param senderId
	 * @param userLocale
	 * @param messagePayloadList
	 * @return
	 */
	public void postPaidNoBillToPaid(String senderId, String userLocale, ArrayList<MessagePayload> messagePayloadList, Long messageTypeId) {

		MessagePayload messagePayload;
		List<Button> buttons = new ArrayList<>();
		Button backButton = PostbackButton.create(Utils.getLabelForBackButton(userLocale), Constants.PAYLOAD_WELCOME_AGAIN);
		buttons.add(backButton);
		ButtonTemplate buttonTemplate = ButtonTemplate.create(createNOBillingProfileMessage(userLocale), buttons);
		messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(buttonTemplate));
		messagePayloadList.add(messagePayload);
		messageTypeId = 0L;
	}

	/**
	 * @param senderId
	 * @param userLocale
	 * @param messagePayloadList
	 * @param messageTypeId
	 * @param phoneNumber
	 * @param billProfile
	 * @param billAmount
	 * @return
	 */
	public void postPaidbillingPaymentHandling(String senderId, String userLocale, ArrayList<MessagePayload> messagePayloadList, Long messageTypeId, String phoneNumber, JSONObject billProfile,
			String billAmount) {
		MessagePayload messagePayload;
		String billingParam = billProfile.getJSONArray(Constants.JSON_KEY_ACTION_BUTTONS).getJSONObject(0).getString(Constants.JSON_KEY_PARAM);
		logger.debug("Billing Param " + billingParam);
		messagePayload = createBillingProfileInformationMessage(userLocale, senderId, billAmount);
		messagePayloadList.add(messagePayload);
		BotConfiguration payBillbaseUrlRaw = chatBotService.getBotConfigurationByKey(Constants.PAY_BILL_BASE_URL);
		String baseUrl = payBillbaseUrlRaw.getValue();
		String paramChanel;
		try {
			paramChanel = Utils.encryptChannelParam(Constants.URL_PARAM_MSISDN_KEY + phoneNumber + Constants.URL_TIME_CHANNEL_KEY + Constants.CHANEL_PARAM);
			// "paramChannel:74524b535742674c35536b693443454c696f45486f645a3676654b59756f4d573479677558446d673266416944446258793530367634734b43625a3868556b76467659472b706c657a5764590a6979747a4c6f594266413d3d";
			String webServiceUrl = baseUrl + paramChanel + "&operationParam=" + billingParam + "&lang=" + userLocale;
			URI uri = new URI(webServiceUrl);
			Map<String, String> values = Utils.callGetWebServiceByRestTemplate(uri, chatBotService);
			logger.debug("Status " + values.get(Constants.RESPONSE_STATUS_KEY));
			if (values.get(Constants.RESPONSE_STATUS_KEY).equals("200")) {
				List<Button> buttons = new ArrayList<>();
				JSONObject iframeObject = new JSONObject(values.get(Constants.RESPONSE_KEY));
				String payBillButtonURl = iframeObject.getString(Constants.JSON_KEY_IFRAME_BILL_PAYMENT_URL);
				UrlButton payButton = UrlButton.create(Utils.getLabelForPayBillButton(userLocale), Utils.createUrl(payBillButtonURl));
				PostbackButton backButton = PostbackButton.create(Utils.getLabelForBackButton(userLocale), Constants.PAYLOAD_CONSUMPTION);
				buttons.add(payButton);
				buttons.add(backButton);
				ButtonTemplate buttonTemplate = ButtonTemplate.create(Utils.getTitleForPayBillButton(userLocale), buttons);
				MessagePayload buttonsMessagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(buttonTemplate));
				messagePayloadList.add(buttonsMessagePayload);
				messageTypeId = 0L;
			}
			messageTypeId = 0L;
		} catch (URISyntaxException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}

	}

	/**
	 * @param payload
	 * @param messenger
	 * @param senderId
	 * @param userLocale
	 * @param messagePayloadList
	 * @param messageId
	 * @param phoneNumber
	 * @param userSelections
	 * @param botWebserviceMessage
	 * @param response
	 */
	public void genericTemplateHandling(String payload, Messenger messenger, String senderId, String userLocale, ArrayList<MessagePayload> messagePayloadList, Long messageId, String phoneNumber,
			UserSelection userSelections, BotWebserviceMessage botWebserviceMessage, String response) {
		if (botWebserviceMessage.getOutType().getInOutTypeId() == 1) {// string
		} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 2) {// Object
			JSONObject jsonResponse = new JSONObject(response);
			if (payload.equalsIgnoreCase(Constants.PAYLOAD_ACCOUNT_DETAILS)) {
				accuntDetailsRouting(messenger, senderId, jsonResponse);
			} else {
				JSONArray ratePlan = jsonResponse.getJSONArray(Constants.JSON_KEY_RATEPLAN);
				JSONArray connect = jsonResponse.getJSONArray(Constants.JSON_KEY_MOBILE_INTERNET);
				JSONArray mobileInternetAddonConsumption = jsonResponse.getJSONArray(Constants.JSON_KEY_MOBILE_INTERNET_ADON);
				JSONArray ratePlanAddonConsumption = jsonResponse.getJSONArray(Constants.JSON_KEY_RATEPLAN_ADON);
				logger.debug(Constants.LOGGER_MOBILE_INTERENET_CONSUMPTION + connect.toString());
				logger.debug(Constants.LOGGER_RATEPLAN_CONSUMPTION + ratePlan.toString());
				if (connect.length() > 0) {// productId For Renew
					userSelections.setProductIdForRenew(connect.getJSONObject(0).getString(Constants.JSON_KEY_PRODUCT_ID) + Constants.OPERATION_NAME_RENEW);
					updateUserSelectionsInCache(senderId, userSelections);
				}
				if (payload.equalsIgnoreCase(Constants.PAYLOAD_CONSUMPTION)) {
					baseConsumptionHandling(messenger, senderId, ratePlan, connect);
				} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_RATEPLAN_DETAILS)) {
					rateplanConsumptionDetails(payload, senderId, userLocale, messagePayloadList, messageId, phoneNumber, botWebserviceMessage, jsonResponse, ratePlan,messenger);
				} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_VIEW_CONNECT_DETAILS) && connect.length() == 0) {
					payload = Constants.EMPTY_STRING;
					userSelections.setParentPayLoad(Constants.EMPTY_STRING);
					updateUserSelectionsInCache(senderId, userSelections);
					handlePayload(Constants.PAYLOAD_CHANGE_BUNDLE, messenger, senderId);
				} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_VIEW_CONNECT_DETAILS) || payload.equals(Constants.PAYLOAD_RATEPLAN_ADDONS_CONSUMPTION)
						|| payload.equals(Constants.PAYLOAD_MOBILEINTERNET_ADDONS_CONSUMPTION)) {
					userSelections.setSubscribed(true);
					ArrayList<String> consumptionNames = new ArrayList<>();
					if (payload.equalsIgnoreCase(Constants.PAYLOAD_RATEPLAN_ADDONS_CONSUMPTION)) {
						setConsumptionNamesList(userLocale, ratePlanAddonConsumption, consumptionNames);
					} else if (payload.equals(Constants.PAYLOAD_MOBILEINTERNET_ADDONS_CONSUMPTION)) {
						setConsumptionNamesList(userLocale, mobileInternetAddonConsumption, consumptionNames);
					}
					/*
					 * if (payload.contains(Constants.PAYLOAD_VIEW_CONNECT_DETAILS) &&
					 * connect.length() == 0) { handlePayload("NO Internet Bundle Found", messenger,
					 * senderId); //messagePayloadList.add(MessagePayload.create(senderId,
					 * MessagingType.RESPONSE, TextMessage.create(createMainBundleDetails(connect,
					 * userLocale)))); }
					 */
					Template template = genericTemplateService.createGenericTemplate(messageId, chatBotService, userLocale, botWebserviceMessage, jsonResponse, phoneNumber, consumptionNames, payload);
					MessagePayload mPayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(template));
					messagePayloadList.add(mPayload);
				} else if (payload.equals(Constants.PAYLOAD_RATEPLAN_ACTIONS) || payload.equals(Constants.PAYLOAD_MOBILE_INTERNET_CONTROLLER)) {
					if (payload.equals(Constants.PAYLOAD_MOBILE_INTERNET_CONTROLLER) && jsonResponse.getJSONArray(Constants.JSON_KEY_MOBILE_INTERNET).length() == 0) {
						handlePayload(Constants.PAYLOAD_NO_MI_BUNDLE_FOUND, messenger, senderId);
					}
					Template template = genericTemplateService.createGenericTemplate(messageId, chatBotService, userLocale, botWebserviceMessage, jsonResponse, phoneNumber, null, payload);
					MessagePayload mPayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(template));
					messagePayloadList.add(mPayload);
				}
			}
		} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 3) {// Array
			if (payload.contains(Constants.PAYLOAD_MIGRATE)) {
				Template gTemplate = migrationService.migrationHandling(payload, phoneNumber, userLocale, new JSONArray(response));
				MessagePayload gPayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));
				messagePayloadList.add(gPayload);
			} else {
				customerEligibilityHandling(payload, senderId, userLocale, messagePayloadList, phoneNumber, userSelections, response);
			}
		}
	}

	/**
	 * @param messenger
	 * @param senderId
	 * @param jsonResponse
	 */
	public void accuntDetailsRouting(Messenger messenger, String senderId, JSONObject jsonResponse) {
		Boolean postPaid = jsonResponse.getBoolean(Constants.JSON_KEY_POSTPAID);
		if (!postPaid) {
			JSONObject balance = jsonResponse.getJSONObject(Constants.JSON_KEY_BALANCE);
			if (balance == null) {
				handlePayload(Constants.PAYLOAD_FAULT_MSG, messenger, senderId);
			} else {
				handlePayload(Constants.PAYLOAD_PREPAID, messenger, senderId);
			}
		} else {
			handlePayload(Constants.PAYLOAD_POSTPAID_DIAL, messenger, senderId);
		}
	}

	/**
	 * @param payload
	 * @param senderId
	 * @param userLocale
	 * @param messagePayloadList
	 * @param messageId
	 * @param phoneNumber
	 * @param userSelections
	 * @param botWebserviceMessage
	 * @param jsonResponse
	 * @param ratePlan
	 */
	public void rateplanConsumptionDetails(String payload, String senderId, String userLocale, ArrayList<MessagePayload> messagePayloadList, Long messageId, String phoneNumber,
			BotWebserviceMessage botWebserviceMessage, JSONObject jsonResponse, JSONArray ratePlan,Messenger messenger) {
		boolean isHasNoConsumption = false;
		if (ratePlan.length() > 0) {
			JSONObject object = ratePlan.getJSONObject(0);
			Set<String> keys = object.keySet();
			for (String key : keys) {
				if (key.equalsIgnoreCase(Constants.JSON_KEY_CONSUMPTION_DETAILS_LIST) && object.getJSONArray(Constants.JSON_KEY_CONSUMPTION_DETAILS_LIST) != null
						&& object.getJSONArray(Constants.JSON_KEY_CONSUMPTION_DETAILS_LIST).length() == 0) {
					isHasNoConsumption = true;
				}
			}
			//messagePayloadList.add(MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(createMainBundleDetails(ratePlan, userLocale))));
			if (!isHasNoConsumption) {
				ArrayList<String> cNames = new ArrayList<>();
				JSONArray consumptionDetailsList = ratePlan.getJSONObject(0).getJSONArray(Constants.JSON_KEY_CONSUMPTION_DETAILS_LIST);
				for (int j = 0; j < consumptionDetailsList.length(); j++) {
					if (userLocale.contains(Constants.LOCALE_AR)) {
						cNames.add(consumptionDetailsList.getJSONObject(j).getJSONObject("consumed").getString(Constants.JSON_KEY_NAME_AR));
					} else {
						cNames.add(consumptionDetailsList.getJSONObject(j).getJSONObject("consumed").getString(Constants.JSON_KEY_NAME_EN));
					}
				}
				Template template = genericTemplateService.createGenericTemplate(messageId, chatBotService, userLocale, botWebserviceMessage, jsonResponse, phoneNumber, cNames, payload);
				MessagePayload mPayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(template));
				messagePayloadList.add(mPayload);
			} else {
				handlePayload("rateplan without meters", messenger, senderId);/*
				MessagePayload mPayload = utilService.createErrorTemplateForNoConsumptionDetails(senderId, userLocale, Constants.PAYLOAD_CONSUMPTION);
				messagePayloadList.add(mPayload);*/
			}
		}
	}

	/**
	 * @param messenger
	 * @param senderId
	 * @param ratePlan
	 * @param connect
	 */
	public void baseConsumptionHandling(Messenger messenger, String senderId, JSONArray ratePlan, JSONArray connect) {
		if (ratePlan.length() > 0 && connect.length() > 0) {
			handlePayload(Constants.PAYLOAD_RATEPLAN_AND_CONNECT, messenger, senderId);
		} else if (ratePlan.length() > 0 && connect.length() == 0) {
			handlePayload(Constants.PAYLOAD_VIEW_RATEPLAN_DETAILS, messenger, senderId);
		} else if (ratePlan.length() == 0 && connect.length() > 0) {
			handlePayload(Constants.PAYLOAD_VIEW_ROOT_CONNECT_DETAILS, messenger, senderId);
		} else if (ratePlan.length() == 0 && connect.length() == 0) {
			handlePayload(Constants.PAYLOAD_VIEW_MOBILEINTERNET_SUBSCRIPTION, messenger, senderId);
		}
	}

	/**
	 * @param payload
	 * @param senderId
	 * @param userLocale
	 * @param messagePayloadList
	 * @param phoneNumber
	 * @param userSelections
	 * @param response
	 */
	public void customerEligibilityHandling(String payload, String senderId, String userLocale, ArrayList<MessagePayload> messagePayloadList, String phoneNumber, UserSelection userSelections,
			String response) {
		// Array
		if (payload.startsWith(Constants.PREFIX_RATEPLAN_SUBSCRIPTION)) {
			JSONArray bundleArray = new JSONArray(response);
			if (bundleArray.length() > 0) {// productIdAndOperationName
				messagePayloadList.add(utilService.getProductsFromJsonByCategory(bundleArray, userSelections.getProductIdAndOperationName(), senderId, userLocale));
			}
		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_RELATED_PRODUCTS)) {
			JSONArray bundleArray = new JSONArray(response);
			if (bundleArray.length() > 0) {
				String productId = userSelections.getParametersListForRelatedProducts().split(Constants.COMMA_CHAR)[0];
				messagePayloadList.add(utilService.getRelatedProductFromJsonByBundleId(bundleArray, productId, senderId, userLocale));
			}
		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_CHANGE_BUNDLE)) {
			JSONArray bundleArray = new JSONArray(response);
			if (bundleArray.length() > 0) {
				/*
				 * if (!userSelections.isSubscribed()) {
				 * messagePayloadList.add(MessagePayload.create(senderId,
				 * MessagingType.RESPONSE,
				 * (TextMessage.create(Utils.informUserThatHeDoesnotSubscribeAtAnyMIBundle(
				 * userLocale))))); }
				 */
				messagePayloadList.add(genericTemplateService.getBundleCategories(bundleArray, senderId, chatBotService, userLocale, phoneNumber));
			} else {// addition
				GenericTemplate gtemplate = genericTemplateService.createGenericTemplateForNotEligiblBundleDials(userLocale, new ArrayList<Button>(), new ArrayList<Element>(), phoneNumber);
				messagePayloadList.add(MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gtemplate)));
			}
		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_BUY_ADDONS_ROOT)) {
			JSONArray categoryArray = new JSONArray(response);
			if (categoryArray.length() > 0) {
				messagePayloadList.add(utilService.getCategoryForMobileInternetAddons(categoryArray, senderId, chatBotService, userLocale));
			}
		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_BUY_ADDONS)) {
			JSONArray categoryArray = new JSONArray(response);
			if (categoryArray.length() > 0) {
				messagePayloadList.add(utilService.getExtraMobileInternetAddonsByCategory(categoryArray, senderId, userLocale, userSelections.getAddonId()));
			}
		}
	}

	/**
	 * @param senderId
	 * @param userLocale
	 * @param messagePayloadList
	 * @param phoneNumber
	 * @param botWebserviceMessage
	 * @param response
	 */
	public void buttonTemplateHandling(String senderId, String userLocale, ArrayList<MessagePayload> messagePayloadList, String phoneNumber, BotWebserviceMessage botWebserviceMessage,
			String response) {
		MessagePayload messagePayload;
		JSONArray jsonArray = new JSONArray(response);
		if (jsonArray.length() > 0) {
			List<BotButton> webServiceMessageButton = chatBotService.findAllButtonsByWebserviceMessage(botWebserviceMessage);
			JSONObject jsonObject = new JSONObject();
			List<Button> realButtons = new ArrayList<>();
			for (int j = 0; j < jsonArray.length(); j++) {
				Button realButton = null;
				for (BotButton wsBotButton : webServiceMessageButton) {
					jsonObject = jsonArray.getJSONObject(j);
					realButton = utilService.createButton(wsBotButton, userLocale, jsonObject, phoneNumber);
				}
				realButtons.add(realButton);
			}
			ButtonTemplate buttonTemplate = ButtonTemplate.create(botWebserviceMessage.getTitle().getEnglishText(), realButtons);
			messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(buttonTemplate));
			messagePayloadList.add(messagePayload);
		}
	}

	public void callSecondryHandover(final String senderId, String phoneNumber, Messenger messenger) {
		BotConfiguration secondryAppIdRaw = chatBotService.getBotConfigurationByKey(Constants.SECONDRY_APP_ID);
		BotConfiguration informClientMSGRaw = chatBotService.getBotConfigurationByKey(Constants.TELL_CLIENT_WAIT_FOR_AGENT_RESPONSE);
		String appId = secondryAppIdRaw.getValue();
		if (phoneNumber == null || !phoneNumber.startsWith("0")) {
			phoneNumber = ".";
		}
		MessagePayload messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(informClientMSGRaw.getValue() + " " + phoneNumber));
		logger.debug("PASS THREAD CONTROL TO " + appId);
		HandoverPayload handoverPayload = HandoverPayload.create(senderId, HandoverAction.pass_thread_control, appId, "information");
		try {
			messenger.send(messagePayload);
			messenger.handover(handoverPayload);
		} catch (MessengerApiException | MessengerIOException io) {
			logger.error(Constants.LOGGER_DIAL_IS + "callSecondryHandover" + Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_EXCEPTION_MESSAGE + io);
			io.printStackTrace();
		}
	}

	/**
	 * @param senderId
	 * @return
	 *//*
		 * public UserSelection getUserSelections(String senderId) { return
		 * chatBotService.getUserSelectionBySenderId(senderId) == null ? new
		 * UserSelection() : chatBotService.getUserSelectionBySenderId(senderId); }
		 */

	public UserSelection getUserSelectionsFromCache(String senderId) {
		return usersSelectionCache.get(senderId) == null ? new UserSelection() : (UserSelection) usersSelectionCache.get(senderId);
	}

	public void updateUserSelectionsInCache(String senderId, UserSelection userSelection) {
		usersSelectionCache.put(senderId, userSelection);
	}

	/*
	 * public void updateUserSelections(UserSelection userSelections, String
	 * senderId) { UserSelection newUserSelection = new UserSelection();
	 * newUserSelection.setAddonId(userSelections.getAddonId());
	 * newUserSelection.setFreeText(userSelections.getFreeText());
	 * newUserSelection.setOriginalPayLoad(userSelections.getOriginalPayLoad());
	 * newUserSelection.setParametersListForRelatedProducts(userSelections.
	 * getParametersListForRelatedProducts());
	 * newUserSelection.setParentPayLoad(userSelections.getParentPayLoad());
	 * newUserSelection.setPhoneNumber(userSelections.getPhoneNumber());
	 * newUserSelection.setProductIdAndOperationName(userSelections.
	 * getProductIdAndOperationName());
	 * newUserSelection.setProductIdForRenew(userSelections.getProductIdForRenew());
	 * newUserSelection.setSenderId(senderId);
	 * newUserSelection.setSubscribed(userSelections.isSubscribed());
	 * newUserSelection.setFreeText(userSelections.getFreeText());
	 * chatBotService.updateUserSelections(newUserSelection); }
	 */

	/**
	 * @param locale
	 * @param bundleDetails
	 * @return
	 
	public String getCommercialNameAndRenewalDateForMainBundle(String locale, JSONObject bundleDetails) {
		String comercialName, renwalDate, details;
		details = Constants.EMPTY_STRING;
		boolean isHasRenewalDate = true;
		if (bundleDetails.get(Constants.JSON_KEY_RENEWAL_DATE).equals(null)) {
			isHasRenewalDate = false;
		}
		if (isHasRenewalDate) {
			if (locale.contains(Constants.LOCALE_AR)) {
				comercialName = bundleDetails.getJSONObject(Constants.JSON_KEY_COMMERCIAL_NAME).getString(Constants.JSON_KEY_VALUE_AR);
				renwalDate = bundleDetails.getJSONObject(Constants.JSON_KEY_RENEWAL_DATE).getString(Constants.JSON_KEY_VALUE_AR);
				details = " باقتك هي  " + comercialName + " تاريخ التجديد " + renwalDate;
			} else {
				comercialName = bundleDetails.getJSONObject(Constants.JSON_KEY_COMMERCIAL_NAME).getString(Constants.JSON_KEY_VALUE_EN);
				renwalDate = bundleDetails.getJSONObject(Constants.JSON_KEY_RENEWAL_DATE).getString(Constants.JSON_KEY_VALUE_EN);
				details = "Your Bundle is " + comercialName + " and Renewal Date is " + renwalDate;
			}
			return details;
		} else {
			if (locale.contains(Constants.LOCALE_AR)) {
				comercialName = bundleDetails.getJSONObject(Constants.JSON_KEY_COMMERCIAL_NAME).getString(Constants.JSON_KEY_VALUE_AR);
				details = " باقتك هي  " + comercialName;
			} else {
				comercialName = bundleDetails.getJSONObject(Constants.JSON_KEY_COMMERCIAL_NAME).getString(Constants.JSON_KEY_VALUE_EN);

				details = "Your Bundle is " + comercialName;
			}
			return details;
		}

	}
*/
	public void takeThreadControl(final String senderId, Messenger messenger) {
		logger.debug("PASS THREAD CONTROL TO BOT");
		HandoverPayload handoverPayload = HandoverPayload.create(senderId, HandoverAction.take_thread_control, "", "information");
		try {
			messenger.handover(handoverPayload);
		} catch (MessengerApiException | MessengerIOException io) {
			logger.error(Constants.LOGGER_DIAL_IS + "callSecondryHandover" + Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_EXCEPTION_MESSAGE + io);
			io.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> getFromCachDependOnWebService(String url, String dial) {
		Map<String, String> mapResponse = new HashMap<>();
		if (url.contains(Constants.URL_KEYWORD_PROFILE)) {
			mapResponse = (Map<String, String>) wsResponseCache.getCachedValue(dial + Constants.CACHED_MAP_PROFILE_KEY_SUFFIX);
		} else if (url.contains(Constants.URL_KEYWORD_BUNDLE)) {
			mapResponse = (Map<String, String>) wsResponseCache.getCachedValue(dial + Constants.CACHED_MAP_ELIGIPLE_PRODUCT_KEY_SUFFIX);
		} else if (url.contains(Constants.URL_KEYWORD_EXTRA)) {
			mapResponse = (Map<String, String>) wsResponseCache.getCachedValue(dial + Constants.CACHED_MAP_ELIGIPLE_EXTRA_KEY_SUFFIX);
		}
		return mapResponse;
	}

	// Mobile Internet Bundle Details [Commercial Name , Renewal Data]
	// Eisa 
	/*String createMainBundleDetails(JSONArray ratePlanArray, String locale) {
		String details = Constants.EMPTY_STRING;
		for (int i = 0; i < ratePlanArray.length(); i++) {
			JSONObject bundleDetails = ratePlanArray.getJSONObject(i);
			details = getCommercialNameAndRenewalDateForMainBundle(locale, bundleDetails);
		}

		return details;
	}*/

	/**
	 * @param payload
	 * @param messenger
	 * @param senderId
	 * @param userSelections
	 * @param botWebserviceMessage
	 * @param response
	 * @param productIdAndOperationName
	 * @return
	 */
	public String postWebServicehandling(String payload, Messenger messenger, String senderId, UserSelection userSelections, BotWebserviceMessage botWebserviceMessage, String response,
			String productIdAndOperationName) {
		Map<String, String> mapResponse;
		// POST
		JSONObject jsonParam = new JSONObject();
		ArrayList<String> paramValuesList = new ArrayList<>();
		if (payload.equals("rasa")) {// Rasa Integration
			String paramKey = botWebserviceMessage.getListParamName();
			JSONObject jsonResponse = utilService.rasaIntegration(botWebserviceMessage.getWsUrl(), userSelections.getFreeText(), paramKey);
			if (!jsonResponse.keySet().isEmpty()) {
				// JSONArray intentRanking = jsonResponse.getJSONArray("intentRanking");
				JSONObject intentObject = jsonResponse.getJSONObject("intent");
				logger.debug("Intent Ranking " + intentObject);
				/*
				 * Map<String, Double> map = new HashMap<>(); for (int i = 0; i <
				 * intentRanking.length(); i++) { JSONObject intentObject =
				 * intentRanking.getJSONObject(i); double conf =
				 * intentObject.getDouble("confidence"); String name =
				 * intentObject.getString("name"); if (Math.round(conf) > 50) { map.put(name,
				 * conf); } }
				 */
				/*
				 * JSONObject intent = jsonResponse.getJSONObject("intent"); String name =
				 * intent.getString("name");
				 * logger.debug("Rasa Response Interaction Name "+name);
				 */
				handlePayload(intentObject.getString("name"), messenger, senderId);
			} else {
				handlePayload(Constants.PAYLOAD_UNEXPECTED_PAYLOAD, messenger, senderId);
			}
		} else {// Buy Product Bundles (Connect & Super Connect)
			String[] paramNames = botWebserviceMessage.getListParamName().split(Constants.COMMA_CHAR);
			if (productIdAndOperationName.length() > 0
					&& (payload.equalsIgnoreCase(Constants.PAYLOAD_RELATED_PRODUCT_SUBSCRIPTION_CONFIRM) || payload.equalsIgnoreCase(Constants.PREFIX_CONFIRM_MOBILEINTERNET_SUBSCRIPTION))) {
				logger.debug(Constants.MI_BUNDLE_SUBSCRIPTION);
				if (paramNames.length == 2) {
					paramValuesList = new ArrayList<>(Arrays.asList(userSelections.getProductIdAndOperationName().split(Constants.COMMA_CHAR)));
				} else if (paramNames.length == 3) {
					paramValuesList = new ArrayList<>(Arrays.asList(userSelections.getParametersListForRelatedProducts().split(Constants.COMMA_CHAR)));
				}
				jsonParam = utilService.setRequestBodyValueForPostCalling(paramValuesList, paramNames);
			} else if (!payload.equals(Constants.PREFIX_BUNDLE_UNSUBSCRIPTION) && !payload.equals(Constants.PAYLOAD_ADDON_SUBSCRIPTION) && !payload.equalsIgnoreCase(Constants.PAYLOAD_MIGRATE_BY_ID)
					&& !payload.equalsIgnoreCase(Constants.PAYLOAD_MIGRATE_BY_NAME)) {// Renew Subscribed Bundle
				logger.debug(Constants.RENEW_BUNDLE_ACTION);
				if (paramNames.length == 2) {
					paramValuesList = new ArrayList<>(Arrays.asList(userSelections.getProductIdForRenew().split(Constants.COMMA_CHAR)));
				}
				jsonParam = utilService.setRequestBodyValueForPostCalling(paramValuesList, paramNames);
			} else if (payload.equals(Constants.PREFIX_BUNDLE_UNSUBSCRIPTION)) {// MI Bundle Unsubscription
				logger.debug(Constants.RENEW_BUNDLE_ACTION);
				if (paramNames.length == 2) {
					paramValuesList = new ArrayList<>(Arrays.asList(userSelections.getProductIdForRenew().split(Constants.COMMA_CHAR)));
					paramValuesList.add(Constants.UNSUBSCRIBE_OPERATION_VALUE);
				}
				jsonParam = utilService.setRequestBodyValueForPostCalling(paramValuesList, paramNames);
			} else if (userSelections.getAddonId() != null && userSelections.getAddonId().length() > 0 && payload.equals(Constants.PAYLOAD_ADDON_SUBSCRIPTION)) {// Add-on Subscription
				logger.debug(Constants.ADDON_SUBSCRIPTION_ACTION);
				if (paramNames.length == 2) {
					paramValuesList = new ArrayList<>(Arrays.asList(userSelections.getAddonId().split(Constants.COMMA_CHAR)));
				}
				jsonParam = utilService.setRequestBodyValueForPostCalling(paramValuesList, paramNames);
			} else if (payload.contains(Constants.MIGRATE_BY_PREFIX)) {
				jsonParam = migrationService.ratePlanMigration(paramNames, payload, userSelections, paramValuesList);
			}
			String stringParam = jsonParam.toString();
			mapResponse = utilService.buyProductOrAddon(botWebserviceMessage, stringParam, chatBotService, senderId);
			if (mapResponse.get(Constants.RESPONSE_STATUS_KEY).equals("200")) {
				response = mapResponse.get(Constants.RESPONSE_KEY);
			}
		}
		return response;
	}

	private MessagePayload createBillingProfileInformationMessage(String userLocale, String senderId, String billAmount) {
		String text = "";
		MessagePayload messagePayload = null;
		if (userLocale == null) {
			userLocale = Constants.LOCALE_EN;
		}
		if (userLocale.contains(Constants.LOCALE_AR)) {
			text = "قيمة فاتورتك الحالية " + billAmount;
			messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(text));
		} else {
			text = "Your current bill amount is " + billAmount;
			messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(text));
		}
		return messagePayload;
	}

	private String createNOBillingProfileMessage(String userLocale) {
		if (userLocale.contains(Constants.LOCALE_AR)) {
			return "0 قيمة فاتورتك الحالية";

		} else {
			return "Your current bill amount is 0";

		}
	}

	/**
	 * @param userLocale
	 * @param connect
	 * @param consumptionNames
	 */
	public void setConsumptionNamesList(String userLocale, JSONArray consumption, ArrayList<String> consumptionNames) {

		for (int i = 0; i < consumption.length(); i++) {
			JSONArray consumptionDetailsList = consumption.getJSONObject(i).getJSONArray(Constants.JSON_KEY_CONSUMPTION_DETAILS_LIST); // Amin
			String baseConsumptionName = getAddonCommercialName(userLocale, consumption, i);
			for (int c = 0; c < consumptionDetailsList.length(); c++) {
				String consumptionName = "";
				if (userLocale.contains(Constants.LOCALE_AR)) {
					consumptionName = consumptionDetailsList.getJSONObject(c).getJSONObject(Constants.JSON_KEY_CONSUMPTION_NAME).get(Constants.JSON_KEY_LABEL_AR) == null ? Constants.UNDERSCORE
							: consumptionDetailsList.getJSONObject(c).getJSONObject(Constants.JSON_KEY_CONSUMPTION_NAME).getString(Constants.JSON_KEY_LABEL_AR);
				} else {
					consumptionName = consumptionDetailsList.getJSONObject(c).getJSONObject(Constants.JSON_KEY_CONSUMPTION_NAME).get(Constants.JSON_KEY_LABEL_EN) == null ? Constants.UNDERSCORE
							: consumptionDetailsList.getJSONObject(c).getJSONObject(Constants.JSON_KEY_CONSUMPTION_NAME).getString(Constants.JSON_KEY_LABEL_EN);
				}
				consumptionNames.add(baseConsumptionName + consumptionName);
			}
		}
	}

	/**
	 * @param userLocale
	 */
	public String getAddonCommercialName(String userLocale, JSONArray consumption, int i) {
		if (userLocale.contains(Constants.LOCALE_AR)) {
			return consumption.getJSONObject(i).getJSONObject(Constants.JSON_KEY_COMMERCIAL_NAME).getString(Constants.JSON_KEY_VALUE_AR) + " - ";
		} else {
			return consumption.getJSONObject(i).getJSONObject(Constants.JSON_KEY_COMMERCIAL_NAME).getString(Constants.JSON_KEY_VALUE_EN) + " - ";
		}
	}

	public void setConsumptionNamesListForMainBundles(String userLocale, JSONArray consumption, ArrayList<String> consumptionNames) {
		for (int i = 0; i < consumption.length(); i++) {
			JSONArray consumptionDetailsList = consumption.getJSONObject(i).getJSONArray(Constants.JSON_KEY_CONSUMPTION_DETAILS_LIST);
			for (int c = 0; c < consumptionDetailsList.length(); c++) {
				String consumptionName = "";
				if (userLocale.contains(Constants.LOCALE_AR)) {
					consumptionName = consumptionDetailsList.getJSONObject(c).getJSONObject(Constants.JSON_KEY_CONSUMPTION_NAME).get(Constants.JSON_KEY_LABEL_AR) == null ? Constants.UNDERSCORE
							: consumptionDetailsList.getJSONObject(c).getJSONObject(Constants.JSON_KEY_CONSUMPTION_NAME).getString(Constants.JSON_KEY_LABEL_AR);
				} else {
					consumptionName = consumptionDetailsList.getJSONObject(c).getJSONObject(Constants.JSON_KEY_CONSUMPTION_NAME).get(Constants.JSON_KEY_LABEL_EN) == null ? Constants.UNDERSCORE
							: consumptionDetailsList.getJSONObject(c).getJSONObject(Constants.JSON_KEY_CONSUMPTION_NAME).getString(Constants.JSON_KEY_LABEL_EN);
				}
				consumptionNames.add(consumptionName);
			}
		}
	}

	private void sendMultipleMessages(ArrayList<MessagePayload> responses, String senderId, Messenger messenger) {
		for (MessagePayload response : responses) {
			try {
				messenger.send(response);
			} catch (MessengerApiException | MessengerIOException e) {
				logger.error(Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_EXCEPTION_MESSAGE + e);
				e.printStackTrace();
			}
			Utils.markAsTypingOff(messenger, senderId);
		}

	}

	private void putInCachDependOnWebService(String url, Map<String, String> response, String dial) {
		if (url.contains(Constants.URL_KEYWORD_PROFILE)) {
			wsResponseCache.addToCentralCache(dial + Constants.CACHED_MAP_PROFILE_KEY_SUFFIX, response);
		} else if (url.contains(Constants.URL_KEYWORD_BUNDLE)) {
			wsResponseCache.addToCentralCache(dial + Constants.CACHED_MAP_ELIGIPLE_PRODUCT_KEY_SUFFIX, response);
		} else if (url.contains(Constants.URL_KEYWORD_EXTRA)) {
			wsResponseCache.addToCentralCache(dial + Constants.CACHED_MAP_ELIGIPLE_EXTRA_KEY_SUFFIX, response);
		}
	}

}
