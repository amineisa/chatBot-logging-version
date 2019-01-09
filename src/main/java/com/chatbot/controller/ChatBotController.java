package com.chatbot.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

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

import com.chatbot.entity.BotInteraction;
import com.chatbot.entity.CustomerProfile;
import com.chatbot.entity.InteractionLogging;
import com.chatbot.entity.UserSelection;
import com.chatbot.services.ChatBotService;
import com.chatbot.services.InteractionHandlingService;
import com.chatbot.services.UtilService;
import com.chatbot.util.Constants;
import com.chatbot.util.Utils;
import com.github.messenger4j.Messenger;
import com.github.messenger4j.exception.MessengerVerificationException;
import com.github.messenger4j.webhook.event.AccountLinkingEvent;
import com.github.messenger4j.webhook.event.AttachmentMessageEvent;
import com.github.messenger4j.webhook.event.PostbackEvent;
import com.github.messenger4j.webhook.event.QuickReplyMessageEvent;
import com.github.messenger4j.webhook.event.ReferralEvent;
import com.github.messenger4j.webhook.event.TextMessageEvent;
import com.github.messenger4j.webhook.event.attachment.Attachment;
import com.github.messenger4j.webhook.event.common.Referral;


@RestController
@RequestMapping("/callback")
@SessionScope
public class ChatBotController {

/*	@Autowired
	private final Messenger messenger;*/

	
	
	@Autowired
	private ChatBotService chatBotService;

	@Autowired
	private UtilService utilService;
	
	@Autowired
	InteractionHandlingService interactionHandlingService ;
	
	@Autowired
	Map<String,Messenger> messengersObjectsMap;
	
	Messenger messenger ;
	
	
	private static int counter = 0; 
	//private static CacheHelper<String, Object> wsResponseCache = new CacheHelper<>("usersResponses");

	//private static CacheHelper<String, Object> userSelectionsCache = new CacheHelper<>("usersSelections");


	
	private static final Logger logger = LoggerFactory.getLogger(ChatBotController.class);

	/*@Autowired
	public ChatBotController(final Messenger sendClient) {
		this.messenger = sendClient;

	}*/
	
