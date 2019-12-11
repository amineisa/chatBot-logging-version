/** copyright Etisalat social team. To present All rights reserved
*/
/**
 * 
 */
package com.chatbot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

import com.chatbot.entity.BotConfiguration;
import com.chatbot.services.ChatBotService;
import com.chatbot.services.PersistentMenuService;
import com.chatbot.util.Constants;
import com.github.messenger4j.Messenger;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerIOException;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;

/**
 * @author A.Eissa 
 */
@org.springframework.context.annotation.Configuration
public class Configuration {
	
	@Autowired
	private ChatBotService chatBotService;
	@Autowired
	private PersistentMenuService persistentMenuService;
	
	private static final Logger logger = LoggerFactory.getLogger(Configuration.class);
	
	
	@Bean
	Map<String, Messenger> messengersObjectsMap() {
		Map<String, Messenger> messengersObjectsMap = new HashMap<>();
		Map<String, String> configCacheObject = cofigurationMap();
		List<String> pagesIds = Arrays.asList(configCacheObject.get("PAGES_IDS").split(","));
		for (String pageId : pagesIds) {
			String appSecret = configCacheObject.get(pageId + "_" + Constants.CONFIGURATION_TABLE_APP_SECRET);
			String pageAccessToken = configCacheObject.get(pageId + "_" + Constants.CONFIGURATION_TABLE_PAGE_ACCESS_TOKEN);
			String verifyToken = configCacheObject.get(pageId + "_" + Constants.CONFIGURATION_TABLE_VERIFY_TOKEN);
			logger.debug(Constants.LOGGER_INFO_PREFIX+"PageID "+pageId + " AppSecret "+appSecret+" Page Access Token "+pageAccessToken+" Verify Token "+verifyToken);
			Messenger messenger = Messenger.create(pageAccessToken, appSecret, verifyToken);
			String update = chatBotService.getBotConfigurationByKey(Constants.UPDATE_PERSISTENCE_MENU_KEY).getValue();
			boolean b = Boolean.parseBoolean(update);
			if (b) {
				updatePersistenceMenu(messenger);
			}
			messengersObjectsMap.put(pageId, messenger);
		}
		return messengersObjectsMap;
	}


	public void updatePersistenceMenu(Messenger messenger) {
		try {
			messenger.updateSettings(persistentMenuService.initSendPersistenceMenu());
		} catch (MessengerApiException | MessengerIOException e) {
			e.printStackTrace();
		}
	}

	
	@Bean
	@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
	public IMap<String, Object> hazelCastCache() {
		logger.info("Initializing Hazelcast Shiro session persistence..");
		Map<String, String> configCacheObject = cofigurationMap(); 
		String firstServereIp = configCacheObject.get(Constants.FIRST_CACHING_SERVER_IP) == null ? "" : configCacheObject.get(Constants.FIRST_CACHING_SERVER_IP);
		String secondServerIp = configCacheObject.get(Constants.SECOND_CACHING_SERVER_IP) == null ? "" : configCacheObject.get(Constants.SECOND_CACHING_SERVER_IP);
		int communicatePort = Integer.parseInt(configCacheObject.get(Constants.COMMUNICATE_PORT_BETWEEN_CACHING_SERVERES));	
		final Config cfg = new Config();
		MapConfig mapConfig = new MapConfig();
		mapConfig.setTimeToLiveSeconds(900);
		Map<String, MapConfig> mapConfigs = new HashMap<>();
		mapConfigs.put("default", mapConfig);
		cfg.setMapConfigs(mapConfigs);
		cfg.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
		cfg.getNetworkConfig().setPort(communicatePort).setPortAutoIncrement(false);
		cfg.getNetworkConfig().getJoin().getTcpIpConfig().setRequiredMember(firstServereIp);
		cfg.getNetworkConfig().getJoin().getTcpIpConfig().addMember(firstServereIp).setEnabled(true);
		cfg.getNetworkConfig().getJoin().getTcpIpConfig().addMember(secondServerIp).setEnabled(true);
		cfg.setInstanceName(Constants.HAZEL_OBJECT_NAME);
		logger.info("Hazelcast Shiro session persistence initialized.");
		//	return Hazelcast.getOrCreateHazelcastInstance(cfg).getMap(Constants.USER_SELECTION_MAP_KEY);
		return Hazelcast.newHazelcastInstance(cfg).getMap(Constants.USER_SELECTION_MAP_KEY);

	}


    @Bean
    public Map<String , String> cofigurationMap(){
    	List<BotConfiguration> botConfigurations = chatBotService.getBotAllConfiguration();
    	Map<String , String > configurationMap = new HashMap<>();
    	botConfigurations.forEach(config ->{
    		configurationMap.put(config.getKey(), config.getValue());
    	});
    	return configurationMap;
    }
}
