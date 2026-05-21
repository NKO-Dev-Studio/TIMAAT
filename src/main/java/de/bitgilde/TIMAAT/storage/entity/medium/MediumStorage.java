package de.bitgilde.TIMAAT.storage.entity.medium;

import de.bitgilde.TIMAAT.model.FIPOP.Category;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySet;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySet_;
import de.bitgilde.TIMAAT.model.FIPOP.Category_;
import de.bitgilde.TIMAAT.model.FIPOP.MediaType;
import de.bitgilde.TIMAAT.model.FIPOP.MediaType_;
import de.bitgilde.TIMAAT.model.FIPOP.Medium;
import de.bitgilde.TIMAAT.model.FIPOP.MediumHasMusic;
import de.bitgilde.TIMAAT.model.FIPOP.MediumHasMusicDetail;
import de.bitgilde.TIMAAT.model.FIPOP.Medium_;
import de.bitgilde.TIMAAT.model.FIPOP.Music;
import de.bitgilde.TIMAAT.model.FIPOP.Title_;
import de.bitgilde.TIMAAT.model.FIPOP.Transcription;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount;
import de.bitgilde.TIMAAT.model.TimeRange;
import de.bitgilde.TIMAAT.storage.db.DbStorage;
import de.bitgilde.TIMAAT.storage.entity.medium.api.MediumFilterCriteria;
import de.bitgilde.TIMAAT.storage.entity.medium.api.MediumSortingField;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

/**
 * Storage used to read and update {@link de.bitgilde.TIMAAT.model.FIPOP.Medium} information
 *
 * @author Nico Kotlenga
 * @since 27.09.25
 */
public class MediumStorage extends DbStorage<Medium, MediumFilterCriteria, MediumSortingField> {

  private static final Logger logger = Logger.getLogger(MediumStorage.class.getName());

  @Inject
  public MediumStorage(EntityManagerFactory emf) {
    super(Medium.class, MediumSortingField.ID, emf);
  }

  /**
   * Sets the given {@link Transcription} as the default transcription of the medium identified by
   * {@code mediumId}, but only if the medium currently has no default transcription assigned. The
   * update is performed atomically as a single conditional {@code UPDATE} statement.
   *
   * @param mediumId        identifies the {@link Medium} whose default transcription should be set
   * @param transcriptionId identifies the {@link Transcription} to set as default
   * @return {@code true} if this call set the default transcription, {@code false} if a default
   *         was already present or no matching medium exists
   */
  public boolean setDefaultTranscriptionIfAbsent(int mediumId, int transcriptionId) {
    logger.log(Level.FINE, "Setting transcription {0} as default for medium {1} if no default is present",
            new Object[]{transcriptionId, mediumId});
    return executeDbTransaction(entityManager -> {
      Transcription transcription = entityManager.getReference(Transcription.class, transcriptionId);
      int updated = entityManager.createQuery(
              "UPDATE Medium m SET m.defaultTranscription = :transcription " +
                      "WHERE m.id = :mediumId AND m.defaultTranscription IS NULL")
              .setParameter("transcription", transcription)
              .setParameter("mediumId", mediumId)
              .executeUpdate();
      return updated == 1;
    });
  }

  public List<MediumHasMusic> updateMediumHasMusicList(int mediumId, Map<Integer, Collection<TimeRange>> timeRangesByMusicId) {
    logger.log(Level.FINE, "Updating medium has music list of medium with id " + mediumId);
    return executeDbTransaction(entityManager -> {
      Medium medium = entityManager.find(Medium.class, mediumId);

      entityManager.createQuery("delete from MediumHasMusic where medium.id= :mediumId")
                   .setParameter("mediumId", mediumId).executeUpdate();
      List<MediumHasMusic> updatedMediumHasMusic = new ArrayList<>();
      for (Map.Entry<Integer, Collection<TimeRange>> currentTimeRangeByMediumId : timeRangesByMusicId.entrySet()) {
        MediumHasMusic currentMediumHasMusic = new MediumHasMusic();
        Music currentMusic = entityManager.find(Music.class, currentTimeRangeByMediumId.getKey());

        currentMediumHasMusic.setMusic(currentMusic);
        currentMediumHasMusic.setMedium(medium);

        entityManager.persist(currentMediumHasMusic);

        for (TimeRange currentTimeRange : currentTimeRangeByMediumId.getValue()) {
          MediumHasMusicDetail currentMediumHasMusicDetail = new MediumHasMusicDetail();

          currentMediumHasMusicDetail.setStartTime(currentTimeRange.getStartTime());
          currentMediumHasMusicDetail.setEndTime(currentTimeRange.getEndTime());
          currentMediumHasMusicDetail.setMediumHasMusic(currentMediumHasMusic);

          entityManager.persist(currentMediumHasMusicDetail);
        }

        updatedMediumHasMusic.add(currentMediumHasMusic);
      }
      entityManager.flush();
      updatedMediumHasMusic.forEach(entityManager::refresh);
      return updatedMediumHasMusic;
    });
  }

  @Override
  protected List<Predicate> createPredicates(MediumFilterCriteria filter, Root<Medium> root, CriteriaBuilder criteriaBuilder, CriteriaQuery<?> criteriaQuery, UserAccount userAccount) {
    List<Predicate> predicates = new ArrayList<>();

    if(filter != null) {
      if (filter.getMediumNameSearch().isPresent()) {
        String searchText = filter.getMediumNameSearch().get();
        predicates.add(criteriaBuilder.like(root.get(Medium_.displayTitle).get(Title_.name), "%" + searchText + "%"));
      }

      if (filter.getCategoryIds().isPresent() && !filter.getCategoryIds().get().isEmpty()) {
        Collection<Integer> categoryIds = filter.getCategoryIds().get();
        Join<Medium, Category> categoryJoin = root.join(Medium_.categories);

        predicates.add(categoryJoin.get(Category_.id).in(categoryIds));
      }

      if (filter.getCategorySetIds().isPresent() && !filter.getCategorySetIds().get().isEmpty()) {
        Collection<Integer> categorySetIds = filter.getCategorySetIds().get();
        Join<Medium, CategorySet> categorySetJoin = root.join(Medium_.categorySets);

        predicates.add(categorySetJoin.get(CategorySet_.id).in(categorySetIds));
      }

      if (filter.getMediaTypeIds().isPresent() && !filter.getMediaTypeIds().get().isEmpty()) {
        Collection<Integer> mediaTypeIds = filter.getMediaTypeIds().get();
        Join<Medium, MediaType> mediaTypeJoin = root.join(Medium_.mediaType);

        predicates.add(mediaTypeJoin.get(MediaType_.id).in(mediaTypeIds));
      }
    }

    return predicates;
  }
}
