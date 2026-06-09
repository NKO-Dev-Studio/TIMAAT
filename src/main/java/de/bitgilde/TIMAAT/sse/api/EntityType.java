package de.bitgilde.TIMAAT.sse.api;

/**
 * Defines the {@link EntityType}s to which {@link EntityUpdateMessage}s can be related to
 *
 * @author Nico Kotlenga (nico@nko-dev.studio) 
 * @since 09.06.26
 */
public enum EntityType {
  MEDIUM("medium"), TRANSCRIPTION("transcription"), MEDIUM_AUDIO_ANALYSIS("medium-audio-analysis");

  private final String eventName;

  EntityType(String eventName) {
    this.eventName = eventName;
  }

  public String getEventName() {
    return eventName;
  }
}
