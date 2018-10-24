package com.chatbot.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "CUSTOMER_PROFILE")
@NamedQuery(name = "CustomerProfile.findAll", query = "SELECT c FROM CustomerProfile c")
public class CustomerProfile {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "SENDER_ID", nullable = false)
	private String senderID;

	@Column(name = "LOCAL")
	private String locale;

	@Column(name = "MSISDN")
	private String msisdn;

	@Column(name = "LINKING_Date")
	private Date linkingDate;

	@Column(name = "FIRST_INSERTION")
	private Date firstInsertion;

	@Column(name = "LAST_GET_PROFILE_DATE")
	private Date lastGetProfileWSCall;

	@Column(name = "CUSTOMER_LAST_SEEN")
	private Date customerLastSeen;
	
	@Column
	private String firstName;
	


	public String getSenderID() {
		return senderID;
	}

	public void setSenderID(String senderID) {
		this.senderID = senderID;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public String getMsisdn() {
		return msisdn;
	}

	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}

	public Date getLinkingDate() {
		return linkingDate;
	}

	public void setLinkingDate(Date linkingDate) {
		this.linkingDate = linkingDate;
	}

	public Date getFirstInsertion() {
		return firstInsertion;
	}

	public void setFirstInsertion(Date firstInsertion) {
		this.firstInsertion = firstInsertion;
	}

	public Date getLastGetProfileWSCall() {
		return lastGetProfileWSCall;
	}

	public void setLastGetProfileWSCall(Date lastGetProfileWSCall) {
		this.lastGetProfileWSCall = lastGetProfileWSCall;
	}

	public Date getCustomerLastSeen() {
		return customerLastSeen;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public void setCustomerLastSeen(Date customerLastSeen) {
		this.customerLastSeen = customerLastSeen;
	}

	 
	
	@Override
	public String toString() {
		return "CustomerProfile [senderID=" + senderID + ", local=" + locale + ", msisdn=" + msisdn + ", linkingDate=" + linkingDate + ", firstInsertion=" + firstInsertion + ", lastGetProfileWSCall="
				+ lastGetProfileWSCall + ", customerLastSeen=" + customerLastSeen + "]";
	}

}
