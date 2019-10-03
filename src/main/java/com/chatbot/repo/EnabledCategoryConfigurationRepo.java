package com.chatbot.repo;

import org.springframework.data.repository.CrudRepository;
//import org.springframework.stereotype.Repository;

import com.chatbot.entity.EnabledCategoryConfiguration;


public interface EnabledCategoryConfigurationRepo extends CrudRepository<EnabledCategoryConfiguration, Long>{

	public EnabledCategoryConfiguration findEnabledCategoryConfigurationById(Long id);
}
