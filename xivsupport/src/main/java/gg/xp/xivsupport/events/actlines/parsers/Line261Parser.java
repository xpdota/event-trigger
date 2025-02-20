package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unused")
public class Line261Parser extends AbstractACTLineParser<Line261Parser.Fields> {

	private static final Logger log = LoggerFactory.getLogger(Line261Parser.class);
	private final XivState state;

	public Line261Parser(PicoContainer container, XivState state) {
		super(container, 261, Line261Parser.Fields.class);
		this.state = state;
	}

	enum Fields {
		updateType, entityId
	}

	enum PosKeys {
		PosX, PosY, PosZ, Heading
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		String updateType = fields.getString(Fields.updateType);
		switch (updateType) {
			case "Remove" -> {
				long entityId = fields.getHex(Fields.entityId);
				state.removeSpecificCombatant(entityId);
			}
			case "Add", "Change" -> {
				// TODO: non-combatants with type==7 do not have 03-lines at all. Thus, we should try to use 261-lines
				// to fill this data.
				Map<PosKeys, Double> pos = new EnumMap<>(PosKeys.class);
				XivCombatant existing = fields.getEntity(Fields.entityId);
				Position existingPos = existing.getPos();
				List<String> raw = fields.getRawLineSplit();
				// Start at first data field, end before hash
				List<String> kvFields = raw.subList(4, raw.size() - 1);
				int type = 0;
				for (int i = 0; i + 1 < kvFields.size(); i += 2) {
					String key = kvFields.get(i);
					String valueRaw = kvFields.get(i + 1);
					// TEMP WORKAROUND - https://github.com/OverlayPlugin/OverlayPlugin/issues/221
					valueRaw = valueRaw.replaceAll(",", ".");
					switch (key) {
						case "PosX" -> pos.put(PosKeys.PosX, Double.parseDouble(valueRaw));
						case "PosY" -> pos.put(PosKeys.PosY, Double.parseDouble(valueRaw));
						case "PosZ" -> pos.put(PosKeys.PosZ, Double.parseDouble(valueRaw));
						case "Heading" -> pos.put(PosKeys.Heading, Double.parseDouble(valueRaw));
						case "Type" -> type = Integer.parseInt(valueRaw);
						case "Radius" -> state.provideCombatantRadius(existing, Float.parseFloat(valueRaw));
						// TODO: this might also be readable from ActorControlExtraEvent category 0x3F
						case "WeaponId" -> state.provideWeaponId(existing, Short.parseShort(valueRaw));
					}
				}
				if (pos.isEmpty()) {
					break;
				}
				// Workaround for type-7 (non-combatants) not appearing in ACT 03-lines, thus no raw data existing
				if (type == 7) {
					state.provideTypeOverride(existing, 7);
				}
				if (existingPos == null) {
					if (pos.size() < 4) {
						if (pos.containsKey(PosKeys.PosX) && pos.containsKey(PosKeys.PosY)) {
							// There is arguably an issue with the OP logic where the logic to write x+y+z+heading if any
							// of them changes does not work on an 'add' line, and the 'add' line will not write 'default'
							// values. So if there is a Z or heading of 0 (very likely), those will not be written.
							// To work around this, we just assume those are 0.
							state.provideCombatantPos(existing, new Position(pos.get(PosKeys.PosX), pos.get(PosKeys.PosY), pos.getOrDefault(PosKeys.PosZ, 0.0), pos.getOrDefault(PosKeys.Heading, 0.0)), true);
						}
						else {
							log.trace("Incomplete position info for 0x{}", Long.toString(existing.getId(), 16));
							return null;
						}
					}
					else {
						state.provideCombatantPos(existing, new Position(pos.get(PosKeys.PosX), pos.get(PosKeys.PosY), pos.get(PosKeys.PosZ), pos.get(PosKeys.Heading)), true);
					}
				}
				else {
					state.provideCombatantPos(existing, new Position(
							pos.getOrDefault(PosKeys.PosX, existingPos.x()),
							pos.getOrDefault(PosKeys.PosY, existingPos.y()),
							pos.getOrDefault(PosKeys.PosZ, existingPos.z()),
							pos.getOrDefault(PosKeys.Heading, existingPos.heading())
					), true);
				}
			}
		}
		return null;
	}

}
