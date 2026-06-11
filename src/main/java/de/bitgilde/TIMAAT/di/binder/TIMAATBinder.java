package de.bitgilde.TIMAAT.di.binder;

import de.bitgilde.TIMAAT.PropertyConstants;
import de.bitgilde.TIMAAT.PropertyManagement;
import de.bitgilde.TIMAAT.TIMAATApp;
import de.bitgilde.TIMAAT.processing.audio.FfmpegAudioEngine;
import de.bitgilde.TIMAAT.processing.video.FfmpegVideoEngine;
import de.bitgilde.TIMAAT.rest.security.authorization.AnalysisListAuthorizationVerifier;
import de.bitgilde.TIMAAT.rest.security.authorization.AnnotationAuthorizationVerifier;
import de.bitgilde.TIMAAT.rest.security.authorization.DbAnnotationAuthorizationVerifier;
import de.bitgilde.TIMAAT.service.task.TaskService;
import de.bitgilde.TIMAAT.service.task.execution.TaskExecutorFactory;
import de.bitgilde.TIMAAT.service.task.execution.TaskExecutorService;
import de.bitgilde.TIMAAT.service.task.storage.DbTaskStorage;
import de.bitgilde.TIMAAT.service.task.storage.TaskStateUpdater;
import de.bitgilde.TIMAAT.service.task.storage.TaskStateUpdaterRegistry;
import de.bitgilde.TIMAAT.service.task.storage.TaskStorage;
import de.bitgilde.TIMAAT.service.task.storage.TaskStorageRegistry;
import de.bitgilde.TIMAAT.service.transcription.TranscriptionService;
import de.bitgilde.TIMAAT.service.transcription.format.vtt.VttParser;
import de.bitgilde.TIMAAT.sse.EntityUpdateEventService;
import de.bitgilde.TIMAAT.storage.entity.AudioAnalysisResultStorage;
import de.bitgilde.TIMAAT.storage.entity.CategorySetStorage;
import de.bitgilde.TIMAAT.storage.entity.CategoryStorage;
import de.bitgilde.TIMAAT.storage.entity.MediumVideoStorage;
import de.bitgilde.TIMAAT.storage.entity.SystemSettingStorage;
import de.bitgilde.TIMAAT.storage.entity.TagStorage;
import de.bitgilde.TIMAAT.storage.entity.actor.ActorStorage;
import de.bitgilde.TIMAAT.storage.entity.analysislist.AnalysisListStorage;
import de.bitgilde.TIMAAT.storage.entity.annotation.AnnotationStorage;
import de.bitgilde.TIMAAT.storage.entity.medium.MediumStorage;
import de.bitgilde.TIMAAT.storage.entity.music.MusicStorage;
import de.bitgilde.TIMAAT.storage.entity.segment.SegmentStructureElementsStorage;
import de.bitgilde.TIMAAT.storage.entity.transcription.TranscriptionStorage;
import de.bitgilde.TIMAAT.storage.file.AnnotationFileStorage;
import de.bitgilde.TIMAAT.storage.file.AudioFileStorage;
import de.bitgilde.TIMAAT.storage.file.ImageFileStorage;
import de.bitgilde.TIMAAT.storage.file.TemporaryFileStorage;
import de.bitgilde.TIMAAT.storage.file.TranscriptionFileStorage;
import de.bitgilde.TIMAAT.storage.file.VideoFileStorage;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.bitgilde.TIMAAT.TIMAATApp.timaatProps;

/**
 * This class is responsible to instantiate the necessary components of TIMAAT and make them available
 * to the DI-system of jersey.
 *
 * @author Nico Kotlenga
 * @since 23.07.25
 */
public class TIMAATBinder extends AbstractBinder {
  private static final Logger logger = Logger.getLogger(TIMAATBinder.class.getName());

