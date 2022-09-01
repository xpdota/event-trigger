package gg.xp.xivsupport.events.actlines.events;

/**
 * Interface used to provide a simple text descriptor of something in a table.
 */
public interface HasPrimaryValue {
	/**
	 * @return A text description of this object. Should be user-friendly and not over-the-top technical (fine-grained
	 * details belong in other fields, or in the {@link #toString()} method).
	 */
	String getPrimaryValue();
}
