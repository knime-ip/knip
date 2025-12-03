#!/bin/bash
# This script is runs the EclipseHelper annotion processor on the given project

# skip execution if we are in the root dir
if [[ -f "$(pwd)/runEclipseHelper.sh" ]]; then
	echo "skipping execution in the root directory"
	exit 0
fi

# Determine which Java to use
if [[ -n $JAVA8 ]]; then
	JAVA_CMD=$JAVA8
	echo "Using JAVA8 environment variable: $JAVA_CMD"
else
	# Fall back to java from PATH (e.g., JDK 21 on Jenkins)
	JAVA_CMD=$(which java)
	if [[ -z $JAVA_CMD ]]; then
		echo "No Java executable found. Please set JAVA8 or ensure java is in PATH."
		exit 0
	fi
	echo "JAVA8 not set. Using java from PATH: $JAVA_CMD"
fi

if [[ -z $KNIP_EXTERNALS_US ]]; then
	echo "KNIP_EXTERNALS_US environment variable not set. Skipping EclipseHelper annotation processor."
	echo "Please set it to the path of the knip-externals update site directory if scijava annotations are needed."
	exit 0
fi


outputDir=$1

# find scijava_common.jar
scijavaCommonJar=$(find "$KNIP_EXTERNALS_US" -name 'scijava-common_*.jar' | head -n 1)
$JAVA_CMD -Dscijava.log.level=debug -classpath "$scijavaCommonJar:$outputDir" org.scijava.annotations.EclipseHelper

# if there are scijava plugin annotations, this file will be created
scijavaPluginFile="${outputDir}/META-INF/json/org.scijava.plugin.Plugin"

if [[ -f $scijavaPluginFile ]]; then
	mkdir -p "$outputDir/../META-INF/json/"
	mv "$scijavaPluginFile" "$outputDir/../META-INF/json/org.scijava.plugin.Plugin"
	echo "Scijava plugin file detected, moving it to the META-INF directory"
fi