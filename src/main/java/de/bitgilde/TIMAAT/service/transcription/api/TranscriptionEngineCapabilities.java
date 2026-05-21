package de.bitgilde.TIMAAT.service.transcription.api;

import java.util.Collection;

/**
 * Snapshot of an engine currently offered by the connected speech-to-text-service together with the
 * model identifiers it provides.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 16.05.26
 */
public record TranscriptionEngineCapabilities(
    String engineIdentifier,
    String engineName,
    Collection<String> modelIdentifiers) {}
