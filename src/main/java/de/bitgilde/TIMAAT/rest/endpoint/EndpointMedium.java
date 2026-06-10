package de.bitgilde.TIMAAT.rest.endpoint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bitgilde.TIMAAT.PropertyConstants;
import de.bitgilde.TIMAAT.SelectElement;
import de.bitgilde.TIMAAT.SelectElementWithToken;
import de.bitgilde.TIMAAT.TIMAATApp;
import de.bitgilde.TIMAAT.model.DataTableInfo;
import de.bitgilde.TIMAAT.model.FIPOP.Actor;
import de.bitgilde.TIMAAT.model.FIPOP.AudioPostProduction;
import de.bitgilde.TIMAAT.model.FIPOP.AudioPostProductionTranslation;
import de.bitgilde.TIMAAT.model.FIPOP.Category;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySet;
import de.bitgilde.TIMAAT.model.FIPOP.CategorySetHasCategory;
import de.bitgilde.TIMAAT.model.FIPOP.Language;
import de.bitgilde.TIMAAT.model.FIPOP.MediaType;
import de.bitgilde.TIMAAT.model.FIPOP.Medium;
import de.bitgilde.TIMAAT.model.FIPOP.MediumAnalysisList;
import de.bitgilde.TIMAAT.model.FIPOP.MediumAudio;
import de.bitgilde.TIMAAT.model.FIPOP.MediumAudioAnalysis;
import de.bitgilde.TIMAAT.model.FIPOP.MediumDocument;
import de.bitgilde.TIMAAT.model.FIPOP.MediumHasActorWithRole;
import de.bitgilde.TIMAAT.model.FIPOP.MediumHasLanguage;
import de.bitgilde.TIMAAT.model.FIPOP.MediumHasMusic;
import de.bitgilde.TIMAAT.model.FIPOP.MediumImage;
import de.bitgilde.TIMAAT.model.FIPOP.MediumLanguageType;
import de.bitgilde.TIMAAT.model.FIPOP.MediumSoftware;
import de.bitgilde.TIMAAT.model.FIPOP.MediumText;
import de.bitgilde.TIMAAT.model.FIPOP.MediumVideo;
import de.bitgilde.TIMAAT.model.FIPOP.MediumVideogame;
import de.bitgilde.TIMAAT.model.FIPOP.Role;
import de.bitgilde.TIMAAT.model.FIPOP.Source;
import de.bitgilde.TIMAAT.model.FIPOP.Tag;
import de.bitgilde.TIMAAT.model.FIPOP.Title;
import de.bitgilde.TIMAAT.model.FIPOP.Transcription;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount;
import de.bitgilde.TIMAAT.model.TimeRange;
import de.bitgilde.TIMAAT.model.fileInformation.AudioInformation;
import de.bitgilde.TIMAAT.model.fileInformation.ImageInformation;
import de.bitgilde.TIMAAT.model.fileInformation.VideoInformation;
import de.bitgilde.TIMAAT.processing.audio.api.FrequencyInformation;
import de.bitgilde.TIMAAT.processing.audio.io.FrequencyFileReader;
import de.bitgilde.TIMAAT.processing.audio.io.WaveformBinaryFileReader;
import de.bitgilde.TIMAAT.processing.video.FfmpegVideoEngine;
import de.bitgilde.TIMAAT.processing.video.exception.VideoEngineException;
import de.bitgilde.TIMAAT.rest.RangedStreamingOutput;
import de.bitgilde.TIMAAT.rest.Secured;
import de.bitgilde.TIMAAT.rest.filter.AuthenticationFilter;
import de.bitgilde.TIMAAT.rest.model.medium.MediumListingQueryParameter;
import de.bitgilde.TIMAAT.rest.model.medium.UpdateMediumDefaultTranscriptionPayload;
import de.bitgilde.TIMAAT.rest.model.medium.UpdateMediumHasMusicListPayload;
import de.bitgilde.TIMAAT.rest.model.medium.UpdateMediumHasMusicListPayload.MediumHasMusicListEntry;
import de.bitgilde.TIMAAT.rest.model.medium.UpdateMediumVideoThumbnailPayload;
import de.bitgilde.TIMAAT.rest.model.transcription.CreateTranscriptionRequest;
import de.bitgilde.TIMAAT.rest.model.transcription.TranscriptionDto;
import de.bitgilde.TIMAAT.security.TIMAATKeyGenerator;
import de.bitgilde.TIMAAT.security.UserLogManager;
import de.bitgilde.TIMAAT.service.task.TaskService;
import de.bitgilde.TIMAAT.service.task.api.MediumAudioAnalysisTask.SupportedMediumType;
import de.bitgilde.TIMAAT.service.task.api.TaskState;
import de.bitgilde.TIMAAT.service.task.exception.TaskServiceException;
import de.bitgilde.TIMAAT.service.transcription.TranscriptionService;
import de.bitgilde.TIMAAT.service.transcription.api.GenerateTranscriptionConfiguration;
import de.bitgilde.TIMAAT.service.transcription.exception.TranscriptionFeatureDisabledException;
import de.bitgilde.TIMAAT.service.transcription.exception.TranscriptionNotFoundException;
import de.bitgilde.TIMAAT.service.transcription.exception.TranscriptionServiceException;
import de.bitgilde.TIMAAT.storage.api.PagingParameter;
import de.bitgilde.TIMAAT.storage.api.SortingParameter;
import de.bitgilde.TIMAAT.storage.entity.MediumVideoStorage;
import de.bitgilde.TIMAAT.storage.entity.SystemSettingStorage;
import de.bitgilde.TIMAAT.storage.entity.medium.MediumStorage;
import de.bitgilde.TIMAAT.storage.entity.medium.api.MediumFilterCriteria;
import de.bitgilde.TIMAAT.storage.entity.medium.exception.MediumNotFoundException;
import de.bitgilde.TIMAAT.storage.file.AudioFileStorage;
import de.bitgilde.TIMAAT.storage.file.ImageFileStorage;
import de.bitgilde.TIMAAT.storage.file.ImageFileStorage.ImageFileType;
import de.bitgilde.TIMAAT.storage.file.TemporaryFileStorage;
import de.bitgilde.TIMAAT.storage.file.TemporaryFileStorage.TemporaryFile;
import de.bitgilde.TIMAAT.storage.file.VideoFileStorage;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.servlet.ServletContext;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
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
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.StreamingOutput;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONObject;
import org.jvnet.hk2.annotations.Service;

import javax.crypto.SecretKey;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.Key;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
@Path("/medium")
public class EndpointMedium {
  private static final Logger logger = Logger.getLogger(EndpointMedium.class.getName());
  private static final int DEFAULT_THUMBNAIL_TIMESTAMP_MS = 1000;

  @Context
  ContainerRequestContext containerRequestContext;
  @Context
  ServletContext servletContext;
  @Inject
  private TaskService taskService;
  @Inject
  private VideoFileStorage videoFileStorage;
  @Inject
  private TemporaryFileStorage temporaryFileStorage;
  @Inject
  private AudioFileStorage audioFileStorage;
  @Inject
  private ImageFileStorage imageFileStorage;
  @Inject
  private MediumStorage mediumStorage;
  @Inject
  private MediumVideoStorage mediumVideoStorage;
  @Inject
  private FfmpegVideoEngine ffmpegVideoEngine;
  @Inject
  private TranscriptionService transcriptionService;
  @Inject
  private de.bitgilde.TIMAAT.storage.file.TranscriptionFileStorage transcriptionFileStorage;
  @Inject
  private SystemSettingStorage systemSettingStorage;


  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("list")
  public DataTableInfo<Medium> getMediaList(@BeanParam MediumListingQueryParameter queryParameter) {
    UserAccount userAccount = (UserAccount) containerRequestContext.getProperty(
            AuthenticationFilter.USER_ACCOUNT_PROPERTY_NAME);
    int draw = queryParameter.getDraw().orElse(0);

    List<Medium> matchingMediums = mediumStorage.getEntriesAsStream(queryParameter, queryParameter, queryParameter,
            userAccount).collect(Collectors.toList());
    long totalMediumEntries = mediumStorage.getNumberOfTotalEntriesRespectingAuthorization(userAccount);
    long filteredMediumEntries = mediumStorage.getNumberOfMatchingEntriesRespectingAuthorization(queryParameter,
            userAccount);


    return new DataTableInfo<>(draw, totalMediumEntries, filteredMediumEntries, matchingMediums);

  }

  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("selectList")
  public List<SelectElement<Integer>> getMediumSelectList(@QueryParam("search") String search) {
    UserAccount userAccount = (UserAccount) containerRequestContext.getProperty(
            AuthenticationFilter.USER_ACCOUNT_PROPERTY_NAME);
    MediumFilterCriteria filterCriteria = new MediumFilterCriteria.Builder().mediumNameSearch(search).build();

    return mediumStorage.getEntriesAsStream(filterCriteria, PagingParameter.NO_PAGING,
                                SortingParameter.defaultSortOrder(), userAccount)
                        .map(currentMedium -> new SelectElement<Integer>(currentMedium.getId(),
                                currentMedium.getDisplayTitle().getName())).collect(Collectors.toList());
  }

  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/select")
  public Response getMediumSelect(@PathParam("id") int id, @QueryParam("start") Integer start, @QueryParam("length") Integer length, @QueryParam("orderby") String orderby, @QueryParam("search") String search) {
    // System.out.println("EndpointMedium: getActorList: start: "+start+" length: "+length+" orderby: "+orderby+" search: "+search);

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, id);
    List<SelectElement> mediumSelectList = new ArrayList<>();
    mediumSelectList.add(new SelectElement<Integer>(id, medium.getDisplayTitle().getName()));

