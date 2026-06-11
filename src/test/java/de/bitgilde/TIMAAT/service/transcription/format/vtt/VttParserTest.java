package de.bitgilde.TIMAAT.service.transcription.format.vtt;

import de.bitgilde.TIMAAT.service.transcription.api.TranscriptionContent;
import de.bitgilde.TIMAAT.service.transcription.api.TranscriptionCue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testsuite of {@link VttParser}
 *
 * @author Nico Kotlenga (nico@nko-dev.studio) 
 * @since 10.06.26
 */
public class VttParserTest {
  private static final String VTT_VALID_FILE = "/test-transcription/valid.vtt";
  private static final String VTT_INVALID_FILE = "/test-transcription/invalid.vtt";
  private static VttParser vttParser;

  @BeforeAll
  public static void setup() {
    vttParser = new VttParser();
  }

  @Test
  public void shouldParseVttFile() throws IOException {
    try (InputStream inputStream = VttParserTest.class.getResourceAsStream(VTT_VALID_FILE)) {
      TranscriptionContent transcriptionContent = assertDoesNotThrow(() -> vttParser.parseVttStream(inputStream));
      assertEquals(4, transcriptionContent.cues().size());

      TranscriptionCue firstCue = new TranscriptionCue(
              Duration.ofHours(0).plusMinutes(59).plusSeconds(42).plusMillis(880),
              Duration.ofHours(0).plusMinutes(59).plusSeconds(46).plusMillis(360),
              "Ah, da wollen u das Backstage-Catering.");
      TranscriptionCue secondCue = new TranscriptionCue(
              Duration.ofHours(0).plusMinutes(59).plusSeconds(46).plusMillis(360),
              Duration.ofHours(0).plusMinutes(59).plusSeconds(52).plusMillis(200), "Hahaha, schnitziert, na toll.");
      TranscriptionCue thirdCue = new TranscriptionCue(
              Duration.ofHours(0).plusMinutes(59).plusSeconds(53).plusMillis(480),
              Duration.ofHours(0).plusMinutes(59).plusSeconds(57).plusMillis(680),
              "Man kann quasi auch bei AO, man kann offenbar hier,");
      TranscriptionCue fourthCue = new TranscriptionCue(
              Duration.ofHours(1).plusMinutes(0).plusSeconds(0).plusMillis(600),
              Duration.ofHours(1).plusMinutes(0).plusSeconds(3).plusMillis(600),
              "den Ziel für einen Bereich spielt für einen Bereich Spenden.");

      assertEquals(firstCue, transcriptionContent.cues().get(0));
      assertEquals(secondCue, transcriptionContent.cues().get(1));
      assertEquals(thirdCue, transcriptionContent.cues().get(2));
      assertEquals(fourthCue, transcriptionContent.cues().get(3));
    }
  }

  @Test
  public void shouldParseValidPartsOfInvalidVttFile() throws IOException {
    try (InputStream inputStream = VttParserTest.class.getResourceAsStream(VTT_INVALID_FILE)) {
      TranscriptionContent transcriptionContent = assertDoesNotThrow(() -> vttParser.parseVttStream(inputStream));
      assertEquals(1, transcriptionContent.cues().size());

      TranscriptionCue firstCue = new TranscriptionCue(
              Duration.ofHours(1).plusMinutes(0).plusSeconds(0).plusMillis(600),
              Duration.ofHours(1).plusMinutes(0).plusSeconds(3).plusMillis(600),
              "den Ziel für einen Bereich spielt für einen Bereich Spenden.");

      assertEquals(firstCue, transcriptionContent.cues().get(0));
    }
  }
}
