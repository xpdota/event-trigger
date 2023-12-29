package gg.xp.xivsupport.timelines.gui;

import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.NoAutoScan;
import gg.xp.xivdata.data.*;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.cdsupport.CustomCooldownManager;
import gg.xp.xivsupport.cdsupport.CustomCooldownsUpdated;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.misc.pulls.PullEndedEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.util.EasyAction;
import gg.xp.xivsupport.models.XivZone;
import gg.xp.xivsupport.replay.ReplayController;
import gg.xp.xivsupport.sys.Threading;
import gg.xp.xivsupport.timelines.CustomTimelineEntry;
import gg.xp.xivsupport.timelines.CustomTimelineItem;
import gg.xp.xivsupport.timelines.TimelineCustomizations;
import gg.xp.xivsupport.timelines.TimelineInfo;
import gg.xp.xivsupport.timelines.TimelineManager;
import gg.xp.xivsupport.timelines.TimelineProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@NoAutoScan
public class TimelineRecordingPopup extends JDialog {

	private static final Logger log = LoggerFactory.getLogger(TimelineRecordingPopup.class);
	private static final ExecutorService exs = Executors.newCachedThreadPool(Threading.namedDaemonThreadFactory("TimelineRecordingPopup"));
	public static final String IMPORT_SOURCE = "Timeline Recorder";

	private final TimelineManager mgr;
	private final TimelinesTab tab;
	private final ReplayController replay;
	private final XivState state;
	private final CustomCooldownManager cdm;
	private final JLabel statusLabel;
	private Map<Long, ExtendedCooldownDescriptor> cdMap;
	private volatile boolean isInTlZone;
	private volatile boolean recording;
	private List<Entry> entries;
	private JButton runButton;
	private JButton closeButton;
	private JButton acceptButton;
	private XivZone zone;

	public TimelineRecordingPopup(TimelineManager mgr, TimelinesTab tab, ReplayController replay, XivState state, CustomCooldownManager cdm) {
		super(SwingUtilities.getWindowAncestor(tab), "Timeline Recording");
		this.mgr = mgr;
		this.tab = tab;
		this.replay = replay;
		this.state = state;
		this.cdm = cdm;
		this.statusLabel = new JLabel("...", SwingConstants.CENTER);
		processZoneChange();
		processCdChange();
		setupGui();
		reset();
		setLocationRelativeTo(tab);
	}

	private void setupGui() {
		JPanel content = new JPanel();
		content.setLayout(new BorderLayout());
		content.add(statusLabel, BorderLayout.CENTER);

		JPanel buttons = new JPanel();
		buttons.setLayout(new WrapLayout());

		acceptButton = new JButton("Accept");
		acceptButton.addActionListener(l -> this.finish());
		buttons.add(acceptButton);
		acceptButton.setEnabled(false);
		runButton = new JButton("Run");
		runButton.addActionListener(l -> exs.submit(this::start));
		buttons.add(runButton);
		closeButton = new EasyAction("Cancel", () -> setVisible(false)).asButton();
		buttons.add(closeButton);

		content.add(buttons, BorderLayout.SOUTH);
		content.setPreferredSize(new Dimension(400, 75));
		setContentPane(content);
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		pack();

	}

	private void finalizeEntries(XivZone zone, List<Entry> entries) {
		TimelineCustomizations cs = mgr.getCustomSettings(zone.getId());
		List<CustomTimelineItem> allEntries = new ArrayList<>(cs.getEntries().stream().filter(ti -> !IMPORT_SOURCE.equals(ti.getImportSource())).toList());
		entries.forEach(entry -> {
			CustomTimelineEntry newEntry = new CustomTimelineEntry();
			newEntry.importSource = IMPORT_SOURCE;
			newEntry.time = entry.time;
			newEntry.name = entry.event.getAbility().getName();
			ActionIcon icon = ActionLibrary.iconForId(entry.event.getAbility().getId());
			CombatJobSelection jobSel = CombatJobSelection.all();
			if (icon != null) {
				newEntry.icon = icon.getIconUrl();
			}
			Job job = entry.cd.getJob();
			JobType jobType = entry.cd.getJobType();
			if (job != null) {
				jobSel.setEnabledForAll(false);
				jobSel.changeJobState(job, true);
			}
			else if (jobType != null) {
				jobSel.setEnabledForAll(false);
				jobSel.changeCategoryState(jobType, true);
			}
			newEntry.enabledJobs = jobSel;
			allEntries.add(newEntry);
		});
		cs.setEntries(allEntries);
		mgr.commitCustomSettings(zone.getId());
		tab.selectZone(zone.getId());
		tab.refresh();
	}

