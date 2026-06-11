package de.bitgilde.TIMAAT.service.transcription.format.vtt;

import de.bitgilde.TIMAAT.service.transcription.api.TranscriptionContent;
import de.bitgilde.TIMAAT.service.transcription.api.TranscriptionCue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testsuite of {@link VttWriter}, verifying the round-trip with {@link VttParser}.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-06-11
 */
public class VttWriterTest {

  private static VttWriter vttWriter;
  private static VttParser vttParser;

  @BeforeAll
  public static void setup() {
    vttWriter = new VttWriter();
    vttParser = new VttParser();
  }

  @Test
  public void shouldWriteVttHeader() throws IOException {
    TranscriptionContent content = new TranscriptionContent(List.of(
            new TranscriptionCue(Duration.ofSeconds(1), Duration.ofSeconds(2), "Hello")));

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    vttWriter.writeVttStream(content, outputStream);

    String written = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(written.startsWith("WEBVTT"), "VTT output must start with the WEBVTT header");
  }

  @Test
  public void shouldRoundTripContentThroughParser() throws IOException {
    TranscriptionContent content = new TranscriptionContent(List.of(
            new TranscriptionCue(Duration.ofHours(0).plusMinutes(59).plusSeconds(42).plusMillis(880),
                    Duration.ofHours(0).plusMinutes(59).plusSeconds(46).plusMillis(360),
                    "Ah, da wollen u das Backstage-Catering."),
            new TranscriptionCue(Duration.ofHours(0).plusMinutes(59).plusSeconds(46).plusMillis(360),
                    Duration.ofHours(0).plusMinutes(59).plusSeconds(52).plusMillis(200), "Hahaha, schnitziert, na toll."),
            new TranscriptionCue(Duration.ofHours(1).plusMinutes(0).plusSeconds(0).plusMillis(600),
                    Duration.ofHours(1).plusMinutes(0).plusSeconds(3).plusMillis(600),
                    "den Ziel für einen Bereich spielt für einen Bereich Spenden.")));

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    vttWriter.writeVttStream(content, outputStream);

    TranscriptionContent parsed = vttParser.parseVttStream(
            new ByteArrayInputStream(outputStream.toByteArray()));

    assertEquals(content.cues(), parsed.cues());
  }
}
