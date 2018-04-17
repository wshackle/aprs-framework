# aprs-framework

This source code provides a framework for the "Agility Performance of Robotic Systems"
project.

It is currently hosted on GitHub at https://github.com/wshackle/aprs-framework

Overview
========

![Overview of Project Modules](/Diagrams/overview.png?raw=true)


It is composed of the following modules each with its own directory and .JInternalFrame.

 * PDDL Planner Module -- copies a PDDL domain and problem file to remote host where pddl planner executes via SSH and receives the output to pass on to executor or display for the local user.   
 * PDDL Executor Module -- receives a PDDL action solution file and generates CRCL programs for subsets of the list of actions waits for programs to be completed before generating CRCL for the next set of actions. Some commands are generated based on both the action and data
in the database.  
 * CRCL Client -- receives the CRCL program and sends the commands from the program one at a time to a CRCL server and monitors each commands status ( presently either the simulation or the fanuc servers )  
 * Fanuc CRCL server --- reads CRCL commands and provide CRCL status updates by forwarding information to/from the Fanuc provided COM interface.  
 * Simulation CRCL Server -- reads CRCL commands and provide CRCL status updates based on a simple simulation.  
 * SP Module -- Monitors data from the vision system and uses this data to update the database.  
 * Vision Simulation/view Module -- Allows the user to generate vision data by simply typing in positions of objects or dragging them, or to view the data currently being supplied by an external vision system  
 * Database Setup Module -- Allows the user to change/store database setup options such as the hostname and user credentials and swap between databases. Currently MySQL and Neo4J are supported. The type of database can be swapped at run-time in a way that all modules will be using the same database. Using JDBC it should be possible to support a larger set of possible databases.  


The language called "Canonical Robot Command Language" (CRCL) provides generic command and status definitions that implement the functionality of typical industrial robots without being specific either to the language of a plan that is being executed or to the language used by a robot controller that executes CRCL commands. It can be used with offline planners that create output to be stored in CRCL files or online where CRCL is communicated in both directions via TCP. CRCL commands and status could also be exchanged over TCP between an operator interface and a robot controller or proxy for a robot controller.

The programming language independent documentation and XML Schema files for 
validation are stored in the main crcl repository:  https://github.com/ros-industrial/crcl


Supervisor
-----

Multiple such system each with its own separate database, vision system(s), robot(s), PDDL planners and executors and
separate unrelated tasks to perform can be coordinated by a supervisor that allows them to all 
be started/stopped simultaneously or for robots to be transferred between the systems.

![Supervisor of two systems](/Diagrams/supervisor_over_fanuc_motoman.png?raw=true)

The supervisor and each subsystem is implemented in a separate window.

![Supervisor Screenshot](/Screenshots/Multi_Aprs_Simulation_Screenshot.png?raw=true)


Build
-----


