package de.bitgilde.TIMAAT.service.transcription.format.vtt;

import de.bitgilde.TIMAAT.service.transcription.api.TranscriptionContent;
import de.bitgilde.TIMAAT.service.transcription.api.TranscriptionCue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class parses a VTT file into a {@link de.bitgilde.TIMAAT.service.transcription.api.TranscriptionContent}
 * representation.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio) 
 * @since 10.06.26
 */
public class VttParser {

  private static final Pattern CUE_START_PATTERN = Pattern.compile(
          "(\\d{2}:)?\\d{2}:\\d{2}\\.\\d{3} --> (\\d{2}:)?\\d{2}:\\d{2}\\.\\d{3}");
  private static final Pattern CUE_TIMESTAMP_PATTERN = Pattern.compile("(\\d{2}:)?\\d{2}:\\d{2}\\.\\d{3}");

  public TranscriptionContent parseVttStream(InputStream inputStream) throws IOException {
    List<TranscriptionCue> cues = new ArrayList<>();

    try (BufferedReader bufferedReader = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      while (bufferedReader.ready()) {
        String currentLine = bufferedReader.readLine();
        if (CUE_START_PATTERN.matcher(currentLine).find()) {
          Duration startTime = parseStartTime(currentLine);
          Duration endTime = parseEndTime(currentLine);
          String cue = parseCue(bufferedReader);

          cues.add(new TranscriptionCue(startTime, endTime, cue));
        }
      }
    }

    return new TranscriptionContent(cues);
  }

  private static String parseCue(BufferedReader bufferedReader) throws IOException {
    StringBuilder cueBuilder = new StringBuilder();
    while (bufferedReader.ready()) {
      String line = bufferedReader.readLine();
      if (line.isBlank()) {
        break;
      }
      cueBuilder.append(line);
    }

    return cueBuilder.toString();
  }

  static Duration parseStartTime(String line) {
    Matcher matcher = CUE_TIMESTAMP_PATTERN.matcher(line);
    matcher.find();
    return parseTimestamp(matcher.group());
  }

  static Duration parseEndTime(String line) {
    Matcher matcher = CUE_TIMESTAMP_PATTERN.matcher(line);
    matcher.find();
    matcher.find();
    return parseTimestamp(matcher.group());
  }

  private static Duration parseTimestamp(String timestamp) {
    String[] parts = timestamp.split("[:.]");
    if (parts.length == 4) {
      return Duration.ofHours(Long.parseLong(parts[0])).plusMinutes(Long.parseLong(parts[1]))
                     .plusSeconds(Long.parseLong(parts[2])).plusMillis(Long.parseLong(parts[3]));
    }
    return Duration.ofMinutes(Long.parseLong(parts[0])).plusSeconds(Long.parseLong(parts[1]))
                   .plusMillis(Long.parseLong(parts[2]));
  }
}
