package com.chatbot.controller;

import static java.util.Optional.empty;

import java.util.ArrayList;
import java.util.List;
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
import org.springframework.web.bind.annotation.CrossOrigin;
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
import com.chatbot.entity.BotInteraction;
import com.chatbot.entity.BotInteractionMessage;
import com.chatbot.entity.BotQuickReplyMessage;
import com.chatbot.entity.BotTextMessage;
import com.chatbot.entity.BotWebserviceMapping;
import com.chatbot.entity.BotWebserviceMessage;
import com.chatbot.services.ChatBotService;
import com.chatbot.util.User;
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
import com.github.messenger4j.send.message.template.common.Element;
import com.github.messenger4j.userprofile.UserProfile;
import com.github.messenger4j.webhook.event.AccountLinkingEvent;
import com.github.messenger4j.webhook.event.PostbackEvent;
import com.github.messenger4j.webhook.event.QuickReplyMessageEvent;
import com.github.messenger4j.webhook.event.TextMessageEvent;

import antlr.debug.Event;
import javassist.bytecode.analysis.Util;

@RestController
@RequestMapping("/callback")
@CrossOrigin(origins = { "http://localhost:4200" })
public class ChatBotController {
	
	private boolean loggedin = false;
	
	private String originalPayLoad = "";
	
	private String parentPayLoad="";
	
	private String phoneNumber="";
	
	@Autowired
	private final Messenger messenger;

	@Autowired
	private ChatBotService chatBotService;

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

