package de.bitgilde.TIMAAT.rest.model.systemsettings;

import jakarta.annotation.Nullable;

/**
 * Request payload for updating the transcription-related system settings. {@code autoTranscribeUploads}
 * is required; {@code defaultEngineIdentifier} and {@code defaultModelIdentifier} must either both be
 * set (to configure a default model) or both be {@code null} (to clear the default).
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-05-24
 */
public record UpdateTranscriptionSettingsRequest(boolean autoTranscribeUploads,
                                                 @Nullable String defaultEngineIdentifier,
                                                 @Nullable String defaultModelIdentifier) {
}
