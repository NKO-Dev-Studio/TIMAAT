package de.bitgilde.TIMAAT.storage.entity.medium.api;

import jakarta.annotation.Nullable;

/**
 * This content will be sent over the {@link de.bitgilde.TIMAAT.sse.EntityUpdateEventService}
 * when default transcription of a {@link de.bitgilde.TIMAAT.model.FIPOP.Medium} change
 *
 * @author Nico Kotlenga (nico@nko-dev.studio) 
 * @since 09.06.26
 */
public record MediumDefaultTranscriptionEntityUpdateMessage(@Nullable Integer defaultTranscriptionId) {

}
