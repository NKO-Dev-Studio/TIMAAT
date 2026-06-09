package de.bitgilde.TIMAAT.service.transcription.api;

import de.bitgilde.TIMAAT.storage.entity.transcription.api.TranscriptionState;

/**
 * This message is send over the {@link de.bitgilde.TIMAAT.sse.EntityUpdateEventService} when the state of
 * a transcription has changed
 *
 * @author Nico Kotlenga (nico@nko-dev.studio) 
 * @since 09.06.26
 */
public record TranscriptionStateEntityUpdateMessage(TranscriptionState state) {
}
