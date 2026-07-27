package de.bitgilde.TIMAAT.storage.api;

/**
 * An {@link ReducedEntity} represents an entity by only containing the ID and a description string.
 *
 * @param <ID> type of the represented entity
 * @author Nico Kotlenga (nico@nko-dev.studio) 
 * @since 01.07.26
 */
public record ReducedEntity<ID>(ID id, String description) {
}
