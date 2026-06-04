package de.bitgilde.TIMAAT.service.transcription;

import de.bitgilde.TIMAAT.PropertyConstants;
import de.bitgilde.TIMAAT.PropertyManagement;
import de.bitgilde.TIMAAT.model.FIPOP.Transcription;
import de.bitgilde.TIMAAT.model.FIPOP.TranscriptionModel;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount;
import de.bitgilde.TIMAAT.service.task.TaskService;
import de.bitgilde.TIMAAT.service.task.api.MediumAudioAnalysisTask.SupportedMediumType;
import de.bitgilde.TIMAAT.service.task.api.Task;
import de.bitgilde.TIMAAT.service.task.api.TaskState;
import de.bitgilde.TIMAAT.service.task.api.TaskType;
import de.bitgilde.TIMAAT.service.task.api.TranscriptionMediumPreparationTask;
import de.bitgilde.TIMAAT.service.task.storage.TaskStateUpdater;
import de.bitgilde.TIMAAT.service.transcription.api.GenerateTranscriptionConfiguration;
import de.bitgilde.TIMAAT.service.transcription.api.TranscriptionEngine;
import de.bitgilde.TIMAAT.service.transcription.api.TranscriptionEngineModel;
import de.bitgilde.TIMAAT.service.transcription.exception.TranscriptionFeatureDisabledException;
import de.bitgilde.TIMAAT.service.transcription.exception.TranscriptionNotFoundException;
import de.bitgilde.TIMAAT.service.transcription.exception.TranscriptionServiceException;
import de.bitgilde.TIMAAT.storage.api.PagingParameter;
import de.bitgilde.TIMAAT.storage.api.SortingParameter;
import de.bitgilde.TIMAAT.storage.entity.SystemSettingStorage;
import de.bitgilde.TIMAAT.storage.entity.api.TranscriptionSystemSettings;
import de.bitgilde.TIMAAT.storage.entity.medium.MediumStorage;
import de.bitgilde.TIMAAT.storage.entity.medium.exception.MediumNotFoundException;
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
import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
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
import studio.nkodev.stt.client.exception.SpeechToTextServiceClientErrorType;

import java.io.Closeable;
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
public class TranscriptionService implements TaskStateUpdater, SpeechToTextTaskStateConsumer, Closeable {

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
  private final boolean featureEnabled;

  @Inject
  public TranscriptionService(TranscriptionStorage transcriptionStorage, SystemSettingStorage systemSettingStorage, AudioFileStorage audioFileStorage, VideoFileStorage videoFileStorage, Provider<TaskService> taskServiceProvider, TemporaryFileStorage temporaryFileStorage, TranscriptionFileStorage transcriptionFileStorage, MediumStorage mediumStorage, PropertyManagement propertyManagement) {
    this(transcriptionStorage, systemSettingStorage, audioFileStorage, videoFileStorage, taskServiceProvider,
            temporaryFileStorage, transcriptionFileStorage, mediumStorage,
            isFeatureEnabled(propertyManagement) ? initSpeechToTextServiceClient(propertyManagement) : null);
  }

  TranscriptionService(TranscriptionStorage transcriptionStorage, SystemSettingStorage systemSettingStorage, AudioFileStorage audioFileStorage, VideoFileStorage videoFileStorage, Provider<TaskService> taskServiceProvider, TemporaryFileStorage temporaryFileStorage, TranscriptionFileStorage transcriptionFileStorage, MediumStorage mediumStorage, @jakarta.annotation.Nullable SpeechToTextServiceClient speechToTextServiceClient) {
    this.transcriptionStorage = transcriptionStorage;
    this.systemSettingStorage = systemSettingStorage;
    this.audioFileStorage = audioFileStorage;
    this.videoFileStorage = videoFileStorage;
    this.taskServiceProvider = taskServiceProvider;
    this.temporaryFileStorage = temporaryFileStorage;
    this.transcriptionFileStorage = transcriptionFileStorage;
    this.mediumStorage = mediumStorage;
    this.speechToTextServiceClient = speechToTextServiceClient;
    this.featureEnabled = speechToTextServiceClient != null;

    if (featureEnabled) {
      resumeMonitoringOfActiveTranscriptions();
      verifyDefaultModelStillAvailable();
    }
    else {
      logger.info(
              "Speech-to-text feature is disabled (" + PropertyConstants.SPEECH_TO_TEXT_ENABLED.key() + "=false). " + "TranscriptionService is running in inactive mode.");
    }
  }

