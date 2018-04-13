package com.chatbot.util;

import static java.util.Optional.empty;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

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
import com.chatbot.services.ChatBotService;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.MessagingType;
import com.github.messenger4j.send.Payload;
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

public class Utils {
	public static boolean isNotEmpty(String obj) {
		return obj != null && obj.length() != 0;
	}

	public static HttpMethod getHttpMethod(int httpMethodId) {
		switch (httpMethodId) {
			case 1:
				return HttpMethod.GET;

			case 2:
				return HttpMethod.POST;

			default:
				return HttpMethod.GET;
		}

	}

	public static MediaType getMediaType(Long mediaTypeId) {
		switch (mediaTypeId.intValue()) {
			case 1:
				return MediaType.APPLICATION_JSON;
			case 2:
				return MediaType.APPLICATION_XML;
			default:
				return MediaType.APPLICATION_JSON;
		}

	}

	
	
	public enum ButtonTypeEnum {
		START(1L),POSTBACK(2l), URL(3l),NESTED(4L),LOGIN(5L),LOGOUT(6L),CALL(7L);
		private final Long buttonTypeId;

		private ButtonTypeEnum(Long typeId) {
			this.buttonTypeId = typeId;
		}

		public Long getValue() {
			return buttonTypeId;
		}
	}
	
	
	// IN Case Zero Level JSONObject
	public static ArrayList<String> inCaseZeroLevelJsonObject(String [] keys ,JSONObject jsonObject , String msg){
		ArrayList<String> values = new ArrayList<String>();
		ArrayList<String> textMsgs = new ArrayList<String>();
			values =  getValuesFromJson(jsonObject, keys);	
			// replace Static message missed values  
		    String finalMsg = replaceValuesByMapping(values, msg);
		    textMsgs.add(finalMsg);
		return textMsgs;
		
	}
	
