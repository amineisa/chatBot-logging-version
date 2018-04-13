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
 * The persistent class for the BOT_MESSAGE_TYPES database table.
 */
@Entity
@Table(name = "bot_message_types")
@NamedQuery(name = "BotMessageType.findAll", query = "SELECT b FROM BotMessageType b")
public class BotMessageType implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "MESSAGE_TYPE_ID")
	private Long messageTypeId;

	@Column(name = "MESSAGE_TYPE_NAME")
	private String messageTypeName;

	// bi-directional many-to-one association to BotInteractionMessage
	@OneToMany(mappedBy = "botMessageType")
	private List<BotInteractionMessage> botInteractionMessages;

	public BotMessageType() {
	}

	public Long getMessageTypeId() {
		return this.messageTypeId;
	}

	public void setMessageTypeId(Long messageTypeId) {
		this.messageTypeId = messageTypeId;
	}

	public String getMessageTypeName() {
		return this.messageTypeName;
	}

	public void setMessageTypeName(String messageTypeName) {
		this.messageTypeName = messageTypeName;
	}

	public List<BotInteractionMessage> getBotInteractionMessages() {
		return this.botInteractionMessages;
	}

	public void setBotInteractionMessages(List<BotInteractionMessage> botInteractionMessages) {
		this.botInteractionMessages = botInteractionMessages;
	}

	public BotInteractionMessage addBotInteractionMessage(BotInteractionMessage botInteractionMessage) {
		getBotInteractionMessages().add(botInteractionMessage);
		botInteractionMessage.setBotMessageType(this);

		return botInteractionMessage;
	}

	public BotInteractionMessage removeBotInteractionMessage(BotInteractionMessage botInteractionMessage) {
		getBotInteractionMessages().remove(botInteractionMessage);
		botInteractionMessage.setBotMessageType(null);

		return botInteractionMessage;
	}

}