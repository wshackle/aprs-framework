/*
 * This software is public domain software, however it is preferred
 * that the following disclaimers be attached.
 * Software Copywrite/Warranty Disclaimer
 * 
 * This software was developed at the National Institute of Standards and
 * Technology by employees of the Federal Government in the course of their
 * official duties. Pursuant to title 17 Section 105 of the United States
 * Code this software is not subject to copyright protection and is in the
 * public domain.
 * 
 * This software is experimental. NIST assumes no responsibility whatsoever 
 * for its use by other parties, and makes no guarantees, expressed or 
 * implied, about its quality, reliability, or any other characteristic. 
 * We would appreciate acknowledgement if the software is used. 
 * This software can be redistributed and/or modified freely provided 
 * that any derivative works bear some notice that they are derived from it, 
 * and any modified versions bear some notice that they have been modified.
 * 
 *  See http://www.copyright.gov/title17/92chap1.html#105
 * 
 */
package aprs.framework.database;

import aprs.framework.spvision.VisionToDBJPanel;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.ProtectionDomain;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class DbSetupBuilder {

    private DbType type = DbType.NEO4J;
    private String host = "localhost";
    private int port = 4000;
    private char[] passwd = new char[0];
    private String user = "neo4j";
    private String dbname = "";
    private boolean connected = false;
    private Map<DbQueryEnum, String> queriesMap= new EnumMap<DbQueryEnum, String>(DbQueryEnum.class);;

    {
        try {
            queriesMap.put(DbQueryEnum.GET_SINGLE_POSE, 
                    getStringResource("aprs/framework/database/neo4j/get_single_pose.txt"));
            queriesMap.put(DbQueryEnum.SET_SINGLE_POSE, 
                    getStringResource("aprs/framework/database/neo4j/set_single_pose.txt"));
        } catch (IOException ex) {
            Logger.getLogger(DbSetupBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
//    {
////        queriesMap = new EnumMap<DbQueryEnum, String>(DbQueryEnum.class);
//        queriesMap.put(DbQueryEnum.GET_SINGLE_POSE, "MATCH pointpath=(source { name:{1} } ) -[:hasPhysicalLocation_RefObject]-> (n) -[r2:hasPoseLocation_Pose] ->(pose) -  [r1:hasPose_Point] -> (p:Point),\n"
//                + "xaxispath= pose - [r3:hasPose_XAxis] -> (xaxis:Vector),\n"
//                + "zaxispath= pose - [r4:hasPose_ZAxis] -> (zaxis:Vector)\n"
//                + "return source.name as name,p.hasPoint_X as x,p.hasPoint_Y as y,p.hasPoint_Z as z, xaxis.hasVector_I as vxi,xaxis.hasVector_J as vxj,xaxis.hasVector_K as vxk, zaxis.hasVector_I as vzi,zaxis.hasVector_J as vzj,zaxis.hasVector_K as vzk");
//        queriesMap.put(DbQueryEnum.SET_SINGLE_POSE,
//                "MERGE (thing:SolidObject { name:{1} } )\n"
//                + "merge (thing) - [:hasPhysicalLocation_RefObject] -> (pl:PhysicalLocation)\n"
//                + "merge (pl) - [:hasPoseLocation_Pose] -> (pose:PoseLocation)\n"
//                + "merge (pose) - [:hasPose_Point] -> (pt:Point)\n"
//                + "merge (pose) - [:hasPose_XAxis] -> (xaxis:Vector)\n"
//                + "merge (pose) - [:hasPose_ZAxis] -> (zaxis:Vector)\n"
//                + "set pt.hasPoint_X= {2},pt.hasPoint_Y= {3},pt.hasPoint_Z= {4}\n"
//                + "set xaxis.hasVector_I={5}, xaxis.hasVector_J={6}, xaxis.hasVector_K={7}\n"
//                + "set zaxis.hasVector_I={8}, zaxis.hasVector_J={9}, zaxis.hasVector_K={10}"
//        );
//    }
    
    private static String getStringResource(String name) throws IOException {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        StringBuilder sb = new StringBuilder();
        try(BufferedReader br = new BufferedReader(new InputStreamReader(cl.getResourceAsStream(name),"UTF-8"))){
           String line=null;
           while(null != (line = br.readLine())) {
                sb.append(line);
                sb.append(System.lineSeparator());
            }
        }
        return sb.toString();
    }
    
    private static class DbSetupInternal implements DbSetup {

        final DbType type;
        final String host;
        final int port;
        final char[] passwd;
        final String user;
        final String dbname;
        final boolean connected;
        final Map<DbQueryEnum,String> queriesMap;

        private DbSetupInternal(
                DbType type, 
                String host, 
                int port, 
                char[] passwd, 
                String user, 
                String dbname, 
                boolean connected,
                Map<DbQueryEnum,String> queriesMap
                ) {
            this.type = type;
            this.host = host;
            this.port = port;
            this.passwd = passwd;
            this.user = user;
            this.dbname = dbname;
            this.connected = connected;
            this.queriesMap = queriesMap;
        }

        @Override
        public DbType getDbType() {
            return type;
        }

        @Override
        public String getHost() {
            return host;
        }

        @Override
        public int getPort() {
            return port;
        }

        @Override
        public String getDbName() {
            return dbname;
        }

        @Override
        public String getDbUser() {
            return user;
        }

        @Override
        public char[] getDbPassword() {
            return passwd;
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public Map<DbQueryEnum, String> getQueriesMap() {
            return queriesMap;
        }
        
    }

    public DbSetupBuilder setup(DbSetup setup) {
        type = setup.getDbType();
        host = setup.getHost();
        dbname = setup.getDbName();
        user = setup.getDbUser();
        connected = setup.isConnected();
        port = setup.getPort();
        passwd = setup.getDbPassword();
        return this;
    }

    public DbSetup build() {
        return new DbSetupInternal(type, host, port, passwd, user, dbname, connected,queriesMap);
    }

    public DbSetupBuilder type(DbType type) {
        if (null != type) {
            this.type = type;
        }
        return this;
    }

    public DbSetupBuilder host(String host) {
        if (null != host) {
            this.host = host;
        }
        return this;
    }

    public DbSetupBuilder port(int port) {
        this.port = port;
        return this;
    }

    public DbSetupBuilder passwd(char[] passwd) {
        if (null != passwd) {
            this.passwd = passwd;
        }
        return this;
    }

    public DbSetupBuilder user(String user) {
        if (null != user) {
            this.user = user;
        }
        return this;
    }

    public DbSetupBuilder dbname(String dbname) {
        if (null != dbname) {
            this.dbname = dbname;
        }
        return this;
    }

    public DbSetupBuilder connected(boolean connected) {
        this.connected = connected;
        return this;
    }

    public DbSetupBuilder queriesMap(Map<DbQueryEnum,String> queriesMap) {
        this.queriesMap = queriesMap;
        return this;
    }
    
    private DbSetupBuilder updateFromArgs(Map<String, String> _argsMap) {

        DbSetupBuilder builder = this;

        String argsMapDbTypeString = _argsMap.get("--dbtype");
        DbSetup curSetup = null;
        DbType dbtype = builder.type;
        if (argsMapDbTypeString != null && argsMapDbTypeString.length() > 0) {
            dbtype = DbType.valueOf(argsMapDbTypeString);
            builder = builder.type(dbtype);
        }
        String dbSpecificHost = _argsMap.get(dbtype + ".host");
        if (null != dbSpecificHost) {
//                this.jTextFieldDBHost.setText(dbSpecificHost);
            builder = builder.host(dbSpecificHost);
            host = dbSpecificHost;
        } else {
            host = _argsMap.get("--dbhost");
            if (null != host && host.length() > 0) {
                builder = builder.host(host);
            }
        }
        String dbSpecificPort = _argsMap.get(dbtype + "." + host + ".port");
        if (null != dbSpecificPort) {
//                this.jTextFieldDBPort.setText(dbSpecificPort);
            port = Integer.parseInt(dbSpecificPort);
            builder = builder.port(port);
        } else {
            String argsMapPortString = _argsMap.get("--dbport");
            if (null != argsMapPortString && argsMapPortString.length() > 0) {
                try {
                    int port = Integer.parseInt(argsMapPortString);
                    builder = builder.port(port);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return updateFromArgs(_argsMap, dbtype, host, port);
    }

    private DbSetupBuilder updateFromArgs(Map<String, String> _argsMap, DbType dbtype, String host, int port) {
        DbSetupBuilder builder = this;
        String dbHostPort = String.format("%s.%s_%d", dbtype.toString(), host, port);
        String dbSpecificName = _argsMap.get(dbHostPort + ".name");
        if (null != dbSpecificName) {
            builder = builder.dbname(dbSpecificName);
        } else {

            String dbNameString = _argsMap.get("--dbname");
            if (null != dbNameString) {
                builder = builder.dbname(dbNameString);
            }
        }
        String dbSpecificUser = _argsMap.get(dbHostPort + ".user");
        if (null != dbSpecificUser) {
            builder = builder.user(dbSpecificUser);
        } else {
            String dbUserString = _argsMap.get("--dbuser");
            if (null != dbUserString) {
                builder = builder.user(dbUserString);
            }
        }
        String dbSpecificPasswd = _argsMap.get(dbHostPort + ".passwd");
        if (null != dbSpecificUser) {
            builder = builder.passwd(dbSpecificPasswd.toCharArray());
        } else {
            String dbPasswd = _argsMap.get("--dbpasswd");
            if (null != dbPasswd) {
                builder = builder.passwd(dbPasswd.toCharArray());
            }
        }
        return builder;
    }

    /**
     * Create an initialized builder from a properties file. It will just create
     * a default if the file does not exist or is empty.
     *
     * @param propertiesFile
     * @return
     */
    public static DbSetupBuilder loadFromPropertiesFile(File propertiesFile) {
        DbSetupBuilder builder = new DbSetupBuilder();
        if (null != propertiesFile && propertiesFile.exists()) {
            Properties props = new Properties();
            try (FileReader fr = new FileReader(propertiesFile)) {
                props.load(fr);
            } catch (IOException ex) {
                Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
            Map<String, String> argsMap = Main.getArgsMap();
            for (String propName : props.stringPropertyNames()) {
                argsMap.put(propName, props.getProperty(propName));
            }
            return builder.updateFromArgs(argsMap);
        }
        return builder;
    }

    /**
     * Create an initialized builder from the PropertiesFile potentially
     * ignoring values in the file in favor the arguments passed instead.
     *
     * @param propertiesFile
     * @param dbtype
     * @param host
     * @param port
     * @return
     */
    public static DbSetupBuilder loadFromPropertiesFile(File propertiesFile, DbType dbtype, String host, int port) {
        DbSetupBuilder builder = new DbSetupBuilder();
        if (null != propertiesFile && propertiesFile.exists()) {
            Properties props = new Properties();
            try (FileReader fr = new FileReader(propertiesFile)) {
                props.load(fr);
            } catch (IOException ex) {
                Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
            Map<String, String> argsMap = Main.getArgsMap();
            for (String propName : props.stringPropertyNames()) {
                argsMap.put(propName, props.getProperty(propName));
            }
            if (dbtype == null) {
                String argsMapDbTypeString = argsMap.get("--dbtype");
                if (argsMapDbTypeString != null && argsMapDbTypeString.length() < 1) {
                    dbtype = DbType.valueOf(argsMapDbTypeString);
                }
            }
            builder = builder.type(dbtype);
            if (host == null || host.length() < 1) {
                String dbSpecificHost = argsMap.get(dbtype + ".host");
                if (null != dbSpecificHost) {
//                this.jTextFieldDBHost.setText(dbSpecificHost);
                    builder = builder.host(dbSpecificHost);
                    host = dbSpecificHost;
                } else {
                    host = argsMap.get("--dbhost");
                    if (null != host && host.length() > 0) {
                        builder = builder.host(host);
                    }
                }
            } else {
                builder = builder.host(host);
            }
            if (port < 1) {
                String dbSpecificPort = argsMap.get(dbtype + "." + host + ".port");
                if (null != dbSpecificPort) {
//                this.jTextFieldDBPort.setText(dbSpecificPort);
                    port = Integer.parseInt(dbSpecificPort);
                    builder = builder.port(port);
                } else {
                    String argsMapPortString = argsMap.get("--dbport");
                    if (null != argsMapPortString && argsMapPortString.length() > 0) {
                        try {
                            port = Integer.parseInt(argsMapPortString);
                            builder = builder.port(port);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                builder = builder.port(port);
            }
            return builder.updateFromArgs(argsMap, dbtype, host, port);
        }
        return builder;
    }

    /**
     * Save the given setup to a properties file.
     * @param propertiesFile
     * @param setup
     */
    public static void savePropertiesFile(File propertiesFile, DbSetup setup) {
        savePropertiesFile(propertiesFile, setup, setup.getDbType(), setup.getHost(), setup.getPort());
    }

    /**
     * Save the given setup to a properties file, possibly replacing some setup
     * values with the passed arguments.
     * @param propertiesFile
     * @param setup
     * @param dbtype
     * @param host
     * @param port
     */
    public static void savePropertiesFile(File propertiesFile, DbSetup setup, DbType dbtype, String host, int port) {
        if (null != propertiesFile) {
            propertiesFile.getParentFile().mkdirs();
            Properties props = new Properties();
            if (propertiesFile.exists()) {
                try (FileReader fr = new FileReader(propertiesFile)) {
                    props.load(fr);
                } catch (IOException ex) {
                    Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            props.put("--dbtype", dbtype.toString());
            if (host == null) {
                host = setup.getHost();
            }
            props.put(dbtype + ".host", host);
            if (port < 1) {
                port = setup.getPort();
            }
            props.put(dbtype + "." + host + ".port", Integer.toString(port));
            String dbHostPort = String.format("%s.%s_%d", dbtype.toString(), host, port);
            props.put(dbHostPort + ".name", setup.getDbName());
            props.put(dbHostPort + ".user", setup.getDbUser());
            props.put(dbHostPort + ".passwd", new String(setup.getDbPassword()));
            try (FileWriter fw = new FileWriter(propertiesFile)) {
                props.store(fw, "");
            } catch (IOException ex) {
                Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public Connection connect() throws SQLException {
        return connect(this.build());
    }
    
    public static  Connection connect(DbSetup setup) throws SQLException {
        return setupConnection(setup.getDbType(),setup.getHost(), setup.getPort(), setup.getDbName(), setup.getDbUser(), new String(setup.getDbPassword()));
    }
    
    public static  Connection setupConnection(DbType dbtype, String host, int port, String db, String username, String password) throws SQLException {
        switch (dbtype) {
            case MYSQL:
//                useBatch = true;
                String mysql_url = "jdbc:mysql://" + host + ":" + port + "/" + db;
                System.out.println("Connection url = " + mysql_url);
                try {
                    Class mySqlDriverClass = Class.forName("com.mysql.jdbc.Driver");
                    System.out.println("neo4JDriverClass = " + mySqlDriverClass);
                    ProtectionDomain mySqlDriverClassProdectionDomain = mySqlDriverClass.getProtectionDomain();
                    System.out.println("mySqlDriverClassProdectionDomain = " + mySqlDriverClassProdectionDomain);
                } catch (ClassNotFoundException classNotFoundException) {
                    classNotFoundException.printStackTrace();
                }
                return DriverManager.getConnection(mysql_url, username, password);
               
            case NEO4J:
//                useBatch = false;
                Properties properties = new Properties();
                properties.put("user", username);
                properties.put("password", password);
                String neo4j_url = "jdbc:neo4j://" + host + ":" + port;
                System.out.println("Connection url = " + neo4j_url);
                try {
                    Class neo4JDriverClass = Class.forName("org.neo4j.jdbc.Driver");
                    System.out.println("neo4JDriverClass = " + neo4JDriverClass);
                    ProtectionDomain neo4jDriverClassProdectionDomain = neo4JDriverClass.getProtectionDomain();
                    System.out.println("neo4jDriverClassProdectionDomain = " + neo4jDriverClassProdectionDomain);
                } catch (ClassNotFoundException classNotFoundException) {
                    classNotFoundException.printStackTrace();
                }
                return DriverManager.getConnection(neo4j_url, properties);
                
//            case NEO4J_BOLT:
//                neo4jJavaDriver = GraphDatabase.driver("bolt://" + host,
//                        AuthTokens.basic(username, password),
//                        Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig());
//                
//                neo4jSession = neo4jJavaDriver.session();
//                break;
//            case NEO4J_BOLT:
//                neo4jJavaDriver = GraphDatabase.driver("bolt://" + host,
//                        AuthTokens.basic(username, password),
//                        Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig());
//                
//                neo4jSession = neo4jJavaDriver.session();
//                break;
        }
        throw new IllegalArgumentException("Unsuppored dbtype ="+dbtype);
    }
}
