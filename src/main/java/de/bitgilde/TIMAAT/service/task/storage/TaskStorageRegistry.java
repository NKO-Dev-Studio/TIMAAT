package de.bitgilde.TIMAAT.service.task.storage;

import de.bitgilde.TIMAAT.service.task.api.Task;
import de.bitgilde.TIMAAT.service.task.api.TaskType;
import de.bitgilde.TIMAAT.service.task.exception.TaskStorageException;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.IterableProvider;

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Registry resolving the {@link TaskStorage} owning a specific {@link TaskType}.
 *
 * @author Nico Kotlenga
 * @since 16.05.26
 */
public class TaskStorageRegistry {

    private final Map<TaskType, TaskStorage> taskStoragesByTaskType;

    @Inject
    public TaskStorageRegistry(IterableProvider<TaskStorage> taskStorages) {
        this.taskStoragesByTaskType = new EnumMap<>(TaskType.class);
        for (TaskStorage taskStorage : taskStorages) {
            TaskStorage previousTaskStorage = taskStoragesByTaskType.put(taskStorage.getSupportedTaskType(), taskStorage);
            if (previousTaskStorage != null) {
                throw new IllegalStateException("Multiple task storages registered for task type " + taskStorage.getSupportedTaskType());
            }
        }
    }

    public TaskStorage getTaskStorage(TaskType taskType) {
        TaskStorage taskStorage = taskStoragesByTaskType.get(taskType);
        if (taskStorage == null) {
            throw new IllegalArgumentException("No task storage registered for task type " + taskType);
        }

        return taskStorage;
    }

    public Stream<? extends Task> getUnfinishedTasks() throws TaskStorageException {
        Stream<? extends Task> unfinishedTasks = Stream.empty();
        for (TaskStorage taskStorage : taskStoragesByTaskType.values()) {
            unfinishedTasks = Stream.concat(unfinishedTasks, taskStorage.getUnfinishedTasks());
        }

        return unfinishedTasks;
    }
}
