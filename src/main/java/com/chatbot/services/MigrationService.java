package com.chatbot.services;

import static java.util.Optional.empty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chatbot.entity.UserSelection;
import com.chatbot.util.Constants;
import com.chatbot.util.Utils;
import com.github.messenger4j.send.message.template.GenericTemplate;
import com.github.messenger4j.send.message.template.button.Button;
import com.github.messenger4j.send.message.template.button.PostbackButton;
import com.github.messenger4j.send.message.template.common.Element;
import com.hazelcast.core.IMap;

/**
 * @author Amin Eisa 
 */
@Service
public class MigrationService {

	@Autowired
	IMap<String, Object> ratePlanForMigrationCache;

	@Autowired
	private UtilService utilService;

	@Autowired
	JSONUtilsService jsonUtilsService;
	
	@Autowired
	ChatBotService chatBotService;

	public static final Logger logger = LoggerFactory.getLogger(MigrationService.class);

	GenericTemplate migrationHandling(String payload, String dial, String locale, JSONArray rateplans) {
		if (payload == "More Rateplans") {
			String cachedList = (String) ratePlanForMigrationCache.get(dial);
			rateplans = new JSONArray(cachedList);
		}
		return displayMigrationRatePlans(dial, locale, rateplans);

	}

	public GenericTemplate displayMigrationRatePlans(String dial, String locale, JSONArray rateplans) {
		Map<String, Object> responseMap = new HashMap<>();
		responseMap = calculateLengthOfRateplans(dial, rateplans);
		String list = (String) responseMap.get(Constants.MIGRATION_RATEPLANS_KEY);
		Boolean displayMoreBtn = (Boolean) responseMap.get(Constants.MIGRATION_DISPLAY_MOREBUTTON);
		JSONArray ratePlansToDisplay = new JSONArray(list);
		if (displayMoreBtn) {
			return createRateplansTemplates(ratePlansToDisplay, locale, displayMoreBtn);
		} else {
			return createRateplansTemplates(ratePlansToDisplay, locale, displayMoreBtn);
		}

		
	}

	Map<String, Object> calculateLengthOfRateplans(String dial, JSONArray originalRateplans) {
		if (originalRateplans.length() == 0) {
			originalRateplans = getCachedRateplanArray(dial);
		}
		Map<String, Object> responseMap = new HashMap<>();
		JSONArray ratePlansToDisplay = new JSONArray();
		JSONArray jsonArrToBeCached = new JSONArray();
		int rateplanLength = originalRateplans.length();
		Boolean displayMoreButton = false;
		if (rateplanLength > 10) {
			displayMoreButton = true;
			for (int i = 0; i < rateplanLength; i++) {
				if (i < 9) {
					ratePlansToDisplay.put(originalRateplans.get(i));
				} else {
					jsonArrToBeCached.put(originalRateplans.get(i));
				}

			}
			responseMap.put(Constants.MIGRATION_RATEPLANS_KEY, ratePlansToDisplay.toString());
			logger.debug("Migration Rateplans To Display " + ratePlansToDisplay);
			responseMap.put(Constants.MIGRATION_DISPLAY_MOREBUTTON, displayMoreButton);
			ratePlanForMigrationCache.put(dial, jsonArrToBeCached.toString());
			logger.debug("Cached Migration Rateplans " + jsonArrToBeCached);
			return responseMap;
		} else {
			responseMap.put(Constants.MIGRATION_RATEPLANS_KEY, originalRateplans.toString());
			responseMap.put(Constants.MIGRATION_DISPLAY_MOREBUTTON, displayMoreButton);
		}
		return responseMap;
	}

