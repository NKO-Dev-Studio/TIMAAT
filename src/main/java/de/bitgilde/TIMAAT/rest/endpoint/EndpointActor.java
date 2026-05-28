package de.bitgilde.TIMAAT.rest.endpoint;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bitgilde.TIMAAT.PropertyConstants;
import de.bitgilde.TIMAAT.SelectElement;
import de.bitgilde.TIMAAT.TIMAATApp;
import de.bitgilde.TIMAAT.model.DataTableInfo;
import de.bitgilde.TIMAAT.model.FIPOP.Actor;
import de.bitgilde.TIMAAT.model.FIPOP.ActorCollective;
import de.bitgilde.TIMAAT.model.FIPOP.ActorHasAddress;
import de.bitgilde.TIMAAT.model.FIPOP.ActorHasEmailAddress;
import de.bitgilde.TIMAAT.model.FIPOP.ActorHasPhoneNumber;
import de.bitgilde.TIMAAT.model.FIPOP.ActorName;
import de.bitgilde.TIMAAT.model.FIPOP.ActorPerson;
import de.bitgilde.TIMAAT.model.FIPOP.ActorPersonIsMemberOfActorCollective;
import de.bitgilde.TIMAAT.model.FIPOP.ActorPersonTranslation;
import de.bitgilde.TIMAAT.model.FIPOP.ActorType;
import de.bitgilde.TIMAAT.model.FIPOP.Address;
import de.bitgilde.TIMAAT.model.FIPOP.AddressType;
import de.bitgilde.TIMAAT.model.FIPOP.Category;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySet;
import de.bitgilde.TIMAAT.model.FIPOP.Citizenship;
import de.bitgilde.TIMAAT.model.FIPOP.CitizenshipTranslation;
import de.bitgilde.TIMAAT.model.FIPOP.EmailAddress;
import de.bitgilde.TIMAAT.model.FIPOP.EmailAddressType;
import de.bitgilde.TIMAAT.model.FIPOP.Language;
import de.bitgilde.TIMAAT.model.FIPOP.Medium;
import de.bitgilde.TIMAAT.model.FIPOP.MediumHasActorWithRole;
import de.bitgilde.TIMAAT.model.FIPOP.MediumImage;
import de.bitgilde.TIMAAT.model.FIPOP.MembershipDetail;
import de.bitgilde.TIMAAT.model.FIPOP.PhoneNumber;
import de.bitgilde.TIMAAT.model.FIPOP.PhoneNumberType;
import de.bitgilde.TIMAAT.model.FIPOP.Role;
import de.bitgilde.TIMAAT.model.FIPOP.Sex;
import de.bitgilde.TIMAAT.model.FIPOP.SexTranslation;
import de.bitgilde.TIMAAT.model.FIPOP.Tag;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount;
import de.bitgilde.TIMAAT.rest.Secured;
import de.bitgilde.TIMAAT.rest.filter.AuthenticationFilter;
import de.bitgilde.TIMAAT.rest.model.actor.ActorListingQueryParameter;
import de.bitgilde.TIMAAT.rest.model.category.UpdateAssignedCategoriesPayload;
import de.bitgilde.TIMAAT.rest.model.categoryset.UpdateAssignedCategorySetsPayload;
import de.bitgilde.TIMAAT.security.UserLogManager;
import de.bitgilde.TIMAAT.storage.entity.actor.ActorStorage;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
@Path("/actor")
public class EndpointActor {
  @Inject
  private ActorStorage actorStorage;
	@Context
	ContainerRequestContext containerRequestContext;
	@Context
	ServletContext servletContext;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	@Path("list")
	public DataTableInfo<Actor> getActorList(@BeanParam ActorListingQueryParameter queryParameter)
	{
    UserAccount userAccount = (UserAccount) containerRequestContext.getProperty(
            AuthenticationFilter.USER_ACCOUNT_PROPERTY_NAME);

		int draw = queryParameter.getDraw().isPresent() ? queryParameter.getDraw().get() : 0;
    List<Actor> actors = actorStorage.getEntriesAsStream(queryParameter, queryParameter, queryParameter, userAccount).collect(
            Collectors.toList());
    Long actorsCount = actorStorage.getNumberOfTotalEntriesRespectingAuthorization(userAccount);
    Long matchingActorsCount = actorStorage.getNumberOfMatchingEntriesRespectingAuthorization(queryParameter, userAccount);

		return new DataTableInfo<>(draw, actorsCount, matchingActorsCount, actors);

	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	@Path("selectList")
	public List<SelectElement<Integer>> getActorSelectList(@BeanParam ActorListingQueryParameter queryParameter)
	{
    UserAccount userAccount = (UserAccount) containerRequestContext.getProperty(
            AuthenticationFilter.USER_ACCOUNT_PROPERTY_NAME);
		return actorStorage.getEntriesAsStream(queryParameter, queryParameter, queryParameter, userAccount)
                .map(currentActor -> new SelectElement<>(currentActor.getId(), currentActor.getDisplayName().getName()))
                .collect(Collectors.toList());
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	@Path("{id}/select")
	public Response getActorSelect(@PathParam("id") int id,
																 @QueryParam("start") Integer start,
																 @QueryParam("length") Integer length,
																 @QueryParam("orderby") String orderby,
																 @QueryParam("search") String search)
	{
		// System.out.println("EndpointActor: getActorList: start: "+start+" length: "+length+" orderby: "+orderby+" search: "+search);

		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		Actor actor = entityManager.find(Actor.class, id);
		List<SelectElement> actorSelectList = new ArrayList<>();
		actorSelectList.add(new SelectElement<Integer>(id, actor.getDisplayName().getName()));

		return Response.ok().entity(actorSelectList).build();

	}

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/categorySets")
  public Collection<CategorySet> getCategorySetsOfActor(@PathParam("id") Integer id) {
    return actorStorage.getCategorySetsOfActor(id);
  }

  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/categorySets")
  public Collection<CategorySet> updateCategorySetsOfActor(@PathParam("id") Integer id, UpdateAssignedCategorySetsPayload updateAssignedCategorySetsPayload){
    return actorStorage.updateAssignedCategorySetsOfActor(id, updateAssignedCategorySetsPayload.getCategorySetIds());
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/categories")
  public Collection<Category> getCategoriesOfActor(@PathParam("id") Integer id) {
    return actorStorage.getCategoriesOfActor(id);
  }

  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/categories")
  public Collection<Category> updateCategoriesOfActor(@PathParam("id") Integer id, UpdateAssignedCategoriesPayload updateAssignedCategoriesPayload){
    return actorStorage.updateAssignedCategoriesOfActor(id, updateAssignedCategoriesPayload.getCategoryIds());
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/assignableCategories/selectList")
  public List<SelectElement<Integer>> getAssignableCategories(@PathParam("id") Integer id){
    return actorStorage.getAssignableCategoriesOfActor(id).map(category -> new SelectElement<>(category.getId(), category.getName())).collect(
            Collectors.toList());
  }

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	@Path("total")
	public Response getActorDatasetsTotal() {
		// System.out.println("EndpointActor: getActorDatasetsTotal");
		Query query = TIMAATApp.emf.createEntityManager()
															 .createQuery("SELECT COUNT (a.id) FROM Actor a");
		long count = (long)query.getSingleResult();
		return Response.ok().entity(count).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	@Path("actorType/list")
	public Response getActorTypeList() {
		// System.out.println("EndpointActor: getActorTypeList");
		List<ActorType> actorTypeList = castList(ActorType.class, TIMAATApp.emf.createEntityManager().createNamedQuery("ActorType.findAll").getResultList());
		return Response.ok().entity(actorTypeList).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	@Path("person/list")
	public Response getPersonList(@QueryParam("draw") Integer draw,
																@QueryParam("start") Integer start,
																@QueryParam("length") Integer length,
																@QueryParam("orderby") String orderby,
																@QueryParam("dir") String direction,
																@QueryParam("search") String search )
	{
		// System.out.println("EndpointActor: getActorPersonList: draw: "+draw+" start: "+start+" length: "+length+" orderby: "+orderby+" dir: "+direction+" search: "+search);
		if ( draw == null ) draw = 0;

		// sanitize user input
		if ( direction != null && direction.equalsIgnoreCase("desc") ) direction = "DESC"; else direction = "ASC";

		String column = "ap.actor.displayName.name";
		if ( orderby != null ) {
			if (orderby.equalsIgnoreCase("name")) column = "ap.actor.displayName.name";
		}

		// calculate total # of records
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		Query countQuery = entityManager.createQuery("SELECT COUNT(ap.actor) FROM ActorPerson ap");
		long recordsTotal = (long) countQuery.getSingleResult();
		long recordsFiltered = recordsTotal;

		// search
		Query query;
		String sql;
		List<Actor> actorList = new ArrayList<>();
		if ( search != null && search.length() > 0 ) {
			// find all matching names
			sql = "SELECT DISTINCT a FROM Actor a, ActorName an WHERE an.actor.actorType.id = 1 AND lower(an.name) LIKE lower(concat('%', :search, '%')) AND an.actor = a ORDER BY a.displayName.name "+direction;
			query = entityManager.createQuery(sql)
													 .setParameter("search", search);
			actorList = castList(Actor.class, query.getResultList());
			// find all media belonging to those titles
			if ( start != null && start > 0 ) query.setFirstResult(start);
			if ( length != null && length > 0 ) query.setMaxResults(length);
			if ( length == -1 ) { // display all results
				length = actorList.size();
				query.setMaxResults(length);
			}

			// for (Actor actor : actorList) {
			// 	if (!(actorList.contains(actor))) { // TODO: should be obsolete with SELECT DISTINCT
			// 		actorList.add(actor);
			// 	}
			// }
			recordsFiltered = actorList.size();
			List<Actor> filteredActorList = new ArrayList<>();
			int i = start;
			int end;
			if ((recordsFiltered - start) < length) {
				end = (int)recordsFiltered;
			}
			else {
				end = start + length;
			}
			for(; i < end; i++) {
				filteredActorList.add(actorList.get(i));
			}
			return Response.ok().entity(new DataTableInfo(draw, recordsTotal, recordsFiltered, filteredActorList)).build();
		} else {
			sql = "SELECT ap.actor FROM ActorPerson ap ORDER BY "+column+" "+direction;
			query = entityManager.createQuery(sql);
			if ( start != null && start > 0 ) query.setFirstResult(start);
			if ( length != null && length > 0 ) query.setMaxResults(length);
			actorList = castList(Actor.class, query.getResultList());
		}
		return Response.ok().entity(new DataTableInfo(draw, recordsTotal, recordsFiltered, actorList)).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	@Path("person/total")
	public Response getPersonDatasetsTotal() {
		// System.out.println("EndpointActor: getPersonDatasetsTotal");
		Query query = TIMAATApp.emf.createEntityManager()
															 .createQuery("SELECT COUNT (ap.id) FROM ActorPerson ap");
		long count = (long)query.getSingleResult();
		return Response.ok().entity(count).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	@Path("person/selectList")
	public Response getPersonSelectList(@QueryParam("start") Integer start,
																			@QueryParam("length") Integer length,
																			@QueryParam("orderby") String orderby,
																			@QueryParam("search") String search)	{
		// String column = "a.id";
		// if ( orderby != null ) {
		// 	if (orderby.equalsIgnoreCase("name")) column = "a.displayName.name"; // TODO change displayName access in DB-Schema
		// }

		// define default query strings
		String actorQuery = "SELECT a FROM Actor a ORDER BY a.displayName.name";
		String actorSearchQuery = "SELECT a FROM Actor a WHERE lower(a.displayName.name) LIKE lower(concat('%', :name,'%')) ORDER BY a.displayName.name";

		// search
		Query query;
		if ( search != null && search.length() > 0 ) {
			// perform search
			query = TIMAATApp.emf.createEntityManager().createQuery(
				actorSearchQuery);
			query.setParameter("name", search);
			// query.setParameter("actorName", search); // birthName
		} else {
			query = TIMAATApp.emf.createEntityManager().createQuery(
				actorQuery);
		}
		// if ( start != null && start > 0 ) query.setFirstResult(start);
		// if ( length != null && length > 0 ) query.setMaxResults(length);

		List<Actor> actorList = castList(Actor.class, query.getResultList());
		List<SelectElement> actorSelectList = new ArrayList<>();
		for (Actor actor : actorList) {
			if (actor.getActorPerson() != null) {
				actorSelectList.add(new SelectElement<Integer>(actor.getId(), actor.getDisplayName().getName()));
			}
		}
		return Response.ok().entity(actorSelectList).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	@Path("collective/list")
	public Response getCollectiveList(@QueryParam("draw") Integer draw,
																		@QueryParam("start") Integer start,
																		@QueryParam("length") Integer length,
																		@QueryParam("orderby") String orderby,
																		@QueryParam("dir") String direction,
																		@QueryParam("search") String search )
	{
		// System.out.println("EndpointActor: getActorPersonList: draw: "+draw+" start: "+start+" length: "+length+" orderby: "+orderby+" dir: "+direction+" search: "+search);
		if ( draw == null ) draw = 0;

		// sanitize user input
		if ( direction != null && direction.equalsIgnoreCase("desc") ) direction = "DESC"; else direction = "ASC";

		String column = "ac.actor.displayName.name";
		if ( orderby != null ) {
			if (orderby.equalsIgnoreCase("name")) column = "ac.actor.displayName.name"; // TODO change displayName access in DB-Schema
		}

		// calculate total # of records
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		Query countQuery = entityManager.createQuery("SELECT COUNT(ac.actor) FROM ActorCollective ac");
		long recordsTotal = (long) countQuery.getSingleResult();
		long recordsFiltered = recordsTotal;

		// search
		Query query;
		String sql;
		List<Actor> actorList = new ArrayList<>();
		if ( search != null && search.length() > 0 ) {
			// find all matching names
			sql = "SELECT DISTINCT a FROM Actor a, ActorName an WHERE an.actor.actorType.id = 2 AND lower(an.name) LIKE lower(concat('%', :search, '%')) AND an.actor = a ORDER BY a.displayName.name "+direction;
			query = entityManager.createQuery(sql)
													 .setParameter("search", search);
			actorList = castList(Actor.class, query.getResultList());
			// find all media belonging to those titles
			if ( start != null && start > 0 ) query.setFirstResult(start);
			if ( length != null && length > 0 ) query.setMaxResults(length);
			if ( length == -1 ) { // display all results
				length = actorList.size();
				query.setMaxResults(length);
			}

			// for (Actor actor : actorList) {
			// 	if (!(actorList.contains(actor))) {// TODO: should be obsolete with SELECT DISTINCT
			// 		actorList.add(actor);
			// 	}
			// }
			recordsFiltered = actorList.size();
			List<Actor> filteredActorList = new ArrayList<>();
			int i = start;
			int end;
			if ((recordsFiltered - start) < length) {
				end = (int)recordsFiltered;
			}
			else {
				end = start + length;
			}
			for(; i < end; i++) {
				filteredActorList.add(actorList.get(i));
			}
			return Response.ok().entity(new DataTableInfo(draw, recordsTotal, recordsFiltered, filteredActorList)).build();
		} else {
			sql = "SELECT ac.actor FROM ActorCollective ac ORDER BY "+column+" "+direction;
			query = entityManager.createQuery(sql);
			if ( start != null && start > 0 ) query.setFirstResult(start);
			if ( length != null && length > 0 ) query.setMaxResults(length);
			actorList = castList(Actor.class, query.getResultList());
		}

		return Response.ok().entity(new DataTableInfo(draw, recordsTotal, recordsFiltered, actorList)).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	@Path("collective/total")
	public Response getCollectiveDatasetsTotal() {
		// System.out.println("EndpointActor: getCollectiveDatasetsTotal");
		Query query = TIMAATApp.emf.createEntityManager()
															 .createQuery("SELECT COUNT (ac.id) FROM ActorCollective ac");
		long count = (long)query.getSingleResult();
		return Response.ok().entity(count).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	@Path("collective/selectList")
	public Response getCollectiveSelectList(@QueryParam("start") Integer start,
																					@QueryParam("length") Integer length,
																					@QueryParam("orderby") String orderby,
																					@QueryParam("search") String search) {
			// String column = "a.id";
			// if ( orderby != null ) {
			// 	if (orderby.equalsIgnoreCase("name")) column = "a.displayName.name"; // TODO change displayName access in DB-Schema
			// }

			// define default query strings
			String actorQuery = "SELECT a FROM Actor a ORDER BY a.displayName.name";
			String actorSearchQuery = "SELECT a FROM Actor a WHERE lower(a.displayName.name) LIKE lower(concat('%', :name,'%')) ORDER BY a.displayName.name";

			// search
			Query query;
			if ( search != null && search.length() > 0 ) {
				// perform search
				query = TIMAATApp.emf.createEntityManager().createQuery(
					actorSearchQuery);
				query.setParameter("name", search);
				// query.setParameter("actorName", search); // birthName
			} else {
				query = TIMAATApp.emf.createEntityManager().createQuery(
					actorQuery);
			}
			// if ( start != null && start > 0 ) query.setFirstResult(start);
			// if ( length != null && length > 0 ) query.setMaxResults(length);

			List<Actor> actorList = castList(Actor.class, query.getResultList());
			List<SelectElement> actorSelectList = new ArrayList<>();
			for (Actor actor : actorList) {
				if (actor.getActorCollective() != null) {
					actorSelectList.add(new SelectElement<Integer>(actor.getId(), actor.getDisplayName().getName()));
				}
			}
			return Response.ok().entity(actorSelectList).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	@Path("addressType/list")
	public Response getAddressTypeList() {
		// System.out.println("EndpointActor: getAddressTypeList");
		List<AddressType> addressTypeList = castList(AddressType.class, TIMAATApp.emf.createEntityManager().createNamedQuery("AddressType.findAll").getResultList());
		return Response.ok().entity(addressTypeList).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	@Path("emailAddressType/list")
	public Response getEmailAddressTypeList() {
		// System.out.println("EndpointActor: getEmailAddressTypeList");
		List<EmailAddressType> emailAddressTypeList = castList(EmailAddressType.class, TIMAATApp.emf.createEntityManager().createNamedQuery("EmailAddressType.findAll").getResultList());
		return Response.ok().entity(emailAddressTypeList).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	@Path("phoneNumberType/list")
	public Response getPhoneNumberTypeList() {
		// System.out.println("EndpointActor: getPhoneNumberTypeList");
		List<PhoneNumberType> emailAddressTypeList = castList(PhoneNumberType.class, TIMAATApp.emf.createEntityManager().createNamedQuery("PhoneNumberType.findAll").getResultList());
		return Response.ok().entity(emailAddressTypeList).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	@Path("{id}/names/list")
	public Response getNamesList(@PathParam("id") int id) {
		// System.out.println("EndpointActor: getNamesList");

		EntityManager entityManager = TIMAATApp.emf.createEntityManager();

		// find actor
		Actor actor = entityManager.find(Actor.class, id);
		if ( actor == null ) return Response.status(Status.NOT_FOUND).build();

		entityManager.refresh(actor);

		return Response.ok().entity(actor.getActorNames()).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	@Path("{id}/role/list")
	public Response getActorHasRoleList(@PathParam("id") int actorId)
	{
		// System.out.println("EndpointActor: getActorHasRoleList - ID: "+ actorId);
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		Actor actor = entityManager.find(Actor.class, actorId);
		List<Role> roleList = actor.getRoles();

		return Response.ok().entity(roleList).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	@Path("{actorId}/role/{roleId}/list")
	public Response getActorRoleInMediumList(@PathParam("actorId") int actorId,
																					 @PathParam("roleId") int roleId)
	{
		// System.out.println("EndpointActor: getActorRoleInMediumList - actorId, roleId: " + actorId + " " + roleId);
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		String sql = "SELECT mhawr FROM MediumHasActorWithRole mhawr WHERE mhawr.actor.id = :actorId AND mhawr.role.id = :roleId";
		// String sql = "SELECT DISTINCT m FROM Medium m WHERE m.actorHasRoles.actor.id = :actorId AND m.actorHasRoles.role.id = :roleId";
		Query query = entityManager.createQuery(sql)
			.setParameter("actorId", actorId)
			.setParameter("roleId", roleId);
		// Actor actor = entityManager.find(Actor.class, actorId);
		// Role role = entityManager.find(Role.class, roleId);
		// ActorHasRole actorHasRole = new ActorHasRole(actor, role);
		List<MediumHasActorWithRole> mhawrList = castList(MediumHasActorWithRole.class, query.getResultList());
		List<Medium> mediumList = new ArrayList<>();
		for (MediumHasActorWithRole mhawr : mhawrList) {
			mediumList.add(mhawr.getMedium());
		}

		return Response.ok().entity(mediumList).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	@Path("withRole/{roleId}")
	public Response getActorsWithThisRoleList(@PathParam("roleId") int roleId)
	{
		// System.out.println("EndpointActor: getActorsWithThisRoleList - ID: "+ roleId);
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		Role role = entityManager.find(Role.class, roleId);
		List<Actor> actorList = role.getActors();

		return Response.ok().entity(actorList).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	@Path("{id}/image/list")
	public Response getActorHasImageList(@PathParam("id") int actorId)
	{
		// System.out.println("EndpointActor: getActorHasImageList - ID: "+ actorId);
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		Actor actor = entityManager.find(Actor.class, actorId);
		List<MediumImage> imageList = actor.getProfileImages();

		return Response.ok().entity(imageList).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	@Path("{actorId}/hasTagList")
	public Response getTagList(@PathParam("actorId") Integer actorId)
	{
		// System.out.println("EndpointActor: getTagList");
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		Actor actor = entityManager.find(Actor.class, actorId);
		if ( actor == null ) return Response.status(Status.NOT_FOUND).build();
		entityManager.refresh(actor);
		return Response.ok().entity(actor.getTags()).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	@Path("sex/selectList")
	public Response getSexSelectList(@QueryParam("page") Integer page,
																	 @QueryParam("search") String search,
																	 @QueryParam("language") String languageCode) {
		// returns list of id and name combinations of all Languages
		// System.out.println("EndpointActor: getSexSelectList");

		if ( languageCode == null) languageCode = "default"; // as long as multi language is not implemented yet, use the 'default' language entry

		// search
		Query query;
		if (search != null && search.length() > 0) {
			query = TIMAATApp.emf.createEntityManager().createQuery(
				"Select st FROM SexTranslation st WHERE lower(st.type) LIKE lower(concat('%', :type, '%')) AND st.language.id = (SELECT l.id FROM Language l WHERE l.code ='"+languageCode+"') ORDER BY st.type ASC");
				query.setParameter("type", search);
		} else {
			query = TIMAATApp.emf.createEntityManager().createQuery(
				"SELECT st FROM SexTranslation st WHERE st.language.id = (SELECT l.id FROM Language l WHERE l.code ='"+languageCode+"') ORDER BY st.type ASC");
		}
		List<SelectElement> sexSelectList = new ArrayList<>();
		List<SexTranslation> sexTranslationList = castList(SexTranslation.class, query.getResultList());
		for (SexTranslation sexTranslation : sexTranslationList) {
			sexSelectList.add(new SelectElement<Integer>(sexTranslation.getSex().getId(),
																					sexTranslation.getType()));
		}
		return Response.ok().entity(sexSelectList).build();
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("{id}")
	@Secured
	public Response createActor(@PathParam("id") int id, String jsonData) {
		// System.out.println("EndpointActor: createActor: " + jsonData);
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
		Actor newActor = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();

		// parse JSON data
		try {
			newActor = mapper.readValue(jsonData, Actor.class);
		} catch (IOException e) {
			System.out.println("EndpointActor: createActor - IOException");
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( newActor == null ) {
			System.out.println("EndpointActor: createActor - newActor == null");
			return Response.status(Status.BAD_REQUEST).build();
		}
		// sanitize object data
		newActor.setId(0);
		// ActorType actorType = entityManager.find(ActorType.class, newActor.getActorType().getId());
		// newActor.setActorType(actorType);
		newActor.setDisplayName(null); //* displayName will be set once name is created
		newActor.setBirthName(null); //* birthName will be set once name is created

		// update log metadata
		Timestamp creationDate = new Timestamp(System.currentTimeMillis());
		newActor.setCreatedAt(creationDate);
		newActor.setLastEditedAt(creationDate);
		if ( containerRequestContext.getProperty("TIMAAT.userID") != null ) {
			newActor.setCreatedByUserAccount(entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID")));
			newActor.setLastEditedByUserAccount((entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID"))));
		} else {
			// DEBUG do nothing - production system should abort with internal server error
			return Response.serverError().build();
		}

		// persist actor
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.persist(newActor);
		entityManager.flush();
		entityTransaction.commit();
		entityManager.refresh(newActor);

		// TODO check if anything additional has to be done for adding profile image m-n relations

		// add log entry
		UserLogManager.getLogger()
									.addLogEntry(newActor.getCreatedByUserAccount().getId(),
															 UserLogManager.LogEvents.ACTORCREATED);
		return Response.ok().entity(newActor).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	@Path("{id}")
	public Response getActor(@PathParam("id") int id) {
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		Actor actor = entityManager.find(Actor.class, id);
		if ( actor == null ) return Response.status(Status.NOT_FOUND).build();
		return Response.ok().entity(actor).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	@Path("{id}/name")
	public Response getActorName(@PathParam("id") int id) {
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		Actor actor = entityManager.find(Actor.class, id);
		if ( actor == null ) return Response.status(Status.NOT_FOUND).build();
		return Response.ok().entity(actor.getDisplayName()).build();
	}

	@PATCH
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("{id}")
	@Secured
	public Response updateActor(@PathParam("id") int id, String jsonData) {
		// System.out.println("EndpointActor: updateActor - jsonData"+ jsonData);
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
		Actor updatedActor = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		Actor actor = entityManager.find(Actor.class, id);

		if ( actor == null ) return Response.status(Status.NOT_FOUND).build();
		// parse JSON data
		try {
			updatedActor = mapper.readValue(jsonData, Actor.class);
		} catch (IOException e) {
			System.out.println("EndpointActor: updateActor - IOException");
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( updatedActor == null ) return Response.notModified().build();

		List<Role> oldRoles = actor.getRoles();
    // update actor
		if (updatedActor.getIsFictional() != null ) actor.setIsFictional(updatedActor.getIsFictional());
		if (updatedActor.getDisplayName() != null ) actor.setDisplayName(updatedActor.getDisplayName());
		actor.setBirthName(updatedActor.getBirthName());
		actor.setPrimaryAddress(updatedActor.getPrimaryAddress());
		actor.setPrimaryEmailAddress(updatedActor.getPrimaryEmailAddress());
		actor.setPrimaryPhoneNumber(updatedActor.getPrimaryPhoneNumber());
		actor.setRoles(updatedActor.getRoles());
		actor.setProfileImages(updatedActor.getProfileImages());
		List<Tag> oldTags = actor.getTags();
		actor.setTags(updatedActor.getTags());

		// update log metadata
		actor.setLastEditedAt(new Timestamp(System.currentTimeMillis()));
		if ( containerRequestContext.getProperty("TIMAAT.userID") != null ) {
			actor.setLastEditedByUserAccount((entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID"))));
		} else {
			// DEBUG do nothing - production system should abort with internal server error
		}

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.merge(actor);
		entityManager.persist(actor);
		entityTransaction.commit();
		entityManager.refresh(actor);
		for (Role role : actor.getRoles()) {
			entityManager.refresh(role);
		}
		for (Role role : oldRoles) {
			entityManager.refresh(role);
		}
		for (Tag tag : actor.getTags()) {
			entityManager.refresh(tag);
		}
		for (Tag tag : oldTags) {
			entityManager.refresh(tag);
		}

		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
															 UserLogManager.LogEvents.ACTOREDITED);
		return Response.ok().entity(actor).build();
	}

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{id}")
	@Secured
	public Response deleteActor(@PathParam("id") int id) {
		// System.out.println("EndpointActor: deleteActor");
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		Actor actor = entityManager.find(Actor.class, id);
		if ( actor == null ) return Response.status(Status.NOT_FOUND).build();

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.remove(actor);
		entityTransaction.commit();

		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
																UserLogManager.LogEvents.ACTORDELETED);
		return Response.ok().build();
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("person/{id}")
	@Secured
	public Response createPerson(@PathParam("id") int id, String jsonData) {
		// System.out.println("EndpointActor: createPerson jsonData: "+jsonData);
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		ActorPerson newPerson = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();

		// parse JSON data
		try {
			newPerson = mapper.readValue(jsonData, ActorPerson.class);
		} catch (IOException e) {
			System.out.println("EndpointActor: createPerson: IOException e !");
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( newPerson == null ) {
			System.out.println("EndpointActor: createPerson: newPerson == null !");
			return Response.status(Status.BAD_REQUEST).build();
		}

		// sanitize object data
		// TODO link to locations
		// Location placeOfBirth = entityManager.find(Location.class, newPerson.getPlaceOfBirth().getLocationId());
		// Location placeOfDeath = entityManager.find(Location.class, newPerson.getPlaceOfDeath().getLocationId());
		Sex sex = entityManager.find(Sex.class, newPerson.getSex().getId());
		newPerson.setSex(sex);

		// update log metadata
		// Not necessary, a person will always be created in conjunction with an actor

		// persist person
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		// entityManager.persist(placeOfBirth);
		// entityManager.persist(placeOfDeath);
		entityManager.persist(newPerson);
		entityManager.flush();
		entityTransaction.commit();
		entityManager.refresh(newPerson);
		// entityManager.refresh(placeOfBirth);
		// entityManager.refresh(placeOfDeath);
		entityManager.refresh(newPerson.getActor());

		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
																UserLogManager.LogEvents.PERSONCREATED);
		return Response.ok().entity(newPerson).build();
	}

	@PATCH
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("person/{id}")
	@Secured
	public Response updatePerson(@PathParam("id") int id, String jsonData) {

		// System.out.println("EndpointActor: updatePerson jsonData: " + jsonData);
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		ActorPerson updatedPerson = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		ActorPerson person = entityManager.find(ActorPerson.class, id);

		if ( person == null ) return Response.status(Status.NOT_FOUND).build();
		// parse JSON data
		try {
			updatedPerson = mapper.readValue(jsonData, ActorPerson.class);
		} catch (IOException e) {
			System.out.println("EndpointActor: updatePerson - IOException");
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( updatedPerson == null ) {
			System.out.println("EndpointActor: updatePerson - updatedPerson == null");
			return Response.notModified().build();
		}

		// update person
		person.setTitle(updatedPerson.getTitle());
		person.setCitizenship(updatedPerson.getCitizenship());
		person.setDateOfBirth(updatedPerson.getDateOfBirth());
		// TODO update place of birth once string is replaced with city object
		person.setPlaceOfBirth(updatedPerson.getPlaceOfBirth());
		person.setDayOfDeath(updatedPerson.getDayOfDeath());
		// TODO update place of death once string is replaced with city object
		person.setPlaceOfDeath(updatedPerson.getPlaceOfDeath());
		person.setSex(updatedPerson.getSex());
		// TODO update person is member of collective

		// update log metadata
		person.getActor().setLastEditedAt(new Timestamp(System.currentTimeMillis()));
		if ( containerRequestContext.getProperty("TIMAAT.userID") != null ) {
			person.getActor().setLastEditedByUserAccount((entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID"))));
		} else {
			// DEBUG do nothing - production system should abort with internal server error
		}

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.merge(person);
		entityManager.persist(person);
		entityTransaction.commit();
		entityManager.refresh(person);

		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
																UserLogManager.LogEvents.PERSONEDITED);
		return Response.ok().entity(person).build();
	}

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("person/{id}")
	@Secured
	public Response deletePerson(@PathParam("id") int id) {
		// System.out.println("EndpointActor: deletePerson with id: "+ id);
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();

		Actor actor = entityManager.find(Actor.class, id);
		if ( actor == null ) return Response.status(Status.NOT_FOUND).build();
		ActorPerson person = entityManager.find(ActorPerson.class, id);
		if ( person == null ) return Response.status(Status.NOT_FOUND).build();

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.remove(person);
		// entityManager.remove(person.getActor().getDisplayName());
		entityManager.remove(person.getActor()); // remove person, then corresponding actor
		entityTransaction.commit();

		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
																UserLogManager.LogEvents.PERSONDELETED);
		return Response.ok().build();
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured
	@Path("{personId}/translation/{id}")
	public Response createPersonTranslation(@PathParam("personId") int personId, @PathParam("id") int id, String jsonData) {

		// System.out.println("PersonEndpoint: createPersonTranslation jsonData: "+jsonData);
		ObjectMapper mapper = new ObjectMapper();
		ActorPersonTranslation newTranslation = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		ActorPerson person = entityManager.find(ActorPerson.class, personId);

		if ( person == null ) return Response.status(Status.NOT_FOUND).build();
		// parse JSON data
		try {
			newTranslation = mapper.readValue(jsonData, ActorPersonTranslation.class);
		} catch (IOException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( newTranslation == null ) return Response.status(Status.BAD_REQUEST).build();

		// sanitize object data
		newTranslation.setId(0);
		newTranslation.setActorPerson(person);
		Language language = entityManager.find(Language.class, newTranslation.getLanguage().getId());
		newTranslation.setLanguage(language);
		person.addActorPersonTranslation(newTranslation);

		// persist personTranslation and person
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.persist(newTranslation);
		entityManager.flush();
		newTranslation.setActorPerson(person);
		entityManager.persist(person);
		entityTransaction.commit();
		entityManager.refresh(newTranslation);
		entityManager.refresh(person);

		// add log entry (always in conjunction with person)
		// UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
		// 																				UserLogManager.LogEvents.PERSONCREATED);

		return Response.ok().entity(newTranslation).build();
	}

	@PATCH
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured
	@Path("{personId}/translation/{id}")
	public Response updateActorPersonTranslation(@PathParam("personId") int personId, @PathParam("id") int id, String jsonData) {

		// System.out.println("ActorPersonEndpoint: updateActorPersonTranslation - jsonData"+ jsonData);
		ObjectMapper mapper = new ObjectMapper();
		ActorPersonTranslation updatedTranslation = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		ActorPersonTranslation personTranslation = entityManager.find(ActorPersonTranslation.class, id);

		if ( personTranslation == null ) return Response.status(Status.NOT_FOUND).build();
		// parse JSON data
		try {
			updatedTranslation = mapper.readValue(jsonData, ActorPersonTranslation.class);
		} catch (IOException e) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( updatedTranslation == null ) return Response.notModified().build();

		// update person translation
		if ( updatedTranslation.getSpecialFeatures() != null ) personTranslation.setSpecialFeatures(updatedTranslation.getSpecialFeatures());

		// persist person translation
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.merge(personTranslation);
		entityManager.persist(personTranslation);
		entityTransaction.commit();
		entityManager.refresh(personTranslation);

		// add log entry (always in conjunction with person)
		// UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
		// 																				UserLogManager.LogEvents.PERSONEDITED);

		return Response.ok().entity(personTranslation).build();

	}

	// not needed yet (should be necessary once several translations for an person exist and individual ones need to be removed)
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{personId}/translation/{id}")
	@Secured
	public Response deleteActorPersonTranslation(@PathParam("personId") int personId, @PathParam("id") int id) {

		// System.out.println("ActorPersonEndpoint: deleteActorPersonTranslation");
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		ActorPersonTranslation personTranslation = entityManager.find(ActorPersonTranslation.class, id);

		if ( personTranslation == null ) return Response.status(Status.NOT_FOUND).build();

		// sanitize person translation
		ActorPerson person = personTranslation.getActorPerson();

		// persist person translation
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.remove(personTranslation);
		entityTransaction.commit();
		entityManager.refresh(person);

		// add log entry (always in conjunction with person)
		// UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
		// 																				UserLogManager.LogEvents.PERSONDELETED);
		return Response.ok().build();
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("collective/{id}")
	@Secured
	public Response createCollective(@PathParam("id") int id, String jsonData) {

		// System.out.println("EndpointActor: createCollective jsonData: "+jsonData);
		ObjectMapper mapper = new ObjectMapper();
		ActorCollective newCollective = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();

		// parse JSON data
		try {
			newCollective = mapper.readValue(jsonData, ActorCollective.class);
		} catch (IOException e) {
			System.out.println("EndpointActor: createCollective: IOException e !");
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( newCollective == null ) {
			System.out.println("EndpointActor: createCollective: newCollective == null !");
			return Response.status(Status.BAD_REQUEST).build();
		}
		// sanitize object data

		// update log metadata
		// Not necessary, a collective will always be created in conjunction with a actor

		// persist collective
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.persist(newCollective);
		entityManager.flush();
		entityTransaction.commit();
		entityManager.refresh(newCollective);
		entityManager.refresh(newCollective.getActor());

		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
																UserLogManager.LogEvents.COLLECTIVECREATED);
		return Response.ok().entity(newCollective).build();
	}

	@PATCH
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("collective/{id}")
	@Secured
	public Response updateCollective(@PathParam("id") int id, String jsonData) {
		// System.out.println("EndpointActor: updateCollective - jsonData: " + jsonData);
		ObjectMapper mapper = new ObjectMapper();
		ActorCollective updatedCollective = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		ActorCollective collective = entityManager.find(ActorCollective.class, id);

		if ( collective == null ) return Response.status(Status.NOT_FOUND).build();
		// parse JSON data
		try {
			updatedCollective = mapper.readValue(jsonData, ActorCollective.class);
		} catch (IOException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( updatedCollective == null ) return Response.notModified().build();

		// update collective
		collective.setFounded(updatedCollective.getFounded());
		collective.setDisbanded(updatedCollective.getDisbanded());

		// update log metadata
		collective.getActor().setLastEditedAt(new Timestamp(System.currentTimeMillis()));
		if ( containerRequestContext.getProperty("TIMAAT.userID") != null ) {
			collective.getActor().setLastEditedByUserAccount((entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID"))));
		} else {
			// DEBUG do nothing - production system should abort with internal server error
		}
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.merge(collective);
		entityManager.persist(collective);
		entityTransaction.commit();
		entityManager.refresh(collective);

		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
																UserLogManager.LogEvents.COLLECTIVEEDITED);
		return Response.ok().entity(collective).build();
	}

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("collective/{id}")
	@Secured
	public Response deleteCollective(@PathParam("id") int id) {
		// System.out.println("EndpointActor: deleteCollective with id: "+ id);
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();

		Actor actor = entityManager.find(Actor.class, id);
		if ( actor == null ) return Response.status(Status.NOT_FOUND).build();
		ActorCollective collective = entityManager.find(ActorCollective.class, id);
		if ( collective == null ) return Response.status(Status.NOT_FOUND).build();

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.remove(collective);
		// entityManager.remove(collective.getActor().getDisplayName());
		entityManager.remove(collective.getActor()); // remove collective, then corresponding actor
		entityTransaction.commit();

		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
																UserLogManager.LogEvents.COLLECTIVEDELETED);
		return Response.ok().build();
	}

	// Currently not in use
	@POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
	@Path("name/{id}")
	@Secured
	public Response createName(@PathParam("id") int id, String jsonData) {
		// System.out.println("EndpointActor: createName: jsonData: "+jsonData);
		ObjectMapper mapper = new ObjectMapper();
		ActorName newName = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();

		// parse JSON data
		try {
			newName = mapper.readValue(jsonData, ActorName.class);
		} catch (IOException e) {
			System.out.println("EndpointActor: createName: IOException e !");
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( newName == null ) {
			System.out.println("EndpointActor: createName: newName == null !");
			return Response.status(Status.BAD_REQUEST).build();
		}
		// sanitize object data
		newName.setId(0);
		Actor actor = entityManager.find(Actor.class, newName.getActor().getId());
		newName.setActor(actor);

		// update log metadata
		// Not necessary, a name will always be created in conjunction with a actor

		// persist name
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.persist(newName);
		actor.getActorNames().add(newName);
		entityManager.persist(actor);
		entityManager.flush();
		newName.setActor(actor);
		entityTransaction.commit();
		entityManager.refresh(newName);
		entityManager.refresh(actor);

		// add log entry
		UserLogManager.getLogger()
									// .addLogEntry(newName.getActor().getCreatedByUserAccount().getId(), UserLogManager.LogEvents.ACTORNAMECREATED);
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
																UserLogManager.LogEvents.ACTORNAMECREATED);

		return Response.ok().entity(newName).build();
	}

	@POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
	@Path("{actorId}/name/{id}")
	@Secured
	public Response addName(@PathParam("actorId") int actorId,
													@PathParam("id") int id,
													String jsonData) {

		// System.out.println("EndpointActor: addName: jsonData: "+jsonData);
		ObjectMapper mapper = new ObjectMapper();
		ActorName newName = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();

		// parse JSON data
		try {
			newName = mapper.readValue(jsonData, ActorName.class);
		} catch (IOException e) {
			System.out.println("EndpointActor: addName: IOException e !");
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( newName == null ) {
			System.out.println("EndpointActor: addName: newName == null !");
			return Response.status(Status.BAD_REQUEST).build();
		}
		// sanitize object data
		newName.setId(0);
		// Language language = entityManager.find(Language.class, newName.getLanguage().getId());
		// newName.setLanguage(language);
		Actor actor = entityManager.find(Actor.class, actorId);
		newName.setActor(actor);

		// update log metadata
		// Not necessary, a name will always be created in conjunction with a actor

		// persist name
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.persist(actor);
		entityManager.persist(newName);
		entityManager.flush();
		newName.setActor(actor);
		entityTransaction.commit();
		entityManager.refresh(newName);
		entityManager.refresh(actor);

		// create actor_has_name-table entries
		// entityTransaction.begin();
		// actor.getActorNames().add(newName);
		// newName.getActors3().add(actor);
		// entityManager.merge(newName);
		// entityManager.merge(actor);
		// entityManager.persist(newName);
		// entityManager.persist(actor);
		// entityTransaction.commit();
		// entityManager.refresh(actor);
		// entityManager.refresh(newName);

		// add log entry
		UserLogManager.getLogger()
									// .addLogEntry(newName.getActor().getCreatedByUserAccount().getId(), UserLogManager.LogEvents.ACTORNAMECREATED);
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
																UserLogManager.LogEvents.ACTORNAMECREATED);

		return Response.ok().entity(newName).build();
	}

	@PATCH
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("name/{id}")
	@Secured
	public Response updateName(@PathParam("id") int id, String jsonData) {
		// System.out.println("EndpointActor: updateName - jsonData: " + jsonData);
		ObjectMapper mapper = new ObjectMapper();
		ActorName updatedName = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		ActorName name = entityManager.find(ActorName.class, id);
		if ( name == null ) return Response.status(Status.NOT_FOUND).build();
		// parse JSON data
		try {
			updatedName = mapper.readValue(jsonData, ActorName.class);
		} catch (IOException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( updatedName == null ) return Response.notModified().build();
		// update name
		if ( updatedName.getName() != null ) name.setName(updatedName.getName());
		name.setUsedFrom(updatedName.getUsedFrom());
		name.setUsedUntil(updatedName.getUsedUntil());

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.merge(name);
		entityManager.persist(name);
		entityTransaction.commit();
		entityManager.refresh(name);

		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
																UserLogManager.LogEvents.ACTORNAMEEDITED);
		return Response.ok().entity(name).build();
	}

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("name/{id}")
	@Secured
	public Response deleteName(@PathParam("id") int id) {
		// System.out.println("EndpointActor: deleteName");
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();

		ActorName name = entityManager.find(ActorName.class, id);
		if ( name == null ) return Response.status(Status.NOT_FOUND).build();

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.remove(name);
		entityTransaction.commit();
		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"), UserLogManager.LogEvents.ACTORNAMEDELETED);
		return Response.ok().build();
	}

	@POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
	@Path("address/{id}")
	@Secured
	public Response createAddress(@PathParam("id") int id, String jsonData) {
		// System.out.println("EndpointActor: createAddress: jsonData: "+jsonData);
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		Address newAddress = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();

		// parse JSON data
		try {
			newAddress = mapper.readValue(jsonData, Address.class);
		} catch (IOException e) {
			System.out.println("EndpointActor: createAddress: IOException e !");
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( newAddress == null ) {
			System.out.println("EndpointActor: createAddress: newAddress == null !");
			return Response.status(Status.BAD_REQUEST).build();
		}
		// sanitize object data
		newAddress.setId(0);

		// update log metadata
		// Not necessary, a address will always be created in conjunction with a actor

		// persist address
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.persist(newAddress);
		entityManager.flush();
		entityTransaction.commit();
		entityManager.refresh(newAddress);

		// add log entry
		UserLogManager.getLogger()
									// .addLogEntry(newAddress.getActor().getCreatedByUserAccount().getId(), UserLogManager.LogEvents.ADDRESSCREATED);
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
																UserLogManager.LogEvents.ADDRESSCREATED);

		return Response.ok().entity(newAddress).build();
	}

	@POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
	@Path("{actorId}/address/{id}")
	@Secured
	public Response addAddress(@PathParam("actorId") int actorId, @PathParam("id") int id, String jsonData) {
		// System.out.println("EndpointActor: addAddress: jsonData: "+jsonData);
		ObjectMapper mapper = new ObjectMapper();
		// mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		Address address = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();

		// parse JSON data
		try {
			address = mapper.readValue(jsonData, Address.class);
		} catch (IOException e) {
			System.out.println("EndpointActor: addAddress: IOException e !");
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( address == null ) {
			System.out.println("EndpointActor: addAddress: address == null !");
			return Response.status(Status.BAD_REQUEST).build();
		}
		// sanitize object data
		address.setId(0);

		// Street street = entityManager.find(Street.class, address.getStreetLocation().getLocationId());
		// address.setStreetLocation(street);
		Actor actor = entityManager.find(Actor.class, actorId);

		// update log metadata
		// Not necessary, an address will always be created in conjunction with a actor

		// persist address
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		// entityManager.persist(street);
		entityManager.persist(address);
		entityManager.flush();
		// address.setStreetLocation(street); //! TODO already set above
		entityTransaction.commit();
		entityManager.refresh(address);
		// entityManager.refresh(street);

		// create actor_has_address-table entries
		ActorHasAddress actorHasAddress = new ActorHasAddress(actor, address);
		entityTransaction.begin();
		actor.getActorHasAddresses().add(actorHasAddress);
		address.getActorHasAddresses().add(actorHasAddress);
		entityManager.merge(address);
		entityManager.merge(actor);
		entityManager.persist(actor);
		entityManager.persist(address);
		entityTransaction.commit();
		entityManager.refresh(actor);
		entityManager.refresh(address);

		// add log entry
		UserLogManager.getLogger()
									// .addLogEntry(newAddress.getActor().getCreatedByUserAccount().getId(), UserLogManager.LogEvents.ADDRESSCREATED);
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
																UserLogManager.LogEvents.ADDRESSCREATED);

		return Response.ok().entity(address).build();
	}

	@PATCH
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("address/{id}")
	@Secured
	public Response updateAddress(@PathParam("id") int id, String jsonData) {
		// System.out.println("EndpointActor: updateAddress - jsonData: " + jsonData);
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		Address updatedAddress = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		Address address = entityManager.find(Address.class, id);
		if ( address == null ) return Response.status(Status.NOT_FOUND).build();
		// parse JSON data
		try {
			updatedAddress = mapper.readValue(jsonData, Address.class);
		} catch (IOException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( updatedAddress == null ) return Response.notModified().build();
		// update address
		address.setCity(updatedAddress.getCity());
		address.setPostOfficeBox(updatedAddress.getPostOfficeBox());
		address.setPostalCode(updatedAddress.getPostalCode());
		address.setStreetAddition(updatedAddress.getStreetAddition());
		address.setStreetNumber(updatedAddress.getStreetNumber());
		address.setStreet(updatedAddress.getStreet());
		// TODO once location system is functionable, relink with street_location_id


		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.merge(address);
		entityManager.persist(address);
		entityTransaction.commit();
		entityManager.refresh(address);

		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
															 UserLogManager.LogEvents.ADDRESSEDITED);
		return Response.ok().entity(address).build();
	}

	@PATCH
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("{actorId}/address/{addressId}")
	@Secured
	public Response updateActorHasAddress(@PathParam("actorId") int actorId, @PathParam("addressId") int addressId, String jsonData) {
		// System.out.println("EndpointActor: updateActorHasAddress - jsonData: " + jsonData);
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
    mapper.setSerializationInclusion(Include.NON_NULL);
		ActorHasAddress updatedActorHasAddress = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		Actor actor = entityManager.find(Actor.class, actorId);
		Address address = entityManager.find(Address.class, addressId);
		ActorHasAddress ahakey = new ActorHasAddress(actor, address);
		ActorHasAddress actorHasAddress = entityManager.find(ActorHasAddress.class, ahakey.getId());

		// parse JSON data
		try {
			updatedActorHasAddress = mapper.readValue(jsonData, ActorHasAddress.class);
		} catch (IOException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( updatedActorHasAddress == null ) return Response.notModified().build();

		// update actorHasAddress
		actorHasAddress.setUsedFrom(updatedActorHasAddress.getUsedFrom());
		actorHasAddress.setUsedUntil(updatedActorHasAddress.getUsedUntil());
		actorHasAddress.setAddressType(updatedActorHasAddress.getAddressType());

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.merge(actorHasAddress);
		entityManager.persist(actorHasAddress);
		entityTransaction.commit();
		entityManager.refresh(actorHasAddress);

		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
															 UserLogManager.LogEvents.ADDRESSEDITED);
		return Response.ok().entity(actorHasAddress).build();
	}

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("address/{id}")
	@Secured
	public Response deleteAddress(@PathParam("id") int id) {
		// System.out.println("EndpointActor: deleteAddress");
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();

		Address address = entityManager.find(Address.class, id);
		if ( address == null ) return Response.status(Status.NOT_FOUND).build();

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.remove(address);
		entityTransaction.commit();
		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
															 UserLogManager.LogEvents.ADDRESSDELETED);
		return Response.ok().build();
	}

	@POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
	@Path("emailAddress/{id}")
	@Secured
	public Response createEmailAddress(@PathParam("id") int id, String jsonData) {
		// System.out.println("EndpointActor: createEmailAddress: jsonData: "+jsonData);
		ObjectMapper mapper = new ObjectMapper();
		EmailAddress newEmailAddress = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();

		// parse JSON data
		try {
			newEmailAddress = mapper.readValue(jsonData, EmailAddress.class);
		} catch (IOException e) {
			System.out.println("EndpointActor: createEmailAddress: IOException e !");
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( newEmailAddress == null ) {
			System.out.println("EndpointActor: createEmailAddress: newEmailAddress == null !");
			return Response.status(Status.BAD_REQUEST).build();
		}
		// sanitize object data
		newEmailAddress.setId(0);

		// update log metadata
		// Not necessary, an email address will always be created in conjunction with an actor

		// persist email address
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.persist(newEmailAddress);
		entityManager.flush();
		entityTransaction.commit();
		entityManager.refresh(newEmailAddress);

		// add log entry
		UserLogManager.getLogger()
									// .addLogEntry(newEmailAddress.getActor().getCreatedByUserAccount().getId(), UserLogManager.LogEvents.ADDRESSCREATED);
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
																UserLogManager.LogEvents.EMAILCREATED);

		return Response.ok().entity(newEmailAddress).build();
	}

	@POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
	@Path("{actorId}/emailAddress/{id}")
	@Secured
	public Response addEmailAddress(@PathParam("actorId") int actorId, @PathParam("id") int id, String jsonData) {
		// System.out.println("EndpointActor: addEmailAddress: jsonData: "+jsonData);
		ObjectMapper mapper = new ObjectMapper();
		EmailAddress emailAddress = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();

		// parse JSON data
		try {
			emailAddress = mapper.readValue(jsonData, EmailAddress.class);
		} catch (IOException e) {
			System.out.println("EndpointActor: addEmailAddress: IOException e !");
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( emailAddress == null ) {
			System.out.println("EndpointActor: addEmailAddress: emailAddress == null !");
			return Response.status(Status.BAD_REQUEST).build();
		}
		// sanitize object data
		emailAddress.setId(0);

		Actor actor = entityManager.find(Actor.class, actorId);
		// Actor actor = entityManager.find(Actor.class, actorId);

		// update log metadata
		// Not necessary, a emailAddress will always be created in conjunction with a actor

		// persist emailAddress
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.persist(emailAddress);
		entityManager.flush();
		entityTransaction.commit();
		entityManager.refresh(emailAddress);

		// create actor_has_emailAddress-table entries
		ActorHasEmailAddress actorHasEmailAddress = new ActorHasEmailAddress(actor, emailAddress);
		entityTransaction.begin();
		actor.getActorHasEmailAddresses().add(actorHasEmailAddress);
		emailAddress.getActorHasEmailAddresses().add(actorHasEmailAddress);
		entityManager.merge(emailAddress);
		entityManager.merge(actor);
		entityManager.persist(actor);
		entityManager.persist(emailAddress);
		entityTransaction.commit();
		entityManager.refresh(actor);
		entityManager.refresh(emailAddress);

		// add log entry
		UserLogManager.getLogger()
									// .addLogEntry(newEmailAddress.getActor().getCreatedByUserAccount().getId(), UserLogManager.LogEvents.ADDRESSCREATED);
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
																UserLogManager.LogEvents.EMAILCREATED);

		return Response.ok().entity(emailAddress).build();
	}

	@PATCH
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("emailAddress/{id}")
	@Secured
	public Response updateEmailAddress(@PathParam("id") int id, String jsonData) {
		// System.out.println("EndpointActor: updateEmailAddress - jsonData: " + jsonData);
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		EmailAddress updatedEmailAddress = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		EmailAddress emailAddress = entityManager.find(EmailAddress.class, id);
		if ( emailAddress == null ) return Response.status(Status.NOT_FOUND).build();
		// parse JSON data
		try {
			updatedEmailAddress = mapper.readValue(jsonData, EmailAddress.class);
		} catch (IOException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( updatedEmailAddress == null ) return Response.notModified().build();
		// update emailAddress
		if ( updatedEmailAddress.getEmail() != null) emailAddress.setEmail(updatedEmailAddress.getEmail());

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.merge(emailAddress);
		entityManager.persist(emailAddress);
		entityTransaction.commit();
		entityManager.refresh(emailAddress);

		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext
									.getProperty("TIMAAT.userID"), UserLogManager.LogEvents.EMAILEDITED);
		return Response.ok().entity(emailAddress).build();
	}

	@PATCH
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("{actorId}/emailAddress/{emailAddressId}")
	@Secured
	public Response updateActorHasEmailAddress(@PathParam("actorId") int actorId, @PathParam("emailAddressId") int emailAddressId, String jsonData) {
		// System.out.println("EndpointActor: updateActorHasEmailAddress - jsonData: " + jsonData);
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
    mapper.setSerializationInclusion(Include.NON_NULL);
		ActorHasEmailAddress updatedActorHasEmailAddress = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		Actor actor = entityManager.find(Actor.class, actorId);
		EmailAddress emailAddress = entityManager.find(EmailAddress.class, emailAddressId);
		ActorHasEmailAddress aheakey = new ActorHasEmailAddress(actor, emailAddress);
		ActorHasEmailAddress actorHasEmailAddress = entityManager.find(ActorHasEmailAddress.class, aheakey.getId());

		// parse JSON data
		try {
			updatedActorHasEmailAddress = mapper.readValue(jsonData, ActorHasEmailAddress.class);
		} catch (IOException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( updatedActorHasEmailAddress == null ) return Response.notModified().build();

		// update actorHasEmailAddress
		actorHasEmailAddress.setEmailAddressType(updatedActorHasEmailAddress.getEmailAddressType());

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.merge(actorHasEmailAddress);
		entityManager.persist(actorHasEmailAddress);
		entityTransaction.commit();
		entityManager.refresh(actorHasEmailAddress);

		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext
									.getProperty("TIMAAT.userID"), UserLogManager.LogEvents.EMAILEDITED);
		return Response.ok().entity(actorHasEmailAddress).build();
	}

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("emailAddress/{id}")
	@Secured
	public Response deleteEmailAddress(@PathParam("id") int id) {
		// System.out.println("EndpointActor: deleteEmailAddress");
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();

		EmailAddress emailAddress = entityManager.find(EmailAddress.class, id);
		if ( emailAddress == null ) return Response.status(Status.NOT_FOUND).build();

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.remove(emailAddress);
		entityTransaction.commit();
		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext
									.getProperty("TIMAAT.userID"), UserLogManager.LogEvents.EMAILDELETED);
		return Response.ok().build();
	}

	@POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
	@Path("phoneNumber/{id}")
	@Secured
	public Response createPhoneNumber(@PathParam("id") int id, String jsonData) {
		// System.out.println("EndpointActor: createPhoneNumber: jsonData: "+jsonData);
		ObjectMapper mapper = new ObjectMapper();
		PhoneNumber newPhoneNumber = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();

		// parse JSON data
		try {
			newPhoneNumber = mapper.readValue(jsonData, PhoneNumber.class);
		} catch (IOException e) {
			System.out.println("EndpointActor: createPhoneNumber: IOException e !");
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( newPhoneNumber == null ) {
			System.out.println("EndpointActor: createPhoneNumber: newPhoneNumber == null !");
			return Response.status(Status.BAD_REQUEST).build();
		}
		// sanitize object data
		newPhoneNumber.setId(0);

		// update log metadata
		// Not necessary, an email address will always be created in conjunction with an actor

		// persist email address
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.persist(newPhoneNumber);
		entityManager.flush();
		entityTransaction.commit();
		entityManager.refresh(newPhoneNumber);

		// add log entry
		UserLogManager.getLogger()
									// .addLogEntry(newPhoneNumber.getActor().getCreatedByUserAccount().getId(), UserLogManager.LogEvents.ADDRESSCREATED);
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
																UserLogManager.LogEvents.PHONENUMBERCREATED);

		return Response.ok().entity(newPhoneNumber).build();
	}

	@POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
	@Path("{actorId}/phoneNumber/{id}")
	@Secured
	public Response addPhoneNumber(@PathParam("actorId") int actorId, @PathParam("id") int id, String jsonData) {
		// System.out.println("EndpointActor: addPhoneNumber: jsonData: "+jsonData);
		ObjectMapper mapper = new ObjectMapper();
		PhoneNumber phoneNumber = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();

		// parse JSON data
		try {
			phoneNumber = mapper.readValue(jsonData, PhoneNumber.class);
		} catch (IOException e) {
			System.out.println("EndpointActor: addPhoneNumber: IOException e !");
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( phoneNumber == null ) {
			System.out.println("EndpointActor: addPhoneNumber: phoneNumber == null !");
			return Response.status(Status.BAD_REQUEST).build();
		}
		// sanitize object data
		phoneNumber.setId(0);

		Actor actor = entityManager.find(Actor.class, actorId);
		// Actor actor = entityManager.find(Actor.class, actorId);

		// update log metadata
		// Not necessary, a phoneNumber will always be created in conjunction with a actor

		// persist phoneNumber
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.persist(phoneNumber);
		entityManager.flush();
		entityTransaction.commit();
		entityManager.refresh(phoneNumber);

		// create actor_has_phoneNumber-table entries
		ActorHasPhoneNumber actorHasPhoneNumber = new ActorHasPhoneNumber(actor, phoneNumber);
		entityTransaction.begin();
		actor.getActorHasPhoneNumbers().add(actorHasPhoneNumber);
		phoneNumber.getActorHasPhoneNumbers().add(actorHasPhoneNumber);
		entityManager.merge(phoneNumber);
		entityManager.merge(actor);
		entityManager.persist(actor);
		entityManager.persist(phoneNumber);
		entityTransaction.commit();
		entityManager.refresh(actor);
		entityManager.refresh(phoneNumber);

		// add log entry
		UserLogManager.getLogger()
									// .addLogEntry(newPhoneNumber.getActor().getCreatedByUserAccount().getId(), UserLogManager.LogEvents.ADDRESSCREATED);
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
																UserLogManager.LogEvents.PHONENUMBERCREATED);

		return Response.ok().entity(phoneNumber).build();
	}

	@PATCH
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("phoneNumber/{id}")
	@Secured
	public Response updatePhoneNumber(@PathParam("id") int id, String jsonData) {
		// System.out.println("EndpointActor: updatePhoneNumber - jsonData: " + jsonData);
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		PhoneNumber updatedPhoneNumber = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		PhoneNumber phoneNumber = entityManager.find(PhoneNumber.class, id);
		if ( phoneNumber == null ) return Response.status(Status.NOT_FOUND).build();
		// parse JSON data
		try {
			updatedPhoneNumber = mapper.readValue(jsonData, PhoneNumber.class);
		} catch (IOException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( updatedPhoneNumber == null ) return Response.notModified().build();
		// update phoneNumber
		if ( updatedPhoneNumber.getPhoneNumber() != null ) phoneNumber.setPhoneNumber(updatedPhoneNumber.getPhoneNumber());

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.merge(phoneNumber);
		entityManager.persist(phoneNumber);
		entityTransaction.commit();
		entityManager.refresh(phoneNumber);

		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
																UserLogManager.LogEvents.PHONENUMBEREDITED);
		return Response.ok().entity(phoneNumber).build();
	}

	@PATCH
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("{actorId}/phoneNumber/{phoneNumberId}")
	@Secured
	public Response updateActorHasPhoneNumber(@PathParam("actorId") int actorId, @PathParam("phoneNumberId") int phoneNumberId, String jsonData) {
		// System.out.println("EndpointActor: updateActorHasPhoneNumber - jsonData: " + jsonData);
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
    mapper.setSerializationInclusion(Include.NON_NULL);
		ActorHasPhoneNumber updatedActorHasPhoneNumber = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		Actor actor = entityManager.find(Actor.class, actorId);
		PhoneNumber phoneNumber = entityManager.find(PhoneNumber.class, phoneNumberId);
		ActorHasPhoneNumber ahpnkey = new ActorHasPhoneNumber(actor, phoneNumber);
		ActorHasPhoneNumber actorHasPhoneNumber = entityManager.find(ActorHasPhoneNumber.class, ahpnkey.getId());

		// parse JSON data
		try {
			updatedActorHasPhoneNumber = mapper.readValue(jsonData, ActorHasPhoneNumber.class);
		} catch (IOException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( updatedActorHasPhoneNumber == null ) return Response.notModified().build();

		// update actorHasPhoneNumber
		actorHasPhoneNumber.setPhoneNumberType(updatedActorHasPhoneNumber.getPhoneNumberType());

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.merge(actorHasPhoneNumber);
		entityManager.persist(actorHasPhoneNumber);
		entityTransaction.commit();
		entityManager.refresh(actorHasPhoneNumber);

		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
																UserLogManager.LogEvents.PHONENUMBEREDITED);
		return Response.ok().entity(actorHasPhoneNumber).build();
	}

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("phoneNumber/{id}")
	@Secured
	public Response deletePhoneNumber(@PathParam("id") int id) {
		// System.out.println("EndpointActor: deletePhoneNumber");
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();

		PhoneNumber phoneNumber = entityManager.find(PhoneNumber.class, id);
		if ( phoneNumber == null ) return Response.status(Status.NOT_FOUND).build();

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.remove(phoneNumber);
		entityTransaction.commit();
		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
																UserLogManager.LogEvents.PHONENUMBERDELETED);
		return Response.ok().build();
	}

	@POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
	@Path("{actorId}/personIsMemberOfCollective/{collectiveId}")
	@Secured
	public Response addPersonIsMemberOfCollective(@PathParam("actorId") int actorId,
																								@PathParam("collectiveId") int collectiveId) throws IOException {
		// System.out.println("EndpointActor: addPersonIsMemberOfCollective:");
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		ActorPerson person = entityManager.find(ActorPerson.class, actorId);
		if (person == null) return Response.status(Status.NOT_FOUND).build();
		ActorCollective collective = entityManager.find(ActorCollective.class, collectiveId);
		if (collective == null) return Response.status(Status.NOT_FOUND).build();
		ActorPersonIsMemberOfActorCollective apimoac = new ActorPersonIsMemberOfActorCollective(person, collective);
		// ActorPersonIsMemberOfActorCollective apimoac = null;
		// try {
		// 	 apimoac = mapper.readValue(jsonData, ActorPersonIsMemberOfActorCollective.class);
		// } catch (IOException e) {
		// 	e.printStackTrace();
		// 	return Response.status(Status.BAD_REQUEST).build();
		// }

		// sanitize object data

		// update log metadata

		// create actor_has_address-table entries
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		person.getActorPersonIsMemberOfActorCollectives().add(apimoac);
		collective.getActorPersonIsMemberOfActorCollectives().add(apimoac);
		entityManager.merge(collective);
		entityManager.merge(person);
		entityManager.persist(person);
		entityManager.persist(collective);
		entityTransaction.commit();
		entityManager.refresh(person);
		entityManager.refresh(collective);

		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
																UserLogManager.LogEvents.MEMBERSHIPCREATED);

		return Response.ok().entity(apimoac).build();
	}

	@PATCH
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("{actorId}/personIsMemberOfCollective/{collectiveId}")
	@Secured
	public Response updatePersonIsMemberOfCollective(@PathParam("actorId") int actorId,
																									 @PathParam("collectiveId") int collectiveId,
																									 String jsonData) {
		// System.out.println("EndpointActor: updateActorPersonIsMemberOfActorCollective - jsonData: " + jsonData);
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
    mapper.setSerializationInclusion(Include.NON_NULL);
		ActorPersonIsMemberOfActorCollective updatedActorPersonIsMemberOfActorCollective = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		ActorPerson person = entityManager.find(ActorPerson.class, actorId);
		ActorCollective collective = entityManager.find(ActorCollective.class, collectiveId);
		if ( person == null || collective == null) return Response.status(Status.NOT_FOUND).build();

		ActorPersonIsMemberOfActorCollective apimoacKey = new ActorPersonIsMemberOfActorCollective(person, collective);
		ActorPersonIsMemberOfActorCollective apimoac = entityManager.find(ActorPersonIsMemberOfActorCollective.class, apimoacKey.getId());
		// parse JSON data
		try {
			updatedActorPersonIsMemberOfActorCollective = mapper.readValue(jsonData, ActorPersonIsMemberOfActorCollective.class);
		} catch (IOException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( updatedActorPersonIsMemberOfActorCollective == null ) return Response.notModified().build();

		// update actorPersonIsMemberOfActorCollective
		apimoac.setActorCollective(collective);

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.merge(apimoac);
		entityManager.persist(apimoac);
		entityTransaction.commit();
		entityManager.refresh(apimoac);

		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
																			UserLogManager.LogEvents.MEMBERSHIPEDITED);
		return Response.ok().entity(apimoac).build();
	}

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{personId}/personIsMemberOfCollective/{collectiveId}")
	@Secured
	public Response deletePersonIsMemberOfCollective(@PathParam("personId") int personId,
																									 @PathParam("collectiveId") int collectiveId) {
		// System.out.println("EndpointActor: deletePersonIsMemberOfCollective");
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();

		ActorPerson person = entityManager.find(ActorPerson.class, personId);
		ActorCollective collective = entityManager.find(ActorCollective.class, collectiveId);
		ActorPersonIsMemberOfActorCollective apimoacKey = new ActorPersonIsMemberOfActorCollective(person, collective);
		ActorPersonIsMemberOfActorCollective apimoac = entityManager.find(ActorPersonIsMemberOfActorCollective.class, apimoacKey.getId());

		if ( person == null || collective == null) return Response.status(Status.NOT_FOUND).build();

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.remove(apimoac);
		entityTransaction.commit();
		entityManager.refresh(person);
		entityManager.refresh(collective);

		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
																UserLogManager.LogEvents.MEMBERSHIPDELETED);
		return Response.ok().build();
	}

	@POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
	@Path("{personId}/{collectiveId}/membershipDetails/{id}")
	@Secured
	public Response addMembershipDetail(@PathParam("personId") int personId,
																			@PathParam("collectiveId") int collectiveId,
																			@PathParam("id") int id,
																			String jsonData) {
		// System.out.println("EndpointActor: addMembership: jsonData: "+jsonData);
		ObjectMapper mapper = new ObjectMapper();
		MembershipDetail newMembershipDetail = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		// parse JSON data
		try {
			newMembershipDetail = mapper.readValue(jsonData, MembershipDetail.class);
		} catch (IOException e) {
			System.out.println("EndpointActor: addMembership: IOException e !");
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( newMembershipDetail == null ) {
			System.out.println("EndpointActor: addMembership: newMembership == null !");
			return Response.status(Status.BAD_REQUEST).build();
		}
		// sanitize object data
		newMembershipDetail.setId(0);
		// Language language = entityManager.find(Language.class, newMembership.getLanguage().getId());
		// newMembership.setLanguage(language);
		ActorPerson person = entityManager.find(ActorPerson.class, personId);
		ActorCollective collective = entityManager.find(ActorCollective.class, collectiveId);
		ActorPersonIsMemberOfActorCollective apimoackey = new ActorPersonIsMemberOfActorCollective(person, collective);
		ActorPersonIsMemberOfActorCollective apimoac = entityManager.find(ActorPersonIsMemberOfActorCollective.class, apimoackey.getId());
		newMembershipDetail.setActorPersonIsMemberOfActorCollective(apimoac);

		// update log metadata
		// Not necessary, a membership will always be created in conjunction with a actor

		// persist membership
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.persist(apimoac);
		entityManager.persist(newMembershipDetail);
		entityManager.flush();
		newMembershipDetail.setActorPersonIsMemberOfActorCollective(apimoac);
		entityTransaction.commit();
		entityManager.refresh(newMembershipDetail);
		entityManager.refresh(apimoac);

		// add log entry
		// always in conjunction with personIsMemberOfCollective
		// UserLogManager.getLogger()
		// 							.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
		// 														UserLogManager.LogEvents.MEMBERSHIPCREATED);

		return Response.ok().entity(newMembershipDetail).build();
	}

	@PATCH
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("membershipDetails/{id}")
	@Secured
	public Response updateMembershipDetail(@PathParam("id") int id,
																	 			 String jsonData) {
		// System.out.println("EndpointActor: updateMembership - jsonData: " + jsonData);
		ObjectMapper mapper = new ObjectMapper();
		MembershipDetail updatedMembership = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		MembershipDetail membershipDetail = entityManager.find(MembershipDetail.class, id);
		if ( membershipDetail == null ) return Response.status(Status.NOT_FOUND).build();
		// parse JSON data
		try {
			updatedMembership = mapper.readValue(jsonData, MembershipDetail.class);
		} catch (IOException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( updatedMembership == null ) return Response.notModified().build();
		// update membership
		// membership.setRole(updatedMembership.getRole()); // TODO connect role
		membershipDetail.setJoinedAt(updatedMembership.getJoinedAt());
		membershipDetail.setLeftAt(updatedMembership.getLeftAt());

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.merge(membershipDetail);
		entityManager.persist(membershipDetail);
		entityTransaction.commit();
		entityManager.refresh(membershipDetail);

		// add log entry
		// UserLogManager.getLogger()
		// 							.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
		// 														UserLogManager.LogEvents.MEMBERSHIPEDITED);
		return Response.ok().entity(membershipDetail).build();
	}

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("membershipDetails/{id}")
	@Secured
	public Response deleteMembershipDetail(@PathParam("id") int id) {
		// System.out.println("EndpointActor: deleteMembership");
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();

		MembershipDetail membershipDetail = entityManager.find(MembershipDetail.class, id);
		if ( membershipDetail == null ) return Response.status(Status.NOT_FOUND).build();

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.remove(membershipDetail);
		entityTransaction.commit();
		// add log entry
		// UserLogManager.getLogger()
		// 							.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"), UserLogManager.LogEvents.MEMBERSHIPDELETED);
		return Response.ok().build();
	}

	@POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
	@Path("citizenship/{id}/{languageId}")
	@Secured
	public Response createCitizenship(@PathParam("id") int id, String jsonData, @PathParam("languageId") int languageId) {
		// System.out.println("EndpointActor: createCitizenship: jsonData: "+jsonData);
		ObjectMapper mapper = new ObjectMapper();
		Citizenship newCitizenship = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();

		// parse JSON data
		// Language language = entityManager.find(Language.class, languageId);
		// if ( language == null ) return Response.status(Status.NOT_FOUND).build();
		try {
			newCitizenship = mapper.readValue(jsonData, Citizenship.class);
		} catch (IOException e) {
			System.out.println("EndpointActor: createCitizenship: IOException e !");
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( newCitizenship == null ) {
			System.out.println("EndpointActor: createCitizenship: newCitizenship == null !");
			return Response.status(Status.BAD_REQUEST).build();
		}
		// sanitize object data
		newCitizenship.setId(0);

		// update log metadata
		// Not necessary, a citizenship will always be created in conjunction with a actor

		// persist citizenship
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.persist(newCitizenship);
		entityManager.flush();
		entityTransaction.commit();
		entityManager.refresh(newCitizenship);

		// CitizenshipTranslation citizenshipTranslation = entityManager.find(CitizenshipTranslation.class, languageId);
		// Language language = entityManager.find(Language.class, citizenshipTranslation.getLanguage().getId());
		// citizenshipTranslation.setLanguage(language);
		// newCitizenship.getCitizenshipTranslations().add(citizenshipTranslation);

		// persist citizenship_translation
		// EntityTransaction entityTransaction = entityManager.getTransaction();
		// entityTransaction.begin();
		// entityManager.persist(language);
		// entityManager.persist(citizenshipTranslation);
		// entityManager.persist(newCitizenship);
		// entityManager.flush();
		// citizenshipTranslation.setLanguage(language);
		// newCitizenship.getCitizenshipTranslations().add(citizenshipTranslation);
		// entityTransaction.commit();
		// entityManager.refresh(newCitizenship);
		// entityManager.refresh(citizenshipTranslation);
		// entityManager.refresh(language);

		// add log entry
		UserLogManager.getLogger()
									// .addLogEntry(newCitizenship.getActor().getCreatedByUserAccount().getId(), UserLogManager.LogEvents.ADDRESSCREATED);
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
																UserLogManager.LogEvents.CITIZENSHIPCREATED);

		return Response.ok().entity(newCitizenship).build();
	}

	@POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
	@Path("{actorId}/citizenship/{id}")
	@Secured
	public Response addCitizenship(@PathParam("actorId") int actorId, @PathParam("id") int id, String jsonData) {
		// System.out.println("EndpointActor: addCitizenship: jsonData: "+jsonData);
		ObjectMapper mapper = new ObjectMapper();
		Citizenship citizenship = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();

		// parse JSON data
		try {
			citizenship = mapper.readValue(jsonData, Citizenship.class);
		} catch (IOException e) {
			System.out.println("EndpointActor: addCitizenship: IOException e !");
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( citizenship == null ) {
			System.out.println("EndpointActor: addCitizenship: citizenship == null !");
			return Response.status(Status.BAD_REQUEST).build();
		}
		// sanitize object data
		citizenship.setId(0);

		// Location location = entityManager.find(Location.class, citizenship.getCountries().getLocationId());
		// citizenship.setLocation(location);

		// update log metadata
		// Not necessary, a citizenship will always be created in conjunction with a actor

		// persist citizenship
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.persist(citizenship);
		entityManager.flush();
		entityTransaction.commit();
		entityManager.refresh(citizenship);

		ActorPerson actorPerson = entityManager.find(ActorPerson.class, actorId);
		citizenship.getPersons().add(actorPerson);

		// persist citizenship
		// EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		// actorPerson.getCitizenships().add(citizenship); // TODO reactivate once citizenship is not a String value anymore
		citizenship.getPersons().add(actorPerson);
		entityManager.merge(citizenship);
		entityManager.merge(actorPerson);
		entityManager.persist(citizenship);
		entityManager.persist(actorPerson);
		entityTransaction.commit();
		entityManager.refresh(actorPerson);
		entityManager.refresh(citizenship);

		// add log entry
		UserLogManager.getLogger()
									// .addLogEntry(newCitizenship.getActor().getCreatedByUserAccount().getId(), UserLogManager.LogEvents.ADDRESSCREATED);
									.addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
																UserLogManager.LogEvents.CITIZENSHIPCREATED);

		return Response.ok().entity(citizenship).build();
	}

	@PATCH
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("citizenship/{id}")
	@Secured
	public Response updateCitizenship(@PathParam("id") int id, String jsonData) {
		// System.out.println("EndpointActor: update citizenship - jsonData: " + jsonData);
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		Citizenship updatedCitizenship = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		Citizenship citizenship = entityManager.find(Citizenship.class, id);
		if ( citizenship == null ) return Response.status(Status.NOT_FOUND).build();
		// parse JSON data
		try {
			updatedCitizenship = mapper.readValue(jsonData, Citizenship.class);
		} catch (IOException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( updatedCitizenship == null ) return Response.notModified().build();
		// update citizenship

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.merge(citizenship);
		entityManager.persist(citizenship);
		entityTransaction.commit();
		entityManager.refresh(citizenship);

		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext
									.getProperty("TIMAAT.userID"), UserLogManager.LogEvents.CITIZENSHIPEDITED);
		return Response.ok().entity(citizenship).build();
	}

	@PATCH
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("citizenship/{id}/{languageId}")
	@Secured
	public Response updateCitizenshipTranslation(@PathParam("id") int id, @PathParam("languageId") int languageId, String jsonData) {
		// System.out.println("EndpointActor: updateCitizenshipTranslation - jsonData: " + jsonData);
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		CitizenshipTranslation updatedCitizenshipTranslation = null;
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();
		CitizenshipTranslation citizenshipTranslation = entityManager.find(CitizenshipTranslation.class, id);
		if ( citizenshipTranslation == null ) return Response.status(Status.NOT_FOUND).build();

		// parse JSON data
		try {
			updatedCitizenshipTranslation = mapper.readValue(jsonData, CitizenshipTranslation.class);
		} catch (IOException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if ( updatedCitizenshipTranslation == null ) return Response.notModified().build();
		// update citizenship
		if (updatedCitizenshipTranslation.getName() != null) citizenshipTranslation.setName(updatedCitizenshipTranslation.getName());
		// if (updatedCitizenshipTranslation.getLanguage() != null) citizenshipTranslation.setLanguage(updatedCitizenshipTranslation.getLanguage()); // TODO check if useful to change language

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.merge(citizenshipTranslation);
		entityManager.persist(citizenshipTranslation);
		entityTransaction.commit();
		entityManager.refresh(citizenshipTranslation);

		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext
									.getProperty("TIMAAT.userID"), UserLogManager.LogEvents.CITIZENSHIPEDITED);
		return Response.ok().entity(citizenshipTranslation).build();
	}

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("citizenship/{id}")
	@Secured
	public Response deleteCitizenship(@PathParam("id") int id) {
		// System.out.println("EndpointActor: deleteCitizenship");
		EntityManager entityManager = TIMAATApp.emf.createEntityManager();

		Citizenship citizenship = entityManager.find(Citizenship.class, id);
		if ( citizenship == null ) return Response.status(Status.NOT_FOUND).build();

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.remove(citizenship);
		entityTransaction.commit();
		// add log entry
		UserLogManager.getLogger()
									.addLogEntry((int) containerRequestContext
									.getProperty("TIMAAT.userID"), UserLogManager.LogEvents.ADDRESSDELETED);
		return Response.ok().build();
	}

	@GET
	@Path("profileImage/{imageId}")
	@Produces("image/png")
	public Response getProfileImage(@PathParam("imageId") int imageId,
																	@QueryParam("token") String fileToken) {
		// verify token
		if ( fileToken == null ) return Response.status(401).build();
		int tokenMediumID = 0;
		try {
			tokenMediumID = EndpointMedium.validateFileToken(fileToken);
		} catch (Exception e) {
			return Response.status(401).build();
		}
		if ( tokenMediumID != imageId ) return Response.status(401).build();

		// load profile image from storage
		File profileImage = new File(TIMAATApp.timaatProps.getProp(PropertyConstants.STORAGE_LOCATION)
			// + "medium/image/" + imageId + "/" + imageId + "-audio-all.png");
			+ "medium/image/" + imageId + "/" + imageId + "-image-scaled.png");
		// if ( !profileImage.exists() ) {
		// 	// try to create waveform
		// 	createAudioWaveform(id, TIMAATApp.timaatProps.getProp(PropertyConstants.STORAGE_LOCATION)
		// 		+ "medium/video/" + id + "-video-original.mp4");
		// 	thumbnail = new File(TIMAATApp.timaatProps.getProp(PropertyConstants.STORAGE_LOCATION)
		// 		+ "medium/video/" + id + "/" + id + "-audio-all.png");
		// }
		if ( !profileImage.exists() || !profileImage.canRead() ) profileImage = new File(servletContext.getRealPath("img/preview-placeholder.png"));

		return Response.ok().entity(profileImage).build();
	}

	@POST
  @Produces(MediaType.APPLICATION_JSON)
	@Path("{actorId}/tag/{tagId}")
	@Secured
	public Response addExistingTag(@PathParam("actorId") int actorId,
																 @PathParam("tagId") int tagId) {


    	EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    	Actor actor = entityManager.find(Actor.class, actorId);
			if ( actor == null ) return Response.status(Status.NOT_FOUND).build();
			Tag tag = entityManager.find(Tag.class, tagId);
			if ( tag == null ) return Response.status(Status.NOT_FOUND).build();

        // attach tag to annotation and vice versa
    		EntityTransaction entityTransaction = entityManager.getTransaction();
    		entityTransaction.begin();
    		actor.getTags().add(tag);
    		tag.getActors().add(actor);
    		entityManager.merge(tag);
    		entityManager.merge(actor);
    		entityManager.persist(actor);
    		entityManager.persist(tag);
    		entityTransaction.commit();
    		entityManager.refresh(actor);

		return Response.ok().entity(tag).build();
	}

	@DELETE
  @Produces(MediaType.APPLICATION_JSON)
	@Path("{actorId}/tag/{tagId}")
	@Secured
	public Response removeTag(@PathParam("actorId") int actorId,
														@PathParam("tagId") int tagId) {

    	EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    	Actor actor = entityManager.find(Actor.class, actorId);
			if ( actor == null ) return Response.status(Status.NOT_FOUND).build();
			Tag tag = entityManager.find(Tag.class, tagId);
    	if ( tag == null ) return Response.status(Status.NOT_FOUND).build();

        	// attach tag to annotation and vice versa
    		EntityTransaction entityTransaction = entityManager.getTransaction();
    		entityTransaction.begin();
    		actor.getTags().remove(tag);
    		tag.getActors().remove(actor);
    		entityManager.merge(tag);
    		entityManager.merge(actor);
    		entityManager.persist(actor);
    		entityManager.persist(tag);
    		entityTransaction.commit();
    		entityManager.refresh(actor);

		return Response.ok().build();
	}

	// @POST
  //   @Produces(MediaType.APPLICATION_JSON)
	// @Path("{id}/tag/{name}") // 	@Path("actor/{id}/tag/{name}")
	// @Secured
	// public Response addTag(@PathParam("id") int id, @PathParam("name") String tagName) {
  //   	EntityManager entityManager = TIMAATApp.emf.createEntityManager();
  //   	Actor actor = entityManager.find(Actor.class, id);
  //   	if ( actor == null ) return Response.status(Status.NOT_FOUND).build();
  //   	// check if tag exists
  //   	Tag tag = null;
  //   	List<Tag> tags = null;
  //   	try {
  //       	tags = (List<Tag>) entityManager.createQuery("SELECT t from Tag t WHERE t.name=:name")
  //       			.setParameter("name", tagName)
  //       			.getResultList();
  //   	} catch(Exception e) {};
  //   	// find tag case sensitive
  //   	for ( Tag listTag : tags )
  //   		if ( listTag.getName().compareTo(tagName) == 0 ) tag = listTag;
  //   	// create tag if it doesn't exist yet
  //   	if ( tag == null ) {
  //   		tag = new Tag();
  //   		tag.setName(tagName);
  //   		EntityTransaction entityTransaction = entityManager.getTransaction();
  //   		entityTransaction.begin();
  //   		entityManager.persist(tag);
  //   		entityTransaction.commit();
  //   		entityManager.refresh(tag);
  //   	}
    	// // check if actor already has tag
    	// if ( !actor.getTags().contains(tag) ) {
      //   	// attach tag to actor and vice versa
    	// 	EntityTransaction entityTransaction = entityManager.getTransaction();
    	// 	entityTransaction.begin();
    	// 	actor.getTags().add(tag);
    	// 	tag.getActors().add(actor);
    	// 	entityManager.merge(tag);
    	// 	entityManager.merge(actor);
    	// 	entityManager.persist(actor);
    	// 	entityManager.persist(tag);
    	// 	entityTransaction.commit();
    	// 	entityManager.refresh(actor);
    	// }
	// 	return Response.ok().entity(tag).build();
	// }

	// @DELETE
  //   @Produces(MediaType.APPLICATION_JSON)
	// @Path("{id}/tag/{name}")
	// @Secured
	// public Response removeTag(@PathParam("id") int id, @PathParam("name") String tagName) {
  //   	EntityManager entityManager = TIMAATApp.emf.createEntityManager();
  //   	Actor actor = entityManager.find(Actor.class, id);
  //   	if ( actor == null ) return Response.status(Status.NOT_FOUND).build();
  //   	// check if actor already has tag
  //   	Tag tag = null;
  //   	for ( Tag actorTag:actor.getTags() ) {
  //   		if ( actorTag.getName().compareTo(tagName) == 0 ) tag = actorTag;
  //   	}
  //   	if ( tag != null ) {
  //       	// attach tag to actor and vice versa
  //   		EntityTransaction entityTransaction = entityManager.getTransaction();
  //   		entityTransaction.begin();
  //   		actor.getTags().remove(tag);
  //   		tag.getActors().remove(actor);
  //   		entityManager.merge(tag);
  //   		entityManager.merge(actor);
  //   		entityManager.persist(actor);
  //   		entityManager.persist(tag);
  //   		entityTransaction.commit();
  //   		entityManager.refresh(actor);
  //   	}
	// 	return Response.ok().build();
	// }

	public static <T> List<T> castList(Class<? extends T> clazz, Collection<?> c) {
    List<T> r = new ArrayList<T>(c.size());
    for(Object o: c)
      r.add(clazz.cast(o));
    return r;
	}

}
