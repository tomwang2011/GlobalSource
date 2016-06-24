/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.netbeansproject.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Shuyang Zhou
 */
public class StringUtil {

	public static String bytesToHexString(byte[] bytes) {
		char[] chars = new char[bytes.length * 2];

		for (int i = 0; i < bytes.length; i++) {
			chars[i * 2] = _HEX_DIGITS[(bytes[i] & 0xFF) >> 4];
			chars[i * 2 + 1] = _HEX_DIGITS[bytes[i] & 0x0F];
		}

		return new String(chars);
	}

	public static String merge(Collection<String> strings, char separator) {
		if (strings.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();

		for (String s : strings) {
			sb.append(s);
			sb.append(separator);
		}

		sb.setLength(sb.length() - 1);

		return sb.toString();
	}

	public static String replace(String s, String oldSub, String newSub) {
		if ((s == null) || (oldSub == null) || oldSub.isEmpty()) {
			return s;
		}

		if (newSub == null) {
			newSub = "";
		}

		int y = s.indexOf(oldSub);

		if (y < 0) {
			return s;
		}

		StringBuilder sb = new StringBuilder();

		int length = oldSub.length();
		int x = 0;

		while (x <= y) {
			sb.append(s.substring(x, y));
			sb.append(newSub);

			x = y + length;
			y = s.indexOf(oldSub, x);
		}

		sb.append(s.substring(x));

		return sb.toString();
	}

	public static String[] split(String s, char delimiter) {
		s = s.trim();

		if (s.isEmpty()) {
			return _emptyStringArray;
		}

		List<String> values = new ArrayList<>();

		int offset = 0;
		int pos = s.indexOf(delimiter, offset);

		while (pos != -1) {
			values.add(s.substring(offset, pos));

			offset = pos + 1;
			pos = s.indexOf(delimiter, offset);
		}

		if (offset < s.length()) {
			values.add(s.substring(offset));
		}

		return values.toArray(new String[values.size()]);
	}

	private static final char[] _HEX_DIGITS = {
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
		'e', 'f'
	};

	private static final String[] _emptyStringArray = new String[0];

}