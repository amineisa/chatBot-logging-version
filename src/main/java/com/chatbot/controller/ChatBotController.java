package com.chatbot.controller;

import static java.util.Optional.empty;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.chatbot.entity.BotButton;
import com.chatbot.entity.BotButtonTemplateMSG;
import com.chatbot.entity.BotGTemplateMessage;
import com.chatbot.entity.BotInteraction;
import com.chatbot.entity.BotInteractionMessage;
import com.chatbot.entity.BotTextResponseMapping;
import com.chatbot.entity.BotWebserviceMapping;
import com.chatbot.entity.BotWebserviceMessage;
import com.chatbot.entity.CustomerProfile;
import com.chatbot.entity.InteractionLogging;
import com.chatbot.services.ChatBotService;
import com.chatbot.services.UtilService;
import com.chatbot.util.Utils;
import com.github.messenger4j.Messenger;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerIOException;
import com.github.messenger4j.exception.MessengerVerificationException;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.MessagingType;
import com.github.messenger4j.send.message.Message;
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
import com.github.messenger4j.webhook.event.AccountLinkingEvent;
import com.github.messenger4j.webhook.event.PostbackEvent;
import com.github.messenger4j.webhook.event.QuickReplyMessageEvent;
import com.github.messenger4j.webhook.event.TextMessageEvent;

import javassist.bytecode.analysis.Util;

@RestController
@RequestMapping("/callback")
// @CrossOrigin(origins = { "http://stg.etisalat.eg" })
public class ChatBotController {

