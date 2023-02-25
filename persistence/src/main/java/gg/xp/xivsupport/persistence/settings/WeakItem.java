package gg.xp.xivsupport.persistence.settings;

/**
 * Interface representing an item where underlying operations take place on a weakly-referenced object,
 * where the outer object becomes effectively worthless once the outer object is GC'd.
 */
public interface WeakItem {
	/**
	 * @return true if and only if the item is gone, and thus this is no longer needed.
	 */
	boolean isGone();
}
