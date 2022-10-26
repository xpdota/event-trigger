package gg.xp.xivsupport.custompartyoverlay;

import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

public class DoNothingComponent extends BasePartyListComponent {
	@Override
	protected Component makeComponent() {
		JPanel panel = new JPanel();
		panel.setOpaque(false);
		panel.setBorder(new LineBorder(new Color(255, 128, 0), 4));
		return panel;
	}

	@Override
	protected void reformatComponent(@NotNull XivPlayerCharacter xpc) {

	}
}
