/** copyright Etisalat social team. To present All rights reserved
*/
/**
 * 
 */
package com.chatbot.services;

import static java.util.Optional.empty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONArray;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.chatbot.entity.BotButton;
import com.chatbot.entity.BotWebserviceMessage;
import com.chatbot.entity.CustomerProfile;
import com.chatbot.entity.InteractionLogging;
import com.chatbot.entity.UserSelection;
import com.chatbot.repo.InteractionLoggingRepo;
import com.chatbot.util.Constants;
import com.chatbot.util.Utils;
import com.github.messenger4j.Messenger;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerIOException;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.MessagingType;
import com.github.messenger4j.send.message.TextMessage;
import com.github.messenger4j.send.message.quickreply.QuickReply;
import com.github.messenger4j.send.message.quickreply.TextQuickReply;
import com.github.messenger4j.webhook.event.attachment.Attachment;
import com.github.messenger4j.webhook.event.attachment.RichMediaAttachment;

/**
 * @author A.Eissa
 */
@Service
public class RasaIntegrationService {

	@Autowired
	private UtilService utilService;
	@Autowired
	private InteractionHandlingService interactionHandlingService;
	@Autowired
	private ChatBotService chatBotService;
	@Autowired
	private InteractionLoggingRepo interactionLoggingRepo;
	
	

	private static final Logger logger = LoggerFactory.getLogger(RasaIntegrationService.class);

