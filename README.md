# Liferay-Source-Netbeans-Project-Builder

Create a build-ext.properties file and fill in the following settings: 

1. Set portal.dirs to your local liferay codebase. It can be a comma separated list.

2. Set the project.dir to your desired location for generated project files

3. Set jdk8.home to your jdk8 home. In case your default jdk is jdk8 already, it is optional.

Run ant build for a clean rebuild, run ant add (or just ant, add is the default target) for an increment build.