  /**
   * Releases the underlying {@link SpeechToTextServiceClient} when the service is
   * destroyed. The client owns a gRPC channel whose non-daemon Netty event-loop
   * threads would otherwise keep the JVM alive and block a clean web application
   * shutdown. When the feature is disabled no client exists, so nothing happens.
   * Any error raised while closing is logged and swallowed so that it cannot
   * disrupt the remaining shutdown steps.
   */
  @PreDestroy
  @Override
  public void close() {
    if (speechToTextServiceClient == null) {
      return;
    }
    try {
      logger.info("Shutting down speech-to-text service client");
      speechToTextServiceClient.close();
    } catch (RuntimeException e) {
      logger.log(Level.WARNING, "Failed to close the speech-to-text service client during shutdown.", e);
    }
  }

  private static boolean isFeatureEnabled(PropertyManagement propertyManagement) {
    return Boolean.parseBoolean(propertyManagement.getProp(PropertyConstants.SPEECH_TO_TEXT_ENABLED));
  }

  /**
   * @return {@code true} if the speech-to-text feature is enabled for this deployment (i.e. property
   * {@code stt.enabled} is set to {@code true} in the TIMAAT properties file); {@code false} otherwise.
   * When disabled, no connection to a speech-to-text-service is established and operations requiring it
   * (engine discovery, transcription creation) become no-ops or throw.
   */
  public boolean isFeatureEnabled() {
    return featureEnabled;
  }


