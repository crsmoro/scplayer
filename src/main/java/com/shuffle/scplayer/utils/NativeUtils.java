package com.shuffle.scplayer.utils;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public abstract class NativeUtils {
	public static Pointer pointerFrom(String string) {
		Pointer pointer = new Memory(Native.WCHAR_SIZE * (string.length() + 1));
		pointer.setString(0L, string);
		return pointer;
	}

	public static Pointer pointerFrom(byte[] bytes) {
		Pointer pointer = new Memory(Native.WCHAR_SIZE * (bytes.length + 1));
		pointer.write(0L, bytes, 0, bytes.length);
		return pointer;
	}

	public static boolean toBoolean(int value) {
		return value == 1;
	}
	
	public static int fromBoolean(boolean value) {
		return value ? 1 : 0;
	}
}
