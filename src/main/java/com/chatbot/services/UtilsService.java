package com.chatbot.services;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.chatbot.entity.BotButton;
import com.chatbot.entity.BotButtonTemplateMSG;
import com.chatbot.entity.BotInteraction;
import com.chatbot.entity.BotInteractionMessage;
import com.chatbot.entity.BotQuickReplyMessage;
import com.chatbot.entity.BotTemplateElement;
import com.chatbot.entity.BotTextMessage;
import com.chatbot.entity.BotTextResponseMapping;
import com.chatbot.entity.BotWebserviceMessage;
import com.chatbot.entity.CustomerProfile;
import com.github.messenger4j.Messenger;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.message.quickreply.QuickReply;
import com.github.messenger4j.send.message.template.ButtonTemplate;
import com.github.messenger4j.send.message.template.Template;
import com.github.messenger4j.send.message.template.button.Button;
import com.github.messenger4j.userprofile.UserProfile;

public interface UtilsService {

	public  MessagePayload getRelatedProductFromJsonByBundleId(JSONArray arrayResponse , String productId ,String senderId ,ChatBotService chatBotService,String locale);
	
	
	public MessagePayload getProductsByCategoryIfLego(String senderId, String locale, JSONArray products) throws JSONException;
	
	
	public  MessagePayload getProductsByCategoryNotLego(String senderId, String locale, JSONArray products) throws JSONException;
	
	
	public  MessagePayload getProductsFromJsonByCategory(JSONArray arrayResponse , String category ,String senderId ,ChatBotService chatBotService , String locale); 
	
	
	public  void setCustomerProfileLocalAsEnglish(CustomerProfile customerProfile , ChatBotService chatBotService) ;
	

	public   void setCustomerProfileLocalAsArabic(CustomerProfile customerProfile ,ChatBotService chatBotService) ;
	
	
	public  CustomerProfile setLinkingInfoForCustomer(String senderId ,Messenger messenger ,String CustomerDial,ChatBotService chatBotService);
	
	
	public  String getTextForQuickReply(String local , BotQuickReplyMessage botQuickReplyMessage,String userDial);
	
	
	public  void createTextMessageInDynamicScenario(String senderId, ArrayList<MessagePayload> messagePayloadList,
			BotWebserviceMessage botWebserviceMessage, String jsonBodyString , ChatBotService chatBotService ,String local) throws JSONException;
		
	public  Map<String,String> callPostWebService(BotWebserviceMessage botWebserviceMessage, String jsonParam , ChatBotService chatBotService ,String senderId , ArrayList<String> paramList) ;
	
	
	public  Map<String,String> callGetWebService(BotWebserviceMessage botWebserviceMessage ,String senderId,ChatBotService chatBotService);
	
	public  String getTextValueForBotTextMessage(BotTextMessage botTextMessage ,String local) ;
	
	public  MessagePayload responseInCaseStaticScenario(String payload, String senderId, String userFirstName,
			BotInteraction botInteraction,BotInteractionMessage botInteractionMessage, 
			Long messageTypeId, Long messageId , ChatBotService chatBotService ,String parentPayLoad ,String locale,String userDial);
	
	
	public  String getKeysString(BotTextResponseMapping botTextResponseMapping , String local) ;
	
	
	public  void getTextMessageIfResponseIsString(String senderId, ArrayList<MessagePayload> messagePayloadList,
			BotWebserviceMessage botWebserviceMessage, String jsonBodyString , ChatBotService chatBotService ,String local) throws JSONException;
	
	
	public  MessagePayload getBundleCategories(JSONArray arrayResponse ,String senderId ,ChatBotService chatBotService , String locale,String dial);
	
	
	public  MessagePayload getExtraMobileInternetAddonsByCategory(JSONArray arrayResponse ,String senderId ,ChatBotService chatBotService , String locale ,String addonId);
	
	
	public  void getTextMessageIfResponseIsObject(String senderId, ArrayList<MessagePayload> messagePayloadList,
			BotWebserviceMessage botWebserviceMessage, String jsonBodyString , ChatBotService chatBotService ,String local) throws JSONException ;
	
	
	public  String getTextForBotTextResponseMapping(String local ,BotTextResponseMapping botTextResponseMapping) ;
	
	public  void getTextMessageIfResponseIsArray(String senderId, ArrayList<MessagePayload> messagePayloadList,
			BotWebserviceMessage botWebserviceMessage, String jsonBodyString ,ChatBotService chatBotService ,String local) throws JSONException;
	
	
	public  MessagePayload createMessagePayload(String parentPayLoad , ChatBotService chatBotService , String senderId ,String local,BotWebserviceMessage botWebserviceMessage,String dialNumber);
	
	
	public  List<QuickReply> createQuickReply(ChatBotService chatBotService , Long messageId ,String local) ;
		
	
	public  String getTextForButtonTemplate(String local , BotButtonTemplateMSG botButtonTemplateMSG) ;
	
	
	public  ButtonTemplate createButtonTemplateInScenario(BotInteractionMessage botInteractionMessage , ChatBotService chatBotService ,String local,String dialNumber) ;
	
	
	public  String getSubTitletBotTemplateElement(BotTemplateElement botTemplateElement , String local) ;
	
	
	public  String getTitletBotTemplateElement(BotTemplateElement botTemplateElement , String local) ;
	
	
	public  Template createGenericTemplate(Long messageId , ChatBotService chatBotService ,String userLocale ,BotWebserviceMessage botWebserviceMessage,JSONObject jsonObject,String dialNumber,ArrayList<String> consumptionNames) ;
	
	
	public  Map<String,ArrayList<String>> inCaseZeroLevelJsonObject(String [] keys ,JSONObject jsonObject , String msg ,String locale);
	
	
	public  Map<String,ArrayList<String>> inCaseZeroLevelJsonArray(String [] keys , JSONArray rootArray , String msg, String locale);
	
	
	public  Map<String,ArrayList<String>> inCaseOneLevelJsonObject(String [] paths , String [] keys ,JSONObject jsonObject , String msg,String locale);
	
	
	public  Map<String,ArrayList<String>> inCaseOneLevelJsonArrayForTextMessage(String [] paths , String [] keys ,JSONArray rootArray,String msg,String locale);
	
	
	public   ArrayList<String> getValuesFromJson(JSONObject jsonObject, String[] keys);
	
	
	public  String replaceValuesByMapping(ArrayList<String> values, String MSg,String locale) ;
	
	
	public  Map<String,ArrayList<String>> inCaseTwoLevelJsonObject(JSONObject jsonObject  , String [] paths , String [] keys ,String msg,String locale);
	
	
	public  Map<String,ArrayList<String>> inCaseTwoLevelJsonArrayForTextMessage(JSONArray rootArray , String [] paths , String [] keys ,String msg,String locale);
	
	
	public   String [] getPaths(String path);
	
	
	public  String [] getKeys(String key );
	
	
	public  Map<String,ArrayList<String>> switchToObjectMode(JSONObject jsonResponse , String [] paths , String [] keys , String msg,String locale) ;
	
	
	public  Map<String,ArrayList<String>> switchToArrayMode(JSONArray jsonResponse , String [] paths , String [] keys , String msg,String locale);
	
	
	public   Button createButton(BotButton botButton , String local ,JSONObject jsonObject,String dialNumber);
	

	

	
	
	
	
	
	
	
	
	
	
	
	
}
