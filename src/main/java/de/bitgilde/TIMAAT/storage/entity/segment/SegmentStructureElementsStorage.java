package de.bitgilde.TIMAAT.storage.entity.segment;

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
import de.bitgilde.TIMAAT.model.FIPOP.CategorySetHasCategory;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySet_;
import de.bitgilde.TIMAAT.model.FIPOP.Category_;
import de.bitgilde.TIMAAT.model.FIPOP.MediumAnalysisList;
import de.bitgilde.TIMAAT.model.FIPOP.MediumAnalysisList_;
import de.bitgilde.TIMAAT.model.FIPOP.SegmentStructureEntity;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccountHasMediumAnalysisList;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccountHasMediumAnalysisList_;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount_;
import de.bitgilde.TIMAAT.storage.db.DbStorage;
import de.bitgilde.TIMAAT.storage.entity.segment.api.SegmentStructureElementFilterCriteria;
import de.bitgilde.TIMAAT.storage.entity.segment.api.SegmentStructureElementType;
import de.bitgilde.TIMAAT.storage.entity.segment.api.SegmentStructureSortingField;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
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
import java.util.stream.Collectors;

/**
 * Storage responsible to access and modify {@link AnalysisSegment}s
 *
 * @author Nico Kotlenga
 * @since 31.12.25
 */
public class SegmentStructureElementsStorage extends DbStorage<AnalysisSegmentStructureElement, SegmentStructureElementFilterCriteria, SegmentStructureSortingField> {

  private static final Map<SegmentStructureElementType, Class<? extends SegmentStructureEntity>> SEGMENT_STRUCTURE_ENTITY_CLASS_BY_SEGMENT_STRUCTURE_TYPE = Map.of(
          SegmentStructureElementType.SEGMENT, AnalysisSegment.class, SegmentStructureElementType.SEQUENCE,
          AnalysisSequence.class, SegmentStructureElementType.TAKE, AnalysisTake.class,
          SegmentStructureElementType.SCENE, AnalysisScene.class, SegmentStructureElementType.ACTION,
          AnalysisAction.class);


  @Inject
  public SegmentStructureElementsStorage(EntityManagerFactory emf) {
    super(AnalysisSegmentStructureElement.class, SegmentStructureSortingField.ID, emf);
  }

  public List<Category> updateCategories(int segmentStructureId, SegmentStructureElementType segmentStructureElementType, Collection<Integer> categoryIds) {
    return executeDbTransaction(entityManager -> {
      Class<? extends SegmentStructureEntity> segmentTypeEntityClass = SEGMENT_STRUCTURE_ENTITY_CLASS_BY_SEGMENT_STRUCTURE_TYPE.get(
              segmentStructureElementType);

      SegmentStructureEntity segmentStructureEntity = entityManager.find(segmentTypeEntityClass, segmentStructureId);
      int mediumAnalysisListId = segmentStructureEntity.getMediumAnalysisList().getId();

      List<Category> categories = categoryIds.isEmpty() ? Collections.emptyList() : entityManager.createQuery(
              "select category from Category category where category.id in (select cshs.category.id from MediumAnalysisList mediumAnalysisList join mediumAnalysisList.restrictedCategorySets categorySets join categorySets.categorySetHasCategories cshs where mediumAnalysisList.id = :mediumAnalysisListId and cshs.category.id in :categoryIds)",
              Category.class).setParameter("categoryIds", categoryIds).setParameter("mediumAnalysisListId",
              mediumAnalysisListId).getResultList();

      segmentStructureEntity.setCategories(categories);
      return categories;
    });
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

  public List<Category> getAssignableCategories(int segmentStructureId, SegmentStructureElementType segmentStructureElementType) {
    return executeDbTransaction(entityManager -> {
      Class<? extends SegmentStructureEntity> segmentTypeEntityClass = SEGMENT_STRUCTURE_ENTITY_CLASS_BY_SEGMENT_STRUCTURE_TYPE.get(
              segmentStructureElementType);
      SegmentStructureEntity segmentStructureEntity = entityManager.find(segmentTypeEntityClass, segmentStructureId);
      return segmentStructureEntity.getMediumAnalysisList().getRestrictedCategorySets().stream()
                                   .flatMap(categorySet -> categorySet.getCategorySetHasCategories().stream())
                                   .map(CategorySetHasCategory::getCategory).collect(Collectors.toList());
    });
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
}
