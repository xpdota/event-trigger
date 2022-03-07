package gg.xp.xivsupport.sys;

public enum KnownLogSource {
	UNKNOWN(false),
	WEBSOCKET_LIVE(false),
	ACT_LOG_FILE(true),
	WEBSOCKET_REPLAY(true),
	FFLOGS(true);

	private final boolean isImport;

	KnownLogSource(boolean isImport) {
		this.isImport = isImport;
	}

	public boolean isImport() {
		return isImport;
	}

	public boolean isSnapshotSupported() {
		return this != FFLOGS;
	}

}