				if (event.isTextMessageEvent()) {
					final TextMessageEvent textMessageEvent = event.asTextMessageEvent();
					final String messageId = textMessageEvent.messageId();
					String text = textMessageEvent.text();

					logger.debug("Received text message from '{}' at '{}' with content: {} (mid: {})", senderId,
							timestamp, text, messageId);
					String textToSend = "";
					// boolean isArabicMsg = isProbablyArabic(text);
					// if (isArabicMsg) {
					// text = translateToEn(text);
					// }
					textToSend = "Hey there! how can I help you?";

					// sendQuickReplyMessage(textToSend, messenger, senderId);
					// sendTextMessage(text, messenger, senderId);
					handlePayload(text, messenger, senderId);
				} /*else if(e){
					
				}*/ else if (event.isQuickReplyMessageEvent()) {
					final QuickReplyMessageEvent quickReplyMessageEvent = event.asQuickReplyMessageEvent();
					
					handlePayload(quickReplyMessageEvent.payload(), messenger, senderId);

				} else if (event.isPostbackEvent()) {
					PostbackEvent postbackEvent = event.asPostbackEvent();
					handlePayload(postbackEvent.payload().get(), messenger, senderId);

				} else if (event.isAccountLinkingEvent()) {
					AccountLinkingEvent accountLinkingEvent = event.asAccountLinkingEvent();
					if (accountLinkingEvent.status().equals(AccountLinkingEvent.Status.LINKED)) {
						loggedin = true;
						phoneNumber = accountLinkingEvent.authorizationCode().get();
						handlePayload(originalPayLoad, messenger, senderId);
					} else {

					}
					// sendQuickReplyMessage(authorizationCode, messenger, senderId);
				}

			});
			logger.debug("Processed callback payload successfully");
			return ResponseEntity.status(HttpStatus.OK).build();
		} catch (MessengerVerificationException e) {
			logger.warn("Processing of callback payload failed: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
	}

	private void sendTextMessage(String text, Messenger messenger, String senderId) {

		final String recipientId = senderId;
		final MessagePayload payload = MessagePayload.create(recipientId, MessagingType.RESPONSE,
				TextMessage.create(text));

		try {
			messenger.send(payload);
		} catch (MessengerApiException | MessengerIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void handlePayload(String payload, Messenger messenger, String senderId) {
		String userFirstName = "";
		try {
			UserProfile userProfile = messenger.queryUserProfile(senderId);
			 userFirstName = userProfile.firstName();
		} catch (MessengerApiException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (MessengerIOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			String text;
			String payloadWithoutPrefix = "";
			if (payload.startsWith("SUB__")) {
				payload = "SUB__";
				payloadWithoutPrefix = payload.substring(5);
			}else if(payload.equalsIgnoreCase("logout")) {
				if(loggedin == true) {
					loggedin = false;
				}
			}
			BotInteraction botInteraction = chatBotService.findInteractionByPayload(payload);
			ArrayList<MessagePayload> messagePayloadList = new ArrayList<>();
			if (!botInteraction.getIsSecure() || loggedin == true) {
				List<BotInteractionMessage> interactionMessageList = chatBotService
						.findInteractionMessagesByInteractionId(botInteraction.getInteractionId());
				MessagePayload messagePayload = null;
				for (BotInteractionMessage botInteractionMessage : interactionMessageList) {

					Long messageTypeId = botInteractionMessage.getBotMessageType().getMessageTypeId();
					Long messageId = botInteractionMessage.getMessageId();

					if (botInteractionMessage.getIsStatic()) {
						messagePayload = Utils.responseInCaseStaticScenario(payload, senderId, userFirstName, botInteraction,
								 botInteractionMessage, messageTypeId, messageId,chatBotService,parentPayLoad);
						messagePayloadList.add(messagePayload);
					}
					// Dynamic Scenario
					else {
						parentPayLoad = botInteraction.getParentPayLoad();
						String jsonBodyString = "";
						BotWebserviceMessage botWebserviceMessage = chatBotService
								.findWebserviceMessageByMessageId(messageId);
				//		String response = Utils.callWebService(botWebserviceMessage);
						if(payload.equalsIgnoreCase("change bundle")) {
							jsonBodyString	 = new String(Utils.getArrayRespo());	
						}else if(payload.equalsIgnoreCase("view bundle details")) {
							jsonBodyString = Utils.getResponse();
						}
						
						System.out.println("Static String");
						
						// Text Message
						if (messageTypeId == Utils.MessageTypeEnum.TEXTMESSAGE.getValue()) {
							Utils.createTextMessageInDynamicScenario(senderId, messagePayloadList, botWebserviceMessage,
									jsonBodyString,chatBotService);
						}else if(messageTypeId == Utils.MessageTypeEnum.ButtonTemplate.getValue()) {
							// string
						//	String buttonResponse = Utils.callWebService(botWebserviceMessage);
							if (botWebserviceMessage.getOutType().getInOutTypeId() == 1) {
								// Object
							} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 2) {
								 
								//Array
							} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 3) {
								JSONArray jsonArray = new JSONArray(jsonBodyString);
								List<BotButton> WSMSGButtons = chatBotService.findAllButtonsByWebserviceMessage(botWebserviceMessage);
								JSONObject jsonObject = new JSONObject();
								List<Button> realButtons = new ArrayList<>();									
									for(int j = 0 ;j<jsonArray.length();j++) {
										Button realButton = null;
										for(BotButton wsBotButton : WSMSGButtons) {
										jsonObject = jsonArray.getJSONObject(j);
										realButton = Utils.createButton(wsBotButton ,"en", jsonObject);
									} 
										realButtons.add(realButton);
								}
															
								   ButtonTemplate buttonTemplate = ButtonTemplate.create(botWebserviceMessage.getTitle().getEnglishText(), realButtons);
								   messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(buttonTemplate));
								   messagePayloadList.add(messagePayload);
								}					
							// Generic Template
						}else if(messageTypeId == Utils.MessageTypeEnum.GENERICTEMPLATEMESSAGE.getValue()) {
							// string
							if (botWebserviceMessage.getOutType().getInOutTypeId() == 1) {
								// Object
							} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 2) {
								
								//Array
							} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 3) {
								
							}
						}
						
						if(parentPayLoad != null) {
						MessagePayload parentMessagePayLoad = Utils.createMessagePayload(parentPayLoad, chatBotService, senderId);
						messagePayloadList.add(parentMessagePayLoad);
						}
					}
				}
				sendMltipleMessages(messagePayloadList);
			}else {
				originalPayLoad = payload;
				MessagePayload messagePayload = null;
				ArrayList<Button> buttons = new ArrayList<Button>();
				BotInteraction loginInteraction = chatBotService.findInteractionByPayload("login");
				List<BotInteractionMessage> loginMessages = chatBotService.findInteractionMessagesByInteractionId(loginInteraction.getInteractionId());
				String title = "";
				for(BotInteractionMessage interactionMSG : loginMessages) {
					Long msgId = interactionMSG.getMessageId();
					Long msgType = interactionMSG.getBotMessageType().getMessageTypeId();
					List<BotButtonTemplateMSG> botButtonTemplates = chatBotService.findBotButtonTemplateMSGByBotInteractionMessage(interactionMSG);
					for(BotButtonTemplateMSG botButtonTemplateMSG : botButtonTemplates) {
						title = botButtonTemplateMSG.getBotText().getEnglishText();
						List<BotButton> botButtons = chatBotService.findAllByBotButtonTemplateMSGId(botButtonTemplateMSG);
						for(BotButton botButton : botButtons) {
							Button button = Utils.createButton(botButton, "en" ,new JSONObject());
							buttons.add(button);
							}
					}					
				}
				ButtonTemplate buttonTemplate = ButtonTemplate.create(title, buttons);
				messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE,
						TemplateMessage.create(buttonTemplate));
				messagePayloadList.add(messagePayload);
				sendMltipleMessages(messagePayloadList);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	

	
	
	
	

	private void sendMltipleMessages(ArrayList<MessagePayload> responses) {

		for (MessagePayload response : responses) {
			try {
				messenger.send(response);
			} catch (MessengerApiException | MessengerIOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void sendQuickReplyMessage(List<QuickReply> quickReplies, String text, Messenger messenger,
			String senderId) {

		final String recipientId = senderId;

		Optional<List<QuickReply>> quickRepliesOp = Optional.of(quickReplies);
		final MessagePayload payload = MessagePayload.create(recipientId, MessagingType.RESPONSE,
				TextMessage.create(text, quickRepliesOp, empty()));

		try {
			messenger.send(payload);
		} catch (MessengerApiException | MessengerIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void sendTemplateMessage(List<Element> elements, Messenger messenger, String recipientId) {
		Template template = GenericTemplate.create(elements);
		final MessagePayload payload = MessagePayload.create(recipientId, MessagingType.RESPONSE,
				TemplateMessage.create(template));

		try {
			messenger.send(payload);
		} catch (MessengerApiException | MessengerIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@RequestMapping(value = "/{param}", method = RequestMethod.GET)
	public ResponseEntity<String> getMsg(@PathVariable("param") String msg) {
		try {
			String output = "Jersey say : " + msg;

			BotWebserviceMessage botWebserviceMessage = chatBotService.findWebserviceMessageByMessageId(6l);
			RestTemplate restTemplate = new RestTemplate();

			// Build request headers if exists
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(Utils.getMediaType(botWebserviceMessage.getContentType()));

			if (Utils.isNotEmpty(botWebserviceMessage.getHeaderParams())) {
				
			}

			// Build request body
			HttpEntity<String> entity = new HttpEntity<String>("", headers);

			// Get response
			ResponseEntity<String> response = restTemplate.exchange(botWebserviceMessage.getWsUrl(),
					Utils.getHttpMethod(botWebserviceMessage.getBotMethodType().getMethodTypeId()), entity,
					String.class);
			System.out.println(response.getBody());
			String jsonBodyString = response.getBody();
			// jsonBodyString = "{ \"id\":1,\"values\":" + jsonBodyString + "}";
			Object jsonBodyObject = new JSONTokener(jsonBodyString).nextValue();
			JSONObject jsonObject = null;
			JSONArray jsonArray = null;
			if (jsonBodyObject instanceof JSONObject) {
				jsonObject = (JSONObject) jsonBodyObject;
				jsonArray = jsonObject.getJSONArray(botWebserviceMessage.getListParamName());
			} else if (jsonBodyObject instanceof JSONArray) {
				jsonArray = (JSONArray) jsonBodyObject;
			}
			List<Element> elements = new ArrayList<>();
			List<Button> buttonsList = new ArrayList<>();
			if (botWebserviceMessage.getOutType().getInOutTypeId() == 2) {
				String params[] = botWebserviceMessage.getOutputParams().split(",");

				for (String string : params) {
					System.out.println("Param is" + jsonObject.getString(string));
				}

			} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 3) {
				List<BotWebserviceMapping> webServiceMappingList = chatBotService
						.findWebserviceMappingByWsId(botWebserviceMessage.getWsMsgId());

				for (int i = 0; i < jsonArray.length(); i++) {
					buttonsList = new ArrayList<>();
					JSONObject jsonObject2 = jsonArray.getJSONObject(i);
					String title = null;
					String subTitle = null;
					String payload = null;
					for (BotWebserviceMapping botWebserviceMapping : webServiceMappingList) {
						Object valueObject = jsonObject2.get(botWebserviceMapping.getFieldName());
						String value = String.valueOf(valueObject);
						if (3 == Utils.MessageTypeEnum.GENERICTEMPLATEMESSAGE.getValue()) {

							if (botWebserviceMapping.getFieldMapedTo().equals("title"))
								title = value;
							else if (botWebserviceMapping.getFieldMapedTo().equals("subTitle"))
								subTitle = value;
							else if (botWebserviceMapping.getFieldMapedTo().equals("payload"))
								payload = value;

						}
					}
					Button button = PostbackButton.create("Subscribe", payload);
					buttonsList.add(button);

					Element element = Element.create(title, Optional.of(subTitle), empty(), empty(),
							Optional.of(buttonsList));

					elements.add(element);

				}
				Template template = GenericTemplate.create(elements);
				
			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ResponseEntity.status(200).body("Hello");

	}

	public enum ButtonTypeEnum {
		POSTBACK(1), URL(2);
		private final int buttonTypeId;

		private ButtonTypeEnum(int typeId) {
			this.buttonTypeId = typeId;
		}

		public int getValue() {
			return buttonTypeId;
		}
	}

	
	@RequestMapping(value = "/checkUser", method = RequestMethod.POST)
	public ResponseEntity<Void> checkUser(@RequestBody User user) {
		System.out.println("User Name is "+user.getUserName());
		return ResponseEntity.status(HttpStatus.OK).build();
	}

	
	@RequestMapping(value = "/test", method = RequestMethod.GET)
	public ResponseEntity<Void> test() {
		System.out.println("User Name is WebLogic");
		return ResponseEntity.status(HttpStatus.OK).build();
	}

}
