package de.bitgilde.TIMAAT.rest.endpoint;

import de.bitgilde.TIMAAT.model.FIPOP.UserAccount;
import de.bitgilde.TIMAAT.rest.filter.AuthenticationFilter;
import de.bitgilde.TIMAAT.rest.model.systemsettings.TranscriptionSettingsDto;
import de.bitgilde.TIMAAT.rest.model.systemsettings.UpdateTranscriptionSettingsRequest;
import de.bitgilde.TIMAAT.service.transcription.TranscriptionService;
import de.bitgilde.TIMAAT.service.transcription.exception.TranscriptionFeatureDisabledException;
import de.bitgilde.TIMAAT.storage.entity.api.TranscriptionSystemSettings;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests covering the HTTP status-code mapping of {@link EndpointSystemSettings#updateTranscriptionSettings}.
 * The endpoint is wired directly with mocked collaborators (Jersey is not involved); fields are populated
 * via reflection because they are HK2 injection targets without setters.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-05-24
 */
public class EndpointSystemSettingsTest {

  private TranscriptionService transcriptionService;
  private ContainerRequestContext containerRequestContext;
  private EndpointSystemSettings endpoint;

  @BeforeEach
  void setUp() throws Exception {
    transcriptionService = mock(TranscriptionService.class);
    containerRequestContext = mock(ContainerRequestContext.class);
    endpoint = new EndpointSystemSettings();

    inject("transcriptionService", transcriptionService);
    inject("containerRequestContext", containerRequestContext);

    when(transcriptionService.isFeatureEnabled()).thenReturn(true);
    when(transcriptionService.getTranscriptionSystemSettings()).thenReturn(
            new TranscriptionSystemSettings(false, null));
  }

  private void inject(String fieldName, Object value) throws Exception {
    Field field = EndpointSystemSettings.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(endpoint, value);
  }

  @Test
  void shouldRespondWithForbiddenWhenFeatureIsDisabled() throws Exception {
    doThrow(new TranscriptionFeatureDisabledException("speech-to-text feature is disabled"))
            .when(transcriptionService).updateTranscriptionSystemSettings(anyBoolean(), any(), any(), any());
    UpdateTranscriptionSettingsRequest request = new UpdateTranscriptionSettingsRequest(false, "whisper", "large-v3");

    Response response = endpoint.updateTranscriptionSettings(request);

    assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
  }

  @Test
  void shouldRespondWithBadRequestOnIllegalArgument() throws Exception {
    doThrow(new IllegalArgumentException("engine and model must both be set or both be null"))
            .when(transcriptionService).updateTranscriptionSystemSettings(anyBoolean(), any(), any(), any());
    UpdateTranscriptionSettingsRequest request = new UpdateTranscriptionSettingsRequest(true, "whisper", null);

    Response response = endpoint.updateTranscriptionSettings(request);

    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  void shouldRespondWithBadRequestWhenRequestBodyIsNull() {
    Response response = endpoint.updateTranscriptionSettings(null);

    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  void shouldRespondWithOkAndUpdatedDtoOnSuccess() throws Exception {
    UserAccount editingUser = new UserAccount();
    when(containerRequestContext.getProperty(eq(AuthenticationFilter.USER_ACCOUNT_PROPERTY_NAME))).thenReturn(
            editingUser);
    when(transcriptionService.getTranscriptionSystemSettings()).thenReturn(
            new TranscriptionSystemSettings(true, null));
    UpdateTranscriptionSettingsRequest request = new UpdateTranscriptionSettingsRequest(true, null, null);

    Response response = endpoint.updateTranscriptionSettings(request);

    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    assertThat(response.getEntity()).isInstanceOf(TranscriptionSettingsDto.class);
    TranscriptionSettingsDto dto = (TranscriptionSettingsDto) response.getEntity();
    assertThat(dto.autoTranscribeUploads()).isTrue();
    verify(transcriptionService).updateTranscriptionSystemSettings(true, null, null, editingUser);
  }
}
