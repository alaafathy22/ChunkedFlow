@echo off
echo Building Parallel File Upload/Download System...

REM Create directories for compiled classes
mkdir target\classes
mkdir target\lib

REM Download dependencies using PowerShell
echo Downloading Spring Boot dependencies...
powershell -Command "$webClient = New-Object System.Net.WebClient; $webClient.DownloadFile('https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-web/2.7.14/spring-boot-starter-web-2.7.14.jar', 'target\lib\spring-boot-starter-web-2.7.14.jar'); $webClient.DownloadFile('https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-data-jpa/2.7.14/spring-boot-starter-data-jpa-2.7.14.jar', 'target\lib\spring-boot-starter-data-jpa-2.7.14.jar'); $webClient.DownloadFile('https://repo1.maven.org/maven2/com/h2database/h2/2.1.214/h2-2.1.214.jar', 'target\lib\h2-2.1.214.jar'); $webClient.DownloadFile('https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-thymeleaf/2.7.14/spring-boot-starter-thymeleaf-2.7.14.jar', 'target\lib\spring-boot-starter-thymeleaf-2.7.14.jar');"

REM Set up classpath
set CLASSPATH=target\classes
for %%i in (target\lib\*.jar) do call :append_classpath %%i
goto :compile

:append_classpath
set CLASSPATH=%CLASSPATH%;%1
goto :eof

:compile
REM Compile Java source files
echo Compiling Java files...
javac -d target\classes -cp %CLASSPATH% src\main\java\com\fileupload\*.java src\main\java\com\fileupload\model\*.java src\main\java\com\fileupload\controller\*.java src\main\java\com\fileupload\service\*.java src\main\java\com\fileupload\repository\*.java src\main\java\com\fileupload\exception\*.java

REM Copy resources
echo Copying resources...
xcopy /E /I src\main\resources target\classes

REM Create JAR file
echo Creating JAR file...
jar cfm parallel-file-uploader.jar src\main\java\META-INF\MANIFEST.MF -C target\classes .

echo Build completed successfully.
echo Run the application with: java -jar parallel-file-uploader.jar
