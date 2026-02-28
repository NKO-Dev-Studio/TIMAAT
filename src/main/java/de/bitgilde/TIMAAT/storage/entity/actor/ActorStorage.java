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
import de.bitgilde.TIMAAT.storage.db.DbStorage;
import de.bitgilde.TIMAAT.storage.entity.actor.api.ActorFilterCriteria;
import de.bitgilde.TIMAAT.storage.entity.actor.api.ActorSortingField;
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
import java.util.stream.Collectors;
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

    if(filter != null){
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
        Join<Actor, CategorySet> actorCategorySetJoin = root.join(Actor_.restrictedCategorySets);
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
    return executeDbTransaction(entityManager -> entityManager.getReference(Actor.class, actorId).getRestrictedCategorySets());
  }

  public Collection<Category> getCategoriesOfActor(int actorId) {
    return executeDbTransaction(entityManager -> entityManager.getReference(Actor.class, actorId).getCategories());
  }

  public Collection<Category> updateAssignedCategoriesOfActor(int actorId, List<Integer> categoryIds) {
    return executeDbTransaction(entityManager -> {
      Actor actor = entityManager.find(Actor.class, actorId, LockModeType.PESSIMISTIC_WRITE);

      List<Category> categories = categoryIds.isEmpty() ? Collections.emptyList() : entityManager.createQuery(
              "select category from Actor actor join actor.restrictedCategorySets categorySets join categorySets.categorySetHasCategories cshc join cshc.category category where category.id in :categoryIds",
              Category.class).setParameter("categoryIds", categoryIds).getResultList();
      actor.setCategories(categories);
      return categories;
    });
  }

  public Stream<Category> getAssignableCategoriesOfActor(int actorId) {
    return executeStreamDbTransaction(entityManager -> entityManager.createQuery(
            "select distinct category from Actor actor join actor.restrictedCategorySets categorySet join categorySet.categorySetHasCategories cshc join cshc.category category where actor.id = :actorId",
            Category.class).setParameter("actorId", actorId).getResultStream());
  }

  public Collection<CategorySet> updateAssignedCategorySetsOfActor(int actorId, List<Integer> categorySetIds) {
    return executeDbTransaction(entityManager -> {
      Actor actor = entityManager.find(Actor.class, actorId, LockModeType.PESSIMISTIC_WRITE);

      List<CategorySet> categorySets = categorySetIds.isEmpty() ? Collections.emptyList() : entityManager.createQuery(
              "select categorySet from CategorySet categorySet where categorySet.id in :categorySetIds",
              CategorySet.class).setParameter("categorySetIds", categorySetIds).getResultList();
      actor.setRestrictedCategorySets(categorySets);

      //Delete categories not part of the new assigned category sets
      if (categorySetIds.isEmpty()) {
        entityManager.createNativeQuery("delete from actor_has_category where actor_id = ?").setParameter(1, actorId)
                     .executeUpdate();
      }
      else {
        String placeholders = categorySetIds.stream().map(id -> "?").collect(Collectors.joining(","));
        Query query = entityManager.createNativeQuery(
                                           "delete from actor_has_category where actor_id = ? and not exists (select 1 from category_set_has_category cshc where cshc.category_id = actor_has_category.category_id and cshc.category_set_id in (" + placeholders + "))")
                                   .setParameter(1, actorId);

        int index = 2;
        for (Integer currentCategorySetId : categorySetIds) {
          query.setParameter(index++, currentCategorySetId);
        }

        query.executeUpdate();
      }

      return categorySets;
    });
  }
}
