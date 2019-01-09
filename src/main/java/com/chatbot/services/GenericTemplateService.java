package com.chatbot.services;

import static java.util.Optional.empty;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chatbot.entity.BotButton;
import com.chatbot.entity.BotGTemplateMessage;
import com.chatbot.entity.BotTemplateElement;
import com.chatbot.entity.BotTextResponseMapping;
import com.chatbot.entity.BotWebserviceMessage;
import com.chatbot.util.Constants;
import com.chatbot.util.Utils;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.MessagingType;
import com.github.messenger4j.send.message.TemplateMessage;
import com.github.messenger4j.send.message.template.GenericTemplate;
import com.github.messenger4j.send.message.template.ListTemplate;
import com.github.messenger4j.send.message.template.Template;
import com.github.messenger4j.send.message.template.button.Button;
import com.github.messenger4j.send.message.template.button.PostbackButton;
import com.github.messenger4j.send.message.template.common.Element;

@Service
public class GenericTemplateService {
	
	@Autowired
	private UtilService utilService;
	
	
	
	
	private static final Logger logger = LoggerFactory.getLogger(GenericTemplateService.class);
	
	public Template createGenericTemplate(Long messageId, ChatBotService chatBotService, String userlocale, BotWebserviceMessage botWebserviceMessage, JSONObject jsonObject, String dialNumber,
			List<String> consumptionNames, String payload) {		
		BotGTemplateMessage botGTemplateMessage = chatBotService.findGTemplateMessageByMessageId(messageId);
		List<BotTemplateElement> botTemplateElementList = chatBotService.findTemplateElementsByGTMsgId(botGTemplateMessage.getGTMsgId());
		List<Element> elements = new ArrayList<>();
		for (BotTemplateElement botTemplateElement : botTemplateElementList) {
			List<BotButton> elementButtonList = chatBotService.findButtonsByTemplateElementId(botTemplateElement.getElementId());
			List<Button> buttonsList = new ArrayList<>();
			for (BotButton botButton : elementButtonList) {
				Button button = utilService.createButton(botButton, userlocale, new JSONObject(), dialNumber);
				buttonsList.add(button);
			}		
			Element element = null;
			String elemnetImageUrl= botTemplateElement.getImageUrl();
			String title = getTitletBotTemplateElement(botTemplateElement, userlocale);
			String subtitle = getSubTitletBotTemplateElement(botTemplateElement, userlocale);
			if(payload.equals(Constants.PAYLOAD_RATEPLAN_ACTIONS) || payload.equals(Constants.PAYLOAD_MOBILE_INTERNET_CONTROLLER)) {
				JSONArray jArray = new JSONArray();
				if(payload.equals(Constants.PAYLOAD_RATEPLAN_ACTIONS)) {
				 jArray = jsonObject.getJSONArray(Constants.JSON_KEY_RATEPLAN);
				}else if(payload.equals(Constants.PAYLOAD_MOBILE_INTERNET_CONTROLLER)) {
				 jArray = jsonObject.getJSONArray(Constants.JSON_KEY_MOBILE_INTERNET);	
				}
				Map<String,String> detailsMap =  createMainBundleDetails(jArray , userlocale);
				title = detailsMap.get("bundleName");
				subtitle = detailsMap.get("details");
				elemnetImageUrl += title + ".png?version=1";
			}
			if (subtitle.contains("?") && botWebserviceMessage != null) {
				Long wsId = botWebserviceMessage.getWsMsgId();
				BotTextResponseMapping botTextResponseMapping = chatBotService.findTextResponseMappingByWsId(wsId).get(0);
				String[] keys = utilService.getKeysString(botTextResponseMapping, userlocale).split(Constants.COMMA_CHAR);
				String[] paths = utilService.getPaths(botTextResponseMapping.getCommonPath());
				Map<String, ArrayList<String>> mapValues = utilService.switchToObjectMode(jsonObject, paths, keys, subtitle, userlocale);
				ArrayList<String> values = mapValues.get(Constants.RESPONSE_MAP_MESSAGE_KEY);
				ArrayList<String> titles = mapValues.get(Constants.RESPONSE_MAP_TITLE_KEY);
				ArrayList<String> percentageList = mapValues.get(Constants.RESPONSE_PERCENTAGE_KEY);
				if (values == null || values.isEmpty()) {
					String msg = Utils.informUserThatHeDoesnotSubscribeAtAnyMIBundle(userlocale);
					createNoConsumptionTemplate(userlocale, elements, botTemplateElement, buttonsList, msg);
				} 
				else {
					for (int i = 0; i < values.size(); i++) {
						if (payload.equalsIgnoreCase(Constants.PAYLOAD_RATEPLAN_ADDONS_CONSUMPTION)) {
							title = consumptionNames.get(i);
						} else {
							title = titles.get(i);
						}
						if ((title.equalsIgnoreCase(Constants.MOBILE_INTERNET_CONSUMPTION_NAME_EN) || title.equalsIgnoreCase(Constants.MOBILE_INTERNET_CONSUMPTION_NAME_AR))
								&& !payload.equalsIgnoreCase("rateplan details")) {
							title = consumptionNames.size() > i ? consumptionNames.get(i) : title;
						}
						String perc = String.valueOf(percentageList.get(i));
						if (perc.contains(Constants.IS_KEY_HAS_DOT)) {
							perc = perc.substring(0, perc.indexOf('.'));
						}
						elemnetImageUrl = botTemplateElement.getImageUrl() + perc + ".png?version=1";
						element = createElement(buttonsList, elemnetImageUrl, title, values.get(i));
						elements.add(element);
					}
				}
				} else {
				if (elemnetImageUrl.length() > 1 ) {
					URL imageUrl = Utils.createUrl(elemnetImageUrl);
					element = Element.create(title, Optional.of(subtitle), Optional.of(imageUrl), empty(), Optional.of(buttonsList));
				} else {
					element = Element.create(title, Optional.of(botTemplateElement.getSubTitle().getEnglishText()), empty(), empty(), Optional.of(buttonsList));
				}
				elements.add(element);
			}
		}
		if (botGTemplateMessage.isListTemplate() != null ) {
			
			return ListTemplate.create(elements);					
		} else {
			return GenericTemplate.create(elements);
		}
	}
	
	
	public String getTitletBotTemplateElement(BotTemplateElement botTemplateElement, String local) {
		String title = Constants.EMPTY_STRING;
		if (local.equalsIgnoreCase(Constants.LOCALE_AR)) {
			title = botTemplateElement.getTitle().getArabicText();
		} else {
			title = botTemplateElement.getTitle().getEnglishText();
		}
		return title;
	}
	
