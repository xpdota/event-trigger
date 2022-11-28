package gg.xp.xivsupport.events.triggers.easytriggers.gui;

import gg.xp.xivsupport.events.triggers.easytriggers.model.Action;

import java.awt.*;

public interface ActionDragDropTarget {

	// TODO: validation

	int indexFor(Point pointOnScreen);

	void previewDrop(Point pointOnScreen);

	void doDrop(int index, Action<?> action);

}
