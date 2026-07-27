package de.bitgilde.TIMAAT.storage.entity.actor;

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

import de.bitgilde.TIMAAT.db.util.DbQueryStringUtil;
import de.bitgilde.TIMAAT.model.FIPOP.Actor;
import de.bitgilde.TIMAAT.model.FIPOP.ActorName_;
import de.bitgilde.TIMAAT.model.FIPOP.ActorType;
import de.bitgilde.TIMAAT.model.FIPOP.ActorType_;
import de.bitgilde.TIMAAT.model.FIPOP.Actor_;
import de.bitgilde.TIMAAT.model.FIPOP.Category;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySet;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySet_;
import de.bitgilde.TIMAAT.model.FIPOP.Category_;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount;
import de.bitgilde.TIMAAT.storage.api.ReducedEntity;
import de.bitgilde.TIMAAT.storage.db.DbStorage;
import de.bitgilde.TIMAAT.storage.entity.actor.api.ActorFilterCriteria;
import de.bitgilde.TIMAAT.storage.entity.actor.api.ActorSortingField;
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
import java.util.stream.Stream;

/**
 * Storage which can be used to access and edit {@link de.bitgilde.TIMAAT.model.FIPOP.Actor} information
 *
 * @author Nico Kotlenga
 * @since 27.12.25
 */
public class ActorStorage extends DbStorage<Actor, ActorFilterCriteria, ActorSortingField> {

  @Inject
  public ActorStorage(EntityManagerFactory emf) {
    super(Actor.class, ActorSortingField.ID, emf);
  }

  @Override
  protected List<Predicate> createPredicates(ActorFilterCriteria filter, Root<Actor> root, CriteriaBuilder criteriaBuilder, CriteriaQuery<?> criteriaQuery, UserAccount userAccount) {
    List<Predicate> predicates = new ArrayList<>();

    if (filter != null) {
      if (filter.getActorNameSearch().isPresent()) {
        String searchText = filter.getActorNameSearch().get();
        predicates.add(criteriaBuilder.like(root.get(Actor_.displayName).get(ActorName_.name), "%" + searchText + "%"));
      }

      if (filter.getExcludedAnnotationId().isPresent()) {
        Integer excludedAnnotationId = filter.getExcludedAnnotationId().get();
        predicates.add(criteriaBuilder.notEqual(root.get(Actor_.id), excludedAnnotationId));
      }

      boolean categoryFilterActive = filter.getCategoryIds().isPresent() && !filter.getCategoryIds().get().isEmpty();
      boolean categorySetFilterActive = filter.getCategorySetIds().isPresent() && !filter.getCategorySetIds().get()
                                                                                         .isEmpty();

      if (categoryFilterActive) {
        Join<Actor, Category> actorCategoryJoin = root.join(Actor_.categories);
        predicates.add(actorCategoryJoin.get(Category_.id).in(filter.getCategoryIds().get()));
      }

      if (categorySetFilterActive) {
        Join<Actor, CategorySet> actorCategorySetJoin = root.join(Actor_.categorySets);
        predicates.add(actorCategorySetJoin.get(CategorySet_.id).in(filter.getCategorySetIds().get()));
      }

      if (filter.getActorTypeIds().isPresent() && !filter.getActorTypeIds().get().isEmpty()) {
        Join<Actor, ActorType> actorTypeJoin = root.join(Actor_.actorType);
        predicates.add(actorTypeJoin.get(ActorType_.id).in(filter.getActorTypeIds().get()));
      }

    }

    return predicates;
  }

  public Collection<CategorySet> getCategorySetsOfActor(int actorId) {
    return executeDbTransaction(entityManager -> entityManager.getReference(Actor.class, actorId).getCategorySets());
  }

  public Collection<Category> getCategoriesOfActor(int actorId) {
    return executeDbTransaction(entityManager -> entityManager.getReference(Actor.class, actorId).getCategories());
  }

