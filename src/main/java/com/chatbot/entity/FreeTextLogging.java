package com.chatbot.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;


@Entity
@Table(name = "FREE_TEXT_LOGGING")
@NamedQuery(name = "FreeTextLogging.findAll", query = "SELECT t FROM FreeTextLogging t")
public class FreeTextLogging implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@Id
	@Column(name = "ID")
	@GeneratedValue(strategy=GenerationType.AUTO, generator="FREE_TEXT_seq_gen")
	@SequenceGenerator(name="FREE_TEXT_seq_gen", sequenceName="FREE_TEXT_SEQ")
	private Long freeTextLoggingId;
	
	@Column(name="RECEIVED_TEXT")
	private String recivedFreeText;
	
	@Column(name="RECEIVING_TIME")
	private  Date receivingTime ;
	
	@OneToOne
	@JoinColumn(name = "INTERACTION_LOGGING")
	private InteractionLogging interactionLogging;

	

	public Long getFreeTextLoggingId() {
		return freeTextLoggingId;
	}

	public void setFreeTextLoggingId(Long freeTextLoggingId) {
		this.freeTextLoggingId = freeTextLoggingId;
	}

	public String getRecivedFreeText() {
		return recivedFreeText;
	}

	public void setRecivedFreeText(String recivedFreeText) {
		this.recivedFreeText = recivedFreeText;
	}

	public Date getReceivingTime() {
		return receivingTime;
	}

	public void setReceivingTime(Date receivingTime) {
		this.receivingTime = receivingTime;
	}

	public InteractionLogging getInteractionLogging() {
		return interactionLogging;
	}

	public void setInteractionLogging(InteractionLogging interactionLogging) {
		this.interactionLogging = interactionLogging;
	}

	
	
	
	
	
	
}
