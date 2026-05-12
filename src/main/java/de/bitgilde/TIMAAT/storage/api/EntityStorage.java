package de.bitgilde.TIMAAT.storage.api;

import de.bitgilde.TIMAAT.model.FIPOP.UserAccount;

import java.util.stream.Stream;

/**
 * Interface describing the functionalities an entity storage will provide
 *
 * @author Nico Kotlenga
 * @since 10.01.26
 */
public interface EntityStorage<ENTITY_TYPE, FILTER_TYPE, SORTING_FIELD_TYPE extends SortingField> {

  /**
   * Loads the entries as stream matching the specified criteria.
   * @param filter which the entities have to match
   * @param pagingParameter used to limit the size of the resulting stream
   * @param sortingParameter used to determine the order of entries inside the stream
   * @param userAccount used to filter entries by authorization
   * @return the resulting {@link Stream}
   */
  Stream<ENTITY_TYPE> getEntriesAsStream(FILTER_TYPE filter, PagingParameter pagingParameter, SortingParameter<SORTING_FIELD_TYPE> sortingParameter, UserAccount userAccount);

  /**
   * Loads the count of entities matching the specified filters which are accessible for the specified
   * @param filter which the items will match
   * @return the number of matching entries
   */
  Long getNumberOfMatchingEntriesRespectingAuthorization(FILTER_TYPE filter, UserAccount userAccount);

  /**
   * @return the number of total entries inside the storage
   */
  Long getNumberOfTotalEntries();

  /**
   * @param userAccount which will be respected by the query
   * @return the number of entries to which the passed user has access to
   */
  Long getNumberOfTotalEntriesRespectingAuthorization(UserAccount userAccount);
}
