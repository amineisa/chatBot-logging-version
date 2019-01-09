package com.chatbot;

import static java.util.Optional.empty;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

import com.chatbot.dao.BotButtonRepo;
import com.chatbot.entity.BotButton;
import com.chatbot.entity.BotConfiguration;
import com.chatbot.entity.PersistenceMenuButton;
import com.chatbot.services.ChatBotService;
import com.chatbot.util.CacheHelper;
import com.chatbot.util.Constants;
import com.chatbot.util.Utils;
import com.github.messenger4j.Messenger;
import com.github.messenger4j.common.SupportedLocale;
import com.github.messenger4j.common.WebviewHeightRatio;
import com.github.messenger4j.common.WebviewShareButtonState;
import com.github.messenger4j.messengerprofile.MessengerSettings;
import com.github.messenger4j.messengerprofile.getstarted.StartButton;
import com.github.messenger4j.messengerprofile.greeting.Greeting;
import com.github.messenger4j.messengerprofile.greeting.LocalizedGreeting;
import com.github.messenger4j.messengerprofile.persistentmenu.LocalizedPersistentMenu;
import com.github.messenger4j.messengerprofile.persistentmenu.PersistentMenu;
import com.github.messenger4j.messengerprofile.persistentmenu.action.CallToAction;
import com.github.messenger4j.messengerprofile.persistentmenu.action.NestedCallToAction;
import com.github.messenger4j.messengerprofile.persistentmenu.action.PostbackCallToAction;
import com.github.messenger4j.messengerprofile.persistentmenu.action.UrlCallToAction;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;

@SpringBootApplication
public class MessengerChatBot {
	@Autowired
	private ChatBotService chatBotService;

	@Autowired
	private BotButtonRepo botButtonRepo;
	
	@Autowired
	private CacheHelper<Object,Object> configurationCache;

	private static final Logger logger = LoggerFactory.getLogger(MessengerChatBot.class);

	public static void main(String[] args) {
		SpringApplication.run(MessengerChatBot.class, args);
	}
	
	
	
	
	@Bean
	Map<String,Messenger> messengersObjectsMap(){
		Map<String,Messenger> messengersObjectsMap = new HashMap<>();
		Map<String, String> configCacheObject = (Map<String, String>) configurationCache.getCachedValue(Constants.CONFIGURATION_CACHE_KEY);
		List<String> pagesIds = Arrays.asList(configCacheObject.get("PAGES_IDS").split(","));
		for(String pageId : pagesIds) {
			System.out.println(" ID IS "+pageId);
			String appSecret = configCacheObject.get(pageId+"_"+Constants.CONFIGURATION_TABLE_APP_SECRET);
			String pageAccessToken =  configCacheObject.get(pageId+"_"+Constants.CONFIGURATION_TABLE_PAGE_ACCESS_TOKEN);
			String verifyToken =  configCacheObject.get(pageId+"_"+Constants.CONFIGURATION_TABLE_VERIFY_TOKEN);
			Messenger messenger = Messenger.create(pageAccessToken, appSecret, verifyToken);
			messengersObjectsMap.put(pageId, messenger);
		}
		for(String key : messengersObjectsMap.keySet()) {
			System.out.println("Key "+key);
		}
		return messengersObjectsMap;
	}
	
