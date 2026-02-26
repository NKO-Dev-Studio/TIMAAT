package de.bitgilde.TIMAAT.storage.entity.annotation;

import de.bitgilde.TIMAAT.db.exception.DbTransactionExecutionException;
import de.bitgilde.TIMAAT.model.FIPOP.Annotation;
import de.bitgilde.TIMAAT.model.FIPOP.AnnotationHasMusic;
import de.bitgilde.TIMAAT.model.FIPOP.AnnotationHasMusicPK;
import de.bitgilde.TIMAAT.model.FIPOP.AnnotationHasMusicTranslationArea;
import de.bitgilde.TIMAAT.model.FIPOP.AnnotationHasMusicTranslationAreaPK;
import de.bitgilde.TIMAAT.model.FIPOP.AnnotationTranslation;
import de.bitgilde.TIMAAT.model.FIPOP.AnnotationTranslation_;
import de.bitgilde.TIMAAT.model.FIPOP.Annotation_;
import de.bitgilde.TIMAAT.model.FIPOP.Category;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySet;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySet_;
import de.bitgilde.TIMAAT.model.FIPOP.Category_;
import de.bitgilde.TIMAAT.model.FIPOP.Language;
import de.bitgilde.TIMAAT.model.FIPOP.MediumAnalysisList;
import de.bitgilde.TIMAAT.model.FIPOP.MediumAnalysisList_;
import de.bitgilde.TIMAAT.model.FIPOP.Music;
import de.bitgilde.TIMAAT.model.FIPOP.MusicTranslation;
import de.bitgilde.TIMAAT.model.FIPOP.MusicTranslationPK;
import de.bitgilde.TIMAAT.model.FIPOP.SelectorSvg;
import de.bitgilde.TIMAAT.model.FIPOP.SvgShapeType;
import de.bitgilde.TIMAAT.model.FIPOP.Tag;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccountHasMediumAnalysisList;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccountHasMediumAnalysisList_;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount_;
import de.bitgilde.TIMAAT.model.IndexBasedRange;
import de.bitgilde.TIMAAT.storage.db.DbStorage;
import de.bitgilde.TIMAAT.storage.entity.TagStorage;
import de.bitgilde.TIMAAT.storage.entity.annotation.api.AnnotationFilterCriteria;
import de.bitgilde.TIMAAT.storage.entity.annotation.api.AnnotationSortingField;
import de.bitgilde.TIMAAT.storage.entity.annotation.api.AnnotationType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.websocket.server.PathParam;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
 * This storage is responsible to access and modify Annotation information
 *
 * @author Nico Kotlenga
 * @since 03.09.25
 */
public class AnnotationStorage extends DbStorage<Annotation, AnnotationFilterCriteria, AnnotationSortingField> {

  private static final Logger logger = Logger.getLogger(AnnotationStorage.class.getName());

  private final TagStorage tagStorage;

  @Inject
  public AnnotationStorage(EntityManagerFactory emf, TagStorage tagStorage) {
    super(Annotation.class, AnnotationSortingField.ID, emf);
    this.tagStorage = tagStorage;
  }

  public boolean removeTranscriptionAreaFromAnnotationHasMusicForLanguage(int annotationId, int musicId, int languageId) throws DbTransactionExecutionException {
    logger.log(Level.FINE, "Removing transcription area from annotation {0} for music {1} in language {2} ",
            new Object[]{annotationId, musicId, languageId});
    return this.executeDbTransaction(entityManager -> {
      AnnotationHasMusicTranslationArea existingMusicTranslationArea = entityManager.find(
              AnnotationHasMusicTranslationArea.class,
              new AnnotationHasMusicTranslationAreaPK(musicId, annotationId, languageId));
      if (existingMusicTranslationArea != null) {
        entityManager.remove(existingMusicTranslationArea);
        return true;
      }

      return false;
    });
  }

