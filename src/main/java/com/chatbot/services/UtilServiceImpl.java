package com.chatbot.services;

import static java.util.Optional.empty;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.cache.CacheResponseStatus;
import org.apache.http.client.cache.HttpCacheContext;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.cache.CachingHttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.stereotype.Service;

import com.chatbot.entity.BotButton;
import com.chatbot.entity.BotButtonTemplateMSG;
import com.chatbot.entity.BotGTemplateMessage;
import com.chatbot.entity.BotInteraction;
import com.chatbot.entity.BotInteractionMessage;
import com.chatbot.entity.BotQuickReplyMessage;
import com.chatbot.entity.BotTemplateElement;
import com.chatbot.entity.BotTextMessage;
import com.chatbot.entity.BotTextResponseMapping;
import com.chatbot.entity.BotWebserviceMessage;
import com.chatbot.entity.CustomerProfile;
import com.chatbot.entity.SubscribeWSBody;
import com.chatbot.util.Utils;
import com.github.messenger4j.Messenger;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerIOException;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.MessagingType;
import com.github.messenger4j.send.message.TemplateMessage;
import com.github.messenger4j.send.message.TextMessage;
import com.github.messenger4j.send.message.quickreply.QuickReply;
import com.github.messenger4j.send.message.quickreply.TextQuickReply;
import com.github.messenger4j.send.message.template.ButtonTemplate;
import com.github.messenger4j.send.message.template.GenericTemplate;
import com.github.messenger4j.send.message.template.Template;
import com.github.messenger4j.send.message.template.button.Button;
import com.github.messenger4j.send.message.template.button.LogInButton;
import com.github.messenger4j.send.message.template.button.LogOutButton;
import com.github.messenger4j.send.message.template.button.PostbackButton;
import com.github.messenger4j.send.message.template.button.UrlButton;
import com.github.messenger4j.send.message.template.common.Element;
import com.github.messenger4j.userprofile.UserProfile;
import org.apache.http.impl.client.cache.CacheConfig;
import aj.org.objectweb.asm.Label;
import sun.misc.BASE64Decoder;

@Service
public class UtilServiceImpl implements UtilsService {

	// IN Case Zero Level JSONObject
	@Override
	public Map<String, ArrayList<String>> inCaseZeroLevelJsonObject(String[] keys, JSONObject jsonObject, String msg,
			String locale) {
		Map<String, ArrayList<String>> mapValues = new HashMap<String, ArrayList<String>>();
		ArrayList<String> titleList = new ArrayList<String>();
		ArrayList<String> values = new ArrayList<String>();
		ArrayList<String> textMsgs = new ArrayList<String>();
		values = getValuesFromJson(jsonObject, keys);
		// replace message missed values
		String finalMsg = replaceValuesByMapping(values, msg, locale);
		textMsgs.add(finalMsg);
		titleList.add(values.get(0));
		textMsgs.add(finalMsg);
		mapValues.put("title", titleList);
		textMsgs.add(finalMsg);
		mapValues.put("msg", textMsgs);
		// return textMsgs;
		return mapValues;

	}

