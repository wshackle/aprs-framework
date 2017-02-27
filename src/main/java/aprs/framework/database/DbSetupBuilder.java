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

import aprs.framework.Utils;
import aprs.framework.spvision.VisionToDBJPanel;
import crcl.ui.XFuture;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.ProtectionDomain;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
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
    private Map<DbQueryEnum, DbQueryInfo> queriesMap;
    private boolean internalQueriesResourceDir = true;
    private String queriesDir;
    private boolean debug = false;
    private int loginTimeout = DEFAULT_LOGIN_TIMEOUT;

    public static Map<DbQueryEnum, DbQueryInfo> getDefaultQueriesMap(DbType type) {
        switch (type) {
            case NEO4J:
                return NEO4J_DEFAULT_QUERIES_MAP;

            case MYSQL:
                return MYSQL_DEFAULT_QUERIES_MAP;
        }
        throw new IllegalArgumentException("No default queries map for " + type);
    }

    private static Map<DbQueryEnum, DbQueryInfo> NEO4J_DEFAULT_QUERIES_MAP;
    private static Map<DbQueryEnum, DbQueryInfo> MYSQL_DEFAULT_QUERIES_MAP;
    private static final String BASE_RESOURCE_DIR = "aprs/framework/database/";

    static {
        try {
            String resDir = "neo4j/v1/";
            Map<DbQueryEnum, DbQueryInfo> map = readRelResourceQueriesDirectory(resDir);
            NEO4J_DEFAULT_QUERIES_MAP = Collections.unmodifiableMap(map);
        } catch (Exception ex) {
            Logger.getLogger(DbSetupBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            String resDir = "mysql/";
            Map<DbQueryEnum, DbQueryInfo> map = readRelResourceQueriesDirectory(resDir);
            MYSQL_DEFAULT_QUERIES_MAP = Collections.unmodifiableMap(map);
        } catch (Exception ex) {
            Logger.getLogger(DbSetupBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static Map<DbQueryEnum, DbQueryInfo> readRelResourceQueriesDirectory(String resDir) throws IOException {
        if (BASE_RESOURCE_DIR.endsWith("/") && resDir.startsWith("/")) {
            resDir = resDir.substring(1);
        }
        if (!BASE_RESOURCE_DIR.endsWith("/") && !resDir.startsWith("/")) {
            resDir = "/" + resDir;
        }
        if (!resDir.endsWith("/")) {
            resDir = resDir + "/";
        }
        return readResourceQueriesDirectory(BASE_RESOURCE_DIR + resDir);
    }

    public static Map<DbQueryEnum, DbQueryInfo> readResourceQueriesDirectory(String resDir) throws IOException {
        Map<DbQueryEnum, DbQueryInfo> map = new EnumMap<>(DbQueryEnum.class);
        for (DbQueryEnum q : DbQueryEnum.values()) {
            String resName = resDir + q.toString().toLowerCase() + ".txt";
            String txt = getStringResource(resName);
            DbQueryInfo info = DbQueryInfo.parse(txt);
            map.put(q, info);
        }
        return map;
    }

    public static Map<DbQueryEnum, DbQueryInfo> readQueriesDirectory(String resDir) throws IOException {
        Map<DbQueryEnum, DbQueryInfo> map = new EnumMap<>(DbQueryEnum.class);
        for (DbQueryEnum q : DbQueryEnum.values()) {
            String resName = resDir + File.separator + q.toString().toLowerCase() + ".txt";
            String txt = getStringFromFile(resName);
            DbQueryInfo info = DbQueryInfo.parse(txt);
            map.put(q, info);
        }
        return map;
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
        try (InputStream stream = cl.getResourceAsStream(name)) {
            if (null == stream) {
                throw new IllegalArgumentException("No resource found for name=" + name);
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
                String line = null;
                while (null != (line = br.readLine())) {
                    sb.append(line);
                    sb.append(System.lineSeparator());
                }
            }
        }
        return sb.toString();
    }

    private static String getStringFromFile(String name) throws IOException {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(name))) {
            String line = null;
            while (null != (line = br.readLine())) {
                sb.append(line);
                sb.append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    private static class DbSetupInternal implements DbSetup {

        private final DbType type;
        private final String host;
        private final int port;
        private final char[] passwd;
        private final String user;
        private final String dbname;
        private final boolean connected;
        private final Map<DbQueryEnum, DbQueryInfo> queriesMap;
        private final boolean internalQueriesResourceDir;
        private final String queriesDir;
        private final boolean debug;
        private final int loginTimeout;

        private DbSetupInternal(
                DbType type,
                String host,
                int port,
                char[] passwd,
                String user,
                String dbname,
                boolean connected,
                Map<DbQueryEnum, DbQueryInfo> queriesMap,
                boolean internalQueriesResourceDir,
                String queriesDir,
                boolean debug,
                int loginTimeout) {
            this.type = type;
            this.host = host;
            this.port = port;
            this.passwd = passwd;
            this.user = user;
            this.dbname = dbname;
            this.connected = connected;
            this.queriesMap = queriesMap;
            this.internalQueriesResourceDir = internalQueriesResourceDir;
            this.queriesDir = queriesDir;
            this.debug = debug;
            this.loginTimeout = loginTimeout;
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
        public Map<DbQueryEnum, DbQueryInfo> getQueriesMap() {
            return queriesMap;
        }

        @Override
        public boolean isInternalQueriesResourceDir() {
            return internalQueriesResourceDir;
        }

        @Override
        public String getQueriesDir() {
            return queriesDir;
        }

        @Override
        public boolean isDebug() {
            return debug;
        }

        @Override
        public int getLoginTimeout() {
            return loginTimeout;
        }

    }

    public DbSetupBuilder setup(DbSetup setup)  {
        type = setup.getDbType();
        host = setup.getHost();
        dbname = setup.getDbName();
        user = setup.getDbUser();
        connected = setup.isConnected();
        port = setup.getPort();
        passwd = setup.getDbPassword();
        internalQueriesResourceDir = setup.isInternalQueriesResourceDir();
        queriesDir = setup.getQueriesDir();
        queriesMap = setup.getQueriesMap();
        debug = setup.isDebug();
        if (null == queriesMap) {
            if (internalQueriesResourceDir) {
                try {
                    queriesMap = readRelResourceQueriesDirectory(queriesDir);
                } catch (IOException ex) {
                    Logger.getLogger(DbSetupBuilder.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                try {
                    queriesMap = readQueriesDirectory(queriesDir);
                } catch (IOException ex) {
                    Logger.getLogger(DbSetupBuilder.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return this;
    }

    public DbSetup build() {
        return new DbSetupInternal(
                type,
                host,
                port,
                passwd,
                user,
                dbname,
                connected,
                ((queriesMap != null) ? Collections.unmodifiableMap(queriesMap) : null),
                internalQueriesResourceDir,
                queriesDir,
                debug,
                loginTimeout);
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

    public DbSetupBuilder queriesDir(String queriesDir) {
        if (null != queriesDir) {
            this.queriesDir = queriesDir;
        }
        return this;
    }

    public DbSetupBuilder internalQueriesResourceDir(boolean internalQueriesResourceDir) {
        this.internalQueriesResourceDir = internalQueriesResourceDir;
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

    public DbSetupBuilder debug(boolean debug) {
        this.debug = debug;
        return this;
    }
    
    public DbSetupBuilder loginTimeout(int loginTimeout) {
        this.loginTimeout = loginTimeout;
        return this;
    }
    
    public DbSetupBuilder queriesMap(Map<DbQueryEnum, DbQueryInfo> queriesMap) {
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

    private DbSetupBuilder updateFromArgs(Map<String, String> _argsMap,
            DbType dbtype,
            String host,
            int port) {
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
        String dbSpecificQueriesDir = _argsMap.get(dbHostPort + ".queriesDir");
        if (null != dbSpecificQueriesDir) {
            builder = builder.queriesDir(dbSpecificQueriesDir);
        } else {
            String queriesDir = _argsMap.get("--queriesDir");
            if (null != queriesDir) {
                builder = builder.queriesDir(queriesDir);
            }
        }
        String dbSpecificInternalQueriesResourceDirString
                = _argsMap.get(dbHostPort + ".internalQueriesResourceDir");
        if (null != dbSpecificQueriesDir) {
            builder = builder.internalQueriesResourceDir(Boolean.valueOf(dbSpecificInternalQueriesResourceDirString));
        } else {
            String internalQueriesResourceDiriesDirString
                    = _argsMap.get("--internalQueriesResourceDir");
            if (null != internalQueriesResourceDiriesDirString) {
                builder = builder.internalQueriesResourceDir(Boolean.valueOf(internalQueriesResourceDiriesDirString));
            }
        }
        return builder;
    }

    public static Map<String, String> getDefaultArgsMap() {
        Map<String, String> defaultArgsMap = new HashMap<>();
        defaultArgsMap.put("--dbhost", "localhost");
        defaultArgsMap.put("--dbport", "7480");
        defaultArgsMap.put("--dbname", "");
        defaultArgsMap.put("--dbuser", "neo4j");
        defaultArgsMap.put("--dbpasswd", "password");
        defaultArgsMap.put("--visionhost", "localhost");
        defaultArgsMap.put("--visionport", "4000");
        defaultArgsMap.put("--commandport", "4001");
        defaultArgsMap.put("--acquirestate", AcquireEnum.ON.toString());
        defaultArgsMap.put("--showgui", "true");
        defaultArgsMap.put("--dbtype", DbType.NEO4J.toString());
        return defaultArgsMap;
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
            Map<String, String> argsMap = getDefaultArgsMap();
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
            Map<String, String> argsMap = getDefaultArgsMap();
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
     *
     * @param propertiesFile
     * @param setup
     */
    public static void savePropertiesFile(File propertiesFile, DbSetup setup) {
        savePropertiesFile(propertiesFile, setup, setup.getDbType(), setup.getHost(), setup.getPort());
    }

    /**
     * Save the given setup to a properties file, possibly replacing some setup
     * values with the passed arguments.
     *
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
            props.put(dbHostPort + ".internalQueriesResourceDir",
                    Boolean.toString(setup.isInternalQueriesResourceDir()));
            String queriesDir = setup.getQueriesDir();
            if (null != queriesDir) {
                props.put(dbHostPort + ".queriesDir", setup.getQueriesDir());
            }
//            try (FileWriter fw = new FileWriter(propertiesFile)) {
//                props.store(fw, "");
//            } catch (IOException ex) {
//                Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
//            }
            Utils.saveProperties(propertiesFile, props);
        }
    }

    public XFuture<Connection>  connect() throws SQLException {
        return connect(this.build());
    }

    public static XFuture<Connection>  connect(DbSetup setup) throws SQLException {
        return setupConnection(setup.getDbType(), setup.getHost(), setup.getPort(), setup.getDbName(), setup.getDbUser(), new String(setup.getDbPassword()),setup.isDebug(), setup.getLoginTimeout());
    }

    public static final int DEFAULT_LOGIN_TIMEOUT = 5; // in seconds 
    
    public static XFuture<Connection> setupConnection(DbType dbtype, String host, int port, String db, String username, String password, boolean debug, int loginTimeout) throws SQLException {
        XFuture<Connection> ret = new XFuture<>();
        new Thread(() -> {
            Connection conn;
            try {
               ret.complete(setupConnectionPriv(dbtype, host, port, db, username, password, debug, DEFAULT_LOGIN_TIMEOUT));
            } catch (SQLException ex) {
                ret.completeExceptionally(ex);
                Logger.getLogger(DbSetupBuilder.class.getName()).log(Level.SEVERE, null, ex);
            }
        }, "connectionSetupThread").start();
        return ret;
        
//        return XFuture. -> {
//            try {
//                return setupConnectionPriv(dbtype,host,port,db,username,password,debug);
//            } catch (SQLException ex) {
//                Logger.getLogger(DbSetupBuilder.class.getName()).log(Level.SEVERE, null, ex);
//            }
//            
//                });
    }   
    private static Connection setupConnectionPriv(DbType dbtype, String host, int port, String db, String username, String password, boolean debug, int loginTimeout) throws SQLException {
      
        switch (dbtype) {
            case MYSQL:
//                useBatch = true;
                String mysql_url = "jdbc:mysql://" + host + ":" + port + "/" + db;
                if (debug) {
                    System.out.println("Connection url = " + mysql_url);
                }
//                try {
//                    Class mySqlDriverClass = Class.forName("com.mysql.jdbc.Driver");
//                    System.out.println("neo4JDriverClass = " + mySqlDriverClass);
//                    ProtectionDomain mySqlDriverClassProtectionDomain = mySqlDriverClass.getProtectionDomain();
//                    System.out.println("mySqlDriverClassProdectionDomain = " + mySqlDriverClassProtectionDomain);
//                } catch (ClassNotFoundException classNotFoundException) {
//                    classNotFoundException.printStackTrace();
//                }
                if(loginTimeout > 0) {
                    DriverManager.setLoginTimeout(loginTimeout);
                }
                return DriverManager.getConnection(mysql_url, username, password);

            case NEO4J:
//                useBatch = false;
                Properties properties = new Properties();
                properties.put("user", username);
                properties.put("password", password);
                String neo4j_url = "jdbc:neo4j://" + host + ":" + port;
                if (debug) {
                    System.out.println("neo4j_url = " + neo4j_url);
                    System.out.println("Connection url = " + neo4j_url);
                    try {
                        Class neo4JDriverClass = Class.forName("org.neo4j.jdbc.Driver");
                        System.out.println("neo4JDriverClass = " + neo4JDriverClass);
                        ProtectionDomain neo4jDriverClassProtectionDomain = neo4JDriverClass.getProtectionDomain();
                        System.out.println("neo4jDriverClassProdectionDomain = " + neo4jDriverClassProtectionDomain);
                    } catch (ClassNotFoundException classNotFoundException) {
                        classNotFoundException.printStackTrace();
                    }
                }
                if(loginTimeout > 0) {
                    DriverManager.setLoginTimeout(loginTimeout);
                }
                return DriverManager.getConnection(neo4j_url, properties);

            case NEO4J_BOLT:
                throw new RuntimeException("Neo4J BOLT driver not supported.");
//                useBatch = false;
//                Properties neo4j_bolt_properties = new Properties();
//                neo4j_bolt_properties.put("user", username);
//                neo4j_bolt_properties.put("password", password);
//                String neo4j_bolt_url = "jdbc:neo4j:bolt://" + host + ":" + port;
//                System.out.println("neo4j_url = " + neo4j_bolt_url);
//                System.out.println("Connection url = " + neo4j_bolt_url);
//                try {
//                    Class neo4JDriverClass = Class.forName("org.neo4j.jdbc.BaseDriver");
//                    System.out.println("neo4JDriverClass = " + neo4JDriverClass);
//                    ProtectionDomain neo4jDriverClassProtectionDomain = neo4JDriverClass.getProtectionDomain();
//                    System.out.println("neo4jDriverClassProdectionDomain = " + neo4jDriverClassProtectionDomain);
//                } catch (ClassNotFoundException classNotFoundException) {
//                    classNotFoundException.printStackTrace();
//                }
//                return DriverManager.getConnection(neo4j_bolt_url, neo4j_bolt_properties);

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
        throw new IllegalArgumentException("Unsuppored dbtype =" + dbtype);
    }
}
