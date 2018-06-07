package com.chatbot.services;

import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;


public class CacheService {

	
	 private static Map<String, String> responses = new HashMap<String, String>();
	    static{
	    	responses.put("ddd","");
	    	responses.put("fff","");
	    }
	     
	     
	    @CachePut(value="customers", key="#id")
	    public String putResponse(String firstName, long id){
	        String  response = responses.get(id);
	        return response;
	    }
	     
	    @Cacheable(value="customers", key="#id")
	    public String get(long id){
	        System.out.println("Service processing...");
	        try{
	            Thread.sleep(3000); 
	        }catch(Exception e){
	        }
	        String cust = responses.get(id);
	        return cust;
	    }
	     
	    @CacheEvict(value = "customers", key = "#id")
	    public void evict(long id){
	    }
}
