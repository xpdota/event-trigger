package gg.xp.xivsupport.gui.groovy;

/**
 * Helper to allow scripts to request that they be startup scripts
 */
public interface ScriptSettingsControl {
	/**
	 * To be called from within the script. Causes the "Allow this script to run on startup?" message to appear.
	 */
	@SuppressWarnings("unused")
	void requestRunOnStartup();

	/**
	 * No-op version of this. Used for testing.
	 */
	@SuppressWarnings("Convert2Lambda") // not intended to be a functional interface
	ScriptSettingsControl noop = new ScriptSettingsControl() {
		@Override
		public void requestRunOnStartup() {

		}
	};
}
