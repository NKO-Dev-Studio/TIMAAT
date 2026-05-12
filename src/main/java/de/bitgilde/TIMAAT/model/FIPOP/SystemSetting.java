package de.bitgilde.TIMAAT.model.FIPOP;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Entity
@Table(name = "system_settings")
public class SystemSetting {
  @Id
  @Column(name = "id", nullable = false)
  private Short id;

  @NotNull
  @Column(name = "auto_transcribe_uploads", nullable = false)
  private Boolean autoTranscribeUploads;

  @NotNull
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "last_edited_at")
  private Instant lastEditedAt;

  @ManyToOne
  @JoinColumn(name = "last_edited_by_user_account_id")
  private UserAccount lastEditedByUserAccount;

  public Short getId() {
    return id;
  }

  public void setId(Short id) {
    this.id = id;
  }

  public Boolean getAutoTranscribeUploads() {
    return autoTranscribeUploads;
  }

  public void setAutoTranscribeUploads(Boolean autoTranscribeUploads) {
    this.autoTranscribeUploads = autoTranscribeUploads;
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

  public UserAccount getLastEditedByUserAccount() {
    return lastEditedByUserAccount;
  }

  public void setLastEditedByUserAccount(UserAccount lastEditedByUserAccount) {
    this.lastEditedByUserAccount = lastEditedByUserAccount;
  }

}