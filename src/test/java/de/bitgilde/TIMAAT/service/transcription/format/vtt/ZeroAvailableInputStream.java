package de.bitgilde.TIMAAT.service.transcription.format.vtt;

import java.io.FilterInputStream;
import java.io.InputStream;

/**
 * Wraps an {@link InputStream} and always reports zero available bytes. This mimics the behavior of streams whose
 * content is lazily pulled on read, like the multipart form data part streams provided by Jersey, where
 * {@link InputStream#available()} returns 0 even though reading would return data.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 12.06.26
 */
class ZeroAvailableInputStream extends FilterInputStream {

  ZeroAvailableInputStream(InputStream inputStream) {
    super(inputStream);
  }

  /**
   * Reports that no bytes are available without blocking, regardless of the state of the wrapped stream.
   *
   * @return always 0
   */
  @Override
  public int available() {
    return 0;
  }
}
