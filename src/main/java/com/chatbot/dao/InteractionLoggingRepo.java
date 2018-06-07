package com.chatbot.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.chatbot.entity.BotInteraction;
import com.chatbot.entity.CustomerProfile;
import com.chatbot.entity.InteractionLogging;

public interface InteractionLoggingRepo extends CrudRepository<InteractionLogging, Long>{
	
	public List <InteractionLogging> findAllByCustomerProfile(CustomerProfile customerProfile);
	
	public List <InteractionLogging> findAllByBotInteraction(BotInteraction botInteraction);
	
	public InteractionLogging findInteractionLoggingByInteractionLoggingId(Long id);


}
