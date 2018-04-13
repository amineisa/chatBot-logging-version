package com.chatbot.entity;

import java.io.Serializable;
import javax.persistence.*;
import java.util.List;


/**
 * The persistent class for the bot_method_type database table.
 * 
 */
@Entity
@Table(name="bot_method_type")
@NamedQuery(name="BotMethodType.findAll", query="SELECT b FROM BotMethodType b")
public class BotMethodType implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name="METHOD_TYPE_ID")
	private int methodTypeId;

	@Column(name="METHOD_TYPE_NAME")
	private String methodTypeName;

	//bi-directional many-to-one association to BotWebserviceMessage
	@OneToMany(mappedBy="botMethodType")
	private List<BotWebserviceMessage> botWebserviceMessages;

	public BotMethodType() {
	}

	public int getMethodTypeId() {
		return this.methodTypeId;
	}

	public void setMethodTypeId(int methodTypeId) {
		this.methodTypeId = methodTypeId;
	}

	public String getMethodTypeName() {
		return this.methodTypeName;
	}

	public void setMethodTypeName(String methodTypeName) {
		this.methodTypeName = methodTypeName;
	}

	public List<BotWebserviceMessage> getBotWebserviceMessages() {
		return this.botWebserviceMessages;
	}

	public void setBotWebserviceMessages(List<BotWebserviceMessage> botWebserviceMessages) {
		this.botWebserviceMessages = botWebserviceMessages;
	}

	public BotWebserviceMessage addBotWebserviceMessage(BotWebserviceMessage botWebserviceMessage) {
		getBotWebserviceMessages().add(botWebserviceMessage);
		botWebserviceMessage.setBotMethodType(this);

		return botWebserviceMessage;
	}

	public BotWebserviceMessage removeBotWebserviceMessage(BotWebserviceMessage botWebserviceMessage) {
		getBotWebserviceMessages().remove(botWebserviceMessage);
		botWebserviceMessage.setBotMethodType(null);

		return botWebserviceMessage;
	}

}