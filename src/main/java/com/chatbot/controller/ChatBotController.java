package com.chatbot.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONException;
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

import com.chatbot.entity.BotButton;
import com.chatbot.entity.BotButtonTemplateMSG;
import com.chatbot.entity.BotInteraction;
import com.chatbot.entity.BotInteractionMessage;
import com.chatbot.entity.BotWebserviceMessage;
import com.chatbot.entity.CustomerProfile;
import com.chatbot.services.ChatBotService;
import com.chatbot.services.UtilService;
import com.chatbot.util.Constants;
import com.chatbot.util.Utils;
import com.github.messenger4j.Messenger;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerIOException;
import com.github.messenger4j.exception.MessengerVerificationException;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.MessagingType;
import com.github.messenger4j.send.message.TemplateMessage;
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
// @CrossOrigin(origins = { "http://stg.etisalat.eg" })
public class ChatBotController {


	private String originalPayLoad = "";

	// private String parentPayLoad = "";

	private String phoneNumber = "";

	@Autowired
	private final Messenger messenger;

	@Autowired
	private ChatBotService chatBotService;

	@Autowired
	private UtilService utilService;
	// This used for bundle which does not has related Products
	private String productIdAndOperationName;

	private String productIdForRenew;
	// This used for bundle which has related Products
	private ArrayList<String> parametersListForRelatedProducts;

	private String addonId;

	private String lastPayLoad = "";

	private ArrayList<String> consumptionNames = new ArrayList<String>();

	private static final Logger logger = LoggerFactory.getLogger(ChatBotController.class);

