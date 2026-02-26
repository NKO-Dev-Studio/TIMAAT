package de.bitgilde.TIMAAT.model.FIPOP;

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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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
 * The persistent class for the actor database table.
 *
 * @author Jens-Martin Loebel <loebel@bitgilde.de>
 * @author Mirko Scherf <mscherf@uni-mainz.de>
 */
@Entity
@NamedQuery(name="Actor.findAll", query="SELECT a FROM Actor a")
public class Actor implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private int id;

	@Column(name="is_fictional", columnDefinition = "BOOLEAN")
	private Boolean isFictional;

	//bi-directional many-to-one association to ActorType
	@ManyToOne
	@JoinColumn(name="actor_type_id")
	private ActorType actorType;

	@ManyToOne
	@JoinColumn(name="created_by_user_account_id")
	@JsonBackReference(value = "Actor-CreatedByUserAccount")
	private UserAccount createdByUserAccount;

	@Transient
	@JsonProperty("createdByUserAccountId")
	private int createdByUserAccountId;

	@ManyToOne
	@JoinColumn(name="last_edited_by_user_account_id")
	@JsonBackReference(value = "Actor-LastEditedByUserAccount")
	private UserAccount lastEditedByUserAccount;

	@Transient
	@JsonProperty("lastEditedByUserAccountId")
	private int lastEditedByUserAccountId;

	@Column(name="created_at")
	private Timestamp createdAt;

	@Column(name="last_edited_at")
	private Timestamp lastEditedAt;

	//bi-directional many-to-one association to Name
	@ManyToOne(cascade = CascadeType.PERSIST)
	@JoinColumn(name="birth_name_actor_name_id")
	private ActorName birthName;

	//bi-directional many-to-one association to Name
	@ManyToOne(cascade = CascadeType.PERSIST)
	@JoinColumn(name="display_name_actor_name_id")
	private ActorName displayName;

	//bi-directional many-to-one association to Address
	@ManyToOne(cascade = CascadeType.PERSIST)
	@JoinColumn(name="primary_address_id")
	private Address primaryAddress;

	//bi-directional many-to-one association to EmailAddress
	@ManyToOne(cascade = CascadeType.PERSIST)
	@JoinColumn(name="primary_email_address_id")
	private EmailAddress actorHasEmailAddress;

	//bi-directional many-to-one association to PhoneNumber
	@ManyToOne(cascade = CascadeType.PERSIST)
	@JoinColumn(name="primary_phone_number_id")
	private PhoneNumber primaryPhoneNumber;

	//bi-directional many-to-one association to ActorHasAddress
	@OneToMany(mappedBy="actor")
	private List<ActorHasAddress> actorHasAddresses;

	//bi-directional many-to-one association to ActorHasEmailAddress
	@OneToMany(mappedBy="actor")
	private List<ActorHasEmailAddress> actorHasEmailAddresses;

	//bi-directional many-to-many association to PhoneNumber
	@OneToMany(mappedBy="actor")
	private List<ActorHasPhoneNumber> actorHasPhoneNumbers;

	//bi-directional many-to-many association to Role
	@ManyToMany
	@JoinTable(
		name="actor_has_role"
		, joinColumns={
			@JoinColumn(name="actor_id")
			}
		, inverseJoinColumns={
			@JoinColumn(name="role_id")
			}
		)
	private List<Role> roles;

  @ManyToMany
  @JoinTable(
          name="actor_restricted_category_set"
          , joinColumns={
          @JoinColumn(name="actor_id")
  }
          , inverseJoinColumns={
          @JoinColumn(name="category_set_id")
  }
  )
  private List<CategorySet> restrictedCategorySets;

  @ManyToMany
  @JoinTable(
          name="actor_has_category"
          , joinColumns={
          @JoinColumn(name="actor_id")
  }
          , inverseJoinColumns={
          @JoinColumn(name="category_id")
  }
  )
  private List<Category> categories;

	//bi-directional many-to-many association to Medium_Image
	@ManyToMany
	@JoinTable(
		name="actor_has_medium_image"
		, joinColumns={
			@JoinColumn(name="actor_id")
			}
		, inverseJoinColumns={
			@JoinColumn(name="medium_image_medium_id")
			}
		)
	private List<MediumImage> profileImages;

	//bi-directional many-to-one association to ActorIsLocatedInCountry
	// @OneToMany(mappedBy="actor")
	// private List<ActorIsLocatedInCountry> actorIsLocatedInCountries;

	//bi-directional many-to-one association to ActorName
	@OneToMany(mappedBy="actor")
	@JsonManagedReference(value = "Actor-ActorName")
	private List<ActorName> actorNames;

	//bi-directional many-to-one association to ActorRelatesToActor
	@OneToMany(mappedBy="actor1")
	@JsonIgnore // TODO temporary until implemented
	// @JsonManagedReference(value = "Actor-ActorRelatesToActor1")
	private List<ActorRelatesToActor> actorRelatesToActors1;

	//bi-directional many-to-one association to ActorRelatesToActor
	@OneToMany(mappedBy="actor2")
	@JsonIgnore // TODO temporary until implemented
	// @JsonManagedReference(value = "Actor-ActorRelatesToActor2")
	private List<ActorRelatesToActor> actorRelatesToActors2;

	//bi-directional many-to-many association to Annotation
	@ManyToMany(mappedBy="actors")
	@JsonIgnore
	private List<Annotation> annotations;

	//bi-directional one-to-one association to Collective
	@OneToOne(mappedBy="actor")
	private ActorCollective actorCollective;

	//bi-directional one-to-one association to ActorPerson
	@OneToOne(mappedBy="actor")
	private ActorPerson actorPerson;

	//bi-directional many-to-one association to SpatialSemanticsTypeActorPerson
	// @OneToMany(mappedBy="actor")
	// private List<SpatialSemanticsTypeActorPerson> spatialSemanticsTypeActorPersons;

	//bi-directional many-to-many association to Tag
	@ManyToMany
	@JoinTable(
		name="actor_has_tag"
		, inverseJoinColumns={
			@JoinColumn(name="tag_id")
			}
		, joinColumns={
			@JoinColumn(name="actor_id")
			}
		)
	private List<Tag> tags;

	public Actor() {
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Boolean getIsFictional() {
		return this.isFictional;
	}

	public void setIsFictional(Boolean isFictional) {
		this.isFictional = isFictional;
	}

	// public String getName() {
	// 	if ( this.actorNames != null && this.actorNames.size() > 0 ) return this.actorNames.get(0).getName(); // TODO get correct list item
	// 	return null;
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

	public ActorType getActorType() {
		return this.actorType;
	}

	public void setActorType(ActorType actorType) {
		this.actorType = actorType;
	}

	public List<ActorHasAddress> getActorHasAddresses() {
		return this.actorHasAddresses;
	}

	public void setActorHasAddresses(List<ActorHasAddress> actorHasAddresses) {
		this.actorHasAddresses = actorHasAddresses;
	}

	public ActorHasAddress addActorHasAddress(ActorHasAddress primaryAddress) {
		getActorHasAddresses().add(primaryAddress);
		primaryAddress.setActor(this);

		return primaryAddress;
	}

	public ActorHasAddress removeActorHasAddress(ActorHasAddress primaryAddress) {
		getActorHasAddresses().remove(primaryAddress);
		primaryAddress.setActor(null);

		return primaryAddress;
	}

	public Address getPrimaryAddress() {
		return this.primaryAddress;
	}

	public void setPrimaryAddress(Address primaryAddress) {
		this.primaryAddress = primaryAddress;
	}

	public List<ActorHasEmailAddress> getActorHasEmailAddresses() {
		return this.actorHasEmailAddresses;
	}

	public void setActorHasEmailAddresses(List<ActorHasEmailAddress> actorHasEmailAddresses) {
		this.actorHasEmailAddresses = actorHasEmailAddresses;
	}

	public ActorHasEmailAddress addActorHasEmailAddress(ActorHasEmailAddress actorHasEmailAddress) {
		getActorHasEmailAddresses().add(actorHasEmailAddress);
		actorHasEmailAddress.setActor(this);

		return actorHasEmailAddress;
	}

	public ActorHasEmailAddress removeActorHasEmailAddress(ActorHasEmailAddress actorHasEmailAddress) {
		getActorHasEmailAddresses().remove(actorHasEmailAddress);
		actorHasEmailAddress.setActor(null);

		return actorHasEmailAddress;
	}

	public EmailAddress getPrimaryEmailAddress() {
		return this.actorHasEmailAddress;
	}

	public void setPrimaryEmailAddress(EmailAddress actorHasEmailAddress) {
		this.actorHasEmailAddress = actorHasEmailAddress;
	}

	public List<ActorHasPhoneNumber> getActorHasPhoneNumbers() {
		return this.actorHasPhoneNumbers;
	}

	public void setActorHasPhoneNumbers(List<ActorHasPhoneNumber> actorHasPhoneNumbers) {
		this.actorHasPhoneNumbers = actorHasPhoneNumbers;
	}

	public ActorHasPhoneNumber addActorHasPhoneNumber(ActorHasPhoneNumber actorHasPhoneNumber) {
		getActorHasPhoneNumbers().add(actorHasPhoneNumber);
		actorHasPhoneNumber.setActor(this);

		return actorHasPhoneNumber;
	}

	public ActorHasPhoneNumber removeActorHasPhoneNumber(ActorHasPhoneNumber actorHasPhoneNumber) {
		getActorHasPhoneNumbers().remove(actorHasPhoneNumber);
		actorHasPhoneNumber.setActor(null);

		return actorHasPhoneNumber;
	}

	public PhoneNumber getPrimaryPhoneNumber() {
		return this.primaryPhoneNumber;
	}

	public void setPrimaryPhoneNumber(PhoneNumber actorHasPhoneNumber) {
		this.primaryPhoneNumber = actorHasPhoneNumber;
	}

	public List<Role> getRoles() {
		return this.roles;
	}

	public void setRoles(List<Role> roles) {
		this.roles = roles;
	}

	public Role addRole(Role role) {
		getRoles().add(role);
		role.addActor(this);

		return role;
	}

	public Role removeRole(Role role) {
		getRoles().remove(role);
		role.removeActor(this);

		return role;
	}

	public List<MediumImage> getProfileImages() {
		return this.profileImages;
	}

	public void setProfileImages(List<MediumImage> profileImages) {
		this.profileImages = profileImages;
	}

	public MediumImage addProfileImage(MediumImage profileImage) {
		getProfileImages().add(profileImage);
		profileImage.addActor(this);

		return profileImage;
	}

	public MediumImage removeProfileImage(MediumImage profileImage) {
		getProfileImages().remove(profileImage);
		profileImage.removeActor(this);

		return profileImage;
	}

	// public List<ActorIsLocatedInCountry> getActorIsLocatedInCountries() {
	// 	return this.actorIsLocatedInCountries;
	// }

	// public void setActorIsLocatedInCountries(List<ActorIsLocatedInCountry> actorIsLocatedInCountries) {
	// 	this.actorIsLocatedInCountries = actorIsLocatedInCountries;
	// }

	// public ActorIsLocatedInCountry addActorIsLocatedInCountry(ActorIsLocatedInCountry actorIsLocatedInCountry) {
	// 	getActorIsLocatedInCountries().add(actorIsLocatedInCountry);
	// 	actorIsLocatedInCountry.setActor(this);

	// 	return actorIsLocatedInCountry;
	// }

	// public ActorIsLocatedInCountry removeActorIsLocatedInCountry(ActorIsLocatedInCountry actorIsLocatedInCountry) {
	// 	getActorIsLocatedInCountries().remove(actorIsLocatedInCountry);
	// 	actorIsLocatedInCountry.setActor(null);

	// 	return actorIsLocatedInCountry;
	// }

	public List<ActorName> getActorNames() {
		return this.actorNames;
	}

	public void setActorNames(List<ActorName> actorNames) {
		this.actorNames = actorNames;
	}

	public ActorName addActorName(ActorName actorName) {
		getActorNames().add(actorName);
		actorName.setActor(this);

		return actorName;
	}

	public ActorName removeActorName(ActorName actorName) {
		getActorNames().remove(actorName);
		actorName.setActor(null);

		return actorName;
	}

	// birth name
	public ActorName getBirthName() {
		return this.birthName;
	}

	public void setBirthName(ActorName birthName) {
		this.birthName = birthName;
	}

	// display name
	public ActorName getDisplayName() {
		return this.displayName;
	}

	public void setDisplayName(ActorName displayName) {
		this.displayName = displayName;
	}

	public List<ActorRelatesToActor> getActorRelatesToActors1() {
		return this.actorRelatesToActors1;
	}

	public void setActorRelatesToActors1(List<ActorRelatesToActor> actorRelatesToActors1) {
		this.actorRelatesToActors1 = actorRelatesToActors1;
	}

	public ActorRelatesToActor addActorRelatesToActors1(ActorRelatesToActor actorRelatesToActors1) {
		getActorRelatesToActors1().add(actorRelatesToActors1);
		actorRelatesToActors1.setActor1(this);

		return actorRelatesToActors1;
	}

	public ActorRelatesToActor removeActorRelatesToActors1(ActorRelatesToActor actorRelatesToActors1) {
		getActorRelatesToActors1().remove(actorRelatesToActors1);
		actorRelatesToActors1.setActor1(null);

		return actorRelatesToActors1;
	}

	public List<ActorRelatesToActor> getActorRelatesToActors2() {
		return this.actorRelatesToActors2;
	}

	public void setActorRelatesToActors2(List<ActorRelatesToActor> actorRelatesToActors2) {
		this.actorRelatesToActors2 = actorRelatesToActors2;
	}

	public ActorRelatesToActor addActorRelatesToActors2(ActorRelatesToActor actorRelatesToActors2) {
		getActorRelatesToActors2().add(actorRelatesToActors2);
		actorRelatesToActors2.setActor2(this);

		return actorRelatesToActors2;
	}

	public ActorRelatesToActor removeActorRelatesToActors2(ActorRelatesToActor actorRelatesToActors2) {
		getActorRelatesToActors2().remove(actorRelatesToActors2);
		actorRelatesToActors2.setActor2(null);

		return actorRelatesToActors2;
	}

	public List<Annotation> getAnnotations() {
		return this.annotations;
	}

	public void setAnnotations(List<Annotation> annotations) {
		this.annotations = annotations;
	}

	public ActorCollective getActorCollective() {
		return this.actorCollective;
	}

	public void setActorCollective(ActorCollective actorCollective) {
		this.actorCollective = actorCollective;
	}

	public ActorPerson getActorPerson() {
		return this.actorPerson;
	}

	public void setActorPerson(ActorPerson actorPerson) {
		this.actorPerson = actorPerson;
	}

	// public List<SpatialSemanticsTypeActorPerson> getSpatialSemanticsTypeActorPersons() {
	// 	return this.spatialSemanticsTypeActorPersons;
	// }

	// public void setSpatialSemanticsTypeActorPersons(List<SpatialSemanticsTypeActorPerson> spatialSemanticsTypeActorPersons) {
	// 	this.spatialSemanticsTypeActorPersons = spatialSemanticsTypeActorPersons;
	// }

	// public SpatialSemanticsTypeActorPerson addSpatialSemanticsTypeActorPerson(SpatialSemanticsTypeActorPerson spatialSemanticsTypeActorPerson) {
	// 	getSpatialSemanticsTypeActorPersons().add(spatialSemanticsTypeActorPerson);
	// 	spatialSemanticsTypeActorPerson.setActor(this);

	// 	return spatialSemanticsTypeActorPerson;
	// }

	// public SpatialSemanticsTypeActorPerson removeSpatialSemanticsTypeActorPerson(SpatialSemanticsTypeActorPerson spatialSemanticsTypeActorPerson) {
	// 	getSpatialSemanticsTypeActorPersons().remove(spatialSemanticsTypeActorPerson);
	// 	spatialSemanticsTypeActorPerson.setActor(null);

	// 	return spatialSemanticsTypeActorPerson;
	// }

	public List<Tag> getTags() {
		return this.tags;
	}

	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

  public List<Category> getCategories() {
    return categories;
  }

  public void setCategories(List<Category> categories) {
    this.categories = categories;
  }

  public List<CategorySet> getRestrictedCategorySets() {
    return restrictedCategorySets;
  }

  public void setRestrictedCategorySets(List<CategorySet> categorySets) {
    this.restrictedCategorySets = categorySets;
  }
}