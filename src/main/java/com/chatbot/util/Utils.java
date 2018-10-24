package com.chatbot.util;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
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

import com.chatbot.entity.BotButton;
import com.chatbot.entity.BotInteraction;
import com.chatbot.entity.BotWebserviceMessage;
import com.chatbot.entity.CustomerProfile;
import com.chatbot.entity.InteractionLogging;
import com.chatbot.services.ChatBotService;
import com.chatbot.services.UtilService;
import com.github.messenger4j.Messenger;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerIOException;
import com.github.messenger4j.send.SenderActionPayload;
import com.github.messenger4j.send.senderaction.SenderAction;
import com.github.messenger4j.userprofile.UserProfile;

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
		String text = Constants.EMPTY_STRING;
		;
		if (local.equalsIgnoreCase(Constants.ARABIC_LOCAL)) {
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

	// DashBoard encryption Utils
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

	/**
	 * @param customerProfile
	 * @param botInteraction
	 */
	public static void interactionLogginghandling(CustomerProfile customerProfile, BotInteraction botInteraction, ChatBotService chatBotService) {
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
		logoutCustomerProfile.setMsisdn(Constants.EMPTY_STRING);
		logoutCustomerProfile.setSenderID(storedCustomerProfile.getSenderID());
		chatBotService.saveCustomerProfile(logoutCustomerProfile);
	}

	public static void saveCustomerInformation(ChatBotService chatBotService, String senderId, Messenger messenger) {
		CustomerProfile newCustomerProfile = new CustomerProfile();
		UserProfile userProfile = Utils.getUserProfile(senderId, messenger);
		String userLocale = userProfile.locale();
		String firstName = userProfile.firstName();
		Date date = new Date();
		Timestamp timestamp = new Timestamp(date.getTime());
		newCustomerProfile.setFirstInsertion(timestamp);
		newCustomerProfile.setSenderID(senderId);
		newCustomerProfile.setCustomerLastSeen(timestamp);
		newCustomerProfile.setLocale(userLocale);
		newCustomerProfile.setFirstName(firstName);
		chatBotService.saveCustomerProfile(newCustomerProfile);

	}

	public static void markAsSeen(Messenger messenger, String userId) {
		try {
			String recipientId = userId;
			SenderAction senderAction = SenderAction.MARK_SEEN;
			SenderActionPayload payload = SenderActionPayload.create(recipientId, senderAction);
			messenger.send(payload);
		} catch (MessengerApiException | MessengerIOException e) {
			logger.error(Constants.LOGGER_EXCEPTION_MESSAGE + e);
			e.printStackTrace();
		}
	}

	public static void markAsTypingOn(Messenger messenger, String userId) {
		try {
			String recipientId = userId;
			SenderAction senderAction = SenderAction.TYPING_ON;

			SenderActionPayload payload = SenderActionPayload.create(recipientId, senderAction);

			messenger.send(payload);
		} catch (MessengerApiException | MessengerIOException e) {
			logger.error(Constants.LOGGER_EXCEPTION_MESSAGE + e);
			e.printStackTrace();
		}
	}

	public static void markAsTypingOff(Messenger messenger, String userId) {
		try {
			String recipientId = userId;
			SenderAction senderAction = SenderAction.TYPING_OFF;
			SenderActionPayload payload = SenderActionPayload.create(recipientId, senderAction);

			messenger.send(payload);
		} catch (MessengerApiException | MessengerIOException e) {
			logger.error(Constants.LOGGER_EXCEPTION_MESSAGE + e);
			e.printStackTrace();
		}
	}

	public static String encryptChannelParam(String url)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
		String key = "etisalatetisalat";
		byte[] keyBytes = key.getBytes();
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

	public static Map<String, String> callGetWebServiceByRestTemplate(URI uri) {
		Map<String, String> responseMap = new HashMap<>();
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.APPLICATION_JSON_UTF8_VALUE);
		int statusId = 0;
		try {
			HttpEntity entity = new HttpEntity<>(headers);
			RestTemplate restTemplate = new RestTemplate();
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			statusId = response.getStatusCodeValue();
			responseMap.put(Constants.RESPONSE_STATUS_KEY, String.valueOf(statusId));
			responseMap.put(Constants.RESPONSE_KEY, response.getBody());
		} catch (Exception e) {
			responseMap.put(Constants.RESPONSE_STATUS_KEY, String.valueOf(statusId));
			logger.error(Constants.LOGGER_EXCEPTION_MESSAGE + e);
			e.printStackTrace();
		}
		return responseMap;
	}

	public static URI createURI(BotWebserviceMessage botWebserviceMessage, String senderId, ChatBotService chatBotService, String phoneNumber) {
		URI uri = null;
		try {
			CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
			String dialNumber = customerProfile.getMsisdn();

			String paramChannel = Utils.encryptChannelParam(Constants.URL_PARAM_MSISDN_KEY + dialNumber + Constants.URL_TIME_CHANNEL_KEY + Constants.CHANEL_PARAM);
			String realParameter = Constants.URL_PARAM_CHANNEL_KEY + paramChannel;
			uri = new URI(botWebserviceMessage.getWsUrl() + "?dial=" + realParameter);
		} catch (URISyntaxException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException e1) {
			logger.error(Constants.LOGGER_DIAL_IS + phoneNumber + Constants.LOGGER_SENDER_ID + senderId + Constants.LOGGER_EXCEPTION_MESSAGE + e1);
			e1.printStackTrace();
		}
		return uri;
	}

	public static String getLabelForViewButton(String locale) {
		if (locale.contains(Constants.ARABIC_LOCAL)) {
			return "عرض";
		}
		return "View";
	}

	public static String getLabelForBackButton(String locale) {
		if (locale.contains(Constants.ARABIC_LOCAL)) {
			return "عودة";
		}
		return "Back";
	}

	public static String getLabelForٍSubscribeButton(String locale) {
		if (locale.contains(Constants.ARABIC_LOCAL)) {
			return "اشترك";
		}
		return "Subscribe";
	}

	public static String informUserThatHeDoesnotSubscribeAtAnyMIBundle(String locale) {
		if (locale.contains(Constants.ARABIC_LOCAL)) {
			return "نأسف أنت غير مشترك بأي من باقات الأنترنت و هذة هي الباقات المتاحة لرقمك";
		}
		return "Sorry ,You are not subscribe to any MI Bundle here are list of the available MI bundle for your dial";
	}

	public static UserProfile getUserProfile(String senderId, Messenger messenger) {
		UserProfile userProfile = null;
		try {
			userProfile = messenger.queryUserProfile(senderId);
		} catch (MessengerApiException | MessengerIOException e) {
			e.printStackTrace();
		}
		return userProfile;
	}
}
