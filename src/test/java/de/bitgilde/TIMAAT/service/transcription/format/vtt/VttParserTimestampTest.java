package de.bitgilde.TIMAAT.service.transcription.format.vtt;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for VttParser timestamp parsing (start and end time extraction from a cue timing line).
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 10.06.26
 */
class VttParserTimestampTest {

  @Test
  void parseStartTime_withoutHours_returnsCorrectDuration() {
    Duration result = VttParser.parseStartTime("00:01.000 --> 00:04.000");

    assertThat(result).isEqualTo(Duration.ofSeconds(1));
  }

  @Test
  void parseStartTime_withHours_returnsCorrectDuration() {
    Duration result = VttParser.parseStartTime("20:00:01.000 --> 00:04.000");

    assertThat(result).isEqualTo(Duration.ofHours(20).plusSeconds(1));
  }

  @Test
  void parseStartTime_withMilliseconds_includesMilliseconds() {
    Duration result = VttParser.parseStartTime("00:01.500 --> 00:04.000");

    assertThat(result).isEqualTo(Duration.ofMillis(1500));
  }

  @Test
  void parseEndTime_withoutHours_returnsCorrectDuration() {
    Duration result = VttParser.parseEndTime("00:01.000 --> 00:04.000");

    assertThat(result).isEqualTo(Duration.ofSeconds(4));
  }

  @Test
  void parseEndTime_withHours_returnsCorrectDuration() {
    Duration result = VttParser.parseEndTime("00:01.000 --> 20:00:04.000");

    assertThat(result).isEqualTo(Duration.ofHours(20).plusSeconds(4));
  }

  @Test
  void parseEndTime_withMilliseconds_includesMilliseconds() {
    Duration result = VttParser.parseEndTime("00:01.000 --> 00:04.750");

    assertThat(result).isEqualTo(Duration.ofSeconds(4).plusMillis(750));
  }

  @Test
  void parseStartTime_mixedHours_startHasHoursEndDoesNot() {
    Duration result = VttParser.parseStartTime("20:00:01.000 --> 00:04.000");

    assertThat(result).isEqualTo(Duration.ofHours(20).plusSeconds(1));
  }

  @Test
  void parseEndTime_mixedHours_endHasHoursStartDoesNot() {
    Duration result = VttParser.parseEndTime("00:01.000 --> 20:00:04.000");

    assertThat(result).isEqualTo(Duration.ofHours(20).plusSeconds(4));
  }
}
