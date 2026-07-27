package de.bitgilde.TIMAAT.storage.entity;

import de.bitgilde.TIMAAT.db.DbAccessComponent;
import de.bitgilde.TIMAAT.model.FIPOP.Category;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySet;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySetHasCategory;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount;
import de.bitgilde.TIMAAT.storage.db.CategoryReferencingEntityStorage;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import org.glassfish.hk2.api.IterableProvider;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Storage to access or modify category set entities
 *
 * @author Nico Kotlenga
 * @since 21.11.25
 */
public class CategorySetStorage extends DbAccessComponent {

  private static final Logger logger = Logger.getLogger(CategorySetStorage.class.getName());

  private final Collection<CategoryReferencingEntityStorage> categoryReferencingEntityStorages;

  @Inject
  public CategorySetStorage(EntityManagerFactory emf, IterableProvider<CategoryReferencingEntityStorage> categoryReferencingEntityStoragesProvider) {
    super(emf);
    this.categoryReferencingEntityStorages = StreamSupport.stream(
            categoryReferencingEntityStoragesProvider.spliterator(), false).collect(Collectors.toSet());
  }

  public CategorySet createCategorySet(String categorySetName, int executedByUserId) {
    return executeDbTransaction(entityManager -> {
      UserAccount userAccount = entityManager.find(UserAccount.class, executedByUserId);

      CategorySet categorySet = new CategorySet();
      categorySet.setName(categorySetName);
      categorySet.setCreatedAt(Timestamp.from(Instant.now()));
      categorySet.setCreatedByUserAccount(userAccount);

      entityManager.persist(categorySet);
      return categorySet;
    });
  }

  public CategorySet updateCategorySet(int categorySetId, String categorySetName, int executedByUserId) {
    logger.log(Level.FINE, "Updating category set with id {0}", categorySetId);

    return executeDbTransaction(entityManager -> {
      UserAccount userAccount = entityManager.find(UserAccount.class, executedByUserId);

      CategorySet categorySet = entityManager.find(CategorySet.class, categorySetId);
      categorySet.setName(categorySetName);
      categorySet.setLastEditedAt(Timestamp.from(Instant.now()));
      categorySet.setLastEditedByUserAccount(userAccount);

      return categorySet;
    });
  }

  public Set<CategorySetHasCategory> updateCategoriesOfCategorySet(int categorySetId, Collection<Integer> categoryIds) {
    logger.log(Level.FINE, "Updating categories of category set with id {0}", categorySetId);

    return executeDbTransaction(entityManager -> {
      CategorySet categorySet = entityManager.find(CategorySet.class, categorySetId);
      entityManager.createQuery("delete from CategorySetHasCategory where categorySet.id = :categorySetId")
                   .setParameter("categorySetId", categorySetId).executeUpdate();

      Set<CategorySetHasCategory> result = new HashSet<>();
      for (Integer categoryId : categoryIds) {
        Category category = entityManager.find(Category.class, categoryId);

        CategorySetHasCategory categorySetHasCategory = new CategorySetHasCategory();
        categorySetHasCategory.setCategorySet(categorySet);
        categorySetHasCategory.setCategory(category);

        entityManager.persist(categorySetHasCategory);
        result.add(categorySetHasCategory);
      }
      entityManager.flush();

      categoryReferencingEntityStorages.forEach(
              currentCategoryReferencingEntityStorage -> currentCategoryReferencingEntityStorage.cleanupCategoryReferencesOfCategorySets(
                      entityManager, Collections.singleton(categorySetId)));

      return result;
    });
  }

  public List<CategorySet> getCategorySetsAssignedToCategory(int categoryId) {
    return executeDbTransaction(entityManager -> entityManager.createQuery(
            "select categorySet from CategorySet categorySet join categorySet.categorySetHasCategories categorySetHasCategories where  categorySetHasCategories.category.id = :categoryId",
            CategorySet.class).setParameter("categoryId", categoryId).getResultList());
  }
}
