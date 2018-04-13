package com.chatbot.dao;

import org.springframework.data.repository.CrudRepository;

import com.chatbot.entity.BotQuickReplyMessage;

public interface BotQuickReplyMessageRepo extends CrudRepository<BotQuickReplyMessage, Long> {
	public BotQuickReplyMessage findByBotInteractionMessageMessageId(Long messageId);
}
