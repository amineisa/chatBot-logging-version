package com.chatbot;

import static java.util.Optional.empty;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import com.chatbot.util.Utils;
import com.chatbot.dao.BotButtonRepo;
import com.chatbot.entity.BotButton;
import com.chatbot.entity.PersistenceMenuButton;
import com.chatbot.services.ChatBotService;
import com.github.messenger4j.Messenger;
import com.github.messenger4j.common.SupportedLocale;
import com.github.messenger4j.common.WebviewHeightRatio;
import com.github.messenger4j.common.WebviewShareButtonState;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerIOException;
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
import com.github.messenger4j.send.message.template.button.LogOutButton;


@SpringBootApplication
@ComponentScan("com.chatbot.*")
public class MessengerChatBotMysqlApplication {
	@Autowired
	private ChatBotService chatBotService;
	
	@Autowired
	private BotButtonRepo botButtonRepo;

	private static final Logger logger = LoggerFactory.getLogger(MessengerChatBotMysqlApplication.class);
	public static void main(String[] args) {
		SpringApplication.run(MessengerChatBotMysqlApplication.class, args);
	}

	@Bean
	public Messenger messengerSendClient(@Value("${messenger4j.appSecret}") final String appSecret,
			@Value("${messenger4j.pageAccessToken}") final String pageAccessToken,
			@Value("${messenger4j.verifyToken}") final String verifyToken) {
		logger.debug("Initializing MessengerSendClient - pageAccessToken: {}", pageAccessToken);

		Messenger messenger = Messenger.create(pageAccessToken, appSecret, verifyToken);

		try {
			messenger.updateSettings(initSendPersistenceMenu());
		} catch (MessengerApiException e) {
			logger.error(e.getMessage());
		} catch (MessengerIOException e) {
			logger.error(e.getMessage());
		}
		return messenger;
	}

	private MessengerSettings initSendPersistenceMenu() {
		//List<PersistenceMenuButton> masterButtons = getMasterButtons();
		List<CallToAction> callToActions = getMasterButtons();

		// supported Local as English Language
		SupportedLocale local = SupportedLocale.en_US;
		// Optional of call To Action list
		Optional<List<CallToAction>> OtipnalPersistenceBtns = Optional.of(callToActions);
		// LocalizedPersistentMenu
		LocalizedPersistentMenu localizedPersistentMenu = LocalizedPersistentMenu.create(local, false,
				OtipnalPersistenceBtns);
		final PersistentMenu persistentMenu = PersistentMenu.create(false, OtipnalPersistenceBtns,
				localizedPersistentMenu);
		Optional<PersistentMenu> persistentMenus = Optional.of(persistentMenu);
		// Start Button
		//BotButton btn = chatBotService.findStartButton(3l);
	//	String msg = btn.getBotText().getArabicText();
		BotButton startBtn = botButtonRepo.findButtonByButtonTypeId(Utils.ButtonTypeEnum.START.getValue());
		StartButton startButton = StartButton.create(startBtn.getButtonPayload());
		Optional<StartButton> opStartButton = Optional.of(startButton);
		// Greeting Section
		String GreetingMsg = startBtn.getBotText().getEnglishText(); 
		LocalizedGreeting localizedGreeting = LocalizedGreeting.create(local,GreetingMsg);
		Greeting greeting = Greeting.create("",new LocalizedGreeting[] { localizedGreeting });
		Optional<Greeting> optionalGreeting = Optional.of(greeting);
		MessengerSettings messemgerSettings = MessengerSettings.create(opStartButton, optionalGreeting, persistentMenus,
				empty(), empty(), empty(), empty());
		return messemgerSettings;
	}

	private List<CallToAction> getMasterButtons() {
		List<PersistenceMenuButton> masterButtons = chatBotService.findMasterButtonInPersistenceMenu();
		List<CallToAction> callToActions = new ArrayList<CallToAction>();
		BotButton realButton = new BotButton();
		for (PersistenceMenuButton buttonRef : masterButtons) {
			realButton = buttonRef.getButton();
			//URLBUTTON
			if (realButton.getButtonType().getId() == Utils.ButtonTypeEnum.URL.getValue()) {
				URL url = null;
				try {
					url = new URL(realButton.getButtonUrl());
				} catch (MalformedURLException e) {
					logger.error(e.getMessage());
				}
				UrlCallToAction urlCallToAction = UrlCallToAction.create(realButton.getBotText().getEnglishText(), url,
						Optional.of(WebviewHeightRatio.FULL), empty(), empty(),
						Optional.of(WebviewShareButtonState.HIDE));
				callToActions.add(urlCallToAction);
				// PostBack 
			} else if (realButton.getButtonType().getId() == Utils.ButtonTypeEnum.POSTBACK.getValue()) {
				String title = realButton.getBotText().getEnglishText();
				String payLoad = realButton.getButtonPayload();
				PostbackCallToAction postbackCallToAction = PostbackCallToAction.create(title, payLoad);
				callToActions.add(postbackCallToAction);
			}else if(realButton.getButtonType().getId() == Utils.ButtonTypeEnum.NESTED.getValue()){
				NestedCallToAction nestedCallToAction = ifNestedButton(buttonRef);
				callToActions.add(nestedCallToAction);
			}
		}

		return callToActions;

	}

	
	

	

	public NestedCallToAction ifNestedButton(PersistenceMenuButton perButton) {
		Long id = perButton.getID();
		List<PersistenceMenuButton> subButtons = chatBotService.findSubButtonsByItsParentId(id);
		List<CallToAction> subNestedButton = new ArrayList<CallToAction>();
		for (PersistenceMenuButton subButton : subButtons) {
			if (subButton.getButton().getButtonType().getId() == Utils.ButtonTypeEnum.NESTED.getValue())  {
			    NestedCallToAction nested =  ifNestedButton(subButton);
			    subNestedButton.add(nested);
				}
			BotButton button = subButton.getButton();
			if (subButton.getButton().getButtonType().getId() == Utils.ButtonTypeEnum.POSTBACK.getValue())   {
			   String  title = button.getBotText().getEnglishText();
			   String  payload = button.getButtonPayload();
				PostbackCallToAction postBackButton = PostbackCallToAction.create(title, payload);
				subNestedButton.add(postBackButton);
			} else if (subButton.getButton().getButtonType().getId() == Utils.ButtonTypeEnum.URL.getValue())   {
				String title = button.getBotText().getEnglishText();
				String url = button.getButtonUrl();
				try {
					UrlCallToAction urlCallToAction = UrlCallToAction.create(title, new URL(url));
					subNestedButton.add(urlCallToAction);
				} catch (MalformedURLException e) {
					logger.error(e.getMessage());
				}
			}
		}
		String title = perButton.getButton().getBotText().getEnglishText();
		return  NestedCallToAction.create(title, subNestedButton);
	}

	
}
