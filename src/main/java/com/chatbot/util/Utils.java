package com.chatbot.util;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.chatbot.entity.BotButton;
import com.chatbot.entity.BotInteraction;
import com.chatbot.entity.CustomerProfile;
import com.chatbot.entity.InteractionLogging;
import com.chatbot.services.ChatBotService;
import com.chatbot.services.UtilService;
import com.github.messenger4j.Messenger;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerIOException;
import com.github.messenger4j.send.SenderActionPayload;
import com.github.messenger4j.send.senderaction.SenderAction;

public class Utils {

	private static final Logger logger = LoggerFactory.getLogger(UtilService.class);

	public enum ButtonTypeEnum {
		START(1L), POSTBACK(2l), URL(3l), NESTED(4L), LOGIN(5L), LOGOUT(6L), CALL(7L);
		private final Long buttonTypeId;

		private ButtonTypeEnum(Long typeId) {
			this.buttonTypeId = typeId;
		}

		public Long getValue() {
			return buttonTypeId;
		}
	}

	public enum MessageTypeEnum {
		TEXTMESSAGE(1l), QUICKREPLYMESSAGE(2l), GENERICTEMPLATEMESSAGE(3l), ButtonTemplate(4l);
		private final Long messageTypeId;

		private MessageTypeEnum(Long messageTypeId) {
			this.messageTypeId = messageTypeId;
		}

		public Long getValue() {
			return messageTypeId;
		}
	}

	// Get Text Value
	public static String getTextValueForButtonLabel(String local, BotButton botButton) {
		String text = "";
		if (local.equalsIgnoreCase("ar")) {
			text = botButton.getBotText().getArabicText();
		} else {
			text = botButton.getBotText().getEnglishText();
		}
		return text;
	}

	// Create Url Method
	public static URL createUrl(String stringUrl) {
		URL url = null;

		try {
			url = new URL(stringUrl);
		} catch (MalformedURLException e) {
			logger.error(e.getMessage(), e);
		}

		return url;

	}

	// DashBoard Utils

