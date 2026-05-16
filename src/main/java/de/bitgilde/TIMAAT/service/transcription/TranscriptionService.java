package de.bitgilde.TIMAAT.service.transcription;

import de.bitgilde.TIMAAT.service.task.api.Task;
import de.bitgilde.TIMAAT.service.task.api.TaskState;
import de.bitgilde.TIMAAT.service.task.api.TaskType;
import de.bitgilde.TIMAAT.service.task.api.TranscriptionMediumPreparationTask;
import de.bitgilde.TIMAAT.service.task.storage.TaskStateUpdater;
import de.bitgilde.TIMAAT.service.transcription.api.GenerateTranscriptionConfiguration;
import de.bitgilde.TIMAAT.sse.EntityUpdateEventService;
import de.bitgilde.TIMAAT.storage.api.PagingParameter;
import de.bitgilde.TIMAAT.storage.api.SortingParameter;
import de.bitgilde.TIMAAT.storage.entity.transcription.TranscriptionStorage;
import de.bitgilde.TIMAAT.storage.entity.transcription.api.TranscriptionFilterCriteria;
import de.bitgilde.TIMAAT.storage.entity.transcription.api.TranscriptionState;
import de.bitgilde.TIMAAT.storage.entity.transcription.api.TranscriptionType;
import jakarta.inject.Inject;

import java.util.Set;
import java.util.logging.Logger;

/**
 * Service responsible to manage Transcriptions. This includes the generation, modifying, and deletion.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 12.05.26
 */
public class TranscriptionService implements TaskStateUpdater {

  private static final Logger logger = Logger.getLogger(TranscriptionService.class.getName());

  private final TranscriptionStorage transcriptionStorage;

  @Inject
  public TranscriptionService(TranscriptionStorage transcriptionStorage, EntityUpdateEventService entityUpdateEventService) {
    this.transcriptionStorage = transcriptionStorage;

    resumeMonitoringOfActiveTranscriptions();
  }

  public void createTranscription(GenerateTranscriptionConfiguration generateTranscriptionConfiguration) {

  }

  @Override
  public TaskType getSupportedTaskType() {
    return TaskType.TRANSCRIPTION_MEDIUM_PREPARATION;
  }

  @Override
  public void updateTaskState(Task task, TaskState taskState) {
    updateTranscriptionMediumPreparationTaskState((TranscriptionMediumPreparationTask) task, taskState);
  }

  public void updateTranscriptionMediumPreparationTaskState(TranscriptionMediumPreparationTask transcriptionMediumPreparationTask, TaskState taskState) {
    logger.info("Updating transcription medium preparation task state");

    TranscriptionState transcriptionState = switch (taskState) {
      case PENDING -> TranscriptionState.WAITING_FOR_PREPARATION;
      case RUNNING -> TranscriptionState.PREPARING;
      case FAILED -> TranscriptionState.FAILED;
      case DONE -> TranscriptionState.PENDING;
    };

    transcriptionStorage.updateTranscriptionsStateOfPreparationTask(transcriptionMediumPreparationTask.getMediumId(), transcriptionState);
  }


  private void resumeMonitoringOfActiveTranscriptions() {
    logger.info("Resuming monitoring of uncompleted speech-to-text tasks");
    TranscriptionFilterCriteria transcriptionFilterCriteria = new TranscriptionFilterCriteria.Builder().transcriptionStates(
            Set.of(TranscriptionState.PENDING, TranscriptionState.COMPLETED)).transcriptionTypes(
            Set.of(TranscriptionType.GENERATED)).build();
    transcriptionStorage.getEntriesAsStream(transcriptionFilterCriteria, PagingParameter.NO_PAGING,
            SortingParameter.defaultSortOrder(), null).forEach(transcription -> {
      if (transcription.getTranscriptionTaskId() != null) {

      }
      else {

      }
    });
  }
}
