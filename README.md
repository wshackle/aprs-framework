# aprs-framework

This source code provides a framework for the "Agility Performance of Robotic Systems"
project.

It is currently hosted on GitHub at https://github.com/wshackle/aprs-framework


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


Build
-----


To build one needs:
  * JDK 1.8+ (http://www.oracle.com/technetwork/java/javase/downloads/index.html)  and
  * maven 3.0.5+ (https://maven.apache.org/download.cgi) 
  
Use the command:

    mvn package
    
OR 

  * An IDE bundled with both maven and a JDK such as Netbeans, IntelliJ, or Eclipse.
      * Netbeans will open a maven project with File -> Open Project just like Netbeans generated Ant projects.
      * IntelliJ and Eclipse both have options to import maven projects.



Run
---

The graphical user interface can be launched from within an IDE with the project 
open or from the command-line with the command:

For linux:

    ./run.sh

For Windows:

    run.bat

Menu
====



Views
=====

Log/Error Display View
----------------------

![Screenshot of GUI with Error/Info Log Window](/Screenshots/Screenshot_Error_Info_Log.png?raw=true)

The default starting screen shows the log of text. Some messages may be useful 
in determining what activities have started, completed or have failed. Depending
 on what options were saved the last time the application was run the  application
may need to connect to an external database server, vision system or robot. Messages
here can help determine if those systems could be connected to as expected.

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


[Object SP] Vision To Database
------------------------------

![Screenshot of GUI with Object 2D View/Simulate Window](/Screenshots/Screenshot_Object_2D_View_Simulate.png?raw=true)

To get to this view:

  1. Check the box in the menu for Startup -> Object 2D View/Simulate (if not already checked).
  2. Select Object 2D View/Simulate from the Window menu.










