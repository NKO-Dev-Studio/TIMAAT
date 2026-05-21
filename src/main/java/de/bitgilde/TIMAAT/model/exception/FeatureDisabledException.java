package de.bitgilde.TIMAAT.model.exception;

/**
 * This {@link RuntimeException} will be thrown when a feature is triggered which is currently
 * disabled.
 *
 * @author Nico Kotlenga (nico@nko-dev.studio) 
 * @since 21.05.26
 */
public class FeatureDisabledException extends RuntimeException {
  private final String featureName;

  public FeatureDisabledException(String featureName) {
    super("Feature " + featureName + " is not available");
    this.featureName = featureName;
  }

  public String getFeatureName() {
    return featureName;
  }
}
