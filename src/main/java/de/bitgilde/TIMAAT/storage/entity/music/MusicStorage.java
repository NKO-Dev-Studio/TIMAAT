package de.bitgilde.TIMAAT.storage.entity.music;

import de.bitgilde.TIMAAT.db.exception.DbTransactionExecutionException;
import de.bitgilde.TIMAAT.model.FIPOP.Category;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySet;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySet_;
import de.bitgilde.TIMAAT.model.FIPOP.Category_;
import de.bitgilde.TIMAAT.model.FIPOP.DynamicMarking;
import de.bitgilde.TIMAAT.model.FIPOP.Language;
import de.bitgilde.TIMAAT.model.FIPOP.Medium;
import de.bitgilde.TIMAAT.model.FIPOP.MediumHasMusic;
import de.bitgilde.TIMAAT.model.FIPOP.MediumHasMusicDetail;
import de.bitgilde.TIMAAT.model.FIPOP.Music;
import de.bitgilde.TIMAAT.model.FIPOP.MusicTextSettingElementType;
import de.bitgilde.TIMAAT.model.FIPOP.MusicTranslation;
import de.bitgilde.TIMAAT.model.FIPOP.MusicType;
import de.bitgilde.TIMAAT.model.FIPOP.MusicType_;
import de.bitgilde.TIMAAT.model.FIPOP.Music_;
import de.bitgilde.TIMAAT.model.FIPOP.MusicalKey;
import de.bitgilde.TIMAAT.model.FIPOP.Tag;
import de.bitgilde.TIMAAT.model.FIPOP.TempoMarking;
import de.bitgilde.TIMAAT.model.FIPOP.Title;
import de.bitgilde.TIMAAT.model.FIPOP.Title_;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount;
import de.bitgilde.TIMAAT.model.FIPOP.VoiceLeadingPattern;
import de.bitgilde.TIMAAT.model.TimeRange;
import de.bitgilde.TIMAAT.storage.db.DbStorage;
import de.bitgilde.TIMAAT.storage.entity.TagStorage;
import de.bitgilde.TIMAAT.storage.entity.music.api.MusicFilterCriteria;
import de.bitgilde.TIMAAT.storage.entity.music.api.MusicSortingField;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
 * Storage used to receive and update music related information
 *
 * @author Nico Kotlenga
 * @since 27.08.25
 */
public class MusicStorage extends DbStorage<Music, MusicFilterCriteria, MusicSortingField> {

  private static final Logger logger = Logger.getLogger(MusicStorage.class.getName());

  private final TagStorage tagStorage;

  @Inject
  public MusicStorage(EntityManagerFactory emf, TagStorage tagStorage) {
    super(Music.class, MusicSortingField.ID, emf);
    this.tagStorage = tagStorage;
  }

  @Override
  protected List<Predicate> createPredicates(MusicFilterCriteria filter, Root<Music> root, CriteriaBuilder criteriaBuilder, CriteriaQuery<?> criteriaQuery, UserAccount userAccount) {
    List<Predicate> predicates = new ArrayList<>();

    if(filter != null) {
      if (filter.getMusicNameSearch().isPresent()) {
        String musicNameSearchText = filter.getMusicNameSearch().get();
        predicates.add(
                criteriaBuilder.like(root.get(Music_.displayTitle).get(Title_.name), "%" + musicNameSearchText + "%"));
      }

      if (filter.getCategoryIds().isPresent() && !filter.getCategoryIds().get().isEmpty()) {
        Collection<Integer> categoryIds = filter.getCategoryIds().get();
        Join<Music, Category> categoryJoin = root.join(Music_.categories);

        predicates.add(categoryJoin.get(Category_.id).in(categoryIds));
      }

      if (filter.getCategorySetIds().isPresent() && !filter.getCategorySetIds().get().isEmpty()) {
        Collection<Integer> categorySetIds = filter.getCategorySetIds().get();
        Join<Music, CategorySet> categorySetJoin = root.join(Music_.categorySets);

        predicates.add(categorySetJoin.get(CategorySet_.id).in(categorySetIds));
      }

      if (filter.getMusicTypeIds().isPresent() && !filter.getMusicTypeIds().get().isEmpty()) {
        Collection<Integer> musicTypeIds = filter.getMusicTypeIds().get();
        Join<Music, MusicType> musicTypeJoin = root.join(Music_.musicType);

        predicates.add(musicTypeJoin.get(MusicType_.id).in(musicTypeIds));
      }
    }


    return predicates;
  }