  @Override
  protected void configure() {
    try {
      //This is not an elegant way, but the h2 infrastructure startup before application context.
      TIMAATApp.emf = initEntityManager();

      bind(TIMAATApp.emf).to(EntityManagerFactory.class);
      bind(timaatProps).to(PropertyManagement.class);

      bindAsContract(VideoFileStorage.class).in(Singleton.class);
      bindAsContract(ImageFileStorage.class).in(Singleton.class);
      bindAsContract(AudioFileStorage.class).in(Singleton.class);
      bindAsContract(AudioAnalysisResultStorage.class).in(Singleton.class);
      bindAsContract(TranscriptionFileStorage.class).in(Singleton.class);

      bindAsContract(EntityUpdateEventService.class).in(Singleton.class);
      bindAsContract(TemporaryFileStorage.class).in(Singleton.class);
      bindAsContract(FfmpegAudioEngine.class).in(Singleton.class);
      bindAsContract(FfmpegVideoEngine.class).in(Singleton.class);
      bindAsContract(VttParser.class).in(Singleton.class);

      bind(DbTaskStorage.class).to(DbTaskStorage.class).to(TaskStorage.class).to(TaskStateUpdater.class)
                               .in(Singleton.class);
      bindAsContract(TaskStorageRegistry.class).in(Singleton.class);
      bindAsContract(TaskStateUpdaterRegistry.class).in(Singleton.class);
      bindAsContract(TaskExecutorFactory.class).in(Singleton.class);
      bindAsContract(TaskExecutorService.class).in(Singleton.class);
      bindAsContract(TaskService.class).in(Singleton.class);
      bindAsContract(SystemSettingStorage.class).in(Singleton.class);
      bind(TranscriptionStorage.class).to(TranscriptionStorage.class).to(TaskStorage.class).in(Singleton.class);
      bind(TranscriptionService.class).to(TranscriptionService.class).to(TaskStateUpdater.class).in(Singleton.class);

      bindAsContract(MusicStorage.class).in(Singleton.class);
      bindAsContract(AnnotationStorage.class).in(Singleton.class);
      bindAsContract(TagStorage.class).in(Singleton.class);
      bindAsContract(ActorStorage.class).in(Singleton.class);
      bindAsContract(MediumStorage.class).in(Singleton.class);
      bindAsContract(AnalysisListStorage.class).in(Singleton.class);
      bindAsContract(MediumVideoStorage.class).in(Singleton.class);
      bindAsContract(AnnotationFileStorage.class).in(Singleton.class);
      bindAsContract(CategoryStorage.class).in(Singleton.class);
      bindAsContract(CategorySetStorage.class).in(Singleton.class);
      bindAsContract(SegmentStructureElementsStorage.class).in(Singleton.class);

      bind(DbAnnotationAuthorizationVerifier.class).to(AnnotationAuthorizationVerifier.class).in(Singleton.class);
      bindAsContract(AnalysisListAuthorizationVerifier.class).in(Singleton.class);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error during instantiating necessary components", e);
      throw new RuntimeException(e);
    }

  }

  /**
   * Initializes TIMAAT DB persistence layer, database
   *
   * @throws InstantiationException
   */
  private EntityManagerFactory initEntityManager() throws InstantiationException {
    Logger.getGlobal().log(Level.INFO, "[TIMAAT::Persistence Unit Init]");
    HashMap<String, String> dbProps = new HashMap<String, String>();
    dbProps.put("jakarta.persistence.jdbc.url", timaatProps.getProp(PropertyConstants.DATABASE_URL));
    dbProps.put("jakarta.persistence.jdbc.driver", timaatProps.getProp(PropertyConstants.DATABASE_DRIVER));
    dbProps.put("jakarta.persistence.jdbc.user", timaatProps.getProp(PropertyConstants.DATABASE_USER));
    dbProps.put("jakarta.persistence.jdbc.password", timaatProps.getProp(PropertyConstants.DATABASE_PASSWORD));

    try {
      // obtain entity manager factory with provided connection settings
      // emf = Persistence.createEntityManagerFactory("FIPOP-JPA", dbProps);
      EntityManagerFactory emf = Persistence.createEntityManagerFactory("FIPOP-JPA", dbProps);
      // emf = Persistence.createEntityManagerFactory("FIPOP-JPA");
      emf.createEntityManager();
      return emf;
    } catch (Exception e) {
      Logger.getLogger(PropertyManagement.class.getName()).log(Level.SEVERE, "TIMAAT::DB Init Error", e);
      throw new InstantiationException("TIMAAT::DB Error:Could not connect to DB. See server log for details.");
    }
  }
}
