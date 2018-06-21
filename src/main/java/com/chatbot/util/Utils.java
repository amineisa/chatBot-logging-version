package com.chatbot.util;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.cache.CacheResponseStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import com.chatbot.entity.BotButton;
import com.chatbot.entity.BotInteraction;
import com.chatbot.entity.CustomerProfile;
import com.chatbot.entity.InteractionLogging;
import com.chatbot.services.ChatBotService;
import com.chatbot.services.UtilService;
import com.github.messenger4j.Messenger;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerIOException;
import com.github.messenger4j.send.SenderActionPayload;
import com.github.messenger4j.send.senderaction.SenderAction;

public class Utils {
	
	private static final Logger logger = LoggerFactory.getLogger(UtilService.class); 
	public  enum ButtonTypeEnum {
		START(1L),POSTBACK(2l), URL(3l),NESTED(4L),LOGIN(5L),LOGOUT(6L),CALL(7L);
		private final Long buttonTypeId;

		private ButtonTypeEnum(Long typeId) {
			this.buttonTypeId = typeId;
		}

		public Long getValue() {
			return buttonTypeId;
		}
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
	
	
	// Get Text Value
			public static String getTextValueForButtonLabel(String local , BotButton botButton) {
				String text = "";
				if(local.equalsIgnoreCase("ar")) {
					text = botButton.getBotText().getArabicText();
				}else {
					text =  botButton.getBotText().getEnglishText();
				}
				return text;
			}
			
			
			// Create Url Method 
			public static URL createUrl(String stringUrl){
				URL url = null;
				
				 try {
					url = new URL(stringUrl);
				} catch (MalformedURLException e) {
					logger.error(e.getMessage() , e);
				}
				 
				 return url ;
				
			}
			
			
	// DashBoard Utils
			
