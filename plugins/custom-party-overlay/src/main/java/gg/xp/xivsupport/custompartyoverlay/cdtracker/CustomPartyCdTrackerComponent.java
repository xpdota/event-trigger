package gg.xp.xivsupport.custompartyoverlay.cdtracker;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.custompartyoverlay.BasePartyListComponent;
import gg.xp.xivsupport.events.state.combatstate.CooldownHelper;
import gg.xp.xivsupport.events.state.combatstate.CooldownStatus;
import gg.xp.xivsupport.events.triggers.jobs.gui.SettingsCdTrackerColorProvider;
import gg.xp.xivsupport.events.triggers.jobs.gui.VisualCdInfo;
import gg.xp.xivsupport.events.triggers.jobs.gui.VisualCdInfoMain;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.tables.renderers.ScaledImageComponent;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomPartyCdTrackerComponent extends BasePartyListComponent {

	private static final Logger log = LoggerFactory.getLogger(CustomPartyCdTrackerComponent.class);

	private final CooldownHelper cdh;
	private final CustomPartyCdTrackerConfig config;
	private final Map<ExtendedCooldownDescriptor, CooldownDisplayComponent> components = new HashMap<>();
	//	private final List<CooldownDisplayComponent> rawList = new ArrayList<>();
	private List<ExtendedCooldownDescriptor> enabled;
	private final JPanel panel = new JPanel(null) {
		{
			setLayout(null);
		}

		@Override
		public void validate() {
			super.validate();
			int spacing = config.getSpacing().get();
			boolean rtl = config.getRightToLeft().get();
			int height = getHeight();
			int width = getWidth();
			Component[] comps = getComponents();
			for (int i = 0; i < comps.length; i++) {
				int xOffset = i * (height + spacing);
				if ((xOffset + height) > width) {
					// Trigger a re-read
					forceReset();
				}
				int realX = rtl ? width - xOffset - height : xOffset;
				comps[i].setBounds(realX, 0, height, height);
			}
		}

		@Override
		public void setBounds(int x, int y, int width, int height) {
			super.setBounds(x, y, width, height);
			validate();
		}

		@Override
		public void paint(Graphics g) {
			super.paint(g);
		}
	};

	public CustomPartyCdTrackerComponent(CooldownHelper cdh, CustomPartyCdTrackerConfig config) {
		this.cdh = cdh;
		this.config = config;
		resetEnabledCds();
		panel.setOpaque(false);
//		panel.setBackground(Color.ORANGE);
		config.addListener(() -> SwingUtilities.invokeLater(panel::validate));
		config.addListener(this::forceReset);
	}

	private int maxItems() {
		return panel.getWidth() / (panel.getHeight() + config.getSpacing().get());
	}

	@Override
	protected Component makeComponent() {
		return panel;
	}

	private void resetEnabledCds() {
		enabled = config.getEnabledCds();
	}

	private void forceReset() {
		// dumb but works
		resetEnabledCds();
		lastJob = null;
	}

	private Job lastJob;

	@Override
	protected void reformatComponent(@NotNull XivPlayerCharacter xpc) {
		if (panel.getParent() == null) {
			// If we aren't visible yet, don't do anything
			return;
		}
		// I don't know if this can happen, but better safe than sorry
		Job job = xpc.getJob();
		if (job == null) {
			return;
		}
		JobType jobCat = job.getCategory();
		if (job != lastJob) {
			lastJob = job;
//			rawList.clear();
			components.clear();
			SwingUtilities.invokeLater(panel::removeAll);
		}
//		Map<ExtendedCooldownDescriptor, VisualCdInfo> statusNew = new HashMap<>();
		boolean scOnlyRez = config.getScOnlyRez().get();
		List<ExtendedCooldownDescriptor> enabled = this.enabled.stream()
				.filter(extendedCooldownDescriptor -> {
					if (extendedCooldownDescriptor == Cooldown.Swiftcast) {
						if (scOnlyRez) {
							return job.usesSwiftRez();
						}
						else {
							return jobCat == JobType.HEALER || jobCat == JobType.CASTER;
						}
					}
					else {
						return extendedCooldownDescriptor.getJob() == job
								|| extendedCooldownDescriptor.getJobType() == jobCat;
					}
				})
				.limit(maxItems())
				.toList();
		for (ExtendedCooldownDescriptor extendedCooldownDescriptor : enabled.subList(0, Math.min(enabled.size(), maxItems()))) {
			CooldownStatus raw = cdh.getCdStatusForPlayer(xpc, extendedCooldownDescriptor);
			VisualCdInfo vci;
			if (raw == null) {
				vci = new VisualCdInfoMain(extendedCooldownDescriptor);
			}
			else {
				vci = new VisualCdInfoMain(raw);
			}
//			statusNew.put(extendedCooldownDescriptor, vci);
			CooldownDisplayComponent disp = components.computeIfAbsent(extendedCooldownDescriptor, k -> {
				CooldownDisplayComponent newComponent = new CooldownDisplayComponent(extendedCooldownDescriptor);
//				rawList.add(newComponent);
				SwingUtilities.invokeLater(() -> panel.add(newComponent));
				return newComponent;
			});
			disp.setData(vci);
		}
		SwingUtilities.invokeLater(panel::validate);
//		statuses = statusNew;
	}

	private final class CooldownDisplayComponent extends JPanel {
		//		private final ExtendedCooldownDescriptor ecd;
//		private VisualCdInfo cdi;
		//		private AutoHeightScalingIcon icon;
		private ScaledImageComponent icon;
		private Color borderColor = Color.PINK;
		private double fillPercent;

		private CooldownDisplayComponent(ExtendedCooldownDescriptor ecd) {
//			this.ecd = ecd;
			setLayout(null);
			ActionIcon icon = ActionLibrary.iconForId(ecd.getPrimaryAbilityId());
			ScaledImageComponent scaleIcon = IconTextRenderer.getIconOnly(icon);
			if (scaleIcon != null) {
				this.icon = scaleIcon.cloneThis();
			}

//			ScaledImageComponent scaleIcon = IconTextRenderer.getIconOnly(icon);
//			if (scaleIcon != null) {
//				AutoHeightScalingIcon stretchyIcon = new AutoHeightScalingIcon(scaleIcon.cloneThis());
//				add(stretchyIcon, BorderLayout.CENTER);
//				this.icon = stretchyIcon;
//			}

			setBackground(Color.PINK);
		}

		@Override
		public void validate() {
			super.validate();
			icon = icon.withNewSize(getHeight() - 2 * config.getBorderWidth().get());
//			icon.setBounds(0, 0, getWidth(), getHeight());
//			icon.invalidate();
		}

		@Override
		public void setBounds(int x, int y, int width, int height) {
			super.setBounds(x, y, width, height);
			icon = icon.withNewSize(height - 2 * config.getBorderWidth().get());
//			icon.setBounds(0, 0, width, height);
//			icon.invalidate();
		}

		public void setData(VisualCdInfo cdi) {
//			this.cdi = cdi;
			SettingsCdTrackerColorProvider colors = config.getColors();
			this.borderColor = switch (cdi.getStatus()) {
				case READY, NOT_YET_USED -> colors.getReadyColor();
				case BUFF_PREAPP -> colors.getPreappColor();
				case BUFF_ACTIVE -> colors.getActiveColor();
				case ON_COOLDOWN -> colors.getOnCdColor();
			};
			fillPercent = cdi.getPercent();

//			setBorder(new LineBorder(borderColor, 4));
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D) g.create();
			g2d.setColor(borderColor);
			// Mostly copied from LineBorder
			Shape outer;
			int x = 0;
			int y = 0;
			int height = getHeight();
			int width = getWidth();
			int offs = config.getBorderRoundness().get();
			if (offs > 0) {
				outer = new RoundRectangle2D.Float(x, y, width, height, offs, offs);
			}
			else {
				outer = new Rectangle2D.Float(x, y, width, height);
			}


//			double fillAmount = (System.currentTimeMillis() % 10_000) / 10_000.0;
			double fillAmount = fillPercent;
//			switch (cdi.getStatus()) {
//
//			}
			// This is technically wrong since it is doing it linearly along the edge instead of radially
			Polygon clipPoly;
			if (fillAmount < 0.125) {
				clipPoly = new Polygon(
						new int[]{0, 0, (int) (width * (8 * fillAmount))},
						new int[]{0, -height, -height},
						3);
			}
			else if (fillAmount < 0.375) {
				clipPoly = new Polygon(
						new int[]{0, 0, width, width},
						new int[]{0, -height, -height, (int) (height * 8.0 * (fillAmount - 0.25))},
						4);
			}
			else if (fillAmount < 0.625) {
				clipPoly = new Polygon(
						new int[]{0, 0, width, width, (int) (-width * (8 * (fillAmount - 0.5)))},
						new int[]{0, -height, -height, height, height},
						5);
			}
			else if (fillAmount < 0.875) {
				clipPoly = new Polygon(
						new int[]{0, 0, width, width, -width, -width},
						new int[]{0, -height, -height, height, height, (int) (-height * 8.0 * (fillAmount - 0.75))},
						6);
			}
			else if (fillAmount < 1.0) {
				clipPoly = new Polygon(
						new int[]{0, 0, width, width, -width, -width, (int) (width * (8 * (fillAmount - 1.0)))},
						new int[]{0, -height, -height, height, height, -height, -height},
						7);
			}
			else {
				clipPoly = null;
			}
			{
				Graphics2D clipped = (Graphics2D) g2d.create();
				if (clipPoly != null) {
					clipPoly.translate(width / 2, height / 2);
					clipped.clip(clipPoly);
				}
				clipped.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				clipped.fill(outer);
			}
			AffineTransform tf = g2d.getTransform();
			int bw = config.getBorderWidth().get();
			tf.translate(bw, bw);
			g2d.setTransform(tf);
			icon.paint(g2d);
		}

	}
}
