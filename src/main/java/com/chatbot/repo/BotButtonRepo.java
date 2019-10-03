package com.chatbot.repo;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.chatbot.entity.BotButton;
import com.chatbot.entity.BotButtonTemplateMSG;
import com.chatbot.entity.BotWebserviceMessage;

public interface BotButtonRepo extends CrudRepository<BotButton, Long> {
	public List<BotButton> findByBotQuickReplyMessageQuickMsgId(Long quickMsgId);

	public List<BotButton> findByBotTemplateElementElementId(Long elementId);
	
	public BotButton findBotButtonByButtonId(Long id);
	
	public BotButton findButtonByButtonType(Long id);
	
	public BotButton findButtonByButtonTypeId(Long id); 
	
	public BotButton findBotButtonByButtonPayload(String payload); 
	
	public List<BotButton> findByBotButtonTemplateMSG(BotButtonTemplateMSG botButtonTemplateMSG);
	
	public List<BotButton> findBotButtonByBotWebserviceMessage(BotWebserviceMessage botWebserviceMessage);

}
