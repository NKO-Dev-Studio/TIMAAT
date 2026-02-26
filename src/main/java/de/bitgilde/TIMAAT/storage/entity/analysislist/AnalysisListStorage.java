package de.bitgilde.TIMAAT.storage.entity.analysislist;

import de.bitgilde.TIMAAT.db.DbAccessComponent;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySet;
import de.bitgilde.TIMAAT.model.FIPOP.MediumAnalysisList;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Storage used to modify and access {@link de.bitgilde.TIMAAT.model.FIPOP.MediumAnalysisList}s
 *
 * @author Nico Kotlenga
 * @since 19.12.25
 */
public class AnalysisListStorage extends DbAccessComponent {

  private static final Logger logger = Logger.getLogger(AnalysisListStorage.class.getName());

  @Inject
  public AnalysisListStorage(EntityManagerFactory emf) {
    super(emf);
  }

  public List<CategorySet> updateCategorySets(int analysisListId, Collection<Integer> categorySetIds) {
    logger.log(Level.FINE, "Update category sets of analysis list having id {0}", analysisListId);

    return executeDbTransaction(entityManager -> {
      MediumAnalysisList analysisList = entityManager.find(MediumAnalysisList.class, analysisListId);
      List<CategorySet> categorySets = categorySetIds.isEmpty() ? Collections.emptyList() : entityManager.createQuery(
              "select categorySet from CategorySet categorySet where categorySet.id in :categorySetIds",
              CategorySet.class).setParameter("categorySetIds", categorySetIds).getResultList();

      analysisList.setRestrictedCategorySets(categorySets);

      //TODO: add cleanup for segment structures
      if (categorySetIds.isEmpty()) {
        entityManager.createNativeQuery(
                             "delete from annotation_has_category where annotation_id in " +
                                     "(select annotation.id from annotation where annotation.medium_analysis_list_id = ?)")
                     .setParameter(1, analysisListId).executeUpdate();
      }
      else {
        String placeholders = categorySetIds.stream()
                                            .map(id -> "?")
                                            .collect(Collectors.joining(","));
        Query query = entityManager.createNativeQuery(
                             "delete from annotation_has_category where annotation_id in " +
                                     "(select annotation.id from annotation where annotation.medium_analysis_list_id = ?) " +
                                     "and category_id not in (select categorySetHasCategory.category_id from category_set_has_category categorySetHasCategory " +
                                     "where categorySetHasCategory.category_set_id in (" + placeholders + "))")
                     .setParameter(1, analysisListId);

        int index = 2;
        for(Integer currentCategorySetId: categorySetIds){
          query.setParameter(index++, currentCategorySetId);
        }
      }

      return categorySets;
    });
  }
}
