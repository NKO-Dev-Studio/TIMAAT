package de.bitgilde.TIMAAT.service.task;

import de.bitgilde.TIMAAT.service.task.api.MediumAudioAnalysisTask;
import de.bitgilde.TIMAAT.service.task.api.MediumAudioAnalysisTask.SupportedMediumType;
import de.bitgilde.TIMAAT.service.task.api.Task;
import de.bitgilde.TIMAAT.service.task.api.TranscriptionMediumPreparationTask;
import de.bitgilde.TIMAAT.service.task.exception.TaskServiceException;
import de.bitgilde.TIMAAT.service.task.exception.TaskStorageException;
import de.bitgilde.TIMAAT.service.task.execution.TaskExecutorService;
import de.bitgilde.TIMAAT.service.task.storage.TaskStorage;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;

import java.util.logging.Level;
import java.util.logging.Logger;
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
 * Service allows the triggering of tasks.
 * This class is orchestrating all task related components to persist and execute tasks
 * <p>
 * During the initialization of the {@link TaskService} a check for unfinished tasks will take place. This is necessary
 * to resume not finished tasks after service restart.
 *
 * @author Nico Kotlenga
 * @since 20.07.25
 */
public class TaskService {
    private static final Logger logger = Logger.getLogger(TaskService.class.getName());

    private final TaskStorage taskStorage;
    private final TaskExecutorService taskExecutorService;

    @Inject
    public TaskService(TaskStorage taskStorage, TaskExecutorService taskExecutorService) throws TaskStorageException {
        this.taskStorage = taskStorage;
        this.taskExecutorService = taskExecutorService;

        executeUnfinishedTasks();
    }

    /**
     * Triggers the execution of a {@link MediumAudioAnalysisTask} for the specified {@link de.bitgilde.TIMAAT.model.FIPOP.Medium}
     *
     * @param mediumId for which the tasks will be executed
     */
    public void executeMediumAudioAnalysisTask(int mediumId, SupportedMediumType supportedMediumType) throws TaskServiceException {
        logger.log(Level.FINE, "Trigger execution of medium audio analysis task for medium having id {0}", mediumId);

        try {
            MediumAudioAnalysisTask mediumAudioAnalysisTask = new MediumAudioAnalysisTask(mediumId, supportedMediumType);
            taskStorage.persistTask(mediumAudioAnalysisTask);
            taskExecutorService.executeTask(mediumAudioAnalysisTask);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while trigger the execution of a medium audio analysis task", e);
            throw new TaskServiceException("Error during trigger the execution of a medium audio analysis task", e);
        }
    }

    /**
     * Triggers the execution of a {@link TranscriptionMediumPreparationTask} for the specified
     * {@link de.bitgilde.TIMAAT.model.FIPOP.Medium}.
     *
     * @param mediumId for which the tasks will be executed
     */
    public void executeTranscriptionMediumPreparationTask(int mediumId, SupportedMediumType supportedMediumType) throws TaskServiceException {
        logger.log(Level.FINE, "Trigger execution of transcription medium preparation task for medium having id {0}", mediumId);

        try {
            TranscriptionMediumPreparationTask transcriptionMediumPreparationTask = new TranscriptionMediumPreparationTask(mediumId, supportedMediumType);
            taskStorage.persistTask(transcriptionMediumPreparationTask);
            taskExecutorService.executeTask(transcriptionMediumPreparationTask);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while trigger the execution of a transcription medium preparation task", e);
            throw new TaskServiceException("Error during trigger the execution of a transcription medium preparation task", e);
        }
    }

    private void executeUnfinishedTasks() throws TaskStorageException {
        logger.log(Level.INFO, "Trigger execution of unfinished tasks");
        try (Stream<? extends Task> unfinishedTasks = taskStorage.getUnfinishedTasks()) {
            unfinishedTasks.forEach(taskExecutorService::executeTask);
        }
    }

    @PreDestroy
    public void shutdown() {
        taskExecutorService.shutdown();
    }
}
