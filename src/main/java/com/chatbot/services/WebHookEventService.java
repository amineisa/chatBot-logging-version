/** copyright Etisalat social team. To present All rights reserved
*/
/**
 * 
 */
package com.chatbot.services;

import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.chatbot.entity.CustomerProfile;
import com.chatbot.entity.UserSelection;
import com.chatbot.util.Constants;
import com.chatbot.util.Utils;
import com.github.messenger4j.Messenger;
import com.github.messenger4j.exception.MessengerVerificationException;
import com.github.messenger4j.webhook.Event;
import com.github.messenger4j.webhook.event.AccountLinkingEvent;
import com.github.messenger4j.webhook.event.PostbackEvent;
import com.github.messenger4j.webhook.event.QuickReplyMessageEvent;
import com.github.messenger4j.webhook.event.TextMessageEvent;

/**
 * @author A.Eissa
 */
@Service
public class WebHookEventService {

	private Messenger messenger;

	@Autowired
	private UtilService utilService;
	@Autowired
	private InteractionHandlingService interactionHandlingService;
	@Autowired
	private Map<String, Messenger> messengerObjectsMap;
	@Autowired
	private ChatBotService chatBotService;

	private static final Logger logger = LoggerFactory.getLogger(WebHookEventService.class);

	@Autowired
	public WebHookEventService(final Map<String, Messenger> messengersObjectsMap) {
		this.messengerObjectsMap = messengersObjectsMap;
	}

	// @Async
	public void allWebhookEventsHandling(final String payload, final String signature) throws MessengerVerificationException {
		String pageId = new JSONObject(payload).getJSONArray(Constants.FB_JSON_KEY_ENTRY).getJSONObject(0).getString(Constants.FB_JSON_KEY_ID);
		this.messenger = this.messengerObjectsMap.get(pageId);
		messenger.onReceiveEvents(payload, Optional.of(signature), event -> {
			final String senderId = event.senderId();
			UserSelection userSelections = utilService.getUserSelectionsFromCache(senderId);
			if (event.isPostbackEvent()) {
				postBackHandling(event, senderId, userSelections);
			} else if (event.isAccountLinkingEvent()) {
				accountLinkingEvent(event, senderId, userSelections);
			} else if (event.isTextMessageEvent()) {
				textMessageHandling(event, senderId, userSelections);
			} else if (event.isQuickReplyMessageEvent()) {
				quickReplyHandling(event, senderId, userSelections);
			}
		});
	}

	/**
	 * @param event
	 * @param senderId
	 */
	private void quickReplyHandling(Event event, final String senderId, UserSelection userSelections) {
		final QuickReplyMessageEvent quickReplyMessageEvent = event.asQuickReplyMessageEvent();
		Instant tStamp = event.asQuickReplyMessageEvent().timestamp();
		logger.debug(Constants.LOGGER_INFO_PREFIX + "QuickReply recieved time Stamp is " + tStamp);
		Instant cachedTimeStamp = userSelections.getEventRecevingTimeStamp() == null ? Instant.now() : userSelections.getEventRecevingTimeStamp();
		logger.debug(Constants.LOGGER_INFO_PREFIX + "Cached time Stamp is " + cachedTimeStamp);
		if (tStamp.compareTo(cachedTimeStamp) != 0) {
			userSelections.setEventRecevingTimeStamp(tStamp);
			utilService.updateUserSelectionsInCache(senderId, userSelections);
			interactionHandlingService.handlePayload(quickReplyMessageEvent.payload(), messenger, senderId);
		} else {
			logger.debug(Constants.LOGGER_INFO_PREFIX + " Repeated Request from facebook");
		}

	}

	/**
	 * @param event
	 * @param senderId
	 * @param userSelections
	 */
	private void postBackHandling(Event event, final String senderId, UserSelection userSelections) {
		Instant tStamp = event.asPostbackEvent().timestamp();
		logger.debug(Constants.LOGGER_INFO_PREFIX + "PostBack recieved timeStamp is " + tStamp);
		PostbackEvent postbackEvent = event.asPostbackEvent();
		Instant cachedTimeStamp = userSelections.getEventRecevingTimeStamp() == null ? Instant.now() : userSelections.getEventRecevingTimeStamp();
		logger.debug(Constants.LOGGER_INFO_PREFIX + "Cached time Stamp is " + cachedTimeStamp);
		if (tStamp.compareTo(cachedTimeStamp) != 0) {
			String pLoad = postbackEvent.payload().get();
			logger.debug(Constants.LOGGER_INFO_PREFIX + " New Request Payload Value is  " + pLoad);
			userSelections.setEventRecevingTimeStamp(tStamp);
			utilService.updateUserSelectionsInCache(senderId, userSelections);
			if (pLoad.equalsIgnoreCase(Constants.PAYLOAD_TALK_TO_AGENT)) {
				talkToAgent(senderId, pLoad);
			} else {
				if (pLoad.equalsIgnoreCase(Constants.PAYLOAD_WELCOME_AGAIN)) {
					logger.debug(Constants.LOGGER_INFO_PREFIX + "Take Thread Control");
					interactionHandlingService.takeThreadControl(senderId, messenger);
				}
				interactionHandlingService.handlePayload(pLoad, messenger, senderId);
			}
		} else {
			logger.debug(Constants.LOGGER_INFO_PREFIX + " Repeated Request from facebook");
		}
	}