  public List<Tag> updateTagsOfAnnotation(int annotationId, Collection<String> tagNames) throws DbTransactionExecutionException {
    return executeDbTransaction(entityManager -> {
      Annotation annotation = entityManager.find(Annotation.class, annotationId);
      List<Tag> updatedTagList = tagStorage.getOrCreateTagsHavingNames(tagNames);

      annotation.setTags(updatedTagList);
      return updatedTagList;
    });
  }

  public List<Category> updateCategoriesOfAnnotation(int annotationId, Collection<Integer> categoryIds) throws DbTransactionExecutionException {
    return executeDbTransaction(entityManager -> {
      Annotation currentAnnotation = entityManager.find(Annotation.class, annotationId);

      List<Category> categories = categoryIds.isEmpty() ? Collections.emptyList() : entityManager.createQuery(
              "select category from Category category where category.id in (select categorySetHasCategories.category.id from MediumAnalysisList mediumAnalysisList join mediumAnalysisList.restrictedCategorySets categorySets join categorySets.categorySetHasCategories categorySetHasCategories where categorySetHasCategories.category.id in :categoryIds)",
              Category.class).setParameter("categoryIds", categoryIds).getResultList();

      currentAnnotation.setCategories(categories);
      return categories;
    });
  }

  public Annotation createAnnotation(CreateAnnotation createAnnotation, UserAccount userAccount) throws DbTransactionExecutionException {
    logger.log(Level.FINE, "Creating new annotation");

    return this.executeDbTransaction(entityManager -> {
      MediumAnalysisList mediumAnalysisList = entityManager.find(MediumAnalysisList.class,
              createAnnotation.analysisListId);
      Language defaultLanguage = entityManager.find(Language.class, 1);
      SvgShapeType polygonShapeType = entityManager.find(SvgShapeType.class, 5);

      Annotation annotation = new Annotation();
      annotation.setMediumAnalysisList(mediumAnalysisList);
      annotation.setStartTime(createAnnotation.getStartTime());
      annotation.setEndTime(createAnnotation.getEndTime());
      annotation.setLayerVisual(createAnnotation.isLayerVisual());
      annotation.setLayerAudio(createAnnotation.isLayerAudio());
      annotation.setCreatedByUserAccount(userAccount);
      annotation.setCreatedAt(Timestamp.from(Instant.now()));
      entityManager.persist(annotation);

      AnnotationTranslation annotationTranslation = new AnnotationTranslation();
      annotationTranslation.setAnnotation(annotation);
      annotationTranslation.setLanguage(defaultLanguage);
      annotationTranslation.setTitle(createAnnotation.getTitle());
      annotationTranslation.setComment(createAnnotation.getComment());
      entityManager.persist(annotationTranslation);

      SelectorSvg selectorSvg = new SelectorSvg();
      selectorSvg.setAnnotation(annotation);
      selectorSvg.setSvgData(createAnnotation.getSelectorSvg().getSvgData());
      selectorSvg.setSvgShapeType(polygonShapeType);
      selectorSvg.setOpacity(createAnnotation.getSelectorSvg().getOpacity());
      selectorSvg.setStrokeWidth(createAnnotation.getSelectorSvg().getStrokeWidth());
      selectorSvg.setColorHex(createAnnotation.getSelectorSvg().getColorHex());
      entityManager.persist(selectorSvg);

      entityManager.flush();
      entityManager.refresh(annotation);

      return annotation;
    });
  }

  public Annotation updateAnnotation(UpdateAnnotation updateAnnotation, UserAccount userAccount) throws DbTransactionExecutionException {
    logger.log(Level.FINE, "Updating annotation with id {0}", new Object[]{updateAnnotation.getId()});
    return this.executeDbTransaction(entityManager -> {
      Annotation annotation = entityManager.find(Annotation.class, updateAnnotation.getId());
      AnnotationTranslation annotationTranslation = annotation.getAnnotationTranslations().get(0);
      SelectorSvg selectorSvg = annotation.getSelectorSvgs().get(0);

      annotationTranslation.setTitle(updateAnnotation.getTitle());
      annotationTranslation.setComment(updateAnnotation.getComment());
      annotation.setStartTime(updateAnnotation.getStartTime());
      annotation.setEndTime(updateAnnotation.getEndTime());
      annotation.setLayerVisual(updateAnnotation.isLayerVisual());
      annotation.setLayerAudio(updateAnnotation.isLayerAudio());

      selectorSvg.setColorHex(updateAnnotation.getSelectorSvg().getColorHex());
      selectorSvg.setOpacity(updateAnnotation.getSelectorSvg().getOpacity());
      selectorSvg.setStrokeWidth(updateAnnotation.getSelectorSvg().getStrokeWidth());
      selectorSvg.setSvgData(updateAnnotation.getSelectorSvg().getSvgData());

      annotation.setLastEditedByUserAccount(userAccount);
      annotation.setLastEditedAt(Timestamp.from(Instant.now()));

      return annotation;
    });
  }

