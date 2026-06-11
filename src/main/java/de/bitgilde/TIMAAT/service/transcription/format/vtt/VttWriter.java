package de.bitgilde.TIMAAT.service.transcription.format.vtt;

import de.bitgilde.TIMAAT.service.transcription.api.TranscriptionContent;
import de.bitgilde.TIMAAT.service.transcription.api.TranscriptionCue;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * This class serializes a {@link de.bitgilde.TIMAAT.service.transcription.api.TranscriptionContent}
 * representation back into a VTT file. It is the inverse of {@link VttParser}; content written by
 * this class can be parsed again by {@link VttParser} without loss.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-06-11
 */
public class VttWriter {

  /**
   * Writes the given {@link TranscriptionContent} as a VTT document into the supplied output
   * stream. The output starts with the {@code WEBVTT} header followed by one block per cue, each
   * consisting of the timestamp line ({@code HH:MM:SS.mmm --> HH:MM:SS.mmm}), the cue text and a
   * blank separator line. The output is UTF-8 encoded. The stream is flushed but <em>not</em>
   * closed, so the caller stays in control of the underlying resource (e.g. a temporary file).
   *
   * @param content      the transcription content to serialize
   * @param outputStream the stream the VTT document is written to
   * @throws IOException if writing to the stream fails
   */
  public void writeVttStream(TranscriptionContent content, OutputStream outputStream) throws IOException {
    Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
    writer.write("WEBVTT\n\n");

    for (TranscriptionCue cue : content.cues()) {
      writer.write(formatTimestamp(cue.startTime()) + " --> " + formatTimestamp(cue.endTime()) + "\n");
      writer.write(cue.cue() + "\n");
      writer.write("\n");
    }

    writer.flush();
  }

  /**
   * Formats a {@link Duration} as a VTT timestamp in the form {@code HH:MM:SS.mmm}.
   *
   * @param duration the duration to format
   * @return the formatted timestamp
   */
  private static String formatTimestamp(Duration duration) {
    return String.format("%02d:%02d:%02d.%03d", duration.toHoursPart(), duration.toMinutesPart(),
            duration.toSecondsPart(), duration.toMillisPart());
  }
}