 /* @Bean
	public Messenger messengerSendClient(@Value("${messenger4j.appSecret}") final String appSecret,
			@Value("${messenger4j.pageAccessToken}") final String pageAccessToken,
			@Value("${messenger4j.verifyToken}") final String verifyToken) {
		logger.debug("Initializing MessengerSendClient - pageAccessToken: {}", pageAccessToken);

		Messenger messenger = Messenger.create(pageAccessToken, appSecret, verifyToken);

		try {
			messenger.updateSettings(initSendPersistenceMenu());
		} catch (MessengerApiException  | MessengerIOException e) {
			logger.error(e.getMessage());
		} 
		return messenger;
	}*/
	
	
	 @Bean
	 @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
	public IMap<String,Object>  hazelCastCache() {
		    logger.info("Initializing Hazelcast Shiro session persistence..");
		    Map<String, String> configCacheObject = (Map<String, String>) configurationCache.getCachedValue(Constants.CONFIGURATION_CACHE_KEY);
		    String firstServereIp = configCacheObject.get(Constants.FIRST_CACHING_SERVER_IP) == null  ? "" : configCacheObject.get(Constants.FIRST_CACHING_SERVER_IP);
		    String secondServerIp = configCacheObject.get(Constants.SECOND_CACHING_SERVER_IP) == null ? "" : configCacheObject.get(Constants.SECOND_CACHING_SERVER_IP);
		    int communicatePort = Integer.parseInt(configCacheObject.get(Constants.COMMUNICATE_PORT_BETWEEN_CACHING_SERVERES));
		   
		    final Config cfg = new Config();
		    MapConfig mapConfig = new MapConfig();
		    mapConfig.setTimeToLiveSeconds(900);
		    Map<String,MapConfig> mapConfigs = new HashMap<>();
		    mapConfigs.put("default", mapConfig);
		    cfg.setMapConfigs(mapConfigs);
		    cfg.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
		    cfg.getNetworkConfig().setPort(communicatePort).setPortAutoIncrement(false);
		    cfg.getNetworkConfig().getJoin().getTcpIpConfig().setRequiredMember(firstServereIp);
		    cfg.getNetworkConfig().getJoin().getTcpIpConfig().addMember(firstServereIp).setEnabled(true);
		    cfg.getNetworkConfig().getJoin().getTcpIpConfig().addMember(secondServerIp).setEnabled(true);
		    cfg.setInstanceName(Constants.HAZEL_OBJECT_NAME);
		    logger.info("Hazelcast Shiro session persistence initialized.");
		    return Hazelcast.newHazelcastInstance(cfg).getMap(Constants.USER_SELECTION_MAP_KEY);

	}
	 
	@Bean
	public CacheHelper<Object, Object> configurationCache() {
		Map<String, String> configurationsMap = new HashMap<>();
		CacheHelper<Object, Object> configurationCache = new CacheHelper<>("configuration");
		List<BotConfiguration> botConfigurations = chatBotService.getBotAllConfiguration();
		for (BotConfiguration botConfiguration : botConfigurations) {
			configurationsMap.put(botConfiguration.getKey(),botConfiguration.getValue());
		}
		configurationCache.addToCentralCache(Constants.CONFIGURATION_CACHE_KEY, configurationsMap);
		return configurationCache;
	}

	/*@Bean
	public Messenger messengerSendClient() {
		boolean updatePersistenceMenu = false;
		Map<String, String> configCacheObject = (Map<String, String>) configurationCache.getCachedValue(Constants.CONFIGURATION_CACHE_KEY);
		String appSecret = configCacheObject.get(Constants.CONFIGURATION_TABLE_APP_SECRET);
		String pageAccessToken =  configCacheObject.get(Constants.CONFIGURATION_TABLE_PAGE_ACCESS_TOKEN);
		String verifyToken =  configCacheObject.get(Constants.CONFIGURATION_TABLE_VERIFY_TOKEN);
		logger.debug("Initializing MessengerSendClient - pageAccessToken: {}", pageAccessToken);
		Messenger messenger = Messenger.create(pageAccessToken, appSecret, verifyToken);
		if(updatePersistenceMenu) {
		try {
			logger.debug(" Update Persistence Menu ");
			messenger.updateSettings(initSendPersistenceMenu());
		} catch (MessengerApiException  | MessengerIOException e) {
			logger.error(e.getMessage());
		} 
		}
		return messenger;
	}*/

