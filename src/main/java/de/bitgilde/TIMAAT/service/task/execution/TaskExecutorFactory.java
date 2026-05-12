package de.bitgilde.TIMAAT.service.task.execution;

import de.bitgilde.TIMAAT.processing.audio.FfmpegAudioEngine;
import de.bitgilde.TIMAAT.storage.entity.AudioAnalysisResultStorage;
import de.bitgilde.TIMAAT.storage.file.AudioContainingMediumFileStorage;
import de.bitgilde.TIMAAT.storage.file.AudioFileStorage;
import de.bitgilde.TIMAAT.storage.file.TemporaryFileStorage;
import de.bitgilde.TIMAAT.storage.file.VideoFileStorage;
import de.bitgilde.TIMAAT.service.task.api.MediumAudioAnalysisTask;
import de.bitgilde.TIMAAT.service.task.api.MediumAudioAnalysisTask.SupportedMediumType;
import de.bitgilde.TIMAAT.service.task.api.Task;
import de.bitgilde.TIMAAT.service.task.api.TaskType;
import jakarta.inject.Inject;

import java.util.Map;

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
 * This factory is responsible to create {@link TaskExecutor} for the passed {@link Task}
 *
 * @author Nico Kotlenga
 * @since 20.07.25
 */
public class TaskExecutorFactory {

    private final TemporaryFileStorage temporaryFileStorage;
    private final FfmpegAudioEngine audioEngine;
    private final AudioAnalysisResultStorage audioAnalysisResultStorage;
    private final Map<SupportedMediumType, AudioContainingMediumFileStorage> audioContainingMediumFileStorageBySupportedMediumType;

    @Inject
    public TaskExecutorFactory(TemporaryFileStorage temporaryFileStorage, VideoFileStorage videoFileStorage, AudioFileStorage audioFileStorage, FfmpegAudioEngine audioEngine, AudioAnalysisResultStorage audioAnalysisResultStorage) {
        this.temporaryFileStorage = temporaryFileStorage;
        this.audioEngine = audioEngine;
        this.audioAnalysisResultStorage = audioAnalysisResultStorage;
        this.audioContainingMediumFileStorageBySupportedMediumType = Map.of(SupportedMediumType.AUDIO, audioFileStorage, SupportedMediumType.VIDEO, videoFileStorage);
    }

    @SuppressWarnings("unchecked")
    public <T extends Task> TaskExecutor<T> createTaskExecutor(T task) {
        if (TaskType.MEDIUM_AUDIO_ANALYSIS.equals(task.getTaskType())) {
            return (TaskExecutor<T>) new MediumAudioAnalysisTaskExecutor(temporaryFileStorage, (MediumAudioAnalysisTask) task,
                    audioContainingMediumFileStorageBySupportedMediumType, audioEngine, audioAnalysisResultStorage);
        }
        return null;
    }
}
