package de.bitgilde.TIMAAT.service.transcription;

import de.bitgilde.TIMAAT.PropertyConstants;
import de.bitgilde.TIMAAT.PropertyManagement;
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
import de.bitgilde.TIMAAT.service.transcription.exception.TranscriptionServiceException;
import de.bitgilde.TIMAAT.storage.api.PagingParameter;
import de.bitgilde.TIMAAT.storage.api.SortingParameter;
import de.bitgilde.TIMAAT.storage.entity.SystemSettingStorage;
import de.bitgilde.TIMAAT.storage.entity.medium.MediumStorage;
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
import jakarta.inject.Provider;
import studio.nkodev.stt.client.SpeechToTextServiceClient;
import studio.nkodev.stt.client.api.SpeechToTextEngine;
import studio.nkodev.stt.client.api.SpeechToTextEngineOutputFormat;
import studio.nkodev.stt.client.api.SpeechToTextTaskState;
import studio.nkodev.stt.client.api.SpeechToTextTaskStateConsumer;
import studio.nkodev.stt.client.config.SpeechToTextServiceClientAuthenticationConfigurationBuilder;
import studio.nkodev.stt.client.config.SpeechToTextServiceClientConfiguration;
import studio.nkodev.stt.client.config.SpeechToTextServiceClientConfigurationBuilder;
import studio.nkodev.stt.client.config.SpeechToTextTransferConfiguration;
import studio.nkodev.stt.client.config.SpeechToTextTransferConfigurationFactory;
import studio.nkodev.stt.client.config.SpeechToTextTransferType;

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
  private final Provider<TaskService> taskServiceProvider;
  private final SpeechToTextServiceClient speechToTextServiceClient;
  private final TemporaryFileStorage temporaryFileStorage;
  private final TranscriptionFileStorage transcriptionFileStorage;
  private final MediumStorage mediumStorage;

  @Inject
  public TranscriptionService(TranscriptionStorage transcriptionStorage, SystemSettingStorage systemSettingStorage, AudioFileStorage audioFileStorage, VideoFileStorage videoFileStorage, Provider<TaskService> taskServiceProvider, TemporaryFileStorage temporaryFileStorage, TranscriptionFileStorage transcriptionFileStorage, MediumStorage mediumStorage, PropertyManagement propertyManagement) {
    this.transcriptionStorage = transcriptionStorage;
    this.systemSettingStorage = systemSettingStorage;
    this.audioFileStorage = audioFileStorage;
    this.videoFileStorage = videoFileStorage;
    this.taskServiceProvider = taskServiceProvider;
    this.temporaryFileStorage = temporaryFileStorage;
    this.transcriptionFileStorage = transcriptionFileStorage;
    this.mediumStorage = mediumStorage;

    this.speechToTextServiceClient = initSpeechToTextServiceClient(propertyManagement);
    resumeMonitoringOfActiveTranscriptions();
    verifyDefaultModelStillAvailable();
  }


  private SpeechToTextServiceClient initSpeechToTextServiceClient(PropertyManagement propertyManagement) {
    logger.info("Initializing SpeechToTextServiceClient");

    String sttHost = propertyManagement.getProp(PropertyConstants.SPEECH_TO_TEXT_HOST);
    int sttPort = Integer.parseInt(propertyManagement.getProp(PropertyConstants.SPEECH_TO_TEXT_PORT));
    SpeechToTextTransferType transferType = SpeechToTextTransferType.valueOf(
            propertyManagement.getProp(PropertyConstants.SPEECH_TO_TEXT_TRANSFER_TYPE));
    Path trustedServerCertificatePath = Path.of(
            propertyManagement.getProp(PropertyConstants.SPEECH_TO_TEXT_TRUSTED_SERVER_CERTIFICATE_PATH));
    boolean authEnabled = Boolean.parseBoolean(
            propertyManagement.getProp(PropertyConstants.SPEECH_TO_TEXT_AUTH_ENABLED));


    SpeechToTextTransferConfiguration audioFileTransferConfiguration;
    SpeechToTextTransferConfiguration resultTransferConfiguration;

    if (SpeechToTextTransferType.STREAMING.equals(transferType)) {
      audioFileTransferConfiguration = SpeechToTextTransferConfigurationFactory.streaming();
      resultTransferConfiguration = SpeechToTextTransferConfigurationFactory.streaming();
    }
    else {
      Path sharedAudio = Path.of(
              propertyManagement.getProp(PropertyConstants.SPEECH_TO_TEXT_TRANSFER_SHARED_AUDIO_STORAGE_PATH));
      Path sharedResult = Path.of(
              propertyManagement.getProp(PropertyConstants.SPEECH_TO_TEXT_TRANSFER_SHARED_RESULT_STORAGE_PATH));

      audioFileTransferConfiguration = SpeechToTextTransferConfigurationFactory.sharedStorage(sharedAudio);
      resultTransferConfiguration = SpeechToTextTransferConfigurationFactory.sharedStorage(sharedResult);
    }

    SpeechToTextServiceClientConfigurationBuilder configurationBuilder = new SpeechToTextServiceClientConfigurationBuilder(
            sttHost, sttPort, trustedServerCertificatePath, audioFileTransferConfiguration,
            resultTransferConfiguration);

    if (authEnabled) {
      logger.log(Level.INFO, "Initialize SpeechToTextServiceClient with authentication");

      Path authCertificatePath = Path.of(
              propertyManagement.getProp(PropertyConstants.SPEECH_TO_TEXT_AUTH_CERTIFICATE_PATH));
      Path authPrivateKeyPath = Path.of(
              propertyManagement.getProp(PropertyConstants.SPEECH_TO_TEXT_AUTH_PRIVATE_KEY_PATH));

      SpeechToTextServiceClientAuthenticationConfigurationBuilder authenticationConfigurationBuilder = new SpeechToTextServiceClientAuthenticationConfigurationBuilder(
              authCertificatePath, authPrivateKeyPath);
      String authPrivateKeyPassword = propertyManagement.getProp(
              PropertyConstants.SPEECH_TO_TEXT_AUTH_PRIVATE_KEY_PASSWORD);
      if (authPrivateKeyPassword != null && !authPrivateKeyPassword.isEmpty()) {
        authenticationConfigurationBuilder.privateKeyPassword(authPrivateKeyPassword);
      }

      configurationBuilder.authenticationConfiguration(authenticationConfigurationBuilder.build());
    }

    SpeechToTextServiceClientConfiguration configuration = configurationBuilder.build();
    return new SpeechToTextServiceClient(configuration);
  }

  /**
   * @return the engines (with their models) currently offered by the connected speech-to-text-service
   */
  public Collection<TranscriptionEngineCapabilities> getAvailableEngineCapabilities() {
    return speechToTextServiceClient.getAvailableEngines().stream()
                                    .map(engine -> new TranscriptionEngineCapabilities(engine.engineIdentifier(),
                                            engine.engineName(), List.copyOf(engine.modelIdentifiers()))).toList();
  }

  /**
   * Creates a new transcription for the medium described by the given configuration. If the medium is already prepared
   * (i.e. a mono audio file exists), a speech-to-text task is started immediately. Otherwise the transcription is
   * persisted in {@link TranscriptionState#WAITING_FOR_PREPARATION} and a preparation task is scheduled; the
   * speech-to-text task is then started once preparation completes.
   *
   * @param generateTranscriptionConfiguration configuration describing the medium and the engine/model to use
   * @throws IllegalArgumentException      if the requested engine is not offered by the connected speech-to-text-service
   *                                       or the requested model is not provided by that engine
   * @throws TranscriptionServiceException if the transcription could not be created (e.g. underlying task scheduling
   *                                       or storage access failed)
   */
  public void createTranscription(GenerateTranscriptionConfiguration generateTranscriptionConfiguration) throws TranscriptionServiceException {
    int mediumId = generateTranscriptionConfiguration.mediumId();
    String engineIdentifier = generateTranscriptionConfiguration.engineIdentifier();
    String modelIdentifier = generateTranscriptionConfiguration.modelIdentifier();

    validateAndUpsertEngineModel(engineIdentifier, modelIdentifier);

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
        taskServiceProvider.get().executeTranscriptionMediumPreparationTask(mediumId, supportedMediumType);
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error while creating transcription for medium {0}. Reason: {1}",
              new Object[]{mediumId, e});
      if (createdTranscription != null) {
        transcriptionStorage.updateTranscriptionState(createdTranscription.getId(), TranscriptionState.FAILED);
      }
      verifyDefaultModelStillAvailable();
      throw new TranscriptionServiceException("Failed to create transcription for medium " + mediumId, e);
    }
    trySetTranscriptionAsDefaultOfMedium(mediumId, createdTranscription.getId());
  }

  private void trySetTranscriptionAsDefaultOfMedium(int mediumId, int transcriptionId) {
    try {
      boolean updated = mediumStorage.setDefaultTranscriptionIfAbsent(mediumId, transcriptionId);
      if (updated) {
        logger.log(Level.FINE, "Set transcription {0} as default for medium {1}",
                new Object[]{transcriptionId, mediumId});
      }
    } catch (Exception e) {
      logger.log(Level.WARNING,
              "Could not set transcription {0} as default for medium {1}; transcription creation will not be failed",
              new Object[]{transcriptionId, mediumId});
      logger.log(Level.WARNING, "Reason", e);
    }
  }

  private SpeechToTextEngine validateAndUpsertEngineModel(String engineIdentifier, String modelIdentifier) {
    SpeechToTextEngine engine = speechToTextServiceClient.getAvailableEngines().stream()
                                                         .filter(candidate -> candidate.engineIdentifier()
                                                                                       .equals(engineIdentifier))
                                                         .findFirst().orElseThrow(() -> new IllegalArgumentException(
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
    long sttTaskId = speechToTextServiceClient.startSpeechToTextTask(monoFile, engineIdentifier, modelIdentifier,
            engineOutputFormat);
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

    Collection<Transcription> transcriptions = transcriptionStorage.updateTranscriptionsStateOfPreparationTask(
            transcriptionMediumPreparationTask.getMediumId(), transcriptionState);

    if (TranscriptionState.PENDING.equals(transcriptionState)) {
      SupportedMediumType supportedMediumType = transcriptionStorage.determineSupportedMediumType(
              transcriptionMediumPreparationTask.getMediumId());
      AudioContainingMediumFileStorage audioContainingMediumFileStorage = getFileStorage(supportedMediumType);
      Optional<Path> monoAudioFile = audioContainingMediumFileStorage.getPathToAudioMonoFile(
              transcriptionMediumPreparationTask.getMediumId());

      if (monoAudioFile.isPresent()) {
        for (Transcription transcription : transcriptions) {
          try {
            String engineIdentifier = transcription.getTranscriptionModel().getId().getEngineIdentifier();
            String modelIdentifier = transcription.getTranscriptionModel().getId().getModelIdentifier();
            startTranscriptionTask(transcription.getId(), monoAudioFile.get(), engineIdentifier, modelIdentifier);
          } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while creating transcription task for transcription {0}. Reason: {1}",
                    new Object[]{transcription.getId(), e});
          }
        }
      }
      else {
        logger.log(Level.SEVERE, "No mono audio file found for medium {0}. Cannot start transcription task",
                transcriptionMediumPreparationTask.getMediumId());
        transcriptions.forEach(transcription -> transcriptionStorage.updateTranscriptionState(transcription.getId(),
                TranscriptionState.FAILED));
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
                                                        .filter(engine -> engine.engineIdentifier()
                                                                                .equals(engineIdentifier)).anyMatch(
                      engine -> engine.modelIdentifiers().contains(modelIdentifier));

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
        transcriptionStorage.updateTranscriptionState(transcription.getId(), TranscriptionState.FAILED);
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
        transcriptionStorage.updateTranscriptionState(relatedTranscriptionId.get(), TranscriptionState.COMPLETED);
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
