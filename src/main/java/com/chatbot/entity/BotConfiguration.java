package com.chatbot.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="BOT_CONFIGRATION")
public class BotConfiguration implements Serializable{

@Id
@Column(name="ID")
private Long id;


@Column(name="LOCAL")
private String local;

@OneToOne()
@JoinColumn(name="TEXT_ID" , referencedColumnName="TEXT_ID")
private BotText textId;

@Column(name="type")
//@NotNull(message="NOT NULL")
private String type;

public Long getId() {
	return id;
}

public void setId(Long id) {
	this.id = id;
}

public String getLocal() {
	return local;
}

public void setLocal(String local) {
	this.local = local;
}

public BotText getTextId() {
	return textId;
}

public void setTextId(BotText textId) {
	this.textId = textId;
}

public String getType() {
	return type;
}

public void setType(String type) {
	this.type = type;
}


	
}
