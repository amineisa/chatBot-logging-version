package com.chatbot.dao;


import org.springframework.data.repository.CrudRepository;

import com.chatbot.entity.CustomerProfile;

public interface CustomerProfileRepo extends CrudRepository<CustomerProfile, String>{
	
	public CustomerProfile findCustomerProfileBySenderID(String senderId);

}
