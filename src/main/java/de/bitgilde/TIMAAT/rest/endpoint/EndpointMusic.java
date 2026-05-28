package de.bitgilde.TIMAAT.rest.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bitgilde.TIMAAT.SelectElement;
import de.bitgilde.TIMAAT.SelectElementWithChildren;
import de.bitgilde.TIMAAT.TIMAATApp;
import de.bitgilde.TIMAAT.db.exception.DbTransactionExecutionException;
import de.bitgilde.TIMAAT.model.DataTableInfo;
import de.bitgilde.TIMAAT.model.FIPOP.Actor;
import de.bitgilde.TIMAAT.model.FIPOP.AnnotationHasMusic;
import de.bitgilde.TIMAAT.model.FIPOP.Articulation;
import de.bitgilde.TIMAAT.model.FIPOP.ArticulationTranslation;
import de.bitgilde.TIMAAT.model.FIPOP.Category;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySet;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySetHasCategory;
import de.bitgilde.TIMAAT.model.FIPOP.ChangeInTempo;
import de.bitgilde.TIMAAT.model.FIPOP.ChangeInTempoTranslation;
import de.bitgilde.TIMAAT.model.FIPOP.DynamicMarkingTranslation;
import de.bitgilde.TIMAAT.model.FIPOP.Language;
import de.bitgilde.TIMAAT.model.FIPOP.Medium;
import de.bitgilde.TIMAAT.model.FIPOP.MediumHasMusic;
import de.bitgilde.TIMAAT.model.FIPOP.Music;
import de.bitgilde.TIMAAT.model.FIPOP.MusicArticulationElement;
import de.bitgilde.TIMAAT.model.FIPOP.MusicChangeInTempoElement;
import de.bitgilde.TIMAAT.model.FIPOP.MusicChurchMusic;
import de.bitgilde.TIMAAT.model.FIPOP.MusicDynamicsElement;
import de.bitgilde.TIMAAT.model.FIPOP.MusicDynamicsElementType;
import de.bitgilde.TIMAAT.model.FIPOP.MusicDynamicsElementTypeTranslation;
import de.bitgilde.TIMAAT.model.FIPOP.MusicFormElement;
import de.bitgilde.TIMAAT.model.FIPOP.MusicFormElementType;
import de.bitgilde.TIMAAT.model.FIPOP.MusicFormElementTypeTranslation;
import de.bitgilde.TIMAAT.model.FIPOP.MusicHasActorWithRole;
import de.bitgilde.TIMAAT.model.FIPOP.MusicNashid;
import de.bitgilde.TIMAAT.model.FIPOP.MusicTextSettingElement;
import de.bitgilde.TIMAAT.model.FIPOP.MusicTextSettingElementType;
import de.bitgilde.TIMAAT.model.FIPOP.MusicTextSettingElementTypeTranslation;
import de.bitgilde.TIMAAT.model.FIPOP.MusicTranslation;
import de.bitgilde.TIMAAT.model.FIPOP.MusicalKeyTranslation;
import de.bitgilde.TIMAAT.model.FIPOP.Role;
import de.bitgilde.TIMAAT.model.FIPOP.Tag;
import de.bitgilde.TIMAAT.model.FIPOP.TempoMarkingTranslation;
import de.bitgilde.TIMAAT.model.FIPOP.Title;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount;
import de.bitgilde.TIMAAT.model.FIPOP.VoiceLeadingPatternTranslation;
import de.bitgilde.TIMAAT.model.TimeRange;
import de.bitgilde.TIMAAT.rest.Secured;
import de.bitgilde.TIMAAT.rest.filter.AuthenticationFilter;
import de.bitgilde.TIMAAT.rest.model.music.CreateUpdateMusicPayload;
import de.bitgilde.TIMAAT.rest.model.music.MusicListingQueryParameter;
import de.bitgilde.TIMAAT.rest.model.music.UpdateMediumHasMusicListPayload;
import de.bitgilde.TIMAAT.rest.model.music.UpdateMediumHasMusicListPayload.MediumHasMusicListEntry;
import de.bitgilde.TIMAAT.rest.model.music.UpdateMusicCategoriesPayload;
import de.bitgilde.TIMAAT.rest.model.music.UpdateMusicCategorySetsPayload;
import de.bitgilde.TIMAAT.rest.model.music.UpdateMusicTranslationListPayload;
import de.bitgilde.TIMAAT.rest.model.tags.UpdateAssignedTagsPayload;
import de.bitgilde.TIMAAT.security.UserLogManager;
import de.bitgilde.TIMAAT.storage.api.PagingParameter;
import de.bitgilde.TIMAAT.storage.api.SortingParameter;
import de.bitgilde.TIMAAT.storage.entity.music.MusicStorage;
import de.bitgilde.TIMAAT.storage.entity.music.api.MusicFilterCriteria;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.ServletContext;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
 * @author Mirko Scherf <mscherf@uni-mainz.de>
 */
