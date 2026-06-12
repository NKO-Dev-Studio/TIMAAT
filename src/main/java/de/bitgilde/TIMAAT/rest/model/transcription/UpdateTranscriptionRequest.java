package de.bitgilde.TIMAAT.rest.model.transcription;

import jakarta.annotation.Nullable;

/**
 * Request payload accepted by {@code PUT /medium/{id}/transcriptions/{transcriptionId}}. Carries
 * the editable base information of a transcription; currently only the {@code name} can be changed.
 * The field is declared {@link Nullable} so the JSON binder can populate the record from payloads
 * that omit it and the endpoint can validate it itself with a clearer message than the binder would
 * produce; a request with a {@code null} or blank name is rejected with {@code 400 Bad Request}.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-06-11
 */
public record UpdateTranscriptionRequest(@Nullable String name) {
}
