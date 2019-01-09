package com.chatbot.util;

public class Constants {

	public static final String CHANEL_PARAM = "CHATBOT";
	public static final String PREFIX_ADDONSUBSCRIPE = "subaddon_";
	public static final String PREFIX_RATEPLAN_SUBSCRIPTION = "sub_";
	public static final String PREFIX_MOBILEINTERNET_ADDON = "MIAddon";
	public static final String PREFIX_RELATED_PRODUCTS = "related";
	public static final String PREFIX_CONFIRM_MOBILEINTERNET_SUBSCRIPTION = "mi_yes_subscripe";
	public static final String PREFIX_RELATED_PRODUCTS_SUBSCRIPTION = "relatedproductsubscription";
	public static final String PREFIX_BUNDLE_UNSUBSCRIPTION ="mi_yes_unsubscribe";
	

	public static final String PAYLOAD_MOBILE_INTERNET_SUBSCRIPTION_CANCEL = "mi_no_subscripe";
	public static final String PAYLOAD_CHANGE_BUNDLE = "change bundle";
	public static final String PAYLOAD_ADDON_SUBSCRIPTION = "subscribe addon";
	public static final String PAYLOAD_BUY_ADDONS = "buy addons";
	public static final String PAYLOAD_MI_CONSUMTION_PARENT = "MI consumption parent";
	public static final String PAYLOAD_RELATED_PRODUCTS = "related product";
	public static final String PAYLOAD_MOBILE_INTERNET_CONFIRMATION_MSG = "MI Bundle subscription confirmation msg";
	public static final String PAYLOAD_CANCEL_PAY_OR_RECHARGE = "cancel pay or recharge";
	public static final String PAYLOAD_CANCEL_RECHARGING = "cancel recharging";
	public static final String PAYLOAD_CANCEL_BILL_PAYMENT = "cancel paying bill";
	public static final String PAYLOAD_FAULT_MSG = "fault MSG";
	public static final String PAYLOAD_ACCOUNT_DETAILS = "account details";
	public static final String PAYLOAD_PREPAID = "prepaid";
	public static final String PAYLOAD_VIEW_CONNECT_DETAILS = "view connect details";
	public static final String PAYLOAD_VIEW_RATEPLAN_DETAILS = "view rateplan details";
	public static final String PAYLOAD_RATEPLAN_AND_CONNECT = "view rateplan and connect details";
	public static final String PAYLOAD_VIEW_ROOT_CONNECT_DETAILS = "view root connect details";
	public static final String PAYLOAD_RATEPLAN_DETAILS = "rateplan details";
	public static final String PAYLOAD_CONSUMPTION = "consumption";
	public static final String PAYLOAD_RELATED_PRODUCT_SUBSCRIPTION_CONFIRM = "mi_yes_subscripe_related_product";
	public static final String PAYLOAD_VIEW_MOBILEINTERNET_SUBSCRIPTION = "view mobile internet subscribe";
	public static final String PAYLOAD_RATEPLAN_ADDONS_CONSUMPTION = "rateplan addons consumption";
	public static final String PAYLOAD_MOBILEINTERNET_ADDONS_CONSUMPTION = "mobile internet addon consumption";
	public static final String PAYLOAD_POSTPAID_DIAL = "postpaid";
	public static final String PAYLOAD_BUY_ADDONS_ROOT = "buy addons root";
	public static final String PAYLOAD_UNEXPECTED_PAYLOAD = "unexpected";
	public static final String PAYLOAD_TALK_TO_AGENT = "Talk to Agent";
	public static final String PAYLOAD_WELCOME_AGAIN = "welcome again";
	public static final String PAYLOAD_FREE_TEXT = "freetext";
	public static final String PAYLOAD_MIGRATE_MORE = "Migrate More";
	public static final String NOTELIGIBLE_ELEMENT_TITLE_EN = "Sorry !";
	public static final String NOTELIGIBLE_ELEMENT_TITLE_AR = "! عفوا";
	public static final String PAYLOAD_MIGRATE = "Migrate";
	public static final String NOCONSUMPTION_ELEMENT_SUBTITLE_EN = " Your current rateplan is not have consumption  ";
	public static final String NOCONSUMPTION_ELEMENT_SUBTITLE_AR = "باقتك ليس عليها اي استهلاك";
	
	
	
