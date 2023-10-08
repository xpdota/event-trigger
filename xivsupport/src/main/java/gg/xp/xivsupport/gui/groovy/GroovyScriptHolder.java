package gg.xp.xivsupport.gui.groovy;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.xivsupport.groovy.GroovyManager;
import gg.xp.xivsupport.groovy.GroovyScriptManager;
import gg.xp.xivsupport.groovy.GroovyScriptResult;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

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
	private boolean startup;
	private boolean dirty;
	@Nullable
	private File file;
	private GroovyScriptResult lastResult;

	private GroovyScriptManager scriptMgr;
	private GroovyManager groovyMgr;
	private GroovyShell shell;

	public GroovyScriptResult run() {
		return run(ScriptSettingsControl.noop);
	}

	public GroovyScriptResult run(ScriptSettingsControl ssc) {
		DisplayControl dc = new DisplayControl();
		try {
			if (shell == null) {
				shell = getGroovyMgr().makeShell();
			}
			Object result;
			try (AutoCloseable ignored = getGroovyMgr().getSandbox().enter()) {
				Script parsed = shell.parse(getScriptContent());
				Binding binding = parsed.getBinding();
				binding.setVariable("displayControl", dc);
				if (ssc != null) {
					binding.setVariable("scriptSettings", ssc);
				}
				result = parsed.run();
			}
			return lastResult = GroovyScriptResult.success(dc, result);
		}
		catch (Throwable t) {
			return lastResult = GroovyScriptResult.failure(dc, t);
		}
	}

	public void save() {
		if (dirty) {
			getScriptMgr().saveScript(this);
		}
		dirty = false;
	}

	public GroovyScriptHolder copyAs(String newName, File newFile) {
		GroovyScriptHolder copy = new GroovyScriptHolder();
		copy.scriptName = newName;
		copy.scriptContent = scriptContent;
		copy.file = newFile;
		copy.strict = strict;
		copy.dirty = true;
		copy.scriptMgr = scriptMgr;
		copy.groovyMgr = groovyMgr;
		return copy;
	}

	// TODO: this is janky - really should separate out metadata
	public void reverseCopy(GroovyScriptHolder other) {
		scriptName = other.scriptName;
		scriptContent = other.scriptContent;
		file = other.file;
		strict = other.strict;
		dirty = other.dirty;
	}

	private GroovyScriptManager getScriptMgr() {
		if (scriptMgr == null) {
			throw new IllegalStateException("scriptMgr has not been set yet");
		}
		return scriptMgr;
	}

	private GroovyManager getGroovyMgr() {
		if (groovyMgr == null) {
			throw new IllegalStateException("groovyMgr has not been set yet");
		}
		return groovyMgr;
	}


	public boolean isSaveable() {
		// Can't factor "dirty" into here because the toolbar doesn't auto update. might look into that.
		return getFile() != null;
	}

	public boolean isDeletable() {
		return getFile() != null;
	}

	@JsonProperty("scriptName")
	public String getScriptName() {
		return scriptName;
	}

	@JsonProperty("scriptName")
	public void setScriptName(String scriptName) {
		this.scriptName = scriptName;
		dirty = true;
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
		dirty = true;
	}

	@JsonProperty("startup")
	public boolean isStartup() {
		return startup;
	}

	@JsonProperty("startup")
	public void setStartup(boolean startup) {
		this.startup = startup;
		dirty = true;
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

	public boolean isMgrSet() {
		return scriptMgr != null && groovyMgr != null;
	}

	public void setScriptMgr(GroovyScriptManager scriptMgr) {
		this.scriptMgr = scriptMgr;
		this.groovyMgr = scriptMgr.getGroovyManager();
	}
}
