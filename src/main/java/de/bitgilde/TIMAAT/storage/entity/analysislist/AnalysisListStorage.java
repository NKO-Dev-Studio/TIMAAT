package de.bitgilde.TIMAAT.storage.entity.analysislist;

import de.bitgilde.TIMAAT.db.DbAccessComponent;
import de.bitgilde.TIMAAT.model.FIPOP.Category;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySet;
import de.bitgilde.TIMAAT.model.FIPOP.MediumAnalysisList;
import de.bitgilde.TIMAAT.storage.api.ReducedEntity;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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


  /**
   * Calculates the assignable categories of the analysis list
   * @param analysisListId
   * @param searchText
   * @return the assignable categories as {@link Stream} of {@link ReducedEntity}
   */
  @SuppressWarnings("unchecked")
  public Stream<ReducedEntity<Integer>> getAssignableCategoriesOfAnalysisList(int analysisListId, @Nullable String searchText) {
    return executeStreamDbTransaction(entityManager -> {
      String likeName = searchText == null ? "" : searchText;
      boolean hasCategorySets = (long) entityManager.createNativeQuery(
                                                            "select count(1) from medium_analysis_list_has_category_set mhcs where mhcs.medium_analysis_list_id = ?")
                                                    .setParameter(1, analysisListId).getSingleResult() > 0;
      Query query;
      if (hasCategorySets) {
        String sql = """
                select distinct c.id, c.name from category c
                join category_set_has_category cs on c.id = cs.category_id
                where lower(c.name) like lower(concat('%', ?,'%'))
                and cs.category_set_id in (
                    select mhcs.category_set_id from medium_analysis_list_has_category_set mhcs
                    where mhcs.medium_analysis_list_id = ?)
                order by c.name asc
                """;
        query = entityManager.createNativeQuery(sql).setParameter(1, likeName).setParameter(2, analysisListId);
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

  //TODO: Implement method
  public List<Category> getRemovedCategoriesAfterCategorySetChange(int analysisListId, Collection<Integer> categorySetIds) {
    if (categorySetIds.isEmpty()) {
      return Collections.emptyList();
    }


    return Collections.emptyList();
  }

  public List<CategorySet> updateCategorySets(int analysisListId, Collection<Integer> categorySetIds) {
    logger.log(Level.FINE, "Update category sets of analysis list having id {0}", analysisListId);

    return executeDbTransaction(entityManager -> {
      MediumAnalysisList analysisList = entityManager.find(MediumAnalysisList.class, analysisListId);
      List<CategorySet> categorySets = categorySetIds.isEmpty() ? Collections.emptyList() : entityManager.createQuery(
              "select categorySet from CategorySet categorySet where categorySet.id in :categorySetIds",
              CategorySet.class).setParameter("categorySetIds", categorySetIds).getResultList();

      analysisList.setCategorySets(categorySets);

      //TODO: add cleanup for segment structures
      if (categorySetIds.isEmpty()) {
        entityManager.createNativeQuery(
                             "delete from annotation_has_category where annotation_id in " + "(select annotation.id from annotation where annotation.medium_analysis_list_id = ?)")
                     .setParameter(1, analysisListId).executeUpdate();
      }
      else {
        String placeholders = categorySetIds.stream().map(id -> "?").collect(Collectors.joining(","));
        Query query = entityManager.createNativeQuery(
                                           "delete from annotation_has_category where annotation_id in " + "(select annotation.id from annotation where annotation.medium_analysis_list_id = ?) " + "and category_id not in (select categorySetHasCategory.category_id from category_set_has_category categorySetHasCategory " + "where categorySetHasCategory.category_set_id in (" + placeholders + "))")
                                   .setParameter(1, analysisListId);

        int index = 2;
        for (Integer currentCategorySetId : categorySetIds) {
          query.setParameter(index++, currentCategorySetId);
        }
      }

      return categorySets;
    });
  }
}
