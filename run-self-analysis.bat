@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%."
set "OUTPUT_DIR=%SCRIPT_DIR%sample-output\self-analysis"

if not "%~1"=="" set "OUTPUT_DIR=%~1"

call "%SCRIPT_DIR%run-cli.bat" "%PROJECT_ROOT%" "%OUTPUT_DIR%"
exit /b %ERRORLEVEL%
