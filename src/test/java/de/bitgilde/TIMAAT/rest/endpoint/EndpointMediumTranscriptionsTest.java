package de.bitgilde.TIMAAT.rest.endpoint;

import de.bitgilde.TIMAAT.model.FIPOP.Medium;
import de.bitgilde.TIMAAT.model.FIPOP.Transcription;
import de.bitgilde.TIMAAT.model.FIPOP.TranscriptionModel;
import de.bitgilde.TIMAAT.model.FIPOP.TranscriptionModelId;
import de.bitgilde.TIMAAT.rest.filter.AuthenticationFilter;
import de.bitgilde.TIMAAT.rest.model.transcription.CreateTranscriptionRequest;
import de.bitgilde.TIMAAT.rest.model.transcription.TranscriptionDto;
import de.bitgilde.TIMAAT.service.transcription.TranscriptionService;
import de.bitgilde.TIMAAT.service.transcription.api.GenerateTranscriptionConfiguration;
import de.bitgilde.TIMAAT.service.transcription.exception.TranscriptionFeatureDisabledException;
import de.bitgilde.TIMAAT.service.transcription.exception.TranscriptionNotFoundException;
import de.bitgilde.TIMAAT.service.transcription.exception.TranscriptionServiceException;
import de.bitgilde.TIMAAT.storage.entity.medium.exception.MediumNotFoundException;
import de.bitgilde.TIMAAT.storage.entity.transcription.api.TranscriptionState;
import de.bitgilde.TIMAAT.storage.entity.transcription.api.TranscriptionType;
import de.bitgilde.TIMAAT.storage.file.TranscriptionFileStorage;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the transcription-related sub-resources exposed by {@link EndpointMedium}
 * ({@code GET /medium/{id}/transcriptions}, {@code GET /medium/{id}/transcriptions/{tid}},
 * {@code POST /medium/{id}/transcriptions}, {@code DELETE /medium/{id}/transcriptions/{tid}}).
 * The endpoint is wired with mocked collaborators (no Jersey involved); injected fields are
 * populated via reflection.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-05-24
 */
public class EndpointMediumTranscriptionsTest {

  private static final int MEDIUM_ID = 42;
  private static final int TRANSCRIPTION_ID = 7;
  private static final int USER_ID = 5;
  private static final String ENGINE = "whisper";
  private static final String MODEL = "large-v3";

  private TranscriptionService transcriptionService;
  private TranscriptionFileStorage transcriptionFileStorage;
  private ContainerRequestContext containerRequestContext;
  private EndpointMedium endpoint;

  @BeforeEach
  void setUp() throws Exception {
    transcriptionService = mock(TranscriptionService.class);
    transcriptionFileStorage = mock(TranscriptionFileStorage.class);
    containerRequestContext = mock(ContainerRequestContext.class);
    when(containerRequestContext.getProperty(AuthenticationFilter.USER_ID_PROPERTY_NAME)).thenReturn(USER_ID);
    endpoint = new EndpointMedium();
    inject("transcriptionService", transcriptionService);
    inject("transcriptionFileStorage", transcriptionFileStorage);
    inject("containerRequestContext", containerRequestContext);
  }

