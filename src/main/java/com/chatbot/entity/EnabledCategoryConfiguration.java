package com.chatbot.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="Enabled_Category_Configuration")
public class EnabledCategoryConfiguration {
	
	@Id
	@Column(name="ID")
	private Long id;
	
	@Column(name="English_Categories")
	private String englishCategories;

	@Column(name="Arabic_Categories")
	private String arabicCategories;
	
	
	@OneToOne()
	@JoinColumn(name = "category_label" , referencedColumnName="TEXT_ID" , nullable=true)
	private BotText categoryLabel;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEnglishCategories() {
		return englishCategories;
	}

	public void setEnglishCategories(String englishCategories) {
		this.englishCategories = englishCategories;
	}

	public String getArabicCategories() {
		return arabicCategories;
	}

	public void setArabicCategories(String arabicCategories) {
		this.arabicCategories = arabicCategories;
	}

	public BotText getCategoryLabel() {
		return categoryLabel;
	}

	public void setCategoryLabel(BotText categoryLabel) {
		this.categoryLabel = categoryLabel;
	}

	

}
