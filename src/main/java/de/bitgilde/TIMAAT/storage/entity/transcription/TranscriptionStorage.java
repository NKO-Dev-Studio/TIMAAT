package de.bitgilde.TIMAAT.storage.entity.transcription;

import de.bitgilde.TIMAAT.db.exception.DbTransactionExecutionException;
import de.bitgilde.TIMAAT.model.FIPOP.Medium;
import de.bitgilde.TIMAAT.model.FIPOP.Transcription;
import de.bitgilde.TIMAAT.model.FIPOP.TranscriptionEngine;
import de.bitgilde.TIMAAT.model.FIPOP.TranscriptionModel;
import de.bitgilde.TIMAAT.model.FIPOP.TranscriptionModelId;
import de.bitgilde.TIMAAT.model.FIPOP.TranscriptionState_;
import de.bitgilde.TIMAAT.model.FIPOP.TranscriptionType_;
import de.bitgilde.TIMAAT.model.FIPOP.Transcription_;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount;
import de.bitgilde.TIMAAT.service.task.api.MediumAudioAnalysisTask.SupportedMediumType;
import de.bitgilde.TIMAAT.service.task.api.Task;
import de.bitgilde.TIMAAT.service.task.api.TaskType;
import de.bitgilde.TIMAAT.service.task.api.TranscriptionMediumPreparationTask;
import de.bitgilde.TIMAAT.service.task.exception.TaskStorageException;
import de.bitgilde.TIMAAT.service.task.storage.TaskStorage;
import de.bitgilde.TIMAAT.storage.db.DbStorage;
import de.bitgilde.TIMAAT.storage.entity.transcription.api.TranscriptionFilterCriteria;
import de.bitgilde.TIMAAT.storage.entity.transcription.api.TranscriptionSortingField;
import de.bitgilde.TIMAAT.storage.entity.transcription.api.TranscriptionState;
import de.bitgilde.TIMAAT.storage.entity.transcription.api.TranscriptionType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Storage responsible to provide access to {@link de.bitgilde.TIMAAT.model.FIPOP.Transcription} entities
 *
 * @author Nico Kotlenga (nico@nko-dev.studio) 
 * @since 12.05.26
 */
public class TranscriptionStorage extends DbStorage<Transcription, TranscriptionFilterCriteria, TranscriptionSortingField> implements TaskStorage {

  @Inject
  public TranscriptionStorage(EntityManagerFactory emf) {
    super(Transcription.class, TranscriptionSortingField.ID, emf);
  }

  @Override
  public TaskType getSupportedTaskType() {
    return TaskType.TRANSCRIPTION_MEDIUM_PREPARATION;
  }

  @Override
  public Stream<? extends Task> getUnfinishedTasks() throws TaskStorageException {
    try {
      return executeStreamDbTransaction(entityManager -> entityManager.createQuery(
              "select distinct transcription.medium.id, medium.mediaType.id from Transcription transcription join transcription.medium medium where transcription.transcriptionState.id in :transcriptionStates",
              Object.class).setParameter("transcriptionStates",
              Set.of(TranscriptionState.WAITING_FOR_PREPARATION.getDatabaseId(),
                      TranscriptionState.PREPARING.getDatabaseId())).getResultStream().map(result -> {
        Object[] row = (Object[]) result;
        SupportedMediumType supportedMediumType = ((Integer) row[1]).equals(
                6) ? SupportedMediumType.VIDEO : SupportedMediumType.AUDIO;
        int mediumId = (Integer) row[0];

        return new TranscriptionMediumPreparationTask(mediumId, supportedMediumType);
      }));
    } catch (DbTransactionExecutionException e) {
      throw new TaskStorageException("Error during loading unfinished transcription medium preparation tasks", e);
    }
  }

  @Override
  public void persistTask(Task task) {
    // Nothing to do here. The transcription lifecycle is created by TranscriptionService.
  }

  public void updateTranscriptionTaskId(int transcriptionId, long transcriptionTaskId) {
    executeDbTransaction(entityManager -> {
      entityManager.createQuery(
                           "update Transcription set transcriptionTaskId = :transcriptionTaskId where id = :transcriptionId")
                   .setParameter("transcriptionId", transcriptionId)
                   .setParameter("transcriptionTaskId", transcriptionTaskId).executeUpdate();
      return Void.TYPE;
    });
  }

  /**
   * Inserts a {@link TranscriptionEngine} row for the given identifier if none exists yet. The DB
   * tables for engines and models keep a historical record of every engine/model used in a
   * transcription; live availability is queried from the speech-to-text-service.
   */
  public void upsertEngine(String engineIdentifier, String engineName) {
    executeDbTransaction(entityManager -> {
      if (entityManager.find(TranscriptionEngine.class, engineIdentifier) == null) {
        TranscriptionEngine engine = new TranscriptionEngine();
        engine.setEngineIdentifier(engineIdentifier);
        engine.setEngineName(engineName);
        entityManager.persist(engine);
      }
      return Void.TYPE;
    });
  }

  /**
   * Inserts a {@link TranscriptionModel} row for the given engine/model pair if none exists yet.
   */
  public void upsertModel(String engineIdentifier, String modelIdentifier) {
    executeDbTransaction(entityManager -> {
      TranscriptionModelId modelId = new TranscriptionModelId();
      modelId.setEngineIdentifier(engineIdentifier);
      modelId.setModelIdentifier(modelIdentifier);
      if (entityManager.find(TranscriptionModel.class, modelId) == null) {
        TranscriptionModel model = new TranscriptionModel();
        model.setId(modelId);
        entityManager.persist(model);
      }
      return Void.TYPE;
    });
  }