	public static final String NOTELIGIBLE_ELEMENT_SUBTITLE_EN = " Your Dial Not Eligible to any addons ";
	public static final String NOTELIGIBLE_ELEMENT_SUBTITLE_AR = " عفوا رقمك ليس له اي اضافات";
	
	public static String profilePayloads = PAYLOAD_VIEW_CONNECT_DETAILS + Constants.COMMA_CHAR + PAYLOAD_RATEPLAN_AND_CONNECT + "," + PAYLOAD_VIEW_RATEPLAN_DETAILS + "," + PAYLOAD_RATEPLAN_DETAILS
			+ "," + PAYLOAD_CONSUMPTION;
	public static final String RESPOSE_MSG_CHANGE_LOCALE_AR = "تم تغير اللغة الي اللغة العربيه ";
	public static final String RESPOSE_MSG_CHANGE_LOCALE_EN = "Language has been changed to English language";

	// logger prefixes
	public static final String LOGGER_SENDER_ID = " SENDER ID IS ";
	public static final String LOGGER_METHOD_NAME = " METHOD NAME IS ";
	public static final String LOGGER_DIAL_IS = " DIAL IS ";
	public static final String LOGGER_EXCEPTION_MESSAGE = " EXCEPTION MESSAGE IS ";
	public static final String LOGGER_CUSTOMER_PROFILE = "Customer Profile ";
	public static final String LOGGER_SUBSCROBER_PROFILE = " Get Subscriber profile Response is ";
	public static final String LOGGER_BUNDLE_SUPSCRIPTION = " Bundle Subscription Details ";
	public static final String LOGGER_ELIGIBLE_PRODUCT = " Eligible Products are ";
	public static final String LOGGER_CACHED_RESPONSE = " Response retrieved from caching";
	public static final String LOGGER_SERVER_RESPONSE = " Response retrieved from server";
	public static final String LOGGER_MOBILE_INTERENET_CONSUMPTION = "Mobile Internet Consumption ";
	public static final String LOGGER_RATEPLAN_CONSUMPTION = "RatePlan Consumption ";
	public static final String LOGGER_SERVICE_URL = "Service URL ";

	// json response keys
	public static final String IS_ARRAY_KEY_IN_JSON_RESPONSE = "#";
	public static final String CACHED_MAP_PROFILE_KEY_SUFFIX = "_PROFILE";
	public static final String CACHED_MAP_ELIGIPLE_PRODUCT_KEY_SUFFIX = "_PRODUCT";
	public static final String CACHED_MAP_ELIGIPLE_EXTRA_KEY_SUFFIX = "_EXTRA";

	public static final String SOCIAL_CONSUMPTION = "socialConsumption";