  public void updateThumbnailPositionMs(int annotationId, Integer thumbnailPositionMs, UserAccount userAccount) throws DbTransactionExecutionException {
    executeDbTransaction(entityManager -> {
      Annotation annotation = entityManager.find(Annotation.class, annotationId);

      annotation.setThumbnailPositionMs(thumbnailPositionMs);
      annotation.setLastEditedByUserAccount(userAccount);
      annotation.setLastEditedAt(Timestamp.from(Instant.now()));

      return Void.TYPE;
    });
  }

  public AnnotationHasMusicTranslationArea setTranscriptionAreaToAnnotationHasMusicForLanguage(int annotationId, int musicId, int languageId, IndexBasedRange indexBasedRange) throws DbTransactionExecutionException {
    logger.log(Level.FINE, "Adding transcription area to annotation {0} for music {1} in language {2}",
            new Object[]{annotationId, musicId, languageId});
    return this.executeDbTransaction(entityManager -> {
      AnnotationHasMusicTranslationArea existingAnnotationHasMusicTranslationArea = entityManager.find(
              AnnotationHasMusicTranslationArea.class,
              new AnnotationHasMusicTranslationAreaPK(musicId, annotationId, languageId));

      if (existingAnnotationHasMusicTranslationArea == null) {
        AnnotationHasMusic annotationHasMusic = entityManager.find(AnnotationHasMusic.class,
                new AnnotationHasMusicPK(annotationId, musicId));
        MusicTranslation musicTranslation = entityManager.find(MusicTranslation.class,
                new MusicTranslationPK(musicId, languageId));

        AnnotationHasMusicTranslationArea createdAnnotationHasMusicTranslationArea = new AnnotationHasMusicTranslationArea();
        createdAnnotationHasMusicTranslationArea.setAnnotationHasMusic(annotationHasMusic);
        createdAnnotationHasMusicTranslationArea.setMusicTranslation(musicTranslation);
        createdAnnotationHasMusicTranslationArea.setStartIndex(indexBasedRange.getStartIndex());
        createdAnnotationHasMusicTranslationArea.setEndIndex(indexBasedRange.getEndIndex());

        entityManager.persist(createdAnnotationHasMusicTranslationArea);
        entityManager.flush();
        entityManager.refresh(createdAnnotationHasMusicTranslationArea);

        return createdAnnotationHasMusicTranslationArea;
      }
      else {
        existingAnnotationHasMusicTranslationArea.setStartIndex(indexBasedRange.getStartIndex());
        existingAnnotationHasMusicTranslationArea.setEndIndex(indexBasedRange.getEndIndex());

        return existingAnnotationHasMusicTranslationArea;
      }
    });
  }

  public AnnotationHasMusic addMusicToAnnotation(int annotationId, int musicId) throws DbTransactionExecutionException {
    logger.log(Level.FINE, "Adding music {0} to annotation {1}", new Object[]{musicId, annotationId});
    return this.executeDbTransaction(entityManager -> {
      AnnotationHasMusic existingAnnotationHasMusic = entityManager.find(AnnotationHasMusic.class,
              new AnnotationHasMusicPK(annotationId, musicId));


      if (existingAnnotationHasMusic == null) {
        Music music = entityManager.find(Music.class, musicId);
        Annotation annotation = entityManager.find(Annotation.class, annotationId);
        AnnotationHasMusic annotationHasMusic = new AnnotationHasMusic();
        annotationHasMusic.setAnnotation(annotation);
        annotationHasMusic.setMusic(music);

        entityManager.persist(annotationHasMusic);
        entityManager.flush();
        entityManager.refresh(annotationHasMusic);

        return annotationHasMusic;
      }

      return existingAnnotationHasMusic;
    });
  }

