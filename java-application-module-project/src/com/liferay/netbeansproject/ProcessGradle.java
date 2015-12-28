package com.liferay.netbeansproject;

import com.liferay.netbeansproject.util.ArgumentsUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author tom
 */
public class ProcessGradle {
	public static void main(String[] args) throws Exception {
		Map<String, String> arguments = ArgumentsUtil.parseArguments(args);

		Path portalDirPath = Paths.get(arguments.get("portal.dir"));
		Path projectDirPath = Paths.get(arguments.get("project.dir"));

		Path gradlewPath = portalDirPath.resolve("gradlew");

		List<String> gradleTask = new ArrayList<>();

		gradleTask.add(gradlewPath.toString());
		gradleTask.add("--init-script=dependency.gradle");
		gradleTask.add("printDependencies");
		gradleTask.add("-p");

		Path moduleDirPath = portalDirPath.resolve("modules");

		gradleTask.add(moduleDirPath.toString());

		Path dependenciesDirPath = projectDirPath.resolve("dependencies");

		Files.createDirectories(dependenciesDirPath);

		gradleTask.add("-PdependencyDirectory=" + dependenciesDirPath);

		ProcessBuilder processBuilder = new ProcessBuilder(gradleTask);

		Process process = processBuilder.start();

		int exitCode = process.waitFor();

		if (exitCode != 0) {
			throw new IOException(
				"Process " + processBuilder.command() + " failed with " +
					exitCode);
		}
	}
}