    return Response.ok().entity(mediumSelectList).build();
  }

  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("image/selectList")
  public Response getImageSelectList(@QueryParam("start") Integer start, @QueryParam("length") Integer length, @QueryParam("orderby") String orderby, @QueryParam("search") String search) {
    // System.out.println("EndpointMedium: getImageSelectList: start: "+start+" length: "+length+" orderby: "+orderby+" search: "+search);

    // TODO search all titles, not displayTitle only
    // define default query strings
    String mediumQuery = "SELECT m FROM Medium m ORDER BY m.displayTitle.name";
    String mediumSearchQuery = "SELECT m FROM Medium m WHERE lower(m.displayTitle.name) LIKE lower(concat('%', :name,'%')) ORDER BY m.displayTitle.name";

    // search
    Query query;
    if (search != null && search.length() > 0) {
      // perform search
      query = TIMAATApp.emf.createEntityManager().createQuery(mediumSearchQuery);
      query.setParameter("name", search);
      // query.setParameter("mediumName", search); // birthName
    }
    else {
      query = TIMAATApp.emf.createEntityManager().createQuery(mediumQuery);
    }
    // if ( start != null && start > 0 ) query.setFirstResult(start);
    // if ( length != null && length > 0 ) query.setMaxResults(length);

    List<Medium> mediumList = castList(Medium.class, query.getResultList());
    List<SelectElementWithToken> mediumSelectList = new ArrayList<>();
    for (Medium medium : mediumList) {
      if (medium.getMediumImage() != null) {
        // if (medium.getViewToken() == null) {
        // 	medium.setViewToken(issueFileToken(medium.getId()));
        // }
        mediumSelectList.add(
                new SelectElementWithToken(medium.getId(), medium.getDisplayTitle().getName(), medium.getViewToken()));
      }
    }

    return Response.ok().entity(mediumSelectList).build();
  }

  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("video/selectList")
  public Response getVideoSelectList(@QueryParam("start") Integer start, @QueryParam("length") Integer length, @QueryParam("orderby") String orderby, @QueryParam("search") String search) {
    // System.out.println("EndpointMedium: getVideoSelectList: start: "+start+" length: "+length+" orderby: "+orderby+" search: "+search);

    // TODO search all titles, not displayTitle only
    // define default query strings
    String mediumQuery = "SELECT m FROM Medium m ORDER BY m.displayTitle.name";
    String mediumSearchQuery = "SELECT m FROM Medium m WHERE lower(m.displayTitle.name) LIKE lower(concat('%', :name,'%')) ORDER BY m.displayTitle.name";

    // search
    Query query;
    if (search != null && search.length() > 0) {
      // perform search
      query = TIMAATApp.emf.createEntityManager().createQuery(mediumSearchQuery);
      query.setParameter("name", search);
      // query.setParameter("mediumName", search); // birthName
    }
    else {
      query = TIMAATApp.emf.createEntityManager().createQuery(mediumQuery);
    }
    // if ( start != null && start > 0 ) query.setFirstResult(start);
    // if ( length != null && length > 0 ) query.setMaxResults(length);

    List<Medium> mediumList = castList(Medium.class, query.getResultList());
    List<SelectElement> mediumSelectList = new ArrayList<>();
    for (Medium medium : mediumList) {
      if (medium.getMediumVideo() != null) {
        mediumSelectList.add(new SelectElement<Integer>(medium.getId(), medium.getDisplayTitle().getName()));
      }
    }

    return Response.ok().entity(mediumSelectList).build();
  }

  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("total")
  public Response getMediaDatasetsTotal() {
    // System.out.println("EndpointMedium: getMediaDatasetsTotal");
    Query query = TIMAATApp.emf.createEntityManager().createQuery("SELECT COUNT (m.id) FROM Medium m");
    long count = (long) query.getSingleResult();
    // int total = ((Integer)TIMAATApp.emf.createEntityManager()
    // 												 .createQuery("SELECT m.id, COUNT(m) FROM Medium m")
    // 												 .getSingleResult()).intValue();
    return Response.ok().entity(count).build();
  }

  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("mediaType/list")
  public Response getMediaTypeList() {
    List<MediaType> mediaTypeList = castList(MediaType.class,
            TIMAATApp.emf.createEntityManager().createNamedQuery("MediaType.findAll").getResultList());
    return Response.ok().entity(mediaTypeList).build();
  }

  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("audio/list")
  public Response getAudioList(@QueryParam("draw") Integer draw, @QueryParam("start") Integer start, @QueryParam("length") Integer length, @QueryParam("orderby") String orderby, @QueryParam("dir") String direction, @QueryParam("search") String search) {
    // System.out.println("EndpointMedium: getAudioList: draw: "+draw+" start: "+start+" length: "+length+" orderby: "+orderby+" dir: "+direction+" search: "+search);
    if (draw == null) {
      draw = 0;
    }

    // sanitize user input
    if (direction != null && direction.equalsIgnoreCase("desc")) {
      direction = "DESC";
    }
    else {
      direction = "ASC";
    }

    String column = "ma.medium.displayTitle.name";
    if (orderby != null) {
      if (orderby.equalsIgnoreCase("title")) {
        column = "ma.medium.displayTitle.name";
      }
      if (orderby.equalsIgnoreCase("duration")) {
        column = "ma.length";
      }
      // if (orderby.equalsIgnoreCase("producer")) column = "";
      if (orderby.equalsIgnoreCase("releaseDate")) {
        column = "ma.medium.releaseDate";
      }
      // if (orderby.equalsIgnoreCase("language")) column = "";
    }

    // calculate total # of records
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Query countQuery = entityManager.createQuery("SELECT COUNT(ma.medium) FROM MediumAudio ma");
    long recordsTotal = (long) countQuery.getSingleResult();
    long recordsFiltered = recordsTotal;

    // search
    Query query;
    String sql;
    List<MediumAudio> mediumAudioList = new ArrayList<>();
    List<Medium> mediumList = new ArrayList<>();
    if (search != null && search.length() > 0) {
      // find all matching titles
      sql = "SELECT DISTINCT ma FROM MediumAudio ma, Medium m, Title t WHERE t IN (m.titles) AND lower(t.name) LIKE lower(concat('%', :search, '%')) AND ma.medium = m ORDER BY " + column + " " + direction;
      query = entityManager.createQuery(sql).setParameter("search", search);
      mediumAudioList = castList(MediumAudio.class, query.getResultList());
      // find all media belonging to those titles
      if (start != null && start > 0) {
        query.setFirstResult(start);
      }
      if (length != null && length > 0) {
        query.setMaxResults(length);
      }
      if (length == -1) { // display all results
        length = mediumAudioList.size();
        query.setMaxResults(length);
      }
      recordsFiltered = mediumAudioList.size();
      for (MediumAudio mediumAudio : mediumAudioList) {
        mediumAudio.getMedium().getMediumAnalysisLists().clear();
        mediumList.add(mediumAudio.getMedium());
      }
      List<Medium> filteredMediumList = new ArrayList<>();
      int i = start;
      int end;
      if ((recordsFiltered - start) < length) {
        end = (int) recordsFiltered;
      }
      else {
        end = start + length;
      }
      for (; i < end; i++) {
        filteredMediumList.add(mediumList.get(i));
      }
      return Response.ok().entity(new DataTableInfo(draw, recordsTotal, recordsFiltered, filteredMediumList)).build();
    }
    else {
      sql = "SELECT ma.medium FROM MediumAudio ma ORDER BY " + column + " " + direction;
      query = entityManager.createQuery(sql);
      if (start != null && start > 0) {
        query.setFirstResult(start);
      }
      if (length != null && length > 0) {
        query.setMaxResults(length);
      }
      mediumList = castList(Medium.class, query.getResultList());
    }

    return Response.ok().entity(new DataTableInfo(draw, recordsTotal, recordsFiltered, mediumList)).build();
  }

  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("document/list")
  public Response getDocumentList(@QueryParam("draw") Integer draw, @QueryParam("start") Integer start, @QueryParam("length") Integer length, @QueryParam("orderby") String orderby, @QueryParam("dir") String direction, @QueryParam("search") String search) {
    // System.out.println("EndpointMedium: getDocumentList: draw: "+draw+" start: "+start+" length: "+length+" orderby: "+orderby+" dir: "+direction+" search: "+search);
    if (draw == null) {
      draw = 0;
    }

    // sanitize user input
    if (direction != null && direction.equalsIgnoreCase("desc")) {
      direction = "DESC";
    }
    else {
      direction = "ASC";
    }

    String column = "md.medium.displayTitle.name";
    if (orderby != null) {
      if (orderby.equalsIgnoreCase("title")) {
        column = "md.medium.displayTitle.name";
      }
      // if (orderby.equalsIgnoreCase("producer")) column = "";
      if (orderby.equalsIgnoreCase("releaseDate")) {
        column = "md.medium.releaseDate";
      }
      // if (orderby.equalsIgnoreCase("language")) column = "";
    }

    // calculate total # of records
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Query countQuery = entityManager.createQuery("SELECT COUNT(md.medium) FROM MediumDocument md");
    long recordsTotal = (long) countQuery.getSingleResult();
    long recordsFiltered = recordsTotal;

    // search
    Query query;
    String sql;
    List<MediumDocument> mediumDocumentList = new ArrayList<>();
    List<Medium> mediumList = new ArrayList<>();
    if (search != null && search.length() > 0) {
      // find all matching titles
      sql = "SELECT DISTINCT md FROM MediumDocument md, Medium m, Title t WHERE t IN (m.titles) AND lower(t.name) LIKE lower(concat('%', :search, '%')) AND md.medium = m ORDER BY " + column + " " + direction;
      query = entityManager.createQuery(sql).setParameter("search", search);
      mediumDocumentList = castList(MediumDocument.class, query.getResultList());
      // find all media belonging to those titles
      if (start != null && start > 0) {
        query.setFirstResult(start);
      }
      if (length != null && length > 0) {
        query.setMaxResults(length);
      }
      if (length == -1) { // display all results
        length = mediumDocumentList.size();
        query.setMaxResults(length);
      }
      recordsFiltered = mediumDocumentList.size();
      for (MediumDocument mediumDocument : mediumDocumentList) {
        mediumDocument.getMedium().getMediumAnalysisLists().clear();
        mediumList.add(mediumDocument.getMedium());
      }
      List<Medium> filteredMediumList = new ArrayList<>();
      int i = start;
      int end;
      if ((recordsFiltered - start) < length) {
        end = (int) recordsFiltered;
      }
      else {
        end = start + length;
      }
      for (; i < end; i++) {
        filteredMediumList.add(mediumList.get(i));
      }
      return Response.ok().entity(new DataTableInfo(draw, recordsTotal, recordsFiltered, filteredMediumList)).build();
    }
    else {
      sql = "SELECT md.medium FROM MediumDocument md ORDER BY " + column + " " + direction;
      query = entityManager.createQuery(sql);
      if (start != null && start > 0) {
        query.setFirstResult(start);
      }
      if (length != null && length > 0) {
        query.setMaxResults(length);
      }
      mediumList = castList(Medium.class, query.getResultList());
    }

    return Response.ok().entity(new DataTableInfo(draw, recordsTotal, recordsFiltered, mediumList)).build();
  }

  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("image/list")
  public Response getImageList(@QueryParam("draw") Integer draw, @QueryParam("start") Integer start, @QueryParam("length") Integer length, @QueryParam("orderby") String orderby, @QueryParam("dir") String direction, @QueryParam("search") String search) {
    // System.out.println("EndpointMedium: getImageList: draw: "+draw+" start: "+start+" length: "+length+" orderby: "+orderby+" dir: "+direction+" search: "+search);
    if (draw == null) {
      draw = 0;
    }

    // sanitize user input
    if (direction != null && direction.equalsIgnoreCase("desc")) {
      direction = "DESC";
    }
    else {
      direction = "ASC";
    }

    String column = "mi.medium.displayTitle.name";
    if (orderby != null) {
      if (orderby.equalsIgnoreCase("title")) {
        column = "mi.medium.displayTitle.name";
      }
      // if (orderby.equalsIgnoreCase("producer")) column = "";
      if (orderby.equalsIgnoreCase("releaseDate")) {
        column = "mi.medium.releaseDate";
      }
      // if (orderby.equalsIgnoreCase("language")) column = "";
    }

    // calculate total # of records
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Query countQuery = entityManager.createQuery("SELECT COUNT(mi.medium) FROM MediumImage mi");
    long recordsTotal = (long) countQuery.getSingleResult();
    long recordsFiltered = recordsTotal;

    // search
    Query query;
    String sql;
    List<MediumImage> mediumImageList = new ArrayList<>();
    List<Medium> mediumList = new ArrayList<>();
    if (search != null && search.length() > 0) {
      // find all matching titles
      sql = "SELECT DISTINCT mi FROM MediumImage mi, Medium m, Title t WHERE t IN (m.titles) AND lower(t.name) LIKE lower(concat('%', :search, '%')) AND mi.medium = m ORDER BY " + column + " " + direction;
      query = entityManager.createQuery(sql).setParameter("search", search);
      mediumImageList = castList(MediumImage.class, query.getResultList());
      // find all media belonging to those titles
      if (start != null && start > 0) {
        query.setFirstResult(start);
      }
      if (length != null && length > 0) {
        query.setMaxResults(length);
      }
      if (length == -1) { // display all results
        length = mediumImageList.size();
        query.setMaxResults(length);
      }
      recordsFiltered = mediumImageList.size();
      for (MediumImage mediumImage : mediumImageList) {
        mediumImage.getMedium().getMediumAnalysisLists().clear();
        mediumList.add(mediumImage.getMedium());
      }
      List<Medium> filteredMediumList = new ArrayList<>();
      int i = start;
      int end;
      if ((recordsFiltered - start) < length) {
        end = (int) recordsFiltered;
      }
      else {
        end = start + length;
      }
      for (; i < end; i++) {
        filteredMediumList.add(mediumList.get(i));
      }
      return Response.ok().entity(new DataTableInfo(draw, recordsTotal, recordsFiltered, filteredMediumList)).build();
    }
    else {
      sql = "SELECT mi.medium FROM MediumImage mi ORDER BY " + column + " " + direction;
      query = entityManager.createQuery(sql);
      if (start != null && start > 0) {
        query.setFirstResult(start);
      }
      if (length != null && length > 0) {
        query.setMaxResults(length);
      }
      mediumList = castList(Medium.class, query.getResultList());
    }

    return Response.ok().entity(new DataTableInfo(draw, recordsTotal, recordsFiltered, mediumList)).build();
  }

  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("software/list")
  public Response getSoftwareList(@QueryParam("draw") Integer draw, @QueryParam("start") Integer start, @QueryParam("length") Integer length, @QueryParam("orderby") String orderby, @QueryParam("dir") String direction, @QueryParam("search") String search) {
    // System.out.println("EndpointMedium: getSoftwareList: draw: "+draw+" start: "+start+" length: "+length+" orderby: "+orderby+" dir: "+direction+" search: "+search);
    if (draw == null) {
      draw = 0;
    }

    // sanitize user input
    if (direction != null && direction.equalsIgnoreCase("desc")) {
      direction = "DESC";
    }
    else {
      direction = "ASC";
    }

    String column = "ms.medium.displayTitle.name";
    if (orderby != null) {
      if (orderby.equalsIgnoreCase("title")) {
        column = "ms.medium.displayTitle.name";
      }
      // if (orderby.equalsIgnoreCase("producer")) column = "";
      if (orderby.equalsIgnoreCase("releaseDate")) {
        column = "ms.medium.releaseDate";
      }
      // if (orderby.equalsIgnoreCase("language")) column = "";
    }

    // calculate total # of records
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Query countQuery = entityManager.createQuery("SELECT COUNT(ms.medium) FROM MediumSoftware ms");
    long recordsTotal = (long) countQuery.getSingleResult();
    long recordsFiltered = recordsTotal;

    // search
    Query query;
    String sql;
    List<MediumSoftware> mediumSoftwareList = new ArrayList<>();
    List<Medium> mediumList = new ArrayList<>();
    if (search != null && search.length() > 0) {
      // find all matching titles
      sql = "SELECT DISTINCT ms FROM MediumSoftware ms, Medium m, Title t WHERE t IN (m.titles) AND lower(t.name) LIKE lower(concat('%', :search, '%')) AND ms.medium = m ORDER BY " + column + " " + direction;
      query = entityManager.createQuery(sql).setParameter("search", search);
      mediumSoftwareList = castList(MediumSoftware.class, query.getResultList());
      // find all media belonging to those titles
      if (start != null && start > 0) {
        query.setFirstResult(start);
      }
      if (length != null && length > 0) {
        query.setMaxResults(length);
      }
      if (length == -1) { // display all results
        length = mediumSoftwareList.size();
        query.setMaxResults(length);
      }
      recordsFiltered = mediumSoftwareList.size();
      for (MediumSoftware mediumSoftware : mediumSoftwareList) {
        mediumSoftware.getMedium().getMediumAnalysisLists().clear();
        mediumList.add(mediumSoftware.getMedium());
      }
      List<Medium> filteredMediumList = new ArrayList<>();
      int i = start;
      int end;
      if ((recordsFiltered - start) < length) {
        end = (int) recordsFiltered;
      }
      else {
        end = start + length;
      }
      for (; i < end; i++) {
        filteredMediumList.add(mediumList.get(i));
      }
      return Response.ok().entity(new DataTableInfo(draw, recordsTotal, recordsFiltered, filteredMediumList)).build();
    }
    else {
      sql = "SELECT ms.medium FROM MediumSoftware ms ORDER BY " + column + " " + direction;
      query = entityManager.createQuery(sql);
      if (start != null && start > 0) {
        query.setFirstResult(start);
      }
      if (length != null && length > 0) {
        query.setMaxResults(length);
      }
      mediumList = castList(Medium.class, query.getResultList());
    }

    return Response.ok().entity(new DataTableInfo(draw, recordsTotal, recordsFiltered, mediumList)).build();
  }

  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("text/list")
  public Response getTextList(@QueryParam("draw") Integer draw, @QueryParam("start") Integer start, @QueryParam("length") Integer length, @QueryParam("orderby") String orderby, @QueryParam("dir") String direction, @QueryParam("search") String search) {
    // System.out.println("EndpointMedium: getTextList: draw: "+draw+" start: "+start+" length: "+length+" orderby: "+orderby+" dir: "+direction+" search: "+search);
    if (draw == null) {
      draw = 0;
    }

    // sanitize user input
    if (direction != null && direction.equalsIgnoreCase("desc")) {
      direction = "DESC";
    }
    else {
      direction = "ASC";
    }

    String column = "mt.medium.displayTitle.name";
    if (orderby != null) {
      if (orderby.equalsIgnoreCase("title")) {
        column = "mt.medium.displayTitle.name";
      }
      // if (orderby.equalsIgnoreCase("producer")) column = "";
      if (orderby.equalsIgnoreCase("releaseDate")) {
        column = "mt.medium.releaseDate";
      }
      // if (orderby.equalsIgnoreCase("language")) column = "";
    }

    // calculate total # of records
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Query countQuery = entityManager.createQuery("SELECT COUNT(mt.medium) FROM MediumText mt");
    long recordsTotal = (long) countQuery.getSingleResult();
    long recordsFiltered = recordsTotal;

    // search
    Query query;
    String sql;
    List<MediumText> mediumTextList = new ArrayList<>();
    List<Medium> mediumList = new ArrayList<>();
    if (search != null && search.length() > 0) {
      // find all matching titles
      sql = "SELECT DISTINCT mt FROM MediumText mt, Medium m, Title t WHERE t IN (m.titles) AND lower(t.name) LIKE lower(concat('%', :search, '%')) AND mt.medium = m ORDER BY " + column + " " + direction;
      query = entityManager.createQuery(sql).setParameter("search", search);
      mediumTextList = castList(MediumText.class, query.getResultList());
      // find all media belonging to those titles
      if (start != null && start > 0) {
        query.setFirstResult(start);
      }
      if (length != null && length > 0) {
        query.setMaxResults(length);
      }
      if (length == -1) { // display all results
        length = mediumTextList.size();
        query.setMaxResults(length);
      }
      recordsFiltered = mediumTextList.size();
      for (MediumText mediumText : mediumTextList) {
        mediumText.getMedium().getMediumAnalysisLists().clear();
        mediumList.add(mediumText.getMedium());
      }
      List<Medium> filteredMediumList = new ArrayList<>();
      int i = start;
      int end;
      if ((recordsFiltered - start) < length) {
        end = (int) recordsFiltered;
      }
      else {
        end = start + length;
      }
      for (; i < end; i++) {
        filteredMediumList.add(mediumList.get(i));
      }
      return Response.ok().entity(new DataTableInfo(draw, recordsTotal, recordsFiltered, filteredMediumList)).build();
    }
    else {
      sql = "SELECT mt.medium FROM MediumText mt ORDER BY " + column + " " + direction;
      query = entityManager.createQuery(sql);
      if (start != null && start > 0) {
        query.setFirstResult(start);
      }
      if (length != null && length > 0) {
        query.setMaxResults(length);
      }
      mediumList = castList(Medium.class, query.getResultList());
    }

    return Response.ok().entity(new DataTableInfo(draw, recordsTotal, recordsFiltered, mediumList)).build();
  }

  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("video/list")
  public Response getVideoList(@QueryParam("draw") Integer draw, @QueryParam("start") Integer start, @QueryParam("length") Integer length, @QueryParam("orderby") String orderby, @QueryParam("dir") String direction, @QueryParam("search") String search) {

    // System.out.println("EndpointMedium: getVideoList: draw: "+draw+" start: "+start+" length: "+length+" orderby: "+orderby+" dir: "+direction+" search: "+search);
    if (draw == null) {
      draw = 0;
    }

    // sanitize user input
    if (direction != null && direction.equalsIgnoreCase("desc")) {
      direction = "DESC";
    }
    else {
      direction = "ASC";
    }

    String column = "mv.medium.displayTitle.name";
    if (orderby != null) {
      if (orderby.equalsIgnoreCase("title")) {
        column = "mv.medium.displayTitle.name";
      }
      if (orderby.equalsIgnoreCase("duration")) {
        column = "mv.length";
      }
      // if (orderby.equalsIgnoreCase("producer")) column = "";
      if (orderby.equalsIgnoreCase("releaseDate")) {
        column = "mv.medium.releaseDate";
      }
      // if (orderby.equalsIgnoreCase("language")) column = "";
    }

    // calculate total # of records
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Query countQuery = entityManager.createQuery("SELECT COUNT(mv.medium) FROM MediumVideo mv");
    long recordsTotal = (long) countQuery.getSingleResult();
    long recordsFiltered = recordsTotal;

    // search
    Query query;
    String sql;
    List<MediumVideo> mediumVideoList = new ArrayList<>();
    List<Medium> mediumList = new ArrayList<>();
    if (search != null && search.length() > 0) {
      // find all matching titles
      sql = "SELECT DISTINCT mv FROM MediumVideo mv, Medium m, Title t WHERE t IN (m.titles) AND lower(t.name) LIKE lower(concat('%', :search, '%')) AND mv.medium = m ORDER BY " + column + " " + direction;
      query = entityManager.createQuery(sql).setParameter("search", search);
      mediumVideoList = castList(MediumVideo.class, query.getResultList());
      // find all media belonging to those titles
      if (start != null && start > 0) {
        query.setFirstResult(start);
      }
      if (length != null && length > 0) {
        query.setMaxResults(length);
      }
      if (length == -1) { // display all results
        length = mediumVideoList.size();
        query.setMaxResults(length);
      }
      recordsFiltered = mediumVideoList.size();
      for (MediumVideo mediumVideo : mediumVideoList) {
        mediumVideo.getMedium().getMediumAnalysisLists().clear();
        mediumList.add(mediumVideo.getMedium());
      }
      List<Medium> filteredMediumList = new ArrayList<>();
      int i = start;
      int end;
      if ((recordsFiltered - start) < length) {
        end = (int) recordsFiltered;
      }
      else {
        end = start + length;
      }
      for (; i < end; i++) {
        filteredMediumList.add(mediumList.get(i));
      }
      return Response.ok().entity(new DataTableInfo(draw, recordsTotal, recordsFiltered, filteredMediumList)).build();
    }
    else {
      sql = "SELECT mv.medium FROM MediumVideo mv ORDER BY " + column + " " + direction;
      query = entityManager.createQuery(sql);
      if (start != null && start > 0) {
        query.setFirstResult(start);
      }
      if (length != null && length > 0) {
        query.setMaxResults(length);
      }
      mediumList = castList(Medium.class, query.getResultList());
      for (Medium medium : mediumList) {
        medium.getMediumAnalysisLists().clear();
      }
      return Response.ok().entity(new DataTableInfo(draw, recordsTotal, recordsFiltered, mediumList)).build();
    }
  }

  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("videogame/list")
  public Response getVideogameList(@QueryParam("draw") Integer draw, @QueryParam("start") Integer start, @QueryParam("length") Integer length, @QueryParam("orderby") String orderby, @QueryParam("dir") String direction, @QueryParam("search") String search) {
    // System.out.println("EndpointMedium: getVideoList: draw: "+draw+" start: "+start+" length: "+length+" orderby: "+orderby+" dir: "+direction+" search: "+search);

    if (draw == null) {
      draw = 0;
    }

    // sanitize user input
    if (direction != null && direction.equalsIgnoreCase("desc")) {
      direction = "DESC";
    }
    else {
      direction = "ASC";
    }

    String column = "mv.medium.displayTitle.name";
    if (orderby != null) {
      if (orderby.equalsIgnoreCase("title")) {
        column = "mv.medium.displayTitle.name";
      }
      // if (orderby.equalsIgnoreCase("producer")) column = "";
      if (orderby.equalsIgnoreCase("releaseDate")) {
        column = "mv.medium.releaseDate";
      }
      // if (orderby.equalsIgnoreCase("language")) column = "";
    }

    // calculate total # of records
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Query countQuery = TIMAATApp.emf.createEntityManager()
                                    .createQuery("SELECT COUNT(mv.medium) FROM MediumVideogame mv");
    long recordsTotal = (long) countQuery.getSingleResult();
    long recordsFiltered = recordsTotal;

    // search
    Query query;
    String sql;
    List<MediumVideogame> mediumVideogameList = new ArrayList<>();
    List<Medium> mediumList = new ArrayList<>();
    if (search != null && search.length() > 0) {
      // find all matching titles
      sql = "SELECT DISTINCT mv FROM MediumVideogame mv, Medium m, Title t WHERE t IN (m.titles) AND lower(t.name) LIKE lower(concat('%', :search, '%')) AND mv.medium = m ORDER BY " + column + " " + direction;
      query = entityManager.createQuery(sql).setParameter("search", search);
      mediumVideogameList = castList(MediumVideogame.class, query.getResultList());
      // find all media belonging to those titles
      if (start != null && start > 0) {
        query.setFirstResult(start);
      }
      if (length != null && length > 0) {
        query.setMaxResults(length);
      }
      if (length == -1) { // display all results
        length = mediumVideogameList.size();
        query.setMaxResults(length);
      }
      recordsFiltered = mediumVideogameList.size();
      for (MediumVideogame mediumVideogame : mediumVideogameList) {
        mediumVideogame.getMedium().getMediumAnalysisLists().clear();
        mediumList.add(mediumVideogame.getMedium());
      }
      List<Medium> filteredMediumList = new ArrayList<>();
      int i = start;
      int end;
      if ((recordsFiltered - start) < length) {
        end = (int) recordsFiltered;
      }
      else {
        end = start + length;
      }
      for (; i < end; i++) {
        filteredMediumList.add(mediumList.get(i));
      }
      return Response.ok().entity(new DataTableInfo(draw, recordsTotal, recordsFiltered, filteredMediumList)).build();
    }
    else {
      sql = "SELECT mv.medium FROM MediumVideogame mv ORDER BY " + column + " " + direction;
      query = entityManager.createQuery(sql);
      if (start != null && start > 0) {
        query.setFirstResult(start);
      }
      if (length != null && length > 0) {
        query.setMaxResults(length);
      }
      mediumList = castList(Medium.class, query.getResultList());
    }

    return Response.ok().entity(new DataTableInfo(draw, recordsTotal, recordsFiltered, mediumList)).build();
  }

  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("hasActor/{actorId}/withRoles/selectList")
  public Response getRoleSelectList(@PathParam("actorId") int actorId, @QueryParam("search") String search, @QueryParam("page") Integer page, @QueryParam("per_page") Integer per_page, @QueryParam("language") String languageCode) {
    // returns list of id and name combinations of all roles of this actor
    // System.out.println("EndpointMedium: getRoleSelectList for actor id: "+ actorId);
    // System.out.println("EndpointMedium: getRoleSelectList - search string: "+ search);

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
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("{mediumId}/hasActorList")
  public Response getActorList(@PathParam("mediumId") Integer mediumId) {
    // System.out.println("EndpointMedium: getActorList");
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, mediumId);
    List<MediumHasActorWithRole> mediumHasActorWithRoleList = medium.getMediumHasActorWithRoles();
    List<Actor> actorList = new ArrayList<>();
    for (MediumHasActorWithRole mediumHasActorWithRole : mediumHasActorWithRoleList) {
      if (!actorList.contains(mediumHasActorWithRole.getActor())) {
        actorList.add(mediumHasActorWithRole.getActor());
      }
    }
    return Response.ok().entity(actorList).build();
  }

  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("{mediumId}/hasActor/{actorId}/withRoleList")
  public Response getActorHasRoleList(@PathParam("mediumId") Integer mediumId, @PathParam("actorId") Integer actorId) {
    // System.out.println("EndpointMedium: getActorHasRoleList");
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();

    Medium medium = entityManager.find(Medium.class, mediumId);
    List<MediumHasActorWithRole> mediumHasActorWithRoleList = medium.getMediumHasActorWithRoles();
    List<Role> roleList = new ArrayList<>();
    for (MediumHasActorWithRole mediumHasActorWithRole : mediumHasActorWithRoleList) {
      if (mediumHasActorWithRole.getActor().getId() == actorId) {
        roleList.add(entityManager.find(Role.class, mediumHasActorWithRole.getRole().getId()));
      }
    }

    return Response.ok().entity(roleList).build();
  }

  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/categorySet/list")
  public Response getCategorySetList(@PathParam("id") Integer id) {
    // System.out.println("EndpointMedium: getCategorySetList - ID: "+ id);
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, id);
    List<CategorySet> categorySetList = medium.getCategorySets();

    return Response.ok().entity(categorySetList).build();
  }

  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/category/list")
  public Response getSelectedCategories(@PathParam("id") Integer id) {
    // System.out.println("EndpointMedium: getSelectedCategories - Id: "+ id);
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, id);
    List<Category> categoryList = medium.getCategories();
    return Response.ok().entity(categoryList).build();
  }

  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/category/selectList")
  public Response getCategorySelectList(@PathParam("id") Integer id, @QueryParam("start") Integer start, @QueryParam("length") Integer length, @QueryParam("orderby") String orderby, @QueryParam("dir") String direction, @QueryParam("search") String search) {
    // System.out.println("EndpointMedium: getCategorySelectList - Id: "+ id);

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, id);
    List<CategorySet> categorySetList = medium.getCategorySets();
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
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("{mediumId}/hasTagList")
  public Response getTagList(@PathParam("mediumId") Integer mediumId) {
    // System.out.println("EndpointMedium: getTagList");
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, mediumId);
    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    entityManager.refresh(medium);
    return Response.ok().entity(medium.getTags()).build();
  }

  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/titles/list")
  public Response getTitlesList(@PathParam("id") int id) {
    // Medium medium = TIMAATApp.emf.createEntityManager().find(Medium.class, id);
    // 	if ( medium == null ) return Response.status(Status.NOT_FOUND).build();
    // // List<Title> titleList = TIMAATApp.emf.createEntityManager().createNamedQuery("titleList.findAll").getResultList();
    // // return Response.ok().entity(titleList).build();

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();

    // find medium
    Medium medium = entityManager.find(Medium.class, id);
    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    entityManager.refresh(medium);

    return Response.ok().entity(medium.getTitles()).build();
  }

  @POST
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("{id}")
  @Secured
  public Response createMedium(@PathParam("id") int id, String jsonData) {
    // System.out.println("EndpointMedium: createMedium: " + jsonData);
    ObjectMapper mapper = new ObjectMapper();
    Medium newMedium = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();

    // parse JSON data
    try {
      newMedium = mapper.readValue(jsonData, Medium.class);
    } catch (IOException e) {
      System.out.println("EndpointMedium: createMedium - IOException");
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (newMedium == null) {
      System.out.println("EndpointMedium: createMedium - newMedium == null");
      return Response.status(Status.BAD_REQUEST).build();
    }
    // sanitize object data
    newMedium.setId(0);
    Title displayTitle = entityManager.find(Title.class, newMedium.getDisplayTitle().getId());
    newMedium.setDisplayTitle(displayTitle);
    newMedium.setOriginalTitle(displayTitle);
    Source source = new Source();

    // update log metadata
    Timestamp creationDate = new Timestamp(System.currentTimeMillis());
    newMedium.setCreatedAt(creationDate);
    newMedium.setLastEditedAt(creationDate);
    if (containerRequestContext.getProperty("TIMAAT.userID") != null) {
      newMedium.setCreatedByUserAccount(
              entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID")));
      newMedium.setLastEditedByUserAccount(
              (entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID"))));
    }
    else {
      // DEBUG do nothing - production system should abort with internal server error
      return Response.serverError().build();
    }

    // persist Medium
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(displayTitle);
    entityManager.persist(newMedium);
    entityManager.flush();
    newMedium.setDisplayTitle(displayTitle);
    newMedium.setOriginalTitle(displayTitle);
    entityTransaction.commit();
    entityManager.refresh(newMedium);
    entityManager.refresh(displayTitle);

    // create medium_has_title-table entry
    entityTransaction.begin();
    newMedium.getTitles().add(displayTitle);
    displayTitle.getMediums3().add(newMedium);
    entityManager.merge(displayTitle);
    entityManager.merge(newMedium);
    entityManager.persist(displayTitle);
    entityManager.persist(newMedium);
    entityTransaction.commit();
    entityManager.refresh(newMedium);
    entityManager.refresh(displayTitle);

    // create source entry of medium
    entityTransaction.begin();
    newMedium.getSources().add(source);
    source.setMedium(newMedium);
    entityManager.persist(source);
    entityManager.persist(newMedium);
    entityTransaction.commit();
    entityManager.refresh(newMedium);
    entityManager.refresh(source);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.MEDIUMCREATED);
    return Response.ok().entity(newMedium).build();
  }

  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}")
  public Response getMedium(@PathParam("id") int id) {
    Medium medium = TIMAATApp.emf.createEntityManager().find(Medium.class, id);
    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.ok().entity(medium).build();
  }

  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/title")
  public Response getMediumTitle(@PathParam("id") int id) {
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, id);
    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.ok().entity(medium.getDisplayTitle()).build();
  }

  @GET
  @Produces(jakarta.ws.rs.core.MediaType.TEXT_PLAIN)
  @Secured
  @Path("{id}/viewToken")
  public Response getViewToken(@PathParam("id") int id) {
    Medium medium = TIMAATApp.emf.createEntityManager().find(Medium.class, id);
    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    String viewToken = medium.getViewToken();
    return Response.ok().entity(viewToken).build();
  }

  @PATCH
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("{id}")
  @Secured
  public Response updateMedium(@PathParam("id") int id, String jsonData) {
    // System.out.println("EndpointMedium: update medium - jsonData"+ jsonData);

    ObjectMapper mapper = new ObjectMapper();
    Medium updatedMedium = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, id);

    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      updatedMedium = mapper.readValue(jsonData, Medium.class);
    } catch (IOException e) {
      System.out.println("EndpointMedium: update medium - IOException");
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedMedium == null) {
      return Response.notModified().build();
    }

    // update medium
    //! Don't change media type. MediumSubtype won't match anymore
    if (updatedMedium.getReleaseDate() != null) {
      medium.setReleaseDate(updatedMedium.getReleaseDate());
    }
    medium.setRecordingStartDate(updatedMedium.getRecordingStartDate());
    medium.setRecordingEndDate(updatedMedium.getRecordingEndDate());
    if (updatedMedium.getRemark() != null) {
      medium.setRemark(updatedMedium.getRemark());
    }
    if (updatedMedium.getCopyright() != null) {
      medium.setCopyright(updatedMedium.getCopyright());
    }
    if (updatedMedium.getDisplayTitle() != null) {
      medium.setDisplayTitle(updatedMedium.getDisplayTitle());
    }
    medium.setMusic(updatedMedium.getMusic());
    medium.setOriginalTitle(updatedMedium.getOriginalTitle()); // originalTitle can be set to null
    List<CategorySet> oldCategorySets = medium.getCategorySets();
    medium.setCategorySets(updatedMedium.getCategorySets());
    List<Category> oldCategories = medium.getCategories();
    medium.setCategories(updatedMedium.getCategories());
    List<Tag> oldTags = medium.getTags();
    medium.setTags(updatedMedium.getTags());

    // update log metadata
    medium.setLastEditedAt(new Timestamp(System.currentTimeMillis()));
    if (containerRequestContext.getProperty("TIMAAT.userID") != null) {
      medium.setLastEditedByUserAccount(
              (entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID"))));
    }
    else {
      // DEBUG do nothing - production system should abort with internal server error
    }

    //* time inconsistency check print out
    // System.out.println(medium.getCreatedAt());
    // System.out.println(medium.getLastEditedAt());

    // persist medium
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(medium);
    entityManager.persist(medium);
    entityTransaction.commit();
    entityManager.refresh(medium);
    for (CategorySet categorySet : medium.getCategorySets()) {
      entityManager.refresh(categorySet);
    }
    for (CategorySet categorySet : oldCategorySets) {
      entityManager.refresh(categorySet);
    }
    for (Category category : medium.getCategories()) {
      entityManager.refresh(category);
    }
    for (Category category : oldCategories) {
      entityManager.refresh(category);
    }
    for (Tag tag : medium.getTags()) {
      entityManager.refresh(tag);
    }
    for (Tag tag : oldTags) {
      entityManager.refresh(tag);
    }

    //* time inconsistency check print out
    // System.out.println("after");
    // System.out.println(medium.getCreatedAt());
    // System.out.println(medium.getLastEditedAt());

    // if ( medium.getMediumVideo() != null ) {
    // medium.getFileStatus(medium.getMediaType().getMediaTypeTranslations().get(0).getType());
    // TODO necessary?
    medium.getFileStatus();
    medium.getViewToken();
    // }

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.MEDIUMEDITED);
    return Response.ok().entity(medium).build();
  }

  @DELETE
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("{id}")
  @Secured
  public Response deleteMedium(@PathParam("id") int id) {
    // System.out.println("EndpointMedium: deleteMedium");
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, id);

    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    // for (Title title:medium.getTitles()) {
    // 	medium.getTitles().remove(title);
    // }
    entityManager.remove(medium.getDisplayTitle());
    entityManager.remove(medium);
    entityTransaction.commit();

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.MEDIUMDELETED);
    return Response.ok().build();
  }

  @POST
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("audio/{id}")
  @Secured
  public Response createAudio(@PathParam("id") int id, String jsonData) {

    // System.out.println("EndpointMedium: createAudio jsonData: "+jsonData);
    ObjectMapper mapper = new ObjectMapper();
    MediumAudio newAudio = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();

    // parse JSON data
    try {
      newAudio = mapper.readValue(jsonData, MediumAudio.class);
    } catch (IOException e) {
      System.out.println("EndpointMedium: createAudio: IOException e !");
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (newAudio == null) {
      System.out.println("EndpointMedium: createAudio: newAudio == null !");
      return Response.status(Status.BAD_REQUEST).build();
    }

    // sanitize object data
    // AudioPostProduction audioPostProduction = new AudioPostProduction();

    // update log metadata
    // Not necessary, a audio will always be created in conjunction with a medium

    // persist audio
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(newAudio);
    entityManager.flush();
    entityTransaction.commit();
    entityManager.refresh(newAudio);
    entityManager.refresh(newAudio.getMedium());

    // add log entry
    UserLogManager.getLogger().addLogEntry(newAudio.getMedium().getCreatedByUserAccount().getId(),
            UserLogManager.LogEvents.AUDIOCREATED);
    return Response.ok().entity(newAudio).build();
  }

  @PATCH
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("audio/{id}")
  @Secured
  public Response updateAudio(@PathParam("id") int id, String jsonData) {

    // System.out.println("EndpointMedium: update audio - jsonData: " + jsonData);
    ObjectMapper mapper = new ObjectMapper();
    MediumAudio updatedAudio = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumAudio audio = entityManager.find(MediumAudio.class, id);

    if (audio == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      updatedAudio = mapper.readValue(jsonData, MediumAudio.class);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedAudio == null) {
      return Response.notModified().build();
    }

    // update audio
    if (updatedAudio.getLength() > 0) {
      audio.setLength(updatedAudio.getLength());
    }

    // update log metadata
    audio.getMedium().setLastEditedAt(new Timestamp(System.currentTimeMillis()));
    if (containerRequestContext.getProperty("TIMAAT.userID") != null) {
      audio.getMedium().setLastEditedByUserAccount(
              (entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID"))));
    }
    else {
      // DEBUG do nothing - production system should abort with internal server error
    }

    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(audio);
    entityManager.persist(audio);
    entityTransaction.commit();
    entityManager.refresh(audio);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.AUDIOEDITED);
    return Response.ok().entity(audio).build();
  }

  @DELETE
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("audio/{id}")
  @Secured
  public Response deleteAudio(@PathParam("id") int id) {
    // System.out.println("EndpointMedium: deleteAudio with id: "+ id);
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, id);
    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    MediumAudio audio = entityManager.find(MediumAudio.class, id);
    if (audio == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(audio);
    entityManager.remove(audio.getMedium().getDisplayTitle());
    entityManager.remove(audio.getMedium()); // remove audio, then corresponding medium
    entityTransaction.commit();
    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.AUDIODELETED);
    return Response.ok().build();
  }

  @POST
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("document/{id}")
  @Secured
  public Response createDocument(@PathParam("id") int id, String jsonData) {
    // System.out.println("EndpointMedium: createDocument jsonData: "+jsonData);
    ObjectMapper mapper = new ObjectMapper();
    MediumDocument newDocument = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    // parse JSON data
    try {
      newDocument = mapper.readValue(jsonData, MediumDocument.class);
    } catch (IOException e) {
      System.out.println("EndpointMedium: createDocument: IOException e !");
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (newDocument == null) {
      System.out.println("EndpointMedium: createDocument: newDocument == null !");
      return Response.status(Status.BAD_REQUEST).build();
    }
    // sanitize object data
    // update log metadata
    // Not necessary, a document will always be created in conjunction with a medium
    // persist document
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(newDocument);
    entityManager.flush();
    entityTransaction.commit();
    entityManager.refresh(newDocument);
    entityManager.refresh(newDocument.getMedium());

    // add log entry
    UserLogManager.getLogger().addLogEntry(newDocument.getMedium().getCreatedByUserAccount().getId(),
            UserLogManager.LogEvents.DOCUMENTCREATED);
    return Response.ok().entity(newDocument).build();
  }

  @PATCH
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("document/{id}")
  @Secured
  public Response updateDocument(@PathParam("id") int id, String jsonData) {
    // System.out.println("EndpointMedium: update document - jsonData: " + jsonData);
    ObjectMapper mapper = new ObjectMapper();
    MediumDocument updatedDocument = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumDocument document = entityManager.find(MediumDocument.class, id);
    if (document == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      updatedDocument = mapper.readValue(jsonData, MediumDocument.class);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedDocument == null) {
      return Response.notModified().build();
    }
    // update document
    // no data to update at the moment
    // update log metadata
    document.getMedium().setLastEditedAt(new Timestamp(System.currentTimeMillis()));
    if (containerRequestContext.getProperty("TIMAAT.userID") != null) {
      document.getMedium().setLastEditedByUserAccount(
              (entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID"))));
    }
    else {
      // DEBUG do nothing - production system should abort with internal server error
    }
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(document);
    entityManager.persist(document);
    entityTransaction.commit();
    entityManager.refresh(document);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.DOCUMENTEDITED);
    return Response.ok().entity(document).build();
  }

  @DELETE
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("document/{id}")
  @Secured
  public Response deleteDocument(@PathParam("id") int id) {
    // System.out.println("EndpointMedium: deleteDocument with id: "+ id);
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, id);
    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    MediumDocument document = entityManager.find(MediumDocument.class, id);
    if (document == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(document);
    entityManager.remove(document.getMedium().getDisplayTitle());
    entityManager.remove(document.getMedium()); // remove document, then corresponding medium
    entityTransaction.commit();
    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.DOCUMENTDELETED);
    return Response.ok().build();
  }

  @POST
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("image/{id}")
  @Secured
  public Response createImage(@PathParam("id") int id, String jsonData) {
    // System.out.println("EndpointMedium: createImage jsonData: "+jsonData);
    ObjectMapper mapper = new ObjectMapper();
    MediumImage newImage = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    // parse JSON data
    try {
      newImage = mapper.readValue(jsonData, MediumImage.class);
    } catch (IOException e) {
      System.out.println("EndpointMedium: createImage: IOException e !");
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (newImage == null) {
      System.out.println("EndpointMedium: createImage: newImage == null !");
      return Response.status(Status.BAD_REQUEST).build();
    }
    // sanitize object data
    // update log metadata
    // Not necessary, an image will always be created in conjunction with a medium
    // persist image
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(newImage);
    entityManager.flush();
    entityTransaction.commit();
    entityManager.refresh(newImage);
    entityManager.refresh(newImage.getMedium());

    // add log entry
    UserLogManager.getLogger().addLogEntry(newImage.getMedium().getCreatedByUserAccount().getId(),
            UserLogManager.LogEvents.IMAGECREATED);
    return Response.ok().entity(newImage).build();
  }

  @PATCH
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("image/{id}")
  @Secured
  public Response updateImage(@PathParam("id") int id, String jsonData) {
    // System.out.println("EndpointMedium: update image - jsonData: " + jsonData);
    ObjectMapper mapper = new ObjectMapper();
    MediumImage updatedImage = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumImage image = entityManager.find(MediumImage.class, id);
    if (image == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      updatedImage = mapper.readValue(jsonData, MediumImage.class);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedImage == null) {
      return Response.notModified().build();
    }

    // update image
    if (updatedImage.getWidth() > 0) {
      image.setWidth(updatedImage.getWidth());
    }
    if (updatedImage.getHeight() > 0) {
      image.setHeight(updatedImage.getHeight());
    }
    if (updatedImage.getBitDepth() != null) {
      image.setBitDepth(updatedImage.getBitDepth());
    }
    // update log metadata
    image.getMedium().setLastEditedAt(new Timestamp(System.currentTimeMillis()));
    if (containerRequestContext.getProperty("TIMAAT.userID") != null) {
      image.getMedium().setLastEditedByUserAccount(
              (entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID"))));
    }
    else {
      // DEBUG do nothing - production system should abort with internal server error
    }
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(image);
    entityManager.persist(image);
    entityTransaction.commit();
    entityManager.refresh(image);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.IMAGEEDITED);
    return Response.ok().entity(image).build();
  }

  @DELETE
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("image/{id}")
  @Secured
  public Response deleteImage(@PathParam("id") int id) {
    // System.out.println("EndpointMedium: deleteImage with id: "+ id);
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, id);
    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    MediumImage image = entityManager.find(MediumImage.class, id);
    if (image == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(image);
    entityManager.remove(image.getMedium().getDisplayTitle());
    entityManager.remove(image.getMedium()); // remove image, then corresponding medium
    entityTransaction.commit();
    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.IMAGEDELETED);
    return Response.ok().build();
  }

  @POST
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("software/{id}")
  @Secured
  public Response createSoftware(@PathParam("id") int id, String jsonData) {
    // System.out.println("EndpointMedium: createSoftware jsonData: "+jsonData);
    ObjectMapper mapper = new ObjectMapper();
    MediumSoftware newSoftware = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    // parse JSON data
    try {
      newSoftware = mapper.readValue(jsonData, MediumSoftware.class);
    } catch (IOException e) {
      System.out.println("EndpointMedium: createSoftware: IOException e !");
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (newSoftware == null) {
      System.out.println("EndpointMedium: createSoftware: newSoftware == null !");
      return Response.status(Status.BAD_REQUEST).build();
    }
    // sanitize object data
    // update log metadata
    // Not necessary, a software will always be created in conjunction with a medium
    // persist software
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(newSoftware);
    entityManager.flush();
    entityTransaction.commit();
    entityManager.refresh(newSoftware);
    entityManager.refresh(newSoftware.getMedium());

    // add log entry
    UserLogManager.getLogger().addLogEntry(newSoftware.getMedium().getCreatedByUserAccount().getId(),
            UserLogManager.LogEvents.SOFTWARECREATED);
    return Response.ok().entity(newSoftware).build();
  }

  @PATCH
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("software/{id}")
  @Secured
  public Response updateSoftware(@PathParam("id") int id, String jsonData) {
    // System.out.println("EndpointMedium: update software - jsonData: " + jsonData);
    ObjectMapper mapper = new ObjectMapper();
    MediumSoftware updatedSoftware = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumSoftware software = entityManager.find(MediumSoftware.class, id);
    if (software == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      updatedSoftware = mapper.readValue(jsonData, MediumSoftware.class);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedSoftware == null) {
      return Response.notModified().build();
    }

    // update software
    if (updatedSoftware.getVersion() != null) {
      software.setVersion(updatedSoftware.getVersion());
    }

    // update log metadata
    software.getMedium().setLastEditedAt(new Timestamp(System.currentTimeMillis()));
    if (containerRequestContext.getProperty("TIMAAT.userID") != null) {
      software.getMedium().setLastEditedByUserAccount(
              (entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID"))));
    }
    else {
      // DEBUG do nothing - production system should abort with internal server error
    }
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(software);
    entityManager.persist(software);
    entityTransaction.commit();
    entityManager.refresh(software);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.SOFTWAREEDITED);
    return Response.ok().entity(software).build();
  }

  @DELETE
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("software/{id}")
  @Secured
  public Response deleteSoftware(@PathParam("id") int id) {
    // System.out.println("EndpointMedium: deleteSoftware with id: "+ id);
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, id);
    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    MediumSoftware software = entityManager.find(MediumSoftware.class, id);
    if (software == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(software);
    entityManager.remove(software.getMedium().getDisplayTitle());
    entityManager.remove(software.getMedium()); // remove software, then corresponding medium
    entityTransaction.commit();
    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.SOFTWAREDELETED);
    return Response.ok().build();
  }

  @POST
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("text/{id}")
  @Secured
  public Response createText(@PathParam("id") int id, String jsonData) {
    // System.out.println("EndpointMedium: createText jsonData: "+jsonData);
    ObjectMapper mapper = new ObjectMapper();
    MediumText newText = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    // parse JSON data
    try {
      newText = mapper.readValue(jsonData, MediumText.class);
    } catch (IOException e) {
      System.out.println("EndpointMedium: createText: IOException e !");
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (newText == null) {
      System.out.println("EndpointMedium: createText: newText == null !");
      return Response.status(Status.BAD_REQUEST).build();
    }
    // sanitize object data
    // update log metadata
    // Not necessary, a text will always be created in conjunction with a medium

    // persist text
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(newText);
    entityManager.flush();
    entityTransaction.commit();
    entityManager.refresh(newText);
    entityManager.refresh(newText.getMedium());

    // add log entry
    UserLogManager.getLogger().addLogEntry(newText.getMedium().getCreatedByUserAccount().getId(),
            UserLogManager.LogEvents.TEXTCREATED);
    return Response.ok().entity(newText).build();
  }

  @PATCH
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("text/{id}")
  @Secured
  public Response updateText(@PathParam("id") int id, String jsonData) {
    // System.out.println("EndpointMedium: update text - jsonData: " + jsonData);
    ObjectMapper mapper = new ObjectMapper();
    MediumText updatedText = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumText text = entityManager.find(MediumText.class, id);
    if (text == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      updatedText = mapper.readValue(jsonData, MediumText.class);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedText == null) {
      return Response.notModified().build();
    }

    // update text
    if (updatedText.getContent() != null) {
      text.setContent(updatedText.getContent());
    }

    // update log metadata
    text.getMedium().setLastEditedAt(new Timestamp(System.currentTimeMillis()));
    if (containerRequestContext.getProperty("TIMAAT.userID") != null) {
      text.getMedium().setLastEditedByUserAccount(
              (entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID"))));
    }
    else {
      // DEBUG do nothing - production system should abort with internal server error
    }
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(text);
    entityManager.persist(text);
    entityTransaction.commit();
    entityManager.refresh(text);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.TEXTEDITED);
    return Response.ok().entity(text).build();
  }

  @DELETE
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("text/{id}")
  @Secured
  public Response deleteText(@PathParam("id") int id) {
    // System.out.println("EndpointMedium: deleteText with id: "+ id);
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, id);
    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    MediumText text = entityManager.find(MediumText.class, id);
    if (text == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(text);
    entityManager.remove(text.getMedium().getDisplayTitle());
    entityManager.remove(text.getMedium()); // remove text, then corresponding medium
    entityTransaction.commit();
    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.TEXTDELETED);
    return Response.ok().build();
  }

  @POST
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("video/{id}")
  @Secured
  public Response createVideo(@PathParam("id") int id, String jsonData) {

    // System.out.println("EndpointMedium: createVideo jsonData: "+jsonData);
    ObjectMapper mapper = new ObjectMapper();
    MediumVideo newVideo = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();

    // parse JSON data
    try {
      newVideo = mapper.readValue(jsonData, MediumVideo.class);
    } catch (IOException e) {
      System.out.println("EndpointMedium: createVideo: IOException e !");
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (newVideo == null) {
      System.out.println("EndpointMedium: createVideo: newVideo == null !");
      return Response.status(Status.BAD_REQUEST).build();
    }

    // sanitize object data

    // update log metadata
    // Not necessary, a video will always be created in conjunction with a medium

    // persist video
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(newVideo);
    entityManager.flush();
    entityTransaction.commit();
    entityManager.refresh(newVideo);
    entityManager.refresh(newVideo.getMedium());

    // add log entry
    UserLogManager.getLogger().addLogEntry(newVideo.getMedium().getCreatedByUserAccount().getId(),
            UserLogManager.LogEvents.VIDEOCREATED);

    return Response.ok().entity(newVideo).build();

  }

  @PATCH
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("video/{id}")
  @Secured
  public Response updateVideo(@PathParam("id") int id, String jsonData) {

    // System.out.println("EndpointMedium: update video - jsonData: " + jsonData);
    ObjectMapper mapper = new ObjectMapper();
    MediumVideo updatedVideo = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumVideo video = entityManager.find(MediumVideo.class, id);

    if (video == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      updatedVideo = mapper.readValue(jsonData, MediumVideo.class);
    } catch (IOException e) {
      System.out.println("EndpointMedium: update video - IOException: " + e.getMessage());
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedVideo == null) {
      return Response.notModified().build();
    }

    // update video
    if (updatedVideo.getLength() > 0) {
      video.setLength(updatedVideo.getLength());
    }
    if (updatedVideo.getVideoCodec() != null) {
      video.setVideoCodec(updatedVideo.getVideoCodec());
    }
    if (updatedVideo.getWidth() > 0) {
      video.setWidth(updatedVideo.getWidth());
    }
    if (updatedVideo.getHeight() > 0) {
      video.setHeight(updatedVideo.getHeight());
    }
    if (updatedVideo.getFrameRate() > 0) {
      video.setFrameRate(updatedVideo.getFrameRate());
    }
    if (updatedVideo.getDataRate() > 0) {
      video.setDataRate(updatedVideo.getDataRate());
    }
    if (updatedVideo.getTotalBitrate() > 0) {
      video.setTotalBitrate(updatedVideo.getTotalBitrate());
    }
    if (updatedVideo.getIsEpisode() != null) {
      video.setIsEpisode(updatedVideo.getIsEpisode());
    }

    // update log metadata
    video.getMedium().setLastEditedAt(new Timestamp(System.currentTimeMillis()));
    if (containerRequestContext.getProperty("TIMAAT.userID") != null) {
      video.getMedium().setLastEditedByUserAccount(
              (entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID"))));
    }
    else {
      // DEBUG do nothing - production system should abort with internal server error
    }

    // persist video
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(video);
    entityManager.persist(video);
    entityTransaction.commit();
    entityManager.refresh(video);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.VIDEOEDITED);

    // TODO necessary?
    // video.getMedium().getFileStatus("video");
    // video.getMedium().getViewToken();

    return Response.ok().entity(video).build();

  }

  @DELETE
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("video/{id}")
  @Secured
  public Response deleteVideo(@PathParam("id") int id) {

    // System.out.println("EndpointMedium: deleteVideo with id: "+ id);
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, id);
    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    MediumVideo video = entityManager.find(MediumVideo.class, id);
    if (video == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    // persist video
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(video);
    entityManager.remove(video.getMedium().getDisplayTitle());
    entityManager.remove(video.getMedium()); // remove video, then corresponding medium
    entityTransaction.commit();

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.VIDEODELETED);

    return Response.ok().build();
  }

  @POST
  @Produces("image/png")
  @Path("video/{id}/thumbnail")
  @Secured
  public Response updateMediumVideoThumbnail(@PathParam("id") int id, @Valid UpdateMediumVideoThumbnailPayload updateMediumVideoThumbnailPayload) throws IOException, VideoEngineException {
    java.nio.file.Path thumbnailPath = createVideoThumbnail(id,
            updateMediumVideoThumbnailPayload.getThumbnailPositionMs());
    return Response.ok().entity(thumbnailPath.toFile()).build();
  }

  @POST
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("videogame/{id}")
  @Secured
  public Response createVideogame(@PathParam("id") int id, String jsonData) {
    // System.out.println("EndpointMedium: createVideogame jsonData: "+jsonData);
    ObjectMapper mapper = new ObjectMapper();
    MediumVideogame newVideogame = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    // parse JSON data
    try {
      newVideogame = mapper.readValue(jsonData, MediumVideogame.class);
    } catch (IOException e) {
      System.out.println("EndpointMedium: createVideogame: IOException e !");
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (newVideogame == null) {
      System.out.println("EndpointMedium: createVideogame: newVideogame == null !");
      return Response.status(Status.BAD_REQUEST).build();
    }
    // sanitize object data
    // update log metadata
    // Not necessary, a videogame will always be created in conjunction with a medium
    // persist videogame
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(newVideogame);
    entityManager.flush();
    entityTransaction.commit();
    entityManager.refresh(newVideogame);
    entityManager.refresh(newVideogame.getMedium());

    // add log entry
    UserLogManager.getLogger().addLogEntry(newVideogame.getMedium().getCreatedByUserAccount().getId(),
            UserLogManager.LogEvents.VIDEOGAMECREATED);
    return Response.ok().entity(newVideogame).build();
  }

  @PATCH
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("videogame/{id}")
  @Secured
  public Response updateVideogame(@PathParam("id") int id, String jsonData) {
    // System.out.println("EndpointMedium: update videogame - jsonData: " + jsonData);
    ObjectMapper mapper = new ObjectMapper();
    MediumVideogame updatedVideogame = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumVideogame videogame = entityManager.find(MediumVideogame.class, id);
    if (videogame == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      updatedVideogame = mapper.readValue(jsonData, MediumVideogame.class);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedVideogame == null) {
      return Response.notModified().build();
    }

    // update videogame
    if (updatedVideogame.getIsEpisode() != null) {
      videogame.setIsEpisode(updatedVideogame.getIsEpisode());
    }

    // update log metadata
    videogame.getMedium().setLastEditedAt(new Timestamp(System.currentTimeMillis()));
    if (containerRequestContext.getProperty("TIMAAT.userID") != null) {
      videogame.getMedium().setLastEditedByUserAccount(
              (entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID"))));
    }
    else {
      // DEBUG do nothing - production system should abort with internal server error
    }
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(videogame);
    entityManager.persist(videogame);
    entityTransaction.commit();
    entityManager.refresh(videogame);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.VIDEOGAMEEDITED);
    return Response.ok().entity(videogame).build();
  }

  @DELETE
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("videogame/{id}")
  @Secured
  public Response deleteVideogame(@PathParam("id") int id) {
    // System.out.println("EndpointMedium: deleteVideogame with id: "+ id);
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, id);
    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    MediumVideogame videogame = entityManager.find(MediumVideogame.class, id);
    if (videogame == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(videogame);
    entityManager.remove(videogame.getMedium().getDisplayTitle());
    entityManager.remove(videogame.getMedium()); // remove videogame, then corresponding medium
    entityTransaction.commit();
    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.VIDEOGAMEDELETED);
    return Response.ok().build();
  }

  @POST
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("title/{id}")
  @Secured
  public Response createTitle(@PathParam("id") int id, String jsonData) {

    // System.out.println("EndpointMedium: createTitle: jsonData: "+jsonData);
    ObjectMapper mapper = new ObjectMapper();
    Title newTitle = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();

    // parse JSON data
    try {
      newTitle = mapper.readValue(jsonData, Title.class);
    } catch (IOException e) {
      System.out.println("EndpointMedium: createTitle: IOException e !");
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (newTitle == null) {
      System.out.println("EndpointMedium: createTitle: newTitle == null !");
      return Response.status(Status.BAD_REQUEST).build();
    }
    // sanitize object data
    newTitle.setId(0);
    Language language = entityManager.find(Language.class, newTitle.getLanguage().getId());
    newTitle.setLanguage(language);

    // update log metadata
    // Not necessary, a title will always be created in conjunction with a medium

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
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("{mediumId}/title/{id}")
  @Secured
  public Response addTitle(@PathParam("mediumId") int mediumId, @PathParam("id") int id, String jsonData) {
    // System.out.println("EndpointMedium: addTitle: jsonData: "+jsonData);

    ObjectMapper mapper = new ObjectMapper();
    Title newTitle = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();

    // parse JSON data
    try {
      newTitle = mapper.readValue(jsonData, Title.class);
    } catch (IOException e) {
      System.out.println("EndpointMedium: addTitle: IOException e !");
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (newTitle == null) {
      System.out.println("EndpointMedium: addTitle: newTitle == null !");
      return Response.status(Status.BAD_REQUEST).build();
    }
    // sanitize object data
    newTitle.setId(0);

    Language language = entityManager.find(Language.class, newTitle.getLanguage().getId());
    newTitle.setLanguage(language);

    Medium medium = entityManager.find(Medium.class, mediumId);

    // update log metadata
    // Not necessary, a title will always be created in conjunction with a medium

    // persist title
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(language);
    entityManager.persist(newTitle);
    entityManager.flush();
    newTitle.setLanguage(language); //! TODO already set above
    entityTransaction.commit();
    entityManager.refresh(newTitle);
    entityManager.refresh(language);

    // create medium_has_title-table entries
    entityTransaction.begin();
    medium.getTitles().add(newTitle);
    newTitle.getMediums3().add(medium);
    entityManager.merge(newTitle);
    entityManager.merge(medium);
    entityManager.persist(newTitle);
    entityManager.persist(medium);
    entityTransaction.commit();
    entityManager.refresh(medium);
    entityManager.refresh(newTitle);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.TITLECREATED);

    return Response.ok().entity(newTitle).build();
  }

  @PATCH
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("title/{id}")
  @Secured
  public Response updateTitle(@PathParam("id") int id, String jsonData) {
    // System.out.println("EndpointMedium: UPDATE TITLE - jsonData: " + jsonData);
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
    // log metadata will be updated with the corresponding medium
    // title.getMedium().setLastEditedAt(new Timestamp(System.currentTimeMillis()));
    // if ( containerRequestContext.getProperty("TIMAAT.userID") != null ) {
    // 	title.getMedium().setLastEditedByUserAccount((entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID"))));
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
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("title/{id}")
  @Secured
  public Response deleteTitle(@PathParam("id") int id) {
    // System.out.println("EndpointMedium: deleteTitle");
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
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  // @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("{mediumId}/languageTrack/{languageTypeId}/{languageId}")
  @Secured
  public Response addMediumHasLanguageItem(@PathParam("mediumId") int mediumId, @PathParam("languageTypeId") int mediumLanguageTypeId, @PathParam("languageId") int languageId) {
    // System.out.println("EndpointMedium: addLanguageTrack: "+mediumId+" "+mediumLanguageTypeId+ " "+languageId);

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Language language = entityManager.find(Language.class, languageId);
    if (language == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    Medium medium = entityManager.find(Medium.class, mediumId);
    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    MediumLanguageType mediumLanguageType = entityManager.find(MediumLanguageType.class, mediumLanguageTypeId);
    if (mediumLanguageType == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    MediumHasLanguage mhl = null;
    try {
      mhl = (MediumHasLanguage) entityManager.createQuery(
                                                     "SELECT mhl FROM MediumHasLanguage mhl WHERE mhl.language=:language AND mhl.medium=:medium AND mhl.mediumLanguageType=:mediumLanguageType")
                                             .setParameter("language", language).setParameter("medium", medium)
                                             .setParameter("mediumLanguageType", mediumLanguageType).getSingleResult();
    } catch (Exception e) {
      // doesn't matter
    }
    if (mhl == null) {
      mhl = new MediumHasLanguage();
      mhl.setLanguage(language);
      mhl.setMedium(medium);
      mhl.setMediumLanguageType(mediumLanguageType);
      try {
        EntityTransaction entityTransaction = entityManager.getTransaction();
        entityTransaction.begin();
        entityManager.persist(mhl);
        entityTransaction.commit();
        entityManager.refresh(medium);
        entityManager.refresh(mhl);
      } catch (Exception e) {
        e.printStackTrace();
        return Response.notModified().build();
      }
    }

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"), UserLogManager.LogEvents.MEDIUMEDITED);

    return Response.ok().entity(mhl).build();
  }

  @PATCH
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("{mediumId}/languageTrack/{languageTypeId}/{languageId}")
  @Secured
  public Response updateMediumHasLanguageItem(@PathParam("mediumId") int mediumId, @PathParam("languageTypeId") int mediumLanguageTypeId, @PathParam("languageId") int languageId, String jsonData) throws IOException {

    // System.out.println("EndpointMedium: updateLanguageTrack: jsonData: "+jsonData);
    ObjectMapper mapper = new ObjectMapper();
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Language language = entityManager.find(Language.class, languageId);
    if (language == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    Medium medium = entityManager.find(Medium.class, mediumId);
    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    MediumLanguageType mediumLanguageType = entityManager.find(MediumLanguageType.class, mediumLanguageTypeId);
    if (mediumLanguageType == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    JsonNode jsonNode = mapper.readTree(jsonData);
    int jsonNodeMedium = jsonNode.get("mediumId").asInt();
    int jsonNodeLanguage = jsonNode.get("languageId").asInt();
    int jsonNodeMediumLanguageType = jsonNode.get("mediumLanguageTypeId").asInt();
    Medium tempMedium = entityManager.find(Medium.class, jsonNodeMedium);
    Language tempLanguage = entityManager.find(Language.class, jsonNodeLanguage);
    MediumLanguageType tempLanguageType = entityManager.find(MediumLanguageType.class, jsonNodeMediumLanguageType);
    MediumHasLanguage mediumHasLanguage = new MediumHasLanguage();
    mediumHasLanguage.setMedium(tempMedium);
    mediumHasLanguage.setLanguage(tempLanguage);
    mediumHasLanguage.setMediumLanguageType(tempLanguageType);

    // TODO This is a temporary fix until language track form is reworked
    // don't update if mediumHasLanguage does already exist
    MediumHasLanguage mhlInDB = null;
    try {
      mhlInDB = (MediumHasLanguage) entityManager.createQuery(
                                                         "SELECT mhlInDB FROM MediumHasLanguage mhlInDB WHERE mhlInDB.language=:language AND mhlInDB.medium=:medium AND mhlInDB.mediumLanguageType=:mediumLanguageType")
                                                 .setParameter("language", tempLanguage)
                                                 .setParameter("medium", tempMedium)
                                                 .setParameter("mediumLanguageType", tempLanguageType)
                                                 .getSingleResult();
    } catch (Exception e) {
      // doesn't matter
    }
    if (mhlInDB != null) {
      // entry already exists, don't add it
      return Response.notModified().build();
    }
    // END of TODO

    try {
      EntityTransaction entityTransaction = entityManager.getTransaction();
      entityTransaction.begin();
      entityManager.createQuery(
                           "UPDATE MediumHasLanguage mhl SET mhl.language=:language, mhl.mediumLanguageType=:mediumLanguageType WHERE mhl.medium=:medium AND mhl.language=:oldLanguage AND mhl.mediumLanguageType=:oldMediumLanguageType")
                   .setParameter("language", mediumHasLanguage.getLanguage())
                   .setParameter("medium", mediumHasLanguage.getMedium())
                   .setParameter("mediumLanguageType", mediumHasLanguage.getMediumLanguageType())
                   .setParameter("oldLanguage", language).setParameter("oldMediumLanguageType", mediumLanguageType)
                   .executeUpdate();
      // entityManager.persist(mhl);
      entityTransaction.commit();
      entityManager.refresh(medium);

    } catch (Exception e) {
      e.printStackTrace();
      return Response.notModified().build();
    }

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"), UserLogManager.LogEvents.MEDIUMEDITED);

    return Response.ok().entity(mediumHasLanguage).build();
  }

  @DELETE
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("{mediumId}/languageTrack/{languageTypeId}/{languageId}")
  @Secured
  public Response deleteMediumHasLanguageItem(@PathParam("mediumId") int mediumId, @PathParam("languageTypeId") int mediumLanguageTypeId, @PathParam("languageId") int languageId) {
    // System.out.println("EndpointMedium: deleteMediumHasLanguageItem");

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Language language = entityManager.find(Language.class, languageId);
    if (language == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    Medium medium = entityManager.find(Medium.class, mediumId);
    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    MediumLanguageType mediumLanguageType = entityManager.find(MediumLanguageType.class, mediumLanguageTypeId);
    if (mediumLanguageType == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    try {
      EntityTransaction entityTransaction = entityManager.getTransaction();
      entityTransaction.begin();
      entityManager.createQuery(
                           "DELETE FROM MediumHasLanguage mhl WHERE mhl.language=:language AND mhl.medium=:medium AND mhl.mediumLanguageType=:mediumLanguageType")
                   .setParameter("language", language).setParameter("medium", medium)
                   .setParameter("mediumLanguageType", mediumLanguageType).executeUpdate();
      entityTransaction.commit();
      entityManager.refresh(medium);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.notModified().build();
    }
    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) crc.getProperty("TIMAAT.userID"), UserLogManager.LogEvents.MEDIUMHASLANGUAGEDELETED);
    return Response.ok().build();
  }

  @POST
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("{mediumId}/hasActor/{actorId}/withRole/{roleId}")
  @Secured
  public Response addMediumHasActorWithRoles(@PathParam("mediumId") int mediumId, @PathParam("actorId") int actorId, @PathParam("roleId") int roleId) {
    // System.out.println("EndpointMedium: addMediumHasActorWithRoles");

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, mediumId);
    if (medium == null) {
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

    MediumHasActorWithRole mhawr = null;
    try {
      Query countQuery = entityManager.createQuery(
                                              "SELECT COUNT(mhawr) FROM MediumHasActorWithRole mhawr WHERE mhawr.medium=:medium AND mhawr.actor=:actor AND mhawr.role=:role")
                                      .setParameter("medium", medium).setParameter("actor", actor)
                                      .setParameter("role", role);
      long recordsTotal = (long) countQuery.getSingleResult();
      if (recordsTotal == 1) {
        mhawr = (MediumHasActorWithRole) entityManager.createQuery(
                                                              "SELECT mhawr FROM MediumHasActorWithRole mhawr WHERE mhawr.medium=:medium AND mhawr.actor=:actor AND mhawr.role=:role")
                                                      .setParameter("medium", medium).setParameter("actor", actor)
                                                      .setParameter("role", role).getSingleResult();
      }
    } catch (Exception e) {
      e.printStackTrace();
      // doesn't matter
    }
    if (mhawr == null) {
      mhawr = new MediumHasActorWithRole();
      mhawr.setMedium(medium);
      mhawr.setActor(actor);
      mhawr.setRole(role);
      try {
        EntityTransaction entityTransaction = entityManager.getTransaction();
        entityTransaction.begin();
        entityManager.persist(mhawr);
        entityTransaction.commit();
        entityManager.refresh(medium);
        entityManager.refresh(actor);
        entityManager.refresh(role);
        entityManager.refresh(mhawr);
      } catch (Exception e) {
        e.printStackTrace();
        return Response.notModified().build();
      }
    }

    // add log entry
    // UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"), UserLogManager.LogEvents.MEDIUMEDITED);

    return Response.ok().entity(mhawr).build();
  }

  @DELETE
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("{mediumId}/hasActor/{actorId}")
  @Secured
  public Response deleteActorFromMediumHasActorWithRoles(@PathParam("mediumId") int mediumId, @PathParam("actorId") int actorId) {
    // System.out.println("EndpointMedium: deleteMediumHasActorWithRolesItem");
    // deletes all entries of medium-actor matches

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, mediumId);
    if (medium == null) {
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
                           "DELETE FROM MediumHasActorWithRole mhawr WHERE mhawr.medium=:medium AND mhawr.actor=:actor")
                   .setParameter("medium", medium).setParameter("actor", actor).executeUpdate();
      entityTransaction.commit();
      entityManager.refresh(medium);
      // entityManager.refresh(actor);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.notModified().build();
    }

    return Response.ok().build();
  }

  @DELETE
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("{mediumId}/hasActor/{actorId}/withRole/{roleId}")
  @Secured
  public Response deleteRoleFromMediumHasActorWithRoles(@PathParam("mediumId") int mediumId, @PathParam("actorId") int actorId, @PathParam("roleId") int roleId) {
    // System.out.println("EndpointMedium: deleteMediumHasActorWithRolesItem");
    // deletes all entries of medium-actor matches

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, mediumId);
    if (medium == null) {
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
                           "DELETE FROM MediumHasActorWithRole mhawr WHERE mhawr.medium=:medium AND mhawr.actor=:actor AND mhawr.role=:role")
                   .setParameter("medium", medium).setParameter("actor", actor).setParameter("role", role)
                   .executeUpdate();
      entityTransaction.commit();
      entityManager.refresh(medium);
      // entityManager.refresh(actor);
      // entityManager.refresh(role);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.notModified().build();
    }

    return Response.ok().build();
  }

  // Currently not in use
  @POST
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("source/{id}")
  @Secured
  public Response createSource(@PathParam("id") int id, String jsonData) {

    // System.out.println("EndpointMedium: createSource: jsonData: "+jsonData);
    ObjectMapper mapper = new ObjectMapper();
    Source newSource = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();

    // parse JSON data
    try {
      newSource = mapper.readValue(jsonData, Source.class);
    } catch (IOException e) {
      System.out.println("EndpointMedium: createSource: IOException e !");
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (newSource == null) {
      System.out.println("EndpointMedium: createSource: newSource == null !");
      return Response.status(Status.BAD_REQUEST).build();
    }
    // sanitize object data
    newSource.setId(0);
    Medium medium = entityManager.find(Medium.class, newSource.getMedium().getId());
    newSource.setMedium(medium);

    // update log metadata
    // Not necessary, a source will always be created in conjunction with a medium

    // persist source
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(newSource);
    medium.getSources().add(newSource);
    // medium.addSource(newSource);
    entityManager.persist(medium);
    entityManager.flush();
    newSource.setMedium(medium);
    entityTransaction.commit();
    entityManager.refresh(newSource);
    entityManager.refresh(medium);

    // add log entry
    // UserLogManager.getLogger().addLogEntry(newSource.getMediums1().get(0).getCreatedByUserAccount().getId(), UserLogManager.LogEvents.SOURCECREATED);

    return Response.ok().entity(newSource).build();
  }

  @PATCH
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("source/{id}")
  @Secured
  public Response updateSource(@PathParam("id") int id, String jsonData) {
    // System.out.println("EndpointMedium: UPDATE SOURCE - jsonData: " + jsonData);
    ObjectMapper mapper = new ObjectMapper();
    Source updatedSource = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Source source = entityManager.find(Source.class, id);
    if (source == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      updatedSource = mapper.readValue(jsonData, Source.class);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedSource == null) {
      return Response.notModified().build();
    }

    // update source
    if (updatedSource.getIsPrimarySource() != null) {
      source.setIsPrimarySource(updatedSource.getIsPrimarySource());
    }
    source.setUrl(updatedSource.getUrl());
    source.setLastAccessed(updatedSource.getLastAccessed());
    if (updatedSource.getIsStillAvailable() != null) {
      source.setIsStillAvailable(updatedSource.getIsStillAvailable());
    }

    // update log metadata
    // log metadata will be updated with the corresponding medium
    // source.getMedium().setLastEditedAt(new Timestamp(System.currentTimeMillis()));
    // if ( containerRequestContext.getProperty("TIMAAT.userID") != null ) {
    // 	source.getMedium().setLastEditedByUserAccount((entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID"))));
    // } else {
    // 	// DEBUG do nothing - production system should abort with internal server error
    // }

    // persist source
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(source);
    entityManager.persist(source);
    entityTransaction.commit();
    entityManager.refresh(source);

    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.SOURCEEDITED);
    return Response.ok().entity(source).build();
  }

  @DELETE
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("source/{id}")
  @Secured
  public Response deleteSource(@PathParam("id") int id) {
    // System.out.println("EndpointMedium: deleteSource");
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Source source = entityManager.find(Source.class, id);
    if (source == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(source);
    entityTransaction.commit();
    // add log entry
    UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
            UserLogManager.LogEvents.SOURCEDELETED);
    return Response.ok().build();
  }

  @POST
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("audioPostProduction/{id}")
  @Secured
  public Response createAudioPostProduction(@PathParam("id") int id) {
    // System.out.println("EndpointMedium: createAudioPostProduction: ");
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();

    // parse JSON data
    AudioPostProduction audioPostProduction = new AudioPostProduction();

    // sanitize object data
    audioPostProduction.setId(0);

    // persist analysis method
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(audioPostProduction);
    entityManager.flush();
    entityTransaction.commit();
    entityManager.refresh(audioPostProduction);
    // entityManager.refresh(audioPostProduction.getAnalysisMethod());

    // add log entry
    // UserLogManager.getLogger().addLogEntry(newAnalysis.getCreatedByUserAccount().getId(), UserLogManager.LogEvents.ANALYSISCREATED);
    return Response.ok().entity(audioPostProduction).build();
  }

  @DELETE
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("audioPostProduction/{id}")
  @Secured
  public Response deleteAudioPostProduction(@PathParam("id") int id) {
    // System.out.println("EndpointMedium: deleteAudioPostProduction");
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AudioPostProduction audioPostProduction = entityManager.find(AudioPostProduction.class, id);

    if (audioPostProduction == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.remove(audioPostProduction);
    //* ON DELETE CASCADE deletes connected audio_post_production_translation entries
    entityTransaction.commit();

    // add log entry
    // UserLogManager.getLogger()
    // 							.addLogEntry((int) containerRequestContext
    // 							.getProperty("TIMAAT.userID"), UserLogManager.LogEvents.ROLEDELETED);
    return Response.ok().build();
  }

  @POST
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("audioPostProduction/{id}/translation")
  @Secured
  public Response createAudioPostProductionTranslation(@PathParam("id") int id, String jsonData) {
    // System.out.println("EndpointMedium: createAudioPostProductionTranslation: " + jsonData);
    ObjectMapper mapper = new ObjectMapper();
    AudioPostProductionTranslation audioPostProductionTranslation = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();

    // parse JSON data
    try {
      audioPostProductionTranslation = mapper.readValue(jsonData, AudioPostProductionTranslation.class);
    } catch (IOException e) {
      System.out.println("EndpointMedium: createAudioPostProductionTranslation: IOException e !");
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (audioPostProductionTranslation == null) {
      System.out.println("EndpointMedium: createAudioPostProductionTranslation: newAudio == null !");
      return Response.status(Status.BAD_REQUEST).build();
    }

    // sanitize object data
    audioPostProductionTranslation.setId(0);
    Language language = entityManager.find(Language.class, audioPostProductionTranslation.getLanguage().getId());
    audioPostProductionTranslation.setLanguage(language);
    AudioPostProduction audioPostProduction = entityManager.find(AudioPostProduction.class, id);
    audioPostProductionTranslation.setAudioPostProduction(audioPostProduction);

    // update log metadata
    // Timestamp creationDate = new Timestamp(System.currentTimeMillis());
    // newAnalysis.setCreatedAt(creationDate);
    // newAnalysis.setLastEditedAt(creationDate);
    // if ( containerRequestContext.getProperty("TIMAAT.userID") != null ) {
    // 	newAnalysis.setCreatedByUserAccount(entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID")));
    // 	newAnalysis.setLastEditedByUserAccount((entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID"))));
    // } else {
    // 	// DEBUG do nothing - production system should abort with internal server error
    // 	return Response.serverError().build();
    // }

    // persist analysis method
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.persist(language);
    entityManager.persist(audioPostProductionTranslation);
    entityManager.flush();
    audioPostProductionTranslation.setLanguage(language);
    audioPostProductionTranslation.setAudioPostProduction(audioPostProduction);
    entityTransaction.commit();
    entityManager.refresh(audioPostProductionTranslation);
    entityManager.refresh(language);
    entityManager.refresh(audioPostProduction);
    // entityManager.refresh(audioPostProduction.getAnalysisMethod());

    // add log entry
    // UserLogManager.getLogger().addLogEntry(newAnalysis.getCreatedByUserAccount().getId(), UserLogManager.LogEvents.ANALYSISCREATED);
    return Response.ok().entity(audioPostProductionTranslation).build();
  }

  @PATCH
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("audioPostProduction/{id}/translation")
  @Secured
  public Response updateAudioPostProductionTranslation(@PathParam("id") int id, String jsonData) {
    // System.out.println("EndpointMedium: updateAudioPostProductionTranslation: " + jsonData);

    ObjectMapper mapper = new ObjectMapper();
    AudioPostProductionTranslation updatedAudioPostProductionTranslation = null;
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    AudioPostProductionTranslation audioPostProductionTranslation = entityManager.find(
            AudioPostProductionTranslation.class, id);

    if (audioPostProductionTranslation == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // parse JSON data
    try {
      updatedAudioPostProductionTranslation = mapper.readValue(jsonData, AudioPostProductionTranslation.class);
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (updatedAudioPostProductionTranslation == null) {
      return Response.status(Status.BAD_REQUEST).build();
    }

    // update audioPostProductionTranslation
    audioPostProductionTranslation.setOverdubbing(updatedAudioPostProductionTranslation.getOverdubbing());
    audioPostProductionTranslation.setReverb(updatedAudioPostProductionTranslation.getReverb());
    audioPostProductionTranslation.setDelay(updatedAudioPostProductionTranslation.getDelay());
    audioPostProductionTranslation.setPanning(updatedAudioPostProductionTranslation.getPanning());
    audioPostProductionTranslation.setBass(updatedAudioPostProductionTranslation.getBass());
    audioPostProductionTranslation.setTreble(updatedAudioPostProductionTranslation.getTreble());

    // update log metadata
    // Timestamp creationDate = new Timestamp(System.currentTimeMillis());
    // newAnalysis.setCreatedAt(creationDate);
    // newAnalysis.setLastEditedAt(creationDate);
    // if ( containerRequestContext.getProperty("TIMAAT.userID") != null ) {
    // 	newAnalysis.setCreatedByUserAccount(entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID")));
    // 	newAnalysis.setLastEditedByUserAccount((entityManager.find(UserAccount.class, containerRequestContext.getProperty("TIMAAT.userID"))));
    // } else {
    // 	// DEBUG do nothing - production system should abort with internal server error
    // 	return Response.serverError().build();
    // }

    // persist analysis method
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    entityManager.merge(audioPostProductionTranslation);
    entityManager.persist(audioPostProductionTranslation);
    entityTransaction.commit();
    entityManager.refresh(audioPostProductionTranslation);

    // add log entry
    // UserLogManager.getLogger().addLogEntry(newAnalysis.getCreatedByUserAccount().getId(), UserLogManager.LogEvents.ANALYSISCREATED);
    return Response.ok().entity(audioPostProductionTranslation).build();
  }

  @POST
  @Path("audio/{id}/upload")
  @Consumes(jakarta.ws.rs.core.MediaType.MULTIPART_FORM_DATA)
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  public Response uploadAudio(@PathParam("id") int id, @FormDataParam("file") InputStream uploadedInputStream) throws TaskServiceException {
    int userId = (int) containerRequestContext.getProperty("TIMAAT.userID");
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumAudio mediumAudio = entityManager.find(MediumAudio.class, id);
    if (mediumAudio == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    if (mediumAudio.getMedium().getFileStatus().compareTo("noFile") != 0) {
      return Response.status(Status.FORBIDDEN).entity("ERROR::Audio file already exists").build();
    }

    try {
      // TODO assume MP3 upload only
      java.nio.file.Path mediumFilePath;
      try (TemporaryFile temporaryFile = temporaryFileStorage.createTemporaryFile()) {
        BufferedOutputStream stream = new BufferedOutputStream(
                new FileOutputStream(temporaryFile.getTemporaryFilePath().toFile()));
        byte[] bytes = new byte[1024];
        int sizeRead;
        while ((sizeRead = uploadedInputStream.read(bytes, 0, 1024)) > 0) {
          stream.write(bytes, 0, sizeRead);
        }
        stream.flush();
        stream.close();

        mediumFilePath = audioFileStorage.persistOriginalFile(temporaryFile.getTemporaryFilePath(), id);
      }

      // get data from ffmpeg
      AudioInformation info = getAudioFileInfo(mediumFilePath);
      // mediumAudio.setAudioCodec("");
      mediumAudio.setLength(info.getDuration());
      mediumAudio.getMedium().setFilePath(mediumFilePath.toString());

      EntityTransaction entityTransaction = entityManager.getTransaction();
      entityTransaction.begin();
      entityManager.merge(mediumAudio);
      entityManager.merge(mediumAudio.getMedium());
      entityManager.persist(mediumAudio);
      entityManager.persist(mediumAudio.getMedium());
      entityManager.flush();
      entityTransaction.commit();
      entityManager.refresh(mediumAudio);

      taskService.executeMediumAudioAnalysisTask(id, SupportedMediumType.AUDIO);
      // add log entry
      UserLogManager.getLogger().addLogEntry(userId, UserLogManager.LogEvents.MEDIUMCREATED);
      executeDefaultTranscriptionCreation(id, userId);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return Response.ok().entity(mediumAudio).build();
  }

  @POST
  @Path("image/{id}/upload")
  @Consumes(jakarta.ws.rs.core.MediaType.MULTIPART_FORM_DATA)
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  public Response uploadImage(@PathParam("id") int id, @FormDataParam("file") InputStream uploadedInputStream, @FormDataParam("file") FormDataContentDisposition fileDetail) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumImage mediumImage = entityManager.find(MediumImage.class, id);
    if (mediumImage == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    if (mediumImage.getMedium().getFileStatus().compareTo("noFile") != 0) {
      return Response.status(Status.FORBIDDEN).entity("ERROR::Image file already exists").build();
    }

    try {
      ImageFileType imageFileType = fileDetail.getFileName().toLowerCase()
                                              .endsWith(".jpg") ? ImageFileType.JPG : ImageFileType.PNG;

      java.nio.file.Path mediumFilePath;
      try (TemporaryFile temporaryFile = temporaryFileStorage.createTemporaryFile()) {
        BufferedOutputStream stream = new BufferedOutputStream(
                new FileOutputStream(temporaryFile.getTemporaryFilePath().toFile()));
        byte[] bytes = new byte[1024];
        int sizeRead;
        while ((sizeRead = uploadedInputStream.read(bytes, 0, 1024)) > 0) {
          stream.write(bytes, 0, sizeRead);
        }
        stream.flush();
        stream.close();

        mediumFilePath = imageFileStorage.persistOriginalImageFile(temporaryFile.getTemporaryFilePath(), id,
                imageFileType);
      }

      // get image metadata
      ImageInformation info = getImageFileInfo(mediumFilePath, imageFileType);
      mediumImage.setWidth(info.getWidth());
      mediumImage.setHeight(info.getHeight());

      EntityTransaction entityTransaction = entityManager.getTransaction();
      entityTransaction.begin();
      entityManager.merge(mediumImage);
      entityManager.merge(mediumImage.getMedium());
      entityManager.persist(mediumImage);
      entityManager.persist(mediumImage.getMedium());
      entityManager.flush();
      entityTransaction.commit();
      entityManager.refresh(mediumImage);

      double width = mediumImage.getWidth();
      double height = mediumImage.getHeight();
      double aspectRatio = (double) width / height;
      double sixteenToNine = 16.0 / 9.0;
      double scaleFactor = 1.0;
      int targetWidth = 0;
      int targetHeight = 0;
      // create thumbnail
      if (aspectRatio >= sixteenToNine && width > 160) {
        targetWidth = 160;
        scaleFactor = 160 / width;
        targetHeight = (int) Math.floor(height * scaleFactor);
      }
      else if (aspectRatio < sixteenToNine && height > 90) {
        scaleFactor = 90 / height;
        targetWidth = (int) Math.floor(width * scaleFactor);
        targetHeight = 90;
      }

      try (TemporaryFile temporaryFile = temporaryFileStorage.createTemporaryFile()) {
        BufferedImage bufferedImage = ImageIO.read(mediumFilePath.toFile());
        if (targetHeight > 0 && targetWidth > 0) {
          bufferedImage = resizeImage(bufferedImage, targetWidth, targetHeight);
        }
        ImageIO.write(bufferedImage, "png", temporaryFile.getTemporaryFilePath().toFile());
        imageFileStorage.persistThumbnailImageFile(temporaryFile.getTemporaryFilePath(), id);
      }


      targetHeight = 0;
      targetWidth = 0;
      // create scaled image for default view
      if (aspectRatio >= sixteenToNine && width > 854) {
        targetWidth = 854;
        scaleFactor = 854 / width;
        targetHeight = (int) Math.floor(height * scaleFactor);
      }
      else if (aspectRatio < sixteenToNine && height > 480) {
        scaleFactor = 480 / height;
        targetWidth = (int) Math.floor(width * scaleFactor);
        targetHeight = 480;
      }

      try (TemporaryFile temporaryFile = temporaryFileStorage.createTemporaryFile()) {
        BufferedImage bufferedImage = ImageIO.read(mediumFilePath.toFile());
        if (targetHeight > 0 && targetWidth > 0) {
          bufferedImage = resizeImage(bufferedImage, targetWidth, targetHeight);
        }
        ImageIO.write(bufferedImage, "png", temporaryFile.getTemporaryFilePath().toFile());
        imageFileStorage.persistScaledImageFile(temporaryFile.getTemporaryFilePath(), id);
      }

      mediumImage.getMedium().setViewToken(issueFileToken(mediumImage.getMediumId()));
      // add log entry
      UserLogManager.getLogger().addLogEntry((int) containerRequestContext.getProperty("TIMAAT.userID"),
              UserLogManager.LogEvents.MEDIUMCREATED);

    } catch (IOException e) {
      e.printStackTrace();
    }

    return Response.ok().entity(mediumImage).build();
  }

  @GET
  @Path("/{id}/mediumAudioAnalysis/frequencyInformation")
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  public Response getVideoFrequencyInformation(@PathParam("id") int id, @QueryParam("startPositionMs") Integer startPositionMs, @QueryParam("endPositionMs") Integer endPositionMs, @QueryParam("token") String fileToken) throws IOException {
    // verify token
    if (fileToken == null) {
      return Response.status(401).build();
    }
    int tokenMediumID = 0;
    try {
      tokenMediumID = validateFileToken(fileToken);
    } catch (Exception e) {
      return Response.status(401).build();
    }
    if (tokenMediumID != id) {
      return Response.status(401).build();
    }


    Optional<java.nio.file.Path> frequencyFilePath = videoFileStorage.getPathToFrequencyFile(id)
                                                                     .or(() -> audioFileStorage.getPathToOriginalFile(
                                                                             id));

    if (frequencyFilePath.isPresent()) {
      FrequencyFileReader frequencyFileReader = new FrequencyFileReader(frequencyFilePath.get());
      Optional<FrequencyInformation> frequencyInformation = frequencyFileReader.getFrequencyInformation(startPositionMs,
              endPositionMs);
      if (frequencyInformation.isPresent()) {
        return Response.ok().entity(frequencyInformation.get()).build();
      }
      else {
        return Response.status(Status.BAD_REQUEST).build();
      }
    }

    return Response.status(Status.NOT_FOUND).build();
  }


  @POST
  @Path("/{id}/mediumAudioAnalysis/start")
  @Consumes(jakarta.ws.rs.core.MediaType.TEXT_PLAIN)
  public Response triggerMediumVideoAudioAnalysis(@PathParam("id") int id, String fileToken) throws TaskServiceException {
    // verify token
    if (fileToken == null) {
      return Response.status(401).build();
    }
    int tokenMediumID = 0;
    try {
      tokenMediumID = validateFileToken(fileToken);
    } catch (Exception e) {
      return Response.status(401).build();
    }
    if (tokenMediumID != id) {
      return Response.status(401).build();
    }


    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumAudioAnalysis mediumAudioAnalysis = entityManager.find(MediumAudioAnalysis.class, id);

    if (mediumAudioAnalysis == null || TaskState.FAILED.getDatabaseId() == mediumAudioAnalysis.getAudioAnalysisState()
                                                                                              .getId() || TaskState.DONE.getDatabaseId() == mediumAudioAnalysis.getAudioAnalysisState()
                                                                                                                                                               .getId()) {
      SupportedMediumType mediumType = null;
      int mediumTypeId = mediumAudioAnalysis.getMedium().getMediaType().getId();

      if (mediumTypeId == 1) {
        mediumType = SupportedMediumType.AUDIO;
      }
      else if (mediumTypeId == 6) {
        mediumType = SupportedMediumType.VIDEO;
      }
      else {
        return Response.status(Status.BAD_REQUEST).build();
      }
      taskService.executeMediumAudioAnalysisTask(id, mediumType);

      return Response.noContent().build();
    }

    return Response.status(Status.PRECONDITION_FAILED).build();
  }

  @POST
  @Path("video/{id}/upload")
  @Consumes(jakarta.ws.rs.core.MediaType.MULTIPART_FORM_DATA)
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  public Response uploadVideo(@PathParam("id") int id, @FormDataParam("file") InputStream uploadedInputStream, @FormDataParam("file") FormDataContentDisposition fileDetail) throws Exception {
    int userId = (int) containerRequestContext.getProperty("TIMAAT.userID");

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    MediumVideo mediumVideo = entityManager.find(MediumVideo.class, id);
    int mediumId = mediumVideo.getMediumId();
    if (mediumVideo == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    if (mediumVideo.getMedium().getFileStatus().compareTo("noFile") != 0) {
      return Response.status(Status.FORBIDDEN).entity("ERROR::Video file already exists").build();
    }

    try {
      // TODO assume MP4 upload only
      java.nio.file.Path originalVideoFilePath;
      try (TemporaryFile uploadTempFile = temporaryFileStorage.createTemporaryFile()) {
        BufferedOutputStream stream = new BufferedOutputStream(
                new FileOutputStream(uploadTempFile.getTemporaryFilePath().toFile()));
        byte[] bytes = new byte[1024];
        int sizeRead;
        while ((sizeRead = uploadedInputStream.read(bytes, 0, 1024)) > 0) {
          stream.write(bytes, 0, sizeRead);
        }
        stream.flush();
        stream.close();

        originalVideoFilePath = videoFileStorage.persistOriginalFile(uploadTempFile.getTemporaryFilePath(), mediumId);
      }

      // get data from ffmpeg
      VideoInformation info = getVideoFileInfo(originalVideoFilePath);
      mediumVideo.setWidth(info.getWidth());
      mediumVideo.setHeight(info.getHeight());
      mediumVideo.setFrameRate(info.getFramerate());
      mediumVideo.setVideoCodec("");
      mediumVideo.setLength(info.getDuration());


      EntityTransaction entityTransaction = entityManager.getTransaction();
      entityTransaction.begin();
      entityManager.merge(mediumVideo);
      entityManager.merge(mediumVideo.getMedium());
      entityManager.persist(mediumVideo);
      entityManager.persist(mediumVideo.getMedium());
      entityManager.flush();
      entityTransaction.commit();
      entityManager.refresh(mediumVideo);

      createVideoThumbnail(mediumId, DEFAULT_THUMBNAIL_TIMESTAMP_MS);
      taskService.executeMediumAudioAnalysisTask(mediumId, SupportedMediumType.VIDEO);
      // add log entry
      UserLogManager.getLogger().addLogEntry(userId, UserLogManager.LogEvents.MEDIUMCREATED);
      executeDefaultTranscriptionCreation(mediumId, userId);

    } catch (IOException e) {
      e.printStackTrace();
    }

    return Response.ok().entity(mediumVideo).build();
  }

  private void executeDefaultTranscriptionCreation(int mediumId, int createdByUserAccountId) {
    if (transcriptionService.isFeatureEnabled() && systemSettingStorage.isAutoTranscribeUploadsEnabled()) {
      try {
        transcriptionService.createTranscriptionWithDefaultModel(mediumId, createdByUserAccountId);
      } catch (Exception e) {
        logger.log(Level.WARNING, "Error during executing default transcription creation of medium " + mediumId, e);
      }
    }
  }

  @HEAD
  @Path("audio/{id}/download")
  @Produces("audio/mpeg")
  public Response getAudioFileInfo(@PathParam("id") int id, @QueryParam("token") String fileToken) {

    // verify token
    if (fileToken == null) {
      return Response.status(401).build();
    }
    int tokenMediumID = 0;
    try {
      tokenMediumID = validateFileToken(fileToken);
    } catch (Exception e) {
      return Response.status(401).build();
    }

    if (tokenMediumID != id) {
      return Response.status(401).build();
    }

    Optional<java.nio.file.Path> audioFilePath = audioFileStorage.getPathToOriginalFile(id);
    if (audioFilePath.isPresent()) {
      File audioFile = audioFilePath.get().toFile();
      return Response.ok().status(Response.Status.PARTIAL_CONTENT)
                     .header(HttpHeaders.CONTENT_LENGTH, audioFile.length()).header("Accept-Ranges", "bytes").build();
    }

    return Response.status(Status.NOT_FOUND).build();
  }

  @HEAD
  @Path("video/{id}/download")
  @Produces("video/mp4")
  public Response getVideoFileInfo(@PathParam("id") int id, @QueryParam("token") String fileToken) {

    // verify token
    if (fileToken == null) {
      return Response.status(401).build();
    }
    int tokenMediumID = 0;
    try {
      tokenMediumID = validateFileToken(fileToken);
    } catch (Exception e) {
      return Response.status(401).build();
    }
    if (tokenMediumID != id) {
      return Response.status(401).build();
    }
    Optional<java.nio.file.Path> convertedVideoFilePath = videoFileStorage.getPathToConvertedVideoFile(id);
    if (convertedVideoFilePath.isPresent()) {
      File file = convertedVideoFilePath.get().toFile();
      return Response.ok().status(Response.Status.PARTIAL_CONTENT).header(HttpHeaders.CONTENT_LENGTH, file.length())
                     .header("Accept-Ranges", "bytes").build();
    }

    Optional<java.nio.file.Path> originalVideoFilePath = videoFileStorage.getPathToOriginalFile(id);
    if (originalVideoFilePath.isPresent()) {
      File file = originalVideoFilePath.get().toFile();
      return Response.ok().status(Response.Status.PARTIAL_CONTENT).header(HttpHeaders.CONTENT_LENGTH, file.length())
                     .header("Accept-Ranges", "bytes").build();
    }

    return Response.status(Status.NOT_FOUND).build();
  }

  @GET
  @Path("audio/{id}/download")
  @Produces("audio/mp4")
  public Response getAudioFile(@Context HttpHeaders headers, @PathParam("id") int id, @QueryParam("token") String fileToken, @QueryParam("force") boolean force) {

    // verify token
    if (fileToken == null) {
      return Response.status(401).build();
    }
    int tokenMediumID = 0;
    try {
      tokenMediumID = validateFileToken(fileToken);
    } catch (Exception e) {
      return Response.status(401).build();
    }
    if (tokenMediumID != id) {
      return Response.status(401).build();
    }

    Medium medium = TIMAATApp.emf.createEntityManager().find(Medium.class, id);
    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    Optional<java.nio.file.Path> audioFilePath = audioFileStorage.getPathToOriginalFile(id);
    if (audioFilePath.isPresent()) {
      return downloadFile(audioFilePath.get(), headers, id + ".mp3");
    }

    return Response.status(Status.NOT_FOUND).build();
  }

  @GET
  @Path("video/{id}/download")
  @Produces("video/mp4")
  public Response getVideoFile(@Context HttpHeaders headers, @PathParam("id") int id, @QueryParam("token") String fileToken, @QueryParam("force") boolean force) {

    // verify token
    if (fileToken == null) {
      return Response.status(401).build();
    }
    int tokenMediumID = 0;
    try {
      tokenMediumID = validateFileToken(fileToken);
    } catch (Exception e) {
      return Response.status(401).build();
    }
    if (tokenMediumID != id) {
      return Response.status(401).build();
    }

    Medium m = TIMAATApp.emf.createEntityManager().find(Medium.class, id);
    if (m == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    Optional<java.nio.file.Path> convertedVideoFilePath = videoFileStorage.getPathToConvertedVideoFile(id);
    if (convertedVideoFilePath.isPresent()) {
      return downloadFile(convertedVideoFilePath.get(), headers, id + ".mp3");
    }

    Optional<java.nio.file.Path> originalVideoFilePath = videoFileStorage.getPathToOriginalFile(id);
    if (originalVideoFilePath.isPresent()) {
      return downloadFile(originalVideoFilePath.get(), headers, id + ".mp3");
    }
    return Response.status(Status.NOT_FOUND).build();
  }

  @GET
  @Path("{type}/{id}/status")
  @Produces(jakarta.ws.rs.core.MediaType.TEXT_PLAIN)
  @Secured
  public Response updateFileStatus(@PathParam("type") String type, @PathParam("id") int id) {
    return Response.ok().entity(mediumFileStatus(id, type)).build();
  }

  @GET
  @Path("{id}/audio/combined")
  @Produces("application/json")
  public Response getAudioWaveform(@PathParam("id") int id, @QueryParam("token") String fileToken) throws IOException {

    // verify token
    if (fileToken == null) {
      return Response.status(401).build();
    }
    int tokenMediumID = 0;
    try {
      tokenMediumID = validateFileToken(fileToken);
    } catch (Exception e) {
      return Response.status(401).build();
    }
    if (tokenMediumID != id) {
      return Response.status(401).build();
    }

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, id);

    Optional<WaveformBinaryFileReader> waveformBinaryFileReader = Optional.empty();
    if (medium.getMediumAudio() != null) {
      waveformBinaryFileReader = audioFileStorage.getPathToWaveformFile(id).map(WaveformBinaryFileReader::new);
    }
    else if (medium.getMediumVideo() != null) {
      waveformBinaryFileReader = videoFileStorage.getPathToWaveformFile(id).map(WaveformBinaryFileReader::new);
    }

    if (waveformBinaryFileReader.isPresent()) {
      List<Float> averageValues = waveformBinaryFileReader.get().readAverageValues();
      return Response.ok(averageValues).build();
    }
    else {
      return Response.status(Status.NOT_FOUND).build();
    }
  }

  @GET
  @Path("image/{id}/preview")
  @Produces("image/png")
  public Response getImagePreview(@PathParam("id") int id, @QueryParam("token") String fileToken) {
    // verify token
    if (fileToken == null) {
      return Response.status(401).build();
    }
    int tokenMediumId = 0;
    try {
      tokenMediumId = validateFileToken(fileToken);
    } catch (Exception e) {
      return Response.status(401).build();
    }
    if (tokenMediumId != id) {
      return Response.status(401).build();
    }

    File image = new File(TIMAATApp.timaatProps.getProp(
            PropertyConstants.STORAGE_LOCATION) + "medium/image/" + id + "/" + id + "-image-scaled.png");
    if (!image.exists() || !image.canRead()) {
      image = new File(servletContext.getRealPath("img/preview-placeholder.png"));
    }

    return Response.ok().entity(image).build();
  }

  @GET
  @Path("image/{id}/thumbnail")
  @Produces("image/png")
  public Response getImageThumbnail(@PathParam("id") int id, @QueryParam("token") String fileToken) {
    // verify token
    if (fileToken == null) {
      return Response.status(401).build();
    }
    int tokenMediumId = 0;
    try {
      tokenMediumId = validateFileToken(fileToken);
    } catch (Exception e) {
      return Response.status(401).build();
    }
    if (tokenMediumId != id) {
      return Response.status(401).build();
    }

    Optional<java.nio.file.Path> videoThumbnail = videoFileStorage.getPathToThumbnailFile(id);
    java.nio.file.Path thumbnailPath = videoThumbnail.orElse(
            java.nio.file.Path.of(servletContext.getRealPath("img/preview-placeholder.png")));

    return Response.ok().entity(thumbnailPath.toFile()).build();
  }

  // TODO: Usage of MediumFileStorage to access image files
  @HEAD
  @Path("image/{id}/download")
  @Produces({"image/png", "image/jpeg"})
  public Response getImageFileInfo(@PathParam("id") int id, @QueryParam("token") String fileToken) {

    // verify token
    if (fileToken == null) {
      return Response.status(401).build();
    }
    int tokenMediumID = 0;
    try {
      tokenMediumID = validateFileToken(fileToken);
    } catch (Exception e) {
      return Response.status(401).build();
    }
    if (tokenMediumID != id) {
      return Response.status(401).build();
    }

    Optional<java.nio.file.Path> scaledImagePath = imageFileStorage.getPathToScaledImage(id);
    if (scaledImagePath.isPresent()) {
      File file = scaledImagePath.get().toFile();
      return Response.ok().status(Response.Status.PARTIAL_CONTENT).header(HttpHeaders.CONTENT_LENGTH, file.length())
                     .header("Accept-Ranges", "bytes").build();
    }

    Optional<java.nio.file.Path> originalImagePath = imageFileStorage.getPathToOriginalImage(id);
    if (originalImagePath.isPresent()) {
      File file = originalImagePath.get().toFile();
      return Response.ok().status(Response.Status.PARTIAL_CONTENT).header(HttpHeaders.CONTENT_LENGTH, file.length())
                     .header("Accept-Ranges", "bytes").build();
    }
    return Response.status(Status.NOT_FOUND).build();
  }

  @GET
  @Path("image/{id}/download")
  @Produces({"image/png", "image/jpeg"})
  public Response getImageFile(@Context HttpHeaders headers, @PathParam("id") int id, @QueryParam("token") String fileToken, @QueryParam("force") boolean force) {
    // System.out.println("triggered getImageOriginalFile");
    // verify token
    if (fileToken == null) {
      return Response.status(401).build();
    }
    int tokenMediumId = 0;
    try {
      tokenMediumId = validateFileToken(fileToken);
    } catch (Exception e) {
      return Response.status(401).build();
    }
    if (tokenMediumId != id) {
      return Response.status(401).build();
    }

    Medium medium = TIMAATApp.emf.createEntityManager().find(Medium.class, id);
    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    Optional<java.nio.file.Path> scaledImagePath = imageFileStorage.getPathToScaledImage(id);
    if (scaledImagePath.isPresent()) {
      return downloadFile(scaledImagePath.get(), headers, id + ".png");
    }

    Optional<java.nio.file.Path> originalImagePath = imageFileStorage.getPathToOriginalImage(id);
    if (originalImagePath.isPresent()) {
      return downloadFile(originalImagePath.get(), headers, id + ".png");
    }
    return Response.status(Status.NOT_FOUND).build();
  }

  @GET
  @Path("video/{id}/thumbnail")
  @Produces("image/jpg")
  public Response getVideoThumbnail(@PathParam("id") int id, @QueryParam("time") Long timeMs, @QueryParam("token") String fileToken) {

    // verify token
    if (fileToken == null) {
      return Response.status(401).build();
    }
    int tokenMediumID = 0;
    try {
      tokenMediumID = validateFileToken(fileToken);
    } catch (Exception e) {
      return Response.status(401).build();
    }
    if (tokenMediumID != id) {
      return Response.status(401).build();
    }

    Optional<java.nio.file.Path> videoThumbnail = Optional.empty();
    if (timeMs != null) {
      videoThumbnail = videoFileStorage.getPathToFrameFile(id, timeMs);
    }

    if (videoThumbnail.isEmpty()) {
      videoThumbnail = videoFileStorage.getPathToThumbnailFile(id);
    }

    java.nio.file.Path thumbnailPath = videoThumbnail.orElse(
            java.nio.file.Path.of(servletContext.getRealPath("img/preview-placeholder.png")));
    return Response.ok().entity(thumbnailPath.toFile()).build();
  }

  /**
   * Gets list of annotations for medium (video)
   * @param id
   * @return
   */
  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("{id}/analysisLists")
  @Secured
  public Response getMediumAnalysisLists(@PathParam("id") int mediumId, @QueryParam("authToken") String authToken) {

    // verify auth token
    int userId = 0;
    if (AuthenticationFilter.isTokenValid(authToken)) {
      userId = AuthenticationFilter.getTokenClaimUserId(authToken);
    }
    else {
      return Response.status(Status.UNAUTHORIZED).build();
    }

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    String sql = "SELECT DISTINCT mal FROM MediumAnalysisList mal, UserAccountHasMediumAnalysisList uahmal WHERE mal.medium.id = :mediumId AND (mal.id = uahmal.mediumAnalysisList.id AND uahmal.userAccount.id = :userId OR mal.globalPermission > 0)";
    Query query = entityManager.createQuery(sql).setParameter("mediumId", mediumId).setParameter("userId", userId);
    List<MediumAnalysisList> mediumAnalysisLists = castList(MediumAnalysisList.class, query.getResultList());

    if (userId == 1) { // admin
      // find medium and get all its' mediumAnalysisLists
      Medium medium = entityManager.find(Medium.class, mediumId);
      if (medium == null) {
        return Response.status(Status.NOT_FOUND).build();
      }
      entityManager.refresh(medium);
      mediumAnalysisLists = medium.getMediumAnalysisLists();
    }

    return Response.ok().entity(mediumAnalysisLists).build();
  }

  @PUT
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("{mediumId}/mediumHasMusicList")
  @Secured
  public List<MediumHasMusic> updateMediumHasMusicList(@PathParam("mediumId") int mediumId, UpdateMediumHasMusicListPayload updateMediumHasMusicListPayload) {
    Map<Integer, Collection<TimeRange>> timeRangesByMusicId = updateMediumHasMusicListPayload.getMediumHasMusicListEntries()
                                                                                             .stream().collect(
                    Collectors.toMap(MediumHasMusicListEntry::getMusicId, MediumHasMusicListEntry::getTimeRanges));
    return mediumStorage.updateMediumHasMusicList(mediumId, timeRangesByMusicId);
  }


  @POST
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("{mediumId}/tag/{tagId}")
  @Secured
  public Response addExistingTag(@PathParam("mediumId") int mediumId, @PathParam("tagId") int tagId) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, mediumId);
    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    Tag tag = entityManager.find(Tag.class, tagId);
    if (tag == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    // attach tag to annotation and vice versa
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    medium.getTags().add(tag);
    tag.getMediums().add(medium);
    entityManager.merge(tag);
    entityManager.merge(medium);
    entityManager.persist(medium);
    entityManager.persist(tag);
    entityTransaction.commit();
    entityManager.refresh(medium);

    return Response.ok().entity(tag).build();
  }

  @DELETE
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("{mediumId}/tag/{tagId}")
  @Secured
  public Response removeTag(@PathParam("mediumId") int mediumId, @PathParam("tagId") int tagId) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, mediumId);
    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    Tag tag = entityManager.find(Tag.class, tagId);
    if (tag == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    // attach tag to annotation and vice versa
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    medium.getTags().remove(tag);
    tag.getMediums().remove(medium);
    entityManager.merge(tag);
    entityManager.merge(medium);
    entityManager.persist(medium);
    entityManager.persist(tag);
    entityTransaction.commit();
    entityManager.refresh(medium);

    return Response.ok().build();
  }

  @POST
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("{mediumId}/categorySet/{categorySetId}")
  @Secured
  public Response addExistingCategorySet(@PathParam("mediumId") int mediumId, @PathParam("categorySetId") int categorySetId) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, mediumId);
    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    CategorySet categorySet = entityManager.find(CategorySet.class, categorySetId);
    if (categorySet == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    // attach categorySet to annotation and vice versa
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    medium.getCategorySets().add(categorySet);
    categorySet.getMediums().add(medium);
    entityManager.merge(categorySet);
    entityManager.merge(medium);
    entityManager.persist(medium);
    entityManager.persist(categorySet);
    entityTransaction.commit();
    entityManager.refresh(medium);

    return Response.ok().entity(categorySet).build();
  }

  @DELETE
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("{mediumId}/categorySet/{categorySetId}")
  @Secured
  public Response removeCategorySet(@PathParam("mediumId") int mediumId, @PathParam("categorySetId") int categorySetId) {

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, mediumId);
    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    CategorySet categorySet = entityManager.find(CategorySet.class, categorySetId);
    if (categorySet == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    // TODO delete categories from media of matching categorySets
    List<Category> categoryList = new ArrayList<>();
    Set<CategorySetHasCategory> cshc = categorySet.getCategorySetHasCategories();
    Iterator<CategorySetHasCategory> itr = cshc.iterator();
    EntityTransaction entityTransaction = entityManager.getTransaction();

    while (itr.hasNext()) {
      categoryList.add(itr.next().getCategory());
    }
    // remove all categories from removed category set from the medium
    List<Category> mediumCategoryList = medium.getCategories();
    List<Category> categoriesToRemove = categoryList.stream().distinct().filter(mediumCategoryList::contains)
                                                    .collect(Collectors.toList());
    entityTransaction.begin();
    for (Category category : categoriesToRemove) {
      medium.getCategories().remove(category);
    }
    entityManager.merge(medium);
    entityManager.persist(medium);
    entityTransaction.commit();
    entityManager.refresh(medium);

    // attach categorySet to medium and vice versa
    entityTransaction.begin();
    medium.getCategorySets().remove(categorySet);
    categorySet.getMediums().remove(medium);
    entityManager.merge(categorySet);
    entityManager.merge(medium);
    entityManager.persist(medium);
    entityManager.persist(categorySet);
    entityTransaction.commit();
    entityManager.refresh(medium);

    return Response.ok().build();
  }

  @POST
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("{mediumId}/category/{categoryId}")
  @Secured
  public Response addExistingCategory(@PathParam("mediumId") int mediumId, @PathParam("categoryId") int categoryId) {
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, mediumId);
    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    Category category = entityManager.find(Category.class, categoryId);
    if (category == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    // attach category to annotation and vice versa
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    medium.getCategories().add(category);
    category.getMediums().add(medium);
    entityManager.merge(category);
    entityManager.merge(medium);
    entityManager.persist(medium);
    entityManager.persist(category);
    entityTransaction.commit();
    entityManager.refresh(medium);

    return Response.ok().entity(category).build();
  }

  @DELETE
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("{mediumId}/category/{categoryId}")
  @Secured
  public Response removeCategory(@PathParam("mediumId") int mediumId, @PathParam("categoryId") int categoryId) {
    // System.out.println("TCL: EndpointMedium - removeCategory");
    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    Medium medium = entityManager.find(Medium.class, mediumId);
    if (medium == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    Category category = entityManager.find(Category.class, categoryId);
    if (category == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    // attach category to medium and vice versa
    EntityTransaction entityTransaction = entityManager.getTransaction();
    entityTransaction.begin();
    medium.getCategories().remove(category);
    category.getMediums().remove(medium);
    entityManager.merge(category);
    entityManager.merge(medium);
    entityManager.persist(medium);
    entityManager.persist(category);
    entityTransaction.commit();
    entityManager.refresh(medium);
    return Response.ok().build();
  }

  @PATCH
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Path("fileLengthFix")
  @Secured
  public Response updateAllFileLengths(@QueryParam("authToken") String authToken) {
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

    Runtime r = Runtime.getRuntime();
    Process p;     // Process tracks one external native process
    BufferedReader is;  // reader for output of process
    String line;
    String filename;

    EntityManager entityManager = TIMAATApp.emf.createEntityManager();
    String sql = "SELECT m FROM Medium m WHERE m.filePath IS NOT NULL";
    Query query = entityManager.createQuery(sql);
    List<Medium> mediumList = castList(Medium.class, query.getResultList());
    EntityTransaction entityTransaction = entityManager.getTransaction();

    for (Medium medium : mediumList) {
      if (!(medium.getFilePath().contains("medium"))) {
        medium.setFilePath(null);
      }
      if ((medium.getMediumAudio() != null || medium.getMediumVideo() != null) && medium.getFilePath() != null) {
        filename = medium.getFilePath();
        String[] commandLine = {TIMAATApp.timaatProps.getProp(
                PropertyConstants.FFMPEG_LOCATION) + "ffprobe" + TIMAATApp.systemExt, "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=width,height,r_frame_rate,codec_name", "-show_entries", "format=duration", "-sexagesimal", "-of", "json", filename};
        try {
          p = r.exec(commandLine);
          is = new BufferedReader(new InputStreamReader(p.getInputStream()));

          try {
            p.waitFor();  // wait for process to complete
          } catch (InterruptedException e) {
            System.err.println(e);  // "can't happen"
          }

          String jsonString = "";
          while ((line = is.readLine()) != null) {
            if (line != null) {
              jsonString += line;
            }
          }

          JSONObject json = new JSONObject(jsonString);
          String sDuration = json.getJSONObject("format").getString("duration");
          String[] hms = sDuration.split(":");
          double dTotalSecs = Integer.parseInt(hms[0]) * 3600 + Integer.parseInt(hms[1]) * 60 + Double.parseDouble(
                  hms[2]);
          long lDuration = (long) (dTotalSecs * 1000);

          if (medium.getMediumAudio() != null) {
            medium.getMediumAudio().setLength(lDuration);
          }
          else if (medium.getMediumVideo() != null) {
            medium.getMediumVideo().setLength(lDuration);
          }

          entityTransaction.begin();
          entityManager.merge(medium);
          entityManager.persist(medium);
          entityTransaction.commit();
          entityManager.refresh(medium);

        } catch (IOException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
      }
    }
    return Response.ok().build();
  }

  private Response downloadFile(java.nio.file.Path filePath, HttpHeaders headers, String asFile) {
    Response response = null;

    // Retrieve the file
    File file = filePath.toFile();

    if (file.exists()) {
      String fileExtension = file.getName().substring(file.getName().lastIndexOf('.') + 1);
      String fileType = "video";
      if (fileExtension.equalsIgnoreCase("png") || fileExtension.equalsIgnoreCase("jpg")) {
        fileType = "image";
      }
      if (fileExtension.equalsIgnoreCase("mp3")) {
        fileType = "audio";
      }

      ResponseBuilder builder = Response.ok();
      if (asFile != null) {
        builder.header("Content-Disposition", "attachment; filename=\"" + asFile + "\"");
      }

      builder.header("Accept-Ranges", "bytes");
      if (headers.getRequestHeaders().containsKey("Range")) {
        String rangeHeader = headers.getHeaderString("Range");
        String[] acceptRanges = rangeHeader.split("=");
        if (acceptRanges.length < 2) {
          return Response.status(Status.BAD_REQUEST).build();
        }
      }

      int from, to = 0;
      if (!headers.getRequestHeaders().containsKey("Range")) {
        builder.header("Content-Length", file.length());
        builder.header("Content-Type", fileType + "/" + fileExtension);
        from = 0;
        to = (int) file.length();
      }
      else {
        String rangeHeader = headers.getHeaderString("Range");
        builder.status(Status.PARTIAL_CONTENT);
        builder.header("Content-Type", fileType + "/" + fileExtension);

        String[] acceptRanges = rangeHeader.split("=");
        String[] bounds = acceptRanges[1].split("-");
        from = Integer.parseInt(bounds[0]);
        to = (int) file.length() - 1;
        if (bounds.length > 1) {
          to = Integer.parseInt(bounds[1]);
        }
        from = Math.max(0, from);
        from = (int) Math.min(file.length(), from);
        to = Math.max(0, to);
        to = (int) Math.min(file.length(), to);
        to = Math.max(from + 1, to);
        builder.header("Content-Length", to - from + 1);
        builder.header("Content-Range", "bytes " + from + "-" + to + "/" + file.length());

//				builder.header("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
//				builder.header("Pragma", "no-cache"); // HTTP 1.0
//				builder.header("Expires", "0"); // Proxies
//				builder.header("Content-Disposition", "attachment; filename=" + file.getName());
      }

      RangedStreamingOutput stream = new RangedStreamingOutput(from, to, file);
      builder.entity(stream);
      response = builder.build();
    }
    else {
      response = Response.status(404).entity("INTERNAL ERROR::FILE NOT FOUND").type("text/plain").build();
    }

    return response;
  }

  //TODO: In future versions another mechanism should be found for that, so the storages are the only components knowing the paths to the files
  public static String mediumFileStatus(int id, String type) {
    File mediumDir = java.nio.file.Path.of(TIMAATApp.timaatProps.getProp(PropertyConstants.STORAGE_LOCATION))
                                       .resolve("medium").resolve(type).resolve(String.valueOf(id)).toFile();
    String fileStatus = "noFile";
    if (!mediumDir.exists()) {
      return fileStatus;
    }

    String fileExtension = ""; // TODO determine correct file type
    if (type.equalsIgnoreCase("audio")) {
      fileExtension = "mp3";
    }
    if (type.equalsIgnoreCase("image")) {
      fileExtension = "png";
    }
    if (type.equalsIgnoreCase("video")) {
      fileExtension = "mp4";
    }

    switch (type) {
      case "audio": // currently mp3 files will only be uploaded but not converted
        if (new File(mediumDir + "/" + id + "-" + type + "." + fileExtension).exists()) {
          fileStatus = "ready";
        }
        break;
      case "image":
        if (new File(mediumDir + "/" + id + "-" + type + "-original." + fileExtension).exists() || new File(
                mediumDir + "-" + type + "-original." + "jpg").exists()) {
          fileStatus = "uploaded";
        }
        if (new File(mediumDir + "/" + id + "-" + type + "-thumb." + fileExtension).exists()) {
          fileStatus = "thumbCreated";
        }
        if (new File(mediumDir + "/" + id + "-" + type + "-scaled." + fileExtension).exists()) {
          fileStatus = "ready";
        }
        break;
      case "video":
        if (new File(mediumDir + "/" + id + "-" + type + "-original." + fileExtension).exists()) {
          fileStatus = "waiting";
        }
        if (new File(mediumDir + "/" + id + "-" + type + "." + fileExtension).exists()) {
          fileStatus = "ready";
        }
        if (new File(mediumDir + "/" + id + "-transcoding.pid").exists()) {
          fileStatus = "transcoding";
        }
        break;
    }

    // TODO implement status "unavailable"

    return fileStatus;
  }

  private AudioInformation getAudioFileInfo(java.nio.file.Path mediumFilePath) throws IOException {
    Runtime r = Runtime.getRuntime();
    Process p;     // Process tracks one external native process
    BufferedReader is;  // reader for output of process
    String line;

    String[] commandLine = {TIMAATApp.timaatProps.getProp(
            PropertyConstants.FFMPEG_LOCATION) + "ffprobe" + TIMAATApp.systemExt, "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=r_frame_rate,codec_name", "-show_entries", "format=duration", "-sexagesimal", "-of", "json", mediumFilePath.toString()};
    AudioInformation audioInfo = new AudioInformation(0, "");

    try {
      p = r.exec(commandLine);
      is = new BufferedReader(new InputStreamReader(p.getInputStream()));

      try {
        p.waitFor();  // wait for process to complete
      } catch (InterruptedException e) {
        System.err.println(e);  // "can't happen"
      }

      String jsonString = "";
      while ((line = is.readLine()) != null) {
        if (line != null) {
          jsonString += line;
        }
      }

      JSONObject json = new JSONObject(jsonString);
      String sDuration = json.getJSONObject("format").getString("duration");
      String[] hms = sDuration.split(":");
      double dTotalSecs = Integer.parseInt(hms[0]) * 3600 + Integer.parseInt(hms[1]) * 60 + Double.parseDouble(hms[2]);
      long lDuration = (long) (dTotalSecs * 1000);
      audioInfo.setDuration(lDuration);
      if (json.getJSONArray("streams").length() > 0) {
        audioInfo.setCodec(json.getJSONArray("streams").getJSONObject(0).getString("codec_name"));
      }
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    return audioInfo;
  }

  private ImageInformation getImageFileInfo(java.nio.file.Path imagePath, ImageFileType imageFileType) throws IOException {
    String suffix = imageFileType.getFileSuffix();
    ImageInformation imageInfo = new ImageInformation(0, 0, "0", "");
    Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix);
    while (iter.hasNext()) {
      ImageReader reader = iter.next();
      try {
        ImageInputStream stream = new FileImageInputStream(imagePath.toFile());
        reader.setInput(stream);
        int width = reader.getWidth(reader.getMinIndex());
        int height = reader.getHeight(reader.getMinIndex());
        // IIOMetadata metadata = reader.getImageMetadata(0);
        // if (metadata.isStandardMetadataFormatSupported()) { // true for all bundled formats
        // IIOMetadataNode bitDepth = (IIOMetadataNode) metadata.getAsTree("jakarta_imageio_1.0")
        // 	.getElementsByTagName('Data').item(0)
        // 	.get_elements_by_tag_name('BitsPerSample').item(0)
        // 	.getAttribute('value');
        imageInfo.setWidth(width);
        imageInfo.setHeight(height);
        stream.flush();
        stream.close();
        // imageInfo.setBitDepth(bitDepth);
        return imageInfo;
      } catch (IOException e) {
        System.out.println("Error reading: " + imagePath + e);
      } finally {
        reader.dispose();
      }
    }
    throw new IOException("Not a known image file: " + imagePath);
    // Runtime r = Runtime.getRuntime();
    // Process p;     // Process tracks one external native process
    // BufferedReader is;  // reader for output of process
    // String line;

    // String[] commandLine = { TIMAATApp.timaatProps.getProp(PropertyConstants.FFMPEG_LOCATION)+"ffprobe"+TIMAATApp.systemExt,
    // "-v", "error", "-select_streams", "v:0",
    // "-show_entries", "stream=width,height,r_frame_rate,codec_name",
    // "-show_entries", "format=duration",
    // "-of", "json", filename };
    // ImageInformation imageInfo = new ImageInformation("0", "0", "0", "");

    // try {
    // 	p = r.exec(commandLine);
    // 	is = new BufferedReader(new InputStreamReader(p.getInputStream()));

    // 	try {
    // 		p.waitFor();  // wait for process to complete
    // 	} catch (InterruptedException e) {
    // 		System.err.println(e);  // "can't happen"
    // 	}

    // 	String jsonString = "";
    // 	while ((line = is.readLine()) != null) if ( line != null ) jsonString += line;

    // 	JSONObject json = new JSONObject(jsonString);
    // 	imageInfo.setWidth(json.getJSONArray("streams").getJSONObject(0).getString("width"));
    // 	imageInfo.setHeight(json.getJSONArray("streams").getJSONObject(0).getString("height"));
    // 	imageInfo.setbitDepth(json.getJSONArray("streams").getJSONObject(0).getString("bitDepth"));
    // 	imageInfo.setFileExtension(json.getJSONArray("streams").getJSONObject(0).getString("fileExtension"));


    // } catch (IOException e1) {
    // 	// TODO Auto-generated catch block
    // 	e1.printStackTrace();
    // }

    // return imageInfo;
  }

  private VideoInformation getVideoFileInfo(java.nio.file.Path mediumFilePath) throws IOException {
    Runtime r = Runtime.getRuntime();
    Process p;     // Process tracks one external native process
    BufferedReader is;  // reader for output of process
    String line;

    String[] commandLine = {TIMAATApp.timaatProps.getProp(
            PropertyConstants.FFMPEG_LOCATION) + "ffprobe" + TIMAATApp.systemExt, "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=width,height,r_frame_rate,codec_name", "-show_entries", "format=duration", "-sexagesimal", "-of", "json", mediumFilePath.toString()};
    VideoInformation videoInfo = new VideoInformation(0, 0, 25, 0, "");

    try {
      p = r.exec(commandLine);
      is = new BufferedReader(new InputStreamReader(p.getInputStream()));

      try {
        p.waitFor();  // wait for process to complete
      } catch (InterruptedException e) {
        System.err.println(e);  // "can't happen"
      }

      String jsonString = "";
      while ((line = is.readLine()) != null) {
        if (line != null) {
          jsonString += line;
        }
      }

      JSONObject json = new JSONObject(jsonString);
      int framerate = 30; // TODO
      videoInfo.setWidth(json.getJSONArray("streams").getJSONObject(0).getInt("width"));
      videoInfo.setHeight(json.getJSONArray("streams").getJSONObject(0).getInt("height"));
      videoInfo.setFramerate(framerate);
      String sDuration = json.getJSONObject("format").getString("duration");
      String[] hms = sDuration.split(":");
      double dTotalSecs = Integer.parseInt(hms[0]) * 3600 + Integer.parseInt(hms[1]) * 60 + Double.parseDouble(hms[2]);
      long lDuration = (long) (dTotalSecs * 1000);
      videoInfo.setDuration(lDuration);
      videoInfo.setCodec(json.getJSONArray("streams").getJSONObject(0).getString("codec_name"));
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    return videoInfo;
  }

  private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) throws IOException {
    BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics2D = resizedImage.createGraphics();
    graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
    graphics2D.dispose();
    return resizedImage;
  }

  private java.nio.file.Path createVideoThumbnail(int mediumId, int frameTimeStampMs) throws IOException, VideoEngineException {
    Optional<java.nio.file.Path> mediumVideoFilePathOptional = videoFileStorage.getPathToOriginalFile(mediumId);

    float frameTimeStampInSeconds = frameTimeStampMs / 1000f;

    if (mediumVideoFilePathOptional.isPresent()) {
      java.nio.file.Path mediumVideoFilePath = mediumVideoFilePathOptional.get();
      try (TemporaryFile temporaryFile = temporaryFileStorage.createTemporaryFile()) {
        ffmpegVideoEngine.extractFrameOfVideo(mediumVideoFilePath, temporaryFile.getTemporaryFilePath(),
                frameTimeStampInSeconds);
        mediumVideoStorage.updateThumbnailPositionMs(mediumId, frameTimeStampMs);
        return videoFileStorage.persistThumbnailFile(temporaryFile.getTemporaryFilePath(), mediumId);
      }
    }

    throw new FileNotFoundException();
  }

  public static String issueFileToken(int mediumID) {
    return issueFileToken(mediumID, null);
  }

  public static String issueFileToken(int mediumID, ContainerRequestContext crc) {
    // System.out.println("issueFileToken for id "+ mediumID);
    Key key = TIMAATKeyGenerator.generateKey();

    JwtBuilder jwtBuilder = Jwts.builder().claim("file", mediumID)
                                .setIssuer(TIMAATApp.timaatProps.getProp(PropertyConstants.SERVER_NAME));
    if (crc != null) {
      jwtBuilder.setSubject(crc.getProperty("TIMAAT.userName").toString())
                .claim("id", Integer.parseInt(crc.getProperty("TIMAAT.userID").toString()));
    }
    String token = jwtBuilder.setIssuedAt(new Date())
                             .setExpiration(EndpointAuthentication.toDate(LocalDateTime.now().plusHours(8L)))
                             .signWith(key, SignatureAlgorithm.HS512).compact();
    return token;
  }

  public static int validateFileToken(String token) throws Exception {
    // Check if the token was issued by the server and if it's not expired
    // Throw an Exception if the token is invalid

    SecretKey key = TIMAATKeyGenerator.generateKey();
    int mediumID = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload()
                       .get("file", Integer.class);

    return mediumID;
  }

  public static <T> List<T> castList(Class<? extends T> clazz, Collection<?> c) {
    List<T> r = new ArrayList<T>(c.size());
    for (Object o : c) {
      r.add(clazz.cast(o));
    }
    return r;
  }

  /**
   * Returns all transcriptions that belong to the given medium. An empty collection is returned
   * when the medium has no transcriptions; if the medium itself does not exist the response is
   * {@code 404 Not Found}.
   *
   * @param mediumId identifies the {@link Medium} whose transcriptions should be returned
   * @return {@code 200 OK} with the transcription DTOs, or {@code 404 Not Found} when the
   * medium does not exist
   */
  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/transcriptions")
  public Response getMediumTranscriptions(@PathParam("id") int mediumId) {
    try {
      Collection<de.bitgilde.TIMAAT.model.FIPOP.Transcription> transcriptions = transcriptionService.getTranscriptionsForMedium(
              mediumId);
      List<TranscriptionDto> dtos = transcriptions.stream().map(TranscriptionDto::new).collect(Collectors.toList());
      return Response.ok(dtos).build();
    } catch (MediumNotFoundException e) {
      return Response.status(Status.NOT_FOUND).entity("{\"reason\":\"" + e.getMessage() + "\"}").build();
    }
  }

  /**
   * Returns the single transcription identified by {@code transcriptionId}, scoped to the given
   * medium so a transcription belonging to a different medium is reported as not found rather
   * than returned out of context.
   *
   * @param mediumId        identifies the {@link Medium} the transcription is expected to belong to
   * @param transcriptionId identifies the transcription to load
   * @return {@code 200 OK} with the transcription DTO, or {@code 404 Not Found} when the
   * transcription does not exist or belongs to a different medium
   */
  @GET
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/transcriptions/{transcriptionId}")
  public Response getMediumTranscription(@PathParam("id") int mediumId, @PathParam("transcriptionId") int transcriptionId) {
    try {
      de.bitgilde.TIMAAT.model.FIPOP.Transcription transcription = transcriptionService.getTranscription(mediumId,
              transcriptionId);
      return Response.ok(new TranscriptionDto(transcription)).build();
    } catch (TranscriptionNotFoundException e) {
      return Response.status(Status.NOT_FOUND).entity("{\"reason\":\"" + e.getMessage() + "\"}").build();
    }
  }

  /**
   * Creates a new transcription for the given medium using the engine and model identified in
   * the payload. Both {@code engineIdentifier} and {@code modelIdentifier} are required; a
   * request that omits either field, or that names an engine/model pair the speech-to-text
   * service does not provide, is rejected with {@code 400 Bad Request}. When the speech-to-text
   * feature is disabled for this deployment the request is refused with {@code 403 Forbidden}.
   *
   * @param mediumId identifies the {@link Medium} the transcription should be created for
   * @param request  payload carrying the engine/model identifiers
   * @return {@code 201 Created} with the new transcription DTO; {@code 400 Bad Request} when
   * the payload is missing or invalid; {@code 403 Forbidden} when the feature is disabled;
   * {@code 500 Internal Server Error} when the transcription could not be created
   */
  @POST
  @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/transcriptions")
  public Response createMediumTranscription(@PathParam("id") int mediumId, CreateTranscriptionRequest request) {
    int userId = (int) containerRequestContext.getProperty("TIMAAT.userID");

    if (request == null) {
      return Response.status(Status.BAD_REQUEST).entity("{\"reason\":\"request body is required\"}").build();
    }
    if (isBlank(request.engineIdentifier()) || isBlank(request.modelIdentifier())) {
      return Response.status(Status.BAD_REQUEST)
                     .entity("{\"reason\":\"engineIdentifier and modelIdentifier are required\"}").build();
    }

    GenerateTranscriptionConfiguration configuration = new GenerateTranscriptionConfiguration(mediumId,
            request.engineIdentifier(), request.modelIdentifier());

    try {
      TranscriptionDto created = transcriptionService.createTranscription(configuration, userId);
      return Response.status(Status.CREATED).entity(created).build();
    } catch (TranscriptionFeatureDisabledException e) {
      return Response.status(Status.FORBIDDEN).entity("{\"reason\":\"" + e.getMessage() + "\"}").build();
    } catch (IllegalArgumentException e) {
      return Response.status(Status.BAD_REQUEST).entity("{\"reason\":\"" + e.getMessage() + "\"}").build();
    } catch (TranscriptionServiceException e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity("{\"reason\":\"" + e.getMessage() + "\"}").build();
    }
  }

  /**
   * Deletes the transcription identified by {@code transcriptionId}, scoped to the given medium
   * so that a transcription belonging to a different medium is reported as not found rather than
   * silently removed. The deletion itself is delegated to
   * {@link TranscriptionService#deleteTranscription(int)}, which takes care of reassigning the
   * medium's default transcription and removing the on-disk transcription file.
   *
   * @param mediumId        identifies the {@link Medium} the transcription is expected to belong
   *                        to
   * @param transcriptionId identifies the transcription to remove
   * @return {@code 204 No Content} on success; {@code 404 Not Found} when the transcription does
   * not exist or belongs to a different medium; {@code 500 Internal Server Error} when the
   * deletion failed for any other reason
   */
  @DELETE
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/transcriptions/{transcriptionId}")
  public Response deleteMediumTranscription(@PathParam("id") int mediumId, @PathParam("transcriptionId") int transcriptionId) {
    if (!transcriptionService.existsForMedium(mediumId, transcriptionId)) {
      return Response.status(Status.NOT_FOUND)
                     .entity("{\"reason\":\"Transcription " + transcriptionId + " does not exist for medium " + mediumId + "\"}")
                     .build();
    }

    try {
      transcriptionService.deleteTranscription(transcriptionId);
      return Response.noContent().build();
    } catch (TranscriptionNotFoundException e) {
      return Response.status(Status.NOT_FOUND).entity("{\"reason\":\"" + e.getMessage() + "\"}").build();
    } catch (TranscriptionServiceException e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity("{\"reason\":\"" + e.getMessage() + "\"}").build();
    }
  }

  /**
   * Streams the SRT file backing the transcription identified by {@code transcriptionId} to the
   * client. The transcription is scoped to the given medium so that a transcription belonging to
   * a different medium is reported as not found rather than being delivered out of context. The
   * response is sent as {@code text/plain} with a {@code Content-Disposition: attachment} header
   * so that browsers trigger a download dialog instead of inlining the subtitles.
   *
   * @param mediumId        identifies the {@link Medium} the transcription is expected to belong to
   * @param transcriptionId identifies the transcription whose SRT file should be downloaded
   * @return {@code 200 OK} streaming the SRT file; {@code 404 Not Found} when the transcription
   * does not exist for the medium or its SRT file is missing on disk; {@code 500 Internal Server
   * Error} when the file could not be streamed because of an IO failure
   */
  @GET
  @Produces("text/plain")
  @Secured
  @Path("{id}/transcriptions/{transcriptionId}/file")
  public Response downloadTranscriptionFile(@PathParam("id") int mediumId, @PathParam("transcriptionId") int transcriptionId) {
    Transcription transcription;
    try {
      transcription = transcriptionService.getTranscription(mediumId, transcriptionId);
    } catch (TranscriptionNotFoundException e) {
      return Response.status(Status.NOT_FOUND)
                     .entity("{\"reason\":\"Transcription " + transcriptionId + " does not exist for medium " + mediumId + "\"}")
                     .build();
    }

    Optional<java.nio.file.Path> srtPath = transcriptionFileStorage.getPathToTranscription(transcriptionId);
    if (srtPath.isEmpty()) {
      return Response.status(Status.NOT_FOUND)
                     .entity("{\"reason\":\"Transcription file for " + transcriptionId + " is missing\"}").build();
    }

    java.nio.file.Path file = srtPath.get();
    StreamingOutput stream = output -> {
      try {
        java.nio.file.Files.copy(file, output);
      } catch (IOException e) {
        throw new jakarta.ws.rs.WebApplicationException(e,
                Response.status(Status.INTERNAL_SERVER_ERROR).entity("{\"reason\":\"" + e.getMessage() + "\"}")
                        .build());
      }
    };

    return Response.ok(stream, "text/vtt").header("Content-Disposition",
                           "attachment; filename=\"" + transcription.getName() + "_" + transcription.getCreatedAt()
                                                                                                    .getEpochSecond() + ".srt\"")
                   .build();
  }

  @POST
  @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
  @Secured
  @Path("{id}/transcriptions/default")
  public Response updateDefaultTranscription(@PathParam("id") int mediumId, UpdateMediumDefaultTranscriptionPayload updateMediumDefaultTranscriptionPayload) {
    try {
      mediumStorage.updateDefaultTranscription(mediumId, updateMediumDefaultTranscriptionPayload.getTranscriptionId());
      return Response.noContent().build();
    } catch (Exception e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity("{\"reason\":\"" + e.getMessage() + "\"}").build();
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }


}
