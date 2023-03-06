package gg.xp.xivsupport.gui.groovy;

import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public record TableColumn<R, C, D>(String name, Function<R, C> func, @Nullable Function<C, D> disp) {

	public TableColumn(String name, Function<R, C> func) {
		this(name, func, null);
	}

	public C get(R obj) {
		return func.apply(obj);
	}

}
