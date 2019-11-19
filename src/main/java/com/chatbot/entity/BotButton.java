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

/**
 * The persistent class for the BOT_BUTTONS database table.
 */
@Entity
@Table(name = "bot_buttons")
@NamedQuery(name = "BotButton.findAll", query = "SELECT b FROM BotButton b")
public class BotButton implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "BUTTON_ID")
	private Long buttonId;

	@Column(name = "BUTTON_PAYLOAD")
	private String buttonPayload;

	@ManyToOne
	@JoinColumn(name = "BUTTON_TYPE" , referencedColumnName="ID")
	private ButtonType buttonType;

	@Column(name = "BUTTON_URL")
	private String buttonUrl;

	@Type(type= "org.hibernate.type.NumericBooleanType")
	@NotNull(message="NOT_NULL")
	@Column(name = "IS_STATIC")
	private Boolean isStatic;

	// bi-directional many-to-one association to BotText
	@ManyToOne()
	@JoinColumn(name = "TEXT_ID")
	private BotText botText;

	// bi-directional many-to-one association to BotQuickReplyMessage
	@ManyToOne()
	@JoinColumn(name = "QUICK_MSG_ID")
	private BotQuickReplyMessage botQuickReplyMessage;

	@ManyToOne
	@JoinColumn(name = "ELEMENT_ID")
	private BotTemplateElement botTemplateElement;
	
	
	@ManyToOne
	@JoinColumn(name = "BUTTON_TEMP_ID")
	private BotButtonTemplateMSG botButtonTemplateMSG;

	@ManyToOne
	@JoinColumn(name = "BUTTON_WS_MAP_ID")
	private BotWebserviceMessage botWebserviceMessage;
	
	
	
	@Column(name = "BUTTON_IMAGE_URL")
	private String buttonImageUrl;


	public Long getButtonId() {
		return this.buttonId;
	}

	public void setButtonId(Long buttonId) {
		this.buttonId = buttonId;
	}

	public String getButtonPayload() {
		return this.buttonPayload;
	}

	public void setButtonPayload(String buttonPayload) {
		this.buttonPayload = buttonPayload;
	}

	public ButtonType getButtonType() {
		return this.buttonType;
	}

	public void setButtonType(ButtonType buttonType) {
		this.buttonType = buttonType;
	}

	public String getButtonUrl() {
		return this.buttonUrl;
	}

	public void setButtonUrl(String buttonUrl) {
		this.buttonUrl = buttonUrl;
	}

	

	public BotText getBotText() {
		return this.botText;
	}

	public void setBotText(BotText botText) {
		this.botText = botText;
	}

	public BotQuickReplyMessage getBotQuickReplyMessage() {
		return this.botQuickReplyMessage;
	}

	public void setBotQuickReplyMessage(BotQuickReplyMessage botQuickReplyMessage) {
		this.botQuickReplyMessage = botQuickReplyMessage;
	}

	public BotTemplateElement getBotTemplateElement() {
		return botTemplateElement;
	}

	public void setBotTemplateElement(BotTemplateElement botTemplateElement) {
		this.botTemplateElement = botTemplateElement;
	}

	public String getButtonImageUrl() {
		return buttonImageUrl;
	}

	public void setButtonImageUrl(String buttonImageUrl) {
		this.buttonImageUrl = buttonImageUrl;
	}

	public Boolean getIsStatic() {
		return isStatic;
	}

	public void setIsStatic(Boolean isStatic) {
		this.isStatic = isStatic;
	}

	public BotButtonTemplateMSG getBotButtonTemplateMSG() {
		return botButtonTemplateMSG;
	}

	public void setBotButtonTemplateMSG(BotButtonTemplateMSG botButtonTemplateMSG) {
		this.botButtonTemplateMSG = botButtonTemplateMSG;
	}

	public BotWebserviceMessage getBotWebserviceMessage() {
		return botWebserviceMessage;
	}

	public void setBotWebserviceMessage(BotWebserviceMessage botWebserviceMessage) {
		this.botWebserviceMessage = botWebserviceMessage;
	}

	@Override
	public String toString() {
		return "BotButton [buttonId=" + buttonId + ", buttonPayload=" + buttonPayload + ", buttonUrl=" + buttonUrl + ", isStatic=" + isStatic + ", botText=" + botText.getEnglishText() 
				+ ", buttonImageUrl=" + buttonImageUrl + "]";
	}

	
	
	
}