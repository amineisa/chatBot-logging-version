package com.chatbot.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.chatbot.util.Constants;

/**
 * @author Amin Eisa 
 */
@Service
public class JSONUtilsService {
	
	private static final Logger logger = LoggerFactory.getLogger(JSONUtilsService.class);

	// IN Case Zero Level JSONObject
	public Map<String, ArrayList<String>> inCaseZeroLevelJsonObject(String[] keys, JSONObject jsonObject, String msg, String locale) {
		Map<String, ArrayList<String>> mapValues = new HashMap<>();
		ArrayList<String> titleList = new ArrayList<>();
		ArrayList<String> values = new ArrayList<>();
		ArrayList<String> textMsgs = new ArrayList<>();
		values =  getValuesFromJson(jsonObject, keys);
		logger.debug(Constants.LOGGER_INFO_PREFIX+"In Text message "+values);
		String finalMsg = replaceValuesByMapping(values, msg);
		textMsgs.add(finalMsg);
		titleList.add(values.get(0));
		textMsgs.add(finalMsg);
		mapValues.put(Constants.RESPONSE_MAP_TITLE_KEY, titleList);
		textMsgs.add(finalMsg);
		mapValues.put(Constants.RESPONSE_MAP_MESSAGE_KEY, textMsgs);
		return mapValues;

	}

	// IN Case Zero Level JSONArray
	public Map<String, ArrayList<String>> inCaseZeroLevelJsonArray(String[] keys, JSONArray rootArray, String msg, String locale) {

		Map<String, ArrayList<String>> mapValues = new HashMap<>();
		ArrayList<String> titleList = new ArrayList<>();
		ArrayList<String> values = new ArrayList<>();
		ArrayList<String> textMsgs = new ArrayList<>();
		for (int i = 0; i < rootArray.length(); i++) {
			try {
				JSONObject childObject = rootArray.getJSONObject(i);
				logger.debug(Constants.LOGGER_INFO_PREFIX+"Resonse json object "+childObject);
				values = getValuesFromJson(childObject, keys);
				//titleList.add(values.get(0));
				//mapValues.put(Constants.RESPONSE_MAP_TITLE_KEY, titleList);
				//mapValues.put(Constants.RESPONSE_MAP_MESSAGE_KEY, textMsgs);
			} catch (JSONException e) {
				logger.error(Constants.LOGGER_EXCEPTION_MESSAGE + e);
				e.printStackTrace();
			}
			String finalMsg = replaceValuesByMapping(values, msg);
			textMsgs.add(finalMsg);
			titleList.add(values.get(0));
		}
		mapValues.put(Constants.RESPONSE_MAP_TITLE_KEY, titleList);
		mapValues.put(Constants.RESPONSE_MAP_MESSAGE_KEY, textMsgs);
		return mapValues;
	}

	// IN Case ONE Level JSONObject
	public Map<String, ArrayList<String>> inCaseOneLevelJsonObject(String[] paths, String[] keys, JSONObject jsonObject, String msg, String locale) {
		Map<String, ArrayList<String>> mapValues = new HashMap<>();
		ArrayList<String> titleList = new ArrayList<>();
		ArrayList<String> values = new ArrayList<>();
		ArrayList<String> textMsgs = new ArrayList<>();
		JSONArray firstLevelArray = new JSONArray();
		JSONObject firstLevelObject = new JSONObject();

		if (paths[0].startsWith(Constants.IS_ARRAY_KEY_IN_JSON_RESPONSE)) {
			paths[0] = paths[0].substring(1, paths[0].length());

			try {
				firstLevelArray = jsonObject.getJSONArray(paths[0]);
			} catch (JSONException e) {
				logger.error(Constants.LOGGER_EXCEPTION_MESSAGE + e);
				e.printStackTrace();
			}
			for (int i = 0; i < firstLevelArray.length(); i++) {
				try {
					firstLevelObject = firstLevelArray.getJSONObject(i);
				} catch (JSONException e) {
					logger.error(Constants.LOGGER_EXCEPTION_MESSAGE + e);
					e.printStackTrace();
				}
				values = getValuesFromJson(firstLevelObject, keys);
				String finalMsg = replaceValuesByMapping(values, msg);
				titleList.add(values.get(0));
				textMsgs.add(finalMsg);
				mapValues.put(Constants.RESPONSE_MAP_TITLE_KEY, titleList);
				textMsgs.add(finalMsg);
				mapValues.put(Constants.RESPONSE_MAP_MESSAGE_KEY, textMsgs);
			}
		} else if (!paths[0].startsWith(Constants.IS_ARRAY_KEY_IN_JSON_RESPONSE)) {
			try {
				firstLevelObject = jsonObject.getJSONObject(paths[0]);
			} catch (JSONException e) {
				logger.error(Constants.LOGGER_EXCEPTION_MESSAGE + e);
				e.printStackTrace();
			}
			values = getValuesFromJson(firstLevelObject, keys);
			String finalMsg = replaceValuesByMapping(values, msg);
			titleList.add(values.get(0));
			textMsgs.add(finalMsg);
			mapValues.put(Constants.RESPONSE_MAP_TITLE_KEY, titleList);
			mapValues.put(Constants.RESPONSE_MAP_MESSAGE_KEY, textMsgs);
		}
		return mapValues;
	}

