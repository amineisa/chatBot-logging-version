package com.chatbot.dao;

import org.springframework.data.repository.CrudRepository;

import com.chatbot.entity.PersistenceMenuButton;
import java.util.List;

public interface PersistenceMenuButtonRepo extends CrudRepository<PersistenceMenuButton, Long>{
		
	List<PersistenceMenuButton> findByParentIdNull();
	
	List<PersistenceMenuButton> findByParentId(Long parentId);
	
	List<PersistenceMenuButton> findByParentIdNotNull();
}