	/**
	 * @param buttonsList
	 * @param elemnetImageUrl
	 * @param title
	 * @param msg
	 * @return
	 */
	public Element createElement(List<Button> buttonsList, String elemnetImageUrl, String title, String msg) {
		Element element;
		if (elemnetImageUrl.length() > 1) {
			URL imageUrl = Utils.createUrl(elemnetImageUrl);
			element = Element.create(title, Optional.of(msg), Optional.of(imageUrl), empty(), Optional.of(buttonsList));
		} else {
			element = Element.create(title, Optional.of(msg), empty(), empty(), Optional.of(buttonsList));
		}
		return element;
	}



	
	public void createNoConsumptionTemplate(String userlocale, List<Element> elements, BotTemplateElement botTemplateElement, List<Button> buttonsList, String msg) {
		String elemnetImageUrl;
		Element element;
		String errorTitle = Constants.EMPTY_STRING;
		elemnetImageUrl = botTemplateElement.getImageUrl() + Constants.URL_WARNING_IMAGE;
		if (userlocale.contains(Constants.LOCALE_AR)) {
			errorTitle = Constants.NOTELIGIBLE_ELEMENT_TITLE_AR;
		} else {
			errorTitle = Constants.NOTELIGIBLE_ELEMENT_TITLE_EN;
		}
		element = createElement(buttonsList, elemnetImageUrl, errorTitle, msg);
		elements.add(element);
	}

	public String getSubTitletBotTemplateElement(BotTemplateElement botTemplateElement, String local) {
		String title = Constants.EMPTY_STRING;
		if (local.equalsIgnoreCase(Constants.LOCALE_AR)) {
			title = botTemplateElement.getSubTitle().getArabicText();
		} else {
			title = botTemplateElement.getSubTitle().getEnglishText();
		}
		return title;
	}