  public Music createMusic(CreateMusic createMusic, int executedByUserId) throws DbTransactionExecutionException {
    logger.log(Level.INFO, "Creating new music entry");

    return executeDbTransaction(entityManager -> {
      Title originalTitle = new Title();
      Language originalTitleLanguage = entityManager.getReference(Language.class, createMusic.titleLanguageId);
      originalTitle.setName(createMusic.getTitle());
      originalTitle.setLanguage(originalTitleLanguage);
      entityManager.persist(originalTitle);

      MusicType musicType = entityManager.find(MusicType.class, createMusic.musicTypeId);
      TempoMarking tempoMarking = createMusic.getTempoMarkingId() != null ? entityManager.getReference(
              TempoMarking.class, createMusic.tempoMarkingId) : null;
      MusicalKey musicKey = createMusic.getMusicalKeyId() != null ? entityManager.getReference(MusicalKey.class,
              createMusic.getMusicalKeyId()) : null;
      DynamicMarking dynamicMarking = createMusic.getDynamicMarkingId() != null ? entityManager.getReference(
              DynamicMarking.class, createMusic.getDynamicMarkingId()) : null;
      MusicTextSettingElementType musicTextSettingElementType = createMusic.getMusicTextSettingElementTypeId() != null ? entityManager.getReference(
              MusicTextSettingElementType.class, createMusic.getMusicTextSettingElementTypeId()) : null;
      List<VoiceLeadingPattern> voiceLeadingPatterns = createMusic.getVoiceLeadingPatternIds().stream()
                                                                  .map(currentVoiceLeadingPatternId -> entityManager.getReference(
                                                                          VoiceLeadingPattern.class,
                                                                          currentVoiceLeadingPatternId))
                                                                  .collect(Collectors.toList());
      UserAccount userAccount = entityManager.find(UserAccount.class, executedByUserId);
      Medium medium = createMusic.getMediumId() != null ? entityManager.getReference(Medium.class,
              createMusic.getMediumId()) : null;

      Music music = new Music();
      music.setOriginalTitle(originalTitle);
      music.setDisplayTitle(originalTitle);
      music.setTempo(createMusic.getTempo());
      music.setTempoMarking(tempoMarking);
      music.setBeat(createMusic.getBeat());
      music.setMusicalKey(musicKey);
      music.setDynamicMarking(dynamicMarking);
      music.setMusicTextSettingElementType(musicTextSettingElementType);
      music.setVoiceLeadingPatternList(voiceLeadingPatterns);
      music.setMusicType(musicType);

      Timestamp creationTimeStamp = Timestamp.from(Instant.now());
      music.setCreatedAt(creationTimeStamp);
      music.setLastEditedAt(creationTimeStamp);
      music.setCreatedByUserAccount(userAccount);
      music.setLastEditedByUserAccount(userAccount);

      entityManager.persist(music);
      entityManager.flush();
      entityManager.refresh(music);
      return music;
    });
  }

