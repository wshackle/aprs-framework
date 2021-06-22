
SET mypath=%~dp0

echo "mypath=" %mypath%

set setupfile=%mypath%\.lastAprsSetupFile.txt
set posmapfile=%mypath%\.lastAprsPosMapFile.txt

set settingsdir=%mypath%\example_settings\fanuc_motoman_multi_system_settings\

if exist %settingsdir% (
    echo Settings directory  %settingsdir% exists
) else (
   echo Settings directory  %settingsdir%  does NOT exist
   PAUSE
)

set openmultifile=%settingsdir%\fanucmotoman.csv
set launchfile=%settingsdir%\launch.txt


echo "setupfile="  %setupfile%
echo "posmafile=" %posmapfile%
echo "openmultifile="  %openmultifile%
echo "launchfile=" %launchfile%

set jarfile=%mypath%\bin\aprs-framework-1.9.1-SNAPSHOT.jar
echo "jarfile=" %jarfile%
dir %jarfile%

if exist %jarfile% (
    echo Jar file  %jarfile% exists
) else (
   echo Jar file  %jarfile%  does NOT exist
   PAUSE
)

set JAVA_HOME="C:\Program Files\AdoptOpenJDK\jdk-8.0.292.10-hotspot"
set JAVA_EXE=%JAVA_HOME%\bin\java.exe

if exist %JAVA_EXE% (
    echo JAVA_EXE file  %JAVA_EXE% exists
) else (
   echo JAVA_EXE file  %JAVA_EXE%  does NOT exist
   PAUSE
)

echo %JAVA_EXE%  -Dcrcl.schemaChangeTime=-1 -Dcrcl.resourceChangeTime=-1 -DaprsLastMultiSystemSetupFile=%setupfile%  -DaprsLastMultiSystemPosMapFile=%posmapfile%   -Dcrcl.user.home=%mypath%\netbeans_run_user_home -Daprs.user.home=%mypath%\netbeans_run_user_home -Duser.home=%mypath%\netbeans_run_user_home   -jar %jarfile% --openMulti %openmultifile% %launchfile% %* 


%JAVA_EXE%  -Dcrcl.schemaChangeTime=-1 -Dcrcl.resourceChangeTime=-1 -DaprsLastMultiSystemSetupFile=%setupfile%  -DaprsLastMultiSystemPosMapFile=%posmapfile%   -Dcrcl.user.home=%mypath%\netbeans_run_user_home -Daprs.user.home=%mypath%\netbeans_run_user_home -Duser.home=%mypath%\netbeans_run_user_home   -jar %jarfile% --openMulti %openmultifile% %launchfile% %* 

PAUSE



