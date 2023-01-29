package gg.xp.telestosupport.doodle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.util.Objects;

public class DynamicTextDoodleSpec extends DoodleSpec implements DynamicDoodle {
	@Serial
	private static final long serialVersionUID = 3263754140212173050L;
	@JsonProperty("position")
	public final DoodleLocation position;
	@JsonProperty("size")
	public final int size;
	@JsonIgnore
	public final String textScript;
	@JsonProperty("text")
	public transient String text;
	@JsonIgnore
	private transient DynamicValueProcessor processor;

	public DynamicTextDoodleSpec(DoodleLocation position, int size, String textScript) {
		this.position = position;
		this.size = size;
		this.textScript = textScript;
	}



	@JsonProperty("text")
	public String getText() {
		return text;
	}

	@Override
	public String type() {
		return "text";
	}

	@Override
	@JsonIgnore
	public void setProcessor(DynamicValueProcessor processor) {
		this.processor = processor;
	}

	@Override
	@JsonIgnore
	public boolean reprocess() {
		String newText = processor.process(textScript, String.class);
		boolean dirty = !Objects.equals(newText, text);
		text = newText;
		return dirty;
	}
}
