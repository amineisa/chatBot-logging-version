package com.chatbot.util;

public class Constants {

	public static final String CHANEL_PARAM="CHATBOT";
	public static final String PREFIX_ADDONSUBSCRIPE ="subaddon_";
	public static final String PREFIX_RATEPLAN_SUBSCRIPTION="sub_";
	public static final String PREFIX_MOBILEINTERNET_ADDON = "MIAddon";
	public static final String PREFIX_RELATED_PRODUCTS = "related";
	public static final String PREFIX_CONFIRM_MOBILEINTERNET_SUBSCRIPTION="mi_yes_subscripe";
	public static final String PREFIX_RELATED_PRODUCTS_SUBSCRIPTION ="relatedproductsubscription";
	
	
	
	public static final String PAYLOAD_MOBILE_INTERNET_SUBSCRIPTION_CANCEL ="mi_no_subscripe";
	public static final String PAYLOAD_CHANGE_BUNDLE ="change bundle";
	public static final String PAYLOAD_ADDON_SUBSCRIPTION ="subscribe addon";
	public static final String PAYLOAD_BUY_ADDONS="buy addons";
	public static final String PAYLOAD_RELATED_PRODUCTS="related product";
	public static final String PAYLOAD_MOBILE_INTERNET_CONFIRMATION_MSG = "MI Bundle subscription confirmation msg";
	public static final String PAYLOAD_CANCEL_PAY_OR_RECHARGE="cancel pay or recharge";
	public static final String PAYLOAD_CANCEL_RECHARGING = "cancel recharging";
	public static final String PAYLOAD_CANCEL_BILL_PAYMENT = "cancel paying bill";
	public static final String PAYLOAD_FAULT_MSG = "fault MSG";
	public static final String PAYLOAD_ACCOUNT_DETAILS = "account details";
	public static final String PAYLOAD_PREPAID = "prepaid";
	public static final String PAYLOAD_VIEW_CONNECT_DETAILS ="view connect details";
	public static final String PAYLOAD_VIEW_RATEPLAN_DETAILS="view rateplan details";
	public static final String PAYLOAD_RATEPLAN_AND_CONNECT="view rateplan and connect details";
	public static final String PAYLOAD_VIEW_ROOT_CONNECT_DETAILS = "view root connect details";
	public static final String PAYLOAD_RATEPLAN_DETAILS ="rateplan details";
	public static final String PAYLOAD_CONSUMPTION="consumption";
	public static final String PAYLOAD_VIEW_MOBILEINTERNET_SUBSCRIPTION = "view mobile internet subscribe";
	public static final String PAYLOAD_RATEPLAN_ADDONS_CONSUMPTION = "rateplan addons consumption";
	public static final String PAYLOAD_MOBILEINTERNET_ADDONS_CONSUMPTION = "mobile internet addon consumption";
	public static final String PAYLOAD_POSTPAID_DIAL = "postpaid";
	public static final String PAYLOAD_BUY_ADDONS_ROOT = "buy addons root";
	public static final String PAYLOAD_UNEXPECTED_PAYLOAD = "unexpected";
	
		
	public static final String NOTELIGIBLE_ELEMENT_TITLE_EN = "Sorry !";
	public static final String NOTELIGIBLE_ELEMENT_TITLE_AR = "! عفوا"; 
	
	public static final String NOTELIGIBLE_ELEMENT_SUBTITLE_EN = " Your Dial Not Eligible to any addons ";
	public static final String NOTELIGIBLE_ELEMENT_SUBTITLE_AR = " عفوا رقمك ليس له اي اضافات";
	
	
	public static String profilePayloads = PAYLOAD_VIEW_CONNECT_DETAILS+","+PAYLOAD_RATEPLAN_AND_CONNECT+","+PAYLOAD_VIEW_RATEPLAN_DETAILS+","+PAYLOAD_RATEPLAN_DETAILS+","+PAYLOAD_CONSUMPTION;	public static final String RESPOSE_MSG_CHANGE_LOCALE_AR = "تم تغير اللغة الي اللغة العربيه ";
	public static final String RESPOSE_MSG_CHANGE_LOCALE_EN = "Language has been changed to English language";
	
	//logger prefixes 
	public static final String LOGGER_SENDER_ID = " SENDER ID IS ";
	public static final String LOGGER_METHOD_NAME = " METHOD NAME IS ";
	public static final String LOGGER_DIAL_IS = " DIAL IS ";
	public static final String LOGGER_EXCEPTION_MESSAGE = " EXCEPTION MESSAGE IS ";
	public static final String LOGGER_CUSTOMER_PROFILE = "Customer Profile ";
	public static final String LOGGER_SUBSCROBER_PROFILE = " Get Subscriber profile Response is ";
	public static final String LOGGER_BUNDLE_SUPSCRIPTION =" Bundle Subscription Details ";
	public static final String LOGGER_ELIGIBLE_PRODUCT = " Eligible Products are ";
	public static final String LOGGER_CACHED_RESPONSE = " Response retrieved from caching";
	public static final String LOGGER_SERVER_RESPONSE = " Response retrieved from server";
	
	//json response keys 
	public static final String IS_ARRAY_KEY_IN_JSON_RESPONSE = "#";	
	public static final String CACHED_MAP_PROFILE_KEY_SUFFIX = "_PROFILE";
	public static final String CACHED_MAP_ELIGIPLE_PRODUCT_KEY_SUFFIX = "_PRODUCT";
	public static final String CACHED_MAP_ELIGIPLE_EXTRA_KEY_SUFFIX = "_EXTRA";
	
	
	public static final String SOCIAL_CONSUMPTION = "socialConsumption";
	 
	 
}
