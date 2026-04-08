@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "JAR_PATH=%SCRIPT_DIR%target\q-scout-for-spring-0.1.0-SNAPSHOT.jar"

if "%~1"=="" goto usage
if "%~2"=="" goto usage

if not exist "%JAR_PATH%" (
  echo Build artifact not found: %JAR_PATH%
  echo Run "mvn -q -DskipTests package" first.
  exit /b 1
)

java -Dloader.main=com.qscout.spring.cli.Main -cp "%JAR_PATH%" org.springframework.boot.loader.launch.PropertiesLauncher %*
exit /b %ERRORLEVEL%

:usage
echo Usage: run-cli.bat ^<project-root^> ^<output-dir^>
exit /b 1
