package com.chatbot.dao;

import org.springframework.data.repository.CrudRepository;

import com.chatbot.entity.BotConfiguration;

public interface BotConfigurationRepo extends CrudRepository<BotConfiguration, Long>{

}
