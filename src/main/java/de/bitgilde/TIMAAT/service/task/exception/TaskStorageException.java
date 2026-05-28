package de.bitgilde.TIMAAT.service.task.exception;

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

import de.bitgilde.TIMAAT.service.task.storage.TaskStorage;

/**
 * This type of exception will be thrown when an error occurred during using the {@link TaskStorage}
 *
 * @author Nico Kotlenga
 * @since 20.07.25
 */
public class TaskStorageException extends Exception {

    public TaskStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
