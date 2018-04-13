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
 * The persistent class for the bot_in_out_type database table.
 */
@Entity
@Table(name = "bot_in_out_type")
@NamedQuery(name = "BotInOutType.findAll", query = "SELECT b FROM BotInOutType b")
public class BotInOutType implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "IN_OUT_TYPE_ID")
	private int inOutTypeId;

	@Column(name = "IN_OUT_TYPE_NAME")
	private String inOutTypeName;

	// bi-directional many-to-one association to BotWebserviceMessage
	@OneToMany(mappedBy = "outType")
	private List<BotWebserviceMessage> botWebserviceMessages;

	public BotInOutType() {
	}

	public int getInOutTypeId() {
		return this.inOutTypeId;
	}

	public void setInOutTypeId(int inOutTypeId) {
		this.inOutTypeId = inOutTypeId;
	}

	public String getInOutTypeName() {
		return this.inOutTypeName;
	}

	public void setInOutTypeName(String inOutTypeName) {
		this.inOutTypeName = inOutTypeName;
	}

	public List<BotWebserviceMessage> getBotWebserviceMessages() {
		return this.botWebserviceMessages;
	}

	public void setBotWebserviceMessages(List<BotWebserviceMessage> botWebserviceMessages) {
		this.botWebserviceMessages = botWebserviceMessages;
	}

	public BotWebserviceMessage addBotWebserviceMessage(BotWebserviceMessage botWebserviceMessage) {
		getBotWebserviceMessages().add(botWebserviceMessage);
		botWebserviceMessage.setOutType(this);

		return botWebserviceMessage;
	}

	public BotWebserviceMessage removeBotWebserviceMessage(BotWebserviceMessage botWebserviceMessage) {
		getBotWebserviceMessages().remove(botWebserviceMessage);
		botWebserviceMessage.setOutType(null);

		return botWebserviceMessage;
	}

}