	public static final String JSON_KEY_NAME_EN = "englishName";
	public static final String JSON_KEY_NAME_AR = "arabicName";
	public static final String JSON_KEY_FOR_PRODUCT = "products";
	public static final String JSON_KEY_FOR_RELATED_PRODUCT = "relatedProduct";
	public static final String JSON_KEY_FOR_ENGLISH_DESCRIPTION = "englishDescription";
	public static final String JSON_KEY_FOR_NAME = "name";
	public static final String JSON_KEY_FOR_PRODUCT_CATEGORY = "productCategories";
	public static final String SUBTITLE_VALUE = "_ _ _";
	public static final String JSON_KEY_CATEGORY_NAME_AR = "categoryNameAr";
	public static final String JSON_KEY_CATEGORY_NAME_EN = "categoryNameEn";
	public static final String JSON_KEY_CATEGORY_KEY = "category";
	public static final String JSON_KEY_CATEGORY_ID = "categoryId";
	public static final String JSON_KEY_LEGO = "LEGO";
	public static final String JSON_KEY_LEGO_HIGH = "LEGO_HIGH";
	public static final String JSON_KEY_LEGO_LOW = "LEGO_LOW";
	public static final String JSON_KEY_ARABIC_DESCRIPTION_KEY = "arabicDescription";
	public static final String JSON_KEY_OPERATION_RESPONSE = "operationResponses";
	public static final String JSON_KEY_POSTPAID = "postPaid";
	public static final String JSON_KEY_BALANCE = "balance";
	public static final String JSON_KEY_BILLING_PROFILE = "billingProfileModel";
	public static final String JSON_KEY_RATEPLAN = "rateplan";
	public static final String JSON_KEY_PRODUCT_ID = "uniqueProductName";
	public static final String JSON_KEY_MOBILE_INTERNET = "connect";
	public static final String JSON_KEY_CONSUMPTION_NAME = "consumptionName";
	public static final String JSON_KEY_LABEL_EN = "englishLabel";
	public static final String JSON_KEY_LABEL_AR = "arabicLabel";
	public static final String JSON_KEY_COMMERCIAL_NAME = "commercialName";
	public static final String JSON_KEY_RENEWAL_DATE = "renewalDate";
	public static final String JSON_KEY_CONSUMPTION_DETAILS_LIST = "consumptionDetailsList";
	public static final String JSON_KEY_CHILED_ELIGIBLE_PRODUCTS = "childEligibleProductModels";
	public static final String JSON_KEY_ACTION_BUTTONS="actionButtons";
	public static final String JSON_KEY_PARAM = "param";
	public static final String JSON_KEY_IFRAME_BILL_PAYMENT_URL = "iframeUrl";
	public static final String JSON_KEY_MIGRATION_ROUTING ="ratePlanRelease2";
	public static final String FB_JSON_KEY_STANDBY ="standby";
	public static final String FB_JSON_KEY_MESSAGE = "message";
	public static final String FB_JSON_KEY_ENTRY = "entry";
	public static final String FB_JSON_KEY_SENDER = "sender";
	public static final String FB_JSON_KEY_ID = "id";
	public static final String FB_JSON_KEY_FREE_TEXT = "text";
	
	

	
	public static final String RESPONSE_STATUS_KEY = "status";
	public static final String RESPONSE_KEY = "response" ;
	public static final String RESPONSE_PERCENTAGE_KEY ="percentage";
	
	public static final String JSON_KEY_VALUE_AR = "arabicValue";
	public static final String JSON_KEY_VALUE_EN = "englishValue";
	
	
	public static final String URL_KEYWORD_PROFILE = "profile";
	public static final String URL_KEYWORD_BUNDLE = "bundle";
	public static final String URL_KEYWORD_EXTRA = "extra";
	public static final String URL_PARAM_CHANNEL_KEY ="paramChannel:";
	public static final String URL_PARAM_MSISDN_KEY = "msisdn=";
	public static final String URL_TIME_CHANNEL_KEY = "&time=1525328875649&channel=";
	public static final String URL_PAY_BILL_AND_RECHARGE_CHANEL = "&URL=facebook";
	public static final String URL_USER_AND_TIME_KEY = "time=1498033943505&user=";
	public static final String URL_WARNING_IMAGE = "warning.png?version=1";
	public static final String URL_LOCALE_AR = "ar&param=";
	public static final String URL_LOCALE_EN = "en&param=";
	public static final String MOBILE_INTERNET_CONSUMPTION_NAME_EN = "Mobile Internet";
	public static final String MOBILE_INTERNET_CONSUMPTION_NAME_AR = "موبايل انترنت";
	public static final String OPERATION_NAME_RENEW = ",RENEW";

	public static final String RESPONSE_MAP_MESSAGE_KEY = "msg";
	public static final String RESPONSE_MAP_TITLE_KEY = "title";
	public static final String INTERNET_BUNDLE_AR = "باقات الانترنت";

	public static final String CONFIGURATION_TABLE_MESSENGER_OBJECT = "MESSENGER_OBJECT";
	public static final String CONFIGURATION_TABLE_APP_SECRET = "APP_SECRET";
	public static final String CONFIGURATION_TABLE_PAGE_ACCESS_TOKEN = "PAGE_ACCESS_TOKEN";
	public static final String CONFIGURATION_TABLE_VERIFY_TOKEN = "VERIFY_TOKEN";
	public static final String CONFIGURATION_TABLE_WARNING_I0MAGE_URL = "WARNING_IMAGE";
	public static final String SECONDRY_APP_ID = "SECONDRY_APP_ID";

