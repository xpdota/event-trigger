package gg.xp.xivsupport.events.fflogs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.reevent.events.BaseEvent;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FflogsMasterDataEvent extends BaseEvent {

	@Serial
	private static final long serialVersionUID = 6406548116923334551L;
	private final List<Actor> actors;

	public record Actor(
			long gameID,
			long id,
			String name,
			@Nullable Long petOwner,
			String type,
			String subType
	) {
	}

	public FflogsMasterDataEvent(@JsonProperty("actors") List<Actor> actors) {
		this.actors = new ArrayList<>(actors);
	}

	public List<Actor> getActors() {
		return Collections.unmodifiableList(actors);
	}
}