	/**Rasa Integration 
	 * @param messenger
	 * @param senderId
	 * @param botWebserviceMessage
	 * @param userSelections
	 */ 
	public void rasaCallingForFreeTextHandling(Messenger messenger, String senderId, BotWebserviceMessage botWebserviceMessage) {
		String paramKey = botWebserviceMessage.getListParamName();
		UserSelection userSelections = utilService.getUserSelectionsFromCache(senderId);
		logger.debug(Constants.LOGGER_INFO_PREFIX + " Rasa service url "+ botWebserviceMessage.getWsUrl());
		JSONObject jsonResponse = rasaIntegration(botWebserviceMessage.getWsUrl(), userSelections.getFreeText(), paramKey);
		rasaResponseHandling(jsonResponse, messenger, senderId);
	}

	
	// Rasa response handling
	private void rasaResponseHandling(JSONObject jsonResponse , Messenger messenger ,String senderId) {
		if(jsonResponse.has(Constants.RASA_RESPONSE_ARRAY_KEY) && jsonResponse.getJSONArray(Constants.RASA_RESPONSE_ARRAY_KEY).length() > 0) {
		JSONArray botResponses = jsonResponse.getJSONArray(Constants.RASA_RESPONSE_ARRAY_KEY);
		CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		UserSelection userSelections = utilService.getUserSelectionsFromCache(senderId);
		for (int i = 0; i < botResponses.length(); i++) {
			JSONObject object = botResponses.getJSONObject(i);
			if (object.keySet().contains(Constants.RASA_RESPONSE_TEXT_KEY)) {
				sendTextMessage(messenger, senderId, object);	
			} else if (object.keySet().contains(Constants.RASA_RESPONSE_ACTION_KEY)) {
				String action = object.getString(Constants.RASA_RESPONSE_ACTION_KEY);
				logger.debug(Constants.LOGGER_INFO_PREFIX +" Rasa returned action is "+action);
				List<InteractionLogging> interactions = interactionLoggingRepo.findAllByCustomerProfileOrderByInteractionCallingDateDesc(customerProfile);
				logger.debug(Constants.LOGGER_INFO_PREFIX +" Check double unexpected action "+interactions.get(1).getBotInteraction().getPayload());
				if(action.equals(interactions.get(1).getBotInteraction().getPayload())) {
					logger.debug(Constants.LOGGER_INFO_PREFIX+"Double unexpected action have been recieved");
					logger.debug(Constants.LOGGER_INFO_PREFIX +"Bot gonna route converstion to agent cause of rasa can't help the client ");
					action=Constants.PAYLOAD_TALK_TO_AGENT;
				}
				interactionHandlingService.handlePayload(action, messenger, senderId);
			}else if(object.keySet().contains(Constants.RAZA_RESPONSE_BUTTON_OPTIONS_KEY)) {
				logger.debug(Constants.LOGGER_INFO_PREFIX +"Rasa runtime payloads handling ");
				handleRunTimeRasaOptions(messenger, senderId, customerProfile, object);
			} else if(object.keySet().contains(Constants.RASA_RESPONSE_DIAL_KEY)) {
				String dial = object.getString(Constants.RASA_RESPONSE_DIAL_KEY);
				if(dial.matches("[0-9]+") && dial.startsWith("01") && dial.length()==11) {
					userSelections.setEmeraldChildDial(dial);
					utilService.updateUserSelectionsInCache(senderId, userSelections);
					interactionHandlingService.handlePayload(Constants.EMERALD_ADD_CHILD_MEMBER_PAYLOAD, messenger, senderId);
				}else {
					interactionHandlingService.handlePayload(Constants.PAYLOAD_ALREADY_LOGGED_IN, messenger, senderId);
				}/*
				userSelections.setUserDialForAuth(dial);
				utilService.updateUserSelectionsInCache(senderId, userSelections);
				interactionHandlingService.handlePayload(Constants.PAYLOAD_DIAL_VALIDITY, messenger, senderId);
				}else {	
				interactionHandlingService.handlePayload(Constants.PAYLOAD_ALREADY_LOGGED_IN, messenger, senderId);
				}*/
			}else if(object.keySet().contains(Constants.RASA_RESPONSE_VERIFICATION_KEY)) {
				String activationCode = object.getString(Constants.RASA_RESPONSE_VERIFICATION_KEY);
				logger.debug(Constants.LOGGER_INFO_PREFIX+"Rasa Auth Verification Code "+activationCode);
				userSelections.setActivationCode(activationCode);
				utilService.updateUserSelectionsInCache(senderId, userSelections);
				interactionHandlingService.handlePayload(Constants.PAYLOAD_VERIFICATION_CODE, messenger, senderId);
			}else if(object.keySet().contains(Constants.RASA_RESPONSE_SCRATCHED_NUMBER)){
				String scratchedNumber = object.getString(Constants.RASA_RESPONSE_SCRATCHED_NUMBER);
				logger.debug(Constants.LOGGER_INFO_PREFIX +"Recharge Scratched number "+scratchedNumber);
				userSelections.setScratcheddNumberForRecharge(scratchedNumber);
				utilService.updateUserSelectionsInCache(senderId, userSelections);
				interactionHandlingService.handlePayload(Constants.PAYLOAD_RECHARGE, messenger, senderId);
			}else {
				logger.debug(Constants.LOGGER_INFO_PREFIX+"Recieving nothing from rasa response ");
				interactionHandlingService.handlePayload(Constants.PAYLOAD_FAULT_MSG, messenger, senderId);
			}
		}
		}else {
			interactionHandlingService.handlePayload(Constants.PAYLOAD_FAULT_MSG, messenger, senderId);
		}
	}


