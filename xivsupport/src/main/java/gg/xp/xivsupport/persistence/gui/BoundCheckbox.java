package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivdata.data.*;

import javax.swing.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BoundCheckbox extends JCheckBox {

	public BoundCheckbox(String text, Supplier<Boolean> getter, Consumer<Boolean> setter) {
		super(text);
		setModel(new ToggleButtonModel() {
			@Override
			public boolean isSelected() {
				return getter.get();
			}

			@Override
			public void setSelected(boolean b) {
				setter.accept(b);
			}

		});
	}
}
