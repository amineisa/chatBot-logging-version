/** copyright Etisalat social team. To present All rights reserved
*/
/**
 * 
 */
package com.chatbot.services;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chatbot.entity.BotConfiguration;
import com.chatbot.util.Constants;
import com.chatbot.util.Utils;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.MessagingType;
import com.github.messenger4j.send.message.TemplateMessage;
import com.github.messenger4j.send.message.TextMessage;
import com.github.messenger4j.send.message.template.ButtonTemplate;
import com.github.messenger4j.send.message.template.button.Button;
import com.github.messenger4j.send.message.template.button.PostbackButton;
import com.github.messenger4j.send.message.template.button.UrlButton;

/**
 * @author A.Eissa 
 */
@Service
public class PostPaidService {
	
	@Autowired
	private ChatBotService chatBotService;
	@Autowired
	private UtilService utilService;

	private static final Logger logger = LoggerFactory.getLogger(PostPaidService.class);
	
	public void postPaidNoBillToPaid(String senderId, String userLocale, ArrayList<MessagePayload> messagePayloadList, Long messageTypeId) {
		MessagePayload messagePayload;
		List<Button> buttons = new ArrayList<>();
		Button backButton = PostbackButton.create(Utils.getLabelForBackButton(userLocale), Constants.PAYLOAD_WELCOME_AGAIN);
		buttons.add(backButton);
		ButtonTemplate buttonTemplate = ButtonTemplate.create(createNOBillingProfileMessage(userLocale), buttons);
		messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(buttonTemplate));
		messagePayloadList.add(messagePayload);
		messageTypeId = 0L;
	}

	/**
	 * @param senderId
	 * @param userLocale
	 * @param messagePayloadList
	 * @param messageTypeId
	 * @param phoneNumber
	 * @param billProfile
	 * @param billAmount
	 * @return
	 */
	public void postPaidbillingPaymentHandling(String senderId, String userLocale, ArrayList<MessagePayload> messagePayloadList, Long messageTypeId, String phoneNumber, JSONObject billProfile,
			String billAmount) {
		MessagePayload messagePayload;
		String billingParam = billProfile.getJSONArray(Constants.JSON_KEY_ACTION_BUTTONS).getJSONObject(0).getString(Constants.JSON_KEY_PARAM);
		logger.debug(Constants.LOGGER_INFO_PREFIX+"Billing Param " + billingParam);
		messagePayload = createBillingProfileInformationMessage(userLocale, senderId, billAmount);
		messagePayloadList.add(messagePayload);
		BotConfiguration payBillbaseUrlRaw = chatBotService.getBotConfigurationByKey(Constants.PAY_BILL_BASE_URL);
		String baseUrl = payBillbaseUrlRaw.getValue();
		String paramChanel;
		try {
			paramChanel = utilService.encryptChannelParam(Constants.URL_PARAM_MSISDN_KEY + phoneNumber + Constants.URL_TIME_CHANNEL_KEY + Constants.CHANEL_PARAM);
			String webServiceUrl = baseUrl + paramChanel + "&operationParam=" + billingParam + "&lang=" + userLocale;
			URI uri = new URI(webServiceUrl);
			Map<String, String> values = utilService.callGetWebServiceByRestTemplate(uri);
			logger.debug(Constants.LOGGER_INFO_PREFIX+"Status " + values.get(Constants.RESPONSE_STATUS_KEY));
			if (values.get(Constants.RESPONSE_STATUS_KEY).equals("200")) {
				List<Button> buttons = new ArrayList<>();
				JSONObject iframeObject = new JSONObject(values.get(Constants.RESPONSE_KEY));
				String payBillButtonURl = iframeObject.getString(Constants.JSON_KEY_IFRAME_BILL_PAYMENT_URL);
				logger.debug(Constants.LOGGER_INFO_PREFIX+" Pay Bill URL is "+payBillButtonURl);
				UrlButton payButton = UrlButton.create(Utils.getLabelForPayBillButton(userLocale), Utils.createUrl(payBillButtonURl));
				PostbackButton backButton = PostbackButton.create(Utils.getLabelForBackButton(userLocale), Constants.PAYLOAD_CONSUMPTION);
				buttons.add(payButton);
				buttons.add(backButton);
				ButtonTemplate buttonTemplate = ButtonTemplate.create(Utils.getTitleForPayBillButton(userLocale), buttons);
				MessagePayload buttonsMessagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(buttonTemplate));
				messagePayloadList.add(buttonsMessagePayload);
				messageTypeId = 0L;
			}
			messageTypeId = 0L;
		} catch (URISyntaxException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}

	}
	
	private MessagePayload createBillingProfileInformationMessage(String userLocale, String senderId, String billAmount) {
		String text = "";
		MessagePayload messagePayload = null;
		if (userLocale == null) {
			userLocale = Constants.LOCALE_EN;
		}
		if (userLocale.contains(Constants.LOCALE_AR)) {
			text = "قيمة فاتورتك الحالية " + billAmount;
			messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(text));
		} else {
			text = "Your current bill amount is " + billAmount;
			messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(text));
		}
		return messagePayload;
	}

	private String createNOBillingProfileMessage(String userLocale) {
		if (userLocale.contains(Constants.LOCALE_AR)) {
			return "0 قيمة فاتورتك الحالية";

		} else {
			return "Your current bill amount is 0";

		}
	}
}
