package de.bitgilde.TIMAAT.storage.file;

import de.bitgilde.TIMAAT.PropertyConstants;
import de.bitgilde.TIMAAT.PropertyManagement;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FileStorage for transcriptions.
 * Transcriptions will be persisted inside a dedicated folder. The files are named using the following schema:
 * {transcriptionId}.srt
 *
 * @author Nico Kotlenga (nico@nko-dev.studio) 
 * @since 16.05.26
 */
public class TranscriptionFileStorage {
  private static final Logger logger = Logger.getLogger(AudioFileStorage.class.getName());

  private final Path transcriptionStoragePath;

  @Inject
  public TranscriptionFileStorage(PropertyManagement propertyManagement) throws IOException {
    this.transcriptionStoragePath = Path.of(propertyManagement.getProp(PropertyConstants.STORAGE_LOCATION))
                                        .resolve("transcription");
    Files.createDirectories(transcriptionStoragePath);
  }


  public Path persistTranscription(Path transcriptionFile, int transcriptionId) throws IOException {
    logger.log(Level.FINE, "Persisting transcription having id {0}", transcriptionId);

    Path transcriptionFilePath = createTranscriptionPath(transcriptionId);
    Files.move(transcriptionFile, transcriptionFilePath, StandardCopyOption.REPLACE_EXISTING);

    return transcriptionFilePath;
  }

  public Optional<Path> getPathToTranscription(int transcriptionId) {
    Path transcriptionPath = createTranscriptionPath(transcriptionId);

    if (Files.exists(transcriptionPath)) {
      return Optional.of(transcriptionPath);
    }

    return Optional.empty();
  }

  private Path createTranscriptionPath(int transcriptionId) {
    return transcriptionStoragePath.resolve(transcriptionId + ".srt");
  }
}
