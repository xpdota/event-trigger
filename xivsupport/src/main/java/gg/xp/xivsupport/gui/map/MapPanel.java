package gg.xp.xivsupport.gui.map;

import com.formdev.flatlaf.util.ScaledImageIcon;
import gg.xp.xivdata.data.ActionLibrary;
import gg.xp.xivdata.data.Job;
import gg.xp.xivdata.data.XivMap;
import gg.xp.xivsupport.events.actlines.events.MapChangeEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
import gg.xp.xivsupport.events.triggers.jobs.gui.CastBarComponent;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.gui.tables.renderers.HpBar;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.tables.renderers.OverlapLayout;
import gg.xp.xivsupport.gui.tables.renderers.RenderUtils;
import gg.xp.xivsupport.models.CombatantType;
import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.io.Serial;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class MapPanel extends JPanel implements MouseMotionListener, MouseListener, MouseWheelListener {

	private static final Logger log = LoggerFactory.getLogger(MapPanel.class);
	@Serial
	private static final long serialVersionUID = 6804697839463860552L;

	private final Object thingsLock = new Object();
	private final Map<Long, PlayerDoohickey> things = new HashMap<>();
	private final RefreshLoop<MapPanel> refresher;
	private double zoomFactor = 1;
	private volatile int curXpan;
	private volatile int curYpan;
	private volatile Point dragPoint;
	private final XivState state;
	private final ActiveCastRepository acr;
	private XivMap map = XivMap.UNKNOWN;
	private Image backgroundImage;
	// -1 indicates no selection
	private volatile long selection = -1;
	private Consumer<@Nullable XivCombatant> selectionCallback = l -> {
	};

	private static final Color enemyColor = new Color(128, 0, 0);
	private static final Color otherPlayerColor = new Color(82, 204, 82);
	private static final Color partyMemberColor = new Color(104, 120, 222);
	private static final Color localPcColor = new Color(150, 199, 255);

	public MapPanel(XivState state, ActiveCastRepository acr) {
		this.state = state;
		this.acr = acr;
		setLayout(null);
		setBackground(new Color(168, 153, 114));
		refresher = new RefreshLoop<>("MapRefresh", this, map -> {
			SwingUtilities.invokeLater(() -> {
				if (map.isShowing()) {
					refresh();
				}
			});
		}, unused -> 100L);
		refresher.start();
		addMouseWheelListener(this);
		addMouseMotionListener(this);
		addMouseListener(this);
		setIgnoreRepaint(true);
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

	public void mapChange(MapChangeEvent event) {
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
		triggerRefresh();
	}

	private void triggerRefresh() {
		refresher.refreshNow();
	}

	private volatile List<XivCombatant> combatants = Collections.emptyList();

	public void setCombatants(List<XivCombatant> combatants) {
		this.combatants = new ArrayList<>(combatants);
		SwingUtilities.invokeLater(this::refresh);
	}

	private void refresh() {
//		log.info("Map refresh");
		List<XivCombatant> combatants = this.combatants;
		map = state.getMap();
		combatants.stream()
				.filter(cbt -> {
					CombatantType type = cbt.getType();
					return type != CombatantType.NONCOM && type != CombatantType.GP && type != CombatantType.OTHER;
				})
				.forEach(cbt -> {
					long id = cbt.getId();
					if (cbt.getPos() == null) {
						return;
					}
					@Nullable CastTracker cast = acr.getCastFor(cbt);
					// Only lock for checking if it's there
					PlayerDoohickey pdh = things.computeIfAbsent(id, (unused) -> createNew(cbt));
					pdh.update(cbt, cast);
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
		triggerRefresh();
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
		triggerRefresh();

	}

	@Override
	public void mouseMoved(MouseEvent e) {

	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON2) {
			resetPanAndZoom();
		}
		else {
			if (e.getComponent() instanceof PlayerDoohickey pd) {
				selectionCallback.accept(pd.cbt);
			}
			else {
				selectionCallback.accept(null);
			}
//			log.info("Clicked on {}", getComponentAt(e.getPoint()));
		}
	}
//
//	@Override
//	public Component getComponentAt(Point p) {
//		for (Component component : getComponents()) {
//			if (component.getBounds().contains(p)) {
//				return component;
//			}
//		}
//		return this;
//	}
//
	@Override
	public void mousePressed(MouseEvent e) {
//		log.info("Pressed");
		dragPoint = MouseInfo.getPointerInfo().getLocation();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
//		log.info("Released");
		triggerRefresh();
	}

	@Override
	public void mouseEntered(MouseEvent e) {
//		log.info("Entered");

	}

	@Override
	public void mouseExited(MouseEvent e) {
//		log.info("Exit");

	}

	public void setSelection(@Nullable XivCombatant selection) {
		long newSelection;
		if (selection == null) {
			newSelection = -1;
		}
		else {
			newSelection = selection.getId();
		}
		if (newSelection != this.selection) {
			log.info("New map selection: {} -> {}",
					this.selection == -1 ? "none" : String.format("0x%X", this.selection),
					newSelection == -1 ? "none" : String.format("0x%X", newSelection));
			this.selection = newSelection;
			Component[] components = getComponents();
			removeAll();
			Arrays.stream(components).sorted(Comparator.comparing(comp -> {
				if (comp instanceof PlayerDoohickey pd) {
					if (pd.isSelected()) {
						return 0;
					}
					else {
						return 1;
					}
				}
				else {
					return -1;
				}
			})).forEach(this::add);
			repaint();
		}
	}

	public void setSelectionCallback(Consumer<@Nullable XivCombatant> selectionCallback) {
		this.selectionCallback = selectionCallback;
	}

	private static String formatTooltip(XivCombatant cbt) {
		StringBuilder tt = new StringBuilder();
		tt.append(cbt.getName()).append(" (").append(String.format("0x%X, %s)", cbt.getId(), cbt.getId()));
		if (cbt.getbNpcId() != 0) {
			tt.append("\nNPC ID ").append(cbt.getbNpcId());
		}
		if (cbt.getbNpcNameId() != 0) {
			tt.append("\nNPC Name ").append(cbt.getbNpcNameId());
		}
		tt.append('\n').append(cbt.getPos());
		if (cbt.getHp() != null) {
			tt.append("\nHP: ").append(String.format("%s / %s", cbt.getHp().current(), cbt.getHp().max()));
		}
		return tt.toString();
	}

	@Override
	protected void paintChildren(Graphics g) {
		super.paintChildren(g);
		things.values().forEach(v -> {
			if (v.isSelected()) {
				v.repaint();
//				v.paintComponent(g);
//				paintComponents();
//				v.paint(g);
			}
		});
	}


	// TODO: name....
	private class PlayerDoohickey extends JPanel {

		private static final Border selectionBorder = new LineBorder(Color.CYAN, 2);
		private static final Color selectedBackground = new Color(192, 255, 255, 128);
		private final JLabel defaultLabel;
		private final XivCombatant cbt;
		// This red should never actually show up
		private Color mainColor = new Color(255, 0, 0);
		private double x;
		private double y;
		private final JPanel inner;
		private Job oldJob;
		private Component icon;
		private final CastBarComponent castBar;
		private final HpBar hpBar;
		private final JLabel nameLabel;
		private final JLabel idLabel;
		private final long cbtId;
		private double facing;

		public PlayerDoohickey(XivCombatant cbt) {
			this.cbt = cbt;
			cbtId = cbt.getId();
			inner = new JPanel() {
				@Override
				public Color getBackground() {
					return mainColor;
				}
			};
			inner.setBorder(new LineBorder(Color.PINK, 2));
			setLayout(null);
			inner.setLayout(new OverlapLayout());
			inner.setOpaque(false);
			setOpaque(false);
			defaultLabel = new JLabel(cbt.getName());
			formatComponent(cbt);
			RenderUtils.setTooltip(this, formatTooltip(cbt));
			addMouseWheelListener(MapPanel.this);
			addMouseMotionListener(MapPanel.this);
			addMouseListener(MapPanel.this);
			add(inner);
			int outerSize = 100;
			int center = 50;
			setSize(outerSize, outerSize);
			int innerW = inner.getPreferredSize().width;
			int innerH = inner.getPreferredSize().height;
			inner.setBounds(new Rectangle(center - (innerW * 2), center - (innerH / 2), innerW, innerH));
			this.castBar = new CastBarComponent() {
				@Override
				public void paint(Graphics g) {
					// Make it a little bit transparent
					((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.90f));
					super.paint(g);
				}
			};

			FacingAngleIndicator arrow = new FacingAngleIndicator();
			arrow.setBounds(center - 10, center - 10, 20, 20);
			add(arrow);

			castBar.setBounds(0, 81, 100, 19);
			add(castBar);
			this.hpBar = new HpBar();
			hpBar.setBounds(0, 62, 100, 19);
			hpBar.setBgTransparency(172);
			hpBar.setFgTransparency(220);
			add(hpBar);

			nameLabel = new JLabel(cbt.getName());
			nameLabel.setBounds(0, 7, 100, 17);
			nameLabel.setOpaque(false);
			nameLabel.setForeground(Color.BLACK);
			nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
			add(nameLabel);

			idLabel = new JLabel(String.format("0x%X", cbt.getId()));
			idLabel.setBounds(0, 23, 100, 17);
			idLabel.setOpaque(false);
			idLabel.setForeground(Color.BLACK);
			idLabel.setHorizontalAlignment(SwingConstants.CENTER);
			add(idLabel);

			revalidate();
		}

		@Override
		public String getToolTipText(MouseEvent event) {
			if (castBar.getBounds().contains(event.getPoint())) {
				return castBar.getToolTipText();
			}
			return super.getToolTipText(event);
		}

		// Setting to -2 so it will never match initially
		long oldHpCurrent = -2;
		long oldHpMax = -2;

		public void update(XivCombatant cbt, @Nullable CastTracker castData) {
			RenderUtils.setTooltip(this, formatTooltip(cbt));
			setBounds(getBounds());
			nameLabel.setText(cbt.getName());
			Position pos = cbt.getPos();
			if (pos != null) {
				this.x = pos.getX();
				this.y = pos.getY();
				this.facing = pos.getHeading();
			}
			if (cbt instanceof XivPlayerCharacter pc) {
				Job newJob = pc.getJob();
				if (newJob != oldJob) {
					inner.remove(icon);
					formatComponent(cbt);
				}
				oldJob = newJob;
			}
			if (castData == null || castData.getCast().getEstimatedTimeSinceExpiry().toMillis() > 5000) {
				castBar.setData(null);
			}
			else {
				castBar.setData(castData);
			}

			HitPoints hp = cbt.getHp();
			long hpCurrent = hp == null ? -1 : hp.current();
			long hpMax = hp == null ? -1 : hp.max();
			// Ignore updates where nothing changed
			if (hpCurrent == oldHpCurrent && hpMax == oldHpMax) {
				return;
			}
			oldHpCurrent = hpCurrent;
			oldHpMax = hpMax;
			if (hp == null) {
				hpBar.setVisible(false);
			}
			else if (cbt.getType() == CombatantType.FAKE || hp.max() == 1 || hp.current() < 0) {
				hpBar.setVisible(false);
			}
			else {
				hpBar.setVisible(true);
				hpBar.setData(cbt, 0);
			}
			hpBar.revalidate();
		}

		private void formatComponent(XivCombatant cbt) {
			if (cbt instanceof XivPlayerCharacter pc) {
				Job job = pc.getJob();
				if (cbt.isThePlayer()) {
					mainColor = localPcColor;
//					inner.setBorder(new LineBorder(localPcColor));
//					inner.setBackground(localPcColor);
				}
				else if (state.getPartyList().contains(cbt)) {
					mainColor = partyMemberColor;
//					inner.setBorder(new LineBorder(partyMemberColor));
//					inner.setBackground(partyMemberColor);
				}
				else {
					mainColor = otherPlayerColor;
//					inner.setBorder(new LineBorder(otherPlayerColor));
//					inner.setBackground(otherPlayerColor);
				}
				icon = IconTextRenderer.getComponent(job, defaultLabel, true, false, true);
//				inner.setOpaque(true);
				// TODO: this doesn't work because it hasn't been added to the container yet
//				MapPanel.this.setComponentZOrder(this, 0);
			}
			else {
				mainColor = enemyColor;
//				inner.setBorder(new LineBorder(enemyColor));
//				inner.setOpaque(false);
				// TODO: find good icon
				icon = IconTextRenderer.getComponent(ActionLibrary.iconForId(2246), defaultLabel, true, false, true);
//				MapPanel.this.setComponentZOrder(this, 5);
			}
			inner.setBorder(new LineBorder(mainColor));
			inner.setOpaque(true);
			inner.add(icon);
			validate();
		}

		@Override
		public int getX() {
			return translateX(this.x) - (getSize().width / 2);
		}


		@Override
		public int getY() {
			return translateY(this.y) - (getSize().height / 2);
		}

		@Override
		public Rectangle getBounds() {
			return new Rectangle(getX(), getY(), getWidth(), getHeight());
		}

		private boolean isSelected() {
			return this.cbtId == selection;
		}

		@Override
		protected void paintComponent(Graphics g) {
			Rectangle bounds = getBounds();
			if (isSelected()) {
				g.setColor(selectedBackground);
				g.fillRect(0, 0, bounds.width, bounds.height);
			}
//			g.setColor(mainColor);
//			int xCenter = bounds.width / 2;
//			int yCenter = bounds.height / 2;
//			int radius = bounds.width / 3;
//			g.drawOval(xCenter - radius, yCenter - radius, radius * 2, radius * 2);
//			super.paintComponent(g);
		}

//		@Override
//		public Color getBackground() {
//			if (isSelected()) {
//				return selectedBackground;
//			}
//			else {
//				return normalBackground;
//			}
//		}

		@Override
		public Border getBorder() {
			if (isSelected()) {
				return selectionBorder;
			}
			else {
				return null;
			}
		}

		private class FacingAngleIndicator extends JComponent {
			@Override
			public void paintComponent(Graphics graph) {
				Graphics2D g = (Graphics2D) graph;
				AffineTransform origTrans = g.getTransform();
				AffineTransform newTrans = new AffineTransform(origTrans);
				Rectangle bounds = getBounds();
				newTrans.translate(bounds.width / 2.0, bounds.height / 2.0);
				newTrans.rotate(-1.0 * facing);
				g.setTransform(newTrans);
				g.setColor(mainColor);
//				g.setColor(new Color(255, 0, 0));
				int sizeBasis = Math.min(bounds.width, bounds.height);
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
				Polygon poly = new Polygon(
						new int[]{0, (int) (sizeBasis / -3.6), 0, (int) (sizeBasis / 3.6)},
						new int[]{(int) (sizeBasis / 2.2), (int) (sizeBasis / -2.8), (int) (sizeBasis / -5.0), (int) (sizeBasis / -2.8)},
						4);
				g.fillPolygon(poly);
				g.setColor(mainColor.darker().darker());
				g.drawPolygon(poly);
//				g.setColor(new Color(0, 255, 0));
//				g.fillRect(sizeBasis / -8, sizeBasis / -2, sizeBasis / 8, sizeBasis / 2);
				g.setTransform(origTrans);
			}

			@Override
			public Border getBorder() {
				return null;
//				return new LineBorder(Color.BLACK, 1);
			}
		}
	}


}