@Service
@Path("/music")
public class EndpointMusic {
  @Context
  private UriInfo uriInfo;
  @Context
  ContainerRequestContext containerRequestContext;
  @Context
  ServletContext servletContext;
  @Inject
  MusicStorage musicStorage;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}")
  public Response getMusic(@PathParam("id") int id, @Context HttpHeaders httpHeaders) {
    String authToken = httpHeaders.getHeaderString("Authorization").substring(7);
    int userId = 0;
    if (AuthenticationFilter.isTokenValid(authToken)) {
      userId = AuthenticationFilter.getTokenClaimUserId(authToken);
    }
    else {
      return Response.status(Status.UNAUTHORIZED).build();
    }


    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Music music = entityManager.find(Music.class, id);
    if (music == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    filterAnnotationReferencesByPermission(userId, Collections.singleton(music));
    return Response.ok().entity(music).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/displayTitle")
  public Title getDisplayTitle(@PathParam("id") int id) {
    Optional<Title> displayTitleOptional = musicStorage.getDisplayTitleOfMusic(id);
    if (displayTitleOptional.isPresent()) {
      return displayTitleOptional.get();
    }
    else {
      throw new NotFoundException("No music with specified id found");
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("getMediumByMusicId/{id}")
  public Response getMediumByMusicId(@PathParam("id") int id) {
    // System.out.println("EndpointMusic: getMediumByMusicId");

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Music music = entityManager.find(Music.class, id);
    if (music == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    Medium medium = null;
    Query countQuery = entityManager.createQuery("SELECT COUNT(m) FROM Medium m WHERE m.music.id = :musicId")
                                    .setParameter("musicId", id);
    long resultsTotal = (long) countQuery.getSingleResult();
    if (resultsTotal == 0) {
      return Response.ok().entity(0).build();
    }
    try {
      medium = entityManager.createQuery("SELECT m FROM Medium m WHERE m.music.id = :musicId", Medium.class)
                            .setParameter("musicId", id).getSingleResult();
    } catch (NoResultException nre) {
      nre.printStackTrace();
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.ok().entity(medium).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("list")
  public DataTableInfo<Music> getMusicList(@BeanParam MusicListingQueryParameter queryParameter) {
    UserAccount userAccount = (UserAccount) containerRequestContext.getProperty(
            AuthenticationFilter.USER_ACCOUNT_PROPERTY_NAME);
    int draw = queryParameter.getDraw().orElse(0);

    List<Music> matchingMusic = musicStorage.getEntriesAsStream(queryParameter, queryParameter, queryParameter,
            userAccount).collect(Collectors.toList());
    long totalMusicEntries = musicStorage.getNumberOfTotalEntriesRespectingAuthorization(userAccount);
    long filteredMusicEntries = musicStorage.getNumberOfMatchingEntriesRespectingAuthorization(queryParameter, userAccount);

    /**
     * TODO: This one can be removed when directly filter on storage layer
     */
    filterAnnotationReferencesByPermission(userAccount.getId(), matchingMusic);
    return new DataTableInfo<>(draw, totalMusicEntries, filteredMusicEntries, matchingMusic);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("selectList")
  public List<SelectElement<Integer>> getMusicSelectList(@QueryParam("search") String searchText) {
    UserAccount userAccount = (UserAccount) containerRequestContext.getProperty(
            AuthenticationFilter.USER_ACCOUNT_PROPERTY_NAME);
    MusicFilterCriteria filterCriteria = new MusicFilterCriteria.Builder().musicNameSearch(searchText).build();
    return musicStorage.getEntriesAsStream(filterCriteria, PagingParameter.NO_PAGING,
                               SortingParameter.defaultSortOrder(), userAccount)
                       .map(currentMusic -> new SelectElement<>(currentMusic.getId(),
                               currentMusic.getDisplayTitle().getName())).collect(Collectors.toList());
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("formElementType/selectList")
  public Response getMusicFormElementTypeSelectList(@QueryParam("language") String languageCode) {
    // System.out.println("EndpointMusic: getMusicFormElementTypeSelectList");

    if (languageCode == null) {
      languageCode = "default"; // as long as multi-language is not implemented yet, use the 'default' language entry
    }
    Query query;
    query = TIMAATApp.emf.createEntityManager().createQuery(
            "SELECT fett FROM MusicFormElementTypeTranslation fett WHERE fett.language.id = (SELECT l.id FROM Language l WHERE l.code = '" + languageCode + "')");
    List<MusicFormElementTypeTranslation> musicFormElementTypeTranslationList = castList(
            MusicFormElementTypeTranslation.class, query.getResultList());
    List<SelectElement> musicFormElementTypeSelectList = new ArrayList<>();
    for (MusicFormElementTypeTranslation musicFormElementTypeTranslation : musicFormElementTypeTranslationList) {
      musicFormElementTypeSelectList.add(
              new SelectElement<Integer>(musicFormElementTypeTranslation.getMusicFormElementType().getId(),
                      musicFormElementTypeTranslation.getType()));
    }
    return Response.ok().entity(musicFormElementTypeSelectList).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("changeInTempoElement/selectList")
  public Response getMusicChangeInTempoElementSelectList(@QueryParam("language") String languageCode) {
    // System.out.println("EndpointMusic: getMusicChangeInTempoElementSelectList");

    if (languageCode == null) {
      languageCode = "default"; // as long as multi-language is not implemented yet, use the 'default' language entry
    }
    Query query;
    query = TIMAATApp.emf.createEntityManager().createQuery(
            "SELECT citt FROM ChangeInTempoTranslation citt WHERE citt.language.id = (SELECT l.id FROM Language l WHERE l.code = '" + languageCode + "')");
    List<ChangeInTempoTranslation> changeInTempoTranslationList = castList(ChangeInTempoTranslation.class,
            query.getResultList());
    List<SelectElement> selectList = new ArrayList<>();
    for (ChangeInTempoTranslation changeInTempoTranslation : changeInTempoTranslationList) {
      selectList.add(new SelectElement<Integer>(changeInTempoTranslation.getChangeInTempo().getId(),
              changeInTempoTranslation.getType()));
    }
    return Response.ok().entity(selectList).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("articulationElement/selectList")
  public Response getMusicArticulationElementSelectList(@QueryParam("language") String languageCode) {
    // System.out.println("EndpointMusic: getMusicArticulationElementSelectList");

    if (languageCode == null) {
      languageCode = "default"; // as long as multi-language is not implemented yet, use the 'default' language entry
    }
    Query query;
    query = TIMAATApp.emf.createEntityManager().createQuery(
            "SELECT at FROM ArticulationTranslation at WHERE at.language.id = (SELECT l.id FROM Language l WHERE l.code = '" + languageCode + "')");
    List<ArticulationTranslation> articulationTranslationList = castList(ArticulationTranslation.class,
            query.getResultList());
    List<SelectElement> selectList = new ArrayList<>();
    for (ArticulationTranslation articulationTranslation : articulationTranslationList) {
      selectList.add(new SelectElement<Integer>(articulationTranslation.getArticulation().getId(),
              articulationTranslation.getType()));
    }
    return Response.ok().entity(selectList).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("musicTextSettingElementType/selectList")
  public Response getMusicTextSettingElementTypeSelectList(@PathParam("id") Integer id, @QueryParam("start") Integer start, @QueryParam("length") Integer length, @QueryParam("language") String languageCode) {
    // System.out.println("EndpointMusic: getMusicTextSettingElementTypeSelectList - Id: "+ id);

    if (languageCode == null) {
      languageCode = "default"; // as long as multi-language is not implemented yet, use the 'default' language entry
    }

    List<SelectElement> selectElementList = new ArrayList<>();
    List<SelectElementWithChildren> selectElementWithChildrenList = new ArrayList<>();
    Query query;
    query = TIMAATApp.emf.createEntityManager().createQuery(
            "SELECT mtstt FROM MusicTextSettingElementTypeTranslation mtstt WHERE mtstt.language.id = (SELECT l.id FROM Language l WHERE l.code = '" + languageCode + "') ORDER BY mtstt.type ASC");
    List<MusicTextSettingElementTypeTranslation> musicTextSettingElementTypeTranslationList = castList(
            MusicTextSettingElementTypeTranslation.class, query.getResultList());
    List<SelectElement> musicTextSettingElementTypeSelectList = new ArrayList<>();
    for (MusicTextSettingElementTypeTranslation musicTextSettingElementTypeTranslation : musicTextSettingElementTypeTranslationList) {
      musicTextSettingElementTypeSelectList.add(new SelectElement<Integer>(
              musicTextSettingElementTypeTranslation.getMusicTextSettingElementType().getId(),
              musicTextSettingElementTypeTranslation.getType()));
    }
    selectElementList = musicTextSettingElementTypeSelectList;

    if (selectElementList.size() > 0) {
      return Response.ok().entity(selectElementList).build();
    }
    else {
      return Response.ok().entity(selectElementWithChildrenList).build();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("musicDynamicsElementType/selectList")
  public Response getMusicDynamicsElementTypeSelectList(@PathParam("id") Integer id, @QueryParam("start") Integer start, @QueryParam("length") Integer length, @QueryParam("language") String languageCode) {
    // System.out.println("EndpointMusic: getMusicDynamicsElementTypeSelectList - Id: "+ id);

    if (languageCode == null) {
      languageCode = "default"; // as long as multi-language is not implemented yet, use the 'default' language entry
    }

    List<SelectElement> selectElementList = new ArrayList<>();
    List<SelectElementWithChildren> selectElementWithChildrenList = new ArrayList<>();
    Query query;
    query = TIMAATApp.emf.createEntityManager().createQuery(
            "SELECT mdett FROM MusicDynamicsElementTypeTranslation mdett WHERE mdett.language.id = (SELECT l.id FROM Language l WHERE l.code = '" + languageCode + "') ORDER BY mdett.type ASC");
    List<MusicDynamicsElementTypeTranslation> musicDynamicsElementTypeTranslationList = castList(
            MusicDynamicsElementTypeTranslation.class, query.getResultList());
    List<SelectElement> musicDynamicsElementTypeSelectList = new ArrayList<>();
    for (MusicDynamicsElementTypeTranslation musicDynamicsElementTypeTranslation : musicDynamicsElementTypeTranslationList) {
      musicDynamicsElementTypeSelectList.add(
              new SelectElement<Integer>(musicDynamicsElementTypeTranslation.getMusicDynamicsElementType().getId(),
                      musicDynamicsElementTypeTranslation.getType()));
    }
    selectElementList = musicDynamicsElementTypeSelectList;

    if (selectElementList.size() > 0) {
      return Response.ok().entity(selectElementList).build();
    }
    else {
      return Response.ok().entity(selectElementWithChildrenList).build();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("hasActor/{actorId}/withRoles/selectList")
  public Response getRoleSelectList(@PathParam("actorId") int actorId, @QueryParam("search") String search, @QueryParam("page") Integer page, @QueryParam("per_page") Integer per_page, @QueryParam("language") String languageCode) {
    // returns list of id and name combinations of all roles of this actor
    // System.out.println("EndpointMusic: getRoleSelectList for actor id: "+ actorId);
    // System.out.println("EndpointMusic: getRoleSelectList - search string: "+ search);

    if (languageCode == null) {
      languageCode = "default"; // as long as multi-language is not implemented yet, use the 'default' language entry
    }

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Actor actor = entityManager.find(Actor.class, actorId);
    if (actor == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    List<Role> roleList = actor.getRoles();
    List<SelectElement> roleSelectList = new ArrayList<>();
    for (Role role : roleList) {
      if (search != null && search.length() > 0) {
        if (role.getRoleTranslations().get(0).getName().toLowerCase().contains(search.toLowerCase())) {
          roleSelectList.add(new SelectElement<Integer>(role.getId(), role.getRoleTranslations().get(0).getName()));
        }
      }
      else {
        roleSelectList.add(new SelectElement<Integer>(role.getId(), role.getRoleTranslations().get(0).getName()));
      }
    }
    return Response.ok().entity(roleSelectList).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{musicId}/hasActorList")
  public Response getActorList(@PathParam("musicId") Integer musicId) {
    // System.out.println("EndpointMusic: getActorList");
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Music music = entityManager.find(Music.class, musicId);
    List<MusicHasActorWithRole> musicHasActorWithRoleList = music.getMusicHasActorWithRoles();
    List<Actor> actorList = new ArrayList<>();
    for (MusicHasActorWithRole musicHasActorWithRole : musicHasActorWithRoleList) {
      if (!actorList.contains(musicHasActorWithRole.getActor())) {
        actorList.add(musicHasActorWithRole.getActor());
      }
    }
    return Response.ok().entity(actorList).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{musicId}/hasActor/{actorId}/withRoleList")
  public Response getActorHasRoleList(@PathParam("musicId") Integer musicId, @PathParam("actorId") Integer actorId) {
    // System.out.println("EndpointMusic: getActorHasRoleList");
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();

    Music music = entityManager.find(Music.class, musicId);
    List<MusicHasActorWithRole> musicHasActorWithRoleList = music.getMusicHasActorWithRoles();
    List<Role> roleList = new ArrayList<>();
    for (MusicHasActorWithRole musicHasActorWithRole : musicHasActorWithRoleList) {
      if (musicHasActorWithRole.getActor().getId() == actorId) {
        roleList.add(entityManager.find(Role.class, musicHasActorWithRole.getRole().getId()));
      }
    }

    return Response.ok().entity(roleList).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{musicId}/mediumHasMusicList")
  public Response getMediumHasMusicList(@PathParam("musicId") Integer musicId) {
    // System.out.println("EndpointMusic: getMediumHasMusicList");
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();

    Music music = entityManager.find(Music.class, musicId);
    List<MediumHasMusic> mediumHasMusicList = music.getMediumHasMusicList();

    return Response.ok().entity(mediumHasMusicList).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/categorySet/list")
  public Response getCategorySetList(@PathParam("id") Integer id) {
    // System.out.println("EndpointMusic: getCategorySetList - ID: "+ id);
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Music music = entityManager.find(Music.class, id);
    List<CategorySet> categorySetList = music.getCategorySets();

    return Response.ok().entity(categorySetList).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/category/list")
  public Response getSelectedCategories(@PathParam("id") Integer id) {
    // System.out.println("EndpointMusic: getSelectedCategories - Id: "+ id);
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Music music = entityManager.find(Music.class, id);
    List<Category> categoryList = music.getCategories();
    return Response.ok().entity(categoryList).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/category/selectList")
  public Response getCategorySelectList(@PathParam("id") Integer id, @QueryParam("start") Integer start, @QueryParam("length") Integer length, @QueryParam("orderby") String orderby, @QueryParam("dir") String direction, @QueryParam("search") String search) {
    // System.out.println("EndpointMusic: getCategorySelectList - Id: "+ id);

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Music music = entityManager.find(Music.class, id);
    List<CategorySet> categorySetList = music.getCategorySets();
    List<Category> categoryList = new ArrayList<>();
    List<SelectElement> categorySelectList = new ArrayList<>();

    for (CategorySet categorySet : categorySetList) {
      Set<CategorySetHasCategory> cshc = categorySet.getCategorySetHasCategories();
      Iterator<CategorySetHasCategory> itr = cshc.iterator();
      while (itr.hasNext()) {
        categoryList.add(itr.next().getCategory());
      }
    }

    // search
    Query query;
    String sql;
    if (search != null && search.length() > 0) {
      // find all matching names
      sql = "SELECT c FROM Category c WHERE lower(c.name) LIKE lower(concat('%', :name,'%')) ORDER BY c.name ASC";
      query = entityManager.createQuery(sql).setParameter("name", search);
      // find all categories belonging to those names
      if (start != null && start > 0) {
        query.setFirstResult(start);
      }
      if (length != null && length > 0) {
        query.setMaxResults(length);
      }
      List<Category> searchCategoryList = castList(Category.class, query.getResultList());
      for (Category category : searchCategoryList) {
        if (categoryList.contains(category)) {
          categorySelectList.add(new SelectElement<Integer>(category.getId(), category.getName()));
        }
      }
    }
    else {
      for (Category category : categoryList) {
        categorySelectList.add(new SelectElement<Integer>(category.getId(), category.getName()));
      }
    }

    return Response.ok().entity(categorySelectList).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{musicId}/hasTagList")
  public Response getTagList(@PathParam("musicId") Integer musicId) {
    // System.out.println("EndpointMusic: getTagList");
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Music music = entityManager.find(Music.class, musicId);
    if (music == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    entityManager.refresh(music);
    return Response.ok().entity(music.getTags()).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{musicId}/hasVoiceLeadingPatternList")
  public Response getVoiceLeadingPatternList(@PathParam("musicId") Integer musicId) {
    // System.out.println("EndpointMusic: getTagList");
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Music music = entityManager.find(Music.class, musicId);
    if (music == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    entityManager.refresh(music);
    return Response.ok().entity(music.getVoiceLeadingPatternList()).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("voiceLeadingPattern/selectList")
  public Response getVoiceLeadingPatternSelectList(@QueryParam("start") Integer start, @QueryParam("length") Integer length, @QueryParam("orderby") String orderby, @QueryParam("dir") String direction, @QueryParam("search") String search, @QueryParam("language") String languageCode) {
    System.out.println("EndpointMusic: getVoiceLeadingPatternSelectList");

    if (languageCode == null) {
      languageCode = "default"; // as long as multi-language is not implemented yet, use the 'default' language entry
    }

    List<SelectElement> selectElementList = new ArrayList<>();
    List<SelectElementWithChildren> selectElementWithChildrenList = new ArrayList<>();
    Query countQuery;
    countQuery = TIMAATApp.emf.createEntityManager().createQuery(
            "SELECT COUNT (vlpt.id) FROM VoiceLeadingPatternTranslation vlpt WHERE vlpt.language.id = (SELECT l.id FROM Language l WHERE l.code = '" + languageCode + "')"); // TODO Fallback?
    long count = (long) countQuery.getSingleResult();
    if (count == 0) {
      return Response.ok().entity(selectElementList).build();
    }
    Query query;
    query = TIMAATApp.emf.createEntityManager().createQuery(
            "SELECT vlpt FROM VoiceLeadingPatternTranslation vlpt WHERE vlpt.language.id = (SELECT l.id FROM Language l WHERE l.code = '" + languageCode + "') ORDER BY vlpt.type ASC");
    List<VoiceLeadingPatternTranslation> voiceLeadingPatternTranslationList = castList(
            VoiceLeadingPatternTranslation.class, query.getResultList());
    List<SelectElement> voiceLeadingPatternSelectList = new ArrayList<>();
    for (VoiceLeadingPatternTranslation voiceLeadingPatternTranslation : voiceLeadingPatternTranslationList) {
      voiceLeadingPatternSelectList.add(
              new SelectElement<Integer>(voiceLeadingPatternTranslation.getVoiceLeadingPattern().getId(),
                      voiceLeadingPatternTranslation.getType()));
    }
    selectElementList = voiceLeadingPatternSelectList;

    if (selectElementList.size() > 0) {
      return Response.ok().entity(selectElementList).build();
    }
    else {
      return Response.ok().entity(selectElementWithChildrenList).build();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/titles/list")
  public Response getTitlesList(@PathParam("id") int id) {
    // // System.out.println("EndpointMusic: getTitlesList");
    // Music music = TIMAATApp.emf.createEntityManager().find(Music.class, id);
    // 	if ( music == null ) return Response.status(Status.NOT_FOUND).build();
    // // List<Title> titleList = TIMAATApp.emf.createEntityManager().createNamedQuery("titleList.findAll").getResultList();
    // // return Response.ok().entity(titleList).build();

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();

    // find music
    Music music = entityManager.find(Music.class, id);
    if (music == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    entityManager.refresh(music);

    return Response.ok().entity(music.getTitles()).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("/medium/selectList")
  public Response getMediumSelectList(@QueryParam("start") Integer start, @QueryParam("length") Integer length, @QueryParam("search") String search, @QueryParam("language") String languageCode) {
    // System.out.println("EndpointMusic: getMediumSelectList - Id: "+ id);

    if (languageCode == null) {
      languageCode = "default"; // as long as multi-language is not implemented yet, use the 'default' language entry
    }

    List<SelectElement> selectElementList = new ArrayList<>();
    List<SelectElementWithChildren> selectElementWithChildrenList = new ArrayList<>();
    Query query;
    if (search != null && search.length() > 0) {
      query = TIMAATApp.emf.createEntityManager().createQuery(
              "SELECT m FROM Medium m WHERE m.music IS NULL AND lower (m.displayTitle.name) LIKE lower(concat('%', :name, '%')) ORDER BY m.displayTitle.name ASC");
      query.setParameter("name", search);
    }
    else {
      query = TIMAATApp.emf.createEntityManager().createQuery(
              "SELECT m FROM Medium m WHERE m.music IS NULL ORDER BY m.displayTitle.name ASC");
    }
    List<Medium> MediumList = castList(Medium.class, query.getResultList());
    List<SelectElement> mediumSelectList = new ArrayList<>();
    for (Medium medium : MediumList) {
      mediumSelectList.add(new SelectElement<Integer>(medium.getId(), medium.getDisplayTitle().getName()));
    }
    selectElementList = mediumSelectList;

    if (selectElementList.size() > 0) {
      return Response.ok().entity(selectElementList).build();
    }
    else {
      return Response.ok().entity(selectElementWithChildrenList).build();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("/articulation/selectList")
  public Response getArticulationSelectList(@PathParam("id") Integer id, @QueryParam("start") Integer start, @QueryParam("length") Integer length, @QueryParam("language") String languageCode) {
    // System.out.println("EndpointMusic: getArticulationSelectList - Id: "+ id);

    if (languageCode == null) {
      languageCode = "default"; // as long as multi-language is not implemented yet, use the 'default' language entry
    }

    List<SelectElement> selectElementList = new ArrayList<>();
    List<SelectElementWithChildren> selectElementWithChildrenList = new ArrayList<>();
    Query query;
    query = TIMAATApp.emf.createEntityManager().createQuery(
            "SELECT at FROM ArticulationTranslation at WHERE at.language.id = (SELECT l.id FROM Language l WHERE l.code = '" + languageCode + "') ORDER BY at.type ASC");
    List<ArticulationTranslation> articulationTranslationList = castList(ArticulationTranslation.class,
            query.getResultList());
    List<SelectElement> articulationSelectList = new ArrayList<>();
    for (ArticulationTranslation articulationTranslation : articulationTranslationList) {
      articulationSelectList.add(new SelectElement<Integer>(articulationTranslation.getArticulation().getId(),
              articulationTranslation.getType()));
    }
    selectElementList = articulationSelectList;

    if (selectElementList.size() > 0) {
      return Response.ok().entity(selectElementList).build();
    }
    else {
      return Response.ok().entity(selectElementWithChildrenList).build();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("/dynamicMarking/selectList")
  public Response getDynamicMarkingSelectList(@PathParam("id") Integer id, @QueryParam("start") Integer start, @QueryParam("length") Integer length, @QueryParam("language") String languageCode) {
    // System.out.println("EndpointMusic: getDynamicMarkingSelectList - Id: "+ id);

    if (languageCode == null) {
      languageCode = "default"; // as long as multi-language is not implemented yet, use the 'default' language entry
    }

    List<SelectElement> selectElementList = new ArrayList<>();
    List<SelectElementWithChildren> selectElementWithChildrenList = new ArrayList<>();
    Query query;
    query = TIMAATApp.emf.createEntityManager().createQuery(
            "SELECT dmt FROM DynamicMarkingTranslation dmt WHERE dmt.language.id = (SELECT l.id FROM Language l WHERE l.code = '" + languageCode + "') ORDER BY dmt.type ASC");
    List<DynamicMarkingTranslation> dynamicMarkingTranslationList = castList(DynamicMarkingTranslation.class,
            query.getResultList());
    List<SelectElement> dynamicMarkingSelectList = new ArrayList<>();
    for (DynamicMarkingTranslation dynamicMarkingTranslation : dynamicMarkingTranslationList) {
      dynamicMarkingSelectList.add(new SelectElement<Integer>(dynamicMarkingTranslation.getDynamicMarking().getId(),
              dynamicMarkingTranslation.getType()));
    }
    selectElementList = dynamicMarkingSelectList;

    if (selectElementList.size() > 0) {
      return Response.ok().entity(selectElementList).build();
    }
    else {
      return Response.ok().entity(selectElementWithChildrenList).build();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("/musicalKey/selectList")
  public Response getMusicalKeySelectList(@PathParam("id") Integer id, @QueryParam("start") Integer start, @QueryParam("length") Integer length, @QueryParam("language") String languageCode) {
    // System.out.println("EndpointMusic: getMusicalKeySelectList - Id: "+ id);

    if (languageCode == null) {
      languageCode = "default"; // as long as multi-language is not implemented yet, use the 'default' language entry
    }

    List<SelectElement> selectElementList = new ArrayList<>();
    List<SelectElementWithChildren> selectElementWithChildrenList = new ArrayList<>();
    Query query;
    query = TIMAATApp.emf.createEntityManager().createQuery(
            "SELECT mkt FROM MusicalKeyTranslation mkt WHERE mkt.language.id = (SELECT l.id FROM Language l WHERE l.code = '" + languageCode + "') ORDER BY mkt.type ASC");
    List<MusicalKeyTranslation> musicalKeyTranslationList = castList(MusicalKeyTranslation.class,
            query.getResultList());
    List<SelectElement> musicalKeySelectList = new ArrayList<>();
    for (MusicalKeyTranslation musicalKeyTranslation : musicalKeyTranslationList) {
      musicalKeySelectList.add(new SelectElement<Integer>(musicalKeyTranslation.getMusicalKey().getId(),
              musicalKeyTranslation.getType()));
    }
    selectElementList = musicalKeySelectList;

    if (selectElementList.size() > 0) {
      return Response.ok().entity(selectElementList).build();
    }
    else {
      return Response.ok().entity(selectElementWithChildrenList).build();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("/tempoMarking/selectList")
  public Response getTempoMarkingSelectList(@PathParam("id") Integer id, @QueryParam("start") Integer start, @QueryParam("length") Integer length, @QueryParam("language") String languageCode) {
    // System.out.println("EndpointMusic: getTempoMarkingSelectList - Id: "+ id);

    if (languageCode == null) {
      languageCode = "default"; // as long as multi-language is not implemented yet, use the 'default' language entry
    }

    List<SelectElement> selectElementList = new ArrayList<>();
    List<SelectElementWithChildren> selectElementWithChildrenList = new ArrayList<>();
    Query query;
    query = TIMAATApp.emf.createEntityManager().createQuery(
            "SELECT tmt FROM TempoMarkingTranslation tmt WHERE tmt.language.id = (SELECT l.id FROM Language l WHERE l.code = '" + languageCode + "') ORDER BY tmt.type ASC");
    List<TempoMarkingTranslation> tempoMarkingTranslationList = castList(TempoMarkingTranslation.class,
            query.getResultList());
    List<SelectElement> tempoMarkingSelectList = new ArrayList<>();
    for (TempoMarkingTranslation tempoMarkingTranslation : tempoMarkingTranslationList) {
      tempoMarkingSelectList.add(new SelectElement<Integer>(tempoMarkingTranslation.getTempoMarking().getId(),
              tempoMarkingTranslation.getType()));
    }
    selectElementList = tempoMarkingSelectList;

    if (selectElementList.size() > 0) {
      return Response.ok().entity(selectElementList).build();
    }
    else {
      return Response.ok().entity(selectElementWithChildrenList).build();
    }
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Secured
  public Music createMusic(@Valid CreateUpdateMusicPayload createUpdateMusicPayload) throws DbTransactionExecutionException {
    int userId = (int) containerRequestContext.getProperty("TIMAAT.userID");
    MusicStorage.CreateMusic createMusic = new MusicStorage.CreateMusic(
            createUpdateMusicPayload.getTitle().getLanguageId(), createUpdateMusicPayload.getTitle().getName(),
            createUpdateMusicPayload.getTempo(), createUpdateMusicPayload.getTempoMarkingId(),
            createUpdateMusicPayload.getBeat(), createUpdateMusicPayload.getMusicalKeyId(),
            createUpdateMusicPayload.getDynamicMarkingId(), createUpdateMusicPayload.getMusicTextSettingElementTypeId(),
            createUpdateMusicPayload.getVoiceLeadingPatternIds(), createUpdateMusicPayload.getInstrumentation(),
            createUpdateMusicPayload.getRemark(), createUpdateMusicPayload.getMediumId(),
            createUpdateMusicPayload.getMusicTypeId());
    Music craetedMusic = musicStorage.createMusic(createMusic, userId);

    // add log entry
    UserLogManager.getLogger().addLogEntry(userId, UserLogManager.LogEvents.MUSICCREATED);

    return craetedMusic;
  }

  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("{id}")
  @Secured
  public Music updateMusic(@PathParam("id") int id, @Valid CreateUpdateMusicPayload createUpdateMusicPayload) throws DbTransactionExecutionException {
    int userId = (int) containerRequestContext.getProperty("TIMAAT.userID");
    MusicStorage.UpdateMusic updateMusic = new MusicStorage.UpdateMusic(id,
            createUpdateMusicPayload.getTitle().getLanguageId(), createUpdateMusicPayload.getTitle().getName(),
            createUpdateMusicPayload.getTempo(), createUpdateMusicPayload.getTempoMarkingId(),
            createUpdateMusicPayload.getBeat(), createUpdateMusicPayload.getMusicalKeyId(),
            createUpdateMusicPayload.getDynamicMarkingId(), createUpdateMusicPayload.getMusicTextSettingElementTypeId(),
            createUpdateMusicPayload.getVoiceLeadingPatternIds(), createUpdateMusicPayload.getInstrumentation(),
            createUpdateMusicPayload.getRemark(), createUpdateMusicPayload.getMediumId(),
            createUpdateMusicPayload.getMusicTypeId());
    Music updatedMusicEntry = musicStorage.updateMusic(updateMusic, userId);

    // add log entry
    UserLogManager.getLogger().addLogEntry(userId, UserLogManager.LogEvents.MUSICEDITED);
    return updatedMusicEntry;
  }

  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("{musicId}/categorySets")
  @Secured
  public List<CategorySet> updateMusicCategorySets(@PathParam("musicId") int musicId, UpdateMusicCategorySetsPayload updateMusicCategorySetsPayload) throws DbTransactionExecutionException {
    return musicStorage.updateCategorySetsOfMusic(musicId, updateMusicCategorySetsPayload.getCategorySetIds());
  }

  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("{musicId}/categories")
  @Secured
  public List<Category> updateMusicCategories(@PathParam("musicId") int musicId, UpdateMusicCategoriesPayload updateMusicCategoriesPayload) throws DbTransactionExecutionException {
    return musicStorage.updateCategoriesOfMusic(musicId, updateMusicCategoriesPayload.getCategoryIds());
  }

  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("{musicId}/tags")
  @Secured
  public List<Tag> updateTags(@PathParam("musicId") int musicId, UpdateAssignedTagsPayload updateAssignedTagsPayload) throws DbTransactionExecutionException {
    return musicStorage.updateTagsOfMusic(musicId, updateAssignedTagsPayload.getTagNames());
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}")
  @Secured
  public Response deleteMusic(@PathParam("id") int id) {
    // System.out.println("EndpointMusic: deleteMusic");
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Music music = entityManager.find(Music.class, id);
    if (music == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(music.getDisplayTitle());
    entityManager.remove(music);
    entityTransaction.commit();

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.MUSICDELETED);
    return Response.ok().build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("churchMusic/{id}")
  @Secured
  public Response createChurchMusic(@PathParam("id") int id, String jsonData) {

    // System.out.println("EndpointMusic: createChurchMusic jsonData: "+jsonData);
    ObjectMapper mapper = new ObjectMapper();
    MusicChurchMusic newChurchMusic = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();

    // parse JSON data
    try {
      newChurchMusic = mapper.readValue(jsonData, MusicChurchMusic.class);
    } catch (IOException e) {
      System.out.println("EndpointMusic: createChurchMusic: IOException e !");
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (newChurchMusic == null) {
      System.out.println("EndpointMusic: createChurchMusic: newChurchMusic == null !");
      return Response.status(Status.BAD_REQUEST).build();
    }
    // sanitize object data

    // update log metadata
    // Not necessary, a churchMusic will always be created in conjunction with a music

    // persist churchMusic
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(newChurchMusic);
    entityManager.flush();
    entityTransaction.commit();
    entityManager.refresh(newChurchMusic);
    entityManager.refresh(newChurchMusic.getMusic());

    // add log entry
    // UserLogManager.getLogger().addLogEntry(newChurchMusic.getMusic().getCreatedByUserAccount().getId(), UserLogManager.LogEvents.CHURCHMUSICCREATED);
    return Response.ok().entity(newChurchMusic).build();
  }

  @PATCH
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("churchMusic/{id}")
  @Secured
  public Response updateChurchMusic(@PathParam("id") int id, String jsonData) {

    // System.out.println("EndpointMusic: update churchMusic - jsonData: " + jsonData);
    ObjectMapper mapper = new ObjectMapper();
    MusicChurchMusic updatedChurchMusic = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MusicChurchMusic churchMusic = entityManager.find(MusicChurchMusic.class, id);

    if (churchMusic == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      updatedChurchMusic = mapper.readValue(jsonData, MusicChurchMusic.class);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedChurchMusic == null) {
      return Response.notModified().build();
    }

    // update churchMusic
    churchMusic.setChurchMusicalKey(updatedChurchMusic.getChurchMusicalKey());

    // update log metadata
    churchMusic.getMusic().setLastEditedAt(new Timestamp(System.currentTimeMillis()));
    if (containerRequestContext.getProperty("TIMAAT.userID") != null) {
      churchMusic.getMusic().setLastEditedByUserAccount(
              (entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID"))));
    }
    else {
      // DEBUG do nothing - production system should abort with internal server error
    }
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(churchMusic);
    entityManager.persist(churchMusic);
    entityTransaction.commit();
    entityManager.refresh(churchMusic);

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
    // 																				UserLogManager.LogEvents.NASHIDEDITED);
    return Response.ok().entity(churchMusic).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("churchMusic/{id}")
  @Secured
  public Response deleteChurchMusic(@PathParam("id") int id) {
    // System.out.println("EndpointMusic: deleteChurchMusic with id: "+ id);
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Music music = entityManager.find(Music.class, id);
    if (music == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    MusicChurchMusic churchMusic = entityManager.find(MusicChurchMusic.class, id);
    if (churchMusic == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(churchMusic);
    entityManager.remove(churchMusic.getMusic().getDisplayTitle());
    entityManager.remove(churchMusic.getMusic()); // remove churchMusic, then corresponding music
    entityTransaction.commit();
    // add log entry
    // UserLogManager.getLogger()
    // 							.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
    // 														UserLogManager.LogEvents.NASHIDDELETED);
    return Response.ok().build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("nashid/{id}")
  @Secured
  public Response createNashid(@PathParam("id") int id, String jsonData) {

    // System.out.println("EndpointMusic: createNashid jsonData: "+jsonData);
    ObjectMapper mapper = new ObjectMapper();
    MusicNashid newNashid = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();

    // parse JSON data
    try {
      newNashid = mapper.readValue(jsonData, MusicNashid.class);
    } catch (IOException e) {
      System.out.println("EndpointMusic: createNashid: IOException e !");
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (newNashid == null) {
      System.out.println("EndpointMusic: createNashid: newNashid == null !");
      return Response.status(Status.BAD_REQUEST).build();
    }
    // sanitize object data

    // update log metadata
    // Not necessary, a nashid will always be created in conjunction with a music

    // persist nashid
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(newNashid);
    entityManager.flush();
    entityTransaction.commit();
    entityManager.refresh(newNashid);
    entityManager.refresh(newNashid.getMusic());

    // add log entry
    // UserLogManager.getLogger().addLogEntry(newNashid.getMusic().getCreatedByUserAccount().getId(), UserLogManager.LogEvents.NASHIDCREATED);
    return Response.ok().entity(newNashid).build();
  }

  @PATCH
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("nashid/{id}")
  @Secured
  public Response updateNashid(@PathParam("id") int id, String jsonData) {

    // System.out.println("EndpointMusic: update nashid - jsonData: " + jsonData);
    ObjectMapper mapper = new ObjectMapper();
    MusicNashid updatedNashid = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MusicNashid nashid = entityManager.find(MusicNashid.class, id);

    if (nashid == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      updatedNashid = mapper.readValue(jsonData, MusicNashid.class);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedNashid == null) {
      return Response.notModified().build();
    }

    // update nashid
    nashid.setJins(updatedNashid.getJins());
    nashid.setMaqam(updatedNashid.getMaqam());

    // update log metadata
    nashid.getMusic().setLastEditedAt(new Timestamp(System.currentTimeMillis()));
    if (containerRequestContext.getProperty("TIMAAT.userID") != null) {
      nashid.getMusic().setLastEditedByUserAccount(
              (entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID"))));
    }
    else {
      // DEBUG do nothing - production system should abort with internal server error
    }
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(nashid);
    entityManager.persist(nashid);
    entityTransaction.commit();
    entityManager.refresh(nashid);

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
    // 																				UserLogManager.LogEvents.NASHIDEDITED);
    return Response.ok().entity(nashid).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("nashid/{id}")
  @Secured
  public Response deleteNashid(@PathParam("id") int id) {
    // System.out.println("EndpointMusic: deleteNashid with id: "+ id);
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Music music = entityManager.find(Music.class, id);
    if (music == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    MusicNashid nashid = entityManager.find(MusicNashid.class, id);
    if (nashid == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(nashid);
    entityManager.remove(nashid.getMusic().getDisplayTitle());
    entityManager.remove(nashid.getMusic()); // remove nashid, then corresponding music
    entityTransaction.commit();
    // add log entry
    // UserLogManager.getLogger()
    // 							.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
    // 														UserLogManager.LogEvents.NASHIDDELETED);
    return Response.ok().build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("title/{id}")
  @Secured
  public Response createTitle(@PathParam("id") int id, String jsonData) {

    // System.out.println("EndpointMusic: createTitle: jsonData: "+jsonData);
    ObjectMapper mapper = new ObjectMapper();
    Title newTitle = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();

    // parse JSON data
    try {
      newTitle = mapper.readValue(jsonData, Title.class);
    } catch (IOException e) {
      System.out.println("EndpointMusic: createTitle: IOException e !");
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (newTitle == null) {
      System.out.println("EndpointMusic: createTitle: newTitle == null !");
      return Response.status(Status.BAD_REQUEST).build();
    }
    // sanitize object data
    newTitle.setId(0);
    Language language = entityManager.find(Language.class, newTitle.getLanguage().getId());
    newTitle.setLanguage(language);

    // update log metadata
    // Not necessary, a title will always be created in conjunction with a music

    // persist title
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(language);
    entityManager.persist(newTitle);
    entityManager.flush();
    newTitle.setLanguage(language);
    entityTransaction.commit();
    entityManager.refresh(newTitle);
    entityManager.refresh(language);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.TITLECREATED);

    return Response.ok().entity(newTitle).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("{musicId}/title/{id}")
  @Secured
  public Response addTitle(@PathParam("musicId") int musicId, @PathParam("id") int id, String jsonData) {

    // System.out.println("EndpointMusic: addTitle: jsonData: "+jsonData);
    ObjectMapper mapper = new ObjectMapper();
    Title newTitle = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();

    // parse JSON data
    try {
      newTitle = mapper.readValue(jsonData, Title.class);
    } catch (IOException e) {
      System.out.println("EndpointMusic: addTitle: IOException e !");
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (newTitle == null) {
      System.out.println("EndpointMusic: addTitle: newTitle == null !");
      return Response.status(Status.BAD_REQUEST).build();
    }
    // sanitize object data
    newTitle.setId(0);
    Language language = entityManager.find(Language.class, newTitle.getLanguage().getId());
    newTitle.setLanguage(language);
    Music music = entityManager.find(Music.class, musicId);

    // update log metadata
    // Not necessary, a title will always be created in conjunction with a music

    // persist title
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(language);
    entityManager.persist(newTitle);
    entityManager.flush();
    newTitle.setLanguage(language);
    entityTransaction.commit();
    entityManager.refresh(newTitle);
    entityManager.refresh(language);

    // create music_has_title-table entries
    entityTransaction.begin();
    music.getTitles().add(newTitle);
    newTitle.getMusicList().add(music);
    entityManager.merge(newTitle);
    entityManager.merge(music);
    entityManager.persist(newTitle);
    entityManager.persist(music);
    entityTransaction.commit();
    entityManager.refresh(music);
    entityManager.refresh(newTitle);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.TITLECREATED);

    return Response.ok().entity(newTitle).build();
  }

  @PATCH
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("title/{id}")
  @Secured
  public Response updateTitle(@PathParam("id") int id, String jsonData) {
    // System.out.println("EndpointMusic: update title - jsonData: " + jsonData);
    ObjectMapper mapper = new ObjectMapper();
    Title updatedTitle = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Title title = entityManager.find(Title.class, id);
    if (title == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      updatedTitle = mapper.readValue(jsonData, Title.class);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedTitle == null) {
      return Response.notModified().build();
    }
    // update title
    if (updatedTitle.getName() != null) {
      title.setName(updatedTitle.getName());
    }
    if (updatedTitle.getLanguage() != null) {
      title.setLanguage(updatedTitle.getLanguage());
    }

    // update log metadata
    // log metadata will be updated with the corresponding music
    // title.getMusic().setLastEditedAt(new Timestamp(System.currentTimeMillis()));
    // if ( containerRequestContext.getProperty("TIMAAT.userID") != null ) {
    // 	title.getMusic().setLastEditedByUserAccount((entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID"))));
    // } else {
    // 	// DEBUG do nothing - production system should abort with internal server error
    // }
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(title);
    entityManager.persist(title);
    entityTransaction.commit();
    entityManager.refresh(title);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.TITLEEDITED);
    return Response.ok().entity(title).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("title/{id}")
  @Secured
  public Response deleteTitle(@PathParam("id") int id) {
    // System.out.println("EndpointMusic: deleteTitle");
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Title title = entityManager.find(Title.class, id);
    if (title == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(title);
    entityTransaction.commit();
    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.TITLEDELETED);
    return Response.ok().build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{musicId}/hasActor/{actorId}/withRole/{roleId}")
  @Secured
  public Response addMusicHasActorWithRoles(@PathParam("musicId") int musicId, @PathParam("actorId") int actorId, @PathParam("roleId") int roleId) {
    // System.out.println("EndpointMusic: addMusicHasActorWithRoles");

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Music music = entityManager.find(Music.class, musicId);
    if (music == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    Actor actor = entityManager.find(Actor.class, actorId);
    if (actor == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    Role role = entityManager.find(Role.class, roleId);
    if (role == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    MusicHasActorWithRole mhawr = null;
    try {
      Query countQuery = entityManager.createQuery(
                                              "SELECT COUNT(mhawr) FROM MusicHasActorWithRole mhawr WHERE mhawr.music=:music AND mhawr.actor=:actor AND mhawr.role=:role")
                                      .setParameter("music", music).setParameter("actor", actor)
                                      .setParameter("role", role);
      long recordsTotal = (long) countQuery.getSingleResult();
      if (recordsTotal == 1) {
        mhawr = (MusicHasActorWithRole) entityManager.createQuery(
                                                             "SELECT mhawr FROM MusicHasActorWithRole mhawr WHERE mhawr.music=:music AND mhawr.actor=:actor AND mhawr.role=:role")
                                                     .setParameter("music", music).setParameter("actor", actor)
                                                     .setParameter("role", role).getSingleResult();
      }
    } catch (Exception e) {
      e.printStackTrace();
      // doesn't matter
    }
    if (mhawr == null) {
      mhawr = new MusicHasActorWithRole();
      mhawr.setMusic(music);
      mhawr.setActor(actor);
      mhawr.setRole(role);
      try {
        EntityTransaction entityTransaction = entityManager.getTransaction();
        entityTransaction.begin();
        entityManager.persist(mhawr);
        entityTransaction.commit();
        entityManager.refresh(music);
        entityManager.refresh(actor);
        entityManager.refresh(role);
        entityManager.refresh(mhawr);
      } catch (Exception e) {
        e.printStackTrace();
        return Response.notModified().build();
      }
    }

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"), UserLogManager.LogEvents.MUSICEDITED);

    return Response.ok().entity(mhawr).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{musicId}/hasActor/{actorId}")
  @Secured
  public Response deleteActorFromMusicHasActorWithRoles(@PathParam("musicId") int musicId, @PathParam("actorId") int actorId) {
    // System.out.println("EndpointMusic: deleteMusicHasActorWithRolesItem");
    // deletes all entries of music-actor matches

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Music music = entityManager.find(Music.class, musicId);
    if (music == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    Actor actor = entityManager.find(Actor.class, actorId);
    if (actor == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    try {
      EntityTransaction entityTransaction = entityManager.getTransaction();
      entityTransaction.begin();
      entityManager.createQuery(
                           "DELETE FROM MusicHasActorWithRole mhawr WHERE mhawr.music=:music AND mhawr.actor=:actor")
                   .setParameter("music", music).setParameter("actor", actor).executeUpdate();
      entityTransaction.commit();
      entityManager.refresh(music);
      // entityManager.refresh(actor);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.notModified().build();
    }

    return Response.ok().build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{musicId}/hasActor/{actorId}/withRole/{roleId}")
  @Secured
  public Response deleteRoleFromMusicHasActorWithRoles(@PathParam("musicId") int musicId, @PathParam("actorId") int actorId, @PathParam("roleId") int roleId) {
    // System.out.println("EndpointMusic: deleteMusicHasActorWithRolesItem");
    // deletes all entries of music-actor matches

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Music music = entityManager.find(Music.class, musicId);
    if (music == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    Actor actor = entityManager.find(Actor.class, actorId);
    if (actor == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    Role role = entityManager.find(Role.class, roleId);
    if (role == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    try {
      EntityTransaction entityTransaction = entityManager.getTransaction();
      entityTransaction.begin();
      entityManager.createQuery(
                           "DELETE FROM MusicHasActorWithRole mhawr WHERE mhawr.music=:music AND mhawr.actor=:actor AND mhawr.role=:role")
                   .setParameter("music", music).setParameter("actor", actor).setParameter("role", role)
                   .executeUpdate();
      entityTransaction.commit();
      entityManager.refresh(music);
      // entityManager.refresh(actor);
      // entityManager.refresh(role);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.notModified().build();
    }

    return Response.ok().build();
  }

  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("{musicId}/mediumHasMusicList")
  @Secured
  public List<MediumHasMusic> updateMediumHasMusicList(@PathParam("musicId") int musicId, UpdateMediumHasMusicListPayload updateMediumHasMusicListPayload) throws DbTransactionExecutionException {
    Map<Integer, Collection<TimeRange>> timeRangesByMediumId = updateMediumHasMusicListPayload.getMediumHasMusicListEntries()
                                                                                              .stream().collect(
                    Collectors.toMap(MediumHasMusicListEntry::getMediumId, MediumHasMusicListEntry::getTimeRanges));
    return musicStorage.updateMediumHasMusic(musicId, timeRangesByMediumId);
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{musicId}/tag/{tagId}")
  @Secured
  public Response addExistingTag(@PathParam("musicId") int musicId, @PathParam("tagId") int tagId) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Music music = entityManager.find(Music.class, musicId);
    if (music == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    Tag tag = entityManager.find(Tag.class, tagId);
    if (tag == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    // attach tag to annotation and vice versa
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    music.getTags().add(tag);
    tag.getMusicList().add(music);
    entityManager.merge(tag);
    entityManager.merge(music);
    entityManager.persist(music);
    entityManager.persist(tag);
    entityTransaction.commit();
    entityManager.refresh(music);

    return Response.ok().entity(tag).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{musicId}/tag/{tagId}")
  @Secured
  public Response removeTag(@PathParam("musicId") int musicId, @PathParam("tagId") int tagId) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Music music = entityManager.find(Music.class, musicId);
    if (music == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    Tag tag = entityManager.find(Tag.class, tagId);
    if (tag == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    // attach tag to annotation and vice versa
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    music.getTags().remove(tag);
    tag.getMusicList().remove(music);
    entityManager.merge(tag);
    entityManager.merge(music);
    entityManager.persist(music);
    entityManager.persist(tag);
    entityTransaction.commit();
    entityManager.refresh(music);

    return Response.ok().build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{musicId}/categorySet/{categorySetId}")
  @Secured
  public Response addExistingCategorySet(@PathParam("musicId") int musicId, @PathParam("categorySetId") int categorySetId) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Music music = entityManager.find(Music.class, musicId);
    if (music == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    CategorySet categorySet = entityManager.find(CategorySet.class, categorySetId);
    if (categorySet == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    // attach categorySet to annotation and vice versa
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    music.getCategorySets().add(categorySet);
    categorySet.getMusicList().add(music);
    entityManager.merge(categorySet);
    entityManager.merge(music);
    entityManager.persist(music);
    entityManager.persist(categorySet);
    entityTransaction.commit();
    entityManager.refresh(music);

    return Response.ok().entity(categorySet).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{musicId}/categorySet/{categorySetId}")
  @Secured
  public Response removeCategorySet(@PathParam("musicId") int musicId, @PathParam("categorySetId") int categorySetId) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Music music = entityManager.find(Music.class, musicId);
    if (music == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    CategorySet categorySet = entityManager.find(CategorySet.class, categorySetId);
    if (categorySet == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    // TODO delete categories from music of matching categorySets
    List<Category> categoryList = new ArrayList<>();
    Set<CategorySetHasCategory> cshc = categorySet.getCategorySetHasCategories();
    Iterator<CategorySetHasCategory> itr = cshc.iterator();
    EntityTransaction entityTransaction = entityManager.getTransaction();

    while (itr.hasNext()) {
      categoryList.add(itr.next().getCategory());
    }
    // remove all categories from removed category set from the music
    List<Category> musicCategoryList = music.getCategories();
    List<Category> categoriesToRemove = categoryList.stream().distinct().filter(musicCategoryList::contains)
                                                    .collect(Collectors.toList());
    entityTransaction.begin();
    for (Category category : categoriesToRemove) {
      music.getCategories().remove(category);
    }
    entityManager.merge(music);
    entityManager.persist(music);
    entityTransaction.commit();
    entityManager.refresh(music);

    // attach categorySet to music and vice versa
    entityTransaction.begin();
    music.getCategorySets().remove(categorySet);
    categorySet.getMusicList().remove(music);
    entityManager.merge(categorySet);
    entityManager.merge(music);
    entityManager.persist(music);
    entityManager.persist(categorySet);
    entityTransaction.commit();
    entityManager.refresh(music);

    return Response.ok().build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{musicId}/category/{categoryId}")
  @Secured
  public Response addExistingCategory(@PathParam("musicId") int musicId, @PathParam("categoryId") int categoryId) {
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Music music = entityManager.find(Music.class, musicId);
    if (music == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    Category category = entityManager.find(Category.class, categoryId);
    if (category == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    // attach category to annotation and vice versa
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    music.getCategories().add(category);
    category.getMusicList().add(music);
    entityManager.merge(category);
    entityManager.merge(music);
    entityManager.persist(music);
    entityManager.persist(category);
    entityTransaction.commit();
    entityManager.refresh(music);

    return Response.ok().entity(category).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{musicId}/category/{categoryId}")
  @Secured
  public Response removeCategory(@PathParam("musicId") int musicId, @PathParam("categoryId") int categoryId) {
    // System.out.println("TCL: EndpointMusic - removeCategory");
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Music music = entityManager.find(Music.class, musicId);
    if (music == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    Category category = entityManager.find(Category.class, categoryId);
    if (category == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    // attach category to music and vice versa
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    music.getCategories().remove(category);
    category.getMusicList().remove(music);
    entityManager.merge(category);
    entityManager.merge(music);
    entityManager.persist(music);
    entityManager.persist(category);
    entityTransaction.commit();
    entityManager.refresh(music);
    return Response.ok().build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{musicId}/musicFormElement")
  public Response createMusicFormElement(@PathParam("musicId") int musicId, String jsonData) {
    // System.out.println("createMusicFormElement - jsonData: "+ jsonData);

    ObjectMapper mapper = new ObjectMapper();
    MusicFormElement musicFormElement = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Music music = entityManager.find(Music.class, musicId);
    if (music == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      musicFormElement = mapper.readValue(jsonData, MusicFormElement.class);
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (musicFormElement == null) {
      return Response.status(Status.BAD_REQUEST).build();
    }

    // sanitize object data
    musicFormElement.setId(0);
    music.addMusicFormElement(musicFormElement);
    musicFormElement.getMusicFormElementTranslations().get(0).setId(0);
    musicFormElement.getMusicFormElementTranslations().get(0).setMusicFormElement(musicFormElement);
    musicFormElement.getMusicFormElementTranslations().get(0).setLanguage(entityManager.find(Language.class, 1));

    // persist musicFormElement and list
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(musicFormElement.getMusicFormElementTranslations().get(0));
    entityManager.persist(musicFormElement);
    entityManager.flush();
    musicFormElement.setMusic(music);
    entityManager.persist(music);
    entityTransaction.commit();
    entityManager.refresh(musicFormElement);
    entityManager.refresh(music);

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
    // 																			 UserLogManager.LogEvents.MUSICFORMELEMENTCREATED);

    return Response.ok().entity(musicFormElement).build();
  }

  @PATCH
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("musicFormElement/{id}")
  @Secured
  public Response updateMusicFormElement(@PathParam("id") int musicFormElementId, String jsonData) {
    // System.out.println("EndpointAnalysisList: updateMusicFormElement "+ jsonData);
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MusicFormElement musicFormElement = entityManager.find(MusicFormElement.class, musicFormElementId);
    if (musicFormElement == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    ObjectMapper mapper = new ObjectMapper();
    MusicFormElement updatedMusicFormElement = null;

    // parse JSON data
    try {
      updatedMusicFormElement = mapper.readValue(jsonData, MusicFormElement.class);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedMusicFormElement == null) {
      return Response.notModified().build();
    }

    // update segment
    if (updatedMusicFormElement.getMusicFormElementTranslations().get(0).getText() != null) {
      musicFormElement.getMusicFormElementTranslations().get(0)
                      .setText(updatedMusicFormElement.getMusicFormElementTranslations().get(0).getText());
    }
    musicFormElement.setMusicFormElementType(updatedMusicFormElement.getMusicFormElementType());
    musicFormElement.setRepeatLastRow(updatedMusicFormElement.getRepeatLastRow());
    musicFormElement.setStartTime(updatedMusicFormElement.getStartTime());
    musicFormElement.setEndTime(updatedMusicFormElement.getEndTime());

    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(musicFormElement);
    entityManager.persist(musicFormElement);
    entityTransaction.commit();
    entityManager.refresh(musicFormElement);

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
    // 																			 UserLogManager.LogEvents.MUSICFORMELEMENTEDITED);

    return Response.ok().entity(musicFormElement).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("musicFormElement/{id}")
  @Secured
  public Response deleteMusicFormElement(@PathParam("id") int musicFormElementId) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MusicFormElement musicFormElement = entityManager.find(MusicFormElement.class, musicFormElementId);
    if (musicFormElement == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    Music music = musicFormElement.getMusic();
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(musicFormElement);
    entityTransaction.commit();
    entityManager.refresh(music);

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
    // 																			 UserLogManager.LogEvents.MUSICFORMELEMENTDELETED);

    return Response.ok().build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("musicFormElementType/{id}")
  public Response getMusicFormElementType(@PathParam("id") int id) {
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MusicFormElementType musicFormElementType = entityManager.find(MusicFormElementType.class, id);
    if (musicFormElementType == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.ok().entity(musicFormElementType).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{musicId}/musicChangeInTempoElement/{changeInTempoId}") // TODO change so that ids are part of jsonData?
  public Response createMusicChangeInTempoElement(@PathParam("musicId") int musicId, @PathParam("changeInTempoId") int changeInTempoId, String jsonData) {
    // System.out.println("createMusicChangeInTempoElement - jsonData: "+ jsonData);

    ObjectMapper mapper = new ObjectMapper();
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();

    Music music = entityManager.find(Music.class, musicId);
    ChangeInTempo changeInTempo = entityManager.find(ChangeInTempo.class, changeInTempoId);
    if (music == null || changeInTempo == null) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    MusicChangeInTempoElement musicChangeInTempoElement = null;
    try {
      musicChangeInTempoElement = mapper.readValue(jsonData, MusicChangeInTempoElement.class);
    } catch (IOException e) {
      System.out.println("createMusicChangeInTempoElement: IOException e !");
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (musicChangeInTempoElement == null) {
      System.out.println("createMusicChangeInTempoElement: musicChangeInTempoElement == null !");
      return Response.status(Status.BAD_REQUEST).build();
    }

    // sanitize object data
    musicChangeInTempoElement.setId(0);
    musicChangeInTempoElement.setMusic(music);
    musicChangeInTempoElement.setChangeInTempo(changeInTempo);
    // changeInTempo.addMusicChangeInTempoElement(musicChangeInTempoElement);

    // persist musicChangeInTempoElement and list
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(musicChangeInTempoElement);
    entityManager.flush();
    music.addMusicChangeInTempoElement(musicChangeInTempoElement);
    entityManager.persist(music);
    entityTransaction.commit();
    entityManager.refresh(musicChangeInTempoElement);
    entityManager.refresh(music);

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
    // 																			 UserLogManager.LogEvents.MUSICFORMELEMENTCREATED);
    return Response.ok().entity(musicChangeInTempoElement).build();
  }

  @PATCH
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("musicChangeInTempoElement/{id}")
  @Secured
  public Response updateMusicChangeInTempoElement(@PathParam("id") int musicChangeInTempoElementId, String jsonData) {
    // System.out.println("EndpointAnalysisList: updateMusicChangeInTempoElement "+ jsonData);
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MusicChangeInTempoElement musicChangeInTempoElement = entityManager.find(MusicChangeInTempoElement.class,
            musicChangeInTempoElementId);
    if (musicChangeInTempoElement == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    ObjectMapper mapper = new ObjectMapper();
    MusicChangeInTempoElement updatedMusicChangeInTempoElement = null;
    // parse JSON data
    try {
      updatedMusicChangeInTempoElement = mapper.readValue(jsonData, MusicChangeInTempoElement.class);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedMusicChangeInTempoElement == null) {
      return Response.notModified().build();
    }

    // update musicChangeInTempoElement
    musicChangeInTempoElement.setStartTime(updatedMusicChangeInTempoElement.getStartTime());
    musicChangeInTempoElement.setEndTime(updatedMusicChangeInTempoElement.getEndTime());
    if (updatedMusicChangeInTempoElement.getChangeInTempo() != null) {
      musicChangeInTempoElement.setChangeInTempo(updatedMusicChangeInTempoElement.getChangeInTempo());
    }
    //* music relation won't be changed

    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(musicChangeInTempoElement);
    entityManager.persist(musicChangeInTempoElement);
    entityTransaction.commit();
    entityManager.refresh(musicChangeInTempoElement);
    entityManager.refresh(musicChangeInTempoElement.getMusic());

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
    // 																			 UserLogManager.LogEvents.MUSICFORMELEMENTEDITED);

    return Response.ok().entity(musicChangeInTempoElement).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("musicChangeInTempoElement/{id}")
  @Secured
  public Response deleteMusicChangeInTempoElement(@PathParam("id") int musicChangeInTempoElementId) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MusicChangeInTempoElement musicChangeInTempoElement = entityManager.find(MusicChangeInTempoElement.class,
            musicChangeInTempoElementId);
    if (musicChangeInTempoElement == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    Music music = musicChangeInTempoElement.getMusic();
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(musicChangeInTempoElement);
    entityTransaction.commit();
    entityManager.refresh(music);

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
    // 																			 UserLogManager.LogEvents.MUSICFORMELEMENTDELETED);

    return Response.ok().build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("changeInTempo/{id}")
  public Response getChangeInTempo(@PathParam("id") int id) {
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    ChangeInTempo changeInTempo = entityManager.find(ChangeInTempo.class, id);
    if (changeInTempo == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.ok().entity(changeInTempo).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{musicId}/musicArticulationElement/{articulationId}") // TODO change so that ids are part of jsonData?
  public Response createMusicArticulationElement(@PathParam("musicId") int musicId, @PathParam("articulationId") int articulationId, String jsonData) {
    // System.out.println("createMusicArticulationElement - jsonData: "+ jsonData);

    ObjectMapper mapper = new ObjectMapper();
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();

    Music music = entityManager.find(Music.class, musicId);
    Articulation articulation = entityManager.find(Articulation.class, articulationId);
    if (music == null || articulation == null) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    MusicArticulationElement musicArticulationElement = null;
    try {
      musicArticulationElement = mapper.readValue(jsonData, MusicArticulationElement.class);
    } catch (IOException e) {
      System.out.println("createMusicArticulationElement: IOException e !");
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (musicArticulationElement == null) {
      System.out.println("createMusicArticulationElement: musicArticulationElement == null !");
      return Response.status(Status.BAD_REQUEST).build();
    }

    // sanitize object data
    musicArticulationElement.setId(0);
    musicArticulationElement.setMusic(music);
    musicArticulationElement.setArticulation(articulation);
    // articulation.addMusicArticulationElement(musicArticulationElement);

    // persist musicArticulationElement and list
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(musicArticulationElement);
    entityManager.flush();
    music.addMusicArticulationElement(musicArticulationElement);
    entityManager.persist(music);
    entityTransaction.commit();
    entityManager.refresh(musicArticulationElement);
    entityManager.refresh(music);

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
    // 																			 UserLogManager.LogEvents.MUSICFORMELEMENTCREATED);
    return Response.ok().entity(musicArticulationElement).build();
  }

  @PATCH
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("musicArticulationElement/{id}")
  @Secured
  public Response updateMusicArticulationElement(@PathParam("id") int musicArticulationElementId, String jsonData) {
    // System.out.println("EndpointAnalysisList: updateMusicArticulationElement "+ jsonData);
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MusicArticulationElement musicArticulationElement = entityManager.find(MusicArticulationElement.class,
            musicArticulationElementId);
    if (musicArticulationElement == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    ObjectMapper mapper = new ObjectMapper();
    MusicArticulationElement updatedMusicArticulationElement = null;
    // parse JSON data
    try {
      updatedMusicArticulationElement = mapper.readValue(jsonData, MusicArticulationElement.class);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedMusicArticulationElement == null) {
      return Response.notModified().build();
    }

    // update musicArticulationElement
    musicArticulationElement.setStartTime(updatedMusicArticulationElement.getStartTime());
    musicArticulationElement.setEndTime(updatedMusicArticulationElement.getEndTime());
    if (updatedMusicArticulationElement.getArticulation() != null) {
      musicArticulationElement.setArticulation(updatedMusicArticulationElement.getArticulation());
    }
    //* music relation won't be changed

    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(musicArticulationElement);
    entityManager.persist(musicArticulationElement);
    entityTransaction.commit();
    entityManager.refresh(musicArticulationElement);
    entityManager.refresh(musicArticulationElement.getMusic());

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
    // 																			 UserLogManager.LogEvents.MUSICFORMELEMENTEDITED);

    return Response.ok().entity(musicArticulationElement).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("musicArticulationElement/{id}")
  @Secured
  public Response deleteMusicArticulationElement(@PathParam("id") int musicArticulationElementId) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MusicArticulationElement musicArticulationElement = entityManager.find(MusicArticulationElement.class,
            musicArticulationElementId);
    if (musicArticulationElement == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    Music music = musicArticulationElement.getMusic();
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(musicArticulationElement);
    entityTransaction.commit();
    entityManager.refresh(music);

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
    // 																			 UserLogManager.LogEvents.MUSICFORMELEMENTDELETED);

    return Response.ok().build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("articulation/{id}")
  public Response getArticulation(@PathParam("id") int id) {
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Articulation articulation = entityManager.find(Articulation.class, id);
    if (articulation == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.ok().entity(articulation).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{musicId}/musicTextSettingElement")
  public Response createMusicTextSettingElement(@PathParam("musicId") int musicId, String jsonData) {
    // System.out.println("createMusicTextSettingElement - jsonData: "+ jsonData);

    ObjectMapper mapper = new ObjectMapper();
    MusicTextSettingElement musicTextSettingElement = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Music music = entityManager.find(Music.class, musicId);
    if (music == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      musicTextSettingElement = mapper.readValue(jsonData, MusicTextSettingElement.class);
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (musicTextSettingElement == null) {
      return Response.status(Status.BAD_REQUEST).build();
    }

    // sanitize object data
    musicTextSettingElement.setId(0);
    music.addMusicTextSettingElement(musicTextSettingElement);
    // musicTextSettingElement.getMusicTextSettingElementType().getMusicTextSettingElementTypeTranslations().get(0).setId(0);
    // musicTextSettingElement.getMusicTextSettingElementType().getMusicTextSettingElementTypeTranslations().get(0).setMusicTextSettingElement(musicTextSettingElement);
    // musicTextSettingElement.getMusicTextSettingElementType().getMusicTextSettingElementTypeTranslations().get(0).setLanguage(entityManager.find(Language.class, 1));

    // persist musicTextSettingElement and list
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    // entityManager.persist(musicTextSettingElement.getMusicTextSettingElementType().getMusicTextSettingElementTranslations().get(0));
    entityManager.persist(musicTextSettingElement);
    entityManager.flush();
    musicTextSettingElement.setMusic(music);
    entityManager.persist(music);
    entityTransaction.commit();
    entityManager.refresh(musicTextSettingElement);
    entityManager.refresh(music);

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
    // 																			 UserLogManager.LogEvents.MUSICFORMELEMENTCREATED);

    return Response.ok().entity(musicTextSettingElement).build();
  }

  @PATCH
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("musicTextSettingElement/{id}")
  @Secured
  public Response updateMusicTextSettingElement(@PathParam("id") int musicTextSettingElementId, String jsonData) {
    // System.out.println("EndpointAnalysisList: updateMusicTextSettingElement "+ jsonData);
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MusicTextSettingElement musicTextSettingElement = entityManager.find(MusicTextSettingElement.class,
            musicTextSettingElementId);
    if (musicTextSettingElement == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    ObjectMapper mapper = new ObjectMapper();
    MusicTextSettingElement updatedMusicTextSettingElement = null;

    // parse JSON data
    try {
      updatedMusicTextSettingElement = mapper.readValue(jsonData, MusicTextSettingElement.class);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedMusicTextSettingElement == null) {
      return Response.notModified().build();
    }

    // update text setting element
    // if ( updatedMusicTextSettingElement.getMusicTextSettingElementTranslations().get(0).getText() != null ) musicTextSettingElement.getMusicTextSettingElementTranslations().get(0).setText(updatedMusicTextSettingElement.getMusicTextSettingElementTranslations().get(0).getText());
    musicTextSettingElement.setMusicTextSettingElementType(
            updatedMusicTextSettingElement.getMusicTextSettingElementType());
    musicTextSettingElement.setStartTime(updatedMusicTextSettingElement.getStartTime());
    musicTextSettingElement.setEndTime(updatedMusicTextSettingElement.getEndTime());

    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(musicTextSettingElement);
    entityManager.persist(musicTextSettingElement);
    entityTransaction.commit();
    entityManager.refresh(musicTextSettingElement);

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
    // 																			 UserLogManager.LogEvents.MUSICFORMELEMENTEDITED);

    return Response.ok().entity(musicTextSettingElement).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("musicTextSettingElement/{id}")
  @Secured
  public Response deleteMusicTextSettingElement(@PathParam("id") int musicTextSettingElementId) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MusicTextSettingElement musicTextSettingElement = entityManager.find(MusicTextSettingElement.class,
            musicTextSettingElementId);
    if (musicTextSettingElement == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    Music music = musicTextSettingElement.getMusic();
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(musicTextSettingElement);
    entityTransaction.commit();
    entityManager.refresh(music);

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
    // 																			 UserLogManager.LogEvents.MUSICFORMELEMENTDELETED);

    return Response.ok().build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("musicTextSettingElementType/{id}")
  public Response getMusicTextSettingElementType(@PathParam("id") int id) {
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MusicTextSettingElementType musicTextSettingElementType = entityManager.find(MusicTextSettingElementType.class, id);
    if (musicTextSettingElementType == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.ok().entity(musicTextSettingElementType).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{musicId}/musicDynamicsElement")
  public Response createMusicDynamicsElement(@PathParam("musicId") int musicId, String jsonData) {
    // System.out.println("createMusicDynamicsElement - jsonData: "+ jsonData);

    ObjectMapper mapper = new ObjectMapper();
    MusicDynamicsElement musicDynamicsElement = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Music music = entityManager.find(Music.class, musicId);
    if (music == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      musicDynamicsElement = mapper.readValue(jsonData, MusicDynamicsElement.class);
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (musicDynamicsElement == null) {
      return Response.status(Status.BAD_REQUEST).build();
    }

    // sanitize object data
    musicDynamicsElement.setId(0);
    music.addMusicDynamicsElement(musicDynamicsElement);
    // musicDynamicsElement.getMusicDynamicsElementType().getMusicDynamicsElementTypeTranslations().get(0).setId(0);
    // musicDynamicsElement.getMusicDynamicsElementType().getMusicDynamicsElementTypeTranslations().get(0).setMusicDynamicsElement(musicDynamicsElement);
    // musicDynamicsElement.getMusicDynamicsElementType().getMusicDynamicsElementTypeTranslations().get(0).setLanguage(entityManager.find(Language.class, 1));

    // persist musicDynamicsElement and list
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    // entityManager.persist(musicDynamicsElement.getMusicDynamicsElementType().getMusicDynamicsElementTranslations().get(0));
    entityManager.persist(musicDynamicsElement);
    entityManager.flush();
    musicDynamicsElement.setMusic(music);
    entityManager.persist(music);
    entityTransaction.commit();
    entityManager.refresh(musicDynamicsElement);
    entityManager.refresh(music);

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
    // 																			 UserLogManager.LogEvents.MUSICFORMELEMENTCREATED);

    return Response.ok().entity(musicDynamicsElement).build();
  }

  @PATCH
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("musicDynamicsElement/{id}")
  @Secured
  public Response updateMusicDynamicsElement(@PathParam("id") int musicDynamicsElementId, String jsonData) {
    // System.out.println("EndpointAnalysisList: updateMusicDynamicsElement "+ jsonData);
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MusicDynamicsElement musicDynamicsElement = entityManager.find(MusicDynamicsElement.class, musicDynamicsElementId);
    if (musicDynamicsElement == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    ObjectMapper mapper = new ObjectMapper();
    MusicDynamicsElement updatedMusicDynamicsElement = null;

    // parse JSON data
    try {
      updatedMusicDynamicsElement = mapper.readValue(jsonData, MusicDynamicsElement.class);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedMusicDynamicsElement == null) {
      return Response.notModified().build();
    }

    // update text setting element
    // if ( updatedMusicDynamicsElement.getMusicDynamicsElementTranslations().get(0).getText() != null ) musicDynamicsElement.getMusicDynamicsElementTranslations().get(0).setText(updatedMusicDynamicsElement.getMusicDynamicsElementTranslations().get(0).getText());
    musicDynamicsElement.setMusicDynamicsElementType(updatedMusicDynamicsElement.getMusicDynamicsElementType());
    musicDynamicsElement.setStartTime(updatedMusicDynamicsElement.getStartTime());
    musicDynamicsElement.setEndTime(updatedMusicDynamicsElement.getEndTime());

    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(musicDynamicsElement);
    entityManager.persist(musicDynamicsElement);
    entityTransaction.commit();
    entityManager.refresh(musicDynamicsElement);

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
    // 																			 UserLogManager.LogEvents.MUSICFORMELEMENTEDITED);

    return Response.ok().entity(musicDynamicsElement).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("musicDynamicsElement/{id}")
  @Secured
  public Response deleteMusicDynamicsElement(@PathParam("id") int musicDynamicsElementId) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MusicDynamicsElement musicDynamicsElement = entityManager.find(MusicDynamicsElement.class, musicDynamicsElementId);
    if (musicDynamicsElement == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    Music music = musicDynamicsElement.getMusic();
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(musicDynamicsElement);
    entityTransaction.commit();
    entityManager.refresh(music);

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
    // 																			 UserLogManager.LogEvents.MUSICFORMELEMENTDELETED);

    return Response.ok().build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("musicDynamicsElementType/{id}")
  public Response getMusicDynamicsElementType(@PathParam("id") int id) {
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MusicDynamicsElementType musicDynamicsElementType = entityManager.find(MusicDynamicsElementType.class, id);
    if (musicDynamicsElementType == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.ok().entity(musicDynamicsElementType).build();
  }

  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{musicId}/musicTranslationList")
  public List<MusicTranslation> updateMusicTranslationList(@PathParam("musicId") int musicId, @Valid UpdateMusicTranslationListPayload updateMusicTranslationListPayload) throws DbTransactionExecutionException {
    return musicStorage.updateTranslationListOfMusic(musicId,
            updateMusicTranslationListPayload.getTranslationsByLanguageId());
  }

  public static <T> List<T> castList(Class<? extends T> clazz, Collection<?> c) {
    List<T> r = new ArrayList<T>(c.size());
    for (Object o : c) {
      r.add(clazz.cast(o));
    }
    return r;
  }

  private static void filterAnnotationReferencesByPermission(int userId, Collection<Music> music) {
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Set<Integer> allowedMediumAnalysisList = entityManager.createQuery(
            "select us.mediumAnalysisList.id from UserAccountHasMediumAnalysisList us where us.userAccount.id = :userId and us.permissionType.id >= 1",
            Integer.class).setParameter("userId", userId).getResultStream().collect(Collectors.toSet());

    music.forEach(currentMusic -> {
      List<AnnotationHasMusic> filteredAnnotationHasMusic = currentMusic.getAnnotationHasMusic().stream()
                                                                        .filter(currentAnnotationHasMusic -> allowedMediumAnalysisList.contains(
                                                                                currentAnnotationHasMusic.getAnnotation()
                                                                                                         .getMediumAnalysisListId()))
                                                                        .collect(Collectors.toList());
      currentMusic.setAnnotationHasMusic(filteredAnnotationHasMusic);
    });
  }
}
