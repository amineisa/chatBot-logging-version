package com.chatbot.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;

@Entity
@Table(name = "Button_Temp_MSG")
@NamedQuery(name = "BotButtonTemplateMSG.findAll", query = "SELECT b FROM BotButtonTemplateMSG b")
public class BotButtonTemplateMSG implements Serializable{
	

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "Button_Template_Id")
	private Long buttonTempMsgId;

	@Column(name = "IS_STATIC")
	@Type(type= "org.hibernate.type.NumericBooleanType")
	@NotNull(message="NOT_NULL")
	private Boolean isStatic;

	// bi-directional many-to-one association to BotText
	@ManyToOne
	@JoinColumn(name = "TEXT_ID")
	private BotText botText;

	@ManyToOne
	@JoinColumn(name = "MESSAGE_ID")
	private BotInteractionMessage botInteractionMessage;

	
	public Long getButtonTempMsgId() {
		return buttonTempMsgId;
	}

	public void setButtonTempMsgId(Long buttonTempMsgId) {
		this.buttonTempMsgId = buttonTempMsgId;
	}



	public Boolean getIsStatic() {
		return this.isStatic;
	}

	public void setIsStatic(Boolean isStatic) {
		this.isStatic = isStatic;
	}



	public BotText getBotText() {
		return this.botText;
	}

	public void setBotText(BotText botText) {
		this.botText = botText;
	}

	public BotInteractionMessage getBotInteractionMessage() {
		return botInteractionMessage;
	}

	public void setBotInteractionMessage(BotInteractionMessage botInteractionMessage) {
		this.botInteractionMessage = botInteractionMessage;
	}



}
