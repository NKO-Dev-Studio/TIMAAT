package de.bitgilde.TIMAAT.model.FIPOP;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(name = "transcription")
public class Transcription {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Integer id;

  @Size(max = 150)
  @NotNull
  @Column(name = "name", nullable = false, length = 150)
  private String name;

  @ManyToOne
  @JoinColumn(name = "model_identifier", referencedColumnName = "model_identifier")
  @JoinColumn(name = "engine_identifier", referencedColumnName = "engine_identifier")
  private TranscriptionModel transcriptionModel;

  @NotNull
  @ManyToOne(optional = false)
  @JoinColumn(name = "medium_id", nullable = false)
  private Medium medium;

  @NotNull
  @ManyToOne(optional = false)
  @JoinColumn(name = "transcription_state_id", nullable = false)
  private TranscriptionState transcriptionState;

  @Column(name = "transcription_task_id")
  private Long transcriptionTaskId;

  @NotNull
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "last_edited_at")
  private Instant lastEditedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_user_account_id")
  private UserAccount createdByUserAccount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "last_edited_by_user_account_id")
  private UserAccount lastEditedByUserAccount;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public TranscriptionModel getTranscriptionModel() {
    return transcriptionModel;
  }

  public void setTranscriptionModel(TranscriptionModel modelIdentifier) {
    this.transcriptionModel = modelIdentifier;
  }

  public Medium getMedium() {
    return medium;
  }

  public void setMedium(Medium medium) {
    this.medium = medium;
  }

  public TranscriptionState getTranscriptionState() {
    return transcriptionState;
  }

  public void setTranscriptionState(TranscriptionState transcriptionState) {
    this.transcriptionState = transcriptionState;
  }

  public Long getTranscriptionTaskId() {
    return transcriptionTaskId;
  }

  public void setTranscriptionTaskId(Long transcriptionTaskId) {
    this.transcriptionTaskId = transcriptionTaskId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getLastEditedAt() {
    return lastEditedAt;
  }

  public void setLastEditedAt(Instant lastEditedAt) {
    this.lastEditedAt = lastEditedAt;
  }

  public UserAccount getCreatedByUserAccount() {
    return createdByUserAccount;
  }

  public void setCreatedByUserAccount(UserAccount createdByUserAccount) {
    this.createdByUserAccount = createdByUserAccount;
  }

  public UserAccount getLastEditedByUserAccount() {
    return lastEditedByUserAccount;
  }

  public void setLastEditedByUserAccount(UserAccount lastEditedByUserAccount) {
    this.lastEditedByUserAccount = lastEditedByUserAccount;
  }

}