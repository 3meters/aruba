package com.aircandi.location;

/**
 * This Interface definition allows you to create OS version-specific  
 * implementations that offer the full Strict Mode functionality
 * available in each platform release. 
 */
public interface IStrictMode {
  /**
   * Enable {@link StrictMode} using whichever platform-specific flags you wish.
   */
   public void enableStrictMode();
}
