package de.bitgilde.TIMAAT.rest.endpoint;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bitgilde.TIMAAT.DisplayElementNameAndPermission;
import de.bitgilde.TIMAAT.SelectElement;
import de.bitgilde.TIMAAT.TIMAATApp;
import de.bitgilde.TIMAAT.db.exception.DbTransactionExecutionException;
import de.bitgilde.TIMAAT.model.FIPOP.AnalysisAction;
import de.bitgilde.TIMAAT.model.FIPOP.AnalysisActionTranslation;
import de.bitgilde.TIMAAT.model.FIPOP.AnalysisScene;
import de.bitgilde.TIMAAT.model.FIPOP.AnalysisSceneTranslation;
import de.bitgilde.TIMAAT.model.FIPOP.AnalysisSegment;
import de.bitgilde.TIMAAT.model.FIPOP.AnalysisSequence;
import de.bitgilde.TIMAAT.model.FIPOP.AnalysisSequenceTranslation;
import de.bitgilde.TIMAAT.model.FIPOP.AnalysisTake;
import de.bitgilde.TIMAAT.model.FIPOP.AnalysisTakeTranslation;
import de.bitgilde.TIMAAT.model.FIPOP.Annotation;
import de.bitgilde.TIMAAT.model.FIPOP.Category;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySet;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySetHasCategory;
import de.bitgilde.TIMAAT.model.FIPOP.Language;
import de.bitgilde.TIMAAT.model.FIPOP.Medium;
import de.bitgilde.TIMAAT.model.FIPOP.MediumAnalysisList;
import de.bitgilde.TIMAAT.model.FIPOP.MediumAnalysisListTranslation;
import de.bitgilde.TIMAAT.model.FIPOP.PermissionType;
import de.bitgilde.TIMAAT.model.FIPOP.SelectorSvg;
import de.bitgilde.TIMAAT.model.FIPOP.Tag;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccountHasMediumAnalysisList;
import de.bitgilde.TIMAAT.notification.NotificationWebSocket;
import de.bitgilde.TIMAAT.rest.Secured;
import de.bitgilde.TIMAAT.rest.filter.AuthenticationFilter;
import de.bitgilde.TIMAAT.rest.model.analysislist.UpdateAnalysisListCategorySetsPayload;
import de.bitgilde.TIMAAT.rest.model.category.UpdateAssignedCategoriesPayload;
import de.bitgilde.TIMAAT.rest.security.authorization.AnalysisListAuthorizationVerifier;
import de.bitgilde.TIMAAT.security.UserLogManager;
import de.bitgilde.TIMAAT.storage.entity.analysislist.AnalysisListStorage;
import de.bitgilde.TIMAAT.storage.entity.segment.SegmentStructureElementsStorage;
import de.bitgilde.TIMAAT.storage.entity.segment.api.SegmentStructureElementType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
 *
 * @author Jens-Martin Loebel <loebel@bitgilde.de>
 * @author Mirko Scherf <mscherf@uni-mainz.de>
 */
@Service
@Path("/analysisList")
public class EndpointAnalysisList {

