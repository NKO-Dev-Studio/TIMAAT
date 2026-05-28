package de.bitgilde.TIMAAT.model.FIPOP;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.bitgilde.TIMAAT.rest.endpoint.EndpointMedium;
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
import jakarta.persistence.PostLoad;
import jakarta.persistence.Transient;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
 * The persistent class for the medium database table.
 *
 * @author Jens-Martin Loebel <loebel@bitgilde.de>
 * @author Mirko Scherf <mscherf@uni-mainz.de>
 */
@Entity
@NamedQuery(name = "Medium.findAll", query = "SELECT m FROM Medium m")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Medium implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  private String copyright;

  @Column(name = "created_at")
  private Timestamp createdAt;

  //bi-directional many-to-one association to UserAccount
  @ManyToOne
  @JoinColumn(name = "created_by_user_account_id")
  @JsonBackReference(value = "Medium-CreatedByUserAccount")
  private UserAccount createdByUserAccount;

  @Transient
  @JsonProperty("createdByUserAccountId")
  private int createdByUserAccountId;

  //bi-directional many-to-one association to UserAccount
  @ManyToOne
  @JoinColumn(name = "last_edited_by_user_account_id")
  @JsonBackReference(value = "Medium-LastEditedByUserAccount")
  private UserAccount lastEditedByUserAccount;

  @Transient
  @JsonProperty("lastEditedByUserAccountId")
  private int lastEditedByUserAccountId;

  @Column(name = "file_hash")
  private String fileHash;

  @Column(name = "file_path")
  @JsonIgnore
  private String filePath;

  @Column(name = "last_edited_at")
  private Timestamp lastEditedAt;

  @Column(name = "release_date", columnDefinition = "DATE")
  private Date releaseDate;

  @Column(name = "recording_start_date", columnDefinition = "DATE")
  private Date recordingStartDate;

  @Column(name = "recording_end_date", columnDefinition = "DATE")
  private Date recordingEndDate;

  private String remark;

  //bi-directional many-to-many association to Annotation
  // @ManyToMany(mappedBy = "mediums")
  // @JsonIgnore
  // private List<Annotation> annotations;

  //bi-directional many-to-one association to MediaCollectionHasMedium
  @OneToMany(mappedBy = "medium")
  @JsonIgnore
  private List<MediaCollectionHasMedium> mediaCollectionHasMediums;

  //bi-directional many-to-one association to MediaType
  @ManyToOne
  @JoinColumn(name = "media_type_id")
  private MediaType mediaType;

  //bi-directional many-to-one association to PropagandaType
  @ManyToOne
  @JoinColumn(name = "propaganda_type_id")
  @JsonBackReference(value = "PropagandaType-Medium")
  private PropagandaType propagandaType;

  //bi-directional many-to-one association to Reference
  @ManyToOne
  @JsonBackReference(value = "Reference-Medium")
  private Reference reference;

  //bi-directional many-to-one association to Title
  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "display_title_title_id")
  private Title displayTitle;

  //bi-directional many-to-one association to Title
  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "original_title_title_id")
  private Title originalTitle;

  //bi-directional many-to-one association to Work
  // @ManyToOne
  // @JoinColumn(name="work_id")
  // private Work work;

  //bi-directional many-to-one association to MediumAnalysisList
  @OneToMany(mappedBy = "medium")
  @JsonManagedReference(value = "Medium-MediumAnalysisList")
  private List<MediumAnalysisList> mediumAnalysisLists;

  //bi-directional one-to-one association to MediumAudio
  @OneToOne(mappedBy = "medium")
  private MediumAudio mediumAudio;

  //bi-directional one-to-one association to MediumDocument
  @OneToOne(mappedBy = "medium")
  private MediumDocument mediumDocument;

  //bi-directional one-to-one association to MediumVideo
  @OneToOne(mappedBy = "medium")
  private MediumVideo mediumVideo;

  //bi-directional one-to-one association to MediumVideogame
  @OneToOne(mappedBy = "medium")
  private MediumVideogame mediumVideogame;

  //bi-directional many-to-many association to ActorHasRole
  @ManyToMany
  @JoinTable(name = "medium_has_actor_with_role", joinColumns = {@JoinColumn(name = "medium_id")}, inverseJoinColumns = {@JoinColumn(name = "actor_has_role_actor_id", referencedColumnName = "actor_id"), @JoinColumn(name = "actor_has_role_role_id", referencedColumnName = "role_id")})
  @JsonIgnore
  private List<ActorHasRole> actorHasRoles;

  //bi-directional many-to-many association to CategorySet
  @ManyToMany
  @JoinTable(name = "medium_has_category_set", joinColumns = {@JoinColumn(name = "medium_id")}, inverseJoinColumns = {@JoinColumn(name = "category_set_id")})
  private List<CategorySet> categorySets;

  //bi-directional many-to-many association to Category
  @ManyToMany
  @JoinTable(name = "medium_has_category", joinColumns = {@JoinColumn(name = "medium_id")}, inverseJoinColumns = {@JoinColumn(name = "category_id")})
  private List<Category> categories;

  //bi-directional many-to-many association to Genre
  // @ManyToMany
  // @JoinTable(
  // 	name="medium_has_genre"
  // 	, joinColumns={
  // 		@JoinColumn(name="medium_id")
  // 		}
  // 	, inverseJoinColumns={
  // 		@JoinColumn(name="genre_id")
  // 		}
  // 	)
  // private List<Genre> genres;

  //bi-directional many-to-one association to MediumHasLanguage
  @OneToMany(mappedBy = "medium")
  private List<MediumHasLanguage> mediumHasLanguages;


  //bi-directional many-to-one association to MediumHasActorWithRole
  @OneToMany(mappedBy = "medium")
  private List<MediumHasActorWithRole> mediumHasActorWithRoles;

  //bi-directional many-to-many association to RatingCategory
  // @ManyToMany(mappedBy="mediums")
  // private List<RatingCategory> ratingCategories;

  //bi-directional many-to-many association to Tag
  @ManyToMany
  @JoinTable(name = "medium_has_tag", inverseJoinColumns = {@JoinColumn(name = "tag_id")}, joinColumns = {@JoinColumn(name = "medium_id")})
  private List<Tag> tags;

  //bi-directional many-to-many association to TargetAudience
  // @ManyToMany(mappedBy="mediums")
  // private List<TargetAudience> targetAudiences;

  //bi-directional many-to-many association to Title
  @ManyToMany(mappedBy = "mediums3")
  private List<Title> titles;

  //bi-directional one-to-one association to MediumImage
  @OneToOne(mappedBy = "medium")
  private MediumImage mediumImage;

  //bi-directional many-to-one association to MediumRelatesToEvent
  @OneToMany(mappedBy = "medium")
  @JsonIgnore
  // @JsonManagedReference(value = "Medium-MediumRelatesToEvent")
  private List<MediumRelatesToEvent> mediumRelatesToEvents;

  //bi-directional many-to-one association to MediumRelatesToMedium
  // @OneToMany(mappedBy="medium1")
  // private List<MediumRelatesToMedium> mediumRelatesToMediums1;

  //bi-directional many-to-one association to MediumRelatesToMedium
  // @OneToMany(mappedBy="medium2")
  // private List<MediumRelatesToMedium> mediumRelatesToMediums2;

  //bi-directional one-to-one association to MediumSoftware
  @OneToOne(mappedBy = "medium")
  private MediumSoftware mediumSoftware;

  //bi-directional one-to-one association to MediumText
  @OneToOne(mappedBy = "medium")
  private MediumText mediumText;

  // bi-directional many-to-one association to MusicalNotation
  @OneToMany(mappedBy = "medium")
  @JsonIgnore
  private List<MusicalNotation> musicalNotations;

  //bi-directional many-to-one association to Source
  @OneToMany(mappedBy = "medium")
  @JsonManagedReference(value = "Medium-Source")
  private List<Source> sources;

  // bi-directional one-to-one association to Music
  @OneToOne
  @JoinColumn(name = "music_id")
  // @JsonBackReference(value="Medium-Music")
  private Music music;

  //bi-directional many-to-one association to MediumHasMusic
  @OneToMany(mappedBy = "medium")
  // @JsonIgnore
  @JsonManagedReference(value = "Medium-MediumHasMusic")
  private List<MediumHasMusic> mediumHasMusicList;

  @OneToOne(mappedBy = "medium")
  private MediumAudioAnalysis mediumAudioAnalysis;

  @Transient
  private String fileStatus;

  @Transient
  private String fileExtension;

  @Transient
  private String viewToken;

  @ManyToOne
  @JoinColumn(name = "default_transcription_id")
  @JsonIgnore
  private Transcription defaultTranscription;

  @OneToMany(mappedBy = "medium")
  @JsonIgnore
  private Set<Transcription> transcriptions = new LinkedHashSet<>();


  public Medium() {
  }

  public int getId() {
    return this.id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getCopyright() {
    return this.copyright;
  }

  public void setCopyright(String copyright) {
    this.copyright = copyright;
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

  public String getFileHash() {
    return this.fileHash;
  }

  public void setFileHash(String fileHash) {
    this.fileHash = fileHash;
  }

  public String getFilePath() {
    return this.filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
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
    if (Objects.isNull(this.getLastEditedByUserAccount())) {
      return 0;
    }
    return this.getLastEditedByUserAccount().getId();
  }

  public Date getReleaseDate() {
    return this.releaseDate;
  }

  public void setReleaseDate(Date releaseDate) {
    this.releaseDate = releaseDate;
  }

  public Date getRecordingStartDate() {
    return this.recordingStartDate;
  }

  public void setRecordingStartDate(Date recordingStartDate) {
    this.recordingStartDate = recordingStartDate;
  }

  public Date getRecordingEndDate() {
    return this.recordingEndDate;
  }

  public void setRecordingEndDate(Date recordingEndDate) {
    this.recordingEndDate = recordingEndDate;
  }

  public String getRemark() {
    return this.remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }

  public List<MediaCollectionHasMedium> getMediaCollectionHasMediums() {
    return this.mediaCollectionHasMediums;
  }

  public void setMediaCollectionHasMediums(List<MediaCollectionHasMedium> mediaCollectionHasMediums) {
    this.mediaCollectionHasMediums = mediaCollectionHasMediums;
  }

  public MediaCollectionHasMedium addMediaCollectionHasMedium(MediaCollectionHasMedium mediaCollectionHasMedium) {
    getMediaCollectionHasMediums().add(mediaCollectionHasMedium);
    mediaCollectionHasMedium.setMedium(this);

    return mediaCollectionHasMedium;
  }

  public MediaCollectionHasMedium removeMediaCollectionHasMedium(MediaCollectionHasMedium mediaCollectionHasMedium) {
    getMediaCollectionHasMediums().remove(mediaCollectionHasMedium);
    mediaCollectionHasMedium.setMedium(null);

    return mediaCollectionHasMedium;
  }

  public MediaType getMediaType() {
    return this.mediaType;
  }

  public void setMediaType(MediaType mediaType) {
    this.mediaType = mediaType;
  }

  public PropagandaType getPropagandaType() {
    return this.propagandaType;
  }

  public void setPropagandaType(PropagandaType propagandaType) {
    this.propagandaType = propagandaType;
  }

  public Reference getReference() {
    return this.reference;
  }

  public void setReference(Reference reference) {
    this.reference = reference;
  }

  public Title getDisplayTitle() {
    return this.displayTitle;
  }

  public void setDisplayTitle(Title displayTitle) {
    this.displayTitle = displayTitle;
  }

  public Title getOriginalTitle() {
    return this.originalTitle;
  }

  public void setOriginalTitle(Title title) {
    this.originalTitle = title;
  }

  // 	public Work getWork() {
  // 	return this.work;
  // }

  // public void setWork(Work work) {
  // 	this.work = work;
  // }

  public List<MediumAnalysisList> getMediumAnalysisLists() {
    return this.mediumAnalysisLists;
  }

  public void setMediumAnalysisLists(List<MediumAnalysisList> mediumAnalysisLists) {
    this.mediumAnalysisLists = mediumAnalysisLists;
  }

  public MediumAnalysisList addMediumAnalysisList(MediumAnalysisList mediumAnalysisList) {
    getMediumAnalysisLists().add(mediumAnalysisList);
    mediumAnalysisList.setMedium(this);

    return mediumAnalysisList;
  }

  public MediumAnalysisList removeMediumAnalysisList(MediumAnalysisList mediumAnalysisList) {
    getMediumAnalysisLists().remove(mediumAnalysisList);
    mediumAnalysisList.setMedium(null);

    return mediumAnalysisList;
  }

  public MediumAudio getMediumAudio() {
    return this.mediumAudio;
  }

  public void setMediumAudio(MediumAudio mediumAudio) {
    this.mediumAudio = mediumAudio;
  }

  public MediumDocument getMediumDocument() {
    return this.mediumDocument;
  }

  public void setMediumDocument(MediumDocument mediumDocument) {
    this.mediumDocument = mediumDocument;
  }

  public List<ActorHasRole> getActorHasRoles() {
    return this.actorHasRoles;
  }

  public void setActorHasRoles(List<ActorHasRole> actorHasRoles) {
    this.actorHasRoles = actorHasRoles;
  }

  public List<CategorySet> getCategorySets() {
    return this.categorySets;
  }

  public void setCategorySets(List<CategorySet> categorySets) {
    this.categorySets = categorySets;
  }

  public List<Category> getCategories() {
    return this.categories;
  }

  public void setCategories(List<Category> categories) {
    this.categories = categories;
  }

  // public List<Genre> getGenres() {
  // 	return this.genres;
  // }

  // public void setGenres(List<Genre> genres) {
  // 	this.genres = genres;
  // }

  public List<MediumHasLanguage> getMediumHasLanguages() {
    return this.mediumHasLanguages;
  }

  public void setMediumHasLanguages(List<MediumHasLanguage> mediumHasLanguages) {
    this.mediumHasLanguages = mediumHasLanguages;
  }

  public MediumHasLanguage addMediumHasLanguage(MediumHasLanguage mediumHasLanguage) {
    getMediumHasLanguages().add(mediumHasLanguage);
    mediumHasLanguage.setMedium(this);

    return mediumHasLanguage;
  }

  public MediumHasLanguage removeMediumHasLanguage(MediumHasLanguage mediumHasLanguage) {
    getMediumHasLanguages().remove(mediumHasLanguage);
    mediumHasLanguage.setMedium(null);

    return mediumHasLanguage;
  }

  // public List<RatingCategory> getRatingCategories() {
  // 	return this.ratingCategories;
  // }

  // public void setRatingCategories(List<RatingCategory> ratingCategories) {
  // 	this.ratingCategories = ratingCategories;
  // }

  public List<Tag> getTags() {
    return this.tags;
  }

  public void setTags(List<Tag> tags) {
    this.tags = tags;
  }

  // public List<TargetAudience> getTargetAudiences() {
  // 	return this.targetAudiences;
  // }

  // public void setTargetAudiences(List<TargetAudience> targetAudiences) {
  // 	this.targetAudiences = targetAudiences;
  // }

  public List<Title> getTitles() {
    return this.titles;
  }

  public void setTitles(List<Title> titles) {
    this.titles = titles;
  }

  public MediumImage getMediumImage() {
    return this.mediumImage;
  }

  public void setMediumImage(MediumImage mediumImage) {
    this.mediumImage = mediumImage;
  }

  public List<MediumRelatesToEvent> getMediumRelatesToEvents() {
    return this.mediumRelatesToEvents;
  }

  public void setMediumRelatesToEvents(List<MediumRelatesToEvent> mediumRelatesToEvents) {
    this.mediumRelatesToEvents = mediumRelatesToEvents;
  }

  public MediumRelatesToEvent addMediumRelatesToEvent(MediumRelatesToEvent mediumRelatesToEvent) {
    getMediumRelatesToEvents().add(mediumRelatesToEvent);
    mediumRelatesToEvent.setMedium(this);

    return mediumRelatesToEvent;
  }

  public MediumRelatesToEvent removeMediumRelatesToEvent(MediumRelatesToEvent mediumRelatesToEvent) {
    getMediumRelatesToEvents().remove(mediumRelatesToEvent);
    mediumRelatesToEvent.setMedium(null);

    return mediumRelatesToEvent;
  }

  // public List<MediumRelatesToMedium> getMediumRelatesToMediums1() {
  // 	return this.mediumRelatesToMediums1;
  // }

  // public void setMediumRelatesToMediums1(List<MediumRelatesToMedium> mediumRelatesToMediums1) {
  // 	this.mediumRelatesToMediums1 = mediumRelatesToMediums1;
  // }

  // public MediumRelatesToMedium addMediumRelatesToMediums1(MediumRelatesToMedium mediumRelatesToMediums1) {
  // 	getMediumRelatesToMediums1().add(mediumRelatesToMediums1);
  // 	mediumRelatesToMediums1.setMedium1(this);

  // 	return mediumRelatesToMediums1;
  // }

  // public MediumRelatesToMedium removeMediumRelatesToMediums1(MediumRelatesToMedium mediumRelatesToMediums1) {
  // 	getMediumRelatesToMediums1().remove(mediumRelatesToMediums1);
  // 	mediumRelatesToMediums1.setMedium1(null);

  // 	return mediumRelatesToMediums1;
  // }

  // public List<MediumRelatesToMedium> getMediumRelatesToMediums2() {
  // 	return this.mediumRelatesToMediums2;
  // }

  // public void setMediumRelatesToMediums2(List<MediumRelatesToMedium> mediumRelatesToMediums2) {
  // 	this.mediumRelatesToMediums2 = mediumRelatesToMediums2;
  // }

  // public MediumRelatesToMedium addMediumRelatesToMediums2(MediumRelatesToMedium mediumRelatesToMediums2) {
  // 	getMediumRelatesToMediums2().add(mediumRelatesToMediums2);
  // 	mediumRelatesToMediums2.setMedium2(this);

  // 	return mediumRelatesToMediums2;
  // }

  // public MediumRelatesToMedium removeMediumRelatesToMediums2(MediumRelatesToMedium mediumRelatesToMediums2) {
  // 	getMediumRelatesToMediums2().remove(mediumRelatesToMediums2);
  // 	mediumRelatesToMediums2.setMedium2(null);

  // 	return mediumRelatesToMediums2;
  // }

  public MediumSoftware getMediumSoftware() {
    return this.mediumSoftware;
  }

  public void setMediumSoftware(MediumSoftware mediumSoftware) {
    this.mediumSoftware = mediumSoftware;
  }

  public MediumText getMediumText() {
    return this.mediumText;
  }

  public void setMediumText(MediumText mediumText) {
    this.mediumText = mediumText;
  }

  public List<MusicalNotation> getMusicalNotations() {
    return this.musicalNotations;
  }

  public void setMusicalNotations(List<MusicalNotation> musicalNotations) {
    this.musicalNotations = musicalNotations;
  }

  public MusicalNotation addMusicalNotation(MusicalNotation musicalNotation) {
    getMusicalNotations().add(musicalNotation);
    musicalNotation.setMedium(this);

    return musicalNotation;
  }

  public MusicalNotation removeMusicalNotation(MusicalNotation musicalNotation) {
    getMusicalNotations().remove(musicalNotation);
    musicalNotation.setMedium(null);

    return musicalNotation;
  }

  public List<Source> getSources() {
    return this.sources;
  }

  public void setSources(List<Source> sources) {
    this.sources = sources;
  }

  public Source addSource(Source source) {
    getSources().add(source);
    source.setMedium(this);

    return source;
  }

  public Source removeSource(Source source) {
    getSources().remove(source);
    source.setMedium(null);

    return source;
  }

  public MediumVideo getMediumVideo() {
    return this.mediumVideo;
  }

  public void setMediumVideo(MediumVideo mediumVideo) {
    this.mediumVideo = mediumVideo;
  }

  public MediumVideogame getMediumVideogame() {
    return mediumVideogame;
  }

  public void setMediumVideogame(MediumVideogame mediumVideogame) {
    this.mediumVideogame = mediumVideogame;
  }

  public List<MediumHasActorWithRole> getMediumHasActorWithRoles() {
    return this.mediumHasActorWithRoles;
  }

  public void setMediumHasActorWithRoles(List<MediumHasActorWithRole> mediumHasActorWithRoles) {
    this.mediumHasActorWithRoles = mediumHasActorWithRoles;
  }

  public MediumHasActorWithRole addMediumHasActorWithRole(MediumHasActorWithRole mediumHasActorWithRole) {
    getMediumHasActorWithRoles().add(mediumHasActorWithRole);
    mediumHasActorWithRole.setMedium(this);

    return mediumHasActorWithRole;
  }

  public MediumHasActorWithRole removeMediumHasActorWithRole(MediumHasActorWithRole mediumHasActorWithRole) {
    getMediumHasActorWithRoles().remove(mediumHasActorWithRole);
    mediumHasActorWithRole.setMedium(null);

    return mediumHasActorWithRole;
  }

  public Music getMusic() {
    return this.music;
  }

  public void setMusic(Music music) {
    this.music = music;
  }

  public List<MediumHasMusic> getMediumHasMusicList() {
    return this.mediumHasMusicList;
  }

  public void setMediumHasMusicList(List<MediumHasMusic> mediumHasMusicList) {
    this.mediumHasMusicList = mediumHasMusicList;
  }

  public MediumHasMusic addMediumHasMusic(MediumHasMusic mediumHasMusic) {
    getMediumHasMusicList().add(mediumHasMusic);
    mediumHasMusic.setMedium(this);

    return mediumHasMusic;
  }

  public MediumHasMusic removeMediumHasMusic(MediumHasMusic mediumHasMusic) {
    getMediumHasMusicList().remove(mediumHasMusic);
    mediumHasMusic.setMedium(null);

    return mediumHasMusic;
  }

  public String getFileStatus() {
    return fileStatus;
  }

  public void setFileStatus(String fileStatus) {
    this.fileStatus = fileStatus;
  }

  public String getFileExtension() {
    return fileExtension;
  }

  public void setFileExtension(String fileExtension) {
    this.fileExtension = fileExtension;
  }

  public String getViewToken() {
    return viewToken;
  }

  public void setViewToken(String viewToken) {
    this.viewToken = viewToken;
  }

  public Transcription getDefaultTranscription() {
    return defaultTranscription;
  }

  public void setDefaultTranscription(Transcription defaultTranscription) {
    this.defaultTranscription = defaultTranscription;
  }

  public Set<Transcription> getTranscriptions() {
    return transcriptions;
  }

  public void setTranscriptions(Set<Transcription> transcriptions) {
    this.transcriptions = transcriptions;
  }

  public MediumAudioAnalysis getMediumAudioAnalysis() {
    return mediumAudioAnalysis;
  }

  public void setMediumAudioAnalysis(MediumAudioAnalysis mediumAudioAnalysis) {
    this.mediumAudioAnalysis = mediumAudioAnalysis;
  }

  public void setLastEditedByUserAccountId(int lastEditedByUserAccountId) {
    this.lastEditedByUserAccountId = lastEditedByUserAccountId;
  }

  public void setCreatedByUserAccountId(int createdByUserAccountId) {
    this.createdByUserAccountId = createdByUserAccountId;
  }

  public Integer getDefaultTranscriptionId() {
    return defaultTranscription != null ? defaultTranscription.getId() : null;
  }

  @PostLoad
  public void setFileStatusAndViewTokenAfterLoad() {
    if (this.fileStatus == null) {
      this.fileStatus = EndpointMedium.mediumFileStatus(this.id,
              this.getMediaType().getMediaTypeTranslations().get(0).getType());
    }
    if (this.viewToken == null) {
      this.viewToken = EndpointMedium.issueFileToken(this.id);
    }
  }

}