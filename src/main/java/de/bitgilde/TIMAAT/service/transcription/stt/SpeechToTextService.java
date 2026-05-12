package de.bitgilde.TIMAAT.service.transcription.stt;

import de.bitgilde.TIMAAT.model.FIPOP.Transcription;
import studio.nkodev.stt.client.SpeechToTextServiceClient;

/**
 * This service is using the {@link studio.nkodev.stt.client.SpeechToTextServiceClient} to trigger and monitor
 * speech to text tasks
 *
 * @author Nico Kotlenga (nico@nko-dev.studio) 
 * @since 12.05.26
 */
public class SpeechToTextService {

  private final SpeechToTextServiceClient speechToTextServiceClient;

  public SpeechToTextService(SpeechToTextServiceClient speechToTextServiceClient) {
    this.speechToTextServiceClient = speechToTextServiceClient;
  }

  public void startSpeechToTextTask(Transcription transcription){

  }

  public void resumeSpeechToTextTask(Transcription transcription){

  }


}
