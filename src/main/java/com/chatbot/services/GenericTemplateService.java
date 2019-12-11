package com.chatbot.services;

import static java.util.Optional.empty;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import com.chatbot.entity.CustomerProfile;
import com.chatbot.entity.UserSelection;
import com.chatbot.util.Constants;
import com.chatbot.util.Utils;
import com.github.messenger4j.Messenger;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.MessagingType;
import com.github.messenger4j.send.message.TemplateMessage;
import com.github.messenger4j.send.message.template.GenericTemplate;
import com.github.messenger4j.send.message.template.ListTemplate;
import com.github.messenger4j.send.message.template.Template;
import com.github.messenger4j.send.message.template.button.Button;
import com.github.messenger4j.send.message.template.button.PostbackButton;
import com.github.messenger4j.send.message.template.common.Element;

/**
 * @author Amin Eisa
 */
@Service
public class GenericTemplateService {

	@Autowired
	private UtilService utilService;
	@Autowired
	private JSONUtilsService jsonUtilService;
	@Autowired
	private ChatBotService chatBotService;
	@Autowired
	private InteractionHandlingService interactionHandlingService;
	@Autowired
	private MigrationService migrationService;
	@Autowired
	private SallefnyService sallefnyService;
	@Autowired
	private AkwaKartService akwaKartService;
	@Autowired
	private EmeraldService emeraldService;
	@Autowired 
	private BusinessErrorService businessErrorService;

	private static final Logger logger = LoggerFactory.getLogger(GenericTemplateService.class);

