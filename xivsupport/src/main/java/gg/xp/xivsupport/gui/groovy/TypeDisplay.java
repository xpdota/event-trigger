package gg.xp.xivsupport.gui.groovy;

import java.util.function.Function;

public record TypeDisplay<X>(Class<X> cls, Function<? super X, Object> func) {

	public boolean applicableTo(Object object) {
		return cls.isInstance(object);
	}

}