	// IN Case Zero Level JSONArray
	@Override
	public Map<String, ArrayList<String>> inCaseZeroLevelJsonArray(String[] keys, JSONArray rootArray, String msg,
			String locale) {

		Map<String, ArrayList<String>> mapValues = new HashMap<String, ArrayList<String>>();
		ArrayList<String> titleList = new ArrayList<String>();
		ArrayList<String> values = new ArrayList<String>();
		ArrayList<String> textMsgs = new ArrayList<String>();
		for (int i = 0; i < rootArray.length(); i++) {

			try {
				JSONObject childObject = rootArray.getJSONObject(i);
				values = getValuesFromJson(childObject, keys);
				titleList.add(values.get(0));
				mapValues.put("title", titleList);
				mapValues.put("msg", textMsgs);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// replace message missed values
			String finalMsg = replaceValuesByMapping(values, msg, locale);
			textMsgs.add(finalMsg);
			titleList.add(values.get(0));
			textMsgs.add(finalMsg);
			mapValues.put("title", titleList);
			textMsgs.add(finalMsg);
			mapValues.put("msg", textMsgs);
		}
		// return textMsgs;

		return mapValues;
	}

	// IN Case ONE Level JSONObject
	@Override
	public Map<String, ArrayList<String>> inCaseOneLevelJsonObject(String[] paths, String[] keys, JSONObject jsonObject,
			String msg, String locale) {
		Map<String, ArrayList<String>> mapValues = new HashMap<String, ArrayList<String>>();
		ArrayList<String> titleList = new ArrayList<String>();
		ArrayList<String> values = new ArrayList<String>();
		ArrayList<String> textMsgs = new ArrayList<String>();
		JSONArray firstLevelArray = new JSONArray();
		JSONObject firstLevelObject = new JSONObject();

		if (paths[0].startsWith("#")) {
			paths[0] = paths[0].substring(1, paths[0].length());

			try {
				firstLevelArray = jsonObject.getJSONArray(paths[0]);
			} catch (JSONException e) {

			}
			for (int i = 0; i < firstLevelArray.length(); i++) {
				try {
					firstLevelObject = firstLevelArray.getJSONObject(i);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				values = getValuesFromJson(firstLevelObject, keys);
				// replace message missed values
				String finalMsg = replaceValuesByMapping(values, msg, locale);
				titleList.add(values.get(0));
				textMsgs.add(finalMsg);
				mapValues.put("title", titleList);
				textMsgs.add(finalMsg);
				mapValues.put("msg", textMsgs);
			}
		} else if (!paths[0].startsWith("#")) {
			try {
				firstLevelObject = jsonObject.getJSONObject(paths[0]);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			values = getValuesFromJson(firstLevelObject, keys);
			// replace message missed values
			String finalMsg = replaceValuesByMapping(values, msg, locale);
			titleList.add(values.get(0));
			textMsgs.add(finalMsg);
			mapValues.put("title", titleList);
			// textMsgs.add(finalMsg);
			mapValues.put("msg", textMsgs);
		}
		// return textMsgs;
		return mapValues;
	}

	// IN Case ONE Level JSONOArray
	@Override
	public Map<String, ArrayList<String>> inCaseOneLevelJsonArrayForTextMessage(String[] paths, String[] keys,
			JSONArray rootArray, String msg, String locale) {
		ArrayList<String> values = new ArrayList<String>();
		Map<String, ArrayList<String>> mapValuse = new HashMap<String, ArrayList<String>>();
		ArrayList<String> textMsgs = new ArrayList<String>();
		ArrayList<String> titleList = new ArrayList<String>();
		Map<String, ArrayList<String>> mapValues = new HashMap<String, ArrayList<String>>();
		for (int i = 0; i < rootArray.length(); i++) {
			try {
				JSONObject childObject = rootArray.getJSONObject(i);
				if (paths[0].startsWith("#")) {
					JSONArray childArray = childObject.getJSONArray(paths[0].substring(1, paths[0].length()));
					for (int j = 0; j < childArray.length(); j++) {
						JSONObject subChild = childArray.getJSONObject(0);
						values = getValuesFromJson(subChild, keys);
						// replace message missed values
						String finalMsg = replaceValuesByMapping(values, msg, locale);
						titleList.add(values.get(0));
						mapValuse.put("title", titleList);
						textMsgs.add(finalMsg);
						mapValuse.put("msg", textMsgs);
						mapValues.put("title", titleList);
						textMsgs.add(finalMsg);
						mapValues.put("msg", textMsgs);

					}
				} else if (!paths[0].startsWith("#")) {
					JSONObject child = childObject.getJSONObject(paths[0]);
					values = getValuesFromJson(child, keys);
					titleList.add(values.get(0));
					// replace message missed values
					String finalMsg = replaceValuesByMapping(values, msg, locale);
					textMsgs.add(finalMsg);
					mapValuse.put("title", titleList);
					textMsgs.add(finalMsg);
					mapValuse.put("msg", textMsgs);
					mapValues.put("title", titleList);
					textMsgs.add(finalMsg);
					mapValues.put("msg", textMsgs);
				}
			} catch (Exception e) {
				System.out.println(e.getStackTrace());
			}
		}

		// return textMsgs;
		return mapValues;
	}

	// Tested and works well get values from json object by its keys
	@Override
	public ArrayList<String> getValuesFromJson(JSONObject jsonObject, String[] keys) {
		ArrayList<String> values = new ArrayList<String>();
		for (int i = 0; i < keys.length; i++) {
			String key = keys[i];
			String value = "";
			try {
				if (key.contains(".")) {
					String jsonKey = key.substring(0, key.indexOf("."));
					String valueKey = key.substring(key.indexOf(".") + 1, key.length());
					JSONObject finalObject = jsonObject.getJSONObject(jsonKey);
					value = finalObject.getString(valueKey);

				} else {
					value = jsonObject.getString(key);
				}
			} catch (JSONException e) {
				value = "_";
			}
			values.add(value);
		}
		return values;
	}

	// Tested works well replace ? in String by its new values
	@Override
	public String replaceValuesByMapping(ArrayList<String> values, String msg, String locale) {

		String messages = "";
		String finalMsg = "";
		for (int i = 0; i < values.size(); i++) {
			String flag = "";
			if (locale.equals("ar")) {
				flag = i + "?";
			} else {
				flag = i + "?";
			}
			if (i == 0) {
				finalMsg = msg.replace(flag, values.get(i));
			} else {
				finalMsg = finalMsg.replace(flag, values.get(i));
			}
		}
		return finalMsg;

	}

	// In case Two Level JSONObject
	@Override
	public Map<String, ArrayList<String>> inCaseTwoLevelJsonObject(JSONObject jsonObject, String[] paths, String[] keys,
			String msg, String locale) {
		ArrayList<String> values = new ArrayList<String>();
		Map<String, ArrayList<String>> mapValues = new HashMap<String, ArrayList<String>>();
		ArrayList<String> titleList = new ArrayList<String>();
		ArrayList<String> textMsgs = new ArrayList<String>();
		ArrayList<String> percentageList = new ArrayList<String>();
		JSONObject firstLevelObject = new JSONObject();
		JSONArray firstLevelArray = new JSONArray();
		String path = paths[0];
		if (path.startsWith("#")) {
			try {
				path = path.substring(1, path.length());
				firstLevelArray = jsonObject.getJSONArray(path);
				System.out.println(firstLevelArray);
				for (int i = 0; i < firstLevelArray.length(); i++) {
					firstLevelObject = firstLevelArray.getJSONObject(i);
					if (paths[1].startsWith("#")) {
						String finalPath = paths[1].substring(1, paths[1].length());
						JSONArray finalJsonArray = firstLevelObject.getJSONArray(finalPath);
						for (int j = 0; j < finalJsonArray.length(); j++) {
							JSONObject finalJsonObject = finalJsonArray.getJSONObject(j);
							// retrieve values from json
							double consumed, total, percentage = 0;
							values = getValuesFromJson(finalJsonObject, keys);
							titleList.add(values.get(0));
							values.remove(0);
							consumed = Double.parseDouble(values.get(0));
							if (consumed == 0.0) {
								percentage = 0;
							} else {
								total = Double.parseDouble(values.get(2));
								percentage = (consumed / total) * 100;
							}
							percentageList.add(String.valueOf(percentage));
							// replace message missed values
							String finalMsg = replaceValuesByMapping(values, msg, locale);
							textMsgs.add(finalMsg);
						}
						mapValues.put("msg", textMsgs);
						mapValues.put("title", titleList);
						mapValues.put("percentage", percentageList);
					} else if (!paths[1].startsWith("#")) {
						JSONObject firstParent = firstLevelObject.getJSONObject(paths[1]);
						values = getValuesFromJson(firstParent, keys);
						// replace message missed values
						titleList.add(values.get(0));
						mapValues.put("title", titleList);
						String finalMsg = replaceValuesByMapping(values, msg, locale);
						textMsgs.add(finalMsg);
						mapValues.put("msg", values);
					}
				}
			} catch (Exception e) {
				System.out.println(e.getStackTrace());
			}
			// TODO: handle exception
		} else if (!path.startsWith("#")) {
			try {
				firstLevelObject = jsonObject.getJSONObject(path);
				if (paths[1].startsWith("#")) {
					JSONArray array = firstLevelObject.getJSONArray(paths[1].substring(1, paths[1].length()));
					for (int i = 0; i < array.length(); i++) {
						JSONObject tempObject = array.getJSONObject(i);
						values = getValuesFromJson(tempObject, keys);
						titleList.add(values.get(0));
						// replace message missed values
						String finalMsg = replaceValuesByMapping(values, msg, locale);
						textMsgs.add(finalMsg);
						textMsgs.add(finalMsg);
						textMsgs.add(finalMsg);
					}
					mapValues.put("msg", textMsgs);
					mapValues.put("title", titleList);
				} else if (!paths[1].startsWith("#")) {
					JSONObject lastObject = firstLevelObject.getJSONObject(paths[1]);
					values = getValuesFromJson(lastObject, keys);
					// replace message missed values
					String finalMsg = replaceValuesByMapping(values, msg, locale);
					textMsgs.add(finalMsg);
					titleList.add(values.get(0));
					textMsgs.add(finalMsg);
					mapValues.put("title", titleList);
					textMsgs.add(finalMsg);
					mapValues.put("msg", textMsgs);
				}
			} catch (Exception e) {
				System.out.println(e.getStackTrace());
			}
		}
		return mapValues;

	}

	// In case Two Level JSONArray
	@Override
	public Map<String, ArrayList<String>> inCaseTwoLevelJsonArrayForTextMessage(JSONArray rootArray, String[] paths,
			String[] keys, String msg, String locale) {
		ArrayList<String> titleList = new ArrayList<String>();
		Map<String, ArrayList<String>> mapValues = new HashMap<String, ArrayList<String>>();
		ArrayList<String> values = new ArrayList<String>();
		ArrayList<String> textMsgs = new ArrayList<String>();
		for (int i = 0; i < rootArray.length(); i++) {
			try {
				JSONObject firstLevel = rootArray.getJSONObject(i);
				if (paths[0].startsWith("#")) {
					JSONArray subArray = firstLevel.getJSONArray(paths[0].substring(1, paths[0].length()));
					for (int j = 0; j < subArray.length(); j++) {
						JSONObject subObject = subArray.getJSONObject(j);
						if (paths[1].startsWith("#")) {
							JSONArray subChildArray = subObject.getJSONArray(paths[1].substring(1, paths[1].length()));
							for (int k = 0; k < subChildArray.length(); k++) {
								JSONObject subChildObject = subChildArray.getJSONObject(k);
								values = getValuesFromJson(subChildObject, keys);
								// replace message missed values
								String finalMsg = replaceValuesByMapping(values, msg, locale);
								textMsgs.add(finalMsg);
							}
						} else if (!paths[1].startsWith("#")) {
							JSONObject childObject = subObject.getJSONObject(paths[1]);
							values = getValuesFromJson(childObject, keys);
							// replace message missed values
							String finalMsg = replaceValuesByMapping(values, msg, locale);
							textMsgs.add(finalMsg);
							titleList.add(values.get(0));
							textMsgs.add(finalMsg);
							mapValues.put("title", titleList);
							textMsgs.add(finalMsg);
							mapValues.put("msg", textMsgs);
						}
					}
				} else if (!paths[0].startsWith("#")) {
					JSONObject firstLevelObject = firstLevel.getJSONObject(paths[0]);
					if (paths[1].startsWith("#")) {
						JSONArray secondLevelArray = firstLevelObject
								.getJSONArray(paths[1].substring(1, paths[1].length()));
						for (int j = 0; j < secondLevelArray.length(); j++) {
							JSONObject subChildObject = secondLevelArray.getJSONObject(j);
							values = getValuesFromJson(subChildObject, keys);
							// replace message missed values
							String finalMsg = replaceValuesByMapping(values, msg, locale);
							textMsgs.add(finalMsg);
							titleList.add(values.get(0));
							textMsgs.add(finalMsg);
							mapValues.put("title", titleList);
							textMsgs.add(finalMsg);
							mapValues.put("msg", textMsgs);
						}
					} else if (!paths[1].startsWith("#")) {
						JSONObject secondlevelObject = firstLevelObject.getJSONObject(paths[1]);
						values = getValuesFromJson(secondlevelObject, keys);
						// replace message missed values
						String finalMsg = replaceValuesByMapping(values, msg, locale);
						textMsgs.add(finalMsg);
						titleList.add(values.get(0));
						textMsgs.add(finalMsg);
						mapValues.put("title", titleList);
						textMsgs.add(finalMsg);
						mapValues.put("msg", textMsgs);
					}

				}
			} catch (Exception e) {
				System.out.println(e.getStackTrace());
			}
		}

		// return textMsgs;
		return mapValues;
	}

	// Method to get Path Details
	@Override
	public String[] getPaths(String path) {
		String[] paths = new String[0];
		if (path.contains(",")) {
			paths = path.split(",");
		} else if (path.length() > 1 && !path.contains(",")) {
			paths = new String[] { path };
		}
		return paths;
	}

	// Method to get keys
	@Override
	public String[] getKeys(String key) {
		String[] keys = new String[0];
		if (key.contains(",")) {
			keys = key.split(",");
		} else if (!key.contains(",")) {
			keys = new String[] { key };
		}
		return keys;
	}

	// Switcher for JSONObject
	@Override
	public Map<String, ArrayList<String>> switchToObjectMode(JSONObject jsonResponse, String[] paths, String[] keys,
			String msg, String locale) {
		ArrayList<String> values = new ArrayList<String>();
		Map<String, ArrayList<String>> mapValues = new HashMap<String, ArrayList<String>>();
		int length = paths.length;
		if (length == 0) {
			mapValues = inCaseZeroLevelJsonObject(keys, jsonResponse, msg, locale);
		} else if (length == 1) {
			mapValues = inCaseOneLevelJsonObject(paths, keys, jsonResponse, msg, locale);
		} else if (length == 2) {
			mapValues = inCaseTwoLevelJsonObject(jsonResponse, paths, keys, msg, locale);
		}
		return mapValues;
	}

	// Switcher for JSONArray
	@Override
	public Map<String, ArrayList<String>> switchToArrayMode(JSONArray jsonResponse, String[] paths, String[] keys,
			String msg, String locale) {
		Map<String, ArrayList<String>> mapValues = new HashMap<String, ArrayList<String>>();
		int length = paths.length;
		if (length == 0) {
			mapValues = inCaseZeroLevelJsonArray(keys, jsonResponse, msg, locale);
		} else if (length == 1) {
			mapValues = inCaseOneLevelJsonArrayForTextMessage(paths, keys, jsonResponse, msg, locale);
		} else if (length == 2) {
			mapValues = inCaseTwoLevelJsonArrayForTextMessage(jsonResponse, paths, keys, msg, locale);
		}
		return mapValues;
	}

	// Create Button
	@Override
	public Button createButton(BotButton botButton, String locale, JSONObject jsonObject, String dialNumber) {
		// PostBack
		if (botButton.getButtonType().getId() == Utils.ButtonTypeEnum.POSTBACK.getValue()) {
			String payload = botButton.getButtonPayload();
			String lable = Utils.getTextValueForButtonLabel(locale, botButton);
			if (payload.contains(".") && lable.contains(".")) {
				String finalPayLoad = getValuesFromJson(jsonObject, new String[] { payload }).get(0);
				String finalText = getValuesFromJson(jsonObject, new String[] { lable }).get(0);
				if (finalPayLoad.contains("_")) {
					finalPayLoad = "sub_" + finalPayLoad;
				}
				return PostbackButton.create(finalText, finalPayLoad);
			}
			return PostbackButton.create(Utils.getTextValueForButtonLabel(locale, botButton),
					botButton.getButtonPayload());
			// URl
		} else if (botButton.getButtonType().getId() == Utils.ButtonTypeEnum.URL.getValue()) {
			URL url = Utils.createUrl(botButton.getButtonUrl());
			if (locale.contains("ar")) {
				try {
					if (botButton.getBotText().getArabicText().equals("اشحن الان")) {
						String par = Utils
								.encryptDPIParam("time=1498033943505" + "&user=" + dialNumber + "&URL=facebook");
						String stringUrl = url + "ar&param=" + par;
						URL newUrl = Utils.createUrl(stringUrl);
						return UrlButton.create(Utils.getTextValueForButtonLabel(locale, botButton), newUrl);

					} else if (botButton.getBotText().getArabicText().equals("ادفع فاتورتك")) {
						String par = Utils
								.encryptDPIParam("time=1498033943505" + "&user=" + dialNumber + "&URL=facebook");
						String stringUrl = par;
						URL newUrl = Utils.createUrl(stringUrl);
						return UrlButton.create(Utils.getTextValueForButtonLabel(locale, botButton), newUrl);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				try {
					if (botButton.getBotText().getEnglishText().equalsIgnoreCase("Recharge Now")) {
						String par = Utils
								.encryptDPIParam("time=1498033943505" + "&user=" + dialNumber + "&URL=facebook");
						String stringUrl = url + "en&param=" + par;
						URL newUrl = Utils.createUrl(stringUrl);
						return UrlButton.create(Utils.getTextValueForButtonLabel(locale, botButton), newUrl);

					} else if (botButton.getBotText().getEnglishText().equalsIgnoreCase("Pay Now")) {
						String par = Utils
								.encryptDPIParam("time=1498033943505" + "&user=" + dialNumber + "&URL=facebook");
						String stringUrl = url + par;
						URL newUrl = Utils.createUrl(stringUrl);
						return UrlButton.create(Utils.getTextValueForButtonLabel(locale, botButton), newUrl);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return UrlButton.create(Utils.getTextValueForButtonLabel(locale, botButton), url);
			// Login
		} else if (botButton.getButtonType().getId() == Utils.ButtonTypeEnum.LOGIN.getValue()) {
			URL url = Utils.createUrl(botButton.getButtonUrl());
			return LogInButton.create(url);
			// Logout
		} else if (botButton.getButtonType().getId() == Utils.ButtonTypeEnum.LOGOUT.getValue()) {
			return LogOutButton.create();
		}
		return null;
	}

	/**
	 * @param messageId
	 * @param chatBotService
	 * @return
	 */
	@Override
	public Template createGenericTemplate(Long messageId, ChatBotService chatBotService, String userlocale,
			BotWebserviceMessage botWebserviceMessage, JSONObject jsonObject, String dialNumber,
			ArrayList<String> consumptionNames) {
		BotGTemplateMessage botGTemplateMessage = chatBotService.findGTemplateMessageByMessageId(messageId);
		List<BotTemplateElement> botTemplateElementList = chatBotService
				.findTemplateElementsByGTMsgId(botGTemplateMessage.getGTMsgId());
		List<Element> elements = new ArrayList<>();
		for (BotTemplateElement botTemplateElement : botTemplateElementList) {
			List<BotButton> elementButtonList = chatBotService
					.findButtonsByTemplateElementId(botTemplateElement.getElementId());
			List<Button> buttonsList = new ArrayList<>();
			for (BotButton botButton : elementButtonList) {
				Button button = createButton(botButton, userlocale, new JSONObject(), dialNumber);
				buttonsList.add(button);
			}

			String elemnetImageUrl = "";
			Element element = null;
			elemnetImageUrl = botTemplateElement.getImageUrl();
			String title = getTitletBotTemplateElement(botTemplateElement, userlocale);
			String subtitle = getSubTitletBotTemplateElement(botTemplateElement, userlocale);
			String msg = "";
			if (subtitle.contains("?") && botWebserviceMessage != null) {
				Long wsId = botWebserviceMessage.getWsMsgId();
				BotTextResponseMapping botTextResponseMapping = chatBotService.findTextResponseMappingByWsId(wsId)
						.get(0);
				String[] keys = getKeysString(botTextResponseMapping, userlocale).split(",");
				String[] paths = getPaths(botTextResponseMapping.getCommonPath());
				Map<String, ArrayList<String>> mapValues = switchToObjectMode(jsonObject, paths, keys, subtitle,
						userlocale);
				ArrayList<String> values = mapValues.get("msg");
				ArrayList<String> titles = mapValues.get("title");
				ArrayList<String> percentageList = mapValues.get("percentage");
				if (values == null || values.size() == 0) {
					elemnetImageUrl = botTemplateElement.getImageUrl() + "warning.png?version=";
					if (userlocale.contains("ar")) {
						msg = "ناسف أنت غير مشترك بأي باقة";
					} else {
						msg = "Your Bundle does not has consumption";
					}
					element = createElement(buttonsList, elemnetImageUrl, "RatePlan", msg);
					elements.add(element);
				} else {
					for (int i = 0; i < values.size(); i++) {
						title = titles.get(i);
						if (title.equalsIgnoreCase("Mobile Internet") || title.equalsIgnoreCase("موبايل انترنت")) {
							title = consumptionNames.get(i);
						}
						String perc = String.valueOf(percentageList.get(i));
						if (perc.contains(".")) {
							perc = perc.substring(0, perc.indexOf("."));
						}
						elemnetImageUrl = botTemplateElement.getImageUrl() + perc + ".png?version=1";
						element = createElement(buttonsList, elemnetImageUrl, title, values.get(i));
						elements.add(element);
					}
				}
			} else {
				if (elemnetImageUrl.length() > 1) {
					URL imageUrl = Utils.createUrl(elemnetImageUrl);
					element = Element.create(title, Optional.of(subtitle), Optional.of(imageUrl), empty(),
							Optional.of(buttonsList));
				} else {
					element = Element.create(title, Optional.of(botTemplateElement.getSubTitle().getEnglishText()),
							empty(), empty(), Optional.of(buttonsList));
				}
				elements.add(element);
			}
		}
		return GenericTemplate.create(elements);
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

	@Override
	public String getTitletBotTemplateElement(BotTemplateElement botTemplateElement, String local) {
		String title = "";
		if (local.equalsIgnoreCase("ar")) {
			title = botTemplateElement.getTitle().getArabicText();
		} else {
			title = botTemplateElement.getTitle().getEnglishText();
		}
		return title;
	}

	@Override
	public String getSubTitletBotTemplateElement(BotTemplateElement botTemplateElement, String local) {
		String title = "";
		if (local.equalsIgnoreCase("ar")) {
			title = botTemplateElement.getSubTitle().getArabicText();
		} else {
			title = botTemplateElement.getSubTitle().getEnglishText();
		}
		return title;
	}

	/**
	 * @param botInteractionMessage
	 * 
	 * @param chatBotService
	 * @return
	 */
	@Override
	public ButtonTemplate createButtonTemplateInScenario(BotInteractionMessage botInteractionMessage,
			ChatBotService chatBotService, String local, String dialNumber) {
		String title = "";
		ArrayList<Button> buttons = new ArrayList<Button>();
		List<BotButtonTemplateMSG> botButtonTemplateMSGs = chatBotService
				.findBotButtonTemplateMSGByBotInteractionMessage(botInteractionMessage);
		for (BotButtonTemplateMSG botButtonTemplateMSG : botButtonTemplateMSGs) {
			title = getTextForButtonTemplate(local, botButtonTemplateMSG);
			List<BotButton> botButtons = chatBotService.findAllByBotButtonTemplateMSGId(botButtonTemplateMSG);
			for (BotButton botButton : botButtons) {
				Button button = createButton(botButton, local, new JSONObject(), dialNumber);
				buttons.add(button);
			}
		}
		return ButtonTemplate.create(title, buttons);

	}

	@Override
	public String getTextForButtonTemplate(String local, BotButtonTemplateMSG botButtonTemplateMSG) {
		String text = "";
		if (local.equalsIgnoreCase("ar")) {
			text = botButtonTemplateMSG.getBotText().getArabicText();
		} else {
			text = botButtonTemplateMSG.getBotText().getEnglishText();
		}
		return text;
	}

	@Override
	public List<QuickReply> createQuickReply(ChatBotService chatBotService, Long messageId, String local) {
		BotQuickReplyMessage botQuickReplyMessage = chatBotService.findQuickReplyMessageByMessageId(messageId);
		List<QuickReply> quickReplies = new ArrayList<>();
		List<BotButton> quickReplyButtonList = chatBotService
				.findButtonsByQuickReplyMessageId(botQuickReplyMessage.getQuickMsgId());
		QuickReply quickReply = null;
		for (BotButton botButton : quickReplyButtonList) {
			String label = Utils.getTextValueForButtonLabel(local, botButton);
			if (botButton.getButtonImageUrl() != null) {
				if (botButton.getButtonImageUrl().length() > 0) {
					URL url = Utils.createUrl(botButton.getButtonImageUrl());

					quickReply = TextQuickReply.create(label, botButton.getButtonPayload(), Optional.of(url));
				} else {
					quickReply = TextQuickReply.create(label, botButton.getButtonPayload(), empty());
				}
			} else {
				quickReply = TextQuickReply.create(label, botButton.getButtonPayload());
			}
			quickReplies.add(quickReply);
		}
		return quickReplies;
	}

	// This Method For call Parent MessagePayLoad again
	@Override
	public MessagePayload createMessagePayload(String parentPayLoad, ChatBotService chatBotService, String senderId,
			String userLocale, BotWebserviceMessage botWebserviceMessage, String dialNumber) {
		MessagePayload messagePayload = null;
		if (parentPayLoad != null) {
			BotInteraction parentBotInteraction = chatBotService.findInteractionByPayload(parentPayLoad);
			List<BotInteractionMessage> parentMsgs = chatBotService
					.findInteractionMessagesByInteractionId(parentBotInteraction.getInteractionId());
			for (BotInteractionMessage interactionMsg : parentMsgs) {
				if (interactionMsg.getBotMessageType()
						.getMessageTypeId() == Utils.MessageTypeEnum.GENERICTEMPLATEMESSAGE.getValue()) {
					Template template = createGenericTemplate(interactionMsg.getMessageId(), chatBotService, userLocale,
							botWebserviceMessage, new JSONObject(), dialNumber, new ArrayList<String>());
					messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE,
							TemplateMessage.create(template));
				}
			}
		}
		return messagePayload;

	}

	/**
	 * @param senderId
	 * @param messagePayloadList
	 * @param botWebserviceMessage
	 * @param jsonBodyString
	 * @throws JSONException
	 */
	@Override
	public void getTextMessageIfResponseIsArray(String senderId, ArrayList<MessagePayload> messagePayloadList,
			BotWebserviceMessage botWebserviceMessage, String jsonBodyString, ChatBotService chatBotService,
			String local) throws JSONException {
		MessagePayload messagePayload;
		List<BotTextResponseMapping> botTextResponseMappings = chatBotService
				.findTextResponseMappingByWsId(botWebserviceMessage.getWsMsgId());
		JSONArray rootArray = new JSONArray(jsonBodyString);
		for (BotTextResponseMapping botTextResponseMapping : botTextResponseMappings) {
			String msg = getTextForBotTextResponseMapping(local, botTextResponseMapping);
			String path = botTextResponseMapping.getCommonPath();
			String[] paths = getPaths(path);
			String keys = getKeysString(botTextResponseMapping, local);

			String[] keysArray = getKeys(keys);
			ArrayList<String> values = switchToArrayMode(rootArray, paths, keysArray, msg, local).get(msg);
			for (String val : values) {
				System.out.println(val);
				messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(val));
				messagePayloadList.add(messagePayload);
			}
		}
	}

	@Override
	public String getTextForBotTextResponseMapping(String local, BotTextResponseMapping botTextResponseMapping) {
		String text = "";
		if (local.equalsIgnoreCase("ar")) {
			text = botTextResponseMapping.getBotText().getArabicText();
		} else {
			text = botTextResponseMapping.getBotText().getEnglishText();
		}
		return text;
	}

	/**
	 * @param senderId
	 * @param messagePayloadList
	 * @param botWebserviceMessage
	 * @param jsonBodyString
	 * @throws JSONException
	 */
	@Override
	public void getTextMessageIfResponseIsObject(String senderId, ArrayList<MessagePayload> messagePayloadList,
			BotWebserviceMessage botWebserviceMessage, String jsonBodyString, ChatBotService chatBotService,
			String local) throws JSONException {
		MessagePayload messagePayload;
		List<BotTextResponseMapping> botTextResponseMappings = chatBotService
				.findTextResponseMappingByWsId(botWebserviceMessage.getWsMsgId());
		JSONObject rootObject = new JSONObject(jsonBodyString);
		for (BotTextResponseMapping botTextResponseMapping : botTextResponseMappings) {
			String msg = getTextForBotTextResponseMapping(local, botTextResponseMapping);
			String path = botTextResponseMapping.getCommonPath();
			String[] paths = getPaths(path);
			String keys = "";
			if (local.equalsIgnoreCase("ar")) {
				keys = botTextResponseMapping.getArParams();
			} else {
				keys = botTextResponseMapping.getEnParams();
			}

			String[] keysArray = getKeys(keys);
			ArrayList<String> values = switchToObjectMode(rootObject, paths, keysArray, msg, local).get("msg");
			for (String val : values) {
				messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(val));
				messagePayloadList.add(messagePayload);
			}
		}
	}

	@Override
	public MessagePayload getBundleCategories(JSONArray arrayResponse, String senderId, ChatBotService chatBotService,
			String locale, String dial) {
		ArrayList<Element> elements = new ArrayList<Element>();
		List<Button> buttonsList = new ArrayList<Button>();
		for (int i = 0; i < arrayResponse.length(); i++) {
			try {
				JSONObject object = arrayResponse.getJSONObject(i);
				JSONObject categoryObject = object.getJSONObject("category");
				String title = "Mobile Internet Bundles";
				String subTitle = "Mobile Internet Bundles";
				String buttonLabel = "";
				ArrayList<String> categories = new ArrayList<String>();
				if (locale.equalsIgnoreCase("ar")) {
					buttonLabel = categoryObject.getString("categoryNameAr");
					String[] categoriesArray = chatBotService.getEnabledCategoryConfigurationDaoById(1l)
							.getArabicCategories().split(",");
					categories = new ArrayList(Arrays.asList(categoriesArray));
				} else {
					buttonLabel = categoryObject.getString("categoryNameEn");
					String[] categoriesArray = chatBotService.getEnabledCategoryConfigurationDaoById(1l)
							.getArabicCategories().split(",");
					categories = new ArrayList(Arrays.asList(categoriesArray));
				}
				String payLoad = "sub_" + categoryObject.getString("categoryId");

				if (categories.contains(buttonLabel)) {
					PostbackButton bundleButton = PostbackButton.create(buttonLabel, payLoad);
					buttonsList.add(bundleButton);
					Element element = Element.create(title, Optional.of(subTitle), empty(), empty(),
							Optional.of(buttonsList));
					elements.add(element);
				}

			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		if (elements.size() > 0) {
			GenericTemplate gTemplate = GenericTemplate.create(elements);
			return MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));
		} else {
			GenericTemplate gTemplate = CreateGenericTemplateForNotEligiblBundleDials(locale, buttonsList, elements,
					dial);

			return MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));
		}
	}

	@Override
	public void getTextMessageIfResponseIsString(String senderId, ArrayList<MessagePayload> messagePayloadList,
			BotWebserviceMessage botWebserviceMessage, String jsonBodyString, ChatBotService chatBotService,
			String local) throws JSONException {
		String text = "";
		if (local.equalsIgnoreCase("ar")) {
			text = botWebserviceMessage.getTitle().getArabicText();
		} else {
			text = botWebserviceMessage.getTitle().getEnglishText();
		}

		TextMessage textMSG = TextMessage.create(text);
		MessagePayload msgPayLoad = MessagePayload.create(senderId, MessagingType.RESPONSE, textMSG);
		messagePayloadList.add(msgPayLoad);
	}

	// Return key As one String according to customer local
	@Override
	public String getKeysString(BotTextResponseMapping botTextResponseMapping, String local) {
		String keys = "";
		if (local.equalsIgnoreCase("ar")) {
			keys = botTextResponseMapping.getArParams();
		} else {
			keys = botTextResponseMapping.getEnParams();
		}
		return keys;
	}

	/**
	 * @param payload
	 * @param senderId
	 * @param userFirstName
	 * @param botInteraction
	 * @param messagePayloadList
	 * @param botInteractionMessage
	 * @param messageTypeId
	 * @param messageId
	 */
	@Override
	public MessagePayload responseInCaseStaticScenario(String payload, String senderId, String userFirstName,
			BotInteraction botInteraction, BotInteractionMessage botInteractionMessage, Long messageTypeId,
			Long messageId, ChatBotService chatBotService, String parentPayLoad, String locale, String userDial) {
		String text;
		MessagePayload messagePayload = null;
		// text message
		if (messageTypeId == Utils.MessageTypeEnum.TEXTMESSAGE.getValue()) {
			BotTextMessage botTextMessage = chatBotService.findTextMessageByMessageId(messageId);
			parentPayLoad = botInteraction.getParentPayLoad();
			text = getTextValueForBotTextMessage(botTextMessage, locale);
			text = text + " " + userFirstName;
			if (payload.equalsIgnoreCase("welcome")) {

				CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
				CustomerProfile newCustomerProfile = new CustomerProfile();
				// Date insertionDate = customerProfile.getFirstInsertion();
				if (customerProfile == null) {
					Date date = new Date();
					Timestamp timestamp = new Timestamp(date.getTime());
					newCustomerProfile.setFirstInsertion(timestamp);
					newCustomerProfile.setSenderID(senderId);
					newCustomerProfile.setCustomerLastSeen(timestamp);
					newCustomerProfile.setLocal(locale);
					chatBotService.saveCustomerProfile(newCustomerProfile);
				} else {
					Date date = new Date();
					Timestamp timestamp = new Timestamp(date.getTime());
					newCustomerProfile.setSenderID(senderId);
					newCustomerProfile.setCustomerLastSeen(timestamp);
					newCustomerProfile.setLocal(locale);
					newCustomerProfile.setCustomerLastSeen(customerProfile.getCustomerLastSeen());
					newCustomerProfile.setFirstInsertion(customerProfile.getFirstInsertion());
					newCustomerProfile.setLastGetProfileWSCall(customerProfile.getLastGetProfileWSCall());
					newCustomerProfile.setMsisdn(customerProfile.getMsisdn());
					newCustomerProfile.setLinkingDate(customerProfile.getLinkingDate());
					chatBotService.saveCustomerProfile(newCustomerProfile);
				}

			}
			messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TextMessage.create(text));

		}
		// quick reply
		else if (messageTypeId == Utils.MessageTypeEnum.QUICKREPLYMESSAGE.getValue()) {
			BotQuickReplyMessage botQuickReplyMessage = chatBotService.findQuickReplyMessageByMessageId(messageId);
			text = getTextForQuickReply(locale, botQuickReplyMessage, userDial);
			List<QuickReply> quickReplies = createQuickReply(chatBotService, messageId, locale);
			Optional<List<QuickReply>> quickRepliesOp = Optional.of(quickReplies);
			messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE,
					TextMessage.create(text, quickRepliesOp, empty()));
		}
		// generic template
		else if (messageTypeId == Utils.MessageTypeEnum.GENERICTEMPLATEMESSAGE.getValue()) {
			Template template = createGenericTemplate(messageId, chatBotService, locale, new BotWebserviceMessage(),
					new JSONObject(), userDial, new ArrayList<String>());
			messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(template));
			// ButtonTemplate
		} else if (messageTypeId == Utils.MessageTypeEnum.ButtonTemplate.getValue()) {
			ButtonTemplate buttonTemplate = createButtonTemplateInScenario(botInteractionMessage, chatBotService,
					locale, userDial);
			messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE,
					TemplateMessage.create(buttonTemplate));
		}
		return messagePayload;
	}

