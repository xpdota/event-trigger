package gg.xp.xivsupport.gui.map;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
import gg.xp.xivsupport.events.state.floormarkers.FloorMarker;
import gg.xp.xivsupport.events.triggers.jobs.gui.CastBarComponent;
import gg.xp.xivsupport.gui.map.omen.ActionOmenInfo;
import gg.xp.xivsupport.gui.map.omen.OmenDisplayMode;
import gg.xp.xivsupport.gui.map.omen.OmenInstance;
import gg.xp.xivsupport.gui.map.omen.OmenLocationType;
import gg.xp.xivsupport.gui.map.omen.OmenShape;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.gui.tables.renderers.HpBar;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.tables.renderers.OverlapLayout;
import gg.xp.xivsupport.gui.tables.renderers.RenderUtils;
import gg.xp.xivsupport.gui.tables.renderers.ScaledImageComponent;
import gg.xp.xivsupport.models.CombatantType;
import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.settings.EnumSetting;
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
import java.awt.geom.Arc2D;
import java.io.Serial;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class MapPanel extends JPanel implements MouseMotionListener, MouseListener, MouseWheelListener {

	private static final Logger log = LoggerFactory.getLogger(MapPanel.class);
	@Serial
	private static final long serialVersionUID = 6804697839463860552L;

	private final Map<Long, EntityDoohickey> things = new HashMap<>();
	private final RefreshLoop<MapPanel> refresher;
	private final MapDataController mdc;
	private final EnumSetting<NameDisplayMode> nameDisp;
	private final EnumSetting<OmenDisplayMode> omenDisp;
	private double zoomFactor = 1;
	private volatile int curXpan;
	private volatile int curYpan;
	private volatile Point dragPoint;
	private XivMap map = XivMap.UNKNOWN;
	private Image backgroundImage;
	// -1 indicates no selection
	private volatile long selection = -1;
	private Consumer<@Nullable XivCombatant> selectionCallback = l -> {
	};
	private Map<FloorMarker, FloorMarkerDoohickey> markers;
	private boolean needZorderCheck;

	private static final Color enemyColor = new Color(145, 0, 0);
	private static final Color fakeEnemyColor = new Color(170, 120, 0);
	private static final Color otherColor = new Color(128, 128, 128);
	private static final Color otherPlayerColor = new Color(82, 204, 82);
	private static final Color partyMemberColor = new Color(104, 120, 222);
	private static final Color localPcColor = new Color(150, 199, 255);
	private static final Color playerHitboxColor = new Color(150, 150, 255, 120);
	private static final Color npcHitboxColor = new Color(250, 30, 30, 90);

	public MapPanel(MapDataController mdc, MapDisplayConfig mapDisplayConfig) {
		this.mdc = mdc;
		omenDisp = mapDisplayConfig.getOmenDisplayMode();
		nameDisp = mapDisplayConfig.getNameDisplayMode();

		setLayout(null);
		setBackground(new Color(168, 153, 114));
		refresher = new RefreshLoop<>("MapRefresh", this, map -> {
			SwingUtilities.invokeLater(() -> {
				if (map.isShowing()) {
					requestRefresh();
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

	@Override
	protected void paintChildren(Graphics g) {
		int count = getComponentCount();
		Rectangle bounds = new Rectangle();
		for (int i = 0; i < count; i++) {
			Component comp = getComponent(i);
			if (comp instanceof EntityDoohickey ed) {
				Graphics subG = g.create();
//				bounds = comp.getBounds(bounds);
//				subG.translate(bounds.x, bounds.y);
				ed.paintUnder(subG);
				subG.dispose();
			}
		}
		super.paintChildren(g);
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
		if (map != null) {
			zoomFactor = map.getScaleFactor();
		}
		else {
			zoomFactor = 1;
		}
		triggerRefresh();
	}

	private void triggerRefresh() {
		refresher.refreshNow();
	}

	private volatile List<XivCombatant> combatants = Collections.emptyList();

	public void setCombatants(List<XivCombatant> combatants) {
		this.combatants = new ArrayList<>(combatants);
		requestRefresh();
	}

	private volatile boolean refreshPending;
	private void requestRefresh() {
		if (refreshPending) {
			return;
		}
		refreshPending = true;
		SwingUtilities.invokeLater(this::refresh);
	}

	private void refresh() {
		refreshPending = false;
//		log.info("Map refresh");
		List<XivCombatant> combatants = this.combatants;
		XivMap mapNow = mdc.getMap();
		if (!Objects.equals(map, mapNow)) {
			map = mapNow;
			setNewBackgroundImage(mapNow);
			resetPanAndZoom();
		}
		if (markers == null) {
			markers = new EnumMap<>(FloorMarker.class);
			for (FloorMarker value : FloorMarker.values()) {
				FloorMarkerDoohickey component = new FloorMarkerDoohickey(value);
				component.setVisible(false);
				markers.put(value, component);
				add(component);
				needZorderCheck = true;
			}
		}
		markers.forEach((marker, component) -> component.reposition(mdc.getFloorMarkers().get(marker)));
		combatants.stream()
				.filter(cbt -> {
					// Further filtering is no longer necessary here since the table pre-filters for us.
					// But we can't exactly display something with no position.
					return cbt.getPos() != null;
				})
				.forEach(cbt -> {
					long id = cbt.getId();
					if (cbt.getPos() == null) {
						return;
					}
					@Nullable CastTracker cast = mdc.getCastFor(cbt);
					// Create if it doesn't already exist
					EntityDoohickey pdh = things.computeIfAbsent(id, (unused) -> createNew(cbt));
					// Update with latest info
					pdh.update(cbt, cast);
				});

		Set<Long> allKeys = things.keySet();
		List<Long> keysToRemove = allKeys.stream().filter(v -> combatants.stream().noneMatch(c -> c.getId() == v)).toList();
		keysToRemove.forEach(k -> {
			EntityDoohickey toRemove = things.remove(k);
			toRemove.setVisible(false);
			remove(toRemove);
		});
		if (needZorderCheck) {
			fixZorder();
		}
		revalidate();
		repaint();
	}

	private EntityDoohickey createNew(XivCombatant cbt) {
		EntityDoohickey player = new EntityDoohickey(cbt);
		add(player);
		needZorderCheck = true;
		return player;
	}

	// Translate in-game X to map coordinates

	/**
	 * @param originalX in-game X coordinate
	 * @return equivalent map coordinates on the current map.
	 */
	private double translateXmap(double originalX) {
		// Already divided by 100
		double c = map.getScaleFactor();
		return (originalX + map.getOffsetX()) * c;
	}

	/**
	 * @param originalY in-game Y coordinate
	 * @return equivalent map coordinates on the current map.
	 */
	private double translateYmap(double originalY) {
		double c = map.getScaleFactor();
		return (originalY + map.getOffsetY()) * c;
	}

	private double translateDistMap(double originalDist) {
		double c = map.getScaleFactor();
		return originalDist * c;
	}

	/**
	 * @param originalX map X coordinate
	 * @return equivalent on-screen coordinate
	 */
	private int translateXscrn(double originalX) {
		return (int) ((originalX * zoomFactor) + curXpan + getWidth() / 2.0);
	}

	/**
	 * @param originalY map Y coordinate
	 * @return equivalent on-screen coordinate
	 */
	private int translateYscrn(double originalY) {
		return (int) ((originalY * zoomFactor) + curYpan + getHeight() / 2.0);
	}

	/**
	 * Translate a raw distance by scaling it appropriately
	 *
	 * @param originalDist map distance
	 * @return equivalent on-screen distance
	 */
	private double translateDistScrn(double originalDist) {
		return originalDist * zoomFactor;
	}

	/**
	 * @param originalX in-game X coordinate
	 * @return equivalent screen coordinate
	 */
	private int translateX(double originalX) {
		return translateXscrn(translateXmap(originalX));
	}

	/**
	 * @param originalY in-game Y coordinate
	 * @return equivalent screen coordinate
	 */
	private int translateY(double originalY) {
		return translateYscrn(translateYmap(originalY));
	}

	private double translateDist(double originalDist) {
		return translateDistScrn(translateDistMap(originalDist));
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
		// Ignored
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON2) {
			resetPanAndZoom();
		}
		else {
			if (e.getComponent() instanceof EntityDoohickey pd) {
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
			log.debug("New map selection: {} -> {}",
					this.selection == -1 ? "none" : String.format("0x%X", this.selection),
					newSelection == -1 ? "none" : String.format("0x%X", newSelection));
			this.selection = newSelection;
			fixZorder();
		}
	}

	private void fixZorder() {
		if (SwingUtilities.isEventDispatchThread()) {
			Component[] components = getComponents();
			removeAll();
			Arrays.stream(components).sorted(Comparator.comparing(comp -> {
				if (comp instanceof EntityDoohickey pd) {
					if (pd.isSelected()) {
						return 0;
					}
					else {
						return 1;
					}
				}
				else {
					return 5;
				}
			})).forEach(this::add);
			repaint();
		}
		else {
			SwingUtilities.invokeLater(this::fixZorder);
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

	// This seems to be pointless, but put back if it breaks something.
//	@Override
//	protected void paintChildren(Graphics g) {
//		super.paintChildren(g);
//		things.values().forEach(v -> {
//			if (v.isSelected()) {
////				v.repaint();
////				v.paintComponent(g);
////				paintComponents();
////				v.paint(g);
//			}
//		});
//	}

	private final class FloorMarkerDoohickey extends JPanel {

		private static final int SIZE = 50;
		private double x;
		private double y;

		private FloorMarkerDoohickey(FloorMarker marker) {
			super(null);
			setOpaque(false);
			ScaledImageComponent iconPre = IconTextRenderer.getIconOnly(marker);
			if (iconPre != null) {
				Component icon = iconPre.cloneThis().withNewSize(SIZE);
				add(icon);
				icon.setBounds(0, 0, SIZE, SIZE);
			}
			else {
				log.warn("Could not load marker icon for {}", marker);
			}
		}

		private void reposition(@Nullable Position position) {
			if (position == null) {
				setVisible(false);
			}
			else {
				x = position.x();
				y = position.y();
				setBounds(getBounds());
				setVisible(true);
			}
		}

		@Override
		public int getX() {
			return translateX(this.x) - getSize().width / 2;
		}


		@Override
		public int getY() {
			return translateY(this.y) - getSize().height / 2;
		}

		@Override
		public Rectangle getBounds() {
			return new Rectangle(getX(), getY(), SIZE, SIZE);
		}
	}

	// TODO: name....
	private class EntityDoohickey extends JPanel {

		private static final Border selectionBorder = new LineBorder(Color.CYAN, 2);
		private static final Color selectedBackground = new Color(192, 255, 255, 175);
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
		private @Nullable CastTracker castData;
		private Position pos = Position.of2d(-10_000, -10_000);

		public EntityDoohickey(XivCombatant cbt) {
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
			inner.setBounds(new Rectangle(center - innerW * 2, center - innerH / 2, innerW, innerH));
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
		long oldUnresolved = -2;

		// TODO: add debuffs or something to this
		public void update(XivCombatant cbt, @Nullable CastTracker castData) {
			RenderUtils.setTooltip(this, formatTooltip(cbt));
			setBounds(getBounds());
			this.castData = castData;
			Position pos = cbt.getPos();
			if (pos != null) {
				this.x = pos.getX();
				this.y = pos.getY();
				this.facing = pos.getHeading();
				this.pos = pos;
			}
			if (cbt instanceof XivPlayerCharacter pc) {
				Job newJob = pc.getJob();
				if (newJob != oldJob) {
					if (icon != null) {
						inner.remove(icon);
					}
					formatComponent(cbt);
				}
				oldJob = newJob;
				// TODO: add "hide names" setting
				switch (nameDisp.get()) {
					case FULL -> {
						nameLabel.setText(cbt.getName());
					}
					case JOB -> {
						nameLabel.setText(pc.getJob().name());
					}
					case HIDE -> {
						nameLabel.setText("");
					}
				}
			}
			else {
				nameLabel.setText(cbt.getName());
			}
			if (castData == null || castData.getEstimatedTimeSinceExpiry().toMillis() > 5000) {
				castBar.setData(null);
			}
			else {
				castBar.setData(castData);
			}

			HitPoints hp = cbt.getHp();
			long hpCurrent = hp == null ? -1 : hp.current();
			long hpMax = hp == null ? -1 : hp.max();
			long unresolved = mdc.unresolvedDamage(cbt);
			// Ignore updates where nothing changed
			if (hpCurrent == oldHpCurrent && hpMax == oldHpMax && unresolved == oldUnresolved) {
				return;
			}
			oldHpCurrent = hpCurrent;
			oldHpMax = hpMax;
			oldUnresolved = unresolved;
			if (hp == null) {
				hpBar.setVisible(false);
			}
			else if (cbt.getType() == CombatantType.FAKE || hp.max() == 1 || hp.current() < 0) {
				hpBar.setVisible(false);
			}
			else {
				hpBar.setVisible(true);
				hpBar.setData(cbt, -1 * unresolved);
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
				else if (mdc.getPartyList().contains(cbt)) {
					mainColor = partyMemberColor;
//					inner.setBorder(new LineBorder(partyMemberColor));
//					inner.setBackground(partyMemberColor);
				}
				else {
					mainColor = otherPlayerColor;
//					inner.setBorder(new LineBorder(otherPlayerColor));
//					inner.setBackground(otherPlayerColor);
				}
				icon = IconTextRenderer.getComponent(job, defaultLabel, true, false, true, null);
//				inner.setOpaque(true);
				// TODO: this doesn't work because it hasn't been added to the container yet
//				MapPanel.this.setComponentZOrder(this, 0);
			}
			else {
				icon = null;
				if (cbt.getType() == CombatantType.FAKE) {
					mainColor = fakeEnemyColor;
				}
				else if (cbt.getType() == CombatantType.NPC) {
					mainColor = enemyColor;
//				inner.setBorder(new LineBorder(enemyColor));
//				inner.setOpaque(false);
					// TODO: find good icon
//				icon = IconTextRenderer.getComponent(ActionLibrary.iconForId(2246), defaultLabel, true, false, true);
//				MapPanel.this.setComponentZOrder(this, 5);
				}
				else {
					mainColor = otherColor;
				}
			}
			inner.setBorder(new LineBorder(mainColor));
			inner.setOpaque(true);
			if (icon != null) {
				inner.add(icon);
			}
			validate();
		}

		@Override
		public int getX() {
			return translateX(this.x) - getSize().width / 2;
		}


		@Override
		public int getY() {
			return translateY(this.y) - getSize().height / 2;
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

		public void paintUnder(Graphics g) {
			switch (omenDisp.get()) {
				case NONE -> {
					return;
				}
				case ENEMIES_ONLY -> {
					if (cbt.walkParentChain() instanceof XivPlayerCharacter) {
						return;
					}
				}
				case SELECTED_ONLY -> {
					if (!isSelected()) {
						return;
					}
				}
			}
			drawOmens(g);
			if (cbt.getRadius() > 0 && !cbt.isFake()) {
				drawHitbox((Graphics2D) g);
			}
		}

		private void drawHitbox(Graphics2D g2d) {
			g2d = (Graphics2D) g2d.create();
			try {
				double radius = translateDist(cbt.getRadius());
				double xCenter = translateX(x);
				double yCenter = translateY(y);
				g2d.setColor(cbt.isPc() ? playerHitboxColor : npcHitboxColor);
				g2d.setStroke(new BasicStroke(3));
				g2d.drawOval((int) (xCenter - radius), (int) (yCenter - radius), (int) (radius * 2), (int) (radius * 2));
			}
			finally {
				g2d.dispose();
			}
		}

		private void drawOmens(Graphics g) {
			mdc.getOmens(cbtId).forEach(omen -> drawOmen((Graphics2D) g, omen));
		}

		private void drawOmen(Graphics2D g2d, OmenInstance omen) {
			g2d = (Graphics2D) g2d.create();
			try {
				// Cast type 0 and 1 are uninteresting
				// "100" effect range is just a raidwide, don't bother drawing if it's a circle
				ActionOmenInfo oi = omen.info();
				if (oi.isRaidwide()) {
					// Don't need to draw these
					// Maybe add a setting, I can see these having niche use cases
					return;
				}
//				Position castPos;
				Color fillColor;
				Color outlineColor;
				int alpha;
				Duration td = omen.timeDeltaFrom(mdc.getTime());
				if (td.isNegative()) {
					// casts - start semi transparent
					alpha = 120;
				}
				else {
					// highlight then gradually fade
					alpha = (int) (200.0 - td.toMillis() / 50.0);
//					alpha = (int) (200.0 - td.toMillis() / 10.0);
//					log.info("Since: {}, Alpha: {}", cd.getEstimatedTimeSinceExpiry().toMillis(), alpha);
				}
				// Don't draw ancient stuff
				if (alpha <= 0) {
					return;
				}
				Position omenPos = omen.omenPosition(omenCbt -> MapPanel.this.combatants
						.stream()
						.filter(cbt -> cbt.getId() == omenCbt.getId()).findFirst().orElse(cbt).getPos());
				if (omenPos == null) {
					return;
				}

				if (cbt.walkParentChain() instanceof XivPlayerCharacter) {
					outlineColor = new Color(80, 200, 255, alpha);
				}
				else {
					outlineColor = new Color(255, 110, 0, alpha);
				}
				fillColor = RenderUtils.withAlpha(outlineColor, alpha / 2);
//				log.info("Outline: {}, Fill: {}", outlineColor.getAlpha(), fillColor.getAlpha());
				double xCenter = translateX(omenPos.x());
				double yCenter = translateY(omenPos.y());
				double radius = translateDist(omen.radius());
				double xModif = translateDist(oi.xAxisModifier());
				/*
				From Valarnin:
				2 - Circle AoE, range directly based on `EffectRange` column
				3 - Cone, range is `EffectRange` + actor's hitbox radius, angle depends on Omen
				4 - Rectangle, range is `EffectRange` + actor's hitbox radius, offset is half of `XAxisModifier` column?
				5 - Circle AoE, range is `EffectRange` + actor's hitbox radius
				6 - I think these are circle AoEs with no actual ground-target AoE shown even as they're resolving, e.g. `Twister`. Should use one of the two formulas (including hitbox raidus or excluding), but not sure.
				8 - "wild charge" rectangle, not sure exactly how width is determined, probably also half of `XAxisModifier`?
				10 - Donut AoE, not sure how inner/outer range is calculated
				11 - cross-shaped AoEs? not 100% sure on this one
				12 - Rectangle, range is `EffectRange`, offset is half of `XAxisModifier` column
				13 - Cone, range is `EffectRange`, angle depends on Omen
				 */
				/*
				My further notes:
				#10 - effect range is the outer radius

				#11 - yes, it's cross

				#12 is a rectangle, but sometimes it is centered on the caster, extending <effectRange> forward and back
					Perhaps cast angle/position will help

				#13 seems to be not only cones, but also things like Omega's "Swivel Cannon" in TOP P5,
				which is a half-room cleave but with the angle offset a bit.

				 */
				// TODO: some of these are wrong due to lack of hitbox size info
				AffineTransform transform = g2d.getTransform();
				switch (oi.type().shape()) {
					case CIRCLE -> {
						g2d.setStroke(new BasicStroke(3));
						g2d.setColor(fillColor);
						g2d.fillOval((int) (xCenter - radius), (int) (yCenter - radius), (int) (radius * 2.0), (int) (radius * 2.0));
						g2d.setColor(outlineColor);
						g2d.drawOval((int) (xCenter - radius), (int) (yCenter - radius), (int) (radius * 2.0), (int) (radius * 2.0));
					}
					case DONUT -> {
						g2d.setStroke(new BasicStroke(3));
//						g2d.setColor(fillColor);
//						g2d.fillOval((int) (xCenter - radius), (int) (yCenter - radius), (int) (radius * 2.0), (int) (radius * 2.0));
						g2d.setColor(outlineColor);
						g2d.drawOval((int) (xCenter - radius), (int) (yCenter - radius), (int) (radius * 2.0), (int) (radius * 2.0));
					}
					case RECTANGLE -> {
						g2d.setStroke(new BasicStroke(3));
						transform.translate(xCenter, yCenter);
						transform.rotate(-omenPos.getHeading());
						g2d.setTransform(transform);
						g2d.setColor(fillColor);
						g2d.fillRect((int) -(xModif / 2.0), 0, (int) xModif, (int) radius);
						g2d.setColor(outlineColor);
						g2d.drawRect((int) -(xModif / 2.0), 0, (int) xModif, (int) radius);
					}
					case RECTANGLE_CENTERED -> {
						g2d.setStroke(new BasicStroke(3));
						transform.translate(xCenter, yCenter);
						transform.rotate(-omenPos.getHeading());
						g2d.setTransform(transform);
						g2d.setColor(fillColor);
						g2d.fillRect((int) -(xModif / 2.0), (int) -radius, (int) xModif, (int) (2 * radius));
						g2d.setColor(outlineColor);
						g2d.drawRect((int) -(xModif / 2.0), (int) -radius, (int) xModif, (int) (2 * radius));
					}
					case CROSS -> {
						g2d.setStroke(new BasicStroke(3));
						transform.translate(xCenter, yCenter);
						transform.rotate(-omenPos.getHeading());
						g2d.setTransform(transform);
						g2d.setColor(fillColor);
						g2d.fillRect((int) -(xModif / 2.0), (int) -radius, (int) xModif, (int) (2 * radius));
						g2d.fillRect((int) -radius, (int) -(xModif / 2.0), (int) (2 * radius), (int) xModif);
						g2d.setColor(outlineColor);
						g2d.drawRect((int) -(xModif / 2.0), (int) -radius, (int) xModif, (int) (2 * radius));
						g2d.drawRect((int) -radius, (int) -(xModif / 2.0), (int) (2 * radius), (int) xModif);
					}
					case CONE -> {
						g2d.setStroke(new BasicStroke(3));
						transform.translate(xCenter, yCenter);
						transform.rotate(-omenPos.getHeading() + Math.PI);
						g2d.setTransform(transform);
						g2d.setColor(fillColor);
						int angleDegrees = oi.coneAngle();
						// Arc2D uses the east side as "zero" and counts CCW
						Arc2D.Double arc = new Arc2D.Double(-radius, -radius, 2 * radius, 2 * radius, 90 - angleDegrees / 2.0f, angleDegrees, Arc2D.PIE);
						g2d.setColor(fillColor);
						g2d.fill(arc);
						g2d.setColor(outlineColor);
						g2d.draw(arc);
					}
				}
			} finally {
				g2d.dispose();
			}
		}

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
