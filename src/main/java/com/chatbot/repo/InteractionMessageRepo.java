package com.chatbot.repo;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.chatbot.entity.BotInteraction;
import com.chatbot.entity.BotInteractionMessage;

public interface InteractionMessageRepo extends CrudRepository<BotInteractionMessage, Long> {
	public List<BotInteractionMessage> findByBotInteractionInteractionIdOrderByMessagePriority(Long interactionId);
	
	public BotInteractionMessage findOneByBotInteraction(BotInteraction botInteraction);
}
