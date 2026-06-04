package de.bitgilde.TIMAAT.rest.model.transcription.engine;

/**
 * Wire representation of a single transcription model offered by a speech-to-text engine, including
 * a flag indicating whether it is currently configured as the deployment's default model. The
 * default flag is derived from the persisted system settings at response time so clients can render
 * engine and model selectors without issuing a second request for the default.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-05-28
 */
public record TranscriptionModelCapabilityDto(String modelIdentifier, boolean isDefault) {
}
