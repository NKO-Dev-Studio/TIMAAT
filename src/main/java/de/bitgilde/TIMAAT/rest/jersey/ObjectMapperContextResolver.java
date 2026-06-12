package de.bitgilde.TIMAAT.rest.jersey;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

/**
 * This {@link jakarta.ws.rs.ext.ContextResolver} is responsible to provide a custom
 * configured {@link com.fasterxml.jackson.databind.ObjectMapper} instance to jersey.
 *
 * @author Nico Kotlenga
 * @since 12.12.25
 */
@Provider
public class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {

  private final ObjectMapper objectMapper;

  public ObjectMapperContextResolver() {
    this.objectMapper = JsonMapper.builder().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                                  .addModule(new JavaTimeModule())
                                  .disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                                  .disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS).build();
  }

  @Override
  public ObjectMapper getContext(Class<?> type) {
    return objectMapper;
  }
}
