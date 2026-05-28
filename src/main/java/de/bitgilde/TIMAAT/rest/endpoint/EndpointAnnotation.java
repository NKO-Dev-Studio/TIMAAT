package de.bitgilde.TIMAAT.rest.endpoint;

import de.bitgilde.TIMAAT.SelectElement;
import de.bitgilde.TIMAAT.TIMAATApp;
import de.bitgilde.TIMAAT.db.exception.DbTransactionExecutionException;
import de.bitgilde.TIMAAT.model.DataTableInfo;
import de.bitgilde.TIMAAT.model.FIPOP.Actor;
import de.bitgilde.TIMAAT.model.FIPOP.Analysis;
import de.bitgilde.TIMAAT.model.FIPOP.Annotation;
import de.bitgilde.TIMAAT.model.FIPOP.AnnotationHasMusic;
import de.bitgilde.TIMAAT.model.FIPOP.AnnotationHasMusicTranslationArea;
import de.bitgilde.TIMAAT.model.FIPOP.Category;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySet;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySetHasCategory;
import de.bitgilde.TIMAAT.model.FIPOP.Event;
import de.bitgilde.TIMAAT.model.FIPOP.MediumAnalysisList;
import de.bitgilde.TIMAAT.model.FIPOP.Tag;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount;
import de.bitgilde.TIMAAT.model.IndexBasedRange;
import de.bitgilde.TIMAAT.notification.NotificationWebSocket;
import de.bitgilde.TIMAAT.processing.video.FfmpegVideoEngine;
import de.bitgilde.TIMAAT.processing.video.exception.VideoEngineException;
import de.bitgilde.TIMAAT.rest.Secured;
import de.bitgilde.TIMAAT.rest.filter.AuthenticationFilter;
import de.bitgilde.TIMAAT.rest.model.CountResult;
import de.bitgilde.TIMAAT.rest.model.annotation.AnnotationListingQueryParameter;
import de.bitgilde.TIMAAT.rest.model.annotation.CreateAnnotationPayload;
import de.bitgilde.TIMAAT.rest.model.annotation.UpdateAnnotationPayload;
import de.bitgilde.TIMAAT.rest.model.annotation.UpdateAnnotationThumbnailPayload;
import de.bitgilde.TIMAAT.rest.model.category.UpdateAssignedCategoriesPayload;
import de.bitgilde.TIMAAT.rest.model.tags.UpdateAssignedTagsPayload;
import de.bitgilde.TIMAAT.rest.security.authorization.AnalysisListAuthorizationVerifier;
import de.bitgilde.TIMAAT.rest.security.authorization.AnnotationAuthorizationVerifier;
import de.bitgilde.TIMAAT.rest.security.authorization.PermissionType;
import de.bitgilde.TIMAAT.security.UserLogManager;
import de.bitgilde.TIMAAT.storage.entity.annotation.AnnotationStorage;
import de.bitgilde.TIMAAT.storage.entity.annotation.AnnotationStorage.CreateAnnotation;
import de.bitgilde.TIMAAT.storage.entity.annotation.AnnotationStorage.UpdateAnnotation;
import de.bitgilde.TIMAAT.storage.entity.annotation.AnnotationStorage.UpdateSelectorSvg;
import de.bitgilde.TIMAAT.storage.file.AnnotationFileStorage;
import de.bitgilde.TIMAAT.storage.file.TemporaryFileStorage;
import de.bitgilde.TIMAAT.storage.file.TemporaryFileStorage.TemporaryFile;
import de.bitgilde.TIMAAT.storage.file.VideoFileStorage;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
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
 * @author Jens-Martin Loebel <loebel@bitgilde.de>
 * @author Mirko Scherf <mscherf@uni-mainz.de>
 */
@Service
@Path("/annotation")
public class EndpointAnnotation {
  private static final Logger logger = Logger.getLogger(EndpointAnnotation.class.getName());

  @Context
  ContainerRequestContext containerRequestContext;
  @Inject
  AnnotationStorage annotationStorage;
  @Inject
  AnnotationAuthorizationVerifier annotationAuthorizationVerifier;
  @Inject
  AnalysisListAuthorizationVerifier analysisListAuthorizationVerifier;
  @Inject
  AnnotationFileStorage annotationFileStorage;
  @Inject
  VideoFileStorage videoFileStorage;
  @Inject
  FfmpegVideoEngine ffmpegVideoEngine;
  @Inject
  TemporaryFileStorage temporaryFileStorage;

