package gg.xp.xivsupport.gui.groovy;

import org.jetbrains.annotations.Nullable;

public record GroovyScriptResult(
		@Nullable Object result,
		@Nullable Throwable failure,
		boolean success
) {
	public static GroovyScriptResult success(@Nullable Object value) {
		return new GroovyScriptResult(value, null, true);
	}
	public static GroovyScriptResult failure(@Nullable Throwable failure) {
		return new GroovyScriptResult(null, failure, false);
	}
}
