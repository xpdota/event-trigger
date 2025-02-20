package gg.xp.xivsupport.timelines.icon;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jetbrains.annotations.Nullable;

import java.net.URL;

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "type")
@JsonSubTypes({
		// TODO: why is this necessary when the interface is sealed?
		@JsonSubTypes.Type(value = UrlTimelineIcon.class, name = "url"),
		@JsonSubTypes.Type(value = ActionTimelineIcon.class, name = "action"),
		@JsonSubTypes.Type(value = StatusTimelineIcon.class, name = "status"),
		@JsonSubTypes.Type(value = IconIdTimelineIcon.class, name = "iconId"),
})
//@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "type")
public sealed interface TimelineIcon permits UrlTimelineIcon, ActionTimelineIcon, StatusTimelineIcon, IconIdTimelineIcon {
	@JsonIgnore
	@Nullable URL getIconUrl();
}
