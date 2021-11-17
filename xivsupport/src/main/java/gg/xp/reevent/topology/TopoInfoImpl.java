package gg.xp.reevent.topology;

import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopoInfoImpl implements TopologyInfo {

	private static final Logger log = LoggerFactory.getLogger(TopoInfoImpl.class);
	private final PersistenceProvider persistence;

	public TopoInfoImpl(PersistenceProvider persistence) {
		this.persistence = persistence;
	}

	@Override
	public boolean isEnabled(BaseToggleableTopo key) {
		String fullKey = key.getFullPropKey();
		boolean out = persistence.get(fullKey, boolean.class, true);
		log.trace("Topo Persistence Read: Key: {}, Value: {}", fullKey, out);
		return out;
	}

	@Override
	public void setEnabled(BaseToggleableTopo key, boolean enabled) {
		String fullKey = key.getFullPropKey();
		log.trace("Topo Persistence Write: Key: {}, Value: {}", fullKey, enabled);
		persistence.save(fullKey, enabled);
	}
}
