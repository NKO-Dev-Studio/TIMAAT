package de.bitgilde.TIMAAT.storage.entity.transcription.api;

/**
 * Different states a {@link de.bitgilde.TIMAAT.model.FIPOP.Transcription} can be in
 *
 * @author Nico Kotlenga (nico@nko-dev.studio) 
 * @since 12.05.26
 */
public enum TranscriptionState {
  WAITING_FOR_PREPARATION(1), PREPARING(2), PENDING(3), RUNNING(4), COMPLETED(5), FAILED(6);

  private final int databaseId;

  TranscriptionState(int databaseId) {
    this.databaseId = databaseId;
  }

  public int getDatabaseId() {
    return databaseId;
  }

  /**
   * Looks up the enum constant whose {@link #getDatabaseId()} matches the given id.
   *
   * @param databaseId the {@code transcription_state.id} value persisted in the DB
   * @return the matching enum constant
   * @throws IllegalArgumentException if no constant has the given database id
   */
  public static TranscriptionState fromDatabaseId(int databaseId) {
    for (TranscriptionState state : values()) {
      if (state.databaseId == databaseId) {
        return state;
      }
    }
    throw new IllegalArgumentException("No TranscriptionState with database id " + databaseId);
  }
}