To build one needs:
  * JDK 1.8+ (http://www.oracle.com/technetwork/java/javase/downloads/index.html)  and
  * maven 3.5.0+ (https://maven.apache.org/download.cgi) 
  
Use the command:

    mvn -Pjar-with-depends package
    
OR 

  * An IDE bundled with both maven and a JDK such as Netbeans, IntelliJ, or Eclipse.
      * Netbeans will open a maven project with File -> Open Project just like Netbeans generated Ant projects.
      * IntelliJ and Eclipse both have options to import maven projects.
       (Be sure to enable the jar-with-depends maven profile to include depends in jar
       to make jar easier to run from command line. It is not required to run from IDE.)

Neo4J Database Install
-----


The most recent demo uses the Neo4J Community Edition version 2.3. This can be 
downloaded by going to

1.  Go to https://neo4j.com/
2.  Follow "Download" link on the top-right
3.  Follow "Other releases" link to https://neo4j.com/download/other-releases/
4.  Get the tar or zip file rather than the exe of dmg file for your operating system in the community column in the center at the bottom
of the page with the latest 2.3.?? version. Using the tar or zip file will make it
easier to setup multiple instances. 
5.  Extract the tar or zip into two separate directories. (One for Motoman, and one for Fanuc or their simulations)

        mkdir motoman-neo4j-database
        cd motoman-neo4j-database/
        tar -xzf ~/Downloads/neo4j-community-2.3.11-unix.tar.gz 
        cd ..
        mkdir fanuc-neo4j-database
        cd fanuc-neo4j-database/
        tar -xzf ~/Downloads/neo4j-community-2.3.11-unix.tar.gz 

6.  Change the ports for the motoman directory. By editing the text file "motoman-neo4j-database/neo4j-community-2.3.11/conf/neo4j-server.properties".
    
    Replace

        # http port (for all data, administrative, and UI access)
        org.neo4j.server.webserver.port=7474

    with

        # http port (for all data, administrative, and UI access)
        org.neo4j.server.webserver.port=7496

    Replace

        # https port (for all data, administrative, and UI access)
        org.neo4j.server.webserver.https.port=7473


    with

        # https port (for all data, administrative, and UI access)
        org.neo4j.server.webserver.https.port=7495


7.  Change the ports for the fanuc directory. By editing the text file "fanuc-neo4j-database/neo4j-community-2.3.11/conf/neo4j-server.properties".
    
    Replace

        # http port (for all data, administrative, and UI access)
        org.neo4j.server.webserver.port=7474

    with

        # http port (for all data, administrative, and UI access)
        org.neo4j.server.webserver.port=7494

    Replace

        # https port (for all data, administrative, and UI access)
        org.neo4j.server.webserver.https.port=7473


    with

        # https port (for all data, administrative, and UI access)
        org.neo4j.server.webserver.https.port=7493

9.  Extract the data from the zip file into the fanuc data directory

    
    cd ~/fanuc-neo4j-database/neo4j-community-2.3.11/data
    unzip ~/aprs-framework/neo4j-database-backup/fanucworkcell-28-feb-2017.zip 


10.  Extract the data from the zip file into the motoman data directory

    
    cd ~/motoman-neo4j-database/neo4j-community-2.3.11/data
    unzip ~/aprs-framework/neo4j-database-backup/fanucworkcell-28-feb-2017.zip 

11. Start the fanuc database.

    On Linux 


         cd ~/fanuc-neo4j-database/neo4j-community-2.3.11/
         bin/neo4j start

    On  Windows


         start neo4j-community-2.3.11-fanuc\bin\Neo4j.bat


12. Login to the web interface for the fanuc database by opening a web browser to http://localhost:7494/.
13. Use username "neo4j" and the password "neo4j" to login the first time.
14. You will be asked to change the password. Change it to "password".
15. Start the motoman database.

    On Linux


         cd ~/motoman-neo4j-database/neo4j-community-2.3.11/
         bin/neo4j start

    On  Windows


         start neo4j-community-2.3.11-motoman\bin\Neo4j.bat



16. Login to the web interface for the motoman database by opening a web browser to http://localhost:7496/.
17. Use username "neo4j" and the password "neo4j" to login the first time.
18. You will be asked to change the password. Change it to "password"
19. When done, one can stop both databases by running "bin/neo4j stop" from the each directory in Linux or just closing the command window(s) in Windows.



Run
---

The graphical user interface with the default launcher can be launched from within an IDE with the project 
open or from the command-line with the command:

For linux:

    ./run.sh

For Windows:

    run.bat

Or directly with:

    java -jar target/aprs-framework-1.1-SNAPSHOT-jar-with-dependencies.jar


To run with the simulated Fanuc/Motoman demo add the arguments "--openMulti example_settings/multiple_simulated_systems_settings/multisim9.csv"

eg.

    ./run.sh --openMulti example_settings/multiple_simulated_systems_settings/multisim9.csv 


Launcher
====

![Screenshot of Launcher](/Screenshots/LauncherAprs.png?raw=true)

When the the program starts without a command line argument to go directly to another
window, the user will be shown a small launcher window. 
To allow the user to open an existing single or multi workcell system, begin with a
new system or open the most recently used system if one exists.


Single Workcell System
====

Menu
----

The GUI for the single workcell system has three main menus on the main frame.

 * File -- Allows the properties files to be saved/loaded or exiting the program.
 * Startup -- Includes checkboxes to specify which modules should be started at
startup. Checking the box for any module that is not already started will start that
module.
 * Window -- Allows access to any window that has been created by moving that window
to the front.
 * Connections -- Allows on to check the status of the connection to the vision system or database.
 * Execute -- Allows PDDL Action plans to be started, paused or aborted.


Code for this should be in following subdirectory:

    src/main/java/aprs/framework/


Log/Error Display View
----------------------

![Screenshot of GUI with Error/Info Log Window](/Screenshots/Screenshot_Error_Info_Log.png?raw=true)

The default starting screen shows the log of text. Some messages may be useful 
in determining what activities have started, completed or have failed. Depending
 on what options were saved the last time the application was run the  application
may need to connect to an external database server, vision system or robot. Messages
here can help determine if those systems could be connected to as expected.

Code for this should be in following subdirectory:

    src/main/java/aprs/framework/logdisplay/  



Database Setup View
-------------------

![Screenshot of GUI with Database Setup Log Window](/Screenshots/Screenshot_Database_Setup.png?raw=true)

To get to this view:

  1. Check the box in the menu for Startup -> Database Setup (if not already checked).
  2. Select Database Setup from the Window menu.


Several other modules need to connect to a database to store/retrieve information
on the state of the robot, part designs, part locations etc.  Two database drivers
are currently built-in Neo4J and MySQL. An option to add custom drivers may be 
added in the future.  Data to connect to one of these databases may be entered here 
and saved locally to be reused on subsequent startups using the menu option File->Save Properties.. 
Even databases from the same vendor may have the same information stored in tables
 or graphs organized differently. To allow different schemas to be used the queries 
are stored in separate text files and can be edited here for testing. The queries
that are built into the application are in internal resource directories. Changes
here will not be preserved over a restart without recompiling the application.

One could have multiple databases setup to connect to. Each can be associated
with a different text file. Near the bottom of the page on can specify the text 
file to save or load from for easy switching.

Code for this should be in following subdirectory:

    src/main/java/aprs/framework/database/  



Explore Graph Database View
---------------------------

![Screenshot of GUI with Explore Graph Database Window](/Screenshots/Screenshot_Explore_Graph_Database.png?raw=true)

To get to this view:

  1. Check the box in the menu for Startup -> Explore Graph Database (if not already checked).
  2. Select Explore Graph Database from the Window menu.

NOTE: This window does not currently work with MySQL. The connection to the 
graph database needs to be setup from the Database Setup view previously discussed.

This view allows one to interactively explore the Graph Database. The entire set of
available node labels can be retrieved with the "Get Node Labels" button at the 
bottom left. These will be displayed on the list at the left. Selecting one will 
perform the query to return all nodes with that label.  Queries are shown in the text 
field at the top. Custom queries can be entered in the same text field. The results
of the query are shown in top table. Selecting a row in the table will select the node
that is the first query result. The selected node will then show  all relationships
from any other node to that node in the middle table and from that node to any other node
in the bottom table. Selecting any relationship in the middle or bottom table and
clicking the corresponding "Go To" button selects the other node in the relationship
and updates all three tables. In this way one can go from node to node to node following
any relationship into for from any other node.

Code for this should be in following subdirectory:

    src/main/java/aprs/framework/database/explore/  



 Object 2D/View Simulate
------------------------

![Screenshot of GUI with Object 2D View/Simulate Window](/Screenshots/Screenshot_Object_2D_View_Simulate.png?raw=true)

To get to this view:

  1. Check the box in the menu for Startup -> Object 2D View/Simulate (if not already checked).
  2. Select Object 2D View/Simulate from the Window menu.


This view allows one to simulate an Object Identification and Localization system
or to visualize the data coming from an external real vision system. 

To simulate an object identification and localization
system.

  1. Check the simuated checkbox.
  2. Choose a TCP port for the simulator to bind to and provide data from. (This
should match the port in the [Object SP] Vision to Database view for the 
Vision Port field).
  3. Set the port text field.
  4. Check the connected checkbox to bind the port and start the server.

The data being produced can be changed by editing the table or by dragging 
objects in the display panel. When entering changes in the table be sure to press
enter to make changes take effect. When dragging objects in the display panel,
press the mouse while over the center of the first letter in the name of the 
object to start dragging it.

To view data coming from an external vision system:

  1. Uncheck the simuated checkbox and the connected checkbox.
  2. Set the TCP port and host name to match the location of the vision 
system.
  3. Check the connected checkbox to connect the port and begin updating the 
display.


Code for this should be in following subdirectory:

    src/main/java/aprs/framework/simview/  


[Object SP] Vision To Database
------------------------------

![Screenshot of GUI with  Vision To Database Window](/Screenshots/Screenshot_Vision_To_Database.png?raw=true)

To get to this view:

  1. Check the box in the menu for Startup -> Object SP (if not already checked).
  2. Select [Object SP] Vision To Database from the Window menu.

This view monitors and controls the transfer of information from the vision system into
the database. The host name or address and port number of the vision system can 
be set. Data coming directly from the vision system is displayed in the bottom
right table. This can be compared with data currently in the database displayed in
the table in the top right. Currently both the positions from the database and 
from the vision system are expected to be in the robot's coordinate system except that
the vision system is not providing a z value as objects are expected to be in a 
common plane just above the table. However a transform from vision to robot coordinates
can be edited in the table in the lower left. The vision system may see multiple parts of the
same type and then assign them the same name. This is noted in the repeats column of
the table. If the checkbox labeled add repeat counts to database names. As highlighted
in the screenshot two large_gear objects were reported in the same frame. The position
for the second large gear was used to update the position of "large_gear_2" in the
database. Work is being done on a more sophisticated way of mapping vision names to 
database entries.

Code for this should be in following subdirectory:

    src/main/java/aprs/framework/spvision/


PDDL Planner
------------

![Screenshot of GUI with  PDDL Planner Window](/Screenshots/Screenshot_PDDL_Planner.png?raw=true)

To get to this view:

  1. Check the box in the menu for Startup -> PDDL Planner (if not already checked).
  2. Select PDDL Planner from the Window menu.

Planning Domain Definition Language(PDDL) is a way of defining both a class of problems in the 
domain file as well as a particular initial and goal state in the problem file. The actual planning
code is part of a different project. (See  https://github.com/usnistgov/crac , the 
example domain used here came from https://github.com/usnistgov/iora. )  This module is 
only replonsible for allowing the input domain and problem files to be selected and passing them to 
an external program, retrieving and displaying the result and passing the output to
the PDDL Executor that will send commands to move the robot. If the planner can not
be installed locally it can be run remotely through SSH. 


Code for this should be in following subdirectory:

     src/main/java/aprs/framework/pddl/planner/  


PDDL Executor
------------

![Screenshot of GUI with  PDDL Planner Window](/Screenshots/Screenshot_PDDL_Executor.png?raw=true)

To get to this view:

  1. Check the box in the menu for Startup -> PDDL Executor (if not already checked).
  2. Select PDDL Actions to CRCL (Executor) from the Window menu.

The PDDL Planner previously discussed outputs a list of actions in a text file. This is
parsed by the executor and displayed in the top table. PDDL actions are converted into
CRCL. The Canonical Robot Control Language is an XML based format with commands more
directly executable by the robot. Some PDDL actions will result in multiple CRCL commands
while others will not be directly linked to any CRCL command but rather change the 
executors state indirectly affecting the CRCL generated related to future actions.
When possible the CRCL commands from multiple PDDL actions are combined into a single 
CRCL program. The information stored in the PDDL action is typically not enough to
generate the CRCL commands. For example the take_part action includes the name of the
part to take but not its current position. In order to generate the CRCL MoveTo command to move 
the robot to a position where the part can be grabbed the executor consults the database
to obtain a position associated with that part. The CRCL generated is displayed in the
bottom table. The continue index marks the PDDL action where CRCL generation will
continue after the robot has successfully executed the most recently generated CRCL program.
Some options can be set from within the executor to control how some CRCL is generated.
For example when the robot needs to look for a part, the arm needs to be moved to a
position where it will not occlude the vision system.  This is specified in the
lookForXYZ option in the options table in the middle.


Code for this should be in following subdirectory:

     src/main/java/aprs/framework/pddl/executor/  


CRCL Client
-----------

![Screenshot of GUI with  CRCL Client Window](/Screenshots/Screenshot_CRCL_Client.png?raw=true)

To get to this view:

  1. Check the box in the menu for Startup -> Robot CRCL Client Gui (if not already checked).
  2. Select the entry starting with "CRCL Client" from the Window menu. (The menu also shows the status of the client and
will therefore vary as the system state varies) 
 
The CRCL client is the same client which can be run stand-alone from the project on 
https://github.com/usnistgov/crcl   under the tools/java/crcl4java/ directory. It
can be used to control and monitor a robot with a CRCL interface. It maybe necessary
the goto the connection tab to set hostname and port of the CRCL server to connect to
and click the connect button to complete the connection. The program tab displays each 
line of the program as it is run. 

CRCL Simulation Server
----------------------

![Screenshot of GUI with  CRCL Simulation Server Window](/Screenshots/Screenshot_CRCL_Simulation_Server.png?raw=true)

To get to this view:

  1. Check the box in the menu for Startup -> Robot CRCL SimServer (if not already checked).
  2. Select CRCL Simulation Server from the Window menu. 
 
The CRCL simulation server is the same application  which can be run stand-alone from the project on 
https://github.com/usnistgov/crcl   under the tools/java/crcl4java/ directory. It provides
a simulated system that accepts commands and produces status that allows testing of 
the CRCL client and indirectly any higher level modules. The port should be the same
as set on the connection tab of the client.

CRCL WebApp
-----------

![Screenshot of Browser with CRCL WebApp](/Screenshots/Screenshot_CRCL_WebApp.png?raw=true)

To get to this view:

  1. Check the box in the menu for Startup -> CRCL Web App (if not already checked).
 
The CRCL Web App provides a CRCL client through a a web application. It is the same
application that can be started from the project on 
https://github.com/usnistgov/crcl   under the tools/java/crcl4java/crcl-vaadin-webapp directory.
Once it is running one can connect to it using other devices such as mobile devices that
can not run the swing application.


























