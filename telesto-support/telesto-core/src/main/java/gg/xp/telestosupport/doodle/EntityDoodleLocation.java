package gg.xp.telestosupport.doodle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.xivsupport.models.XivEntity;

import java.io.Serial;

public class EntityDoodleLocation extends DoodleLocation {

	@Serial
	private static final long serialVersionUID = -1865602355862944584L;
	@JsonIgnore
	public final XivEntity entity;

	public EntityDoodleLocation(XivEntity entity) {
		this.entity = entity;
	}

	@JsonProperty("id")
	public String formatId() {
		return Long.toString(entity.getId(), 16).toUpperCase();
	}

	@Override
	public String coordsType() {
		return "entity";
	}
}
