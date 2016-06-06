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

import com.liferay.netbeansproject.resolvers.ProjectDependencyResolver;

/**
 * @author Tom Wang
 */
public class ModuleDependency {

	public ModuleDependency(String modulePath, boolean test) {
		_modulePath = modulePath;
		_test = test;
	}

	public Module getModule(
		ProjectDependencyResolver projectDependencyResolver) {

		return projectDependencyResolver.resolve(_modulePath);
	}

	public boolean isTest() {
		return _test;
	}

	private final String _modulePath;
	private final boolean _test;

}