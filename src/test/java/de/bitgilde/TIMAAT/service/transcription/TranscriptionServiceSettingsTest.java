package de.bitgilde.TIMAAT.service.transcription;

import de.bitgilde.TIMAAT.model.FIPOP.TranscriptionModel;
import de.bitgilde.TIMAAT.model.FIPOP.TranscriptionModelId;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount;
import de.bitgilde.TIMAAT.service.task.TaskService;
import de.bitgilde.TIMAAT.service.transcription.api.TranscriptionEngine;
import de.bitgilde.TIMAAT.service.transcription.api.TranscriptionEngineModel;
import de.bitgilde.TIMAAT.service.transcription.exception.TranscriptionFeatureDisabledException;
import de.bitgilde.TIMAAT.service.transcription.format.vtt.VttParser;
import de.bitgilde.TIMAAT.sse.EntityUpdateEventService;
import de.bitgilde.TIMAAT.storage.entity.SystemSettingStorage;
import de.bitgilde.TIMAAT.storage.entity.api.TranscriptionSystemSettings;
import de.bitgilde.TIMAAT.storage.entity.medium.MediumStorage;
import de.bitgilde.TIMAAT.storage.entity.transcription.TranscriptionStorage;
import de.bitgilde.TIMAAT.storage.file.AudioFileStorage;
import de.bitgilde.TIMAAT.storage.file.TemporaryFileStorage;
import de.bitgilde.TIMAAT.storage.file.TranscriptionFileStorage;
import de.bitgilde.TIMAAT.storage.file.VideoFileStorage;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import studio.nkodev.stt.client.SpeechToTextServiceClient;
import studio.nkodev.stt.client.api.SpeechToTextEngine;
import studio.nkodev.stt.client.api.SpeechToTextEngineOutputFormat;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests of the system-settings related behavior of {@link TranscriptionService} together with the
 * feature-gating introduced by the {@code stt.enabled} property. Feature-disabled state is modeled
 * by passing a {@code null} {@link SpeechToTextServiceClient} into the package-private constructor
 * — the same wiring the production constructor performs when {@code stt.enabled=false}.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-05-24
 */
public class TranscriptionServiceSettingsTest {

  private static final String ENGINE_ID = "whisper";
  private static final String ENGINE_NAME = "Whisper";
  private static final String MODEL_ID = "large-v3";

  private TranscriptionStorage transcriptionStorage;
  private SystemSettingStorage systemSettingStorage;
  private AudioFileStorage audioFileStorage;
  private VideoFileStorage videoFileStorage;
  private Provider<TaskService> taskServiceProvider;
  private TemporaryFileStorage temporaryFileStorage;
  private TranscriptionFileStorage transcriptionFileStorage;
  private MediumStorage mediumStorage;
  private SpeechToTextServiceClient speechToTextServiceClient;
  private EntityUpdateEventService entityUpdateEventService;
  private VttParser vttParser;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    transcriptionStorage = mock(TranscriptionStorage.class);
    systemSettingStorage = mock(SystemSettingStorage.class);
    audioFileStorage = mock(AudioFileStorage.class);
    videoFileStorage = mock(VideoFileStorage.class);
    taskServiceProvider = (Provider<TaskService>) mock(Provider.class);
    temporaryFileStorage = mock(TemporaryFileStorage.class);
    transcriptionFileStorage = mock(TranscriptionFileStorage.class);
    mediumStorage = mock(MediumStorage.class);
    speechToTextServiceClient = mock(SpeechToTextServiceClient.class);
    entityUpdateEventService = mock(EntityUpdateEventService.class);
    vttParser = mock(VttParser.class);

