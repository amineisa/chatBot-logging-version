package com.chatbot.controller;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.context.request.RequestContextHolder;

import com.chatbot.entity.BotButton;
import com.chatbot.entity.BotButtonTemplateMSG;
import com.chatbot.entity.BotInteraction;
import com.chatbot.entity.BotInteractionMessage;
import com.chatbot.entity.BotWebserviceMessage;
import com.chatbot.entity.CustomerProfile;
import com.chatbot.entity.UserSelections;
import com.chatbot.services.ChatBotService;
import com.chatbot.services.UtilService;
import com.chatbot.util.CacheHelper;
import com.chatbot.util.Constants;
import com.chatbot.util.Utils;
import com.github.messenger4j.Messenger;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerIOException;
import com.github.messenger4j.exception.MessengerVerificationException;
import com.github.messenger4j.send.HandoverAction;
import com.github.messenger4j.send.HandoverPayload;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.MessagingType;
import com.github.messenger4j.send.message.TemplateMessage;
import com.github.messenger4j.send.message.TextMessage;
import com.github.messenger4j.send.message.template.ButtonTemplate;
import com.github.messenger4j.send.message.template.GenericTemplate;
import com.github.messenger4j.send.message.template.Template;
import com.github.messenger4j.send.message.template.button.Button;
import com.github.messenger4j.send.message.template.common.Element;
import com.github.messenger4j.userprofile.UserProfile;
import com.github.messenger4j.webhook.event.AccountLinkingEvent;
import com.github.messenger4j.webhook.event.PostbackEvent;
import com.github.messenger4j.webhook.event.QuickReplyMessageEvent;
import com.github.messenger4j.webhook.event.TextMessageEvent;

@RestController
@RequestMapping("/callback")
@SessionScope
public class ChatBotController {

	@Autowired
	private final Messenger messenger;

	@Autowired
	private ChatBotService chatBotService;

	@Autowired
	private UtilService utilService;

	private static CacheHelper<String, Object> wsResponseCache = new CacheHelper<>("usersResponses");

	private static CacheHelper<String, Object> userSelectionsCache = new CacheHelper<>("usersSelections");

	private static final Logger logger = LoggerFactory.getLogger(ChatBotController.class);

