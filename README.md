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



