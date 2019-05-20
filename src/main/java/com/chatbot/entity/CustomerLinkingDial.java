package com.chatbot.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name="CUSTOMER_LINKING_DIALS")
@NamedQuery(name="CustomerLinkingDial.findAll" , query="select cl from CustomerLinkingDial cl")
public class CustomerLinkingDial implements Serializable{
	private static final long serialVersionUID = 1L; 

	@Id
	@Column(name="ID")
	//@GeneratedValue(strategy=GenerationType.AUTO, generator="LINKINK_DIALS_seq_gen")
	//@SequenceGenerator(name="LINKINK_DIALS_seq_gen", sequenceName="LINKINK_DIALS_SEQ")
	private Long id;
	
	@Column(name="DIAL")
	private String dial;
	
	@Column(name="LINKING_DATE")
	private Date linkingDate;
	
	@Column(name="UNLINKING_DATE")
	private Date unlinkingDate;
	
	@ManyToOne
	@JoinColumn(referencedColumnName="SENDER_ID" , name="CUSTOMER_ID")
	private CustomerProfile customerProfile;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDial() {
		return dial;
	}

	public void setDial(String dial) {
		this.dial = dial;
	}

	public Date getLinkingDate() {
		return linkingDate;
	}

	public void setLinkingDate(Date linkingDate) {
		this.linkingDate = linkingDate;
	}

	public Date getUnlinkingDate() {
		return unlinkingDate;
	}

	public void setUnlinkingDate(Date unlinkingDate) {
		this.unlinkingDate = unlinkingDate;
	}

	public CustomerProfile getCustomerProfile() {
		return customerProfile;
	}

	public void setCustomerProfile(CustomerProfile customerProfile) {
		this.customerProfile = customerProfile;
	}
	
	
	
	
	
}