	private void finish() {
		List<Entry> entries = new ArrayList<>(this.entries);
		exs.submit(() -> finalizeEntries(this.zone, entries));
		setVisible(false);
		reset();
	}

	@Override
	public void setVisible(boolean b) {
		super.setVisible(b);
	}

	private void setStatusLabel(String newStatus) {
		statusLabel.setText(newStatus);
	}

	@HandleEvents
	public void zoneChange(ZoneChangeEvent zce) {
		processZoneChange();
	}

	private void processZoneChange() {
		XivZone zone = state.getZone();
		if (zone == null) {
			isInTlZone = false;
		}
		else {
			isInTlZone = mgr.getTimeline(zone.getId()) != null;
		}
	}

	@HandleEvents
	public void cdChange(CustomCooldownsUpdated event) {
		processCdChange();
	}

	private void processCdChange() {
		Map<Long, ExtendedCooldownDescriptor> cdIdMap = new HashMap<>();
		for (ExtendedCooldownDescriptor cd : cdm.getAllCds()) {
			Collection<Long> ids = cd.getAllRelevantAbilityIds();
			ids.forEach(id -> cdIdMap.put(id, cd));
		}
		cdMap = cdIdMap;
	}

	@HandleEvents
	public void pullEnd(PullEndedEvent event) {
		log.info("Pull ended");
		recording = false;
	}

	@HandleEvents
	public void abilityUsed(AbilityUsedEvent event) {
		if (!recording) {
			return;
		}
		ExtendedCooldownDescriptor cd = cdMap.get(event.getAbility().getId());
		if (cd != null && cd.abilityIdMatches(event.getAbility().getId())) {
			log.info("CD used: {} -> {}", event.getAbility(), cd);
			double time = mgr.getCurrentProcessor().getEffectiveTime();
			entries.add(new Entry(time, cd, event));
		}
	}

	private void reset() {
		setStatusLabel("Click 'Run' to start...");
		entries = new ArrayList<>();
		acceptButton.setEnabled(false);
		runButton.setEnabled(true);
		closeButton.setEnabled(true);
		setDefaultCloseOperation(HIDE_ON_CLOSE);
	}

	private void start() {
		// First, advance to the beginning of a valid zone
		try {
			setStatusLabel("Finding beginning of zone...");
			replay.advanceByAsyncWhile(() -> !isInTlZone).get();
			this.zone = state.getZone();
			KnownDuty duty = KnownDuty.forZone(zone.getId());
			TimelineInfo tlInfo = mgr.getInfoForZone(zone.getId());
			TimelineProcessor tl = mgr.getTimeline(zone.getId());
			StringBuilder confirmation = new StringBuilder("You are in zone ")
					.append(zone.getId());
			String zoneName = zone.getName();
			if (!zoneName.equals(String.valueOf(zone.getId()))) {
				confirmation
						.append(" '")
						.append(zone.getName())
						.append('\'');
			}

			if (duty != null) {
				confirmation.append(" (").append(duty.getName()).append(')');
			}
			confirmation.append('\n')
					.append("The timeline file is ")
					.append(tlInfo.filename());
			confirmation.append("\nStart recording?");
			setStatusLabel("...");
			int result = JOptionPane.showConfirmDialog(this, confirmation.toString(), "Start Recording?", JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.NO_OPTION) {
				reset();
				return;
			}
			tab.selectZone(zone.getId());
			runButton.setEnabled(false);
			closeButton.setEnabled(false);
			setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			setStatusLabel("Recording...");
			recording = true;
			replay.advanceByAsyncWhile(() -> recording).get();
			entries.forEach(e -> log.info("Entry: {}", e));
			setStatusLabel("Done! Collected %s entries.".formatted(entries.size()));
			acceptButton.setEnabled(true);
			closeButton.setEnabled(true);
			setDefaultCloseOperation(HIDE_ON_CLOSE);
		}
		catch (Throwable t) {
			log.error("Error when replaying: ", t);
			setStatusLabel("Error, see log");
			throw new RuntimeException(t);
		}
	}

	private record Entry(double time, ExtendedCooldownDescriptor cd, AbilityUsedEvent event) {
	}

}
