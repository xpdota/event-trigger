package gg.xp.xivsupport.gui.imprt;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import gg.xp.reevent.events.Event;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface ImportSpec<X extends Event> {

	String typeLabel();

	String extendedLabel();

	List<X> readEvents();

	void launch(List<X> events);
}