    when(systemSettingStorage.getDefaultTranscriptionModel()).thenReturn(Optional.empty());
    when(transcriptionStorage.getEntriesAsStream(any(), any(), any(), any())).thenReturn(Stream.empty());
  }

  private TranscriptionService featureEnabledService() {
    when(speechToTextServiceClient.getAvailableEngines()).thenReturn(
            List.of(new SpeechToTextEngine(ENGINE_ID, ENGINE_NAME, List.of(MODEL_ID),
                    List.of(SpeechToTextEngineOutputFormat.SRT))));
    TranscriptionService service = new TranscriptionService(transcriptionStorage, systemSettingStorage,
            audioFileStorage, videoFileStorage, taskServiceProvider, temporaryFileStorage, transcriptionFileStorage,
            mediumStorage, speechToTextServiceClient, entityUpdateEventService, vttParser);
    clearInvocations(transcriptionStorage, systemSettingStorage);
    return service;
  }

  private TranscriptionService featureDisabledService() {
    return new TranscriptionService(transcriptionStorage, systemSettingStorage, audioFileStorage, videoFileStorage,
            taskServiceProvider, temporaryFileStorage, transcriptionFileStorage, mediumStorage,
            (SpeechToTextServiceClient) null, entityUpdateEventService, vttParser);
  }

  @Nested
  class FeatureGating {

    @Test
    void shouldReportFeatureEnabledWhenSpeechToTextClientIsConfigured() {
      TranscriptionService service = featureEnabledService();

      assertThat(service.isFeatureEnabled()).isTrue();
    }

    @Test
    void shouldReportFeatureDisabledWhenNoSpeechToTextClientIsConfigured() {
      TranscriptionService service = featureDisabledService();

      assertThat(service.isFeatureEnabled()).isFalse();
    }

    @Test
    void shouldReturnEmptyEngineListWhenFeatureDisabled() {
      TranscriptionService service = featureDisabledService();

      Collection<TranscriptionEngine> engines = service.getAvailableEngineCapabilities();

      assertThat(engines).isEmpty();
      verifyNoInteractions(speechToTextServiceClient);
    }

    @Test
    void shouldNotResumeMonitoringOrVerifyDefaultModelWhenFeatureDisabled() {
      featureDisabledService();

      verify(transcriptionStorage, never()).getEntriesAsStream(any(), any(), any(), any());
      verify(systemSettingStorage, never()).getDefaultTranscriptionModel();
    }

    @Test
    void shouldReturnLiveEngineCapabilitiesWhenFeatureEnabled() {
      TranscriptionService service = featureEnabledService();

      Collection<TranscriptionEngine> engines = service.getAvailableEngineCapabilities();

      assertThat(engines).hasSize(1);
      TranscriptionEngine capability = engines.iterator().next();
      assertThat(capability.engineIdentifier()).isEqualTo(ENGINE_ID);
      assertThat(capability.engineName()).isEqualTo(ENGINE_NAME);
      assertThat(capability.models()).containsExactly(new TranscriptionEngineModel(MODEL_ID, false));
    }

    @Test
    void shouldMarkConfiguredDefaultModelInEngineCapabilities() {
      when(systemSettingStorage.getDefaultTranscriptionModel()).thenReturn(
              Optional.of(buildTranscriptionModel(ENGINE_ID, MODEL_ID)));
      TranscriptionService service = featureEnabledService();

      Collection<TranscriptionEngine> engines = service.getAvailableEngineCapabilities();

      assertThat(engines).singleElement().satisfies(capability -> {
        assertThat(capability.engineIdentifier()).isEqualTo(ENGINE_ID);
        assertThat(capability.models()).containsExactly(new TranscriptionEngineModel(MODEL_ID, true));
      });
    }

    @Test
    void shouldMarkOnlyTheMatchingModelAsDefaultAcrossMultipleEnginesAndModels() {
      when(speechToTextServiceClient.getAvailableEngines()).thenReturn(
              List.of(new SpeechToTextEngine(ENGINE_ID, ENGINE_NAME, List.of(MODEL_ID, "small-v3"),
                              List.of(SpeechToTextEngineOutputFormat.SRT)),
                      new SpeechToTextEngine("vosk", "Vosk", List.of("de", "en"),
                              List.of(SpeechToTextEngineOutputFormat.SRT))));
      when(systemSettingStorage.getDefaultTranscriptionModel()).thenReturn(
              Optional.of(buildTranscriptionModel("vosk", "de")));
      TranscriptionService service = new TranscriptionService(transcriptionStorage, systemSettingStorage,
              audioFileStorage, videoFileStorage, taskServiceProvider, temporaryFileStorage, transcriptionFileStorage,
              mediumStorage, speechToTextServiceClient, entityUpdateEventService, vttParser);
      clearInvocations(transcriptionStorage, systemSettingStorage);

      Collection<TranscriptionEngine> engines = service.getAvailableEngineCapabilities();

      assertThat(engines).hasSize(2);
      assertThat(engines).filteredOn(capability -> capability.engineIdentifier().equals(ENGINE_ID)).singleElement()
                         .satisfies(capability -> assertThat(capability.models()).containsExactly(
                                 new TranscriptionEngineModel(MODEL_ID, false),
                                 new TranscriptionEngineModel("small-v3", false)));
      assertThat(engines).filteredOn(capability -> capability.engineIdentifier().equals("vosk")).singleElement()
                         .satisfies(capability -> assertThat(capability.models()).containsExactly(
                                 new TranscriptionEngineModel("de", true), new TranscriptionEngineModel("en", false)));
    }

    @Test
    void shouldNotMarkAnyModelAsDefaultWhenDefaultIsConfiguredOnDifferentEngine() {
      when(systemSettingStorage.getDefaultTranscriptionModel()).thenReturn(
              Optional.of(buildTranscriptionModel("some-other-engine", MODEL_ID)));
      TranscriptionService service = featureEnabledService();

      Collection<TranscriptionEngine> engines = service.getAvailableEngineCapabilities();

      assertThat(engines).singleElement().satisfies(capability -> assertThat(capability.models()).containsExactly(
              new TranscriptionEngineModel(MODEL_ID, false)));
    }
  }

  private static TranscriptionModel buildTranscriptionModel(String engineIdentifier, String modelIdentifier) {
    TranscriptionModelId id = new TranscriptionModelId();
    id.setEngineIdentifier(engineIdentifier);
    id.setModelIdentifier(modelIdentifier);
    TranscriptionModel model = new TranscriptionModel();
    model.setId(id);
    return model;
  }

  @Nested
  class UpdateTranscriptionSystemSettings {

    @Test
    void shouldRejectWhenOnlyEngineIdentifierIsSet() {
      TranscriptionService service = featureEnabledService();

      assertThatThrownBy(() -> service.updateTranscriptionSystemSettings(true, ENGINE_ID, null, null)).isInstanceOf(
              IllegalArgumentException.class).hasMessageContaining("both be set or both be null");

      verify(systemSettingStorage, never()).updateTranscriptionSystemSettings(anyBoolean(), any(), any(), any());
    }

    @Test
    void shouldRejectWhenOnlyModelIdentifierIsSet() {
      TranscriptionService service = featureEnabledService();

      assertThatThrownBy(() -> service.updateTranscriptionSystemSettings(false, null, MODEL_ID, null)).isInstanceOf(
              IllegalArgumentException.class);

      verify(systemSettingStorage, never()).updateTranscriptionSystemSettings(anyBoolean(), any(), any(), any());
    }

    @Test
    void shouldRejectSettingDefaultModelWhenFeatureDisabled() {
      TranscriptionService service = featureDisabledService();

      assertThatThrownBy(
              () -> service.updateTranscriptionSystemSettings(false, ENGINE_ID, MODEL_ID, null)).isInstanceOf(
              TranscriptionFeatureDisabledException.class);

      verifyNoInteractions(systemSettingStorage);
    }

    @Test
    void shouldAllowClearingDefaultEvenWhenFeatureDisabled() throws TranscriptionFeatureDisabledException {
      TranscriptionService service = featureDisabledService();

      service.updateTranscriptionSystemSettings(true, null, null, null);

      verify(systemSettingStorage).updateTranscriptionSystemSettings(true, null, null, null);
    }

    @Test
    void shouldRejectUnknownEngineWhenSettingDefault() {
      TranscriptionService service = featureEnabledService();

      assertThatThrownBy(
              () -> service.updateTranscriptionSystemSettings(true, "unknown-engine", MODEL_ID, null)).isInstanceOf(
              IllegalArgumentException.class).hasMessageContaining("unknown-engine");

      verify(systemSettingStorage, never()).updateTranscriptionSystemSettings(anyBoolean(), any(), any(), any());
    }

    @Test
    void shouldRejectUnknownModelOnKnownEngineWhenSettingDefault() {
      TranscriptionService service = featureEnabledService();

      assertThatThrownBy(
              () -> service.updateTranscriptionSystemSettings(true, ENGINE_ID, "unknown-model", null)).isInstanceOf(
              IllegalArgumentException.class).hasMessageContaining("unknown-model");

      verify(systemSettingStorage, never()).updateTranscriptionSystemSettings(anyBoolean(), any(), any(), any());
    }

    @Test
    void shouldUpsertEngineAndModelAndPersistWhenSettingDefault() throws TranscriptionFeatureDisabledException {
      TranscriptionService service = featureEnabledService();
      UserAccount editingUser = new UserAccount();

      service.updateTranscriptionSystemSettings(true, ENGINE_ID, MODEL_ID, editingUser);

      verify(transcriptionStorage).upsertEngine(ENGINE_ID, ENGINE_NAME);
      verify(transcriptionStorage).upsertModel(ENGINE_ID, MODEL_ID);
      verify(systemSettingStorage).updateTranscriptionSystemSettings(true, ENGINE_ID, MODEL_ID, editingUser);
    }

    @Test
    void shouldNotTouchTranscriptionStorageWhenJustTogglingAutoTranscribe() throws TranscriptionFeatureDisabledException {
      TranscriptionService service = featureEnabledService();

      service.updateTranscriptionSystemSettings(true, null, null, null);

      verify(transcriptionStorage, never()).upsertEngine(any(), any());
      verify(transcriptionStorage, never()).upsertModel(any(), any());
      verify(systemSettingStorage).updateTranscriptionSystemSettings(true, null, null, null);
    }
  }

  @Nested
  class GetTranscriptionSystemSettings {

    @Test
    void shouldDelegateToSystemSettingStorage() {
      TranscriptionService service = featureEnabledService();
      TranscriptionSystemSettings expected = new TranscriptionSystemSettings(true, null);
      when(systemSettingStorage.getTranscriptionSystemSettings()).thenReturn(expected);

      TranscriptionSystemSettings actual = service.getTranscriptionSystemSettings();

      assertThat(actual).isSameAs(expected);
    }
  }
}
