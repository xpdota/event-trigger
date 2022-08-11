package gg.xp.xivsupport.custompartyoverlay;

import gg.xp.xivsupport.gui.tables.renderers.ComponentListRenderer;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class NameJobComponent extends BasePartyListComponent{

	private final ComponentListRenderer listRenderer;
	private final JLabel label = new JLabel();

	public NameJobComponent() {
		listRenderer = new ComponentListRenderer(2, true);
		listRenderer.setOpaque(false);
		label.setOpaque(false);
		label.setForeground(new Color(255, 255, 255));
	}

	@Override
	protected Component makeComponent() {
		return listRenderer;
	}

	@Override
	protected void reformatComponent(@NotNull XivPlayerCharacter xpc) {
		String name = xpc.getName();
		Component icon = IconTextRenderer.getIconOnly(xpc.getJob());
		label.setText(name);
		if (icon != null) {
			listRenderer.setComponents(List.of(icon, label));
		}
		else {
			listRenderer.setComponents(Collections.singletonList(label));
		}
	}
}