	// IN Case ONE Level JSONOArray
	public Map<String, ArrayList<String>> inCaseOneLevelJsonArrayForTextMessage(String[] paths, String[] keys, JSONArray rootArray, String msg, String locale) {
		ArrayList<String> values = new ArrayList<>();
		Map<String, ArrayList<String>> mapValuse = new HashMap<>();
		ArrayList<String> textMsgs = new ArrayList<>();
		ArrayList<String> titleList = new ArrayList<>();
		Map<String, ArrayList<String>> mapValues = new HashMap<>();
		for (int i = 0; i < rootArray.length(); i++) {
			try {
				JSONObject childObject = rootArray.getJSONObject(i);
				if (paths[0].startsWith(Constants.IS_ARRAY_KEY_IN_JSON_RESPONSE)) {
					JSONArray childArray = childObject.getJSONArray(paths[0].substring(1, paths[0].length()));
					for (int j = 0; j < childArray.length(); j++) {
						JSONObject subChild = childArray.getJSONObject(0);
						values = getValuesFromJson(subChild, keys);
						String finalMsg = replaceValuesByMapping(values, msg);
						titleList.add(values.get(0));
						mapValuse.put(Constants.RESPONSE_MAP_TITLE_KEY, titleList);
						textMsgs.add(finalMsg);
						mapValuse.put(Constants.RESPONSE_MAP_MESSAGE_KEY, textMsgs);
						mapValues.put(Constants.RESPONSE_MAP_TITLE_KEY, titleList);
						textMsgs.add(finalMsg);
						mapValues.put(Constants.RESPONSE_MAP_MESSAGE_KEY, textMsgs);

					}
				} else if (!paths[0].startsWith(Constants.IS_ARRAY_KEY_IN_JSON_RESPONSE)) {
					JSONObject child = childObject.getJSONObject(paths[0]);
					values = getValuesFromJson(child, keys);
					titleList.add(values.get(0));
					String finalMsg = replaceValuesByMapping(values, msg);
					textMsgs.add(finalMsg);
					mapValuse.put(Constants.RESPONSE_MAP_TITLE_KEY, titleList);
					textMsgs.add(finalMsg);
					mapValuse.put(Constants.RESPONSE_MAP_MESSAGE_KEY, textMsgs);
					mapValues.put(Constants.RESPONSE_MAP_TITLE_KEY, titleList);
					textMsgs.add(finalMsg);
					mapValues.put(Constants.RESPONSE_MAP_MESSAGE_KEY, textMsgs);
				}
			} catch (Exception e) {
				logger.error(Constants.LOGGER_EXCEPTION_MESSAGE + e);
				e.printStackTrace();
			}
		}
		return mapValues;
	}

