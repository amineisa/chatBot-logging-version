/** copyright Etislata social team. To present All rights reserved
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

import org.springframework.stereotype.Service;

import com.chatbot.entity.BotButton;
import com.chatbot.entity.BotQuickReplyMessage;
import com.chatbot.util.Constants;
import com.chatbot.util.Utils;
import com.github.messenger4j.send.message.quickreply.QuickReply;
import com.github.messenger4j.send.message.quickreply.TextQuickReply;

/**
 * @author A.Eissa 
 */
@Service
public class QuickReplyService {
	
	
	public List<QuickReply> createQuickReply( Long messageId, String local , ChatBotService chatBotService) {
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
		String text = local.equalsIgnoreCase(Constants.LOCALE_AR) ? botQuickReplyMessage.getBotText().getArabicText() :botQuickReplyMessage.getBotText().getEnglishText(); 
		return Utils.replacePlaceholderByNameValue(text, userFirstName, phoneNumber);
	}

}
