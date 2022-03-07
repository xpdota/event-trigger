package gg.xp.xivsupport.sys;

import gg.xp.reevent.context.SubState;
import gg.xp.reevent.scan.ScanMe;

@ScanMe
public class PrimaryLogSource implements SubState {

	private KnownLogSource logSource = KnownLogSource.UNKNOWN;

	public PrimaryLogSource() {}


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
