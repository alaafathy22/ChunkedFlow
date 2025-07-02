@REM Maven Start Up Batch script
@REM
@REM Required ENV vars:
@REM   JAVA_HOME - location of a JDK home dir
@REM
@REM Optional ENV vars
@REM   MAVEN_BATCH_ECHO - set to 'on' to enable the echoing of the batch commands
@REM   MAVEN_BATCH_PAUSE - set to 'on' to wait for a keystroke before ending
@REM

@echo off
if "%MAVEN_BATCH_ECHO%" == "on" echo %MAVEN_BATCH_ECHO%

set MAVEN_CMD_LINE_ARGS=%*

set DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar

@REM Extension to allow automatically downloading the maven-wrapper.jar.
if not exist %USERPROFILE%\.m2\wrapper\maven-wrapper.jar (
    echo Downloading Maven Wrapper...
    powershell -Command "(New-Object Net.WebClient).DownloadFile('%DOWNLOAD_URL%', '%USERPROFILE%\.m2\wrapper\maven-wrapper.jar')"
)

@REM Execute Maven
java -jar %USERPROFILE%\.m2\wrapper\maven-wrapper.jar %MAVEN_CMD_LINE_ARGS%
