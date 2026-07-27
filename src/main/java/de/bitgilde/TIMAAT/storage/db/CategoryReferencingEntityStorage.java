package de.bitgilde.TIMAAT.storage.db;

import jakarta.persistence.EntityManager;

import java.util.Collection;

/**
 * This interface can be assigned to storages manage entities
 * which have a relation to {@link de.bitgilde.TIMAAT.model.FIPOP.Category}s and {@link de.bitgilde.TIMAAT.model.FIPOP.CategorySet}s
 *
 * @author Nico Kotlenga (nico@nko-dev.studio) 
 * @since 27.07.26
 */
public interface CategoryReferencingEntityStorage {
  /**
   * Cleanup the referenced categories of the specified category sets.
   * A cleanup can be necessary when the assigned {@link de.bitgilde.TIMAAT.model.FIPOP.Category}s of
   * {@link de.bitgilde.TIMAAT.model.FIPOP.CategorySet}s has changed
   * @param categorySetIds
   */
  void cleanupCategoryReferencesOfCategorySets(EntityManager entityManager, Collection<Integer> categorySetIds);
}
