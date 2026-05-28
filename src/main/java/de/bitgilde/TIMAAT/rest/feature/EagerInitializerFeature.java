package de.bitgilde.TIMAAT.rest.feature;

import de.bitgilde.TIMAAT.service.task.TaskService;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.hk2.api.ServiceLocator;

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
 * This feature will be used to eager initialize components.
 * This is necessary to start execution of start up relevant business logic (like execution of task not finished on last execution)s.
 *
 * @author Nico Kotlenga
 * @since 27.07.25
 */
@Provider
public class EagerInitializerFeature implements Feature {

    private final ServiceLocator serviceLocator;

    @Inject
    public EagerInitializerFeature(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    @Override
    public boolean configure(FeatureContext context) {
        TaskService taskService = serviceLocator.getService(TaskService.class);
        return true;
    }
}