  public Transcription createTranscription(int mediumId, String engineIdentifier, String modelIdentifier, TranscriptionState transcriptionState) {
    return executeDbTransaction(entityManager -> {
      de.bitgilde.TIMAAT.model.FIPOP.TranscriptionState transcriptionStateEntity = entityManager.getReference(
              de.bitgilde.TIMAAT.model.FIPOP.TranscriptionState.class, transcriptionState.getDatabaseId());
      de.bitgilde.TIMAAT.model.FIPOP.TranscriptionType transcriptionTypeEntity = entityManager.getReference(
              de.bitgilde.TIMAAT.model.FIPOP.TranscriptionType.class, TranscriptionType.GENERATED.getDatabaseId());
      Medium medium = entityManager.getReference(Medium.class, mediumId);
      TranscriptionModelId transcriptionModelId = new TranscriptionModelId();
      transcriptionModelId.setEngineIdentifier(engineIdentifier);
      transcriptionModelId.setModelIdentifier(modelIdentifier);
      TranscriptionModel transcriptionModel = entityManager.getReference(TranscriptionModel.class,
              transcriptionModelId);

      Transcription transcription = new Transcription();
      transcription.setName(engineIdentifier + " / " + modelIdentifier);
      transcription.setMedium(medium);
      transcription.setTranscriptionState(transcriptionStateEntity);
      transcription.setTranscriptionType(transcriptionTypeEntity);
      transcription.setTranscriptionModel(transcriptionModel);
      transcription.setCreatedAt(Instant.now());

      entityManager.persist(transcription);

      return transcription;
    });
  }

  public Optional<Integer> getTranscriptionIdRelatedToTranscriptionTask(long transcriptionTaskId) {
    return executeDbTransaction(entityManager -> {
      try {
        Integer transcriptionId = entityManager.createQuery(
                "select transcription.id from Transcription transcription where transcription.transcriptionTaskId = :transcriptionTaskId",
                Integer.class).setParameter("transcriptionTaskId", transcriptionTaskId).getSingleResult();
        return Optional.of(transcriptionId);
      } catch (NoResultException e) {
        return Optional.empty();
      }
    });
  }

  public void updateTranscriptionState(int transcriptionId, TranscriptionState transcriptionState) {
    executeDbTransaction(entityManager -> {
      Transcription transcription = entityManager.createQuery(
                                                         "select transcription from Transcription transcription where transcription.id = :id", Transcription.class)
                                                 .setParameter("id", transcriptionId).getSingleResult();
      de.bitgilde.TIMAAT.model.FIPOP.TranscriptionState transcriptionStateEntity = entityManager.getReference(
              de.bitgilde.TIMAAT.model.FIPOP.TranscriptionState.class, transcriptionState.getDatabaseId());
      transcription.setTranscriptionState(transcriptionStateEntity);

      return Void.TYPE;
    });
  }

  public List<Transcription> updateTranscriptionsStateOfPreparationTask(int mediumId, TranscriptionState transcriptionState) {
    return executeDbTransaction(entityManager -> {
      de.bitgilde.TIMAAT.model.FIPOP.TranscriptionState transcriptionStateEntity = entityManager.getReference(
              de.bitgilde.TIMAAT.model.FIPOP.TranscriptionState.class, transcriptionState.getDatabaseId());

      List<Transcription> transcriptions = entityManager.createQuery(
              "select transcription from Transcription transcription where transcription.medium.id = :mediumId and transcription.transcriptionState.id in :preparationStates",
              Transcription.class).setParameter("mediumId", mediumId).setParameter("preparationStates",
              Set.of(TranscriptionState.WAITING_FOR_PREPARATION.getDatabaseId(),
                      TranscriptionState.PREPARING.getDatabaseId())).getResultList();

      transcriptions.forEach(transcription -> transcription.setTranscriptionState(transcriptionStateEntity));

      return transcriptions;
    });
  }

  public SupportedMediumType determineSupportedMediumType(int mediumId) {
    return executeDbTransaction(entityManager -> {
      Medium medium = entityManager.find(Medium.class, mediumId);
      if (medium == null) {
        throw new IllegalArgumentException("Medium with id " + mediumId + " not found");
      }
      int mediaTypeId = medium.getMediaType().getId();
      return switch (mediaTypeId) {
        case 1 -> SupportedMediumType.AUDIO;
        case 6 -> SupportedMediumType.VIDEO;
        default ->
                throw new IllegalArgumentException("Unsupported media type id " + mediaTypeId + " for transcription");
      };
    });
  }

  @Override
  protected List<Predicate> createPredicates(@Nullable TranscriptionFilterCriteria filter, Root<Transcription> root, CriteriaBuilder criteriaBuilder, CriteriaQuery<?> criteriaQuery, @Nullable UserAccount userAccount) {
    if (filter != null) {
      List<Predicate> predicates = new ArrayList<>(2);

      if (filter.getTranscriptionStates().isPresent()) {
        Collection<Integer> transcriptionStateIds = filter.getTranscriptionStates().get().stream()
                                                          .map(TranscriptionState::getDatabaseId)
                                                          .collect(Collectors.toSet());
        predicates.add(
                root.get(Transcription_.transcriptionState).get(TranscriptionState_.id).in(transcriptionStateIds));
      }


      if (filter.getTranscriptionTypes().isPresent()) {
        Collection<Integer> transcriptionTypeIds = filter.getTranscriptionTypes().get().stream()
                                                         .map(TranscriptionType::getDatabaseId)
                                                         .collect(Collectors.toSet());
        predicates.add(root.get(Transcription_.transcriptionType).get(TranscriptionType_.id).in(transcriptionTypeIds));
      }

      return predicates;
    }

    return Collections.emptyList();
  }
}
