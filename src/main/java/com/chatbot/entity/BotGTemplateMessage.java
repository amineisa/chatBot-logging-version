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

import org.hibernate.annotations.Type;


/**
 * The persistent class for the bot_g_template_message database table.
 * 
 */
@Entity
@Table(name="bot_g_template_message")
@NamedQuery(name="BotGTemplateMessage.findAll", query="SELECT b FROM BotGTemplateMessage b")
public class BotGTemplateMessage implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name="G_T_MSG_ID")
	private Long gTMsgId;

	//bi-directional many-to-one association to BotInteractionMessage
	@ManyToOne
	@JoinColumn(name="MESSAGE_ID")
	private BotInteractionMessage botInteractionMessage;

	//bi-directional many-to-one association to BotTemplateElement
	@OneToMany(mappedBy="botGTemplateMessage")
	private List<BotTemplateElement> botTemplateElements;

	@Type(type= "org.hibernate.type.NumericBooleanType")
	@Column(name="IS_LIST_TEMPLATE")
	private Boolean isListTemplate;
	

	public Long getGTMsgId() {
		return this.gTMsgId;
	}

	public void setGTMsgId(Long gTMsgId) {
		this.gTMsgId = gTMsgId;
	}

	public BotInteractionMessage getBotInteractionMessage() {
		return this.botInteractionMessage;
	}

	public void setBotInteractionMessage(BotInteractionMessage botInteractionMessage) {
		this.botInteractionMessage = botInteractionMessage;
	}

	public List<BotTemplateElement> getBotTemplateElements() {
		return this.botTemplateElements;
	}

	public void setBotTemplateElements(List<BotTemplateElement> botTemplateElements) {
		this.botTemplateElements = botTemplateElements;
	}

	public BotTemplateElement addBotTemplateElement(BotTemplateElement botTemplateElement) {
		getBotTemplateElements().add(botTemplateElement);
		botTemplateElement.setBotGTemplateMessage(this);

		return botTemplateElement;
	}

	public BotTemplateElement removeBotTemplateElement(BotTemplateElement botTemplateElement) {
		getBotTemplateElements().remove(botTemplateElement);
		botTemplateElement.setBotGTemplateMessage(null);

		return botTemplateElement;
	}

	public Boolean isListTemplate() {
		return isListTemplate;
	}

	public void setListTemplate(Boolean isListTemplate) {
		this.isListTemplate = isListTemplate;
	}

	
	
}