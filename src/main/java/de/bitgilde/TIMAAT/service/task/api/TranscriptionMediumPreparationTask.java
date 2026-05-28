package de.bitgilde.TIMAAT.service.task.api;

/**
 * This task will be executed before creating the actual transcription.
 * It prepares a medium file by extracting the audio layer and convert it to mono,
 * reducing the size of information which needed to get transferred to the speech-to-text-service
 *
 * @author Nico Kotlenga (nico@nko-dev.studio) 
 * @since 12.05.26
 */
public class TranscriptionMediumPreparationTask extends Task {

  private final int mediumId;
  private final MediumAudioAnalysisTask.SupportedMediumType mediumType;

  public TranscriptionMediumPreparationTask(int mediumId, MediumAudioAnalysisTask.SupportedMediumType mediumType) {
    super(TaskType.TRANSCRIPTION_MEDIUM_PREPARATION);
    this.mediumId = mediumId;
    this.mediumType = mediumType;
  }

  public int getMediumId() {
    return mediumId;
  }

  public MediumAudioAnalysisTask.SupportedMediumType getMediumType() {
    return mediumType;
  }
}
