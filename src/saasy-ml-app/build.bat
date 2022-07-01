:: Set variables
SET PROJECT_DIR=C:\Users\VST\git\vspace\opssat\saasy-ml\opssat-saasy-ml\src\saasy-ml-app
SET NMF_SDK_PACKAGE_DIR=C:\Users\VST\git\vspace\opssat\saasy-ml\opssat-saasy-ml-nanosat-mo-framework\sdk\sdk-package

:: Set run flag variable in case we want to build and run
SET RUN_FLAG=%1
if "%~1"=="" SET RUN_FLAG=0

:: Build the app
CD %PROJECT_DIR%
CALL mvn clean install

:: Build the sdk package
CD %NMF_SDK_PACKAGE_DIR%
CALL mvn clean install

:: Start Supervisor
if %RUN_FLAG%==1 START "NMF Supervisor" /D %NMF_SDK_PACKAGE_DIR%\target\nmf-sdk-2.1.0-SNAPSHOT\home\nmf\nanosat-mo-supervisor-sim "nanosat-mo-supervisor-sim.bat"

:: Start Consumer Test Tool
if %RUN_FLAG%==1 START "Consumer Test Tool" /MIN /D %NMF_SDK_PACKAGE_DIR%\target\nmf-sdk-2.1.0-SNAPSHOT\home\nmf\consumer-test-tool "consumer-test-tool.bat"