  public Collection<Category> updateAssignedCategoriesOfActor(int actorId, List<Integer> categoryIds) {
    return executeDbTransaction(entityManager -> {
      Actor actor = entityManager.find(Actor.class, actorId, LockModeType.PESSIMISTIC_READ);
      List<Category> updatedCategories;

      if (categoryIds.isEmpty()) {
        updatedCategories = Collections.emptyList();
      }
      else {
        String inPlaceHolder = DbQueryStringUtil.createInPlaceHolderValue(categoryIds.size());
        String query = """
                select distinct c.id, c.name
                from category c
                         left join category_set_has_category cshc on c.id = cshc.category_id
                where (not exists(select 1
                                  from actor_has_category_set ahcs
                                  where ahcs.actor_id = ?) or exists(select 1
                                                                     from actor_has_category_set ahcs
                                                                     where ahcs.actor_id = ?
                                                                       and cshc.category_set_id = ahcs.category_set_id))
                      and c.id in %s
                """.formatted(inPlaceHolder);
        Query categoryQuery = entityManager.createNativeQuery(query, Category.class).setParameter(1, actorId)
                                           .setParameter(2, actorId);

        for (int i = 0; i < categoryIds.size(); i++) {
          int parameterIndex = i + 3;
          categoryQuery.setParameter(parameterIndex, categoryIds.get(i));
        }
        updatedCategories = categoryQuery.getResultList();
      }

      actor.setCategories(updatedCategories);
      return updatedCategories;
    });
  }

  /**
   * Calculates the assignable categories of the actor
   * @param actorId
   * @param searchText
   * @return the assignable categories as {@link Stream} of {@link ReducedEntity}
   */
  @SuppressWarnings("unchecked")
  public Stream<ReducedEntity<Integer>> getAssignableCategoriesOfActor(int actorId, @Nullable String searchText) {
    return executeStreamDbTransaction(entityManager -> {
      boolean hasCategorySets = (long) entityManager.createNativeQuery(
                                                            "select count(1) from actor_has_category_set ahcs where ahcs.actor_id = ?").setParameter(1, actorId)
                                                    .getSingleResult() > 0;
      Query query;
      String likeName = searchText == null ? "" : searchText;

      if (hasCategorySets) {
        String sql = """
                select distinct c.id, c.name from category c
                join category_set_has_category cs on c.id = cs.category_id
                where lower(c.name) like lower(concat('%', ?,'%'))
                and cs.category_set_id in (
                    select ahcs.category_set_id from actor_has_category_set ahcs
                    where ahcs.actor_id = ?)
                order by c.name asc
                """;
        query = entityManager.createNativeQuery(sql).setParameter(1, likeName).setParameter(2, actorId);
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

  public List<Category> getRemovedCategoriesAfterCategorySetChange(int actorId, Collection<Integer> categorySetIds) {
    if (categorySetIds.isEmpty()) {
      return Collections.emptyList();
    }
    String inPlaceHolder = DbQueryStringUtil.createInPlaceHolderValue(categorySetIds.size());

    String queryString = """
                     select c.id, c.name
                     from actor_has_category ahc
                     join category c on c.id = ahc.category_id
                     where ahc.actor_id = ? and not exists(select 1
                                      from category_set_has_category cshc
                                      where cshc.category_id = ahc.category_id and cshc.category_set_id in %s)
            """.formatted(inPlaceHolder);

    return executeDbTransaction(entityManager -> {
      Query query = entityManager.createNativeQuery(queryString, Category.class).setParameter(1, actorId);

      int currentParameterIndex = 2;
      for (int currentCategorySetId : categorySetIds) {
        query.setParameter(currentParameterIndex++, currentCategorySetId);
      }

      return query.getResultList();
    });
  }

  public Collection<CategorySet> updateAssignedCategorySetsOfActor(int actorId, List<Integer> categorySetIds) {
    return executeDbTransaction(entityManager -> {
      Actor actor = entityManager.find(Actor.class, actorId, LockModeType.PESSIMISTIC_WRITE);

      List<CategorySet> categorySets = categorySetIds.isEmpty() ? Collections.emptyList() : entityManager.createQuery(
              "select categorySet from CategorySet categorySet where categorySet.id in :categorySetIds",
              CategorySet.class).setParameter("categorySetIds", categorySetIds).getResultList();
      actor.setCategorySets(categorySets);

      if (!categorySetIds.isEmpty()) {
        String inPlaceHolder = DbQueryStringUtil.createInPlaceHolderValue(categorySetIds.size());
        String deleteQueryStatement = """
                delete from actor_has_category ahc where ahc.actor_id = ? and not exists (
                    select 1 from category c
                        join category_set_has_category cshc on cshc.category_id = c.id
                        where c.id = ahc.category_id and cshc.category_set_id in %s
                    )
                """.formatted(inPlaceHolder);
        Query deleteQuery = entityManager.createNativeQuery(deleteQueryStatement).setParameter(1, actorId);

        for (int i = 0; i < categorySetIds.size(); i++) {
          int parameterIndex = i + 2;
          deleteQuery.setParameter(parameterIndex, categorySetIds.get(i));
        }

        deleteQuery.executeUpdate();
      }
      return categorySets;
    });
  }
}
