#!/bin/sh
set -e
rm -rf out
mkdir -p out releases
javac --release 17 -encoding UTF-8 -d out src/ru/leti/toposort/*.java
jar cfe releases/toposort-visualizer-release.jar ru.leti.toposort.Main -C out .
echo "Build complete: releases/toposort-visualizer-release.jar"
