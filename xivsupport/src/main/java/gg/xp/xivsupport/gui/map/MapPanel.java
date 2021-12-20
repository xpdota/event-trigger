package gg.xp.xivsupport.gui.map;

import gg.xp.xivdata.jobs.ActionIcon;
import gg.xp.xivdata.jobs.Job;
import gg.xp.xivdata.jobs.XivMap;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.tables.renderers.OverlapLayout;
import gg.xp.xivsupport.gui.tables.renderers.RenderUtils;
import gg.xp.xivsupport.models.CombatantType;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.Serial;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MapPanel extends TitleBorderFullsizePanel {

	private static final Logger log = LoggerFactory.getLogger(MapPanel.class);
	@Serial
	private static final long serialVersionUID = 6804697839463860552L;

	private final JPanel mapPanel;
	private final Map<Long, PlayerDoohickey> things = new HashMap<>();
	private static final int MAP_SIZE = 400;
	private static final int MAP_UI_OFFSET = MAP_SIZE / 2;
	private final XivState state;
	private XivMap map = XivMap.UNKNOWN;

	public MapPanel(XivState state) {
		super("Map");
		this.state = state;
		// TODO: revisit zooming later
		setPreferredSize(getMaximumSize());
		setLayout(new FlowLayout());
//		JPanel zoomPanel = new ZoomPanel();
//		zoomPanel.setPreferredSize(getMaximumSize());
//		zoomPanel.add(new JLabel("Test Text Here"));
//		add(zoomPanel);
		mapPanel = new JPanel();
		mapPanel.setLayout(null);
		mapPanel.setPreferredSize(new Dimension(MAP_SIZE, MAP_SIZE));
//		mapPanel.setSize(new Dimension(400, 400));
		mapPanel.setBorder(new LineBorder(Color.BLUE, 2));
		add(mapPanel);
		Insets insets = mapPanel.getInsets();
//		mapPanel.repaint();
		// TODO: this isn't a very good way of doing it, because it runs the entire thing in the EDT whereas we really
		// don't need the computational parts to be on the EDT.
		// TODO: lower this back down alter
		new Timer(1000, e -> {
			this.refresh();
		}).start();
	}

	private void refresh() {
//		log.info("Map refresh");
		List<XivCombatant> combatants = state.getCombatantsListCopy();
		map = state.getMap();
		combatants.stream()
				.filter(cbt -> {
					CombatantType type = cbt.getType();
					return type != CombatantType.NONCOM && type != CombatantType.FAKE && type != CombatantType.GP && type != CombatantType.OTHER;
				})
//				.filter(XivCombatant::isPc)
				.forEach(cbt -> {
					long id = cbt.getId();
					if (cbt.getPos() == null) {
						return;
					}
					things.computeIfAbsent(id, (unused) -> createNew(cbt))
							.update(cbt);
				});

		Set<Long> allKeys = things.keySet();
		List<Long> keysToRemove = allKeys.stream().filter(v -> combatants.stream().noneMatch(c -> c.getId() == v))
				.collect(Collectors.toList());
		keysToRemove.forEach(k -> {
			PlayerDoohickey toRemove = things.remove(k);
			toRemove.setVisible(false);
			remove(toRemove);
		});
		revalidate();
		repaint();
	}

	private PlayerDoohickey createNew(XivCombatant cbt) {
		PlayerDoohickey player = new PlayerDoohickey(cbt);
		mapPanel.add(player);
		return player;
	}

	private int translateX(double originalX) {
		// Already divided by 100
		double c = map.getScaleFactor();
		double x = (originalX + map.getOffsetX()) * c * 200;
		return (int) (((41.0 / c) * ((x + 1024) / 2048) + 1) * 100) / 100 + MAP_UI_OFFSET;
//		return (int) ((x + map.getOffsetX()) * map.getScaleFactor() + MAP_UI_OFFSET);
	}

	private int translateY(double originalY) {
		double c = map.getScaleFactor();
		double y = (originalY + map.getOffsetY()) * c * 200;
		return (int) (((41.0 / c) * ((y + 1024) / 2048) + 1) * 100) / 100 + MAP_UI_OFFSET;
//		return (int) ((y + map.getOffsetY()) * map.getScaleFactor() + MAP_UI_OFFSET);
	}

	// TODO: name....
	private class PlayerDoohickey extends JPanel {

		private final JLabel defaultLabel;
		private double x;
		private double y;
		private Job oldJob;
		private Component icon;

		public PlayerDoohickey(XivCombatant cbt) {
//			setPreferredSize(new Dimension(50, 50));
			setBorder(new LineBorder(Color.PINK, 2));
			setLayout(new OverlapLayout());
			setOpaque(false);
			defaultLabel = new JLabel(cbt.getName());
			addIcon(cbt);
			RenderUtils.setTooltip(this, String.format("%s (0x%x, %s)", cbt.getName(), cbt.getId(), cbt.getId()));
		}

		public void update(XivCombatant cbt) {
			RenderUtils.setTooltip(this, String.format("%s (0x%x, %s)", cbt.getName(), cbt.getId(), cbt.getId()));
			Position pos = cbt.getPos();
			if (pos != null) {
				this.x = pos.getX();
				this.y = pos.getY();
				setCenterPoint();
			}
			if (cbt instanceof XivPlayerCharacter) {
				Job newJob = ((XivPlayerCharacter) cbt).getJob();
				if (newJob != oldJob) {
					remove(icon);
					addIcon(cbt);
				}
				oldJob = newJob;
			}
		}

		private void addIcon(XivCombatant cbt) {
			if (cbt instanceof XivPlayerCharacter) {
				Job job = ((XivPlayerCharacter) cbt).getJob();
				setBorder(new LineBorder(new Color(168, 243, 243)));
//				log.info("JOB: {}", job);
				icon = IconTextRenderer.getComponent(job, defaultLabel, true, false, true);
			}
			else {
				setBorder(new LineBorder(new Color(128, 0, 0)));
				// TODO: find good icon
				icon = IconTextRenderer.getComponent(ActionIcon.forId(2246), defaultLabel, true, false, true);
			}
			add(icon);
			validate();
		}

		public void setCenterPoint() {
			Dimension size = getSize();
			if (size.width == 0 || size.height == 0) {
				size = getPreferredSize();
			}
			setBounds(translateX(this.x) - (size.width / 2), translateY(this.y) - (size.height / 2), size.width, size.height);
		}
	}


}