	Map<String,String> createMainBundleDetails(JSONArray ratePlanArray, String userlocale) {
		Map<String,String> details = new HashMap<>();
		for (int i = 0; i < ratePlanArray.length(); i++) {
			JSONObject bundleDetails = ratePlanArray.getJSONObject(i);
			details = getCommercialNameAndRenewalDateForMainBundle(userlocale, bundleDetails);
		}

		return details;
	}
	
	
	public Map<String , String > getCommercialNameAndRenewalDateForMainBundle(String userlocale, JSONObject bundleDetails) {
		String comercialName, renwalDate, details;
		Map<String , String > detailsMap = new HashMap<>();
		boolean isHasRenewalDate = true;
		if (bundleDetails.get(Constants.JSON_KEY_RENEWAL_DATE).equals(null)) {
			isHasRenewalDate = false;
		}
		if (isHasRenewalDate) {
			if (userlocale.contains(Constants.LOCALE_AR)) {
				comercialName = bundleDetails.getJSONObject(Constants.JSON_KEY_COMMERCIAL_NAME).getString(Constants.JSON_KEY_VALUE_AR);
				renwalDate = bundleDetails.getJSONObject(Constants.JSON_KEY_RENEWAL_DATE).getString(Constants.JSON_KEY_VALUE_AR);
				details = " باقتك هي  " + comercialName + " تاريخ التجديد " + renwalDate;
			} else {
				comercialName = bundleDetails.getJSONObject(Constants.JSON_KEY_COMMERCIAL_NAME).getString(Constants.JSON_KEY_VALUE_EN);
				renwalDate = bundleDetails.getJSONObject(Constants.JSON_KEY_RENEWAL_DATE).getString(Constants.JSON_KEY_VALUE_EN);
				details = "Your Bundle is " + comercialName + " and Renewal Date is " + renwalDate;
			}
		} else {
			if (userlocale.contains(Constants.LOCALE_AR)) {
				comercialName = bundleDetails.getJSONObject(Constants.JSON_KEY_COMMERCIAL_NAME).getString(Constants.JSON_KEY_VALUE_AR);
				details = " باقتك هي  " + comercialName;
			} else {
				comercialName = bundleDetails.getJSONObject(Constants.JSON_KEY_COMMERCIAL_NAME).getString(Constants.JSON_KEY_VALUE_EN);

				details = "Your Bundle is " + comercialName;
			}
		}
		detailsMap.put("bundleName" , comercialName);
		detailsMap.put("details", details);
		return detailsMap;
	}
	
	
	public MessagePayload getBundleCategories(JSONArray arrayResponse, String senderId, ChatBotService chatBotService, String locale, String dial) {
		ArrayList<Element> elements = new ArrayList<>();
		ArrayList<String> categories = new ArrayList<>();
		String[] categoriesArray = chatBotService.getEnabledCategoryConfigurationDaoById(1l).getEnglishCategories().split(Constants.COMMA_CHAR);
		categories = new ArrayList<>(Arrays.asList(categoriesArray));
		for (int i = 0; i < arrayResponse.length(); i++) {
			try {
				JSONObject object = arrayResponse.getJSONObject(i);
				JSONObject categoryObject = object.getJSONObject(Constants.JSON_KEY_CATEGORY_KEY);
				String title, subTitle, buttonLabel;
				title = subTitle = buttonLabel = Constants.EMPTY_STRING;
				String name = Utils.getLabelForViewButton(locale);
				if (categories.contains(categoryObject.get(Constants.JSON_KEY_CATEGORY_ID))) {
					if (locale.equalsIgnoreCase(Constants.LOCALE_AR)) {
						buttonLabel = categoryObject.getString(Constants.JSON_KEY_CATEGORY_NAME_AR);
					} else {
						buttonLabel = categoryObject.getString(Constants.JSON_KEY_CATEGORY_NAME_EN);
					}
					String payLoad = Constants.PREFIX_RATEPLAN_SUBSCRIPTION + categoryObject.getString(Constants.JSON_KEY_CATEGORY_ID);

					title = buttonLabel;
					subTitle = Constants.SUBTITLE_VALUE;
					List<Button> buttonsList = new ArrayList<>();
					PostbackButton bundleButton = PostbackButton.create(name, payLoad);
					PostbackButton backButton = PostbackButton.create(Utils.getLabelForBackButton(locale), Constants.PAYLOAD_CONSUMPTION);
					buttonsList.add(bundleButton);
					buttonsList.add(backButton);
					Element element = Element.create(title, Optional.of(subTitle), empty(), empty(), Optional.of(buttonsList));
					elements.add(element);
				}

			} catch (JSONException e) {
				logger.error(Constants.LOGGER_EXCEPTION_MESSAGE + e);
				e.printStackTrace();
			}
		}
		if (!elements.isEmpty()) {
			GenericTemplate gTemplate = GenericTemplate.create(elements);
			return MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));
		} else {
			List<Button> buttonsList = new ArrayList<>();
			GenericTemplate gTemplate = createGenericTemplateForNotEligiblBundleDials(locale, buttonsList, elements, dial);

			return MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));
		}
	}
	
	public GenericTemplate createGenericTemplateForNotEligiblBundleDials(String locale, List<Button> buttonsList, List<Element> elements, String dial) {
		if (locale.contains(Constants.LOCALE_AR)) {
			PostbackButton bundleButton = PostbackButton.create(Utils.getLabelForBackButton(locale), Constants.PAYLOAD_MI_CONSUMTION_PARENT);
			buttonsList.add(bundleButton);
			Element element = Element.create(Constants.INTERNET_BUNDLE_AR, Optional.of("نأسف هذا الرقم" + " ' " + dial + " ' " + "لا توجد باقات صالحة له"), empty(), empty(), Optional.of(buttonsList));
			elements.add(element);
			return GenericTemplate.create(elements);
		} else {
			PostbackButton bundleButton = PostbackButton.create(Utils.getLabelForBackButton(locale), Constants.PAYLOAD_MI_CONSUMTION_PARENT);
			buttonsList.add(bundleButton);
			Element element = Element.create(Constants.MOBILE_INTERNET_CONSUMPTION_NAME_EN, Optional.of("Your dial" + " ' " + dial + " ' " + "is not eligible to any bundle"), empty(), empty(),
					Optional.of(buttonsList));
			elements.add(element);
			return GenericTemplate.create(elements);
		}
	}

}
