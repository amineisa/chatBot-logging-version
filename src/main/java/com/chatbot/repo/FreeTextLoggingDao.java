package com.chatbot.repo;

import org.springframework.data.repository.CrudRepository;

import com.chatbot.entity.FreeTextLogging;

public interface FreeTextLoggingDao extends CrudRepository<FreeTextLogging, Long>{

}