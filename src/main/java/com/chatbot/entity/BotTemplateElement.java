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
 * The persistent class for the bot_template_element database table.
 */
@Entity
@Table(name = "bot_template_element")
@NamedQuery(name = "BotTemplateElement.findAll", query = "SELECT b FROM BotTemplateElement b")
public class BotTemplateElement implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "ELEMENT_ID")
	private Long elementId;

	@Column(name = "IMAGE_URL")
	private String imageUrl;

	@Type(type= "org.hibernate.type.NumericBooleanType")
	@NotNull(message="NOT_NULL")
	private Boolean isStatic;

	// bi-directional many-to-one association to BotButton
	@OneToMany(mappedBy = "botTemplateElement")
	private List<BotButton> botButtons;

	// bi-directional many-to-one association to BotGTemplateMessage
	@ManyToOne
	@JoinColumn(name = "G_T_MSG_ID")
	private BotGTemplateMessage botGTemplateMessage;

	// bi-directional many-to-one association to BotText
	@ManyToOne
	@JoinColumn(name = "TITLE_TEXT_ID")
	private BotText title;

	// bi-directional many-to-one association to BotText
	@ManyToOne
	@JoinColumn(name = "SUB_TITLE_TEXT_ID")
	private BotText subTitle;
	
	
	@ManyToOne
	@JoinColumn(name = "BOTWEBSERVICEMESSAGE_ID")
	private BotWebserviceMessage botWebserviceMessage;

	public BotTemplateElement() {
	}

	public Long getElementId() {
		return this.elementId;
	}

	public void setElementId(Long elementId) {
		this.elementId = elementId;
	}

	public String getImageUrl() {
		return this.imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
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
		botButton.setBotTemplateElement(this);

		return botButton;
	}

	public BotButton removeBotButton(BotButton botButton) {
		getBotButtons().remove(botButton);
		botButton.setBotTemplateElement(null);

		return botButton;
	}

	public BotGTemplateMessage getBotGTemplateMessage() {
		return this.botGTemplateMessage;
	}

	public void setBotGTemplateMessage(BotGTemplateMessage botGTemplateMessage) {
		this.botGTemplateMessage = botGTemplateMessage;
	}

	public BotText getTitle() {
		return title;
	}

	public void setTitle(BotText title) {
		this.title = title;
	}

	public BotText getSubTitle() {
		return subTitle;
	}

	public void setSubTitle(BotText subTitle) {
		this.subTitle = subTitle;
	}

	public BotWebserviceMessage getBotWebserviceMessage() {
		return botWebserviceMessage;
	}

	public void setBotWebserviceMessage(BotWebserviceMessage botWebserviceMessage) {
		this.botWebserviceMessage = botWebserviceMessage;
	}

	
	
}