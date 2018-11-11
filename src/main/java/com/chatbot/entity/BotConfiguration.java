package com.chatbot.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="BOT_CONFIGURATION")
public class BotConfiguration implements Serializable{

	private static final long serialVersionUID = 1L;


@Id
@Column(name="ID")
private Long id;


@Column(name="KEY")
private String key;

@Column(name="VALUE")
private String value;

public Long getId() {
	return id;
}

public void setId(Long id) {
	this.id = id;
}

public String getKey() {
	return key;
}

public void setKey(String key) {
	this.key = key;
}

public String getValue() {
	return value;
}

public void setValue(String value) {
	this.value = value;
}



	
}
