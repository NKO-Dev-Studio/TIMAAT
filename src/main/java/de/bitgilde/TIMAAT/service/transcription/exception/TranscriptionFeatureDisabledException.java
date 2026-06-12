package de.bitgilde.TIMAAT.service.transcription.exception;

/**
 * Thrown when an operation requires the speech-to-text feature to be enabled for the deployment
 * (i.e. {@code stt.enabled=true} in the TIMAAT properties file) but the feature is currently
 * disabled. Callers should typically translate this to an HTTP 409 Conflict response.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-05-24
 */
public class TranscriptionFeatureDisabledException extends TranscriptionException {
  private static final long serialVersionUID = 1L;

  public TranscriptionFeatureDisabledException(String message) {
    super(message);
  }
}
