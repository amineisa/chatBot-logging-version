package com.chatbot.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chatbot.dao.BotButtonRepo;
import com.chatbot.dao.BotButtonTemplateMSGRepo;
import com.chatbot.dao.BotConfigurationRepo;
import com.chatbot.dao.BotGTemplateMessageRepo;
import com.chatbot.dao.BotInteractionRepo;
import com.chatbot.dao.BotQuickReplyMessageRepo;
import com.chatbot.dao.BotTemplateElementRepo;
import com.chatbot.dao.BotTextMessageRepo;
import com.chatbot.dao.BotTextResponseMappingRepo;
import com.chatbot.dao.BotWebserviceMappingRepo;
import com.chatbot.dao.BotWebserviceMessageRepo;
import com.chatbot.dao.CustomerProfileRepo;
import com.chatbot.dao.EnabledCategoryConfigurationRepo;
import com.chatbot.dao.InteractionLoggingRepo;
import com.chatbot.dao.InteractionMessageRepo;
import com.chatbot.dao.PersistenceMenuButtonRepo;
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

@Service
public class ChatBotServiceImpl implements ChatBotService {
	@Autowired
	private InteractionMessageRepo interactionMessageRepo;

	@Autowired
	private BotQuickReplyMessageRepo botQuickReplyMessageRepo;

	@Autowired
	private BotButtonRepo botButtonRepo;

	@Autowired
	private BotInteractionRepo botInteractionRepo;

	@Autowired
	private BotTextMessageRepo botTextMessageRepo;

	@Autowired
	private BotGTemplateMessageRepo botGTemplateMessageRepo;

	@Autowired
	private BotTemplateElementRepo botTemplateElementRepo;

	@Autowired
	private PersistenceMenuButtonRepo persistenceMenuButtonRepo;
	
	@Autowired
	private BotWebserviceMessageRepo botWebserviceMessageRepo;
	
	@Autowired
	private BotWebserviceMappingRepo botWebserviceMappingRepo;
	
	@Autowired
	private BotTextResponseMappingRepo botTextResponseMappingRepo;
	
	@Autowired
	private BotButtonTemplateMSGRepo botButtonTemplateMSGRepo;
	
	@Autowired
	private CustomerProfileRepo customerProfileRepo;
	
	@Autowired
	private InteractionLoggingRepo interactionLoggingRepo;
	
	@Autowired
	private BotConfigurationRepo botConfigurationRepo;
	
	
	
	
	@Autowired
	private EnabledCategoryConfigurationRepo  enabledCategoryConfigurationRepo;
	
	public List<BotInteractionMessage> findInteractionMessagesByInteractionId(Long interactionId) {
		return interactionMessageRepo.findByBotInteractionInteractionIdOrderByMessagePriority(interactionId);
	}
	
	public BotInteractionMessage findMessageByInteraction(BotInteraction botInteraction){
		return interactionMessageRepo.findOneByBotInteraction(botInteraction);
	}

	public BotQuickReplyMessage findQuickReplyMessageByMessageId(Long messageId) {
		return botQuickReplyMessageRepo.findByBotInteractionMessageMessageId(messageId);
	}

	public List<BotButton> findButtonsByQuickReplyMessageId(Long quickMsgId) {
		return botButtonRepo.findByBotQuickReplyMessageQuickMsgId(quickMsgId);
	}

	public BotInteraction findInteractionByPayload(String payload) {
		return botInteractionRepo.findByPayload(payload);
	}

	public BotTextMessage findTextMessageByMessageId(Long messageId) {
		return botTextMessageRepo.findByBotInteractionMessageMessageId(messageId);
	}

	public BotGTemplateMessage findGTemplateMessageByMessageId(Long messageId) {
		return botGTemplateMessageRepo.findByBotInteractionMessageMessageId(messageId);
	}

	@Override
	public List<BotTemplateElement> findTemplateElementsByGTMsgId(Long gTMsgId) {

		return botTemplateElementRepo.findByBotGTemplateMessageGTMsgId(gTMsgId);
	}

	@Override
	public List<BotButton> findButtonsByTemplateElementId(Long elementId) {
		return botButtonRepo.findByBotTemplateElementElementId(elementId);
	}

