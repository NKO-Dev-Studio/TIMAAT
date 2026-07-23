package de.bitgilde.TIMAAT.storage.entity.medium;

import de.bitgilde.TIMAAT.db.exception.DbTransactionExecutionException;
import de.bitgilde.TIMAAT.db.util.DbQueryStringUtil;
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
import de.bitgilde.TIMAAT.sse.EntityUpdateEventService;
import de.bitgilde.TIMAAT.sse.api.EntityType;
import de.bitgilde.TIMAAT.storage.api.ReducedEntity;
import de.bitgilde.TIMAAT.storage.db.DbStorage;
import de.bitgilde.TIMAAT.storage.entity.medium.api.MediumDefaultTranscriptionEntityUpdateMessage;
import de.bitgilde.TIMAAT.storage.entity.medium.api.MediumFilterCriteria;
import de.bitgilde.TIMAAT.storage.entity.medium.api.MediumSortingField;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

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

  private final EntityUpdateEventService entityUpdateEventService;

  @Inject
  public MediumStorage(EntityManagerFactory emf, EntityUpdateEventService entityUpdateEventService) {
    super(Medium.class, MediumSortingField.ID, emf);
    this.entityUpdateEventService = entityUpdateEventService;
  }

  /**
   * Checks whether a {@link Medium} with the given id exists.
   *
   * @param mediumId identifies the medium whose existence should be checked
   * @return {@code true} if a medium with the given id exists, {@code false} otherwise
   */
  public boolean existsById(int mediumId) {
    return executeDbTransaction(entityManager -> entityManager.find(Medium.class, mediumId) != null);
  }


  /**
   * Calculates the assignable categories of the medium
   * @param mediumId
   * @param searchText
   * @return the assignable categories as {@link Stream} of {@link ReducedEntity}
   */
  @SuppressWarnings("unchecked")
  public Stream<ReducedEntity<Integer>> getAssignableCategoriesOfMedium(int mediumId, @Nullable String searchText) {
    return executeStreamDbTransaction(entityManager -> {
      boolean hasCategorySets = (long) entityManager.createNativeQuery(
                                                            "select count(1) from medium_has_category_set mhcs where mhcs.medium_id = ?").setParameter(1, mediumId)
                                                    .getSingleResult() > 0;
      Query query;
      String likeName = searchText == null ? "" : searchText;
      if (hasCategorySets) {
        String sql = """
                select distinct c.id, c.name from category c
                join category_set_has_category cs on c.id = cs.category_id
                where lower(c.name) like lower(concat('%', ?,'%'))
                and cs.category_set_id in (
                    select mhcs.category_set_id from medium_has_category_set mhcs
                    where mhcs.medium_id = ?)
                order by c.name asc
                """;
        query = entityManager.createNativeQuery(sql).setParameter(1, likeName).setParameter(2, mediumId);
      }
      else {
        String sql = "select c.id, c.name from category c where lower(c.name) like lower(concat('%', ?,'%')) order by c.name asc";
        query = entityManager.createNativeQuery(sql).setParameter(1, likeName);
      }

      Stream<Object[]> resultStream = query.getResultStream();
      return resultStream.map(
              currentResult -> new ReducedEntity<>((Integer) currentResult[0], (String) currentResult[1]));
    });
  }

  public Collection<Category> updateAssignedCategoriesOfMedium(int mediumId, List<Integer> categoryIds) {
    return executeDbTransaction(entityManager -> {
      Medium medium = entityManager.find(Medium.class, mediumId, LockModeType.PESSIMISTIC_WRITE);
      String inPlaceHolder = DbQueryStringUtil.createInPlaceHolderValue(categoryIds.size());
      String query = """
              select distinct c.id, c.name
              from category c
                       left join category_set_has_category cshc on c.id = cshc.category_id
              where (not exists(select 1
                                from medium_has_category_set mhcs
                                where mhcs.medium_id = ?) or exists(select 1
                                                                   from medium_has_category_set mhcs
                                                                   where mhcs.medium_id = ?
                                                                     and cshc.category_set_id = mhcs.category_set_id))
                    and c.id in %s
              """.formatted(inPlaceHolder);
      Query categoryQuery = entityManager.createNativeQuery(query, Category.class).setParameter(1, mediumId)
                                         .setParameter(2, mediumId);

      for (int i = 0; i < categoryIds.size(); i++) {
        int parameterIndex = i + 3;
        categoryQuery.setParameter(parameterIndex, categoryIds.get(i));
      }
      List<Category> updatedCategories = categoryQuery.getResultList();

      medium.setCategories(updatedCategories);
      return updatedCategories;
    });
  }

  public List<CategorySet> updateCategorySetsOfMedium(int mediumId, List<Integer> categorySetIds) throws DbTransactionExecutionException {
    logger.log(Level.FINE, "Updating category sets of medium with id " + mediumId);
    return executeDbTransaction(entityManager -> {
      Medium medium = entityManager.find(Medium.class, mediumId, LockModeType.PESSIMISTIC_WRITE);
      List<CategorySet> updatedCategorySets = categorySetIds.isEmpty() ? Collections.emptyList() : entityManager.createQuery(
              "select categorySet from CategorySet categorySet where categorySet.id in :categorySetIds",
              CategorySet.class).setParameter("categorySetIds", categorySetIds).getResultList();

      if (!categorySetIds.isEmpty()) {
        String inPlaceHolder = DbQueryStringUtil.createInPlaceHolderValue(categorySetIds.size());
        String deleteQueryStatement = """
                delete from medium_has_category mhc where mhc.medium_id = ? and not exists (
                    select 1 from category c
                        join category_set_has_category cshc on cshc.category_id = c.id
                        where c.id = mhc.category_id and cshc.category_set_id in %s
                    )
                """.formatted(inPlaceHolder);
        Query deleteQuery = entityManager.createNativeQuery(deleteQueryStatement).setParameter(1, mediumId);

        for (int i = 0; i < categorySetIds.size(); i++) {
          int parameterIndex = i + 2;
          deleteQuery.setParameter(parameterIndex, categorySetIds.get(i));
        }

        deleteQuery.executeUpdate();
      }


      medium.setCategorySets(updatedCategorySets);
      return updatedCategorySets;
    });
  }

  public List<Category> getRemovedCategoriesAfterCategorySetChange(int mediumId, Collection<Integer> categorySetIds) {
    if (categorySetIds.isEmpty()) {
      return Collections.emptyList();
    }
    String inPlaceHolder = DbQueryStringUtil.createInPlaceHolderValue(categorySetIds.size());

    String queryString = """
                     select c.id, c.name
                     from medium_has_category mhc
                     join category c on c.id = mhc.category_id
                     where mhc.medium_id = ? and not exists(select 1
                                      from category_set_has_category cshc
                                      where cshc.category_id = mhc.category_id and cshc.category_set_id in %s)
            """.formatted(inPlaceHolder);

    return executeDbTransaction(entityManager -> {
      Query query = entityManager.createNativeQuery(queryString, Category.class).setParameter(1, mediumId);

      int currentParameterIndex = 2;
      for (int currentCategorySetId : categorySetIds) {
        query.setParameter(currentParameterIndex++, currentCategorySetId);
      }

      return query.getResultList();
    });
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
                                         "UPDATE Medium m SET m.defaultTranscription = :transcription " + "WHERE m.id = :mediumId AND m.defaultTranscription IS NULL")
                                 .setParameter("transcription", transcription).setParameter("mediumId", mediumId)
                                 .executeUpdate();
      if (updated == 1) {
        sendDefaultTranscriptionChangedEntityUpdateMessage(mediumId, transcriptionId);
      }


      return updated == 1;
    });
  }

  public void updateDefaultTranscription(int mediumId, int transcriptionId) {
    logger.log(Level.FINE, "Setting transcription {0} as default for medium {1}",
            new Object[]{transcriptionId, mediumId});
    executeDbTransaction(entityManagert -> {
      Medium medium = entityManagert.getReference(Medium.class, mediumId);
      Transcription transcription = entityManagert.getReference(Transcription.class, transcriptionId);

      if (transcription.getMedium().getId() == mediumId) {
        medium.setDefaultTranscription(transcription);
        sendDefaultTranscriptionChangedEntityUpdateMessage(mediumId, transcriptionId);
      }
      else {
        throw new IllegalArgumentException(
                "Transcription with id " + transcription + " is not a transcription of medium with id " + mediumId);
      }

      return Void.TYPE;
    });
  }

  /**
   * Atomically replaces the default transcription of the given medium if its current default
   * transcription matches {@code currentDefaultTranscriptionId}. If the medium currently has a
   * different (or no) default transcription, the call is a no-op.
   *
   * @param mediumId                      identifies the {@link Medium} to update
   * @param currentDefaultTranscriptionId identifies the {@link Transcription} the medium is
   *                                      expected to currently reference as default
   * @param newDefaultTranscriptionId     identifies the {@link Transcription} to set as the new
   *                                      default; {@code null} clears the default
   * @return {@code true} if the default transcription was replaced, {@code false} otherwise
   */
  public boolean replaceDefaultTranscription(int mediumId, int currentDefaultTranscriptionId, @Nullable Integer newDefaultTranscriptionId) {
    logger.log(Level.FINE, "Replacing default transcription of medium {0} from transcription {1} to transcription {2}",
            new Object[]{mediumId, currentDefaultTranscriptionId, newDefaultTranscriptionId});
    return executeDbTransaction(entityManager -> {
      Transcription newDefault = newDefaultTranscriptionId == null ? null : entityManager.getReference(
              Transcription.class, newDefaultTranscriptionId);
      int updated = entityManager.createQuery(
                                         "UPDATE Medium m SET m.defaultTranscription = :newDefault " + "WHERE m.id = :mediumId AND m.defaultTranscription.id = :currentDefaultId")
                                 .setParameter("newDefault", newDefault).setParameter("mediumId", mediumId)
                                 .setParameter("currentDefaultId", currentDefaultTranscriptionId).executeUpdate();

      if (updated == 1) {
        sendDefaultTranscriptionChangedEntityUpdateMessage(mediumId, newDefaultTranscriptionId);
      }

      return updated == 1;
    });
  }

  private void sendDefaultTranscriptionChangedEntityUpdateMessage(int mediumId, Integer transcriptionId) {
    try {
      MediumDefaultTranscriptionEntityUpdateMessage message = new MediumDefaultTranscriptionEntityUpdateMessage(
              transcriptionId);
      entityUpdateEventService.sendEntityChangeMessage(EntityType.MEDIUM, mediumId, message);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Could not send default transcription changed entity update message", e);
    }
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

    if (filter != null) {
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
