package com.chatbot.entity;

import java.util.ArrayList;

public class UserSelections {

	private String originalPayLoad;

	private String phoneNumber;

	private String productIdAndOperationName;

	private String addonId;

	private String billingProfileParam; 
	
	private String lastPayLoad;

	private String parentPayLoad;

	private String productIdForRenew;

	private boolean isSubscribed;

	private ArrayList<String> parametersListForRelatedProducts;

	private ArrayList<String> consumptionNames;

	private String freeText;

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

	public String getLastPayLoad() {
		return lastPayLoad;
	}

	public void setLastPayLoad(String lastPayLoad) {
		this.lastPayLoad = lastPayLoad;
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

	public ArrayList<String> getParametersListForRelatedProducts() {
		return parametersListForRelatedProducts;
	}

	public void setParametersListForRelatedProducts(ArrayList<String> parametersListForRelatedProducts) {
		this.parametersListForRelatedProducts = parametersListForRelatedProducts;
	}

	public ArrayList<String> getConsumptionNames() {
		return consumptionNames;
	}

	public void setConsumptionNames(ArrayList<String> consumptionNames) {
		this.consumptionNames = consumptionNames;
	}

	public void setFreeText(String freeText) {
		this.freeText = freeText;
	}

	public String getFreeText() {
		return freeText;
	}
	

	public String getBillingProfileParam() {
		return billingProfileParam;
	}

	public void setBillingProfileParam(String billingProfileParam) {
		this.billingProfileParam = billingProfileParam;
	}

	public boolean isSubscribed() {
		return isSubscribed;
	}

	public void setSubscribed(boolean isSubscribed) {
		this.isSubscribed = isSubscribed;
	}

	@Override
	public String toString() {
		return " Original PayLoad " + this.getOriginalPayLoad() + " Phone Number " + this.getPhoneNumber() + " ProductId And OperationName " + this.getProductIdAndOperationName() + " Addon Id "
				+ this.getAddonId() + " last PayLoad " + this.getLastPayLoad() + " Parent PayLoad " + this.getParentPayLoad() + " ProductID For Renew " + this.getProductIdForRenew();
	}

}