	// get text for BotTextMessage according to local
	@Override
	public String getTextValueForBotTextMessage(BotTextMessage botTextMessage, String local) {
		String text = "";
		if (local.equalsIgnoreCase("ar")) {
			text = botTextMessage.getBotText().getArabicText();
		} else {
			text = botTextMessage.getBotText().getEnglishText();
		}
		return text;
	}

	/**
	 * call webService
	 * 
	 * @return String response
	 * @param botWebserviceMessage
	 */
	@Override
	public Map<String, String> callGetWebService(BotWebserviceMessage botWebserviceMessage, String senderId,
			ChatBotService chatBotService) {
		/*CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		Map<String, String> resposeMap = new HashMap<String, String>();
		String dialNumber = customerProfile.getMsisdn();
		Long time = System.currentTimeMillis() / 1000;
		String par = "";
		String paramChannel = "4e47684968446e4e7067726d3968507a4f77585273684d3152647046703752454c6d4a4b59533978484557636750357151644c487154544370445343414d7252";
		String response = null;
		try {
			par = Utils.encryptDPIParam("time=1498033943505" + "&user=" + dialNumber + "&URL=facebook");

			String realParameter = "param:" + par +",paramChannel:"+paramChannel ;
			HttpClient client = HttpClients.createDefault();

			HttpGet httpGet = new HttpGet(botWebserviceMessage.getWsUrl());
			URI uri = new URIBuilder(httpGet.getURI()).addParameter("dial", realParameter).build();
			System.out.println("URL is " + uri);
			httpGet.setURI(uri);
			HttpResponse response2 = client.execute(httpGet);
			HttpEntity entity = response2.getEntity();
			int responseStatusId = response2.getStatusLine().getStatusCode();
			response = EntityUtils.toString(entity, "UTF-8");
			if (responseStatusId == 200) {
				resposeMap.put("status", String.valueOf(responseStatusId));
				System.out.println(response);
				resposeMap.put("response", response);
			} else {
				resposeMap.put("status", String.valueOf(responseStatusId));
				resposeMap.put("response", response);
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}*/

		return cachingResponse(botWebserviceMessage, senderId, chatBotService);
	}

