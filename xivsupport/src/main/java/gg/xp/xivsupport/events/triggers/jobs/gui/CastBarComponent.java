package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivsupport.events.state.combatstate.CastResult;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
import gg.xp.xivsupport.gui.tables.renderers.ResourceBar;
import gg.xp.xivsupport.models.XivAbility;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class CastBarComponent extends ResourceBar {

	public static final Color defaultInProgressColor = new Color(47, 82, 26);
	public static final Color defaultSuccessColor = new Color(9, 46, 115);
	public static final Color defaultInterruptedColor = new Color(94, 10, 10);
	public static final Color defaultUnknownColor = new Color(100, 71, 9);
	public static final Color defaultBackgroundColor = new Color(20, 20, 20);
	public static final Color defaultTextColor = Color.WHITE;

	private Color inProgressColor = defaultInProgressColor;
	private Color successColor = defaultSuccessColor;
	private Color interruptedColor = defaultInterruptedColor;
	private Color unknownColor = defaultUnknownColor;

	private @Nullable CastTracker tracker;

	public CastBarComponent() {
		setOpaque(false);
		setColor3(defaultBackgroundColor);
		setTextColor(defaultTextColor);
	}

	public void setData(@Nullable CastTracker tracker) {
		this.tracker = tracker;
		if (tracker != null) {
			long duration = tracker.getCastDuration().toMillis();
			long current = tracker.getElapsedDuration().toMillis();
			double pct = ((double) current) / duration;
			setPercent1(pct);
			CastResult result = tracker.getResult();
			// TODO: colors
			setColor1(switch (result) {
				case IN_PROGRESS -> inProgressColor;
				case SUCCESS -> successColor;
				case INTERRUPTED -> interruptedColor;
				case UNKNOWN -> unknownColor;
			});
			setTextOptions(makeText(tracker));
			revalidate();
		}
	}

	protected String makeText(CastTracker tracker) {
		return tracker.getCast().getAbility().getName();
	}

	public void setInProgressColor(Color inProgressColor) {
		this.inProgressColor = inProgressColor;
	}

	public void setSuccessColor(Color successColor) {
		this.successColor = successColor;
	}

	public void setInterruptedColor(Color interruptedColor) {
		this.interruptedColor = interruptedColor;
	}

	public void setUnknownColor(Color unknownColor) {
		this.unknownColor = unknownColor;
	}

	@Override
	public void setBackground(Color bg) {
		setColor3(bg);
	}

	@Override
	public void paint(Graphics g) {
		// Just do nothing if there's no active cast
		if (tracker == null) {
			return;
		}
		super.paint(g);
	}

	@Override
	public String getToolTipText() {
		CastTracker tracker = this.tracker;
		if (tracker == null) {
			// TODO: should it be null?
			return null;
		}
		XivAbility ability = tracker.getCast().getAbility();
		String skillNameAndId = String.format("(%s - 0x%X)", ability.getName(), ability.getId());
		return switch (tracker.getResult()) {
			case IN_PROGRESS -> String.format("%.03fs / %.03fs %s", tracker.getElapsedDuration().toMillis() / 1000.0, tracker.getCastDuration().toMillis() / 1000.0, skillNameAndId);
			case SUCCESS -> String.format("Done, %.03fs %s", tracker.getCastDuration().toMillis() / 1000.0, skillNameAndId);
			case INTERRUPTED -> String.format("Interrupted at %.03fs / %.03fs %s", tracker.getElapsedDuration().toMillis() / 1000.0, tracker.getCastDuration().toMillis() / 1000.0, skillNameAndId);
			case UNKNOWN -> String.format("??? %.03fs / %.03fs %s", tracker.getElapsedDuration().toMillis() / 1000.0, tracker.getCastDuration().toMillis() / 1000.0, skillNameAndId);
		};
	}
}
