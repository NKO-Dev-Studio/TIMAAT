package de.bitgilde.TIMAAT.service.transcription.api;

/**
 * Configuration required to create a new Transcription
 *
 * @author Nico Kotlenga (nico@nko-dev.studio) 
 * @since 12.05.26
 */
public record GenerateTranscriptionConfiguration(int mediumId, String engineIdentifier, String modelIdentifier) {

}
