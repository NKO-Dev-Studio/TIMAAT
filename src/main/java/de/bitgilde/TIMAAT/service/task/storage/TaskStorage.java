package de.bitgilde.TIMAAT.service.task.storage;

import de.bitgilde.TIMAAT.service.task.api.Task;
import de.bitgilde.TIMAAT.service.task.exception.TaskStorageException;

import java.util.stream.Stream;

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
 * Interface describing the functionalities of a storage holding {@link Task} information
 *
 * @author Nico Kotlenga
 * @since 18.07.25
 */
public interface TaskStorage extends TaskStateUpdater {

    /**
     * @return a {@link Stream} of unfinished {@link Task}s.
     * This information can be interesting on service restart to figure out if all tasks have been finished
     * since last execution.
     */
    Stream<? extends Task> getUnfinishedTasks() throws TaskStorageException;

    /**
     * Persists the {@link Task}
     *
     * @param task
     */
    void persistTask(Task task) throws TaskStorageException;
}
