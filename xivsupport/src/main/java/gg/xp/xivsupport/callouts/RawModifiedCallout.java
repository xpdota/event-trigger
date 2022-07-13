package gg.xp.xivsupport.callouts;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.speech.HasCalloutTrackingKey;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.Serial;
import java.util.Collections;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;

@SystemEvent
public class RawModifiedCallout<X> extends BaseEvent implements HasCalloutTrackingKey, HasPrimaryValue {
	private static final Logger log = LoggerFactory.getLogger(RawModifiedCallout.class);
	@Serial
	private static final long serialVersionUID = 5660021283907309369L;
	private final String description;
	private final String tts;
	private final String text;
	private final @Nullable X event;
	private final Map<String, Object> arguments;
	private final Function<? super X, ? extends @Nullable Component> guiProvider;
	private final Predicate<RawModifiedCallout<X>> expiry;
	private @Nullable HasCalloutTrackingKey replaces;
	private final @Nullable Color colorOverride;
	private final CalloutTrackingKey key = new CalloutTrackingKey();
	private static final int maxErrors = 10;
	private int errorCount;

	public RawModifiedCallout(String description, String tts, String text, @Nullable X event, Map<String, Object> arguments, Function<? super X, ? extends @Nullable Component> guiProvider, Predicate<RawModifiedCallout<X>> expiry, @Nullable Color colorOverride) {
		this.description = description;
		this.tts = tts;
		this.text = text;
		this.event = event;
		this.arguments = arguments;
		this.guiProvider = guiProvider;
		this.expiry = expiry;
		this.colorOverride = colorOverride;
	}

	public String getTts() {
		return tts;
	}

	public String getText() {
		return text;
	}

	public @Nullable X getEvent() {
		return event;
	}

	public Map<String, Object> getArguments() {
		return Collections.unmodifiableMap(arguments);
	}

	public Function<? super X, ? extends Component> getGuiProvider() {
		return guiProvider;
	}

	public BooleanSupplier getExpiry() {
		return () -> expiry.test(this);
	}

	public @Nullable HasCalloutTrackingKey getReplaces() {
		return replaces;
	}

	public @Nullable Color getColorOverride() {
		return colorOverride;
	}

	public void setReplaces(@Nullable HasCalloutTrackingKey replaces) {
		if (replaces == null) {
			this.replaces = null;
		}
		else {
			this.replaces = replaces;
		}
	}

	public boolean shouldLogError() {
		errorCount++;
		if (errorCount < maxErrors) {
			return true;
		}
		else if (errorCount == maxErrors) {
			log.error("Hit the maximum number of logged errors for ModifiableCallout '{}', silencing future errors", description);
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public CalloutTrackingKey trackingKey() {
		return key;
	}

	@Override
	public String getPrimaryValue() {
		return tts;
	}
}
