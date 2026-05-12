package de.bitgilde.TIMAAT.storage.entity.transcription;

import de.bitgilde.TIMAAT.model.FIPOP.Transcription;
import de.bitgilde.TIMAAT.model.FIPOP.TranscriptionState_;
import de.bitgilde.TIMAAT.model.FIPOP.Transcription_;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount;
import de.bitgilde.TIMAAT.storage.db.DbStorage;
import de.bitgilde.TIMAAT.storage.entity.transcription.api.TranscriptionFilterCriteria;
import de.bitgilde.TIMAAT.storage.entity.transcription.api.TranscriptionSortingField;
import de.bitgilde.TIMAAT.storage.entity.transcription.api.TranscriptionState;
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
import java.util.stream.Collectors;

/**
 * Storage responsible to provide access to {@link de.bitgilde.TIMAAT.model.FIPOP.Transcription} entities
 *
 * @author Nico Kotlenga (nico@nko-dev.studio) 
 * @since 12.05.26
 */
public class TranscriptionStorage extends DbStorage<Transcription, TranscriptionFilterCriteria, TranscriptionSortingField> {

  @Inject
  public TranscriptionStorage(EntityManagerFactory emf) {
    super(Transcription.class, TranscriptionSortingField.ID, emf);
  }

  @Override
  protected List<Predicate> createPredicates(@Nullable TranscriptionFilterCriteria filter, Root<Transcription> root, CriteriaBuilder criteriaBuilder, CriteriaQuery<?> criteriaQuery, @Nullable UserAccount userAccount) {
    if (filter != null) {
      List<Predicate> predicates = new ArrayList<>(1);

      if (filter.getTranscriptionStates().isPresent()) {
        Collection<Integer> transcriptionStateIds = filter.getTranscriptionStates().get().stream()
                                                          .map(TranscriptionState::getDatabaseId)
                                                          .collect(Collectors.toSet());
        predicates.add(
                root.get(Transcription_.transcriptionState).get(TranscriptionState_.id).in(transcriptionStateIds));
      }

      return predicates;
    }

    return Collections.emptyList();
  }
}
