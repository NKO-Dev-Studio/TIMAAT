package de.bitgilde.TIMAAT.rest.model.transcription;

import org.glassfish.jersey.media.multipart.FormDataParam;

import java.io.InputStream;

/**
 * Defines the request payload used to import a transcription.
 * The import is expecting a VTT file as well as a name of the new generated transcription.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio) 
 * @since 12.06.26
 */
public record ImportTranscriptionRequest(@FormDataParam("vttFile") InputStream vttFile,
                                         @FormDataParam("transcriptionName") String transcriptionName) {
}
