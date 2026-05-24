package de.bitgilde.TIMAAT.rest.model.systemsettings;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;

/**
 * Wire representation of the currently configured default transcription engine/model pair. Both
 * identifier fields are {@code null} when no default has been configured yet.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-05-24
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record TranscriptionDefaultModelDto(@Nullable String engineIdentifier,
                                           @Nullable String modelIdentifier) {
}
