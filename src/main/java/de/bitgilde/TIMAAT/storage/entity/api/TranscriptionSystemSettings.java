package de.bitgilde.TIMAAT.storage.entity.api;

import de.bitgilde.TIMAAT.model.FIPOP.TranscriptionModel;
import jakarta.annotation.Nullable;

/**
 * Snapshot of the transcription-related fields of the singleton system settings row, used as a
 * return type by {@link de.bitgilde.TIMAAT.storage.entity.SystemSettingStorage} so callers do not
 * have to hold on to a managed JPA entity outside the storage transaction.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-05-24
 */
public record TranscriptionSystemSettings(boolean autoTranscribeUploads,
                                          @Nullable TranscriptionModel defaultTranscriptionModel) {
}
