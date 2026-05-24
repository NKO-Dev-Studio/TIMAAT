package de.bitgilde.TIMAAT.storage.entity;

import de.bitgilde.TIMAAT.db.DbAccessComponent;
import de.bitgilde.TIMAAT.model.FIPOP.SystemSetting;
import de.bitgilde.TIMAAT.model.FIPOP.TranscriptionModel;
import de.bitgilde.TIMAAT.model.FIPOP.TranscriptionModelId;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount;
import de.bitgilde.TIMAAT.storage.entity.api.TranscriptionSystemSettings;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.time.Instant;
import java.util.Optional;

/**
 * Storage providing access to the singleton {@link SystemSetting} row.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 16.05.26
 */
public class SystemSettingStorage extends DbAccessComponent {

  private static final short SINGLETON_ID = 1;

  @Inject
  public SystemSettingStorage(EntityManagerFactory emf) {
    super(emf);
  }

  /**
   * @return the {@link TranscriptionModel} currently configured as the default model for new transcriptions,
   * or {@link Optional#empty()} if no default has been configured (or no settings row exists yet)
   */
  public Optional<TranscriptionModel> getDefaultTranscriptionModel() {
    return executeDbTransaction(entityManager -> {
      SystemSetting systemSetting = entityManager.find(SystemSetting.class, SINGLETON_ID);
      if (systemSetting == null) {
        return Optional.<TranscriptionModel>empty();
      }
      return Optional.ofNullable(systemSetting.getDefaultTranscriptionModel());
    });
  }

  /**
   * Reads the transcription-related system settings (default engine/model and the auto-transcribe-uploads
   * flag) from the singleton settings row. If no row exists yet, a snapshot reflecting the documented
   * defaults (auto-transcribe disabled, no default model) is returned.
   *
   * @return a snapshot of the current transcription system settings, never {@code null}
   */
  public TranscriptionSystemSettings getTranscriptionSystemSettings() {
    return executeDbTransaction(entityManager -> {
      SystemSetting systemSetting = entityManager.find(SystemSetting.class, SINGLETON_ID);
      if (systemSetting == null) {
        return new TranscriptionSystemSettings(false, null);
      }
      return new TranscriptionSystemSettings(Boolean.TRUE.equals(systemSetting.getAutoTranscribeUploads()),
              systemSetting.getDefaultTranscriptionModel());
    });
  }

  /**
   * Updates the transcription-related system settings in the singleton settings row. If no row exists
   * yet, one is created. The {@code lastEditedAt} timestamp is set to now and {@code lastEditedByUserAccount}
   * is set to the given user (when non-{@code null}).
   *
   * @param autoTranscribeUploads whether newly uploaded media should be transcribed automatically
   * @param defaultEngineIdentifier identifier of the engine to use as default, or {@code null} to clear the default
   * @param defaultModelIdentifier identifier of the model to use as default, or {@code null} to clear the default
   * @param editingUser the user performing the change; used to stamp {@code lastEditedByUserAccount}, may be {@code null}
   * @throws de.bitgilde.TIMAAT.db.exception.DbTransactionExecutionException if the update fails at the database level
   */
  public void updateTranscriptionSystemSettings(boolean autoTranscribeUploads, @Nullable String defaultEngineIdentifier, @Nullable String defaultModelIdentifier, @Nullable UserAccount editingUser) {
    executeDbTransaction(entityManager -> {
      SystemSetting systemSetting = entityManager.find(SystemSetting.class, SINGLETON_ID);
      Instant now = Instant.now();

      if (systemSetting == null) {
        systemSetting = createSystemSetting(entityManager, now);
      }

      systemSetting.setAutoTranscribeUploads(autoTranscribeUploads);
      updateTranscriptionDefaultModel(entityManager, systemSetting, defaultEngineIdentifier, defaultModelIdentifier);
      systemSetting.setLastEditedAt(now);
      if (editingUser != null) {
        systemSetting.setLastEditedByUserAccount(editingUser);
      }
      return Void.TYPE;
    });
  }

  public void updateTranscriptionDefaultModel(@Nullable String defaultEngineIdentifier, @Nullable String defaultModelIdentifier, @Nullable UserAccount editingUser) {
    executeDbTransaction(entityManager -> {
      SystemSetting systemSetting = entityManager.find(SystemSetting.class, SINGLETON_ID);
      Instant now = Instant.now();

      if (systemSetting == null) {
        systemSetting = createSystemSetting(entityManager, now);
      }

      updateTranscriptionDefaultModel(entityManager, systemSetting, defaultEngineIdentifier, defaultModelIdentifier);
      systemSetting.setLastEditedAt(now);
      if (editingUser != null) {
        systemSetting.setLastEditedByUserAccount(editingUser);
      }

      return Void.TYPE;
    });
  }

  private void updateTranscriptionDefaultModel(EntityManager entityManager, SystemSetting systemSetting, @Nullable String defaultEngineIdentifier, @Nullable String defaultModelIdentifier) {
    if (defaultEngineIdentifier == null || defaultModelIdentifier == null) {
      systemSetting.setDefaultTranscriptionModel(null);
    }
    else {
      TranscriptionModelId modelId = new TranscriptionModelId();
      modelId.setEngineIdentifier(defaultEngineIdentifier);
      modelId.setModelIdentifier(defaultModelIdentifier);
      TranscriptionModel model = entityManager.find(TranscriptionModel.class, modelId);
      if (model == null) {
        throw new IllegalArgumentException(
                "Transcription model '" + defaultEngineIdentifier + "/" + defaultModelIdentifier + "' is unknown");
      }
      systemSetting.setDefaultTranscriptionModel(model);
    }
  }

  private SystemSetting createSystemSetting(EntityManager entityManager, Instant createdAt) {
    SystemSetting systemSetting = new SystemSetting();
    systemSetting.setId(SINGLETON_ID);
    systemSetting.setCreatedAt(createdAt);

    entityManager.persist(systemSetting);
    return systemSetting;
  }
}