	@Autowired
	public ChatBotController(final Messenger sendClient) {
		this.messenger = sendClient;

	}

	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<String> verifyWebhook(@RequestParam("hub.mode") final String mode, @RequestParam("hub.verify_token") final String verifyToken,
			@RequestParam("hub.challenge") final String challenge) {
		logger.debug("Received Webhook event verification request - mode: {} | verifyToken: {} | challenge: {}", mode, verifyToken, challenge);
		try {
			this.messenger.verifyWebhook(mode, verifyToken);
			return ResponseEntity.status(HttpStatus.OK).body(challenge);
		} catch (MessengerVerificationException e) {
			logger.warn("Webhook verification failed: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
		}
	}

	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<Void> handleCallback(@RequestBody final String payload, @RequestHeader("X-Hub-Signature") final String signature) {

		logger.debug("Received Messenger Platform callback - payload: {} | signature: {}", payload, signature);
		try {
			messenger.onReceiveEvents(payload, Optional.of(signature), event -> {
				final String senderId = event.senderId();
				RequestContextHolder.getRequestAttributes().getSessionMutex();
				chatBotService.getCustomerProfileBySenderId(senderId);
				UserSelections userSelections = getUserSelections(senderId);
				Utils.markAsSeen(messenger, senderId);
				// add text received
				if (event.isPostbackEvent()) {
					PostbackEvent postbackEvent = event.asPostbackEvent();
					String pLoad = postbackEvent.payload().get();
					if (pLoad.equalsIgnoreCase("Talk to Agent")) {
						BotInteraction interaction = chatBotService.findInteractionByPayload(pLoad);
						CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
						if( customerProfile != null ) {
						Utils.interactionLogginghandling(customerProfile, interaction, chatBotService);
						}
						callSecondryHandover(senderId);
					} else {
						handlePayload(postbackEvent.payload().get(), messenger, senderId);
					}
				} else if (event.isAccountLinkingEvent()) {
					AccountLinkingEvent accountLinkingEvent = event.asAccountLinkingEvent();
					if ((accountLinkingEvent.status().equals(AccountLinkingEvent.Status.LINKED))) {
						utilService.setLinkingInfoForCustomer(senderId, accountLinkingEvent.authorizationCode().get(), chatBotService);
						handlePayload(userSelections.getOriginalPayLoad(), messenger, senderId);
					} else if (accountLinkingEvent.status().equals(AccountLinkingEvent.Status.UNLINKED)) {
						Utils.userLogout(senderId, chatBotService);
					}
				} else if (event.isTextMessageEvent()) {
					final TextMessageEvent textMessageEvent = event.asTextMessageEvent();
					String text = textMessageEvent.text();
					handlePayload(text, messenger, senderId);
				} else if (event.isQuickReplyMessageEvent()) {
					final QuickReplyMessageEvent quickReplyMessageEvent = event.asQuickReplyMessageEvent();
					handlePayload(quickReplyMessageEvent.payload(), messenger, senderId);
				}
			});
			logger.debug(" Processed callback payload successfully");// add customer id
			return ResponseEntity.status(HttpStatus.OK).build();
		} catch (MessengerVerificationException e) {
			logger.warn("sProcessing of callback payload failed: {}", e.getMessage()); // add customer
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
	}

	/**
	 * @param senderId
	 */
	public void callPrimaryAppAgainHandover(final String senderId) {
		HandoverPayload handoverPayload = HandoverPayload.create(senderId, HandoverAction.take_thread_control, "187350432162719", "information");
		try {
			messenger.handover(handoverPayload);
		} catch (MessengerApiException | MessengerIOException e) {
			logger.error(Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_EXCEPTION_MESSAGE + e);
			e.printStackTrace();
		} 
	}

	public void callSecondryHandover(final String senderId) {													
		//1732093117014725
		HandoverPayload handoverPayload = HandoverPayload.create(senderId, HandoverAction.pass_thread_control, "263902037430900", "information");
		try {
			messenger.handover(handoverPayload);
		} catch (MessengerApiException | MessengerIOException io ) {
			logger.error(Constants.LOGGER_DIAL_IS + "callSecondryHandover" + Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_EXCEPTION_MESSAGE + io);
			io.printStackTrace();
		} 
	}

	private void handlePayload(String payload, Messenger messenger, String senderId) {
		String phoneNumber = Constants.EMPTY_STRING;
		Utils.markAsTypingOn(messenger, senderId);
		CustomerProfile customerprofile = chatBotService.getCustomerProfileBySenderId(senderId);
		if (customerprofile == null) {
			Utils.saveCustomerInformation(chatBotService, senderId, messenger);
		} else {
			Utils.updateCustomerLastSeen(chatBotService.getCustomerProfileBySenderId(senderId), phoneNumber, chatBotService);
		}
		payload = payloadSettings(payload, senderId);
		CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		if (customerProfile == null)
			customerProfile = new CustomerProfile();
		String userFirstName , userLocale;
		 userLocale =userFirstName = Constants.EMPTY_STRING;
            UserProfile userProfile = Utils.getUserProfile(senderId, messenger);
			userFirstName = userProfile.firstName();
		    userLocale = getLocaleValue(customerProfile, userProfile);
			logger.debug("Handle payload for customer " + customerProfile.toString());
		
		try {
			ArrayList<MessagePayload> messagePayloadList = new ArrayList<>();
			if (payload.equalsIgnoreCase(Constants.LOCALE_EN)) {
				utilService.setCustomerProfileLocalAsEnglish(customerProfile, chatBotService);
				messagePayloadList.add(utilService.changeLanguageResponse(Constants.LOCALE_EN, senderId));
				sendMultipleMessages(messagePayloadList, senderId);
			} else if (payload.equalsIgnoreCase(Constants.ARABIC_LOCAL)) {
				utilService.setCustomerProfileLocalAsArabic(customerProfile, chatBotService);
				messagePayloadList.add(utilService.changeLanguageResponse(Constants.ARABIC_LOCAL, senderId));
				sendMultipleMessages(messagePayloadList, senderId);
			} else {
				BotInteraction botInteraction = chatBotService.findInteractionByPayload(payload);
				if (botInteraction == null) {					
					logger.debug(Constants.LOGGER_DIAL_IS + phoneNumber + Constants.LOGGER_SENDER_ID + senderId + " Interaction For Unexpected payload");
					UserSelections userSelections = getUserSelections(senderId);
					userSelections.setFreeText(payload);
					userSelectionsCache.addToCentralCache(senderId, userSelections);
					handlePayload("rasa", messenger, senderId);
				}else {
				Utils.interactionLogginghandling(customerProfile, botInteraction, chatBotService);
				phoneNumber = customerProfile.getMsisdn() != null ? customerProfile.getMsisdn() : Constants.EMPTY_STRING;
				logger.debug(Constants.LOGGER_DIAL_IS + phoneNumber + " " + botInteraction.toString());
				if (!botInteraction.getIsSecure() || phoneNumber.length() > 0) {
					UserSelections userSelections = getUserSelections(senderId);
					if (botInteraction.getParentPayLoad() != null && userSelections != null) {
						userSelections.setParentPayLoad(botInteraction.getParentPayLoad());
						userSelectionsCache.addToCentralCache(senderId, userSelections);
					}
					List<BotInteractionMessage> interactionMessageList = chatBotService.findInteractionMessagesByInteractionId(botInteraction.getInteractionId());
					MessagePayload messagePayload = null;
					for (BotInteractionMessage botInteractionMessage : interactionMessageList) {
						Long messageTypeId = botInteractionMessage.getBotMessageType().getMessageTypeId();
						Long messageId = botInteractionMessage.getMessageId();

						if (botInteractionMessage.getIsStatic()) {
							messagePayload = utilService.responseInCaseStaticScenario(payload, senderId, userFirstName, botInteractionMessage, messageTypeId, messageId, chatBotService,
									userLocale, phoneNumber,messenger);
							messagePayloadList.add(messagePayload);
						}
						// Dynamic Scenario
						else {
							dynamicScenarioController(payload, messenger, senderId, customerProfile, userLocale, botInteraction, messagePayloadList, messageTypeId, messageId, phoneNumber);
						}
					}
					sendMultipleMessages(messagePayloadList, senderId);
					String parentPayLoad = Constants.EMPTY_STRING;
					if (userSelections != null && !payload.equalsIgnoreCase(Constants.PAYLOAD_CHANGE_BUNDLE)) {
						parentPayLoad = userSelections.getParentPayLoad() == null ? Constants.EMPTY_STRING  : userSelections.getParentPayLoad();
					}

					if (parentPayLoad.length() > 0) {
						userSelections.setParentPayLoad(null);
						userSelectionsCache.addToCentralCache(senderId, userSelections);
						handlePayload(parentPayLoad, messenger, senderId);
					}
				} else {
					UserSelections userSelections = getUserSelections(senderId);
					userSelections.setOriginalPayLoad(payload);
					userSelectionsCache.addToCentralCache(senderId, userSelections);
					BotInteraction loginInteraction = chatBotService.findInteractionByPayload("login");
					String title = Constants.EMPTY_STRING;;
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
					sendMultipleMessages(messagePayloadList, senderId);
				}
			}}
		} catch (Exception e) {
			logger.error(Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_EXCEPTION_MESSAGE + e);
			e.printStackTrace();
			handlePayload(Constants.PAYLOAD_FAULT_MSG, messenger, senderId);
		}

	}

	/**
	 * @param senderId
	 * @return
	 */
	public UserSelections getUserSelections(String senderId) {
		UserSelections userSelections;
		if (userSelectionsCache.getCachedValue(senderId) == null) {
			userSelections = new UserSelections();
		} else {
			userSelections = (UserSelections) userSelectionsCache.getCachedValue(senderId);
		}
		return userSelections;
	}

	// special handling payload
	private String payloadSettings(String payload, String senderId) {
		UserSelections userSelections = getUserSelections(senderId);
		if (payload.startsWith(Constants.PREFIX_ADDONSUBSCRIPE)) {// addonId
			userSelections.setAddonId(payload.substring(9, payload.length()) + Constants.COMMA_CHAR + "ACTIVATE");
			payload = Constants.PAYLOAD_ADDON_SUBSCRIPTION;
			userSelectionsCache.addToCentralCache(senderId, userSelections);
		}
		if (payload.startsWith(Constants.PREFIX_RATEPLAN_SUBSCRIPTION)) {// productIdAndOperationName
			userSelections.setProductIdAndOperationName(payload.substring(4, payload.length()));
			userSelectionsCache.addToCentralCache(senderId, userSelections);
			payload = payload.substring(0, 4);
		} else if (payload.startsWith(Constants.PREFIX_MOBILEINTERNET_ADDON)) {// addonId for MI
			userSelections.setAddonId(payload.substring(7, payload.length()));
			userSelectionsCache.addToCentralCache(senderId, userSelections);
			payload = Constants.PAYLOAD_BUY_ADDONS;
		} else if (payload.startsWith(Constants.PREFIX_RELATED_PRODUCTS)) {// productIdAndOperationName which has related products
			String[] params = payload.split(Constants.COMMA_CHAR);
			ArrayList<String> allParameters = new ArrayList<>();
			allParameters.add(params[1]);
			allParameters.add(params[2]);
			userSelections.setParametersListForRelatedProducts(allParameters);
			payload = Constants.PAYLOAD_RELATED_PRODUCTS;
		} else if (payload.equalsIgnoreCase(Constants.PREFIX_CONFIRM_MOBILEINTERNET_SUBSCRIPTION)) {
			ArrayList<String> parametersListForRelatedProducts = userSelections.getParametersListForRelatedProducts();
			logger.debug("User Selection" + userSelections.toString());
			if (parametersListForRelatedProducts != null && parametersListForRelatedProducts.size() == 3) {
				payload = Constants.PREFIX_CONFIRM_MOBILEINTERNET_SUBSCRIPTION + "_related_product";
			} else if (parametersListForRelatedProducts != null && parametersListForRelatedProducts.size() == 2) {
				payload = Constants.PREFIX_CONFIRM_MOBILEINTERNET_SUBSCRIPTION;
			}
			// MObile Internet subscription in case not has related products
		} else if (payload.contains(Constants.COMMA_CHAR) && !payload.contains(Constants.PREFIX_RELATED_PRODUCTS_SUBSCRIPTION)) {// productIdAndOperationName subscription
			userSelections.setProductIdAndOperationName(payload);
			String[] parameters = userSelections.getProductIdAndOperationName().split(Constants.COMMA_CHAR);
			ArrayList<String> parametersListForRelatedProducts = new ArrayList<>(Arrays.asList(parameters));
			userSelections.setParametersListForRelatedProducts(parametersListForRelatedProducts);
			payload = Constants.PAYLOAD_MOBILE_INTERNET_CONFIRMATION_MSG;
			userSelectionsCache.addToCentralCache(senderId, userSelections);
		} else if (payload.contains(Constants.PREFIX_RELATED_PRODUCTS_SUBSCRIPTION)) {
			String relatedId = payload.substring(0, payload.indexOf(','));
			ArrayList<String> parametersListForRelatedProducts = new ArrayList<>();
			if (userSelections.getParametersListForRelatedProducts().size() < 3) {
				parametersListForRelatedProducts = userSelections.getParametersListForRelatedProducts();
				parametersListForRelatedProducts.add(relatedId);
				userSelections.setParametersListForRelatedProducts(parametersListForRelatedProducts);
				userSelectionsCache.addToCentralCache(senderId, userSelections);
			}
			logger.debug(" User Selections " + userSelections.toString());
			payload = Constants.PAYLOAD_MOBILE_INTERNET_CONFIRMATION_MSG;
		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_MOBILE_INTERNET_SUBSCRIPTION_CANCEL)) {
			payload = Constants.PAYLOAD_CHANGE_BUNDLE;
		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_CANCEL_RECHARGING) || payload.equalsIgnoreCase(Constants.PAYLOAD_CANCEL_BILL_PAYMENT)) {
			payload = Constants.PAYLOAD_CANCEL_PAY_OR_RECHARGE;
		}
		return payload;
	}

