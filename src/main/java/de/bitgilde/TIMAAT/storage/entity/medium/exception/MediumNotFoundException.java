package de.bitgilde.TIMAAT.storage.entity.medium.exception;

/**
 * Signals that an operation targeted a {@link de.bitgilde.TIMAAT.model.FIPOP.Medium} which does
 * not exist (or no longer exists). Callers in the REST layer should typically map this to
 * {@code 404 Not Found}.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio)
 * @since 2026-05-24
 */
public class MediumNotFoundException extends Exception {

  private static final long serialVersionUID = 1L;

  private final int mediumId;

  public MediumNotFoundException(int mediumId) {
    super("No medium with id " + mediumId + " found");
    this.mediumId = mediumId;
  }

  public int getMediumId() {
    return mediumId;
  }
}
