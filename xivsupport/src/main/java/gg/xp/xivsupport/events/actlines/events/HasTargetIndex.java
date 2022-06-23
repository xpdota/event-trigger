package gg.xp.xivsupport.events.actlines.events;

public interface HasTargetIndex {

	long getTargetIndex();

	long getNumberOfTargets();

	 default boolean isFirstTarget() {
		 return getTargetIndex() == 0;
	 }

	default boolean isLastTarget() {
		return getTargetIndex() >= getNumberOfTargets() - 1;
	}
}
