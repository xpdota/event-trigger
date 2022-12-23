package gg.xp.xivsupport.custompartyoverlay;

import com.fasterxml.jackson.core.type.TypeReference;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.gui.overlay.OverlayConfig;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.gui.overlay.XivOverlay;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.CustomJsonListSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
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
	private final CustomPartyOverlayComponentFactory factory;
	private final JPanel panel;

	private List<List<RefreshablePartyListComponent>> refreshables = Collections.emptyList();
	private final IntSetting yOffset;
	private final CustomJsonListSetting<CustomOverlayComponentSpec> elements;


	public CustomPartyOverlay(OverlayConfig oc,
	                          PersistenceProvider persistence,
	                          XivState state,
	                          CustomPartyOverlayComponentFactory factory
	) {
		super("Custom Party Overlay", "custom-party-overlay", oc, persistence);
		this.elements = CustomJsonListSetting.builder(persistence, new TypeReference<CustomOverlayComponentSpec>() {
				}, "custom-party-overlay.components", "custom-party-overlay.failures")
				.withDefaultProvider(this::getDefaults).build();
		List<CustomOverlayComponentSpec> existingItems = elements.getItems();
		List<CustomOverlayComponentSpec> newItems = new ArrayList<>();
		for (CustomPartyOverlayComponentType type : CustomPartyOverlayComponentType.values()) {
			if (!type.shouldBePresent()) {
				continue;
			}
			if (existingItems.stream().noneMatch(spec -> spec.componentType == type)) {
				getDefaults().stream().filter(defaultItem -> defaultItem.componentType == type)
						.findFirst()
						.ifPresent(newItems::add);
			}
		}
		newItems.forEach(elements::addItem);
		this.yOffset = new IntSetting(persistence, "custom-party-overlay.y-offset", 39, 0, 1000);
		this.yOffset.addListener(this::placeComponents);
		this.elements.addListener(this::placeComponents);
		this.state = state;
		this.factory = factory;
		this.panel = new JPanel(null);
		panel.setOpaque(false);
		getPanel().add(panel);
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

	@Override
	protected void onBecomeVisible() {
		placeComponents();
	}

	private void placeComponents() {
		SwingUtilities.invokeLater(() -> {

			panel.removeAll();
			int maxX = 10;
			int maxY = 10;
			List<List<RefreshablePartyListComponent>> refreshables = new ArrayList<>(8);
			List<CustomOverlayComponentSpec> componentSpecs = elements.getItems();
			for (int i = 0; i < 8; i++) {
				List<RefreshablePartyListComponent> list = new ArrayList<>();
				refreshables.add(list);
				int offset = i * yOffset.get();
				for (CustomOverlayComponentSpec spec : componentSpecs) {
					RefreshablePartyListComponent ref = factory.makeComponent(spec);
					if (ref == null) {
						continue;
					}
					Component component = ref.getComponent();
					panel.add(component);
					component.setBounds(spec.x, spec.y + offset, spec.width, spec.height);
					maxX = Math.max(maxX, spec.x + spec.width);
					maxY = Math.max(maxY, spec.y + offset + spec.height);
					list.add(ref);
					log.trace("Added: {} -> {} -> {}", i, spec.componentType, component);
				}
			}
			panel.setPreferredSize(new Dimension(maxX + 10, maxY + 10));
			this.refreshables = refreshables;
			try {
				panel.validate();
			}
			catch (Throwable t) {
				log.error("Error validating!", t);
			}
			repackSize();
			SwingUtilities.invokeLater(() -> {
			});
		});
	}

	private List<CustomOverlayComponentSpec> getDefaults() {
		List<CustomOverlayComponentSpec> specs = new ArrayList<>();
		{
			CustomOverlayComponentSpec comp = new CustomOverlayComponentSpec();
			comp.x = 0;
			comp.y = 0;
			comp.width = 90;
			comp.height = 20;
			comp.componentType = CustomPartyOverlayComponentType.NAME;
			specs.add(comp);
		}
		{
			CustomOverlayComponentSpec comp = new CustomOverlayComponentSpec();
			comp.x = 90;
			comp.y = 0;
			comp.width = 20;
			comp.height = 20;
			comp.componentType = CustomPartyOverlayComponentType.JOB;
			specs.add(comp);
		}
		{
			CustomOverlayComponentSpec comp = new CustomOverlayComponentSpec();
			comp.x = 110;
			comp.y = 0;
			comp.width = 180;
			comp.height = 20;
			comp.componentType = CustomPartyOverlayComponentType.HP;
			specs.add(comp);
		}
		{
			CustomOverlayComponentSpec comp = new CustomOverlayComponentSpec();
			comp.x = 290;
			comp.y = 0;
			comp.width = 298;
			comp.height = 35;
			comp.componentType = CustomPartyOverlayComponentType.BUFFS_WITH_TIMERS;
			specs.add(comp);
		}
		{
			CustomOverlayComponentSpec comp = new CustomOverlayComponentSpec();
			comp.x = 50;
			comp.y = 20;
			comp.width = 55;
			comp.height = 20;
			comp.componentType = CustomPartyOverlayComponentType.CUSTOM_BUFFS;
			comp.enabled = false;
			specs.add(comp);
		}
		{
			CustomOverlayComponentSpec comp = new CustomOverlayComponentSpec();
			comp.x = 110;
			comp.y = 20;
			comp.width = 120;
			comp.height = 14;
			comp.componentType = CustomPartyOverlayComponentType.CAST_BAR;
			specs.add(comp);
		}
		{
			CustomOverlayComponentSpec comp = new CustomOverlayComponentSpec();
			comp.x = 230;
			comp.y = 20;
			comp.width = 60;
			comp.height = 14;
			comp.componentType = CustomPartyOverlayComponentType.MP_BAR;
			specs.add(comp);
		}
		{
			CustomOverlayComponentSpec comp = new CustomOverlayComponentSpec();
			comp.x = 0;
			comp.y = 0;
			comp.width = 120;
			comp.height = 40;
			comp.enabled = false;
			comp.componentType = CustomPartyOverlayComponentType.COOLDOWNS;
			specs.add(comp);
		}
		return specs;

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

	public IntSetting getYOffset() {
		return yOffset;
	}

	public CustomJsonListSetting<CustomOverlayComponentSpec> getElements() {
		return elements;
	}

	public void resetToDefault() {
		getYOffset().delete();
		elements.setItems(getDefaults());
	}
}
