package de.bitgilde.TIMAAT.storage.entity;

import de.bitgilde.TIMAAT.db.DbAccessComponent;
import de.bitgilde.TIMAAT.model.FIPOP.Category;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySet;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySetHasCategory;
import de.bitgilde.TIMAAT.storage.db.CategoryReferencingEntityStorage;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import org.glassfish.hk2.api.IterableProvider;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
 * Storage used to change and read category information
 *
 * @author Nico Kotlenga
 * @since 21.11.25
 */
public class CategoryStorage extends DbAccessComponent {

  private static final Logger logger = Logger.getLogger(CategoryStorage.class.getName());

  private final Collection<CategoryReferencingEntityStorage> categoryReferencingEntityStorages;

  @Inject
  public CategoryStorage(EntityManagerFactory emf, IterableProvider<CategoryReferencingEntityStorage> categoryReferencingEntityStoragesProvider) {
    super(emf);
    this.categoryReferencingEntityStorages = StreamSupport.stream(
            categoryReferencingEntityStoragesProvider.spliterator(), false).collect(Collectors.toSet());
  }

  public Category createCategory(String categoryName) {
    logger.log(Level.FINE, "Creating new category");

    return executeDbTransaction(entityManager -> {
      Category category = new Category();
      category.setName(categoryName);

      entityManager.persist(category);
      return category;
    });
  }

  public Category updateCategory(int categoryId, String categoryName) {
    logger.log(Level.FINE, "Updating category with id {0}", categoryId);

    return executeDbTransaction(entityManager -> {
      Category category = entityManager.find(Category.class, categoryId);
      category.setName(categoryName);

      return category;
    });
  }

  @SuppressWarnings("unchecked")
  public Set<CategorySetHasCategory> updateCategorySetsOfCategory(int categoryId, Collection<Integer> categorySetIds) {
    logger.log(Level.FINE, "Updating category sets of category with id {0}", categoryId);

    return executeDbTransaction(entityManager -> {
      Category category = entityManager.find(Category.class, categoryId);
      Collection<Integer> previousAssignedCategorySetIds = ((List<Number>) entityManager.createQuery(
                                                                                                "select categorySet.id from CategorySetHasCategory where category.id = :categoryId")
                                                                                        .setParameter("categoryId",
                                                                                                categoryId)
                                                                                        .getResultList()).stream()
                                                                                                         .map(Number::intValue)
                                                                                                         .toList();

      entityManager.createQuery("delete from CategorySetHasCategory where category.id = :categoryId")
                   .setParameter("categoryId", categoryId).executeUpdate();

      Set<CategorySetHasCategory> result = new HashSet<>();
      for (Integer categorySetId : categorySetIds) {
        CategorySet categorySet = entityManager.find(CategorySet.class, categorySetId);

        CategorySetHasCategory categorySetHasCategory = new CategorySetHasCategory();
        categorySetHasCategory.setCategorySet(categorySet);
        categorySetHasCategory.setCategory(category);

        entityManager.persist(categorySetHasCategory);
        result.add(categorySetHasCategory);
      }

      Collection<Integer> removedCategorySetIds = previousAssignedCategorySetIds.stream()
                                                                                .filter(currentPreviousAssignedCategorySetId -> !categorySetIds.contains(
                                                                                        currentPreviousAssignedCategorySetId))
                                                                                .toList();
      if (!removedCategorySetIds.isEmpty()) {
        entityManager.flush();
        categoryReferencingEntityStorages.forEach(
                storage -> storage.cleanupCategoryReferencesOfCategorySets(entityManager, removedCategorySetIds));
      }

      return result;
    });
  }

  public List<Category> getCategoriesAssignedToCategorySet(int categorySetId) {
    logger.log(Level.FINE, "Loading categories assigned to category set with id {0}", categorySetId);

    return executeDbTransaction(entityManager -> entityManager.createQuery(
            "select category from Category category join category.categorySetHasCategories categorySetsHasCategories where categorySetsHasCategories.categorySet.id = :categorySetId",
            Category.class).setParameter("categorySetId", categorySetId).getResultList());
  }
}
