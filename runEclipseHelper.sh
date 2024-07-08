#!/bin/bash
# This script is runs the EclipseHelper annotion processor on the given project

# skip execution if we are in the root dir
if [[ -f "$(pwd)/runEclipseHelper.sh" ]]; then
	echo "skipping execution in the root directory"
	exit 0
fi

if [[ -z $JAVA8 ]]; then
	echo "JAVA8 environment variable not set. Please set it to the path of a Java 8 java executable"
	exit 1
fi

if [[ -z $KNIP_EXTERNALS_US ]]; then
	echo "KNIP_EXTERNALS_US environment variable not set. Please set it to the path of the knip-externals update site directory"
	exit 1
fi


outputDir=$1

# find scijava_common.jar
scijavaCommonJar=$(find "$KNIP_EXTERNALS_US" -name 'scijava-common_*.jar' | head -n 1)
$JAVA8 -Dscijava.log.level=debug -classpath "$scijavaCommonJar:$outputDir" org.scijava.annotations.EclipseHelper

# if there are scijava plugin annotations, this file will be created
scijavaPluginFile="${outputDir}/META-INF/json/org.scijava.plugin.Plugin"

if [[ -f $scijavaPluginFile ]]; then
	mkdir -p "$outputDir/../META-INF/json/"
	mv "$scijavaPluginFile" "$outputDir/../META-INF/json/org.scijava.plugin.Plugin"
	echo "Scijava plugin file detected, moving it to the META-INF directory"
fi