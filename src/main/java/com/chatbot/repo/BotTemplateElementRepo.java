package com.chatbot.repo;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.chatbot.entity.BotTemplateElement;

public interface BotTemplateElementRepo extends CrudRepository<BotTemplateElement, Long> {
	public List<BotTemplateElement> findByBotGTemplateMessageGTMsgId(Long gTMsgId);

}
