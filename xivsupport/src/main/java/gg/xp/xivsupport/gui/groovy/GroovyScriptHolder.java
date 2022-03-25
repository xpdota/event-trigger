package gg.xp.xivsupport.gui.groovy;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import groovy.lang.GroovyShell;

import java.util.function.Supplier;

@JsonAutoDetect(
		getterVisibility = JsonAutoDetect.Visibility.NONE,
		setterVisibility = JsonAutoDetect.Visibility.NONE,
		fieldVisibility = JsonAutoDetect.Visibility.NONE,
		isGetterVisibility = JsonAutoDetect.Visibility.NONE,
		creatorVisibility = JsonAutoDetect.Visibility.NONE
)
public class GroovyScriptHolder {
	@JsonProperty("name")
	public String scriptName;
	@JsonProperty("content")
	public String scriptContent;
	@JsonProperty("strict")
	public boolean strict;
	public boolean shouldSave;
	public Object result;

	public Supplier<GroovyShell> shellProvider;
	public GroovyShell shell;

	public GroovyScriptResult run() {
		try {
			if (shell == null) {
				if (shellProvider == null) {
					//noinspection ThrowCaughtLocally
					throw new IllegalStateException("Neither Shell nor ShellProvider set");
				}
				else {
					shell = shellProvider.get();
				}
			}
			Object result = shell.parse(scriptContent).run();
			return GroovyScriptResult.success(result);
		}
		catch (Throwable t) {
			return GroovyScriptResult.failure(t);
		}
	}


}
