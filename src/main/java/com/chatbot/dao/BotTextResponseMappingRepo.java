package com.chatbot.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.chatbot.entity.BotTextResponseMapping;

public interface BotTextResponseMappingRepo extends CrudRepository<BotTextResponseMapping, Long> {
	public List<BotTextResponseMapping> findByBotWebserviceMessageWsMsgId(Long wsMsgId);
}
