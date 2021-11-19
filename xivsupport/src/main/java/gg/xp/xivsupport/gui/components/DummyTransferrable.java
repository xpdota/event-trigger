package gg.xp.xivsupport.gui.components;

import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.Serializable;

public class DummyTransferrable implements Transferable, Serializable {
	private static final long serialVersionUID = 4133123683982896282L;

	DummyTransferrable() {
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[0];
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return false;
	}

	@NotNull
	@Override
	public Object getTransferData(DataFlavor flavor) {
		return new Object();
	}
}
