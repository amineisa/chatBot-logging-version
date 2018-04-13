package com.chatbot.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * The persistent class for the bot_webservice_mapping database table.
 */
@Entity
@Table(name = "bot_webservice_mapping")
@NamedQuery(name = "BotWebserviceMapping.findAll", query = "SELECT b FROM BotWebserviceMapping b")
public class BotWebserviceMapping implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "WS_MAP_ID")
	private Long wsMapId;

	@Column(name = "FIELD_MAPED_TO")
	private String fieldMapedTo;

	@Column(name = "FIELD_NAME")
	private String fieldName;

	@Column(name = "FIELD_TYPE")
	private Long fieldType;

	// bi-directional many-to-one association to BotWebserviceMessage
	@ManyToOne
	@JoinColumn(name = "WS_MSG_ID")
	private BotWebserviceMessage botWebserviceMessage;

	public BotWebserviceMapping() {
	}

	public Long getWsMapId() {
		return this.wsMapId;
	}

	public void setWsMapId(Long wsMapId) {
		this.wsMapId = wsMapId;
	}

	public String getFieldMapedTo() {
		return this.fieldMapedTo;
	}

	public void setFieldMapedTo(String fieldMapedTo) {
		this.fieldMapedTo = fieldMapedTo;
	}

	public String getFieldName() {
		return this.fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public BotWebserviceMessage getBotWebserviceMessage() {
		return this.botWebserviceMessage;
	}

	public void setBotWebserviceMessage(BotWebserviceMessage botWebserviceMessage) {
		this.botWebserviceMessage = botWebserviceMessage;
	}

	public Long getFieldType() {
		return fieldType;
	}

	public void setFieldType(Long fieldType) {
		this.fieldType = fieldType;
	}

}