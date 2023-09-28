package gg.xp.xivsupport.gui.groovy;

public interface ScriptSettingsControl {
	void requestRunOnStartup();

	public static ScriptSettingsControl noop = new ScriptSettingsControl() {
		@Override
		public void requestRunOnStartup() {

		}
	};
}
