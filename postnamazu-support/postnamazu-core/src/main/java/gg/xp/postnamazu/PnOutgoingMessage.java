package gg.xp.postnamazu;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.events.triggers.marks.AutoMarkLanguage;
import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.models.XivEntity;

import java.io.Serial;
import java.util.Map;

public class PnOutgoingMessage extends BaseEvent implements HasPrimaryValue {

	@Serial
	private static final long serialVersionUID = 5779519876994337089L;
	private final String command;
	private final PnQueueType queueType;
	private final Object payload;

	@Deprecated
	public PnOutgoingMessage(String command, Object payload) {
		this.command = command;
		this.payload = payload;
		if (command.equals("command") && payload instanceof String cmdStr && cmdStr.startsWith("/mk")) {
			this.queueType = PnQueueType.MARK;
		}
		else {
			this.queueType = switch (command) {
				case "command" -> PnQueueType.COMMAND;
				case "mark" -> PnQueueType.MARK;
				default -> PnQueueType.NONE;
			};
		}
	}

	public PnOutgoingMessage(String command, PnQueueType queueType, Object payload) {
		this.command = command;
		this.queueType = queueType;
		this.payload = payload;
	}

	public String getCommand() {
		return command;
	}

	public Object getPayload() {
		return payload;
	}

	public PnQueueType getQueueType() {
		return queueType;
	}

	@Override
	public String getPrimaryValue() {
		return String.format("%s: %s", command, payload);
	}

	public static PnOutgoingMessage command(String command) {
		return new PnOutgoingMessage("command", PnQueueType.COMMAND, command);
	}

	public static PnOutgoingMessage markerCommand(String command) {
		return new PnOutgoingMessage("command", PnQueueType.MARK, command);
	}

	public static PnOutgoingMessage mark(XivEntity entity, MarkerSign marker) {
		return mark(entity.getId(), marker);
	}

	public static PnOutgoingMessage mark(long actorId, MarkerSign marker) {
		return new PnOutgoingMessage("mark", PnQueueType.MARK, Map.of(
				// Yes, this is E00_0000 rather than E000_0000
				// I don't know why, the resulting packet still shows E000_0000
				"ActorID", actorId,
				"MarkType", marker.getCommand(AutoMarkLanguage.JP)
		));
	}

}