	private MessengerSettings initSendPersistenceMenu() {
		List<CallToAction> callToActions = getMasterButtons();

		// supported Local as English Language
		SupportedLocale arlocal = SupportedLocale.ar_AR;
		SupportedLocale enlocal = SupportedLocale.en_US;
		// Optional of call To Action list
		Optional<List<CallToAction>> otipnalPerBtns = Optional.of(callToActions);
		// LocalizedPersistentMenu
		LocalizedPersistentMenu localizedPersistentMenu = LocalizedPersistentMenu.create(arlocal, false, otipnalPerBtns);
		final PersistentMenu persistentMenu = PersistentMenu.create(false, otipnalPerBtns, localizedPersistentMenu);
		Optional<PersistentMenu> persistentMenus = Optional.of(persistentMenu);
		// Start Button
		BotButton startBtn = botButtonRepo.findButtonByButtonTypeId(Utils.ButtonTypeEnum.START.getValue());
		StartButton startButton = StartButton.create(startBtn.getButtonPayload());
		Optional<StartButton> opStartButton = Optional.of(startButton);
		// Greeting Section
		String greetingMessage = startBtn.getBotText().getArabicText();
		LocalizedGreeting localizedGreeting = LocalizedGreeting.create(enlocal, greetingMessage);
		Greeting greeting = Greeting.create(Constants.EMPTY_STRING, new LocalizedGreeting[] { localizedGreeting });
		Optional<Greeting> optionalGreeting = Optional.of(greeting);
		return MessengerSettings.create(opStartButton, optionalGreeting, persistentMenus, empty(), empty(), empty(), empty());
	}

	private List<CallToAction> getMasterButtons() {
		List<PersistenceMenuButton> masterButtons = chatBotService.findMasterButtonInPersistenceMenu();
		List<CallToAction> callToActions = new ArrayList<>();
		BotButton realButton = new BotButton();
		for (PersistenceMenuButton buttonRef : masterButtons) {
			realButton = buttonRef.getButton();
			// URLBUTTON
			if (realButton.getButtonType().getId() == Utils.ButtonTypeEnum.URL.getValue()) {
				URL url = null;
				try {
					url = new URL(realButton.getButtonUrl());
				} catch (MalformedURLException e) {
					logger.error(e.getMessage());
				}
				UrlCallToAction urlCallToAction = UrlCallToAction.create(realButton.getBotText().getArabicText(), url, Optional.of(WebviewHeightRatio.FULL), empty(), empty(),
						Optional.of(WebviewShareButtonState.HIDE));
				callToActions.add(urlCallToAction);
				// PostBack
			} else if (realButton.getButtonType().getId() == Utils.ButtonTypeEnum.POSTBACK.getValue()) {
				String title = realButton.getBotText().getArabicText();
				String payLoad = realButton.getButtonPayload();
				PostbackCallToAction postbackCallToAction = PostbackCallToAction.create(title, payLoad);
				callToActions.add(postbackCallToAction);
			} else if (realButton.getButtonType().getId() == Utils.ButtonTypeEnum.NESTED.getValue()) {
				NestedCallToAction nestedCallToAction = ifNestedButton(buttonRef);
				callToActions.add(nestedCallToAction);
			}
		}

		return callToActions;

	}

	public NestedCallToAction ifNestedButton(PersistenceMenuButton perButton) {
		Long id = perButton.getID();
		List<PersistenceMenuButton> subButtons = chatBotService.findSubButtonsByItsParentId(id);
		List<CallToAction> subNestedButton = new ArrayList<>();
		for (PersistenceMenuButton subButton : subButtons) {
			if (subButton.getButton().getButtonType().getId() == Utils.ButtonTypeEnum.NESTED.getValue()) {
				NestedCallToAction nested = ifNestedButton(subButton);
				subNestedButton.add(nested);
			}
			BotButton button = subButton.getButton();
			if (subButton.getButton().getButtonType().getId() == Utils.ButtonTypeEnum.POSTBACK.getValue()) {
				String title = button.getBotText().getArabicText();
				String payload = button.getButtonPayload();
				PostbackCallToAction postBackButton = PostbackCallToAction.create(title, payload);
				subNestedButton.add(postBackButton);
			} else if (subButton.getButton().getButtonType().getId() == Utils.ButtonTypeEnum.URL.getValue()) {
				String title = button.getBotText().getArabicText();
				String url = button.getButtonUrl();
				try {
					UrlCallToAction urlCallToAction = UrlCallToAction.create(title, new URL(url));
					subNestedButton.add(urlCallToAction);
				} catch (MalformedURLException e) {
					logger.error(e.getMessage());
				}
			}
		}
		String title = perButton.getButton().getBotText().getArabicText();
		return NestedCallToAction.create(title, subNestedButton);
	}

}