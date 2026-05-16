package de.bitgilde.TIMAAT.service.transcription;

import de.bitgilde.TIMAAT.model.FIPOP.Transcription;
import de.bitgilde.TIMAAT.model.FIPOP.TranscriptionModel;
import de.bitgilde.TIMAAT.service.task.TaskService;
import de.bitgilde.TIMAAT.service.task.api.MediumAudioAnalysisTask.SupportedMediumType;
import de.bitgilde.TIMAAT.service.task.api.Task;
import de.bitgilde.TIMAAT.service.task.api.TaskState;
import de.bitgilde.TIMAAT.service.task.api.TaskType;
import de.bitgilde.TIMAAT.service.task.api.TranscriptionMediumPreparationTask;
import de.bitgilde.TIMAAT.service.task.storage.TaskStateUpdater;
import de.bitgilde.TIMAAT.service.transcription.api.GenerateTranscriptionConfiguration;
import de.bitgilde.TIMAAT.service.transcription.api.TranscriptionEngineCapabilities;
import de.bitgilde.TIMAAT.storage.api.PagingParameter;
import de.bitgilde.TIMAAT.storage.api.SortingParameter;
import de.bitgilde.TIMAAT.storage.entity.SystemSettingStorage;
import de.bitgilde.TIMAAT.storage.entity.transcription.TranscriptionStorage;
import de.bitgilde.TIMAAT.storage.entity.transcription.api.TranscriptionFilterCriteria;
import de.bitgilde.TIMAAT.storage.entity.transcription.api.TranscriptionState;
import de.bitgilde.TIMAAT.storage.entity.transcription.api.TranscriptionType;
import de.bitgilde.TIMAAT.storage.file.AudioContainingMediumFileStorage;
import de.bitgilde.TIMAAT.storage.file.AudioFileStorage;
import de.bitgilde.TIMAAT.storage.file.TemporaryFileStorage;
import de.bitgilde.TIMAAT.storage.file.TemporaryFileStorage.TemporaryFile;
import de.bitgilde.TIMAAT.storage.file.TranscriptionFileStorage;
import de.bitgilde.TIMAAT.storage.file.VideoFileStorage;
import jakarta.inject.Inject;
import studio.nkodev.stt.client.SpeechToTextServiceClient;
import studio.nkodev.stt.client.api.SpeechToTextEngine;
import studio.nkodev.stt.client.api.SpeechToTextEngineOutputFormat;
import studio.nkodev.stt.client.api.SpeechToTextTaskState;
import studio.nkodev.stt.client.api.SpeechToTextTaskStateConsumer;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service responsible to manage Transcriptions. This includes the generation, modifying, and deletion.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 12.05.26
 */
public class TranscriptionService implements TaskStateUpdater, SpeechToTextTaskStateConsumer {

  private static final Logger logger = Logger.getLogger(TranscriptionService.class.getName());

  private static final SpeechToTextEngineOutputFormat engineOutputFormat = SpeechToTextEngineOutputFormat.SRT;

  private final TranscriptionStorage transcriptionStorage;
  private final SystemSettingStorage systemSettingStorage;
  private final AudioFileStorage audioFileStorage;
  private final VideoFileStorage videoFileStorage;
  private final TaskService taskService;
  private final SpeechToTextServiceClient speechToTextServiceClient;
  private final TemporaryFileStorage temporaryFileStorage;
  private final TranscriptionFileStorage transcriptionFileStorage;

  @Inject
  public TranscriptionService(TranscriptionStorage transcriptionStorage, SystemSettingStorage systemSettingStorage, AudioFileStorage audioFileStorage, VideoFileStorage videoFileStorage, TaskService taskService, SpeechToTextServiceClient speechToTextServiceClient, TemporaryFileStorage temporaryFileStorage, TranscriptionFileStorage transcriptionFileStorage) {
    this.transcriptionStorage = transcriptionStorage;
    this.systemSettingStorage = systemSettingStorage;
    this.audioFileStorage = audioFileStorage;
    this.videoFileStorage = videoFileStorage;
    this.taskService = taskService;
    this.speechToTextServiceClient = speechToTextServiceClient;
    this.temporaryFileStorage = temporaryFileStorage;
    this.transcriptionFileStorage = transcriptionFileStorage;

    resumeMonitoringOfActiveTranscriptions();
    verifyDefaultModelStillAvailable();
  }

  /**
   * @return the engines (with their models) currently offered by the connected speech-to-text-service
   */
  public Collection<TranscriptionEngineCapabilities> getAvailableEngineCapabilities() {
    return speechToTextServiceClient.getAvailableEngines().stream()
        .map(engine -> new TranscriptionEngineCapabilities(
            engine.engineIdentifier(),
            engine.engineName(),
            List.copyOf(engine.modelIdentifiers())))
        .toList();
  }

