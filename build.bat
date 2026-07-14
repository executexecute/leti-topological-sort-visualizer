@echo off
setlocal
if exist out rmdir /s /q out
mkdir out
javac --release 17 -encoding UTF-8 -d out src\ru\leti\toposort\*.java
if errorlevel 1 exit /b 1
if not exist releases mkdir releases
jar cfe releases\toposort-visualizer-release.jar ru.leti.toposort.Main -C out .
if errorlevel 1 exit /b 1
echo Build complete: releases\toposort-visualizer-release.jar
