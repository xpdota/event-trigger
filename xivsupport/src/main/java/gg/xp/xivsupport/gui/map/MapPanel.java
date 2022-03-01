package gg.xp.xivsupport.gui.map;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.ActionLibrary;
import gg.xp.xivdata.data.Job;
import gg.xp.xivdata.data.XivMap;
import gg.xp.xivsupport.events.actlines.events.MapChangeEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
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
import java.net.URL;
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
	private Image backgroundImage;

	private static final Color enemyColor = new Color(128, 0, 0);
	private static final Color otherPlayerColor = new Color(82, 204, 82);
	private static final Color partyMemberColor = new Color(104, 120, 222);
	private static final Color localPcColor = new Color(150, 199, 255);

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
		new RefreshLoop<>("MapRefresh", this, map -> {
			SwingUtilities.invokeLater(() -> {
				if (map.isShowing()) {
					map.refresh();
				}
			});
		}, unused -> 100L).start();
		addMouseWheelListener(this);
		addMouseMotionListener(this);
		addMouseListener(this);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (backgroundImage != null) {
			g.drawImage(
					backgroundImage,
					// Image is 2048x2048, and 0,0 is the top left, but our internal coords treat 0,0 as the center
					translateXscrn(-1024),
					translateYscrn(-1024),
					(int) (2048 * zoomFactor),
					(int) (2048 * zoomFactor),
					getBackground(),
					null);
		}
	}

	@HandleEvents
	public void mapChange(EventContext context, MapChangeEvent event) {
		setNewBackgroundImage(event.getMap());
		resetPanAndZoom();
	}

	private void setNewBackgroundImage(XivMap map) {
		URL image = map.getImage();
		if (image == null) {
			this.backgroundImage = null;
		}
		else {
			this.backgroundImage = Toolkit.getDefaultToolkit().getImage(image);
		}
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

	private double translateXmap(double originalX) {
		// Already divided by 100
		double c = map.getScaleFactor();
		return (originalX + map.getOffsetX()) * c;
	}

	private double translateYmap(double originalY) {
		double c = map.getScaleFactor();
		return (originalY + map.getOffsetY()) * c;
	}

	private int translateXscrn(double originalX) {
		return (int) ((originalX * zoomFactor) + curXpan + getWidth() / 2.0);
	}

	private int translateYscrn(double originalY) {
		return (int) ((originalY * zoomFactor) + curYpan + getHeight() / 2.0);
	}

	private int translateX(double originalX) {
		return translateXscrn(translateXmap(originalX));
	}

	private int translateY(double originalY) {
		return translateYscrn(translateYmap(originalY));
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

	@SuppressWarnings({"NonAtomicOperationOnVolatileField", "NumericCastThatLosesPrecision"})
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
		// Roundoff error - make sure it actually snaps back to exactly 1.0 if it's somewhere close to that.
		if (zoomFactor > 0.94 && zoomFactor < 1.06) {
			zoomFactor = 1.0;
		}
		double xRel = MouseInfo.getPointerInfo().getLocation().getX() - getLocationOnScreen().getX() - getWidth() / 2.0;
		double yRel = MouseInfo.getPointerInfo().getLocation().getY() - getLocationOnScreen().getY() - getHeight() / 2.0;

		double zoomDiv = zoomFactor / prevZoomFactor;

		curXpan = (int) ((zoomDiv) * (curXpan) + (1 - zoomDiv) * xRel);
		curYpan = (int) ((zoomDiv) * (curYpan) + (1 - zoomDiv) * yRel);
		refresh();

	}

	@Override
	public void mouseMoved(MouseEvent e) {

	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON2) {
			resetPanAndZoom();
		}
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

	private static String formatTooltip(XivCombatant cbt) {
		return String.format("%s (0x%x, %s)", cbt.getName(), cbt.getId(), cbt.getId());
	}

	// TODO: name....
	private class PlayerDoohickey extends JPanel {

		private final JLabel defaultLabel;
		private double x;
		private double y;
		private Job oldJob;
		private Component icon;

		public PlayerDoohickey(XivCombatant cbt) {
			setBorder(new LineBorder(Color.PINK, 2));
			setLayout(new OverlapLayout());
			setOpaque(false);
			defaultLabel = new JLabel(cbt.getName());
			formatComponent(cbt);
			RenderUtils.setTooltip(this, formatTooltip(cbt));
			addMouseWheelListener(MapPanel.this);
			addMouseMotionListener(MapPanel.this);
			addMouseListener(MapPanel.this);
		}

		public void update(XivCombatant cbt) {
			RenderUtils.setTooltip(this, formatTooltip(cbt));
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
				if (cbt.isThePlayer()) {
					setBorder(new LineBorder(localPcColor));
					setBackground(localPcColor);
				} else if (state.getPartyList().contains(cbt)) {
					setBorder(new LineBorder(partyMemberColor));
					setBackground(partyMemberColor);
				} else {
					setBorder(new LineBorder(otherPlayerColor));
					setBackground(otherPlayerColor);
				}
				icon = IconTextRenderer.getComponent(job, defaultLabel, true, false, true);
				setOpaque(true);
				// TODO: this doesn't work because it hasn't been added to the container yet
//				MapPanel.this.setComponentZOrder(this, 0);
			}
			else {
				setBorder(new LineBorder(enemyColor));
				setOpaque(false);
				// TODO: find good icon
				icon = IconTextRenderer.getComponent(ActionLibrary.iconForId(2246), defaultLabel, true, false, true);
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