	// Persistence Menu
	@Override
	public List<PersistenceMenuButton> findMasterButtonInPersistenceMenu() {
		return persistenceMenuButtonRepo.findByParentIdNull();
	}

	@Override
	public List<PersistenceMenuButton> findSubButtonsByItsParentId(Long parentId) {
		return persistenceMenuButtonRepo.findByParentId(parentId);
	}

	@Override
	public List<PersistenceMenuButton> findAllChildrenList() {
		return persistenceMenuButtonRepo.findByParentIdNotNull();
	}

	@Override
	public BotButton findButtonById(Long buttonId) {
		return botButtonRepo.findBotButtonByButtonId(buttonId);
	}

	@Override
	public BotButton findStartButton(Long id) {
	return	botButtonRepo.findButtonByButtonType(id);
		 
	}

	@Override
	public BotWebserviceMessage findWebserviceMessageByMessageId(Long messageId) {
				return botWebserviceMessageRepo.findByBotInteractionMessageMessageId(messageId);
	}


	@Override
	public List<BotWebserviceMapping> findWebserviceMappingByWsId(Long wsId) {
		return botWebserviceMappingRepo.findByBotWebserviceMessageWsMsgId(wsId);
	}

	@Override
	public List<BotTextResponseMapping> findTextResponseMappingByWsId(Long wsId) {
		return botTextResponseMappingRepo.findByBotWebserviceMessageWsMsgId(wsId);
	}

	@Override
	public BotButtonTemplateMSG findBotButtonTemplate(Long id) {
		return botButtonTemplateMSGRepo.findBotButtonTemplateMSGByButtonTempMsgId(id);
	}

	@Override
	public List<BotButton> findAllByBotButtonTemplateMSGId(BotButtonTemplateMSG botButtonTemplateMSG) {
		return botButtonRepo.findByBotButtonTemplateMSG(botButtonTemplateMSG);
	}

	@Override
	public List<BotButtonTemplateMSG> findBotButtonTemplateMSGByBotInteractionMessage(BotInteractionMessage interactionMsg) {
		return botButtonTemplateMSGRepo.findBotButtonTemplateMSGByBotInteractionMessage(interactionMsg);
	}

	
	@Override
	public List<BotButton> findAllButtonsByWebserviceMessage(BotWebserviceMessage botWebserviceMessage) {
		return botButtonRepo.findBotButtonByBotWebserviceMessage(botWebserviceMessage);
	}

	@Override
	public CustomerProfile getCustomerProfileBySenderId(String senderId) {
		return customerProfileRepo.findCustomerProfileBySenderID(senderId);
	}

	@Override
	public CustomerProfile saveCustomerProfile(CustomerProfile customerProfile) {
		return customerProfileRepo.save(customerProfile);
	}

	@Override
	public List<InteractionLogging> getInteractionLoggingsByCustomer(CustomerProfile customerProfile) {
		return interactionLoggingRepo.findAllByCustomerProfile(customerProfile);
	}

	@Override
	public List<InteractionLogging> getInteractionLoggingsByInteraction(BotInteraction botInteraction) {
		return interactionLoggingRepo.findAllByBotInteraction(botInteraction);
	}

	@Override
	public InteractionLogging getInteractionLoggingByID(Long id) {
		return interactionLoggingRepo.findInteractionLoggingByInteractionLoggingId(id);
	}

	@Override
	public InteractionLogging saveInteractionLogging(InteractionLogging interactionLogging) {
		return interactionLoggingRepo.save(interactionLogging);
	}
	
	@Override
	public EnabledCategoryConfiguration getEnabledCategoryConfigurationDaoById(Long id) {
		return enabledCategoryConfigurationRepo.findEnabledCategoryConfigurationById(id) ;
	}

	@Override
	public BotButtonTemplateMSG findBotButtonTemplateByMessageId(BotInteractionMessage botInteractionMessage) {
		return botButtonTemplateMSGRepo.findBotButtonTemplateMSGBybotInteractionMessage(botInteractionMessage) ;
	}

	@Override
	public List<BotConfiguration> getBotAllConfiguration() {
		return botConfigurationRepo.findAll();
	}

	@Override
	public BotConfiguration getBotConfigurationByKey(String key) {
		return botConfigurationRepo.findBotConfigurationByKey(key);
	}


	

}
