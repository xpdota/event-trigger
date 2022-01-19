package gg.xp.xivsupport.gui.map;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.jobs.ActionIcon;
import gg.xp.xivdata.jobs.Job;
import gg.xp.xivdata.jobs.XivMap;
import gg.xp.xivsupport.events.actlines.events.MapChangeEvent;
import gg.xp.xivsupport.events.state.XivState;
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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.Serial;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapPanel extends JPanel implements MouseMotionListener, MouseListener, MouseWheelListener {

	private static final Logger log = LoggerFactory.getLogger(MapPanel.class);
	@Serial
	private static final long serialVersionUID = 6804697839463860552L;

	private final Map<Long, PlayerDoohickey> things = new HashMap<>();
	private double zoomFactor = 1;
	private volatile int curXpan;
	private volatile int curYpan;
	private volatile Point dragPoint;
	private final XivState state;
	private XivMap map = XivMap.UNKNOWN;

	public MapPanel(XivState state) {
		this.state = state;
//		setLayout(new FlowLayout());
		setLayout(null);
//		setPreferredSize(new Dimension(MAP_SIZE, MAP_SIZE));
//		mapPanel.setBorder(new LineBorder(Color.BLUE, 2));
		setBackground(new Color(168, 153, 114));
		// TODO: this isn't a very good way of doing it, because it runs the entire thing in the EDT whereas we really
		// don't need the computational parts to be on the EDT.
		// TODO: lower this back down alter
		new Timer(100, e -> {
			if (this.isShowing()) {
				this.refresh();
			}
		}).start();
		addMouseWheelListener(this);
		addMouseMotionListener(this);
		addMouseListener(this);
	}

	@HandleEvents
	public void mapChange(EventContext context, MapChangeEvent event) {
		resetPanAndZoom();
	}

	private void resetPanAndZoom() {
		curXpan = 0;
		curYpan = 0;
		zoomFactor = 1;
		SwingUtilities.invokeLater(this::refresh);
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
		List<Long> keysToRemove = allKeys.stream().filter(v -> combatants.stream().noneMatch(c -> c.getId() == v)).toList();
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
		add(player);
		return player;
	}

	private int translateX(double originalX) {
		// Already divided by 100
		double c = map.getScaleFactor();
		double x = (originalX + map.getOffsetX()) * c * 200;
		return (int) (((((41.0 / c) * ((x + 1024) / 2048) + 1) * 100) / 100 * zoomFactor) + curXpan + getWidth() / 2.0);
	}

	private int translateY(double originalY) {
		double c = map.getScaleFactor();
		double y = (originalY + map.getOffsetY()) * c * 200;
		return (int) (((((41.0 / c) * ((y + 1024) / 2048) + 1) * 100) / 100 * zoomFactor) + curYpan + getHeight() / 2.0);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		Point curPoint = e.getLocationOnScreen();
		double xDiff = curPoint.x - dragPoint.x;
		double yDiff = curPoint.y - dragPoint.y;
		curXpan += xDiff;
		curYpan += yDiff;
//		log.info("Map Panel Drag: {},{}", xDiff, yDiff);
		dragPoint = curPoint;
		refresh();
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		double prevZoomFactor = zoomFactor;
		//Zoom in
		if (e.getWheelRotation() < 0) {
			zoomFactor *= 1.1;
		}
		//Zoom out
		if (e.getWheelRotation() > 0) {
			zoomFactor /= 1.1;
		}
		double xRel = MouseInfo.getPointerInfo().getLocation().getX() - getLocationOnScreen().getX() - getWidth() / 2.0;
		double yRel = MouseInfo.getPointerInfo().getLocation().getY() - getLocationOnScreen().getY() - getHeight() / 2.0;
//		log.info("Wheel move: xrel/yrel: {} {}", xRel, yRel);

		double zoomDiv = zoomFactor / prevZoomFactor;

//		log.info("Before: {} {}", curXpan, curYpan);
		curXpan = (int) ((zoomDiv) * (curXpan) + (1 - zoomDiv) * xRel);
		curYpan = (int) ((zoomDiv) * (curYpan) + (1 - zoomDiv) * yRel);
//		log.info("After: {} {}", curXpan, curYpan);
//		log.info("New map zoom factor: {} (rel {})", zoomFactor, zoomDiv);
		refresh();

	}

	@Override
	public void mouseMoved(MouseEvent e) {

	}

	@Override
	public void mouseClicked(MouseEvent e) {

	}

	@Override
	public void mousePressed(MouseEvent e) {
//		log.info("Pressed");
		dragPoint = MouseInfo.getPointerInfo().getLocation();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
//		log.info("Released");
		refresh();
	}

	@Override
	public void mouseEntered(MouseEvent e) {
//		log.info("Entered");

	}

	@Override
	public void mouseExited(MouseEvent e) {
//		log.info("Exit");

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
			formatComponent(cbt);
			RenderUtils.setTooltip(this, String.format("%s (0x%x, %s)", cbt.getName(), cbt.getId(), cbt.getId()));
			addMouseWheelListener(MapPanel.this);
			addMouseMotionListener(MapPanel.this);
			addMouseListener(MapPanel.this);
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
					formatComponent(cbt);
				}
				oldJob = newJob;
			}
		}

		private void formatComponent(XivCombatant cbt) {
			if (cbt instanceof XivPlayerCharacter) {
				Job job = ((XivPlayerCharacter) cbt).getJob();
				setBorder(new LineBorder(new Color(168, 243, 243)));
				setBackground((new Color(168, 243, 243)));
//				log.info("JOB: {}", job);
				icon = IconTextRenderer.getComponent(job, defaultLabel, true, false, true);
				setOpaque(true);
				// TODO: this doesn't work because it hasn't been added to the container yet
//				MapPanel.this.setComponentZOrder(this, 0);
			}
			else {
				setBorder(new LineBorder(new Color(128, 0, 0)));
				setOpaque(false);
				// TODO: find good icon
				icon = IconTextRenderer.getComponent(ActionIcon.forId(2246), defaultLabel, true, false, true);
//				MapPanel.this.setComponentZOrder(this, 5);
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
