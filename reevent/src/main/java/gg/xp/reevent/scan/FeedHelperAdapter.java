package gg.xp.reevent.scan;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.TypedEventHandler;

import java.lang.annotation.Annotation;

public interface FeedHelperAdapter<X extends Annotation, Y extends Event, Z> {

	Class<Y> eventType();

	TypedEventHandler<Y> makeHandler(FeedHandlerChildInfo<X, Z> info);

}
