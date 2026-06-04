package de.bitgilde.TIMAAT.service.transcription.api;

import java.util.Collection;

/**
 * Snapshot of an engine currently offered by the connected speech-to-text-service together with its
 * models. Each model carries an {@code isDefault} flag derived from the persisted system settings so
 * callers can render engine and model selectors without issuing a separate request for the
 * deployment-wide default model.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-05-28
 */
public record TranscriptionEngine(String engineIdentifier,
                                  String engineName,
                                  Collection<TranscriptionEngineModel> models) {
}
