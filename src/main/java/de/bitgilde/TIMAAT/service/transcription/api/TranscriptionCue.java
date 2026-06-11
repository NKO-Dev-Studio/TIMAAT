package de.bitgilde.TIMAAT.service.transcription.api;

import java.time.Duration;
import java.util.Objects;

/**
 * A single {@link TranscriptionCue} of {@link TranscriptionContent}
 *
 * @author Nico Kotlenga (nico@nko-dev.studio) 
 * @since 10.06.26
 */
public record TranscriptionCue(Duration startTime, Duration endTime, String cue) {

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TranscriptionCue that = (TranscriptionCue) o;
    return Objects.equals(cue, that.cue) && Objects.equals(endTime, that.endTime) && Objects.equals(startTime,
            that.startTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(startTime, endTime, cue);
  }
}