	public Template createGenericTemplate(Long messageId, ChatBotService chatBotService, CustomerProfile customerProfile, BotWebserviceMessage botWebserviceMessage, JSONObject jsonObject,
			List<String> consumptionNames, String payload) {
		String dialNumber = customerProfile.getMsisdn();
		String userlocale = customerProfile.getLocale();
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
				String elemnetImageUrl = botTemplateElement.getImageUrl();
				String title = getTitletBotTemplateElement(botTemplateElement, userlocale);
				String subtitle = getSubTitletBotTemplateElement(botTemplateElement, userlocale);
				if (payload.equals(Constants.PAYLOAD_RATEPLAN_ACTIONS) || payload.equals(Constants.PAYLOAD_MOBILE_INTERNET_CONTROLLER)) {
					logger.debug(Constants.LOGGER_INFO_PREFIX + " Normal Rateplan");
					JSONArray jArray = new JSONArray();
					if (payload.equals(Constants.PAYLOAD_RATEPLAN_ACTIONS)) {
						jArray = jsonObject.getJSONArray(Constants.JSON_KEY_RATEPLAN);
						Map<String, String> detailsMap = createMainBundleDetails(jArray, userlocale);
						title = detailsMap.get(Constants.BUNDLE_NAME_KEY);
						subtitle = detailsMap.get(Constants.DETAILS_KEY_SUBTITLE);
						elemnetImageUrl += title + Constants.ELEMENT_PHOTO_EXTENSTION;
						// display Emerald actions at operations template
						String productName = jArray.getJSONObject(0).getString(Constants.JSON_KEY_PRODUCT_ID);
						if (productName.contains(Constants.EMERALD_PRODUCT_FAMILY_NAME)) {
							UserSelection userSelection = utilService.getUserSelectionsFromCache(customerProfile.getMsisdn());
							userSelection.setEmeraldRateplanProductName(productName);
							utilService.updateUserSelectionsInCache(customerProfile.getSenderID(), userSelection);
							List<Button> emeraldActions = new ArrayList<>();
							emeraldActions.add(buttonsList.get(0));
							emeraldActions.add(buttonsList.get(1));
							PostbackButton emeraldActionButton = PostbackButton.create(getEmeraldButtonLabel(userlocale), Constants.PAYLOAD_EMERALD_ACTIONS);
							emeraldActions.add(emeraldActionButton);
							buttonsList = new ArrayList<>(emeraldActions);
						}
					} else if (payload.equals(Constants.PAYLOAD_MOBILE_INTERNET_CONTROLLER)) {
						jArray = jsonObject.getJSONArray(Constants.JSON_KEY_MOBILE_INTERNET);
						Map<String, String> detailsMap = createMainBundleDetails(jArray, userlocale);
						title = detailsMap.get(Constants.BUNDLE_NAME_KEY);
						subtitle = detailsMap.get(Constants.DETAILS_KEY_SUBTITLE);
						elemnetImageUrl += title + Constants.ELEMENT_PHOTO_EXTENSTION;
					}

				} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_RATEPLAN_WITHOUT_METER)) {
					logger.debug(Constants.LOGGER_INFO_PREFIX + " Ahlan Rateplan");
					UserSelection userSelection = utilService.getUserSelectionsFromCache(customerProfile.getSenderID());
					title += userSelection.getBalanceValue();
					logger.debug(Constants.LOGGER_INFO_PREFIX + " parent payload " + userSelection.getParentPayLoad());
					element = Element.create(title, Optional.of(subtitle), Optional.of(Utils.createUrl(botTemplateElement.getImageUrl())), empty(), Optional.of(buttonsList));
				}
				if (subtitle != null && subtitle.contains("?") && botWebserviceMessage != null && payload != null && !payload.equals(Constants.PAYLOAD_MOBILE_INTERNET_CONTROLLER)) {
					if (checkAkwaKart(jsonObject).get("hasAkwaKart") == "true" && payload.equalsIgnoreCase(Constants.PAYLOAD_RATEPLAN_ADDONS_CONSUMPTION)) {
						element = akwaKartService.getAkwaKartConsumptionValue(new JSONObject(checkAkwaKart(jsonObject).get("consumption")), userlocale);
						elements.add(element);
					} else {
						Long wsId = botWebserviceMessage.getWsMsgId();
						BotTextResponseMapping botTextResponseMapping = chatBotService.findTextResponseMappingByWsId(wsId).get(0);
						String[] keys = utilService.getKeysString(botTextResponseMapping, userlocale).split(Constants.COMMA_CHAR);
						String[] paths = utilService.getPaths(botTextResponseMapping.getCommonPath());
						Map<String, ArrayList<String>> mapValues = utilService.switchToObjectMode(jsonObject, paths, keys, subtitle, userlocale);
						ArrayList<String> values = mapValues.get(Constants.RESPONSE_MAP_MESSAGE_KEY);
						ArrayList<String> titles = mapValues.get(Constants.RESPONSE_MAP_TITLE_KEY);
						ArrayList<String> percentageList = new ArrayList<>();
						if (mapValues.containsKey(Constants.RESPONSE_PERCENTAGE_KEY)) {
							percentageList = mapValues.get(Constants.RESPONSE_PERCENTAGE_KEY);
						}
						if (values == null || values.isEmpty()) {
							String msg = Utils.informUserThatHeDoesnotSubscribeAtAnyMIBundle(userlocale);
							createNoConsumptionTemplate(userlocale, elements, botTemplateElement, buttonsList, msg);
						} else {
							for (int i = 0; i < values.size(); i++) {
								title = payload.equalsIgnoreCase(Constants.PAYLOAD_RATEPLAN_ADDONS_CONSUMPTION) ? consumptionNames.get(i) : titles.get(i);
								if ((title.equalsIgnoreCase(Constants.MOBILE_INTERNET_CONSUMPTION_NAME_EN) || title.equalsIgnoreCase(Constants.MOBILE_INTERNET_CONSUMPTION_NAME_AR))
										&& !payload.equalsIgnoreCase(Constants.PAYLOAD_RATEPLAN_DETAILS)) {
									title = consumptionNames.size() > i ? consumptionNames.get(i) : title;
								}
								String perc = String.valueOf(percentageList.get(i));
								if (perc.contains(Constants.IS_KEY_HAS_DOT)) {
									perc = perc.substring(0, perc.indexOf('.'));
								}
								elemnetImageUrl = botTemplateElement.getImageUrl() + perc + Constants.ELEMENT_PHOTO_EXTENSTION;
								element = createElement(buttonsList, elemnetImageUrl, title, values.get(i));
								elements.add(element);
							}
						}
					}
				} else {
					element = elemnetImageUrl != null && elemnetImageUrl.length() > 20
							? Element.create(title, Optional.of(subtitle), Optional.of(Utils.createUrl(elemnetImageUrl)), empty(), Optional.of(buttonsList))
							: Element.create(title, Optional.of(botTemplateElement.getSubTitle().getEnglishText()), empty(), empty(), Optional.of(buttonsList));
					elements.add(element);
				}
			}
		
		if (botGTemplateMessage.isListTemplate() != null) {
			return ListTemplate.create(elements);
		} else {
			return GenericTemplate.create(elements);
		}
	}

	/**
	 * @param userlocale
	 * @return
	 */
	private String getEmeraldButtonLabel(String userlocale) {
		return userlocale.contains(Constants.LOCALE_AR) ? "خدمات اميرالد" : "Emerald Services";
	}

	/**
	 * @param jsonObject
	 * @return
	 */
	private Map<String, String> checkAkwaKart(JSONObject jsonObject) {
		Map<String, String> values = new HashMap<>();
		boolean hasAkwaKart = false;
		JSONObject consumption = new JSONObject();
		JSONArray ratePlanAddons = jsonObject.getJSONArray("ratePlanAddons");
		for (int i = 0; i < ratePlanAddons.length(); i++) {
			JSONObject addonConsumption = ratePlanAddons.optJSONObject(0);
			JSONArray consumptionDetails = addonConsumption.getJSONArray("consumptionDetailsList");
			for (int o = 0; o < consumptionDetails.length(); o++) {
				consumption = consumptionDetails.getJSONObject(o);
				if (consumption.get("consumed") == null) {
					hasAkwaKart = true;
					values.put("hasAkwaKart", String.valueOf(hasAkwaKart));
					values.put("consumption", consumption.toString());
				}
			}
		}
		logger.debug(Constants.LOGGER_INFO_PREFIX + "AkwaKart Consumpton " + hasAkwaKart);
		return values;
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
		Element element;
		String errorTitle = Constants.EMPTY_STRING;
		String elemnetImageUrl = botTemplateElement.getImageUrl() + Constants.URL_WARNING_IMAGE;
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

	Map<String, String> createMainBundleDetails(JSONArray ratePlanArray, String userlocale) {
		Map<String, String> details = new HashMap<>();
		for (int i = 0; i < ratePlanArray.length(); i++) {
			JSONObject bundleDetails = ratePlanArray.getJSONObject(i);
			details = getCommercialNameAndRenewalDateForMainBundle(userlocale, bundleDetails);
		}

		return details;
	}

	public Map<String, String> getCommercialNameAndRenewalDateForMainBundle(String userlocale, JSONObject bundleDetails) {
		String comercialName, renwalDate, details;
		Map<String, String> detailsMap = new HashMap<>();
		boolean isHasRenewalDate = hasRenewalDate(bundleDetails);
		comercialName = jsonUtilService.getArabicOrEnglishValue(bundleDetails.getJSONObject(Constants.JSON_KEY_COMMERCIAL_NAME), userlocale);
		details = Constants.EMPTY_STRING;
		if (isHasRenewalDate) {
			renwalDate = jsonUtilService.getRenewalDateValue(bundleDetails.getJSONObject(Constants.JSON_KEY_RENEWAL_DATE), userlocale);
			details = finalDetails(comercialName, renwalDate, userlocale);
		} else {
			details = finalDetailsWithoutRenewalDate(comercialName, userlocale);
		}
		detailsMap.put(Constants.BUNDLE_NAME_KEY, comercialName);
		detailsMap.put(Constants.DETAILS_KEY_SUBTITLE, details);
		return detailsMap;
	}

	private String finalDetailsWithoutRenewalDate(String comercialName, String userlocale) {
		if (userlocale.contains(Constants.LOCALE_AR)) {
			return " باقتك هي  " + comercialName;
		} else {
			return "Your Bundle is " + comercialName;
		}
	}

	private String finalDetails(String comercialName, String renwalDate, String userLocale) {
		if (userLocale.contains(Constants.LOCALE_AR)) {
			return " باقتك هي  " + comercialName + " تاريخ التجديد " + renwalDate;
		} else {
			return "Your Bundle is " + comercialName + " and Renewal Date is " + renwalDate;
		}

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
				String title, subTitle;
				title = subTitle = Constants.EMPTY_STRING;
				String name = Utils.getLabelForViewButton(locale);
				if (categories.contains(categoryObject.get(Constants.JSON_KEY_CATEGORY_ID))) {
					title = jsonUtilService.getCategoryNameValue(categoryObject, locale);
					String payLoad = Constants.PREFIX_RATEPLAN_SUBSCRIPTION + categoryObject.getString(Constants.JSON_KEY_CATEGORY_ID);
					subTitle = Constants.SUBTITLE_VALUE;
					List<Button> buttonsList = new ArrayList<>();
					PostbackButton bundleButton = PostbackButton.create(name, payLoad);
					PostbackButton backButton = PostbackButton.create(Utils.getLabelForBackButton(locale), Constants.PAYLOAD_MAIN_MENU);
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

	public boolean hasRenewalDate(JSONObject bundleDetails) {
		return bundleDetails.get(Constants.JSON_KEY_RENEWAL_DATE) == null;
	}

	public void generictemplateWithJsonObject(String payload, String response, ArrayList<MessagePayload> messagePayloadList, CustomerProfile customerProfile, Messenger messenger,
			BotWebserviceMessage botWebserviceMessage) {
		// Object
		String senderId = customerProfile.getSenderID();
		String userLocale = customerProfile.getLocale();
		Long messageId = botWebserviceMessage.getBotInteractionMessage().getMessageId();
		UserSelection userSelections = utilService.getUserSelectionsFromCache(senderId);
		JSONObject jsonResponse = new JSONObject(response);
		if (payload.equalsIgnoreCase(Constants.PAYLOAD_ACCOUNT_DETAILS)) {
			accountDetailsRouting(messenger, senderId, jsonResponse);
		} else if (payload.equalsIgnoreCase(Constants.EMERALD_TRANSFER_AND_DISTRIBUTE_PRODUCTS_PAYLOAD)) {
			logger.debug(Constants.LOGGER_INFO_PREFIX + "Emerald Parent Trafic Cases for distribute " + response);
			Template gTemplate = emeraldService.getAllEmeraldTraficCases(new JSONObject(response), customerProfile.getLocale());
			MessagePayload  gPayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));
			messagePayloadList.add(gPayload);
		}else if(payload.equalsIgnoreCase(Constants.EMERALD_CHILD_TRAFICCASES_FOR_TRANSFER_PAYLOAD)) {
			logger.debug(Constants.LOGGER_INFO_PREFIX + "Emerald child Trafic cases for transfer " + response);
			Template gTemplate = emeraldService.getAllEmeraldTraficCasesByChildDial(new JSONObject(response), customerProfile,userSelections);
			MessagePayload  gPayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));
			messagePayloadList.add(gPayload);
		} else if (payload.equalsIgnoreCase(Constants.BALANCE_DEDUCTION_AKWAKART)) {
			logger.debug(Constants.LOGGER_INFO_PREFIX + "Balance Deduction get eligible products ");
			Template gTemplate = akwaKartService.checkEligibilityAndReturnProducts(response, userLocale);
			MessagePayload gPayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));
			messagePayloadList.add(gPayload);
		} else {
			JSONArray ratePlan = jsonResponse.getJSONArray(Constants.JSON_KEY_RATEPLAN);
			JSONArray connect = jsonResponse.getJSONArray(Constants.JSON_KEY_MOBILE_INTERNET);
			JSONArray mobileInternetAddonConsumption = jsonResponse.getJSONArray(Constants.JSON_KEY_MOBILE_INTERNET_ADON);
			boolean isPostPaid = jsonResponse.getBoolean("postPaid");
			userSelections.setPostPaid(isPostPaid);
			logger.debug(Constants.LOGGER_INFO_PREFIX + "Profile type postpaid "+isPostPaid);
			utilService.updateUserSelectionsInCache(senderId, userSelections);
			if (connect.length() > 0) {// productId For Renew
				userSelections.setProductIdForRenew(connect.getJSONObject(0).getString(Constants.JSON_KEY_PRODUCT_ID));
				utilService.updateUserSelectionsInCache(senderId, userSelections);
			}
			if (payload.equalsIgnoreCase(Constants.PAYLOAD_CONSUMPTION)) {
				baseConsumptionHandling(messenger, senderId, ratePlan, connect);
			} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_RATEPLAN_DETAILS)) {
				rateplanConsumptionDetails(payload, senderId, messagePayloadList, messageId, botWebserviceMessage, jsonResponse, ratePlan);
			} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_VIEW_CONNECT_DETAILS) && connect.length() == 0) {
				payload = Constants.EMPTY_STRING;
				userSelections.setParentPayLoad(Constants.EMPTY_STRING);
				utilService.updateUserSelectionsInCache(senderId, userSelections);
				interactionHandlingService.handlePayload(Constants.PAYLOAD_CHANGE_BUNDLE, messenger, senderId);
			} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_VIEW_CONNECT_DETAILS) || payload.equals(Constants.PAYLOAD_RATEPLAN_ADDONS_CONSUMPTION)
					|| payload.equals(Constants.PAYLOAD_MOBILEINTERNET_ADDONS_CONSUMPTION)) {
				userSelections.setSubscribed(true);
				ArrayList<String> consumptionNames = new ArrayList<>();
				if (payload.equalsIgnoreCase(Constants.PAYLOAD_RATEPLAN_ADDONS_CONSUMPTION)) {// Check here
					// setConsumptionNamesList(userLocale, ratePlanAddonConsumption,
					// consumptionNames);
				} else if (payload.equals(Constants.PAYLOAD_MOBILEINTERNET_ADDONS_CONSUMPTION)) {
					setConsumptionNamesList(userLocale, mobileInternetAddonConsumption, consumptionNames);
				}
				Template template = createGenericTemplate(messageId, chatBotService, customerProfile, botWebserviceMessage, jsonResponse, consumptionNames, payload);
				MessagePayload mPayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(template));
				messagePayloadList.add(mPayload);
			} else if (payload.equals(Constants.PAYLOAD_RATEPLAN_ACTIONS) || payload.equals(Constants.PAYLOAD_MOBILE_INTERNET_CONTROLLER)) {
				if (payload.equals(Constants.PAYLOAD_MOBILE_INTERNET_CONTROLLER) && jsonResponse.getJSONArray(Constants.JSON_KEY_MOBILE_INTERNET).length() == 0) {
					Template template =  businessErrorService.profileWithoutMobileInternetPackage(userLocale);
					MessagePayload mPayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(template));
					messagePayloadList.add(mPayload);
				}else{
				Template template = createGenericTemplate(messageId, chatBotService, customerProfile, botWebserviceMessage, jsonResponse, null, payload);
				MessagePayload mPayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(template));
				messagePayloadList.add(mPayload);
				}
			}
		}

	}

	/**
	 * @param messenger
	 * @param senderId
	 * @param ratePlan
	 * @param connect
	 */
	public void baseConsumptionHandling(Messenger messenger, String senderId, JSONArray ratePlan, JSONArray connect) {
		if (ratePlan.length() > 0 && connect.length() > 0) {
			interactionHandlingService.handlePayload(Constants.PAYLOAD_RATEPLAN_AND_CONNECT, messenger, senderId);
		} else if (ratePlan.length() > 0 && connect.length() == 0) {
			interactionHandlingService.handlePayload(Constants.PAYLOAD_VIEW_RATEPLAN_DETAILS, messenger, senderId);
		} else if (ratePlan.length() == 0 && connect.length() > 0) {
			interactionHandlingService.handlePayload(Constants.PAYLOAD_VIEW_ROOT_CONNECT_DETAILS, messenger, senderId);
		} else if (ratePlan.length() == 0 && connect.length() == 0) {
			interactionHandlingService.handlePayload(Constants.PAYLOAD_VIEW_MOBILEINTERNET_SUBSCRIPTION, messenger, senderId);
		}
	}

	/**
	 * @param messenger
	 * @param senderId
	 * @param jsonResponse
	 */
	public void accountDetailsRouting(Messenger messenger, String senderId, JSONObject jsonResponse) {
		Boolean postPaid = jsonResponse.getBoolean(Constants.JSON_KEY_POSTPAID);
		if (!postPaid) {
			JSONObject balance = jsonResponse.getJSONObject(Constants.JSON_KEY_BALANCE);
			if (balance == null) {
				interactionHandlingService.handlePayload(Constants.PAYLOAD_FAULT_MSG, messenger, senderId);
			} else {
				interactionHandlingService.handlePayload(Constants.PAYLOAD_PREPAID, messenger, senderId);
			}
		} else {
			interactionHandlingService.handlePayload(Constants.PAYLOAD_POSTPAID_DIAL, messenger, senderId);
		}
	}

	/**
	 * @param payload
	 * @param senderId
	 * @param userLocale
	 * @param messagePayloadList
	 * @param messageId
	 * @param phoneNumber
	 * @param userSelections
	 * @param botWebserviceMessage
	 * @param jsonResponse
	 * @param ratePlan
	 */
	public void rateplanConsumptionDetails(String payload, String senderId, ArrayList<MessagePayload> messagePayloadList, Long messageId, BotWebserviceMessage botWebserviceMessage,
			JSONObject jsonResponse, JSONArray ratePlan) {
		boolean hasNoConsumption = false;
		CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		String userLocale = customerProfile.getLocale();
		if (ratePlan.length() > 0) {
			JSONObject object = ratePlan.getJSONObject(0);
			Set<String> keys = object.keySet();
			for (String key : keys) {
				if (key.equalsIgnoreCase(Constants.JSON_KEY_CONSUMPTION_DETAILS_LIST) && object.getJSONArray(Constants.JSON_KEY_CONSUMPTION_DETAILS_LIST) != null
						&& object.getJSONArray(Constants.JSON_KEY_CONSUMPTION_DETAILS_LIST).length() == 0) {
					hasNoConsumption = true;
				}
			}
			if (!hasNoConsumption) {
				ArrayList<String> cNames = new ArrayList<>();
				JSONArray consumptionDetailsList = ratePlan.getJSONObject(0).getJSONArray(Constants.JSON_KEY_CONSUMPTION_DETAILS_LIST);
				for (int j = 0; j < consumptionDetailsList.length(); j++) {
					cNames.add(jsonUtilService.getNameValue(consumptionDetailsList.getJSONObject(j).getJSONObject("consumed"), userLocale));
				}
				Template template = createGenericTemplate(messageId, chatBotService, customerProfile, botWebserviceMessage, jsonResponse, cNames, payload);
				MessagePayload mPayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(template));
				messagePayloadList.add(mPayload);
			}
		}
	}

	/**
	 * @param userLocale
	 * @param connect
	 * @param consumptionNames
	 */
	public void setConsumptionNamesList(String userLocale, JSONArray consumption, ArrayList<String> consumptionNames) {

		for (int i = 0; i < consumption.length(); i++) {
			JSONArray consumptionDetailsList = consumption.getJSONObject(i).getJSONArray(Constants.JSON_KEY_CONSUMPTION_DETAILS_LIST); // Amin
			String baseConsumptionName = getAddonCommercialName(userLocale, consumption, i);
			for (int c = 0; c < consumptionDetailsList.length(); c++) {
				if (consumptionDetailsList.getJSONObject(c).get(Constants.JSON_KEY_CONSUMPTION_NAME) != null) {
					String consumptionName = jsonUtilService.getConsumptionNameValue(consumptionDetailsList.getJSONObject(c).getJSONObject(Constants.JSON_KEY_CONSUMPTION_NAME), userLocale);
					consumptionNames.add(baseConsumptionName + consumptionName);
				}
			}
		}
	}

	/**
	 * @param userLocale
	 */
	public String getAddonCommercialName(String userLocale, JSONArray consumption, int i) {
		return jsonUtilService.getArabicOrEnglishValue(consumption.getJSONObject(i).getJSONObject(Constants.JSON_KEY_COMMERCIAL_NAME), userLocale) + " - ";
	}

	public void genericTemplateWithJsonِArray(String payload, String response, ArrayList<MessagePayload> messagePayloadList, CustomerProfile customerProfile) {

		// Array
		String senderId = customerProfile.getSenderID();
		String userLocale = customerProfile.getLocale();
		String phoneNumber = customerProfile.getMsisdn();
		UserSelection userSelections = utilService.getUserSelectionsFromCache(senderId);
		if (payload.equalsIgnoreCase(Constants.PALOAD_SALLEFNY)) {
			JSONArray products = new JSONArray(response);
			GenericTemplate gTemplate = sallefnyService.sallefnyHandling(products, userLocale);
			MessagePayload messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));
			messagePayloadList.add(messagePayload);
		} else if (payload.contains(Constants.PAYLOAD_MIGRATE)) {
			Template gTemplate = migrationService.migrationHandling(payload, phoneNumber, userLocale, new JSONArray(response));
			MessagePayload gPayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));
			messagePayloadList.add(gPayload);
		} else if (payload.equalsIgnoreCase(Constants.BALANCE_DEDUCTION_AKWAKART)) {
			Template gTemplate = akwaKartService.checkEligibilityAndReturnProducts(response, userLocale);
			MessagePayload gPayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));
			messagePayloadList.add(gPayload);
		} else {
			customerEligibilityHandling(payload, senderId, userLocale, messagePayloadList, phoneNumber, userSelections, response);
		}

	}

	/**
	 * @param payload
	 * @param senderId
	 * @param userLocale
	 * @param messagePayloadList
	 * @param phoneNumber
	 * @param userSelections
	 * @param response
	 */
	public void customerEligibilityHandling(String payload, String senderId, String userLocale, ArrayList<MessagePayload> messagePayloadList, String phoneNumber, UserSelection userSelections,
			String response) {
		// Array
		if (payload.startsWith(Constants.PREFIX_RATEPLAN_SUBSCRIPTION)) {
			JSONArray bundleArray = new JSONArray(response);
			if (bundleArray.length() > 0) {// productIdAndOperationName
				messagePayloadList.add(utilService.getProductsFromJsonByCategory(bundleArray, userSelections.getProductIdAndOperationName(), senderId, userLocale));
			}
		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_RELATED_PRODUCTS)) {
			JSONArray bundleArray = new JSONArray(response);
			if (bundleArray.length() > 0) {
				String productId = userSelections.getParametersListForRelatedProducts().split(Constants.COMMA_CHAR)[0];
				messagePayloadList.add(utilService.getRelatedProductFromJsonByBundleId(bundleArray, productId, senderId, userLocale));
			}
		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_CHANGE_BUNDLE)) {
			JSONArray bundleArray = new JSONArray(response);
			if (bundleArray.length() > 0) {
				messagePayloadList.add(getBundleCategories(bundleArray, senderId, chatBotService, userLocale, phoneNumber));
			} else {// addition
				GenericTemplate gtemplate = createGenericTemplateForNotEligiblBundleDials(userLocale, new ArrayList<Button>(), new ArrayList<Element>(), phoneNumber);
				messagePayloadList.add(MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gtemplate)));
			}
		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_BUY_ADDONS_ROOT)) {
			JSONArray categoryArray = new JSONArray(response);
			if (categoryArray.length() > 0) {
				messagePayloadList.add(utilService.getCategoryForMobileInternetAddons(categoryArray, senderId, userLocale));
			}
		} else if (payload.equalsIgnoreCase(Constants.PAYLOAD_BUY_ADDONS)) {
			JSONArray categoryArray = new JSONArray(response);
			if (categoryArray.length() > 0) {
				messagePayloadList.add(utilService.getExtraMobileInternetAddonsByCategory(categoryArray, senderId, userLocale, userSelections.getAddonId()));
			}
		}
	}

}
