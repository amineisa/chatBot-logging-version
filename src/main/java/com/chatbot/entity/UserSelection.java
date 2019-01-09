package com.chatbot.entity;

import java.io.Serializable;


/*@Entity
@Table(name="USER_SELECTIONS")
@NamedQuery( query="SELECT u FROM UserSelection u ", name = "UserSelection.findAll")*/
public class UserSelection implements Serializable{

	private static final long serialVersionUID = 1L;
	
	/*@Id
	@Column(name="SENDER_ID")*/
	private String senderId;
	
	/*@Column(name="ORIGINAL_PAYLOAD")*/
	private String originalPayLoad;

	/*@Column(name="PHONE_NUMBER")*/
	private String phoneNumber;

	/*@Column(name="PRODUCT_ID_OPERATION_NAME")*/
	private String productIdAndOperationName;

	/*@Column(name="AADON_ID")*/
	private String addonId;

	/*@Column(name="PARENT_PAYLOAD")*/
	private String parentPayLoad;

	/*@Column(name="PRODUCT_ID_FOR_RENEW")*/
	private String productIdForRenew;

	/*@Column(name="IS_SUBSCRIBE")*/
	private boolean isSubscribed;

	/*@Column(name="PARAMETER_FOR_RP")*/
	private String parametersListForRelatedProducts;

	/*@Column(name="FREE_TEXT")*/
	private String freeText;
	
	/*@Column(name="MIGRATION_ID")*/
	private String rateplanNameForMigration;
	
	/*@Column(name="MIGRATION_NAME")*/
	private int rateplanIdForMigration;
	
	public String getSenderId() {
		return senderId;
	}

	public void setSenderId(String senderId) {
		this.senderId = senderId;
	}

	public String getOriginalPayLoad() {
		return originalPayLoad;
	}

	public void setOriginalPayLoad(String originalPayLoad) {
		this.originalPayLoad = originalPayLoad;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getProductIdAndOperationName() {
		return productIdAndOperationName;
	}

	public void setProductIdAndOperationName(String productIdAndOperationName) {
		this.productIdAndOperationName = productIdAndOperationName;
	}

	public String getAddonId() {
		return addonId;
	}

	public void setAddonId(String addonId) {
		this.addonId = addonId;
	}


	public String getParentPayLoad() {
		return parentPayLoad;
	}

	public void setParentPayLoad(String parentPayLoad) {
		this.parentPayLoad = parentPayLoad;
	}

	public String getProductIdForRenew() {
		return productIdForRenew;
	}

	public void setProductIdForRenew(String productIdForRenew) {
		this.productIdForRenew = productIdForRenew;
	}

	public String getParametersListForRelatedProducts() {
		return parametersListForRelatedProducts;
	}

	public void setParametersListForRelatedProducts(String parametersListForRelatedProducts) {
		this.parametersListForRelatedProducts = parametersListForRelatedProducts;
	}

	public boolean isSubscribed() {
		return isSubscribed;
	}

	public void setSubscribed(boolean isSubscribed) {
		this.isSubscribed = isSubscribed;
	}
	
	

	public String getFreeText() {
		return freeText;
	}

	public void setFreeText(String freeText) {
		this.freeText = freeText;
	}

	public String getRateplanNameForMigration() {
		return rateplanNameForMigration;
	}

	public void setRateplanNameForMigration(String rateplanNameForMigration) {
		this.rateplanNameForMigration = rateplanNameForMigration;
	}

	public int getRateplanIdForMigration() {
		return rateplanIdForMigration;
	}

	public void setRateplanIdForMigration(int rateplanIdForMigration) {
		this.rateplanIdForMigration = rateplanIdForMigration;
	}

	
	

}
