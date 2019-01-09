package com.chatbot.util;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
 
public class HazelCastClient {
 
    @SuppressWarnings({ "deprecation" })
	
        
    void callPrintMap(){
    	String hostAndPort = "10.222.193.26:1526";
    	ClientConfig clientConfig = new ClientConfig();
        clientConfig.getNetworkConfig().addAddress(hostAndPort);
       // clientConfig.addAddress("localhost");
        //clientConfig.addAddress("10.222.193.26");
        clientConfig.getAddresses().add("10.222.193.19");
        clientConfig.getAddresses().add("10.222.193.26");
        HazelcastInstance client = HazelcastClient.newHazelcastClient(clientConfig);
        IMap<Object, Object> map = client.getMap("customers");
        printMap(map);
    	}
    
  
    @SuppressWarnings("unchecked")
    private static void printMap(@SuppressWarnings("rawtypes") Map map){
    	System.out.println("Map Size:" + map.size());
		Set<Entry<Integer,String>> customers = map.entrySet();
       for(Iterator<Entry<Integer, String>> iterator = customers.iterator(); iterator.hasNext();) {
        	Entry<Integer, String> entry = (Entry<Integer, String>) iterator.next();
        	System.out.println("Customer Id : "+ entry.getKey()+" Customer Name : "+entry.getValue());
      }	
    }
}
