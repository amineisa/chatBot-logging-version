package com.chatbot.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chatbot.entity.BotButton;
import com.chatbot.entity.BotInteraction;
import com.chatbot.entity.CustomerLinkingDial;
import com.chatbot.entity.CustomerProfile;
import com.chatbot.entity.FreeTextLogging;
import com.chatbot.entity.InteractionLogging;
import com.chatbot.services.ChatBotService;
import com.chatbot.services.UtilService;
import com.github.messenger4j.Messenger;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerIOException;
import com.github.messenger4j.send.SenderActionPayload;
import com.github.messenger4j.send.senderaction.SenderAction;
import com.github.messenger4j.userprofile.UserProfile;

/**
 * @author Amin Eisa
 */
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


	

	// Get Text Value For Button Label
	public static String getTextValueForButtonLabel(String local, BotButton botButton) {
		String text = Constants.EMPTY_STRING;
		if (local.equalsIgnoreCase(Constants.LOCALE_AR)) {
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

	

	/**
	 * Update last seen property for last time of user interact with Bot
	 * 
	 * @param customerProfile
	 * @param chatBotService
	 */
	public static void updateCustomerLastSeen(CustomerProfile customerProfile, ChatBotService chatBotService) {
		Date date = new Date();
		CustomerProfile updatedCustomerProfile = new CustomerProfile();
		updatedCustomerProfile.setLastName(customerProfile.getLastName());
		updatedCustomerProfile.setFirstInsertion(customerProfile.getFirstInsertion());
		updatedCustomerProfile.setLastGetProfileWSCall(customerProfile.getLastGetProfileWSCall());
		updatedCustomerProfile.setLinkingDate(customerProfile.getLinkingDate());
		updatedCustomerProfile.setLocale(customerProfile.getLocale());
		updatedCustomerProfile.setMsisdn(customerProfile.getMsisdn());
		updatedCustomerProfile.setFirstName(customerProfile.getFirstName());
		updatedCustomerProfile.setSenderID(customerProfile.getSenderID());
		Timestamp timeStamp = new Timestamp(date.getTime());
		updatedCustomerProfile.setCustomerLastSeen(timeStamp);
		chatBotService.saveCustomerProfile(updatedCustomerProfile);
	}

	/**
	 * Normal interaction saving handling in database
	 * 
	 * @param customerProfile
	 * @param botInteraction
	 * @param chatBotService
	 */
	public static InteractionLogging interactionLogginghandling(CustomerProfile customerProfile, BotInteraction botInteraction, ChatBotService chatBotService) {
		Date date = new Date();
		Timestamp timeStamp = new Timestamp(date.getTime());
		InteractionLogging interactionLogging = new InteractionLogging();
		interactionLogging.setBotInteraction(botInteraction);
		interactionLogging.setInteractionCallingDate(timeStamp);
		interactionLogging.setCustomerProfile(customerProfile);
		chatBotService.saveInteractionLogging(interactionLogging);
		return chatBotService.saveInteractionLogging(interactionLogging);
	}

	/**
	 * Free text saving handling in database
	 * 
	 * @param interactionLogging
	 * @param text
	 * @param chatBotService
	 */
	public static void freeTextinteractionLogginghandling(InteractionLogging interactionLogging, String text, ChatBotService chatBotService) {
		Date date = new Date();
		Timestamp timeStamp = new Timestamp(date.getTime());
		FreeTextLogging freeTextLogging = new FreeTextLogging();
		freeTextLogging.setInteractionLogging(interactionLogging);
		freeTextLogging.setReceivingTime(timeStamp);
		freeTextLogging.setRecivedFreeText(text);
		chatBotService.saveFreeTextLogging(freeTextLogging);
	}

	/**
	 * Handling user logging out in database
	 * 
	 * @param senderId
	 * @param chatBotService
	 */
	public static CustomerProfile userLogout(final String senderId, ChatBotService chatBotService) {
		CustomerProfile storedCustomerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		CustomerProfile logoutCustomerProfile = new CustomerProfile();
		logoutCustomerProfile.setCustomerLastSeen(storedCustomerProfile.getCustomerLastSeen());
		logoutCustomerProfile.setFirstInsertion(storedCustomerProfile.getFirstInsertion());
		logoutCustomerProfile.setLastGetProfileWSCall(storedCustomerProfile.getLastGetProfileWSCall());
		logoutCustomerProfile.setLinkingDate(storedCustomerProfile.getLinkingDate());
		logoutCustomerProfile.setLocale(storedCustomerProfile.getLocale());
		logoutCustomerProfile.setMsisdn(Constants.EMPTY_STRING);
		logoutCustomerProfile.setFirstName(storedCustomerProfile.getFirstName());
		logoutCustomerProfile.setSenderID(storedCustomerProfile.getSenderID());
		logoutCustomerProfile.setLastName(storedCustomerProfile.getLastName());
		return chatBotService.saveCustomerProfile(logoutCustomerProfile);
	}

	/**
	 * Saving || updating if it exist Customer information
	 * 
	 * @param chatBotService
	 * @param senderId
	 * @param userLocale
	 * @param firstName
	 * @param lastName
	 */
	public static CustomerProfile saveCustomerInformation(ChatBotService chatBotService, String senderId, String userLocale, String firstName, String lastName) {
		CustomerProfile cProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		if (cProfile == null) {
			CustomerProfile newCustomerProfile = new CustomerProfile();
			Date date = new Date();
			Timestamp timestamp = new Timestamp(date.getTime());
			newCustomerProfile.setFirstInsertion(timestamp);
			newCustomerProfile.setSenderID(senderId);
			newCustomerProfile.setMsisdn("");
			newCustomerProfile.setCustomerLastSeen(timestamp);
			newCustomerProfile.setLocale(userLocale);
			newCustomerProfile.setFirstName(firstName);
			newCustomerProfile.setLastName(lastName);
			return chatBotService.saveCustomerProfile(newCustomerProfile);
		} else {
			Date date = new Date();
			CustomerProfile newCustomerProfile = new CustomerProfile();
			Timestamp timestamp = new Timestamp(date.getTime());
			newCustomerProfile.setFirstInsertion(cProfile.getFirstInsertion());
			newCustomerProfile.setSenderID(cProfile.getSenderID());
			newCustomerProfile.setMsisdn(cProfile.getMsisdn());
			newCustomerProfile.setLinkingDate(cProfile.getLinkingDate());
			newCustomerProfile.setCustomerLastSeen(timestamp);
			newCustomerProfile.setLocale(cProfile.getLocale());
			newCustomerProfile.setFirstName(cProfile.getFirstName());
			newCustomerProfile.setLastName(lastName);
			return chatBotService.saveCustomerProfile(newCustomerProfile);
		}
	}

	/**
	 * Mark message as seen in messenger tab
	 * 
	 * @param messenger
	 * @param userId
	 */
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

	/**
	 * Mark in messenger as bot typing a response to client during sending reply
	 * 
	 * @param messenger
	 * @param userId
	 */
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

	/**
	 * Mark in messenger as bot is stop typing after sending reply
	 * 
	 * @param messenger
	 * @param userId
	 */
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

	/**
	 * Save user linked dial in database
	 * 
	 * @param customerProfile
	 * @param chatBotService
	 */
	public static CustomerLinkingDial setLinkedDial(CustomerProfile customerProfile, ChatBotService chatBotService) {

		CustomerLinkingDial customerLinkingDial = new CustomerLinkingDial();
		Date linkingDate = new Date();
		Timestamp timestamp = new Timestamp(linkingDate.getTime());
		customerLinkingDial.setUnlinkingDate(timestamp);
		customerLinkingDial.setLinkingDate(timestamp);
		customerLinkingDial.setDial(customerProfile.getMsisdn());
		customerLinkingDial.setCustomerProfile(customerProfile);
		return chatBotService.saveCustomerLinkingDial(customerLinkingDial);
	}

	/**
	 * Update unlinking time for specific user's dial
	 * 
	 * @param chatBotService
	 * @param msisdn
	 */
	public static void updateUnlinkindDate(ChatBotService chatBotService, String msisdn) {
		Date unLinkingDate = new Date();
		Timestamp unLinkingTime = new Timestamp(unLinkingDate.getTime());
		CustomerLinkingDial storedObject = chatBotService.getCustomerLinkingDialById(msisdn);
		if(storedObject != null) {
		CustomerLinkingDial newCustomerLinkingDial = new CustomerLinkingDial();
		newCustomerLinkingDial.setCustomerProfile(storedObject.getCustomerProfile());
		newCustomerLinkingDial.setDial(storedObject.getDial());
		newCustomerLinkingDial.setLinkingDate(storedObject.getLinkingDate());
		newCustomerLinkingDial.setUnlinkingDate(unLinkingTime);
		}
		
	}
	
	/**
	 * Retrieve View Button label according to locale
	 * 
	 * @param locale
	 */
	public static String getLabelForViewButton(String locale) {
		if (locale.contains(Constants.LOCALE_AR)) {
			return Constants.BUTTON_LABEL_VIEW_AR;
		}
		return Constants.BUTTON_LABEL_VIEW_EN;
	}

	/**
	 * Retrieve pay Bill Button label according to locale
	 * 
	 * @param locale
	 */
	public static String getLabelForPayBillButton(String locale) {
		if (locale.contains(Constants.LOCALE_AR)) {
			return Constants.BUTTON_LABEL_PAY_BILL_AR;
		}
		return Constants.BUTTON_LABEL_PAY_BILL_EN;
	}

	/**
	 * Retrieve title for pay bill template according to locale
	 * 
	 * @param locale
	 */
	public static String getTitleForPayBillButton(String locale) {
		if (locale.contains(Constants.LOCALE_AR)) {
			return "عاوز تدفع فاتورتك دلوقتي ؟";
		}
		return "Do you want to pay your bill now ?";
	}

	/**
	 * Retrieve Back Button label according to locale
	 * 
	 * @param locale
	 */
	public static String getLabelForBackButton(String locale) {
		if (locale.contains(Constants.LOCALE_AR)) {
			return Constants.BUTTON_LABEL_BACK_AR;
		}
		return Constants.BUTTON_LABEL_BACK_EN;
	}

	/**
	 * Retrieve Subscription Button label according to locale
	 * 
	 * @param locale
	 */
	public static String subscribeButtonLabel(String locale) {
		if (locale.contains(Constants.LOCALE_AR)) {
			return Constants.BUTTON_LABEL_SUBSCRIBE_AR;
		}
		return Constants.BUTTON_LABEL_BACK_SUBSCRIBE_EN;
	}

	public static String informUserThatHeDoesnotSubscribeAtAnyMIBundle(String locale) {
		if (locale.contains(Constants.LOCALE_AR)) {
			return "نأسف أنت غير مشترك بأي من باقات الأنترنت .سوف نتأكد من الباقات المتاحة لرقمك";
		}
		return "Sorry ,You are not subscribe to any MI Bundle. We will check available MI bundles for your number";
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

	/**
	 * Retrieve More Button label according to locale
	 * 
	 * @param locale
	 */
	public static String moreButtonLabel(String locale) {
		if (locale.contains(Constants.LOCALE_AR)) {
			return Constants.MORE_BUTTON_LABEL_AR;
		} else {
			return Constants.MORE_BUTTON_LABEL_EN;
		}

	}

	public static String moreElementTitle(String locale) {
		if (locale.contains(Constants.LOCALE_AR)) {
			return "المزيد من الباقات ";
		} else {
			return "More rateplans";
		}
	}

	public static String moreElementSubTitle(String locale) {
		if (locale.contains(Constants.LOCALE_AR)) {
			return "يمكنك رؤية المزيد من الباقات الاخري بالضغط علي زرار أكثر ";
		} else {
			return "You can check more rateplans by press More Button";
		}
	}

	public static String replacePlaceholderByNameValue(String text, String userFirstName , String phoneNumber) {		
		if(text.contains(Constants.NUMBER_PLACEHOLDER) && phoneNumber != null) {
			text = text.replaceFirst(Constants.NUMBER_PLACEHOLDER, phoneNumber);
		}else if(text.contains(Constants.NUMBER_PLACEHOLDER) && phoneNumber == null || phoneNumber =="") {
			text = text.replaceFirst(Constants.NUMBER_PLACEHOLDER, "");
		}else if (text.contains(Constants.NAME_PLACEHOLDER) && userFirstName != null) {
			text = text.replaceFirst(Constants.NAME_PLACEHOLDER, userFirstName);
		} else if (text.contains(Constants.NAME_PLACEHOLDER) && (userFirstName == null || userFirstName == "")) {
			text = text.replaceFirst(Constants.NAME_PLACEHOLDER, "");
		}
		return text;
	}
	
	
	
	
	static String generateActivationCode(){
		Random random = new Random();
		return String.format("%04d", random.nextInt(10000));
		//System.out.printf("%04d%n", random.nextInt(10000));
	}
	
	
	public static String extractPhoneNumbers(String text) {
		String dial ="";
		 Pattern p = Pattern.compile("\\d+");
	        Matcher m = p.matcher(text);
	        while(m.find()) {
	            dial = m.group();
	        }
	        return dial;
	}
	
	
	
	
	
	
	
	
/*	public static void main(String[] args) {
		System.out.println(generateActivationCode());
		String text = "this is my phoneNumber 0110113960 and my second number is 01122200685";
		System.out.println("Number is "+extractPhoneNumbers(text));
		
	}
	*/
	
	
	
	
	
}