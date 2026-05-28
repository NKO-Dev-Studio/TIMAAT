package de.bitgilde.TIMAAT.service.task.storage;

import de.bitgilde.TIMAAT.service.task.api.TaskType;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.IterableProvider;

import java.util.EnumMap;
import java.util.Map;

/**
 * Registry resolving the {@link TaskStateUpdater} owning a specific {@link TaskType}.
 *
 * @author Nico Kotlenga
 * @since 16.05.26
 */
public class TaskStateUpdaterRegistry {

    private final Map<TaskType, TaskStateUpdater> taskStateUpdatersByTaskType;

    @Inject
    public TaskStateUpdaterRegistry(IterableProvider<TaskStateUpdater> taskStateUpdaters) {
        this.taskStateUpdatersByTaskType = new EnumMap<>(TaskType.class);
        for (TaskStateUpdater taskStateUpdater : taskStateUpdaters) {
            TaskStateUpdater previousTaskStateUpdater = taskStateUpdatersByTaskType.put(taskStateUpdater.getSupportedTaskType(), taskStateUpdater);
            if (previousTaskStateUpdater != null) {
                throw new IllegalStateException("Multiple task state updaters registered for task type " + taskStateUpdater.getSupportedTaskType());
            }
        }
    }

    public TaskStateUpdater getTaskStateUpdater(TaskType taskType) {
        TaskStateUpdater taskStateUpdater = taskStateUpdatersByTaskType.get(taskType);
        if (taskStateUpdater == null) {
            throw new IllegalArgumentException("No task state updater registered for task type " + taskType);
        }

        return taskStateUpdater;
    }
}
