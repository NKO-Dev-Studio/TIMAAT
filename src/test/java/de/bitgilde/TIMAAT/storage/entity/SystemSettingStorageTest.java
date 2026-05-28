package de.bitgilde.TIMAAT.storage.entity;

import de.bitgilde.TIMAAT.db.exception.DbTransactionExecutionException;
import de.bitgilde.TIMAAT.model.FIPOP.SystemSetting;
import de.bitgilde.TIMAAT.model.FIPOP.TranscriptionModel;
import de.bitgilde.TIMAAT.model.FIPOP.TranscriptionModelId;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount;
import de.bitgilde.TIMAAT.storage.entity.api.TranscriptionSystemSettings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SystemSettingStorage} covering the singleton-row upsert semantics,
 * the validation of the default-model reference, and the snapshot mapping returned by
 * {@link SystemSettingStorage#getTranscriptionSystemSettings()}. The JPA layer is fully mocked
 * — no in-memory database is started — so the tests verify orchestration only (which {@code find},
 * {@code persist} and field-set calls happen, and how exceptions are wrapped by
 * {@link de.bitgilde.TIMAAT.db.DbAccessComponent#executeDbTransaction}).
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-05-24
 */
public class SystemSettingStorageTest {

  private static final short SINGLETON_ID = 1;
  private static final String ENGINE_ID = "whisper";
  private static final String MODEL_ID = "large-v3";

  private EntityManagerFactory entityManagerFactory;
  private EntityManager entityManager;
  private EntityTransaction entityTransaction;

  private SystemSettingStorage storage;

  @BeforeEach
  void setUp() {
    entityManagerFactory = mock(EntityManagerFactory.class);
    entityManager = mock(EntityManager.class);
    entityTransaction = mock(EntityTransaction.class);

    when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
    when(entityManager.getTransaction()).thenReturn(entityTransaction);

    storage = new SystemSettingStorage(entityManagerFactory);
  }

  @Nested
  class GetTranscriptionSystemSettings {

    @Test
    void shouldReturnDefaultSnapshotWhenNoSettingsRowExists() {
      when(entityManager.find(SystemSetting.class, SINGLETON_ID)).thenReturn(null);

      TranscriptionSystemSettings settings = storage.getTranscriptionSystemSettings();

      assertThat(settings.autoTranscribeUploads()).isFalse();
      assertThat(settings.defaultTranscriptionModel()).isNull();
    }

    @Test
    void shouldReflectExistingRowWithAutoTranscribeAndNoDefaultModel() {
      SystemSetting row = new SystemSetting();
      row.setId(SINGLETON_ID);
      row.setAutoTranscribeUploads(true);
      row.setDefaultTranscriptionModel(null);
      when(entityManager.find(SystemSetting.class, SINGLETON_ID)).thenReturn(row);

      TranscriptionSystemSettings settings = storage.getTranscriptionSystemSettings();

      assertThat(settings.autoTranscribeUploads()).isTrue();
      assertThat(settings.defaultTranscriptionModel()).isNull();
    }

    @Test
    void shouldReflectExistingRowWithConfiguredDefaultModel() {
      TranscriptionModel model = transcriptionModel(ENGINE_ID, MODEL_ID);
      SystemSetting row = new SystemSetting();
      row.setId(SINGLETON_ID);
      row.setAutoTranscribeUploads(false);
      row.setDefaultTranscriptionModel(model);
      when(entityManager.find(SystemSetting.class, SINGLETON_ID)).thenReturn(row);

      TranscriptionSystemSettings settings = storage.getTranscriptionSystemSettings();

      assertThat(settings.autoTranscribeUploads()).isFalse();
      assertThat(settings.defaultTranscriptionModel()).isSameAs(model);
    }
  }

  @Nested
  class UpdateTranscriptionSystemSettings {

    @Test
    void shouldCreateSingletonRowWhenNoneExists() {
      when(entityManager.find(SystemSetting.class, SINGLETON_ID)).thenReturn(null);

      Instant before = Instant.now();
      storage.updateTranscriptionSystemSettings(true, null, null, null);
      Instant after = Instant.now();

      ArgumentCaptor<SystemSetting> persisted = ArgumentCaptor.forClass(SystemSetting.class);
      verify(entityManager).persist(persisted.capture());
      SystemSetting row = persisted.getValue();
      assertThat(row.getId()).isEqualTo(SINGLETON_ID);
      assertThat(row.getAutoTranscribeUploads()).isTrue();
      assertThat(row.getDefaultTranscriptionModel()).isNull();
      assertThat(row.getCreatedAt()).isBetween(before, after);
      assertThat(row.getLastEditedAt()).isBetween(before, after);
      verify(entityTransaction).commit();
    }

    @Test
    void shouldMutateExistingSingletonRowAndNotPersist() {
      SystemSetting existingRow = existingRow(false, null);
      when(entityManager.find(SystemSetting.class, SINGLETON_ID)).thenReturn(existingRow);

      Instant before = Instant.now();
      storage.updateTranscriptionSystemSettings(true, null, null, null);
      Instant after = Instant.now();

      assertThat(existingRow.getAutoTranscribeUploads()).isTrue();
      assertThat(existingRow.getDefaultTranscriptionModel()).isNull();
      assertThat(existingRow.getLastEditedAt()).isBetween(before, after);
      verify(entityManager, never()).persist(any());
      verify(entityTransaction).commit();
    }

    @Test
    void shouldClearDefaultModelWhenBothIdentifiersAreNull() {
      SystemSetting existingRow = existingRow(true, transcriptionModel(ENGINE_ID, MODEL_ID));
      when(entityManager.find(SystemSetting.class, SINGLETON_ID)).thenReturn(existingRow);

      storage.updateTranscriptionSystemSettings(true, null, null, null);

      assertThat(existingRow.getDefaultTranscriptionModel()).isNull();
      verify(entityManager, never()).find(eq(TranscriptionModel.class), any());
    }

    @Test
    void shouldAttachKnownTranscriptionModelAsDefault() {
      SystemSetting existingRow = existingRow(false, null);
      TranscriptionModel knownModel = transcriptionModel(ENGINE_ID, MODEL_ID);
      when(entityManager.find(SystemSetting.class, SINGLETON_ID)).thenReturn(existingRow);
      when(entityManager.find(eq(TranscriptionModel.class), any(TranscriptionModelId.class))).thenReturn(knownModel);

      storage.updateTranscriptionSystemSettings(true, ENGINE_ID, MODEL_ID, null);

      assertThat(existingRow.getDefaultTranscriptionModel()).isSameAs(knownModel);
      ArgumentCaptor<TranscriptionModelId> lookupKey = ArgumentCaptor.forClass(TranscriptionModelId.class);
      verify(entityManager).find(eq(TranscriptionModel.class), lookupKey.capture());
      assertThat(lookupKey.getValue().getEngineIdentifier()).isEqualTo(ENGINE_ID);
      assertThat(lookupKey.getValue().getModelIdentifier()).isEqualTo(MODEL_ID);
    }

    @Test
    void shouldRollbackAndWrapIllegalArgumentWhenModelIsUnknown() {
      SystemSetting existingRow = existingRow(false, null);
      when(entityManager.find(SystemSetting.class, SINGLETON_ID)).thenReturn(existingRow);
      when(entityManager.find(eq(TranscriptionModel.class), any(TranscriptionModelId.class))).thenReturn(null);

      assertThatThrownBy(() -> storage.updateTranscriptionSystemSettings(true, ENGINE_ID, MODEL_ID, null)).isInstanceOf(
              DbTransactionExecutionException.class).hasCauseInstanceOf(IllegalArgumentException.class);

      verify(entityTransaction).rollback();
      verify(entityTransaction, never()).commit();
    }

    @Test
    void shouldStampLastEditedByUserAccountWhenUserProvided() {
      SystemSetting existingRow = existingRow(false, null);
      when(entityManager.find(SystemSetting.class, SINGLETON_ID)).thenReturn(existingRow);
      UserAccount editingUser = new UserAccount();

      storage.updateTranscriptionSystemSettings(false, null, null, editingUser);

      assertThat(existingRow.getLastEditedByUserAccount()).isSameAs(editingUser);
    }

    @Test
    void shouldLeaveLastEditedByUserAccountAloneWhenNoUserProvided() {
      UserAccount previousUser = new UserAccount();
      SystemSetting existingRow = existingRow(false, null);
      existingRow.setLastEditedByUserAccount(previousUser);
      when(entityManager.find(SystemSetting.class, SINGLETON_ID)).thenReturn(existingRow);

      storage.updateTranscriptionSystemSettings(false, null, null, null);

      assertThat(existingRow.getLastEditedByUserAccount()).isSameAs(previousUser);
    }
  }

  @Nested
  class GetDefaultTranscriptionModel {

    @Test
    void shouldReturnEmptyWhenNoRowExists() {
      when(entityManager.find(SystemSetting.class, SINGLETON_ID)).thenReturn(null);

      Optional<TranscriptionModel> result = storage.getDefaultTranscriptionModel();

      assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenRowHasNoDefault() {
      SystemSetting row = existingRow(false, null);
      when(entityManager.find(SystemSetting.class, SINGLETON_ID)).thenReturn(row);

      Optional<TranscriptionModel> result = storage.getDefaultTranscriptionModel();

      assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnConfiguredDefaultModel() {
      TranscriptionModel model = transcriptionModel(ENGINE_ID, MODEL_ID);
      SystemSetting row = existingRow(false, model);
      when(entityManager.find(SystemSetting.class, SINGLETON_ID)).thenReturn(row);

      Optional<TranscriptionModel> result = storage.getDefaultTranscriptionModel();

      assertThat(result).contains(model);
    }
  }

  private SystemSetting existingRow(boolean autoTranscribe, TranscriptionModel defaultModel) {
    SystemSetting row = new SystemSetting();
    row.setId(SINGLETON_ID);
    row.setAutoTranscribeUploads(autoTranscribe);
    row.setDefaultTranscriptionModel(defaultModel);
    row.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
    return row;
  }

  private TranscriptionModel transcriptionModel(String engineIdentifier, String modelIdentifier) {
    TranscriptionModelId id = new TranscriptionModelId();
    id.setEngineIdentifier(engineIdentifier);
    id.setModelIdentifier(modelIdentifier);
    TranscriptionModel model = new TranscriptionModel();
    model.setId(id);
    return model;
  }
}
