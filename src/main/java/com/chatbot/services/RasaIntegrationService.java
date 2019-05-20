/** copyright Etisalat social team. To present All rights reserved
*/
/**
 * 
 */
package com.chatbot.services;

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
import java.util.HashMap;
import java.util.Map;

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

import com.chatbot.entity.BotWebserviceMessage;
import com.chatbot.entity.CustomerProfile;
import com.chatbot.entity.UserSelection;
import com.chatbot.util.Constants;
import com.chatbot.util.Utils;
import com.github.messenger4j.Messenger;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerIOException;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.MessagingType;
import com.github.messenger4j.send.message.TextMessage;
import com.github.messenger4j.webhook.event.attachment.Attachment;
import com.github.messenger4j.webhook.event.attachment.RichMediaAttachment;

/**
 * @author A.Eissa
 */
@Service
public class RasaIntegrationService {

	@Autowired
	UtilService utilService;
	@Autowired
	InteractionHandlingService interactionHandlingService;
	@Autowired
	ChatBotService chatBotService;

	private static final Logger logger = LoggerFactory.getLogger(RasaIntegrationService.class);

	/**
	 * @param messenger
	 * @param senderId
	 * @param botWebserviceMessage
	 * @param userSelections
	 */
	public void rasaCallingForFreeTextHandling(Messenger messenger, String senderId, BotWebserviceMessage botWebserviceMessage) {
		// Rasa Integration
		String paramKey = botWebserviceMessage.getListParamName();
		UserSelection userSelections = utilService.getUserSelectionsFromCache(senderId);
		JSONObject jsonResponse = rasaIntegration(botWebserviceMessage.getWsUrl(), userSelections.getFreeText(), paramKey);
		rasaResponseHandling(jsonResponse, messenger, senderId);
	
		/*
		 * 
		 * 
		 * if (!jsonResponse.keySet().isEmpty()) { // JSONArray intentRanking =
		 * jsonResponse.getJSONArray("intentRanking"); JSONObject intentObject =
		 * jsonResponse.getJSONObject("intent"); logger.debug("Intent Ranking " +
		 * intentObject);
		 * 
		 * Map<String, Double> map = new HashMap<>(); for (int i = 0; i <
		 * intentRanking.length(); i++) { JSONObject intentObject =
		 * intentRanking.getJSONObject(i); double conf =
		 * intentObject.getDouble("confidence"); String name =
		 * intentObject.getString("name"); if (Math.round(conf) > 50) { map.put(name,
		 * conf); } }
		 * 
		 * 
		 * JSONObject intent = jsonResponse.getJSONObject("intent"); String name =
		 * intent.getString("name");
		 * logger.debug("Rasa Response Interaction Name "+name);
		 * 
		 * return name; //handlePayload(intentObject.getString("name"), messenger,
		 * senderId); }else { return Constants.PAYLOAD_UNEXPECTED_PAYLOAD;
		 * //handlePayload(Constants.PAYLOAD_UNEXPECTED_PAYLOAD, messenger, senderId); }
		 */
	}

	
	// Rasa response handling
	private void rasaResponseHandling(JSONObject jsonResponse , Messenger messenger ,String senderId) {
		JSONArray botResponses = jsonResponse.getJSONArray(Constants.RASA_RESPONSE_ARRAY_KEY);
		CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		UserSelection userSelections = utilService.getUserSelectionsFromCache(senderId);
		if(botResponses.length()>0) {
		for (int i = 0; i < botResponses.length(); i++) {
			JSONObject object = botResponses.getJSONObject(i);
			if (object.keySet().contains(Constants.RASA_RESPONSE_TEXT_KEY)) {
				sendTextMessage(messenger, senderId, object);	
			} else if (object.keySet().contains(Constants.RASA_RESPONSE_ACTION_KEY)) {
				String action = object.getString(Constants.RASA_RESPONSE_ACTION_KEY);
				logger.debug("Rasa Action "+action);
				interactionHandlingService.handlePayload(action, messenger, senderId);
			}else if(object.keySet().contains(Constants.RASA_RESPONSE_DIAL_KEY)) {
				if(customerProfile.getMsisdn() == null) {
				String dial = object.getString(Constants.RASA_RESPONSE_DIAL_KEY);
				logger.debug("Rasa Auth Dial "+dial);
				userSelections.setUserDialForAuth(dial);
				utilService.updateUserSelectionsInCache(senderId, userSelections);
				interactionHandlingService.handlePayload(Constants.PAYLOAD_DIAL_VALIDITY, messenger, senderId);
				}else {	
				interactionHandlingService.handlePayload(Constants.PAYLOAD_ALREADY_LOGGED_IN, messenger, senderId);
				}
			}else if(object.keySet().contains(Constants.RASA_RESPONSE_VERIFICATION_KEY)) {
				String activationCode = object.getString(Constants.RASA_RESPONSE_VERIFICATION_KEY);
				logger.debug("Rasa Auth Verification Code "+activationCode);
				userSelections.setActivationCode(activationCode);
				utilService.updateUserSelectionsInCache(senderId, userSelections);
				interactionHandlingService.handlePayload(Constants.PAYLOAD_VERIFICATION_CODE, messenger, senderId);
			}else if(object.keySet().contains(Constants.RASA_RESPONSE_SCRATCHED_NUMBER)){
				String scratchedNumber = object.getString(Constants.RASA_RESPONSE_SCRATCHED_NUMBER);
				logger.debug("Rasa Recharge Scratched number "+scratchedNumber);
				userSelections.setScratcheddNumberForRecharge(scratchedNumber);
				utilService.updateUserSelectionsInCache(senderId, userSelections);
				interactionHandlingService.handlePayload(Constants.PAYLOAD_RECHARGE, messenger, senderId);
			}else {
				interactionHandlingService.handlePayload(Constants.PAYLOAD_FAULT_MSG, messenger, senderId);
			}
		}/*
		if (!action.equals("") && action.length() > 0) {
			logger.debug("Rasa Action "+action);
			interactionHandlingService.handlePayload(action, messenger, senderId);
		}*/
		}else {
			interactionHandlingService.handlePayload(Constants.PAYLOAD_FAULT_MSG, messenger, senderId);
		}
	}
	
	
	
	/**
	 * 
	 */
	private void sendTextMessage(Messenger messenger ,  String senderId ,JSONObject object) {
		TextMessage textMessage = TextMessage.create(object.getString(Constants.RASA_RESPONSE_TEXT_KEY));
		MessagePayload payload = MessagePayload.create(senderId, MessagingType.RESPONSE, textMessage);
		try {
			Utils.markAsTypingOn(messenger, senderId);
			messenger.send(payload);
			Utils.markAsTypingOff(messenger, senderId);
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
			logger.debug("Rasa Response " + response.getBody());
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
		headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		//JSONObject requestBody = new JSONObject();
		Map<String,String> requestBody = new HashMap<>();
		requestBody.put(paramKey, payload);
		HttpEntity<Map<String,String>> request = new HttpEntity<>(requestBody);
		logger.debug("Rasa Request "+requestBody);
		
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
		logger.debug("Rasa Response "+ response.getBody());
		if (response.getStatusCodeValue() != 200) {
			return new JSONObject();
		}
		return new JSONObject(response.getBody());
	}


}
