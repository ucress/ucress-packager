@echo off
@setlocal

set RUN_DIR=%cd%

if exist "%cd%\bin\bootstrap.jar" goto okExec
cd ..

if exist "%cd%\bin\bootstrap.jar" goto okExec
echo "Can not find bootstrap.jar"
goto end

:okExec
set APP_HOME=%cd%
echo JAVA_HOME : "%JAVA_HOME%"
echo APP_HOME  : "%APP_HOME%"
"%JAVA_HOME%\bin\java.exe" "-Dapp.home=%APP_HOME%" -classpath "bin\bootstrap.jar" com.ucress.loader.Launcher ${mainClass}

:end
cd "%RUN_DIR%"
@endlocal
