
SET mypath=%~dp0

echo "mypath=" %mypath%

set setupfile=%mypath%\.lastAprsSetupFile.txt
set posmapfile=%mypath%\.lastAprsPosMapFile.txt

set openmultifile=%mypath%\aprs-framework\example_settings\fanuc_motoman_multi_system_settings\fanucmotoman.csv
set launchfile=%mypath%\aprs-framework\example_settings\fanuc_motoman_multi_system_settings\launch.txt


echo "setupfile="  %setupfile%
echo "posmafile=" %posmapfile%
echo "openmultifile="  %openmultifile%
echo "launchfile=" %launchfile%

set jarfile=%mypath%\aprs-framework-1.9.1-SNAPSHOT-install\aprs-framework\bin\aprs-framework-1.9.1-SNAPSHOT.jar
echo "jarfile=" %jarfile%
dir %jarfile%

if exist %jarfile% (
    echo Jar file  %jarfile% exists
) else (
   echo Jar file  %jarfile%  does NOT exist
   PAUSE
)

#set startclass=aprs.launcher.LauncherAprsJFrame



echo "C:\Program Files\AdoptOpenJDK\jdk-8.0.212.04-hotspot\bin\java.exe"  -Dcrcl.schemaChangeTime=-1 -Dcrcl.resourceChangeTime=-1 -DaprsLastMultiSystemSetupFile=%setupfile%  -DaprsLastMultiSystemPosMapFile=%posmapfile%   -Dcrcl.user.home=%mypath%\aprs-framework\netbeans_run_user_home -Daprs.user.home=%mypath%\aprs-framework\netbeans_run_user_home -Duser.home=%mypath%\aprs-framework\aprs-framework\netbeans_run_user_home   -jar %jarfile% --openMulti %openmultifile% %launchfile% %* 


"C:\Program Files\AdoptOpenJDK\jdk-8.0.212.04-hotspot\bin\java.exe"  -Dcrcl.schemaChangeTime=-1 -Dcrcl.resourceChangeTime=-1 -DaprsLastMultiSystemSetupFile=%setupfile%  -DaprsLastMultiSystemPosMapFile=%posmapfile%   -Dcrcl.user.home=%mypath%\aprs-framework\netbeans_run_user_home -Daprs.user.home=%mypath%\aprs-framework\netbeans_run_user_home -Duser.home=%mypath%\aprs-framework\aprs-framework\netbeans_run_user_home   -jar %jarfile% --openMulti %openmultifile% %launchfile% %* 

PAUSE



