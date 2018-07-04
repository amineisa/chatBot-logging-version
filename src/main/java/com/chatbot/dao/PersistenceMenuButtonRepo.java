package com.chatbot.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.chatbot.entity.PersistenceMenuButton;

public interface PersistenceMenuButtonRepo extends CrudRepository<PersistenceMenuButton, Long>{
		
	List<PersistenceMenuButton> findByParentIdNull();
	
	List<PersistenceMenuButton> findByParentId(Long parentId);
	
	List<PersistenceMenuButton> findByParentIdNotNull();
}
