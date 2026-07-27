package de.bitgilde.TIMAAT.storage.entity.segment;

import de.bitgilde.TIMAAT.db.util.DbQueryStringUtil;
import de.bitgilde.TIMAAT.model.FIPOP.AnalysisAction;
import de.bitgilde.TIMAAT.model.FIPOP.AnalysisScene;
import de.bitgilde.TIMAAT.model.FIPOP.AnalysisSegment;
import de.bitgilde.TIMAAT.model.FIPOP.AnalysisSegmentStructureElement;
import de.bitgilde.TIMAAT.model.FIPOP.AnalysisSegmentStructureElementId_;
import de.bitgilde.TIMAAT.model.FIPOP.AnalysisSegmentStructureElement_;
import de.bitgilde.TIMAAT.model.FIPOP.AnalysisSequence;
import de.bitgilde.TIMAAT.model.FIPOP.AnalysisTake;
import de.bitgilde.TIMAAT.model.FIPOP.Category;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySet;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySet_;
import de.bitgilde.TIMAAT.model.FIPOP.Category_;
import de.bitgilde.TIMAAT.model.FIPOP.MediumAnalysisList;
import de.bitgilde.TIMAAT.model.FIPOP.MediumAnalysisList_;
import de.bitgilde.TIMAAT.model.FIPOP.SegmentStructureEntity;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccountHasMediumAnalysisList;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccountHasMediumAnalysisList_;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount_;
import de.bitgilde.TIMAAT.storage.api.ReducedEntity;
import de.bitgilde.TIMAAT.storage.db.CategoryReferencingEntityStorage;
import de.bitgilde.TIMAAT.storage.db.DbStorage;
import de.bitgilde.TIMAAT.storage.entity.analysislist.AnalysisListStorage;
import de.bitgilde.TIMAAT.storage.entity.segment.api.SegmentStructureElementFilterCriteria;
import de.bitgilde.TIMAAT.storage.entity.segment.api.SegmentStructureElementType;
import de.bitgilde.TIMAAT.storage.entity.segment.api.SegmentStructureSortingField;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
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

/**
 * Storage responsible to access and modify {@link AnalysisSegment}s
 *
 * @author Nico Kotlenga
 * @since 31.12.25
 */
public class SegmentStructureElementsStorage extends DbStorage<AnalysisSegmentStructureElement, SegmentStructureElementFilterCriteria, SegmentStructureSortingField> implements CategoryReferencingEntityStorage {

  private static final Logger logger = Logger.getLogger(SegmentStructureElementsStorage.class.getName());

  private static final Map<SegmentStructureElementType, Class<? extends SegmentStructureEntity>> SEGMENT_STRUCTURE_ENTITY_CLASS_BY_SEGMENT_STRUCTURE_TYPE = Map.of(
          SegmentStructureElementType.SEGMENT, AnalysisSegment.class, SegmentStructureElementType.SEQUENCE,
          AnalysisSequence.class, SegmentStructureElementType.TAKE, AnalysisTake.class,
          SegmentStructureElementType.SCENE, AnalysisScene.class, SegmentStructureElementType.ACTION,
          AnalysisAction.class);


  private final AnalysisListStorage analysisListStorage;

  @Inject
  public SegmentStructureElementsStorage(EntityManagerFactory emf, AnalysisListStorage analysisListStorage) {
    super(AnalysisSegmentStructureElement.class, SegmentStructureSortingField.ID, emf);
    this.analysisListStorage = analysisListStorage;
  }

