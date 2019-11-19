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

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chatbot.entity.BotButton;
import com.chatbot.entity.BotQuickReplyMessage;
import com.chatbot.entity.CustomerProfile;
import com.chatbot.util.Constants;
import com.chatbot.util.Utils;
import com.github.messenger4j.Messenger;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.MessagingType;
import com.github.messenger4j.send.message.TextMessage;
import com.github.messenger4j.send.message.quickreply.QuickReply;
import com.github.messenger4j.send.message.quickreply.TextQuickReply;

/**
 * @author A.Eissa
 */
@Service
public class QuickReplyService {

	@Autowired
	private InteractionHandlingService interactionHandlingService;

	public List<QuickReply> createQuickReply(Long messageId, String local, ChatBotService chatBotService) {
		BotQuickReplyMessage botQuickReplyMessage = chatBotService.findQuickReplyMessageByMessageId(messageId);
		List<QuickReply> quickReplies = new ArrayList<>();
		List<BotButton> quickReplyButtonList = chatBotService.findButtonsByQuickReplyMessageId(botQuickReplyMessage.getQuickMsgId());
		QuickReply quickReply = null;
		for (BotButton botButton : quickReplyButtonList) {
			String label = Utils.getTextValueForButtonLabel(local, botButton);
			if (botButton.getButtonImageUrl() != null) {
				if (botButton.getButtonImageUrl().length() > 0) {
					URL url = Utils.createUrl(botButton.getButtonImageUrl());
					quickReply = TextQuickReply.create(label, botButton.getButtonPayload(), Optional.of(url));
				} else {
					quickReply = TextQuickReply.create(label, botButton.getButtonPayload(), empty());
				}
			} else {
				quickReply = TextQuickReply.create(label, botButton.getButtonPayload());
			}
			quickReplies.add(quickReply);
		}
		return quickReplies;
	}

	public String getTextForQuickReply(String local, BotQuickReplyMessage botQuickReplyMessage, String userFirstName, String phoneNumber) {
		String text = local.equalsIgnoreCase(Constants.LOCALE_AR) ? botQuickReplyMessage.getBotText().getArabicText() : botQuickReplyMessage.getBotText().getEnglishText();
		return Utils.replacePlaceholderByNameValue(text, userFirstName, phoneNumber);
	}

