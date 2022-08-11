package gg.xp.xivsupport.custompartyoverlay.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.custompartyoverlay.CustomOverlayComponentSpec;
import gg.xp.xivsupport.custompartyoverlay.CustomPartyOverlay;
import gg.xp.xivsupport.custompartyoverlay.CustomPartyOverlayComponentFactory;
import gg.xp.xivsupport.custompartyoverlay.CustomPartyOverlayComponentType;
import gg.xp.xivsupport.custompartyoverlay.RefreshablePartyListComponent;
import gg.xp.xivsupport.events.actionresolution.SequenceIdTracker;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.gui.NoCellEditor;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.EditMode;
import gg.xp.xivsupport.gui.tables.StandardColumns;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.ManaPoints;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.models.XivStatusEffect;
import gg.xp.xivsupport.models.XivWorld;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingSpinner;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.CustomJsonListSetting;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@ScanMe
public class CustomPartyConfig implements PluginTab {

	private static final Logger log = LoggerFactory.getLogger(CustomPartyConfig.class);

	public final CustomPartyOverlay overlay;
	private final CustomJsonListSetting<CustomOverlayComponentSpec> elementsSetting;
	private final XivPlayerCharacter dummyCharacter = new XivPlayerCharacter(
			0x12345678,
			"Player Name",
			Job.SGE,
			XivWorld.of(),
			true,
			1,
			new HitPoints(12345, 45678),
			ManaPoints.of(4444, 10_000),
			new Position(100, 100, 0, 0),
			0, 0,
			1,
			90,
			0,
			8000
	);
	private final Map<CustomPartyOverlayComponentType, RefreshablePartyListComponent> componentCache = new ConcurrentHashMap<>();
	private final Map<Component, CustomOverlayComponentSpec> componentToSpecMapping = new ConcurrentHashMap<>();
	private final CustomPartyOverlayComponentFactory factory;
	private ComponentDragPanel dragArea;
	private @Nullable Component selection;

	public CustomPartyConfig(CustomPartyOverlay overlay, XivState state, SequenceIdTracker sqid, StandardColumns cols) {
		this.overlay = overlay;
		elementsSetting = overlay.getElements();
		// Set up fake data
		XivCombatant enemy = new XivCombatant(0x4001_1001, "Enemy");
		factory = new CustomPartyOverlayComponentFactory(new StatusEffectRepository(state, sqid) {
			@Override
			public List<BuffApplied> statusesOnTarget(XivEntity entity) {
				BuffApplied vuln = new BuffApplied(new XivStatusEffect(0x584), 30.0, enemy, dummyCharacter, 16) {
					@Override
					public Duration getEstimatedRemainingDuration() {
						return Duration.ofSeconds(15);
					}
				};
				return new ArrayList<>(Collections.nCopies(45, vuln));
			}
		}, cols, new ActiveCastRepository() {
			@Override
			public @Nullable CastTracker getCastFor(XivCombatant cbt) {
				return new CastTracker(new AbilityCastStart(
						new XivAbility(0x4095), dummyCharacter, enemy, 2.5)
				) {
					@Override
					public Duration getElapsedDuration() {
						return Duration.ofSeconds(1);
					}
				};
			}
		}, sqid);
	}

	@Override
	public String getTabName() {
		return "Custom Party Overlay";
	}