  public List<Category> updateCategories(int segmentStructureId, SegmentStructureElementType segmentStructureElementType, List<Integer> categoryIds) {
    return executeDbTransaction(entityManager -> {
      Class<? extends SegmentStructureEntity> segmentTypeEntityClass = SEGMENT_STRUCTURE_ENTITY_CLASS_BY_SEGMENT_STRUCTURE_TYPE.get(
              segmentStructureElementType);

      SegmentStructureEntity segmentStructureEntity = entityManager.find(segmentTypeEntityClass, segmentStructureId);
      int mediumAnalysisListId = segmentStructureEntity.getMediumAnalysisList().getId();
      entityManager.lock(segmentStructureEntity.getMediumAnalysisList(), LockModeType.PESSIMISTIC_READ);

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
                                  from medium_analysis_list_has_category_set mhcs
                                  where mhcs.medium_analysis_list_id = ?) or exists(select 1
                                                                     from medium_analysis_list_has_category_set mhcs
                                                                     where mhcs.medium_analysis_list_id = ?
                                                                       and cshc.category_set_id = mhcs.category_set_id))
                      and c.id in %s
                """.formatted(inPlaceHolder);
        Query categoryQuery = entityManager.createNativeQuery(query, Category.class)
                                           .setParameter(1, mediumAnalysisListId).setParameter(2, mediumAnalysisListId);

        for (int i = 0; i < categoryIds.size(); i++) {
          int parameterIndex = i + 3;
          categoryQuery.setParameter(parameterIndex, categoryIds.get(i));
        }
        updatedCategories = categoryQuery.getResultList();
      }

      segmentStructureEntity.setCategories(updatedCategories);
      return updatedCategories;
    });
  }

  private int getMediumAnalysisListIdOfSegmentStructureElement(SegmentStructureElementType segmentStructureElementType, int segmentStructureId) {
    return executeDbTransaction(entityManager -> entityManager.createQuery(
            "select mediumAnalysisList.id from AnalysisSegmentStructureElement segmentStructureElement join segmentStructureElement.mediumAnalysisList mediumAnalysisList where segmentStructureElement.id.id = :id and segmentStructureElement.id.structureElementType = :type",
            Integer.class).setParameter("id", segmentStructureId).setParameter("type",
            segmentStructureElementType.toString()).getSingleResult());
  }

  public MediumAnalysisList getMediumAnalysisListOfSegmentStructureElement(SegmentStructureElementType segmentStructureElementType, int segmentStructureId) {
    return executeDbTransaction(entityManager -> entityManager.createQuery(
            "select mediumAnalysisList from AnalysisSegmentStructureElement segmentStructureElement join segmentStructureElement.mediumAnalysisList mediumAnalysisList where segmentStructureElement.id.id = :id and segmentStructureElement.id.structureElementType = :type",
            MediumAnalysisList.class).setParameter("id", segmentStructureId).setParameter("type",
            segmentStructureElementType.toString()).getSingleResult());
  }

  public List<Category> getAssignedCategories(int segmentStructureId, SegmentStructureElementType segmentStructureElementType) {
    return executeDbTransaction(entityManager -> {
      Class<? extends SegmentStructureEntity> segmentTypeEntityClass = SEGMENT_STRUCTURE_ENTITY_CLASS_BY_SEGMENT_STRUCTURE_TYPE.get(
              segmentStructureElementType);
      SegmentStructureEntity segmentStructureEntity = entityManager.find(segmentTypeEntityClass, segmentStructureId);
      return segmentStructureEntity.getCategories();
    });
  }

  public Stream<ReducedEntity<Integer>> getAssignableCategories(int segmentStructureId, SegmentStructureElementType segmentStructureElementType, @Nullable String searchText) {
    int mediumAnalysisListId = getMediumAnalysisListIdOfSegmentStructureElement(segmentStructureElementType,
            segmentStructureId);
    return analysisListStorage.getAssignableCategoriesOfAnalysisList(mediumAnalysisListId, searchText);
  }

  @Override
  protected List<Predicate> createPredicates(SegmentStructureElementFilterCriteria filter, Root<AnalysisSegmentStructureElement> root, CriteriaBuilder criteriaBuilder, CriteriaQuery<?> criteriaQuery, UserAccount userAccount) {
    List<Predicate> predicates = new ArrayList<>();

    if (filter != null) {
      if (filter.getSegmentStructureElementNameSearch().isPresent()) {
        String searchText = filter.getSegmentStructureElementNameSearch().get();
        predicates.add(criteriaBuilder.like(root.get(AnalysisSegmentStructureElement_.name), "%" + searchText + "%"));
      }

      boolean categoryFilterActive = filter.getCategoryIds().isPresent() && !filter.getCategoryIds().get().isEmpty();
      if (categoryFilterActive) {
        Join<AnalysisSegmentStructureElement, Category> categoryJoin = root.join(
                AnalysisSegmentStructureElement_.categories);
        predicates.add(categoryJoin.get(Category_.id).in(filter.getCategoryIds().get()));
      }

      boolean categorySetFilterActive = filter.getCategorySetIds().isPresent() && !filter.getCategorySetIds().get()
                                                                                         .isEmpty();
      if (categorySetFilterActive) {
        Join<AnalysisSegmentStructureElement, MediumAnalysisList> mediumAnalysisListJoin = root.join(
                AnalysisSegmentStructureElement_.mediumAnalysisList);
        Join<MediumAnalysisList, CategorySet> categorySetJoin = mediumAnalysisListJoin.join(
                MediumAnalysisList_.categorySets);
        predicates.add(categorySetJoin.get(CategorySet_.id).in(filter.getCategorySetIds().get()));
      }

      if (filter.getSegmentStructureElementTypes().isPresent() && !filter.getSegmentStructureElementTypes().get()
                                                                         .isEmpty()) {
        predicates.add(root.get(AnalysisSegmentStructureElement_.id)
                           .get(AnalysisSegmentStructureElementId_.structureElementType)
                           .in(filter.getSegmentStructureElementTypes().get()));
      }
    }

    if (userAccount != null) {
      Join<AnalysisSegmentStructureElement, MediumAnalysisList> mediumAnalysisListJoin = root.join(
              AnalysisSegmentStructureElement_.mediumAnalysisList);
      Join<MediumAnalysisList, UserAccountHasMediumAnalysisList> userAccountHasMediumAnalysisList = mediumAnalysisListJoin.join(
              MediumAnalysisList_.userAccountHasMediumAnalysisLists);
      Join<UserAccountHasMediumAnalysisList, UserAccount> userAccountJoin = userAccountHasMediumAnalysisList.join(
              UserAccountHasMediumAnalysisList_.userAccount);

      Predicate mediumAnalysisListHasGlobalAccess = criteriaBuilder.greaterThanOrEqualTo(
              mediumAnalysisListJoin.get(MediumAnalysisList_.globalPermission), (byte) 1);
      Predicate userHasAccess = criteriaBuilder.equal(userAccountJoin.get(UserAccount_.id), userAccount.getId());

      predicates.add(criteriaBuilder.or(mediumAnalysisListHasGlobalAccess, userHasAccess));
    }

    return predicates;
  }

  @Override
  public void cleanupCategoryReferencesOfCategorySets(EntityManager entityManager, Collection<Integer> categorySetIds) {
    logger.log(Level.FINE, "Cleanup category references of annotation entities related to category sets {0}",
            categorySetIds);
    String idInPlaceholder = DbQueryStringUtil.createInPlaceHolderValue(categorySetIds.size());
    String idQueryStatement = """
            select m.id from medium_analysis_list m
            where m.id in (
                select mhcs.medium_analysis_list_id from medium_analysis_list_has_category_set mhcs
                                     where mhcs.category_set_id in %s
            )
            order by m.id asc
            for share
            """.formatted(idInPlaceholder);
    Query idQuery = entityManager.createNativeQuery(idQueryStatement);
    int currentIdQueryParameterIndex = 1;
    for (int currentCategorySetId : categorySetIds) {
      idQuery.setParameter(currentIdQueryParameterIndex++, currentCategorySetId);
    }
    List<Integer> ids = ((List<Number>) idQuery.getResultList()).stream().map(Number::intValue).toList();

    if (!ids.isEmpty()) {
      String deleteUnreferencedInPlaceHolder = DbQueryStringUtil.createInPlaceHolderValue(ids.size());
      String deleteUnreferencedCategoriesQueryStatement = """
              delete ahc from analysis_segment_has_category ahc
              join analysis_segment a on ahc.analysis_segment_id = a.id
              where a.analysis_list_id in %s
              and ahc.category_id not in (
                  select cscs.category_id from medium_analysis_list_has_category_set mhcs
                  join category_set_has_category cscs on mhcs.category_set_id = cscs.category_set_id
                  where mhcs.medium_analysis_list_id = a.analysis_list_id
              )
              """.formatted(deleteUnreferencedInPlaceHolder);
      Query deleteUnreferencedCategoriesQuery = entityManager.createNativeQuery(
              deleteUnreferencedCategoriesQueryStatement);
      int currentDeleteUnreferencedCategoriesQueryParameterIndex = 1;
      for (int currentId : ids) {
        deleteUnreferencedCategoriesQuery.setParameter(currentDeleteUnreferencedCategoriesQueryParameterIndex++,
                currentId);
      }
      deleteUnreferencedCategoriesQuery.executeUpdate();
    }
  }
}
