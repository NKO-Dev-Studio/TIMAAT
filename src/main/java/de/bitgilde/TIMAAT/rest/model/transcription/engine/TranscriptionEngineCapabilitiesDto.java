package de.bitgilde.TIMAAT.rest.model.transcription.engine;

import java.util.Collection;

/**
 * Wire representation of an engine currently offered by the connected speech-to-text-service together
 * with its models. Each model carries an {@code isDefault} flag so clients can render engine and
 * model selectors and highlight the active default without issuing a separate request for the
 * deployment-wide default model.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-05-28
 */
public record TranscriptionEngineCapabilitiesDto(String engineIdentifier,
                                                 String engineName,
                                                 Collection<TranscriptionModelCapabilityDto> models) {
}
