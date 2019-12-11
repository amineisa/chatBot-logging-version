/** copyright Etisalat social team. To present All rights reserved
*/
/**
 * 
 */
package com.chatbot.services;

import static java.util.Optional.empty;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chatbot.util.Constants;
import com.chatbot.util.Utils;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.MessagingType;
import com.github.messenger4j.send.message.TextMessage;
import com.github.messenger4j.send.message.quickreply.QuickReply;
import com.github.messenger4j.send.message.quickreply.TextQuickReply;
import com.github.messenger4j.send.message.template.GenericTemplate;
import com.github.messenger4j.send.message.template.Template;
import com.github.messenger4j.send.message.template.button.Button;
import com.github.messenger4j.send.message.template.button.PostbackButton;
import com.github.messenger4j.send.message.template.common.Element;

/**
 * @author A.Eissa
 */
@Service
public class BusinessErrorService {
	@Autowired
	private ChatBotService chatBotService;

	public void handleFaultMessage(String locale, String senderId, ArrayList<MessagePayload> messagePayloadList) {
		String msg = locale.contains(Constants.LOCALE_AR) ? Constants.AR_FAULT_MSG : Constants.EN_FAULT_MSG;
		messagePayloadList.add(MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(msg)));
		QuickReply rateplanButton = TextQuickReply.create(ratePlanLabel(locale), Constants.PAYLOAD_RATEPLAN_DETAILS);
		QuickReply internetButton = TextQuickReply.create(internetButtonLabel(locale), Constants.PAYLOAD_FULL_INTERNET);
		QuickReply myServuceButton = TextQuickReply.create(serviceLabel(locale), "account and cach");
		QuickReply needHelpButton = TextQuickReply.create(needHelpLabel(locale), "NEED_HELP");
		ArrayList<QuickReply> quickReplies = new ArrayList<>();
		quickReplies.add(rateplanButton);
		quickReplies.add(internetButton);
		quickReplies.add(myServuceButton);
		quickReplies.add(needHelpButton);
		Optional<List<QuickReply>> quickRepliesOp = Optional.of(quickReplies);
		MessagePayload messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(quickReplyTiltle(locale), quickRepliesOp, empty()));
		messagePayloadList.add(messagePayload);
	}

	/**
	 * @param locale
	 * @return
	 */
	private String internetButtonLabel(String locale) {
		return locale.contains(Constants.LOCALE_AR)?"انترنت":"My Internet";
	}

	/**
	 * @param locale
	 * @return
	 */
	private String quickReplyTiltle(String locale) {
		return locale.contains(Constants.LOCALE_AR) ? "اقدر اساعدك بايه تاني:":"But I can still help you in the below services: ";
	}

	private String ratePlanLabel(String locale) {
		return locale.contains(Constants.LOCALE_AR) ? "اتفاصيل نظامي" : "My Plan";
	}

	private String serviceLabel(String locale) {
		return locale.contains(Constants.LOCALE_AR) ? "خدمات الحساب" : "My Services ";
	}

	private String needHelpLabel(String locale) {
		return locale.contains(Constants.LOCALE_AR) ? "مساعدة" : "Need Help";
	}

	public Template profileWithoutMobileInternetPackage(String locale) {
		List<Button> buttons = new ArrayList<>();
		List<Element> elements = new ArrayList<>();
		String stringUrl = chatBotService.getBotConfigurationByKey(Constants.CONFIGURATION_TABLE_WARNING_IMAGE_URL).getValue();
		URL url = Utils.createUrl(stringUrl);
		PostbackButton backButton = PostbackButton.create(showBundleButtonLabel(locale), Constants.PAYLOAD_CHANGE_BUNDLE);
		buttons.add(backButton);
		Element element = Element.create(elementTitle(locale), Optional.of(elementsubTitle(locale)), Optional.of(url), empty(), Optional.of(buttons));
		elements.add(element);
		return GenericTemplate.create(elements);
	}

	/**
	 * @param locale
	 * @return
	 */
	private String elementTitle(String locale) {
		return locale.contains(Constants.LOCALE_AR) ? Constants.NOTELIGIBLE_ELEMENT_TITLE_AR:Constants.NOTELIGIBLE_ELEMENT_TITLE_EN;
	}

	private String showBundleButtonLabel(String locale){
		return locale.contains(Constants.LOCALE_AR) ? "عرض الباقات المتاحة":"Show bundles";
	}

	private String elementsubTitle(String locale) {
		return locale.contains(Constants.LOCALE_AR) ? "انت مش مشترك في باقات الموبيل انترنت ممكن تشترك دلوقتي في الباقة اللي تناسبك":"you are not subscribe at any MI Bundle but you can subscribe at on of below bundles";
	}
	
	
	
	

}
