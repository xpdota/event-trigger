package gg.xp.xivsupport.gui.map.omen;

public enum OmenEventType {
	PRE_CAST,
	CAST_FINISHED,
	INSTANT;

	public boolean isInProgress() {
		return this == PRE_CAST;
	}
}
