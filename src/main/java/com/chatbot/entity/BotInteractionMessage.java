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
 * The persistent class for the BOT_INTERACTION_MESSAGES database table.
 */
@Entity
@Table(name = "bot_interaction_messages")
@NamedQuery(name = "BotInteractionMessage.findAll", query = "SELECT b FROM BotInteractionMessage b")
public class BotInteractionMessage implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "MESSAGE_ID")
	private Long messageId;

	@Column(name = "MESSAGE_PRIORITY")
	private Long messagePriority;

	// bi-directional many-to-one association to BotMessageType
	@ManyToOne
	@JoinColumn(name = "MESSAGE_TYPE")
	private BotMessageType botMessageType;

	// bi-directional many-to-one association to BotInteraction
	@ManyToOne
	@JoinColumn(name = "INTERACTION_ID")
	private BotInteraction botInteraction;
	
	
	@Type(type= "org.hibernate.type.NumericBooleanType")
	@NotNull(message="NOT_NULL")
	@Column(name = "IS_STATIC")
	private Boolean isStatic;
	

	public BotInteractionMessage() {
	}

	public Long getMessageId() {
		return this.messageId;
	}

	public void setMessageId(Long messageId) {
		this.messageId = messageId;
	}

	public Long getMessagePriority() {
		return this.messagePriority;
	}

	public void setMessagePriority(Long messagePriority) {
		this.messagePriority = messagePriority;
	}

	public BotMessageType getBotMessageType() {
		return this.botMessageType;
	}

	public void setBotMessageType(BotMessageType botMessageType) {
		this.botMessageType = botMessageType;
	}

	public BotInteraction getBotInteraction() {
		return this.botInteraction;
	}

	public void setBotInteraction(BotInteraction botInteraction) {
		this.botInteraction = botInteraction;
	}

	public Boolean getIsStatic() {
		return isStatic;
	}

	public void setIsStatic(Boolean isStatic) {
		this.isStatic = isStatic;
	}

	
	
}