package de.bitgilde.TIMAAT.service.transcription;

import de.bitgilde.TIMAAT.model.FIPOP.Medium;
import de.bitgilde.TIMAAT.model.FIPOP.Transcription;
import de.bitgilde.TIMAAT.service.task.TaskService;
import de.bitgilde.TIMAAT.service.transcription.exception.TranscriptionNotFoundException;
import de.bitgilde.TIMAAT.sse.EntityUpdateEventService;
import de.bitgilde.TIMAAT.storage.entity.SystemSettingStorage;
import de.bitgilde.TIMAAT.storage.entity.medium.MediumStorage;
import de.bitgilde.TIMAAT.storage.entity.medium.exception.MediumNotFoundException;
import de.bitgilde.TIMAAT.storage.entity.transcription.TranscriptionStorage;
import de.bitgilde.TIMAAT.storage.entity.transcription.api.TranscriptionFilterCriteria;
import de.bitgilde.TIMAAT.storage.file.AudioFileStorage;
import de.bitgilde.TIMAAT.storage.file.TemporaryFileStorage;
import de.bitgilde.TIMAAT.storage.file.TranscriptionFileStorage;
import de.bitgilde.TIMAAT.storage.file.VideoFileStorage;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import studio.nkodev.stt.client.SpeechToTextServiceClient;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests of {@link TranscriptionService#getTranscriptionsForMedium(int)},
 * {@link TranscriptionService#getTranscription(int, int)} and
 * {@link TranscriptionService#existsForMedium(int, int)}, covering the medium-scoped
 * lookup behaviour and the medium/transcription mismatch handling.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-05-24
 */
public class TranscriptionServiceMediumLookupTest {

  private static final int MEDIUM_ID = 42;
  private static final int OTHER_MEDIUM_ID = 99;
  private static final int TRANSCRIPTION_ID = 7;

  private TranscriptionStorage transcriptionStorage;
  private SystemSettingStorage systemSettingStorage;
  private AudioFileStorage audioFileStorage;
  private VideoFileStorage videoFileStorage;
  private TaskService taskService;
  private Provider<TaskService> taskServiceProvider;
  private TemporaryFileStorage temporaryFileStorage;
  private TranscriptionFileStorage transcriptionFileStorage;
  private MediumStorage mediumStorage;
  private SpeechToTextServiceClient speechToTextServiceClient;
  private EntityUpdateEventService entityUpdateEventService;

  private TranscriptionService service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    transcriptionStorage = mock(TranscriptionStorage.class);
    systemSettingStorage = mock(SystemSettingStorage.class);
    audioFileStorage = mock(AudioFileStorage.class);
    videoFileStorage = mock(VideoFileStorage.class);
    taskService = mock(TaskService.class);
    taskServiceProvider = (Provider<TaskService>) mock(Provider.class);
    when(taskServiceProvider.get()).thenReturn(taskService);
    temporaryFileStorage = mock(TemporaryFileStorage.class);
    transcriptionFileStorage = mock(TranscriptionFileStorage.class);
    mediumStorage = mock(MediumStorage.class);
    speechToTextServiceClient = mock(SpeechToTextServiceClient.class);
    entityUpdateEventService = mock(EntityUpdateEventService.class);

    when(speechToTextServiceClient.getAvailableEngines()).thenReturn(Collections.emptyList());
    when(systemSettingStorage.getDefaultTranscriptionModel()).thenReturn(Optional.empty());
    when(transcriptionStorage.getEntriesAsStream(any(), any(), any(), any())).thenReturn(Stream.empty());

    service = new TranscriptionService(transcriptionStorage, systemSettingStorage, audioFileStorage, videoFileStorage,
            taskServiceProvider, temporaryFileStorage, transcriptionFileStorage, mediumStorage,
            speechToTextServiceClient, entityUpdateEventService);

    // The constructor triggers resumeMonitoringOfActiveTranscriptions which calls
    // getEntriesAsStream once; clear that so per-test verify counts start from zero.
    clearInvocations(transcriptionStorage);
  }

  @Test
  void shouldReturnTranscriptionsFilteredByMedium() throws Exception {
    when(mediumStorage.existsById(MEDIUM_ID)).thenReturn(true);
    Transcription transcription = transcription(TRANSCRIPTION_ID, MEDIUM_ID);
    when(transcriptionStorage.getEntriesAsStream(any(TranscriptionFilterCriteria.class), any(), any(),
            any())).thenReturn(Stream.of(transcription));

    Collection<Transcription> result = service.getTranscriptionsForMedium(MEDIUM_ID);

    assertThat(result).containsExactly(transcription);

    ArgumentCaptor<TranscriptionFilterCriteria> captor = ArgumentCaptor.forClass(TranscriptionFilterCriteria.class);
    verify(transcriptionStorage).getEntriesAsStream(captor.capture(), any(), any(), any());
    assertThat(captor.getValue().getMediumId()).contains(MEDIUM_ID);
  }

  @Test
  void shouldReturnEmptyCollectionWhenMediumHasNoTranscriptions() throws Exception {
    when(mediumStorage.existsById(MEDIUM_ID)).thenReturn(true);
    when(transcriptionStorage.getEntriesAsStream(any(TranscriptionFilterCriteria.class), any(), any(),
            any())).thenReturn(Stream.empty());

    Collection<Transcription> result = service.getTranscriptionsForMedium(MEDIUM_ID);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldThrowMediumNotFoundExceptionWhenListingTranscriptionsForMissingMedium() {
    when(mediumStorage.existsById(MEDIUM_ID)).thenReturn(false);

    assertThatThrownBy(() -> service.getTranscriptionsForMedium(MEDIUM_ID)).isInstanceOf(MediumNotFoundException.class);

    verify(transcriptionStorage, never()).getEntriesAsStream(any(TranscriptionFilterCriteria.class), any(), any(),
            any());
  }

  @Test
  void shouldReturnTranscriptionWhenIdAndMediumMatch() throws Exception {
    Transcription transcription = transcription(TRANSCRIPTION_ID, MEDIUM_ID);
    when(transcriptionStorage.findById(TRANSCRIPTION_ID)).thenReturn(Optional.of(transcription));

    Transcription result = service.getTranscription(MEDIUM_ID, TRANSCRIPTION_ID);

    assertThat(result).isSameAs(transcription);
  }

  @Test
  void shouldThrowNotFoundExceptionWhenTranscriptionDoesNotExist() {
    when(transcriptionStorage.findById(TRANSCRIPTION_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getTranscription(MEDIUM_ID, TRANSCRIPTION_ID)).isInstanceOf(
            TranscriptionNotFoundException.class);
  }

  @Test
  void shouldThrowNotFoundExceptionWhenTranscriptionBelongsToDifferentMedium() {
    Transcription transcription = transcription(TRANSCRIPTION_ID, OTHER_MEDIUM_ID);
    when(transcriptionStorage.findById(TRANSCRIPTION_ID)).thenReturn(Optional.of(transcription));

    assertThatThrownBy(() -> service.getTranscription(MEDIUM_ID, TRANSCRIPTION_ID)).isInstanceOf(
            TranscriptionNotFoundException.class);
  }

  @Test
  void shouldReturnTrueWhenTranscriptionExistsForMedium() {
    when(transcriptionStorage.existsForMedium(MEDIUM_ID, TRANSCRIPTION_ID)).thenReturn(true);

    boolean result = service.existsForMedium(MEDIUM_ID, TRANSCRIPTION_ID);

    assertThat(result).isTrue();
  }

  @Test
  void shouldReturnFalseWhenTranscriptionDoesNotExistForMedium() {
    when(transcriptionStorage.existsForMedium(MEDIUM_ID, TRANSCRIPTION_ID)).thenReturn(false);

    boolean result = service.existsForMedium(MEDIUM_ID, TRANSCRIPTION_ID);

    assertThat(result).isFalse();
  }

  private Transcription transcription(int id, int mediumId) {
    Transcription transcription = new Transcription();
    transcription.setId(id);

    Medium medium = new Medium();
    medium.setId(mediumId);
    transcription.setMedium(medium);

    return transcription;
  }
}