  public void createTranscription(GenerateTranscriptionConfiguration generateTranscriptionConfiguration) {
    int mediumId = generateTranscriptionConfiguration.mediumId();
    String engineIdentifier = generateTranscriptionConfiguration.engineIdentifier();
    String modelIdentifier = generateTranscriptionConfiguration.modelIdentifier();

    SpeechToTextEngine engine = validateAndUpsertEngineModel(engineIdentifier, modelIdentifier);

    SupportedMediumType supportedMediumType = transcriptionStorage.determineSupportedMediumType(mediumId);
    AudioContainingMediumFileStorage fileStorage = getFileStorage(supportedMediumType);
    Optional<Path> monoFile = fileStorage.getPathToAudioMonoFile(mediumId);


    Transcription createdTranscription = null;
    try {
      if (monoFile.isPresent()) {
        logger.log(Level.INFO, "Mono file available for medium {0}. Creating transcription in PENDING state.",
                mediumId);
        createdTranscription = transcriptionStorage.createTranscription(mediumId, engineIdentifier, modelIdentifier,
                TranscriptionState.PENDING);
        startTranscriptionTask(createdTranscription.getId(), monoFile.get(), engineIdentifier, modelIdentifier);
      }
      else {
        logger.log(Level.INFO, "No mono file for medium {0}. Creating transcription in WAITING_FOR_PREPARATION state.",
                mediumId);
        createdTranscription = transcriptionStorage.createTranscription(mediumId, engineIdentifier, modelIdentifier,
                TranscriptionState.WAITING_FOR_PREPARATION);
        taskService.executeTranscriptionMediumPreparationTask(mediumId, supportedMediumType);
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error while creating transcription for medium {0}. Reason: {1}",
              new Object[]{mediumId, e});
      if (createdTranscription != null) {
        transcriptionStorage.updateTranscriptionState(createdTranscription.getId(), TranscriptionState.FAILED);
      }
      verifyDefaultModelStillAvailable();
    }

  }

