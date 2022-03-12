package gg.xp.xivsupport.sys;

import gg.xp.reevent.context.SubState;

public class PrimaryLogSource implements SubState {

	private KnownLogSource logSource = KnownLogSource.UNKNOWN;

	public void setLogSource(KnownLogSource logSource) {
		this.logSource = logSource;
	}

	public KnownLogSource getLogSource() {
		return logSource;
	}

	public boolean isActImport() {
		return logSource == KnownLogSource.ACT_LOG_FILE;
	}

	public boolean isSnapshotSupported() {
		return logSource.isSnapshotSupported();
	}
}
