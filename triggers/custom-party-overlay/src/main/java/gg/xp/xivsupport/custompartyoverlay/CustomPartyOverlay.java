package gg.xp.xivsupport.custompartyoverlay;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.actionresolution.SequenceIdTracker;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.gui.overlay.OverlayConfig;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.gui.overlay.XivOverlay;
import gg.xp.xivsupport.gui.tables.StandardColumns;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ScanMe
public class CustomPartyOverlay extends XivOverlay {

	private static final Logger log = LoggerFactory.getLogger(CustomPartyOverlay.class);

	private final XivState state;
	private final StatusEffectRepository buffs;
	private final BooleanSetting showPredictedHp;
	private final ActiveCastRepository acr;
	private final SequenceIdTracker sqidTracker;

	private List<CustomOverlayComponentSpec> componentSpecs = Collections.emptyList();

	private List<List<RefreshablePartyListComponent>> refreshables = Collections.emptyList();
	private int yOffset;
	private final ListSett


	public CustomPartyOverlay(OverlayConfig oc,
	                          PersistenceProvider persistence,
	                          XivState state,
	                          StatusEffectRepository buffs,
	                          StandardColumns cols,
	                          ActiveCastRepository acr,
	                          SequenceIdTracker sqidTracker) {
		super("Custom Party Overlay", "custom-party-overlay", oc, persistence);
		this.state = state;
		this.buffs = buffs;
		showPredictedHp = cols.getShowPredictedHp();
		this.acr = acr;
		this.sqidTracker = sqidTracker;
		getPanel().setLayout(null);
		setupSpecs();
		new RefreshLoop<>("CustomPartyRefresh", this, customPartyOverlay -> {
			if (isVisible()) {
				customPartyOverlay.periodicRefresh();
			}
		}, unused -> calculateUnscaledFrameTime(33)).start();
	}

	@Override
	public void finishInit() {
		super.finishInit();
		placeComponents();
	}

	public void periodicRefresh() {
		for (int i = 0; i < refreshables.size(); i++) {
			XivPlayerCharacter partySlot = getPartySlot(i);
			List<RefreshablePartyListComponent> refsForSlot = refreshables.get(i);
			for (RefreshablePartyListComponent refreshable : refsForSlot) {
				refreshable.refresh(partySlot);
			}
		}
		getPanel().repaint();
	}

	@Override
	public void setVisible(boolean visible) {
		periodicRefresh();
		super.setVisible(visible);
	}

	private void placeComponents() {
		setupSpecs();
		JPanel panel = getPanel();
		panel.removeAll();
		int maxX = 10;
		int maxY = 10;
		List<List<RefreshablePartyListComponent>> refreshables = new ArrayList<>(8);
		for (int i = 0; i < 8; i++) {
			List<RefreshablePartyListComponent> list = new ArrayList<>();
			refreshables.add(list);
			int offset = i * yOffset;
			for (CustomOverlayComponentSpec spec : componentSpecs) {
				RefreshablePartyListComponent ref = makeComponent(spec);
				if (ref == null) {
					continue;
				}
				Component component = ref.getComponent();
				component.setBounds(spec.x, spec.y + offset, spec.width, spec.height);
				maxX = Math.max(maxX, spec.x + spec.width);
				maxY = Math.max(maxY, spec.y + offset + spec.height);
				panel.add(component);
				list.add(ref);
				log.info("Added: {} -> {} -> {}", i, spec.componentType, component);
			}
		}
		panel.setPreferredSize(new Dimension(maxX + 10, maxY + 10));
		this.refreshables = refreshables;
		repackSize();
	}

	private void setupSpecs() {
		List<CustomOverlayComponentSpec> specs = new ArrayList<>();
		{
			CustomOverlayComponentSpec comp = new CustomOverlayComponentSpec();
			comp.x = 10;
			comp.y = 20;
			comp.width = 100;
			comp.height = 20;
			comp.componentType = CustomPartyOverlayComponentType.NAME_JOB;
			specs.add(comp);
		}
		{
			CustomOverlayComponentSpec comp = new CustomOverlayComponentSpec();
			comp.x = 110;
			comp.y = 20;
			comp.width = 130;
			comp.height = 20;
			comp.componentType = CustomPartyOverlayComponentType.HP;
			specs.add(comp);
		}
		{
			CustomOverlayComponentSpec comp = new CustomOverlayComponentSpec();
			comp.x = 240;
			comp.y = 20;
			comp.width = 300;
			comp.height = 25;
			comp.componentType = CustomPartyOverlayComponentType.BUFFS_WITH_TIMERS;
			specs.add(comp);
		}
		{
			CustomOverlayComponentSpec comp = new CustomOverlayComponentSpec();
			comp.x = 110;
			comp.y = 40;
			comp.width = 90;
			comp.height = 14;
			comp.componentType = CustomPartyOverlayComponentType.CAST_BAR;
			specs.add(comp);
		}
		{
			CustomOverlayComponentSpec comp = new CustomOverlayComponentSpec();
			comp.x = 200;
			comp.y = 40;
			comp.width = 40;
			comp.height = 14;
			comp.componentType = CustomPartyOverlayComponentType.MP_BAR;
			specs.add(comp);
		}
		componentSpecs = specs;
		yOffset = 37;

	}

	private @Nullable XivPlayerCharacter getPartySlot(int partySlot) {
		List<XivPlayerCharacter> partyList = state.getPartyList();
		if (partySlot >= partyList.size()) {
			return null;
		}
		else {
			return partyList.get(partySlot);
		}
	}

	private RefreshablePartyListComponent makeComponent(CustomOverlayComponentSpec spec) {
		CustomPartyOverlayComponentType type = spec.componentType;
		RefreshablePartyListComponent component;
		component = switch (type) {
			case NOTHING -> new DoNothingComponent();
			case NAME_JOB -> new NameJobComponent();
			case HP -> new HpBarComponent(showPredictedHp, sqidTracker);
			case BUFFS -> new BuffsComponent(buffs);
			case BUFFS_WITH_TIMERS -> new BuffsWithTimersComponent(buffs);
			case CAST_BAR -> new CastBarPartyComponent(acr);
			case MP_BAR -> new MpBarComponent();
		};
		return component;
	}


}