  public Music updateMusic(UpdateMusic updateMusic, int executedByUserId) throws DbTransactionExecutionException {
    logger.log(Level.FINE, "Updating music having id {0}", updateMusic.getId());

    return executeDbTransaction(entityManager -> {
      UserAccount userAccount = entityManager.find(UserAccount.class, executedByUserId);
      Music music = entityManager.find(Music.class, updateMusic.getId());
      music.getOriginalTitle().setName(updateMusic.getTitle());
      if (music.getOriginalTitle().getLanguage().getId() != updateMusic.getTitleLanguageId()) {
        Language updatedLanguage = entityManager.getReference(Language.class, updateMusic.getTitleLanguageId());
        music.getOriginalTitle().setLanguage(updatedLanguage);
      }

      Integer currentTempoMarkingId = music.getTempoMarking() != null ? music.getTempoMarking().getId() : null;
      if (!Objects.equals(currentTempoMarkingId, updateMusic.getTempoMarkingId())) {
        TempoMarking updatedTempoMarking = updateMusic.getTempoMarkingId() != null ? entityManager.getReference(
                TempoMarking.class, updateMusic.getTempoMarkingId()) : null;
        music.setTempoMarking(updatedTempoMarking);
      }

      Integer currentMusicalKeyId = music.getMusicalKey() != null ? music.getMusicalKey().getId() : null;
      if (!Objects.equals(currentMusicalKeyId, updateMusic.getMusicalKeyId())) {
        MusicalKey updatedMusicalKey = updateMusic.getMusicalKeyId() != null ? entityManager.getReference(
                MusicalKey.class, updateMusic.getMusicalKeyId()) : null;
        music.setMusicalKey(updatedMusicalKey);
      }

      Integer currentDynamicMarkingId = music.getDynamicMarking() != null ? music.getDynamicMarking().getId() : null;
      if (!Objects.equals(currentDynamicMarkingId, updateMusic.getDynamicMarkingId())) {
        DynamicMarking updatedDynamicMarking = updateMusic.getDynamicMarkingId() != null ? entityManager.getReference(
                DynamicMarking.class, updateMusic.getDynamicMarkingId()) : null;
        music.setDynamicMarking(updatedDynamicMarking);
      }

      Integer currentMusicTextSettingElementTypeId = music.getMusicTextSettingElementType() != null ? music.getMusicTextSettingElementType()
                                                                                                           .getId() : null;
      if (!Objects.equals(currentMusicTextSettingElementTypeId, updateMusic.getMusicTextSettingElementTypeId())) {
        MusicTextSettingElementType musicTextSettingElementType = updateMusic.getMusicTextSettingElementTypeId() != null ? entityManager.getReference(
                MusicTextSettingElementType.class, updateMusic.getMusicTextSettingElementTypeId()) : null;
        music.setMusicTextSettingElementType(musicTextSettingElementType);
      }

      List<VoiceLeadingPattern> updatedVoiceLeadingPatterns = updateMusic.getVoiceLeadingPatternIds().stream()
                                                                         .map(currentVoiceLeadingPatternId -> entityManager.getReference(
                                                                                 VoiceLeadingPattern.class,
                                                                                 currentVoiceLeadingPatternId))
                                                                         .collect(Collectors.toList());
      music.setVoiceLeadingPatternList(updatedVoiceLeadingPatterns);

      music.setTempo(updateMusic.getTempo());
      music.setBeat(updateMusic.getBeat());
      music.setInstrumentation(updateMusic.getInstrumentation());
      music.setRemark(updateMusic.getRemark());
      music.setLastEditedAt(Timestamp.from(Instant.now()));
      music.setLastEditedByUserAccount(userAccount);

      entityManager.flush();
      entityManager.refresh(music);
      return music;
    });
  }

  public List<MusicTranslation> updateTranslationListOfMusic(int musicId, Map<Integer, String> translationByMusicId) throws DbTransactionExecutionException {
    logger.log(Level.FINE, "Updating translation list of music with id " + musicId);
    return executeDbTransaction((entityManager) -> {
      Music music = entityManager.getReference(Music.class, musicId);

      Map<Integer, MusicTranslation> existingMusicTranslationsByLanguageId = entityManager.createQuery(
              "select musicTranslation from MusicTranslation musicTranslation where musicTranslation.id.musicId = :musicId",
              MusicTranslation.class).setParameter("musicId", musicId).getResultStream().collect(
              Collectors.toMap(musicTranslation -> musicTranslation.getLanguage().getId(),
                      musicTranslation -> musicTranslation));

      List<MusicTranslation> musicTranslations = new ArrayList<>();
      for (Map.Entry<Integer, String> currentUpdatedTranscriptionByLanguageId : translationByMusicId.entrySet()) {
        if (existingMusicTranslationsByLanguageId.containsKey(currentUpdatedTranscriptionByLanguageId.getKey())) {
          MusicTranslation existingMusicTranslation = existingMusicTranslationsByLanguageId.get(
                  currentUpdatedTranscriptionByLanguageId.getKey());
          existingMusicTranslation.setTranslation(currentUpdatedTranscriptionByLanguageId.getValue());

          musicTranslations.add(existingMusicTranslation);
        }
        else {
          Language language = entityManager.getReference(Language.class,
                  currentUpdatedTranscriptionByLanguageId.getKey());
          MusicTranslation createdMusicTranslation = new MusicTranslation();
          createdMusicTranslation.setMusic(music);
          createdMusicTranslation.setTranslation(currentUpdatedTranscriptionByLanguageId.getValue());
          createdMusicTranslation.setLanguage(language);

          entityManager.persist(createdMusicTranslation);
          musicTranslations.add(createdMusicTranslation);
        }
      }

      Set<Integer> deletableLanguageIds = new HashSet<>(existingMusicTranslationsByLanguageId.keySet());
      deletableLanguageIds.removeAll(translationByMusicId.keySet());

      if (!deletableLanguageIds.isEmpty()) {
        entityManager.createQuery(
                             "delete from MusicTranslation where id.musicId = :musicId and id.languageId in :languageIds")
                     .setParameter("musicId", musicId).setParameter("languageIds", deletableLanguageIds)
                     .executeUpdate();
      }
      return musicTranslations;
    });
  }