	GenericTemplate createRateplansTemplates(JSONArray ratePlansToDisplay, String locale, boolean displayMoreButton) {
		List<Element> elements = new ArrayList<>();
		
		if(ratePlansToDisplay.length() == 0) {
			List<Button> buttonsList = new ArrayList<>();
			Element element = createNotEligibilityRateplans(locale,buttonsList);
			elements.add(element);		
			return GenericTemplate.create(elements);
		}else {
		for (int i = 0; i < ratePlansToDisplay.length(); i++) {
			List<Button> buttonsList = new ArrayList<>();
			String rateplanSubmitAttributeVallue = Constants.EMPTY_STRING;
			String description, migrateButtonLabel, rateplanName;
			description = migrateButtonLabel = rateplanName = Constants.EMPTY_STRING;
			JSONObject rateplan = ratePlansToDisplay.getJSONObject(i);
			boolean submitById = rateplan.getBoolean(Constants.JSON_KEY_MIGRATION_ROUTING);
			if (submitById) {
				rateplanSubmitAttributeVallue = Constants.PREFIX_MIGRATE_ID + rateplan.getString(Constants.FB_JSON_KEY_ID);
			} else {
				rateplanSubmitAttributeVallue = Constants.PREFIX_MIGRATE_NAME + rateplan.getString(Constants.JSON_KEY_FOR_NAME);
			}
			rateplanName = jsonUtilsService.getNameValue(rateplan, locale);
			description = jsonUtilsService.getDescriptionValue(rateplan, locale);
			if (locale.contains(Constants.LOCALE_AR)) {
				migrateButtonLabel = Constants.MIGRATE_BUTTON_LABEL_AR;
			} else {
				migrateButtonLabel = Constants.MIGRATE_BUTTON_LABEL_EN;
			}
			Button button = PostbackButton.create(migrateButtonLabel, "Migrate_" + rateplanSubmitAttributeVallue);
			Button backButton = PostbackButton.create(Utils.getLabelForBackButton(locale), Constants.PAYLOAD_RATEPLAN_ACTIONS);
			buttonsList.add(button);
			buttonsList.add(backButton);
			Element element = Element.create(rateplanName, Optional.of(description), Optional.of(Utils.createUrl("https://www.etisalat.eg/7C5MEA2BK7E140G49F661MN324099FA9/images/1399314203597.jpg")),
					empty(), Optional.of(buttonsList));
			elements.add(element);
		}
		if (displayMoreButton) {
			Button moreButton = PostbackButton.create(Utils.moreButtonLabel(locale), Constants.PAYLOAD_MIGRATE_MORE);
			List<Button> morebuttonList = new ArrayList<>();
			morebuttonList.add(moreButton);
			Element moreElement = Element.create(Utils.moreElementTitle(locale), Optional.of(Utils.moreElementSubTitle(locale)),
					Optional.of(Utils.createUrl("https://www.logolynx.com/images/logolynx/9e/9e416d753c5e058caaf238fcd2bcc62e.png")), empty(), Optional.of(morebuttonList));
			elements.add(moreElement);
		}

		return GenericTemplate.create(elements);
		}
	}

	/**
	 * @param locale
	 * @return
	 */
	private Element createNotEligibilityRateplans(String locale, List<Button> buttons) {
		String title , subtitle ;
		String imageUrl = chatBotService.getBotConfigurationByKey(Constants.CONFIGURATION_TABLE_WARNING_IMAGE_URL).getValue();
		if(locale.contains(Constants.LOCALE_AR)) {
			title = Constants.NOTELIGIBLE_ELEMENT_TITLE_AR;
			subtitle= chatBotService.getBotConfigurationByKey(Constants.NOT_ELIGIBLE_RATEPLAN_SUBTITLE_AR).getValue();
		}else {
			title = Constants.NOTELIGIBLE_ELEMENT_TITLE_EN;
			subtitle= chatBotService.getBotConfigurationByKey(Constants.NOT_ELIGIBLE_RATEPLAN_SUBTITLE_EN).getValue();
			
		}
		return Element.create(title,Optional.of(subtitle),Optional.of(Utils.createUrl(imageUrl)), empty(), Optional.of(buttons));
	}

	JSONArray getCachedRateplanArray(String dial) {
		String stringRateplans = (String) ratePlanForMigrationCache.get(dial);
		if(stringRateplans != null) {
			return new JSONArray(stringRateplans);
		}else {
			return new JSONArray();
		}
	}

	public JSONObject ratePlanMigration(ArrayList<String> paramNames, String payload, UserSelection userSelections, ArrayList<String> paramValuesList) {
		JSONObject jsonParam = new JSONObject();
		if (payload.equalsIgnoreCase(Constants.PAYLOAD_MIGRATE_BY_ID)) {
			paramValuesList = new ArrayList<>(Arrays.asList(String.valueOf(userSelections.getRateplanIdForMigration()).split(Constants.COMMA_CHAR)));
			jsonParam = utilService.setRequestBodyValueForPostCalling(paramValuesList, paramNames);
		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_MIGRATE_BY_NAME)) {
			if (paramNames.size() == 2) {
				paramValuesList = new ArrayList<>(Arrays.asList(userSelections.getRateplanNameForMigration().split(Constants.COMMA_CHAR)));
				paramValuesList.add(Constants.MIGRATATION_OPERATION_VALUE);
			}
			jsonParam = utilService.setRequestBodyValueForPostCalling(paramValuesList, paramNames);
		}
		return jsonParam;

	}

}
