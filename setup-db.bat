@echo off
cd /d "%~dp0"
set MYSQL="C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if not exist %MYSQL% (
  echo MySQL not found at %MYSQL%
  echo Edit setup-db.bat with your mysql.exe path, then run again.
  exit /b 1
)
echo This will create the complaint_tracker database and tables.
set /p DBPASS="Enter MySQL root password: "
%MYSQL% -u root -p"%DBPASS%" < schema.sql
if errorlevel 1 (
  echo Database setup failed. Check your password and that MySQL is running.
  exit /b 1
)
echo Database ready. Update config.properties with the same password, then run run.bat