	@Override
	public Component getTabContents() {
		JTabbedPane pane = new JTabbedPane();

		{
			JPanel mainOverlayPanel = new JPanel();
			mainOverlayPanel.setLayout(new BorderLayout());

			{
				JPanel panel = new JPanel(new WrapLayout());
				BooleanSetting enabled = overlay.getEnabled();

				panel.add(new BooleanSettingGui(enabled, "Enable Overlay", true).getComponent());
				panel.add(new IntSettingSpinner(overlay.getYOffset(), "Y Offset Between Party Members").getComponent());
				overlay.getYOffset().addListener(this::resetComponents);
				// TODO: put these back when something new is added so there's actually a reason
//				JButton addButton = new JButton("Add Component");
//				JButton removeButton = new JButton("Remove Component");
				JButton resetButton = new JButton("Reset to Default");
				resetButton.addActionListener(l -> {
					overlay.resetToDefault();
				});
//				panel.add(addButton);
//				panel.add(removeButton);
				panel.add(resetButton);

				mainOverlayPanel.add(panel, BorderLayout.NORTH);
			}
			{
				this.dragArea = new ComponentDragPanel();
				elementsSetting.addListener(this::resetComponents);
				TableWithFilterAndDetails<CustomOverlayComponentSpec, Object> table = TableWithFilterAndDetails.builder("Components", elementsSetting::getItems)
						.addMainColumn(new CustomColumn<>("En", item -> item.enabled, c -> {
							c.setMinWidth(22);
							c.setMaxWidth(22);
							c.setCellRenderer(StandardColumns.checkboxRenderer);
							c.setCellEditor(new StandardColumns.CustomCheckboxEditor<>(safeEdit((item, value) -> item.enabled = value)));
						}))
						.addMainColumn(new CustomColumn<>("Type", item -> item.componentType.getFriendlyName(), c -> {
							c.setCellEditor(new NoCellEditor());
						}))
						.addMainColumn(new CustomColumn<>("X", item -> item.x, c -> c.setCellEditor(StandardColumns.intEditorNonNull(safeEdit((item, value) -> item.x = value)))))
						.addMainColumn(new CustomColumn<>("Y", item -> item.y, c -> c.setCellEditor(StandardColumns.intEditorNonNull(safeEdit((item, value) -> item.y = value)))))
						.addMainColumn(new CustomColumn<>("Width", item -> item.width, c -> c.setCellEditor(StandardColumns.intEditorNonNull(safeEdit((item, value) -> item.width = value)))))
						.addMainColumn(new CustomColumn<>("Height", item -> item.height, c -> c.setCellEditor(StandardColumns.intEditorNonNull(safeEdit((item, value) -> item.width = value)))))
						.setSelectionEquivalence((a, b) -> a.componentType == b.componentType)
						.build();
				table.setEditMode(EditMode.AUTO);
				elementsSetting.addListener(table::signalNewData);
				JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, dragArea, new JScrollPane(table.getMainTable()));
				SwingUtilities.invokeLater(() -> {
					splitPane.setDividerLocation(0.8);
					splitPane.setResizeWeight(0.8);
					table.signalNewData();
				});
				mainOverlayPanel.add(splitPane);
			}

			pane.add("Main", mainOverlayPanel);
			resetComponents();
		}

