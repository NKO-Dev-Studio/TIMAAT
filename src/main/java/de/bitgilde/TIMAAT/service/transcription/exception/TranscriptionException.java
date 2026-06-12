package de.bitgilde.TIMAAT.service.transcription.exception;

import de.bitgilde.TIMAAT.service.transcription.TranscriptionService;

/**
 * This exception will be thrown when an error occurred during using the {@link TranscriptionService}.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 21.05.26
 */
public class TranscriptionException extends Exception {

  public TranscriptionException(String message) {
    super(message);
  }

  public TranscriptionException(String message, Throwable cause) {
    super(message, cause);
  }
}
