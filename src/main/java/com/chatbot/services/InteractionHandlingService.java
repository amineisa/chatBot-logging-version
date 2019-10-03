package com.chatbot.services;

import static java.util.Optional.empty;

import java.util.ArrayList;
import java.util.Arrays;
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

import com.chatbot.controller.ChatBotController;
import com.chatbot.entity.BotButton;
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
import com.github.messenger4j.send.BroadCastMessageCreation;
import com.github.messenger4j.send.HandoverAction;
import com.github.messenger4j.send.HandoverPayload;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.MessagingType;
import com.github.messenger4j.send.message.Message;
import com.github.messenger4j.send.message.TemplateMessage;
import com.github.messenger4j.send.message.TextMessage;
import com.github.messenger4j.send.message.quickreply.QuickReply;
import com.github.messenger4j.send.message.template.ButtonTemplate;
import com.github.messenger4j.send.message.template.Template;
import com.github.messenger4j.send.message.template.button.Button;
import com.github.messenger4j.userprofile.UserProfile;

/**
 * @author Amin Eisa
 */
@Service
public class InteractionHandlingService {

	@Autowired
	private UtilService utilService;
	@Autowired
	private ChatBotService chatBotService;
	@Autowired
	private MigrationService migrationService;
	@Autowired
	private GenericTemplateService genericTemplateService;
	@Autowired
	private PostPaidService postPaidService;
	@Autowired
	private QuickReplyService quickReplyService;
	@Autowired
	RasaIntegrationService rasaIntegration;
	@Autowired
	SallefnyService sallefnyService;
	@Autowired
	AuthService authService;
	@Autowired
	RechargeService rechargeService;
	@Autowired
	AkwaKartService akwaKartService;
	@Autowired
	BalanceDeductionService balanceDeductionService;
	private static CacheHelper<String, Object> wsResponseCache = new CacheHelper<>("usersResponses");

	private static final Logger logger = LoggerFactory.getLogger(ChatBotController.class);

