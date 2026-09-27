@echo off
cd /d "%~dp0"
if not exist out\Main.class call compile.bat
if errorlevel 1 exit /b 1
echo Starting server...
java -cp "out;lib\*" Main
