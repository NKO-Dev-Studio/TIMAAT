package de.bitgilde.TIMAAT.service.transcription;

import de.bitgilde.TIMAAT.model.FIPOP.Medium;
import de.bitgilde.TIMAAT.model.FIPOP.Transcription;
import de.bitgilde.TIMAAT.service.task.TaskService;
import de.bitgilde.TIMAAT.service.transcription.exception.TranscriptionException;
import de.bitgilde.TIMAAT.service.transcription.exception.TranscriptionNotFoundException;
import de.bitgilde.TIMAAT.service.transcription.format.vtt.VttParser;
import de.bitgilde.TIMAAT.service.transcription.format.vtt.VttWriter;
import de.bitgilde.TIMAAT.sse.EntityUpdateEventService;
import de.bitgilde.TIMAAT.storage.entity.SystemSettingStorage;
import de.bitgilde.TIMAAT.storage.entity.medium.MediumStorage;
import de.bitgilde.TIMAAT.storage.entity.transcription.TranscriptionStorage;
import de.bitgilde.TIMAAT.storage.file.AudioFileStorage;
import de.bitgilde.TIMAAT.storage.file.TemporaryFileStorage;
import de.bitgilde.TIMAAT.storage.file.TranscriptionFileStorage;
import de.bitgilde.TIMAAT.storage.file.VideoFileStorage;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.nkodev.stt.client.SpeechToTextServiceClient;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests of {@link TranscriptionService#deleteTranscription(int)} covering the full orchestration:
 * not-found handling, default-transcription reassignment on the referencing medium,
 * DB and file removal ordering, and error wrapping.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-05-21
 */
public class TranscriptionServiceDeleteTest {

  private static final int MEDIUM_ID = 42;
  private static final int TRANSCRIPTION_ID = 7;
  private static final int OTHER_TRANSCRIPTION_ID = 9;

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
  private VttParser vttParser;
  private VttWriter vttWriter;

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
    vttParser = mock(VttParser.class);
    vttWriter = mock(VttWriter.class);


    when(speechToTextServiceClient.getAvailableEngines()).thenReturn(Collections.emptyList());
    when(systemSettingStorage.getDefaultTranscriptionModel()).thenReturn(Optional.empty());
    when(transcriptionStorage.getEntriesAsStream(any(), any(), any(), any())).thenReturn(
            java.util.stream.Stream.empty());

    service = new TranscriptionService(transcriptionStorage, systemSettingStorage, audioFileStorage, videoFileStorage,
            taskServiceProvider, temporaryFileStorage, transcriptionFileStorage, mediumStorage,
            speechToTextServiceClient, entityUpdateEventService, vttParser, vttWriter);
  }

  @Test
  void shouldThrowNotFoundExceptionWhenTranscriptionDoesNotExist() {
    when(transcriptionStorage.findById(TRANSCRIPTION_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.deleteTranscription(TRANSCRIPTION_ID)).isInstanceOf(
            TranscriptionNotFoundException.class).hasMessageContaining(String.valueOf(TRANSCRIPTION_ID));

    verify(transcriptionStorage, never()).deleteTranscription(anyInt());
    verifyNoFileDeletion();
  }

  @Test
  void shouldDeleteTranscriptionAndFileWhenTranscriptionNotReferencedAsDefault() throws Exception {
    Transcription transcription = transcription(TRANSCRIPTION_ID, MEDIUM_ID);
    when(transcriptionStorage.findById(TRANSCRIPTION_ID)).thenReturn(Optional.of(transcription));
    when(transcriptionStorage.findLatestOtherTranscriptionForMedium(MEDIUM_ID, TRANSCRIPTION_ID)).thenReturn(
            Optional.empty());
    when(transcriptionStorage.deleteTranscription(TRANSCRIPTION_ID)).thenReturn(true);

    service.deleteTranscription(TRANSCRIPTION_ID);

    verify(mediumStorage).replaceDefaultTranscription(MEDIUM_ID, TRANSCRIPTION_ID, null);
    verify(transcriptionStorage).deleteTranscription(TRANSCRIPTION_ID);
    verify(transcriptionFileStorage).deleteTranscription(TRANSCRIPTION_ID);
  }

  @Test
  void shouldReassignDefaultTranscriptionToLatestOtherWhenReferenced() throws Exception {
    Transcription transcription = transcription(TRANSCRIPTION_ID, MEDIUM_ID);
    Transcription latestOther = transcription(OTHER_TRANSCRIPTION_ID, MEDIUM_ID);

    when(transcriptionStorage.findById(TRANSCRIPTION_ID)).thenReturn(Optional.of(transcription));
    when(transcriptionStorage.findLatestOtherTranscriptionForMedium(MEDIUM_ID, TRANSCRIPTION_ID)).thenReturn(
            Optional.of(latestOther));
    when(transcriptionStorage.deleteTranscription(TRANSCRIPTION_ID)).thenReturn(true);

    service.deleteTranscription(TRANSCRIPTION_ID);

    verify(mediumStorage).replaceDefaultTranscription(MEDIUM_ID, TRANSCRIPTION_ID, OTHER_TRANSCRIPTION_ID);
    verify(transcriptionStorage).deleteTranscription(TRANSCRIPTION_ID);
    verify(transcriptionFileStorage).deleteTranscription(TRANSCRIPTION_ID);
  }

  @Test
  void shouldClearDefaultTranscriptionWhenNoOtherTranscriptionAvailable() throws Exception {
    Transcription transcription = transcription(TRANSCRIPTION_ID, MEDIUM_ID);

    when(transcriptionStorage.findById(TRANSCRIPTION_ID)).thenReturn(Optional.of(transcription));
    when(transcriptionStorage.findLatestOtherTranscriptionForMedium(MEDIUM_ID, TRANSCRIPTION_ID)).thenReturn(
            Optional.empty());
    when(transcriptionStorage.deleteTranscription(TRANSCRIPTION_ID)).thenReturn(true);

    service.deleteTranscription(TRANSCRIPTION_ID);

    verify(mediumStorage).replaceDefaultTranscription(eq(MEDIUM_ID), eq(TRANSCRIPTION_ID), isNull());
  }

  @Test
  void shouldNotDeleteFileWhenDbDeletionFails() {
    Transcription transcription = transcription(TRANSCRIPTION_ID, MEDIUM_ID);
    when(transcriptionStorage.findById(TRANSCRIPTION_ID)).thenReturn(Optional.of(transcription));
    when(transcriptionStorage.findLatestOtherTranscriptionForMedium(MEDIUM_ID, TRANSCRIPTION_ID)).thenReturn(
            Optional.empty());
    when(transcriptionStorage.deleteTranscription(TRANSCRIPTION_ID)).thenThrow(new RuntimeException("DB down"));

    assertThatThrownBy(() -> service.deleteTranscription(TRANSCRIPTION_ID)).isInstanceOf(TranscriptionException.class)
                                                                           .hasCauseInstanceOf(RuntimeException.class);

    verifyNoFileDeletion();
  }

  @Test
  void shouldWrapFileDeletionFailureInTranscriptionServiceException() throws IOException {
    Transcription transcription = transcription(TRANSCRIPTION_ID, MEDIUM_ID);
    when(transcriptionStorage.findById(TRANSCRIPTION_ID)).thenReturn(Optional.of(transcription));
    when(transcriptionStorage.findLatestOtherTranscriptionForMedium(MEDIUM_ID, TRANSCRIPTION_ID)).thenReturn(
            Optional.empty());
    when(transcriptionStorage.deleteTranscription(TRANSCRIPTION_ID)).thenReturn(true);
    when(transcriptionFileStorage.deleteTranscription(TRANSCRIPTION_ID)).thenThrow(new IOException("disk failure"));

    assertThatThrownBy(() -> service.deleteTranscription(TRANSCRIPTION_ID)).isInstanceOf(TranscriptionException.class)
                                                                           .hasCauseInstanceOf(IOException.class);
  }

  @Test
  void shouldPropagateNotFoundExceptionWithoutWrapping() {
    when(transcriptionStorage.findById(TRANSCRIPTION_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.deleteTranscription(TRANSCRIPTION_ID)).isExactlyInstanceOf(
            TranscriptionNotFoundException.class);
  }

  private Transcription transcription(int id, int mediumId) {
    Transcription transcription = new Transcription();
    transcription.setId(id);

    Medium medium = new Medium();
    medium.setId(mediumId);
    transcription.setMedium(medium);

    return transcription;
  }

  private void verifyNoFileDeletion() {
    try {
      verify(transcriptionFileStorage, never()).deleteTranscription(anyInt());
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }
}
