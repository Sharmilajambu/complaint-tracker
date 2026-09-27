@echo off
cd /d "%~dp0"
if not exist out mkdir out
echo Compiling...
javac -cp "lib\*" -d out src\main\java\Main.java src\main\java\db\*.java src\main\java\handler\*.java src\main\java\model\*.java src\main\java\service\*.java src\main\java\util\*.java
if errorlevel 1 (
  echo Compile failed.
  exit /b 1
)
echo Done. Run run.bat to start the server.
