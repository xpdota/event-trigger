package gg.xp.xivsupport.gui.overlay;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;


public class WindowsUtils implements ActiveWindowTitleGetter {

	private final User32 u32 = User32.INSTANCE;

	@Override
	public String getActiveWindowTitle() {
		WinDef.HWND fgWindow = u32.GetForegroundWindow();
		return getWindowTitle(fgWindow);
	}

	@Override
	public boolean isFfxivActive() {
		return FFXIV_WINDOW_TITLE.equalsIgnoreCase(getActiveWindowTitle());
	}

	public String getWindowTitle(WinDef.HWND window) {
		int length = u32.GetWindowTextLength(window);
		if (length == 0) return "";
		/* Use the character encoding for the default locale */
		char[] chars = new char[length + 1];
		u32.GetWindowText(window, chars, length + 1);
		return new String(chars).substring(0, length);
	}

	// TODO: move the windows-specific stuff into a new class
	// https://discord.com/channels/551474815727304704/594899820976668673/1211457234802970645
	/*
	[3:38 PM] wexxlee: This should work?  Tested it in all 3 window modes with focus, another window in focus, and
	minimized and outputs right result...

	using System.Diagnostics;
	using System.Runtime.InteropServices;

	long WS_POPUP = 0x80000000L;
	long WS_CAPTION = 0x00C00000L;

	[DllImport("user32.dll")]
	static extern long GetWindowLongPtr(IntPtr hWnd, int nIndex);

	var process = Process.GetProcessesByName("ffxiv_dx11").FirstOrDefault();
	if (process != null)
	{
		IntPtr mainWindowHandle = process.MainWindowHandle;
		long style = GetWindowLongPtr(mainWindowHandle, -16);
		string windowType = "";
		if ((style & WS_POPUP) != 0) {
			windowType = "Borderless";
		} else if ((style & WS_CAPTION) != 0) {
			windowType = "Windowed";
		} else
		{
			windowType = "Exclusive Fullscreen";
		}

		Console.WriteLine($"Type: {windowType}");
	}
	 */

	public enum FfxivWindowMode {
		NONE,
		WINDOWED,
		BORDERLESS,
		EXCLUSIVE_FULLSCREEN
	}

	public @Nullable WinDef.HWND getFfxivWindow() {
		AtomicReference<WinDef.HWND> out = new AtomicReference<>();
		u32.EnumWindows((hwnd, data) -> {
			int length = u32.GetWindowTextLength(hwnd);
			if (length == 0) return true;
			/* Use the character encoding for the default locale */
			char[] chars = new char[length + 1];
			u32.GetWindowText(hwnd, chars, length + 1);
			String title = new String(chars).substring(0, length);
			if (title.equalsIgnoreCase(FFXIV_WINDOW_TITLE)) {
				out.set(hwnd);
				return false;
			}
			return true;
		}, Pointer.NULL);
		return out.get();
	}

	@SuppressWarnings("unused")
	public FfxivWindowMode getFfxivWindowMode() {
		WinDef.HWND window = getFfxivWindow();
		if (window == null) {
			return FfxivWindowMode.NONE;
		}
		int style = u32.GetWindowLong(window, WinUser.GWL_STYLE);
		if ((style & WinUser.WS_POPUP) != 0) {
			return FfxivWindowMode.BORDERLESS;
		}
		else if ((style & WinUser.WS_CAPTION) != 0) {
			return FfxivWindowMode.WINDOWED;
		}
		else {
			return FfxivWindowMode.EXCLUSIVE_FULLSCREEN;
		}
	}
}
