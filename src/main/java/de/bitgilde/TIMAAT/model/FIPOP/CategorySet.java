package de.bitgilde.TIMAAT.model.FIPOP;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/*
 Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

/**
 * The persistent class for the category_set database table.
 *
 * @author Jens-Martin Loebel <loebel@bitgilde.de>
 * @author Mirko Scherf <mscherf@uni-mainz.de>
 */
@Entity
@Table(name="category_set")
@NamedQuery(name="CategorySet.findAll", query="SELECT c FROM CategorySet c")
public class CategorySet implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private int id;

	@Column(name="created_at")
	private Timestamp createdAt;

	@Column(name="last_edited_at")
	private Timestamp lastEditedAt;

	private String name;

	//bi-directional many-to-one association to AnalysisSpeech
	// @OneToMany(mappedBy="categorySet")
	// private List<AnalysisSpeech> analysisSpeeches;

	//bi-directional many-to-one association to AnalysisVoice
	// @OneToMany(mappedBy="categorySet")
	// private List<AnalysisVoice> analysisVoices;

	//bi-directional many-to-one association to UserAccount
	@ManyToOne
	@JoinColumn(name="created_by_user_account_id")
	@JsonBackReference(value = "CategorySet-CreatedByUserAccount")
	private UserAccount createdByUserAccount;

	@Transient
	@JsonProperty("createdByUserAccountId")
	private int createdByUserAccountId;

	//bi-directional many-to-one association to UserAccount
	@ManyToOne
	@JoinColumn(name="last_edited_by_user_account_id")
	@JsonBackReference(value = "CategorySet-LastEditedByUserAccount")
	private UserAccount lastEditedByUserAccount;

	@Transient
	@JsonProperty("lastEditedByUserAccountId")
	private int lastEditedByUserAccountId;


	//bi-directional many-to-one association to CategorySetHasCategory
	@OneToMany(mappedBy="categorySet", cascade = CascadeType.PERSIST)
	@JsonManagedReference(value = "CategorySet-CategorySetHasCategory")
	private Set<CategorySetHasCategory> categorySetHasCategories;

	//bi-directional many-to-many association to Medium
	@ManyToMany(mappedBy= "restrictedCategorySets")
	@JsonIgnore
	private List<Medium> mediums;

	//bi-directional many-to-many association to Music
	@ManyToMany(mappedBy= "restrictedCategorySets")
	@JsonIgnore
	private List<Music> musicList;

	//bi-directional many-to-one association to UserAccountHasCategorySet
	@ManyToMany(mappedBy= "restrictedCategorySets")
	@JsonIgnore
	private List<MediumAnalysisList> mediumAnalysisLists;

	//bi-directional many-to-one association to UserAccountHasCategorySet
	@OneToMany(mappedBy="categorySet")
	@JsonManagedReference(value = "CategorySet-UserAccountHasCategorySet")
	private List<UserAccountHasCategorySet> userAccountHasCategorySets;

	public CategorySet() {
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Timestamp getCreatedAt() {
		return this.createdAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}

	public Timestamp getLastEditedAt() {
		return this.lastEditedAt;
	}

	public void setLastEditedAt(Timestamp lastEditedAt) {
		this.lastEditedAt = lastEditedAt;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	// public List<AnalysisSpeech> getAnalysisSpeeches() {
	// 	return this.analysisSpeeches;
	// }

	// public void setAnalysisSpeeches(List<AnalysisSpeech> analysisSpeeches) {
	// 	this.analysisSpeeches = analysisSpeeches;
	// }

	// public AnalysisSpeech addAnalysisSpeech(AnalysisSpeech analysisSpeech) {
	// 	getAnalysisSpeeches().add(analysisSpeech);
	// 	analysisSpeech.setCategorySet(this);

	// 	return analysisSpeech;
	// }

	// public AnalysisSpeech removeAnalysisSpeech(AnalysisSpeech analysisSpeech) {
	// 	getAnalysisSpeeches().remove(analysisSpeech);
	// 	analysisSpeech.setCategorySet(null);

	// 	return analysisSpeech;
	// }

	// public List<AnalysisVoice> getAnalysisVoices() {
	// 	return this.analysisVoices;
	// }

	// public void setAnalysisVoices(List<AnalysisVoice> analysisVoices) {
	// 	this.analysisVoices = analysisVoices;
	// }

	// public AnalysisVoice addAnalysisVoice(AnalysisVoice analysisVoice) {
	// 	getAnalysisVoices().add(analysisVoice);
	// 	analysisVoice.setCategorySet(this);

	// 	return analysisVoice;
	// }

	// public AnalysisVoice removeAnalysisVoice(AnalysisVoice analysisVoice) {
	// 	getAnalysisVoices().remove(analysisVoice);
	// 	analysisVoice.setCategorySet(null);

	// 	return analysisVoice;
	// }

	public UserAccount getCreatedByUserAccount() {
		return this.createdByUserAccount;
	}

	public void setCreatedByUserAccount(UserAccount createdByUserAccount) {
		this.createdByUserAccount = createdByUserAccount;
	}

	public int getCreatedByUserAccountId() {
		return this.getCreatedByUserAccount().getId();
	}

	public UserAccount getLastEditedByUserAccount() {
		return this.lastEditedByUserAccount;
	}

	public void setLastEditedByUserAccount(UserAccount lastEditedByUserAccount) {
		this.lastEditedByUserAccount = lastEditedByUserAccount;
	}

	public int getLastEditedByUserAccountId() {
		if (Objects.isNull(this.getLastEditedByUserAccount())) return 0;
		return this.getLastEditedByUserAccount().getId();
	}

	public Set<CategorySetHasCategory> getCategorySetHasCategories() {
		return this.categorySetHasCategories;
	}

	public void setCategorySetHasCategories(Set<CategorySetHasCategory> categorySetHasCategories) {
		this.categorySetHasCategories = categorySetHasCategories;
	}

	public CategorySetHasCategory addCategorySetHasCategory(CategorySetHasCategory categorySetHasCategory) {
		getCategorySetHasCategories().add(categorySetHasCategory);
		categorySetHasCategory.setCategorySet(this);

		return categorySetHasCategory;
	}

	public CategorySetHasCategory removeCategorySetHasCategory(CategorySetHasCategory categorySetHasCategory) {
		getCategorySetHasCategories().remove(categorySetHasCategory);
		categorySetHasCategory.setCategorySet(null);

		return categorySetHasCategory;
	}

	public List<Medium> getMediums() {
		return this.mediums;
	}

	public void setMediums(List<Medium> mediums) {
		this.mediums = mediums;
	}

	public List<Music> getMusicList() {
		return this.musicList;
	}

	public void setMusicList(List<Music> musicList) {
		this.musicList = musicList;
	}

	public List<MediumAnalysisList> getMediumAnalysisLists() {
		return this.mediumAnalysisLists;
	}

	public void setMediumAnalysisLists(List<MediumAnalysisList> mediumAnalysisLists) {
		this.mediumAnalysisLists = mediumAnalysisLists;
	}

	public List<UserAccountHasCategorySet> getUserAccountHasCategorySets() {
		return this.userAccountHasCategorySets;
	}

	public void setUserAccountHasCategorySets(List<UserAccountHasCategorySet> userAccountHasCategorySets) {
		this.userAccountHasCategorySets = userAccountHasCategorySets;
	}

	public UserAccountHasCategorySet addUserAccountHasCategorySet(UserAccountHasCategorySet userAccountHasCategorySet) {
		getUserAccountHasCategorySets().add(userAccountHasCategorySet);
		userAccountHasCategorySet.setCategorySet(this);

		return userAccountHasCategorySet;
	}

	public UserAccountHasCategorySet removeUserAccountHasCategorySet(UserAccountHasCategorySet userAccountHasCategorySet) {
		getUserAccountHasCategorySets().remove(userAccountHasCategorySet);
		userAccountHasCategorySet.setCategorySet(null);

		return userAccountHasCategorySet;
	}

}