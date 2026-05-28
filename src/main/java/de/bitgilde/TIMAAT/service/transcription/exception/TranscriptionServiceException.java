package de.bitgilde.TIMAAT.service.transcription.exception;

import de.bitgilde.TIMAAT.service.transcription.TranscriptionService;

/**
 * This exception will be thrown when an error occurred during using the {@link TranscriptionService}.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 21.05.26
 */
public class TranscriptionServiceException extends Exception {

  public TranscriptionServiceException(String message) {
    super(message);
  }

  public TranscriptionServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
