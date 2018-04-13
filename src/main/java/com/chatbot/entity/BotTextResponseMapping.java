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
 * The persistent class for the bot_text_response_mapping database table.
 */
@Entity
@Table(name = "bot_text_response_mapping")
@NamedQuery(name = "BotTextResponseMapping.findAll", query = "SELECT b FROM BotTextResponseMapping b")
public class BotTextResponseMapping implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "RS_MAP_ID")
	private Long rsMapId;

	@Column(name = "COMMON_PATH")
	private String commonPath;

	@Column(name = "EN_PARAMS")
	private String enParams;

	@Column(name = "AR_PARAMS")
	private String arParams;

	// bi-directional many-to-one association to BotWebserviceMessage
	@ManyToOne
	@JoinColumn(name = "WS_MSG_ID")
	private BotWebserviceMessage botWebserviceMessage;

	// bi-directional many-to-one association to BotText
	@ManyToOne
	@JoinColumn(name = "TEXT_ID")
	private BotText botText;

	public BotTextResponseMapping() {
	}

	public Long getRsMapId() {
		return this.rsMapId;
	}

	public void setRsMapId(Long rsMapId) {
		this.rsMapId = rsMapId;
	}

	public String getCommonPath() {
		return this.commonPath;
	}

	public void setCommonPath(String commonPath) {
		this.commonPath = commonPath;
	}

	public BotWebserviceMessage getBotWebserviceMessage() {
		return this.botWebserviceMessage;
	}

	public void setBotWebserviceMessage(BotWebserviceMessage botWebserviceMessage) {
		this.botWebserviceMessage = botWebserviceMessage;
	}

	public BotText getBotText() {
		return this.botText;
	}

	public void setBotText(BotText botText) {
		this.botText = botText;
	}

	public String getEnParams() {
		return enParams;
	}

	public void setEnParams(String enParams) {
		this.enParams = enParams;
	}

	public String getArParams() {
		return arParams;
	}

	public void setArParams(String arParams) {
		this.arParams = arParams;
	}

}