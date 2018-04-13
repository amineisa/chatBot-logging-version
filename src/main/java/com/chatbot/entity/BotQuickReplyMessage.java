package com.chatbot.entity;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;

/**
 * The persistent class for the BOT_QUICK_REPLY_MESSAGE database table.
 */
@Entity
@Table(name = "bot_quick_reply_message")
@NamedQuery(name = "BotQuickReplyMessage.findAll", query = "SELECT b FROM BotQuickReplyMessage b")
public class BotQuickReplyMessage implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "QUICK_MSG_ID")
	private Long quickMsgId;

	@Column(name = "IS_STATIC")
	@Type(type= "org.hibernate.type.NumericBooleanType")
	@NotNull(message="NOT_NULL")
	private Boolean isStatic;

	// bi-directional many-to-one association to BotButton
	@OneToMany(mappedBy = "botQuickReplyMessage")
	private List<BotButton> botButtons;

	// bi-directional many-to-one association to BotText
	@ManyToOne
	@JoinColumn(name = "TEXT_ID")
	private BotText botText;

	@ManyToOne
	@JoinColumn(name = "MESSAGE_ID")
	private BotInteractionMessage botInteractionMessage;

	public BotQuickReplyMessage() {
	}

	public Long getQuickMsgId() {
		return this.quickMsgId;
	}

	public void setQuickMsgId(Long quickMsgId) {
		this.quickMsgId = quickMsgId;
	}

	public Boolean getIsStatic() {
		return this.isStatic;
	}

	public void setIsStatic(Boolean isStatic) {
		this.isStatic = isStatic;
	}

	public List<BotButton> getBotButtons() {
		return this.botButtons;
	}

	public void setBotButtons(List<BotButton> botButtons) {
		this.botButtons = botButtons;
	}

	public BotButton addBotButton(BotButton botButton) {
		getBotButtons().add(botButton);
		botButton.setBotQuickReplyMessage(this);

		return botButton;
	}

	public BotButton removeBotButton(BotButton botButton) {
		getBotButtons().remove(botButton);
		botButton.setBotQuickReplyMessage(null);

		return botButton;
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