  public List<MediumHasMusic> updateMediumHasMusic(int musicId, Map<Integer, Collection<TimeRange>> timeRangesByMediumId) throws DbTransactionExecutionException {
    logger.log(Level.FINE, "Updating medium has music list of music with id " + musicId);
    return executeDbTransaction(entityManager -> {
      Music music = entityManager.find(Music.class, musicId);

      entityManager.createQuery("delete from MediumHasMusic where music.id= :musicId").setParameter("musicId", musicId)
                   .executeUpdate();
      List<MediumHasMusic> updatedMediumHasMusic = new ArrayList<>();
      for (Map.Entry<Integer, Collection<TimeRange>> currentTimeRangeByMediumId : timeRangesByMediumId.entrySet()) {
        MediumHasMusic currentMediumHasMusic = new MediumHasMusic();
        Medium currentMedium = entityManager.find(Medium.class, currentTimeRangeByMediumId.getKey());

        currentMediumHasMusic.setMusic(music);
        currentMediumHasMusic.setMedium(currentMedium);

        entityManager.persist(currentMediumHasMusic);

        for (TimeRange currentTimeRange : currentTimeRangeByMediumId.getValue()) {
          MediumHasMusicDetail currentMediumHasMusicDetail = new MediumHasMusicDetail();

          currentMediumHasMusicDetail.setStartTime(currentTimeRange.getStartTime());
          currentMediumHasMusicDetail.setEndTime(currentTimeRange.getEndTime());
          currentMediumHasMusicDetail.setMediumHasMusic(currentMediumHasMusic);

          entityManager.persist(currentMediumHasMusicDetail);
        }

        updatedMediumHasMusic.add(currentMediumHasMusic);
      }
      entityManager.flush();
      updatedMediumHasMusic.forEach(entityManager::refresh);
      return updatedMediumHasMusic;
    });
  }

  public List<CategorySet> updateCategorySetsOfMusic(int musicId, List<Integer> categorySetIds) throws DbTransactionExecutionException {
    logger.log(Level.FINE, "Updating category sets of music with id " + musicId);
    return executeDbTransaction(entityManager -> {
      Music music = entityManager.find(Music.class, musicId);
      List<CategorySet> updatedCategorySets = categorySetIds.stream().map(currentCategorySetId -> entityManager.find(
              CategorySet.class, currentCategorySetId)).collect(Collectors.toList());
      music.setRestrictedCategorySets(updatedCategorySets);

      return updatedCategorySets;
    });
  }

  public List<Category> updateCategoriesOfMusic(int musicId, List<Integer> categoryIds) throws DbTransactionExecutionException {
    logger.log(Level.FINE, "Updating category sets of music with id " + musicId);
    return executeDbTransaction(entityManager -> {
      Music music = entityManager.find(Music.class, musicId);
      List<Category> updatedCategories = categoryIds.stream()
                                                    .map(currentCategoryId -> entityManager.find(Category.class,
                                                            categoryIds)).collect(Collectors.toList());
      music.setCategories(updatedCategories);

      return updatedCategories;
    });
  }