		return pane;
	}

	private void resetComponents() {
		SwingUtilities.invokeLater(() -> {
			dragArea.removeAll();
			componentToSpecMapping.clear();
			List<CustomOverlayComponentSpec> items = elementsSetting.getItems();
			for (CustomOverlayComponentSpec item : items) {
				RefreshablePartyListComponent rplc = getComponentFor(item);
				if (rplc == null) {
					continue;
				}
				Component component = rplc.getComponent();
				Point adjustedLoc = dragArea.logicalToScreen(new Point(item.x, item.y));
				component.setBounds(adjustedLoc.x, adjustedLoc.y, item.width, item.height);
				rplc.refresh(dummyCharacter);
				dragArea.add(component);
				componentToSpecMapping.put(component, item);
			}
			SwingUtilities.invokeLater(() -> {
				dragArea.revalidate();
				dragArea.repaint();
			});
		});
	}

	private RefreshablePartyListComponent getComponentFor(CustomOverlayComponentSpec item) {
		// TODO: kind of hacky
		if (item.enabled) {
			return componentCache.computeIfAbsent(item.componentType, (type) -> factory.makeComponent(item));
		}
		else {
			return null;
		}
	}

	private <X> BiConsumer<CustomOverlayComponentSpec, X> safeEdit(BiConsumer<CustomOverlayComponentSpec, X> editFunc) {
		return (t, u) -> {
			editFunc.accept(t, u);
			elementsSetting.commit();
		};
	}

	private void commitDrag() {
		Component sel = selection;
		if (sel == null) {
			return;
		}
		CustomOverlayComponentSpec spec = componentToSpecMapping.get(sel);
		Point locScreen = sel.getLocation();
		Point loc = dragArea.screenToLogical(locScreen);
		Dimension size = sel.getSize();
		spec.x = loc.x;
		spec.y = loc.y;
		spec.width = size.width;
		spec.height = size.height;
		elementsSetting.commit();
	}

	private enum DragType {
		MOVE,
		HORIZ,
		VERT,
		DIAG,
		NONE
	}

	private class ComponentDragPanel extends JPanel implements MouseMotionListener, MouseListener {

		private volatile Point dragPoint;
		private DragType dragType;

		ComponentDragPanel() {
			super(null);
			addMouseListener(this);
			addMouseMotionListener(this);
		}

		Point logicalToScreen(Point logical) {
			return new Point(logical.x + 100, logical.y + 100);
		}

		Point screenToLogical(Point screen) {
			return new Point(screen.x - 100, screen.y - 100);
		}

		@Override
		protected void paintComponent(Graphics g) {
			g.setColor(new Color(25, 25, 25));
			Rectangle bounds = getBounds();
			g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
			g.setColor(new Color(128, 128, 128));
			g.drawLine(0, 100, getWidth(), 100);
			int lowerLine = overlay.getYOffset().get() + 100;
			g.drawLine(0, lowerLine, getWidth(), lowerLine);
			g.drawLine(100, 0, 100, getHeight());
			g.drawString("This view shows a preview of a party frame. Note that it does not reflect your overlay scaling, and only shows a single frame.", 105, 20);
			g.drawString("Place components below this line.", 105, 95);
			g.drawString("To expand the vertical space for each party frame, use the 'Y Offset' setting above.", 105, lowerLine + 20);
//			super.paintComponent(g);
		}

		@Override
		protected void paintChildren(Graphics gg) {
			Graphics2D g = (Graphics2D) gg;
			super.paintChildren(g);
			Component sel = selection;
			if (sel != null) {
				g.setColor(new Color(255, 100, 0));
				g.setStroke(new BasicStroke(4));
				g.drawRect(sel.getX(), sel.getY(), sel.getWidth(), sel.getHeight());
			}
		}

		private @Nullable Component looseComponentAt(Point point) {
			Component exactComp = componentAtNotSelf(point);
			// No hard match, try loose match
			if (exactComp == null) {
				int[] x = {5, 5, -5, -5};
				int[] y = {5, -5, -5, 5};
				for (int i = 0; i < 4; i++) {
					Point p = new Point(point);
					p.translate(x[i], y[i]);
					Component component = componentAtNotSelf(p);
					if (component != null) {
						return component;
					}
				}
				return null;
			}
			else {
				return exactComp;
			}
		}

		private @Nullable Component componentAtNotSelf(Point point) {
			Component comp = getComponentAt(point);
			if (comp == this) {
				return null;
			}
			else {
				return comp;
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			Component componentAt = looseComponentAt(e.getPoint());
			if (componentAt != null) {
				selection = componentAt;
				log.info("Clicked Component {}", componentAt);
				CustomOverlayComponentSpec spec = componentToSpecMapping.get(componentAt);
				log.info("Selected spec type: {}", spec.componentType);
			}
			else {
				selection = null;
			}
			repaint();
		}

		@Override
		public void mousePressed(MouseEvent e) {
			dragPoint = e.getLocationOnScreen();
			dragType = getDragType(e.getPoint());
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			commitDrag();
		}

		@Override
		public void mouseEntered(MouseEvent e) {

		}

		@Override
		public void mouseExited(MouseEvent e) {

		}

		@Override
		public void mouseDragged(MouseEvent e) {
			Component sel = selection;
			if (sel == null) {
				return;
			}
			Point curPoint = e.getLocationOnScreen();
			int deltaX = curPoint.x - dragPoint.x;
			int deltaY = curPoint.y - dragPoint.y;
			dragPoint = curPoint;
			Rectangle bounds = sel.getBounds();
			switch (dragType) {
				case MOVE -> bounds.translate(deltaX, deltaY);
				case HORIZ -> bounds.setSize(bounds.width + deltaX, bounds.height);
				case VERT -> bounds.setSize(bounds.width, bounds.height + deltaY);
				case DIAG -> bounds.setSize(bounds.width + deltaX, bounds.height + deltaY);
				case NONE -> {
					// Nothing
				}
			}
			sel.setBounds(bounds);
			repaint();
//			log.info("Drag: {}, {}, {} -> {}", e.getPoint().getX(), e.getPoint().getY(), dragType, sel);
		}

		private DragType getDragType(Point point) {
			Component sel = selection;
			if (sel != null) {
				if (sel.getBounds().contains(point)) {
					return DragType.MOVE;
				}
				else {
					Rectangle bounds = sel.getBounds();
					Rectangle rightRect = new Rectangle(bounds.x + bounds.width, bounds.y, 10, bounds.height);
					Rectangle southRect = new Rectangle(bounds.x, bounds.y + bounds.height, bounds.width, 10);
					Rectangle cornerRect = new Rectangle(bounds.x + bounds.width, bounds.y + bounds.height, 10, 10);
					if (rightRect.contains(point)) {
						return DragType.HORIZ;
					}
					else if (southRect.contains(point)) {
						return DragType.VERT;
					}
					else if (cornerRect.contains(point)) {
						return DragType.DIAG;
					}
				}
			}
			return DragType.NONE;
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			DragType dragType = getDragType(e.getPoint());
			int cursor = switch (dragType) {
				case MOVE -> Cursor.MOVE_CURSOR;
				case HORIZ -> Cursor.E_RESIZE_CURSOR;
				case VERT -> Cursor.S_RESIZE_CURSOR;
				case DIAG -> Cursor.SE_RESIZE_CURSOR;
				case NONE -> Cursor.DEFAULT_CURSOR;
			};
			setCursor(Cursor.getPredefinedCursor(cursor));
		}
	}
}
