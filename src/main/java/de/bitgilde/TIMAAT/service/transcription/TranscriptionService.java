package de.bitgilde.TIMAAT.service.transcription;

import de.bitgilde.TIMAAT.service.transcription.api.GenerateTranscriptionConfiguration;
import de.bitgilde.TIMAAT.service.transcription.stt.SpeechToTextService;
import de.bitgilde.TIMAAT.storage.api.PagingParameter;
import de.bitgilde.TIMAAT.storage.api.SortingParameter;
import de.bitgilde.TIMAAT.storage.entity.transcription.TranscriptionStorage;
import de.bitgilde.TIMAAT.storage.entity.transcription.api.TranscriptionFilterCriteria;
import de.bitgilde.TIMAAT.storage.entity.transcription.api.TranscriptionState;
import de.bitgilde.TIMAAT.storage.entity.transcription.api.TranscriptionType;
import jakarta.inject.Inject;

import java.util.Set;
import java.util.logging.Logger;

/**
 * Service responsible to manage Transcriptions. This includes the generation, modifying, and deletion.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 12.05.26
 */
public abstract class TranscriptionService {

  private static final Logger logger = Logger.getLogger(TranscriptionService.class.getName());

  private final TranscriptionStorage transcriptionStorage;
  private final SpeechToTextService speechToTextService;

  @Inject
  public TranscriptionService(TranscriptionStorage transcriptionStorage, SpeechToTextService speechToTextService) {
    this.transcriptionStorage = transcriptionStorage;
    this.speechToTextService = speechToTextService;

    resumeMonitoringOfActiveTranscriptions();
  }

  public void generateTranscription(GenerateTranscriptionConfiguration generateTranscriptionConfiguration) {

  }



  private void resumeMonitoringOfActiveTranscriptions() {
    logger.info("Resuming monitoring of uncompleted speech-to-text tasks");
    TranscriptionFilterCriteria transcriptionFilterCriteria = new TranscriptionFilterCriteria.Builder().transcriptionStates(
            Set.of(TranscriptionState.PENDING, TranscriptionState.COMPLETED)).transcriptionTypes(
            Set.of(TranscriptionType.GENERATED)).build();
    transcriptionStorage.getEntriesAsStream(transcriptionFilterCriteria, PagingParameter.NO_PAGING,
            SortingParameter.defaultSortOrder(), null).forEach(transcription -> {
      if (transcription.getTranscriptionTaskId() != null) {

      }
      else {

      }
    });
  }
}
