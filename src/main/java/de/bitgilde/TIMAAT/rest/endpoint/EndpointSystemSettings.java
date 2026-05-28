package de.bitgilde.TIMAAT.rest.endpoint;

import de.bitgilde.TIMAAT.model.FIPOP.TranscriptionModel;
import de.bitgilde.TIMAAT.model.FIPOP.TranscriptionModelId;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount;
import de.bitgilde.TIMAAT.rest.Secured;
import de.bitgilde.TIMAAT.rest.filter.AuthenticationFilter;
import de.bitgilde.TIMAAT.rest.model.systemsettings.TranscriptionDefaultModelDto;
import de.bitgilde.TIMAAT.rest.model.systemsettings.TranscriptionSettingsDto;
import de.bitgilde.TIMAAT.rest.model.systemsettings.UpdateTranscriptionSettingsRequest;
import de.bitgilde.TIMAAT.service.transcription.TranscriptionService;
import de.bitgilde.TIMAAT.service.transcription.exception.TranscriptionFeatureDisabledException;
import de.bitgilde.TIMAAT.storage.entity.api.TranscriptionSystemSettings;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.jvnet.hk2.annotations.Service;

/**
 * REST resource exposing the singleton system-settings row. Currently every persisted field is
 * transcription-related, so all routes live under {@code /system-settings/transcription}. When
 * non-transcription settings are added, additional sub-paths can be introduced on the same resource
 * without breaking existing URLs.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-05-24
 */
@Service
@Path("/system-settings")
public class EndpointSystemSettings {

  @Inject
  TranscriptionService transcriptionService;

  @Context
  ContainerRequestContext containerRequestContext;

  /**
   * Returns the transcription-related system settings, including a {@code featureEnabled} flag that
   * tells the client whether the deployment offers speech-to-text at all (driven by the
   * {@code stt.enabled} property). When the feature is disabled the persisted preferences are still
   * returned but will not take effect until the feature is re-enabled.
   *
   * @return the current transcription-related system settings as a DTO
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("transcription")
  public TranscriptionSettingsDto getTranscriptionSettings() {
    TranscriptionSystemSettings settings = transcriptionService.getTranscriptionSystemSettings();
    return toDto(settings);
  }

  /**
   * Updates the transcription-related system settings. Engine and model identifiers must either both be
   * provided (to configure a default model) or both be omitted (to clear the default). When the
   * speech-to-text feature is disabled, providing an engine/model identifier is refused with
   * {@code 403 Forbidden}; clearing the default and toggling {@code autoTranscribeUploads} are still
   * allowed so the preference is preserved across enable/disable cycles.
   *
   * @param request the new settings to apply
   * @return {@code 200 OK} with the updated settings; {@code 400 Bad Request} if the engine/model pair
   * is inconsistent or unknown; {@code 403 Forbidden} if a default model is requested while the feature is disabled
   */
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Secured
  @Path("transcription")
  public Response updateTranscriptionSettings(UpdateTranscriptionSettingsRequest request) {
    if (request == null) {
      return Response.status(Status.BAD_REQUEST).entity("{\"reason\":\"request body is required\"}").build();
    }

    UserAccount editingUser = (UserAccount) containerRequestContext.getProperty(
            AuthenticationFilter.USER_ACCOUNT_PROPERTY_NAME);

    try {
      transcriptionService.updateTranscriptionSystemSettings(request.autoTranscribeUploads(),
              request.defaultEngineIdentifier(), request.defaultModelIdentifier(), editingUser);
    } catch (TranscriptionFeatureDisabledException e) {
      return Response.status(Status.FORBIDDEN).entity("{\"reason\":\"" + e.getMessage() + "\"}").build();
    } catch (IllegalArgumentException e) {
      return Response.status(Status.BAD_REQUEST).entity("{\"reason\":\"" + e.getMessage() + "\"}").build();
    }

    return Response.ok(toDto(transcriptionService.getTranscriptionSystemSettings())).build();
  }

  /**
   * Returns just the currently configured default transcription engine/model pair. Both identifier
   * fields are {@code null} when no default has been configured. Provided as a separate endpoint so
   * clients that only need the default (e.g. when offering "Generate transcription" on a medium) do
   * not have to fetch the full settings payload.
   *
   * @return the default model DTO; identifiers are {@code null} when no default is configured
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("transcription/default-model")
  public TranscriptionDefaultModelDto getTranscriptionDefaultModel() {
    TranscriptionModel defaultModel = transcriptionService.getTranscriptionSystemSettings().defaultTranscriptionModel();
    return toDefaultModelDto(defaultModel);
  }

  private TranscriptionSettingsDto toDto(TranscriptionSystemSettings settings) {
    TranscriptionModel defaultModel = settings.defaultTranscriptionModel();
    return new TranscriptionSettingsDto(transcriptionService.isFeatureEnabled(), settings.autoTranscribeUploads(),
            toDefaultModelDto(defaultModel));
  }

  private TranscriptionDefaultModelDto toDefaultModelDto(@Nullable TranscriptionModel defaultModel) {
    if (defaultModel == null) {
      return new TranscriptionDefaultModelDto(null, null);
    }
    TranscriptionModelId id = defaultModel.getId();
    return new TranscriptionDefaultModelDto(id.getEngineIdentifier(), id.getModelIdentifier());
  }
}