			public static String encryptDPIParam(String encryptedString) throws Exception {
		        byte[] decryptionKey = new byte[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
		        Cipher c = Cipher.getInstance("AES");
		        SecretKeySpec k = new SecretKeySpec(decryptionKey, "AES");
		        c.init(Cipher.ENCRYPT_MODE, k);
		        byte[] utf8 = encryptedString.getBytes("UTF8");
		        byte[] enc = c.doFinal(utf8);
		        return DatatypeConverter.printBase64Binary(enc);
		    }
			
			
			
			public static CustomerProfile updateCustomerLastSeen(CustomerProfile customerProfile) {
				Date date = new Date();
				CustomerProfile updatedCustomerProfile = new CustomerProfile();
				updatedCustomerProfile.setFirstInsertion(customerProfile.getFirstInsertion());
				updatedCustomerProfile.setLastGetProfileWSCall(customerProfile.getLastGetProfileWSCall());
				updatedCustomerProfile.setLinkingDate(customerProfile.getLinkingDate());
				updatedCustomerProfile.setLocal(customerProfile.getLocal());
				updatedCustomerProfile.setMsisdn(customerProfile.getMsisdn());
				updatedCustomerProfile.setSenderID(customerProfile.getSenderID());
				Timestamp timeStamp = new Timestamp(date.getTime());
				updatedCustomerProfile.setCustomerLastSeen(timeStamp);
				return updatedCustomerProfile;
			}
			
			
			public static boolean isNotEmpty(String obj) {
				return obj != null && obj.length() != 0;
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
			
			
			/**
			 * @param customerProfile
			 * @param botInteraction
			 */
			public static void interactionLogginghandling(CustomerProfile customerProfile, BotInteraction botInteraction , ChatBotService chatBotService) {
				Date date = new Date();
				Timestamp timeStamp = new Timestamp(date.getTime());
				InteractionLogging interactionLogging = new InteractionLogging();
				interactionLogging.setBotInteraction(botInteraction);
				interactionLogging.setInteractionCallingDate(timeStamp);
				interactionLogging.setCustomerProfile(customerProfile);
				chatBotService.saveInteractionLogging(interactionLogging);
				InteractionLogging interactionlogging = chatBotService.saveInteractionLogging(interactionLogging);
			}
			
			
			public static  void userLogout(final String senderId , ChatBotService chatBotService) {
				CustomerProfile storedCustomerProfile = chatBotService.getCustomerProfileBySenderId(senderId);
				CustomerProfile logoutCustomerProfile = new CustomerProfile();
				logoutCustomerProfile.setCustomerLastSeen(storedCustomerProfile.getCustomerLastSeen());
				logoutCustomerProfile.setFirstInsertion(storedCustomerProfile.getFirstInsertion());
				logoutCustomerProfile.setLastGetProfileWSCall(storedCustomerProfile.getLastGetProfileWSCall());
				logoutCustomerProfile.setLinkingDate(storedCustomerProfile.getLinkingDate());
				logoutCustomerProfile.setLocal(storedCustomerProfile.getLocal());
				logoutCustomerProfile.setMsisdn("");
				logoutCustomerProfile.setSenderID(storedCustomerProfile.getSenderID());
				chatBotService.saveCustomerProfile(logoutCustomerProfile);
			}
			
			
			
		public static void sendRequest(HttpClient cachingClient, HttpContext localContext) {
			        HttpGet httpget = new HttpGet("http://10.195.5.179:7777/dashboard/user/profile?dial=param%3AJZRuLMFnPNW6CDaD71SqZV2dVn7xfe4asOcXTVBHIAzM7Ct%2FwSZZrYvHJB3Pjl%2BAN3Ii4GGpJMWRzZwn6hY%2B1A%3D%3D%2CparamChannel%3A4e47684968446e4e7067726d3968507a4f77585273684d3152647046703752454c6d4a4b59533978484557636750357151644c487154544370445343414d7252");
			        HttpResponse response = null;
			        try {
			            response = cachingClient.execute(httpget, localContext);
			        } catch (ClientProtocolException e) {
			        	logger.error(e.getMessage() , e);
			        } catch (IOException e) {
			        	logger.error(e.getMessage() , e);
			        }
			        Header [] headers = response.getAllHeaders();
			        for(Header headr : headers) {
			        	System.out.println("Header is "+headr.getName() +"   "+headr.getValue());
			        }
			        
			        HttpEntity entity = response.getEntity();
			        try {
			            EntityUtils.consume(entity);
			        } catch (IOException e) {
			        	logger.error(e.getMessage() , e);
			        }

			    }
			 
			 
			  public static void checkResponse(CacheResponseStatus responseStatus) {
			        switch (responseStatus) {
			            case CACHE_HIT:
			                System.out.println("A response was generated from the cache with no requests "
			                        + "sent upstream");
			                break;
			            case CACHE_MODULE_RESPONSE:
			                System.out.println("The response was generated directly by the caching module");
			                break;
			            case CACHE_MISS:
			                System.out.println("The response came from an upstream server");
			                break;
			            case VALIDATED:
			                System.out.println("The response was generated from the cache after validating "
			                        + "the entry with the origin server");
			                break;
			        }
			    }
	
	
	
			  
			  public static void cachingResponse() {
				  CacheConfig cacheConfig = CacheConfig.custom()
					        .setMaxCacheEntries(1000)
					        .setMaxObjectSize(8192)
					        .build();

			        HttpClient cachingClient = new CachingHttpClient(new DefaultHttpClient(), cacheConfig);

			        HttpContext localContext = new BasicHttpContext();

			        sendRequest(cachingClient, localContext);
			        CacheResponseStatus responseStatus = (CacheResponseStatus) localContext.getAttribute(
			                CachingHttpClient.CACHE_RESPONSE_STATUS);
			        checkResponse(responseStatus);


			        sendRequest(cachingClient, localContext);
			        responseStatus = (CacheResponseStatus) localContext.getAttribute(
			                CachingHttpClient.CACHE_RESPONSE_STATUS);
			        checkResponse(responseStatus);
			    }
			  
			  
			  
			  public static void markAsSeen(Messenger messenger , String userId) {
				  final String recipientId = userId;
				  final SenderAction senderAction = SenderAction.MARK_SEEN;

				  final SenderActionPayload payload = SenderActionPayload.create(recipientId, senderAction);

				  try {
					messenger.send(payload);
				} catch (MessengerApiException | MessengerIOException e) {
					logger.error(e.getMessage() , e);
				}
			  }
			  
			  
			  public static void markAsTypingOn(Messenger messenger , String userId) {
				  final String recipientId = userId;
				  final SenderAction senderAction = SenderAction.TYPING_ON;

				  final SenderActionPayload payload = SenderActionPayload.create(recipientId, senderAction);

				  try {
					messenger.send(payload);
				} catch (MessengerApiException | MessengerIOException e) {
					logger.error(e.getMessage() , e);
				}
			  }
			  
			  
			  public static void markAsTypingOff(Messenger messenger , String userId) {
				  final String recipientId = userId;
				  final SenderAction senderAction = SenderAction.TYPING_OFF;

				  final SenderActionPayload payload = SenderActionPayload.create(recipientId, senderAction);

				  try {
					messenger.send(payload);
				} catch (MessengerApiException | MessengerIOException e) {
					logger.error(e.getMessage() , e);
				}
			  }
			  
			  
			  
			  

				    public  List<String> getParameterNames(Method method) {
				        Parameter[] parameters = method.getParameters();
				        List<String> parameterNames = new ArrayList<>();

				        for (Parameter parameter : parameters) {
				            if(!parameter.isNamePresent()) {
				                throw new IllegalArgumentException("Parameter names are not present!");
				            }
				            
				            String parameterName = parameter.getName();
				            parameterNames.add(parameterName);
				        }

				        return parameterNames;
				    }

				   			
			  
			  
}
