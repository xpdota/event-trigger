package gg.xp.telestosupport.subscriber;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

import java.io.Serial;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class TelestoSubscriptionMessage extends BaseEvent implements HasPrimaryValue {

	@Serial
	private static final long serialVersionUID = -4087263606032064311L;
	private final int version;
	private final int id;
	private final String notificationId;
	private final String notificationType;
	private final Map<String, Object> payload;

	@JsonCreator
	public TelestoSubscriptionMessage(
			@JsonProperty("version") int version,
			@JsonProperty("id") int id,
			@JsonProperty("notificationid") String notificationId,
			@JsonProperty("notificationtype") String notificationType,
			@JsonProperty("payload") Map<String, Object> payload
	) {
		this.version = version;
		this.id = id;
		this.notificationId = notificationId;
		this.notificationType = notificationType.intern();
		this.payload = payload.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey().intern(), Map.Entry::getValue));
	}

	public int getVersion() {
		return version;
	}

	public int getId() {
		return id;
	}

	public String getNotificationId() {
		return notificationId;
	}

	public String getNotificationType() {
		return notificationType;
	}


	public Map<String, Object> getPayload() {
		return Collections.unmodifiableMap(payload);
	}

	@Override
	public boolean shouldSave() {
		return true;
	}

	@Override
	public String getPrimaryValue() {
		return payload.toString();
	}

	@Override
	public String toString() {
		return "TelestoSubscriptionMessage{" +
		       "version=" + version +
		       ", id=" + id +
		       ", notificationId='" + notificationId + '\'' +
		       ", notificationType='" + notificationType + '\'' +
		       ", payload=" + payload +
		       '}';
	}
}
