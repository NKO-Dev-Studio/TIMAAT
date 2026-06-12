package de.bitgilde.TIMAAT.service.transcription.api;

/**
 * This message is send over the {@link de.bitgilde.TIMAAT.sse.EntityUpdateEventService} when the
 * content (the underlying VTT file) of a transcription has changed. The transcription content is
 * not part of the transcription DTO, so the {@code contentChanged} flag signals to the client that
 * it should reload the content if it is currently displaying it.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-06-11
 */
public record TranscriptionContentEntityUpdateMessage(boolean contentChanged) {
}
