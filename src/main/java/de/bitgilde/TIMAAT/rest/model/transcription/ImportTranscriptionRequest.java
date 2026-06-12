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
public class ImportTranscriptionRequest {

  @FormDataParam("vttFile")
  private InputStream vttFile;

  @FormDataParam("transcriptionName")
  private String transcriptionName;

  public InputStream getVttFile() {
    return vttFile;
  }

  public void setVttFile(InputStream vttFile) {
    this.vttFile = vttFile;
  }

  public String getTranscriptionName() {
    return transcriptionName;
  }

  public void setTranscriptionName(String transcriptionName) {
    this.transcriptionName = transcriptionName;
  }
}