	public static String encryptDPIParam(String encryptedString) throws Exception {
		byte[] decryptionKey = new byte[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
		Cipher c = Cipher.getInstance("AES");
		SecretKeySpec k = new SecretKeySpec(decryptionKey, "AES");
		c.init(Cipher.ENCRYPT_MODE, k);
		byte[] utf8 = encryptedString.getBytes("UTF8");
		byte[] enc = c.doFinal(utf8);
		return DatatypeConverter.printBase64Binary(enc);
	}

	public static void updateCustomerLastSeen(CustomerProfile customerProfile, String phoneNumber, ChatBotService chatBotService) {
		String methodName = new Object() {
		}.getClass().getEnclosingMethod().getName();
		if (phoneNumber != null && !phoneNumber.equals("")) {
			// logger.debug("Dial is " + phoneNumber + " Method Name is " + methodName + "
			// Parameter is " + customerProfile.toString());
		}
		Date date = new Date();
		CustomerProfile updatedCustomerProfile = new CustomerProfile();
		updatedCustomerProfile.setFirstInsertion(customerProfile.getFirstInsertion());
		updatedCustomerProfile.setLastGetProfileWSCall(customerProfile.getLastGetProfileWSCall());
		updatedCustomerProfile.setLinkingDate(customerProfile.getLinkingDate());
		updatedCustomerProfile.setLocale(customerProfile.getLocale());
		updatedCustomerProfile.setMsisdn(customerProfile.getMsisdn());
		updatedCustomerProfile.setSenderID(customerProfile.getSenderID());
		Timestamp timeStamp = new Timestamp(date.getTime());
		updatedCustomerProfile.setCustomerLastSeen(timeStamp);
		chatBotService.saveCustomerProfile(updatedCustomerProfile);
	}

	public static boolean isNotEmpty(String obj) {
		return obj != null && obj.length() != 0;
	}

	public static MediaType getMediaType(Long mediaTypeId) {
		switch (mediaTypeId.intValue()) {
		case 1:
			return MediaType.APPLICATION_JSON;
		case 2:
			return MediaType.APPLICATION_XML;
		default:
			return MediaType.APPLICATION_JSON;
		}

	}

	public static HttpMethod getHttpMethod(int httpMethodId) {
		switch (httpMethodId) {
		case 1:
			return HttpMethod.GET;

		case 2:
			return HttpMethod.POST;

		default:
			return HttpMethod.GET;
		}

	}

	/**
	 * @param customerProfile
	 * @param botInteraction
	 */
	public static void interactionLogginghandling(CustomerProfile customerProfile, BotInteraction botInteraction, ChatBotService chatBotService, String phoneNumber) {
		String methodName = new Object() {
		}.getClass().getEnclosingMethod().getName();
		logger.debug("Dial is " + phoneNumber + " Method Name is " + methodName + " Parameters are Customer Profile " + customerProfile.toString() + "" + "Interaction " + botInteraction.toString());
		Date date = new Date();
		Timestamp timeStamp = new Timestamp(date.getTime());
		InteractionLogging interactionLogging = new InteractionLogging();
		interactionLogging.setBotInteraction(botInteraction);
		interactionLogging.setInteractionCallingDate(timeStamp);
		interactionLogging.setCustomerProfile(customerProfile);
		chatBotService.saveInteractionLogging(interactionLogging);
	    chatBotService.saveInteractionLogging(interactionLogging);
	}

	public static void userLogout(final String senderId, ChatBotService chatBotService) {
		CustomerProfile storedCustomerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		CustomerProfile logoutCustomerProfile = new CustomerProfile();
		logoutCustomerProfile.setCustomerLastSeen(storedCustomerProfile.getCustomerLastSeen());
		logoutCustomerProfile.setFirstInsertion(storedCustomerProfile.getFirstInsertion());
		logoutCustomerProfile.setLastGetProfileWSCall(storedCustomerProfile.getLastGetProfileWSCall());
		logoutCustomerProfile.setLinkingDate(storedCustomerProfile.getLinkingDate());
		logoutCustomerProfile.setLocale(storedCustomerProfile.getLocale());
		logoutCustomerProfile.setMsisdn("");
		logoutCustomerProfile.setSenderID(storedCustomerProfile.getSenderID());
		chatBotService.saveCustomerProfile(logoutCustomerProfile);
	}

	public static void saveCustomerInformation(ChatBotService chatBotService, String senderId, String locale) {
		CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		CustomerProfile newCustomerProfile = new CustomerProfile();
		if (customerProfile == null) {
			Date date = new Date();
			Timestamp timestamp = new Timestamp(date.getTime());
			newCustomerProfile.setFirstInsertion(timestamp);
			newCustomerProfile.setSenderID(senderId);
			newCustomerProfile.setCustomerLastSeen(timestamp);
			newCustomerProfile.setLocale(locale);
			chatBotService.saveCustomerProfile(newCustomerProfile);
		} else {
			Date date = new Date();
			Timestamp timestamp = new Timestamp(date.getTime());
			newCustomerProfile.setSenderID(senderId);
			newCustomerProfile.setCustomerLastSeen(timestamp);
			newCustomerProfile.setLocale(locale);
			newCustomerProfile.setCustomerLastSeen(customerProfile.getCustomerLastSeen());
			newCustomerProfile.setFirstInsertion(customerProfile.getFirstInsertion());
			newCustomerProfile.setLastGetProfileWSCall(customerProfile.getLastGetProfileWSCall());
			newCustomerProfile.setMsisdn(customerProfile.getMsisdn());
			newCustomerProfile.setLinkingDate(customerProfile.getLinkingDate());
			chatBotService.saveCustomerProfile(newCustomerProfile);
		}

	}

	public static void updateCustomerlastCalling(ChatBotService chatBotService, String senderId, String locale) {
		CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		CustomerProfile newCustomerProfile = new CustomerProfile();
		if (customerProfile == null) {
			Date date = new Date();
			Timestamp timestamp = new Timestamp(date.getTime());
			newCustomerProfile.setFirstInsertion(timestamp);
			newCustomerProfile.setSenderID(senderId);
			newCustomerProfile.setCustomerLastSeen(timestamp);
			newCustomerProfile.setLocale(locale);
			newCustomerProfile.setLastGetProfileWSCall(timestamp);
			chatBotService.saveCustomerProfile(newCustomerProfile);
		} else {
			Date date = new Date();
			Timestamp timestamp = new Timestamp(date.getTime());
			newCustomerProfile.setSenderID(senderId);
			newCustomerProfile.setCustomerLastSeen(timestamp);
			newCustomerProfile.setLocale(locale);
			newCustomerProfile.setCustomerLastSeen(customerProfile.getCustomerLastSeen());
			newCustomerProfile.setFirstInsertion(customerProfile.getFirstInsertion());
			newCustomerProfile.setLastGetProfileWSCall(timestamp);
			newCustomerProfile.setMsisdn(customerProfile.getMsisdn());
			newCustomerProfile.setLinkingDate(customerProfile.getLinkingDate());
			chatBotService.saveCustomerProfile(newCustomerProfile);
		}

	}

	

	public static void markAsSeen(Messenger messenger, String userId) {
		final String recipientId = userId;
		final SenderAction senderAction = SenderAction.MARK_SEEN;

		final SenderActionPayload payload = SenderActionPayload.create(recipientId, senderAction);

		try {
			messenger.send(payload);
		} catch (MessengerApiException | MessengerIOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static void markAsTypingOn(Messenger messenger, String userId) {
		final String recipientId = userId;
		final SenderAction senderAction = SenderAction.TYPING_ON;

		final SenderActionPayload payload = SenderActionPayload.create(recipientId, senderAction);

		try {
			messenger.send(payload);
		} catch (MessengerApiException | MessengerIOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static void markAsTypingOff(Messenger messenger, String userId) {
		final String recipientId = userId;
		final SenderAction senderAction = SenderAction.TYPING_OFF;

		final SenderActionPayload payload = SenderActionPayload.create(recipientId, senderAction);

		try {
			messenger.send(payload);
		} catch (MessengerApiException | MessengerIOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	/*public List<String> getParameterNames(Method method) {
		Parameter[] parameters = method.getParameters();
		List<String> parameterNames = new ArrayList<>();

		for (Parameter parameter : parameters) {
			if (!parameter.isNamePresent()) {
				throw new IllegalArgumentException("Parameter names are not present!");
			}

			String parameterName = parameter.getName();
			parameterNames.add(parameterName);
		}

		return parameterNames;
	}*/

	public static String encryptChannelParam(String url)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
		String key = "etisalatetisalat";
		byte keyBytes[] = key.getBytes();
		SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
		Cipher ecipher = Cipher.getInstance("AES");
		ecipher.init(Cipher.ENCRYPT_MODE, secretKey);
		byte[] utf8 = url.getBytes("UTF8");
		byte[] enc = ecipher.doFinal(utf8);
		String encryptedParams = new sun.misc.BASE64Encoder().encode(enc);
		byte[] encryptedBytes = encryptedParams.getBytes();
		StringBuilder strbuf = new StringBuilder(encryptedBytes.length * 2);
		for (int i = 0; i < encryptedBytes.length; i++) {
			if (((int) encryptedBytes[i] & 0xff) < 0x10) {
				strbuf.append("0");
			}
			strbuf.append(Long.toString((int) encryptedBytes[i] & 0xff, 16));
		}
		String toBeSentParams = strbuf.toString();
		return toBeSentParams;
	}

	public static Map<String,String> callGetWebServiceByRestTemplate(URI uri) {
		Map<String, String> responseMap = new HashMap<String, String>();
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.APPLICATION_JSON_UTF8_VALUE);
		try {
		ResponseEntity<String> response = null;	
		HttpEntity entity = new HttpEntity<>(headers);
		RestTemplate restTemplate = new RestTemplate();
		response = restTemplate.exchange(uri, 
		        HttpMethod.GET, 
		        entity, 
		        String.class);
		int statusId = response.getStatusCodeValue();
		if(statusId == 200) {
			responseMap.put("status", String.valueOf(statusId));
			responseMap.put("response", response.getBody());
		}else {
			responseMap.put("status", String.valueOf(statusId));
			responseMap.put("response", response.getBody());
		}
		}catch(Exception e){
			logger.error(e.getMessage());
		}
		return responseMap;
	}

}
