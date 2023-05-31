package gg.xp.xivsupport.groovy;

import gg.xp.xivsupport.gui.groovy.DisplayControl;
import org.jetbrains.annotations.Nullable;

public record GroovyScriptResult(
		@Nullable Object result,
		@Nullable Throwable failure,
		boolean success,
		DisplayControl displayControl
) {
	public static GroovyScriptResult success(DisplayControl dc, @Nullable Object value) {
		return new GroovyScriptResult(value, null, true, dc);
	}
	public static GroovyScriptResult failure(DisplayControl dc, @Nullable Throwable failure) {
		return new GroovyScriptResult(null, failure, false, dc);
	}
}
