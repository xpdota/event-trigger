package gg.xp.events.state;

import gg.xp.events.models.XivEntity;
import gg.xp.events.models.XivZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XivState {

	private static final Logger log = LoggerFactory.getLogger(XivState.class);

	private XivZone zone;
	private XivEntity player;

	// Note: can be null until we've seen a 02-line
	public XivEntity getPlayer() {
		return player;
	}

	public void setPlayer(XivEntity player) {
		log.info("Player changed to {}", player);
		this.player = player;
	}

	// Note: can be null until we've seen a 01-line
	public XivZone getZone() {
		return zone;
	}

	public void setZone(XivZone zone) {
		log.info("Zone changed to {}", zone);
		this.zone = zone;
	}
}
