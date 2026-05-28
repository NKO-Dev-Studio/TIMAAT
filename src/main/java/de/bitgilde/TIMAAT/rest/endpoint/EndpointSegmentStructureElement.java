package de.bitgilde.TIMAAT.rest.endpoint;

import de.bitgilde.TIMAAT.model.DataTableInfo;
import de.bitgilde.TIMAAT.model.FIPOP.MediumAnalysisList;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount;
import de.bitgilde.TIMAAT.rest.Secured;
import de.bitgilde.TIMAAT.rest.filter.AuthenticationFilter;
import de.bitgilde.TIMAAT.rest.model.analysissegment.SegmentStructureElementListingQueryParameter;
import de.bitgilde.TIMAAT.rest.model.segment.SegmentStructureElement;
import de.bitgilde.TIMAAT.storage.entity.segment.SegmentStructureElementsStorage;
import de.bitgilde.TIMAAT.storage.entity.segment.api.SegmentStructureElementType;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import org.jvnet.hk2.annotations.Service;

import java.util.List;
import java.util.stream.Collectors;

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
 * Endpoint class to access information of analysis segments
 *
 * @author Nico Kotlenga
 * @since 31.12.25
 */
@Service
@Path("/segment-structure-elements")
public class EndpointSegmentStructureElement {
  @Inject
  SegmentStructureElementsStorage segmentStructureElementsStorage;
  @Context
  ContainerRequestContext containerRequestContext;

  @GET
  @Produces
  @Path("list")
  @Secured
  public DataTableInfo<SegmentStructureElement> listSegmentStructureElements(@BeanParam SegmentStructureElementListingQueryParameter queryParameter) {
    int draw = queryParameter.getDraw().orElse(0);

    UserAccount userAccount = (UserAccount) containerRequestContext.getProperty(
            AuthenticationFilter.USER_ACCOUNT_PROPERTY_NAME);
    List<SegmentStructureElement> matchingEntries = segmentStructureElementsStorage.getEntriesAsStream(queryParameter,
            queryParameter, queryParameter, userAccount).map(SegmentStructureElement::new).collect(Collectors.toList());
    long countOfMatchingEntries = segmentStructureElementsStorage.getNumberOfMatchingEntriesRespectingAuthorization(queryParameter, userAccount);
    long totalCount = segmentStructureElementsStorage.getNumberOfTotalEntriesRespectingAuthorization(userAccount);
    return new DataTableInfo<>(draw, totalCount, countOfMatchingEntries, matchingEntries);
  }

  @GET
  @Produces
  @Path("{segmentStructureType}/{segmentStructureId}/analysis-list")
  public MediumAnalysisList loadAnalysisListOfSegmentStructureElement(@PathParam("segmentStructureType") SegmentStructureElementType segmentStructureType, @PathParam("segmentStructureId") int segmentStructureId) {
    return segmentStructureElementsStorage.getMediumAnalysisListOfSegmentStructureElement(segmentStructureType,
            segmentStructureId);
  }
}
