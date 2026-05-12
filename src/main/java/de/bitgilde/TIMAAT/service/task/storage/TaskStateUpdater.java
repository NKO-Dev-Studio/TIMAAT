package de.bitgilde.TIMAAT.service.task.storage;

import de.bitgilde.TIMAAT.service.task.api.Task;
import de.bitgilde.TIMAAT.service.task.api.TaskState;

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
 * Component offers methods to update the {@link TaskState} of a {@link Task}
 *
 * @author Nico Kotlenga
 * @since 20.07.25
 */
public interface TaskStateUpdater {

    /**
     * Updates the {@link TaskState} of a {@link Task} to the specified value
     *
     * @param task      which state will be updated
     * @param taskState to which the task will be set to
     */
    void updateTaskState(Task task, TaskState taskState);
}
