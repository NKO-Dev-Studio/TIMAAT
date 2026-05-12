package de.bitgilde.TIMAAT.service.task.api;

/**
 * This task will be executed before creating the actual transcription.
 * It prepares a medium file by extracting the audio layer and convert it to mono,
 * reducing the size of information which needed to get transferred to the speech-to-text-service
 *
 * @author Nico Kotlenga (nico@nko-dev.studio) 
 * @since 12.05.26
 */
public record TranscriptionMediumPreparationTask(long mediumId) {
}