	public void quickReplyDynamicScenario(CustomerProfile customerProfile, String payload, ArrayList<MessagePayload> messagePayloadList, String response, String text, Messenger messenger) {
		JSONObject responeObject = new JSONObject(response);
		List<QuickReply> quickReplies = new ArrayList<>();
		switch (payload) {
		case Constants.EMERALED_REMOVE_MEMBER_BUTTON_PAYLOAD:
			if (responeObject.getJSONArray("activeChildInfoModelList") != null) {
				JSONArray activeChildInfoModelList = responeObject.getJSONArray("activeChildInfoModelList");
				if (activeChildInfoModelList.length() > 0) {
					for (int i = 0; i < activeChildInfoModelList.length(); i++) {
						String activeDialNumber = activeChildInfoModelList.getJSONObject(i).getString("childDial");
						TextQuickReply childNumber = TextQuickReply.create(0 + activeDialNumber, Constants.EMERALD_PAYLOAD_REMOVE_CHILD_PREFIX + activeDialNumber);
						quickReplies.add(childNumber);
					}
					Optional<List<QuickReply>> quickRepliesOp = Optional.of(quickReplies);
					MessagePayload messagePayload = MessagePayload.create(customerProfile.getSenderID(), MessagingType.RESPONSE, TextMessage.create(text, quickRepliesOp, empty()));
					messagePayloadList.add(messagePayload);
				} else {
					interactionHandlingService.handlePayload(Constants.EMERALD_NO_CHILD_FOUND_TO_REMOVE, messenger, customerProfile.getSenderID());
				}
			}			
			break;
		case Constants.EMERALD_GET_DIALS_FOR_DISTRIBUTE_PAYLOAD:
			if(responeObject.get(Constants.EMERALD_CHILD_LIST_KEY) instanceof JSONArray) {
				if(responeObject.getJSONArray(Constants.EMERALD_CHILD_LIST_KEY).length()>1) {
				JSONArray familyDials = responeObject.getJSONArray(Constants.EMERALD_CHILD_LIST_KEY);
				for (int i = 0; i < familyDials.length(); i++) {
					String activeDialNumber = familyDials.getString(i).substring(0, 10);
					TextQuickReply dialNumber = TextQuickReply.create(0 + activeDialNumber, Constants.EMERALD_ASK_ABOUT_AMOUT_FOR_DISTRIBUTION + activeDialNumber);
					quickReplies.add(dialNumber);
				}
				Optional<List<QuickReply>> quickRepliesOp = Optional.of(quickReplies);
				MessagePayload messagePayload = MessagePayload.create(customerProfile.getSenderID(), MessagingType.RESPONSE, TextMessage.create(text, quickRepliesOp, empty()));
				messagePayloadList.add(messagePayload);
				}else {
					interactionHandlingService.handlePayload(Constants.EMERALD_NO_CHILD_FOUND_TO_REMOVE, messenger, customerProfile.getSenderID());
				}
			} else {
				interactionHandlingService.handlePayload(Constants.EMERALD_NO_CHILD_FOUND_TO_REMOVE, messenger, customerProfile.getSenderID());
			}
			break;
		case Constants.EMERALD_CHILD_TRANSFER_TO_PAYLOAD:
			if(responeObject.get(Constants.EMERALD_CHILD_LIST_KEY) instanceof JSONArray) {
				if(responeObject.getJSONArray(Constants.EMERALD_CHILD_LIST_KEY).length()>1) {
				JSONArray familyDials = responeObject.getJSONArray(Constants.EMERALD_CHILD_LIST_KEY);
				for (int i = 0; i < familyDials.length(); i++) {
					String activeDialNumber = familyDials.getString(i).substring(0, 10);
					TextQuickReply dialNumber = TextQuickReply.create(0 + activeDialNumber, Constants.EMERALD_ASK_ABOUT_AMOUT_FOR_TRANSFER+ activeDialNumber);
					quickReplies.add(dialNumber);
				}
				Optional<List<QuickReply>> quickRepliesOp = Optional.of(quickReplies);
				MessagePayload messagePayload = MessagePayload.create(customerProfile.getSenderID(), MessagingType.RESPONSE, TextMessage.create(text, quickRepliesOp, empty()));
				messagePayloadList.add(messagePayload);
				}else {
					interactionHandlingService.handlePayload(Constants.EMERALD_NO_CHILD_FOUND_TO_REMOVE, messenger, customerProfile.getSenderID());
				}
			} else {
				interactionHandlingService.handlePayload(Constants.EMERALD_NO_CHILD_FOUND_TO_REMOVE, messenger, customerProfile.getSenderID());
			}
			break;
		case Constants.EMERALD_CHILD_TRANSFER_FROM_PAYLOAD:
			if(responeObject.get(Constants.EMERALD_CHILD_LIST_KEY) instanceof JSONArray) {
				if(responeObject.getJSONArray(Constants.EMERALD_CHILD_LIST_KEY).length()>1) {
				JSONArray familyDials = responeObject.getJSONArray(Constants.EMERALD_CHILD_LIST_KEY);
				for (int i = 0; i < familyDials.length(); i++) {
					String activDialNumber = familyDials.getString(i).substring(0, 10);
					TextQuickReply dialNumber = TextQuickReply.create(0+activDialNumber, Constants.EMERALD_CHILD_TRAFICCASES_FOR_TRANSFER_PAYLOAD +Constants.COMMA_CHAR+activDialNumber);
					quickReplies.add(dialNumber);
				}
				Optional<List<QuickReply>> quickRepliesOp = Optional.of(quickReplies);
				MessagePayload messagePayload = MessagePayload.create(customerProfile.getSenderID(), MessagingType.RESPONSE, TextMessage.create(text, quickRepliesOp, empty()));
				messagePayloadList.add(messagePayload);
				}else {
					interactionHandlingService.handlePayload(Constants.EMERALD_NO_CHILD_FOUND_TO_REMOVE, messenger, customerProfile.getSenderID());
				}
			}
			break;
		default:
			break;
		}}
}

