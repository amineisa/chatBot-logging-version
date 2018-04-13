package com.chatbot.entity;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * The persistent class for the BOT_TEXTS database table.
 */
@Entity
@Table(name = "bot_texts")
@NamedQuery(name = "BotText.findAll", query = "SELECT b FROM BotText b")
public class BotText implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "TEXT_ID")
	private Long textId;

	@Column(name = "ARABIC_TEXT")
	private String arabicText;

	@Column(name = "ENGLISH_TEXT")
	private String englishText;

	// bi-directional many-to-one association to BotButton
	@OneToMany(mappedBy = "botText")
	private List<BotButton> botButtons;

	// bi-directional many-to-one association to BotQuickReplyMessage
	@OneToMany(mappedBy = "botText")
	private List<BotQuickReplyMessage> botQuickReplyMessages;

	// bi-directional many-to-one association to BotTextMessage
	@OneToMany(mappedBy = "botText")
	private List<BotTextMessage> botTextMessages;
	
	// bi-directional many-to-one association to BotTextMessage
		@OneToMany(mappedBy = "botText")
		private List<BotButtonTemplateMSG> botButtonTemplate;

	public BotText() {
	}

	public Long getTextId() {
		return this.textId;
	}

	public void setTextId(Long textId) {
		this.textId = textId;
	}

	public String getArabicText() {
		return this.arabicText;
	}

	public void setArabicText(String arabicText) {
		this.arabicText = arabicText;
	}

	public String getEnglishText() {
		return this.englishText;
	}

	public void setEnglishText(String englishText) {
		this.englishText = englishText;
	}

	public List<BotButton> getBotButtons() {
		return this.botButtons;
	}

	public void setBotButtons(List<BotButton> botButtons) {
		this.botButtons = botButtons;
	}

	public BotButton addBotButton(BotButton botButton) {
		getBotButtons().add(botButton);
		botButton.setBotText(this);

		return botButton;
	}

	public BotButton removeBotButton(BotButton botButton) {
		getBotButtons().remove(botButton);
		botButton.setBotText(null);

		return botButton;
	}

	public List<BotQuickReplyMessage> getBotQuickReplyMessages() {
		return this.botQuickReplyMessages;
	}

	public void setBotQuickReplyMessages(List<BotQuickReplyMessage> botQuickReplyMessages) {
		this.botQuickReplyMessages = botQuickReplyMessages;
	}

	public BotQuickReplyMessage addBotQuickReplyMessage(BotQuickReplyMessage botQuickReplyMessage) {
		getBotQuickReplyMessages().add(botQuickReplyMessage);
		botQuickReplyMessage.setBotText(this);

		return botQuickReplyMessage;
	}

	public BotQuickReplyMessage removeBotQuickReplyMessage(BotQuickReplyMessage botQuickReplyMessage) {
		getBotQuickReplyMessages().remove(botQuickReplyMessage);
		botQuickReplyMessage.setBotText(null);

		return botQuickReplyMessage;
	}

	public List<BotTextMessage> getBotTextMessages() {
		return this.botTextMessages;
	}

	public void setBotTextMessages(List<BotTextMessage> botTextMessages) {
		this.botTextMessages = botTextMessages;
	}

	public BotTextMessage addBotTextMessage(BotTextMessage botTextMessage) {
		getBotTextMessages().add(botTextMessage);
		botTextMessage.setBotText(this);

		return botTextMessage;
	}

	public BotTextMessage removeBotTextMessage(BotTextMessage botTextMessage) {
		getBotTextMessages().remove(botTextMessage);
		botTextMessage.setBotText(null);

		return botTextMessage;
	}

	public List<BotButtonTemplateMSG> getBotButtonTemplate() {
		return botButtonTemplate;
	}

	public void setBotButtonTemplate(List<BotButtonTemplateMSG> botButtonTemplate) {
		this.botButtonTemplate = botButtonTemplate;
	}
	

}