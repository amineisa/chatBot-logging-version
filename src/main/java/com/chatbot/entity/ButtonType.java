package com.chatbot.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name ="Bot_BUTTON_TYPES")
public class ButtonType implements Serializable{
	@Id
	@Column (name="ID")
	private Long id;
	
	@Column (name="BUTTON_NAME")
	private String buttonName;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getButtonName() {
		return buttonName;
	}

	public void setButtonName(String buttonName) {
		this.buttonName = buttonName;
	}

	
	
}
