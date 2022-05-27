package gg.xp.reevent.events;

public interface TypedEventHandler<X extends Event> extends EventHandler<X>{
	Class<X> getType();
}
