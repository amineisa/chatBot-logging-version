package com.chatbot.entity;

import java.io.Serializable;
import java.time.Instant;

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
	private String emeraldChildDial;
	private String emeraldChildDialToRemove;
	private String emeraldRateplanProductName;
	private String emeraldDialForDistribute;
	private String emeraldTraficCaseID;
	private String emeraldDistributeAmount;
	private String emeraldTransferFromDial;
	private String emeraldTransferToDial;
	private String emeraldDistbuteAfterTrans; 	
	private String activationCode;	
	private String scratcheddNumberForRecharge;
	private String akwaKartCategoryName;
	private Instant eventRecevingTimeStamp;
	private int rateplanIdForMigration;
	private boolean isSubscribed;
	private String akwakartProductName;
	private int accountDeductionHistory; 
	private String balanceValue;
	
	
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

	


	public String getEmeraldChildDial() {
		return emeraldChildDial;
	}

	public void setEmeraldChildDial(String emeraldChildDial) {
		this.emeraldChildDial = emeraldChildDial;
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

	public int getAccountDeductionHistory() {
		return accountDeductionHistory;
	}

	public void setAccountDeductionHistory(int accountDeductionHistory) {
		this.accountDeductionHistory = accountDeductionHistory;
	}

	public String getBalanceValue() {
		return balanceValue;
	}

	public void setBalanceValue(String balanceValue) {
		this.balanceValue = balanceValue;
	}

	public String getEmeraldRateplanProductName() {
		return emeraldRateplanProductName;
	}

	public void setEmeraldRateplanProductName(String emeraldRateplanProductName) {
		this.emeraldRateplanProductName = emeraldRateplanProductName;
	}

	public String getEmeraldChildDialToRemove() {
		return emeraldChildDialToRemove;
	}

	public void setEmeraldChildDialToRemove(String emeraldChildDialToRemove) {
		this.emeraldChildDialToRemove = emeraldChildDialToRemove;
	}

	public String getEmeraldDialForDistribute() {
		return emeraldDialForDistribute;
	}

	public void setEmeraldDialForDistribute(String emeraldDialForDistribute) {
		this.emeraldDialForDistribute = emeraldDialForDistribute;
	}

	

	public String getEmeraldDistributeAmount() {
		return emeraldDistributeAmount;
	}

	public void setEmeraldDistributeAmount(String emeraldDistributeAmount) {
		this.emeraldDistributeAmount = emeraldDistributeAmount;
	}

	public String getEmeraldTransferFromDial() {
		return emeraldTransferFromDial;
	}

	public void setEmeraldTransferFromDial(String emeraldTransferFromDial) {
		this.emeraldTransferFromDial = emeraldTransferFromDial;
	}

	public String getEmeraldTraficCaseID() {
		return emeraldTraficCaseID;
	}

	public void setEmeraldTraficCaseID(String emeraldTraficCaseID) {
		this.emeraldTraficCaseID = emeraldTraficCaseID;
	}

	public String getEmeraldTransferToDial() {
		return emeraldTransferToDial;
	}

	public void setEmeraldTransferToDial(String emeraldTransferToDial) {
		this.emeraldTransferToDial = emeraldTransferToDial;
	}

	public String getEmeraldDistbuteAfterTrans() {
		return emeraldDistbuteAfterTrans;
	}

	public void setEmeraldDistbuteAfterTrans(String emeraldDistbuteAfterTrans) {
		this.emeraldDistbuteAfterTrans = emeraldDistbuteAfterTrans;
	}
	

	public Instant getEventRecevingTimeStamp() {
		return eventRecevingTimeStamp;
	}

	public void setEventRecevingTimeStamp(Instant eventRecevingTimeStamp) {
		this.eventRecevingTimeStamp = eventRecevingTimeStamp;
	}

	@Override
	public String toString() {
		return "UserSelection [senderId=" + senderId + ", originalPayLoad=" + originalPayLoad + ", phoneNumber=" + phoneNumber + ", productIdAndOperationName=" + productIdAndOperationName
				+ ", addonId=" + addonId + ", parentPayLoad=" + parentPayLoad + ", productIdForRenew=" + productIdForRenew + ", parametersListForRelatedProducts=" + parametersListForRelatedProducts
				+ ", freeText=" + freeText + ", rateplanNameForMigration=" + rateplanNameForMigration + ", productNameForSallefny=" + productNameForSallefny + ", emeraldChildDial=" + emeraldChildDial
				+ ", emeraldChildDialToRemove=" + emeraldChildDialToRemove + ", emeraldRateplanProductName=" + emeraldRateplanProductName + ", emeraldDialForDistribute=" + emeraldDialForDistribute
				+ ", emeraldTraficCaseID=" + emeraldTraficCaseID + ", emeraldDistributeAmount=" + emeraldDistributeAmount + ", emeraldTransferFromDial=" + emeraldTransferFromDial
				+ ", emeraldTransferToDial=" + emeraldTransferToDial + ", emeraldDistbuteAfterTrans=" + emeraldDistbuteAfterTrans + ", activationCode=" + activationCode
				+ ", scratcheddNumberForRecharge=" + scratcheddNumberForRecharge + ", akwaKartCategoryName=" + akwaKartCategoryName + ", rateplanIdForMigration=" + rateplanIdForMigration
				+ ", isSubscribed=" + isSubscribed + ", akwakartProductName=" + akwakartProductName + ", accountDeductionHistory=" + accountDeductionHistory + ", balanceValue=" + balanceValue + "]";
	}
	
	
	
	
}