  public List<Tag> updateTagsOfMusic(int musicId, List<String> tagNames) throws DbTransactionExecutionException {
    logger.log(Level.FINE, "Updating tags of music with id " + musicId);
    return executeDbTransaction(entityManager -> {
      Music music = entityManager.find(Music.class, musicId);
      List<Tag> tags = tagStorage.getOrCreateTagsHavingNames(tagNames);

      music.setTags(tags);
      return tags;
    });
  }

  /**
   * Returns the display title of a {@link Music} entry
   * @param musicId for which the display title will be loadedf
   * @return an {@link Optional} containing the display title when a music entry with the specified id has been founde
   * @throws DbTransactionExecutionException when an error occurred during executing the query
   */
  public Optional<Title> getDisplayTitleOfMusic(int musicId) throws DbTransactionExecutionException {
    logger.log(Level.FINE, "Getting display title for music with id " + musicId);
    return executeDbTransaction(entityManager -> {
      Music music = entityManager.find(Music.class, musicId);
      if (music == null) {
        return Optional.empty();
      }
      else {
        return Optional.of(music.getDisplayTitle());
      }
    });
  }

  public static class CreateMusic {
    private final int titleLanguageId;
    private final String title;
    private final Short tempo;
    private final Integer tempoMarkingId;
    private final String beat;
    private final Integer musicalKeyId;
    private final Integer dynamicMarkingId;
    private final Integer musicTextSettingElementTypeId;
    private final List<Integer> voiceLeadingPatternIds;
    private final String instrumentation;
    private final String remark;
    private final Integer mediumId;
    private final Integer musicTypeId;


    public CreateMusic(int titleLanguageId, String title, Short tempo, Integer tempoMarkingId, String beat, Integer musicalKeyId, Integer dynamicMarkingId, Integer musicTextSettingElementTypeId, List<Integer> voiceLeadingPatternIds, String instrumentation, String remark, Integer mediumId, Integer musicTypeId) {
      this.title = title;
      this.titleLanguageId = titleLanguageId;
      this.tempo = tempo;
      this.tempoMarkingId = tempoMarkingId;
      this.beat = beat;
      this.musicalKeyId = musicalKeyId;
      this.dynamicMarkingId = dynamicMarkingId;
      this.musicTextSettingElementTypeId = musicTextSettingElementTypeId;
      this.voiceLeadingPatternIds = voiceLeadingPatternIds;
      this.instrumentation = instrumentation;
      this.remark = remark;
      this.mediumId = mediumId;
      this.musicTypeId = musicTypeId;
    }

    public String getTitle() {
      return title;
    }

    public Short getTempo() {
      return tempo;
    }

    public Integer getTempoMarkingId() {
      return tempoMarkingId;
    }

    public String getBeat() {
      return beat;
    }

    public Integer getMusicalKeyId() {
      return musicalKeyId;
    }

    public Integer getDynamicMarkingId() {
      return dynamicMarkingId;
    }

    public Integer getMusicTextSettingElementTypeId() {
      return musicTextSettingElementTypeId;
    }

    public List<Integer> getVoiceLeadingPatternIds() {
      return voiceLeadingPatternIds;
    }

    public String getInstrumentation() {
      return instrumentation;
    }

    public String getRemark() {
      return remark;
    }

    public int getTitleLanguageId() {
      return titleLanguageId;
    }

    public Integer getMediumId() {
      return mediumId;
    }

    public Integer getMusicTypeId() {
      return musicTypeId;
    }
  }

  public static final class UpdateMusic extends CreateMusic {

    private final int id;

    public UpdateMusic(int id, int titleLanguageId, String title, Short tempo, Integer tempoMarkingId, String beat, Integer musicalKeyId, Integer dynamicMarkingId, Integer musicTextSettingElementTypeId, List<Integer> voiceLeadingPatternIds, String instrumentation, String remark, Integer mediumId, Integer musicTypeId) {
      super(titleLanguageId, title, tempo, tempoMarkingId, beat, musicalKeyId, dynamicMarkingId,
              musicTextSettingElementTypeId, voiceLeadingPatternIds, instrumentation, remark, mediumId, musicTypeId);
      this.id = id;
    }

    public int getId() {
      return id;
    }
  }
}
