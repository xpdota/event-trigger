package gg.xp.events.actlines.parsers;

import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.models.XivAbility;
import gg.xp.events.models.XivCombatant;
import gg.xp.events.models.XivEntity;
import gg.xp.events.models.XivStatusEffect;
import gg.xp.events.state.RawXivCombatantInfo;
import gg.xp.events.state.XivState;

import java.util.EnumMap;
import java.util.Map;

public class FieldMapper<K extends Enum<K>> {

	private final Map<K, String> raw;
	private final EventContext<Event> context;

	public FieldMapper(Map<K, String> raw, EventContext<Event> context) {
		this.raw = new EnumMap<>(raw);
		this.context = context;
	}

	public String getString(K key) {
		return raw.get(key);
	}

	public long getLong(K key) {
		return Long.parseLong(raw.get(key), 10);
	}

	public long getHex(K key) {
		return Long.parseLong(raw.get(key), 16);
	}

	public double getDouble(K key) {
		return Double.parseDouble(raw.get(key));
	}

	public XivAbility getAbility(K idKey, K nameKey) {
		long id = getHex(idKey);
		String name = getString(nameKey);
		return new XivAbility(id, name);
	}

	public XivStatusEffect getStatus(K idKey, K nameKey) {
		long id = getHex(idKey);
		String name = getString(nameKey);
		return new XivStatusEffect(id, name);
	}
	public XivCombatant getEntity(K idKey, K nameKey) {
		long id = getHex(idKey);
		String name = getString(nameKey);
		XivState xivState = context.getStateInfo().get(XivState.class);
		XivCombatant xivCombatant = xivState.getCombatants().get(id);
		if (xivCombatant != null) {
			return xivCombatant;
		}
		else {
			// TODO: is this right?
			return new XivCombatant(id, name, false, false);
		}
	}

}
