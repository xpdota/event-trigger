package gg.xp.xivsupport.skillhitoverlay;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.actlines.events.AbilityResolvedEvent;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.DamageEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.HitSeverity;
import gg.xp.xivsupport.events.misc.ProxyForAppendOnlyList;
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;
import gg.xp.xivsupport.events.misc.pulls.PullTracker;
import gg.xp.xivsupport.gui.overlay.OverlayConfig;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.gui.overlay.XivOverlay;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.renderers.ActionAndStatusRenderer;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.ColorSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;

import javax.swing.*;
import javax.swing.plaf.TableHeaderUI;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ScanMe
public class SkillHitTracker extends XivOverlay implements FilteredEventHandler {

	private final IntSetting numberOfRows;
	private final ColorSetting fg;
	private final ColorSetting bg;
	private final JTable table;
	private final CustomTableModel<SkillTracker> model;

	public SkillHitTracker(OverlayConfig oc, PersistenceProvider pers) {
		super("Skill Hit Tracker", "skill-hit-tracker", oc, pers);
		numberOfRows = new IntSetting(pers, "skill-hit-tracker.num-rows", 6, 1, 20);
		fg = new ColorSetting(pers, "skill-hit-tracker.fg-color", new Color(255, 255, 255));
		bg = new ColorSetting(pers, "skill-hit-tracker.bg-color", new Color(20, 20, 20, 20));
		model = CustomTableModel.builder(() -> displaySkillList)
				.addColumn(new CustomColumn<>("Skill", i -> i.ability, c -> {
					c.setCellRenderer(new ActionAndStatusRenderer());
					c.setMinWidth(150);
					c.setMaxWidth(150);
				}))
				.addColumn(new CustomColumn<>("Crit %", i -> String.format("%.01f %%", 100.0 * i.c / (float) i.hits), 50))
				.addColumn(new CustomColumn<>("Dhit %", i -> String.format("%.01f %%", 100.0 * i.d / (float) i.hits), 50))
				.addColumn(new CustomColumn<>("DCrit %", i -> String.format("%.01f %%", 100.0 * i.dc / (float) i.hits), 50))
				.addColumn(new CustomColumn<>("Max", i -> i.maxHit, 50))
				.addColumn(new CustomColumn<>("Total", i -> i.total, 60))
				.build();
		table = model.makeTable();
		table.setCellSelectionEnabled(false);
		table.setOpaque(false);
		table.setBackground(new Color(0, 0, 0, 0));
		fg.addAndRunListener(() -> table.setForeground(fg.get()));
		JTableHeader header = table.getTableHeader();
		header.setBackground(new Color(0, 0, 0, 0));
		fg.addAndRunListener(() -> header.setForeground(fg.get()));
		header.setOpaque(false);
		JPanel innerPanel = new JPanel() {
			@Override
			public void paint(Graphics g) {
				((Graphics2D) g).setBackground(getBackground());
				g.clearRect(0, 0, getWidth(), getHeight());
				super.paint(g);
			}
		};
		innerPanel.setLayout(new BorderLayout());
		innerPanel.setOpaque(true);
		bg.addAndRunListener(() -> innerPanel.setBackground(bg.get()));
		innerPanel.add(table, BorderLayout.CENTER);
		innerPanel.add(header, BorderLayout.NORTH);
		getPanel().add(innerPanel);
		RefreshLoop<SkillHitTracker> refresher = new RefreshLoop<>("DotTrackerOverlay", this, SkillHitTracker::refresh, unused -> 1000L);
		repackSize();
		refresher.start();
	}

	private void refresh() {
		displaySkillList = new ArrayList<>(new ProxyForAppendOnlyList<>(skillList)).stream()
				.sorted(Comparator.comparing(st -> -st.total))
				.limit(numberOfRows.get())
				.toList();
		model.signalNewData();
	}

	@Override
	protected void repackSize() {
		table.setPreferredSize(new Dimension(table.getPreferredSize().width, table.getRowHeight() * numberOfRows.get()));
		super.repackSize();
	}

	@Override
	public boolean enabled(EventContext context) {
		// Only track if overlay is enabled
		return getEnabled().get();
	}

	// These three are only accessed from event loop thread, except SkillList which is read (but not modified)
	// via ProxyForAppendOnlyList
	private final Map<Long, SkillTracker> skillMap = new HashMap<>();
	private volatile List<SkillTracker> skillList = new ArrayList<>();
	private boolean resetOnNext;
	// This is accessed from the refresher thread and from the GUI thread, hence why it is replaced entirely rather
	// than being modified in place.
	private volatile List<SkillTracker> displaySkillList = Collections.emptyList();

	private static final class SkillTracker {
		final XivAbility ability;
		int c;
		int d;
		int dc;
		int hits;
		long maxHit;
		long total;

		private SkillTracker(XivAbility ability) {
			this.ability = ability;
		}

		private void process(DamageEffect de) {
			long amount = de.getAmount();
			HitSeverity severity = de.getSeverity();
			hits++;
			total += amount;
			if (amount > maxHit) {
				maxHit = amount;
			}
			switch (severity) {
				case CRIT -> c++;
				case DHIT -> d++;
				case CRIT_DHIT -> dc++;
			}
		}
	}

	@HandleEvents
	public void pullStart(EventContext context, PullStartedEvent pse) {
		flagForReset();
		skillList = new ArrayList<>();
	}

	private void flagForReset() {
		resetOnNext = true;
	}

	private void doReset() {
		skillList.clear();
		skillMap.clear();
	}

	@HandleEvents
	public void hit(EventContext context, AbilityResolvedEvent event) {
		if (resetOnNext) {
			doReset();
			resetOnNext = false;
		}
		if (event.getSource().isThePlayer()) {
			for (DamageEffect de : event.getEffectsOfType(DamageEffect.class)) {
				skillMap.computeIfAbsent(event.getAbility().getId(), unused -> {
							SkillTracker st = new SkillTracker(event.getAbility());
							skillList.add(st);
							return st;
						}).process(de);
			}
		}
	}

}
