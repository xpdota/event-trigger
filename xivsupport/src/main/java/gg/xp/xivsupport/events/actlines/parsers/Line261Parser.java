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
			case "Add", "Change" -> {
				Map<PosKeys, Double> pos = new EnumMap<>(PosKeys.class);
				XivCombatant existing = fields.getEntity(Fields.entityId);
				Position existingPos = existing.getPos();
				List<String> raw = fields.getRawLineSplit();
				// Start at first data field, end before hash
				List<String> kvFields = raw.subList(4, raw.size() - 1);
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
						case "Radius" -> state.provideCombatantRadius(existing, Float.parseFloat(valueRaw));
					}
				}
				if (pos.isEmpty()) {
					break;
				}
				if (existingPos == null) {
					if (pos.size() < 4) {
						log.info("Incomplete position info for 0x{}", Long.toString(existing.getId(), 16));
						return null;
					}
					else {
						state.provideCombatantPos(existing, new Position(pos.get(PosKeys.PosX), pos.get(PosKeys.PosY), pos.get(PosKeys.PosZ), pos.get(PosKeys.Heading)));
					}
				}
				else {
					state.provideCombatantPos(existing, new Position(
							pos.getOrDefault(PosKeys.PosX, existingPos.x()),
							pos.getOrDefault(PosKeys.PosY, existingPos.y()),
							pos.getOrDefault(PosKeys.PosZ, existingPos.z()),
							pos.getOrDefault(PosKeys.Heading, existingPos.heading())
					));
				}
			}
		}
		return null;
	}

}
