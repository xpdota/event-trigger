package gg.xp.xivsupport.gui.tabs;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.actlines.events.HasEffects;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.events.misc.RawEventStorage;
import gg.xp.xivsupport.events.misc.pulls.PullTracker;
import gg.xp.xivsupport.groovy.GroovyManager;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.GlobalGuiOptions;
import gg.xp.xivsupport.gui.tables.RightClickOptionRepo;
import gg.xp.xivsupport.gui.tables.StandardColumns;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.AbilityResolutionFilter;
import gg.xp.xivsupport.gui.tables.filters.EventAbilityOrBuffFilter;
import gg.xp.xivsupport.gui.tables.filters.EventClassFilterFilter;
import gg.xp.xivsupport.gui.tables.filters.EventEntityFilter;
import gg.xp.xivsupport.gui.tables.filters.GroovyFilter;
import gg.xp.xivsupport.gui.tables.filters.PullNumberFilter;
import gg.xp.xivsupport.gui.tables.filters.SystemEventFilter;
import gg.xp.xivsupport.gui.tables.renderers.AbilityEffectListRenderer;
import gg.xp.xivsupport.gui.tables.renderers.ActionAndStatusRenderer;
import gg.xp.xivsupport.gui.tables.renderers.NameJobRenderer;
import gg.xp.xivsupport.gui.tables.timedisplay.TimeDisplayController;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.replay.ReplayController;
import gg.xp.xivsupport.replay.gui.ReplayAdvancePseudoFilter;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.MutablePicoContainer;

import javax.swing.*;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@ScanMe
public class EventsTabFactory {

	private final MutablePicoContainer container;
	private final GlobalGuiOptions globalGuiOpts;
	private final RightClickOptionRepo rightClicks;
	private final EventMaster master;
	private final ReplayController replay;

	public EventsTabFactory(MutablePicoContainer container, GlobalGuiOptions globalGuiOpts, RightClickOptionRepo rightClicks, EventMaster master) {
		this.container = container;
		this.globalGuiOpts = globalGuiOpts;
		this.rightClicks = rightClicks;
		this.master = master;
		replay = container.getComponent(ReplayController.class);
	}

	public Component getEventsTab() {
		// TODO: jump to parent button
		// Main table
		RawEventStorage rawStorage = container.getComponent(RawEventStorage.class);
		PullTracker pulls = container.getComponent(PullTracker.class);
		ActionAndStatusRenderer asRenderer = ActionAndStatusRenderer.full();
		NameJobRenderer nameJobRenderer = new NameJobRenderer();
		BooleanSetting displayIdsSetting = globalGuiOpts.displayIds();
		displayIdsSetting.addAndRunListener(() -> asRenderer.setShowId(displayIdsSetting.get()));
		displayIdsSetting.addAndRunListener(() -> nameJobRenderer.setShowId(displayIdsSetting.get()));
		TimeDisplayController tdc = new TimeDisplayController();
		TableWithFilterAndDetails<Event, Map.Entry<Field, Object>> table = TableWithFilterAndDetails.builder("Events", rawStorage::getEvents,
						currentEvent -> {
							if (currentEvent == null) {
								return Collections.emptyList();
							}
							else {
								return currentEvent.dumpFields()
										.entrySet()
										.stream()
										.filter(e -> !"serialVersionUID".equals(e.getKey().getName()))
										.collect(Collectors.toList());
							}
						})
				.addMainColumn(tdc.getColumnDef())
				.addMainColumn(new CustomColumn<>("Type", e -> e.getClass().getSimpleName()))
				.addMainColumn(new CustomColumn<>("Source", e -> e instanceof HasSourceEntity ? ((HasSourceEntity) e).getSource() : null, c -> c.setCellRenderer(nameJobRenderer)))
				.addMainColumn(new CustomColumn<>("Target", e -> e instanceof HasTargetEntity ? ((HasTargetEntity) e).getTarget() : null, c -> c.setCellRenderer(nameJobRenderer)))
				.addMainColumn(new CustomColumn<>("Buff/Ability", Function.identity(), c -> {
					c.setCellRenderer(asRenderer);
				}))
				.addMainColumn(new CustomColumn<>("Effects", e -> {
					if (e instanceof HasEffects event) {
						return event.getEffects();
					}
					return null;
				}, c -> c.setCellRenderer(new AbilityEffectListRenderer())))
				.addMainColumn(new CustomColumn<>("Parent", e -> {
					Event parent = e.getParent();
					return parent == null ? null : parent.getClass().getSimpleName();
				}))
				.addDetailsColumn(StandardColumns.fieldName)
				.addDetailsColumn(StandardColumns.fieldValue)
				.addDetailsColumn(StandardColumns.identity)
				.addDetailsColumn(StandardColumns.fieldType)
				.addDetailsColumn(StandardColumns.fieldDeclaredIn)
				.withRightClickRepo(rightClicks)
				.addFilter(SystemEventFilter::new)
				.addFilter(EventClassFilterFilter::new)
				.addFilter(AbilityResolutionFilter::new)
				.addFilter(EventEntityFilter::eventSourceFilter)
				.addFilter(EventEntityFilter::eventTargetFilter)
				.addFilter(EventAbilityOrBuffFilter::new)
//				.addFilter(FreeformEventFilter::new)
				.addFilter(GroovyFilter.forClass(Event.class, container.getComponent(GroovyManager.class)))
				.addFilter(r -> {
					PullNumberFilter pullNumberFilter = new PullNumberFilter(pulls, r);
					if (container.getComponent(PullNumberFilter.class) == null) {
						container.addComponent(pullNumberFilter);
					}
					return pullNumberFilter;
				})
				.addWidget(unused -> new BooleanSettingGui(displayIdsSetting, "Show IDs", true).getComponent())
				.addWidget(replayNextPseudoFilter(Event.class))
				.addWidget(tdc::configureWidget)
				.addWidget(t -> {
					JButton button = new JButton("New Window");
					button.addActionListener(l -> newWindow(button));
					return button;
				})
				.setAppendOrPruneOnly(true)
				.build();

		displayIdsSetting.addListener(table::repaint);
		tables.add(new WeakReference<>(table));
		return table;
	}

	private final AtomicInteger windowCounter = new AtomicInteger(1);

	public JFrame newWindow(Component basis) {
		JFrame newWindow = new JFrame("Events (" + windowCounter.getAndIncrement() + ')');
		newWindow.setContentPane((Container) getEventsTab());
		newWindow.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		Window parent = SwingUtilities.getWindowAncestor(basis);
		newWindow.setLocationRelativeTo(parent);
		newWindow.setSize(parent.getSize());
		newWindow.setLocation(new Point(parent.getX() + 100, parent.getY() + 100));
		newWindow.setVisible(true);
		newWindow.revalidate();
		return newWindow;
	}

	@HandleEvents(order = 1_000_000)
	public void refreshTables(EventContext context, Event event) {
		for (WeakReference<TableWithFilterAndDetails<?, ?>> tableRef : tables) {
			TableWithFilterAndDetails<?, ?> table = tableRef.get();
			if (table != null) {
				table.signalNewData();
			}
		}
	}

	private final List<WeakReference<TableWithFilterAndDetails<?, ?>>> tables = new CopyOnWriteArrayList<>();

	private <X extends Event> @Nullable Function<TableWithFilterAndDetails<X, ?>, Component> replayNextPseudoFilter(Class<X> clazz) {
		if (replay == null) {
			return null;
		}
		// TODO: this also attaches its own event handler
		return table -> new ReplayAdvancePseudoFilter<>(clazz, master, replay, table).getComponent();
	}


}
