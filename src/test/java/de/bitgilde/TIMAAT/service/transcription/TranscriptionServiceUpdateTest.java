package de.bitgilde.TIMAAT.service.transcription;

import de.bitgilde.TIMAAT.model.FIPOP.Medium;
import de.bitgilde.TIMAAT.model.FIPOP.Transcription;
import de.bitgilde.TIMAAT.model.FIPOP.TranscriptionState;
import de.bitgilde.TIMAAT.model.FIPOP.TranscriptionType;
import de.bitgilde.TIMAAT.rest.model.transcription.TranscriptionDto;
import de.bitgilde.TIMAAT.service.task.TaskService;
import de.bitgilde.TIMAAT.service.transcription.api.TranscriptionContent;
import de.bitgilde.TIMAAT.service.transcription.api.TranscriptionContentEntityUpdateMessage;
import de.bitgilde.TIMAAT.service.transcription.api.TranscriptionCue;
import de.bitgilde.TIMAAT.service.transcription.exception.TranscriptionNotFoundException;
import de.bitgilde.TIMAAT.service.transcription.exception.TranscriptionServiceException;
import de.bitgilde.TIMAAT.service.transcription.format.vtt.VttParser;
import de.bitgilde.TIMAAT.service.transcription.format.vtt.VttWriter;
import de.bitgilde.TIMAAT.sse.EntityUpdateEventService;
import de.bitgilde.TIMAAT.sse.api.EntityType;
import de.bitgilde.TIMAAT.storage.entity.SystemSettingStorage;
import de.bitgilde.TIMAAT.storage.entity.medium.MediumStorage;
import de.bitgilde.TIMAAT.storage.entity.transcription.TranscriptionStorage;
import de.bitgilde.TIMAAT.storage.file.AudioFileStorage;
import de.bitgilde.TIMAAT.storage.file.TemporaryFileStorage;
import de.bitgilde.TIMAAT.storage.file.TemporaryFileStorage.TemporaryFile;
import de.bitgilde.TIMAAT.storage.file.TranscriptionFileStorage;
import de.bitgilde.TIMAAT.storage.file.VideoFileStorage;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import studio.nkodev.stt.client.SpeechToTextServiceClient;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests of {@link TranscriptionService#updateTranscriptionName(int, int, String, int)} and
 * {@link TranscriptionService#updateTranscriptionContent(int, int, TranscriptionContent, int)},
 * covering the success paths, the medium-scoped not-found handling and the
 * write-to-temporary-file-before-overwrite behaviour for content updates.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-06-11
 */
public class TranscriptionServiceUpdateTest {

  private static final int MEDIUM_ID = 42;
  private static final int OTHER_MEDIUM_ID = 99;
  private static final int TRANSCRIPTION_ID = 7;
  private static final int USER_ID = 3;

  private TranscriptionStorage transcriptionStorage;
  private TemporaryFileStorage temporaryFileStorage;
  private TranscriptionFileStorage transcriptionFileStorage;
  private EntityUpdateEventService entityUpdateEventService;
  private VttWriter vttWriter;

  private TranscriptionService service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    transcriptionStorage = mock(TranscriptionStorage.class);
    SystemSettingStorage systemSettingStorage = mock(SystemSettingStorage.class);
    AudioFileStorage audioFileStorage = mock(AudioFileStorage.class);
    VideoFileStorage videoFileStorage = mock(VideoFileStorage.class);
    TaskService taskService = mock(TaskService.class);
    Provider<TaskService> taskServiceProvider = (Provider<TaskService>) mock(Provider.class);
    when(taskServiceProvider.get()).thenReturn(taskService);
    temporaryFileStorage = mock(TemporaryFileStorage.class);
    transcriptionFileStorage = mock(TranscriptionFileStorage.class);
    MediumStorage mediumStorage = mock(MediumStorage.class);
    SpeechToTextServiceClient speechToTextServiceClient = mock(SpeechToTextServiceClient.class);
    entityUpdateEventService = mock(EntityUpdateEventService.class);
    VttParser vttParser = mock(VttParser.class);
    vttWriter = mock(VttWriter.class);

    when(speechToTextServiceClient.getAvailableEngines()).thenReturn(Collections.emptyList());
    when(systemSettingStorage.getDefaultTranscriptionModel()).thenReturn(Optional.empty());
    when(transcriptionStorage.getEntriesAsStream(any(), any(), any(), any())).thenReturn(Stream.empty());

    service = new TranscriptionService(transcriptionStorage, systemSettingStorage, audioFileStorage, videoFileStorage,
            taskServiceProvider, temporaryFileStorage, transcriptionFileStorage, mediumStorage,
            speechToTextServiceClient, entityUpdateEventService, vttParser, vttWriter);

    clearInvocations(transcriptionStorage);
  }

  @Test
  void shouldUpdateNameAndSendChangeMessage() throws Exception {
    when(transcriptionStorage.existsForMedium(MEDIUM_ID, TRANSCRIPTION_ID)).thenReturn(true);
    Transcription updated = transcription(TRANSCRIPTION_ID, MEDIUM_ID);
    updated.setName("Neuer Name");
    when(transcriptionStorage.updateTranscriptionMetadata(TRANSCRIPTION_ID, "Neuer Name", USER_ID)).thenReturn(updated);

    TranscriptionDto result = service.updateTranscriptionName(MEDIUM_ID, TRANSCRIPTION_ID, "Neuer Name", USER_ID);

    assertThat(result.name()).isEqualTo("Neuer Name");
    verify(transcriptionStorage).updateTranscriptionMetadata(TRANSCRIPTION_ID, "Neuer Name", USER_ID);
    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(entityUpdateEventService).sendEntityChangeMessage(eq(EntityType.TRANSCRIPTION), eq(TRANSCRIPTION_ID),
            captor.capture());
    assertThat(captor.getValue()).isInstanceOf(TranscriptionDto.class);
  }

  @Test
  void shouldThrowNotFoundWhenUpdatingNameOfMissingTranscription() {
    when(transcriptionStorage.existsForMedium(MEDIUM_ID, TRANSCRIPTION_ID)).thenReturn(false);

    assertThatThrownBy(
            () -> service.updateTranscriptionName(MEDIUM_ID, TRANSCRIPTION_ID, "Neuer Name", USER_ID)).isInstanceOf(
            TranscriptionNotFoundException.class);

    verify(transcriptionStorage, never()).updateTranscriptionMetadata(anyInt(), any(), anyInt());
    verify(entityUpdateEventService, never()).sendEntityChangeMessage(any(), any(), any());
  }

  @Test
  void shouldWriteContentToTemporaryFileThenPersistAndSendChangeMessage(@TempDir Path tempDir) throws Exception {
    when(transcriptionStorage.existsForMedium(MEDIUM_ID, TRANSCRIPTION_ID)).thenReturn(true);


    Path temporaryFilePath = tempDir.resolve("transcription-temp.vtt");
    when(temporaryFileStorage.createTemporaryFile()).thenReturn(new TemporaryFile(temporaryFilePath));
    TranscriptionContent content = content();

    service.updateTranscriptionContent(MEDIUM_ID, TRANSCRIPTION_ID, content, USER_ID);

    verify(vttWriter).writeVttStream(eq(content), any(OutputStream.class));
    verify(transcriptionFileStorage).persistTranscription(temporaryFilePath, TRANSCRIPTION_ID);
    verify(transcriptionStorage).touchTranscription(TRANSCRIPTION_ID, USER_ID);
    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(entityUpdateEventService).sendEntityChangeMessage(eq(EntityType.TRANSCRIPTION), eq(TRANSCRIPTION_ID),
            captor.capture());
    assertThat(captor.getValue()).isEqualTo(new TranscriptionContentEntityUpdateMessage(true));
  }

  @Test
  void shouldThrowNotFoundWhenUpdatingContentOfMissingTranscription() throws Exception {
    when(transcriptionStorage.existsForMedium(MEDIUM_ID, TRANSCRIPTION_ID)).thenReturn(false);

    assertThatThrownBy(
            () -> service.updateTranscriptionContent(MEDIUM_ID, TRANSCRIPTION_ID, content(), USER_ID)).isInstanceOf(
            TranscriptionNotFoundException.class);

    verify(temporaryFileStorage, never()).createTemporaryFile();
    verify(transcriptionFileStorage, never()).persistTranscription(any(), anyInt());
    verify(transcriptionStorage, never()).touchTranscription(anyInt(), anyInt());
    verify(entityUpdateEventService, never()).sendEntityChangeMessage(any(), any(), any());
  }

  @Test
  void shouldNotPersistOrNotifyWhenWritingContentFails(@TempDir Path tempDir) throws Exception {
    when(transcriptionStorage.existsForMedium(MEDIUM_ID, TRANSCRIPTION_ID)).thenReturn(true);

    Path temporaryFilePath = tempDir.resolve("transcription-temp.vtt");
    when(temporaryFileStorage.createTemporaryFile()).thenReturn(new TemporaryFile(temporaryFilePath));
    doThrow(new IOException("write failure")).when(vttWriter).writeVttStream(any(), any());

    assertThatThrownBy(
            () -> service.updateTranscriptionContent(MEDIUM_ID, TRANSCRIPTION_ID, content(), USER_ID)).isInstanceOf(
            TranscriptionServiceException.class).hasCauseInstanceOf(IOException.class);

    verify(transcriptionFileStorage, never()).persistTranscription(any(), anyInt());
    verify(transcriptionStorage, never()).touchTranscription(anyInt(), anyInt());
    verify(entityUpdateEventService, never()).sendEntityChangeMessage(any(), any(), any());
  }

  private TranscriptionContent content() {
    return new TranscriptionContent(
            List.of(new TranscriptionCue(Duration.ofSeconds(1), Duration.ofSeconds(2), "Hallo")));
  }

  private Transcription transcription(int id, int mediumId) {
    Transcription transcription = new Transcription();
    transcription.setId(id);
    transcription.setName("Original");

    Medium medium = new Medium();
    medium.setId(mediumId);
    transcription.setMedium(medium);

    TranscriptionState state = new TranscriptionState();
    state.setId(de.bitgilde.TIMAAT.storage.entity.transcription.api.TranscriptionState.COMPLETED.getDatabaseId());
    transcription.setTranscriptionState(state);

    TranscriptionType type = new TranscriptionType();
    type.setId(de.bitgilde.TIMAAT.storage.entity.transcription.api.TranscriptionType.GENERATED.getDatabaseId());
    transcription.setTranscriptionType(type);

    transcription.setCreatedAt(Instant.EPOCH);

    return transcription;
  }
}
