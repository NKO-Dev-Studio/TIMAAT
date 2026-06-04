package de.bitgilde.TIMAAT.rest.model.systemsettings;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Wire representation of the transcription-related system settings. {@code featureEnabled} signals
 * whether the deployment offers speech-to-text at all (driven by the {@code stt.enabled} property);
 * when {@code false} the remaining fields still reflect the persisted preferences but will not have
 * any effect until the feature is re-enabled.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-05-24
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record TranscriptionSettingsDto(boolean featureEnabled, boolean autoTranscribeUploads,
                                       TranscriptionDefaultModelDto transcriptionDefaultModel) {
}