  private SpeechToTextEngine validateAndUpsertEngineModel(String engineIdentifier, String modelIdentifier) {
    SpeechToTextEngine engine = speechToTextServiceClient.getAvailableEngines().stream()
        .filter(candidate -> candidate.engineIdentifier().equals(engineIdentifier))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "Engine '" + engineIdentifier + "' is not provided by the connected speech-to-text-service"));

    if (!engine.modelIdentifiers().contains(modelIdentifier)) {
      throw new IllegalArgumentException(
          "Model '" + modelIdentifier + "' is not provided by engine '" + engineIdentifier + "'");
    }

    transcriptionStorage.upsertEngine(engineIdentifier, engine.engineName());
    transcriptionStorage.upsertModel(engineIdentifier, modelIdentifier);

    return engine;
  }

  private void startTranscriptionTask(int transcriptionId, Path monoFile, String engineIdentifier, String modelIdentifier) {
    long sttTaskId = speechToTextServiceClient.startSpeechToTextTask(monoFile, engineIdentifier, modelIdentifier, engineOutputFormat);
    transcriptionStorage.updateTranscriptionTaskId(transcriptionId, sttTaskId);
    speechToTextServiceClient.observeTask(sttTaskId, this);
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

    Collection<Transcription> transcriptions = transcriptionStorage.updateTranscriptionsStateOfPreparationTask(transcriptionMediumPreparationTask.getMediumId(),
            transcriptionState);

    if(TranscriptionState.PENDING.equals(transcriptionState)) {
      SupportedMediumType supportedMediumType = transcriptionStorage.determineSupportedMediumType(transcriptionMediumPreparationTask.getMediumId());
      AudioContainingMediumFileStorage audioContainingMediumFileStorage = getFileStorage(supportedMediumType);
      Optional<Path> monoAudioFile = audioContainingMediumFileStorage.getPathToAudioMonoFile(transcriptionMediumPreparationTask.getMediumId());

      if(monoAudioFile.isPresent()){
        for(Transcription transcription : transcriptions){
          try{
            String engineIdentifier = transcription.getTranscriptionModel().getId().getEngineIdentifier();
            String modelIdentifier = transcription.getTranscriptionModel().getId().getModelIdentifier();
            startTranscriptionTask(transcription.getId(), monoAudioFile.get(), engineIdentifier, modelIdentifier);
          }catch (Exception e){
            logger.log(Level.SEVERE, "Error while creating transcription task for transcription {0}. Reason: {1}", new Object[]{transcription.getId(), e});
          }
        }
      }else {
        logger.log(Level.SEVERE, "No mono audio file found for medium {0}. Cannot start transcription task", transcriptionMediumPreparationTask.getMediumId());
        transcriptions.forEach(transcription -> transcriptionStorage.updateTranscriptionState(transcription.getId(), TranscriptionState.FAILED));
      }
    }
  }


  private AudioContainingMediumFileStorage getFileStorage(SupportedMediumType supportedMediumType) {
    return switch (supportedMediumType) {
      case AUDIO -> audioFileStorage;
      case VIDEO -> videoFileStorage;
    };
  }

  private void verifyDefaultModelStillAvailable() {
    try {
      Optional<TranscriptionModel> defaultModel = systemSettingStorage.getDefaultTranscriptionModel();
      if (defaultModel.isEmpty()) {
        return;
      }

      String engineIdentifier = defaultModel.get().getId().getEngineIdentifier();
      String modelIdentifier = defaultModel.get().getId().getModelIdentifier();

      boolean stillAvailable = speechToTextServiceClient.getAvailableEngines().stream()
          .filter(engine -> engine.engineIdentifier().equals(engineIdentifier))
          .anyMatch(engine -> engine.modelIdentifiers().contains(modelIdentifier));

      if (!stillAvailable) {
        logger.log(Level.WARNING,
            "Default transcription model {0}/{1} is no longer provided by the speech-to-text-service. Clearing default.",
            new Object[]{engineIdentifier, modelIdentifier});
        systemSettingStorage.clearDefaultTranscriptionModel();
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "Could not verify availability of the default transcription model", e);
    }
  }

  private void resumeMonitoringOfActiveTranscriptions() {
    logger.info("Resuming monitoring of uncompleted speech-to-text tasks");
    TranscriptionFilterCriteria transcriptionFilterCriteria = new TranscriptionFilterCriteria.Builder().transcriptionStates(
            Set.of(TranscriptionState.PENDING)).transcriptionTypes(Set.of(TranscriptionType.GENERATED)).build();
    transcriptionStorage.getEntriesAsStream(transcriptionFilterCriteria, PagingParameter.NO_PAGING,
            SortingParameter.defaultSortOrder(), null).forEach(transcription -> {
      if (transcription.getTranscriptionTaskId() != null) {
        speechToTextServiceClient.observeTask(transcription.getTranscriptionTaskId(), this);
      }
      else {
        logger.log(Level.WARNING, "Found pending transcription without task id. Transcription id: {0}",
                transcription.getId());
        transcriptionStorage.updateTranscriptionState(transcription.getId(),
                TranscriptionState.FAILED);
      }
    });
  }

  @Override
  public void onTaskStateChanged(long taskId, SpeechToTextTaskState speechToTextTaskState) {
    switch (speechToTextTaskState) {
      case DONE -> handleSpeechToTextTaskCompleted(taskId);
      case FAILED -> handleSpeechToTextTaskFailed(taskId);
    }
  }

  private void handleSpeechToTextTaskCompleted(long taskId) {
    logger.log(Level.FINE, "Speech to text task completed with id {0}", taskId);
    Optional<Integer> relatedTranscriptionId = transcriptionStorage.getTranscriptionIdRelatedToTranscriptionTask(
            taskId);

    if (relatedTranscriptionId.isPresent()) {
      try (TemporaryFile temporaryFile = temporaryFileStorage.createTemporaryFile()) {
        speechToTextServiceClient.saveResultsOfTask(taskId, temporaryFile.getTemporaryFilePath());
        transcriptionFileStorage.persistTranscription(temporaryFile.getTemporaryFilePath(),
                relatedTranscriptionId.get());
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Error while handling speech to text task completion", e);
        transcriptionStorage.updateTranscriptionState(relatedTranscriptionId.get(), TranscriptionState.FAILED);
      }
    }
    else {
      logger.log(Level.WARNING, "No transcription id found for transcription task with id {0}", taskId);
    }
  }

  private void handleSpeechToTextTaskFailed(long taskId) {
    Optional<Integer> relatedTranscriptionId = transcriptionStorage.getTranscriptionIdRelatedToTranscriptionTask(
            taskId);
    if (relatedTranscriptionId.isPresent()) {
      transcriptionStorage.updateTranscriptionState(relatedTranscriptionId.get(), TranscriptionState.FAILED);
    }
    else {
      logger.log(Level.WARNING, "No transcription id found for transcription task with id {0}", taskId);
    }
  }
}
