package de.bitgilde.TIMAAT.storage.db;

import de.bitgilde.TIMAAT.db.DbAccessComponent;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount;
import de.bitgilde.TIMAAT.storage.api.EntityStorage;
import de.bitgilde.TIMAAT.storage.api.PagingParameter;
import de.bitgilde.TIMAAT.storage.api.SortOrder;
import de.bitgilde.TIMAAT.storage.api.SortingParameter;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;
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
 * Storage using the database as persistence layer.
 * @param <ENTITY_TYPE> which is managed by this {@link DbStorage}
 * @param <FILTER_TYPE> which can be applied to filter listing results
 * @param <SORTING_FIELD_TYPE> which can be used to order the listing results
 *
 * @author Nico Kotlenga
 * @since 13.12.25
 */
public abstract class DbStorage<ENTITY_TYPE, FILTER_TYPE, SORTING_FIELD_TYPE extends DbSortingField<ENTITY_TYPE>> extends DbAccessComponent implements EntityStorage<ENTITY_TYPE, FILTER_TYPE, SORTING_FIELD_TYPE> {

  private static final Logger logger = Logger.getLogger(DbStorage.class.getName());

  private final Class<ENTITY_TYPE> entityTypeClass;
  private final SORTING_FIELD_TYPE defaultSortingField;

  public DbStorage(Class<ENTITY_TYPE> entityTypeClass, SORTING_FIELD_TYPE defaultSortingField, EntityManagerFactory emf) {
    super(emf);
    this.entityTypeClass = entityTypeClass;
    this.defaultSortingField = defaultSortingField;
  }

  /**
   * Loads the entries as stream matching the specified criteria.
   * @param filter which the entities have to match
   * @param pagingParameter used to limit the size of the resulting stream
   * @param sortingParameter used to determine the order of entries inside the stream
   * @param userAccount used to filter entries by authorization
   * @return the resulting {@link Stream}
   */
  public Stream<ENTITY_TYPE> getEntriesAsStream(FILTER_TYPE filter, PagingParameter pagingParameter, SortingParameter<SORTING_FIELD_TYPE> sortingParameter, @Nullable UserAccount userAccount) {
    logger.log(Level.FINER, "Loading  entries matching the specified filter criteria");

    return executeStreamDbTransaction(entityManager -> {
      CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
      CriteriaQuery<ENTITY_TYPE> criteriaQuery = criteriaBuilder.createQuery(entityTypeClass);
      Root<ENTITY_TYPE> root = criteriaQuery.from(entityTypeClass);

      List<Predicate> predicates = createPredicates(filter, root, criteriaBuilder, criteriaQuery, userAccount);
      Order sortOrder = createSortOrder(sortingParameter, root, criteriaBuilder);

      criteriaQuery.distinct(true);
      criteriaQuery.where(predicates.toArray(new Predicate[0]));
      criteriaQuery.orderBy(sortOrder);
      criteriaQuery.select(root);

      Stream<ENTITY_TYPE> resultStream = entityManager.createQuery(criteriaQuery).getResultStream();
      return adjustStreamToPassedPagingParameter(resultStream, pagingParameter);
    });
  }

  /**
   * Loads the count of entities matching the specified filters
   * @param filter which the items will match
   * @return the number of matching entries
   */
  @Override
  public Long getNumberOfMatchingEntriesRespectingAuthorization(FILTER_TYPE filter, UserAccount userAccount) {
    logger.log(Level.FINE, "Loading number of matching music entries");
    return executeDbTransaction(entityManager -> {
      CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
      CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);

      Root<ENTITY_TYPE> root = criteriaQuery.from(entityTypeClass);
      criteriaQuery.select(criteriaBuilder.countDistinct(root));
      List<Predicate> predicates = createPredicates(filter, root, criteriaBuilder, criteriaQuery, userAccount);

      criteriaQuery.where(predicates.toArray(new Predicate[0]));
      return entityManager.createQuery(criteriaQuery).getSingleResult();
    });
  }

  /**
   * @return the number of total entries inside the storage
   */
  public Long getNumberOfTotalEntries() {
    logger.log(Level.FINE, "Loading number of total entries");
    return executeDbTransaction(entityManager -> {
      CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
      CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);

      Root<?> root = criteriaQuery.from(entityTypeClass);
      criteriaQuery.select(criteriaBuilder.countDistinct(root));

      return entityManager.createQuery(criteriaQuery).getSingleResult();
    });
  }

  public Long getNumberOfTotalEntriesRespectingAuthorization(UserAccount userAccount) {
    logger.log(Level.FINE, "Loading number of total entries accessible for user account");
    return executeDbTransaction(entityManager -> {
      CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
      CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);

      Root<ENTITY_TYPE> root = criteriaQuery.from(entityTypeClass);
      List<Predicate> predicates = createPredicates(null, root, criteriaBuilder, criteriaQuery, userAccount);

      criteriaQuery.select(criteriaBuilder.countDistinct(root));
      criteriaQuery.where(predicates.toArray(new Predicate[0]));

      return entityManager.createQuery(criteriaQuery).getSingleResult();
    });
  }


  /**
   * Creates the {@link Predicate}s to filter entities by the specified filter criteria
   * @param filter which need to get fulfilled
   * @param root of the current query
   * @param criteriaBuilder which can be used the create the {@link Predicate}s
   * @param userAccount which can be provided to create filters respecting the authorization
   * @return a {@link List} of {@link Predicate}s
   */
  protected abstract List<Predicate> createPredicates(@Nullable FILTER_TYPE filter, Root<ENTITY_TYPE> root, CriteriaBuilder criteriaBuilder, CriteriaQuery<?> criteriaQuery, @Nullable UserAccount userAccount);

  /**
   * Creates the {@link Order} to match current defined {@link SortingParameter}
   * @param sortingParameter to which the resulting listing
   * @param root of the current query
   * @param criteriaBuilder which can be used the create the {@link Predicate}s
   * @return a {@link List} of {@link Predicate}s
   */
  private Order createSortOrder(SortingParameter<SORTING_FIELD_TYPE> sortingParameter, Root<ENTITY_TYPE> root, CriteriaBuilder criteriaBuilder) {
    SortOrder sortOrder = SortOrder.ASC;
    if (sortingParameter.getSortOrder().isPresent()) {
      sortOrder = sortingParameter.getSortOrder().get();
    }

    SORTING_FIELD_TYPE sortingField = this.defaultSortingField;
    if (sortingParameter.getSortingField().isPresent()) {
      sortingField = sortingParameter.getSortingField().get();
    }

    if (SortOrder.ASC.equals(sortOrder)) {
      return criteriaBuilder.asc(sortingField.getPathFromRootEntity(root));
    }
    else {
      return criteriaBuilder.desc(sortingField.getPathFromRootEntity(root));
    }
  }

  /**
   * Adjust the stream to match the specified {@link PagingParameter}
   * @param stream which will be adjusted
   * @param pagingParameter which should be matched
   * @return the adjusted {@link Stream}
   */
  private Stream<ENTITY_TYPE> adjustStreamToPassedPagingParameter(Stream<ENTITY_TYPE> stream, PagingParameter pagingParameter) {
    Stream<ENTITY_TYPE> resultingStream = stream;
    if (pagingParameter.startIndex().isPresent()) {
      resultingStream = resultingStream.skip(pagingParameter.startIndex().get());
    }
    if (pagingParameter.limit().isPresent()) {
      resultingStream = resultingStream.limit(pagingParameter.limit().get());
    }

    return resultingStream;
  }
}
