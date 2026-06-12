package de.bitgilde.TIMAAT.service.transcription.exception;

/**
 * This exception is thrown if the transcription file is corrupted or does not match the expected format
 *
 * @author Nico Kotlenga (nico@nko-dev.studio) 
 * @since 12.06.26
 */
public class TranscriptionContentFormatException extends TranscriptionException {
  public TranscriptionContentFormatException(String message) {
    super(message);
  }
}
