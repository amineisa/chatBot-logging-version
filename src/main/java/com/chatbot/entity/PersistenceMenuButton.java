package com.chatbot.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;

@Entity
@Table(name="Bot_Persistence_menu_Buttons")
public class PersistenceMenuButton implements Serializable{
	@Id
	@Column(name="PersistenceMenuButton_id")
	private Long ID;
	
	@JoinColumn(name="Button_id" , referencedColumnName="BUTTON_ID")
	@OneToOne
	private BotButton button;
	
	@Column(name="parent_Id")
	private Long parentId;
	
	@Column(name="priority")
	private Long priority;
	
	@Column(name="IS_NESTED")
	@Type(type= "org.hibernate.type.NumericBooleanType")
	@NotNull(message="NOT_NULL")
	private Boolean isNested;

	public Long getID() {
		return ID;
	}

	public void setID(Long iD) {
		ID = iD;
	}

	public BotButton getButton() {
		return button;
	}

	public void setButton(BotButton button) {
		this.button = button;
	}

	public Long getParentId() {
		return parentId;
	}

	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}

	public Long getPriority() {
		return priority;
	}

	public void setPriority(Long priority) {
		this.priority = priority;
	}

	public Boolean getIsNested() {
		return isNested;
	}

	public void setIsNested(Boolean isNested) {
		this.isNested = isNested;
	}

	
	
	
}