	/**
	 * @param event
	 * @param senderId
	 * @param userSelections
	 */
	private void textMessageHandling(Event event, final String senderId, UserSelection userSelections) {
		final TextMessageEvent textMessageEvent = event.asTextMessageEvent();
		Instant tStamp = textMessageEvent.timestamp();
		logger.debug(Constants.LOGGER_INFO_PREFIX + "TextMessage event recieved time Stamp is " + tStamp);
		Instant cachedTimeStamp = userSelections.getEventRecevingTimeStamp() == null ? Instant.now() : userSelections.getEventRecevingTimeStamp();
		logger.debug(Constants.LOGGER_INFO_PREFIX + "Cached time Stamp is " + cachedTimeStamp);
		if (tStamp.compareTo(cachedTimeStamp) != 0) {
			userSelections.setEventRecevingTimeStamp(tStamp);
			utilService.updateUserSelectionsInCache(senderId, userSelections);
			String text = textMessageEvent.text();
			logger.debug(Constants.LOGGER_INFO_PREFIX + " New TextMessage event is Request Text Value is  " + text);
			if (textIsNumberEmeraldDistributeAndTransfer(text) && userSelections.getCurrentOperation().equals(Constants.DISTRIBUTE_OPERATION)) {
				userSelections.setEmeraldDistributeAmount(text);
				utilService.updateUserSelectionsInCache(senderId, userSelections);
				interactionHandlingService.handlePayload(Constants.EMERALD_DISTRIBUTE_SUBMIT_ORDER_PAYLOAD, messenger, senderId);
			} else if (textIsNumberEmeraldDistributeAndTransfer(text) && userSelections.getCurrentOperation().equals(Constants.TRANSFER_OPERATION)) {
				userSelections.setEmeraldDistributeAmount(text);
				utilService.updateUserSelectionsInCache(senderId, userSelections);
				interactionHandlingService.handlePayload(Constants.EMERALD_TRANSFER_SUBMIT_ORDER_PAYLOAD, messenger, senderId);
			} else if (textIsScratchedNumer(text)) {
				logger.debug(Constants.LOGGER_INFO_PREFIX + "Recharge Scratched number " + text);
				userSelections.setScratcheddNumberForRecharge(text);
				utilService.updateUserSelectionsInCache(senderId, userSelections);
				interactionHandlingService.handlePayload(Constants.PAYLOAD_RECHARGE, messenger, senderId);
			} else {
				interactionHandlingService.handlePayload(text, messenger, senderId);
			}
		}
	}

	/**
	 * @param event
	 * @param senderId
	 * @param userSelections
	 */
	private void accountLinkingEvent(Event event, final String senderId, UserSelection userSelections) {
		AccountLinkingEvent accountLinkingEvent = event.asAccountLinkingEvent();
		logger.debug(Constants.LOGGER_INFO_PREFIX + "ACCOUNT LINKING EVENT");
		CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		if ((accountLinkingEvent.status().equals(AccountLinkingEvent.Status.LINKED))) {
			String msisdn = accountLinkingEvent.authorizationCode().get();
			logger.debug(Constants.LOGGER_INFO_PREFIX + "Linked  msisdn " + msisdn);
			utilService.setLinkingInfoForCustomer(senderId, accountLinkingEvent.authorizationCode().get());
			// Utils.setLinkedDial(customerProfile, chatBotService);
			String orginalPayload = userSelections.getOriginalPayLoad() == null ? Constants.PAYLOAD_GET_STARTED : userSelections.getOriginalPayLoad();
			interactionHandlingService.handlePayload(orginalPayload, messenger, senderId);
		} else if (accountLinkingEvent.status().equals(AccountLinkingEvent.Status.UNLINKED)) {
			String msisdn = chatBotService.getCustomerProfileBySenderId(senderId).getMsisdn();
			logger.debug(Constants.LOGGER_INFO_PREFIX + "UNLINKING MSISDN " + msisdn);
			Utils.updateUnlinkindDate(chatBotService, customerProfile.getMsisdn());
			customerProfile.setMsisdn("");
			chatBotService.saveCustomerProfile(customerProfile);
			interactionHandlingService.handlePayload(Constants.PAYLOAD_LOGIN_INTERACTION, messenger, senderId);
		}
	}

	/**
	 * @param senderId
	 * @param pLoad
	 */
	private void talkToAgent(final String senderId, String pLoad) {
		String phoneNumber = " _ ";
		CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		chatBotService.findInteractionByPayload(pLoad);
		if (customerProfile != null) {
			phoneNumber = customerProfile.getMsisdn();
		}
		logger.debug(Constants.LOGGER_INFO_PREFIX + "Send thread control to second app ");
		interactionHandlingService.callSecondryHandover(senderId, phoneNumber, messenger);
	}

	/**
	 * @param text
	 * @return
	 */
	private boolean textIsNumberEmeraldDistributeAndTransfer(String text) {
		return text.length() <= 6 && text.matches("[0-9]+");

	}

	private boolean textIsScratchedNumer(String text) {
		return text.length() > 12 && text.length() > 15 && text.matches("[0-9]+");
	}

	/**
	 * @param mode
	 * @param verifyToken
	 * @param challenge
	 * @return
	 */
	public ResponseEntity<String> webHookVerification(final String mode, final String verifyToken, final String challenge) {
		logger.debug(Constants.LOGGER_INFO_PREFIX + "Received Webhook event verification request - mode: {} | verifyToken: {} | challenge: {}", mode, verifyToken, challenge);
		try {
			Entry<String, Messenger> entry = this.messengerObjectsMap.entrySet().iterator().next();
			this.messenger = this.messengerObjectsMap.get(entry.getKey());
			messenger.verifyWebhook(mode, verifyToken);
			return ResponseEntity.status(HttpStatus.OK).body(challenge);
		} catch (MessengerVerificationException e) {
			logger.warn(Constants.LOGGER_INFO_PREFIX + "Webhook verification failed: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
		}
	}

}