  private static SpeechToTextServiceClient initSpeechToTextServiceClient(PropertyManagement propertyManagement) {
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
   * Returns the engines (with their models) currently offered by the connected speech-to-text-service.
   * Each model carries a flag indicating whether it is the deployment's configured default, derived
   * from the persisted system settings, so clients can render selectors without issuing a separate
   * request for the default model. If the feature is disabled for this deployment, an empty
   * collection is returned and no connection to the speech-to-text-service is attempted.
   *
   * @return the available engine capabilities with default-flag per model, or an empty collection
   * when the feature is disabled
   */
  public Collection<TranscriptionEngine> getAvailableEngineCapabilities() {
    if (!featureEnabled) {
      return List.of();
    }
    Optional<TranscriptionModel> defaultModel = systemSettingStorage.getDefaultTranscriptionModel();
    String defaultEngineIdentifier = defaultModel.map(model -> model.getId().getEngineIdentifier()).orElse(null);
    String defaultModelIdentifier = defaultModel.map(model -> model.getId().getModelIdentifier()).orElse(null);

    return speechToTextServiceClient.getAvailableEngines().stream()
                                    .map(engine -> new TranscriptionEngine(engine.engineIdentifier(),
                                            engine.engineName(), engine.modelIdentifiers().stream()
                                                                       .map(modelIdentifier -> new TranscriptionEngineModel(
                                                                               modelIdentifier,
                                                                               engine.engineIdentifier()
                                                                                     .equals(defaultEngineIdentifier) && modelIdentifier.equals(
                                                                                       defaultModelIdentifier)))
                                                                       .toList())).toList();
  }

  /**
   * Reads the current transcription-related system settings (auto-transcribe-uploads flag and default engine/model).
   *
   * @return a snapshot of the current settings, never {@code null}
   */
  public TranscriptionSystemSettings getTranscriptionSystemSettings() {
    return systemSettingStorage.getTranscriptionSystemSettings();
  }

  /**
   * Updates the transcription-related system settings. When a default engine/model is requested, the pair is
   * validated against the live speech-to-text-service offering and the corresponding {@code TranscriptionEngine}
   * and {@code TranscriptionModel} rows are upserted so they can be referenced. Engine and model identifiers
   * must either both be {@code null} (clear the default) or both be set.
   *
   * @param autoTranscribeUploads whether newly uploaded media should be transcribed automatically
   * @param defaultEngineIdentifier identifier of the engine to use as default, or {@code null} to clear the default
   * @param defaultModelIdentifier identifier of the model to use as default, or {@code null} to clear the default
   * @param editingUser the user performing the change, used to stamp {@code lastEditedByUserAccount}, may be {@code null}
   * @throws IllegalArgumentException if exactly one of the engine/model identifiers is {@code null}, or if the
   *                                  requested engine/model pair is not offered by the connected speech-to-text-service
   * @throws TranscriptionFeatureDisabledException if a default engine/model is requested while the feature is disabled
   */
  public void updateTranscriptionSystemSettings(boolean autoTranscribeUploads, @Nullable String defaultEngineIdentifier, @Nullable String defaultModelIdentifier, @Nullable UserAccount editingUser) throws TranscriptionFeatureDisabledException {
    boolean engineSet = defaultEngineIdentifier != null;
    boolean modelSet = defaultModelIdentifier != null;
    if (engineSet != modelSet) {
      throw new IllegalArgumentException(
              "Default engine and model identifiers must either both be set or both be null");
    }

    if (engineSet) {
      if (!featureEnabled) {
        throw new TranscriptionFeatureDisabledException(
                "Cannot configure a default transcription model while the speech-to-text feature is disabled");
      }
      SpeechToTextEngine speechToTextEngine = getSpeechToTextEngineHavingModel(defaultEngineIdentifier,
              defaultModelIdentifier);
      upsertEngineModel(speechToTextEngine, defaultModelIdentifier);
    }

    systemSettingStorage.updateTranscriptionSystemSettings(autoTranscribeUploads, defaultEngineIdentifier,
            defaultModelIdentifier, editingUser);
  }

  public Transcription createTranscriptionWithDefaultModel(int mediumId, int createdByUserAccountIdUserAccountId) throws TranscriptionServiceException {
    if (!featureEnabled) {
      throw new TranscriptionFeatureDisabledException("Speech-to-text feature is disabled for this deployment");
    }

    Optional<TranscriptionModel> transcriptionModel = systemSettingStorage.getDefaultTranscriptionModel();
    if (transcriptionModel.isPresent()) {
      GenerateTranscriptionConfiguration generateTranscriptionConfiguration = new GenerateTranscriptionConfiguration(
              mediumId, transcriptionModel.get().getId().getEngineIdentifier(),
              transcriptionModel.get().getId().getModelIdentifier());
      return createTranscription(generateTranscriptionConfiguration, createdByUserAccountIdUserAccountId);
    }
    else {
      throw new TranscriptionServiceException(
              "Cannot create transcription with default model. No default model defined.");
    }
  }

  /**
   * Creates a new transcription for the medium described by the given configuration. If the medium is already prepared
   * (i.e. a mono audio file exists), a speech-to-text task is started immediately. Otherwise the transcription is
   * persisted in {@link TranscriptionState#WAITING_FOR_PREPARATION} and a preparation task is scheduled; the
   * speech-to-text task is then started once preparation completes.
   *
   * @param generateTranscriptionConfiguration configuration describing the medium and the engine/model to use
   * @param createdByUserAccountId             id of the {@link UserAccount} created this transcription
   * @throws IllegalArgumentException      if the requested engine is not offered by the connected speech-to-text-service
   *                                       or the requested model is not provided by that engine
   * @throws TranscriptionServiceException if the transcription could not be created (e.g. underlying task scheduling
   *                                       or storage access failed)
   */
  public Transcription createTranscription(GenerateTranscriptionConfiguration generateTranscriptionConfiguration, int createdByUserAccountId) throws TranscriptionServiceException {
    if (!featureEnabled) {
      throw new TranscriptionFeatureDisabledException("Speech-to-text feature is disabled for this deployment");
    }
    int mediumId = generateTranscriptionConfiguration.mediumId();
    String engineIdentifier = generateTranscriptionConfiguration.engineIdentifier();
    String modelIdentifier = generateTranscriptionConfiguration.modelIdentifier();

    SpeechToTextEngine speechToTextEngine = getSpeechToTextEngineHavingModel(engineIdentifier, modelIdentifier);
    upsertEngineModel(speechToTextEngine, modelIdentifier);

    SupportedMediumType supportedMediumType = transcriptionStorage.determineSupportedMediumType(mediumId);
    AudioContainingMediumFileStorage fileStorage = getFileStorage(supportedMediumType);
    Optional<Path> monoFile = fileStorage.getPathToAudioMonoFile(mediumId);


    Transcription createdTranscription = null;
    try {
      if (monoFile.isPresent()) {
        logger.log(Level.INFO, "Mono file available for medium {0}. Creating transcription in PENDING state.",
                mediumId);
        createdTranscription = transcriptionStorage.createTranscription(mediumId, engineIdentifier, modelIdentifier,
                TranscriptionState.PENDING, createdByUserAccountId);
        startTranscriptionTask(createdTranscription.getId(), monoFile.get(), engineIdentifier, modelIdentifier);
      }
      else {
        logger.log(Level.INFO, "No mono file for medium {0}. Creating transcription in WAITING_FOR_PREPARATION state.",
                mediumId);
        createdTranscription = transcriptionStorage.createTranscription(mediumId, engineIdentifier, modelIdentifier,
                TranscriptionState.WAITING_FOR_PREPARATION, createdByUserAccountId);
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
    return createdTranscription;
  }

  /**
   * Returns all transcriptions that belong to the given medium, ordered by the storage's
   * default sort order. The medium is checked first so callers can distinguish "medium has no
   * transcriptions" (empty collection) from "medium does not exist" ({@link MediumNotFoundException}).
   *
   * @param mediumId identifies the {@link de.bitgilde.TIMAAT.model.FIPOP.Medium} whose
   *                 transcriptions should be returned
   * @return the transcriptions of the given medium, never {@code null}
   * @throws MediumNotFoundException if no medium with the given id exists
   */
  public Collection<Transcription> getTranscriptionsForMedium(int mediumId) throws MediumNotFoundException {
    if (!mediumStorage.existsById(mediumId)) {
      throw new MediumNotFoundException(mediumId);
    }
    TranscriptionFilterCriteria filter = new TranscriptionFilterCriteria.Builder().mediumId(mediumId).build();
    return transcriptionStorage.getEntriesAsStream(filter, PagingParameter.NO_PAGING,
            SortingParameter.defaultSortOrder(), null).toList();
  }

  /**
   * Loads a single {@link Transcription} belonging to the given medium. The medium id is part of
   * the lookup so that the transcription URL path is honoured: a transcription belonging to a
   * different medium is reported as {@link TranscriptionNotFoundException} rather than returned
   * out of context.
   *
   * @param mediumId        identifies the {@link de.bitgilde.TIMAAT.model.FIPOP.Medium} the
   *                        transcription is expected to belong to
   * @param transcriptionId identifies the {@link Transcription} to load
   * @return the matching {@link Transcription}
   * @throws TranscriptionNotFoundException if no transcription with the given id exists or the
   *                                        transcription belongs to a different medium
   */
  public Transcription getTranscription(int mediumId, int transcriptionId) throws TranscriptionNotFoundException {
    Transcription transcription = transcriptionStorage.findById(transcriptionId).orElseThrow(
            () -> new TranscriptionNotFoundException(transcriptionId));
    if (transcription.getMedium() == null || transcription.getMedium().getId() != mediumId) {
      throw new TranscriptionNotFoundException(transcriptionId);
    }
    return transcription;
  }

  /**
   * Checks whether a transcription identified by {@code transcriptionId} exists and belongs to the
   * medium identified by {@code mediumId}. Unlike {@link #getTranscription(int, int)} this does
   * not load the full {@link Transcription} entity, so it is the cheaper option when callers only
   * need an existence / scope check (e.g. validating a URL path before deletion).
   *
   * @param mediumId        identifies the {@link de.bitgilde.TIMAAT.model.FIPOP.Medium} the
   *                        transcription is expected to belong to
   * @param transcriptionId identifies the {@link Transcription} to look up
   * @return {@code true} if a transcription with the given id exists and is bound to the given
   * medium; {@code false} otherwise (no transcription with that id, or it belongs to a different
   * medium)
   */
  public boolean existsForMedium(int mediumId, int transcriptionId) {
    return transcriptionStorage.existsForMedium(mediumId, transcriptionId);
  }

  /**
   * Removes the transcription identified by {@code transcriptionId}. Before removal, the
   * transcription's medium is checked: if it still references this transcription as its default,
   * the default is replaced with the most recently created remaining transcription of the same
   * medium, or cleared if none remain. The on-disk transcription file is removed only after the
   * database row has been deleted successfully.
   *
   * @param transcriptionId identifies the transcription to remove
   * @throws TranscriptionNotFoundException if no transcription with the given id exists
   * @throws TranscriptionServiceException  if the deletion failed for any other reason
   */
  public void deleteTranscription(int transcriptionId) throws TranscriptionServiceException {
    Transcription transcription = transcriptionStorage.findById(transcriptionId).orElseThrow(
            () -> new TranscriptionNotFoundException(transcriptionId));
    int mediumId = transcription.getMedium().getId();

    try {
      reassignDefaultTranscriptionIfReferenced(mediumId, transcriptionId);
      transcriptionStorage.deleteTranscription(transcriptionId);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to delete transcription " + transcriptionId + " from database", e);
      throw new TranscriptionServiceException("Failed to delete transcription " + transcriptionId, e);
    }

    try {
      transcriptionFileStorage.deleteTranscription(transcriptionId);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to delete transcription file for transcription " + transcriptionId, e);
      throw new TranscriptionServiceException(
              "Failed to delete transcription file for transcription " + transcriptionId, e);
    }
  }

  private void reassignDefaultTranscriptionIfReferenced(int mediumId, int transcriptionId) {
    Integer newDefaultId = transcriptionStorage.findLatestOtherTranscriptionForMedium(mediumId, transcriptionId)
                                               .map(Transcription::getId).orElse(null);
    mediumStorage.replaceDefaultTranscription(mediumId, transcriptionId, newDefaultId);
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

  private SpeechToTextEngine getSpeechToTextEngineHavingModel(String engineIdentifier, String modelIdentifier) {
    SpeechToTextEngine engine = speechToTextServiceClient.getAvailableEngines().stream()
                                                         .filter(candidate -> candidate.engineIdentifier()
                                                                                       .equals(engineIdentifier))
                                                         .findFirst().orElseThrow(() -> new IllegalArgumentException(
                    "Engine '" + engineIdentifier + "' is not provided by the connected speech-to-text-service"));

    if (!engine.modelIdentifiers().contains(modelIdentifier)) {
      throw new IllegalArgumentException(
              "Model '" + modelIdentifier + "' is not provided by engine '" + engineIdentifier + "'");
    }

    return engine;
  }

  private void upsertEngineModel(SpeechToTextEngine speechToTextEngine, String modelIdentifier) {
    transcriptionStorage.upsertEngine(speechToTextEngine.engineIdentifier(), speechToTextEngine.engineName());
    transcriptionStorage.upsertModel(speechToTextEngine.engineIdentifier(), modelIdentifier);
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
      Collection<SpeechToTextEngine> speechToTextEngines = speechToTextServiceClient.getAvailableEngines();

      if (defaultModel.isEmpty()) {
        logger.log(Level.WARNING, "No transcription model currently set as default.");
        useAnyModelAsDefaultModel(speechToTextEngines);
        return;
      }


      String engineIdentifier = defaultModel.get().getId().getEngineIdentifier();
      String modelIdentifier = defaultModel.get().getId().getModelIdentifier();

      boolean stillAvailable = speechToTextEngines.stream()
                                                  .filter(engine -> engine.engineIdentifier().equals(engineIdentifier))
                                                  .anyMatch(engine -> engine.modelIdentifiers()
                                                                            .contains(modelIdentifier));

      if (!stillAvailable) {
        logger.log(Level.WARNING,
                "Default transcription model {0}/{1} is no longer provided by the speech-to-text-service.",
                new Object[]{engineIdentifier, modelIdentifier});
        useAnyModelAsDefaultModel(speechToTextEngines);
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "Could not verify availability of the default transcription model", e);
    }
  }

  private void useAnyModelAsDefaultModel(Collection<SpeechToTextEngine> speechToTextEngines) {
    Optional<SpeechToTextEngine> speechToTextEngineWithAtLeastOneModelOptional = speechToTextEngines.stream()
                                                                                                    .filter(speechToTextEngine -> !speechToTextEngine.modelIdentifiers()
                                                                                                                                                     .isEmpty())
                                                                                                    .findFirst();
    if (speechToTextEngineWithAtLeastOneModelOptional.isPresent()) {
      SpeechToTextEngine speechToTextEngineWithAtLeastOneModel = speechToTextEngineWithAtLeastOneModelOptional.get();
      String modelIdentifier = speechToTextEngineWithAtLeastOneModel.modelIdentifiers().stream().findFirst().get();
      upsertEngineModel(speechToTextEngineWithAtLeastOneModel, modelIdentifier);

      logger.log(Level.INFO, "Setting {0}/{1} as new transcription default model.",
              new Object[]{speechToTextEngineWithAtLeastOneModel.engineIdentifier(), modelIdentifier});
      systemSettingStorage.updateTranscriptionDefaultModel(speechToTextEngineWithAtLeastOneModel.engineIdentifier(),
              modelIdentifier, null);
    }
    else {
      logger.log(Level.WARNING, "No model available which can be used as the default transcription model.");
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

  @Override
  public void onObservationError(long taskId, SpeechToTextServiceClientErrorType errorType, boolean terminal) {
    logger.log(Level.WARNING, "Received speech to text task observation error: {0}, terminal: {1}",
            new Object[]{errorType, terminal});
    if (terminal) {
      handleSpeechToTextTaskFailed(taskId);
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
