package de.bitgilde.TIMAAT.service.transcription;

import de.bitgilde.TIMAAT.service.task.TaskService;
import de.bitgilde.TIMAAT.service.transcription.format.vtt.VttParser;
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

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link TranscriptionService} releases its gRPC-backed
 * {@link SpeechToTextServiceClient} when the service is destroyed. The client
 * owns non-daemon Netty event-loop threads, so failing to close it on shutdown
 * keeps the JVM alive and prevents the web application from stopping cleanly.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 29.05.26
 */
class TranscriptionServiceShutdownTest {

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
    return new TranscriptionService(transcriptionStorage, systemSettingStorage, audioFileStorage, videoFileStorage,
            taskServiceProvider, temporaryFileStorage, transcriptionFileStorage, mediumStorage,
            speechToTextServiceClient, entityUpdateEventService, vttParser);
  }

  private TranscriptionService featureDisabledService() {
    return new TranscriptionService(transcriptionStorage, systemSettingStorage, audioFileStorage, videoFileStorage,
            taskServiceProvider, temporaryFileStorage, transcriptionFileStorage, mediumStorage,
            (SpeechToTextServiceClient) null, entityUpdateEventService, vttParser);
  }

  @Test
  void closeShutsDownSpeechToTextClientWhenFeatureEnabled() {
    TranscriptionService service = featureEnabledService();

    service.close();

    verify(speechToTextServiceClient).close();
  }

  @Test
  void closeDoesNothingWhenFeatureDisabled() {
    TranscriptionService service = featureDisabledService();

    assertThatNoException().isThrownBy(service::close);
  }

  @Test
  void closeSwallowsExceptionsRaisedByClient() {
    TranscriptionService service = featureEnabledService();
    doThrow(new RuntimeException("shutdown failed")).when(speechToTextServiceClient).close();

    assertThatNoException().isThrownBy(service::close);
  }
}
