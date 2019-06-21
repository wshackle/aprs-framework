


@REM set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_31\
@REM set PATH=%JAVA_HOME%\bin;%PATH%
@REM set PATH=%PATH%;"C:\Program Files\NetBeans 8.0.2\java\maven\bin";
@REM mvn package


@REM set OLDDIR=%CD%
SET mypath=%~dp0
@REM cd %mypath%


java -jar %mypath%\aprs-framework-1.9.1-SNAPSHOT.jar  %*
@REM chdir /d %OLDDIR% 
@REM restore current directory
