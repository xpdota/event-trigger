package gg.xp.xivdata.data;

public enum JobSelectionState {
	NOT_SELECTED(false),
	SELECTED(true),
	SELECTED_FROM_PARENT(true);

	private final boolean countsAsEnabled;

	JobSelectionState(boolean countsAsEnabled) {
		this.countsAsEnabled = countsAsEnabled;
	}

	public boolean countsAsEnabled() {
		return countsAsEnabled;
	}
}
