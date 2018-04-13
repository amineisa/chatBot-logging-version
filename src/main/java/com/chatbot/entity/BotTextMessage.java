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
 * The persistent class for the BOT_TEXT_MESSAGES database table.
 */
@Entity
@Table(name = "bot_text_messages")
@NamedQuery(name = "BotTextMessage.findAll", query = "SELECT b FROM BotTextMessage b")
public class BotTextMessage implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "TEXT_MSG_ID")
	private Long textMsgId;

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
	
	public BotTextMessage() {
	}

	public Long getTextMsgId() {
		return this.textMsgId;
	}

	public void setTextMsgId(Long textMsgId) {
		this.textMsgId = textMsgId;
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