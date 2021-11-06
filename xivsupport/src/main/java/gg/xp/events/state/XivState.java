package gg.xp.events.state;

import gg.xp.events.XivEntity;
import gg.xp.events.XivZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XivState {

	private static final Logger log = LoggerFactory.getLogger(XivState.class);

	private XivZone zone;
	private XivEntity player;

	public XivEntity getPlayer() {
		return player;
	}

	public void setPlayer(XivEntity player) {
		log.info("Player changed to {}", player);
		this.player = player;
	}

	public XivZone getZone() {
		return zone;
	}

	public void setZone(XivZone zone) {
		log.info("Zone changed to {}", zone);
		this.zone = zone;
	}
}
