package de.bitgilde.TIMAAT.rest.endpoint;

import de.bitgilde.TIMAAT.rest.Secured;
import de.bitgilde.TIMAAT.service.transcription.TranscriptionService;
import de.bitgilde.TIMAAT.service.transcription.api.TranscriptionEngine;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jvnet.hk2.annotations.Service;

import java.util.Collection;

/**
 * REST resource exposing transcription-related operations such as engine capability discovery.
 * Settings persisted in the singleton system-settings row are intentionally handled by
 * {@link EndpointSystemSettings} so that this resource only deals with the transcription domain
 * itself.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-05-24
 */
@Service
@Path("/transcription")
public class EndpointTranscription {

  @Inject
  TranscriptionService transcriptionService;

  /**
   * Returns the engines (and their models) currently offered by the connected speech-to-text-service.
   * Each model in the payload is tagged with an {@code isDefault} flag so clients can render engine
   * and model selectors without issuing a separate request for the deployment-wide default. When the
   * speech-to-text feature is disabled for this deployment an empty collection is returned so callers
   * can render an "unavailable" state without special-casing the response code.
   *
   * @return the available engine capabilities, each model annotated with whether it is the configured default
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("engines")
  public Collection<TranscriptionEngine> listAvailableEngines() {
    return transcriptionService.getAvailableEngineCapabilities();
  }
}
