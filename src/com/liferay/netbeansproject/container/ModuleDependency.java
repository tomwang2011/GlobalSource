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

package com.liferay.netbeansproject.container;

import java.nio.file.Path;

/**
 * @author Tom Wang
 */
public class ModuleDependency {

	public ModuleDependency(Path moduleRelativePath, boolean test) {
		_moduleRelativePath = moduleRelativePath;
		_test = test;
	}

	public Path getModuleRelativePath() {
		return _moduleRelativePath;
	}

	public boolean isTest() {
		return _test;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("{moduleRelativePath=");
		sb.append(_moduleRelativePath);
		sb.append(", test=");
		sb.append(_test);

		return sb.toString();
	}

	private final Path _moduleRelativePath;
	private final boolean _test;

}