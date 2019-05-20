package com.chatbot.entity;

import java.io.Serializable;

import lombok.ToString;

@ToString
public class UserSelection implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private String senderId;
	private String originalPayLoad;
	private String phoneNumber;
	private String productIdAndOperationName;
	private String addonId;
	private String parentPayLoad;
	private String productIdForRenew;
	private String parametersListForRelatedProducts;
	private String freeText;
	private String rateplanNameForMigration;
	private String productNameForSallefny;	
	private String userDialForAuth;	
	private String activationCode;	
	private String scratcheddNumberForRecharge;
	private String akwaKartCategoryName;
	private int rateplanIdForMigration;
	private boolean isSubscribed;
	private String akwakartProductName;
	
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

	public String getProductNameForSallefny() {
		return productNameForSallefny;
	}

	public void setProductNameForSallefny(String productNameForSallefny) {
		this.productNameForSallefny = productNameForSallefny;
	}

	public String getUserDialForAuth() {
		return userDialForAuth;
	}

	public void setUserDialForAuth(String userDialForAuth) {
		this.userDialForAuth = userDialForAuth;
	}

	public String getActivationCode() {
		return activationCode;
	}

	public void setActivationCode(String activationCode) {
		this.activationCode = activationCode;
	}

	public String getScratcheddNumberForRecharge() {
		return scratcheddNumberForRecharge;
	}

	public void setScratcheddNumberForRecharge(String scratcheddNumberForRecharge) {
		this.scratcheddNumberForRecharge = scratcheddNumberForRecharge;
	}

	public String getAkwaKartCategoryName() {
		return akwaKartCategoryName;
	}

	public void setAkwaKartCategoryName(String akwaKartCategoryName) {
		this.akwaKartCategoryName = akwaKartCategoryName;
	}

	public String getAkwakartProductName() {
		return akwakartProductName;
	}

	public void setAkwakartProductName(String akwakartProductName) {
		this.akwakartProductName = akwakartProductName;
	}

	
	
	
	

}
