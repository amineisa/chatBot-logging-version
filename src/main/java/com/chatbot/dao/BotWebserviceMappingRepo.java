package com.chatbot.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.chatbot.entity.BotWebserviceMapping;

public interface BotWebserviceMappingRepo extends CrudRepository<BotWebserviceMapping, Long> {
	public List<BotWebserviceMapping> findByBotWebserviceMessageWsMsgId(Long wsMsgId);
}