  public boolean removeMusicFromAnnotation(int annotationId, int musicId) throws DbTransactionExecutionException {
    logger.log(Level.FINE, "Remove music {0} from annotation {1}", new Object[]{musicId, annotationId});
    return this.executeDbTransaction(entityManager -> {
      AnnotationHasMusic annotationHasMusic = entityManager.find(AnnotationHasMusic.class,
              new AnnotationHasMusicPK(annotationId, musicId));
      if (annotationHasMusic != null) {
        entityManager.remove(annotationHasMusic);
        return true;
      }

      return false;
    });
  }

  public MediumAnalysisList getMediumAnalysisListOfAnnotation(@PathParam("id") int id) {
    logger.log(Level.FINE, "Loading medium analysis list from annotation {0}", new Object[]{id});
    return this.executeDbTransaction(entityManager -> entityManager.createQuery(
            "select mediumAnalysisList from Annotation annotation join annotation.mediumAnalysisList mediumAnalysisList where annotation.id = :annotationId",
            MediumAnalysisList.class).setParameter("annotationId", id).getSingleResult());
  }

  public Annotation getAnnotationById(int annotationId) throws DbTransactionExecutionException {
    logger.log(Level.FINE, "Getting annotation with id {0} from DB", annotationId);
    return this.executeDbTransaction(entityManager -> entityManager.find(Annotation.class, annotationId));
  }

