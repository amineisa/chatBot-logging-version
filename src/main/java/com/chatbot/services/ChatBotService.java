package com.chatbot.services;

import java.util.List;

import com.chatbot.entity.BotButton;
import com.chatbot.entity.BotButtonTemplateMSG;
import com.chatbot.entity.BotConfiguration;
import com.chatbot.entity.BotGTemplateMessage;
import com.chatbot.entity.BotInteraction;
import com.chatbot.entity.BotInteractionMessage;
import com.chatbot.entity.BotQuickReplyMessage;
import com.chatbot.entity.BotTemplateElement;
import com.chatbot.entity.BotTextMessage;
import com.chatbot.entity.BotTextResponseMapping;
import com.chatbot.entity.BotWebserviceMapping;
import com.chatbot.entity.BotWebserviceMessage;
import com.chatbot.entity.CustomerProfile;
import com.chatbot.entity.EnabledCategoryConfiguration;
import com.chatbot.entity.InteractionLogging;
import com.chatbot.entity.PersistenceMenuButton;


public interface ChatBotService {
	public List<BotInteractionMessage> findInteractionMessagesByInteractionId(Long interactionId);

	public BotQuickReplyMessage findQuickReplyMessageByMessageId(Long messageId);

	public BotInteraction findInteractionByPayload(String payload);

	public BotTextMessage findTextMessageByMessageId(Long messageId);

	public BotGTemplateMessage findGTemplateMessageByMessageId(Long messageId);

	public List<BotTemplateElement> findTemplateElementsByGTMsgId(Long gTMsgId);

	// BotButton
	public List<BotButton> findButtonsByQuickReplyMessageId(Long quickMsgId);
	
	public List<BotButton> findButtonsByTemplateElementId(Long elementId);
	
	public BotButton findButtonById(Long buttonId);
	
	public BotButton findStartButton(Long id);
	
	public BotWebserviceMessage findWebserviceMessageByMessageId(Long messageId);
	
	public List<BotWebserviceMapping> findWebserviceMappingByWsId(Long wsId);
	
	public List<BotButton> findAllByBotButtonTemplateMSGId(BotButtonTemplateMSG botButtonTemplateMSG);
	
	public List<BotButton> findAllButtonsByWebserviceMessage(BotWebserviceMessage botWebserviceMessage);
	// Persistence Menu
	
	public List<PersistenceMenuButton> findMasterButtonInPersistenceMenu();
	
	public List<PersistenceMenuButton> findSubButtonsByItsParentId(Long parentId);
	
	public List<PersistenceMenuButton> findAllChildrenList();

	// BotTextResponseMapping
	public List<BotTextResponseMapping> findTextResponseMappingByWsId(Long wsId);
	
	
	// BotButtonTemplate
	
	public BotButtonTemplateMSG findBotButtonTemplate(Long id);
	
	public BotButtonTemplateMSG findBotButtonTemplateByMessageId(BotInteractionMessage botInteractionMessage);
	
	public List<BotButtonTemplateMSG> findBotButtonTemplateMSGByBotInteractionMessage(BotInteractionMessage interactionMSG);
	
	
	//Customer Profile 
	
	public CustomerProfile getCustomerProfileBySenderId(String senderId);
	
	public CustomerProfile saveCustomerProfile(CustomerProfile customerProfile);
	
	// InteractionLogging
	
	public List<InteractionLogging> getInteractionLoggingsByCustomer(CustomerProfile customerProfile);
	
	public List<InteractionLogging> getInteractionLoggingsByInteraction(BotInteraction botInteraction);
	
	public InteractionLogging getInteractionLoggingByID(Long id);
	
	public InteractionLogging saveInteractionLogging(InteractionLogging interactionLogging);
	

	
	public EnabledCategoryConfiguration getEnabledCategoryConfigurationDaoById(Long id);

	
	// Message
	public BotInteractionMessage findMessageByInteraction(BotInteraction botInteraction);
	
	
	// BotConfiguration 
	public List<BotConfiguration> getBotAllConfiguration();
	public BotConfiguration getBotConfigurationByKey(String key);
	
}
