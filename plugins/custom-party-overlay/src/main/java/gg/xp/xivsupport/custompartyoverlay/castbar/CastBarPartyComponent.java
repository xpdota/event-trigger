package gg.xp.xivsupport.custompartyoverlay.castbar;

import gg.xp.xivsupport.custompartyoverlay.BasePartyListComponent;
import gg.xp.xivsupport.custompartyoverlay.JobIconComponent;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
import gg.xp.xivsupport.events.triggers.jobs.gui.CastBarComponent;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class CastBarPartyComponent extends BasePartyListComponent {

	private final ActiveCastRepository acr;
	private final CastBarComponent bar;
	private final JobIconComponent jobIcon;
	private final CastBarComponentConfig config;
	private JPanel panel;

	public CastBarPartyComponent(ActiveCastRepository acr, CastBarComponentConfig config) {
		this.acr = acr;
		bar = new CastBarComponent();
		this.config = config;
		jobIcon = new JobIconComponent();
		config.addAndRunListener(this::applySettings);
	}

	private void applySettings() {
		bar.setBackground(config.getBackgroundColor().get());
		bar.setSuccessColor(config.getSuccessColor().get());
		bar.setInProgressColor(config.getInProgressColor().get());
		bar.setInterruptedColor(config.getInterruptedColor().get());
		bar.setUnknownColor(config.getUnknownColor().get());
		bar.setTextColor(config.getTextColor().get());
		redoLayout();
	}


	@Override
	protected Component makeComponent() {
		panel = new JPanel(new BorderLayout()) {
			@Override
			public void setBounds(int x, int y, int width, int height) {
				jobIcon.setPreferredSize(new Dimension(height, height));
				super.setBounds(x, y, width, height);
			}
		};
		panel.setOpaque(false);
		redoLayout();
		return panel;
	}

	private void redoLayout() {
		if (panel == null) {
			return;
		}
		panel.removeAll();
		panel.add(bar, BorderLayout.CENTER);
		switch (config.getJobIconPlacementSetting().get()) {
			case LEFT -> panel.add(jobIcon, BorderLayout.WEST);
			case RIGHT -> panel.add(jobIcon, BorderLayout.EAST);
		}
		panel.revalidate();

	}

	@Override
	protected void reformatComponent(@NotNull XivPlayerCharacter xpc) {
		@Nullable CastTracker castFor = acr.getCastFor(xpc);
		if (castFor != null) {
			if (castFor.getCast().getEstimatedTimeSinceExpiry().toMillis() > 3_000) {
				castFor = null;
			}
		}
		bar.setData(castFor);
		if (castFor != null) {
			XivCombatant target = castFor.getCast().getTarget();
			jobIcon.setJobFrom(target);
		}
		else {
			jobIcon.setJob(null);
		}
	}
}
