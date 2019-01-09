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

@Service
public class MigrationService {

	@Autowired
    IMap<String, Object> ratePlanForMigrationCache ;
	
	@Autowired
	private UtilService utilService;

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
		String list = (String)responseMap.get(Constants.MIGRATION_RATEPLANS_KEY);
		Boolean displayMoreBtn = (Boolean)responseMap.get(Constants.MIGRATION_DISPLAY_MOREBUTTON) ;
		JSONArray ratePlansToDisplay =new JSONArray(list) ;
		if(displayMoreBtn) {
			return createRateplansTemplates(ratePlansToDisplay, locale, displayMoreBtn);
		}else {
			return createRateplansTemplates(ratePlansToDisplay, locale, displayMoreBtn);
		}

	}

	Map<String, Object> calculateLengthOfRateplans(String dial, JSONArray originalRateplans) {
		if(originalRateplans.length() == 0) {
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
			logger.debug("Migration Rateplans To Display "+ratePlansToDisplay);
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
		for (int i = 0; i < ratePlansToDisplay.length(); i++) {
			List<Button> buttonsList = new ArrayList<>();
			String rateplanSubmitAttributeVallue = Constants.EMPTY_STRING;
			String description = "";
			JSONObject rateplan = ratePlansToDisplay.getJSONObject(i);
			boolean submitById = rateplan.getBoolean(Constants.JSON_KEY_MIGRATION_ROUTING);
			if (submitById) {
				rateplanSubmitAttributeVallue = Constants.PREFIX_MIGRATE_ID+rateplan.getString(Constants.FB_JSON_KEY_ID);
			} else {
				rateplanSubmitAttributeVallue = Constants.PREFIX_MIGRATE_NAME+rateplan.getString(Constants.JSON_KEY_FOR_NAME);
			}
			if (locale.contains(Constants.LOCALE_AR)) {
				description = rateplan.getString(Constants.JSON_KEY_NAME_AR);
			} else {
				description = rateplan.getString(Constants.JSON_KEY_NAME_EN);
			}
			Button button = PostbackButton.create(Constants.MIGRATE_BUTTON_LABEL, "Migrate_"+rateplanSubmitAttributeVallue);
			buttonsList.add(button);
			Element element = Element.create(description, Optional.of(Constants.SUBTITLE_VALUE), empty(), empty(), Optional.of(buttonsList));
			elements.add(element);
		}
		if (displayMoreButton) {
			Button moreButton = PostbackButton.create(Utils.moreButtonLabel(locale), Constants.PAYLOAD_MIGRATE_MORE );
			List<Button> morebuttonList = new ArrayList<>();
			morebuttonList.add(moreButton);
			Element moreElement = Element.create(Utils.moreElementTitle(locale), Optional.of(Utils.moreElementSubTitle(locale)), empty(), empty(), Optional.of(morebuttonList));
			elements.add(moreElement);
		}

		return GenericTemplate.create(elements);
	}
	
	
	JSONArray getCachedRateplanArray(String dial){
		String stringRateplans = (String) ratePlanForMigrationCache.get(dial);
		return new JSONArray(stringRateplans);
	}

	public JSONObject ratePlanMigration( String[] paramNames , String payload ,UserSelection userSelections,ArrayList<String> paramValuesList) {
		JSONObject jsonParam = new JSONObject();
		if (payload.equalsIgnoreCase(Constants.PAYLOAD_MIGRATE_BY_ID)) {
			paramValuesList = new ArrayList<>(Arrays.asList(String.valueOf(userSelections.getRateplanIdForMigration()).split(Constants.COMMA_CHAR)));
			jsonParam = utilService.setRequestBodyValueForPostCalling(paramValuesList, paramNames);	
		   }else if(payload.equalsIgnoreCase(Constants.PAYLOAD_MIGRATE_BY_NAME)) {
			if (paramNames.length == 2) {
				paramValuesList = new ArrayList<>(Arrays.asList(userSelections.getRateplanNameForMigration().split(Constants.COMMA_CHAR)));
				paramValuesList.add(Constants.MIGRATATION_OPERATION_VALUE);
			}
			jsonParam = utilService.setRequestBodyValueForPostCalling(paramValuesList, paramNames);
		}
		return jsonParam;
		
	}

}
