package de.bitgilde.TIMAAT.rest.model.transcription;

import jakarta.annotation.Nullable;

/**
 * Request payload accepted by {@code POST /medium/{id}/transcriptions}. Both
 * {@code engineIdentifier} and {@code modelIdentifier} are required; the endpoint rejects
 * requests where either field is {@code null} or blank with {@code 400 Bad Request}. They are
 * declared {@link Nullable} so the JSON binder can populate the record from payloads that omit
 * one of the fields and the endpoint can validate them itself with a clearer message than the
 * binder would produce.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-05-24
 */
public record CreateTranscriptionRequest(@Nullable String engineIdentifier,
                                         @Nullable String modelIdentifier) {
}
