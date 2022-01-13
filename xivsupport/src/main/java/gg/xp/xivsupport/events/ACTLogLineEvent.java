package gg.xp.xivsupport.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.xivsupport.persistence.Compressible;

import java.io.Serial;
import java.time.ZonedDateTime;

@SystemEvent
public class ACTLogLineEvent extends BaseEvent implements Compressible {

	@Serial
	private static final long serialVersionUID = -5255204546093791693L;
	private final String logLine;
	private final String[] rawFields;
	private final ZonedDateTime timestamp;

	public ACTLogLineEvent(String logLine) {
		this.logLine = logLine;
		rawFields = logLine.split("\\|");
		this.timestamp = ZonedDateTime.parse(rawFields[1]);
		setHappenedAt(timestamp.toInstant());
	}

	public String getLogLine() {
		return logLine;
	}

	public String[] getRawFields() {
		return rawFields;
	}

	public ZonedDateTime getTimestamp() {
		return timestamp;
	}

	public String getEmulatedActLogLine() {

		int msgNum = Integer.parseInt(rawFields[0]);
		// https://github.com/quisquous/cactbot/blob/129e5040447a14bfcdb00fd4c71dfe489fe0225e/resources/netlog_defs.ts
		String messageName = switch (msgNum) {
			case 0 -> "ChatLog";
			case 1 -> "Territory";
			case 2 -> "ChangePrimaryPlayer";
			case 3 -> "AddCombatant";
			case 4 -> "RemoveCombatant";
			case 11 -> "PartyList";
			case 12 -> "PlayerStats";
			case 20 -> "StartsCasting";
			case 21 -> "ActionEffect";
			case 22 -> "AOEActionEffect";
			case 23 -> "CancelAction";
			case 24 -> "DoTHoT";
			case 25 -> "Death";
			case 26 -> "StatusAdd";
			case 27 -> "TargetIcon";
			case 28 -> "WaymarkMarker";
			case 29 -> "SignMarker";
			case 30 -> "StatusRemove";
			case 31 -> "Gauge";
			case 32 -> "World";
			case 33 -> "Director";
			case 34 -> "NameToggle";
			case 35 -> "Tether";
			case 36 -> "LimitBreak";
			case 37 -> "EffectResult";
			case 38 -> "StatusList";
			case 39 -> "UpdateHp";
			case 40 -> "ChangeMap";
			case 41 -> "SystemLogMessage";
			case 249 -> "ParserInfo";
			case 250 -> "ProcessInfo";
			case 251 -> "Debug";
			case 252 -> "PacketDump";
			case 253 -> "Version";
			case 254 -> "Error";
			default -> "";
		};
		String typeString = String.format("%s %02X", messageName, msgNum);
		StringBuilder lineBuilder = new StringBuilder();
		// Just faking the timestamp for now
		lineBuilder.append("[12:34:56.789] ").append(typeString);
		// ACT log lines do not have the checksum at the end
		for (int i = 2; i < rawFields.length - 1; i++) {
			String rawField = rawFields[i];
			lineBuilder.append(":").append(rawField);
		}
		return lineBuilder.toString();
	}

	@Override
	public void compress() {
		for (int i = 0; i < rawFields.length; i++) {
			if (rawFields[i].length() <= 16) {
				rawFields[i] = rawFields[i].intern();
			}
		}
	}

	@Override
	public void decompress() {
		// Nothing to do
	}
}
