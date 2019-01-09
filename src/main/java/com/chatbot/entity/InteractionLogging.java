package com.chatbot.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;


@Entity
@Table(name = "INTERACTION_LOGGING")
@NamedQuery(name = "InteractionLogging.findAll", query = "SELECT il FROM CustomerProfile il")
public class InteractionLogging implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO, generator="INTERACTION_LOGGING_seq_gen")
	@SequenceGenerator(name="INTERACTION_LOGGING_seq_gen", sequenceName="INTERACTION_LOGGING_SEQ")
	@Column(name = "INTERACTION_LOGGING")
	private Long interactionLoggingId;

	@Column(name = "ADDITIONAL_PARAM")
	private String additionalParam;

	@ManyToOne
	@JoinColumn(name = "INTERACTION_ID" , referencedColumnName="INTERACTION_ID")
	private BotInteraction botInteraction;

	
	@ManyToOne
	@JoinColumn(name = "SENDER_ID" ,  referencedColumnName="SENDER_ID")
	private CustomerProfile customerProfile;
	
	@Column(name="INTERACTION_CALLING_DATE")
	private Date interactionCallingDate;
	
	@Column(name="BOT_RESPONSE_DATE")
	private Date botResponseDate;
	

	public Long getInteractionLoggingId() {
		return interactionLoggingId;
	}

	public void setInteractionLoggingId(Long interactionLoggingId) {
		this.interactionLoggingId = interactionLoggingId;
	}

	public String getAdditionalParam() {
		return additionalParam;
	}

	public void setAdditionalParam(String additionalParam) {
		this.additionalParam = additionalParam;
	}

	public BotInteraction getBotInteraction() {
		return botInteraction;
	}

	public void setBotInteraction(BotInteraction botInteraction) {
		this.botInteraction = botInteraction;
	}

	public CustomerProfile getCustomerProfile() {
		return customerProfile;
	}

	public void setCustomerProfile(CustomerProfile customerProfile) {
		this.customerProfile = customerProfile;
	}

	public Date getInteractionCallingDate() {
		return interactionCallingDate;
	}

	public void setInteractionCallingDate(Date interactionCallingDate) {
		this.interactionCallingDate = interactionCallingDate;
	}

	public Date getBotResponseDate() {
		return botResponseDate;
	}

	public void setBotResponseDate(Date botResponseDate) {
		this.botResponseDate = botResponseDate;
	}

	
	
	
}