/*
 * List<QuickReply> quickReplies = new ArrayList<>(); if
 * (payload.equals(Constants.EMERALED_REMOVE_MEMBER_BUTTON_PAYLOAD)) {
 * JSONObject responeObject = new JSONObject(response); if
 * (responeObject.getJSONArray("activeChildInfoModelList") != null) { JSONArray
 * activeChildInfoModelList =
 * responeObject.getJSONArray("activeChildInfoModelList"); if
 * (activeChildInfoModelList.length() > 0) { for (int i = 0; i <
 * activeChildInfoModelList.length(); i++) { String activeDialNumber =
 * activeChildInfoModelList.getJSONObject(i).getString("childDial");
 * TextQuickReply childNumber = TextQuickReply.create(0 + activeDialNumber,
 * Constants.EMERALD_PAYLOAD_REMOVE_CHILD_PREFIX + activeDialNumber);
 * quickReplies.add(childNumber); } Optional<List<QuickReply>> quickRepliesOp =
 * Optional.of(quickReplies); MessagePayload messagePayload =
 * MessagePayload.create(customerProfile.getSenderID(), MessagingType.RESPONSE,
 * TextMessage.create(text, quickRepliesOp, empty()));
 * messagePayloadList.add(messagePayload); } else {
 * interactionHandlingService.handlePayload(Constants.
 * EMERALD_NO_CHILD_FOUND_TO_REMOVE, messenger, customerProfile.getSenderID());
 * } } } else if
 * (payload.equals(Constants.EMERALD_GET_DIALS_FOR_DISTRIBUTE_PAYLOAD)) {
 * JSONObject responeObject = new JSONObject(response);
 * if(responeObject.get(Constants.EMERALD_CHILD_LIST_KEY) instanceof JSONArray)
 * { if(responeObject.getJSONArray(Constants.EMERALD_CHILD_LIST_KEY).length()>0)
 * { JSONArray familyDials =
 * responeObject.getJSONArray(Constants.EMERALD_CHILD_LIST_KEY); for (int i = 0;
 * i < familyDials.length(); i++) { String activeDialNumber =
 * familyDials.getString(i).substring(0, 10); TextQuickReply dialNumber =
 * TextQuickReply.create(0 + activeDialNumber,
 * Constants.EMERALD_ASK_ABOUT_AMOUT_FOR_DISTRIBUTION + activeDialNumber);
 * quickReplies.add(dialNumber); } Optional<List<QuickReply>> quickRepliesOp =
 * Optional.of(quickReplies); MessagePayload messagePayload =
 * MessagePayload.create(customerProfile.getSenderID(), MessagingType.RESPONSE,
 * TextMessage.create(text, quickRepliesOp, empty()));
 * messagePayloadList.add(messagePayload); }else {
 * interactionHandlingService.handlePayload(Constants.
 * EMERALD_NO_CHILD_FOUND_TO_REMOVE, messenger, customerProfile.getSenderID());
 * } } else { interactionHandlingService.handlePayload(Constants.
 * EMERALD_NO_CHILD_FOUND_TO_REMOVE, messenger, customerProfile.getSenderID());
 * } }else if (payload.equals(Constants.EMERALD_CHILD_TRANSFER_TO_PAYLOAD)) {
 * JSONObject responeObject = new JSONObject(response);
 * if(responeObject.get(Constants.EMERALD_CHILD_LIST_KEY) instanceof JSONArray)
 * { if(responeObject.getJSONArray(Constants.EMERALD_CHILD_LIST_KEY).length()>0)
 * { JSONArray familyDials =
 * responeObject.getJSONArray(Constants.EMERALD_CHILD_LIST_KEY); for (int i = 0;
 * i < familyDials.length(); i++) { String activeDialNumber =
 * familyDials.getString(i).substring(0, 10); TextQuickReply dialNumber =
 * TextQuickReply.create(0 + activeDialNumber,
 * Constants.EMERALD_ASK_ABOUT_AMOUT_FOR_TRANSFER+ activeDialNumber);
 * quickReplies.add(dialNumber); } Optional<List<QuickReply>> quickRepliesOp =
 * Optional.of(quickReplies); MessagePayload messagePayload =
 * MessagePayload.create(customerProfile.getSenderID(), MessagingType.RESPONSE,
 * TextMessage.create(text, quickRepliesOp, empty()));
 * messagePayloadList.add(messagePayload); }else {
 * interactionHandlingService.handlePayload(Constants.
 * EMERALD_NO_CHILD_FOUND_TO_REMOVE, messenger, customerProfile.getSenderID());
 * } } else { interactionHandlingService.handlePayload(Constants.
 * EMERALD_NO_CHILD_FOUND_TO_REMOVE, messenger, customerProfile.getSenderID());
 * } }else
 * if(payload.equalsIgnoreCase(Constants.EMERALD_CHILD_TRANSFER_FROM_PAYLOAD)) {
 * JSONObject responeObject = new JSONObject(response);
 * if(responeObject.get(Constants.EMERALD_CHILD_LIST_KEY) instanceof JSONArray)
 * { if(responeObject.getJSONArray(Constants.EMERALD_CHILD_LIST_KEY).length()>0)
 * { JSONArray familyDials =
 * responeObject.getJSONArray(Constants.EMERALD_CHILD_LIST_KEY); for (int i = 0;
 * i < familyDials.length(); i++) { String activDialNumber =
 * familyDials.getString(i).substring(0, 10); TextQuickReply dialNumber =
 * TextQuickReply.create(0+activDialNumber,
 * Constants.EMERALD_CHILD_TRAFICCASES_FOR_TRANSFER_PAYLOAD
 * +Constants.COMMA_CHAR+activDialNumber); quickReplies.add(dialNumber); }
 * Optional<List<QuickReply>> quickRepliesOp = Optional.of(quickReplies);
 * MessagePayload messagePayload =
 * MessagePayload.create(customerProfile.getSenderID(), MessagingType.RESPONSE,
 * TextMessage.create(text, quickRepliesOp, empty()));
 * messagePayloadList.add(messagePayload); }else {
 * interactionHandlingService.handlePayload(Constants.
 * EMERALD_NO_CHILD_FOUND_TO_REMOVE, messenger, customerProfile.getSenderID());
 * } }} }
 * 
 * } }
 */