  @Context
  ContainerRequestContext containerRequestContext;
  @Inject
  AnalysisListStorage analysisListStorage;
  @Inject
  AnalysisListAuthorizationVerifier analysisListAuthorizationVerifier;
  @Inject
  SegmentStructureElementsStorage segmentStructureElementsStorage;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}/displayNames")
  @Secured
  public Response getDisplayNamesAndPermissions(@PathParam("id") Integer mediumAnalysisListId, @QueryParam("authToken") String authToken) {
    // System.out.println("EndpointAnalysisList: getDisplayNames - ID: "+ mediumAnalysisListId);

    // verify auth token
    int userId = 0;
    if (AuthenticationFilter.isTokenValid(authToken)) {
      userId = AuthenticationFilter.getTokenClaimUserId(authToken);
    }
    else {
      return Response.status(Status.UNAUTHORIZED).build();
    }
    // check for permission level
    if (EndpointUserAccount.getPermissionLevelForAnalysisList(userId,
            mediumAnalysisListId) < 3) { // only mods and admins may see permission list
      return Response.status(Status.FORBIDDEN).build();
    }

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumAnalysisList mediumAnalysisList = entityManager.find(MediumAnalysisList.class, mediumAnalysisListId);
    List<UserAccountHasMediumAnalysisList> userAccountHasMediumAnalysisListList = mediumAnalysisList.getUserAccountHasMediumAnalysisLists();
    List<DisplayElementNameAndPermission> displayElementNameAndPermission = new ArrayList<>();
    for (UserAccountHasMediumAnalysisList userAccountHasMediumAnalysisList : userAccountHasMediumAnalysisListList) {
      displayElementNameAndPermission.add(
              new DisplayElementNameAndPermission(userAccountHasMediumAnalysisList.getUserAccount().getId(),
                      userAccountHasMediumAnalysisList.getUserAccount().getDisplayName(),
                      userAccountHasMediumAnalysisList.getPermissionType().getId()));
    }
    displayElementNameAndPermission.sort(
            Comparator.comparing((DisplayElementNameAndPermission d) -> d.getDisplayName().toLowerCase()));
    return Response.ok().entity(displayElementNameAndPermission).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/hasTagList")
  public Response getMediumAnalysisListTagList(@PathParam("id") Integer id) {
    // System.out.println("EndpointAnalysisList: getTagList");
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumAnalysisList mediumAnalysisList = entityManager.find(MediumAnalysisList.class, id);
    if (mediumAnalysisList == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    entityManager.refresh(mediumAnalysisList);
    return Response.ok().entity(mediumAnalysisList.getTags()).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/elements")
  public Response getMediumAnalysisListElements(@PathParam("id") Integer id) {
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumAnalysisList mediumAnalysisList = entityManager.find(MediumAnalysisList.class, id);
    List<Annotation> annotations = mediumAnalysisList.getAnnotations();
    List<AnalysisSegment> segments = mediumAnalysisList.getAnalysisSegments();
    List<Object> elementList = new ArrayList<Object>();
    elementList.addAll(annotations);
    elementList.addAll(segments);
    //* NB: returns an unsorted list
    return Response.ok().entity(elementList).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/categorySet/list")
  public Response getCategorySetList(@PathParam("id") Integer id) {
    // System.out.println("EndpointAnalysisList: getCategorySetList - ID: "+ id);
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumAnalysisList mediumAnalysisList = entityManager.find(MediumAnalysisList.class, id);
    List<CategorySet> categorySetList = mediumAnalysisList.getRestrictedCategorySets();

    return Response.ok().entity(categorySetList).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{segmentType}/{id}/category/list")
  public List<Category> getSegmentStructureElementCategories(@PathParam("id") int id, @PathParam("segmentType") SegmentStructureElementType segmentStructureElementType) {
    return segmentStructureElementsStorage.getAssignedCategories(id, segmentStructureElementType);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{segmentType}/{id}/category/selectList")
  public List<SelectElement<Integer>> getSegmentStructureElementCategoriesSelectList(@PathParam("id") int id, @PathParam("segmentType") SegmentStructureElementType segmentStructureElementType) {
    return segmentStructureElementsStorage.getAssignableCategories(id, segmentStructureElementType).stream().map(currentCategory -> new SelectElement<>(currentCategory.getId(), currentCategory.getName())).collect(
            Collectors.toList());
  }

  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{segmentType}/{id}/categories")
  public List<Category> updateSegmentStructureElementCategories(@PathParam("id") int id, @PathParam("segmentType") SegmentStructureElementType segmentStructureElementType, UpdateAssignedCategoriesPayload updateAssignedCategoriesPayload) {
    return segmentStructureElementsStorage.updateCategories(id, segmentStructureElementType,
            updateAssignedCategoriesPayload.getCategoryIds());
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Secured
  @Path("medium/{mediumId}")
  public Response createAnalysisList(@PathParam("mediumId") int mediumId, String jsonData) {
    ObjectMapper mapper = new ObjectMapper();
    MediumAnalysisList newList = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, mediumId);
    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      newList = mapper.readValue(jsonData, MediumAnalysisList.class);
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (newList == null) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    // sanitize object data
    newList.setId(0);
    newList.setMedium(medium);
    newList.setAnalysisSegments(new ArrayList<AnalysisSegment>());
    newList.setAnnotations(new ArrayList<Annotation>());
    newList.setUuid(UUID.randomUUID().toString());
    newList.getMediumAnalysisListTranslations().get(0).setId(0);
    newList.getMediumAnalysisListTranslations().get(0).setLanguage(entityManager.find(Language.class, 1));
    newList.getMediumAnalysisListTranslations().get(0).setMediumAnalysisList(newList);
    if (containerRequestContext.getProperty("TIMAAT.userID") != null) {
      newList.setCreatedByUserAccount(
              entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID")));
      newList.setLastEditedByUserAccount(
              (entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID"))));
    }
    else {
      // DEBUG do nothing - production system should abort with internal server error
      return Response.serverError().build();
    }
    Timestamp creationDate = new Timestamp(System.currentTimeMillis());
    newList.setCreatedAt(creationDate);
    newList.setLastEditedAt(creationDate);

    // persist analysisList and polygons
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(newList.getMediumAnalysisListTranslations().get(0));
    entityManager.persist(newList);
    medium.addMediumAnalysisList(newList);
    entityManager.persist(medium);
    entityManager.flush();
    entityTransaction.commit();
    entityManager.refresh(newList);
    entityManager.refresh(medium);

    // set initial permission for newly created analysis list
    UserAccount userAccount = entityManager.find(UserAccount.class,
            containerRequestContext.getProperty("TIMAAT.userID"));
    UserAccountHasMediumAnalysisList uahmal = new UserAccountHasMediumAnalysisList(userAccount, newList);
    PermissionType permissionType = entityManager.find(PermissionType.class, 4); // List creator becomes list admin
    uahmal.setPermissionType(permissionType);
    entityTransaction.begin();
    userAccount.getUserAccountHasMediumAnalysisLists().add(uahmal);
    newList.getUserAccountHasMediumAnalysisLists().add(uahmal);
    entityManager.merge(userAccount);
    entityManager.merge(newList);
    entityManager.persist(userAccount);
    entityManager.persist(newList);
    entityTransaction.commit();
    entityManager.refresh(userAccount);
    entityManager.refresh(newList);

    // add log entry
    UserLogManager.getLogger()
                  .addLogEntry(newList.getCreatedByUserAccount().getId(), UserLogManager.LogEvents.ANALYSISLISTCREATED);

    return Response.ok().entity(newList).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}")
  public Response getAnalysisList(@PathParam("id") Integer id) {
    // System.out.println("EndpointAnalysisList: getAnalysisList - ID: "+ id);
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumAnalysisList mediumAnalysisList = entityManager.find(MediumAnalysisList.class, id);

    return Response.ok().entity(mediumAnalysisList).build();
  }

  @PATCH
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("{id}")
  @Secured
  public Response updateAnalysisList(@PathParam("id") int mediumAnalysisListId, String jsonData, @QueryParam("authToken") String authToken) {

    // System.out.println("EndpointAnalysisList: updateAnalysisList "+ jsonData);

    // verify auth token
    int userId = 0;
    if (AuthenticationFilter.isTokenValid(authToken)) {
      userId = AuthenticationFilter.getTokenClaimUserId(authToken);
    }
    else {
      return Response.status(Status.UNAUTHORIZED).build();
    }
    // check for permission level
    if (EndpointUserAccount.getPermissionLevelForAnalysisList(userId, mediumAnalysisListId) < 2) {
      return Response.status(Status.FORBIDDEN).build();
    }

    ObjectMapper mapper = new ObjectMapper();
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumAnalysisList updatedList = null;
    MediumAnalysisList mediumAnalysisList = entityManager.find(MediumAnalysisList.class, mediumAnalysisListId);
    if (mediumAnalysisList == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    // parse JSON data
    try {
      mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
      updatedList = mapper.readValue(jsonData, MediumAnalysisList.class);
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedList == null) {
      return Response.notModified().build();
    }

    // update analysisList
    if (updatedList.getMediumAnalysisListTranslations() != null) {
      for (MediumAnalysisListTranslation trans : updatedList.getMediumAnalysisListTranslations()) {
        mediumAnalysisList.setTitle(trans.getTitle(), trans.getLanguage().getCode());
        mediumAnalysisList.setText(trans.getText(), trans.getLanguage().getCode());
      }
    }
    List<CategorySet> oldCategorySets = mediumAnalysisList.getRestrictedCategorySets();
    mediumAnalysisList.setRestrictedCategorySets(updatedList.getRestrictedCategorySets());
    List<Tag> oldTags = mediumAnalysisList.getTags();
    mediumAnalysisList.setTags(updatedList.getTags());
    mediumAnalysisList.setGlobalPermission(updatedList.getGlobalPermission());
    if (updatedList.getUuid() == null) {
      mediumAnalysisList.setUuid(
              UUID.randomUUID().toString()); // update entries that existed before uuid became a string
    }

    if (containerRequestContext.getProperty("TIMAAT.userID") != null) {
      mediumAnalysisList.setLastEditedByUserAccount(
              (entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID"))));
    }
    else {
      // DEBUG do nothing - production system should abort with internal server error
      return Response.serverError().build();
    }
    mediumAnalysisList.setLastEditedAt(new Timestamp(System.currentTimeMillis()));

    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(mediumAnalysisList);
    entityManager.persist(mediumAnalysisList);
    entityTransaction.commit();
    entityManager.refresh(mediumAnalysisList);
    for (CategorySet categorySet : mediumAnalysisList.getRestrictedCategorySets()) {
      entityManager.refresh(categorySet);
    }
    for (CategorySet categorySet : oldCategorySets) {
      entityManager.refresh(categorySet);
    }
    for (Tag tag : mediumAnalysisList.getTags()) {
      entityManager.refresh(tag);
    }
    for (Tag tag : oldTags) {
      entityManager.refresh(tag);
    }


    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISLISTEDITED);

    return Response.ok().entity(mediumAnalysisList).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}")
  @Secured
  public Response deleteAnalysisList(@PathParam("id") int mediumAnalysisListId, @QueryParam("authToken") String authToken) {

    // verify auth token
    int userId = 0;
    if (AuthenticationFilter.isTokenValid(authToken)) {
      userId = AuthenticationFilter.getTokenClaimUserId(authToken);
    }
    else {
      return Response.status(Status.UNAUTHORIZED).build();
    }
    // check for permission level
    if (EndpointUserAccount.getPermissionLevelForAnalysisList(userId, mediumAnalysisListId) < 4) {
      return Response.status(Status.FORBIDDEN).build();
    }

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumAnalysisList mediumAnalysisList = entityManager.find(MediumAnalysisList.class, mediumAnalysisListId);
    if (mediumAnalysisList == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    // remove all associated annotations
    for (Annotation anno : mediumAnalysisList.getAnnotations()) {
      entityManager.remove(anno);
    }
    mediumAnalysisList.getAnnotations().clear();
    // remove all associated segments
    for (AnalysisSegment segment : mediumAnalysisList.getAnalysisSegments()) {
      entityManager.remove(segment);
    }
    mediumAnalysisList.getAnalysisSegments().clear();
    entityManager.remove(mediumAnalysisList);
    entityTransaction.commit();

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISLISTDELETED);

    return Response.ok().build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{analysisListId}/userAccount/{userAccountId}/withPermission/{permissionId}")
  @Secured
  public Response addUserAccountHasMediumAnalysisListWithPermission(@PathParam("analysisListId") int mediumAnalysisListId, @PathParam("userAccountId") int userAccountId, @PathParam("permissionId") int permissionTypeId, @QueryParam("authToken") String authToken) {
    // System.out.println("EndpointMediumAnalysisList: addUserAccountHasMediumAnalysisListWithPermission");

    // verify auth token
    int userId = 0;
    if (AuthenticationFilter.isTokenValid(authToken)) {
      userId = AuthenticationFilter.getTokenClaimUserId(authToken);
    }
    else {
      return Response.status(Status.UNAUTHORIZED).build();
    }
    // check for permission level
    // only users with administrate permission level may add users with moderate or administrate permission
    int permissionLevel = EndpointUserAccount.getPermissionLevelForAnalysisList(userId, mediumAnalysisListId);
    // IF user is not sys admin AND (
    // user is without high enough permission level to set others to moderate or administrate
    // OR user is without high enough permission level to set any permission levels)
    if ((userId != 1) && ((permissionLevel != 4 && (permissionTypeId == 3 || permissionTypeId == 4)) || (permissionLevel != 3 && permissionLevel != 4))) {
      return Response.status(Status.FORBIDDEN).build();
    } // else user has permission for requested change

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumAnalysisList mediumAnalysisList = entityManager.find(MediumAnalysisList.class, mediumAnalysisListId);
    if (mediumAnalysisList == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    UserAccount userAccount = entityManager.find(UserAccount.class, userAccountId);
    if (userAccount == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    PermissionType permissionType = entityManager.find(PermissionType.class, permissionTypeId);
    if (permissionType == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    UserAccountHasMediumAnalysisList uahmal = null;
    try {
      Query countQuery = entityManager.createQuery(
                                              "SELECT COUNT(uahmal) FROM UserAccountHasMediumAnalysisList uahmal WHERE uahmal.mediumAnalysisList=:mediumAnalysisList AND uahmal.userAccount=:userAccount")
                                      .setParameter("mediumAnalysisList", mediumAnalysisList)
                                      .setParameter("userAccount", userAccount);
      // .setParameter("permissionType", permissionType);
      long recordsTotal = (long) countQuery.getSingleResult();
      if (recordsTotal >= 1) {
        return Response.status(Status.FORBIDDEN).build();  // an entry already exists, do not create a new one
        // uahmal = (UserAccountHasMediumAnalysisList) entityManager.createQuery(
        // 	"SELECT uahmal FROM UserAccountHasMediumAnalysisList uahmal WHERE uahmal.mediumAnalysisList=:mediumAnalysisList AND uahmal.userAccount=:userAccount")
        // 		.setParameter("mediumAnalysisList", mediumAnalysisList)
        // 		.setParameter("userAccount", userAccount)
        // 		// .setParameter("permissionType", permissionType)
        // 		.getSingleResult();
      }
    } catch (Exception e) {
      e.printStackTrace();
      // doesn't matter
    }
    if (uahmal == null) {
      // System.out.println("EndpointMediumAnalysisList: UserAccountHasMediumAnalysisList - create new entry");
      uahmal = new UserAccountHasMediumAnalysisList();
      uahmal.setMediumAnalysisList(mediumAnalysisList);
      uahmal.setUserAccount(userAccount);
      uahmal.setPermissionType(permissionType);
      try {
        EntityTransaction entityTransaction = entityManager.getTransaction();
        entityTransaction.begin();
        entityManager.persist(uahmal);
        entityTransaction.commit();
        entityManager.refresh(mediumAnalysisList);
        entityManager.refresh(userAccount);
        entityManager.refresh(permissionType);
        entityManager.refresh(uahmal);
      } catch (Exception e) {
        e.printStackTrace();
        return Response.notModified().build();
      }
    }

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISLISTEDITED);

    return Response.ok().entity(uahmal).build();
  }

  @PATCH
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{analysisListId}/userAccount/{userAccountId}/withPermission/{permissionId}")
  @Secured
  public Response updateUserAccountHasMediumAnalysisListWithPermission(@PathParam("analysisListId") int mediumAnalysisListId, @PathParam("userAccountId") int userAccountId, @PathParam("permissionId") int permissionTypeId, @QueryParam("authToken") String authToken) {
    // System.out.println("EndpointMediumAnalysisList: updateUserAccountHasMediumAnalysisListWithPermission");

    // verify auth token
    int userId = 0;
    if (AuthenticationFilter.isTokenValid(authToken)) {
      userId = AuthenticationFilter.getTokenClaimUserId(authToken);
    }
    else {
      return Response.status(Status.UNAUTHORIZED).build();
    }
    // check for permission level
    // only users with administrate permission level may add users with moderate or administrate permission
    int permissionLevel = EndpointUserAccount.getPermissionLevelForAnalysisList(userId, mediumAnalysisListId);
    // IF user is not sys admin AND (
    // user is without high enough permission level to set others to moderate or administrate
    // OR user is without high enough permission level to set any permission levels)
    if ((userId != 1) && ((permissionLevel != 4 && (permissionTypeId == 3 || permissionTypeId == 4)) || (permissionLevel != 3 && permissionLevel != 4))) {
      return Response.status(Status.FORBIDDEN).build();
    } // else user has permission for requested change

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumAnalysisList mediumAnalysisList = entityManager.find(MediumAnalysisList.class, mediumAnalysisListId);
    if (mediumAnalysisList == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    UserAccount userAccount = entityManager.find(UserAccount.class, userAccountId);
    if (userAccount == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    PermissionType permissionType = entityManager.find(PermissionType.class, permissionTypeId);
    if (permissionType == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    UserAccountHasMediumAnalysisList uahmalKey = new UserAccountHasMediumAnalysisList(userAccount, mediumAnalysisList);
    UserAccountHasMediumAnalysisList uahmal = entityManager.find(UserAccountHasMediumAnalysisList.class,
            uahmalKey.getId());

    uahmal.setPermissionType(permissionType);

    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(uahmal);
    entityManager.persist(uahmal);
    entityTransaction.commit();
    entityManager.refresh(uahmal);


    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISLISTEDITED);

    return Response.ok().entity(uahmal).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{analysisListId}/userAccount/{userAccountId}")
  @Secured
  public Response deleteUserAccountHasMediumAnalysisListWithPermission(@PathParam("analysisListId") int mediumAnalysisListId, @PathParam("userAccountId") int userAccountId, @QueryParam("authToken") String authToken) {
    // verify auth token
    int userId = 0;
    if (AuthenticationFilter.isTokenValid(authToken)) {
      userId = AuthenticationFilter.getTokenClaimUserId(authToken);
    }
    else {
      return Response.status(Status.UNAUTHORIZED).build();
    }

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    UserAccountHasMediumAnalysisList userToBeRemoved = null;
    try {
      userToBeRemoved = (UserAccountHasMediumAnalysisList) entityManager.createQuery(
                                                                                "SELECT uahmal FROM UserAccountHasMediumAnalysisList uahmal WHERE uahmal.mediumAnalysisList.id=:mediumAnalysisListId AND uahmal.userAccount.id=:userAccountId")
                                                                        .setParameter("mediumAnalysisListId",
                                                                                mediumAnalysisListId)
                                                                        .setParameter("userAccountId", userAccountId)
                                                                        .getSingleResult();
    } catch (NoResultException nre) {
      nre.printStackTrace();
      return Response.status(Status.NOT_FOUND).build();
    }
    // check for permission level
    // only users with administrate permission level may add users with moderate or administrate permission
    int permissionLevel = EndpointUserAccount.getPermissionLevelForAnalysisList(userId, mediumAnalysisListId);
    // IF user is not sys admin AND (
    // user is without high enough permission level to set others to moderate or administrate
    // OR user is without high enough permission level to set any permission levels)
    if ((userId != 1) && ((permissionLevel != 4 && (userToBeRemoved.getPermissionType()
                                                                   .getId() == 3 || userToBeRemoved.getPermissionType()
                                                                                                   .getId() == 4)) || (permissionLevel != 3 && permissionLevel != 4))) {
      return Response.status(Status.FORBIDDEN).build();
    } // else user has permission for requested change

    MediumAnalysisList mediumAnalysisList = entityManager.find(MediumAnalysisList.class, mediumAnalysisListId);
    if (mediumAnalysisList == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    UserAccount userAccount = entityManager.find(UserAccount.class, userAccountId);
    if (userAccount == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    UserAccountHasMediumAnalysisList uahmalKey = new UserAccountHasMediumAnalysisList(userAccount, mediumAnalysisList);
    UserAccountHasMediumAnalysisList uahmal = entityManager.find(UserAccountHasMediumAnalysisList.class,
            uahmalKey.getId());

    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(uahmal);
    entityTransaction.commit();
    entityManager.refresh(mediumAnalysisList);
    entityManager.refresh(userAccount);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISLISTEDITED);

    return Response.ok().build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{analysisListId}/segment")
  public Response createAnalysisSegment(@PathParam("analysisListId") int mediumAnalysisListId, String jsonData, @QueryParam("authToken") String authToken) {

    // System.out.println("createAnalysisSegment - jsonData: "+ jsonData);
    // verify auth token
    int userId = 0;
    if (AuthenticationFilter.isTokenValid(authToken)) {
      userId = AuthenticationFilter.getTokenClaimUserId(authToken);
    }
    else {
      return Response.status(Status.UNAUTHORIZED).build();
    }
    // check for permission level
    if (EndpointUserAccount.getPermissionLevelForAnalysisList(userId, mediumAnalysisListId) < 2) {
      return Response.status(Status.FORBIDDEN).build();
    }

    ObjectMapper mapper = new ObjectMapper();
    AnalysisSegment analysisSegment = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumAnalysisList mediumAnalysisList = entityManager.find(MediumAnalysisList.class, mediumAnalysisListId);
    if (mediumAnalysisList == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    // parse JSON data
    try {
      analysisSegment = mapper.readValue(jsonData, AnalysisSegment.class);
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (analysisSegment == null) {
      return Response.status(Status.BAD_REQUEST).build();
    }

    // sanitize object data
    analysisSegment.setId(0);
    mediumAnalysisList.addAnalysisSegment(analysisSegment);
    analysisSegment.getAnalysisSegmentTranslations().get(0).setId(0);
    analysisSegment.getAnalysisSegmentTranslations().get(0).setAnalysisSegment(analysisSegment);
    analysisSegment.getAnalysisSegmentTranslations().get(0).setLanguage(entityManager.find(Language.class, 1));

    // persist analysisSegment and list
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(analysisSegment.getAnalysisSegmentTranslations().get(0));
    entityManager.persist(analysisSegment);
    entityManager.flush();
    analysisSegment.setMediumAnalysisList(mediumAnalysisList);
    entityManager.persist(mediumAnalysisList);
    entityTransaction.commit();
    entityManager.refresh(analysisSegment);
    entityManager.refresh(mediumAnalysisList);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISSEGMENTCREATED);

    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "addSegment", analysisSegment.getMediumAnalysisList().getId(), analysisSegment);

    return Response.ok().entity(analysisSegment).build();
  }

  @PATCH
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("segment/{id}")
  @Secured
  public Response updateAnalysisSegment(@PathParam("id") int segmentId, String jsonData, @QueryParam("authToken") String authToken) {
    // System.out.println("EndpointAnalysisList: updateAnalysisSegment "+ jsonData);
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisSegment analysisSegment = entityManager.find(AnalysisSegment.class, segmentId);
    if (analysisSegment == null) {
      return Response.status(Status.NOT_FOUND).build();
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
    if (EndpointUserAccount.getPermissionLevelForAnalysisList(userId,
            analysisSegment.getMediumAnalysisList().getId()) < 2) {
      return Response.status(Status.FORBIDDEN).build();
    }

    ObjectMapper mapper = new ObjectMapper();
    AnalysisSegment updatedSegment = null;

    // parse JSON data
    try {
      updatedSegment = mapper.readValue(jsonData, AnalysisSegment.class);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedSegment == null) {
      return Response.notModified().build();
    }

    // update segment
    if (updatedSegment.getAnalysisSegmentTranslations().get(0).getName() != null) {
      analysisSegment.getAnalysisSegmentTranslations().get(0)
                     .setName(updatedSegment.getAnalysisSegmentTranslations().get(0).getName());
    }
    if (updatedSegment.getAnalysisSegmentTranslations().get(0).getShortDescription() != null) {
      analysisSegment.getAnalysisSegmentTranslations().get(0)
                     .setShortDescription(updatedSegment.getAnalysisSegmentTranslations().get(0).getShortDescription());
    }
    if (updatedSegment.getAnalysisSegmentTranslations().get(0).getComment() != null) {
      analysisSegment.getAnalysisSegmentTranslations().get(0)
                     .setComment(updatedSegment.getAnalysisSegmentTranslations().get(0).getComment());
    }
    if (updatedSegment.getAnalysisSegmentTranslations().get(0).getTranscript() != null) {
      analysisSegment.getAnalysisSegmentTranslations().get(0)
                     .setTranscript(updatedSegment.getAnalysisSegmentTranslations().get(0).getTranscript());
    }
    analysisSegment.setStartTime(updatedSegment.getStartTime());
    analysisSegment.setEndTime(updatedSegment.getEndTime());
    analysisSegment.setCategories(updatedSegment.getCategories());

    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(analysisSegment);
    entityManager.persist(analysisSegment);
    entityTransaction.commit();
    entityManager.refresh(analysisSegment);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISSEGMENTEDITED);
    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "editSegment", analysisSegment.getMediumAnalysisList().getId(), analysisSegment);

    return Response.ok().entity(analysisSegment).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("segment/{id}")
  @Secured
  public Response deleteAnalysisSegment(@PathParam("id") int segmentId, @QueryParam("authToken") String authToken) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisSegment analysisSegment = entityManager.find(AnalysisSegment.class, segmentId);
    if (analysisSegment == null) {
      return Response.status(Status.NOT_FOUND).build();
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
    if (EndpointUserAccount.getPermissionLevelForAnalysisList(userId,
            analysisSegment.getMediumAnalysisList().getId()) < 2) {
      return Response.status(Status.FORBIDDEN).build();
    }

    MediumAnalysisList mediumAnalysisList = analysisSegment.getMediumAnalysisList();
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(analysisSegment);
    entityTransaction.commit();
    entityManager.refresh(mediumAnalysisList);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISSEGMENTDELETED);

    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "removeSegment", mediumAnalysisList.getId(), analysisSegment);

    return Response.ok().build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{segmentId}/sequence")
  public Response createAnalysisSequence(@PathParam("segmentId") int segmentId, String jsonData, @QueryParam("authToken") String authToken) {
    // System.out.println("EndpointAnalysisList: createAnalysisSequence - jsonData: "+ jsonData);

    ObjectMapper mapper = new ObjectMapper();
    AnalysisSequence analysisSequence = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisSegment analysisSegment = entityManager.find(AnalysisSegment.class, segmentId);
    if (analysisSegment == null) {
      return Response.status(Status.NOT_FOUND).build();
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
    if (EndpointUserAccount.getPermissionLevelForAnalysisList(userId,
            analysisSegment.getMediumAnalysisList().getId()) < 2) {
      return Response.status(Status.FORBIDDEN).build();
    }

    // parse JSON data
    try {
      analysisSequence = mapper.readValue(jsonData, AnalysisSequence.class);
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (analysisSequence == null) {
      return Response.status(Status.BAD_REQUEST).build();
    }

    // sanitize object data
    analysisSequence.setId(0);
    analysisSegment.addAnalysisSequence(analysisSequence);
    analysisSequence.getAnalysisSequenceTranslations().get(0).setId(0);
    analysisSequence.getAnalysisSequenceTranslations().get(0).setAnalysisSequence(analysisSequence);
    analysisSequence.getAnalysisSequenceTranslations().get(0).setLanguage(entityManager.find(Language.class, 1));


    // persist analysisSequence and list
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(analysisSequence);
    entityManager.flush();
    analysisSequence.setAnalysisSegment(analysisSegment);
    entityManager.persist(analysisSegment);
    entityTransaction.commit();
    entityManager.refresh(analysisSequence);
    entityManager.refresh(analysisSegment);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISSEQUENCECREATED);

    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "add-sequence", analysisSequence.getAnalysisSegment().getId(), analysisSequence);

    return Response.ok().entity(analysisSequence).build();
  }

  @PATCH
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("sequence/{id}")
  @Secured
  public Response updateAnalysisSequence(@PathParam("id") int sequenceId, String jsonData, @QueryParam("authToken") String authToken) {
    // System.out.println("EndpointAnalysisList: updateAnalysisSegment "+ jsonData);

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisSequence analysisSequence = entityManager.find(AnalysisSequence.class, sequenceId);
    if (analysisSequence == null) {
      return Response.status(Status.NOT_FOUND).build();
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
    if (EndpointUserAccount.getPermissionLevelForAnalysisList(userId,
            analysisSequence.getAnalysisSegment().getMediumAnalysisList().getId()) < 2) {
      return Response.status(Status.FORBIDDEN).build();
    }

    ObjectMapper mapper = new ObjectMapper();
    AnalysisSequence updatedSequence = null;

    // parse JSON data
    try {
      updatedSequence = mapper.readValue(jsonData, AnalysisSequence.class);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedSequence == null) {
      return Response.notModified().build();
    }

    // update sequence
    analysisSequence.setStartTime(updatedSequence.getStartTime());
    analysisSequence.setEndTime(updatedSequence.getEndTime());
    analysisSequence.setCategories(updatedSequence.getCategories());

    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(analysisSequence);
    entityManager.persist(analysisSequence);
    entityTransaction.commit();
    entityManager.refresh(analysisSequence);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISSEQUENCEEDITED);
    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "edit-sequence", analysisSequence.getAnalysisSegment().getId(), analysisSequence);

    return Response.ok().entity(analysisSequence).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("sequence/{id}")
  @Secured
  public Response deleteAnalysisSequence(@PathParam("id") int id, @QueryParam("authToken") String authToken) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisSequence analysisSequence = entityManager.find(AnalysisSequence.class, id);
    if (analysisSequence == null) {
      return Response.status(Status.NOT_FOUND).build();
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
    if (EndpointUserAccount.getPermissionLevelForAnalysisList(userId,
            analysisSequence.getAnalysisSegment().getMediumAnalysisList().getId()) < 2) {
      return Response.status(Status.FORBIDDEN).build();
    }

    AnalysisSegment analysisSegment = analysisSequence.getAnalysisSegment();
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(analysisSequence);
    entityTransaction.commit();
    entityManager.refresh(analysisSegment);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISSEQUENCEDELETED);

    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "remove-sequence", analysisSegment.getId(), analysisSequence);

    return Response.ok().build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Secured
  @Path("sequence/{sequenceId}/translation")
  public Response createAnalysisSequenceTranslation(@PathParam("sequenceId") int sequenceId, String jsonData) {
    ObjectMapper mapper = new ObjectMapper();
    AnalysisSequenceTranslation newTranslation = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisSequence analysisSequence = entityManager.find(AnalysisSequence.class, sequenceId);

    if (analysisSequence == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      newTranslation = mapper.readValue(jsonData, AnalysisSequenceTranslation.class);
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (newTranslation == null) {
      return Response.status(Status.BAD_REQUEST).build();
    }

    // sanitize object data
    newTranslation.setId(0);
    newTranslation.setAnalysisSequence(analysisSequence);
    Language language = entityManager.find(Language.class, newTranslation.getLanguage().getId());
    newTranslation.setLanguage(language);
    analysisSequence.addAnalysisSequenceTranslation(newTranslation);

    // persist analysisSequence and list
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(newTranslation);
    entityManager.flush();
    newTranslation.setAnalysisSequence(analysisSequence);
    entityManager.persist(analysisSequence);
    entityTransaction.commit();
    entityManager.refresh(newTranslation);
    entityManager.refresh(analysisSequence);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISSEQUENCECREATED);

    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "add-sequence-translation", newTranslation.getAnalysisSequence().getId(), newTranslation);

    return Response.ok().entity(newTranslation).build();
  }

  @PATCH
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("sequence/translation/{translationId}")
  @Secured
  public Response updateAnalysisSequenceTranslation(@PathParam("translationId") int translationId, String jsonData) {
    // System.out.println("updateAnalysisSequenceTranslation - jsonData: "+ jsonData);
    ObjectMapper mapper = new ObjectMapper();
    AnalysisSequenceTranslation updatedTranslation = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisSequenceTranslation analysisSequenceTranslation = entityManager.find(AnalysisSequenceTranslation.class,
            translationId);

    if (analysisSequenceTranslation == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      updatedTranslation = mapper.readValue(jsonData, AnalysisSequenceTranslation.class);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedTranslation == null) {
      return Response.notModified().build();
    }

    // update sequence translation
    if (updatedTranslation.getName() != null) {
      analysisSequenceTranslation.setName(updatedTranslation.getName());
    }
    analysisSequenceTranslation.setShortDescription(updatedTranslation.getShortDescription());
    analysisSequenceTranslation.setComment(updatedTranslation.getComment());
    analysisSequenceTranslation.setTranscript(updatedTranslation.getTranscript());

    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(analysisSequenceTranslation);
    entityManager.persist(analysisSequenceTranslation);
    entityTransaction.commit();
    entityManager.refresh(analysisSequenceTranslation);

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"), UserLogManager.LogEvents.ANALYSISSEQUENCEEDITED);

    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "edit-sequence", analysisSequenceTranslation.getAnalysisSequence().getId(), analysisSequenceTranslation);

    return Response.ok().entity(analysisSequenceTranslation).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("sequence/translation/{translationId}")
  @Secured
  public Response deleteAnalysisSequenceTranslation(@PathParam("translationId") int translationId, String jsonData) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisSequenceTranslation analysisSequenceTranslation = entityManager.find(AnalysisSequenceTranslation.class,
            translationId);

    if (analysisSequenceTranslation == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    AnalysisSequence analysisSequence = analysisSequenceTranslation.getAnalysisSequence();
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(analysisSequenceTranslation);
    entityTransaction.commit();
    entityManager.refresh(analysisSequence);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISSEQUENCEDELETED);

    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "remove-sequence", analysisSequence.getId(), analysisSequenceTranslation);

    return Response.ok().build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{segmentId}/scene")
  public Response createAnalysisScene(@PathParam("segmentId") int segmentId, String jsonData, @QueryParam("authToken") String authToken) {
    ObjectMapper mapper = new ObjectMapper();
    AnalysisScene newScene = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisSegment analysisSegment = entityManager.find(AnalysisSegment.class, segmentId);
    if (analysisSegment == null) {
      return Response.status(Status.NOT_FOUND).build();
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
    if (EndpointUserAccount.getPermissionLevelForAnalysisList(userId,
            analysisSegment.getMediumAnalysisList().getId()) < 2) {
      return Response.status(Status.FORBIDDEN).build();
    }

    // parse JSON data
    try {
      newScene = mapper.readValue(jsonData, AnalysisScene.class);
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (newScene == null) {
      return Response.status(Status.BAD_REQUEST).build();
    }

    // sanitize object data
    newScene.setId(0);
    analysisSegment.addAnalysisScene(newScene);
    newScene.getAnalysisSceneTranslations().get(0).setId(0);
    newScene.getAnalysisSceneTranslations().get(0).setAnalysisScene(newScene);
    newScene.getAnalysisSceneTranslations().get(0).setLanguage(entityManager.find(Language.class, 1));

    // persist analysisScene and list
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(newScene);
    entityManager.flush();
    newScene.setAnalysisSegment(analysisSegment);
    entityManager.persist(analysisSegment);
    entityTransaction.commit();
    entityManager.refresh(newScene);
    entityManager.refresh(analysisSegment);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISSCENECREATED);

    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"), "add-scene",
            newScene.getAnalysisSegment().getId(), newScene);

    return Response.ok().entity(newScene).build();
  }

  @PATCH
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("scene/{id}")
  @Secured
  public Response updateAnalysisScene(@PathParam("id") int id, String jsonData, @QueryParam("authToken") String authToken) {
    // System.out.println("EndpointAnalysisList: updateAnalysisScene "+ jsonData);

    ObjectMapper mapper = new ObjectMapper();
    AnalysisScene updatedScene = null;

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisScene analysisScene = entityManager.find(AnalysisScene.class, id);
    if (analysisScene == null) {
      return Response.status(Status.NOT_FOUND).build();
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
    if (EndpointUserAccount.getPermissionLevelForAnalysisList(userId,
            analysisScene.getAnalysisSegment().getMediumAnalysisList().getId()) < 2) {
      return Response.status(Status.FORBIDDEN).build();
    }

    // parse JSON data
    try {
      updatedScene = mapper.readValue(jsonData, AnalysisScene.class);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedScene == null) {
      return Response.notModified().build();
    }

    // update scene
    analysisScene.setStartTime(updatedScene.getStartTime());
    analysisScene.setEndTime(updatedScene.getEndTime());
    analysisScene.setCategories(updatedScene.getCategories());

    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(analysisScene);
    entityManager.persist(analysisScene);
    entityTransaction.commit();
    entityManager.refresh(analysisScene);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISSCENEEDITED);
    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "edit-scene", analysisScene.getAnalysisSegment().getId(), analysisScene);

    return Response.ok().entity(analysisScene).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("scene/{id}")
  @Secured
  public Response deleteAnalysisScene(@PathParam("id") int id, @QueryParam("authToken") String authToken) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisScene analysisScene = entityManager.find(AnalysisScene.class, id);
    if (analysisScene == null) {
      return Response.status(Status.NOT_FOUND).build();
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
    if (EndpointUserAccount.getPermissionLevelForAnalysisList(userId,
            analysisScene.getAnalysisSegment().getMediumAnalysisList().getId()) < 2) {
      return Response.status(Status.FORBIDDEN).build();
    }

    AnalysisSegment analysisSegment = analysisScene.getAnalysisSegment();
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(analysisScene);
    entityTransaction.commit();
    entityManager.refresh(analysisSegment);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISSCENEDELETED);

    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "remove-scene", analysisSegment.getId(), analysisScene);

    return Response.ok().build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Secured
  @Path("scene/{sceneId}/translation")
  public Response createAnalysisSceneTranslation(@PathParam("sceneId") int sceneId, String jsonData) {
    ObjectMapper mapper = new ObjectMapper();
    AnalysisSceneTranslation newTranslation = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisScene analysisScene = entityManager.find(AnalysisScene.class, sceneId);

    if (analysisScene == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      newTranslation = mapper.readValue(jsonData, AnalysisSceneTranslation.class);
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (newTranslation == null) {
      return Response.status(Status.BAD_REQUEST).build();
    }

    // sanitize object data
    newTranslation.setId(0);
    newTranslation.setAnalysisScene(analysisScene);
    Language language = entityManager.find(Language.class, newTranslation.getLanguage().getId());
    newTranslation.setLanguage(language);
    analysisScene.addAnalysisSceneTranslation(newTranslation);

    // persist analysisScene and list
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(newTranslation);
    entityManager.flush();
    newTranslation.setAnalysisScene(analysisScene);
    entityManager.persist(analysisScene);
    entityTransaction.commit();
    entityManager.refresh(newTranslation);
    entityManager.refresh(analysisScene);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISSCENECREATED);

    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "add-scene-translation", newTranslation.getAnalysisScene().getId(), newTranslation);

    return Response.ok().entity(newTranslation).build();
  }

  @PATCH
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("scene/translation/{translationId}")
  @Secured
  public Response updateAnalysisSceneTranslation(@PathParam("translationId") int translationId, String jsonData) {
    // System.out.println("updateAnalysisSceneTranslation - jsonData: "+ jsonData);
    ObjectMapper mapper = new ObjectMapper();
    AnalysisSceneTranslation updatedTranslation = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisSceneTranslation analysisSceneTranslation = entityManager.find(AnalysisSceneTranslation.class,
            translationId);

    if (analysisSceneTranslation == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      updatedTranslation = mapper.readValue(jsonData, AnalysisSceneTranslation.class);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedTranslation == null) {
      return Response.notModified().build();
    }

    // update scene translation
    if (updatedTranslation.getName() != null) {
      analysisSceneTranslation.setName(updatedTranslation.getName());
    }
    analysisSceneTranslation.setShortDescription(updatedTranslation.getShortDescription());
    analysisSceneTranslation.setComment(updatedTranslation.getComment());
    analysisSceneTranslation.setTranscript(updatedTranslation.getTranscript());

    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(analysisSceneTranslation);
    entityManager.persist(analysisSceneTranslation);
    entityTransaction.commit();
    entityManager.refresh(analysisSceneTranslation);

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"), UserLogManager.LogEvents.ANALYSISSCENEEDITED);

    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "edit-scene", analysisSceneTranslation.getAnalysisScene().getId(), analysisSceneTranslation);

    return Response.ok().entity(analysisSceneTranslation).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("scene/translation/{translationId}")
  @Secured
  public Response deleteAnalysisSceneTranslation(@PathParam("translationId") int translationId) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisSceneTranslation analysisSceneTranslation = entityManager.find(AnalysisSceneTranslation.class,
            translationId);

    if (analysisSceneTranslation == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    AnalysisScene analysisScene = analysisSceneTranslation.getAnalysisScene();
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(analysisSceneTranslation);
    entityTransaction.commit();
    entityManager.refresh(analysisScene);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISSCENEDELETED);

    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "remove-scene", analysisScene.getId(), analysisSceneTranslation);

    return Response.ok().build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{sequenceId}/take")
  public Response createAnalysisTake(@PathParam("sequenceId") int sequenceId, String jsonData, @QueryParam("authToken") String authToken) {
    ObjectMapper mapper = new ObjectMapper();
    AnalysisTake newTake = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisSequence analysisSequence = entityManager.find(AnalysisSequence.class, sequenceId);
    if (analysisSequence == null) {
      return Response.status(Status.NOT_FOUND).build();
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
    if (EndpointUserAccount.getPermissionLevelForAnalysisList(userId,
            analysisSequence.getAnalysisSegment().getMediumAnalysisList().getId()) < 2) {
      return Response.status(Status.FORBIDDEN).build();
    }

    // parse JSON data
    try {
      newTake = mapper.readValue(jsonData, AnalysisTake.class);
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (newTake == null) {
      return Response.status(Status.BAD_REQUEST).build();
    }

    // sanitize object data
    newTake.setId(0);
    analysisSequence.addAnalysisTake(newTake);
    newTake.getAnalysisTakeTranslations().get(0).setId(0);
    newTake.getAnalysisTakeTranslations().get(0).setAnalysisTake(newTake);
    newTake.getAnalysisTakeTranslations().get(0).setLanguage(entityManager.find(Language.class, 1));


    // persist analysisTake and list
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(newTake);
    entityManager.flush();
    newTake.setAnalysisSequence(analysisSequence);
    entityManager.persist(analysisSequence);
    entityTransaction.commit();
    entityManager.refresh(newTake);
    entityManager.refresh(analysisSequence);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISTAKECREATED);

    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"), "add-take",
            newTake.getAnalysisSequence().getId(), newTake);

    return Response.ok().entity(newTake).build();
  }

  @PATCH
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("take/{id}")
  @Secured
  public Response updateAnalysisTake(@PathParam("id") int id, String jsonData, @QueryParam("authToken") String authToken) {
    // System.out.println("EndpointAnalysisList: updateAnalysisTake "+ jsonData);
    ObjectMapper mapper = new ObjectMapper();
    AnalysisTake updatedTake = null;

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisTake analysisTake = entityManager.find(AnalysisTake.class, id);
    if (analysisTake == null) {
      return Response.status(Status.NOT_FOUND).build();
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
    if (EndpointUserAccount.getPermissionLevelForAnalysisList(userId,
            analysisTake.getAnalysisSequence().getAnalysisSegment().getMediumAnalysisList().getId()) < 2) {
      return Response.status(Status.FORBIDDEN).build();
    }

    // parse JSON data
    try {
      updatedTake = mapper.readValue(jsonData, AnalysisTake.class);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedTake == null) {
      return Response.notModified().build();
    }

    // update take
    analysisTake.setStartTime(updatedTake.getStartTime());
    analysisTake.setEndTime(updatedTake.getEndTime());
    analysisTake.setCategories(updatedTake.getCategories());

    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(analysisTake);
    entityManager.persist(analysisTake);
    entityTransaction.commit();
    entityManager.refresh(analysisTake);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISTAKEEDITED);
    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"), "edit-take",
            analysisTake.getAnalysisSequence().getId(), analysisTake);

    return Response.ok().entity(analysisTake).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("take/{id}")
  @Secured
  public Response deleteAnalysisTake(@PathParam("id") int id, @QueryParam("authToken") String authToken) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisTake analysisTake = entityManager.find(AnalysisTake.class, id);
    if (analysisTake == null) {
      return Response.status(Status.NOT_FOUND).build();
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
    if (EndpointUserAccount.getPermissionLevelForAnalysisList(userId,
            analysisTake.getAnalysisSequence().getAnalysisSegment().getMediumAnalysisList().getId()) < 2) {
      return Response.status(Status.FORBIDDEN).build();
    }

    AnalysisSequence analysisSequence = analysisTake.getAnalysisSequence();
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(analysisTake);
    entityTransaction.commit();
    entityManager.refresh(analysisSequence);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISTAKEDELETED);

    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "remove-take", analysisSequence.getId(), analysisTake);

    return Response.ok().build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Secured
  @Path("take/{takeId}/translation")
  public Response createAnalysisTakeTranslation(@PathParam("takeId") int takeId, String jsonData) {
    ObjectMapper mapper = new ObjectMapper();
    AnalysisTakeTranslation newTranslation = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisTake analysisTake = entityManager.find(AnalysisTake.class, takeId);

    if (analysisTake == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      newTranslation = mapper.readValue(jsonData, AnalysisTakeTranslation.class);
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (newTranslation == null) {
      return Response.status(Status.BAD_REQUEST).build();
    }

    // sanitize object data
    newTranslation.setId(0);
    newTranslation.setAnalysisTake(analysisTake);
    Language language = entityManager.find(Language.class, newTranslation.getLanguage().getId());
    newTranslation.setLanguage(language);
    analysisTake.addAnalysisTakeTranslation(newTranslation);

    // persist analysisTake and list
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(newTranslation);
    entityManager.flush();
    newTranslation.setAnalysisTake(analysisTake);
    entityManager.persist(analysisTake);
    entityTransaction.commit();
    entityManager.refresh(newTranslation);
    entityManager.refresh(analysisTake);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISTAKECREATED);

    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "add-take-translation", newTranslation.getAnalysisTake().getId(), newTranslation);

    return Response.ok().entity(newTranslation).build();
  }

  @PATCH
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("take/translation/{translationId}")
  @Secured
  public Response updateAnalysisTakeTranslation(@PathParam("translationId") int translationId, String jsonData) {
    // System.out.println("updateAnalysisTakeTranslation - jsonData: "+ jsonData);
    ObjectMapper mapper = new ObjectMapper();
    AnalysisTakeTranslation updatedTranslation = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisTakeTranslation analysisTakeTranslation = entityManager.find(AnalysisTakeTranslation.class, translationId);

    if (analysisTakeTranslation == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      updatedTranslation = mapper.readValue(jsonData, AnalysisTakeTranslation.class);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedTranslation == null) {
      return Response.notModified().build();
    }

    // update take translation
    if (updatedTranslation.getName() != null) {
      analysisTakeTranslation.setName(updatedTranslation.getName());
    }
    analysisTakeTranslation.setShortDescription(updatedTranslation.getShortDescription());
    analysisTakeTranslation.setComment(updatedTranslation.getComment());
    analysisTakeTranslation.setTranscript(updatedTranslation.getTranscript());

    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(analysisTakeTranslation);
    entityManager.persist(analysisTakeTranslation);
    entityTransaction.commit();
    entityManager.refresh(analysisTakeTranslation);

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"), UserLogManager.LogEvents.ANALYSISTAKEEDITED);

    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"), "edit-take",
            analysisTakeTranslation.getAnalysisTake().getId(), analysisTakeTranslation);

    return Response.ok().entity(analysisTakeTranslation).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("take/translation/{translationId}")
  @Secured
  public Response deleteAnalysisTakeTranslation(@PathParam("translationId") int translationId) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisTakeTranslation analysisTakeTranslation = entityManager.find(AnalysisTakeTranslation.class, translationId);

    if (analysisTakeTranslation == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    AnalysisTake analysisTake = analysisTakeTranslation.getAnalysisTake();
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(analysisTakeTranslation);
    entityTransaction.commit();
    entityManager.refresh(analysisTake);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISTAKEDELETED);

    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "remove-take", analysisTake.getId(), analysisTakeTranslation);

    return Response.ok().build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{sceneId}/action")
  public Response createAnalysisAction(@PathParam("sceneId") int sceneId, String jsonData, @QueryParam("authToken") String authToken) {
    ObjectMapper mapper = new ObjectMapper();
    AnalysisAction newAction = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisScene analysisScene = entityManager.find(AnalysisScene.class, sceneId);
    if (analysisScene == null) {
      return Response.status(Status.NOT_FOUND).build();
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
    if (EndpointUserAccount.getPermissionLevelForAnalysisList(userId,
            analysisScene.getAnalysisSegment().getMediumAnalysisList().getId()) < 2) {
      return Response.status(Status.FORBIDDEN).build();
    }

    // parse JSON data
    try {
      newAction = mapper.readValue(jsonData, AnalysisAction.class);
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (newAction == null) {
      return Response.status(Status.BAD_REQUEST).build();
    }

    // sanitize object data
    newAction.setId(0);
    analysisScene.addAnalysisAction(newAction);
    newAction.getAnalysisActionTranslations().get(0).setId(0);
    newAction.getAnalysisActionTranslations().get(0).setAnalysisAction(newAction);
    newAction.getAnalysisActionTranslations().get(0).setLanguage(entityManager.find(Language.class, 1));

    // persist analysisAction and list
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(newAction);
    entityManager.flush();
    newAction.setAnalysisScene(analysisScene);
    entityManager.persist(analysisScene);
    entityTransaction.commit();
    entityManager.refresh(newAction);
    entityManager.refresh(analysisScene);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISACTIONCREATED);

    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "add-action", newAction.getAnalysisScene().getId(), newAction);

    return Response.ok().entity(newAction).build();
  }

  @PATCH
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("action/{id}")
  @Secured
  public Response updateAnalysisAction(@PathParam("id") int id, String jsonData, @QueryParam("authToken") String authToken) {
    // System.out.println("EndpointAnalysisList: updateAnalysisAction "+ jsonData);
    ObjectMapper mapper = new ObjectMapper();
    AnalysisAction updatedAction = null;

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisAction analysisAction = entityManager.find(AnalysisAction.class, id);
    if (analysisAction == null) {
      return Response.status(Status.NOT_FOUND).build();
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
    if (EndpointUserAccount.getPermissionLevelForAnalysisList(userId,
            analysisAction.getAnalysisScene().getAnalysisSegment().getMediumAnalysisList().getId()) < 2) {
      return Response.status(Status.FORBIDDEN).build();
    }

    // parse JSON data
    try {
      updatedAction = mapper.readValue(jsonData, AnalysisAction.class);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedAction == null) {
      return Response.notModified().build();
    }

    // update action
    analysisAction.setStartTime(updatedAction.getStartTime());
    analysisAction.setEndTime(updatedAction.getEndTime());
    analysisAction.setCategories(updatedAction.getCategories());

    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(analysisAction);
    entityManager.persist(analysisAction);
    entityTransaction.commit();
    entityManager.refresh(analysisAction);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISACTIONEDITED);
    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "edit-action", analysisAction.getAnalysisScene().getId(), analysisAction);

    return Response.ok().entity(analysisAction).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("action/{id}")
  @Secured
  public Response deleteAnalysisAction(@PathParam("id") int id, @QueryParam("authToken") String authToken) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisAction analysisAction = entityManager.find(AnalysisAction.class, id);
    if (analysisAction == null) {
      return Response.status(Status.NOT_FOUND).build();
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
    if (EndpointUserAccount.getPermissionLevelForAnalysisList(userId,
            analysisAction.getAnalysisScene().getAnalysisSegment().getMediumAnalysisList().getId()) < 2) {
      return Response.status(Status.FORBIDDEN).build();
    }

    AnalysisScene analysisScene = analysisAction.getAnalysisScene();
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(analysisAction);
    entityTransaction.commit();
    entityManager.refresh(analysisScene);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISACTIONDELETED);

    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "remove-action", analysisScene.getId(), analysisAction);

    return Response.ok().build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Secured
  @Path("action/{actionId}/translation")
  public Response createAnalysisActionTranslation(@PathParam("actionId") int actionId, String jsonData) {
    ObjectMapper mapper = new ObjectMapper();
    AnalysisActionTranslation newTranslation = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisAction analysisAction = entityManager.find(AnalysisAction.class, actionId);

    if (analysisAction == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      newTranslation = mapper.readValue(jsonData, AnalysisActionTranslation.class);
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (newTranslation == null) {
      return Response.status(Status.BAD_REQUEST).build();
    }

    // sanitize object data
    newTranslation.setId(0);
    newTranslation.setAnalysisAction(analysisAction);
    Language language = entityManager.find(Language.class, newTranslation.getLanguage().getId());
    newTranslation.setLanguage(language);
    analysisAction.addAnalysisActionTranslation(newTranslation);

    // persist analysisAction and list
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(newTranslation);
    entityManager.flush();
    newTranslation.setAnalysisAction(analysisAction);
    entityManager.persist(analysisAction);
    entityTransaction.commit();
    entityManager.refresh(newTranslation);
    entityManager.refresh(analysisAction);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISACTIONCREATED);

    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "add-action-translation", newTranslation.getAnalysisAction().getId(), newTranslation);

    return Response.ok().entity(newTranslation).build();
  }

  @PATCH
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("action/translation/{translationId}")
  @Secured
  public Response updateAnalysisActionTranslation(@PathParam("translationId") int translationId, String jsonData) {
    // System.out.println("updateAnalysisActionTranslation - jsonData: "+ jsonData);
    ObjectMapper mapper = new ObjectMapper();
    AnalysisActionTranslation updatedTranslation = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisActionTranslation analysisActionTranslation = entityManager.find(AnalysisActionTranslation.class,
            translationId);

    if (analysisActionTranslation == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      updatedTranslation = mapper.readValue(jsonData, AnalysisActionTranslation.class);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedTranslation == null) {
      return Response.notModified().build();
    }

    // update action translation
    if (updatedTranslation.getName() != null) {
      analysisActionTranslation.setName(updatedTranslation.getName());
    }
    analysisActionTranslation.setShortDescription(updatedTranslation.getShortDescription());
    analysisActionTranslation.setComment(updatedTranslation.getComment());
    analysisActionTranslation.setTranscript(updatedTranslation.getTranscript());

    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(analysisActionTranslation);
    entityManager.persist(analysisActionTranslation);
    entityTransaction.commit();
    entityManager.refresh(analysisActionTranslation);

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"), UserLogManager.LogEvents.ANALYSISACTIONEDITED);

    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "edit-action", analysisActionTranslation.getAnalysisAction().getId(), analysisActionTranslation);

    return Response.ok().entity(analysisActionTranslation).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("action/translation/{translationId}")
  @Secured
  public Response deleteAnalysisActionTranslation(@PathParam("translationId") int translationId) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AnalysisActionTranslation analysisActionTranslation = entityManager.find(AnalysisActionTranslation.class,
            translationId);

    if (analysisActionTranslation == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    AnalysisAction analysisAction = analysisActionTranslation.getAnalysisAction();
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(analysisActionTranslation);
    entityTransaction.commit();
    entityManager.refresh(analysisAction);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.ANALYSISACTIONDELETED);

    // send notification action
    NotificationWebSocket.notifyUserAction((String) containerRequestContext.getProperty("TIMAAT.userName"),
            "remove-action", analysisAction.getId(), analysisActionTranslation);

    return Response.ok().build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{analysisListId}/categorySet/{categorySetId}")
  @Secured
  public Response addExistingCategorySet(@PathParam("analysisListId") int mediumAnalysisListId, @PathParam("categorySetId") int categorySetId, @QueryParam("authToken") String authToken) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumAnalysisList mediumAnalysisList = entityManager.find(MediumAnalysisList.class, mediumAnalysisListId);
    if (mediumAnalysisList == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    CategorySet categorySet = entityManager.find(CategorySet.class, categorySetId);
    if (categorySet == null) {
      return Response.status(Status.NOT_FOUND).build();
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
    if (EndpointUserAccount.getPermissionLevelForAnalysisList(userId, mediumAnalysisListId) < 2) {
      return Response.status(Status.FORBIDDEN).build();
    }

    // attach categorySet to annotation and vice versa
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    mediumAnalysisList.getRestrictedCategorySets().add(categorySet);
    categorySet.getMediumAnalysisLists().add(mediumAnalysisList);
    entityManager.merge(categorySet);
    entityManager.merge(mediumAnalysisList);
    entityManager.persist(mediumAnalysisList);
    entityManager.persist(categorySet);
    entityTransaction.commit();
    entityManager.refresh(mediumAnalysisList);

    return Response.ok().entity(categorySet).build();
  }

  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("{analysisListId}/categorySets")
  @Secured
  public List<CategorySet> updateCategorySets(@PathParam("analysisListId") int analysisListId, UpdateAnalysisListCategorySetsPayload updateAnalysisListCategorySetsPayload) throws DbTransactionExecutionException {
    verifyAuthorizationToAnalysisList(analysisListId,
            de.bitgilde.TIMAAT.rest.security.authorization.PermissionType.WRITE);
    return analysisListStorage.updateCategorySets(analysisListId,
            updateAnalysisListCategorySetsPayload.getCategorySetIds());
  }


  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{analysisListId}/categorySet/{categorySetId}")
  @Secured
  public Response removeCategorySet(@PathParam("analysisListId") int mediumAnalysisListId, @PathParam("categorySetId") int categorySetId, @QueryParam("authToken") String authToken) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumAnalysisList mediumAnalysisList = entityManager.find(MediumAnalysisList.class, mediumAnalysisListId);
    if (mediumAnalysisList == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    CategorySet categorySet = entityManager.find(CategorySet.class, categorySetId);
    if (categorySet == null) {
      return Response.status(Status.NOT_FOUND).build();
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
    if (EndpointUserAccount.getPermissionLevelForAnalysisList(userId, mediumAnalysisListId) < 2) {
      return Response.status(Status.FORBIDDEN).build();
    }

    // delete categories of matching categorySets from annotations and segment structure elements
    List<Category> categoryList = new ArrayList<>();
    Set<CategorySetHasCategory> cshc = categorySet.getCategorySetHasCategories();
    Iterator<CategorySetHasCategory> itr = cshc.iterator();
    EntityTransaction entityTransaction = entityManager.getTransaction();

    // fill categoryList will all categories of the category set that will be deleted
    while (itr.hasNext()) {
      categoryList.add(itr.next().getCategory());
    }

    // remove categories from removed category set from all associated annotations of the analysis list the category set is removed from
    for (Annotation annotation : mediumAnalysisList.getAnnotations()) {
      List<Category> annotationCategoryList = annotation.getCategories();
      List<Category> categoriesToRemove = categoryList.stream().distinct().filter(annotationCategoryList::contains)
                                                      .collect(Collectors.toList());
      entityTransaction.begin();
      for (Category category : categoriesToRemove) {
        annotation.getCategories().remove(category);
      }
      entityManager.merge(annotation);
      entityManager.persist(annotation);
      entityTransaction.commit();
      entityManager.refresh(annotation);
    }

    // remove categories from removed category set from all associated segments of the analysis list the category set is removed from
    for (AnalysisSegment analysisSegment : mediumAnalysisList.getAnalysisSegments()) {
      List<Category> analysisSegmentCategoryList = analysisSegment.getCategories();
      List<Category> segmentCategoriesToRemove = categoryList.stream().distinct()
                                                             .filter(analysisSegmentCategoryList::contains)
                                                             .collect(Collectors.toList());
      entityTransaction.begin();
      for (Category category : segmentCategoriesToRemove) {
        analysisSegment.getCategories().remove(category);
      }
      entityManager.merge(analysisSegment);
      entityManager.persist(analysisSegment);
      entityTransaction.commit();
      entityManager.refresh(analysisSegment);

      // remove categories from removed category set from all associated sequences of the analysis list the category set is removed from
      for (AnalysisSequence analysisSequence : analysisSegment.getAnalysisSequences()) {
        List<Category> analysisSequenceCategoryList = analysisSequence.getCategories();
        List<Category> sequenceCategoriesToRemove = categoryList.stream().distinct()
                                                                .filter(analysisSequenceCategoryList::contains)
                                                                .collect(Collectors.toList());
        entityTransaction.begin();
        for (Category category : sequenceCategoriesToRemove) {
          analysisSequence.getCategories().remove(category);
        }
        entityManager.merge(analysisSequence);
        entityManager.persist(analysisSequence);
        entityTransaction.commit();
        entityManager.refresh(analysisSequence);

        // remove categories from removed category set from all associated takes of the analysis list the category set is removed from
        for (AnalysisTake analysisTake : analysisSequence.getAnalysisTakes()) {
          List<Category> analysisTakeCategoryList = analysisTake.getCategories();
          List<Category> takeCategoriesToRemove = categoryList.stream().distinct()
                                                              .filter(analysisTakeCategoryList::contains)
                                                              .collect(Collectors.toList());
          entityTransaction.begin();
          for (Category category : takeCategoriesToRemove) {
            analysisTake.getCategories().remove(category);
          }
          entityManager.merge(analysisTake);
          entityManager.persist(analysisTake);
          entityTransaction.commit();
          entityManager.refresh(analysisTake);
        }
      }

      // remove categories from removed category set from all associated scenes of the analysis list the category set is removed from
      for (AnalysisScene analysisScene : analysisSegment.getAnalysisScenes()) {
        List<Category> analysisSceneCategoryList = analysisScene.getCategories();
        List<Category> sceneCategoriesToRemove = categoryList.stream().distinct()
                                                             .filter(analysisSceneCategoryList::contains)
                                                             .collect(Collectors.toList());
        entityTransaction.begin();
        for (Category category : sceneCategoriesToRemove) {
          analysisScene.getCategories().remove(category);
        }
        entityManager.merge(analysisScene);
        entityManager.persist(analysisScene);
        entityTransaction.commit();
        entityManager.refresh(analysisScene);

        // remove categories from removed category set from all associated actions of the analysis list the category set is removed from
        for (AnalysisAction analysisAction : analysisScene.getAnalysisActions()) {
          List<Category> analysisActionCategoryList = analysisAction.getCategories();
          List<Category> actionCategoriesToRemove = categoryList.stream().distinct()
                                                                .filter(analysisActionCategoryList::contains)
                                                                .collect(Collectors.toList());
          entityTransaction.begin();
          for (Category category : actionCategoriesToRemove) {
            analysisAction.getCategories().remove(category);
          }
          entityManager.merge(analysisAction);
          entityManager.persist(analysisAction);
          entityTransaction.commit();
          entityManager.refresh(analysisAction);
        }
      }
    }


    // attach categorySet to annotation and vice versa
    entityTransaction.begin();
    mediumAnalysisList.getRestrictedCategorySets().remove(categorySet);
    categorySet.getMediumAnalysisLists().remove(mediumAnalysisList);
    entityManager.merge(categorySet);
    entityManager.merge(mediumAnalysisList);
    entityManager.persist(mediumAnalysisList);
    entityManager.persist(categorySet);
    entityTransaction.commit();
    entityManager.refresh(mediumAnalysisList);

    return Response.ok().build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{analysisListId}/tag/{tagId}")
  @Secured
  public Response addExistingTag(@PathParam("analysisListId") int analysisListId, @PathParam("tagId") int tagId) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumAnalysisList analysisList = entityManager.find(MediumAnalysisList.class, analysisListId);
    if (analysisList == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    Tag tag = entityManager.find(Tag.class, tagId);
    if (tag == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    // attach tag to annotation and vice versa
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    analysisList.getTags().add(tag);
    tag.getMediumAnalysisLists().add(analysisList);
    entityManager.merge(tag);
    entityManager.merge(analysisList);
    entityManager.persist(analysisList);
    entityManager.persist(tag);
    entityTransaction.commit();
    entityManager.refresh(analysisList);

    return Response.ok().entity(tag).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{analysisListId}/tag/{tagId}")
  @Secured
  public Response removeTag(@PathParam("analysisListId") int analysisListId, @PathParam("tagId") int tagId) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumAnalysisList analysisList = entityManager.find(MediumAnalysisList.class, analysisListId);
    if (analysisList == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    Tag tag = entityManager.find(Tag.class, tagId);
    if (tag == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    // attach tag to annotation and vice versa
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    analysisList.getTags().remove(tag);
    tag.getMediumAnalysisLists().remove(analysisList);
    entityManager.merge(tag);
    entityManager.merge(analysisList);
    entityManager.persist(analysisList);
    entityManager.persist(tag);
    entityTransaction.commit();
    entityManager.refresh(analysisList);

    return Response.ok().build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("keyframeFix")
  @Secured
  public Response getOldKeyframeTimestamps(@QueryParam("authToken") String authToken) {
    // verify auth token
    int userId = 0;
    if (AuthenticationFilter.isTokenValid(authToken)) {
      userId = AuthenticationFilter.getTokenClaimUserId(authToken);
    }
    else {
      return Response.status(Status.UNAUTHORIZED).build();
    }
    if (userId != 1) { // only Admin may update file lengths
      return Response.status(Status.FORBIDDEN).build();
    }

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    String exclude1 = "\"\"";
    String exclude2 = "\"{\"keyframes\":[{\"time\":0,\"shapes\":[]}]}\"";
    String exclude3 = "\"[]\"";
    String sql = "SELECT s FROM SelectorSvg s WHERE s.svgData <> :exclude1 AND s.svgData <> :exclude2 AND s.svgData <> :exclude3";
    Query query = entityManager.createQuery(sql).setParameter("exclude1", exclude1).setParameter("exclude2", exclude2)
                               .setParameter("exclude3", exclude3);
    List<SelectorSvg> selectorSvgList = castList(SelectorSvg.class, query.getResultList());

    return Response.ok(selectorSvgList).build();
  }

  @PATCH
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("keyframeFix")
  @Secured
  public Response updateOldKeyframeTimestamps(@QueryParam("authToken") String authToken, String jsonData) {
    // verify auth token
    int userId = 0;
    if (AuthenticationFilter.isTokenValid(authToken)) {
      userId = AuthenticationFilter.getTokenClaimUserId(authToken);
    }
    else {
      return Response.status(Status.UNAUTHORIZED).build();
    }
    if (userId != 1) { // only Admin may update file lengths
      return Response.status(Status.FORBIDDEN).build();
    }

    ObjectMapper mapper = new ObjectMapper();
    List<SelectorSvg> selectorSvgList = null;
    try {
      selectorSvgList = Arrays.asList(mapper.readValue(jsonData, SelectorSvg[].class));
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (selectorSvgList == null) {
      return Response.notModified().build();
    }

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    EntityTransaction entityTransaction = entityManager.getTransaction();
    SelectorSvg selectorSvg;
    for (SelectorSvg updatedSelectorSvg : selectorSvgList) {
      selectorSvg = entityManager.find(SelectorSvg.class, updatedSelectorSvg.getId());
      if (selectorSvg == null) {
        continue; // step over invalid entries (which shouldn't occur)
      }
      selectorSvg.setSvgData(updatedSelectorSvg.getSvgData());
      entityTransaction.begin();
      entityManager.merge(selectorSvg);
      entityManager.persist(selectorSvg);
      entityTransaction.commit();
      entityManager.refresh(selectorSvg);
    }

    return Response.ok().build();
  }

  public static <T> List<T> castList(Class<? extends T> clazz, Collection<?> c) {
    List<T> r = new ArrayList<T>(c.size());
    for (Object o : c) {
      r.add(clazz.cast(o));
    }
    return r;
  }

  private void verifyAuthorizationToAnalysisList(int analysisListId, de.bitgilde.TIMAAT.rest.security.authorization.PermissionType permissionType) {
    UserAccount userAccount = (UserAccount) containerRequestContext.getProperty(
            AuthenticationFilter.USER_ACCOUNT_PROPERTY_NAME);

    if (!analysisListAuthorizationVerifier.verifyAuthorizationToAnalysisList(analysisListId, userAccount,
            permissionType)) {
      throw new ForbiddenException("User has no access to analysis list");
    }
  }
}
