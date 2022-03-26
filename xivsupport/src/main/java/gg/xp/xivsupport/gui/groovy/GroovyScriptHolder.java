package gg.xp.xivsupport.gui.groovy;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import groovy.lang.GroovyShell;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
		getterVisibility = JsonAutoDetect.Visibility.NONE,
		setterVisibility = JsonAutoDetect.Visibility.NONE,
		fieldVisibility = JsonAutoDetect.Visibility.NONE,
		isGetterVisibility = JsonAutoDetect.Visibility.NONE,
		creatorVisibility = JsonAutoDetect.Visibility.NONE
)
public class GroovyScriptHolder {
	private String scriptName;
	private String scriptContent;
	private boolean strict;
	private boolean dirty;
	@Nullable
	private File file;
	private GroovyScriptResult lastResult;

	private GroovyManager manager;
	private GroovyShell shell;

	public GroovyScriptResult run() {
		try {
			if (shell == null) {
				shell = getMgr().makeShell();
			}
			Object result = shell.parse(getScriptContent()).run();
			return lastResult = GroovyScriptResult.success(result);
		}
		catch (Throwable t) {
			return lastResult = GroovyScriptResult.failure(t);
		}
	}

	public void save() {
		if (dirty) {
			getMgr().saveScript(this);
		}
		dirty = false;
	}

	private GroovyManager getMgr() {
		if (getManager() == null) {
			throw new IllegalStateException("Neither Shell nor GroovyManager set");
		}
		return getManager();
	}

	public boolean isSaveable() {
		return getFile() != null;
	}

	@JsonProperty("scriptName")
	public String getScriptName() {
		return scriptName;
	}

	@JsonProperty("scriptName")
	public void setScriptName(String scriptName) {
		this.scriptName = scriptName;
	}

	public String getScriptContent() {
		return scriptContent;
	}

	public void setScriptContent(String scriptContent) {
		if (this.scriptContent != null) {
			// Don't dirty on initial set
			dirty = true;
		}
		this.scriptContent = scriptContent;
	}

	@JsonProperty("strict")
	public boolean isStrict() {
		return strict;
	}

	@JsonProperty("strict")
	public void setStrict(boolean strict) {
		this.strict = strict;
	}

	public @Nullable File getFile() {
		return file;
	}

	public void setFile(@NotNull File file) {
		//noinspection ConstantConditions
		if (file == null) {
			throw new RuntimeException("New file cannot be null");
		}
		if (this.file != null) {
			try {
				Files.move(this.file.toPath(), file.toPath());
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		this.file = file;
	}

	public GroovyScriptResult getLastResult() {
		return lastResult;
	}

	public GroovyManager getManager() {
		return manager;
	}

	void setManager(GroovyManager manager) {
		this.manager = manager;
	}
}
