package com.liferay.netbeansproject.gradle;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class GradleDependencyExtractor {

	public static void main(String[] args) throws IOException {
		StringBuilder sb = new StringBuilder("compile=");

		sb.append(args[0]);
		sb.append("\ncompileTest=");
		sb.append(args[1]);

		Files.write(
			Paths.get("dependency.properties"), Arrays.asList(sb),
			Charset.defaultCharset());
	}

}