	/**
	 * @param messenger
	 * @param senderId
	 * @param customerProfile
	 * @param object
	 */
	public void handleRunTimeRasaOptions(Messenger messenger, String senderId, CustomerProfile customerProfile, JSONObject object) {
		JSONArray payloads = object.getJSONArray(Constants.RAZA_RESPONSE_BUTTON_OPTIONS_KEY);
		List<QuickReply> quickReplies = new ArrayList<>();
		for(int index = 0;index < payloads.length();index++) {
			String payload = payloads.getString(index);
			BotButton button = chatBotService.findAllButtonsByPayload(payload).get(0);
			if(button != null) {
			String locale = customerProfile.getLocale();
			String label = Utils.getTextValueForButtonLabel(locale, button);
			QuickReply quickReply = TextQuickReply.create(label, payload, empty());
			quickReplies.add(quickReply);
			}
		}
		Optional<List<QuickReply>> quickRepliesOp = Optional.of(quickReplies);
		String text = getOptionsHeaderText(customerProfile.getLocale());
		MessagePayload messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(text , quickRepliesOp, empty()));
		ArrayList<MessagePayload> messages = new ArrayList<>();
		messages.add(messagePayload);
		interactionHandlingService.sendMultipleMessages(messages, senderId, messenger, null);
	}
	
	
	
	/**
	 * @param locale
	 * @return
	 */
	private String getOptionsHeaderText(String locale) {
		if(locale.contains(Constants.LOCALE_AR)) {
			return chatBotService.getBotConfigurationByKey(Constants.RASA_RUNTIME_OPTIONS_TEXT_AR_KEY).getValue();
		}
			return chatBotService.getBotConfigurationByKey(Constants.RASA_RUNTIME_OPTIONS_TEXT_EN_KEY).getValue(); 
	}


	/**
	 * 
	 */
	private void sendTextMessage(Messenger messenger ,  String senderId ,JSONObject object) {
		TextMessage textMessage = TextMessage.create(object.getString(Constants.RASA_RESPONSE_TEXT_KEY));
		MessagePayload payload = MessagePayload.create(senderId, MessagingType.RESPONSE, textMessage);
		try {
			Utils.markAsTypingOn(messenger, senderId);
			Utils.markAsTypingOff(messenger, senderId);
			messenger.send(payload);
		} catch (MessengerApiException | MessengerIOException e) {
			e.printStackTrace();
		}
		
	}


	// Audio message Handling 
	public void richMediaAttachment(Attachment attachment, String senderId , Messenger messenger,String audioApiUrl) {
		RichMediaAttachment richMediaAttachment = attachment.asRichMediaAttachment();
		URL url = richMediaAttachment.url();
		JSONObject jsonResponse = new JSONObject();
		Long ct = System.currentTimeMillis();
		Path target = null;
		String stringPath = System.getProperty("user.home") + "/Desktop/" + senderId;
		File file = new File(stringPath);
		if (!file.exists()) {
			file.mkdir();
		}
		try (InputStream in = url.openStream()) {
			String fullPAth = stringPath + "/audio-" + ct + ".wav";
			target = Paths.get(fullPAth);
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		JSONObject jsonParam = new JSONObject();
		jsonParam.put("data", target);
		/*Map<String, String> requestBody = new HashMap<>();
		requestBody.put("data", target.toString());*/
		HttpEntity<JSONObject> request = new HttpEntity<>(jsonParam);
		logger.debug("Rasa Request param" + jsonParam);
		try {
			ResponseEntity<String> response = restTemplate.exchange(new URI(audioApiUrl), HttpMethod.POST, request, String.class);
			jsonResponse = new JSONObject(response.getBody());
		} catch (RestClientException | URISyntaxException e) {
			e.printStackTrace();
		}
		rasaResponseHandling(jsonResponse, messenger, senderId);

	}
	
	
	
	/**
	 * Rasa Channel handling for free text interactions
	 * 
	 * @param senderId
	 * @param payload
	 * @param messenger
	 */
	public String rasaChannel(String senderId, String payload) {
		UserSelection userSelections = utilService.getUserSelectionsFromCache(senderId);
		userSelections.setFreeText(payload);
		utilService.updateUserSelectionsInCache(senderId, userSelections);
		return Constants.RASA_PAYLOAD;
	}
	
	public JSONObject rasaIntegration(String url, String payload, String paramKey) {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		JSONObject jResonse = new JSONObject();
		headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		Map<String,String> requestBody = new HashMap<>();
		requestBody.put(paramKey, payload);
		try {
		HttpEntity<Map<String,String>> request = new HttpEntity<>(requestBody);
		logger.debug(Constants.LOGGER_INFO_PREFIX+"Rasa WS request's body is  "+requestBody);
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
		logger.debug(Constants.LOGGER_INFO_PREFIX+"Rasa WS response is "+ response.getBody());
		jResonse = new JSONObject(response.getBody());
		return jResonse;
		}catch (Exception e) {
			e.printStackTrace();
		}
		return jResonse;
	}
	
}
