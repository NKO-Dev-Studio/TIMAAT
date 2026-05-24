package de.bitgilde.TIMAAT.rest.model.transcription;

import de.bitgilde.TIMAAT.storage.entity.transcription.api.TranscriptionState;
import de.bitgilde.TIMAAT.storage.entity.transcription.api.TranscriptionType;
import jakarta.annotation.Nullable;

import java.time.Instant;

/**
 * Wire representation of a {@link de.bitgilde.TIMAAT.model.FIPOP.Transcription}. Exposes only the
 * fields needed by the UI so the persistence model can evolve without breaking clients. The
 * {@code engineIdentifier} and {@code modelIdentifier} mirror the persisted model id of the
 * underlying {@link de.bitgilde.TIMAAT.model.FIPOP.TranscriptionModel}; both are {@code null} for
 * imported transcriptions that were not produced by an engine/model pair.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-05-24
 */
public record TranscriptionDto(int id,
                               String name,
                               int mediumId,
                               @Nullable String engineIdentifier,
                               @Nullable String modelIdentifier,
                               TranscriptionState state,
                               TranscriptionType type,
                               Instant createdAt) {
}
