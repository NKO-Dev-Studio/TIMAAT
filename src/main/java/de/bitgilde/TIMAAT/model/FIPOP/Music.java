package de.bitgilde.TIMAAT.model.FIPOP;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
 * The persistent class for the music database table.
 *
 * @author Mirko Scherf <mscherf@uni-mainz.de>
 */
@Entity
@NamedQuery(name="Music.findAll", query="SELECT m FROM Music m")
public class Music implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

	private String beat;

	private String harmony;

	private String instrumentation;

	private String melody;

	private Short tempo;

	private String remark;

	@Column(name="created_at")
	private Timestamp createdAt;

	@Column(name="last_edited_at")
	private Timestamp lastEditedAt;

	//bi-directional many-to-one association to UserAccount
	@ManyToOne
	@JoinColumn(name="created_by_user_account_id")
	@JsonBackReference(value = "Music-CreatedByUserAccount")
	private UserAccount createdByUserAccount;

	@Transient
	@JsonProperty("createdByUserAccountId")
	private int createdByUserAccountId;

	//bi-directional many-to-one association to UserAccount
	@ManyToOne
	@JoinColumn(name="last_edited_by_user_account_id")
	@JsonBackReference(value = "Music-LastEditedByUserAccount")
	private UserAccount lastEditedByUserAccount;

	@Transient
	@JsonProperty("lastEditedByUserAccountId")
	private int lastEditedByUserAccountId;

	//bi-directional many-to-one association to AudioPostProduction
	@ManyToOne
	@JoinColumn(name="audio_post_production_id")
	private AudioPostProduction audioPostProduction;

	//bi-directional many-to-one association to DynamicMarking
	@ManyToOne
	@JoinColumn(name="dynamic_marking_id")
	private DynamicMarking dynamicMarking;

	//bi-directional many-to-one association to MusicType
	@ManyToOne
	@JoinColumn(name="music_type_id")
	private MusicType musicType;

	//bi-directional many-to-one association to MusicalKey
	@ManyToOne
	@JoinColumn(name="musical_key_id")
	private MusicalKey musicalKey;

	//bi-directional many-to-many association to ActorHasRole
	@ManyToMany
	@JoinTable(
		name="music_has_actor_with_role"
		, joinColumns={
			@JoinColumn(name="music_id")
			}
		, inverseJoinColumns={
			@JoinColumn(name="actor_has_role_actor_id", referencedColumnName="actor_id"),
			@JoinColumn(name="actor_has_role_role_id", referencedColumnName="role_id")
			}
		)
	@JsonIgnore
	private List<ActorHasRole> actorHasRoles;

	//bi-directional many-to-one association to MusicHasActorWithRole
	@OneToMany(mappedBy="music")
	private List<MusicHasActorWithRole> musicHasActorWithRoles;

	//bi-directional many-to-many association to CategorySet
	@ManyToMany
	@JoinTable(
		name="music_restricted_category_set"
		, joinColumns={
			@JoinColumn(name="music_id")
			}
		, inverseJoinColumns={
			@JoinColumn(name="category_set_id")
			}
		)
	private List<CategorySet> restrictedCategorySets;

	//bi-directional many-to-many association to Category
	@ManyToMany
	@JoinTable(
		name="music_has_category"
		, joinColumns={
			@JoinColumn(name="music_id")
			}
		, inverseJoinColumns={
			@JoinColumn(name="category_id")
			}
		)
	private List<Category> categories;

	//bi-directional many-to-many association to Tag
	@ManyToMany
	@JoinTable(
		name="music_has_tag"
		, inverseJoinColumns={
			@JoinColumn(name="tag_id")
			}
		, joinColumns={
			@JoinColumn(name="music_id")
			}
		)
	private List<Tag> tags;

	//bi-directional many-to-many association to VoiceLeadingPattern
	@ManyToMany
	@JoinTable(
		name="music_has_voice_leading_pattern"
		, inverseJoinColumns={
			@JoinColumn(name="voice_leading_pattern_id")
			}
		, joinColumns={
			@JoinColumn(name="music_id")
			}
		)
	private List<VoiceLeadingPattern> voiceLeadingPatternList;

	//bi-directional many-to-one association to Title
	@ManyToOne(cascade = CascadeType.PERSIST)
	@JoinColumn(name="display_title_title_id")
	private Title displayTitle;

	//bi-directional many-to-one association to Title
	@ManyToOne(cascade = CascadeType.PERSIST)
	@JoinColumn(name="original_title_title_id")
	private Title originalTitle;

	//bi-directional many-to-many association to Title
	@ManyToMany(mappedBy="musicList")
	private List<Title> titleList;

	//bi-directional one-to-one association to MusicNashid
	@OneToOne(mappedBy="music")
	private MusicNashid musicNashid;

	//bi-directional one-to-one association to MusicChurchMusic
	@OneToOne(mappedBy="music")
	private MusicChurchMusic musicChurchMusic;

	//bi-directional many-to-one association to TempoMarking
	@ManyToOne
	@JoinColumn(name="tempo_marking_id")
	private TempoMarking tempoMarking;

	//bi-directional many-to-one association to MusicTextSettingElementType
	@ManyToOne
	@JoinColumn(name="music_text_setting_element_type_id")
	private MusicTextSettingElementType musicTextSettingElementType;

	//bi-directional many-to-one association to MusicFormElement
	@OneToMany(mappedBy="music")
  @JsonManagedReference(value = "Music-MusicFormElement")
	private List<MusicFormElement> musicFormElementList;

	//bi-directional many-to-one association to MusicChangeInTempoElement
	@OneToMany(mappedBy="music")
	@JsonManagedReference(value = "Music-MusicChangeInTempoElement")
	private List<MusicChangeInTempoElement> musicChangeInTempoElementList;

	//bi-directional many-to-one association to MusicArticulationElement
	@OneToMany(mappedBy="music")
	@JsonManagedReference(value = "Music-MusicArticulationElement")
	private List<MusicArticulationElement> musicArticulationElementList;

	//bi-directional many-to-one association to MusicTextSettingElement
	@OneToMany(mappedBy="music")
	@JsonManagedReference(value = "Music-MusicTextSettingElement")
	private List<MusicTextSettingElement> musicTextSettingElementList;

	//bi-directional many-to-one association to MusicDynamicsElement
	@OneToMany(mappedBy="music")
	@JsonManagedReference(value = "Music-MusicDynamicsElement")
	private List<MusicDynamicsElement> musicDynamicsElementList;

    @OneToMany(mappedBy="music")
    @JsonManagedReference(value = "Music-MusicTranslation")
    private List<MusicTranslation> musicTranslationList;

	//bi-directional many-to-one association to MediumHasMusic
	@OneToMany(mappedBy="music")
	@JsonManagedReference(value = "Music-MediumHasMusic")
	private List<MediumHasMusic> mediumHasMusicList;

	// bi-directional one-to-one association to Medium
	// @Transient
	@OneToOne(mappedBy="music")
	// @JsonManagedReference(value="Medium-Music")
	private Medium medium;

  @OneToMany(mappedBy = "music")
  @JsonIgnoreProperties("music")
  private List<AnnotationHasMusic> annotationHasMusic;

	public Music() {
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

	public UserAccount getCreatedByUserAccount() {
		return this.createdByUserAccount;
	}

	public void setCreatedByUserAccount(UserAccount createdByUserAccount) {
		this.createdByUserAccount = createdByUserAccount;
	}

	public Timestamp getLastEditedAt() {
		return this.lastEditedAt;
	}

	public void setLastEditedAt(Timestamp lastEditedAt) {
		this.lastEditedAt = lastEditedAt;
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

	public String getBeat() {
		return this.beat;
	}

	public void setBeat(String beat) {
		this.beat = beat;
	}

	public String getHarmony() {
		return this.harmony;
	}

	public void setHarmony(String harmony) {
		this.harmony = harmony;
	}

	public String getInstrumentation() {
		return this.instrumentation;
	}

	public void setInstrumentation(String instrumentation) {
		this.instrumentation = instrumentation;
	}

	public String getMelody() {
		return this.melody;
	}

	public void setMelody(String melody) {
		this.melody = melody;
	}

	public Short getTempo() {
		return this.tempo;
	}

	public void setTempo(Short tempo) {
		this.tempo = tempo;
	}

	public String getRemark() {
		return this.remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public AudioPostProduction getAudioPostProduction() {
		return this.audioPostProduction;
	}

	public void setAudioPostProduction(AudioPostProduction audioPostProduction) {
		this.audioPostProduction = audioPostProduction;
	}

	public DynamicMarking getDynamicMarking() {
		return this.dynamicMarking;
	}

	public void setDynamicMarking(DynamicMarking dynamicMarking) {
		this.dynamicMarking = dynamicMarking;
	}

	public MusicType getMusicType() {
		return this.musicType;
	}

	public void setMusicType(MusicType musicType) {
		this.musicType = musicType;
	}

	public MusicalKey getMusicalKey() {
		return this.musicalKey;
	}

	public void setMusicalKey(MusicalKey musicalKey) {
		this.musicalKey = musicalKey;
	}

	public List<ActorHasRole> getActorHasRoles() {
		return this.actorHasRoles;
	}

	public void setActorHasRoles(List<ActorHasRole> actorHasRoles) {
		this.actorHasRoles = actorHasRoles;
	}

	public List<MusicHasActorWithRole> getMusicHasActorWithRoles() {
		return this.musicHasActorWithRoles;
	}

	public void setMusicHasActorWithRoles(List<MusicHasActorWithRole> musicHasActorWithRoles) {
		this.musicHasActorWithRoles = musicHasActorWithRoles;
	}

	public MusicHasActorWithRole addMusicHasActorWithRole(MusicHasActorWithRole musicHasActorWithRole) {
		getMusicHasActorWithRoles().add(musicHasActorWithRole);
		musicHasActorWithRole.setMusic(this);

		return musicHasActorWithRole;
	}

	public MusicHasActorWithRole removeMusicHasActorWithRole(MusicHasActorWithRole musicHasActorWithRole) {
		getMusicHasActorWithRoles().remove(musicHasActorWithRole);
		musicHasActorWithRole.setMusic(null);

		return musicHasActorWithRole;
	}

	public List<Title> getTitles() {
		return this.titleList;
	}

	public void setTitles(List<Title> titles) {
		this.titleList = titles;
	}

	public Title getDisplayTitle() {
		return this.displayTitle;
	}

	public void setDisplayTitle(Title title) {
		this.displayTitle = title;
	}

	public Title getOriginalTitle() {
		return this.originalTitle;
	}

	public void setOriginalTitle(Title title) {
		this.originalTitle = title;
	}

	public List<CategorySet> getRestrictedCategorySets() {
		return this.restrictedCategorySets;
	}

	public void setRestrictedCategorySets(List<CategorySet> categorySets) {
		this.restrictedCategorySets = categorySets;
	}

	public List<Category> getCategories() {
		return this.categories;
	}

	public void setCategories(List<Category> categories) {
		this.categories = categories;
	}

	public List<Tag> getTags() {
		return this.tags;
	}

	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

	public MusicNashid getMusicNashid() {
		return this.musicNashid;
	}

	public void setMusicNashid(MusicNashid musicNashid) {
		this.musicNashid = musicNashid;
	}

	public MusicChurchMusic getMusicChurchMusic() {
		return this.musicChurchMusic;
	}

	public void setMusicChurchMusic(MusicChurchMusic musicChurchMusic) {
		this.musicChurchMusic = musicChurchMusic;
	}

	public TempoMarking getTempoMarking() {
		return this.tempoMarking;
	}

	public void setTempoMarking(TempoMarking tempoMarking) {
		this.tempoMarking = tempoMarking;
	}

	public MusicTextSettingElementType getMusicTextSettingElementType() {
		return this.musicTextSettingElementType;
	}

	public void setTextSetting(MusicTextSettingElementType musicTextSettingElementType) {
		this.musicTextSettingElementType = musicTextSettingElementType;
	}

	public List<VoiceLeadingPattern> getVoiceLeadingPatternList() {
		return this.voiceLeadingPatternList;
	}

	public void setVoiceLeadingPatternList(List<VoiceLeadingPattern> voiceLeadingPatternList) {
		this.voiceLeadingPatternList = voiceLeadingPatternList;
	}

	public List<MusicFormElement> getMusicFormElementList() {
		return this.musicFormElementList;
	}

	public void setMusicFormElementList(List<MusicFormElement> musicFormElementList) {
		this.musicFormElementList = musicFormElementList;
	}

	public MusicFormElement addMusicFormElement(MusicFormElement musicFormElement) {
		getMusicFormElementList().add(musicFormElement);
		musicFormElement.setMusic(this);

		return musicFormElement;
	}

	public MusicFormElement removeMusicFormElement(MusicFormElement musicFormElement) {
		getMusicFormElementList().remove(musicFormElement);

		return musicFormElement;
	}

	public List<MusicChangeInTempoElement> getMusicChangeInTempoElementList() {
		return this.musicChangeInTempoElementList;
	}

	public void setMusicChangeInTempoElementList(List<MusicChangeInTempoElement> musicChangeInTempoElementList) {
		this.musicChangeInTempoElementList = musicChangeInTempoElementList;
	}

	public MusicChangeInTempoElement addMusicChangeInTempoElement(MusicChangeInTempoElement musicChangeInTempoElement) {
		getMusicChangeInTempoElementList().add(musicChangeInTempoElement);
		musicChangeInTempoElement.setMusic(this);

		return musicChangeInTempoElement;
	}

	public MusicChangeInTempoElement removeMusicChangeInTempoElement(MusicChangeInTempoElement musicChangeInTempoElement) {
		getMusicChangeInTempoElementList().remove(musicChangeInTempoElement);
		musicChangeInTempoElement.setMusic(null);

		return musicChangeInTempoElement;
	}

	public List<MusicArticulationElement> getMusicArticulationElementList() {
		return this.musicArticulationElementList;
	}

	public void setMusicArticulationElementList(List<MusicArticulationElement> musicArticulationElementList) {
		this.musicArticulationElementList = musicArticulationElementList;
	}

	public MusicArticulationElement addMusicArticulationElement(MusicArticulationElement musicArticulationElement) {
		getMusicArticulationElementList().add(musicArticulationElement);
		musicArticulationElement.setMusic(this);

		return musicArticulationElement;
	}

	public MusicArticulationElement removeMusicArticulationElement(MusicArticulationElement musicArticulationElement) {
		getMusicArticulationElementList().remove(musicArticulationElement);
		musicArticulationElement.setMusic(null);

		return musicArticulationElement;
	}

	public List<MusicDynamicsElement> getMusicDynamicsElementList() {
		return this.musicDynamicsElementList;
	}

	public void setMusicDynamicsElementList(List<MusicDynamicsElement> musicDynamicsElementList) {
		this.musicDynamicsElementList = musicDynamicsElementList;
	}

	public MusicDynamicsElement addMusicDynamicsElement(MusicDynamicsElement musicDynamicsElement) {
		getMusicDynamicsElementList().add(musicDynamicsElement);
		musicDynamicsElement.setMusic(this);

		return musicDynamicsElement;
	}

	public MusicDynamicsElement removeMusicDynamicsElement(MusicDynamicsElement musicDynamicsElement) {
		getMusicDynamicsElementList().remove(musicDynamicsElement);
		musicDynamicsElement.setMusic(null);

		return musicDynamicsElement;
	}

	public List<MusicTextSettingElement> getMusicTextSettingElementList() {
		return this.musicTextSettingElementList;
	}

	public void setMusicTextSettingElementList(List<MusicTextSettingElement> musicTextSettingElementList) {
		this.musicTextSettingElementList = musicTextSettingElementList;
	}

	public MusicTextSettingElement addMusicTextSettingElement(MusicTextSettingElement musicTextSettingElement) {
		getMusicTextSettingElementList().add(musicTextSettingElement);
		musicTextSettingElement.setMusic(this);

		return musicTextSettingElement;
	}

	public MusicTextSettingElement removeMusicTextSettingElement(MusicTextSettingElement musicTextSettingElement) {
		getMusicTextSettingElementList().remove(musicTextSettingElement);
		musicTextSettingElement.setMusic(null);

		return musicTextSettingElement;
	}

	public List<MediumHasMusic> getMediumHasMusicList() {
		return this.mediumHasMusicList;
	}

	public void setMediumHasMusicList(List<MediumHasMusic> mediumHasMusicList) {
		this.mediumHasMusicList = mediumHasMusicList;
	}

	public MediumHasMusic addMediumHasMusic(MediumHasMusic mediumHasMusic) {
		getMediumHasMusicList().add(mediumHasMusic);
		mediumHasMusic.setMusic(this);

		return mediumHasMusic;
	}

	public MediumHasMusic removeMediumHasMusic(MediumHasMusic mediumHasMusic) {
		getMediumHasMusicList().remove(mediumHasMusic);
		mediumHasMusic.setMusic(null);

		return mediumHasMusic;
	}

  public void setCreatedByUserAccountId(int createdByUserAccountId) {
    this.createdByUserAccountId = createdByUserAccountId;
  }

  public List<AnnotationHasMusic> getAnnotationHasMusic() {
    return annotationHasMusic;
  }

  public void setAnnotationHasMusic(List<AnnotationHasMusic> annotationHasMusic) {
    this.annotationHasMusic = annotationHasMusic;
  }

    public void setMusicTextSettingElementType(MusicTextSettingElementType musicTextSettingElementType) {
        this.musicTextSettingElementType = musicTextSettingElementType;
    }
}