	private boolean loggedin = false;

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
	public ResponseEntity<String> verifyWebhook(@RequestParam("hub.mode") final String mode,
			@RequestParam("hub.verify_token") final String verifyToken,
			@RequestParam("hub.challenge") final String challenge) {

		logger.debug("Received Webhook verification request - mode: {} | verifyToken: {} | challenge: {}", mode,
				verifyToken, challenge);
		try {
			this.messenger.verifyWebhook(mode, verifyToken);
			return ResponseEntity.status(HttpStatus.OK).body(challenge);
		} catch (MessengerVerificationException e) {
			logger.warn("Webhook verification failed: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
		}
	}

	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<Void> handleCallback(@RequestBody final String payload,
			@RequestHeader("X-Hub-Signature") final String signature) {

		logger.debug("Received Messenger Platform callback - payload: {} | signature: {}", payload, signature);
		try {
			// this.receiveClient.processCallbackPayload(payload, signature);
			messenger.onReceiveEvents(payload, Optional.of(signature), event -> {

				final String senderId = event.senderId();
				final java.time.Instant timestamp = event.timestamp();

				if (event.isQuickReplyMessageEvent()) {
					Utils.markAsSeen(messenger, senderId);
					final QuickReplyMessageEvent quickReplyMessageEvent = event.asQuickReplyMessageEvent();
					handlePayload(quickReplyMessageEvent.payload(), messenger, senderId);

				} else if (event.isPostbackEvent()) {
					Utils.markAsSeen(messenger, senderId);
					PostbackEvent postbackEvent = event.asPostbackEvent();
					handlePayload(postbackEvent.payload().get(), messenger, senderId);

				} else if (event.isAccountLinkingEvent()) {
					Utils.markAsSeen(messenger, senderId);
					AccountLinkingEvent accountLinkingEvent = event.asAccountLinkingEvent();
					if (accountLinkingEvent.status().equals(AccountLinkingEvent.Status.LINKED)) {
						CustomerProfile customerProfile = utilService.setLinkingInfoForCustomer(senderId, messenger,
								accountLinkingEvent.authorizationCode().get(), chatBotService);
						chatBotService.saveCustomerProfile(customerProfile);

						// phoneNumber = accountLinkingEvent.authorizationCode().get();
						handlePayload(originalPayLoad, messenger, senderId);
					} else if (accountLinkingEvent.status().equals(AccountLinkingEvent.Status.UNLINKED)) {
						Utils.markAsSeen(messenger, senderId);
						Utils.userLogout(senderId, chatBotService);

					}
				}
			});
			logger.debug("Processed callback payload successfully");
			return ResponseEntity.status(HttpStatus.OK).build();
		} catch (MessengerVerificationException e) {
			logger.warn("Processing of callback payload failed: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
	}

	private void handlePayload(String payload, Messenger messenger, String senderId) {
		String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
		logger.debug("Methoud Name Is "+methodName+" Parameters names Payload and sender ID values are  "+payload +" "+senderId);
		Utils.markAsTypingOn(messenger, senderId);
		payload = payLoadSettings(payload);
		String parentPayLoad = null;
		CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		String userFirstName = "";
		String userLocale = "";
		UserProfile userProfile = null;
		try {
			Date date = new Date();
			Timestamp timestamp = new Timestamp(date.getTime());
			userProfile = messenger.queryUserProfile(senderId);
			userFirstName = userProfile.firstName();
			CustomerProfile customer = null;
			try {
				userLocale = getLocaleValue(senderId, userProfile);
			} catch (NullPointerException nullPointerException) {
				customer = new CustomerProfile();
				logger.error("Sender ID is "+senderId +" Exceptoion is "+nullPointerException.getMessage());
			}
		} catch (MessengerApiException e1) {
			logger.error("Sender ID is "+senderId +" Exceptoion is "+e1.getMessage());
		} catch (MessengerIOException e1) {
			logger.error("Sender ID is "+senderId +" Exceptoion is "+e1.getMessage());
		}

		try {
			ArrayList<MessagePayload> messagePayloadList = new ArrayList<>();
			if (payload.equalsIgnoreCase("en_us")) {
				utilService.setCustomerProfileLocalAsEnglish(customerProfile, chatBotService);
				messagePayloadList.add(utilService.changeLanguageResponse("en_us", senderId));
				sendMultipleMessages(messagePayloadList,senderId);
			} else if (payload.equalsIgnoreCase("ar")) {
				utilService.setCustomerProfileLocalAsArabic(customerProfile, chatBotService);
				messagePayloadList.add(utilService.changeLanguageResponse("ar", senderId));
				sendMultipleMessages(messagePayloadList,senderId);
			} else {
				BotInteraction botInteraction = chatBotService.findInteractionByPayload(payload);
				if (botInteraction == null) {
					handlePayload("unexpected", messenger, senderId);
				}
				Utils.interactionLogginghandling(customerProfile, botInteraction, chatBotService);
				
				if (customerProfile != null) {
					try {
						phoneNumber = customerProfile.getMsisdn() != null ? customerProfile.getMsisdn() : "";
						logger.error(botInteraction.toString() + " Dial is "+phoneNumber);//(botInteraction.toString());
						//System.out.println(botInteraction.toString());
					} catch (NullPointerException e) {
						phoneNumber = "";
						logger.error("Sender ID is "+senderId +" Exceptoion is "+e.getMessage());
					}
				}

				if (!botInteraction.getIsSecure() || phoneNumber.length() > 0) {
					parentPayLoad = botInteraction.getParentPayLoad();
					List<BotInteractionMessage> interactionMessageList = chatBotService
							.findInteractionMessagesByInteractionId(botInteraction.getInteractionId());
					MessagePayload messagePayload = null;
					for (BotInteractionMessage botInteractionMessage : interactionMessageList) {

						Long messageTypeId = botInteractionMessage.getBotMessageType().getMessageTypeId();
						Long messageId = botInteractionMessage.getMessageId();

						if (botInteractionMessage.getIsStatic()) {
							messagePayload = utilService.responseInCaseStaticScenario(payload, senderId, userFirstName,
									botInteraction, botInteractionMessage, messageTypeId, messageId, chatBotService,
									parentPayLoad, userLocale, phoneNumber);
							CustomerProfile newCustomerProfile = Utils
									.updateCustomerLastSeen(chatBotService.getCustomerProfileBySenderId(senderId));
							chatBotService.saveCustomerProfile(newCustomerProfile);
							messagePayloadList.add(messagePayload);
						}
						// Dynamic Scenario
						else {
							dynamicScenarioController(payload, messenger, senderId, customerProfile, userLocale,
									botInteraction, messagePayloadList, messageTypeId, messageId,parentPayLoad);
						}
					}
					sendMultipleMessages(messagePayloadList,senderId);
					if(payload.equalsIgnoreCase("change bundle")) {
						parentPayLoad = null;
						lastPayLoad = payload;
					}
					if (parentPayLoad != null && lastPayLoad =="" ) {
						Utils.markAsTypingOn(messenger, senderId);
						handlePayload(parentPayLoad, messenger, senderId);
					}
				} else {
					userLocale = getLocaleValue(senderId, userProfile);
					CustomerProfile newCustomerProfile = Utils
							.updateCustomerLastSeen(chatBotService.getCustomerProfileBySenderId(senderId));
					chatBotService.saveCustomerProfile(newCustomerProfile);
					originalPayLoad = payload;
					MessagePayload messagePayload = null;
					ArrayList<Button> buttons = new ArrayList<Button>();
					BotInteraction loginInteraction = chatBotService.findInteractionByPayload("login");
					List<BotInteractionMessage> loginMessages = chatBotService
							.findInteractionMessagesByInteractionId(loginInteraction.getInteractionId());
					String title = "";
					for (BotInteractionMessage interactionMSG : loginMessages) {
						Long msgId = interactionMSG.getMessageId();
						Long msgType = interactionMSG.getBotMessageType().getMessageTypeId();
						List<BotButtonTemplateMSG> botButtonTemplates = chatBotService
								.findBotButtonTemplateMSGByBotInteractionMessage(interactionMSG);
						for (BotButtonTemplateMSG botButtonTemplateMSG : botButtonTemplates) {
							title = utilService.getTextForButtonTemplate(userLocale, botButtonTemplateMSG);
							List<BotButton> botButtons = chatBotService
									.findAllByBotButtonTemplateMSGId(botButtonTemplateMSG);
							for (BotButton botButton : botButtons) {
								Button button = utilService.createButton(botButton, userLocale, new JSONObject(),
										phoneNumber);
								buttons.add(button);
							}
						}
					}
					ButtonTemplate buttonTemplate = ButtonTemplate.create(title, buttons);
					messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE,
							TemplateMessage.create(buttonTemplate));
					messagePayloadList.add(messagePayload);
					sendMultipleMessages(messagePayloadList,senderId);
				}
			}
		} catch (Exception e) {
			logger.error("Sender ID is "+senderId +" Exceptoion is "+e.getMessage() +" Method Name "+methodName);
		}

	}

	private String payLoadSettings(String payload) {
		if (payload.startsWith("subaddon_")) {
			addonId = payload.substring(10, payload.length()) + "," + "ACTIVATE";
			payload = "subscribe addon";
		}
		if (payload.startsWith("sub_")) {
			productIdAndOperationName = payload.substring(4, payload.length());
			payload = payload.substring(0, 4);
		} else if (payload.startsWith("MIAddon")) {
			addonId = payload.substring(7, payload.length());
			payload = "buy addons";
			// payload.substring(0,7);
		} else if (payload.startsWith("related")) {
			productIdAndOperationName = payload;
			payload = "related product";
			// payload.substring(0,payload.indexOf(","));

		} else if (payload.contains(",") && !payload.contains("relatedproductsubscription")) {
			productIdAndOperationName = payload;
			String[] parameters = productIdAndOperationName.split(",");
			parametersListForRelatedProducts = new ArrayList<String>(Arrays.asList(parameters));
			payload = "MI Bundle subscription confirmation msg";
		} else if (payload.equalsIgnoreCase("mi_yes_subscripe")) {
			if (parametersListForRelatedProducts.size() == 3) {
				payload = "mi_yes_subscripe_related_product";
			} else if (parametersListForRelatedProducts.size() == 2) {
				payload = "mi_yes_subscripe";
			}
		} else if (payload.contains("relatedproductsubscription")) {
			String[] parameters = productIdAndOperationName.split(",");
			int length = parameters.length;
			// parameters[length+1] = payload.split(",")[0];
			parametersListForRelatedProducts = new ArrayList<String>(Arrays.asList(parameters));
			parametersListForRelatedProducts.add(payload.split(",")[0]);
			parametersListForRelatedProducts.remove(0);
			// productIdAndOperationNameAndRelatedProducts = productIdAndOperationName;
			System.out.println(parametersListForRelatedProducts.size());
			payload = "MI Bundle subscription confirmation msg";

		} else if (payload.equalsIgnoreCase("mi_no_subscripe")) {
			payload = "change bundle";
		} else if (payload.equalsIgnoreCase("cancel recharging") || payload.equalsIgnoreCase("cancel paying bill")) {
			payload = "cancel pay or recharge";
		} else if (payload.contains("MIAddon")) {
			addonId = payload.substring(7, payload.length());
			payload = "buy addons";

		}
		return payload;
	}

	private void dynamicScenarioController(String payload, Messenger messenger, String senderId,
			CustomerProfile customerProfile, String userLocale, BotInteraction botInteraction,
			ArrayList<MessagePayload> messagePayloadList, Long messageTypeId, Long messageId,String parentPayLoad) throws JSONException {
		String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
		logger.debug("Methoud Name Is "+methodName);
		logger.debug("Parameters names Payload , senderID , user Locale and Interaction  values are  "+payload +" "+senderId+" "+botInteraction.toString());
		MessagePayload messagePayload;
		CustomerProfile newCustomerProfile = Utils
				.updateCustomerLastSeen(chatBotService.getCustomerProfileBySenderId(senderId));
		chatBotService.saveCustomerProfile(newCustomerProfile);
		CustomerProfile customerProfileOne = chatBotService.getCustomerProfileBySenderId(senderId);
		Date dateOne = new Date();
		customerProfile.setCustomerLastSeen(new Timestamp(dateOne.getTime()));
		chatBotService.saveCustomerProfile(customerProfile);
	     parentPayLoad = botInteraction.getParentPayLoad();
		// String jsonBodyString = "";
		BotWebserviceMessage botWebserviceMessage = chatBotService.findWebserviceMessageByMessageId(messageId);
		String response = "";
		Map<String, String> mapResponse = new HashMap<String, String>();
		if (botWebserviceMessage.getBotMethodType().getMethodTypeId() == 1) {
			if(payload.equalsIgnoreCase("change bundle")) {
				mapResponse = utilService.getEligibleProducts(botWebserviceMessage, senderId, chatBotService);
			}else if(payload.equalsIgnoreCase("view connect details") || payload.equalsIgnoreCase("view rateplan and connect details")
					|| payload.equalsIgnoreCase("view rateplan details") || payload.equalsIgnoreCase("rateplan details") || payload.equalsIgnoreCase("consumption")) {
				mapResponse = utilService.getSubscriberProfile(botWebserviceMessage, senderId, chatBotService);
			}else {
				mapResponse = utilService.callGetWebService(botWebserviceMessage, senderId, chatBotService);
			}
			if (mapResponse.get("status").equals("200")) {
				response = mapResponse.get("response");
			} else {
				// TextMessage textMSG = TextMessage.create("Sorry You can try again later");
				handlePayload("fault MSG", messenger, senderId);
			}
		} else if (botWebserviceMessage.getBotMethodType().getMethodTypeId() == 2) {
			JSONObject jsonParam = new JSONObject();
			ArrayList<String> paramValuesList = new ArrayList<String>();
			if (productIdAndOperationName != null && productIdAndOperationName.length() > 10) {
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
			} else if (productIdForRenew.length() > 0 && !payload.equals("subscribe addon")) {
				String paramName = botWebserviceMessage.getListParamName();
				String paramNames[] = paramName.split(",");
				if (paramNames.length == 2) {
					paramValuesList = new ArrayList<>(Arrays.asList(productIdForRenew.split(",")));
				}
				for (int p = 0; p < paramNames.length; p++) {
					jsonParam.put(paramNames[p], paramValuesList.get(p));
				}
			} else if (addonId.length() > 0 && payload.equals("subscribe addon")) {
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
			mapResponse = utilService.callPostWebService(botWebserviceMessage, stringParam, chatBotService, senderId,
					paramValuesList);
			if (mapResponse.get("status").equals("200")) {
				response = mapResponse.get("response");
			}
		}
		// Text Message
		if (messageTypeId == Utils.MessageTypeEnum.TEXTMESSAGE.getValue()) {
			utilService.createTextMessageInDynamicScenario(senderId, messagePayloadList, botWebserviceMessage, response,
					chatBotService, userLocale);
		} else if (messageTypeId == Utils.MessageTypeEnum.ButtonTemplate.getValue()) {
			// string
			// String buttonResponse = utilServicecallWebService(botWebserviceMessage);
			if (botWebserviceMessage.getOutType().getInOutTypeId() == 1) {
				// Object
			} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 2) {

				// Array
			} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 3) {
				JSONArray jsonArray = new JSONArray(response);
				if (jsonArray.length() > 0) {
					List<BotButton> WSMSGButtons = chatBotService
							.findAllButtonsByWebserviceMessage(botWebserviceMessage);
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
					ButtonTemplate buttonTemplate = ButtonTemplate
							.create(botWebserviceMessage.getTitle().getEnglishText(), realButtons);
					messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE,
							TemplateMessage.create(buttonTemplate));
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
				if (payload.equalsIgnoreCase("account details")) {
					Boolean postPaid = jsonResponse.getBoolean("postPaid");
					if (postPaid == false) {
						JSONObject balance = jsonResponse.getJSONObject("balance");
						if (balance.equals(null)) {
							handlePayload("fault MSG", messenger, senderId);
						} else {
							handlePayload("prepaid", messenger, senderId);
						}
					} else {
						Object billingProfileModel = jsonResponse.get("billingProfileModel");
						if (billingProfileModel.equals(null)) {
							handlePayload("fault MSG", messenger, senderId);
						} else {
							handlePayload("postpaid", messenger, senderId);
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
					if (payload.equalsIgnoreCase("consumption")) {
						if (ratePlan.length() > 0 && connect.length() > 0) {
							handlePayload("view rateplan and connect details", messenger, senderId);
						} else if (ratePlan.length() > 0 && connect.length() == 0) {
							handlePayload("view rateplan details", messenger, senderId);
						} else if (ratePlan.length() == 0 && connect.length() > 0) {
							handlePayload("view root connect details", messenger, senderId);
						} else if (ratePlan.length() == 0 && connect.length() == 0) {
							// show subscribe mobile internet button
							handlePayload("view mobile internet subscribe", messenger, senderId);
						}
					} else if (payload.equalsIgnoreCase("rateplan details")) {
						Template template = utilService.createGenericTemplate(messageId, chatBotService, userLocale,
								botWebserviceMessage, jsonResponse, phoneNumber, consumptionNames);
						MessagePayload mPayload = MessagePayload.create(senderId, MessagingType.RESPONSE,
								TemplateMessage.create(template));
						messagePayloadList.add(mPayload);
					} else if (payload.equalsIgnoreCase("view connect details") && connect.length() == 0) {
						
						handlePayload("change bundle", messenger, senderId);
					} else if (payload.equalsIgnoreCase("view connect details")) {

						for (int i = 0; i < connect.length(); i++) {
							System.out.println(connect);
							if (userLocale.contains("ar")) {
								consumptionNames.add(connect.getJSONObject(i).getJSONObject("commercialName")
										.getString("arabicValue"));
							} else {
								consumptionNames.add(connect.getJSONObject(i).getJSONObject("commercialName")
										.getString("englishValue"));
							}
						}
						Template template = utilService.createGenericTemplate(messageId, chatBotService, userLocale,
								botWebserviceMessage, jsonResponse, phoneNumber, consumptionNames);
						MessagePayload mPayload = MessagePayload.create(senderId, MessagingType.RESPONSE,
								TemplateMessage.create(template));
						messagePayloadList.add(mPayload);
					} else if (payload.equals("rateplan addons consumption")) {
						Template template = utilService.createGenericTemplate(messageId, chatBotService, userLocale,
								botWebserviceMessage, jsonResponse, phoneNumber, consumptionNames);
						MessagePayload mPayload = MessagePayload.create(senderId, MessagingType.RESPONSE,
								TemplateMessage.create(template));
						messagePayloadList.add(mPayload);
					} else if (payload.equals("mobile internet addon consumption")) {
						Template template = utilService.createGenericTemplate(messageId, chatBotService, userLocale,
								botWebserviceMessage, jsonResponse, phoneNumber, consumptionNames);
						MessagePayload mPayload = MessagePayload.create(senderId, MessagingType.RESPONSE,
								TemplateMessage.create(template));
						messagePayloadList.add(mPayload);
					}

				} // Array
			} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 3) {
				if (payload.startsWith("sub_")) {
					JSONArray bundleArray = new JSONArray(response);
					if (bundleArray.length() > 0) {
						messagePayloadList.add(utilService.getProductsFromJsonByCategory(bundleArray,
								productIdAndOperationName, senderId, chatBotService, userLocale));
					}
				} else if (payload.equalsIgnoreCase("related product")) {
					JSONArray bundleArray = new JSONArray(response);
					if (bundleArray.length() > 0) {
						String productId = productIdAndOperationName.split(",")[1];
						messagePayloadList.add(utilService.getRelatedProductFromJsonByBundleId(bundleArray, productId,
								senderId, chatBotService, userLocale));
					}
				} else if (payload.equalsIgnoreCase("change bundle")) {
					JSONArray bundleArray = new JSONArray(response);
					if (bundleArray.length() > 0) {
						messagePayloadList.add(utilService.getBundleCategories(bundleArray, senderId, chatBotService,
								userLocale, phoneNumber));
					}else {
						// additon
						parentPayLoad = null;
						GenericTemplate gtemplate = utilService.CreateGenericTemplateForNotEligiblBundleDials(userLocale, new ArrayList<Button>(), new ArrayList<Element>(), phoneNumber);
					    messagePayloadList.add(MessagePayload.create(senderId, MessagingType.RESPONSE,TemplateMessage.create(gtemplate)));	
					}
				} else if (payload.equalsIgnoreCase("buy addons root")) {
					JSONArray categoryArray = new JSONArray(response);
					if (categoryArray.length() > 0) {
						messagePayloadList.add(utilService.getCategoryForMobileInternetAddons(categoryArray, senderId,
								chatBotService, userLocale));
					}
				} else if (payload.equalsIgnoreCase("buy addons")) {
					JSONArray categoryArray = new JSONArray(response);
					if (categoryArray.length() > 0) {
						messagePayloadList.add(utilService.getExtraMobileInternetAddonsByCategory(categoryArray,
								senderId, chatBotService, userLocale, addonId));
					}
				}

			}
		}

	}

	private String getLocaleValue(String senderId, UserProfile userProfile) {
		String userLocale;
		CustomerProfile customer;
		customer = chatBotService.getCustomerProfileBySenderId(senderId) == null ? customer = new CustomerProfile()
				: chatBotService.getCustomerProfileBySenderId(senderId);
		userLocale = customer.getLocal() == null ? userProfile.locale() : customer.getLocal();
		return userLocale;
	}

	private void sendMultipleMessages(ArrayList<MessagePayload> responses,String senderId) {
		for (MessagePayload response : responses) {
			try {
				messenger.send(response);
			} catch (MessengerApiException | MessengerIOException e) {
				logger.error("Sender ID is "+senderId +" Exceptoion is "+e.getMessage());
			}
			Utils.markAsTypingOff(messenger, senderId);
		}
		
	}

	


}
