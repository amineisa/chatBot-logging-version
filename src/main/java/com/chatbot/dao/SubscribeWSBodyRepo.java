package com.chatbot.dao;

import org.springframework.data.repository.CrudRepository;

import com.chatbot.entity.SubscribeWSBody;

public interface SubscribeWSBodyRepo extends CrudRepository<SubscribeWSBody, String>{

	public SubscribeWSBody findSubscribeWSBodyByBundleId(String id);
	 
}
