package gg.xp.events.state;

import gg.xp.events.models.XivEntity;
import gg.xp.events.models.XivPlayerCharacter;
import gg.xp.events.models.XivZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class XivState {

	private static final Logger log = LoggerFactory.getLogger(XivState.class);

	private XivZone zone;
	private XivEntity player;
	private List<XivPlayerCharacter> partyList = Collections.emptyList();

	// Note: can be null until we've seen a 02-line
	public XivEntity getPlayer() {
		return player;
	}

	public void setPlayer(XivEntity player) {
		log.info("Player changed to {}", player);
		this.player = player;
		sortParty();
	}

	// Note: can be null until we've seen a 01-line
	public XivZone getZone() {
		return zone;
	}

	public void setZone(XivZone zone) {
		log.info("Zone changed to {}", zone);
		this.zone = zone;
	}

	public void setPartyList(List<XivPlayerCharacter> partyList) {
		this.partyList = new ArrayList<>(partyList);
		sortParty();
		log.info("Party list changed to {}", this.partyList.stream().map(XivEntity::getName).collect(Collectors.joining(", ")));
	}

	public List<XivPlayerCharacter> getPartyList() {
		return new ArrayList<>(partyList);
	}

	// TODO: concurrency issues?
	private void sortParty() {
		partyList.sort(Comparator.comparing(p -> {
			if (player != null && player.getId() == p.getId()) {
				// Always sort main player first
				return -1;
			}
			else {
				// Uhhh...... TODO actual party sorting....probably requires job data first
				return (int) p.getId() % 256;
			}
		}));
	}
}
