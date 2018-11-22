package com.chatbot.dao;

import org.springframework.data.repository.CrudRepository;

import com.chatbot.entity.UserSelection;


public interface UserSelectionDao extends CrudRepository<UserSelection, String>{
		
		UserSelection  findBySenderId(String senderId);	
		
}
