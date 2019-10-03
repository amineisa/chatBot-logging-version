package com.chatbot.repo;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.chatbot.entity.BotConfiguration;

public interface BotConfigurationRepo extends CrudRepository<BotConfiguration, Long> {

	public List<BotConfiguration> findAll();
	public BotConfiguration findBotConfigurationByKey(String key);

}
