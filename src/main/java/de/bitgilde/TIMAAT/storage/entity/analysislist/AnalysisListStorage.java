package de.bitgilde.TIMAAT.storage.entity.analysislist;

import de.bitgilde.TIMAAT.db.DbAccessComponent;
import de.bitgilde.TIMAAT.db.util.DbQueryStringUtil;
import de.bitgilde.TIMAAT.model.FIPOP.Category;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySet;
import de.bitgilde.TIMAAT.model.FIPOP.MediumAnalysisList;
import de.bitgilde.TIMAAT.storage.api.ReducedEntity;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
  
  @SuppressWarnings("unchecked")
  public List<Category> getRemovedCategoriesAfterCategorySetChange(int analysisListId, List<Integer> categorySetIds) {
    List<Category> removedCategories;
    if (categorySetIds.isEmpty()) {
      removedCategories = Collections.emptyList();

    }
    else {
      String inPlaceHolder = DbQueryStringUtil.createInPlaceHolderValue(categorySetIds.size());
      removedCategories = executeDbTransaction(entityManager -> {
        String queryStatement = """
                select distinct c.id, c.name
                from category c
                where c.id in (
                    select ahc.category_id
                    from annotation_has_category ahc
                    join annotation a on a.id = ahc.annotation_id
                    where a.medium_analysis_list_id = ?
                    union
                    select ashc.category_id
                    from analysis_segement_has_category ashc
                    join analysis_segment s on s.id = ashc.analysis_segment_id
                    where s.analysis_list_id = ?
                )
                and c.id not in (
                    select cscs.category_id
                    from category_set_has_category_set cscs
                    where cscs.category_set_id in %s
                )
                """.formatted(inPlaceHolder);
        Query query = entityManager.createNativeQuery(queryStatement, Category.class).setParameter(1, analysisListId)
                                   .setParameter(2, analysisListId);
        for (int i = 0; i < categorySetIds.size(); i++) {
          int parameterIndex = i + 3;
          query.setParameter(parameterIndex, categorySetIds);
        }

        return query.getResultList();
      });
    }

    return removedCategories;
  }

  public List<CategorySet> updateCategorySets(int analysisListId, List<Integer> categorySetIds) {
    logger.log(Level.FINE, "Update category sets of analysis list having id {0}", analysisListId);

    return executeDbTransaction(entityManager -> {
      MediumAnalysisList analysisList = entityManager.find(MediumAnalysisList.class, analysisListId,
              LockModeType.PESSIMISTIC_WRITE);
      List<CategorySet> categorySets = categorySetIds.isEmpty() ? Collections.emptyList() : entityManager.createQuery(
              "select categorySet from CategorySet categorySet where categorySet.id in :categorySetIds",
              CategorySet.class).setParameter("categorySetIds", categorySetIds).getResultList();
      analysisList.setCategorySets(categorySets);

      if (!categorySets.isEmpty()) {
        String inPlaceHolder = DbQueryStringUtil.createInPlaceHolderValue(categorySetIds.size());
        String deleteAnnotationCategoryQueryStatement = """
                delete from annotation_has_category ahc where ahc.annotation_id in
                    (select a.id from annotation a
                        where a.medium_analysis_list_id = ?)
                and not exists (
                    select 1 from category c
                        join category_set_has_category cshc on cshc.category_id = c.id
                        where c.id = ahc.category_id and cshc.category_set_id in %s
                )
                """.formatted(inPlaceHolder);
        Query deleteAnnotationCategoryQuery = entityManager.createNativeQuery(deleteAnnotationCategoryQueryStatement)
                                                           .setParameter(1, analysisListId);

        String deleteSegmentStructureElementCategoryQueryStatement = """
                delete from analysis_segment_has_category ashc where ashc.analysis_segment_id in
                   (select a.id from analysis_segment a
                        where a.analysis_list_id = ?)
                and not exists (
                        select 1 from category c
                        join category_set_has_category cshc on cshc.category_id = c.id
                        where c.id = ashc.category_id and cshc.category_set_id in %s
                )
                """.formatted(inPlaceHolder);
        Query deleteSegmentStructureElementCategoryQuery = entityManager.createNativeQuery(
                deleteSegmentStructureElementCategoryQueryStatement).setParameter(1, analysisListId);

        for (int i = 0; i < categorySetIds.size(); i++) {
          int parameterIndex = i + 2;
          deleteAnnotationCategoryQuery.setParameter(parameterIndex, categorySetIds.get(i));
          deleteSegmentStructureElementCategoryQuery.setParameter(parameterIndex, categorySetIds.get(i));
        }

        deleteAnnotationCategoryQuery.executeUpdate();
        deleteSegmentStructureElementCategoryQuery.executeUpdate();
      }

      return categorySets;
    });
  }
}
