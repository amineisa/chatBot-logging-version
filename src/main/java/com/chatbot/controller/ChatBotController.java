package com.chatbot.controller;

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
import com.chatbot.services.ChatBotService;
import com.chatbot.services.WebHookEventService;
import com.chatbot.util.Constants;
import com.chatbot.util.Utils;
import com.github.messenger4j.exception.MessengerVerificationException;

/**
 * @author Amin Eisa
 */
@RestController
@RequestMapping("/callback")
@SessionScope
public class ChatBotController {

	@Autowired
	private ChatBotService chatBotService;
	@Autowired
	private WebHookEventService webHookEventService;
	private  int counter = 0;
	

	private static final Logger logger = LoggerFactory.getLogger(ChatBotController.class);


	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<String> verifyWebhook(HttpServletRequest req, @RequestParam("hub.mode") final String mode, @RequestParam("hub.verify_token") final String verifyToken,
			@RequestParam("hub.challenge") final String challenge) {
		return webHookEventService.webHookVerification(mode, verifyToken, challenge);
	}

	

	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<Void> handleCallback(@RequestBody final String payload, @RequestHeader("X-Hub-Signature") final String signature) {
		logger.debug(Constants.LOGGER_INFO_PREFIX + "Received Messenger Platform callback - payload: {} | signature: {}", payload, signature);
		if (payload.contains(Constants.FB_JSON_KEY_STANDBY)) {
			return botStandbyHandling(payload);
		}else{
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
			webHookEventService.allWebhookEventsHandling(payload, signature);
			logger.debug(Constants.LOGGER_INFO_PREFIX + " Processed callback payload successfully");
			return ResponseEntity.status(HttpStatus.OK).build();
		} catch (MessengerVerificationException e) {
			logger.warn(Constants.LOGGER_ERROR_PREFIX + "sProcessing of callback payload failed: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
	}



	/**
	 * @param payload
	 * @return
	 */
	public ResponseEntity<Void> botStandbyHandling(final String payload) {
		counter++;
		if (payload.contains(Constants.FB_JSON_KEY_MESSAGE) && counter == 1) {
			logger.debug(Constants.LOGGER_INFO_PREFIX + "Bot is standby mode thread controlle is with agent ");
			JSONObject standbyJsonObject = new JSONObject(payload);
			BotInteraction botInteraction = chatBotService.findInteractionByPayload(Constants.PAYLOAD_FREE_TEXT);
			String senderId = standbyJsonObject.getJSONArray(Constants.FB_JSON_KEY_ENTRY).getJSONObject(0).getJSONArray(Constants.FB_JSON_KEY_STANDBY).getJSONObject(0)
					.getJSONObject(Constants.FB_JSON_KEY_SENDER).getString(Constants.FB_JSON_KEY_ID);
			CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
			if (customerProfile != null) {
				Utils.interactionLogginghandling(customerProfile, botInteraction, chatBotService);
			}
		}
		if (counter > 1) {
			counter = 0;
		}
		return ResponseEntity.status(HttpStatus.OK).build();
	}

}
