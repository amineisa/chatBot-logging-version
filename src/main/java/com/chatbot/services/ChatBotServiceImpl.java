package com.chatbot.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
import com.chatbot.entity.CustomerLinkingDial;
import com.chatbot.entity.CustomerProfile;
import com.chatbot.entity.EnabledCategoryConfiguration;
import com.chatbot.entity.FreeTextLogging;
import com.chatbot.entity.InteractionLogging;
import com.chatbot.entity.PersistenceMenuButton;
import com.chatbot.repo.BotButtonRepo;
import com.chatbot.repo.BotButtonTemplateMSGRepo;
import com.chatbot.repo.BotConfigurationRepo;
import com.chatbot.repo.BotGTemplateMessageRepo;
import com.chatbot.repo.BotInteractionRepo;
import com.chatbot.repo.BotQuickReplyMessageRepo;
import com.chatbot.repo.BotTemplateElementRepo;
import com.chatbot.repo.BotTextMessageRepo;
import com.chatbot.repo.BotTextResponseMappingRepo;
import com.chatbot.repo.BotWebserviceMappingRepo;
import com.chatbot.repo.BotWebserviceMessageRepo;
import com.chatbot.repo.CustomerLinkingDialDao;
import com.chatbot.repo.CustomerProfileRepo;
import com.chatbot.repo.EnabledCategoryConfigurationRepo;
import com.chatbot.repo.FreeTextLoggingDao;
import com.chatbot.repo.InteractionLoggingRepo;
import com.chatbot.repo.InteractionMessageRepo;
import com.chatbot.repo.PersistenceMenuButtonRepo;

/**
 * @author Amin Eisa 
 */
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
	private FreeTextLoggingDao freeTextLoggingDao;
	@Autowired
	private CustomerLinkingDialDao customerLinkingDialDao;
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

	@Override
	public FreeTextLogging saveFreeTextLogging(FreeTextLogging freeTextLogging) {
		return freeTextLoggingDao.save(freeTextLogging);
	}

	@Override
	public CustomerLinkingDial saveCustomerLinkingDial(CustomerLinkingDial customerLinkingDial) {
		return customerLinkingDialDao.save(customerLinkingDial);
	}

	@Override
	public CustomerLinkingDial getCustomerLinkingDialById(String dial) {
		return customerLinkingDialDao.getCustomerLinkingDialByDial(dial);
	}

	/* (non-Javadoc)
	 * @see com.chatbot.services.ChatBotService#findButtonByPayload(java.lang.String)
	 */
	@Override
	public List<BotButton> findAllButtonsByPayload(String payload) {
		return botButtonRepo.findByButtonPayload(payload);
	}

	
	
	
	

}