	// In case Two Level JSONObject
	public Map<String, ArrayList<String>> inCaseTwoLevelJsonObject(JSONObject jsonObject, String[] paths, String[] keys, String msg, String locale) {
		ArrayList<String> values = new ArrayList<>();
		Map<String, ArrayList<String>> mapValues = new HashMap<>();
		ArrayList<String> titleList = new ArrayList<>();
		ArrayList<String> textMsgs = new ArrayList<>();
		ArrayList<String> percentageList = new ArrayList<>();
		JSONObject firstLevelObject = new JSONObject();
		JSONArray firstLevelArray = new JSONArray();
		String path = paths[0];
		if (path.startsWith(Constants.IS_ARRAY_KEY_IN_JSON_RESPONSE)) {
			try {
				path = path.substring(1, path.length());
				firstLevelArray = jsonObject.getJSONArray(path);
				for (int i = 0; i < firstLevelArray.length(); i++) {
					firstLevelObject = firstLevelArray.getJSONObject(i);
					if (paths[1].startsWith(Constants.IS_ARRAY_KEY_IN_JSON_RESPONSE)) {
						String finalPath = paths[1].substring(1, paths[1].length());
						JSONArray finalJsonArray = firstLevelObject.getJSONArray(finalPath);
						for (int j = 0; j < finalJsonArray.length(); j++) {
							try{
								boolean isTime = false;
								JSONObject finalJsonObject = finalJsonArray.getJSONObject(j);
								double consumed, total, percentage;
								consumed = total = percentage= 0;
								values = getValuesFromJson(finalJsonObject, keys);
								titleList.add(values.get(0));
								values.remove(0);
								if(values.get(0).contains(":") || values.get(2).contains(":")){
									isTime = true;
								}
								if(!isTime){
									consumed = Double.parseDouble(values.get(0));							
									if (consumed == 0.0) {
										percentage = 0;
									}else{
										total = Double.parseDouble(values.get(2));
										percentage = (consumed / total) * 100;
									}
								}else{
									consumed = timeToMins(values.get(0));							
									if (consumed == 0.0) {
										percentage = 0;
									}else{
										total = timeToMins(values.get(2));
										percentage = (consumed / total) * 100;
									}
								}
								logger.debug(Constants.LOGGER_INFO_PREFIX+"Meter Percentage : "+percentage);
								percentageList.add(String.valueOf(percentage));
								String finalMsg = replaceValuesByMapping(values, msg);
								textMsgs.add(finalMsg);
							}catch(Exception e){
								e.printStackTrace();
							}							
						}
						mapValues.put(Constants.RESPONSE_MAP_MESSAGE_KEY, textMsgs);
						mapValues.put(Constants.RESPONSE_MAP_TITLE_KEY, titleList);
						mapValues.put(Constants.RESPONSE_PERCENTAGE_KEY, percentageList);
					} else if (!paths[1].startsWith(Constants.IS_ARRAY_KEY_IN_JSON_RESPONSE)) {
						JSONObject firstParent = firstLevelObject.getJSONObject(paths[1]);
						values = getValuesFromJson(firstParent, keys);
						titleList.add(values.get(0));
						mapValues.put(Constants.RESPONSE_MAP_TITLE_KEY, titleList);
						String finalMsg = replaceValuesByMapping(values, msg);
						textMsgs.add(finalMsg);
						mapValues.put(Constants.RESPONSE_MAP_MESSAGE_KEY, values);
					}
				}
			} catch (Exception e) {
				logger.error(Constants.LOGGER_EXCEPTION_MESSAGE + e);
				e.printStackTrace();
			}
		} else if (!path.startsWith(Constants.IS_ARRAY_KEY_IN_JSON_RESPONSE)) {
			try {
				firstLevelObject = jsonObject.getJSONObject(path);
				if (paths[1].startsWith(Constants.IS_ARRAY_KEY_IN_JSON_RESPONSE)) {
					JSONArray array = firstLevelObject.getJSONArray(paths[1].substring(1, paths[1].length()));
					for (int i = 0; i < array.length(); i++) {
						JSONObject tempObject = array.getJSONObject(i);
						values = getValuesFromJson(tempObject, keys);
						titleList.add(values.get(0));
						String finalMsg = replaceValuesByMapping(values, msg);
						textMsgs.add(finalMsg);
						textMsgs.add(finalMsg);
						textMsgs.add(finalMsg);
					}
					mapValues.put(Constants.RESPONSE_MAP_MESSAGE_KEY, textMsgs);
					mapValues.put(Constants.RESPONSE_MAP_TITLE_KEY, titleList);
				} else if (!paths[1].startsWith(Constants.IS_ARRAY_KEY_IN_JSON_RESPONSE)) {
					JSONObject lastObject = firstLevelObject.getJSONObject(paths[1]);
					values = getValuesFromJson(lastObject, keys);
					String finalMsg = replaceValuesByMapping(values, msg);
					textMsgs.add(finalMsg);
					titleList.add(values.get(0));
					textMsgs.add(finalMsg);
					mapValues.put(Constants.RESPONSE_MAP_TITLE_KEY, titleList);
					textMsgs.add(finalMsg);
					mapValues.put(Constants.RESPONSE_MAP_MESSAGE_KEY, textMsgs);
				}
			} catch (Exception e) {
				logger.error(Constants.LOGGER_EXCEPTION_MESSAGE + e);
				e.printStackTrace();
			}
		}
		return mapValues;

	}

