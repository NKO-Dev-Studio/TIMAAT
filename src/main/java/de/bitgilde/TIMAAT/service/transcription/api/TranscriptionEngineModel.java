package de.bitgilde.TIMAAT.service.transcription.api;

/**
 * Snapshot of a single transcription model offered by a {@link TranscriptionEngine}, including a
 * flag indicating whether it is currently configured as the deployment's default model.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-05-28
 */
public record TranscriptionEngineModel(String modelIdentifier, boolean isDefault) {
}
