package gg.xp.xivsupport.events.actlines.events;

public interface HasTargetIndex {

	long getTargetIndex();

	long getNumberOfTargets();

	default boolean isLastTarget() {
		return getTargetIndex() >= getNumberOfTargets() - 1;
	}
}