	// IN Case Zero Level JSONArray
	public static ArrayList<String> inCaseZeroLevelJsonArray(String [] keys , JSONArray rootArray , String msg){
		ArrayList<String> values = new ArrayList<String>();
		ArrayList<String> textMsgs = new ArrayList<String>();
		for(int i = 0 ; i < rootArray.length();i++) {
			
			try {
				JSONObject childObject = rootArray.getJSONObject(i);
				values = getValuesFromJson(childObject, keys);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// replace Static message missed values  
		    String finalMsg = replaceValuesByMapping(values, msg);
		    textMsgs.add(finalMsg);
		}
		return textMsgs;
		
	}
	
	// IN Case ONE Level JSONObject  
	public static ArrayList<String> inCaseOneLevelJsonObject(String [] paths , String [] keys ,JSONObject jsonObject , String msg){
		ArrayList<String> values = new ArrayList<String>();
		ArrayList<String> textMsgs = new ArrayList<String>();
		JSONArray firstLevelArray = new JSONArray();
		JSONObject firstLevelObject = new JSONObject();  
		if(paths[0].startsWith("#")) {
			paths[0] = paths[0].substring(1,paths[0].length());
			
			try {
				firstLevelArray = jsonObject.getJSONArray(paths[0]);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for (int i = 0; i < firstLevelArray.length(); i++) {
				try {
					firstLevelObject = firstLevelArray.getJSONObject(i);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			values = getValuesFromJson(firstLevelObject, keys);
			// replace Static message missed values  
		    String finalMsg = replaceValuesByMapping(values, msg);
		    textMsgs.add(finalMsg);
			}
		}else if(!paths[0].startsWith("#")) {
			try {
				firstLevelObject = jsonObject.getJSONObject(paths[0]);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			values = getValuesFromJson(jsonObject, keys);
			// replace Static message missed values  
		    String finalMsg = replaceValuesByMapping(values, msg);
		    textMsgs.add(finalMsg);
		}
		return textMsgs;
	}
	
		// IN Case ONE Level JSONOArray
		public static ArrayList<String> inCaseOneLevelJsonArray(String [] paths , String [] keys ,JSONArray rootArray,String msg){
			ArrayList<String> values = new ArrayList<String>();
			ArrayList<String> textMsgs = new ArrayList<String>();
			for (int i = 0; i < rootArray.length(); i++) {
				try {
					JSONObject childObject = rootArray.getJSONObject(i);
				if(paths[0].startsWith("#")) 
				{
					JSONArray childArray = childObject.getJSONArray(paths[0].substring(1,paths[0].length()));
					for(int j = 0 ; j < childArray.length() ; j++) {
						JSONObject subChild = childArray.getJSONObject(0);
						values = getValuesFromJson(subChild, keys);
						// replace Static message missed values  
					    String finalMsg = replaceValuesByMapping(values, msg);
					    textMsgs.add(finalMsg);
					}
				}else if(!paths[0].startsWith("#")) 
				{
					JSONObject child = childObject.getJSONObject(paths[0]);
					values = getValuesFromJson(child, keys);
					// replace Static message missed values  
				    String finalMsg = replaceValuesByMapping(values, msg);
				    textMsgs.add(finalMsg);
				}}catch (Exception e) {
					System.out.println(e.getStackTrace());
				}
			}
			
			return textMsgs;
		}
	
	
		public enum MessageTypeEnum {
			TEXTMESSAGE(1l), QUICKREPLYMESSAGE(2l), GENERICTEMPLATEMESSAGE(3l),ButtonTemplate(4l);
			private final Long messageTypeId;

			private MessageTypeEnum(Long messageTypeId) {
				this.messageTypeId = messageTypeId;
			}

			public Long getValue() {
				return messageTypeId;
			}
		}
	
	
	
		
	
	// Tested and works well get values from json object by its keys 
		public static  ArrayList<String> getValuesFromJson(JSONObject jsonObject, String[] keys) {
			ArrayList<String> values = new ArrayList<String>();
			for (int i = 0; i < keys.length; i++) {
				String key = keys[i];
				String value = "";
				try {
				if (key.contains(".")) {
					String jsonKey = key.substring(0,key.indexOf("."));
					String valueKey = key.substring(key.indexOf(".")+1,key.length());
					JSONObject finalObject = jsonObject.getJSONObject(jsonKey);
					value = finalObject.getString(valueKey);
				} else {
					value = jsonObject.getString(key);
				}
				values.add(value);
				}catch (Exception e) {
					System.out.println(e.getMessage());
				}}
			return values;
		}
	
	
		
		// Tested works well replace ?  in String by its new values 
		public static String replaceValuesByMapping(ArrayList<String> values, String staticMSg) {

			String messages ="";
			String finalMsg = "";
			for (int i = 0; i < values.size(); i++) {
				String flag = i + "?";
				if (i == 0) {
					finalMsg = staticMSg.replace(flag, values.get(i));
				} else {
					finalMsg = finalMsg.replace(flag, values.get(i));
				}
			}
			return finalMsg;

		}
	
		
		
		// In case Two Level JSONObject 
		public static ArrayList<String> inCaseTwoLevelJsonObject(JSONObject jsonObject  , String [] paths , String [] keys ,String msg){
			ArrayList<String> values = new ArrayList<String>();
			ArrayList<String> textMsgs = new ArrayList<String>();
  			JSONObject firstLevelObject = new JSONObject();
			JSONArray firstLevelArray = new JSONArray();
			String path = paths[0];
			if(path.startsWith("#")) {
				try {
				path = path.substring(1,path.length());
				firstLevelArray = jsonObject.getJSONArray(path);
				for(int i = 0 ;i < firstLevelArray.length();i++) {
					firstLevelObject = firstLevelArray.getJSONObject(i);
					if(paths[1].startsWith("#")) {
						String finalPath = paths[1].substring(1,paths[1].length());
						JSONArray finalJsonArray = firstLevelObject.getJSONArray(finalPath);
						for(int j = 0 ; j < finalJsonArray.length();j++) {
							JSONObject finalJsonObject = finalJsonArray.getJSONObject(j);
							// retrieve values from json
						    values = getValuesFromJson(finalJsonObject, keys);
						    // replace Static message missed values  
						    String finalMsg = replaceValuesByMapping(values, msg);
						    textMsgs.add(finalMsg);
						}
					}else if(!paths[1].startsWith("#")) {
						JSONObject firstParent = firstLevelObject.getJSONObject(paths[1]);
						values = getValuesFromJson(firstParent, keys);
						// replace Static message missed values  
					    String finalMsg = replaceValuesByMapping(values, msg);
					    textMsgs.add(finalMsg);
					}
			}}catch (Exception e) {
				System.out.println(e.getStackTrace());
			}
				// TODO: handle exception
			}else if(!path.startsWith("#")) {
				try {
				firstLevelObject = jsonObject.getJSONObject(path);
				if(paths[1].startsWith("#")) {
					JSONArray array = firstLevelObject.getJSONArray(paths[1].substring(1, paths[1].length()));
					for (int i = 0; i < array.length(); i++) {
						JSONObject tempObject = array.getJSONObject(i);
						values = getValuesFromJson(tempObject, keys);
						// replace Static message missed values  
					    String finalMsg = replaceValuesByMapping(values, msg);
					    textMsgs.add(finalMsg);
					}
				}else if(!paths[1].startsWith("#")) {
					JSONObject lastObject = firstLevelObject.getJSONObject(paths[1]);
					values = getValuesFromJson(lastObject, keys);
					// replace Static message missed values  
				    String finalMsg = replaceValuesByMapping(values, msg);
				    textMsgs.add(finalMsg);
				}}catch (Exception e) {
					System.out.println(e.getStackTrace());
				}
			}
			return textMsgs;
		}
		
		
		
		// In case Two Level JSONArray
		public static ArrayList<String> inCaseTwoLevelJsonArray(JSONArray rootArray , String [] paths , String [] keys ,String msg){
			ArrayList<String> values = new ArrayList<String>();
			ArrayList<String> textMsgs = new ArrayList<String>();
			for (int i = 0; i < rootArray.length(); i++) {
				try {
				JSONObject firstLevel = rootArray.getJSONObject(i);
				if(paths[0].startsWith("#")) 
				{
					JSONArray subArray = firstLevel.getJSONArray(paths[0].substring(1,paths[0].length()));
					for(int j = 0 ; j < subArray.length(); j++) 
					{
						JSONObject subObject = subArray.getJSONObject(j);
						if(paths[1].startsWith("#")) {
							JSONArray subChildArray = subObject.getJSONArray(paths[1].substring(1, paths[1].length()));
							for (int k = 0; k < subChildArray.length(); k++) {
								JSONObject subChildObject = subChildArray.getJSONObject(k);
									values = getValuesFromJson(subChildObject, keys);
									// replace Static message missed values  
								    String finalMsg = replaceValuesByMapping(values, msg);
								    textMsgs.add(finalMsg);
								}
						}else if(!paths[1].startsWith("#")) {
							 JSONObject childObject = subObject.getJSONObject(paths[1]);
							 values = getValuesFromJson(childObject, keys);
							// replace Static message missed values  
							    String finalMsg = replaceValuesByMapping(values, msg);
							    textMsgs.add(finalMsg);
						}
					}
				}else if (!paths[0].startsWith("#")) 
				{
					JSONObject firstLevelObject = firstLevel.getJSONObject(paths[0]);
					if(paths[1].startsWith("#")) {
						JSONArray secondLevelArray = firstLevelObject.getJSONArray(paths[1].substring(1, paths[1].length()));
						for(int j = 0 ; j < secondLevelArray.length() ; j++) {
							JSONObject subChildObject = secondLevelArray.getJSONObject(j);
							values = getValuesFromJson(subChildObject, keys);
							// replace Static message missed values  
						    String finalMsg = replaceValuesByMapping(values, msg);
						    textMsgs.add(finalMsg);
						}
					}else if(!paths[1].startsWith("#")) {
						JSONObject secondlevelObject = firstLevelObject.getJSONObject(paths[1]);
						values = getValuesFromJson(secondlevelObject, keys);
						// replace Static message missed values  
					    String finalMsg = replaceValuesByMapping(values, msg);
					    textMsgs.add(finalMsg);
					}
					
				}}catch(Exception e) {
				System.out.println(e.getStackTrace());	
				}	
			}
			
			return textMsgs;
		}
	
		// Method to get Path Details
		public static  String [] getPaths(String path) {
			String [] paths = new String[0];
			 if(path.contains(",")) {
				 paths = path.split(",");	
			}else if(path.length() > 1 && !path.contains(",")) {
			paths = new String [] {path};		
			}
			return  paths;
		}
		
		// Method to get keys
		public static String [] getKeys(String key ) {
			String [] keys = new String[0];
					 if(key.contains(",")) {
						 keys = key.split(",");
					 }
					else if(!key.contains(",")){
							 keys = new String [] {key};
					 }
					return keys;	 
				}
		
		// Switcher for JSONObject 
		public static ArrayList<String> switchToObjectMode(JSONObject jsonResponse , String [] paths , String [] keys , String msg) {
			ArrayList<String> values = new ArrayList<String>(); 
			int length = paths.length;
			if(length == 0) {
				 values = inCaseZeroLevelJsonObject(keys, jsonResponse, msg);
			 }else if(length == 1) {
				 values = inCaseOneLevelJsonObject(paths, keys, jsonResponse, msg);
			 }else if(length == 2) {
				    values = inCaseTwoLevelJsonObject(jsonResponse, paths, keys , msg);	    
			 }	
			return values;
		}
		
		// Switcher for JSONArray
		public static ArrayList<String> switchToArrayMode(JSONArray jsonResponse , String [] paths , String [] keys , String msg){
			ArrayList<String> values = new ArrayList<String>(); 
			int length = paths.length;
			if(length == 0) {
				 values = inCaseZeroLevelJsonArray(keys, jsonResponse, msg);	
				 }else if(length == 1) {
				 values = inCaseOneLevelJsonArray(paths, keys, jsonResponse, msg);
			 }else if(length == 2) {
				    values = inCaseTwoLevelJsonArray(jsonResponse, paths, keys, msg);	    
			 }	
			return values;
		}
		
		// Create Button
		public static  Button createButton(BotButton botButton , String local ,JSONObject jsonObject) {
			// PostBack
			if(botButton.getButtonType().getId() == Utils.ButtonTypeEnum.POSTBACK.getValue()) {
				String payload = botButton.getButtonPayload();
				String lable  = botButton.getBotText().getEnglishText();
 				if(payload.contains(".") && lable.contains(".")) {
					String finalPayLoad = getValuesFromJson(jsonObject, new String[] {payload}).get(0);
					String finalText = getValuesFromJson(jsonObject, new String[] {lable}).get(0);
					return PostbackButton.create(finalText, finalPayLoad);
				}
				return PostbackButton.create(getTextValue(local, botButton), botButton.getButtonPayload());
				// URl 	
			}else if(botButton.getButtonType().getId() == Utils.ButtonTypeEnum.URL.getValue()) {
				URL url = createUrl(botButton.getButtonImageUrl());
				return UrlButton.create(getTextValue(local, botButton), url);
			// Login	
			}else if(botButton.getButtonType().getId() == Utils.ButtonTypeEnum.LOGIN.getValue()) {
				URL url = createUrl(botButton.getButtonUrl());
				return LogInButton.create(url);
			// Logout	
			}else if(botButton.getButtonType().getId() == Utils.ButtonTypeEnum.LOGOUT.getValue()) {
				return LogOutButton.create();
			}
			return null;
		} 
		
		
		// Create Url Method 
		public static URL createUrl(String stringUrl) {
			URL url = null;
			try {
				 url =  new URL(stringUrl);
			} catch (MalformedURLException e) {
				
			}
			return url;
		}
	
		// Get Text Value
		public static String getTextValue(String local , BotButton botButton) {
			if(local.equalsIgnoreCase("ar")) {
				return botButton.getBotText().getArabicText();
			}else if(local.equalsIgnoreCase("en")) {
				return botButton.getBotText().getEnglishText();
			}
			return "";
		}
		
		
		
		
		/**
		 * @param messageId
		 * @param chatBotService
		 * @return
		 */
		public static Template createGenericTemplate(Long messageId , ChatBotService chatBotService) {
			BotGTemplateMessage botGTemplateMessage = chatBotService
					.findGTemplateMessageByMessageId(messageId);
			List<BotTemplateElement> botTemplateElementList = chatBotService
					.findTemplateElementsByGTMsgId(botGTemplateMessage.getGTMsgId());
			List<Element> elements = new ArrayList<>();
			for (BotTemplateElement botTemplateElement : botTemplateElementList) {
				List<BotButton> elementButtonList = chatBotService
						.findButtonsByTemplateElementId(botTemplateElement.getElementId());
				List<Button> buttonsList = new ArrayList<>();
				for (BotButton botButton : elementButtonList) {
					Button button = Utils.createButton(botButton, "en" , new JSONObject());
					buttonsList.add(button);
				}
				
			String elemnetImageUrl = "";
			Element element = null;
			elemnetImageUrl = botTemplateElement.getImageUrl();
			if(elemnetImageUrl.length()>1) {
				URL imageUrl = Utils.createUrl(elemnetImageUrl);
				element = Element.create(botTemplateElement.getTitle().getEnglishText(),
						Optional.of(botTemplateElement.getSubTitle().getEnglishText()),
						Optional.of(imageUrl), empty(),
						Optional.of(buttonsList));
			}else {
				element = Element.create(botTemplateElement.getTitle().getEnglishText(),
						Optional.of(botTemplateElement.getSubTitle().getEnglishText()),
						empty(), empty(),
						Optional.of(buttonsList));
			}
				elements.add(element);
				
			}
			 
			return GenericTemplate.create(elements);
		} 
		
		
		/**
		 * @param botInteractionMessage
		 * @param chatBotService
		 * @return
		 */
		public static ButtonTemplate createButtonTemplateInStaticScenario(BotInteractionMessage botInteractionMessage , ChatBotService chatBotService) {
			String title = "";
			ArrayList<Button> buttons = new ArrayList<Button>();
			List<BotButtonTemplateMSG> botButtonTemplateMSGs = 
					chatBotService.findBotButtonTemplateMSGByBotInteractionMessage(botInteractionMessage);
			for(BotButtonTemplateMSG botButtonTemplateMSG : botButtonTemplateMSGs) {
				title = botButtonTemplateMSG.getBotText().getEnglishText();
				List<BotButton> botButtons = chatBotService.findAllByBotButtonTemplateMSGId(botButtonTemplateMSG);
				for(BotButton botButton : botButtons) {
					Button button = Utils.createButton(botButton, "en", new JSONObject());
					buttons.add(button);
					}
			}
			return ButtonTemplate.create(title, buttons);
			 
		}
		
		
		/*
		 * 
		 */
		public static void changeBundle(String response , BotWebserviceMessage botWebserviceMessage ) {
			
			try {
				JSONObject jsonObject = new JSONObject(response);
			} catch (JSONException e) {
			} 
		}
		
		
		
		
		public static List<QuickReply> createQuickReply(ChatBotService chatBotService , Long messageId) {
			BotQuickReplyMessage botQuickReplyMessage = chatBotService
					.findQuickReplyMessageByMessageId(messageId);
			List<QuickReply> quickReplies = new ArrayList<>();
			List<BotButton> quickReplyButtonList = chatBotService
					.findButtonsByQuickReplyMessageId(botQuickReplyMessage.getQuickMsgId());
			QuickReply quickReply = null;
			for (BotButton botButton : quickReplyButtonList) {
				if (botButton.getButtonImageUrl() != null) {
					URL url = createUrl(botButton.getButtonImageUrl());
					quickReply = TextQuickReply.create(botButton.getBotText().getEnglishText(),
							botButton.getButtonPayload(),
							Optional.of(url));
				}else {
					quickReply = TextQuickReply.create(botButton.getBotText().getEnglishText(),
							botButton.getButtonPayload());
				}
				quickReplies.add(quickReply);
		}
		return quickReplies;
		}
		
		
		// This Method For call Parent MessagePayLoad again 
		public static MessagePayload createMessagePayload(String parentPayLoad , ChatBotService chatBotService , String senderId) {
			MessagePayload messagePayload = null;
			if(parentPayLoad != null) {
			BotInteraction parentBotInteraction = chatBotService.findInteractionByPayload(parentPayLoad);
			List<BotInteractionMessage> parentMsgs = chatBotService.
					findInteractionMessagesByInteractionId(parentBotInteraction.getInteractionId());
			for(BotInteractionMessage interactionMsg : parentMsgs ) {
				if(interactionMsg.getBotMessageType().getMessageTypeId() == Utils.MessageTypeEnum.GENERICTEMPLATEMESSAGE.getValue()) {
					Template template = Utils.createGenericTemplate(interactionMsg.getMessageId(), chatBotService);
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
		public static void getTextMessageIfResponseIsArray(String senderId, ArrayList<MessagePayload> messagePayloadList,
				BotWebserviceMessage botWebserviceMessage, String jsonBodyString ,ChatBotService chatBotService) throws JSONException {
			MessagePayload messagePayload;
			List<BotTextResponseMapping> botTextResponseMappings = chatBotService
					.findTextResponseMappingByWsId(botWebserviceMessage.getWsMsgId());
			JSONArray rootArray = new JSONArray(jsonBodyString);
			for (BotTextResponseMapping botTextResponseMapping : botTextResponseMappings) {
				String msg = botTextResponseMapping.getBotText().getEnglishText();
				String path = botTextResponseMapping.getCommonPath();
				String[] paths = Utils.getPaths(path);
				String keys = botTextResponseMapping.getEnParams();
				String[] keysArray = Utils.getKeys(keys);
				ArrayList<String> values = Utils.switchToArrayMode(rootArray, paths, keysArray,
						msg);
				for (String val : values) {
					System.out.println(val);
					messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE,
							TextMessage.create(val));
					messagePayloadList.add(messagePayload);
				}
			}
		}

		
		/**
		 * @param senderId
		 * @param messagePayloadList
		 * @param botWebserviceMessage
		 * @param jsonBodyString
		 * @throws JSONException
		 */
		public static void getTextMessageIfResponseIsObject(String senderId, ArrayList<MessagePayload> messagePayloadList,
				BotWebserviceMessage botWebserviceMessage, String jsonBodyString , ChatBotService chatBotService) throws JSONException {
			MessagePayload messagePayload;
			List<BotTextResponseMapping> botTextResponseMappings = chatBotService
					.findTextResponseMappingByWsId(botWebserviceMessage.getWsMsgId());
			JSONObject rootObject = new JSONObject(jsonBodyString);
			for (BotTextResponseMapping botTextResponseMapping : botTextResponseMappings) {
				String msg = botTextResponseMapping.getBotText().getEnglishText();
				String path = botTextResponseMapping.getCommonPath();
				String[] paths = Utils.getPaths(path);
				String keys = botTextResponseMapping.getEnParams();
				String[] keysArray = Utils.getKeys(keys);
				ArrayList<String> values = Utils.switchToObjectMode(rootObject, paths, keysArray,
						msg);
				for (String val : values) {
					messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE,
							TextMessage.create(val));
					messagePayloadList.add(messagePayload);
				}
			}
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
		public static MessagePayload responseInCaseStaticScenario(String payload, String senderId, String userFirstName,
				BotInteraction botInteraction,BotInteractionMessage botInteractionMessage, 
				Long messageTypeId, Long messageId , ChatBotService chatBotService ,String parentPayLoad) {
			String text;
			MessagePayload messagePayload = null;
			// text message
			if (messageTypeId == Utils.MessageTypeEnum.TEXTMESSAGE.getValue()) {
				BotTextMessage botTextMessage = chatBotService.findTextMessageByMessageId(messageId);
				parentPayLoad = botInteraction.getParentPayLoad();
				text = botTextMessage.getBotText().getEnglishText();
				if(payload.equalsIgnoreCase("welcome")) {
					text = text + " "+ userFirstName; 
				}
				messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE,
						TextMessage.create(text));
			}
			// quick reply
			else if (messageTypeId == Utils.MessageTypeEnum.QUICKREPLYMESSAGE.getValue()) {
				BotQuickReplyMessage botQuickReplyMessage = chatBotService
						.findQuickReplyMessageByMessageId(messageId);
				text = botQuickReplyMessage.getBotText().getEnglishText();
				/*List<QuickReply> quickReplies = new ArrayList<>();
				List<BotButton> quickReplyButtonList = chatBotService
						.findButtonsByQuickReplyMessageId(botQuickReplyMessage.getQuickMsgId());
				QuickReply quickReply = null;
				for (BotButton botButton : quickReplyButtonList) {
					if (botButton.getButtonImageUrl() != null)
						quickReply = TextQuickReply.create(botButton.getBotText().getEnglishText(),
								botButton.getButtonPayload(),
								Optional.of(new URL(botButton.getButtonImageUrl())));
					else
						quickReply = TextQuickReply.create(botButton.getBotText().getEnglishText(),
								botButton.getButtonPayload());

					quickReplies.add(quickReply);
				}

				String text = botQuickReplyMessage.getBotText().getEnglishText();*/
				List<QuickReply> quickReplies = Utils.createQuickReply(chatBotService, messageId);
				Optional<List<QuickReply>> quickRepliesOp = Optional.of(quickReplies);
				messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE,
						TextMessage.create(text, quickRepliesOp, empty()));
			}
			// generic template
			else if (messageTypeId == Utils.MessageTypeEnum.GENERICTEMPLATEMESSAGE.getValue()) {
				Template template = Utils.createGenericTemplate(messageId , chatBotService);
				messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE,
						TemplateMessage.create(template));
			// ButtonTemplate	
			}else if(messageTypeId == Utils.MessageTypeEnum.ButtonTemplate.getValue()) {
				ButtonTemplate buttonTemplate = Utils.createButtonTemplateInStaticScenario(botInteractionMessage , chatBotService);
				messagePayload = MessagePayload.create(senderId, MessagingType.RESPONSE,
						TemplateMessage.create(buttonTemplate));
			}
			return messagePayload;
		}
		
		
		
		
		
		/**
		 * call webService
		 * @return String response
		 * @param botWebserviceMessage
		 */
		public static String callWebService(BotWebserviceMessage botWebserviceMessage) {
			RestTemplate restTemplate = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(Utils.getMediaType(botWebserviceMessage.getContentType()));
			if (Utils.isNotEmpty(botWebserviceMessage.getHeaderParams())) {
				// split by comma separated to get all params then split by equal to get key and
				// value for
				// each param

				// Map<String, String> headersMap = restRepoDTO.getRequestHeaders().stream()
				// .collect(Collectors.toMap(RestRequestHeader::getRestHeaderName,
				// RestRequestHeader::getRestHeaderValue));
				//
				// headers.setAll(headersMap);
			}
			HttpEntity<String> entity = new HttpEntity<String>("", headers);
			// Get response
			ResponseEntity<String> response = restTemplate.exchange(botWebserviceMessage.getWsUrl(),
					Utils.getHttpMethod(botWebserviceMessage.getBotMethodType().getMethodTypeId()), entity,
					String.class);
			return response.getBody();
		}
		
		/**
		 * @param senderId
		 * @param messagePayloadList
		 * @param botWebserviceMessage
		 * @param jsonBodyString
		 * @throws JSONException
		 */
		public static void createTextMessageInDynamicScenario(String senderId, ArrayList<MessagePayload> messagePayloadList,
				BotWebserviceMessage botWebserviceMessage, String jsonBodyString , ChatBotService chatBotService) throws JSONException {
			// string
			if (botWebserviceMessage.getOutType().getInOutTypeId() == 1) {
				// Object
			} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 2) {
				Utils.getTextMessageIfResponseIsObject(senderId, messagePayloadList, botWebserviceMessage,
						jsonBodyString,chatBotService);
				//Array
			} else if (botWebserviceMessage.getOutType().getInOutTypeId() == 3) {
				Utils.getTextMessageIfResponseIsArray(senderId, messagePayloadList, botWebserviceMessage,
						jsonBodyString,chatBotService);
			}
		}

		
		
		public static String getResponse() {
			return "{ \n" + "   \"rateplan\":[ \n" + "      { \n" + "         \"productFees\":\"140.0\",\n"
					+ "         \"uniqueProductName\":\"Hero_Prepaid_Tariff_3\",\n" + "         \"commercialName\":{ \n"
					+ "            \"englishLabel\":\"My Rate Plan\",\n" + "            \"arabicLabel\":\"نظام خطي\",\n"
					+ "            \"arabicValue\":\"Hero Prepaid Tariff 3 AR NAME\",\n"
					+ "            \"englishValue\":\"COMMERCIAL Hero Prepaid Tariff 3\"\n" + "         },\n"
					+ "         \"renewalDate\":{ \n" + "            \"englishLabel\":\"Renewal date\",\n"
					+ "            \"arabicLabel\":\"صالحة حتى\",\n" + "            \"arabicValue\":\"-\",\n"
					+ "            \"englishValue\":\"-\"\n" + "         },\n" + "         \"daysLeft\":{ \n"
					+ "            \"englishLabel\":\"{0} Days left to renew\",\n"
					+ "            \"arabicLabel\":\"متبقي {0} يوم\",\n" + "            \"arabicValue\":\"-\",\n"
					+ "            \"englishValue\":\"-\"\n" + "         },\n" + "         \"productCategories\":[ \n"
					+ "            \"PORTAL_MBB\",\n" + "            \"PORTAL_RATEPLAN\"\n" + "         ],\n"
					+ "         \"consumptionDetails\":{ \n" + "            \"70\":{ \n" + "               \"total\":{ \n"
					+ "                  \"englishLabel\":\"Total\",\n" + "                  \"arabicLabel\":\"المجموع\",\n"
					+ "                  \"arabicValue\":\"0\",\n" + "                  \"englishValue\":\"0\",\n"
					+ "                  \"arabicUnit\":\"ميجا\",\n" + "                  \"englishUnit\":\"MB\",\n"
					+ "                  \"name\":\"INQUIRY_DATA_TOTAL_USAGE_OFF_PEAK\",\n"
					+ "                  \"value\":\"0\"\n" + "               },\n" + "               \"consumed\":{ \n"
					+ "                  \"englishLabel\":\"Consumed\",\n"
					+ "                  \"arabicLabel\":\"استهلاك\",\n" + "                  \"arabicValue\":\"0\",\n"
					+ "                  \"englishValue\":\"0\",\n" + "                  \"arabicUnit\":\"ميجا\",\n"
					+ "                  \"englishUnit\":\"MB\",\n"
					+ "                  \"bundleResourceID\":\"1000085\",\n"
					+ "                  \"englishName\":\"Mobile Broad Band\",\n"
					+ "                  \"name\":\"INQUIRY_DATA_CONSUMED_USAGE_OFF_PEAK\",\n"
					+ "                  \"type\":\"METER\",\n" + "                  \"unit\":\"Mobile Broad Band\",\n"
					+ "                  \"value\":\"0\"\n" + "               },\n" + "               \"remaining\":{ \n"
					+ "                  \"englishLabel\":\"Remaining\",\n"
					+ "                  \"arabicLabel\":\"المتبقى\",\n" + "                  \"arabicValue\":\"0\",\n"
					+ "                  \"englishValue\":\"0\",\n" + "                  \"arabicUnit\":\"ميجا\",\n"
					+ "                  \"englishUnit\":\"MB\",\n"
					+ "                  \"name\":\"INQUIRY_DATA_REMAINING_USAGE_OFF_PEAK\",\n"
					+ "                  \"value\":\"0\"\n" + "               },\n"
					+ "               \"quotaInFees\":null,\n" + "               \"percentage\":null,\n"
					+ "               \"consumptionName\":{ \n" + "                  \"arabicLabel\":null,\n"
					+ "                  \"englishLabel\":2333333\n" + "               },\n"
					+ "               \"consumedPercentage\":\"0\",\n"
					+ "               \"consumedPercentageFraction\":\"0.0\",\n"
					+ "               \"consumptionType\":\"DATA\",\n" + "               \"order\":null,\n"
					+ "               \"meterType\":null,\n" + "               \"socialConsumption\":false,\n"
					+ "              \"streamingConsumption\":false,\n" + "               \"voiceConsumption\":false,\n"
					+ "               \"css\":null,\n" + "               \"bundleResourceID\":null,\n"
					+ "               \"bundleType\":null\n" + "            },\n" + "            \"71\":{ \n"
					+ "               \"total\":{ \n" + "                  \"englishLabel\":\"Total\",\n"
					+ "                  \"arabicLabel\":\"المجموع\",\n" + "                  \"arabicValue\":\"0\",\n"
					+ "                  \"englishValue\":\"0\",\n" + "                  \"arabicUnit\":\"ميجا\",\n"
					+ "                  \"englishUnit\":\"MB\",\n"
					+ "                  \"name\":\"INQUIRY_DATA_TOTAL_USAGE_ALL_DAY\",\n"
					+ "                  \"value\":\"0\"\n" + "               },\n" + "               \"consumed\":{ \n"
					+ "                  \"englishLabel\":\"Consumed\",\n"
					+ "                  \"arabicLabel\":\"استهلاك\",\n" + "                  \"arabicValue\":\"0\",\n"
					+ "                  \"englishValue\":\"0\",\n" + "                  \"arabicUnit\":\"ميجا\",\n"
					+ "                  \"englishUnit\":\"MB\",\n"
					+ "                  \"bundleResourceID\":\"1000085\",\n"
					+ "                  \"englishName\":\"Mobile Broad Band\",\n"
					+ "                  \"name\":\"INQUIRY_DATA_CONSUMED_USAGE_ALL_DAY\",\n"
					+ "                  \"type\":\"METER\",\n" + "                  \"unit\":\"Mobile Broad Band\",\n"
					+ "                  \"value\":\"0\"\n" + "               },\n" + "               \"remaining\":{ \n"
					+ "                  \"englishLabel\":\"Remaining\",\n"
					+ "                  \"arabicLabel\":\"المتبقى\",\n" + "                  \"arabicValue\":\"0\",\n"
					+ "                  \"englishValue\":\"0\",\n" + "                  \"arabicUnit\":\"ميجا\",\n"
					+ "                  \"englishUnit\":\"MB\",\n"
					+ "                  \"name\":\"INQUIRY_DATA_REMAINING_USAGE_ALL_DAY\",\n"
					+ "                  \"value\":\"0\"\n" + "               },\n"
					+ "               \"quotaInFees\":null,\n" + "               \"percentage\":null,\n"
					+ "               \"consumptionName\":{ \n" + "                  \"arabicLabel\":null,\n"
					+ "                  \"englishLabel\":Minute TO Other\n" + "               },\n"
					+ "               \"consumedPercentage\":\"0\",\n"
					+ "               \"consumedPercentageFraction\":\"0.0\",\n"
					+ "               \"consumptionType\":\"DATA\",\n" + "               \"order\":null,\n"
					+ "               \"meterType\":null,\n" + "               \"socialConsumption\":false,\n"
					+ "               \"streamingConsumption\":false,\n" + "               \"voiceConsumption\":false,\n"
					+ "               \"css\":null,\n" + "               \"bundleResourceID\":null,\n"
					+ "               \"bundleType\":null\n" + "            }\n" + "         },\n"
					+ "         \"cssClassName\":\"green-bar\",\n" + "         \"index\":1,\n"
					+ "         \"newMIConnect\":false,\n" + "        \"bazingaRateplan\":false,\n"
					+ "         \"productRelationshipsList\":[ \n" + " \n" + "         ],\n"
					+ "         \"operationResponses\":[ \n" + "            { \n"
					+ "               \"arabicDescription\":null,\n" + "               \"arabicName\":null,\n"
					+ "               \"bundleResourceID\":null,\n" + "               \"englishDescription\":null,\n"
					+ "               \"englishName\":null,\n" + "               \"operationName\":\"REMOVE_CHILD\",\n"
					+ "               \"operationType\":null,\n" + "               \"order\":null,\n"
					+ "               \"suggestedMethod\":null,\n" + "               \"group\":null\n" + "            },\n"
					+ "            { \n" + "               \"arabicDescription\":null,\n"
					+ "               \"arabicName\":null,\n" + "               \"bundleResourceID\":null,\n"
					+ "               \"englishDescription\":null,\n" + "               \"englishName\":null,\n"
					+ "               \"operationName\":\"ADD_CHILD\",\n" + "               \"operationType\":null,\n"
					+ "               \"order\":null,\n" + "               \"suggestedMethod\":null,\n"
					+ "               \"group\":null\n" + "            },\n" + "            { \n"
					+ "               \"arabicDescription\":null,\n" + "               \"arabicName\":null,\n"
					+ "               \"bundleResourceID\":null,\n" + "               \"englishDescription\":null,\n"
					+ "               \"englishName\":null,\n" + "               \"operationName\":\"BUY_EXTRA_ADDONS\",\n"
					+ "               \"operationType\":\"DUMMY\",\n" + "               \"order\":null,\n"
					+ "               \"suggestedMethod\":null,\n" + "               \"group\":null\n" + "            },\n"
					+ "            { \n" + "               \"arabicDescription\":null,\n"
					+ "               \"arabicName\":null,\n" + "               \"bundleResourceID\":null,\n"
					+ "               \"englishDescription\":null,\n" + "               \"englishName\":null,\n"
					+ "               \"operationName\":\"MANAGE_CHILD\",\n"
					+ "               \"operationType\":\"DUMMY\",\n" + "               \"order\":null,\n"
					+ "               \"suggestedMethod\":null,\n" + "               \"group\":null\n" + "            },\n"
					+ "            { \n" + "               \"arabicDescription\":null,\n"
					+ "               \"arabicName\":null,\n" + "               \"bundleResourceID\":null,\n"
					+ "               \"englishDescription\":null,\n" + "               \"englishName\":null,\n"
					+ "               \"operationName\":\"PAY_BILL\",\n" + "               \"operationType\":\"DUMMY\",\n"
					+ "               \"order\":null,\n" + "               \"suggestedMethod\":null,\n"
					+ "               \"group\":null\n" + "            },\n" + "            { \n"
					+ "               \"arabicDescription\":null,\n" + "               \"arabicName\":null,\n"
					+ "               \"bundleResourceID\":null,\n" + "               \"englishDescription\":null,\n"
					+ "               \"englishName\":null,\n" + "               \"operationName\":\"RECHARGE\",\n"
					+ "               \"operationType\":\"DUMMY\",\n" + "               \"order\":null,\n"
					+ "               \"suggestedMethod\":null,\n" + "               \"group\":null\n" + "            },\n"
					+ "            { \n" + "               \"arabicDescription\":null,\n"
					+ "               \"arabicName\":null,\n" + "               \"bundleResourceID\":null,\n"
					+ "               \"englishDescription\":null,\n" + "               \"englishName\":null,\n"
					+ "               \"operationName\":\"MIGRATE\",\n" + "               \"operationType\":null,\n"
					+ "               \"order\":null,\n" + "               \"suggestedMethod\":null,\n"
					+ "               \"group\":null\n" + "            }\n" + "         ],\n"
					+ "         \"familyParent\":false,\n" + "         \"fafList\":null,\n"
					+ "         \"encFafList\":null,\n" + "         \"productSuspended\":false,\n"
					+ "         \"giftRedeemed\":false,\n" + "         \"unsubscribeAddons\":false,\n"
					+ "         \"freeze\":null,\n" + "         \"cyber\":false,\n" + "         \"offerType\":null,\n"
					+ "         \"timeExpire\":null,\n" + "         \"remainingAdslAddonQuota\":null,\n"
					+ "         \"remainingAdslAddonUnit\":null,\n" + "         \"adslQuotaAddon\":false,\n"
					+ "         \"consumptionDummy\":[ \n" + "            { \n" + "               \"total\":{ \n"
					+ "                  \"englishLabel\":\"Total\",\n" + "                  \"arabicLabel\":\"المجموع\",\n"
					+ "                  \"arabicValue\":\"0\",\n" + "                  \"englishValue\":\"0\",\n"
					+ "                  \"arabicUnit\":\"ميجا\",\n" + "                  \"englishUnit\":\"MB\",\n"
					+ "                  \"name\":\"INQUIRY_DATA_TOTAL_USAGE_ALL_DAY\",\n"
					+ "                  \"value\":\"0\"\n" + "               },\n" + "               \"consumed\":{ \n"
					+ "                  \"englishLabel\":\"Consumed\",\n"
					+ "                  \"arabicLabel\":\"استهلاك\",\n" + "                  \"arabicValue\":\"0\",\n"
					+ "                  \"englishValue\":\"0\",\n" + "                  \"arabicUnit\":\"ميجا\",\n"
					+ "                  \"englishUnit\":\"MB\",\n"
					+ "                  \"bundleResourceID\":\"1000085\",\n"
					+ "                  \"englishName\":\"Mobile Broad Band\",\n"
					+ "                  \"name\":\"INQUIRY_DATA_CONSUMED_USAGE_ALL_DAY\",\n"
					+ "                  \"type\":\"METER\",\n" + "                  \"unit\":\"Mobile Broad Band\",\n"
					+ "                  \"value\":\"0\"\n" + "               },\n" + "               \"remaining\":{ \n"
					+ "                  \"englishLabel\":\"Remaining\",\n"
					+ "                  \"arabicLabel\":\"المتبقى\",\n" + "                  \"arabicValue\":\"0\",\n"
					+ "                  \"englishValue\":\"0\",\n" + "                  \"arabicUnit\":\"ميجا\",\n"
					+ "                  \"englishUnit\":\"MB\",\n"
					+ "                  \"name\":\"INQUIRY_DATA_REMAINING_USAGE_ALL_DAY\",\n"
					+ "                  \"value\":\"0\"\n" + "               },\n"
					+ "               \"quotaInFees\":null,\n" + "               \"percentage\":null,\n"
					+ "               \"consumptionName\":{ \n" + "                  \"arabicLabel\":null,\n"
					+ "                  \"englishLabel\":Minute to Others\n" + "               },\n"
					+ "               \"consumedPercentage\":\"0\",\n"
					+ "               \"consumedPercentageFraction\":\"0.0\",\n"
					+ "               \"consumptionType\":\"DATA\",\n" + "               \"order\":null,\n"
					+ "               \"meterType\":null,\n" + "               \"socialConsumption\":false,\n"
					+ "               \"streamingConsumption\":false,\n" + "               \"voiceConsumption\":false,\n"
					+ "               \"css\":null,\n" + "               \"bundleResourceID\":null,\n"
					+ "               \"bundleType\":null\n" + "            },\n" + "            { \n"
					+ "               \"total\":{ \n" + "                  \"englishLabel\":\"Total\",\n"
					+ "                  \"arabicLabel\":\"المجموع\",\n" + "                  \"arabicValue\":\"0\",\n"
					+ "                  \"englishValue\":\"0\",\n" + "                  \"arabicUnit\":\"ميجا\",\n"
					+ "                  \"englishUnit\":\"MB\",\n"
					+ "                  \"name\":\"INQUIRY_DATA_TOTAL_USAGE_OFF_PEAK\",\n"
					+ "                  \"value\":\"0\"\n" + "               },\n" + "               \"consumed\":{ \n"
					+ "                  \"englishLabel\":\"Consumed\",\n"
					+ "                  \"arabicLabel\":\"استهلاك\",\n" + "                  \"arabicValue\":\"0\",\n"
					+ "                  \"englishValue\":\"0\",\n" + "                  \"arabicUnit\":\"ميجا\",\n"
					+ "                  \"englishUnit\":\"MB\",\n"
					+ "                  \"bundleResourceID\":\"1000085\",\n"
					+ "                  \"englishName\":\"Mobile Broad Band\",\n"
					+ "                  \"name\":\"INQUIRY_DATA_CONSUMED_USAGE_OFF_PEAK\",\n"
					+ "                  \"type\":\"METER\",\n" + "                  \"unit\":\"Mobile Broad Band\",\n"
					+ "                  \"value\":\"0\"\n" + "               },\n" + "               \"remaining\":{ \n"
					+ "                  \"englishLabel\":\"Remaining\",\n"
					+ "                  \"arabicLabel\":\"المتبقى\",\n" + "                  \"arabicValue\":\"0\",\n"
					+ "                  \"englishValue\":\"0\",\n" + "                  \"arabicUnit\":\"ميجا\",\n"
					+ "                  \"englishUnit\":\"MB\",\n"
					+ "                  \"name\":\"INQUIRY_DATA_REMAINING_USAGE_OFF_PEAK\",\n"
					+ "                  \"value\":\"0\"\n" + "               },\n"
					+ "               \"quotaInFees\":null,\n" + "               \"percentage\":null,\n"
					+ "               \"consumptionName\":{ \n" + "                  \"arabicLabel\":null,\n"
					+ "                  \"englishLabel\":MInutes \n" + "               },\n"
					+ "               \"consumedPercentage\":\"0\",\n"
					+ "               \"consumedPercentageFraction\":\"0.0\",\n"
					+ "               \"consumptionType\":\"DATA\",\n" + "               \"order\":null,\n"
					+ "               \"meterType\":null,\n" + "               \"socialConsumption\":false,\n"
					+ "               \"streamingConsumption\":false,\n" + "               \"voiceConsumption\":false,\n"
					+ "               \"css\":null,\n" + "               \"bundleResourceID\":null,\n"
					+ "               \"bundleType\":null\n" + "            }\n" + "         ],\n"
					+ "         \"consumptionDetailsList\":[ \n" + "            { \n" + "               \"total\":{ \n"
					+ "                  \"englishLabel\":\"Total\",\n" + "                  \"arabicLabel\":\"المجموع\",\n"
					+ "                  \"arabicValue\":\"0\",\n" + "                  \"englishValue\":\"300\",\n"
					+ "                  \"arabicUnit\":\"ميجا\",\n" + "                  \"englishUnit\":\"MB\",\n"
					+ "                  \"name\":\"INQUIRY_DATA_TOTAL_USAGE_ALL_DAY\",\n"
					+ "                  \"value\":\"0\"\n" + "               },\n" + "               \"consumed\":{ \n"
					+ "                  \"englishLabel\":\"Consumed\",\n"
					+ "                  \"arabicLabel\":\"استهلاك\",\n" + "                  \"arabicValue\":\"0\",\n"
					+ "                  \"englishValue\":\"200\",\n" + "                  \"arabicUnit\":\"ميجا\",\n"
					+ "                  \"englishUnit\":\"MB\",\n"
					+ "                  \"bundleResourceID\":\"1000085\",\n"
					+ "                  \"englishName\":\"Mobile Broad Band\",\n"
					+ "                  \"name\":\"INQUIRY_DATA_CONSUMED_USAGE_ALL_DAY\",\n"
					+ "                  \"type\":\"METER\",\n" + "                  \"unit\":\"Mobile Broad Band\",\n"
					+ "                  \"value\":\"0\"\n" + "               },\n" + "               \"remaining\":{ \n"
					+ "                  \"englishLabel\":\"Remaining\",\n"
					+ "                  \"arabicLabel\":\"المتبقى\",\n" + "                  \"arabicValue\":\"0\",\n"
					+ "                  \"englishValue\":\"100\",\n" + "                  \"arabicUnit\":\"ميجا\",\n"
					+ "                  \"englishUnit\":\"MB\",\n"
					+ "                  \"name\":\"INQUIRY_DATA_REMAINING_USAGE_ALL_DAY\",\n"
					+ "                  \"value\":\"0\"\n" + "               },\n"
					+ "               \"quotaInFees\":null,\n" + "               \"percentage\":null,\n"
					+ "               \"consumptionName\":{ \n" + "                  \"arabicLabel\":null,\n"
					+ "                  \"englishLabel\":Hours\n" + "               },\n"
					+ "               \"consumedPercentage\":\"0\",\n"
					+ "               \"consumedPercentageFraction\":\"0.0\",\n"
					+ "               \"consumptionType\":\"DATA\",\n" + "               \"order\":null,\n"
					+ "               \"meterType\":null,\n" + "               \"socialConsumption\":false,\n"
					+ "               \"streamingConsumption\":false,\n" + "               \"voiceConsumption\":false,\n"
					+ "               \"css\":null,\n" + "               \"bundleResourceID\":null,\n"
					+ "               \"bundleType\":null\n" + "            },\n" + "            { \n"
					+ "               \"total\":{ \n" + "                  \"englishLabel\":\"Total\",\n"
					+ "                  \"arabicLabel\":\"المجموع\",\n" + "                  \"arabicValue\":\"0\",\n"
					+ "                  \"englishValue\":\"300\",\n" + "                  \"arabicUnit\":\"ميجا\",\n"
					+ "                  \"englishUnit\":\"MB\",\n"
					+ "                  \"name\":\"INQUIRY_DATA_TOTAL_USAGE_OFF_PEAK\",\n"
					+ "                  \"value\":\"0\"\n" + "               },\n" + "               \"consumed\":{ \n"
					+ "                  \"englishLabel\":\"Consumed\",\n"
					+ "                  \"arabicLabel\":\"استهلاك\",\n" + "                  \"arabicValue\":\"0\",\n"
					+ "                  \"englishValue\":\"140\",\n" + "                  \"arabicUnit\":\"ميجا\",\n"
					+ "                  \"englishUnit\":\"MB\",\n"
					+ "                  \"bundleResourceID\":\"1000085\",\n"
					+ "                  \"englishName\":\"Mobile Broad Band\",\n"
					+ "                  \"name\":\"INQUIRY_DATA_CONSUMED_USAGE_OFF_PEAK\",\n"
					+ "                  \"type\":\"METER\",\n" + "                  \"unit\":\"Mobile Broad Band\",\n"
					+ "                  \"value\":\"0\"\n" + "               },\n" + "               \"remaining\":{ \n"
					+ "                  \"englishLabel\":\"Remaining\",\n"
					+ "                  \"arabicLabel\":\"المتبقى\",\n" + "                  \"arabicValue\":\"0\",\n"
					+ "                  \"englishValue\":\"160\",\n" + "                  \"arabicUnit\":\"ميجا\",\n"
					+ "                  \"englishUnit\":\"MB\",\n"
					+ "                  \"name\":\"INQUIRY_DATA_REMAINING_USAGE_OFF_PEAK\",\n"
					+ "                  \"value\":\"0\"\n" + "               },\n"
					+ "               \"quotaInFees\":null,\n" + "               \"percentage\":null,\n"
					+ "               \"consumptionName\":{ \n" + "                  \"arabicLabel\":null,\n"
					+ "                  \"englishLabel\":Day\n" + "               },\n"
					+ "               \"consumedPercentage\":\"0\",\n"
					+ "               \"consumedPercentageFraction\":\"0.0\",\n"
					+ "               \"consumptionType\":\"DATA\",\n" + "               \"order\":null,\n"
					+ "               \"meterType\":null,\n" + "               \"socialConsumption\":false,\n"
					+ "               \"streamingConsumption\":false,\n" + "               \"voiceConsumption\":false,\n"
					+ "               \"css\":null,\n" + "               \"bundleResourceID\":null,\n"
					+ "               \"bundleType\":null\n" + "            }\n" + "         ]\n" + "      }\n" + "   ],\n"
					+ "   \"connect\":[ \n" + " \n" + "   ],\n" + "   \"addons\":[ \n" + " \n" + "   ],\n"
					+ "   \"ratePlanAddons\":[ \n" + " \n" + "   ],\n" + "   \"payAsYouGo\":[ \n" + " \n" + "   ],\n"
					+ "   \"voiceExtra\":[ \n" + " \n" + "   ],\n" + "   \"billingProfileModel\":{ \n"
					+ "      \"amount\":0.0,\n" + "      \"openAmount\":0.0,\n" + "      \"billDate\":null,\n"
					+ "      \"inAdvanceAmount\":0.0,\n" + "      \"customerCode\":null\n" + "   },\n"
					+ "   \"postPaid\":false,\n" + "   \"msisdn\":null,\n" + "   \"balance\":{ \n"
					+ "      \"englishLabel\":\"My Balance\",\n" + "      \"arabicLabel\":\"رصيدك الحالي\",\n"
					+ "      \"arabicValue\":\"0.0\",\n" + "      \"englishValue\":\"0.0\",\n"
					+ "      \"arabicUnit\":\"جنيه\",\n" + "      \"englishUnit\":\"EGP\"\n" + "   },\n"
					+ "   \"paramChannel\":null,\n" + "   \"productOperationModel\":null,\n" + "   \"halloween\":{ \n"
					+ "      \"render\":false\n" + "   },\n" + "   \"ratePlanConsumptionRender\":true,\n"
					+ "   \"connectConsumptionRender\":true,\n" + "   \"lteExtra\":{ \n"
					+ "      \"uniqueProductName\":\"LTE_OFFER_OPT_IN\",\n" + "      \"render\":true,\n"
					+ "      \"lteqrchanelSuccess\":false\n" + "   },\n" + "   \"flatResponse\":{ \n"
					+ "      \"limitValue\":null,\n" + "      \"uniqueProductName\":null,\n"
					+ "      \"preferredNumber\":null\n" + "   },\n" + "   \"menuelnetSectionRender\":true,\n"
					+ "   \"connectActionsList\":[ \n" + " \n" + "   ],\n" + "   \"paygActionsList\":[ \n" + " \n"
					+ "   ],\n" + "   \"rateplanActionsList\":[ \n" + "      [ \n" + "         { \n"
					+ "            \"name\":\"change_tariff_key\",\n" + "            \"order\":\"2\",\n"
					+ "            \"param\":\"\",\n" + "            \"nextUrl\":\"migration\",\n"
					+ "            \"css\":\"icon-change\",\n" + "            \"operationName\":\"\",\n"
					+ "            \"active\":false,\n" + "            \"serviceName\":\"\",\n"
					+ "            \"fullUrl\":true\n" + "         }\n" + "      ]\n" + "   ],\n"
					+ "   \"fullUsageActionsList\":null,\n" + "   \"fullMIUsageActionsList\":[ \n" + " \n" + "   ],\n"
					+ "   \"fullDataUsageActionsList\":null,\n" + "   \"familyResponse\":{ \n"
					+ "      \"settlementAmount\":null,\n" + "      \"borrowMaxAmount\":null,\n"
					+ "      \"uniqueProductName\":null,\n" + "      \"hasChilds\":false\n" + "   },\n"
					+ "   \"connectResponse\":{ \n" + "      \"uniqueProductName\":null,\n"
					+ "      \"blockRORActive\":false,\n" + "      \"blockRORRenwableActive\":false,\n"
					+ "      \"blockROROnDemandActive\":false\n" + "   },\n" + "   \"dialTypeMI\":false,\n"
					+ "   \"dialTypeData\":false,\n" + "   \"dialTypeAdsl\":false\n" + "}";
		}
	
		
	
		
		public static List<String>readJsonFile() {
			StringBuffer response = new StringBuffer();
			String fileName = "webapp//Bundles.txt";
			List<String> list = new ArrayList<>();

			try (BufferedReader br = Files.newBufferedReader(Paths.get(fileName))) {

				//br returns as stream and convert it into a List
				list = br.lines().collect(Collectors.toList());

			} catch (IOException e) {
				e.printStackTrace();
			}
		
			return list;
		}
		
		public static StringBuffer getArrayRespo() {
			StringBuffer finalResponse = new StringBuffer();
			List<String> lines = readJsonFile(); 
			for(String st:lines) {
				finalResponse.append(st);
			}
			return finalResponse;
		}
		
		
		
		
		
		
		
		
		
		
		
		
		
		
	/*	public static String getArrayResponse() {
			
			return "[\n" + 
					"  {\n" + 
					"    \"category\": {\n" + 
					"      \"categoryNameAr\": \"باقات كونكت\",\n" + 
					"      \"categoryNameEn\": \"Connect Bundles\",\n" + 
					"      \"categoryId\": \"PORTAL_CONNECT_MONTHLY\",\n" + 
					"      \"offerId\": null\n" + 
					"    },\n" + 
					"    \"products\": [\n" + 
					"      {\n" + 
					"        \"id\": \"12732\",\n" + 
					"        \"fees\": \"15.0\",\n" + 
					"        \"active\": false,\n" + 
					"        \"isActive\": \"false\",\n" + 
					"        \"productRelationshipsList\": {},\n" + 
					"        \"operationResponses\": [\n" + 
					"          {\n" + 
					"            \"arabicDescription\": \"اشتراك\",\n" + 
					"            \"arabicName\": \"اشتراك\",\n" + 
					"            \"bundleResourceID\": \"1000200\",\n" + 
					"            \"englishDescription\": \"ACTIVATE\",\n" + 
					"            \"englishName\": \"ACTIVATE\",\n" + 
					"            \"operationName\": \"ACTIVATE\",\n" + 
					"            \"operationType\": null,\n" + 
					"            \"order\": null,\n" + 
					"            \"suggestedMethod\": null,\n" + 
					"            \"group\": null\n" + 
					"          }\n" + 
					"        ],\n" + 
					"        \"commNameEnglish\": null,\n" + 
					"        \"commNameArabic\": null,\n" + 
					"        \"addOn\": null,\n" + 
					"        \"newMIConnect\": false,\n" + 
					"        \"bazingaRateplan\": false,\n" + 
					"        \"extraSocial\": false,\n" + 
					"        \"extraStreaming\": false,\n" + 
					"        \"extraVoice\": false,\n" + 
					"        \"discountGift\": \"1\",\n" + 
					"        \"extraGift\": \"3\",\n" + 
					"        \"buyOneGift\": null,\n" + 
					"        \"doubleGift\": null,\n" + 
					"        \"discountGiftDescArabic\": null,\n" + 
					"        \"extraGiftDescArabic\": null,\n" + 
					"        \"buyOneGiftDescArabic\": null,\n" + 
					"        \"doubleGiftDescArabic\": null,\n" + 
					"        \"discountGiftDescEnglish\": null,\n" + 
					"        \"extraGiftDescEnglish\": null,\n" + 
					"        \"buyOneGiftDescEnglish\": null,\n" + 
					"        \"doubleGiftDescEnglish\": null,\n" + 
					"        \"giftFullfilmentParameterName\": null,\n" + 
					"        \"giftFullfilmentParameterValue\": null,\n" + 
					"        \"menuValidityAr\": null,\n" + 
					"        \"menuValidityEn\": null,\n" + 
					"        \"categoryCssName\": null,\n" + 
					"        \"hideProduct\": false,\n" + 
					"        \"productPrice\": null,\n" + 
					"        \"englishDescription\": \"Enjoy 500 MB with 15 EGP/ month\",\n" + 
					"        \"arabicDescription\": \"احصل على 500 ميجا ب 15 ج/الشهر \",\n" + 
					"        \"englishName\": \"Connect 15\",\n" + 
					"        \"arabicName\": \"كونكت 15\",\n" + 
					"        \"name\": \"Terra_New_Connect_15\",\n" + 
					"        \"relatedProduct\": [],\n" + 
					"        \"extraEnglishDescription\": \"1\",\n" + 
					"        \"extraArabicDescription\": \"1\"\n" + 
					"      },\n" + 
					"      {\n" + 
					"        \"id\": \"12731\",\n" + 
					"        \"fees\": \"15.0\",\n" + 
					"        \"active\": false,\n" + 
					"        \"isActive\": \"true\",\n" + 
					"        \"productRelationshipsList\": {},\n" + 
					"        \"operationResponses\": [\n" + 
					"          {\n" + 
					"            \"arabicDescription\": null,\n" + 
					"            \"arabicName\": null,\n" + 
					"            \"bundleResourceID\": null,\n" + 
					"            \"englishDescription\": null,\n" + 
					"            \"englishName\": null,\n" + 
					"            \"operationName\": \"DEACTIVATE\",\n" + 
					"            \"operationType\": null,\n" + 
					"            \"order\": null,\n" + 
					"            \"suggestedMethod\": null,\n" + 
					"            \"group\": null\n" + 
					"          }\n" + 
					"        ],\n" + 
					"        \"commNameEnglish\": null,\n" + 
					"        \"commNameArabic\": null,\n" + 
					"        \"addOn\": null,\n" + 
					"        \"newMIConnect\": false,\n" + 
					"        \"bazingaRateplan\": false,\n" + 
					"        \"extraSocial\": false,\n" + 
					"        \"extraStreaming\": false,\n" + 
					"        \"extraVoice\": false,\n" + 
					"        \"discountGift\": null,\n" + 
					"        \"extraGift\": null,\n" + 
					"        \"buyOneGift\": null,\n" + 
					"        \"doubleGift\": null,\n" + 
					"        \"discountGiftDescArabic\": null,\n" + 
					"        \"extraGiftDescArabic\": null,\n" + 
					"        \"buyOneGiftDescArabic\": null,\n" + 
					"        \"doubleGiftDescArabic\": null,\n" + 
					"        \"discountGiftDescEnglish\": null,\n" + 
					"        \"extraGiftDescEnglish\": null,\n" + 
					"        \"buyOneGiftDescEnglish\": null,\n" + 
					"        \"doubleGiftDescEnglish\": null,\n" + 
					"        \"giftFullfilmentParameterName\": null,\n" + 
					"        \"giftFullfilmentParameterValue\": null,\n" + 
					"        \"menuValidityAr\": null,\n" + 
					"        \"menuValidityEn\": null,\n" + 
					"        \"categoryCssName\": null,\n" + 
					"        \"hideProduct\": false,\n" + 
					"        \"productPrice\": null,\n" + 
					"        \"englishDescription\": \"Get unlimited social (facebook,whatsapp,instagram&twitter) + 100 MB \",\n" + 
					"        \"arabicDescription\": \"احصل على سوشيال بلاحدود (فيسبوك ،واتس أب،انستجرام وتويتر) + 100 ميجا اضافيه   \",\n" + 
					"        \"englishName\": \"super Social Unlimited\",\n" + 
					"        \"arabicName\": \"Super Social Unlimited\",\n" + 
					"        \"name\": \"CONNECT_UNLIMITED_15_PREPAID\",\n" + 
					"        \"relatedProduct\": [],\n" + 
					"        \"extraEnglishDescription\": null,\n" + 
					"        \"extraArabicDescription\": null\n" + 
					"      },\n" + 
					"      {\n" + 
					"        \"id\": \"13707\",\n" + 
					"        \"fees\": \"25.0\",\n" + 
					"        \"active\": false,\n" + 
					"        \"isActive\": \"false\",\n" + 
					"        \"productRelationshipsList\": {},\n" + 
					"        \"operationResponses\": [\n" + 
					"          {\n" + 
					"            \"arabicDescription\": \"اشتراك\",\n" + 
					"            \"arabicName\": \"اشتراك\",\n" + 
					"            \"bundleResourceID\": \"1000200\",\n" + 
					"            \"englishDescription\": \"ACTIVATE\",\n" + 
					"            \"englishName\": \"ACTIVATE\",\n" + 
					"            \"operationName\": \"ACTIVATE\",\n" + 
					"            \"operationType\": null,\n" + 
					"            \"order\": null,\n" + 
					"            \"suggestedMethod\": null,\n" + 
					"            \"group\": null\n" + 
					"          }\n" + 
					"        ],\n" + 
					"        \"commNameEnglish\": null,\n" + 
					"        \"commNameArabic\": null,\n" + 
					"        \"addOn\": null,\n" + 
					"        \"newMIConnect\": false,\n" + 
					"        \"bazingaRateplan\": false,\n" + 
					"        \"extraSocial\": false,\n" + 
					"        \"extraStreaming\": false,\n" + 
					"        \"extraVoice\": false,\n" + 
					"        \"discountGift\": null,\n" + 
					"        \"extraGift\": null,\n" + 
					"        \"buyOneGift\": null,\n" + 
					"        \"doubleGift\": null,\n" + 
					"        \"discountGiftDescArabic\": null,\n" + 
					"        \"extraGiftDescArabic\": null,\n" + 
					"        \"buyOneGiftDescArabic\": null,\n" + 
					"        \"doubleGiftDescArabic\": null,\n" + 
					"        \"discountGiftDescEnglish\": null,\n" + 
					"        \"extraGiftDescEnglish\": null,\n" + 
					"        \"buyOneGiftDescEnglish\": null,\n" + 
					"        \"doubleGiftDescEnglish\": null,\n" + 
					"        \"giftFullfilmentParameterName\": null,\n" + 
					"        \"giftFullfilmentParameterValue\": null,\n" + 
					"        \"menuValidityAr\": null,\n" + 
					"        \"menuValidityEn\": null,\n" + 
					"        \"categoryCssName\": null,\n" + 
					"        \"hideProduct\": false,\n" + 
					"        \"productPrice\": null,\n" + 
					"        \"englishDescription\": \"Get 350MB for 7LE/ week with high speed for all websites\",\n" + 
					"        \"arabicDescription\": \"احصل علي 350 ميجا ب 7ج/ الاسبوع بالسرعة القصوى لكل المواقع\",\n" + 
					"        \"englishName\": \"Weekly bundle 7 LE\",\n" + 
					"        \"arabicName\": \"باقة الاسبوع 7 ج\",\n" + 
					"        \"name\": \"CONNECT_NEW_CONNECT_BUNDLE2\",\n" + 
					"        \"relatedProduct\": [],\n" + 
					"        \"extraEnglishDescription\": null,\n" + 
					"        \"extraArabicDescription\": null\n" + 
					"      },\n" + 
					"      {\n" + 
					"        \"id\": \"835\",\n" + 
					"        \"fees\": \"30.0\",\n" + 
					"        \"active\": false,\n" + 
					"        \"isActive\": \"false\",\n" + 
					"        \"productRelationshipsList\": {},\n" + 
					"        \"operationResponses\": [\n" + 
					"          {\n" + 
					"            \"arabicDescription\": \"اشتراك\",\n" + 
					"            \"arabicName\": \"اشتراك\",\n" + 
					"            \"bundleResourceID\": \"1000200\",\n" + 
					"            \"englishDescription\": \"ACTIVATE\",\n" + 
					"            \"englishName\": \"ACTIVATE\",\n" + 
					"            \"operationName\": \"ACTIVATE\",\n" + 
					"            \"operationType\": null,\n" + 
					"            \"order\": null,\n" + 
					"            \"suggestedMethod\": null,\n" + 
					"            \"group\": null\n" + 
					"          }\n" + 
					"        ],\n" + 
					"        \"commNameEnglish\": null,\n" + 
					"        \"commNameArabic\": null,\n" + 
					"        \"addOn\": null,\n" + 
					"        \"newMIConnect\": false,\n" + 
					"        \"bazingaRateplan\": false,\n" + 
					"        \"extraSocial\": false,\n" + 
					"        \"extraStreaming\": false,\n" + 
					"        \"extraVoice\": false,\n" + 
					"        \"discountGift\": \"1\",\n" + 
					"        \"extraGift\": \"3\",\n" + 
					"        \"buyOneGift\": null,\n" + 
					"        \"doubleGift\": \"3\",\n" + 
					"        \"discountGiftDescArabic\": null,\n" + 
					"        \"extraGiftDescArabic\": null,\n" + 
					"        \"buyOneGiftDescArabic\": null,\n" + 
					"        \"doubleGiftDescArabic\": null,\n" + 
					"        \"discountGiftDescEnglish\": null,\n" + 
					"        \"extraGiftDescEnglish\": null,\n" + 
					"        \"buyOneGiftDescEnglish\": null,\n" + 
					"        \"doubleGiftDescEnglish\": null,\n" + 
					"        \"giftFullfilmentParameterName\": null,\n" + 
					"        \"giftFullfilmentParameterValue\": null,\n" + 
					"        \"menuValidityAr\": null,\n" + 
					"        \"menuValidityEn\": null,\n" + 
					"        \"categoryCssName\": null,\n" + 
					"        \"hideProduct\": false,\n" + 
					"        \"productPrice\": null,\n" + 
					"        \"englishDescription\": \"Enjoy 1500 MB with 30 EGP/ month\",\n" + 
					"        \"arabicDescription\": \"احصل على 1500 ميجا ب 30 ج/الشهر\",\n" + 
					"        \"englishName\": \"Connect 30\",\n" + 
					"        \"arabicName\": \"كونكت 30\",\n" + 
					"        \"name\": \"Terra_New_Connect_30\",\n" + 
					"        \"relatedProduct\": [],\n" + 
					"        \"extraEnglishDescription\": \"1\",\n" + 
					"        \"extraArabicDescription\": \"1\"\n" + 
					"      },\n" + 
					"      {\n" + 
					"        \"id\": \"836\",\n" + 
					"        \"fees\": \"60.0\",\n" + 
					"        \"active\": false,\n" + 
					"        \"isActive\": \"false\",\n" + 
					"        \"productRelationshipsList\": {},\n" + 
					"        \"operationResponses\": [\n" + 
					"          {\n" + 
					"            \"arabicDescription\": \"اشتراك\",\n" + 
					"            \"arabicName\": \"اشتراك\",\n" + 
					"            \"bundleResourceID\": \"1000200\",\n" + 
					"            \"englishDescription\": \"ACTIVATE\",\n" + 
					"            \"englishName\": \"ACTIVATE\",\n" + 
					"            \"operationName\": \"ACTIVATE\",\n" + 
					"            \"operationType\": null,\n" + 
					"            \"order\": null,\n" + 
					"            \"suggestedMethod\": null,\n" + 
					"            \"group\": null\n" + 
					"          }\n" + 
					"        ],\n" + 
					"        \"commNameEnglish\": null,\n" + 
					"        \"commNameArabic\": null,\n" + 
					"        \"addOn\": null,\n" + 
					"        \"newMIConnect\": false,\n" + 
					"        \"bazingaRateplan\": false,\n" + 
					"        \"extraSocial\": false,\n" + 
					"        \"extraStreaming\": false,\n" + 
					"        \"extraVoice\": false,\n" + 
					"        \"discountGift\": \"1\",\n" + 
					"        \"extraGift\": \"3\",\n" + 
					"        \"buyOneGift\": null,\n" + 
					"        \"doubleGift\": \"3\",\n" + 
					"        \"discountGiftDescArabic\": null,\n" + 
					"        \"extraGiftDescArabic\": null,\n" + 
					"        \"buyOneGiftDescArabic\": null,\n" + 
					"        \"doubleGiftDescArabic\": null,\n" + 
					"        \"discountGiftDescEnglish\": null,\n" + 
					"        \"extraGiftDescEnglish\": null,\n" + 
					"        \"buyOneGiftDescEnglish\": null,\n" + 
					"        \"doubleGiftDescEnglish\": null,\n" + 
					"        \"giftFullfilmentParameterName\": null,\n" + 
					"        \"giftFullfilmentParameterValue\": null,\n" + 
					"        \"menuValidityAr\": null,\n" + 
					"        \"menuValidityEn\": null,\n" + 
					"        \"categoryCssName\": null,\n" + 
					"        \"hideProduct\": false,\n" + 
					"        \"productPrice\": null,\n" + 
					"        \"englishDescription\": \"Enjoy 3500 MB with 60 EGP/ month\",\n" + 
					"        \"arabicDescription\": \"احصل على 3500 ميجا ب 60 ج/الشهر\",\n" + 
					"        \"englishName\": \"Connect 60\",\n" + 
					"        \"arabicName\": \"كونكت 60\",\n" + 
					"        \"name\": \"Terra_New_Connect_60\",\n" + 
					"        \"relatedProduct\": [],\n" + 
					"        \"extraEnglishDescription\": \"1\",\n" + 
					"        \"extraArabicDescription\": \"1\"\n" + 
					"      },\n" + 
					"      {\n" + 
					"        \"id\": \"837\",\n" + 
					"        \"fees\": \"100.0\",\n" + 
					"        \"active\": false,\n" + 
					"        \"isActive\": \"true\",\n" + 
					"        \"productRelationshipsList\": {},\n" + 
					"        \"operationResponses\": [\n" + 
					"          {\n" + 
					"            \"arabicDescription\": null,\n" + 
					"            \"arabicName\": null,\n" + 
					"            \"bundleResourceID\": null,\n" + 
					"            \"englishDescription\": null,\n" + 
					"            \"englishName\": null,\n" + 
					"            \"operationName\": \"DEACTIVATE\",\n" + 
					"            \"operationType\": null,\n" + 
					"            \"order\": null,\n" + 
					"            \"suggestedMethod\": null,\n" + 
					"            \"group\": null\n" + 
					"          }\n" + 
					"        ],\n" + 
					"        \"commNameEnglish\": null,\n" + 
					"        \"commNameArabic\": null,\n" + 
					"        \"addOn\": null,\n" + 
					"        \"newMIConnect\": false,\n" + 
					"        \"bazingaRateplan\": false,\n" + 
					"        \"extraSocial\": false,\n" + 
					"        \"extraStreaming\": false,\n" + 
					"        \"extraVoice\": false,\n" + 
					"        \"discountGift\": \"1\",\n" + 
					"        \"extraGift\": \"3\",\n" + 
					"        \"buyOneGift\": null,\n" + 
					"        \"doubleGift\": \"3\",\n" + 
					"        \"discountGiftDescArabic\": null,\n" + 
					"        \"extraGiftDescArabic\": null,\n" + 
					"        \"buyOneGiftDescArabic\": null,\n" + 
					"        \"doubleGiftDescArabic\": null,\n" + 
					"        \"discountGiftDescEnglish\": null,\n" + 
					"        \"extraGiftDescEnglish\": null,\n" + 
					"        \"buyOneGiftDescEnglish\": null,\n" + 
					"        \"doubleGiftDescEnglish\": null,\n" + 
					"        \"giftFullfilmentParameterName\": null,\n" + 
					"        \"giftFullfilmentParameterValue\": null,\n" + 
					"        \"menuValidityAr\": null,\n" + 
					"        \"menuValidityEn\": null,\n" + 
					"        \"categoryCssName\": null,\n" + 
					"        \"hideProduct\": false,\n" + 
					"        \"productPrice\": null,\n" + 
					"        \"englishDescription\": \"Enjoy 8000 MB for 100 EGP/ month\",\n" + 
					"        \"arabicDescription\": \"احصل على 8000 ميجا ب 100 ج/الشهر\",\n" + 
					"        \"englishName\": \"Connect 100\",\n" + 
					"        \"arabicName\": \"كونكت 100\",\n" + 
					"        \"name\": \"CONNECT_LIMITED_100_PREPAID\",\n" + 
					"        \"relatedProduct\": [],\n" + 
					"        \"extraEnglishDescription\": \"1\",\n" + 
					"        \"extraArabicDescription\": \"1\"\n" + 
					"      },\n" + 
					"      {\n" + 
					"        \"id\": \"838\",\n" + 
					"        \"fees\": \"150.0\",\n" + 
					"        \"active\": false,\n" + 
					"        \"isActive\": \"true\",\n" + 
					"        \"productRelationshipsList\": {},\n" + 
					"        \"operationResponses\": [\n" + 
					"          {\n" + 
					"            \"arabicDescription\": null,\n" + 
					"            \"arabicName\": null,\n" + 
					"            \"bundleResourceID\": null,\n" + 
					"            \"englishDescription\": null,\n" + 
					"            \"englishName\": null,\n" + 
					"            \"operationName\": \"DEACTIVATE\",\n" + 
					"            \"operationType\": null,\n" + 
					"            \"order\": null,\n" + 
					"            \"suggestedMethod\": null,\n" + 
					"            \"group\": null\n" + 
					"          }\n" + 
					"        ],\n" + 
					"        \"commNameEnglish\": null,\n" + 
					"        \"commNameArabic\": null,\n" + 
					"        \"addOn\": null,\n" + 
					"        \"newMIConnect\": false,\n" + 
					"        \"bazingaRateplan\": false,\n" + 
					"        \"extraSocial\": false,\n" + 
					"        \"extraStreaming\": false,\n" + 
					"        \"extraVoice\": false,\n" + 
					"        \"discountGift\": \"1\",\n" + 
					"        \"extraGift\": \"3\",\n" + 
					"        \"buyOneGift\": null,\n" + 
					"        \"doubleGift\": \"3\",\n" + 
					"        \"discountGiftDescArabic\": null,\n" + 
					"        \"extraGiftDescArabic\": null,\n" + 
					"        \"buyOneGiftDescArabic\": null,\n" + 
					"        \"doubleGiftDescArabic\": null,\n" + 
					"        \"discountGiftDescEnglish\": null,\n" + 
					"        \"extraGiftDescEnglish\": null,\n" + 
					"        \"buyOneGiftDescEnglish\": null,\n" + 
					"        \"doubleGiftDescEnglish\": null,\n" + 
					"        \"giftFullfilmentParameterName\": null,\n" + 
					"        \"giftFullfilmentParameterValue\": null,\n" + 
					"        \"menuValidityAr\": null,\n" + 
					"        \"menuValidityEn\": null,\n" + 
					"        \"categoryCssName\": null,\n" + 
					"        \"hideProduct\": false,\n" + 
					"        \"productPrice\": null,\n" + 
					"        \"englishDescription\": \"Enjoy 12288 MB for 150 EGP/ month\",\n" + 
					"        \"arabicDescription\": \"احصل على 12288 ميجا ب 150 ج/الشهر\",\n" + 
					"        \"englishName\": \"Connect 150\",\n" + 
					"        \"arabicName\": \"كونكت 150\",\n" + 
					"        \"name\": \"CONNECT_LIMITED_150_PREPAID\",\n" + 
					"        \"relatedProduct\": [],\n" + 
					"        \"extraEnglishDescription\": \"1\",\n" + 
					"        \"extraArabicDescription\": \"1\"\n" + 
					"      }\n" + 
					"    ],\n" + 
					"    \"childEligibleProductModels\": []\n" + 
					"  },\n" + 
					"  {\n" + 
					"    \"category\": {\n" + 
					"      \"categoryNameAr\": \" باقات سوبر كونكت الجديده\",\n" + 
					"      \"categoryNameEn\": \"New Super Connect Bundles\",\n" + 
					"      \"categoryId\": \"PORTAL_CONNECT_NEW_MI\",\n" + 
					"      \"offerId\": null\n" + 
					"    },\n" + 
					"    \"products\": [\n" + 
					"      {\n" + 
					"        \"id\": null,\n" + 
					"        \"fees\": \"10.0\",\n" + 
					"        \"active\": true,\n" + 
					"        \"isActive\": \"true\",\n" + 
					"        \"productRelationshipsList\": {\n" + 
					"          \"EXTRA\": [\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"4\",\n" + 
					"              \"newMIConnect\": true,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": true,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE EN DESC\",\n" + 
					"              \"arabicDescription\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE AR DESC\",\n" + 
					"              \"englishName\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE EN NAME\",\n" + 
					"              \"arabicName\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE AR NAME\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_DOUBLE_VOICE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": true,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": \"Super streaming\",\n" + 
					"              \"commNameArabic\": \"سوبر ستريمنج\",\n" + 
					"              \"addOn\": \"2\",\n" + 
					"              \"newMIConnect\": true,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": true,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Super streaming\",\n" + 
					"              \"arabicDescription\": \" ميجابيتس سوبر ستريمنج\",\n" + 
					"              \"englishName\": \"Super streaming\",\n" + 
					"              \"arabicName\": \"ميجابيتس سوبر ستريمنج\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_DOUBLE_STREAMING\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": \"Super social\",\n" + 
					"              \"commNameArabic\": \"سوبر سوشيال\",\n" + 
					"              \"addOn\": \"1\",\n" + 
					"              \"newMIConnect\": true,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": true,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Super social\",\n" + 
					"              \"arabicDescription\": \"ميجابيتس سوبر سوشيال \",\n" + 
					"              \"englishName\": \"Super social\",\n" + 
					"              \"arabicName\": \" ميجابيتس سوبر سوشيال\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_DOUBLE_SOCIAL\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            }\n" + 
					"          ],\n" + 
					"          \"EXTRA_ADDON\": [\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"RC\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 1250MB to be used on all websites, addon to be renewed with the bundle\",\n" + 
					"              \"arabicDescription\": \"احصل علي 1250 ميجا اضافية تستخدمهم علي كل المواقع, يتم تجديد الباقة الاضافية كل شهر مع الباقة\",\n" + 
					"              \"englishName\": \"Super Extra 20LE – 1250MB renewable\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 20ج – 1250 ميجا متجددة\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_3_RENEWABLE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"RB\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 500MB to be used on all websites, addon to be renewed with the bundle\",\n" + 
					"              \"arabicDescription\": \"احصل علي 500 ميجا اضافية تستخدمهم علي كل المواقع, يتم تجديد الباقة الاضافية كل شهر مع الباقة\",\n" + 
					"              \"englishName\": \"Super Extra 10LE – 500MB renewable\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 10ج – 500 ميجا متجددة\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_2_RENEWABLE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"A\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 200MB to be used on all websites \",\n" + 
					"              \"arabicDescription\": \"احصل علي 200 ميجا اضافية تستخدمهم علي كل المواقع.\",\n" + 
					"              \"englishName\": \"Super Extra 5LE – 200MB\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 5ج – 200 ميجا\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_1_ONDEMAND\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"C\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 1250MB to be used on all websites\",\n" + 
					"              \"arabicDescription\": \"احصل علي 1250 ميجا اضافية تستخدمهم علي كل المواقع.\",\n" + 
					"              \"englishName\": \"Super Extra 20LE – 1250MB\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 20ج – 1250 ميجا\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_3_ONDEMAND\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": null,\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"SHARED NEW MI\",\n" + 
					"              \"arabicDescription\": \"SHARED NEW MI\",\n" + 
					"              \"englishName\": \"SHARED NEW MI\",\n" + 
					"              \"arabicName\": \"SHARED NEW MI\",\n" + 
					"              \"name\": \"SHARED_NEW_MI\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"RA\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 200MB to be used on all websites, addon to be renewed with the bundle\",\n" + 
					"              \"arabicDescription\": \"احصل علي 200 ميجا اضافية تستخدمهم علي كل المواقع, يتم تجديد الباقة الاضافية كل شهر مع الباقة\",\n" + 
					"              \"englishName\": \"Super Extra 5LE – 200MB renewable\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 5ج – 200 ميجا متجددة\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_1_RENEWABLE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"B\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 500MB to be used on all websites\",\n" + 
					"              \"arabicDescription\": \"احصل علي 500 ميجا اضافية تستخدمهم علي كل المواقع.\",\n" + 
					"              \"englishName\": \"Super Extra 10LE – 500MB\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 10ج – 500 ميجا\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_2_ONDEMAND\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            }\n" + 
					"          ]\n" + 
					"        },\n" + 
					"        \"operationResponses\": [\n" + 
					"          {\n" + 
					"            \"arabicDescription\": null,\n" + 
					"            \"arabicName\": null,\n" + 
					"            \"bundleResourceID\": null,\n" + 
					"            \"englishDescription\": null,\n" + 
					"            \"englishName\": null,\n" + 
					"            \"operationName\": \"DEACTIVATE\",\n" + 
					"            \"operationType\": null,\n" + 
					"            \"order\": null,\n" + 
					"            \"suggestedMethod\": null,\n" + 
					"            \"group\": null\n" + 
					"          }\n" + 
					"        ],\n" + 
					"        \"commNameEnglish\": null,\n" + 
					"        \"commNameArabic\": null,\n" + 
					"        \"addOn\": null,\n" + 
					"        \"newMIConnect\": true,\n" + 
					"        \"bazingaRateplan\": false,\n" + 
					"        \"extraSocial\": false,\n" + 
					"        \"extraStreaming\": false,\n" + 
					"        \"extraVoice\": false,\n" + 
					"        \"discountGift\": null,\n" + 
					"        \"extraGift\": null,\n" + 
					"        \"buyOneGift\": null,\n" + 
					"        \"doubleGift\": null,\n" + 
					"        \"discountGiftDescArabic\": null,\n" + 
					"        \"extraGiftDescArabic\": null,\n" + 
					"        \"buyOneGiftDescArabic\": null,\n" + 
					"        \"doubleGiftDescArabic\": null,\n" + 
					"        \"discountGiftDescEnglish\": null,\n" + 
					"        \"extraGiftDescEnglish\": null,\n" + 
					"        \"buyOneGiftDescEnglish\": null,\n" + 
					"        \"doubleGiftDescEnglish\": null,\n" + 
					"        \"giftFullfilmentParameterName\": null,\n" + 
					"        \"giftFullfilmentParameterValue\": null,\n" + 
					"        \"menuValidityAr\": null,\n" + 
					"        \"menuValidityEn\": null,\n" + 
					"        \"categoryCssName\": null,\n" + 
					"        \"hideProduct\": false,\n" + 
					"        \"productPrice\": null,\n" + 
					"        \"englishDescription\": \"Super Connect 10LE - 200MB All + 200 super MB\",\n" + 
					"        \"arabicDescription\": \"سوبر كونكت 10ج - 200 ميجا + 200 سوبر ميجابيتس\",\n" + 
					"        \"englishName\": \"Super Connect 10LE\",\n" + 
					"        \"arabicName\": \"سوبر كونكت 10ج \",\n" + 
					"        \"name\": \"NEW_MI_BUNDLE_PRODUCT_NO_1\",\n" + 
					"        \"relatedProduct\": [\n" + 
					"          {\n" + 
					"            \"id\": null,\n" + 
					"            \"fees\": \"0.0\",\n" + 
					"            \"active\": false,\n" + 
					"            \"isActive\": \"false\",\n" + 
					"            \"productRelationshipsList\": {},\n" + 
					"            \"operationResponses\": [],\n" + 
					"            \"commNameEnglish\": null,\n" + 
					"            \"commNameArabic\": null,\n" + 
					"            \"addOn\": \"4\",\n" + 
					"            \"newMIConnect\": true,\n" + 
					"            \"bazingaRateplan\": false,\n" + 
					"            \"extraSocial\": false,\n" + 
					"            \"extraStreaming\": false,\n" + 
					"            \"extraVoice\": true,\n" + 
					"            \"discountGift\": null,\n" + 
					"            \"extraGift\": null,\n" + 
					"            \"buyOneGift\": null,\n" + 
					"            \"doubleGift\": null,\n" + 
					"            \"discountGiftDescArabic\": null,\n" + 
					"            \"extraGiftDescArabic\": null,\n" + 
					"            \"buyOneGiftDescArabic\": null,\n" + 
					"            \"doubleGiftDescArabic\": null,\n" + 
					"            \"discountGiftDescEnglish\": null,\n" + 
					"            \"extraGiftDescEnglish\": null,\n" + 
					"            \"buyOneGiftDescEnglish\": null,\n" + 
					"            \"doubleGiftDescEnglish\": null,\n" + 
					"            \"giftFullfilmentParameterName\": null,\n" + 
					"            \"giftFullfilmentParameterValue\": null,\n" + 
					"            \"menuValidityAr\": null,\n" + 
					"            \"menuValidityEn\": null,\n" + 
					"            \"categoryCssName\": null,\n" + 
					"            \"hideProduct\": false,\n" + 
					"            \"productPrice\": null,\n" + 
					"            \"englishDescription\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE EN DESC\",\n" + 
					"            \"arabicDescription\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE AR DESC\",\n" + 
					"            \"englishName\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE EN NAME\",\n" + 
					"            \"arabicName\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE AR NAME\",\n" + 
					"            \"name\": \"NEW_MI_BUNDLE_ADDON_DOUBLE_VOICE\",\n" + 
					"            \"relatedProduct\": [],\n" + 
					"            \"extraEnglishDescription\": null,\n" + 
					"            \"extraArabicDescription\": null\n" + 
					"          },\n" + 
					"          {\n" + 
					"            \"id\": null,\n" + 
					"            \"fees\": \"0.0\",\n" + 
					"            \"active\": true,\n" + 
					"            \"isActive\": \"false\",\n" + 
					"            \"productRelationshipsList\": {},\n" + 
					"            \"operationResponses\": [],\n" + 
					"            \"commNameEnglish\": \"Super streaming\",\n" + 
					"            \"commNameArabic\": \"سوبر ستريمنج\",\n" + 
					"            \"addOn\": \"2\",\n" + 
					"            \"newMIConnect\": true,\n" + 
					"            \"bazingaRateplan\": false,\n" + 
					"            \"extraSocial\": false,\n" + 
					"            \"extraStreaming\": true,\n" + 
					"            \"extraVoice\": false,\n" + 
					"            \"discountGift\": null,\n" + 
					"            \"extraGift\": null,\n" + 
					"            \"buyOneGift\": null,\n" + 
					"            \"doubleGift\": null,\n" + 
					"            \"discountGiftDescArabic\": null,\n" + 
					"            \"extraGiftDescArabic\": null,\n" + 
					"            \"buyOneGiftDescArabic\": null,\n" + 
					"            \"doubleGiftDescArabic\": null,\n" + 
					"            \"discountGiftDescEnglish\": null,\n" + 
					"            \"extraGiftDescEnglish\": null,\n" + 
					"            \"buyOneGiftDescEnglish\": null,\n" + 
					"            \"doubleGiftDescEnglish\": null,\n" + 
					"            \"giftFullfilmentParameterName\": null,\n" + 
					"            \"giftFullfilmentParameterValue\": null,\n" + 
					"            \"menuValidityAr\": null,\n" + 
					"            \"menuValidityEn\": null,\n" + 
					"            \"categoryCssName\": null,\n" + 
					"            \"hideProduct\": false,\n" + 
					"            \"productPrice\": null,\n" + 
					"            \"englishDescription\": \"Super streaming\",\n" + 
					"            \"arabicDescription\": \" ميجابيتس سوبر ستريمنج\",\n" + 
					"            \"englishName\": \"Super streaming\",\n" + 
					"            \"arabicName\": \"ميجابيتس سوبر ستريمنج\",\n" + 
					"            \"name\": \"NEW_MI_BUNDLE_DOUBLE_STREAMING\",\n" + 
					"            \"relatedProduct\": [],\n" + 
					"            \"extraEnglishDescription\": null,\n" + 
					"            \"extraArabicDescription\": null\n" + 
					"          },\n" + 
					"          {\n" + 
					"            \"id\": null,\n" + 
					"            \"fees\": \"0.0\",\n" + 
					"            \"active\": false,\n" + 
					"            \"isActive\": \"false\",\n" + 
					"            \"productRelationshipsList\": {},\n" + 
					"            \"operationResponses\": [],\n" + 
					"            \"commNameEnglish\": \"Super social\",\n" + 
					"            \"commNameArabic\": \"سوبر سوشيال\",\n" + 
					"            \"addOn\": \"1\",\n" + 
					"            \"newMIConnect\": true,\n" + 
					"            \"bazingaRateplan\": false,\n" + 
					"            \"extraSocial\": true,\n" + 
					"            \"extraStreaming\": false,\n" + 
					"            \"extraVoice\": false,\n" + 
					"            \"discountGift\": null,\n" + 
					"            \"extraGift\": null,\n" + 
					"            \"buyOneGift\": null,\n" + 
					"            \"doubleGift\": null,\n" + 
					"            \"discountGiftDescArabic\": null,\n" + 
					"            \"extraGiftDescArabic\": null,\n" + 
					"            \"buyOneGiftDescArabic\": null,\n" + 
					"            \"doubleGiftDescArabic\": null,\n" + 
					"            \"discountGiftDescEnglish\": null,\n" + 
					"            \"extraGiftDescEnglish\": null,\n" + 
					"            \"buyOneGiftDescEnglish\": null,\n" + 
					"            \"doubleGiftDescEnglish\": null,\n" + 
					"            \"giftFullfilmentParameterName\": null,\n" + 
					"            \"giftFullfilmentParameterValue\": null,\n" + 
					"            \"menuValidityAr\": null,\n" + 
					"            \"menuValidityEn\": null,\n" + 
					"            \"categoryCssName\": null,\n" + 
					"            \"hideProduct\": false,\n" + 
					"            \"productPrice\": null,\n" + 
					"            \"englishDescription\": \"Super social\",\n" + 
					"            \"arabicDescription\": \"ميجابيتس سوبر سوشيال \",\n" + 
					"            \"englishName\": \"Super social\",\n" + 
					"            \"arabicName\": \" ميجابيتس سوبر سوشيال\",\n" + 
					"            \"name\": \"NEW_MI_BUNDLE_DOUBLE_SOCIAL\",\n" + 
					"            \"relatedProduct\": [],\n" + 
					"            \"extraEnglishDescription\": null,\n" + 
					"            \"extraArabicDescription\": null\n" + 
					"          }\n" + 
					"        ],\n" + 
					"        \"extraEnglishDescription\": \"ODS_OFFER_ADDITIONAL_DESC_EN\",\n" + 
					"        \"extraArabicDescription\": \"ODS_OFFER_ADDITIONAL_DESC_AR\"\n" + 
					"      },\n" + 
					"      {\n" + 
					"        \"id\": null,\n" + 
					"        \"fees\": \"20.0\",\n" + 
					"        \"active\": false,\n" + 
					"        \"isActive\": \"false\",\n" + 
					"        \"productRelationshipsList\": {\n" + 
					"          \"EXTRA\": [\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": \"Super streaming\",\n" + 
					"              \"commNameArabic\": \"سوبر ستريمنج\",\n" + 
					"              \"addOn\": \"2\",\n" + 
					"              \"newMIConnect\": true,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": true,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Super streaming\",\n" + 
					"              \"arabicDescription\": \" ميجابيتس سوبر ستريمنج\",\n" + 
					"              \"englishName\": \"Super streaming\",\n" + 
					"              \"arabicName\": \"ميجابيتس سوبر ستريمنج\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_DOUBLE_STREAMING\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"4\",\n" + 
					"              \"newMIConnect\": true,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": true,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE EN DESC\",\n" + 
					"              \"arabicDescription\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE AR DESC\",\n" + 
					"              \"englishName\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE EN NAME\",\n" + 
					"              \"arabicName\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE AR NAME\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_DOUBLE_VOICE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": \"Super social\",\n" + 
					"              \"commNameArabic\": \"سوبر سوشيال\",\n" + 
					"              \"addOn\": \"1\",\n" + 
					"              \"newMIConnect\": true,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": true,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Super social\",\n" + 
					"              \"arabicDescription\": \"ميجابيتس سوبر سوشيال \",\n" + 
					"              \"englishName\": \"Super social\",\n" + 
					"              \"arabicName\": \" ميجابيتس سوبر سوشيال\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_DOUBLE_SOCIAL\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            }\n" + 
					"          ],\n" + 
					"          \"EXTRA_ADDON\": [\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"RA\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 200MB to be used on all websites, addon to be renewed with the bundle\",\n" + 
					"              \"arabicDescription\": \"احصل علي 200 ميجا اضافية تستخدمهم علي كل المواقع, يتم تجديد الباقة الاضافية كل شهر مع الباقة\",\n" + 
					"              \"englishName\": \"Super Extra 5LE – 200MB renewable\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 5ج – 200 ميجا متجددة\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_1_RENEWABLE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"B\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 500MB to be used on all websites\",\n" + 
					"              \"arabicDescription\": \"احصل علي 500 ميجا اضافية تستخدمهم علي كل المواقع.\",\n" + 
					"              \"englishName\": \"Super Extra 10LE – 500MB\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 10ج – 500 ميجا\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_2_ONDEMAND\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"RC\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 1250MB to be used on all websites, addon to be renewed with the bundle\",\n" + 
					"              \"arabicDescription\": \"احصل علي 1250 ميجا اضافية تستخدمهم علي كل المواقع, يتم تجديد الباقة الاضافية كل شهر مع الباقة\",\n" + 
					"              \"englishName\": \"Super Extra 20LE – 1250MB renewable\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 20ج – 1250 ميجا متجددة\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_3_RENEWABLE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"RB\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 500MB to be used on all websites, addon to be renewed with the bundle\",\n" + 
					"              \"arabicDescription\": \"احصل علي 500 ميجا اضافية تستخدمهم علي كل المواقع, يتم تجديد الباقة الاضافية كل شهر مع الباقة\",\n" + 
					"              \"englishName\": \"Super Extra 10LE – 500MB renewable\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 10ج – 500 ميجا متجددة\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_2_RENEWABLE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"A\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 200MB to be used on all websites \",\n" + 
					"              \"arabicDescription\": \"احصل علي 200 ميجا اضافية تستخدمهم علي كل المواقع.\",\n" + 
					"              \"englishName\": \"Super Extra 5LE – 200MB\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 5ج – 200 ميجا\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_1_ONDEMAND\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"C\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 1250MB to be used on all websites\",\n" + 
					"              \"arabicDescription\": \"احصل علي 1250 ميجا اضافية تستخدمهم علي كل المواقع.\",\n" + 
					"              \"englishName\": \"Super Extra 20LE – 1250MB\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 20ج – 1250 ميجا\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_3_ONDEMAND\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": null,\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"SHARED NEW MI\",\n" + 
					"              \"arabicDescription\": \"SHARED NEW MI\",\n" + 
					"              \"englishName\": \"SHARED NEW MI\",\n" + 
					"              \"arabicName\": \"SHARED NEW MI\",\n" + 
					"              \"name\": \"SHARED_NEW_MI\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            }\n" + 
					"          ]\n" + 
					"        },\n" + 
					"        \"operationResponses\": [\n" + 
					"          {\n" + 
					"            \"arabicDescription\": null,\n" + 
					"            \"arabicName\": null,\n" + 
					"            \"bundleResourceID\": null,\n" + 
					"            \"englishDescription\": null,\n" + 
					"            \"englishName\": null,\n" + 
					"            \"operationName\": \"ACTIVATE_BUNDLE\",\n" + 
					"            \"operationType\": null,\n" + 
					"            \"order\": null,\n" + 
					"            \"suggestedMethod\": null,\n" + 
					"            \"group\": null\n" + 
					"          }\n" + 
					"        ],\n" + 
					"        \"commNameEnglish\": null,\n" + 
					"        \"commNameArabic\": null,\n" + 
					"        \"addOn\": null,\n" + 
					"        \"newMIConnect\": true,\n" + 
					"        \"bazingaRateplan\": false,\n" + 
					"        \"extraSocial\": false,\n" + 
					"        \"extraStreaming\": false,\n" + 
					"        \"extraVoice\": false,\n" + 
					"        \"discountGift\": null,\n" + 
					"        \"extraGift\": null,\n" + 
					"        \"buyOneGift\": null,\n" + 
					"        \"doubleGift\": null,\n" + 
					"        \"discountGiftDescArabic\": null,\n" + 
					"        \"extraGiftDescArabic\": null,\n" + 
					"        \"buyOneGiftDescArabic\": null,\n" + 
					"        \"doubleGiftDescArabic\": null,\n" + 
					"        \"discountGiftDescEnglish\": null,\n" + 
					"        \"extraGiftDescEnglish\": null,\n" + 
					"        \"buyOneGiftDescEnglish\": null,\n" + 
					"        \"doubleGiftDescEnglish\": null,\n" + 
					"        \"giftFullfilmentParameterName\": null,\n" + 
					"        \"giftFullfilmentParameterValue\": null,\n" + 
					"        \"menuValidityAr\": null,\n" + 
					"        \"menuValidityEn\": null,\n" + 
					"        \"categoryCssName\": null,\n" + 
					"        \"hideProduct\": false,\n" + 
					"        \"productPrice\": null,\n" + 
					"        \"englishDescription\": \"Super Connect 20LE - 1000MB All + 1000 Super MB\",\n" + 
					"        \"arabicDescription\": \"سوبر كونكت 20ج - 1000 ميجا + 1000 سوبر ميجابيتس\",\n" + 
					"        \"englishName\": \"Super Connect 20LE\",\n" + 
					"        \"arabicName\": \"سوبر كونكت 20ج \",\n" + 
					"        \"name\": \"NEW_MI_BUNDLE_PRODUCT_NO_2\",\n" + 
					"        \"relatedProduct\": [\n" + 
					"          {\n" + 
					"            \"id\": null,\n" + 
					"            \"fees\": \"0.0\",\n" + 
					"            \"active\": false,\n" + 
					"            \"isActive\": \"false\",\n" + 
					"            \"productRelationshipsList\": {},\n" + 
					"            \"operationResponses\": [],\n" + 
					"            \"commNameEnglish\": \"Super streaming\",\n" + 
					"            \"commNameArabic\": \"سوبر ستريمنج\",\n" + 
					"            \"addOn\": \"2\",\n" + 
					"            \"newMIConnect\": true,\n" + 
					"            \"bazingaRateplan\": false,\n" + 
					"            \"extraSocial\": false,\n" + 
					"            \"extraStreaming\": true,\n" + 
					"            \"extraVoice\": false,\n" + 
					"            \"discountGift\": null,\n" + 
					"            \"extraGift\": null,\n" + 
					"            \"buyOneGift\": null,\n" + 
					"            \"doubleGift\": null,\n" + 
					"            \"discountGiftDescArabic\": null,\n" + 
					"            \"extraGiftDescArabic\": null,\n" + 
					"            \"buyOneGiftDescArabic\": null,\n" + 
					"            \"doubleGiftDescArabic\": null,\n" + 
					"            \"discountGiftDescEnglish\": null,\n" + 
					"            \"extraGiftDescEnglish\": null,\n" + 
					"            \"buyOneGiftDescEnglish\": null,\n" + 
					"            \"doubleGiftDescEnglish\": null,\n" + 
					"            \"giftFullfilmentParameterName\": null,\n" + 
					"            \"giftFullfilmentParameterValue\": null,\n" + 
					"            \"menuValidityAr\": null,\n" + 
					"            \"menuValidityEn\": null,\n" + 
					"            \"categoryCssName\": null,\n" + 
					"            \"hideProduct\": false,\n" + 
					"            \"productPrice\": null,\n" + 
					"            \"englishDescription\": \"Super streaming\",\n" + 
					"            \"arabicDescription\": \" ميجابيتس سوبر ستريمنج\",\n" + 
					"            \"englishName\": \"Super streaming\",\n" + 
					"            \"arabicName\": \"ميجابيتس سوبر ستريمنج\",\n" + 
					"            \"name\": \"NEW_MI_BUNDLE_DOUBLE_STREAMING\",\n" + 
					"            \"relatedProduct\": [],\n" + 
					"            \"extraEnglishDescription\": null,\n" + 
					"            \"extraArabicDescription\": null\n" + 
					"          },\n" + 
					"          {\n" + 
					"            \"id\": null,\n" + 
					"            \"fees\": \"0.0\",\n" + 
					"            \"active\": false,\n" + 
					"            \"isActive\": \"false\",\n" + 
					"            \"productRelationshipsList\": {},\n" + 
					"            \"operationResponses\": [],\n" + 
					"            \"commNameEnglish\": null,\n" + 
					"            \"commNameArabic\": null,\n" + 
					"            \"addOn\": \"4\",\n" + 
					"            \"newMIConnect\": true,\n" + 
					"            \"bazingaRateplan\": false,\n" + 
					"            \"extraSocial\": false,\n" + 
					"            \"extraStreaming\": false,\n" + 
					"            \"extraVoice\": true,\n" + 
					"            \"discountGift\": null,\n" + 
					"            \"extraGift\": null,\n" + 
					"            \"buyOneGift\": null,\n" + 
					"            \"doubleGift\": null,\n" + 
					"            \"discountGiftDescArabic\": null,\n" + 
					"            \"extraGiftDescArabic\": null,\n" + 
					"            \"buyOneGiftDescArabic\": null,\n" + 
					"            \"doubleGiftDescArabic\": null,\n" + 
					"            \"discountGiftDescEnglish\": null,\n" + 
					"            \"extraGiftDescEnglish\": null,\n" + 
					"            \"buyOneGiftDescEnglish\": null,\n" + 
					"            \"doubleGiftDescEnglish\": null,\n" + 
					"            \"giftFullfilmentParameterName\": null,\n" + 
					"            \"giftFullfilmentParameterValue\": null,\n" + 
					"            \"menuValidityAr\": null,\n" + 
					"            \"menuValidityEn\": null,\n" + 
					"            \"categoryCssName\": null,\n" + 
					"            \"hideProduct\": false,\n" + 
					"            \"productPrice\": null,\n" + 
					"            \"englishDescription\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE EN DESC\",\n" + 
					"            \"arabicDescription\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE AR DESC\",\n" + 
					"            \"englishName\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE EN NAME\",\n" + 
					"            \"arabicName\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE AR NAME\",\n" + 
					"            \"name\": \"NEW_MI_BUNDLE_ADDON_DOUBLE_VOICE\",\n" + 
					"            \"relatedProduct\": [],\n" + 
					"            \"extraEnglishDescription\": null,\n" + 
					"            \"extraArabicDescription\": null\n" + 
					"          },\n" + 
					"          {\n" + 
					"            \"id\": null,\n" + 
					"            \"fees\": \"0.0\",\n" + 
					"            \"active\": false,\n" + 
					"            \"isActive\": \"false\",\n" + 
					"            \"productRelationshipsList\": {},\n" + 
					"            \"operationResponses\": [],\n" + 
					"            \"commNameEnglish\": \"Super social\",\n" + 
					"            \"commNameArabic\": \"سوبر سوشيال\",\n" + 
					"            \"addOn\": \"1\",\n" + 
					"            \"newMIConnect\": true,\n" + 
					"            \"bazingaRateplan\": false,\n" + 
					"            \"extraSocial\": true,\n" + 
					"            \"extraStreaming\": false,\n" + 
					"            \"extraVoice\": false,\n" + 
					"            \"discountGift\": null,\n" + 
					"            \"extraGift\": null,\n" + 
					"            \"buyOneGift\": null,\n" + 
					"            \"doubleGift\": null,\n" + 
					"            \"discountGiftDescArabic\": null,\n" + 
					"            \"extraGiftDescArabic\": null,\n" + 
					"            \"buyOneGiftDescArabic\": null,\n" + 
					"            \"doubleGiftDescArabic\": null,\n" + 
					"            \"discountGiftDescEnglish\": null,\n" + 
					"            \"extraGiftDescEnglish\": null,\n" + 
					"            \"buyOneGiftDescEnglish\": null,\n" + 
					"            \"doubleGiftDescEnglish\": null,\n" + 
					"            \"giftFullfilmentParameterName\": null,\n" + 
					"            \"giftFullfilmentParameterValue\": null,\n" + 
					"            \"menuValidityAr\": null,\n" + 
					"            \"menuValidityEn\": null,\n" + 
					"            \"categoryCssName\": null,\n" + 
					"            \"hideProduct\": false,\n" + 
					"            \"productPrice\": null,\n" + 
					"            \"englishDescription\": \"Super social\",\n" + 
					"            \"arabicDescription\": \"ميجابيتس سوبر سوشيال \",\n" + 
					"            \"englishName\": \"Super social\",\n" + 
					"            \"arabicName\": \" ميجابيتس سوبر سوشيال\",\n" + 
					"            \"name\": \"NEW_MI_BUNDLE_DOUBLE_SOCIAL\",\n" + 
					"            \"relatedProduct\": [],\n" + 
					"            \"extraEnglishDescription\": null,\n" + 
					"            \"extraArabicDescription\": null\n" + 
					"          }\n" + 
					"        ],\n" + 
					"        \"extraEnglishDescription\": \"ODS_OFFER_ADDITIONAL_DESC_EN\",\n" + 
					"        \"extraArabicDescription\": \"ODS_OFFER_ADDITIONAL_DESC_AR\"\n" + 
					"      },\n" + 
					"      {\n" + 
					"        \"id\": null,\n" + 
					"        \"fees\": \"35.0\",\n" + 
					"        \"active\": false,\n" + 
					"        \"isActive\": \"false\",\n" + 
					"        \"productRelationshipsList\": {\n" + 
					"          \"EXTRA\": [\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"4\",\n" + 
					"              \"newMIConnect\": true,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": true,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE EN DESC\",\n" + 
					"              \"arabicDescription\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE AR DESC\",\n" + 
					"              \"englishName\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE EN NAME\",\n" + 
					"              \"arabicName\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE AR NAME\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_DOUBLE_VOICE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": \"Super social\",\n" + 
					"              \"commNameArabic\": \"سوبر سوشيال\",\n" + 
					"              \"addOn\": \"1\",\n" + 
					"              \"newMIConnect\": true,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": true,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Super social\",\n" + 
					"              \"arabicDescription\": \"ميجابيتس سوبر سوشيال \",\n" + 
					"              \"englishName\": \"Super social\",\n" + 
					"              \"arabicName\": \" ميجابيتس سوبر سوشيال\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_DOUBLE_SOCIAL\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": \"Super streaming\",\n" + 
					"              \"commNameArabic\": \"سوبر ستريمنج\",\n" + 
					"              \"addOn\": \"2\",\n" + 
					"              \"newMIConnect\": true,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": true,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Super streaming\",\n" + 
					"              \"arabicDescription\": \" ميجابيتس سوبر ستريمنج\",\n" + 
					"              \"englishName\": \"Super streaming\",\n" + 
					"              \"arabicName\": \"ميجابيتس سوبر ستريمنج\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_DOUBLE_STREAMING\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            }\n" + 
					"          ],\n" + 
					"          \"EXTRA_ADDON\": [\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"B\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 500MB to be used on all websites\",\n" + 
					"              \"arabicDescription\": \"احصل علي 500 ميجا اضافية تستخدمهم علي كل المواقع.\",\n" + 
					"              \"englishName\": \"Super Extra 10LE – 500MB\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 10ج – 500 ميجا\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_2_ONDEMAND\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": null,\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"SHARED NEW MI\",\n" + 
					"              \"arabicDescription\": \"SHARED NEW MI\",\n" + 
					"              \"englishName\": \"SHARED NEW MI\",\n" + 
					"              \"arabicName\": \"SHARED NEW MI\",\n" + 
					"              \"name\": \"SHARED_NEW_MI\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"RB\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 500MB to be used on all websites, addon to be renewed with the bundle\",\n" + 
					"              \"arabicDescription\": \"احصل علي 500 ميجا اضافية تستخدمهم علي كل المواقع, يتم تجديد الباقة الاضافية كل شهر مع الباقة\",\n" + 
					"              \"englishName\": \"Super Extra 10LE – 500MB renewable\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 10ج – 500 ميجا متجددة\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_2_RENEWABLE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"RA\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 200MB to be used on all websites, addon to be renewed with the bundle\",\n" + 
					"              \"arabicDescription\": \"احصل علي 200 ميجا اضافية تستخدمهم علي كل المواقع, يتم تجديد الباقة الاضافية كل شهر مع الباقة\",\n" + 
					"              \"englishName\": \"Super Extra 5LE – 200MB renewable\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 5ج – 200 ميجا متجددة\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_1_RENEWABLE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"A\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 200MB to be used on all websites \",\n" + 
					"              \"arabicDescription\": \"احصل علي 200 ميجا اضافية تستخدمهم علي كل المواقع.\",\n" + 
					"              \"englishName\": \"Super Extra 5LE – 200MB\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 5ج – 200 ميجا\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_1_ONDEMAND\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"C\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 1250MB to be used on all websites\",\n" + 
					"              \"arabicDescription\": \"احصل علي 1250 ميجا اضافية تستخدمهم علي كل المواقع.\",\n" + 
					"              \"englishName\": \"Super Extra 20LE – 1250MB\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 20ج – 1250 ميجا\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_3_ONDEMAND\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"RC\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 1250MB to be used on all websites, addon to be renewed with the bundle\",\n" + 
					"              \"arabicDescription\": \"احصل علي 1250 ميجا اضافية تستخدمهم علي كل المواقع, يتم تجديد الباقة الاضافية كل شهر مع الباقة\",\n" + 
					"              \"englishName\": \"Super Extra 20LE – 1250MB renewable\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 20ج – 1250 ميجا متجددة\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_3_RENEWABLE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            }\n" + 
					"          ]\n" + 
					"        },\n" + 
					"        \"operationResponses\": [\n" + 
					"          {\n" + 
					"            \"arabicDescription\": null,\n" + 
					"            \"arabicName\": null,\n" + 
					"            \"bundleResourceID\": null,\n" + 
					"            \"englishDescription\": null,\n" + 
					"            \"englishName\": null,\n" + 
					"            \"operationName\": \"ACTIVATE_BUNDLE\",\n" + 
					"            \"operationType\": null,\n" + 
					"            \"order\": null,\n" + 
					"            \"suggestedMethod\": null,\n" + 
					"            \"group\": null\n" + 
					"          }\n" + 
					"        ],\n" + 
					"        \"commNameEnglish\": null,\n" + 
					"        \"commNameArabic\": null,\n" + 
					"        \"addOn\": null,\n" + 
					"        \"newMIConnect\": true,\n" + 
					"        \"bazingaRateplan\": false,\n" + 
					"        \"extraSocial\": false,\n" + 
					"        \"extraStreaming\": false,\n" + 
					"        \"extraVoice\": false,\n" + 
					"        \"discountGift\": null,\n" + 
					"        \"extraGift\": null,\n" + 
					"        \"buyOneGift\": null,\n" + 
					"        \"doubleGift\": null,\n" + 
					"        \"discountGiftDescArabic\": null,\n" + 
					"        \"extraGiftDescArabic\": null,\n" + 
					"        \"buyOneGiftDescArabic\": null,\n" + 
					"        \"doubleGiftDescArabic\": null,\n" + 
					"        \"discountGiftDescEnglish\": null,\n" + 
					"        \"extraGiftDescEnglish\": null,\n" + 
					"        \"buyOneGiftDescEnglish\": null,\n" + 
					"        \"doubleGiftDescEnglish\": null,\n" + 
					"        \"giftFullfilmentParameterName\": null,\n" + 
					"        \"giftFullfilmentParameterValue\": null,\n" + 
					"        \"menuValidityAr\": null,\n" + 
					"        \"menuValidityEn\": null,\n" + 
					"        \"categoryCssName\": null,\n" + 
					"        \"hideProduct\": false,\n" + 
					"        \"productPrice\": null,\n" + 
					"        \"englishDescription\": \"Super Connect 35LE – 2,500MB All + 2,500 Super MB\",\n" + 
					"        \"arabicDescription\": \"سوبر كونكت 35ج – 2,500 ميجا + 2,500 سوبر ميجابيتس\",\n" + 
					"        \"englishName\": \"Super Connect 35LE\",\n" + 
					"        \"arabicName\": \"سوبر كونكت 35ج \",\n" + 
					"        \"name\": \"NEW_MI_BUNDLE_PRODUCT_NO_3\",\n" + 
					"        \"relatedProduct\": [\n" + 
					"          {\n" + 
					"            \"id\": null,\n" + 
					"            \"fees\": \"0.0\",\n" + 
					"            \"active\": false,\n" + 
					"            \"isActive\": \"false\",\n" + 
					"            \"productRelationshipsList\": {},\n" + 
					"            \"operationResponses\": [],\n" + 
					"            \"commNameEnglish\": null,\n" + 
					"            \"commNameArabic\": null,\n" + 
					"            \"addOn\": \"4\",\n" + 
					"            \"newMIConnect\": true,\n" + 
					"            \"bazingaRateplan\": false,\n" + 
					"            \"extraSocial\": false,\n" + 
					"            \"extraStreaming\": false,\n" + 
					"            \"extraVoice\": true,\n" + 
					"            \"discountGift\": null,\n" + 
					"            \"extraGift\": null,\n" + 
					"            \"buyOneGift\": null,\n" + 
					"            \"doubleGift\": null,\n" + 
					"            \"discountGiftDescArabic\": null,\n" + 
					"            \"extraGiftDescArabic\": null,\n" + 
					"            \"buyOneGiftDescArabic\": null,\n" + 
					"            \"doubleGiftDescArabic\": null,\n" + 
					"            \"discountGiftDescEnglish\": null,\n" + 
					"            \"extraGiftDescEnglish\": null,\n" + 
					"            \"buyOneGiftDescEnglish\": null,\n" + 
					"            \"doubleGiftDescEnglish\": null,\n" + 
					"            \"giftFullfilmentParameterName\": null,\n" + 
					"            \"giftFullfilmentParameterValue\": null,\n" + 
					"            \"menuValidityAr\": null,\n" + 
					"            \"menuValidityEn\": null,\n" + 
					"            \"categoryCssName\": null,\n" + 
					"            \"hideProduct\": false,\n" + 
					"            \"productPrice\": null,\n" + 
					"            \"englishDescription\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE EN DESC\",\n" + 
					"            \"arabicDescription\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE AR DESC\",\n" + 
					"            \"englishName\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE EN NAME\",\n" + 
					"            \"arabicName\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE AR NAME\",\n" + 
					"            \"name\": \"NEW_MI_BUNDLE_ADDON_DOUBLE_VOICE\",\n" + 
					"            \"relatedProduct\": [],\n" + 
					"            \"extraEnglishDescription\": null,\n" + 
					"            \"extraArabicDescription\": null\n" + 
					"          },\n" + 
					"          {\n" + 
					"            \"id\": null,\n" + 
					"            \"fees\": \"0.0\",\n" + 
					"            \"active\": false,\n" + 
					"            \"isActive\": \"false\",\n" + 
					"            \"productRelationshipsList\": {},\n" + 
					"            \"operationResponses\": [],\n" + 
					"            \"commNameEnglish\": \"Super social\",\n" + 
					"            \"commNameArabic\": \"سوبر سوشيال\",\n" + 
					"            \"addOn\": \"1\",\n" + 
					"            \"newMIConnect\": true,\n" + 
					"            \"bazingaRateplan\": false,\n" + 
					"            \"extraSocial\": true,\n" + 
					"            \"extraStreaming\": false,\n" + 
					"            \"extraVoice\": false,\n" + 
					"            \"discountGift\": null,\n" + 
					"            \"extraGift\": null,\n" + 
					"            \"buyOneGift\": null,\n" + 
					"            \"doubleGift\": null,\n" + 
					"            \"discountGiftDescArabic\": null,\n" + 
					"            \"extraGiftDescArabic\": null,\n" + 
					"            \"buyOneGiftDescArabic\": null,\n" + 
					"            \"doubleGiftDescArabic\": null,\n" + 
					"            \"discountGiftDescEnglish\": null,\n" + 
					"            \"extraGiftDescEnglish\": null,\n" + 
					"            \"buyOneGiftDescEnglish\": null,\n" + 
					"            \"doubleGiftDescEnglish\": null,\n" + 
					"            \"giftFullfilmentParameterName\": null,\n" + 
					"            \"giftFullfilmentParameterValue\": null,\n" + 
					"            \"menuValidityAr\": null,\n" + 
					"            \"menuValidityEn\": null,\n" + 
					"            \"categoryCssName\": null,\n" + 
					"            \"hideProduct\": false,\n" + 
					"            \"productPrice\": null,\n" + 
					"            \"englishDescription\": \"Super social\",\n" + 
					"            \"arabicDescription\": \"ميجابيتس سوبر سوشيال \",\n" + 
					"            \"englishName\": \"Super social\",\n" + 
					"            \"arabicName\": \" ميجابيتس سوبر سوشيال\",\n" + 
					"            \"name\": \"NEW_MI_BUNDLE_DOUBLE_SOCIAL\",\n" + 
					"            \"relatedProduct\": [],\n" + 
					"            \"extraEnglishDescription\": null,\n" + 
					"            \"extraArabicDescription\": null\n" + 
					"          },\n" + 
					"          {\n" + 
					"            \"id\": null,\n" + 
					"            \"fees\": \"0.0\",\n" + 
					"            \"active\": false,\n" + 
					"            \"isActive\": \"false\",\n" + 
					"            \"productRelationshipsList\": {},\n" + 
					"            \"operationResponses\": [],\n" + 
					"            \"commNameEnglish\": \"Super streaming\",\n" + 
					"            \"commNameArabic\": \"سوبر ستريمنج\",\n" + 
					"            \"addOn\": \"2\",\n" + 
					"            \"newMIConnect\": true,\n" + 
					"            \"bazingaRateplan\": false,\n" + 
					"            \"extraSocial\": false,\n" + 
					"            \"extraStreaming\": true,\n" + 
					"            \"extraVoice\": false,\n" + 
					"            \"discountGift\": null,\n" + 
					"            \"extraGift\": null,\n" + 
					"            \"buyOneGift\": null,\n" + 
					"            \"doubleGift\": null,\n" + 
					"            \"discountGiftDescArabic\": null,\n" + 
					"            \"extraGiftDescArabic\": null,\n" + 
					"            \"buyOneGiftDescArabic\": null,\n" + 
					"            \"doubleGiftDescArabic\": null,\n" + 
					"            \"discountGiftDescEnglish\": null,\n" + 
					"            \"extraGiftDescEnglish\": null,\n" + 
					"            \"buyOneGiftDescEnglish\": null,\n" + 
					"            \"doubleGiftDescEnglish\": null,\n" + 
					"            \"giftFullfilmentParameterName\": null,\n" + 
					"            \"giftFullfilmentParameterValue\": null,\n" + 
					"            \"menuValidityAr\": null,\n" + 
					"            \"menuValidityEn\": null,\n" + 
					"            \"categoryCssName\": null,\n" + 
					"            \"hideProduct\": false,\n" + 
					"            \"productPrice\": null,\n" + 
					"            \"englishDescription\": \"Super streaming\",\n" + 
					"            \"arabicDescription\": \" ميجابيتس سوبر ستريمنج\",\n" + 
					"            \"englishName\": \"Super streaming\",\n" + 
					"            \"arabicName\": \"ميجابيتس سوبر ستريمنج\",\n" + 
					"            \"name\": \"NEW_MI_BUNDLE_DOUBLE_STREAMING\",\n" + 
					"            \"relatedProduct\": [],\n" + 
					"            \"extraEnglishDescription\": null,\n" + 
					"            \"extraArabicDescription\": null\n" + 
					"          }\n" + 
					"        ],\n" + 
					"        \"extraEnglishDescription\": \"ODS_OFFER_ADDITIONAL_DESC_EN\",\n" + 
					"        \"extraArabicDescription\": \"ODS_OFFER_ADDITIONAL_DESC_AR\"\n" + 
					"      },\n" + 
					"      {\n" + 
					"        \"id\": null,\n" + 
					"        \"fees\": \"65.0\",\n" + 
					"        \"active\": false,\n" + 
					"        \"isActive\": \"false\",\n" + 
					"        \"productRelationshipsList\": {\n" + 
					"          \"EXTRA\": [\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"4\",\n" + 
					"              \"newMIConnect\": true,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": true,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE EN DESC\",\n" + 
					"              \"arabicDescription\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE AR DESC\",\n" + 
					"              \"englishName\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE EN NAME\",\n" + 
					"              \"arabicName\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE AR NAME\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_DOUBLE_VOICE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": \"Super social\",\n" + 
					"              \"commNameArabic\": \"سوبر سوشيال\",\n" + 
					"              \"addOn\": \"1\",\n" + 
					"              \"newMIConnect\": true,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": true,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Super social\",\n" + 
					"              \"arabicDescription\": \"ميجابيتس سوبر سوشيال \",\n" + 
					"              \"englishName\": \"Super social\",\n" + 
					"              \"arabicName\": \" ميجابيتس سوبر سوشيال\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_DOUBLE_SOCIAL\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": \"Super streaming\",\n" + 
					"              \"commNameArabic\": \"سوبر ستريمنج\",\n" + 
					"              \"addOn\": \"2\",\n" + 
					"              \"newMIConnect\": true,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": true,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Super streaming\",\n" + 
					"              \"arabicDescription\": \" ميجابيتس سوبر ستريمنج\",\n" + 
					"              \"englishName\": \"Super streaming\",\n" + 
					"              \"arabicName\": \"ميجابيتس سوبر ستريمنج\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_DOUBLE_STREAMING\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            }\n" + 
					"          ],\n" + 
					"          \"EXTRA_ADDON\": [\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"A\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 200MB to be used on all websites \",\n" + 
					"              \"arabicDescription\": \"احصل علي 200 ميجا اضافية تستخدمهم علي كل المواقع.\",\n" + 
					"              \"englishName\": \"Super Extra 5LE – 200MB\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 5ج – 200 ميجا\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_1_ONDEMAND\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"RC\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 1250MB to be used on all websites, addon to be renewed with the bundle\",\n" + 
					"              \"arabicDescription\": \"احصل علي 1250 ميجا اضافية تستخدمهم علي كل المواقع, يتم تجديد الباقة الاضافية كل شهر مع الباقة\",\n" + 
					"              \"englishName\": \"Super Extra 20LE – 1250MB renewable\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 20ج – 1250 ميجا متجددة\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_3_RENEWABLE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": null,\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"SHARED NEW MI\",\n" + 
					"              \"arabicDescription\": \"SHARED NEW MI\",\n" + 
					"              \"englishName\": \"SHARED NEW MI\",\n" + 
					"              \"arabicName\": \"SHARED NEW MI\",\n" + 
					"              \"name\": \"SHARED_NEW_MI\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"RB\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 500MB to be used on all websites, addon to be renewed with the bundle\",\n" + 
					"              \"arabicDescription\": \"احصل علي 500 ميجا اضافية تستخدمهم علي كل المواقع, يتم تجديد الباقة الاضافية كل شهر مع الباقة\",\n" + 
					"              \"englishName\": \"Super Extra 10LE – 500MB renewable\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 10ج – 500 ميجا متجددة\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_2_RENEWABLE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"B\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 500MB to be used on all websites\",\n" + 
					"              \"arabicDescription\": \"احصل علي 500 ميجا اضافية تستخدمهم علي كل المواقع.\",\n" + 
					"              \"englishName\": \"Super Extra 10LE – 500MB\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 10ج – 500 ميجا\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_2_ONDEMAND\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"C\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 1250MB to be used on all websites\",\n" + 
					"              \"arabicDescription\": \"احصل علي 1250 ميجا اضافية تستخدمهم علي كل المواقع.\",\n" + 
					"              \"englishName\": \"Super Extra 20LE – 1250MB\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 20ج – 1250 ميجا\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_3_ONDEMAND\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"RA\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 200MB to be used on all websites, addon to be renewed with the bundle\",\n" + 
					"              \"arabicDescription\": \"احصل علي 200 ميجا اضافية تستخدمهم علي كل المواقع, يتم تجديد الباقة الاضافية كل شهر مع الباقة\",\n" + 
					"              \"englishName\": \"Super Extra 5LE – 200MB renewable\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 5ج – 200 ميجا متجددة\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_1_RENEWABLE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            }\n" + 
					"          ]\n" + 
					"        },\n" + 
					"        \"operationResponses\": [\n" + 
					"          {\n" + 
					"            \"arabicDescription\": null,\n" + 
					"            \"arabicName\": null,\n" + 
					"            \"bundleResourceID\": null,\n" + 
					"            \"englishDescription\": null,\n" + 
					"            \"englishName\": null,\n" + 
					"            \"operationName\": \"ACTIVATE_BUNDLE\",\n" + 
					"            \"operationType\": null,\n" + 
					"            \"order\": null,\n" + 
					"            \"suggestedMethod\": null,\n" + 
					"            \"group\": null\n" + 
					"          }\n" + 
					"        ],\n" + 
					"        \"commNameEnglish\": null,\n" + 
					"        \"commNameArabic\": null,\n" + 
					"        \"addOn\": null,\n" + 
					"        \"newMIConnect\": true,\n" + 
					"        \"bazingaRateplan\": false,\n" + 
					"        \"extraSocial\": false,\n" + 
					"        \"extraStreaming\": false,\n" + 
					"        \"extraVoice\": false,\n" + 
					"        \"discountGift\": null,\n" + 
					"        \"extraGift\": null,\n" + 
					"        \"buyOneGift\": null,\n" + 
					"        \"doubleGift\": null,\n" + 
					"        \"discountGiftDescArabic\": null,\n" + 
					"        \"extraGiftDescArabic\": null,\n" + 
					"        \"buyOneGiftDescArabic\": null,\n" + 
					"        \"doubleGiftDescArabic\": null,\n" + 
					"        \"discountGiftDescEnglish\": null,\n" + 
					"        \"extraGiftDescEnglish\": null,\n" + 
					"        \"buyOneGiftDescEnglish\": null,\n" + 
					"        \"doubleGiftDescEnglish\": null,\n" + 
					"        \"giftFullfilmentParameterName\": null,\n" + 
					"        \"giftFullfilmentParameterValue\": null,\n" + 
					"        \"menuValidityAr\": null,\n" + 
					"        \"menuValidityEn\": null,\n" + 
					"        \"categoryCssName\": null,\n" + 
					"        \"hideProduct\": false,\n" + 
					"        \"productPrice\": null,\n" + 
					"        \"englishDescription\": \"Super Connect 65LE – 5,000MB All + 5,000 Super MB\",\n" + 
					"        \"arabicDescription\": \"سوبر كونكت 65ج – 5,000 ميجا + 5,000 سوبر ميجابيتس\",\n" + 
					"        \"englishName\": \"Super Connect 65LE\",\n" + 
					"        \"arabicName\": \"سوبر كونكت 65ج \",\n" + 
					"        \"name\": \"NEW_MI_BUNDLE_PRODUCT_NO_4\",\n" + 
					"        \"relatedProduct\": [\n" + 
					"          {\n" + 
					"            \"id\": null,\n" + 
					"            \"fees\": \"0.0\",\n" + 
					"            \"active\": false,\n" + 
					"            \"isActive\": \"false\",\n" + 
					"            \"productRelationshipsList\": {},\n" + 
					"            \"operationResponses\": [],\n" + 
					"            \"commNameEnglish\": null,\n" + 
					"            \"commNameArabic\": null,\n" + 
					"            \"addOn\": \"4\",\n" + 
					"            \"newMIConnect\": true,\n" + 
					"            \"bazingaRateplan\": false,\n" + 
					"            \"extraSocial\": false,\n" + 
					"            \"extraStreaming\": false,\n" + 
					"            \"extraVoice\": true,\n" + 
					"            \"discountGift\": null,\n" + 
					"            \"extraGift\": null,\n" + 
					"            \"buyOneGift\": null,\n" + 
					"            \"doubleGift\": null,\n" + 
					"            \"discountGiftDescArabic\": null,\n" + 
					"            \"extraGiftDescArabic\": null,\n" + 
					"            \"buyOneGiftDescArabic\": null,\n" + 
					"            \"doubleGiftDescArabic\": null,\n" + 
					"            \"discountGiftDescEnglish\": null,\n" + 
					"            \"extraGiftDescEnglish\": null,\n" + 
					"            \"buyOneGiftDescEnglish\": null,\n" + 
					"            \"doubleGiftDescEnglish\": null,\n" + 
					"            \"giftFullfilmentParameterName\": null,\n" + 
					"            \"giftFullfilmentParameterValue\": null,\n" + 
					"            \"menuValidityAr\": null,\n" + 
					"            \"menuValidityEn\": null,\n" + 
					"            \"categoryCssName\": null,\n" + 
					"            \"hideProduct\": false,\n" + 
					"            \"productPrice\": null,\n" + 
					"            \"englishDescription\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE EN DESC\",\n" + 
					"            \"arabicDescription\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE AR DESC\",\n" + 
					"            \"englishName\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE EN NAME\",\n" + 
					"            \"arabicName\": \"NEW MI BUNDLE ADDON DOUBLE ADDON VOICE AR NAME\",\n" + 
					"            \"name\": \"NEW_MI_BUNDLE_ADDON_DOUBLE_VOICE\",\n" + 
					"            \"relatedProduct\": [],\n" + 
					"            \"extraEnglishDescription\": null,\n" + 
					"            \"extraArabicDescription\": null\n" + 
					"          },\n" + 
					"          {\n" + 
					"            \"id\": null,\n" + 
					"            \"fees\": \"0.0\",\n" + 
					"            \"active\": false,\n" + 
					"            \"isActive\": \"false\",\n" + 
					"            \"productRelationshipsList\": {},\n" + 
					"            \"operationResponses\": [],\n" + 
					"            \"commNameEnglish\": \"Super social\",\n" + 
					"            \"commNameArabic\": \"سوبر سوشيال\",\n" + 
					"            \"addOn\": \"1\",\n" + 
					"            \"newMIConnect\": true,\n" + 
					"            \"bazingaRateplan\": false,\n" + 
					"            \"extraSocial\": true,\n" + 
					"            \"extraStreaming\": false,\n" + 
					"            \"extraVoice\": false,\n" + 
					"            \"discountGift\": null,\n" + 
					"            \"extraGift\": null,\n" + 
					"            \"buyOneGift\": null,\n" + 
					"            \"doubleGift\": null,\n" + 
					"            \"discountGiftDescArabic\": null,\n" + 
					"            \"extraGiftDescArabic\": null,\n" + 
					"            \"buyOneGiftDescArabic\": null,\n" + 
					"            \"doubleGiftDescArabic\": null,\n" + 
					"            \"discountGiftDescEnglish\": null,\n" + 
					"            \"extraGiftDescEnglish\": null,\n" + 
					"            \"buyOneGiftDescEnglish\": null,\n" + 
					"            \"doubleGiftDescEnglish\": null,\n" + 
					"            \"giftFullfilmentParameterName\": null,\n" + 
					"            \"giftFullfilmentParameterValue\": null,\n" + 
					"            \"menuValidityAr\": null,\n" + 
					"            \"menuValidityEn\": null,\n" + 
					"            \"categoryCssName\": null,\n" + 
					"            \"hideProduct\": false,\n" + 
					"            \"productPrice\": null,\n" + 
					"            \"englishDescription\": \"Super social\",\n" + 
					"            \"arabicDescription\": \"ميجابيتس سوبر سوشيال \",\n" + 
					"            \"englishName\": \"Super social\",\n" + 
					"            \"arabicName\": \" ميجابيتس سوبر سوشيال\",\n" + 
					"            \"name\": \"NEW_MI_BUNDLE_DOUBLE_SOCIAL\",\n" + 
					"            \"relatedProduct\": [],\n" + 
					"            \"extraEnglishDescription\": null,\n" + 
					"            \"extraArabicDescription\": null\n" + 
					"          },\n" + 
					"          {\n" + 
					"            \"id\": null,\n" + 
					"            \"fees\": \"0.0\",\n" + 
					"            \"active\": false,\n" + 
					"            \"isActive\": \"false\",\n" + 
					"            \"productRelationshipsList\": {},\n" + 
					"            \"operationResponses\": [],\n" + 
					"            \"commNameEnglish\": \"Super streaming\",\n" + 
					"            \"commNameArabic\": \"سوبر ستريمنج\",\n" + 
					"            \"addOn\": \"2\",\n" + 
					"            \"newMIConnect\": true,\n" + 
					"            \"bazingaRateplan\": false,\n" + 
					"            \"extraSocial\": false,\n" + 
					"            \"extraStreaming\": true,\n" + 
					"            \"extraVoice\": false,\n" + 
					"            \"discountGift\": null,\n" + 
					"            \"extraGift\": null,\n" + 
					"            \"buyOneGift\": null,\n" + 
					"            \"doubleGift\": null,\n" + 
					"            \"discountGiftDescArabic\": null,\n" + 
					"            \"extraGiftDescArabic\": null,\n" + 
					"            \"buyOneGiftDescArabic\": null,\n" + 
					"            \"doubleGiftDescArabic\": null,\n" + 
					"            \"discountGiftDescEnglish\": null,\n" + 
					"            \"extraGiftDescEnglish\": null,\n" + 
					"            \"buyOneGiftDescEnglish\": null,\n" + 
					"            \"doubleGiftDescEnglish\": null,\n" + 
					"            \"giftFullfilmentParameterName\": null,\n" + 
					"            \"giftFullfilmentParameterValue\": null,\n" + 
					"            \"menuValidityAr\": null,\n" + 
					"            \"menuValidityEn\": null,\n" + 
					"            \"categoryCssName\": null,\n" + 
					"            \"hideProduct\": false,\n" + 
					"            \"productPrice\": null,\n" + 
					"            \"englishDescription\": \"Super streaming\",\n" + 
					"            \"arabicDescription\": \" ميجابيتس سوبر ستريمنج\",\n" + 
					"            \"englishName\": \"Super streaming\",\n" + 
					"            \"arabicName\": \"ميجابيتس سوبر ستريمنج\",\n" + 
					"            \"name\": \"NEW_MI_BUNDLE_DOUBLE_STREAMING\",\n" + 
					"            \"relatedProduct\": [],\n" + 
					"            \"extraEnglishDescription\": null,\n" + 
					"            \"extraArabicDescription\": null\n" + 
					"          }\n" + 
					"        ],\n" + 
					"        \"extraEnglishDescription\": \"ODS_OFFER_ADDITIONAL_DESC_EN\",\n" + 
					"        \"extraArabicDescription\": \"ODS_OFFER_ADDITIONAL_DESC_AR\"\n" + 
					"      },\n" + 
					"      {\n" + 
					"        \"id\": null,\n" + 
					"        \"fees\": \"130.0\",\n" + 
					"        \"active\": false,\n" + 
					"        \"isActive\": \"false\",\n" + 
					"        \"productRelationshipsList\": {\n" + 
					"          \"EXTRA_ADDON\": [\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"RA\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 200MB to be used on all websites, addon to be renewed with the bundle\",\n" + 
					"              \"arabicDescription\": \"احصل علي 200 ميجا اضافية تستخدمهم علي كل المواقع, يتم تجديد الباقة الاضافية كل شهر مع الباقة\",\n" + 
					"              \"englishName\": \"Super Extra 5LE – 200MB renewable\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 5ج – 200 ميجا متجددة\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_1_RENEWABLE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"A\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 200MB to be used on all websites \",\n" + 
					"              \"arabicDescription\": \"احصل علي 200 ميجا اضافية تستخدمهم علي كل المواقع.\",\n" + 
					"              \"englishName\": \"Super Extra 5LE – 200MB\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 5ج – 200 ميجا\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_1_ONDEMAND\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"C\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 1250MB to be used on all websites\",\n" + 
					"              \"arabicDescription\": \"احصل علي 1250 ميجا اضافية تستخدمهم علي كل المواقع.\",\n" + 
					"              \"englishName\": \"Super Extra 20LE – 1250MB\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 20ج – 1250 ميجا\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_3_ONDEMAND\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"B\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 500MB to be used on all websites\",\n" + 
					"              \"arabicDescription\": \"احصل علي 500 ميجا اضافية تستخدمهم علي كل المواقع.\",\n" + 
					"              \"englishName\": \"Super Extra 10LE – 500MB\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 10ج – 500 ميجا\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_2_ONDEMAND\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"RC\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 1250MB to be used on all websites, addon to be renewed with the bundle\",\n" + 
					"              \"arabicDescription\": \"احصل علي 1250 ميجا اضافية تستخدمهم علي كل المواقع, يتم تجديد الباقة الاضافية كل شهر مع الباقة\",\n" + 
					"              \"englishName\": \"Super Extra 20LE – 1250MB renewable\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 20ج – 1250 ميجا متجددة\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_3_RENEWABLE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": null,\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"SHARED NEW MI\",\n" + 
					"              \"arabicDescription\": \"SHARED NEW MI\",\n" + 
					"              \"englishName\": \"SHARED NEW MI\",\n" + 
					"              \"arabicName\": \"SHARED NEW MI\",\n" + 
					"              \"name\": \"SHARED_NEW_MI\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"RB\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 500MB to be used on all websites, addon to be renewed with the bundle\",\n" + 
					"              \"arabicDescription\": \"احصل علي 500 ميجا اضافية تستخدمهم علي كل المواقع, يتم تجديد الباقة الاضافية كل شهر مع الباقة\",\n" + 
					"              \"englishName\": \"Super Extra 10LE – 500MB renewable\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 10ج – 500 ميجا متجددة\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_2_RENEWABLE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            }\n" + 
					"          ]\n" + 
					"        },\n" + 
					"        \"operationResponses\": [\n" + 
					"          {\n" + 
					"            \"arabicDescription\": null,\n" + 
					"            \"arabicName\": null,\n" + 
					"            \"bundleResourceID\": null,\n" + 
					"            \"englishDescription\": null,\n" + 
					"            \"englishName\": null,\n" + 
					"            \"operationName\": \"ACTIVATE_BUNDLE\",\n" + 
					"            \"operationType\": null,\n" + 
					"            \"order\": null,\n" + 
					"            \"suggestedMethod\": null,\n" + 
					"            \"group\": null\n" + 
					"          }\n" + 
					"        ],\n" + 
					"        \"commNameEnglish\": null,\n" + 
					"        \"commNameArabic\": null,\n" + 
					"        \"addOn\": null,\n" + 
					"        \"newMIConnect\": true,\n" + 
					"        \"bazingaRateplan\": false,\n" + 
					"        \"extraSocial\": false,\n" + 
					"        \"extraStreaming\": false,\n" + 
					"        \"extraVoice\": false,\n" + 
					"        \"discountGift\": null,\n" + 
					"        \"extraGift\": null,\n" + 
					"        \"buyOneGift\": null,\n" + 
					"        \"doubleGift\": null,\n" + 
					"        \"discountGiftDescArabic\": null,\n" + 
					"        \"extraGiftDescArabic\": null,\n" + 
					"        \"buyOneGiftDescArabic\": null,\n" + 
					"        \"doubleGiftDescArabic\": null,\n" + 
					"        \"discountGiftDescEnglish\": null,\n" + 
					"        \"extraGiftDescEnglish\": null,\n" + 
					"        \"buyOneGiftDescEnglish\": null,\n" + 
					"        \"doubleGiftDescEnglish\": null,\n" + 
					"        \"giftFullfilmentParameterName\": null,\n" + 
					"        \"giftFullfilmentParameterValue\": null,\n" + 
					"        \"menuValidityAr\": null,\n" + 
					"        \"menuValidityEn\": null,\n" + 
					"        \"categoryCssName\": null,\n" + 
					"        \"hideProduct\": false,\n" + 
					"        \"productPrice\": null,\n" + 
					"        \"englishDescription\": \"Super Connect 130LE –20,000MB\",\n" + 
					"        \"arabicDescription\": \"سوبر كونكت 130ج – 20,000 ميجا\",\n" + 
					"        \"englishName\": \"Super Connect 130LE\",\n" + 
					"        \"arabicName\": \"سوبر كونكت 130ج \",\n" + 
					"        \"name\": \"NEW_MI_BUNDLE_PRODUCT_NO_5\",\n" + 
					"        \"relatedProduct\": [],\n" + 
					"        \"extraEnglishDescription\": null,\n" + 
					"        \"extraArabicDescription\": null\n" + 
					"      },\n" + 
					"      {\n" + 
					"        \"id\": null,\n" + 
					"        \"fees\": \"200.0\",\n" + 
					"        \"active\": false,\n" + 
					"        \"isActive\": \"false\",\n" + 
					"        \"productRelationshipsList\": {\n" + 
					"          \"EXTRA_ADDON\": [\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": null,\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"SHARED NEW MI\",\n" + 
					"              \"arabicDescription\": \"SHARED NEW MI\",\n" + 
					"              \"englishName\": \"SHARED NEW MI\",\n" + 
					"              \"arabicName\": \"SHARED NEW MI\",\n" + 
					"              \"name\": \"SHARED_NEW_MI\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"RC\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 1250MB to be used on all websites, addon to be renewed with the bundle\",\n" + 
					"              \"arabicDescription\": \"احصل علي 1250 ميجا اضافية تستخدمهم علي كل المواقع, يتم تجديد الباقة الاضافية كل شهر مع الباقة\",\n" + 
					"              \"englishName\": \"Super Extra 20LE – 1250MB renewable\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 20ج – 1250 ميجا متجددة\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_3_RENEWABLE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"RB\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 500MB to be used on all websites, addon to be renewed with the bundle\",\n" + 
					"              \"arabicDescription\": \"احصل علي 500 ميجا اضافية تستخدمهم علي كل المواقع, يتم تجديد الباقة الاضافية كل شهر مع الباقة\",\n" + 
					"              \"englishName\": \"Super Extra 10LE – 500MB renewable\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 10ج – 500 ميجا متجددة\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_2_RENEWABLE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"C\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 1250MB to be used on all websites\",\n" + 
					"              \"arabicDescription\": \"احصل علي 1250 ميجا اضافية تستخدمهم علي كل المواقع.\",\n" + 
					"              \"englishName\": \"Super Extra 20LE – 1250MB\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 20ج – 1250 ميجا\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_3_ONDEMAND\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"A\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 200MB to be used on all websites \",\n" + 
					"              \"arabicDescription\": \"احصل علي 200 ميجا اضافية تستخدمهم علي كل المواقع.\",\n" + 
					"              \"englishName\": \"Super Extra 5LE – 200MB\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 5ج – 200 ميجا\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_1_ONDEMAND\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"RA\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 200MB to be used on all websites, addon to be renewed with the bundle\",\n" + 
					"              \"arabicDescription\": \"احصل علي 200 ميجا اضافية تستخدمهم علي كل المواقع, يتم تجديد الباقة الاضافية كل شهر مع الباقة\",\n" + 
					"              \"englishName\": \"Super Extra 5LE – 200MB renewable\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 5ج – 200 ميجا متجددة\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_1_RENEWABLE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"B\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 500MB to be used on all websites\",\n" + 
					"              \"arabicDescription\": \"احصل علي 500 ميجا اضافية تستخدمهم علي كل المواقع.\",\n" + 
					"              \"englishName\": \"Super Extra 10LE – 500MB\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 10ج – 500 ميجا\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_2_ONDEMAND\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            }\n" + 
					"          ]\n" + 
					"        },\n" + 
					"        \"operationResponses\": [\n" + 
					"          {\n" + 
					"            \"arabicDescription\": null,\n" + 
					"            \"arabicName\": null,\n" + 
					"            \"bundleResourceID\": null,\n" + 
					"            \"englishDescription\": null,\n" + 
					"            \"englishName\": null,\n" + 
					"            \"operationName\": \"ACTIVATE_BUNDLE\",\n" + 
					"            \"operationType\": null,\n" + 
					"            \"order\": null,\n" + 
					"            \"suggestedMethod\": null,\n" + 
					"            \"group\": null\n" + 
					"          }\n" + 
					"        ],\n" + 
					"        \"commNameEnglish\": null,\n" + 
					"        \"commNameArabic\": null,\n" + 
					"        \"addOn\": null,\n" + 
					"        \"newMIConnect\": true,\n" + 
					"        \"bazingaRateplan\": false,\n" + 
					"        \"extraSocial\": false,\n" + 
					"        \"extraStreaming\": false,\n" + 
					"        \"extraVoice\": false,\n" + 
					"        \"discountGift\": null,\n" + 
					"        \"extraGift\": null,\n" + 
					"        \"buyOneGift\": null,\n" + 
					"        \"doubleGift\": null,\n" + 
					"        \"discountGiftDescArabic\": null,\n" + 
					"        \"extraGiftDescArabic\": null,\n" + 
					"        \"buyOneGiftDescArabic\": null,\n" + 
					"        \"doubleGiftDescArabic\": null,\n" + 
					"        \"discountGiftDescEnglish\": null,\n" + 
					"        \"extraGiftDescEnglish\": null,\n" + 
					"        \"buyOneGiftDescEnglish\": null,\n" + 
					"        \"doubleGiftDescEnglish\": null,\n" + 
					"        \"giftFullfilmentParameterName\": null,\n" + 
					"        \"giftFullfilmentParameterValue\": null,\n" + 
					"        \"menuValidityAr\": null,\n" + 
					"        \"menuValidityEn\": null,\n" + 
					"        \"categoryCssName\": null,\n" + 
					"        \"hideProduct\": false,\n" + 
					"        \"productPrice\": null,\n" + 
					"        \"englishDescription\": \"Super Connect 200LE – 32,000MB\",\n" + 
					"        \"arabicDescription\": \"سوبر كونكت 200ج – 32,000 ميجا\",\n" + 
					"        \"englishName\": \"Super Connect 200LE\",\n" + 
					"        \"arabicName\": \"سوبر كونكت 200ج\",\n" + 
					"        \"name\": \"NEW_MI_BUNDLE_PRODUCT_NO_6\",\n" + 
					"        \"relatedProduct\": [],\n" + 
					"        \"extraEnglishDescription\": \"ODS_OFFER_ADDITIONAL_DESC_EN\",\n" + 
					"        \"extraArabicDescription\": \"ODS_OFFER_ADDITIONAL_DESC_AR\"\n" + 
					"      },\n" + 
					"      {\n" + 
					"        \"id\": null,\n" + 
					"        \"fees\": \"300.0\",\n" + 
					"        \"active\": false,\n" + 
					"        \"isActive\": \"false\",\n" + 
					"        \"productRelationshipsList\": {\n" + 
					"          \"EXTRA_ADDON\": [\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"B\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 500MB to be used on all websites\",\n" + 
					"              \"arabicDescription\": \"احصل علي 500 ميجا اضافية تستخدمهم علي كل المواقع.\",\n" + 
					"              \"englishName\": \"Super Extra 10LE – 500MB\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 10ج – 500 ميجا\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_2_ONDEMAND\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"C\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 1250MB to be used on all websites\",\n" + 
					"              \"arabicDescription\": \"احصل علي 1250 ميجا اضافية تستخدمهم علي كل المواقع.\",\n" + 
					"              \"englishName\": \"Super Extra 20LE – 1250MB\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 20ج – 1250 ميجا\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_3_ONDEMAND\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"A\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 200MB to be used on all websites \",\n" + 
					"              \"arabicDescription\": \"احصل علي 200 ميجا اضافية تستخدمهم علي كل المواقع.\",\n" + 
					"              \"englishName\": \"Super Extra 5LE – 200MB\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 5ج – 200 ميجا\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_1_ONDEMAND\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"RA\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 200MB to be used on all websites, addon to be renewed with the bundle\",\n" + 
					"              \"arabicDescription\": \"احصل علي 200 ميجا اضافية تستخدمهم علي كل المواقع, يتم تجديد الباقة الاضافية كل شهر مع الباقة\",\n" + 
					"              \"englishName\": \"Super Extra 5LE – 200MB renewable\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 5ج – 200 ميجا متجددة\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_1_RENEWABLE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"RB\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 500MB to be used on all websites, addon to be renewed with the bundle\",\n" + 
					"              \"arabicDescription\": \"احصل علي 500 ميجا اضافية تستخدمهم علي كل المواقع, يتم تجديد الباقة الاضافية كل شهر مع الباقة\",\n" + 
					"              \"englishName\": \"Super Extra 10LE – 500MB renewable\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 10ج – 500 ميجا متجددة\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_2_RENEWABLE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": \"RC\",\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"Get extra 1250MB to be used on all websites, addon to be renewed with the bundle\",\n" + 
					"              \"arabicDescription\": \"احصل علي 1250 ميجا اضافية تستخدمهم علي كل المواقع, يتم تجديد الباقة الاضافية كل شهر مع الباقة\",\n" + 
					"              \"englishName\": \"Super Extra 20LE – 1250MB renewable\",\n" + 
					"              \"arabicName\": \"سوبر اكسترا 20ج – 1250 ميجا متجددة\",\n" + 
					"              \"name\": \"NEW_MI_BUNDLE_ADDON_EXTRA_ADDON_3_RENEWABLE\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            },\n" + 
					"            {\n" + 
					"              \"id\": null,\n" + 
					"              \"fees\": \"0.0\",\n" + 
					"              \"active\": false,\n" + 
					"              \"isActive\": \"false\",\n" + 
					"              \"productRelationshipsList\": {},\n" + 
					"              \"operationResponses\": [],\n" + 
					"              \"commNameEnglish\": null,\n" + 
					"              \"commNameArabic\": null,\n" + 
					"              \"addOn\": null,\n" + 
					"              \"newMIConnect\": false,\n" + 
					"              \"bazingaRateplan\": false,\n" + 
					"              \"extraSocial\": false,\n" + 
					"              \"extraStreaming\": false,\n" + 
					"              \"extraVoice\": false,\n" + 
					"              \"discountGift\": null,\n" + 
					"              \"extraGift\": null,\n" + 
					"              \"buyOneGift\": null,\n" + 
					"              \"doubleGift\": null,\n" + 
					"              \"discountGiftDescArabic\": null,\n" + 
					"              \"extraGiftDescArabic\": null,\n" + 
					"              \"buyOneGiftDescArabic\": null,\n" + 
					"              \"doubleGiftDescArabic\": null,\n" + 
					"              \"discountGiftDescEnglish\": null,\n" + 
					"              \"extraGiftDescEnglish\": null,\n" + 
					"              \"buyOneGiftDescEnglish\": null,\n" + 
					"              \"doubleGiftDescEnglish\": null,\n" + 
					"              \"giftFullfilmentParameterName\": null,\n" + 
					"              \"giftFullfilmentParameterValue\": null,\n" + 
					"              \"menuValidityAr\": null,\n" + 
					"              \"menuValidityEn\": null,\n" + 
					"              \"categoryCssName\": null,\n" + 
					"              \"hideProduct\": false,\n" + 
					"              \"productPrice\": null,\n" + 
					"              \"englishDescription\": \"SHARED NEW MI\",\n" + 
					"              \"arabicDescription\": \"SHARED NEW MI\",\n" + 
					"              \"englishName\": \"SHARED NEW MI\",\n" + 
					"              \"arabicName\": \"SHARED NEW MI\",\n" + 
					"              \"name\": \"SHARED_NEW_MI\",\n" + 
					"              \"relatedProduct\": [],\n" + 
					"              \"extraEnglishDescription\": null,\n" + 
					"              \"extraArabicDescription\": null\n" + 
					"            }\n" + 
					"          ]\n" + 
					"        },\n" + 
					"        \"operationResponses\": [\n" + 
					"          {\n" + 
					"            \"arabicDescription\": null,\n" + 
					"            \"arabicName\": null,\n" + 
					"            \"bundleResourceID\": null,\n" + 
					"            \"englishDescription\": null,\n" + 
					"            \"englishName\": null,\n" + 
					"            \"operationName\": \"ACTIVATE_BUNDLE\",\n" + 
					"            \"operationType\": null,\n" + 
					"            \"order\": null,\n" + 
					"            \"suggestedMethod\": null,\n" + 
					"            \"group\": null\n" + 
					"          }\n" + 
					"        ],\n" + 
					"        \"commNameEnglish\": null,\n" + 
					"        \"commNameArabic\": null,\n" + 
					"        \"addOn\": null,\n" + 
					"        \"newMIConnect\": true,\n" + 
					"        \"bazingaRateplan\": false,\n" + 
					"        \"extraSocial\": false,\n" + 
					"        \"extraStreaming\": false,\n" + 
					"        \"extraVoice\": false,\n" + 
					"        \"discountGift\": null,\n" + 
					"        \"extraGift\": null,\n" + 
					"        \"buyOneGift\": null,\n" + 
					"        \"doubleGift\": null,\n" + 
					"        \"discountGiftDescArabic\": null,\n" + 
					"        \"extraGiftDescArabic\": null,\n" + 
					"        \"buyOneGiftDescArabic\": null,\n" + 
					"        \"doubleGiftDescArabic\": null,\n" + 
					"        \"discountGiftDescEnglish\": null,\n" + 
					"        \"extraGiftDescEnglish\": null,\n" + 
					"        \"buyOneGiftDescEnglish\": null,\n" + 
					"        \"doubleGiftDescEnglish\": null,\n" + 
					"        \"giftFullfilmentParameterName\": null,\n" + 
					"        \"giftFullfilmentParameterValue\": null,\n" + 
					"        \"menuValidityAr\": null,\n" + 
					"        \"menuValidityEn\": null,\n" + 
					"        \"categoryCssName\": null,\n" + 
					"        \"hideProduct\": false,\n" + 
					"        \"productPrice\": null,\n" + 
					"        \"englishDescription\": \"Super Connect 300LE – 50,000MB\",\n" + 
					"        \"arabicDescription\": \"سوبر كونكت 300ج – 50,000 ميجا\",\n" + 
					"        \"englishName\": \"Super Connect 300LE\",\n" + 
					"        \"arabicName\": \"سوبر كونكت 300ج \",\n" + 
					"        \"name\": \"NEW_MI_BUNDLE_PRODUCT_NO_7\",\n" + 
					"        \"relatedProduct\": [],\n" + 
					"        \"extraEnglishDescription\": \"ODS_OFFER_ADDITIONAL_DESC_EN\",\n" + 
					"        \"extraArabicDescription\": \"ODS_OFFER_ADDITIONAL_DESC_AR\"\n" + 
					"      }\n" + 
					"    ],\n" + 
					"    \"childEligibleProductModels\": []\n" + 
					"  },\n" + 
					"  {\n" + 
					"    \"category\": {\n" + 
					"      \"categoryNameAr\": \"كونكت كونترول\",\n" + 
					"      \"categoryNameEn\": \"Connect Control\",\n" + 
					"      \"categoryId\": \"PORTAL_CYBER\",\n" + 
					"      \"offerId\": null\n" + 
					"    },\n" + 
					"    \"products\": [\n" + 
					"      {\n" + 
					"        \"id\": \"15423\",\n" + 
					"        \"fees\": \"0.0\",\n" + 
					"        \"active\": false,\n" + 
					"        \"isActive\": \"false\",\n" + 
					"        \"productRelationshipsList\": {},\n" + 
					"        \"operationResponses\": [\n" + 
					"          {\n" + 
					"            \"arabicDescription\": \"اشتراك\",\n" + 
					"            \"arabicName\": \"اشتراك\",\n" + 
					"            \"bundleResourceID\": \"1000200\",\n" + 
					"            \"englishDescription\": \"ACTIVATE\",\n" + 
					"            \"englishName\": \"ACTIVATE\",\n" + 
					"            \"operationName\": \"ACTIVATE\",\n" + 
					"            \"operationType\": null,\n" + 
					"            \"order\": null,\n" + 
					"            \"suggestedMethod\": null,\n" + 
					"            \"group\": null\n" + 
					"          }\n" + 
					"        ],\n" + 
					"        \"commNameEnglish\": null,\n" + 
					"        \"commNameArabic\": null,\n" + 
					"        \"addOn\": null,\n" + 
					"        \"newMIConnect\": false,\n" + 
					"        \"bazingaRateplan\": false,\n" + 
					"        \"extraSocial\": false,\n" + 
					"        \"extraStreaming\": false,\n" + 
					"        \"extraVoice\": false,\n" + 
					"        \"discountGift\": null,\n" + 
					"        \"extraGift\": null,\n" + 
					"        \"buyOneGift\": null,\n" + 
					"        \"doubleGift\": null,\n" + 
					"        \"discountGiftDescArabic\": null,\n" + 
					"        \"extraGiftDescArabic\": null,\n" + 
					"        \"buyOneGiftDescArabic\": null,\n" + 
					"        \"doubleGiftDescArabic\": null,\n" + 
					"        \"discountGiftDescEnglish\": null,\n" + 
					"        \"extraGiftDescEnglish\": null,\n" + 
					"        \"buyOneGiftDescEnglish\": null,\n" + 
					"        \"doubleGiftDescEnglish\": null,\n" + 
					"        \"giftFullfilmentParameterName\": null,\n" + 
					"        \"giftFullfilmentParameterValue\": null,\n" + 
					"        \"menuValidityAr\": null,\n" + 
					"        \"menuValidityEn\": null,\n" + 
					"        \"categoryCssName\": null,\n" + 
					"        \"hideProduct\": false,\n" + 
					"        \"productPrice\": null,\n" + 
					"        \"englishDescription\": \"CYBER BUNDLE 2\",\n" + 
					"        \"arabicDescription\": \"سايبر 2\",\n" + 
					"        \"englishName\": \"CYBER BUNDLE 2\",\n" + 
					"        \"arabicName\": \"سايبر 2\",\n" + 
					"        \"name\": \"CYBER_BUNDLE_2\",\n" + 
					"        \"relatedProduct\": [],\n" + 
					"        \"extraEnglishDescription\": null,\n" + 
					"        \"extraArabicDescription\": null\n" + 
					"      },\n" + 
					"      {\n" + 
					"        \"id\": \"15422\",\n" + 
					"        \"fees\": \"0.0\",\n" + 
					"        \"active\": false,\n" + 
					"        \"isActive\": \"false\",\n" + 
					"        \"productRelationshipsList\": {},\n" + 
					"        \"operationResponses\": [\n" + 
					"          {\n" + 
					"            \"arabicDescription\": \"اشتراك\",\n" + 
					"            \"arabicName\": \"اشتراك\",\n" + 
					"            \"bundleResourceID\": \"1000200\",\n" + 
					"            \"englishDescription\": \"ACTIVATE\",\n" + 
					"            \"englishName\": \"ACTIVATE\",\n" + 
					"            \"operationName\": \"ACTIVATE\",\n" + 
					"            \"operationType\": null,\n" + 
					"            \"order\": null,\n" + 
					"            \"suggestedMethod\": null,\n" + 
					"            \"group\": null\n" + 
					"          }\n" + 
					"        ],\n" + 
					"        \"commNameEnglish\": null,\n" + 
					"        \"commNameArabic\": null,\n" + 
					"        \"addOn\": null,\n" + 
					"        \"newMIConnect\": false,\n" + 
					"        \"bazingaRateplan\": false,\n" + 
					"        \"extraSocial\": false,\n" + 
					"        \"extraStreaming\": false,\n" + 
					"        \"extraVoice\": false,\n" + 
					"        \"discountGift\": null,\n" + 
					"        \"extraGift\": null,\n" + 
					"        \"buyOneGift\": null,\n" + 
					"        \"doubleGift\": null,\n" + 
					"        \"discountGiftDescArabic\": null,\n" + 
					"        \"extraGiftDescArabic\": null,\n" + 
					"        \"buyOneGiftDescArabic\": null,\n" + 
					"        \"doubleGiftDescArabic\": null,\n" + 
					"        \"discountGiftDescEnglish\": null,\n" + 
					"        \"extraGiftDescEnglish\": null,\n" + 
					"        \"buyOneGiftDescEnglish\": null,\n" + 
					"        \"doubleGiftDescEnglish\": null,\n" + 
					"        \"giftFullfilmentParameterName\": null,\n" + 
					"        \"giftFullfilmentParameterValue\": null,\n" + 
					"        \"menuValidityAr\": null,\n" + 
					"        \"menuValidityEn\": null,\n" + 
					"        \"categoryCssName\": null,\n" + 
					"        \"hideProduct\": false,\n" + 
					"        \"productPrice\": null,\n" + 
					"        \"englishDescription\": \"CYBER BUNDLE 1\",\n" + 
					"        \"arabicDescription\": \"سايبر 1\",\n" + 
					"        \"englishName\": \"CYBER BUNDLE 1\",\n" + 
					"        \"arabicName\": \"سايبر 1\",\n" + 
					"        \"name\": \"CYBER_BUNDLE_1\",\n" + 
					"        \"relatedProduct\": [],\n" + 
					"        \"extraEnglishDescription\": null,\n" + 
					"        \"extraArabicDescription\": null\n" + 
					"      }\n" + 
					"    ],\n" + 
					"    \"childEligibleProductModels\": []\n" + 
					"  }\n" + 
					"]";
		}
*/	
}
