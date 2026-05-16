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

import de.bitgilde.TIMAAT.service.task.execution.TaskExecutorService;

/**
 * This enum describes the different {@link TaskType}s which can be executed by the {@link TaskExecutorService}
 *
 * @author Nico Kotlenga
 * @since 18.07.25
 */
public enum TaskType {
    MEDIUM_AUDIO_ANALYSIS,
    TRANSCRIPTION_MEDIUM_PREPARATION;
}