	private void dynamicScenarioController(String payload, Messenger messenger, String senderId, CustomerProfile customerProfile, String userLocale, BotInteraction botInteraction,
			ArrayList<MessagePayload> messagePayloadList, Long messageTypeId, Long messageId, String phoneNumber) {
		logger.debug(Constants.LOGGER_DIAL_IS + phoneNumber + Constants.LOGGER_METHOD_NAME + " dynamicScenario and Interaction is " + botInteraction.toString());
		MessagePayload messagePayload;
		UserSelections userSelections = getUserSelections(senderId);
		boolean isSubscribed = false ;
		BotWebserviceMessage botWebserviceMessage = chatBotService.findWebserviceMessageByMessageId(messageId);
		String response = Constants.EMPTY_STRING;
		Map<String, String> mapResponse = new HashMap<>();
		String productIdAndOperationName = Constants.EMPTY_STRING;
		if (userSelections != null) {
			productIdAndOperationName = userSelections.getProductIdAndOperationName() == null ? Constants.EMPTY_STRING : userSelections.getProductIdAndOperationName();
		}
		if (botWebserviceMessage.getBotMethodType().getMethodTypeId() == 1) { // GET
			String url = botWebserviceMessage.getWsUrl();
			boolean cacheValue = false;
			Map<String , String> cachedMAp = getFromCachDependOnWebService(url, phoneNumber);
			if (cachedMAp == null || cachedMAp.size() == 0) {
				mapResponse = utilService.callGetWebService(botWebserviceMessage, senderId, chatBotService, phoneNumber);
				cacheValue = true;
				logger.debug(Constants.LOGGER_SERVER_RESPONSE);
			} else {
				mapResponse = cachedMAp;
				logger.debug(Constants.LOGGER_CACHED_RESPONSE);
			}
			if (mapResponse.get(Constants.RESPONSE_STATUS_KEY).equals("200")) {
				if (cacheValue) {
					putInCachDependOnWebService(url, mapResponse, phoneNumber);
				}
				response = mapResponse.get(Constants.RESPONSE_KEY);
			} else {
				handlePayload(Constants.PAYLOAD_FAULT_MSG, messenger, senderId);
			}
		} else if (botWebserviceMessage.getBotMethodType().getMethodTypeId() == 2) { // POST
			JSONObject jsonParam = new JSONObject();
			ArrayList<String> paramValuesList = new ArrayList<>();
			//Rasa Integration 
			if(payload.equals("rasa")) {
				String paramKey = botWebserviceMessage.getListParamName();
				JSONObject jsonResponse  = utilService.rasaIntegration(botWebserviceMessage.getWsUrl(), userSelections.getFreeText(),paramKey);
				if(!jsonResponse.keySet().isEmpty()) {
					JSONArray intentRanking = jsonResponse.getJSONArray("intentRanking");
					Map<String , Double> map = new HashMap<>();
					for(int i = 0 ;i<intentRanking.length();i++ ) {
						JSONObject intentObject = intentRanking.getJSONObject(i);
						double conf = intentObject.getDouble("confidence");
						String name = intentObject.getString("name");
						if(Math.round(conf)>50) {
							map.put(name, conf);
						}
						
					}
					JSONObject intent = jsonResponse.getJSONObject("intent");
					String name = intent.getString("name");
					handlePayload(name, messenger, senderId);
				}else {
					handlePayload(Constants.PAYLOAD_UNEXPECTED_PAYLOAD, messenger, senderId);
				}
			}else {
			// Buy Product Bundles (Connect & Super Connect)
			if (productIdAndOperationName.length() > 0 && (payload.equalsIgnoreCase(Constants.PAYLOAD_RELATED_PRODUCT_SUBSCRIPTION_CONFIRM) || payload.equalsIgnoreCase(Constants.PREFIX_CONFIRM_MOBILEINTERNET_SUBSCRIPTION))) {
				logger.debug(Constants.MI_BUNDLE_SUBSCRIPTION);
				String paramName = botWebserviceMessage.getListParamName();
				String[] paramNames = paramName.split(Constants.COMMA_CHAR);
				if (paramNames.length == 2) {
					paramValuesList = new ArrayList<>(Arrays.asList(productIdAndOperationName.split(Constants.COMMA_CHAR)));
				} else if (paramNames.length == 3) {
					paramValuesList = userSelections.getParametersListForRelatedProducts();
				}
				for (int p = 0; p < paramNames.length; p++) {
					jsonParam.put(paramNames[p], paramValuesList.get(p));
				}
			} // Renew Subscribed Bundle
			else if (userSelections.getProductIdForRenew().length() > 0 && !payload.equals(Constants.PAYLOAD_ADDON_SUBSCRIPTION)) {
				logger.debug(Constants.RENEW_BUNDLE_ACTION);
				String paramName = botWebserviceMessage.getListParamName();
				String[] paramNames = paramName.split(Constants.COMMA_CHAR);
				if (paramNames.length == 2) {
					paramValuesList = new ArrayList<>(Arrays.asList(userSelections.getProductIdForRenew().split(Constants.COMMA_CHAR)));
				}
				for (int p = 0; p < paramNames.length; p++) {
					jsonParam.put(paramNames[p], paramValuesList.get(p));
				}
			} // Add-on Subscription
			else if (userSelections.getAddonId().length() > 0 && payload.equals(Constants.PAYLOAD_ADDON_SUBSCRIPTION)) {
				logger.debug(Constants.ADDON_SUBSCRIPTION_ACTION);
				String paramName = botWebserviceMessage.getListParamName();
				String[] paramNames  = paramName.split(Constants.COMMA_CHAR);
				if (paramNames.length == 2) {
					paramValuesList = new ArrayList<>(Arrays.asList(userSelections.getAddonId().split(Constants.COMMA_CHAR)));
				}
				for (int p = 0; p < paramNames.length; p++) {
					jsonParam.put(paramNames[p], paramValuesList.get(p));
				}
			}
			String stringParam = jsonParam.toString();
			mapResponse = utilService.buyProductOrAddon(botWebserviceMessage, stringParam, chatBotService, senderId);
			if (mapResponse.get(Constants.RESPONSE_STATUS_KEY).equals("200")) {
				response = mapResponse.get(Constants.RESPONSE_KEY);
			}
		}
			}
		// Text Message
		if (messageTypeId != 0 && messageTypeId == Utils.MessageTypeEnum.TEXTMESSAGE.getValue()) {
			utilService.createTextMessageInDynamicScenario(senderId, messagePayloadList, botWebserviceMessage, response, chatBotService, userLocale);
		} else if (messageTypeId != 0 && messageTypeId == Utils.MessageTypeEnum.ButtonTemplate.getValue()) {
			if (botWebserviceMessage.getOutType().getInOutTypeId() ==  1) {// string
			} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 2) {// Object
			} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 3) {// Array
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
			// Generic Template
		} else if (messageTypeId != 0 && messageTypeId == Utils.MessageTypeEnum.GENERICTEMPLATEMESSAGE.getValue()) {
			if (botWebserviceMessage.getOutType().getInOutTypeId() == 1) {// string
			} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 2) {// Object
				JSONObject jsonResponse = new JSONObject(response);
				if (payload.equalsIgnoreCase(Constants.PAYLOAD_ACCOUNT_DETAILS)) {
					Boolean postPaid = jsonResponse.getBoolean(Constants.JSON_KEY_POSTPAID);
					if (postPaid) {
						JSONObject balance = jsonResponse.getJSONObject(Constants.JSON_KEY_BALANCE);
						if (balance == null) {
							handlePayload(Constants.PAYLOAD_FAULT_MSG, messenger, senderId);
						} else {
							handlePayload(Constants.PAYLOAD_PREPAID, messenger, senderId);
						}
					} else {
						Object billingProfileModel = jsonResponse.get(Constants.JSON_KEY_BILLING_PROFILE);
						if (billingProfileModel == null) {
							handlePayload(Constants.PAYLOAD_FAULT_MSG, messenger, senderId);
						} else {
							handlePayload(Constants.PAYLOAD_POSTPAID_DIAL, messenger, senderId);
						}

					}
				} else {
					JSONArray ratePlan = new JSONArray();
					JSONArray connect = new JSONArray();
					ratePlan = jsonResponse.getJSONArray(Constants.JSON_KEY_RATEPLAN);
					connect = jsonResponse.getJSONArray(Constants.JSON_KEY_MOBILE_INTERNET);
					logger.debug(Constants.LOGGER_MOBILE_INTERENET_CONSUMPTION + connect.toString());
					logger.debug(Constants.LOGGER_RATEPLAN_CONSUMPTION+ ratePlan.toString());
					if (connect.length() > 0) {// productIdForRenew
						userSelections.setProductIdForRenew(connect.getJSONObject(0).getString(Constants.JSON_KEY_PRODUCT_ID) + Constants.OPERATION_NAME_RENEW);
					}
					if (payload.equalsIgnoreCase(Constants.PAYLOAD_CONSUMPTION)) {
						if (ratePlan.length() > 0 && connect.length() > 0) {
							handlePayload(Constants.PAYLOAD_RATEPLAN_AND_CONNECT, messenger, senderId);
						} else if (ratePlan.length() > 0 && connect.length() == 0) {
							handlePayload(Constants.PAYLOAD_VIEW_RATEPLAN_DETAILS, messenger, senderId);
						} else if (ratePlan.length() == 0 && connect.length() > 0) {
							handlePayload(Constants.PAYLOAD_VIEW_ROOT_CONNECT_DETAILS, messenger, senderId);
						} else if (ratePlan.length() == 0 && connect.length() == 0) {
							handlePayload(Constants.PAYLOAD_VIEW_MOBILEINTERNET_SUBSCRIPTION, messenger, senderId);
						}
					} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_RATEPLAN_DETAILS)) {
						Template template = utilService.createGenericTemplate(messageId, chatBotService, userLocale, botWebserviceMessage, jsonResponse, phoneNumber,
								userSelections.getConsumptionNames(), payload);
						MessagePayload mPayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(template));
						messagePayloadList.add(mPayload);
					} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_VIEW_CONNECT_DETAILS) && connect.length() == 0) {
						handlePayload(Constants.PAYLOAD_CHANGE_BUNDLE, messenger, senderId);
						payload = Constants.EMPTY_STRING;
						userSelections.setParentPayLoad(Constants.EMPTY_STRING);
						userSelectionsCache.addToCentralCache(senderId, userSelections);
					} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_VIEW_CONNECT_DETAILS) || payload.equals(Constants.PAYLOAD_RATEPLAN_ADDONS_CONSUMPTION) || payload.equals(Constants.PAYLOAD_MOBILEINTERNET_ADDONS_CONSUMPTION)) {
						userSelections.setSubscribed(true);
						ArrayList<String> consumptionNames = new ArrayList<>();
						for (int i = 0; i < connect.length(); i++) {
							JSONArray consumptionDetailsList = connect.getJSONObject(i).getJSONArray(Constants.JSON_KEY_CONSUMPTION_DETAILS_LIST);
							for(int c = 0;c<consumptionDetailsList.length();c++) {
								String consumptionName = Constants.EMPTY_STRING;
								if (userLocale.contains(Constants.ARABIC_LOCAL)) {
									consumptionName = consumptionDetailsList.getJSONObject(c).getJSONObject(Constants.JSON_KEY_CONSUMPTION_NAME).get(Constants.JSON_KEY_LABEL_AR) == null ? Constants.UNDERSCORE:consumptionDetailsList.getJSONObject(c).getJSONObject(Constants.JSON_KEY_CONSUMPTION_NAME).getString(Constants.JSON_KEY_LABEL_AR);
								}else {
									consumptionName = consumptionDetailsList.getJSONObject(c).getJSONObject(Constants.JSON_KEY_CONSUMPTION_NAME).get(Constants.JSON_KEY_LABEL_EN) == null ? Constants.UNDERSCORE:consumptionDetailsList.getJSONObject(c).getJSONObject(Constants.JSON_KEY_CONSUMPTION_NAME).getString(Constants.JSON_KEY_LABEL_EN);
								}
								consumptionNames.add(consumptionName);
							}
						}
						if(payload.contains(Constants.PAYLOAD_VIEW_CONNECT_DETAILS)) {					    
						 messagePayloadList.add(MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(createMainBundleDetails(connect,userLocale))));
						}
						
						userSelections.setConsumptionNames(consumptionNames);
						userSelectionsCache.addToCentralCache(senderId, userSelections);
						Template template = utilService.createGenericTemplate(messageId, chatBotService, userLocale, botWebserviceMessage, jsonResponse, phoneNumber,
								userSelections.getConsumptionNames(), payload);
						MessagePayload mPayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(template));
						messagePayloadList.add(mPayload);
					}

				}
			} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 3) {// Array
				if (payload.startsWith(Constants.PREFIX_RATEPLAN_SUBSCRIPTION)) {
					JSONArray bundleArray = new JSONArray(response);
					if (bundleArray.length() > 0) {// productIdAndOperationName
						messagePayloadList.add(utilService.getProductsFromJsonByCategory(bundleArray, userSelections.getProductIdAndOperationName(), senderId, chatBotService, userLocale));
					}
				} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_RELATED_PRODUCTS)) {
					JSONArray bundleArray = new JSONArray(response);
					if (bundleArray.length() > 0) {
						String productId = userSelections.getParametersListForRelatedProducts().get(0);
						messagePayloadList.add(utilService.getRelatedProductFromJsonByBundleId(bundleArray, productId, senderId, userLocale));
					}
				} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_CHANGE_BUNDLE)) {
					JSONArray bundleArray = new JSONArray(response);
					if (bundleArray.length() > 0) {
						if(!userSelections.isSubscribed()) {
						 messagePayloadList.add(MessagePayload.create(senderId, MessagingType.RESPONSE, (TextMessage.create(Utils.informUserThatHeDoesnotSubscribeAtAnyMIBundle(userLocale)))));
						}
						messagePayloadList.add(utilService.getBundleCategories(bundleArray, senderId, chatBotService, userLocale, phoneNumber));
					} else {// addition
						GenericTemplate gtemplate = utilService.createGenericTemplateForNotEligiblBundleDials(userLocale, new ArrayList<Button>(), new ArrayList<Element>(), phoneNumber);
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
						messagePayloadList.add(utilService.getExtraMobileInternetAddonsByCategory(categoryArray, senderId,  userLocale, userSelections.getAddonId()));
					}
				}

			}
		}

	}

	private String getLocaleValue(CustomerProfile customer, UserProfile userProfile) {
		String userLocale;
		userLocale = customer.getLocale() == null ? userProfile.locale() : customer.getLocale();
		return userLocale;
	}

	private void sendMultipleMessages(ArrayList<MessagePayload> responses, String senderId) {
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
	
	
	String createMainBundleDetails(JSONArray ratePlanArray , String locale){
		String comercialName;
		String renwalDate;
		String details;
		renwalDate = details = comercialName=Constants.EMPTY_STRING;
		for(int i =  0 ;i<ratePlanArray.length();i++) {
			JSONObject bundleDetails = ratePlanArray.getJSONObject(i);
			if(locale.contains(Constants.ARABIC_LOCAL)) {
				comercialName = bundleDetails.getJSONObject(Constants.JSON_KEY_COMMERCIAL_NAME).getString(Constants.JSON_KEY_VALUE_AR);
				renwalDate = bundleDetails.getJSONObject(Constants.JSON_KEY_RENEWAL_DATE).getString(Constants.JSON_KEY_VALUE_AR);
				details = " باقتك هي  "+comercialName +" تاريخ التجديد "+renwalDate;
			}else {
				comercialName = bundleDetails.getJSONObject(Constants.JSON_KEY_COMMERCIAL_NAME).getString(Constants.JSON_KEY_VALUE_EN);
				renwalDate = bundleDetails.getJSONObject(Constants.JSON_KEY_RENEWAL_DATE).getString(Constants.JSON_KEY_VALUE_EN);
				details = "Your MI Bundle is "+comercialName +" and Renewal Date is "+renwalDate;
			}
		}
		
		return details;
	}
	
	
}
