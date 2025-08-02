package gg.xp.xivsupport.events.triggers.easytriggers.gui.tree;

import gg.xp.xivsupport.events.triggers.easytriggers.model.BaseTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.TriggerFolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public record TriggerTransferable(TriggerTransferData data) implements Transferable {
	public static final DataFlavor TRIGGER_FLAVOR = new DataFlavor(BaseTrigger.class, "Trigger");


	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[]{TRIGGER_FLAVOR};
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return flavor == TRIGGER_FLAVOR;
	}

	@Override
	public @NotNull Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		if (flavor == TRIGGER_FLAVOR) {
			return data;
		}
		throw new UnsupportedFlavorException(flavor);
	}

}
