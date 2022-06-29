:: Set variables
SET APP_NAME_SLUG=saasy-ml
SET PROJECT_DIR=C:\Users\VST\git\vspace\opssat\opssat-saasy-ml\src\saasy-ml-sepp
SET NMF_PROJECT_DIR=C:\Users\VST\git\vspace\opssat\nanosat-mo-framework\sdk\examples\space\saasy-ml
SET NMF_SDK_PACKAGE_DIR=C:\Users\VST\git\vspace\opssat\nanosat-mo-framework\sdk\sdk-package

:: Set run flag variable in case we want to build and run
SET RUN_FLAG=%1
if "%~1"=="" SET RUN_FLAG=0

:: Copy the NMF App to the NMF project dir
XCOPY %PROJECT_DIR%\ %NMF_PROJECT_DIR%\ /E /C /R /Y

:: Build the app
CD %NMF_PROJECT_DIR%
CALL mvn clean install

:: Build the sdk package
CD %NMF_SDK_PACKAGE_DIR%
CALL mvn clean install

:: Return to the app repo directory and copy the config file
:: CD %PROJECT_DIR%
:: COPY conf\config.properties %NMF_SDK_PACKAGE_DIR%\target\nmf-sdk-2.1.0-SNAPSHOT\home\%APP_NAME_SLUG%\config.properties
:: COPY conf\datapool.xml %NMF_SDK_PACKAGE_DIR%\target\nmf-sdk-2.1.0-SNAPSHOT\home\%APP_NAME_SLUG%\datapool.xml

:: Start Supervisor
if %RUN_FLAG%==1 START "NMF Supervisor" /D %NMF_SDK_PACKAGE_DIR%\target\nmf-sdk-2.1.0-SNAPSHOT\home\nmf\nanosat-mo-supervisor-sim "nanosat-mo-supervisor-sim.bat"

:: Start Consumer Test Tool
if %RUN_FLAG%==1 START "Consumer Test Tool" /MIN /D %NMF_SDK_PACKAGE_DIR%\target\nmf-sdk-2.1.0-SNAPSHOT\home\nmf\consumer-test-tool "consumer-test-tool.bat"