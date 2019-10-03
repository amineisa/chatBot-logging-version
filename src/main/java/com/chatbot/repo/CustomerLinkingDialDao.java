package com.chatbot.repo;

import org.springframework.data.repository.CrudRepository;

import com.chatbot.entity.CustomerLinkingDial;

public interface CustomerLinkingDialDao extends CrudRepository<CustomerLinkingDial, Long>{

	public CustomerLinkingDial getCustomerLinkingDialByDial(String dial);
}