  @Override
  protected List<Predicate> createPredicates(AnnotationFilterCriteria filter, Root<Annotation> root, CriteriaBuilder criteriaBuilder, CriteriaQuery<?> criteriaQuery, UserAccount userAccount) {
    List<Predicate> predicates = new ArrayList<>();

    if (filter != null) {
      if (filter.getAnnotationNameSearch().isPresent() && !filter.getAnnotationNameSearch().get().isBlank()) {
        String annotationNameSearch = filter.getAnnotationNameSearch().get();
        Join<Annotation, AnnotationTranslation> annotationTranslationJoin = root.join(
                Annotation_.annotationTranslations);

        predicates.add(criteriaBuilder.like(annotationTranslationJoin.get(AnnotationTranslation_.title),
                "%" + annotationNameSearch + "%"));
      }

      if (filter.getMediumAnalysisListIds().isPresent() && !filter.getMediumAnalysisListIds().get().isEmpty()) {
        predicates.add(root.get(Annotation_.mediumAnalysisList).get(MediumAnalysisList_.id)
                           .in(filter.getMediumAnalysisListIds().get()));
      }

      boolean categoryFilterActive = filter.getCategoryIds().isPresent() && !filter.getCategoryIds().get().isEmpty();
      boolean categorySetFilterActive = filter.getCategorySetIds().isPresent() && !filter.getCategorySetIds().get()
                                                                                         .isEmpty();
      boolean hasCategoryFilterActive = filter.hasCategories().isPresent();

      if (categoryFilterActive || hasCategoryFilterActive) {
        Join<Annotation, Category> categoryJoin = root.join(Annotation_.categories, JoinType.LEFT);

        if (hasCategoryFilterActive) {
          if (filter.hasCategories().get()) {
            predicates.add(categoryJoin.get(Category_.id).isNotNull());
          }
          else {
            predicates.add(categoryJoin.get(Category_.id).isNull());
          }
        }

        if (categoryFilterActive) {
          predicates.add(categoryJoin.get(Category_.id).in(filter.getCategoryIds().get()));
        }
      }


      if (categorySetFilterActive) {
        Join<MediumAnalysisList, CategorySet> categorySetJoin = root.join(Annotation_.mediumAnalysisList)
                                                                    .join(MediumAnalysisList_.categorySets);
        predicates.add(categorySetJoin.get(CategorySet_.id).in(filter.getCategorySetIds().get()));
      }

      if (filter.getAnnotationTypes().isPresent() && !filter.getAnnotationTypes().get().isEmpty()) {
        List<Predicate> typePredicates = new ArrayList<>();
        for (AnnotationType annotationType : filter.getAnnotationTypes().get()) {
          if (AnnotationType.VIDEO.equals(annotationType)) {
            typePredicates.add(criteriaBuilder.equal(root.get(Annotation_.layerVisual), true));
          }
          if (AnnotationType.AUDIO.equals(annotationType)) {
            typePredicates.add(criteriaBuilder.equal(root.get(Annotation_.layerAudio), true));
          }
        }

        predicates.add(criteriaBuilder.or(typePredicates.toArray(new Predicate[0])));
      }
    }

    if (userAccount != null) {
      Join<Annotation, MediumAnalysisList> mediumAnalysisListJoin = root.join(Annotation_.mediumAnalysisList);
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

  public static class CreateAnnotation {
    private final String title;
    private final String comment;
    private final int startTime;
    private final int endTime;
    private final boolean layerVisual;
    private final boolean layerAudio;
    private final UpdateSelectorSvg selectorSvg;
    private final int analysisListId;

    public CreateAnnotation(String title, String comment, int startTime, int endTime, boolean layerVisual, boolean layerAudio, UpdateSelectorSvg selectorSvg, int analysisListId) {
      this.title = title;
      this.comment = comment;
      this.startTime = startTime;
      this.endTime = endTime;
      this.layerVisual = layerVisual;
      this.layerAudio = layerAudio;
      this.selectorSvg = selectorSvg;
      this.analysisListId = analysisListId;
    }

    public String getTitle() {
      return title;
    }

    public String getComment() {
      return comment;
    }

    public int getStartTime() {
      return startTime;
    }

    public int getEndTime() {
      return endTime;
    }

    public boolean isLayerVisual() {
      return layerVisual;
    }

    public boolean isLayerAudio() {
      return layerAudio;
    }

    public UpdateSelectorSvg getSelectorSvg() {
      return selectorSvg;
    }

    public int getAnalysisListId() {
      return analysisListId;
    }
  }

  public static final class UpdateAnnotation {
    private final int id;
    private final String title;
    private final String comment;
    private final int startTime;
    private final int endTime;
    private final boolean layerVisual;
    private final boolean layerAudio;
    private final UpdateSelectorSvg selectorSvg;

    public UpdateAnnotation(int id, String title, String comment, int startTime, int endTime, boolean layerVisual, boolean layerAudio, UpdateSelectorSvg selectorSvg) {
      this.id = id;
      this.title = title;
      this.comment = comment;
      this.startTime = startTime;
      this.endTime = endTime;
      this.layerVisual = layerVisual;
      this.layerAudio = layerAudio;
      this.selectorSvg = selectorSvg;
    }

    public int getId() {
      return id;
    }

    public String getTitle() {
      return title;
    }

    public int getStartTime() {
      return startTime;
    }

    public String getComment() {
      return comment;
    }

    public int getEndTime() {
      return endTime;
    }

    public boolean isLayerVisual() {
      return layerVisual;
    }

    public boolean isLayerAudio() {
      return layerAudio;
    }

    public UpdateSelectorSvg getSelectorSvg() {
      return selectorSvg;
    }
  }

  public static final class UpdateSelectorSvg {

    private final String colorHex;
    private final byte opacity;
    private final int strokeWidth;
    private final String svgData;

    public UpdateSelectorSvg(String colorHex, byte opacity, int strokeWidth, String svgData) {
      this.colorHex = colorHex;
      this.opacity = opacity;
      this.strokeWidth = strokeWidth;
      this.svgData = svgData;
    }

    public String getColorHex() {
      return colorHex;
    }

    public byte getOpacity() {
      return opacity;
    }

    public int getStrokeWidth() {
      return strokeWidth;
    }

    public String getSvgData() {
      return svgData;
    }
  }
}