	/**
	 * Payload Setting to change payload value at runtime according to interaction
	 * 
	 * @param payload
	 *            which received from Bot
	 * @param senderId
	 *            used for update user cached values
	 * @return return new payload's value
	 */
	private String payloadSetting(String payload, String senderId) {
		UserSelection userSelections = utilService.getUserSelectionsFromCache(senderId);
		if (payload.startsWith(Constants.PREFIX_ADDONSUBSCRIPE)) {// addonId
			userSelections.setAddonId(payload.substring(9, payload.length()) + Constants.COMMA_CHAR + "ACTIVATE");
			payload = Constants.PAYLOAD_ADDON_SUBSCRIPTION;
			utilService.updateUserSelectionsInCache(senderId, userSelections);
		}
		if (payload.startsWith(Constants.PREFIX_RATEPLAN_SUBSCRIPTION)) {// productIdAndOperationName
			userSelections.setProductIdAndOperationName(payload.substring(4, payload.length()));
			utilService.updateUserSelectionsInCache(senderId, userSelections);
			payload = payload.substring(0, 4);
		} else if (payload.startsWith(Constants.PREFIX_MOBILEINTERNET_ADDON)) {// addonId for MI
			userSelections.setAddonId(payload.substring(7, payload.length()));
			utilService.updateUserSelectionsInCache(senderId, userSelections);
			payload = Constants.PAYLOAD_BUY_ADDONS;
		} else if (payload.startsWith(Constants.PREFIX_RELATED_PRODUCTS)) {// productIdAndOperationName which has related products
			String[] params = payload.split(Constants.COMMA_CHAR);
			String allParameters = params[1] + "," + params[2];
			userSelections.setParametersListForRelatedProducts(allParameters);
			utilService.updateUserSelectionsInCache(senderId, userSelections);
			payload = Constants.PAYLOAD_RELATED_PRODUCTS;
		} else if (payload.equalsIgnoreCase(Constants.PREFIX_CONFIRM_MOBILEINTERNET_SUBSCRIPTION)) {
			String stringParametersListForRelatedProducts = userSelections.getParametersListForRelatedProducts();
			if (stringParametersListForRelatedProducts != null) {
				String[] parametersListForRelatedProducts = stringParametersListForRelatedProducts.split(",");
				logger.debug(Constants.LOGGER_INFO_PREFIX+"User Selection" + userSelections.toString());
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
			String parametersListForRelatedProducts = Constants.EMPTY_STRING;
			for (int i = 0; i < parameters.length; i++) {
				parametersListForRelatedProducts = parameters[i] + Constants.COMMA_CHAR;
			}
			userSelections.setParametersListForRelatedProducts(parametersListForRelatedProducts);
			payload = Constants.PAYLOAD_MOBILE_INTERNET_CONFIRMATION_MSG;
			utilService.updateUserSelectionsInCache(senderId, userSelections);
		} else if (payload.contains(Constants.PREFIX_RELATED_PRODUCTS_SUBSCRIPTION)) {
			String relatedId = payload.substring(0, payload.indexOf(','));
			String[] parameterArry = userSelections.getParametersListForRelatedProducts().split(Constants.COMMA_CHAR);
			if (parameterArry.length < 3) {
				String stingParametersListForRelatedProducts = userSelections.getParametersListForRelatedProducts() + Constants.COMMA_CHAR + relatedId;
				userSelections.setParametersListForRelatedProducts(stingParametersListForRelatedProducts);
				utilService.updateUserSelectionsInCache(senderId, userSelections);
			}
			payload = Constants.PAYLOAD_MOBILE_INTERNET_CONFIRMATION_MSG;
		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_MOBILE_INTERNET_SUBSCRIPTION_CANCEL)) {
			payload = Constants.PAYLOAD_CHANGE_BUNDLE;
		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_CANCEL_RECHARGING) || payload.equalsIgnoreCase(Constants.PAYLOAD_CANCEL_BILL_PAYMENT)) {
			payload = Constants.PAYLOAD_CANCEL_PAY_OR_RECHARGE;
		} else if (payload.contains(Constants.PREFIX_MIGRATE_ID)) {
			userSelections.setRateplanIdForMigration(Integer.parseInt(payload.split(Constants.COMMA_CHAR)[1]));
			utilService.updateUserSelectionsInCache(senderId, userSelections);
			payload = Constants.PAYLOAD_CONFIRMATION_MIGRATE;
		} else if (payload.contains(Constants.PREFIX_MIGRATE_NAME)) {
			userSelections.setRateplanNameForMigration(payload.split(Constants.COMMA_CHAR)[1]);
			utilService.updateUserSelectionsInCache(senderId, userSelections);
			payload = Constants.PAYLOAD_CONFIRMATION_MIGRATE;
		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_MIGRATE_CONFIRM)) {
			if (userSelections.getRateplanIdForMigration() != 0) {
				payload = Constants.PAYLOAD_MIGRATE_BY_ID;
			} else if (userSelections.getRateplanNameForMigration() != null) {
				payload = Constants.PAYLOAD_MIGRATE_BY_NAME;
			}
		} else if (payload.startsWith(Constants.PREFIX_SALLEFNY_INTERACTION)) {
			String productName = payload.substring(5, payload.length());
			userSelections.setProductNameForSallefny(productName);
			utilService.updateUserSelectionsInCache(senderId, userSelections);
			payload = Constants.PREFIX_SALLEFNY_INTERACTION;
		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_AKWAKART_CATEGORY_MIX) || payload.equalsIgnoreCase(Constants.PAYLOAD_AKWAKART_CATEGORY_MIN)) {
			userSelections.setAkwaKartCategoryName(payload);
			utilService.updateUserSelectionsInCache(senderId, userSelections);
			payload = "recharge now";
			// Constants.PAYLOAD_RECHARGE;
		} else if (payload.startsWith(Constants.PAYLOAD_RREFIX_TESLA)) {
			userSelections.setAkwakartProductName(payload);
			utilService.updateUserSelectionsInCache(senderId, userSelections);
			payload = Constants.PAYLOAD_RREFIX_TESLA;

		} else if (payload.startsWith(Constants.PREFIX_DEDUCTION_DURATION)) {
			userSelections.setAccountDeductionHistory(Integer.parseInt(payload.substring(payload.indexOf('_') + 1, payload.length())));
			utilService.updateUserSelectionsInCache(senderId, userSelections);
			payload = Constants.PAYLOAD_BALANCE_DEDUCTION;
		}
		return payload;
	}

	/**
	 * for payload handling and to get interaction according to payload's value
	 * 
	 * @param payload
	 * @param messenger
	 * @param senderId
	 */
	public void handlePayload(String payload, Messenger messenger, String senderId) {
		String userFirstName, userLocale, lastName;
		userFirstName = userLocale = lastName = Constants.EMPTY_STRING;
		Utils.markAsTypingOn(messenger, senderId);
		CustomerProfile customerProfile = Utils.saveCustomerInformation(chatBotService, senderId, userLocale, userFirstName, lastName);
		logger.debug(Constants.LOGGER_INFO_PREFIX+"Payload value is " + payload);
		if (payload.equals(Constants.PAYLOAD_TALK_TO_AGENT)) {
			String phoneNumber = " _ ";
			if (customerProfile != null) {
				phoneNumber = customerProfile.getMsisdn();
			}
			logger.debug(Constants.LOGGER_INFO_PREFIX+"Send thread control to second app ");
			callSecondryHandover(senderId, phoneNumber, messenger);
		} else if (payload != null) {
			if (payload.equalsIgnoreCase(Constants.PAYLOAD_GET_STARTED)) {
				UserProfile userProfile = Utils.getUserProfile(senderId, messenger);
				userFirstName = userProfile.firstName() == null ? Constants.EMPTY_STRING : userProfile.firstName();
				lastName = userProfile.lastName() == null ? Constants.EMPTY_STRING : userProfile.lastName();
				userLocale = userProfile.locale() == null ? Constants.LOCALE_EN : userProfile.locale();
			} else {
				payload = payloadSetting(payload, senderId);
			}
			String phoneNumber = Constants.EMPTY_STRING;
			userLocale = customerProfile.getLocale() == null ? Constants.LOCALE_EN : customerProfile.getLocale();
			logger.debug(Constants.LOGGER_INFO_PREFIX+"Handle payload for customer " + customerProfile.toString());
			try {
				ArrayList<MessagePayload> messagePayloadList = new ArrayList<>();
				if (payload.equalsIgnoreCase("broadcast")) {
					logger.debug(Constants.LOGGER_INFO_PREFIX+"Sending broadcast message");
					List<Message> messages = new ArrayList<>();
					TextMessage textMsg = TextMessage.create("Hi , All Friends ");
					messages.add(textMsg);
					BroadCastMessageCreation broadCastMessage = BroadCastMessageCreation.create(messages);
					try {
						logger.debug(Constants.LOGGER_INFO_PREFIX+"Send  BroadCast Messanger MSG ");
						messenger.sendBroadCastMessage(broadCastMessage);
					} catch (MessengerApiException | MessengerIOException e) {
						e.printStackTrace();
					}
				} else if (payload.equalsIgnoreCase(Constants.LOCALE_EN) || payload.equalsIgnoreCase(Constants.LOCALE_AR))// Locale Setting
				{
					messagePayloadList = utilService.userlocaleSetting(customerProfile, senderId, payload);
					sendMultipleMessages(messagePayloadList, senderId, messenger, null);
				} else {
					BotInteraction botInteraction = chatBotService.findInteractionByPayload(payload);
					if (botInteraction == null && !payload.equals(Constants.PAYLOAD_MIGRATE_MORE)) { // Rasa integration in case free text.
						String rasaEnabled = chatBotService.getBotConfigurationByKey(Constants.RASA_ENABLED_KEY).getValue() == null ? "false"
								: chatBotService.getBotConfigurationByKey(Constants.RASA_ENABLED_KEY).getValue();
						logger.debug(Constants.LOGGER_INFO_PREFIX+"Is Rasa Enabled " + rasaEnabled);
						boolean rasa = Boolean.parseBoolean(rasaEnabled);
						if (rasa) {
							logger.debug(Constants.LOGGER_INFO_PREFIX + " { rasa is enabled } ");
							String rasaPayload = rasaIntegration.rasaChannel(senderId, payload);
							handlePayload(rasaPayload, messenger, senderId);
						} else {
							logger.debug(Constants.LOGGER_INFO_PREFIX + " { Unexpected interaction is enabled } ");
							handlePayload(Constants.PAYLOAD_UNEXPECTED_PAYLOAD, messenger, senderId);
						}
					} else if (payload.equals(Constants.PAYLOAD_MIGRATE_MORE)) {// Rateplan Migration in case retrieved rateplans more than 10
						migrationInCaseMorethanTenRateplans(senderId, messenger, customerProfile);
					} else { // All Other Normal Interactions flow.
						InteractionLogging interactionLogging = Utils.interactionLogginghandling(customerProfile, botInteraction, chatBotService);
						phoneNumber = customerProfile.getMsisdn() != null ? customerProfile.getMsisdn() : Constants.EMPTY_STRING;
						if (botInteraction != null && payload.equalsIgnoreCase(Constants.NEW_PAYLOAD_LOGIN_INTERACTION) || payload.equals(Constants.PAYLOAD_LOGIN_INTERACTION)
								|| !botInteraction.getIsSecure() || phoneNumber.length() > 0) {
							handleAuthenticatedUser(customerProfile, payload, messenger, messagePayloadList, interactionLogging);
						} else { // In case Secured Interaction which need authentication before interact with
							handleUnauthenticatedUser(payload, senderId, messenger);
						}
					}
				}
			} catch (Exception e) {
				logger.error(Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_EXCEPTION_MESSAGE + e);
				e.printStackTrace();
			}
		}

	}

	/**
	 * Authenticated User interactions handling
	 * 
	 * @param customerProfile
	 * @param payload
	 * @param messenger
	 * @param interactionLogging
	 *            for interaction logging in database
	 */
	private void handleAuthenticatedUser(CustomerProfile customerProfile, String payload, Messenger messenger, ArrayList<MessagePayload> messagePayloadList, InteractionLogging interactionLogging) {
		String senderId = customerProfile.getSenderID();
		String phoneNumber = customerProfile.getMsisdn();
		BotInteraction botInteraction = interactionLogging.getBotInteraction();
		UserSelection userSelections = utilService.getUserSelectionsFromCache(senderId);
		setParentPayloadValueInCache(userSelections, botInteraction, senderId);
		List<BotInteractionMessage> interactionMessageList = chatBotService.findInteractionMessagesByInteractionId(botInteraction.getInteractionId());
		MessagePayload messagePayload = null;
		for (BotInteractionMessage botInteractionMessage : interactionMessageList) {
			if (botInteractionMessage.getIsStatic()) {
				messagePayload = responseInCaseStaticScenario(customerProfile, payload, botInteractionMessage);
				messagePayloadList.add(messagePayload);
			} else {// Dynamic Scenario
				logger.debug(Constants.LOGGER_INFO_PREFIX+Constants.LOGGER_DIAL_IS + phoneNumber + Constants.LOGGER_METHOD_NAME + " dynamicScenario and Interaction is " + botInteraction.toString());
				dynamicScenarioController(customerProfile, payload, messenger, messagePayloadList, botInteractionMessage);
			}
		}
		if (!messagePayloadList.isEmpty()) {
			sendMultipleMessages(messagePayloadList, senderId, messenger, interactionLogging);
		}
		String parentPayLoad = getParentPayloadValueFromCache(senderId, payload);
		if (parentPayLoad.length() > 0) {
			userSelections.setParentPayLoad(null);
			utilService.updateUserSelectionsInCache(senderId, userSelections);
			handlePayload(parentPayLoad, messenger, senderId);

		}
	}

	/**
	 * Handle unauthenticated user to display login template
	 * 
	 * @param payload
	 *            the original payload which received from user before log in
	 * @param senderId
	 *            to get user cached value for specific user
	 * @param messenger
	 * 
	 */
	private void handleUnauthenticatedUser(String payload, String senderId, Messenger messenger) {
		UserSelection userSelections = utilService.getUserSelectionsFromCache(senderId);
		userSelections.setOriginalPayLoad(payload);
		logger.debug(Constants.LOGGER_INFO_PREFIX + " Orignal payload for unauthenticated user is "+payload);
		utilService.updateUserSelectionsInCache(senderId, userSelections);
		handlePayload(Constants.LOGGER_INFO_PREFIX+Constants.PAYLOAD_LOGIN_INTERACTION, messenger, senderId);
	}

	/**
	 * Save || update parent payload for interaction at user selection caching
	 * 
	 * @param userSelections
	 * @param botInteraction
	 *            to get parent payload value from it.
	 * @param senderId
	 *            for get specific userSelection according to senderId from cached
	 *            map.
	 */
	private void setParentPayloadValueInCache(UserSelection userSelections, BotInteraction botInteraction, String senderId) {
		if (botInteraction.getParentPayLoad() != null && userSelections != null) {
			userSelections.setParentPayLoad(botInteraction.getParentPayLoad());
			utilService.updateUserSelectionsInCache(senderId, userSelections);
		}
	}

	/**
	 * Retrieve parent payload from cached map
	 * 
	 * @param senderId
	 * @param payload
	 * @return parent payload's value
	 */
	private String getParentPayloadValueFromCache(String senderId, String payload) {
		UserSelection userSelections = utilService.getUserSelectionsFromCache(senderId);
		if (userSelections != null && !payload.equalsIgnoreCase(Constants.PAYLOAD_CHANGE_BUNDLE)) {
			return userSelections.getParentPayLoad() == null ? Constants.EMPTY_STRING : userSelections.getParentPayLoad();
		} else {
			return Constants.EMPTY_STRING;
		}
	}

	/**
	 * Rateplan migration in case Dashboard response contains more than 10 rateplan
	 * 
	 * @param senderId
	 * @param messenger
	 * @param customerProfile
	 */
	private void migrationInCaseMorethanTenRateplans(String senderId, Messenger messenger, CustomerProfile customerProfile) {
		ArrayList<MessagePayload> messagePayloadList = new ArrayList<>();
		Template template = migrationService.displayMigrationRatePlans(customerProfile.getMsisdn(), customerProfile.getLocale(), new JSONArray());
		MessagePayload messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(template));
		messagePayloadList.add(messagePayload);
		sendMultipleMessages(messagePayloadList, senderId, messenger, null);
	}

	/**
	 * handle method for dynamic scenario web service calling (POST , GET)
	 * 
	 * @param customerProfile`
	 * @param payload
	 * @param messenger
	 * @param messagePayloadList
	 * @param messageTypeId
	 * @param messageId
	 * 
	 */
	private void dynamicScenarioController(CustomerProfile customerProfile, String payload, Messenger messenger, ArrayList<MessagePayload> messagePayloadList,
			BotInteractionMessage botInteractionMessage) {
		Long messageId = botInteractionMessage.getMessageId();
		String senderId = customerProfile.getSenderID();
		BotWebserviceMessage botWebserviceMessage = chatBotService.findWebserviceMessageByMessageId(messageId);
		String response = Constants.EMPTY_STRING;
		Map<String, String> mapResponse = new HashMap<>();
		if (payload.equals(Constants.RASA_PAYLOAD)) {
			rasaIntegration.rasaCallingForFreeTextHandling(messenger, senderId, botWebserviceMessage);
		} else {
			if (botWebserviceMessage.getBotMethodType().getMethodTypeId() == 1) {// GET
				mapResponse = getWebServiceHandling(messagePayloadList, payload, botInteractionMessage, customerProfile);
			} else if (botWebserviceMessage.getBotMethodType().getMethodTypeId() == 2) {// POST
				mapResponse = postWebServiceRequestBodyCreationAndCalling(payload, messenger, senderId, botWebserviceMessage);
			}
			if (mapResponse.size() > 0 && mapResponse.get(Constants.RESPONSE_STATUS_KEY).equals("200")) {
				response = mapResponse.get(Constants.RESPONSE_KEY);
				logger.debug(Constants.LOGGER_INFO_PREFIX+"WS Response Status is " + mapResponse.get(Constants.RESPONSE_STATUS_KEY));
				logger.debug(Constants.LOGGER_INFO_PREFIX+"WS Response is " + response);
				if (payload.equalsIgnoreCase(Constants.PAYLOAD_VERIFICATION_CODE)) {
					UserSelection userSelections = utilService.getUserSelectionsFromCache(senderId);
					utilService.setLinkingInfoForCustomer(senderId, userSelections.getUserDialForAuth());
					handlePayload(userSelections.getOriginalPayLoad(), messenger, senderId);
				} else {
					templatesTypeHandling(customerProfile, payload, messenger, messagePayloadList, senderId, botWebserviceMessage, response);
				}
			} else {
				handlePayload(Constants.PAYLOAD_FAULT_MSG, messenger, senderId);
			}
		}
	}

	/**
	 * Response Handling in case static response just for get response from database
	 * without any Dashboard integration
	 * 
	 * @param customerProfile
	 * @param payload
	 * @param botInteractionMessage
	 * @return Messenjer4j MessagePayload according to its type
	 */
	public MessagePayload responseInCaseStaticScenario(CustomerProfile customerProfile, String payload, BotInteractionMessage botInteractionMessage) {
		Long messageTypeId = botInteractionMessage.getBotMessageType().getMessageTypeId();
		Long messageId = botInteractionMessage.getMessageId();
		String userDial = customerProfile.getMsisdn();
		String locale = customerProfile.getLocale();
		String senderId = customerProfile.getSenderID();
		String userFirstName = customerProfile.getFirstName();
		String text = Constants.EMPTY_STRING;
		MessagePayload messagePayload = null;
		logger.debug(Constants.LOGGER_DIAL_IS + userDial + " Method Name is Static Scenario Message is " + botInteractionMessage.toString());
		// text message
		if (messageTypeId == Utils.MessageTypeEnum.TEXTMESSAGE.getValue()) {
			BotTextMessage botTextMessage = chatBotService.findTextMessageByMessageId(messageId);
			text = utilService.getTextValueForBotTextMessage(botTextMessage, locale, userFirstName, userDial);
			messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(text));

		}
		// quick reply
		else if (messageTypeId == Utils.MessageTypeEnum.QUICKREPLYMESSAGE.getValue()) {
			BotQuickReplyMessage botQuickReplyMessage = chatBotService.findQuickReplyMessageByMessageId(messageId);
			text = quickReplyService.getTextForQuickReply(locale, botQuickReplyMessage, userFirstName, userDial);
			List<QuickReply> quickReplies = quickReplyService.createQuickReply(messageId, locale, chatBotService);
			Optional<List<QuickReply>> quickRepliesOp = Optional.of(quickReplies);
			messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(text, quickRepliesOp, empty()));
		}
		// generic template
		else if (messageTypeId == Utils.MessageTypeEnum.GENERICTEMPLATEMESSAGE.getValue()) {
			Template template = genericTemplateService.createGenericTemplate(messageId, chatBotService, customerProfile, new BotWebserviceMessage(), new JSONObject(), new ArrayList<String>(),
					payload);
			messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(template));
			// ButtonTemplate
		} else if (messageTypeId == Utils.MessageTypeEnum.ButtonTemplate.getValue()) {
			ButtonTemplate buttonTemplate = utilService.createButtonTemplateInScenario(botInteractionMessage, locale, userDial);
			messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(buttonTemplate));
		}
		return messagePayload;
	}

	/**
	 * Set response value as Messenger Template
	 * 
	 * @param customerProfile
	 * @param payload
	 * @param messenger
	 * @param messagePayloadList
	 * @param senderId
	 * @param botWebserviceMessage
	 * @param response
	 */
	public void templatesTypeHandling(CustomerProfile customerProfile, String payload, Messenger messenger, ArrayList<MessagePayload> messagePayloadList, String senderId,
			BotWebserviceMessage botWebserviceMessage, String response) {
		String phoneNumber = customerProfile.getMsisdn();
		String userLocale = customerProfile.getLocale();
		Long messageTypeId = botWebserviceMessage.getBotInteractionMessage().getBotMessageType().getMessageTypeId();
		Long messageId = botWebserviceMessage.getBotInteractionMessage().getMessageId();
		if (messageTypeId != 0 && messageTypeId == Utils.MessageTypeEnum.TEXTMESSAGE.getValue()) {// Text Message
			utilService.createTextMessageInDynamicScenario(payload, senderId, messagePayloadList, botWebserviceMessage, response, userLocale);
		} else if (messageTypeId != 0 && messageTypeId == Utils.MessageTypeEnum.ButtonTemplate.getValue()) {// Button Template
			if (botWebserviceMessage.getOutType().getInOutTypeId() == 1) {// String
			} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 2) {// Object
			} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 3) {// Array
				buttonTemplateHandling(senderId, userLocale, messagePayloadList, phoneNumber, botWebserviceMessage, response);
			}
		} else if (messageTypeId != 0 && messageTypeId == Utils.MessageTypeEnum.GENERICTEMPLATEMESSAGE.getValue()) {// Generic Template
			if (payload.equals(Constants.PAYLOAD_RATEPLAN_DETAILS)) {
				JSONObject baseResponse = new JSONObject(response);
				JSONArray consumptionDetailsList = baseResponse.getJSONArray(Constants.JSON_KEY_RATEPLAN).getJSONObject(0).getJSONArray(Constants.JSON_KEY_CONSUMPTION_DETAILS_LIST);
				if (consumptionDetailsList.length() == 0) {
					JSONUtilsService jsonUtilsService = new JSONUtilsService();
					JSONObject balance = baseResponse.getJSONObject(Constants.JSON_KEY_BALANCE);
					String balanceValue = jsonUtilsService.getArabicOrEnglishValue(balance, userLocale);
					UserSelection userSelections = utilService.getUserSelectionsFromCache(senderId);
					utilService.updateUserSelectionsInCache(senderId, userSelections);
					userSelections.setBalanceValue(balanceValue);
					utilService.updateUserSelectionsInCache(senderId, userSelections);
					handlePayload(Constants.PAYLOAD_RATEPLAN_WITHOUT_METER, messenger, senderId);
				} else {
					genericTemplateHandling(customerProfile, payload, messenger, messagePayloadList, messageId, botWebserviceMessage, response);
				}
			} else {
				genericTemplateHandling(customerProfile, payload, messenger, messagePayloadList, messageId, botWebserviceMessage, response);

			}
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

	public void genericTemplateHandling(CustomerProfile customerProfile, String payload, Messenger messenger, ArrayList<MessagePayload> messagePayloadList, Long messageId,
			BotWebserviceMessage botWebserviceMessage, String response) {
		if (botWebserviceMessage.getOutType().getInOutTypeId() == 1) {// string
		} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 2 && response.startsWith("{")) { // Object
			genericTemplateService.generictemplateWithJsonObject(payload, response, messagePayloadList, customerProfile, messenger, botWebserviceMessage);
		} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 3 && response.startsWith("[")) {// Array
			genericTemplateService.genericTemplateWithJsonِArray(payload, response, messagePayloadList, customerProfile);
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
		logger.debug(Constants.LOGGER_INFO_PREFIX+"PASS THREAD CONTROL TO " + appId);
		HandoverPayload handoverPayload = HandoverPayload.create(senderId, HandoverAction.pass_thread_control, appId, "information");
		try {
			messenger.send(messagePayload);
			messenger.handover(handoverPayload);
		} catch (MessengerApiException | MessengerIOException io) {
			logger.error(Constants.LOGGER_DIAL_IS + "callSecondryHandover" + Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_EXCEPTION_MESSAGE + io);
			io.printStackTrace();
		}
	}

	public void takeThreadControl(final String senderId, Messenger messenger) {
		logger.debug(Constants.LOGGER_INFO_PREFIX + " Take thread control from page inbox");
		HandoverPayload handoverPayload = HandoverPayload.create(senderId, HandoverAction.take_thread_control, "", "information");
		try {
			messenger.handover(handoverPayload);
		} catch (MessengerApiException | MessengerIOException io) {
			logger.error(Constants.LOGGER_DIAL_IS + "callSecondryHandover" + Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_EXCEPTION_MESSAGE + io);
			io.printStackTrace();
		}
	}

	/**
	 * @param payload
	 * @param messenger
	 * @param senderId
	 * @param botWebserviceMessage
	 * @param response
	 * @param productIdAndOperationName
	 * @return
	 */
	public Map<String, String> postWebServiceRequestBodyCreationAndCalling(String payload, Messenger messenger, String senderId, BotWebserviceMessage botWebserviceMessage) {
		UserSelection userSelections = utilService.getUserSelectionsFromCache(senderId);
		CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		JSONObject jsonParam = new JSONObject();
		ArrayList<String> paramValuesList = new ArrayList<>();
		ArrayList<String> paramNames = new ArrayList<>(Arrays.asList(botWebserviceMessage.getListParamName().split(Constants.COMMA_CHAR)));
		if (payload.equalsIgnoreCase(Constants.PAYLOAD_RELATED_PRODUCT_SUBSCRIPTION_CONFIRM)
				|| payload.equalsIgnoreCase(Constants.PREFIX_CONFIRM_MOBILEINTERNET_SUBSCRIPTION) && userSelections.getProductIdAndOperationName() != null) {
			if (paramNames.size() == 2) {
				paramValuesList = new ArrayList<>(Arrays.asList(userSelections.getProductIdAndOperationName().split(Constants.COMMA_CHAR)));
			} else if (paramNames.size() == 3) {
				paramValuesList = new ArrayList<>(Arrays.asList(userSelections.getParametersListForRelatedProducts().split(Constants.COMMA_CHAR)));
			}
			jsonParam = utilService.setRequestBodyValueForPostCalling(paramValuesList, paramNames);
			logger.debug(Constants.LOGGER_INFO_PREFIX + " Post WS body values {} " + jsonParam);
		} else if (payload.equals(Constants.PAYLOAD_BALANCE_DEDUCTION)) {
			jsonParam = balanceDeductionService.setRequestBody();
			logger.debug(Constants.LOGGER_INFO_PREFIX+"Balance deduction request body " + jsonParam);
		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_RELATED_PRODUCT_SUBSCRIPTION_CONFIRM)
				|| payload.equalsIgnoreCase(Constants.PREFIX_CONFIRM_MOBILEINTERNET_SUBSCRIPTION) && userSelections.getProductIdAndOperationName() == null) {
			handlePayload(Constants.PAYLOAD_CHANGE_BUNDLE, messenger, senderId);
		} else if (payload.equals(Constants.PREFIX_BUNDLE_UNSUBSCRIPTION) || payload.equals(Constants.PAYLOAD_RENEWAL_BUNDLE)) {// MI Bundle Unsubscription & Renew Subscribed Bundle
			String uniqueProductName = getMainBundleIdForRenewOrUnsupscripe(userSelections, customerProfile, senderId);
			paramValuesList.add(uniqueProductName);
			String operationName = payload.equals(Constants.PAYLOAD_RENEWAL_BUNDLE) ? Constants.RENEW_OPERATION_VALUE : Constants.UNSUBSCRIBE_OPERATION_VALUE;
			paramValuesList.add(operationName);
			jsonParam = utilService.setRequestBodyValueForPostCalling(paramValuesList, paramNames);
			logger.debug(Constants.LOGGER_INFO_PREFIX+"Rateplan unsubscribe service parameters values " + jsonParam);
		} else if (userSelections.getAddonId() != null && userSelections.getAddonId().length() > 0 && payload.equals(Constants.PAYLOAD_ADDON_SUBSCRIPTION)) {// Add-on Subscription
			logger.debug(Constants.LOGGER_INFO_PREFIX+Constants.ADDON_SUBSCRIPTION_ACTION);
			paramValuesList = new ArrayList<>(Arrays.asList(userSelections.getAddonId().split(Constants.COMMA_CHAR)));
			jsonParam = utilService.setRequestBodyValueForPostCalling(paramValuesList, paramNames);
			logger.debug(Constants.LOGGER_INFO_PREFIX+"Add-on subscribe service parameters values " + jsonParam);
		} else if (payload.contains(Constants.MIGRATE_BY_PREFIX)) {
			jsonParam = migrationService.ratePlanMigration(paramNames, payload, userSelections, paramValuesList);
			logger.debug(Constants.LOGGER_INFO_PREFIX+"Migration Service Parameter values  " + jsonParam.toString());
		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_SALLEFNY_CONFIRMATION_YES)) {
			paramValuesList = new ArrayList<>(Arrays.asList(userSelections.getProductNameForSallefny().split(Constants.COMMA_CHAR)));
			jsonParam = utilService.setRequestBodyValueForPostCalling(paramValuesList, paramNames);
			logger.debug(Constants.LOGGER_INFO_PREFIX+"Sallefny Request Body " + jsonParam);
		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_DIAL_VALIDITY)) {
			jsonParam = authService.checkDialValidity(userSelections, customerProfile, paramNames, paramValuesList);
			logger.debug(Constants.LOGGER_INFO_PREFIX+"Dial Validity Request Body " + jsonParam);
		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_VERIFICATION_CODE)) {
			jsonParam = authService.checkVerificationCode(userSelections, paramNames, paramValuesList);
			logger.debug(Constants.LOGGER_INFO_PREFIX+"Verification Code Request Body " + jsonParam);
		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_RECHARGE)) {
			jsonParam = rechargeService.normalRecharge(userSelections, customerProfile, paramNames, paramValuesList);
			logger.debug(Constants.LOGGER_INFO_PREFIX+"Recharge Request Body " + jsonParam);
		} else if (payload.startsWith(Constants.PAYLOAD_RREFIX_TESLA)) {
			logger.debug(Constants.LOGGER_INFO_PREFIX+"Deduct from balance product name " + userSelections.getAkwaKartCategoryName());
			paramValuesList.add(userSelections.getAkwakartProductName());
			paramValuesList.add(Constants.ACTIVATE_OPERATION);
			jsonParam = akwaKartService.deductFromBalanceRequestBody(paramNames, paramValuesList);
			logger.debug(Constants.LOGGER_INFO_PREFIX+"Deduct Request body " + jsonParam);
		}
		return utilService.postWSCalling(botWebserviceMessage, jsonParam.toString(), senderId);
		// }

	}

	/**
	 * @param messagePayloadList
	 * @param payload
	 * @param botInteractionMessage
	 * @param customerProfile
	 * @param messenger
	 * @return
	 */
	public Map<String, String> getWebServiceHandling(ArrayList<MessagePayload> messagePayloadList, String payload, BotInteractionMessage botInteractionMessage, CustomerProfile customerProfile) {
		Map<String, String> mapResponse = new HashMap<>();
		String senderId = customerProfile.getSenderID();
		String phoneNumber = customerProfile.getMsisdn();
		Long messageId = botInteractionMessage.getMessageId();
		Long messageTypeId = botInteractionMessage.getBotMessageType().getMessageTypeId();
		BotWebserviceMessage botWebserviceMessage = chatBotService.findWebserviceMessageByMessageId(messageId);
		String url = botWebserviceMessage.getWsUrl();
		boolean cacheValue = false;
		Map<String, String> cachedMap = getFromCachDependOnWebService(url, phoneNumber);
		if (cachedMap == null || cachedMap.size() == 0) {
			mapResponse = utilService.getCalling(botWebserviceMessage, senderId, phoneNumber);
			cacheValue = true;
			logger.debug(Constants.LOGGER_INFO_PREFIX+Constants.LOGGER_SERVER_RESPONSE);
		} else {
			mapResponse = cachedMap;
			logger.debug(Constants.LOGGER_INFO_PREFIX+Constants.LOGGER_CACHED_RESPONSE);
		}
		if (mapResponse.get(Constants.RESPONSE_STATUS_KEY).equals("200")) {
			wsSuccessStatusHandling(payload, messagePayloadList, messageTypeId, senderId, mapResponse, url, cacheValue);
		}
		return mapResponse;

	}

	/**
	 * @param payload
	 * @param messagePayloadList
	 * @param messageTypeId
	 * @param senderId
	 * @param phoneNumber
	 * @param userLocale
	 * @param mapResponse
	 * @param url
	 * @param cacheValue
	 * @return WebService response as String value
	 */
	public void wsSuccessStatusHandling(String payload, ArrayList<MessagePayload> messagePayloadList, Long messageTypeId, String senderId, Map<String, String> mapResponse, String url,
			boolean cacheValue) {
		CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		String phoneNumber = customerProfile.getMsisdn();
		String userLocale = customerProfile.getLocale();
		if (cacheValue) {
			if (payload.equalsIgnoreCase(Constants.PAYLOAD_POSTPAID_DIAL)) {
				// lastBill
				JSONObject billProfile = new JSONObject(mapResponse.get(Constants.RESPONSE_KEY));
				String billAmount = "";
				if (!billProfile.get(Constants.JSON_KEY_BIILAMOUNT).equals(null)) {
					billAmount = billProfile.getString(Constants.JSON_KEY_BIILAMOUNT) == null || billProfile.getString(Constants.JSON_KEY_BIILAMOUNT) == "" ? ""
							: billProfile.getString(Constants.JSON_KEY_BIILAMOUNT);
				}
				if (billAmount.equals("")) {// post paid without bill to paid
					logger.debug(Constants.LOGGER_INFO_PREFIX+" Bill Amount value is "+billAmount);
					postPaidService.postPaidNoBillToPaid(senderId, userLocale, messagePayloadList, messageTypeId);
				} else {// post paid with bill to paid
					logger.debug(Constants.LOGGER_INFO_PREFIX+" Bill Amount value is "+billAmount);
					postPaidService.postPaidbillingPaymentHandling(senderId, userLocale, messagePayloadList, messageTypeId, phoneNumber, billProfile, billAmount);
				}
			}
			putInCachDependOnWebService(url, mapResponse, phoneNumber);
		}

	}

	/**
	 * @param userSelections
	 * @param customerProfile
	 * @param senderId
	 * @return
	 */
	private String getMainBundleIdForRenewOrUnsupscripe(UserSelection userSelections, CustomerProfile customerProfile, String senderId) {
		if (userSelections.getProductIdForRenew() != null) {
			return userSelections.getProductIdForRenew().split(Constants.COMMA_CHAR)[0];
		} else {
			BotWebserviceMessage getProfileWS = new BotWebserviceMessage();
			String subscriperProfileUrl = chatBotService.getBotConfigurationByKey(Constants.GET_SUBSCRIPER_PROFILE_URL_KEY).getValue();
			getProfileWS.setWsUrl(subscriperProfileUrl);
			String resp = utilService.getCalling(getProfileWS, senderId, customerProfile.getMsisdn()).get(Constants.RESPONSE_KEY);
			JSONObject jsonResp = new JSONObject(resp);
			return jsonResp.getJSONArray(Constants.JSON_KEY_MOBILE_INTERNET).getJSONObject(0).getString(Constants.JSON_KEY_PRODUCT_ID);

		}
	}

	public void sendMultipleMessages(ArrayList<MessagePayload> responses, String senderId, Messenger messenger, InteractionLogging interactionLogging) {
		for (MessagePayload response : responses) {
			try {
				Utils.markAsTypingOn(messenger, senderId);
				Utils.markAsTypingOff(messenger, senderId);
				messenger.send(response);
			} catch (MessengerApiException | MessengerIOException e) {
				logger.error(Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_EXCEPTION_MESSAGE + e);
				e.printStackTrace();
			}
			if (interactionLogging != null) {
				utilService.interactionLoggingUpdateResponseTime(interactionLogging);
			}
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
}
