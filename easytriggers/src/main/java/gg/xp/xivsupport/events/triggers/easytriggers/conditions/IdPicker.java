package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivdata.data.ActionInfo;
import gg.xp.xivdata.data.ActionLibrary;
import gg.xp.xivdata.data.HasIconURL;
import gg.xp.xivdata.data.StatusEffectInfo;
import gg.xp.xivdata.data.StatusEffectLibrary;
import gg.xp.xivsupport.gui.ResettableField;
import gg.xp.xivsupport.gui.library.ActionTableFactory;
import gg.xp.xivsupport.gui.library.StatusTable;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.tables.renderers.ScaledImageComponent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class IdPicker<X> extends JPanel implements ResettableField {

	private static final Logger log = LoggerFactory.getLogger(IdPicker.class);

	private final TextFieldWithValidation<X> textBox;
	private final JLabel currentItemLabel;
	private final Function<X, String> itemToLabel;
	private final Supplier<Long> getter;
	private final Function<Long, X> idToItem;
	private final Function<X, Long> itemToId;
	private final JPanel iconHolder;
	private final Function<X, HasIconURL> itemToIcon;

	public IdPicker(Supplier<Long> getter, Consumer<Long> setter, Function<Long, X> idToItem, Function<X, Long> itemToId, Function<X, String> itemToLabel, Function<Window, X> picker, Function<X, HasIconURL> itemToIcon) {
		this.itemToLabel = itemToLabel;
		this.idToItem = idToItem;
		this.itemToId = itemToId;
		this.getter = getter;
		this.itemToIcon = itemToIcon;
		iconHolder = new JPanel(new BorderLayout(0, 0));
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		JButton pickButton = new JButton("Pick");
		textBox = new TextFieldWithValidation<>(s -> {
			long id = Long.parseLong(s);
			X apply = idToItem.apply(id);
			if (apply == null) {
				throw new IllegalArgumentException("Cannot be null");
			}
			return apply;
		}, newValue -> {
			setter.accept(itemToId.apply(newValue));
			refresh();
		}, () -> getter.get().toString());
		currentItemLabel = new JLabel();
		refresh();
		pickButton.addActionListener(l -> {
			X item = picker.apply(SwingUtilities.getWindowAncestor(this));
			if (item != null) {
				setter.accept(itemToId.apply(item));
			}
			refresh();
		});
		add(pickButton);
		add(Box.createHorizontalStrut(3));
		add(textBox);
		add(Box.createHorizontalStrut(3));
		add(iconHolder);
		add(Box.createHorizontalStrut(2));
		add(currentItemLabel);
	}

	private @Nullable X currentItem() {
		Long id = getter.get();
		if (id == null) {
			return null;
		}
		return idToItem.apply(id);
	}

	private void refresh() {
		X current = currentItem();
		if (current == null) {
			currentItemLabel.setText("Unknown Item");
			return;
		}
		else {
			SwingUtilities.invokeLater(textBox::resetText);
			Long newId = itemToId.apply(current);
			String newName = itemToLabel.apply(current);
			String fullLabel = String.format("%s (%s, 0x%x)", newName, newId, newId);
			currentItemLabel.setText(fullLabel);
			HasIconURL icon = itemToIcon.apply(current);
			iconHolder.removeAll();
			if (icon != null) {
				ScaledImageComponent iconComponent = IconTextRenderer.getIconOnly(icon);
				if (iconComponent != null) {
					iconHolder.add(iconComponent.cloneThis());
				}
			}
			revalidate();
			repaint();
		}
	}

	@Override
	public void reset() {
		refresh();
	}
}