  @GET
  @Produces
  @Path("list")
  @Secured
  public DataTableInfo<Annotation> getAnnotationList(@BeanParam @Valid AnnotationListingQueryParameter annotationListingQueryParameter) {
    UserAccount userAccount = (UserAccount) containerRequestContext.getProperty(
            AuthenticationFilter.USER_ACCOUNT_PROPERTY_NAME);
    int draw = annotationListingQueryParameter.getDraw().orElse(0);

    List<Annotation> matchingAnnotations = annotationStorage.getEntriesAsStream(annotationListingQueryParameter,
            annotationListingQueryParameter, annotationListingQueryParameter, userAccount).collect(Collectors.toList());
    long totalAnnotationsCount = annotationStorage.getNumberOfTotalEntriesRespectingAuthorization(userAccount);
    long filteredAnnotationsCount = annotationStorage.getNumberOfMatchingEntriesRespectingAuthorization(annotationListingQueryParameter, userAccount);

    return new DataTableInfo<>(draw, totalAnnotationsCount, filteredAnnotationsCount, matchingAnnotations);
  }

  @GET
  @Produces
  @Path("count")
  public CountResult getCountOfAnnotations(@BeanParam @Valid AnnotationListingQueryParameter annotationListingQueryParameter) {
    UserAccount userAccount = (UserAccount) containerRequestContext.getProperty(
            AuthenticationFilter.USER_ACCOUNT_PROPERTY_NAME);
    long filteredAnnotationsCount = annotationStorage.getNumberOfMatchingEntriesRespectingAuthorization(annotationListingQueryParameter, userAccount);

    return new CountResult(filteredAnnotationsCount);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}")
  @Secured
  public Response getAnnotation(@PathParam("id") int id) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Annotation annotation = entityManager.find(Annotation.class, id);
    if (annotation == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    return Response.ok().entity(annotation).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}/analysis-list")
  @Secured
  public MediumAnalysisList getMediumAnalysisListOfAnnotation(@PathParam("id") int id) {
    return annotationStorage.getMediumAnalysisListOfAnnotation(id);
  }


  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/actors/{actorID}")
  public Response addActor(@PathParam("id") int id, @PathParam("actorID") int actorID, @QueryParam("authToken") String authToken) {

    EntityManager em = TIMAATApp.emf.createEntityManager();
    Annotation annotation = em.find(Annotation.class, id);
    Actor actor = em.find(Actor.class, actorID);

    if (annotation == null || actor == null) {
      return Response.ok().entity(false).build();
    }
    if (annotation.getActors().contains(actor)) {
      return Response.ok().entity(false).build();
    }

    // verify auth token
    int userId = 0;
    if (AuthenticationFilter.isTokenValid(authToken)) {
      userId = AuthenticationFilter.getTokenClaimUserId(authToken);
    }
    else {
      return Response.status(Status.UNAUTHORIZED).build();
    }
    // check for permission level
    if (EndpointUserAccount.getPermissionLevelForAnalysisList(userId, annotation.getMediumAnalysisList().getId()) < 2) {
      return Response.status(Status.FORBIDDEN).build();
    }

    annotation.getActors().add(actor);
    actor.getAnnotations().add(annotation);

    EntityTransaction entityTransaction = em.getTransaction();
    entityTransaction.begin();
    em.merge(annotation);
    em.persist(annotation);
    em.merge(actor);
    em.persist(actor);
    entityTransaction.commit();
    em.refresh(annotation);
    em.refresh(actor);

    // TODO log entry annotation modified
    // TODO ? should this send notification event as well ?

    return Response.ok().entity(true).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/actors/{actorID}")
  public Response removeActor(@PathParam("id") int id, @PathParam("actorID") int actorID, @QueryParam("authToken") String authToken) {
    EntityManager em = TIMAATApp.emf.createEntityManager();
    Annotation annotation = em.find(Annotation.class, id);
    Actor actor = em.find(Actor.class, actorID);

    if (annotation == null || actor == null) {
      return Response.ok().entity(false).build();
    }
    if (annotation.getActors().contains(actor) == false) {
      return Response.ok().entity(false).build();
    }

    // verify auth token
    int userId = 0;
    if (AuthenticationFilter.isTokenValid(authToken)) {
      userId = AuthenticationFilter.getTokenClaimUserId(authToken);
    }
    else {
      return Response.status(Status.UNAUTHORIZED).build();
    }
    // check for permission level
    if (EndpointUserAccount.getPermissionLevelForAnalysisList(userId, annotation.getMediumAnalysisList().getId()) < 2) {
      return Response.status(Status.FORBIDDEN).build();
    }

    annotation.getActors().remove(actor);
    actor.getAnnotations().remove(annotation);

    EntityTransaction entityTransaction = em.getTransaction();
    entityTransaction.begin();
    em.merge(annotation);
    em.persist(annotation);
    em.merge(actor);
    em.persist(actor);
    entityTransaction.commit();
    em.refresh(annotation);
    em.refresh(actor);

    return Response.ok().entity(true).build();
  }

  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{annotationId}/music/{musicId}/translationArea/{languageId}")
  public AnnotationHasMusicTranslationArea updateAnnotationHasMusicTranslationArea(@PathParam("annotationId") int annotationId, @PathParam("musicId") int musicId, @PathParam("languageId") int languageId, IndexBasedRange indexBasedRange) throws DbTransactionExecutionException {
    return annotationStorage.setTranscriptionAreaToAnnotationHasMusicForLanguage(annotationId, musicId, languageId,
            indexBasedRange);
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{annotationId}/music/{musicId}/translationArea/{languageId}")
  public boolean removeAnnotationHasMusicTranslationArea(@PathParam("annotationId") int annotationId, @PathParam("musicId") int musicId, @PathParam("languageId") int languageId) throws DbTransactionExecutionException {
    return annotationStorage.removeTranscriptionAreaFromAnnotationHasMusicForLanguage(annotationId, musicId,
            languageId);
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{annotationId}/music/{musicId}")
  public AnnotationHasMusic addMusic(@PathParam("annotationId") int annotationId, @PathParam("musicId") int musicId) throws DbTransactionExecutionException {
    verifyAuthorizationToAnnotationHavingId(annotationId, PermissionType.WRITE);
    return annotationStorage.addMusicToAnnotation(annotationId, musicId);
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{annotationId}/music/{musicId}")
  public boolean removeMusic(@PathParam("annotationId") int annotationId, @PathParam("musicId") int musicId) throws DbTransactionExecutionException {
    verifyAuthorizationToAnnotationHavingId(annotationId, PermissionType.WRITE);
    return annotationStorage.removeMusicFromAnnotation(annotationId, musicId);
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/events/{eventId}")
  public Response addEvent(@PathParam("id") int id, @PathParam("eventId") int eventId, @QueryParam("authToken") String authToken) {
    EntityManager em = TIMAATApp.emf.createEntityManager();
    Annotation annotation = em.find(Annotation.class, id);
    Event event = em.find(Event.class, eventId);

    if (annotation == null || event == null) {
      return Response.ok().entity(false).build();
    }
    if (annotation.getEvents().contains(event)) {
      return Response.ok().entity(false).build();
    }

    // verify auth token
    int userId = 0;
    if (AuthenticationFilter.isTokenValid(authToken)) {
      userId = AuthenticationFilter.getTokenClaimUserId(authToken);
    }
    else {
      return Response.status(Status.UNAUTHORIZED).build();
    }
    // check for permission level
    if (EndpointUserAccount.getPermissionLevelForAnalysisList(userId, annotation.getMediumAnalysisList().getId()) < 2) {
      return Response.status(Status.FORBIDDEN).build();
    }

    annotation.getEvents().add(event);
    event.getAnnotations().add(annotation);

    EntityTransaction entityTransaction = em.getTransaction();
    entityTransaction.begin();
    em.merge(annotation);
    em.persist(annotation);
    em.merge(event);
    em.persist(event);
    entityTransaction.commit();
    em.refresh(annotation);
    em.refresh(event);

    // TODO log entry annotation modified
    // TODO ? should this send notification event as well ?

    return Response.ok().entity(true).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/events/{eventId}")
  public Response removeEvent(@PathParam("id") int id, @PathParam("eventId") int eventId, @QueryParam("authToken") String authToken) {
    EntityManager em = TIMAATApp.emf.createEntityManager();
    Annotation annotation = em.find(Annotation.class, id);
    Event event = em.find(Event.class, eventId);

    if (annotation == null || event == null) {
      return Response.ok().entity(false).build();
    }
    if (annotation.getEvents().contains(event) == false) {
      return Response.ok().entity(false).build();
    }

    // verify auth token
    int userId = 0;
    if (AuthenticationFilter.isTokenValid(authToken)) {
      userId = AuthenticationFilter.getTokenClaimUserId(authToken);
    }
    else {
      return Response.status(Status.UNAUTHORIZED).build();
    }
    // check for permission level
    if (EndpointUserAccount.getPermissionLevelForAnalysisList(userId, annotation.getMediumAnalysisList().getId()) < 2) {
      return Response.status(Status.FORBIDDEN).build();
    }

    annotation.getEvents().remove(event);
    event.getAnnotations().remove(annotation);

    EntityTransaction entityTransaction = em.getTransaction();
    entityTransaction.begin();
    em.merge(annotation);
    em.persist(annotation);
    em.merge(event);
    em.persist(event);
    entityTransaction.commit();
    em.refresh(annotation);
    em.refresh(event);

    return Response.ok().entity(true).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/actors")
  public Response getAnnotationActors(@PathParam("id") int id, @QueryParam("draw") Integer draw, @QueryParam("start") Integer start, @QueryParam("length") Integer length, @QueryParam("orderby") String orderby, @QueryParam("dir") String direction, @QueryParam("search") String search, // not supported
                                      @QueryParam("asDataTable") String asDataTable) {
    // System.out.println("EndpointAnnotation: getAnnotationActors: draw: "+draw+" start: "+start+" length: "+length+" orderby: "+orderby+" dir: "+direction+" search: "+search+" asDataTable: "+asDataTable);
    // sanitize user input
    if (draw == null) {
      draw = 0;
    }
    if (direction != null && direction.equalsIgnoreCase("desc")) {
      direction = "DESC";
    }
    else {
      direction = "ASC";
    }
    // String column = "a.id";
    // if ( orderby != null ) {
    // 	if (orderby.equalsIgnoreCase("name")) column = "a.displayName.name"; // TODO change displayName access in DB-Schema
    // }

    // retrieve annotation
    Annotation anno = TIMAATApp.emf.createEntityManager().find(Annotation.class, id);
    if (asDataTable == null) {
      if (anno != null) {
        return Response.ok().entity(anno.getActors()).build();
      }
      else {
        return Response.status(Status.NOT_FOUND).build();
      }
    }
    else {
      if (anno == null) {
        return Response.ok().entity(new DataTableInfo(draw, 0, 0, new ArrayList<Actor>())).build();
      }
      else {
        List<Actor> actors = anno.getActors();
        if (actors.size() == 0) {
          return Response.ok().entity(new DataTableInfo(draw, 0, 0, actors)).build();
        }
        if (direction.compareTo("ASC") == 0) {
          Collections.sort(actors,
                  (Comparator<Actor>) (Actor a1, Actor a2) -> a1.getDisplayName().getName().toLowerCase().compareTo(
                          a2.getDisplayName().getName().toLowerCase()));
        }
        else {
          Collections.sort(actors,
                  ((Comparator<Actor>) (Actor a1, Actor a2) -> a1.getDisplayName().getName().toLowerCase().compareTo(
                          a2.getDisplayName().getName().toLowerCase())).reversed());
        }

        if (start != null) {
          if (start < 0) {
            start = 0;
          }
          if (start > actors.size() - 1) {
            start = actors.size() - 1;
          }
          if (length == null) {
            length = 1;
          }
          if (length < 1) {
            length = 1;
          }
          if ((start + length) > actors.size()) {
            length = actors.size() - start;
          }
          return Response.ok().entity(new DataTableInfo(draw, actors.size(), actors.size(),
                  actors.subList(start, start + length))).build();
        }
        else {
          return Response.ok().entity(new DataTableInfo(draw, actors.size(), actors.size(), actors)).build();
        }
      }
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/analysis")
  public Response getAnnotationAnalysis(@PathParam("id") int id, @QueryParam("draw") Integer draw, @QueryParam("start") Integer start, @QueryParam("length") Integer length, @QueryParam("orderby") String orderby, @QueryParam("dir") String direction, @QueryParam("search") String search, //* not supported
                                        @QueryParam("asDataTable") String asDataTable) {
    // System.out.println("EndpointAnnotation: getAnnotationAnalysis: draw: "+draw+" start: "+start+" length: "+length+" orderby: "+orderby+" dir: "+direction+" search: "+search+" asDataTable: "+asDataTable);
    // sanitize user input
    if (draw == null) {
      draw = 0;
    }
    if (direction != null && direction.equalsIgnoreCase("desc")) {
      direction = "DESC";
    }
    else {
      direction = "ASC";
    }
    // String column = "a.id";
    // if ( orderby != null ) {
    // 	if (orderby.equalsIgnoreCase("name")) column = "a.analysisMethod.analysisMethodType.analysisMethodTypeTranslation.name";
    // }

    // retrieve annotation
    Annotation anno = TIMAATApp.emf.createEntityManager().find(Annotation.class, id);
    if (asDataTable == null) {
      if (anno != null) {
        return Response.ok().entity(anno.getAnalysis()).build();
      }
      else {
        return Response.status(Status.NOT_FOUND).build();
      }
    }
    else {
      if (anno == null) {
        return Response.ok().entity(new DataTableInfo(draw, 0, 0, new ArrayList<Analysis>())).build();
      }
      else {
        List<Analysis> analysis = anno.getAnalysis();
        if (analysis.size() == 0) {
          return Response.ok().entity(new DataTableInfo(draw, 0, 0, analysis)).build();
        }
        if (direction.compareTo("ASC") == 0) {
          Collections.sort(analysis,
                  (Comparator<Analysis>) (Analysis a1, Analysis a2) -> a1.getAnalysisMethod().getAnalysisMethodType()
                                                                         .getAnalysisMethodTypeTranslations().get(0)
                                                                         .getName().toLowerCase().compareTo(
                                  a2.getAnalysisMethod().getAnalysisMethodType().getAnalysisMethodTypeTranslations()
                                    .get(0).getName().toLowerCase()));
        }
        else {
          Collections.sort(analysis,
                  ((Comparator<Analysis>) (Analysis a1, Analysis a2) -> a1.getAnalysisMethod().getAnalysisMethodType()
                                                                          .getAnalysisMethodTypeTranslations().get(0)
                                                                          .getName().toLowerCase().compareTo(
                                  a2.getAnalysisMethod().getAnalysisMethodType().getAnalysisMethodTypeTranslations()
                                    .get(0).getName().toLowerCase())).reversed());
        }
        if (start != null) {
          if (start < 0) {
            start = 0;
          }
          if (start > analysis.size() - 1) {
            start = analysis.size() - 1;
          }
          if (length == null) {
            length = 1;
          }
          if (length < 1) {
            length = 1;
          }
          if ((start + length) > analysis.size()) {
            length = analysis.size() - start;
          }
          return Response.ok().entity(new DataTableInfo(draw, analysis.size(), analysis.size(),
                  analysis.subList(start, start + length))).build();
        }
        else {
          return Response.ok().entity(new DataTableInfo(draw, analysis.size(), analysis.size(), analysis)).build();
        }
      }
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/events")
  public Response getAnnotationEvents(@PathParam("id") int id, @QueryParam("draw") Integer draw, @QueryParam("start") Integer start, @QueryParam("length") Integer length, @QueryParam("orderby") String orderby, @QueryParam("dir") String direction, @QueryParam("search") String search, // not supported
                                      @QueryParam("asDataTable") String asDataTable) {
    // System.out.println("EndpointAnnotation: getAnnotationEvent: draw: "+draw+" start: "+start+" length: "+length+" orderby: "+orderby+" dir: "+direction+" search: "+search+" asDataTable: "+asDataTable);
    // sanitize user input
    if (draw == null) {
      draw = 0;
    }
    if (direction != null && direction.equalsIgnoreCase("desc")) {
      direction = "DESC";
    }
    else {
      direction = "ASC";
    }
    // String column = "e.id";
    // if ( orderby != null ) {
    // 	if (orderby.equalsIgnoreCase("name")) column = "et.name";
    // }

    // retrieve annotation
    Annotation anno = TIMAATApp.emf.createEntityManager().find(Annotation.class, id);
    if (asDataTable == null) {
      if (anno != null) {
        return Response.ok().entity(anno.getEvents()).build();
      }
      else {
        return Response.status(Status.NOT_FOUND).build();
      }
    }
    else {
      if (anno == null) {
        return Response.ok().entity(new DataTableInfo(draw, 0, 0, new ArrayList<Event>())).build();
      }
      else {
        List<Event> events = anno.getEvents();
        if (events.size() == 0) {
          return Response.ok().entity(new DataTableInfo(draw, 0, 0, events)).build();
        }
        if (direction.compareTo("ASC") == 0) {
          Collections.sort(events,
                  (Comparator<Event>) (Event a1, Event a2) -> a1.getEventTranslations().get(0).getName().toLowerCase()
                                                                .compareTo(a2.getEventTranslations().get(0).getName()
                                                                             .toLowerCase()));
        }
        else {
          Collections.sort(events,
                  ((Comparator<Event>) (Event a1, Event a2) -> a1.getEventTranslations().get(0).getName().toLowerCase()
                                                                 .compareTo(a2.getEventTranslations().get(0).getName()
                                                                              .toLowerCase())).reversed());
        }

        if (start != null) {
          if (start < 0) {
            start = 0;
          }
          if (start > events.size() - 1) {
            start = events.size() - 1;
          }
          if (length == null) {
            length = 1;
          }
          if (length < 1) {
            length = 1;
          }
          if ((start + length) > events.size()) {
            length = events.size() - start;
          }
          return Response.ok().entity(new DataTableInfo(draw, events.size(), events.size(),
                  events.subList(start, start + length))).build();
        }
        else {
          return Response.ok().entity(new DataTableInfo(draw, events.size(), events.size(), events)).build();
        }
      }
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/category/list")
  public Response getSelectedCategories(@PathParam("id") Integer id) {
    // System.out.println("EndpointAnnotation: getSelectedCategories - Id: "+ id);
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Annotation annotation = entityManager.find(Annotation.class, id);
    List<Category> categoryList = annotation.getCategories();
    return Response.ok().entity(categoryList).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/category/selectList")
  public Response getCategorySelectList(@PathParam("id") Integer id, @QueryParam("start") Integer start, @QueryParam("length") Integer length, @QueryParam("orderby") String orderby, @QueryParam("dir") String direction, @QueryParam("search") String search) {
    // System.out.println("EndpointAnnotation: getCategorySelectList - Id: "+ id);

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Annotation annotation = entityManager.find(Annotation.class, id);
    MediumAnalysisList mediumAnalysisList = annotation.getMediumAnalysisList();
    List<CategorySet> categorySetList = mediumAnalysisList.getCategorySets();
    List<Category> categoryList = new ArrayList<>();
    // List<Category> annotationCategories = annotation.getCategories();
    List<SelectElement> categorySelectList = new ArrayList<>();

    for (CategorySet categorySet : categorySetList) {
      Set<CategorySetHasCategory> cshc = categorySet.getCategorySetHasCategories();
      Iterator<CategorySetHasCategory> itr = cshc.iterator();
      while (itr.hasNext()) {
        // categorySelectList.add(new SelectElement<Integer>(itr.next().getCategory().getId(), itr.next().getCategory().getName()));
        categoryList.add(itr.next().getCategory());
      }
    }
    // for (Category category : categoryList) {
    // 	categorySelectList.add(new SelectElement<Integer>(category.getId(), category.getName()))
    // }

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
      Collections.sort(categoryList, (Comparator<Category>) (Category c1, Category c2) -> c1.getName().toLowerCase()
                                                                                            .compareTo(c2.getName()
                                                                                                         .toLowerCase()));
      for (Category category : categoryList) {
        categorySelectList.add(new SelectElement<Integer>(category.getId(), category.getName()));
      }
    }

    return Response.ok().entity(categorySelectList).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/hasTagList")
  public Response getTagList(@PathParam("id") Integer annotationId) {
    // System.out.println("EndpointAnnotation: getTagList");
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Annotation annotation = entityManager.find(Annotation.class, annotationId);
    if (annotation == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    entityManager.refresh(annotation);
    return Response.ok().entity(annotation.getTags()).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("mediumAnalysisList/{mediumAnalysisListId}")
  @Secured
  public Annotation createAnnotation(@PathParam("mediumAnalysisListId") int mediumAnalysisListId, CreateAnnotationPayload createAnnotationPayload) throws VideoEngineException, IOException {
    verifyAuthorizationToAnalysisList(mediumAnalysisListId, PermissionType.WRITE);

    UserAccount userAccount = (UserAccount) containerRequestContext.getProperty(
            AuthenticationFilter.USER_ACCOUNT_PROPERTY_NAME);

    UpdateSelectorSvg updateSelectorSvg = new UpdateSelectorSvg(createAnnotationPayload.getSelectorSvg().getColorHex(),
            createAnnotationPayload.getSelectorSvg().getOpacity(),
            createAnnotationPayload.getSelectorSvg().getStrokeWidth(),
            createAnnotationPayload.getSelectorSvg().getSvgData());

    CreateAnnotation createAnnotation = new CreateAnnotation(createAnnotationPayload.getTitle(),
            createAnnotationPayload.getComment(), createAnnotationPayload.getStartTime(),
            createAnnotationPayload.getEndTime(), createAnnotationPayload.isLayerVisual(),
            createAnnotationPayload.isLayerAudio(), updateSelectorSvg, mediumAnalysisListId);

    Annotation createdAnnotation = annotationStorage.createAnnotation(createAnnotation, userAccount);
    int annotationThumbnailPosition = createAnnotationPayload.getThumbnailPositionMs() != null ? createAnnotationPayload.getThumbnailPositionMs() : createAnnotationPayload.getStartTime();

    if (updateAnnotationThumbnail(createdAnnotation, annotationThumbnailPosition, userAccount).isPresent()) {
      createdAnnotation.setThumbnailPositionMs(annotationThumbnailPosition);
    }

    return createdAnnotation;
  }

  @GET
  @Produces("image/png")
  @Path("/{id}/thumbnail")
  @Secured
  public Response getAnnotationThumbnail(@PathParam("id") int annotationId) throws VideoEngineException, IOException {
    verifyAuthorizationToAnnotationHavingId(annotationId, PermissionType.READ);
    Optional<java.nio.file.Path> thumbnailFilePath = annotationFileStorage.getPathToThumbnail(annotationId);

    if (thumbnailFilePath.isPresent()) {
      return Response.ok(thumbnailFilePath.get().toFile()).build();
    }
    else {
      Annotation annotation = annotationStorage.getAnnotationById(annotationId);

      if (annotation.getThumbnailPositionMs() != null) {
        logger.log(Level.WARNING,
                "Could not find thumbnail for annotation with id {0} which should be present. Creating new one.",
                annotationId);
        Optional<java.nio.file.Path> originalVideoFilePath = videoFileStorage.getPathToOriginalFile(
                annotation.getMediumId());
        if (originalVideoFilePath.isPresent()) {
          java.nio.file.Path createdThumbnailPath = createThumbnailForAnnotation(annotationId,
                  originalVideoFilePath.get(), annotation.getThumbnailPositionMs());
          return Response.ok(createdThumbnailPath.toFile()).build();
        }
      }
    }

    return Response.status(Status.NOT_FOUND).build();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces("image/png")
  @Path("/{id}/thumbnail")
  @Secured
  public Response updateAnnotationThumbnail(@PathParam("id") int annotationId, UpdateAnnotationThumbnailPayload updateAnnotationThumbnailPayload) throws VideoEngineException, IOException {
    verifyAuthorizationToAnnotationHavingId(annotationId, PermissionType.WRITE);

    UserAccount userAccount = (UserAccount) containerRequestContext.getProperty(
            AuthenticationFilter.USER_ACCOUNT_PROPERTY_NAME);
    Annotation annotation = annotationStorage.getAnnotationById(annotationId);

    Optional<java.nio.file.Path> createdAnnotationThumbnail = updateAnnotationThumbnail(annotation,
            updateAnnotationThumbnailPayload.getThumbnailPositionMs(), userAccount);
    if (createdAnnotationThumbnail.isPresent()) {
      return Response.ok(createdAnnotationThumbnail.get().toFile()).build();
    }

    return Response.status(Status.BAD_REQUEST).build();
  }

  private Optional<java.nio.file.Path> updateAnnotationThumbnail(Annotation annotation, int thumbnailPositionMs, UserAccount userAccount) throws VideoEngineException, IOException {
    Optional<java.nio.file.Path> originalVideoFilePathOptional = videoFileStorage.getPathToOriginalFile(
            annotation.getMediumId());
    if (originalVideoFilePathOptional.isPresent()) {
      annotationStorage.updateThumbnailPositionMs(annotation.getId(), thumbnailPositionMs, userAccount);
      return Optional.of(createThumbnailForAnnotation(annotation.getId(), originalVideoFilePathOptional.get(),
              thumbnailPositionMs));
    }

    return Optional.empty();
  }

  /**
   * This method creates a thumbnail for the specified annotation
   * <br/>
   * To create a thumbnail the annotation must be related to a medium video. On other medium types there is no video stream
   * to gather a thumbnail frame.
   * @param annotationId
   * @param thumbnailPositionMs
   * @param originalVideoFilePath from which the thumbnail will be extracted from
   * @return optional containing path to the created thumbnail.
   *
   * @throws IOException
   * @throws VideoEngineException
   */
  private java.nio.file.Path createThumbnailForAnnotation(int annotationId, java.nio.file.Path originalVideoFilePath, int thumbnailPositionMs) throws IOException, VideoEngineException {
    float frameTimeStampInSeconds = thumbnailPositionMs / 1000f;
    try (TemporaryFile temporaryFile = temporaryFileStorage.createTemporaryFile()) {
      ffmpegVideoEngine.extractFrameOfVideo(originalVideoFilePath, temporaryFile.getTemporaryFilePath(),
              frameTimeStampInSeconds);
      return annotationFileStorage.persistThumbnail(annotationId, temporaryFile.getTemporaryFilePath());
    }
  }

  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("{id}")
  @Secured
  public Annotation updateAnnotation(@PathParam("id") int id, @Valid UpdateAnnotationPayload updateAnnotationPayload) {
    verifyAuthorizationToAnnotationHavingId(id, PermissionType.WRITE);

    UserAccount userAccount = (UserAccount) containerRequestContext.getProperty(
            AuthenticationFilter.USER_ACCOUNT_PROPERTY_NAME);

    UpdateSelectorSvg updateSelectorSvg = new UpdateSelectorSvg(updateAnnotationPayload.getSelectorSvg().getColorHex(),
            updateAnnotationPayload.getSelectorSvg().getOpacity(),
            updateAnnotationPayload.getSelectorSvg().getStrokeWidth(),
            updateAnnotationPayload.getSelectorSvg().getSvgData());
    UpdateAnnotation updateAnnotation = new UpdateAnnotation(id, updateAnnotationPayload.getTitle(),
            updateAnnotationPayload.getComment(), updateAnnotationPayload.getStartTime(),
            updateAnnotationPayload.getEndTime(), updateAnnotationPayload.isLayerVisual(),
            updateAnnotationPayload.isLayerAudio(), updateSelectorSvg);
    Annotation updatedAnnotation = annotationStorage.updateAnnotation(updateAnnotation, userAccount);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANNOTATIONEDITED);

    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "editAnnotation", updatedAnnotation.getMediumAnalysisList().getId(), updatedAnnotation);
    return updatedAnnotation;
  }

  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("{id}/tags")
  @Secured
  public List<Tag> updateTags(@PathParam("id") int annotationId, UpdateAssignedTagsPayload updateAssignedTagsPayload) {
    verifyAuthorizationToAnnotationHavingId(annotationId, PermissionType.WRITE);
    return annotationStorage.updateTagsOfAnnotation(annotationId, updateAssignedTagsPayload.getTagNames());
  }

  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("{id}/categories")
  @Secured
  public List<Category> updateCategories(@PathParam("id") int annotationId, UpdateAssignedCategoriesPayload updateAssignedCategoriesPayload) {
    verifyAuthorizationToAnnotationHavingId(annotationId, PermissionType.WRITE);
    return annotationStorage.updateCategoriesOfAnnotation(annotationId,
            updateAssignedCategoriesPayload.getCategoryIds());
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}")
  @Secured
  public Response deleteAnnotation(@PathParam("id") int id) {
    verifyAuthorizationToAnnotationHavingId(id, PermissionType.WRITE);

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Annotation annotation = entityManager.find(Annotation.class, id);
    if (annotation == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    MediumAnalysisList mal = annotation.getMediumAnalysisList();
/*
		mal.removeAnnotation(annotation);
		entityManager.persist(mal);
*/
    entityManager.remove(annotation);
    entityTransaction.commit();
    entityManager.refresh(mal);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANNOTATIONDELETED);

    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "removeAnnotation", mal.getId(), annotation);

    return Response.ok().build();
  }


  public static <T> List<T> castList(Class<? extends T> clazz, Collection<?> c) {
    List<T> r = new ArrayList<T>(c.size());
    for (Object o : c) {
      r.add(clazz.cast(o));
    }
    return r;
  }

  private void verifyAuthorizationToAnnotationHavingId(int annotationId, PermissionType permissionType) throws DbTransactionExecutionException, ForbiddenException {
    int userId = (int) containerRequestContext.getProperty(AuthenticationFilter.USER_ID_PROPERTY_NAME);


    if (!annotationAuthorizationVerifier.verifyAuthorizationToAnnotation(annotationId, userId, permissionType)) {
      throw new ForbiddenException("User has no access to annotation");
    }
  }

  private void verifyAuthorizationToAnalysisList(int analysisListId, PermissionType permissionType) {
    UserAccount userAccount = (UserAccount) containerRequestContext.getProperty(
            AuthenticationFilter.USER_ACCOUNT_PROPERTY_NAME);

    if (!analysisListAuthorizationVerifier.verifyAuthorizationToAnalysisList(analysisListId, userAccount,
            permissionType)) {
      throw new ForbiddenException("User has no access to analysis list");
    }
  }
}
