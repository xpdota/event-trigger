package gg.xp.xivsupport.gui.tabs;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.actlines.events.HasStatusEffect;
import gg.xp.xivsupport.events.actlines.events.HasZone;
import gg.xp.xivsupport.events.misc.NpcYellEvent;
import gg.xp.xivsupport.gui.library.ActionTableFactory;
import gg.xp.xivsupport.gui.library.NpcYellTableFactory;
import gg.xp.xivsupport.gui.library.RsvTable;
import gg.xp.xivsupport.gui.library.StatusTable;
import gg.xp.xivsupport.gui.library.StatusTableFactory;
import gg.xp.xivsupport.gui.library.ZonesTable;
import gg.xp.xivsupport.gui.library.ZonesTableFactory;
import gg.xp.xivsupport.gui.tables.CustomRightClickOption;
import gg.xp.xivsupport.gui.tables.RightClickOptionRepo;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.rsv.RsvEntry;
import gg.xp.xivsupport.sys.Threading;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

@ScanMe
public class LibraryTab extends JTabbedPane {

	private static final ExecutorService exs = Executors.newCachedThreadPool(Threading.namedDaemonThreadFactory("LibraryTab"));
	private static final Logger log = LoggerFactory.getLogger(LibraryTab.class);

	private final TableWithFilterAndDetails<ActionInfo, Object> abilityTable;
	private final TableWithFilterAndDetails<StatusEffectInfo, Object> statusTable;
	private final TableWithFilterAndDetails<ZoneInfo, Object> zonesTable;
	private final TableWithFilterAndDetails<RsvEntry, Object> rsvTable;
	private final TableWithFilterAndDetails<NpcYellInfo, Object> npcYellTable;
	private volatile boolean loaded;

	public LibraryTab(ActionTableFactory atf, StatusTableFactory stf, ZonesTableFactory ztf, NpcYellTableFactory nytf, RightClickOptionRepo rightClicks) {
		super(LEFT);
		{
			abilityTable = atf.table();
			addTab("Actions/Abilities", abilityTable);
			rightClicks.addOption(makeCro("View in Action Library", abilityTable, HasAbility.class, ha -> ActionLibrary.forId(ha.getAbility().getId())));
		}
		{
			statusTable = stf.table();
			addTab("Status Effects", statusTable);
			rightClicks.addOption(makeCro("View in Status Library", statusTable, HasStatusEffect.class, hse -> hse.getBuff().getInfo()));
		}
		{
			zonesTable = ztf.table();
			addTab("Zones", zonesTable);
			rightClicks.addOption(makeCro("View in Zone Library", zonesTable, HasZone.class, hz -> ZoneLibrary.infoForZone((int) hz.getZone().getId())));
		}
		{
			rsvTable = RsvTable.table();
			addTab("RSV Entries", rsvTable);
		}
		{
			npcYellTable = nytf.table();
			addTab("Npc Yells", npcYellTable);
			rightClicks.addOption(makeCro("View in Library", npcYellTable, NpcYellEvent.class, NpcYellEvent::getYell));
		}
	}

	private <X, Y> CustomRightClickOption makeCro(String label, TableWithFilterAndDetails<X, ?> tbl,
	                                              Class<Y> foreignRowClass,
	                                              Function<Y, @Nullable X> converter
	) {
		return CustomRightClickOption.forRow(
				label,
				foreignRowClass,
				foreignRow -> {
					X item = converter.apply(foreignRow);
					if (item == null) {
						return;
					}
					log.info("Selecting: {}", foreignRow);
					boolean loaded = this.loaded;
					log.info("Loaded: {}", loaded);
					GuiUtil.bringToFront(tbl);
					exs.submit(() -> {
						if (!loaded) {
							int attempts = 20;
							for (int i = attempts; i > 0; i--) {
								CompletableFuture<Boolean> done = new CompletableFuture<>();
								SwingUtilities.invokeLater(() -> {
									tbl.setAndScrollToSelection(item);
									X selAfter = tbl.getCurrentSelection();
									done.complete(Objects.equals(selAfter, item));
								});
								try {
									boolean isDone = done.get();
									if (isDone) {
										return;
									}
									// Wait up to 5 seconds for the table to load
									Thread.sleep(250);
								}
								catch (Exception e) {
									log.error("Error", e);
								}

							}
						}
						else {
							SwingUtilities.invokeLater(() -> {
								tbl.setAndScrollToSelection(item);
							});
						}
					});
				}, foreignRow -> converter.apply(foreignRow) != null);
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			abilityTable.signalNewData();
			statusTable.signalNewData();
			zonesTable.signalNewData();
			rsvTable.signalNewData();
			npcYellTable.signalNewData();
		}
		super.setVisible(visible);
		if (visible) {
			this.loaded = true;
		}
	}
}
