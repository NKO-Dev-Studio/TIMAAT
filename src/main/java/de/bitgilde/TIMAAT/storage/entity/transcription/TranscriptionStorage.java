package de.bitgilde.TIMAAT.storage.entity.transcription;

import de.bitgilde.TIMAAT.db.exception.DbTransactionExecutionException;
import de.bitgilde.TIMAAT.model.FIPOP.Transcription;
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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
      return executeStreamDbTransaction(entityManager -> entityManager.createQuery("select distinct transcription.medium.id, medium.mediaType.id from Transcription transcription join transcription.medium medium where transcription.transcriptionState.id in :transcriptionStates", Object.class)
              .setParameter("transcriptionStates", Set.of(TranscriptionState.WAITING_FOR_PREPARATION.getDatabaseId(), TranscriptionState.PREPARING.getDatabaseId()))
              .getResultStream()
              .map(result -> {
                Object[] row = (Object[]) result;
                SupportedMediumType supportedMediumType = ((Integer) row[1]).equals(6) ? SupportedMediumType.VIDEO : SupportedMediumType.AUDIO;
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

  public List<Transcription> updateTranscriptionsStateOfPreparationTask(int mediumId, TranscriptionState transcriptionState) {
    return executeDbTransaction(entityManager -> {
      de.bitgilde.TIMAAT.model.FIPOP.TranscriptionState transcriptionStateEntity = entityManager.getReference(
              de.bitgilde.TIMAAT.model.FIPOP.TranscriptionState.class, transcriptionState.getDatabaseId());

      List<Transcription> transcriptions = entityManager.createQuery("select transcription from Transcription transcription where transcription.medium.id = :mediumId and transcription.transcriptionState.id in :preparationStates", Transcription.class)
              .setParameter("mediumId", mediumId)
              .setParameter("preparationStates", Set.of(TranscriptionState.WAITING_FOR_PREPARATION.getDatabaseId(), TranscriptionState.PREPARING.getDatabaseId()))
              .getResultList();

      transcriptions.forEach(transcription -> transcription.setTranscriptionState(transcriptionStateEntity));

      return transcriptions;
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
        predicates.add(
                root.get(Transcription_.transcriptionType).get(TranscriptionType_.id).in(transcriptionTypeIds));
      }

      return predicates;
    }

    return Collections.emptyList();
  }
}
