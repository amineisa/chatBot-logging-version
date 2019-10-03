package com.chatbot.services;

import static java.util.Optional.empty;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chatbot.entity.BotButton;
import com.chatbot.entity.PersistenceMenuButton;
import com.chatbot.repo.BotButtonRepo;
import com.chatbot.util.Constants;
import com.chatbot.util.Utils;
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

/**
 * @author Amin Eisa 
 */
@Service
public class PersistentMenuService {
	
	
	@Autowired
	private BotButtonRepo botButtonRepo;
	@Autowired
	private ChatBotService chatBotService;
	
	private static final Logger logger = LoggerFactory.getLogger(PersistentMenuService.class);
	
	
	public MessengerSettings initSendPersistenceMenu() {
		logger.debug(Constants.LOGGER_INFO_PREFIX+"Update persistence Menu ");
		List<CallToAction> callToActions = getMasterButtons();
		logger.debug(Constants.LOGGER_INFO_PREFIX+"Parents buttons list size "+callToActions.size());
		// supported Local as English Language
		SupportedLocale enlocal = SupportedLocale.en_US;
		// Optional of call To Action list
		Optional<List<CallToAction>> otipnalPerBtns = Optional.of(callToActions);
		// LocalizedPersistentMenu
		LocalizedPersistentMenu localizedPersistentMenu = LocalizedPersistentMenu.create(enlocal, false, otipnalPerBtns);
		logger.debug(Constants.LOGGER_INFO_PREFIX+"Localization creation ");
		final PersistentMenu persistentMenu = PersistentMenu.create(false, otipnalPerBtns, localizedPersistentMenu);
		Optional<PersistentMenu> persistentMenus = Optional.of(persistentMenu);
		// Start Button
		logger.debug(Constants.LOGGER_INFO_PREFIX+"Create GetStarted button ");
		BotButton startBtn = botButtonRepo.findButtonByButtonTypeId(Utils.ButtonTypeEnum.START.getValue());
		StartButton startButton = StartButton.create(startBtn.getButtonPayload());
		Optional<StartButton> opStartButton = Optional.of(startButton);
		// Greeting Section
		String greetingMessage = startBtn.getBotText().getEnglishText();
		LocalizedGreeting localizedGreeting = LocalizedGreeting.create(enlocal, greetingMessage);
		Greeting greeting = Greeting.create(Constants.EMPTY_STRING,  localizedGreeting );
		Optional<Greeting> optionalGreeting = Optional.of(greeting);
		logger.debug(Constants.LOGGER_INFO_PREFIX+"Create Messenger Setting ");
		return MessengerSettings.create(opStartButton, optionalGreeting, persistentMenus, empty(), empty(), empty(), empty());
		}
	

	private List<CallToAction> getMasterButtons() {
		List<PersistenceMenuButton> masterButtons = chatBotService.findMasterButtonInPersistenceMenu();
		List<CallToAction> callToActions = new ArrayList<>();
		BotButton realButton = new BotButton();
		for (PersistenceMenuButton buttonRef : masterButtons) {
			logger.debug(Constants.LOGGER_INFO_PREFIX+"All Real Buttons ");
			realButton = buttonRef.getButton();
			// URLBUTTON
			if (realButton.getButtonType().getId() == Utils.ButtonTypeEnum.URL.getValue()) {		
				URL url = null;
				try {
					url = new URL(realButton.getButtonUrl());
				} catch (MalformedURLException e) {
					logger.error(e.getMessage());
				}
				UrlCallToAction urlCallToAction = UrlCallToAction.create(realButton.getBotText().getEnglishText(), url, Optional.of(WebviewHeightRatio.FULL), empty(), empty(),
						Optional.of(WebviewShareButtonState.HIDE));
				callToActions.add(urlCallToAction);
				// PostBack
			} else if (realButton.getButtonType().getId() == Utils.ButtonTypeEnum.POSTBACK.getValue()) {
				String title = realButton.getBotText().getEnglishText();
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
				String title = button.getBotText().getEnglishText();
				String payload = button.getButtonPayload();
				PostbackCallToAction postBackButton = PostbackCallToAction.create(title, payload);
				subNestedButton.add(postBackButton);
			} else if (subButton.getButton().getButtonType().getId() == Utils.ButtonTypeEnum.URL.getValue()) {
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
		return NestedCallToAction.create(title, subNestedButton);
	}

}
