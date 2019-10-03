package com.chatbot.repo;

import org.springframework.data.repository.CrudRepository;

import com.chatbot.entity.BotWebserviceMessage;

public interface BotWebserviceMessageRepo extends CrudRepository<BotWebserviceMessage, Long> {
	public BotWebserviceMessage findByBotInteractionMessageMessageId(Long messageId);

}
