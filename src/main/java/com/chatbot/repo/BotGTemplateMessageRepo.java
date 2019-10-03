package com.chatbot.repo;

import org.springframework.data.repository.CrudRepository;

import com.chatbot.entity.BotGTemplateMessage;

public interface BotGTemplateMessageRepo extends CrudRepository<BotGTemplateMessage, Long> {
	public BotGTemplateMessage findByBotInteractionMessageMessageId(Long messageId);
}