  private void inject(String fieldName, Object value) throws Exception {
    Field field = EndpointMedium.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(endpoint, value);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldReturnTranscriptionsOfMediumAsDtoCollection() throws Exception {
    Transcription transcription = transcription(TRANSCRIPTION_ID, MEDIUM_ID, ENGINE, MODEL);
    when(transcriptionService.getTranscriptionsForMedium(MEDIUM_ID)).thenReturn(List.of(transcription));

    Response response = endpoint.getMediumTranscriptions(MEDIUM_ID);

    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    Iterable<TranscriptionDto> result = (Iterable<TranscriptionDto>) response.getEntity();
    assertThat(result).hasSize(1);
    TranscriptionDto dto = result.iterator().next();
    assertThat(dto.id()).isEqualTo(TRANSCRIPTION_ID);
    assertThat(dto.mediumId()).isEqualTo(MEDIUM_ID);
    assertThat(dto.engineIdentifier()).isEqualTo(ENGINE);
    assertThat(dto.modelIdentifier()).isEqualTo(MODEL);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldReturnEmptyCollectionWhenMediumHasNoTranscriptions() throws Exception {
    when(transcriptionService.getTranscriptionsForMedium(MEDIUM_ID)).thenReturn(List.of());

    Response response = endpoint.getMediumTranscriptions(MEDIUM_ID);

    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    assertThat((Iterable<TranscriptionDto>) response.getEntity()).isEmpty();
  }

  @Test
  void shouldReturnNotFoundWhenMediumDoesNotExistForList() throws Exception {
    when(transcriptionService.getTranscriptionsForMedium(MEDIUM_ID)).thenThrow(new MediumNotFoundException(MEDIUM_ID));

    Response response = endpoint.getMediumTranscriptions(MEDIUM_ID);

    assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  @Test
  void shouldReturnSingleTranscriptionDtoWhenFound() throws Exception {
    Transcription transcription = transcription(TRANSCRIPTION_ID, MEDIUM_ID, ENGINE, MODEL);
    when(transcriptionService.getTranscription(MEDIUM_ID, TRANSCRIPTION_ID)).thenReturn(transcription);

    Response response = endpoint.getMediumTranscription(MEDIUM_ID, TRANSCRIPTION_ID);

    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    assertThat(response.getEntity()).isInstanceOf(TranscriptionDto.class);
    TranscriptionDto dto = (TranscriptionDto) response.getEntity();
    assertThat(dto.id()).isEqualTo(TRANSCRIPTION_ID);
    assertThat(dto.mediumId()).isEqualTo(MEDIUM_ID);
  }

  @Test
  void shouldReturnNotFoundWhenTranscriptionDoesNotExist() throws Exception {
    when(transcriptionService.getTranscription(MEDIUM_ID, TRANSCRIPTION_ID)).thenThrow(
            new TranscriptionNotFoundException(TRANSCRIPTION_ID));

    Response response = endpoint.getMediumTranscription(MEDIUM_ID, TRANSCRIPTION_ID);

    assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  @Test
  void shouldCreateTranscriptionAndReturnCreatedWithDto() throws Exception {
    Transcription created = transcription(TRANSCRIPTION_ID, MEDIUM_ID, ENGINE, MODEL);
    when(transcriptionService.createTranscription(any(GenerateTranscriptionConfiguration.class),
            eq(USER_ID))).thenReturn(created);
    CreateTranscriptionRequest request = new CreateTranscriptionRequest(ENGINE, MODEL);

    Response response = endpoint.createMediumTranscription(MEDIUM_ID, request);

    assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
    assertThat(response.getEntity()).isInstanceOf(TranscriptionDto.class);
    TranscriptionDto dto = (TranscriptionDto) response.getEntity();
    assertThat(dto.id()).isEqualTo(TRANSCRIPTION_ID);
    assertThat(dto.engineIdentifier()).isEqualTo(ENGINE);
    assertThat(dto.modelIdentifier()).isEqualTo(MODEL);

    ArgumentCaptor<GenerateTranscriptionConfiguration> configurationCaptor = ArgumentCaptor.forClass(
            GenerateTranscriptionConfiguration.class);
    ArgumentCaptor<Integer> userIdCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(transcriptionService).createTranscription(configurationCaptor.capture(), userIdCaptor.capture());
    assertThat(configurationCaptor.getValue().mediumId()).isEqualTo(MEDIUM_ID);
    assertThat(configurationCaptor.getValue().engineIdentifier()).isEqualTo(ENGINE);
    assertThat(configurationCaptor.getValue().modelIdentifier()).isEqualTo(MODEL);
    assertThat(userIdCaptor.getValue()).isEqualTo(USER_ID);
  }

  @Test
  void shouldReturnBadRequestWhenCreateRequestBodyIsNull() {
    Response response = endpoint.createMediumTranscription(MEDIUM_ID, null);

    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  void shouldReturnBadRequestWhenEngineIdentifierIsMissing() {
    CreateTranscriptionRequest request = new CreateTranscriptionRequest(null, MODEL);

    Response response = endpoint.createMediumTranscription(MEDIUM_ID, request);

    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  void shouldReturnBadRequestWhenModelIdentifierIsMissing() {
    CreateTranscriptionRequest request = new CreateTranscriptionRequest(ENGINE, null);

    Response response = endpoint.createMediumTranscription(MEDIUM_ID, request);

    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  void shouldReturnBadRequestWhenEngineModelPairIsUnknown() throws Exception {
    doThrow(new IllegalArgumentException("Model 'large-v3' is not provided by engine 'whisper'")).when(
            transcriptionService).createTranscription(any(GenerateTranscriptionConfiguration.class), anyInt());
    CreateTranscriptionRequest request = new CreateTranscriptionRequest(ENGINE, MODEL);

    Response response = endpoint.createMediumTranscription(MEDIUM_ID, request);

    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  void shouldReturnForbiddenWhenSpeechToTextFeatureIsDisabled() throws Exception {
    doThrow(new TranscriptionFeatureDisabledException("speech-to-text feature is disabled")).when(transcriptionService)
                                                                                            .createTranscription(
                                                                                                    any(GenerateTranscriptionConfiguration.class),
                                                                                                    anyInt());
    CreateTranscriptionRequest request = new CreateTranscriptionRequest(ENGINE, MODEL);

    Response response = endpoint.createMediumTranscription(MEDIUM_ID, request);

    assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
  }

  @Test
  void shouldReturnInternalServerErrorWhenServiceFails() throws Exception {
    doThrow(new TranscriptionServiceException("backend exploded")).when(transcriptionService).createTranscription(
            any(GenerateTranscriptionConfiguration.class), anyInt());
    CreateTranscriptionRequest request = new CreateTranscriptionRequest(ENGINE, MODEL);

    Response response = endpoint.createMediumTranscription(MEDIUM_ID, request);

    assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
  }

  @Test
  void shouldReturnNoContentWhenDeletingTranscription() throws Exception {
    when(transcriptionService.existsForMedium(MEDIUM_ID, TRANSCRIPTION_ID)).thenReturn(true);

    Response response = endpoint.deleteMediumTranscription(MEDIUM_ID, TRANSCRIPTION_ID);

    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    verify(transcriptionService).deleteTranscription(TRANSCRIPTION_ID);
  }

  @Test
  void shouldReturnNotFoundWhenDeletingMissingTranscription() throws Exception {
    when(transcriptionService.existsForMedium(MEDIUM_ID, TRANSCRIPTION_ID)).thenReturn(false);

    Response response = endpoint.deleteMediumTranscription(MEDIUM_ID, TRANSCRIPTION_ID);

    assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    verify(transcriptionService, never()).deleteTranscription(anyInt());
  }

  @Test
  void shouldReturnInternalServerErrorWhenDeleteFails() throws Exception {
    when(transcriptionService.existsForMedium(MEDIUM_ID, TRANSCRIPTION_ID)).thenReturn(true);
    doThrow(new TranscriptionServiceException("backend exploded")).when(transcriptionService)
                                                                  .deleteTranscription(TRANSCRIPTION_ID);

    Response response = endpoint.deleteMediumTranscription(MEDIUM_ID, TRANSCRIPTION_ID);

    assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
  }

  @Test
  void shouldStreamSrtFileWhenTranscriptionExistsForMedium(@TempDir Path tempDir) throws Exception {
    Path srtFile = tempDir.resolve(TRANSCRIPTION_ID + ".srt");
    String srtContent = "1\n00:00:01,000 --> 00:00:02,000\nHello World\n";
    Files.writeString(srtFile, srtContent, StandardCharsets.UTF_8);
    when(transcriptionService.existsForMedium(MEDIUM_ID, TRANSCRIPTION_ID)).thenReturn(true);
    when(transcriptionFileStorage.getPathToTranscription(TRANSCRIPTION_ID)).thenReturn(Optional.of(srtFile));

    Response response = endpoint.downloadTranscriptionFile(MEDIUM_ID, TRANSCRIPTION_ID);

    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    assertThat(response.getHeaderString("Content-Disposition")).contains(TRANSCRIPTION_ID + ".srt");
    assertThat(response.getMediaType().toString()).isEqualTo("text/plain");
    assertThat(response.getEntity()).isInstanceOf(StreamingOutput.class);

    StreamingOutput stream = (StreamingOutput) response.getEntity();
    ByteArrayOutputStream sink = new ByteArrayOutputStream();
    stream.write(sink);
    assertThat(sink.toString(StandardCharsets.UTF_8)).isEqualTo(srtContent);
  }

  @Test
  void shouldReturnNotFoundWhenTranscriptionDoesNotExistForMediumOnDownload() {
    when(transcriptionService.existsForMedium(MEDIUM_ID, TRANSCRIPTION_ID)).thenReturn(false);

    Response response = endpoint.downloadTranscriptionFile(MEDIUM_ID, TRANSCRIPTION_ID);

    assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    verify(transcriptionFileStorage, never()).getPathToTranscription(anyInt());
  }

  @Test
  void shouldReturnNotFoundWhenSrtFileIsMissingOnDisk() {
    when(transcriptionService.existsForMedium(MEDIUM_ID, TRANSCRIPTION_ID)).thenReturn(true);
    when(transcriptionFileStorage.getPathToTranscription(TRANSCRIPTION_ID)).thenReturn(Optional.empty());

    Response response = endpoint.downloadTranscriptionFile(MEDIUM_ID, TRANSCRIPTION_ID);

    assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  @Test
  void shouldReturnInternalServerErrorWhenStreamingFails(@TempDir Path tempDir) throws Exception {
    Path missingFile = tempDir.resolve("does-not-exist.srt");
    when(transcriptionService.existsForMedium(MEDIUM_ID, TRANSCRIPTION_ID)).thenReturn(true);
    when(transcriptionFileStorage.getPathToTranscription(TRANSCRIPTION_ID)).thenReturn(Optional.of(missingFile));

    Response response = endpoint.downloadTranscriptionFile(MEDIUM_ID, TRANSCRIPTION_ID);

    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    StreamingOutput stream = (StreamingOutput) response.getEntity();

    WebApplicationException thrown = catchThrowableOfType(() -> stream.write(new ByteArrayOutputStream()),
            WebApplicationException.class);
    assertThat(thrown).isNotNull();
    assertThat(thrown.getResponse().getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
  }

  private Transcription transcription(int id, int mediumId, String engineIdentifier, String modelIdentifier) {
    Transcription transcription = new Transcription();
    transcription.setId(id);
    transcription.setName(engineIdentifier + " / " + modelIdentifier);
    transcription.setCreatedAt(Instant.parse("2026-05-24T10:00:00Z"));

    Medium medium = new Medium();
    medium.setId(mediumId);
    transcription.setMedium(medium);

    TranscriptionModelId modelId = new TranscriptionModelId();
    modelId.setEngineIdentifier(engineIdentifier);
    modelId.setModelIdentifier(modelIdentifier);
    TranscriptionModel model = new TranscriptionModel();
    model.setId(modelId);
    transcription.setTranscriptionModel(model);

    de.bitgilde.TIMAAT.model.FIPOP.TranscriptionState stateEntity = new de.bitgilde.TIMAAT.model.FIPOP.TranscriptionState();
    stateEntity.setId(TranscriptionState.PENDING.getDatabaseId());
    transcription.setTranscriptionState(stateEntity);

    de.bitgilde.TIMAAT.model.FIPOP.TranscriptionType typeEntity = new de.bitgilde.TIMAAT.model.FIPOP.TranscriptionType();
    typeEntity.setId(TranscriptionType.GENERATED.getDatabaseId());
    transcription.setTranscriptionType(typeEntity);

    return transcription;
  }
}