	@Override
	public Map<String, String> callPostWebService(BotWebserviceMessage botWebserviceMessage, String jsonParam,
			ChatBotService chatBotService, String senderId, ArrayList<String> paramList) {
		CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		String dialNumber = customerProfile.getMsisdn();
		Map<String, String> mapResponse = new HashMap<String, String>();
		Long time = System.currentTimeMillis() / 1000;
		String par = "";
		String paramChannel = "4e47684968446e4e7067726d3968507a4f77585273684d3152647046703752454c6d4a4b59533978484557636750357151644c487154544370445343414d7252";

		try {
			par = Utils.encryptDPIParam("time=1498033943505" + "&user=" + dialNumber + "&URL=facebook");
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String realParameter = "param:" + par + ",paramChannel:" + paramChannel;
		CloseableHttpClient client = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(botWebserviceMessage.getWsUrl());
		try {
			URI uri = new URIBuilder(httpPost.getURI()).addParameter("dial", realParameter).build();
			httpPost.setURI(uri);
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String stringResponse = "";
		StringEntity entity = null;
		int responseStatusId = 0;
		try {
			entity = new StringEntity(jsonParam);
			httpPost.setEntity(entity);
			httpPost.setHeader("Accept", "application/json");
			httpPost.setHeader("Content-type", "application/json");
			CloseableHttpResponse response = client.execute(httpPost);
			HttpEntity responseEntity = response.getEntity();
			responseStatusId = response.getStatusLine().getStatusCode();
			stringResponse = EntityUtils.toString(responseEntity, "UTF-8");
			client.close();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (responseStatusId == 200) {
			mapResponse.put("status", String.valueOf(responseStatusId));
			mapResponse.put("response", "Response Retrieved");
		} else {
			mapResponse.put("status", String.valueOf(responseStatusId));
			mapResponse.put("response", "Response did not Retrieve");
		}
		return mapResponse;
	}

	/**
	 * @param senderId
	 * @param messagePayloadList
	 * @param botWebserviceMessage
	 * @param jsonBodyString
	 * @throws JSONException
	 */
	@Override
	public void createTextMessageInDynamicScenario(String senderId, ArrayList<MessagePayload> messagePayloadList,
			BotWebserviceMessage botWebserviceMessage, String jsonBodyString, ChatBotService chatBotService,
			String local) throws JSONException {
		// string
		System.out.println("doing");
		if (botWebserviceMessage.getOutType().getInOutTypeId() == 1) {
			getTextMessageIfResponseIsString(senderId, messagePayloadList, botWebserviceMessage, jsonBodyString,
					chatBotService, local);
			// Object
		} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 2) {
			getTextMessageIfResponseIsObject(senderId, messagePayloadList, botWebserviceMessage, jsonBodyString,
					chatBotService, local);
			// Array
		} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 3) {
			getTextMessageIfResponseIsArray(senderId, messagePayloadList, botWebserviceMessage, jsonBodyString,
					chatBotService, local);
		}
	}

	@Override
	public String getTextForQuickReply(String local, BotQuickReplyMessage botQuickReplyMessage, String userDial) {
		String text = "";
		if (local.equalsIgnoreCase("ar")) {
			text = botQuickReplyMessage.getBotText().getArabicText() + " " + userDial + " ?";
		} else {
			text = botQuickReplyMessage.getBotText().getEnglishText() + " " + userDial + " ?";
		}
		return text;
	}

	@Override
	public CustomerProfile setLinkingInfoForCustomer(String senderId, Messenger messenger, String customerDial,
			ChatBotService chatBotService) {
		UserProfile userProfile = null;
		try {
			userProfile = messenger.queryUserProfile(senderId);
		} catch (MessengerApiException e) {
			System.out.println(e.getMessage());
		} catch (MessengerIOException e) {
			System.out.println(e.getMessage());
		}
		CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		CustomerProfile newCustomerProfile = new CustomerProfile();
		Date date = new Date();
		Timestamp timestamp = new Timestamp(date.getTime());
		newCustomerProfile.setCustomerLastSeen(timestamp);
		newCustomerProfile.setFirstInsertion(customerProfile.getFirstInsertion());
		newCustomerProfile.setLinkingDate(timestamp);
		newCustomerProfile.setLocal(customerProfile.getLocal());
		if (customerDial.startsWith("0")) {
			newCustomerProfile.setMsisdn(customerDial);
		} else {
			newCustomerProfile.setMsisdn("");
		}
		newCustomerProfile.setSenderID(senderId);

		return newCustomerProfile;
	}

	@Override
	public void setCustomerProfileLocalAsArabic(CustomerProfile customerProfile, ChatBotService chatBotService) {
		CustomerProfile customerProfileForLocal = new CustomerProfile();
		Date date = new Date();
		Timestamp timestamp = new Timestamp(date.getTime());
		customerProfileForLocal.setCustomerLastSeen(timestamp);
		customerProfileForLocal.setFirstInsertion(customerProfile.getFirstInsertion());
		customerProfileForLocal.setLastGetProfileWSCall(customerProfile.getLastGetProfileWSCall());
		customerProfileForLocal.setLinkingDate(customerProfile.getLinkingDate());
		customerProfileForLocal.setLocal("ar");
		customerProfileForLocal.setMsisdn(customerProfile.getMsisdn());
		customerProfileForLocal.setSenderID(customerProfile.getSenderID());
		chatBotService.saveCustomerProfile(customerProfileForLocal);
	}

	@Override
	public void setCustomerProfileLocalAsEnglish(CustomerProfile customerProfile, ChatBotService chatBotService) {
		CustomerProfile customerProfileForLocal = new CustomerProfile();
		Date date = new Date();
		Timestamp timestamp = new Timestamp(date.getTime());
		customerProfileForLocal.setCustomerLastSeen(timestamp);
		customerProfileForLocal.setFirstInsertion(customerProfile.getFirstInsertion());
		customerProfileForLocal.setLastGetProfileWSCall(customerProfile.getLastGetProfileWSCall());
		customerProfileForLocal.setLinkingDate(customerProfile.getLinkingDate());
		customerProfileForLocal.setLocal("en_us");
		customerProfileForLocal.setMsisdn(customerProfile.getMsisdn());
		customerProfileForLocal.setSenderID(customerProfile.getSenderID());
		chatBotService.saveCustomerProfile(customerProfileForLocal);
	}

	@Override
	public MessagePayload getProductsFromJsonByCategory(JSONArray arrayResponse, String category, String senderId,
			ChatBotService chatBotService, String locale) {
		MessagePayload fmsg = null;
		for (int i = 0; i < arrayResponse.length(); i++) {
			JSONObject object = new JSONObject();
			try {
				object = arrayResponse.getJSONObject(i);
				JSONArray products = new JSONArray();
				String categoryId = object.getJSONObject("category").getString("categoryId");
				if (category.equals(categoryId)) {
					JSONArray childEligibleProductModels = object.getJSONArray("childEligibleProductModels");
					if (categoryId.equalsIgnoreCase("LEGO")) {
						List<Element> elements = new ArrayList<Element>();
						for (int o = 0; o < childEligibleProductModels.length(); o++) {
							List<Button> buttons = new ArrayList<Button>();
							JSONObject childEligibleProduct = childEligibleProductModels.getJSONObject(o);
							JSONObject categoryObject = childEligibleProduct.getJSONObject("category");
							String label = categoryObject.getString("categoryNameEn");
							String payload = categoryObject.getString("categoryId");
							String title = "LEGO Bundles";
							String subTitle = "LEGO Bundles";
							PostbackButton button = PostbackButton.create(label, "sub_" + payload);
							Button backButton = PostbackButton.create("Back", "change bundle");
							buttons.add(backButton);
							buttons.add(button);

							Element element = Element.create(title, Optional.of(subTitle), empty(), empty(),
									Optional.of(buttons));
							elements.add(element);
						}
						GenericTemplate gTemplate = GenericTemplate.create(elements);
						fmsg = MessagePayload.create(senderId, MessagingType.RESPONSE,
								TemplateMessage.create(gTemplate));
					} else {
						products = object.getJSONArray("products");
						fmsg = getProductsByCategoryNotLego(senderId, locale, products);
					}

				}
			} catch (JSONException e) {
				System.out.println(e.getMessage());
			}
		}
		return fmsg;

	}

	@Override
	public MessagePayload getProductsByCategoryNotLego(String senderId, String locale, JSONArray products)
			throws JSONException {
		MessagePayload fmsg;
		List<Element> elements = new ArrayList<Element>();
		for (int j = 0; j < products.length(); j++) {
			List<Button> buttons = new ArrayList<Button>();
			SubscribeWSBody subscribeWSBody = new SubscribeWSBody();
			JSONObject childObject = products.getJSONObject(j);
			String name = childObject.getString("name");
			String operationName = childObject.getJSONArray("operationResponses").getJSONObject(0)
					.getString("operationName");

			JSONArray relatedProducts = childObject.getJSONArray("relatedProduct");
			String subtitle = "";
			String title = "";
			if (locale.equalsIgnoreCase("ar")) {
				title = childObject.getString("arabicName");
				subtitle = childObject.getString("arabicDescription");
			} else {
				title = childObject.getString("englishName");
				subtitle = childObject.getString("englishDescription");
			}
			if (relatedProducts.length() > 0) {
				Button button = PostbackButton.create(getLabelForSubscriptionButton(locale),
						"related" + "," + name + "," + operationName);
				buttons.add(button);
			} else {
				Button button = PostbackButton.create(getLabelForSubscriptionButton(locale),
						name + "," + operationName);
				buttons.add(button);
			}
			Button backButton = PostbackButton.create("Back", "change bundle");
			buttons.add(backButton);
			Element element = Element.create(title, Optional.of(subtitle), empty(), empty(), Optional.of(buttons));
			elements.add(element);
			// chatBotService.saveSubscribeWSBody(subscribeWSBody);
		}
		GenericTemplate gTemplate = GenericTemplate.create(elements);
		fmsg = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));
		return fmsg;
	}

	@Override
	public MessagePayload getProductsByCategoryIfLego(String senderId, String locale, JSONArray products)
			throws JSONException {
		MessagePayload fmsg;
		ArrayList<Element> elements = new ArrayList<Element>();
		for (int j = 0; j < products.length(); j++) {
			List<Button> buttons = new ArrayList<Button>();
			JSONObject childObject = products.getJSONObject(j);
			String name = childObject.getString("name");
			String operationName = childObject.getJSONArray("operationResponses").getJSONObject(0)
					.getString("operationName");

			JSONArray relatedProducts = childObject.getJSONArray("relatedProduct");
			String subtitle = "";
			String title = "";
			if (locale.equalsIgnoreCase("ar")) {
				title = childObject.getString("arabicName");
				subtitle = childObject.getString("arabicDescription");
			} else {
				title = childObject.getString("englishName");
				subtitle = childObject.getString("englishDescription");
			}
			if (relatedProducts.length() > 0) {
				Button button = PostbackButton.create(getLabelForSubscriptionButton(locale),
						"related" + "," + name + "," + operationName);
				buttons.add(button);
			} else {
				Button button = PostbackButton.create(getLabelForSubscriptionButton(locale),
						name + "," + operationName);
				buttons.add(button);
			}
			Button backButton = PostbackButton.create("Back", "change bundle");
			buttons.add(backButton);
			Element element = Element.create(title, Optional.of(subtitle), empty(), empty(), Optional.of(buttons));
			elements.add(element);
		}
		GenericTemplate gTemplate = GenericTemplate.create(elements);
		fmsg = MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));
		return fmsg;
	}

	@Override
	public MessagePayload getRelatedProductFromJsonByBundleId(JSONArray arrayResponse, String productId,
			String senderId, ChatBotService chatBotService, String locale) {
		MessagePayload fmsg = null;
		for (int i = 0; i < arrayResponse.length(); i++) {
			JSONObject object = new JSONObject();
			try {
				object = arrayResponse.getJSONObject(i);
				JSONArray products = new JSONArray();
				products = object.getJSONArray("products");
				for (int j = 0; j < products.length(); j++) {

					JSONObject childObject = products.getJSONObject(j);
					String name = childObject.getString("name");
					JSONArray relatedProducts = childObject.getJSONArray("relatedProduct");
					if (name.equals(productId)) {
						List<Element> elements = new ArrayList<Element>();
						for (int r = 0; r < relatedProducts.length(); r++) {
							List<Button> buttons = new ArrayList<Button>();
							JSONObject relatedObject = relatedProducts.getJSONObject(r);
							String relatedProductName = relatedObject.getString("name");
							String title = relatedObject.getString("englishName");
							String subtitle = relatedObject.getString("englishDescription");
							Button button = PostbackButton.create(getLabelForSubscriptionButton(locale),
									relatedProductName + "," + "relatedproductsubscription");
							buttons.add(button);
							Button backButton = PostbackButton.create("Back", "change bundle");
							buttons.add(backButton);
							Element element = Element.create(title, Optional.of(subtitle), empty(), empty(),
									Optional.of(buttons));
							elements.add(element);
						}
						GenericTemplate gTemplate = GenericTemplate.create(elements);
						return MessagePayload.create(senderId, MessagingType.RESPONSE,
								TemplateMessage.create(gTemplate));
					}
				}
			} catch (JSONException e) {
				System.out.println(e.getMessage());
			}
		}
		return fmsg;
	}

	@Override
	public MessagePayload getExtraMobileInternetAddonsByCategory(JSONArray arrayResponse, String senderId,
			ChatBotService chatBotService, String locale, String addonId) {
		ArrayList<Element> elements = new ArrayList<Element>();
		for (int i = 0; i < arrayResponse.length(); i++) {
			try {
				List<Button> buttonsList = new ArrayList<Button>();
				JSONObject object = arrayResponse.getJSONObject(i);

				JSONArray retrievedCategoriesArray = object.getJSONArray("productCategories");

				String title = "Mobile Internet Bundles";
				String subTitle = "Mobile Internet Bundles";
				String buttonLabel = "";
				ArrayList<String> categories = new ArrayList<String>();
				String id = object.getString("id");
				if (locale.equalsIgnoreCase("ar")) {
					buttonLabel = object.getString("arabicName");
					String[] categoriesArray = chatBotService.getEnabledCategoryConfigurationDaoById(2l)
							.getArabicCategories().split(",");
					categories = new ArrayList(Arrays.asList(categoriesArray));
				} else {
					buttonLabel = object.getString("englishName");
					String[] categoriesArray = chatBotService.getEnabledCategoryConfigurationDaoById(2l)
							.getArabicCategories().split(",");
					categories = new ArrayList(Arrays.asList(categoriesArray));
				}
				String payLoad = "subaddon_" + id;

				for (int o = 0; o < retrievedCategoriesArray.length(); o++) {
					String category = retrievedCategoriesArray.get(o).toString();
					if (category.equals(addonId)) {
						PostbackButton bundleButton = PostbackButton.create(buttonLabel, payLoad);
						buttonsList.add(bundleButton);
						Element element = Element.create(title, Optional.of(subTitle), empty(), empty(),
								Optional.of(buttonsList));
						elements.add(element);
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}

		}
		GenericTemplate gTemplate = GenericTemplate.create(elements);
		return MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));

	}

	public MessagePayload getCategoryForMobileInternetAddons(JSONArray arrayResponse, String senderId,
			ChatBotService chatBotService, String locale) {
		Map<String, String> categoriesMap = new HashMap<String, String>();
		ArrayList<Element> elements = new ArrayList<Element>();
		List<Button> buttonsList = new ArrayList<Button>();
		String title = "Mobile Internet Bundles";
		String subTitle = "Mobile Internet Bundles";
		for (int i = 0; i < arrayResponse.length(); i++) {
			try {

				JSONObject object = arrayResponse.getJSONObject(i);
				JSONArray retrievedCategoriesArray = object.getJSONArray("productCategories");

				String buttonLabel = "";
				ArrayList<String> categoriesLabels = new ArrayList<String>();
				ArrayList<String> categoriesPayloads = new ArrayList<String>();
				String id = object.getString("id");
				if (locale.equalsIgnoreCase("ar")) {
					buttonLabel = object.getString("arabicName");
					String[] categoriesArrayPayLoads = chatBotService.getEnabledCategoryConfigurationDaoById(2l)
							.getArabicCategories().split(",");
					String[] categoriesArrayLabels = chatBotService.getEnabledCategoryConfigurationDaoById(2l)
							.getCategoryLabel().getArabicText().split(",");
					categoriesLabels = new ArrayList(Arrays.asList(categoriesArrayLabels));
					categoriesPayloads = new ArrayList<>(Arrays.asList(categoriesArrayPayLoads));
				} else {
					buttonLabel = object.getString("englishName");
					String[] categoriesArrayPayLoads = chatBotService.getEnabledCategoryConfigurationDaoById(2l)
							.getArabicCategories().split(",");
					String[] categoriesArrayLabels = chatBotService.getEnabledCategoryConfigurationDaoById(2l)
							.getCategoryLabel().getEnglishText().split(",");
					categoriesLabels = new ArrayList(Arrays.asList(categoriesArrayPayLoads));
					categoriesPayloads = new ArrayList<>(Arrays.asList(categoriesArrayPayLoads));
				}

				for (int j = 0; j < retrievedCategoriesArray.length(); j++) {
					String category = retrievedCategoriesArray.get(j).toString();
					if (categoriesPayloads.contains(category)) {
						int index = categoriesPayloads.indexOf(category);
						categoriesMap.put(category, categoriesLabels.get(index));

					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}

		}
		if (categoriesMap.size() > 0) {
			for (String key : categoriesMap.keySet()) {
				PostbackButton bundleButton = PostbackButton.create(key, "MIAddon" + categoriesMap.get(key));
				buttonsList.add(bundleButton);
				Element element = Element.create(title, Optional.of(subTitle), empty(), empty(),
						Optional.of(buttonsList));
				elements.add(element);
			}
		}

		GenericTemplate gTemplate = GenericTemplate.create(elements);
		return MessagePayload.create(senderId, MessagingType.RESPONSE, TemplateMessage.create(gTemplate));

	}

	private String getLabelForSubscriptionButton(String locale) {
		String label = "";
		if (locale.contains("ar")) {
			label = "اشترك";
		} else {
			label = "Subscribe";
		}
		return label;
	}

	private GenericTemplate CreateGenericTemplateForNotEligiblBundleDials(String locale, List<Button> buttonsList,
			ArrayList<Element> elements, String dial) {

		if (locale.contains("ar")) {
			PostbackButton bundleButton = PostbackButton.create("عودة", "MI consumption parent");
			buttonsList.add(bundleButton);
			Element element = Element.create("باقات الانترنت ",
					Optional.of("نأسف هذا الرقم" + " ' " + dial + " ' " + "لا توجد باقات صالحة له"), empty(), empty(),
					Optional.of(buttonsList));
			elements.add(element);
			return GenericTemplate.create(elements);
		} else {
			PostbackButton bundleButton = PostbackButton.create("Back", "MI consumption parent");
			buttonsList.add(bundleButton);
			Element element = Element.create("Mobile Internet",
					Optional.of("Your dial" + " ' " + dial + " ' " + "is not eligible to any bundle"), empty(), empty(),
					Optional.of(buttonsList));
			elements.add(element);
			return GenericTemplate.create(elements);
		}
	}

	
	public Map<String, String> cachingResponse(BotWebserviceMessage botWebserviceMessage, String senderId,
			ChatBotService chatBotService) {
		
		CustomerProfile customerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
		Map<String, String> resposeMap = new HashMap<String, String>();
		String dialNumber = customerProfile.getMsisdn();
		String stringResponse = "";
		CacheConfig cacheConfig = CacheConfig.custom()
		        .setMaxCacheEntries(1000)
		        .setMaxObjectSize(8192)
		        .build();
		RequestConfig requestConfig = RequestConfig.custom()
		        .setConnectTimeout(30000)
		        .setSocketTimeout(30000)
		        .build();
		CloseableHttpClient cachingClient = CachingHttpClients.custom()
		        .setCacheConfig(cacheConfig)
		        .setDefaultRequestConfig(requestConfig)
		        .build();
		HttpCacheContext context = HttpCacheContext.create();
		String par = "";
		String paramChannel = "4e47684968446e4e7067726d3968507a4f77585273684d3152647046703752454c6d4a4b59533978484557636750357151644c487154544370445343414d7252";
		
			try {
				par = Utils.encryptDPIParam("time=1498033943505" + "&user=" + dialNumber + "&URL=facebook");
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			CloseableHttpResponse response = null;
			String realParameter = "param:" + par/* +",paramChannel:"+paramChannel */;

			HttpGet httpGet = new HttpGet(botWebserviceMessage.getWsUrl());
			URI uri = null;
			try {
				uri = new URIBuilder(httpGet.getURI()).addParameter("dial", realParameter).build();
			} catch (URISyntaxException e1) {
				e1.printStackTrace();
			}
			httpGet.setURI(uri);
		try {
			response = cachingClient.execute(httpGet, context);
			HttpEntity entity = response.getEntity();
			stringResponse = EntityUtils.toString(entity, "UTF-8");
			int responseStatusId = response.getStatusLine().getStatusCode();
			if (responseStatusId == 200) {
				resposeMap.put("status", String.valueOf(responseStatusId));
				System.out.println(response);
				resposeMap.put("response", stringResponse);
			} else {
				resposeMap.put("status", String.valueOf(responseStatusId));
				resposeMap.put("response", stringResponse);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
		    CacheResponseStatus responseStatus = context.getCacheResponseStatus();
		    switch (responseStatus) {
		        case CACHE_HIT:
		            System.out.println("A response was generated from the cache with " +
		                    "no requests sent upstream");
		            break;
		        case CACHE_MODULE_RESPONSE:
		            System.out.println("The response was generated directly by the " +
		                    "caching module");
		            break;
		        case CACHE_MISS:
		            System.out.println("The response came from an upstream server");
		            break;
		        case VALIDATED:
		            System.out.println("The response was generated from the cache " +
		                    "after validating the entry with the origin server");
		            break;
		    }
		} finally {
		    try {
				response.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return resposeMap;
	}
	
	
}
