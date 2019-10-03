package com.chatbot.repo;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.chatbot.entity.BotButtonTemplateMSG;
import com.chatbot.entity.BotInteractionMessage;

public interface BotButtonTemplateMSGRepo extends CrudRepository<BotButtonTemplateMSG, Long>{
	
	
	BotButtonTemplateMSG findBotButtonTemplateMSGByButtonTempMsgId(Long id);
	
	List<BotButtonTemplateMSG> findBotButtonTemplateMSGByBotInteractionMessage(BotInteractionMessage interactionMsg);

	BotButtonTemplateMSG findBotButtonTemplateMSGBybotInteractionMessage(BotInteractionMessage botInteractionMessage);
}
