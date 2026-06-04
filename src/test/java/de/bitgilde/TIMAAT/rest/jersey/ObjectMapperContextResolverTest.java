package de.bitgilde.TIMAAT.rest.jersey;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Verifies that the {@link ObjectMapperContextResolver} provides an
 * {@link com.fasterxml.jackson.databind.ObjectMapper} capable of (de)serializing
 * {@code java.time} types and emitting ISO-8601 instead of numeric timestamps.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 29.05.26
 */
class ObjectMapperContextResolverTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    this.objectMapper = new ObjectMapperContextResolver().getContext(Object.class);
  }

  @Test
  void serializesInstantWithoutFailing() {
    assertThatNoException().isThrownBy(() -> objectMapper.writeValueAsString(Instant.parse("2026-05-29T10:15:30Z")));
  }

  @Test
  void serializesInstantAsNumericTimeStamp() throws Exception {
    String json = objectMapper.writeValueAsString(Instant.parse("2026-05-29T10:15:30Z"));

    assertThat(json).isEqualTo("1780049730000");
  }

  @Test
  void deserializesInstantFromIso8601String() throws Exception {
    Instant instant = objectMapper.readValue("\"2026-05-29T10:15:30Z\"", Instant.class);

    assertThat(instant).isEqualTo(Instant.parse("2026-05-29T10:15:30Z"));
  }
}