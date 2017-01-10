
The files here are in OWL a Web Ontology Language and are used to create example databases.

Note:

The files contain an import statement with a hard-coded path that likely needs to be changed to match your system.

Import(<file:/home/shackle/Demo-zeid-9-Jan-2017/Demo/OWL/DemoKittingWorkstationClasses.owl>)

The database can be generated with commands of the form:

java -jar /usr/local/jar/owl2gdb/owl2gdb-1.0-jar-with-dependencies.jar -o /usr/local/zeid/github/usnistgov/iora/_build/owl/FanucWorkstationInstance.owl -g /usr/local/zeid/workspace/neo4j-instances/ports/7474

> java -jar /usr/local/jar/owl2gdb/owl2gdb-1.0-jar-with-dependencies.jar -o /usr/local/zeid/github/usnistgov/iora/_build/owl/MotomanWorkstationInstance.owl -g /usr/local/zeid/workspace/neo4j-instances/ports/7476

Again hard-coded paths need to be adjusted for your system.

