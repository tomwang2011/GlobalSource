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

package com.liferay.netbeansproject;

import com.liferay.netbeansproject.util.PropertiesUtil;
import com.liferay.netbeansproject.util.StringUtil;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 *
 * @author Tom Wang
 */
public class ModuleProject {

	public static void main(String[] args) throws Exception, IOException {
		Properties properties = PropertiesUtil.loadProperties(
			Paths.get("build.properties"));

		final Set<String> blackListDirs = new HashSet<>();

		final String projectDir = properties.getProperty("project.dir");

		blackListDirs.addAll(
			Arrays.asList(
				StringUtil.split(
					properties.getProperty("blackListDirs"), ',')));

		_clean(projectDir);
	}

	private static void _clean(String projectDir) throws IOException {
		Files.walkFileTree(
			Paths.get(projectDir), new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(
				Path file, BasicFileAttributes attrs) throws IOException {

				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc)
				throws IOException {

				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}
}