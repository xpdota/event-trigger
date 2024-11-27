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
	private final @Nullable String sound;
	private final @Nullable X event;
	private final Map<String, Object> arguments;
	private final Function<? super X, ? extends @Nullable Component> guiProvider;
	private Predicate<RawModifiedCallout<X>> expiry;
	private @Nullable HasCalloutTrackingKey replaces;
	private @Nullable Color colorOverride;
	private final ModifiedCalloutHandle handle;
	private final CalloutTrackingKey key = new CalloutTrackingKey();
	private static final int maxErrors = 10;
	private int errorCount;
	private volatile boolean forceExpired;

	public RawModifiedCallout(String description, String tts, String text, @Nullable String sound, @Nullable X event, Map<String, Object> arguments, Function<? super X, ? extends @Nullable Component> guiProvider, Predicate<RawModifiedCallout<X>> expiry, @Nullable Color colorOverride, @Nullable ModifiedCalloutHandle handle) {
		this.description = description;
		this.tts = tts;
		this.text = text;
		this.sound = sound;
		this.event = event;
		this.arguments = arguments;
		this.guiProvider = guiProvider;
		this.expiry = expiry;
		this.colorOverride = colorOverride;
		this.handle = handle;
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
		return () -> expiry.test(this) || this.forceExpired;
	}

	public void addExpiryPredicate(Predicate<RawModifiedCallout<X>> condition) {
		this.expiry = this.expiry.or(condition);
	}

	public void addExpiryCondition(BooleanSupplier newExpiry) {
		this.expiry = this.expiry.or(ignored -> newExpiry.getAsBoolean());
	}

	public @Nullable HasCalloutTrackingKey getReplaces() {
		return replaces;
	}

	public @Nullable Color getColorOverride() {
		return colorOverride;
	}

	public @Nullable String getSound() {
		return sound;
	}

	public @Nullable ModifiedCalloutHandle getHandle() {
		return handle;
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

	public void setColorOverride(Color colorOverride) {
		this.colorOverride = colorOverride;
	}

	public String getDescription() {
		return description;
	}

	public void forceExpire() {
		this.forceExpired = true;
	}

	@Override
	public String toString() {
		return "RawModifiedCallout{" +
		       "description='" + description + '\'' +
		       ", tts='" + tts + '\'' +
		       ", text='" + text + '\'' +
		       ", sound='" + sound + '\'' +
		       ", event=" + event +
		       ", arguments=" + arguments +
		       ", key=" + key +
		       '}';
	}
}
