/** copyright Etisalat social team. To present All rights reserved
*/
/**
 * 
 */
package com.chatbot.services;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.chatbot.entity.BotConfiguration;
import com.chatbot.entity.CustomerProfile;
import com.chatbot.util.Constants;
import com.chatbot.util.Utils;
import com.github.messenger4j.common.WebviewHeightRatio;
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
	
	public void postPaidNoBillToPaid(String senderId, String userLocale, ArrayList<MessagePayload> messagePayloadList/*, Long messageTypeId*/) {
		MessagePayload messagePayload;
		List<Button> buttons = new ArrayList<>();
		Button backButton = PostbackButton.create(Utils.getLabelForBackButton(userLocale), Constants.PAYLOAD_WELCOME_AGAIN);
		buttons.add(backButton);
		ButtonTemplate buttonTemplate = ButtonTemplate.create(createNOBillingProfileMessage(userLocale), buttons);
		messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(buttonTemplate));
		messagePayloadList.add(messagePayload);
		//messageTypeId = 0L;
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
	public void postPaidbillingPaymentHandling(ArrayList<MessagePayload> messagePayloadList, JSONObject billProfile, String billAmount,CustomerProfile customerProfile) {
		String billingParam = billProfile.getJSONArray(Constants.JSON_KEY_ACTION_BUTTONS).getJSONObject(0).getString(Constants.JSON_KEY_PARAM);
		MessagePayload messagePayload = null;
		logger.debug(Constants.LOGGER_INFO_PREFIX+"Billing Param " + billingParam);
		BotConfiguration payBillbaseUrlRaw = chatBotService.getBotConfigurationByKey(Constants.PAY_BILL_BASE_URL);
		String baseUrl = payBillbaseUrlRaw.getValue();
		long currentTimeInSecond = System.currentTimeMillis() / 1000;
		try {
			String param = utilService.encryptDPIParam(Constants.URL_TIME_KEY + currentTimeInSecond + Constants.URL_PARAM_MSISDN_KEY + customerProfile.getMsisdn() + Constants.URL_ETISALAT_VALUE);
			String paramChannel = utilService.encryptChannelParam(Constants.URL_CHANNEL_KEY + Constants.CHANEL_PARAM);
			String webServiceUrl = baseUrl +param+"&operationParam=" + replaceParamValue(billingParam) +"&paramChannel:"+paramChannel+ "&lang=" + customerProfile.getLocale();
			logger.debug(Constants.LOGGER_INFO_PREFIX+"Bill payment URL "+webServiceUrl);
			URI uri = new URI(webServiceUrl);
			ResponseEntity<String> responseEntity = utilService.callGetWebServiceByRestTemplate(uri);
			logger.debug(Constants.LOGGER_INFO_PREFIX+"Status " + responseEntity.getStatusCodeValue());
			if (responseEntity.getStatusCode().equals(HttpStatus.OK)) {
				createBillingProfileInformationMessage(messagePayloadList,customerProfile.getLocale(), customerProfile.getSenderID(), billAmount);
				List<Button> buttons = new ArrayList<>();
				JSONObject iframeObject = new JSONObject(responseEntity.getBody());
				String payBillButtonURl = iframeObject.getString(Constants.JSON_KEY_IFRAME_BILL_PAYMENT_URL);
				logger.debug(Constants.LOGGER_INFO_PREFIX+" Pay Bill URL is "+payBillButtonURl);
				UrlButton payButton = UrlButton.create(Utils.getLabelForPayBillButton(customerProfile.getLocale()), Utils.createUrl(payBillButtonURl),Optional.of(WebviewHeightRatio.TALL));
				PostbackButton backButton = PostbackButton.create(Utils.getLabelForBackButton(customerProfile.getLocale()), Constants.PAYLOAD_CONSUMPTION);
				buttons.add(payButton);
				buttons.add(backButton);
				ButtonTemplate buttonTemplate = ButtonTemplate.create(Utils.getTitleForPayBillButton(customerProfile.getLocale()), buttons);
				messagePayload = MessagePayload.create(customerProfile.getSenderID(), MessagingType.RESPONSE, TemplateMessage.create(buttonTemplate));
				messagePayloadList.add(messagePayload);
			}
		}catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	private void createBillingProfileInformationMessage(ArrayList<MessagePayload> messagePayloadList,String userLocale, String senderId, String billAmount) {
		String text = userLocale.contains(Constants.LOCALE_AR)?  "قيمة فاتورتك الحالية " + billAmount:"Your current bill amount is " + billAmount;
		messagePayloadList.add(MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(text)));

	}

	private String createNOBillingProfileMessage(String userLocale) {
		return userLocale.contains(Constants.LOCALE_AR)? "0 قيمة فاتورتك الحالية":"Your current bill amount is 0";
	}
	
	
	private  String replaceParamValue(String param) {
		String newParam = param.replaceAll("%2", "%252");
		newParam = newParam.replaceAll("%3", "%253");
		return newParam ;
	}

	
}