	@Autowired
	public ChatBotController(final Messenger sendClient) {
		this.messenger = sendClient;

	}

	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<String> verifyWebhook(@RequestParam("hub.mode") final String mode, @RequestParam("hub.verify_token") final String verifyToken,
			@RequestParam("hub.challenge") final String challenge) {

		logger.debug("Received Webhook verification request - mode: {} | verifyToken: {} | challenge: {}", mode, verifyToken, challenge);
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
				String methodName = new Object() {
				}.getClass().getEnclosingMethod().getName();
				final String senderId = event.senderId();
				Utils.markAsSeen(messenger, senderId);
				// add text received
				if (event.isQuickReplyMessageEvent()) {
					logger.debug("Sender ID is " + senderId + " Method Name " + methodName + " Event is QUICK REPLY");
					final QuickReplyMessageEvent quickReplyMessageEvent = event.asQuickReplyMessageEvent();
					handlePayload(quickReplyMessageEvent.payload(), messenger, senderId);

				} else if (event.isPostbackEvent()) {
					logger.debug("Sender ID is " + senderId + " Method Name " + methodName + " Event is POSTBACK");
					PostbackEvent postbackEvent = event.asPostbackEvent();
					handlePayload(postbackEvent.payload().get(), messenger, senderId);
				} else if (event.isAccountLinkingEvent()) {
					AccountLinkingEvent accountLinkingEvent = event.asAccountLinkingEvent();
					if ((accountLinkingEvent.status().equals(AccountLinkingEvent.Status.LINKED))) {
						utilService.setLinkingInfoForCustomer(senderId, messenger, accountLinkingEvent.authorizationCode().get(), chatBotService);
						logger.debug("Sender ID is " + senderId + " Method Name " + methodName + " USER is logged in ");
						handlePayload(originalPayLoad, messenger, senderId);
					} else if (accountLinkingEvent.status().equals(AccountLinkingEvent.Status.UNLINKED)) {
						Utils.userLogout(senderId, chatBotService);
						logger.debug("Sender ID is " + senderId + " Method Name " + methodName + " USER is logged out");
					}
				} else if (event.isTextMessageEvent()) {
					logger.debug("Sender ID is " + senderId + " Method Name " + methodName + " Event is TEXTMESSAGE");
					final TextMessageEvent textMessageEvent = event.asTextMessageEvent();
					String text = textMessageEvent.text();
					handlePayload(text, messenger, senderId);
				}

			});
			logger.debug(" Processed callback payload successfully");// add customer id
			return ResponseEntity.status(HttpStatus.OK).build();
		} catch (MessengerVerificationException e) {
			logger.warn("sProcessing of callback payload failed: {}", e.getMessage()); // add customer
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
	}

	private void handlePayload(String payload, Messenger messenger, String senderId) {
		String methodName = new Object() {
		}.getClass().getEnclosingMethod().getName();
		logger.debug("Dial is " + phoneNumber + " Methoud Name Is " + methodName + " Parameters names Payload and sender ID values are  " + payload + " " + senderId);
		Utils.markAsTypingOn(messenger, senderId);
		Utils.updateCustomerLastSeen(chatBotService.getCustomerProfileBySenderId(senderId), phoneNumber, chatBotService);
		payload = payLoadSettings(payload);
		String parentPayLoad = null;
		CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		if (customerProfile == null)
			customerProfile = new CustomerProfile();
		logger.debug("Handle payload for customer " + customerProfile.toString());
		String userFirstName = "";
		String userLocale = "";
		UserProfile userProfile = null;
		try {
			userProfile = messenger.queryUserProfile(senderId);
			userFirstName = userProfile.firstName();
			userLocale = getLocaleValue(customerProfile, userProfile);
			logger.debug("Dial is " + phoneNumber + " Sender ID is " + senderId + " USER Locale is " + userLocale);
		} catch (MessengerApiException e1) {
			logger.error("Dial is " + phoneNumber + " Sender ID is " + senderId + " Exceptoion is " + e1.getMessage());
		} catch (MessengerIOException e1) {
			logger.error("Dial is " + phoneNumber + " Sender ID is " + senderId + " Exceptoion is " + e1.getMessage());
		}

		try {
			ArrayList<MessagePayload> messagePayloadList = new ArrayList<>();
			if (payload.equalsIgnoreCase("en_us")) {
				utilService.setCustomerProfileLocalAsEnglish(customerProfile, chatBotService);
				messagePayloadList.add(utilService.changeLanguageResponse("en_us", senderId));
				sendMultipleMessages(messagePayloadList, senderId);
			} else if (payload.equalsIgnoreCase("ar")) {
				utilService.setCustomerProfileLocalAsArabic(customerProfile, chatBotService);
				messagePayloadList.add(utilService.changeLanguageResponse("ar", senderId));
				sendMultipleMessages(messagePayloadList, senderId);
			} else {
				BotInteraction botInteraction = chatBotService.findInteractionByPayload(payload);
				if (botInteraction == null) {
					logger.debug("Dial is " + phoneNumber + " Sender ID is " + senderId + " Interaction is Unexpected payload");
					handlePayload(Constants.PAYLOAD_UNEXPECTED_PAYLOAD, messenger, senderId);
				}
				Utils.interactionLogginghandling(customerProfile, botInteraction, chatBotService, phoneNumber);

				phoneNumber = customerProfile.getMsisdn() != null ? customerProfile.getMsisdn() : "";
				logger.debug(" Dial is " + phoneNumber + " " + botInteraction.toString());

				if (!botInteraction.getIsSecure() || phoneNumber.length() > 0) {
					parentPayLoad = botInteraction.getParentPayLoad();
					List<BotInteractionMessage> interactionMessageList = chatBotService.findInteractionMessagesByInteractionId(botInteraction.getInteractionId());
					MessagePayload messagePayload = null;

					for (BotInteractionMessage botInteractionMessage : interactionMessageList) {
						logger.debug("Customer Profile " + customerProfile.toString() + botInteractionMessage.toString());
						Long messageTypeId = botInteractionMessage.getBotMessageType().getMessageTypeId();
						Long messageId = botInteractionMessage.getMessageId();

						if (botInteractionMessage.getIsStatic()) {
							messagePayload = utilService.responseInCaseStaticScenario(payload, senderId, userFirstName, botInteraction, botInteractionMessage, messageTypeId, messageId, chatBotService,
									parentPayLoad, userLocale, phoneNumber);
							messagePayloadList.add(messagePayload);
						}
						// Dynamic Scenario
						else {
							dynamicScenarioController(payload, messenger, senderId, customerProfile, userLocale, botInteraction, messagePayloadList, messageTypeId, messageId, parentPayLoad,
									phoneNumber);
						}
					}
					sendMultipleMessages(messagePayloadList, senderId);
					
					// TODO
					if (payload.equalsIgnoreCase(Constants.PAYLOAD_CHANGE_BUNDLE)) {
						parentPayLoad = null;
						lastPayLoad = payload;
					}
					if (parentPayLoad != null && lastPayLoad == "") {
						handlePayload(parentPayLoad, messenger, senderId); 
					}
				} else {
					originalPayLoad = payload;//For return payload state after login
					MessagePayload messagePayload = null;
					BotInteraction loginInteraction = chatBotService.findInteractionByPayload("login");
					String title = "";

					BotInteractionMessage interactionMSG = chatBotService.findMessageByInteraction(loginInteraction);
					BotButtonTemplateMSG botButtonTemplate = chatBotService.findBotButtonTemplateByMessageId(interactionMSG);
					title = utilService.getTextForButtonTemplate(userLocale, botButtonTemplate);
					List<BotButton> botButtons = chatBotService.findAllByBotButtonTemplateMSGId(botButtonTemplate);

					//messenger4j
					ArrayList<Button> buttons = new ArrayList<Button>();
					for (BotButton botButton : botButtons) {
						Button button = utilService.createButton(botButton, userLocale, new JSONObject(), phoneNumber);
						buttons.add(button);
					}

					ButtonTemplate buttonTemplate = ButtonTemplate.create(title, buttons);
					messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(buttonTemplate));
					messagePayloadList.add(messagePayload);
					sendMultipleMessages(messagePayloadList, senderId);
				}
			}
		} catch (Exception e) {
			logger.error("Sender ID is " + senderId + " Exceptoion is " + e.getMessage() + " Method Name " + methodName);
		}

	}

	//special handling payload
	private String payLoadSettings(String payload) {
		//TODO test again 
		// TODO change to constants 
		if (payload.startsWith(Constants.PREFIX_ADDONSUBSCRIPE)) {
			addonId = payload.substring(10, payload.length()) + "," + "ACTIVATE";
			payload = Constants.PAYLOAD_ADDON_SUBSCRIPTION;
		}
		if (payload.startsWith(Constants.PREFIX_RATEPLAN_SUBSCRIPTION)) {
			productIdAndOperationName = payload.substring(4, payload.length());
			payload = payload.substring(0, 4);
		} else if (payload.startsWith(Constants.PREFIX_MOBILEINTERNET_ADDON)) {
			addonId = payload.substring(7, payload.length());
			payload = Constants.PAYLOAD_BUY_ADDONS;
		} else if (payload.startsWith(Constants.PREFIX_RELATED_PRODUCTS)) {
			productIdAndOperationName = payload;
			payload = Constants.PAYLOAD_RELATED_PRODUCTS;
		} else if (payload.equalsIgnoreCase(Constants.PREFIX_CONFIRM_MOBILEINTERNET_SUBSCRIPTION)) {
			if (parametersListForRelatedProducts.size() == 3) {
				payload = Constants.PREFIX_CONFIRM_MOBILEINTERNET_SUBSCRIPTION+"_related_product";
			} else if (parametersListForRelatedProducts.size() == 2) {
				payload = Constants.PREFIX_CONFIRM_MOBILEINTERNET_SUBSCRIPTION;
			}
		} else if (payload.contains(",") && !payload.contains(Constants.PREFIX_RELATED_PRODUCTS_SUBSCRIPTION)) {
			productIdAndOperationName = payload;
			String[] parameters = productIdAndOperationName.split(",");
			parametersListForRelatedProducts = new ArrayList<String>(Arrays.asList(parameters));
			payload = Constants.PAYLOAD_MOBILE_INTERNET_CONFIRMATION_MSG;
		} else if (payload.contains(Constants.PREFIX_RELATED_PRODUCTS_SUBSCRIPTION)) {
			String[] parameters = productIdAndOperationName.split(",");
			parametersListForRelatedProducts = new ArrayList<String>(Arrays.asList(parameters));
			parametersListForRelatedProducts.add(payload.split(",")[0]);
			parametersListForRelatedProducts.remove(0);
			payload = Constants.PAYLOAD_MOBILE_INTERNET_CONFIRMATION_MSG;

		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_MOBILE_INTERNET_SUBSCRIPTION_CANCEL)) {
			payload = Constants.PAYLOAD_CHANGE_BUNDLE;
		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_CANCEL_RECHARGING) || payload.equalsIgnoreCase(Constants.PAYLOAD_CANCEL_BILL_PAYMENT)) {
			payload = Constants.PAYLOAD_CANCEL_PAY_OR_RECHARGE;
		}
		return payload;
	}

	private void dynamicScenarioController(String payload, Messenger messenger, String senderId, CustomerProfile customerProfile, String userLocale, BotInteraction botInteraction,
			ArrayList<MessagePayload> messagePayloadList, Long messageTypeId, Long messageId, String parentPayLoad, String phoneNumber) throws JSONException {
		String methodName = new Object() {
		}.getClass().getEnclosingMethod().getName();
		logger.debug("Dial is " + phoneNumber + " Methoud Name Is " + methodName + " Parameters names Payload , senderID , user Locale and Interaction  values are  " + payload + " " + senderId + " "
				+ botInteraction.toString());
		MessagePayload messagePayload;
		parentPayLoad = botInteraction.getParentPayLoad();
		// String jsonBodyString = "";
		BotWebserviceMessage botWebserviceMessage = chatBotService.findWebserviceMessageByMessageId(messageId);
		String response = "";
		Map<String, String> mapResponse = new HashMap<String, String>();
		if (botWebserviceMessage.getBotMethodType().getMethodTypeId() == 1) { //GET
			String [] profilePayloads = Constants.profilePayloads.split(",");
			List<String> payloadsList = new ArrayList<String>(Arrays.asList(profilePayloads)); 
			if (payload.equalsIgnoreCase(Constants.PAYLOAD_CHANGE_BUNDLE)) {
				mapResponse = utilService.getEligibleProducts(botWebserviceMessage, senderId, chatBotService, phoneNumber);
			} else if (payloadsList.contains(payload)) {
				mapResponse = utilService.getSubscriberProfile(botWebserviceMessage, senderId, chatBotService, phoneNumber);
			} else {
				mapResponse = utilService.callGetWebService(botWebserviceMessage, senderId, chatBotService, phoneNumber);
			}
			if (mapResponse.get("status").equals("200")) {
				response = mapResponse.get("response");
			} else {
				handlePayload(Constants.PAYLOAD_FAULT_MSG, messenger, senderId);
			}
		} else if (botWebserviceMessage.getBotMethodType().getMethodTypeId() == 2) { //POST
			JSONObject jsonParam = new JSONObject();
			ArrayList<String> paramValuesList = new ArrayList<String>();
			if (productIdAndOperationName != null && !productIdAndOperationName.isEmpty()) {
				String paramName = botWebserviceMessage.getListParamName();
				String paramNames[] = paramName.split(",");
				if (paramNames.length == 2) {
					paramValuesList = new ArrayList<>(Arrays.asList(productIdAndOperationName.split(",")));
				} else if (paramNames.length == 3) {
					paramValuesList = parametersListForRelatedProducts;
				}
				for (int p = 0; p < paramNames.length; p++) {
					jsonParam.put(paramNames[p], paramValuesList.get(p));
				}
			} else if (productIdForRenew.length() > 0 && !payload.equals(Constants.PAYLOAD_ADDON_SUBSCRIPTION)) {
				String paramName = botWebserviceMessage.getListParamName();
				String paramNames[] = paramName.split(",");
				if (paramNames.length == 2) {
					paramValuesList = new ArrayList<>(Arrays.asList(productIdForRenew.split(",")));
				}
				for (int p = 0; p < paramNames.length; p++) {
					jsonParam.put(paramNames[p], paramValuesList.get(p));
				}
			} else if (addonId.length() > 0 && payload.equals(Constants.PAYLOAD_ADDON_SUBSCRIPTION)) {
				String paramName = botWebserviceMessage.getListParamName();
				String paramNames[] = paramName.split(",");
				if (paramNames.length == 2) {
					paramValuesList = new ArrayList<>(Arrays.asList(addonId.split(",")));
				}
				for (int p = 0; p < paramNames.length; p++) {
					jsonParam.put(paramNames[p], paramValuesList.get(p));
				}
			}
			String stringParam = jsonParam.toString();
			mapResponse = utilService.callPostWebService(botWebserviceMessage, stringParam, chatBotService, senderId, paramValuesList);
			if (mapResponse.get("status").equals("200")) {
				response = mapResponse.get("response");
			}
		}
		// Text Message
		if (messageTypeId == Utils.MessageTypeEnum.TEXTMESSAGE.getValue()) {
			utilService.createTextMessageInDynamicScenario(senderId, messagePayloadList, botWebserviceMessage, response, chatBotService, userLocale);
		} else if (messageTypeId == Utils.MessageTypeEnum.ButtonTemplate.getValue()) {
			// string
			if (botWebserviceMessage.getOutType().getInOutTypeId() == 1) {
				// Object
			} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 2) {
				// Array
			} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 3) {
				JSONArray jsonArray = new JSONArray(response);
				if (jsonArray.length() > 0) {
					List<BotButton> WSMSGButtons = chatBotService.findAllButtonsByWebserviceMessage(botWebserviceMessage);
					JSONObject jsonObject = new JSONObject();
					List<Button> realButtons = new ArrayList<>();
					for (int j = 0; j < jsonArray.length(); j++) {
						Button realButton = null;
						for (BotButton wsBotButton : WSMSGButtons) {
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
		} else if (messageTypeId == Utils.MessageTypeEnum.GENERICTEMPLATEMESSAGE.getValue()) {
			// string
			if (botWebserviceMessage.getOutType().getInOutTypeId() == 1) {
				// Object
			} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 2) {
				JSONObject jsonResponse = new JSONObject(response);
				if (payload.equalsIgnoreCase(Constants.PAYLOAD_ACCOUNT_DETAILS)) {
					Boolean postPaid = jsonResponse.getBoolean("postPaid");
					if (postPaid == false) {
						JSONObject balance = jsonResponse.getJSONObject("balance");
						if (balance.equals(null)) {
							handlePayload(Constants.PAYLOAD_FAULT_MSG, messenger, senderId);
						} else {
							handlePayload(Constants.PAYLOAD_PREPAID, messenger, senderId);
						}
					} else {
						Object billingProfileModel = jsonResponse.get("billingProfileModel");
						if (billingProfileModel.equals(null)) {
							handlePayload(Constants.PAYLOAD_FAULT_MSG, messenger, senderId);
						} else {
							handlePayload(Constants.PAYLOAD_POSTPAID_DIAL, messenger, senderId);
						}

					}
				} else {
					JSONArray ratePlan = new JSONArray();
					JSONArray connect = new JSONArray();
					ratePlan = jsonResponse.getJSONArray("rateplan");
					connect = jsonResponse.getJSONArray("connect");
					if (connect.length() > 0) {
						productIdForRenew = connect.getJSONObject(0).getString("uniqueProductName") + ",RENEW";
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
						Template template = utilService.createGenericTemplate(messageId, chatBotService, userLocale, botWebserviceMessage, jsonResponse, phoneNumber, consumptionNames);
						MessagePayload mPayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(template));
						messagePayloadList.add(mPayload);
					} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_VIEW_CONNECT_DETAILS) && connect.length() == 0) {
						handlePayload(Constants.PAYLOAD_CHANGE_BUNDLE, messenger, senderId);
					} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_VIEW_CONNECT_DETAILS)) {
						for (int i = 0; i < connect.length(); i++) {
							if (userLocale.contains("ar")) {
								consumptionNames.add(connect.getJSONObject(i).getJSONObject("commercialName").getString("arabicValue"));
							} else {
								consumptionNames.add(connect.getJSONObject(i).getJSONObject("commercialName").getString("englishValue"));
							}
						}
						Template template = utilService.createGenericTemplate(messageId, chatBotService, userLocale, botWebserviceMessage, jsonResponse, phoneNumber, consumptionNames);
						MessagePayload mPayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(template));
						messagePayloadList.add(mPayload);
					} else if (payload.equals(Constants.PAYLOAD_RATEPLAN_ADDONS_CONSUMPTION )) {
						Template template = utilService.createGenericTemplate(messageId, chatBotService, userLocale, botWebserviceMessage, jsonResponse, phoneNumber, consumptionNames);
						MessagePayload mPayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(template));
						messagePayloadList.add(mPayload);
					} else if (payload.equals(Constants.PAYLOAD_MOBILEINTERNET_ADDONS_CONSUMPTION)) {
						Template template = utilService.createGenericTemplate(messageId, chatBotService, userLocale, botWebserviceMessage, jsonResponse, phoneNumber, consumptionNames);
						MessagePayload mPayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(template));
						messagePayloadList.add(mPayload);
					}

				} // Array
			} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 3) {
				if (payload.startsWith(Constants.PREFIX_RATEPLAN_SUBSCRIPTION)) {
					JSONArray bundleArray = new JSONArray(response);
					if (bundleArray.length() > 0) {
						messagePayloadList.add(utilService.getProductsFromJsonByCategory(bundleArray, productIdAndOperationName, senderId, chatBotService, userLocale));
					}
				} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_RELATED_PRODUCTS)) {
					JSONArray bundleArray = new JSONArray(response);
					if (bundleArray.length() > 0) {
						String productId = productIdAndOperationName.split(",")[1];
						messagePayloadList.add(utilService.getRelatedProductFromJsonByBundleId(bundleArray, productId, senderId, chatBotService, userLocale));
					}
				} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_CHANGE_BUNDLE)) {
					JSONArray bundleArray = new JSONArray(response);
					if (bundleArray.length() > 0) {
						messagePayloadList.add(utilService.getBundleCategories(bundleArray, senderId, chatBotService, userLocale, phoneNumber));
					} else {
						// additon
						parentPayLoad = null;
						GenericTemplate gtemplate = utilService.CreateGenericTemplateForNotEligiblBundleDials(userLocale, new ArrayList<Button>(), new ArrayList<Element>(), phoneNumber);
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
						messagePayloadList.add(utilService.getExtraMobileInternetAddonsByCategory(categoryArray, senderId, chatBotService, userLocale, addonId));
					}
				}

			}
		}

	}

	private String getLocaleValue(CustomerProfile customer, UserProfile userProfile) {
		String userLocale;
		userLocale = customer.getLocal() == null ? userProfile.locale() : customer.getLocal();
		return userLocale;
	}

	private void sendMultipleMessages(ArrayList<MessagePayload> responses, String senderId) {
		for (MessagePayload response : responses) {
			try {
				messenger.send(response);
			} catch (MessengerApiException | MessengerIOException e) {
				logger.error("Sender ID is " + senderId + " Exceptoion is " + e.getMessage());
			}
			Utils.markAsTypingOff(messenger, senderId);
		}

	}

}
