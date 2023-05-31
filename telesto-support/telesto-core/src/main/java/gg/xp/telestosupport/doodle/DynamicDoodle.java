package gg.xp.telestosupport.doodle;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface DynamicDoodle {

	@JsonIgnore
	void setProcessor(DynamicValueProcessor processor);

	/**
	 * Reprocess this dynamic doodle
	 *
	 * @return true if the doodle has changed as a result of the call and must be re-sent to the server
	 */
	@JsonIgnore
	boolean reprocess();

	@JsonIgnore
	boolean isExpired();

	@JsonIgnore
	String getName();
}
