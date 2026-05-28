package de.bitgilde.TIMAAT.service.task.execution;

import de.bitgilde.TIMAAT.processing.audio.FfmpegAudioEngine;
import de.bitgilde.TIMAAT.processing.audio.api.PcmMono16BitLittleEndian;
import de.bitgilde.TIMAAT.processing.audio.exception.AudioEngineException;
import de.bitgilde.TIMAAT.service.task.api.MediumAudioAnalysisTask.SupportedMediumType;
import de.bitgilde.TIMAAT.service.task.api.TranscriptionMediumPreparationTask;
import de.bitgilde.TIMAAT.service.task.exception.TaskExecutionException;
import de.bitgilde.TIMAAT.storage.file.AudioContainingMediumFileStorage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Prepares the audio file used as input for generated transcriptions.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 12.05.26
 */
public class TranscriptionMediumPreparationTaskExecutor extends TaskExecutor<TranscriptionMediumPreparationTask> {

  private static final Logger logger = Logger.getLogger(TranscriptionMediumPreparationTaskExecutor.class.getName());

  private final AudioContainingMediumFileStorage audioContainingMediumFileStorage;
  private final FfmpegAudioEngine audioEngine;

  public TranscriptionMediumPreparationTaskExecutor(TranscriptionMediumPreparationTask task, Map<SupportedMediumType, AudioContainingMediumFileStorage> audioContainingMediumFileStorageBySupportedMediumType, FfmpegAudioEngine audioEngine) {
    super(task);
    this.audioContainingMediumFileStorage = audioContainingMediumFileStorageBySupportedMediumType.get(
            task.getMediumType());
    this.audioEngine = audioEngine;
  }

  @Override
  public void execute() throws TaskExecutionException {
    int mediumId = task.getMediumId();
    logger.log(Level.INFO, "Executing transcription medium preparation task for medium having id {0}", mediumId);

    if (audioContainingMediumFileStorage.getPathToAudioMonoFile(mediumId).isPresent()) {
      logger.log(Level.INFO, "Audio mono file for medium having id {0} already exists", mediumId);
      return;
    }

    Optional<Path> pathToOriginalMediumFile = audioContainingMediumFileStorage.getPathToOriginalFile(mediumId);
    if (pathToOriginalMediumFile.isEmpty()) {
      throw new TaskExecutionException("Medium with id " + mediumId + " has no original file");
    }

    try (PcmMono16BitLittleEndian pcmAudioFile = audioEngine.convertAudioChannelsTo16BitLittleEndian(
            pathToOriginalMediumFile.get(), true)) {
      audioContainingMediumFileStorage.persistAudioMonoFile(pcmAudioFile.getAudioFilePath(), mediumId);
    } catch (AudioEngineException | IOException e) {
      throw new TaskExecutionException("Error while preparing transcription medium file", e);
    }

    logger.log(Level.INFO, "Finished transcription medium preparation task for medium having id {0}", mediumId);
  }
}