	@Autowired
	 public ChatBotController(final Map<String,Messenger> messengersObjectsMap ) {
		this.messengersObjectsMap = messengersObjectsMap;
	}

	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<String> verifyWebhook( HttpServletRequest req ,@RequestParam("hub.mode") final String mode, @RequestParam("hub.verify_token") final String verifyToken,
			@RequestParam("hub.challenge") final String challenge) {
		logger.debug("Received Webhook event verification request - mode: {} | verifyToken: {} | challenge: {}", mode, verifyToken, challenge);
		try {
/*			String url = req.getRequestURI();
			String body = req.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);
			req.getParameterNames();*/
			this.messenger = this.messengersObjectsMap.get("347837262369425"); 
			messenger.verifyWebhook(mode, verifyToken);
			return ResponseEntity.status(HttpStatus.OK).body(challenge);
		} catch (MessengerVerificationException e) {
			logger.warn("Webhook verification failed: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
		} 
	}
	
	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<Void> handleCallback(@RequestBody final String payload, @RequestHeader("X-Hub-Signature") final String signature) {
		logger.debug("Counter before  "+counter);
		logger.debug("Received Messenger Platform callback - payload: {} | signature: {}", payload, signature);
		if (payload.contains(Constants.FB_JSON_KEY_STANDBY) ) {
			return botStandbyHandling(payload);
		} else {
			return webhookEventesHandling(payload, signature);
		}
	}
	
	/**
	 * @param payload
	 * @param signature
	 * @return
	 */
	public ResponseEntity<Void> webhookEventesHandling(final String payload, final String signature) {
		try {
			String pageId = new JSONObject(payload).getJSONArray("entry").getJSONObject(0).getString("id");
			this.messenger = this.messengersObjectsMap.get(pageId);
			messenger.onReceiveEvents(payload, Optional.of(signature), event -> {
				final String senderId = event.senderId();
				UserSelection userSelections = interactionHandlingService.getUserSelectionsFromCache(senderId);
						//interactionHandlingService.getUserSelections(senderId);
				Utils.markAsSeen(messenger, senderId);
				if (event.isPostbackEvent()) {
					PostbackEvent postbackEvent = event.asPostbackEvent();
					String pLoad = postbackEvent.payload().get();
					if (pLoad.equalsIgnoreCase(Constants.PAYLOAD_TALK_TO_AGENT)) {
						String phoneNumber = " _ ";
						CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
						BotInteraction interaction = chatBotService.findInteractionByPayload(pLoad);
						if(customerProfile != null ) {
							phoneNumber = customerProfile.getMsisdn();
						}
						interactionHandlingService.callSecondryHandover(senderId,phoneNumber,messenger);
					} else {
						if(pLoad.equalsIgnoreCase(Constants.PAYLOAD_WELCOME_AGAIN)){
							logger.debug("TAke Thread Control");
							try {
							interactionHandlingService.takeThreadControl(senderId,messenger);
							}catch (Exception e) {
								e.printStackTrace();
							}
						}
						interactionHandlingService.handlePayload(postbackEvent.payload().get(), messenger, senderId);
					}
				} else if (event.isAccountLinkingEvent()) {
					AccountLinkingEvent accountLinkingEvent = event.asAccountLinkingEvent();
					logger.debug("ACCOUNT LINKING EVENT");
					if ((accountLinkingEvent.status().equals(AccountLinkingEvent.Status.LINKED))) {
						String msisdnR = accountLinkingEvent.authorizationCode().get();
						logger.debug("LINKED MSISDN "+msisdnR);
						CustomerProfile customerProfile = utilService.setLinkingInfoForCustomer(senderId, accountLinkingEvent.authorizationCode().get(), chatBotService);
						Utils.setLinkedDial(customerProfile, chatBotService);
						interactionHandlingService.handlePayload(userSelections.getOriginalPayLoad(), messenger, senderId);							
					} else if (accountLinkingEvent.status().equals(AccountLinkingEvent.Status.UNLINKED)) {
						String msisdn = chatBotService.getCustomerProfileBySenderId(senderId).getMsisdn();
						logger.debug("UNLINKED MSISDN "+msisdn);
						Utils.updateUnlinkindDate( chatBotService,msisdn);	
						interactionHandlingService.handlePayload(Constants.PAYLOAD_WELCOME_AGAIN, messenger, senderId);
					}
				} else if (event.isTextMessageEvent()) {
					final TextMessageEvent textMessageEvent = event.asTextMessageEvent();
					String text = textMessageEvent.text();
					interactionHandlingService.handlePayload(text, messenger, senderId);
				} else if (event.isQuickReplyMessageEvent()) {
					final QuickReplyMessageEvent quickReplyMessageEvent = event.asQuickReplyMessageEvent();
					interactionHandlingService.handlePayload(quickReplyMessageEvent.payload(), messenger, senderId);
				}else if(event.isReferralEvent()) {
					final ReferralEvent referralEvent = event.asReferralEvent();
					Referral referral= referralEvent.referral();
				    logger.debug("SOURCE IS "+referral.source());
					logger.debug("REFERRAL PAYLOAD "+referral.refPayload());
					logger.debug("REFERRAL TYPE "+referral.type());
				}else if(event.isAttachmentMessageEvent()) {
					// double latitude , longitude ;
					// latitude = longitude = 0;
					 AttachmentMessageEvent attachmentEvent = event.asAttachmentMessageEvent();
					 List<Attachment> attachements = attachmentEvent.attachments();
					 attachements.forEach(attachement ->{
						 if(attachement.isLocationAttachment()) {
							 Map<String,Long> location = new HashMap<String,Long>();  
							 double latitude  =  attachement.asLocationAttachment().latitude();
							 double longitude = attachement.asLocationAttachment().longitude();
							// location.put("latitude", value);
							// location.put("longitude", value)
						 }
					 });
				}

			});
			logger.debug(" Processed callback payload successfully");
			return ResponseEntity.status(HttpStatus.OK).build();
		} catch (MessengerVerificationException e) {
			logger.warn("sProcessing of callback payload failed: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
	}

	/**
	 * @param payload
	 * @return
	 */
	public ResponseEntity<Void> botStandbyHandling(final String payload) {
		counter++;
		if(payload.contains(Constants.FB_JSON_KEY_MESSAGE) && counter == 1 ) {
			logger.debug("Counter in if statement "+counter);
		String recivedFreeText = "";
		logger.debug("Bot is standby mode thread controlle is with agent ");
		logger.debug("PAYLOAD in case standby status "+payload);
		JSONObject standbyJsonObject  = new JSONObject(payload);
		BotInteraction botInteraction = chatBotService.findInteractionByPayload(Constants.PAYLOAD_FREE_TEXT);
		recivedFreeText = standbyJsonObject.getJSONArray(Constants.FB_JSON_KEY_ENTRY).getJSONObject(0).getJSONArray(Constants.FB_JSON_KEY_STANDBY).getJSONObject(0).getJSONObject(Constants.FB_JSON_KEY_MESSAGE).getString(Constants.FB_JSON_KEY_FREE_TEXT);
		String senderId = standbyJsonObject.getJSONArray(Constants.FB_JSON_KEY_ENTRY).getJSONObject(0).getJSONArray(Constants.FB_JSON_KEY_STANDBY).getJSONObject(0).getJSONObject(Constants.FB_JSON_KEY_SENDER).getString(Constants.FB_JSON_KEY_ID);
		CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		if(customerProfile!= null) {
			InteractionLogging interactionLogging = Utils.interactionLogginghandling(customerProfile, botInteraction, chatBotService);
			Utils.freeTextinteractionLogginghandling(interactionLogging, recivedFreeText , chatBotService);
		}	}	
		if(counter > 1) {
			counter = 0;
		}
		return ResponseEntity.status(HttpStatus.OK).build();
	}

	
	
	
	

	


	
	
	

}
