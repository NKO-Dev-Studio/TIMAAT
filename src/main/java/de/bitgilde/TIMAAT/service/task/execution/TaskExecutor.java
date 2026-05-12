package de.bitgilde.TIMAAT.service.task.execution;

import de.bitgilde.TIMAAT.service.task.api.Task;
import de.bitgilde.TIMAAT.service.task.exception.TaskExecutionException;

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
 * Execution unit of a {@link Task}
 *
 * @author Nico Kotlenga
 * @since 20.07.25
 */
public abstract class TaskExecutor<T extends Task> {

    final T task;

    public TaskExecutor(T task) {
        this.task = task;
    }

    public abstract void execute() throws TaskExecutionException;
}
