package com.chatbot.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chatbot.entity.BotInteraction;
import com.chatbot.entity.CustomerProfile;
import com.chatbot.entity.InteractionLogging;

public interface InteractionLoggingRepo extends JpaRepository<InteractionLogging, Long>{
	
	public List <InteractionLogging> findAllByCustomerProfile(CustomerProfile customerProfile);
	
	public List <InteractionLogging> findAllByBotInteraction(BotInteraction botInteraction);
	
	public InteractionLogging findInteractionLoggingByInteractionLoggingId(Long id);
	
	//@Query(value = "SELECT * FROM InteractionLogging i WHERE i.status = 1", nativeQuery = true)
	public List<InteractionLogging> findAllByCustomerProfileOrderByInteractionCallingDateDesc(CustomerProfile customerProfile); 
	
	
}
