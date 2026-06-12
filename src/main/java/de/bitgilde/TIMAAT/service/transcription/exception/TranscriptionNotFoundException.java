package de.bitgilde.TIMAAT.service.transcription.exception;

import de.bitgilde.TIMAAT.service.transcription.TranscriptionService;

/**
 * Signals that an operation of the {@link TranscriptionService} targeted a
 * {@link de.bitgilde.TIMAAT.model.FIPOP.Transcription} which does not exist (or no longer exists).
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-05-21
 */
public class TranscriptionNotFoundException extends TranscriptionException {

  private final int transcriptionId;

  public TranscriptionNotFoundException(int transcriptionId) {
    super("No transcription with id " + transcriptionId + " found");
    this.transcriptionId = transcriptionId;
  }

  public int getTranscriptionId() {
    return transcriptionId;
  }
}
