package de.bitgilde.TIMAAT.model.FIPOP;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

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
 * The persistent class for the medium_analysis_list database table.
 *
 * @author Jens-Martin Loebel <loebel@bitgilde.de>
 * @author Mirko Scherf <mscherf@uni-mainz.de>
 */
@Entity
@Table(name="medium_analysis_list")
@NamedQuery(name="MediumAnalysisList.findAll", query="SELECT m FROM MediumAnalysisList m")
// @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class,
// property  = "id",
// scope     = MediumAnalysisList.class)
public class MediumAnalysisList implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private int id;

	@Transient
	@JsonProperty("mediumID")
	private int mediumID;

	@Transient
	@JsonProperty("mediumType")
	private String mediumType;

	// TODO text and title from translation

	@Column(name="global_permission")
	private byte globalPermission;

	@Column(name="created_at")
	private Timestamp createdAt;

	@Column(name="last_edited_at")
	private Timestamp lastEditedAt;

	private String uuid;

	//bi-directional many-to-one association to AnalysisSegment
	@OneToMany(mappedBy="mediumAnalysisList")
  @JsonManagedReference(value = "MediumAnalysisList-AnalysisSegment")
	private List<AnalysisSegment> analysisSegments;

	//bi-directional many-to-one association to Annotation
	@OneToMany(mappedBy="mediumAnalysisList", cascade=CascadeType.PERSIST)
  @JsonManagedReference(value = "MediumAnalysisList-Annotation")
	private List<Annotation> annotations;

	//bi-directional many-to-many association to CategorySet
	@ManyToMany
	@JoinTable(
		name="medium_analysis_list_restricted_category_set"
		, inverseJoinColumns={
			@JoinColumn(name="category_set_id")
			}
		, joinColumns={
			@JoinColumn(name="medium_analysis_list_id")
			}
		)
	private List<CategorySet> restrictedCategorySets;

	//bi-directional many-to-one association to MediaCollectionAnalysisList
	// @ManyToOne
	// @JoinColumn(name="media_collection_analysis_list_id")
	// private MediaCollectionAnalysisList mediaCollectionAnalysisList;

	//bi-directional many-to-one association to Medium
	@ManyToOne
  @JsonBackReference(value = "Medium-MediumAnalysisList")
	private Medium medium;

	@OneToOne(mappedBy="mediumAnalysisList")
	// @JsonManagedReference(value="MediumAnalysisList-Publication")
	// @JoinColumn(name="medium_analysis_list_id", nullable = true)
	private Publication publication;

	//bi-directional many-to-one association to UserAccount
	@ManyToOne
	@JoinColumn(name="created_by_user_account_id")
	@JsonBackReference(value = "MediumAnalysisList-CreatedByUserAccount")
	private UserAccount createdByUserAccount;

	@Transient
	@JsonProperty("createdByUserAccountId")
	private int createdByUserAccountId;

	//bi-directional many-to-one association to UserAccount
	@ManyToOne
	@JoinColumn(name="last_edited_by_user_account_id")
	@JsonBackReference(value = "MediumAnalysisList-LastEditedByUserAccount")
	private UserAccount lastEditedByUserAccount;

	@Transient
	@JsonProperty("lastEditedByUserAccountId")
	private int lastEditedByUserAccountId;

	//bi-directional many-to-many association to Tag
	@ManyToMany
	@JoinTable(
		name="medium_analysis_list_has_tag"
		, inverseJoinColumns={
			@JoinColumn(name="tag_id")
			}
		, joinColumns={
			@JoinColumn(name="medium_analysis_list_id")
			}
		)
	private List<Tag> tags;

	//bi-directional many-to-one association to MediumAnalysisListTranslation
	@OneToMany(mappedBy="mediumAnalysisList")
	private List<MediumAnalysisListTranslation> mediumAnalysisListTranslations;

	//bi-directional many-to-one association to UserAccountHasMediumAnalysisList
	@OneToMany(mappedBy="mediumAnalysisList")
	@JsonManagedReference(value = "MediumAnalysisList-UserAccountHasMediumAnalysisList")
	private List<UserAccountHasMediumAnalysisList> userAccountHasMediumAnalysisLists;

	public MediumAnalysisList() {
	}

	public int getId() {
		return this.id;
	}

	public int getMediumID() {
		if ( this.medium != null ) return this.medium.getId();
		return 0;
	}

	public String getMediumType() {
		if (this.medium != null) return this.medium.getMediaType().getMediaTypeTranslations().get(0).getType();
		return "";
	}

	public void setId(int id) {
		this.id = id;
	}

	public byte getGlobalPermission() {
		return this.globalPermission;
	}

	public void setGlobalPermission(byte globalPermission) {
		this.globalPermission = globalPermission;
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

	public List<AnalysisSegment> getAnalysisSegments() {
		return this.analysisSegments;
	}

	public void setAnalysisSegments(List<AnalysisSegment> analysisSegments) {
		this.analysisSegments = analysisSegments;
	}

	public AnalysisSegment addAnalysisSegment(AnalysisSegment analysisSegment) {
		getAnalysisSegments().add(analysisSegment);
		analysisSegment.setMediumAnalysisList(this);

		return analysisSegment;
	}

	public AnalysisSegment removeAnalysisSegment(AnalysisSegment analysisSegment) {
		getAnalysisSegments().remove(analysisSegment);
		// analysisSegment.setMediumAnalysisList(null);

		return analysisSegment;
	}

	public List<Annotation> getAnnotations() {
		return this.annotations;
	}

	public void setAnnotations(List<Annotation> annotations) {
		this.annotations = annotations;
	}

	public Annotation addAnnotation(Annotation annotation) {
		getAnnotations().add(annotation);
		annotation.setMediumAnalysisList(this);

		return annotation;
	}

	public Annotation removeAnnotation(Annotation annotation) {
		getAnnotations().remove(annotation);
		annotation.setMediumAnalysisList(null);

		return annotation;
	}

	public List<CategorySet> getRestrictedCategorySets() {
		return this.restrictedCategorySets;
	}

	public void setRestrictedCategorySets(List<CategorySet> categorySets) {
		this.restrictedCategorySets = categorySets;
	}

	// public MediaCollectionAnalysisList getMediaCollectionAnalysisList() {
	// 	return this.mediaCollectionAnalysisList;
	// }

	// public void setMediaCollectionAnalysisList(MediaCollectionAnalysisList mediaCollectionAnalysisList) {
	// 	this.mediaCollectionAnalysisList = mediaCollectionAnalysisList;
	// }

	public Medium getMedium() {
		return this.medium;
	}

	public void setMedium(Medium medium) {
		this.medium = medium;
	}

	public Publication getPublication() {
		return this.publication;
	}

	public void setPublication(Publication publication) {
		this.publication = publication;
	}

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

	public List<Tag> getTags() {
		return this.tags;
	}

	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

	public List<MediumAnalysisListTranslation> getMediumAnalysisListTranslations() {
		return this.mediumAnalysisListTranslations;
	}

	public void setMediumAnalysisListTranslations(List<MediumAnalysisListTranslation> mediumAnalysisListTranslations) {
		this.mediumAnalysisListTranslations = mediumAnalysisListTranslations;
	}

	public MediumAnalysisListTranslation addMediumAnalysisListTranslation(MediumAnalysisListTranslation mediumAnalysisListTranslation) {
		getMediumAnalysisListTranslations().add(mediumAnalysisListTranslation);
		mediumAnalysisListTranslation.setMediumAnalysisList(this);

		return mediumAnalysisListTranslation;
	}

	public MediumAnalysisListTranslation removeMediumAnalysisListTranslation(MediumAnalysisListTranslation mediumAnalysisListTranslation) {
		getMediumAnalysisListTranslations().remove(mediumAnalysisListTranslation);
		mediumAnalysisListTranslation.setMediumAnalysisList(null);

		return mediumAnalysisListTranslation;
	}

	public void setTitle(String title, String langcode) {
		if ( this.mediumAnalysisListTranslations == null ) return;
		for ( MediumAnalysisListTranslation trans : this.mediumAnalysisListTranslations ) {
			if ( trans.getLanguage().getCode().compareTo(langcode) == 0 ) trans.setTitle(title);
		}
	}

	public void setText(String text, String langcode) {
		if ( this.mediumAnalysisListTranslations == null ) return;
		for ( MediumAnalysisListTranslation trans : this.mediumAnalysisListTranslations ) {
			if ( trans.getLanguage().getCode().compareTo(langcode) == 0 ) trans.setText(text);
		}
	}

	public List<UserAccountHasMediumAnalysisList> getUserAccountHasMediumAnalysisLists() {
		return this.userAccountHasMediumAnalysisLists;
	}

	public void setUserAccountHasMediumAnalysisLists(List<UserAccountHasMediumAnalysisList> userAccountHasMediumAnalysisLists) {
		this.userAccountHasMediumAnalysisLists = userAccountHasMediumAnalysisLists;
	}

	public UserAccountHasMediumAnalysisList addUserAccountHasMediumAnalysisList(UserAccountHasMediumAnalysisList userAccountHasMediumAnalysisList) {
		getUserAccountHasMediumAnalysisLists().add(userAccountHasMediumAnalysisList);
		userAccountHasMediumAnalysisList.setMediumAnalysisList(this);

		return userAccountHasMediumAnalysisList;
	}

	public UserAccountHasMediumAnalysisList removeUserAccountHasMediumAnalysisList(UserAccountHasMediumAnalysisList userAccountHasMediumAnalysisList) {
		getUserAccountHasMediumAnalysisLists().remove(userAccountHasMediumAnalysisList);
		userAccountHasMediumAnalysisList.setMediumAnalysisList(null);

		return userAccountHasMediumAnalysisList;
	}

	public String getUuid() {
		return this.uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

}