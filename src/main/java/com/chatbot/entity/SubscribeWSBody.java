package com.chatbot.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name="SUBSCRIPTION_WRAPPER_ENTITY")
@NamedQuery(name="SubscribeWSBody.findAll" , query="select s from SubscribeWSBody s ")
public class SubscribeWSBody implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@Id
	@Column(name="BUNDLE_ID")
	private String bundleId;
	
	@Column(name="OPERATION_NAME")
	private String operationName;
	
	@Column(name="EXTRA_ID")
	private String extraId;
	
	@Column(name="SEVICE_NAME")
	private String serviceName;

	public String getBundleId() {
		return bundleId;
	}

	public void setBundleId(String bundleId) {
		this.bundleId = bundleId;
	}

	public String getOperationName() {
		return operationName;
	}

	public void setOperationName(String operationName) {
		this.operationName = operationName;
	}

	public String getExtraId() {
		return extraId;
	}

	public void setExtraId(String extraId) {
		this.extraId = extraId;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	
	
	

}
