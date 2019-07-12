


@REM set OLDDIR=%CD%
SET mypath=%~dp0
@REM cd %mypath%\aprs-framework


start %mypath%\neo4j-community-2.3.11-motoman\bin\Neo4j.bat

echo %time%
timeout 15 > NUL
echo %time%

start %mypath%\neo4j-community-2.3.11-fanuc\bin\Neo4j.bat

echo %time%
timeout 15 > NUL
echo %time%