	public static final String CONFIGURATION_CACHE_KEY = "configurationCache";
	
	public static final String LOCALE_EN = "en_us";
	public static final String LOCALE_AR = "ar";
	public static final String IS_KEY_HAS_DOT = ".";
	public static final String COMMA_CHAR = ",";
	public static final String EMPTY_STRING = "";
	public static final String UNDERSCORE = "_";
	public static final String ADDON_SUBSCRIPTION_ACTION = "Action is Add-on Subscription";
	public static final String RENEW_BUNDLE_ACTION = "Action is Renew Subscribed Bundle ";
	public static final String MI_BUNDLE_SUBSCRIPTION = "Action is Mobile Internet Bundle Subscription";
	
	public static final String BUTTON_LABEL_CHARGE_AR = "اشحن الان";
	public static final String BUTTON_LABEL_CHARGE_EN = "Recharge Now";
	public static final String JSON_KEY_RATEPLAN_ADON = "ratePlanAddons";
	public static final String JSON_KEY_MOBILE_INTERNET_ADON = "addons";
	public static final String TELL_CLIENT_WAIT_FOR_AGENT_RESPONSE = "WAIT_FOR_AGENT_RESPONSE";
	public static final String REQUEST_TIME_OUT_VALUE = "REQUEST_TIME_OUT_VALUE";
	public static final String JSON_KEY_BIILAMOUNT = "billAmount";
	public static final String PAY_BILL_BASE_URL = "PAY_BILL_BASE_URL";
	
	
	public static final String HAZEL_OBJECT_NAME = "CASH_OBJECT_NAME";
	
	public static final String HAZEL_CACHE_MAP_CONTAINER_NAME = "CACHES";
	public static final String USER_SELECTION_MAP_KEY = "USERS_SELCTIONS";
	
	
	public static final String FIRST_CACHING_SERVER_IP = "FIRST_SEVER_IP";
	public static final String SECOND_CACHING_SERVER_IP = "SECOND_SERVER_IP";
	public static final String COMMUNICATE_PORT_BETWEEN_CACHING_SERVERES = "COMMUNICATE_PORT";
	
	public static final String MIGRATION_RATEPLANS_KEY = "rateplans";
	public static final String MIGRATION_DISPLAY_MOREBUTTON = "displayMoreBtn";
	public static final String MORE_BUTTON_LABEL_AR = "أكثر";
	public static final String MORE_BUTTON_LABEL_EN = "More";
	public static final String MIGRATE_BUTTON_LABEL= "Migrate";
	public static final String PREFIX_MIGRATE_ID = "MigrateID,";
	public static final String PREFIX_MIGRATE_NAME = "MigrateName,";
	public static final String PAYLOAD_MIGRATE_BY_ID = "Migratebyid";
	public static final String PAYLOAD_MIGRATE_BY_NAME = "Migratebyname";
	public static final String PAYLOAD_CONFIRMATION_MIGRATE = "Migrate_Confirmation";
	public static final String PAYLOAD_MIGRATE_CONFIRM = "rateplan_migrate_yes";
	public static final String CONFIGURATION_TABLE_WARNING_IMAGE_URL = "WARNING_IMAGE";
	public static final String MIGRATATION_OPERATION_VALUE = "MIGRATE";
	public static final String MIGRATE_BY_PREFIX = "Migrateby";
	public static final String UNSUBSCRIBE_OPERATION_VALUE = "DEACTIVATE";
	public static final String PAYLOAD_RATEPLAN_ACTIONS = "rateplan actions";
	public static final String PAYLOAD_MOBILE_INTERNET_CONTROLLER = "Mobile Internet controller";
	public static final String PAYLOAD_NO_MI_BUNDLE_FOUND = "No MI Bundle Found";
	
	

	

	

}
