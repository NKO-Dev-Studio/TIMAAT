package de.bitgilde.TIMAAT.service.task.api;

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
 * {@link Task} responsible to execute the audio analysis for {@link de.bitgilde.TIMAAT.model.FIPOP.Medium}s of type video
 *
 * @author Nico Kotlenga
 * @since 20.07.25
 */
public class MediumAudioAnalysisTask extends Task {

    private final int mediumId;
    private final SupportedMediumType mediumType;

    public MediumAudioAnalysisTask(int mediumId, SupportedMediumType mediumType) {
        super(TaskType.MEDIUM_AUDIO_ANALYSIS);
        this.mediumId = mediumId;
        this.mediumType = mediumType;
    }

    public int getMediumId() {
        return mediumId;
    }

    public SupportedMediumType getMediumType() {
        return mediumType;
    }

    public enum SupportedMediumType {
        AUDIO,
        VIDEO;
    }
}