	// In case Two Level JSONArray
	public Map<String, ArrayList<String>> inCaseTwoLevelJsonArrayForTextMessage(JSONArray rootArray, String[] paths, String[] keys, String msg, String locale) {
		ArrayList<String> titleList = new ArrayList<>();
		Map<String, ArrayList<String>> mapValues = new HashMap<>();
		ArrayList<String> values = new ArrayList<>();
		ArrayList<String> textMsgs = new ArrayList<>();
		for (int i = 0; i < rootArray.length(); i++) {
			try {
				JSONObject firstLevel = rootArray.getJSONObject(i);
				if (paths[0].startsWith(Constants.IS_ARRAY_KEY_IN_JSON_RESPONSE)) {
					JSONArray subArray = firstLevel.getJSONArray(paths[0].substring(1, paths[0].length()));
					for (int j = 0; j < subArray.length(); j++) {
						JSONObject subObject = subArray.getJSONObject(j);
						if (paths[1].startsWith(Constants.IS_ARRAY_KEY_IN_JSON_RESPONSE)) {
							JSONArray subChildArray = subObject.getJSONArray(paths[1].substring(1, paths[1].length()));
							for (int k = 0; k < subChildArray.length(); k++) {
								JSONObject subChildObject = subChildArray.getJSONObject(k);
								values = getValuesFromJson(subChildObject, keys);
								String finalMsg = replaceValuesByMapping(values, msg);
								textMsgs.add(finalMsg);
							}
						} else if (!paths[1].startsWith(Constants.IS_ARRAY_KEY_IN_JSON_RESPONSE)) {
							JSONObject childObject = subObject.getJSONObject(paths[1]);
							values = getValuesFromJson(childObject, keys);
							String finalMsg = replaceValuesByMapping(values, msg);
							textMsgs.add(finalMsg);
							titleList.add(values.get(0));
							textMsgs.add(finalMsg);
							mapValues.put(Constants.RESPONSE_MAP_TITLE_KEY, titleList);
							textMsgs.add(finalMsg);
							mapValues.put(Constants.RESPONSE_MAP_MESSAGE_KEY, textMsgs);
						}
					}
				} else if (!paths[0].startsWith(Constants.IS_ARRAY_KEY_IN_JSON_RESPONSE)) {
					JSONObject firstLevelObject = firstLevel.getJSONObject(paths[0]);
					if (paths[1].startsWith(Constants.IS_ARRAY_KEY_IN_JSON_RESPONSE)) {
						JSONArray secondLevelArray = firstLevelObject.getJSONArray(paths[1].substring(1, paths[1].length()));
						for (int j = 0; j < secondLevelArray.length(); j++) {
							JSONObject subChildObject = secondLevelArray.getJSONObject(j);
							values = getValuesFromJson(subChildObject, keys);
							String finalMsg = replaceValuesByMapping(values, msg);
							textMsgs.add(finalMsg);
							titleList.add(values.get(0));
							textMsgs.add(finalMsg);
							mapValues.put(Constants.RESPONSE_MAP_TITLE_KEY, titleList);
							textMsgs.add(finalMsg);
							mapValues.put(Constants.RESPONSE_MAP_MESSAGE_KEY, textMsgs);
						}
					} else if (!paths[1].startsWith(Constants.IS_ARRAY_KEY_IN_JSON_RESPONSE)) {
						JSONObject secondlevelObject = firstLevelObject.getJSONObject(paths[1]);
						values = getValuesFromJson(secondlevelObject, keys);
						String finalMsg = replaceValuesByMapping(values, msg);
						textMsgs.add(finalMsg);
						titleList.add(values.get(0));
						textMsgs.add(finalMsg);
						mapValues.put(Constants.RESPONSE_MAP_TITLE_KEY, titleList);
						textMsgs.add(finalMsg);
						mapValues.put(Constants.RESPONSE_MAP_MESSAGE_KEY, textMsgs);
					}

				}
			} catch (Exception e) {
				logger.error(Constants.LOGGER_EXCEPTION_MESSAGE + e);
				e.printStackTrace();
			}
		}
		return mapValues;
	}
	
	
	// Tested and works well get values from json object by its keys
		public ArrayList<String> getValuesFromJson(JSONObject jsonObject, String[] keys) {
			ArrayList<String> values = new ArrayList<>();
			for (int i = 0; i < keys.length; i++) {
				String key = keys[i];
				String value = Constants.EMPTY_STRING;
				try {
					if (key.contains(Constants.IS_KEY_HAS_DOT)) {
						String jsonKey = key.substring(0, key.indexOf(Constants.IS_KEY_HAS_DOT));
						String valueKey = key.substring(key.indexOf(Constants.IS_KEY_HAS_DOT) + 1, key.length());
						JSONObject finalObject = jsonObject.getJSONObject(jsonKey);
						value = finalObject.getString(valueKey);
					} else {
						if(key.contains("Date") ||key.contains("date") ) {
							Date d = new Date(jsonObject.getLong(key));
							value = d.toString();
						}else {
							value = jsonObject.getString(key);
						}
					} 
				} catch (JSONException e) {
					e.printStackTrace();
					value = Constants.UNDERSCORE;
				}
				values.add(value);
			}
			return values;
		}
		
		
		// Tested works well replace ? in String by its new values
		public String replaceValuesByMapping(ArrayList<String> values, String msg) {
			String finalMsg = Constants.EMPTY_STRING;
			for (int i = 0; i < values.size(); i++) {
				String flag = i+"?";
				if (i == 0) {
					finalMsg = msg.replace(flag, values.get(i));
				} else {
					finalMsg = finalMsg.replace(flag, values.get(i));
				}
			}
			return finalMsg;

		}
		
