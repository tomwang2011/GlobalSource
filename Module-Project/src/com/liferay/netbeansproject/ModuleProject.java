/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.liferay.netbeansproject;

import com.liferay.netbeansproject.util.GradleUtil;
import com.liferay.netbeansproject.container.Module;
import com.liferay.netbeansproject.container.Module.ModuleDependency;
import com.liferay.netbeansproject.util.PropertiesUtil;
import com.liferay.netbeansproject.util.StringUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tom
 */
public class ModuleProject {
	public static void main(String[] args) throws IOException {
		Properties properties = PropertiesUtil.loadProperties(
			Paths.get("build.properties"));

		final Set<Path> blackListPaths = new HashSet<>();

		final StringBuilder dependenciesSB = new StringBuilder();

		// TODO 1) populate the blackListPaths from properties

		Files.walkFileTree(
			Paths.get(properties.getProperty("portal.dir")),
			new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(
					Path dir, BasicFileAttributes attrs) throws IOException {

					if (blackListPaths.contains(dir)) {
						return FileVisitResult.SKIP_SUBTREE;
					}

					if (Files.exists(dir.resolve("src"))) {
						try {
							Module module = ModuleProject.createModule(dir, dependenciesSB);
						}
						catch (Exception ex) {
							Logger.getLogger(ModuleProject.class.getName()).log(Level.SEVERE, null, ex);
						}

						return FileVisitResult.SKIP_SUBTREE;
					}

					return FileVisitResult.CONTINUE;
//					if (dir.endsWith(".gradle")) {
//						return FileVisitResult.SKIP_SUBTREE;
//					}
//
//					else if (dir.endsWith("sample")) {
//						return FileVisitResult.SKIP_SUBTREE;
//					}
//
//					else if (dir.endsWith("src")) {
//						Path parentPath = dir.getParent();
//
//						try {
//							Module module = ModuleProject.createModule(parentPath);
//
//							linkModuletoMap(module, parentPath.getParent());
//						}
//						catch (Exception ex) {
////							Logger.getLogger(PortalFileVisitor.class.getName()).log(
////								Level.SEVERE, null, ex);
//						}
//						return FileVisitResult.SKIP_SUBTREE;
//					}
//					return FileVisitResult.CONTINUE;
				}

			});

		System.out.println(dependenciesSB);
	}

	public static Module createModule(Path modulePath, StringBuilder dependenciesSB) throws Exception {
		Path testUnitPath = _resolveTestPath(modulePath, "unit");

		Path testIntegrationPath = _resolveTestPath(modulePath, "integration");

		Path sourceResourcePath = _resolveResourcePath(modulePath, "main");

		Path testUnitResourcePath = _resolveResourcePath(modulePath, "test");

		Path testIntegrationResourcePath =
			_resolveResourcePath(modulePath, "integration");

		List<ModuleDependency> projectDependencyList =
			GradleUtil.getModuleDependencies(modulePath);

		String dependencies = GradleUtil.getJarDependencies(modulePath);

		_dependenciesSB.append(dependencies);

		List<String> jarDependencyList = _formatDependency(dependencies);

		return new Module(
			modulePath, _resolveSourcePath(modulePath), sourceResourcePath, testUnitPath,
			testUnitResourcePath, testIntegrationPath,
			testIntegrationResourcePath, jarDependencyList,
			projectDependencyList);
	}

	public static void linkModuletoMap(Module module, Path path) {
		List<Module> moduleList = _projectMap.get(path);

		if(moduleList == null) {
			moduleList = new ArrayList<>();
		}

		moduleList.add(module);

		_projectMap.put(path, moduleList);
	}

	private static List<String> _formatDependency(String dependencies) {
		List<String> dependencyList = new ArrayList<>();

		for (String dependency : StringUtil.split(dependencies, '\n')) {
			if(!dependency.isEmpty()) {
				dependencyList.add(dependency.trim());
			}
		}
		return dependencyList;
	}

	private static Path _resolveResourcePath(Path modulePath, String type) {
		if (Files.exists(modulePath.resolve(
			"src" + File.separator + type + File.separator + "resources"))) {

			return modulePath.resolve(
				"src" + File.separator + type + File.separator + "resources");
		}

		return null;
	}

	private static Path _resolveSourcePath(Path modulePath) {

		Path mainJavaPath = modulePath.resolve(_mainJavaPath);

		if (Files.exists(mainJavaPath)) {
			return mainJavaPath;
		}
		
		if (Files.exists(modulePath.resolve(
			"src" + File.separator + "main"))) {

			return null;
		}

		return modulePath.resolve("src");
	}

	private static Path _resolveTestPath(Path modulePath, String type) {
		if (Files.exists(modulePath.resolve(
			"src" + File.separator + "test" + File.separator + "java")) &&
			type.equals("unit")) {

			return modulePath.resolve("src" + File.separator + "test");
		}
		else if (Files.exists(modulePath.resolve(
			"src" + File.separator + "testIntegration" + File.separator +
				"java")) && type.equals("integration")) {

			return modulePath.resolve("src" + File.separator + "testIntegration");
		}
		else if (Files.exists(modulePath.resolve(
			"test" + File.separator + type))) {

			return modulePath.resolve("test" + File.separator + type);
		}

		return null;
	}

	private static final Map<Path, List<Module>> _projectMap = new HashMap<>();

	private static final Path _mainJavaPath = Paths.get("src", "main", "java");
}
