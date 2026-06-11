package de.bitgilde.TIMAAT.service.transcription.api;

import java.util.List;

/**
 * Represents the content of a transcription
 *
 * @author Nico Kotlenga (nico@nko-dev.studio) 
 * @since 10.06.26
 */
public record TranscriptionContent(List<TranscriptionCue> cues) {

}