		private  double timeToMins(String s) {
			String[] hourMin = s.split(":");
			double hour = Integer.parseInt(hourMin[0]);
			double mins = Integer.parseInt(hourMin[1]);
			double hoursInMins = hour * 60;
			return hoursInMins + mins;
		}
		
		public String getDescriptionValue(JSONObject jsonObject , String locale) {
			if (locale.contains(Constants.LOCALE_AR)) {
				return jsonObject.get(Constants.JSON_KEY_ARABIC_DESCRIPTION_KEY).equals(null) ?Constants.SUBTITLE_VALUE : jsonObject.get(Constants.JSON_KEY_ARABIC_DESCRIPTION_KEY).toString();
			} else {
				return jsonObject.get(Constants.JSON_KEY_FOR_ENGLISH_DESCRIPTION).equals(null) ? Constants.SUBTITLE_VALUE : jsonObject.get(Constants.JSON_KEY_FOR_ENGLISH_DESCRIPTION).toString();
			}
		}
		
		public String getNameValue(JSONObject jsonObject , String locale) {
			if (locale.contains(Constants.LOCALE_AR)) {
				return jsonObject.getString(Constants.JSON_KEY_NAME_AR);
			} else {
				return jsonObject.getString(Constants.JSON_KEY_NAME_EN);
			}
		}
		
		
		
		public String getCategoryNameValue(JSONObject jsonObject , String locale) {
			if (locale.contains(Constants.LOCALE_AR)) {
				return jsonObject.getString(Constants.JSON_KEY_CATEGORY_NAME_AR);
			} else {
				return jsonObject.getString(Constants.JSON_KEY_CATEGORY_NAME_EN);
			}
		}
		
		public String getArabicOrEnglishValue(JSONObject jsonObject , String locale) {
			return locale.contains(Constants.LOCALE_AR) ? jsonObject.getString(Constants.JSON_KEY_VALUE_AR) :jsonObject.getString(Constants.JSON_KEY_VALUE_EN); 
		}
		
		public String getConsumptionNameValue(JSONObject jsonObject , String locale) {
			if(locale.contains(Constants.LOCALE_AR)) {
				return jsonObject.get(Constants.JSON_KEY_LABEL_AR).equals(null) ? Constants.UNDERSCORE : jsonObject.getString(Constants.JSON_KEY_LABEL_AR).toString();
			} else {
				
				return jsonObject.get(Constants.JSON_KEY_LABEL_EN).equals(null) ? Constants.UNDERSCORE : jsonObject.getString(Constants.JSON_KEY_LABEL_EN).toString();
			}
		}
		
		public String getRenewalDateValue(JSONObject jsonObject , String locale) {
			if(locale.contains(Constants.LOCALE_AR)) {
				return jsonObject.getString(Constants.JSON_KEY_VALUE_AR);
			} else {
				return jsonObject.getString(Constants.JSON_KEY_VALUE_EN);
			}
		}
		

}
