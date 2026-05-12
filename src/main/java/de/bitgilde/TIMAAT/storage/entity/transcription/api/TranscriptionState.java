package de.bitgilde.TIMAAT.storage.entity.transcription.api;

/**
 * Different states a {@link de.bitgilde.TIMAAT.model.FIPOP.Transcription} can be in
 *
 * @author Nico Kotlenga (nico@nko-dev.studio) 
 * @since 12.05.26
 */
public enum TranscriptionState {
  PREPARING(1), PENDING(2), RUNNING(3), COMPLETED(4), FAILED(5);

  private final int databaseId;

  TranscriptionState(int databaseId) {
    this.databaseId = databaseId;
  }

  public int getDatabaseId() {
    return databaseId;
  }
}
