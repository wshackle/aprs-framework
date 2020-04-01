/*
 * This software is public domain software, however it is preferred
 * that the following disclaimers be attached.
 * Software Copyright/Warranty Disclaimer
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
package aprs.database;

import aprs.misc.Utils;
import aprs.database.vision.VisionToDBJPanel;
import static aprs.misc.AprsCommonLogger.println;
import crcl.utils.XFuture;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Builder to create DbSetup instances from data queried from the user and/or
 * obtained from text resource files.
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
    private @Nullable
    Map<DbQueryEnum, DbQueryInfo> queriesMap;
    private boolean internalQueriesResourceDir = true;
    private @MonotonicNonNull
    String queriesDir = null;
    private boolean debug = false;
    private int loginTimeout = DEFAULT_LOGIN_TIMEOUT;
    private @Nullable
    String startScript = null;

    private static final String BASE_RESOURCE_DIR = "aprs/database/";

    /**
     * Read a resource directory relative to default base of text resource files
     * with information for each required query type and create a map of query
     * types to database query information objects.
     *
     * @param resDir resource directory relative to default base directory
     * @return map of query types to query information objects
     *
     * @throws IOException directory doesn't exist etc.
     */
    private static Map<DbQueryEnum, DbQueryInfo> readRelResourceQueriesDirectory(String resDir) throws IOException {
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

    /**
     * Read a resource directory of text resource files with information for
     * each required query type and create a map of query types to database
     * query information objects.
     *
     * @param resDir resource directory
     * @return map of query types to query information objects
     *
     * @throws IOException directory doesn't exist etc.
     */
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

    /**
     * Read a directory of text resource files with information for each
     * required query type and create a map of query types to database query
     * information objects.
     *
     * @param resDir resource directory
     * @return map of query types to query information objects
     *
     * @throws IOException directory doesn't exist etc.
     */
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

    private static String getStringResource(String name) throws IOException {
        StringBuilder sb = new StringBuilder();
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        if (null != cl) {
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
        }
        return sb.toString();
    }

    private static String getStringFromFile(String name) throws IOException {
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

        private final @Nullable
        Map<DbQueryEnum, DbQueryInfo> queriesMap;

        private final boolean internalQueriesResourceDir;
        private final String queriesDir;
        private final boolean debug;
        private final int loginTimeout;

        private final @Nullable
        String startScript;

        private DbSetupInternal(
                DbType type,
                String host,
                int port,
                char[] passwd,
                String user,
                String dbname,
                boolean connected,
                @Nullable Map<DbQueryEnum, DbQueryInfo> queriesMap,
                boolean internalQueriesResourceDir,
                String queriesDir,
                boolean debug,
                int loginTimeout,
                @Nullable String startScript) {
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
            this.startScript = startScript;
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
            if (null == queriesMap) {
                return Collections.emptyMap();
            }
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

        @Override
        public @Nullable
        String getStartScript() {
            return startScript;
        }

    }

    /**
     * Copies the options from the given setup to the builder in order to create
     * a new DbSetup instance that differs in only a subset of the options.
     *
     * @param setup object to copy properties from
     * @return new builder with copied initial properties
     */
    @EnsuresNonNull("this.queriesDir")
    public DbSetupBuilder setup(DbSetup setup) {
        type = setup.getDbType();
        host = setup.getHost();
        dbname = setup.getDbName();
        user = setup.getDbUser();
        connected = setup.isConnected();
        port = setup.getPort();
        passwd = setup.getDbPassword();
        internalQueriesResourceDir = setup.isInternalQueriesResourceDir();
        String setupQueriesDir = setup.getQueriesDir();

        queriesMap = setup.getQueriesMap();
        debug = setup.isDebug();
        if (null == queriesMap) {
            if (internalQueriesResourceDir) {
                try {
                    queriesMap = readRelResourceQueriesDirectory(setupQueriesDir);
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "", ex);
                }
            } else {
                try {
                    queriesMap = readQueriesDirectory(setupQueriesDir);
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "", ex);
                }
            }
        }
        queriesDir = setupQueriesDir;
        return this;
    }

    /**
     * Create a new instance from the current options.
     *
     * @return new instance
     */
    public DbSetup build() {
        if (connected && (type == null || type == DbType.NONE)) {
            throw new IllegalArgumentException("type = " + type);
        }
        if (null == queriesDir) {
            throw new IllegalStateException("queriesDir not set");
        }
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
                loginTimeout,
                startScript);
    }

    /**
     * Set type option in builder
     *
     * @param type database type
     * @return this builder
     */
    public DbSetupBuilder type(@Nullable DbType type) {
        if (null != type) {
            this.type = type;
        }
        return this;
    }

    /**
     * Set host option in builder
     *
     * @param host database server host
     * @return this builder
     */
    public DbSetupBuilder host(String host) {
        if (null != host) {
            this.host = host;
        }
        return this;
    }

    /**
     * Set port option in builder
     *
     * @param port database server port
     * @return this builder
     */
    public DbSetupBuilder port(int port) {
        this.port = port;
        return this;
    }

    /**
     * Set passwd option in builder
     *
     * @param passwd database passwd
     * @return this builder
     */
    public DbSetupBuilder passwd(char[] passwd) {
        if (null != passwd) {
            this.passwd = passwd;
        }
        return this;
    }

    /**
     * Set user option in builder
     *
     * @param user database username
     * @return this builder
     */
    public DbSetupBuilder user(String user) {
        if (null != user) {
            this.user = user;
        }
        return this;
    }

    /**
     * Set _queriesDir option in builder
     *
     * @param _queriesDir queries directory path
     * @return this builder
     */
    @EnsuresNonNull("this.queriesDir")
    public DbSetupBuilder queriesDir(String _queriesDir) {
        this.queriesDir = _queriesDir;
        return this;
    }

    /**
     * Set internalQueriesResourceDir option in builder
     *
     * @param internalQueriesResourceDir directory path
     * @return this builder
     */
    public DbSetupBuilder internalQueriesResourceDir(boolean internalQueriesResourceDir) {
        this.internalQueriesResourceDir = internalQueriesResourceDir;
        return this;
    }

    /**
     * Set startScript option in builder
     *
     * @param startScript script path
     * @return this builder
     */
    public DbSetupBuilder startScript(String startScript) {
        this.startScript = startScript;
        return this;
    }

    /**
     * Set dbname option in builder
     *
     * @param dbname database name
     * @return this builder
     */
    public DbSetupBuilder dbname(String dbname) {
        if (null != dbname) {
            this.dbname = dbname;
        }
        return this;
    }

    /**
     * Set connected option in builder
     *
     * @param connected desired connection state
     * @return this builder
     */
    public DbSetupBuilder connected(boolean connected) {
        this.connected = connected;
        return this;
    }

    /**
     * Set debug option in builder
     *
     * @param debug desired debug preference
     * @return this builder
     */
    public DbSetupBuilder debug(boolean debug) {
        this.debug = debug;
        return this;
    }

    /**
     * Set loginTimeout option in builder
     *
     * @param loginTimeout desired login timeout
     * @return this builder
     */
    public DbSetupBuilder loginTimeout(int loginTimeout) {
        this.loginTimeout = loginTimeout;
        return this;
    }

    /**
     * Set queriesMap option in builder
     *
     * @param queriesMap map of queries
     * @return this builder
     */
    public DbSetupBuilder queriesMap(Map<DbQueryEnum, DbQueryInfo> queriesMap) {
        this.queriesMap = queriesMap;
        return this;
    }

    private DbSetupBuilder updateFromArgs(Map<String, String> _argsMap) {

        DbSetupBuilder builder = this;

        String argsMapDbTypeString = _argsMap.get("--dbtype");
        DbType dbtype = builder.type;
        if (argsMapDbTypeString != null && argsMapDbTypeString.length() > 0) {
            dbtype = DbType.valueOf(argsMapDbTypeString);
            builder = builder.type(dbtype);
        }
        String dbSpecificHost = _argsMap.get(dbtype + ".host");
        if (null != dbSpecificHost) {
            builder = builder.host(dbSpecificHost);
            host = dbSpecificHost;
        } else {
            String h = _argsMap.get("--dbhost");
            if (null != h && h.length() > 0) {
                host = h;
                builder = builder.host(h);
            }
        }
        String dbSpecificPort = _argsMap.get(dbtype + "." + host + ".port");
        if (null != dbSpecificPort) {
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
        if (null != dbSpecificPasswd) {
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
        String startScript = _argsMap.get("startScript");
        if (null != startScript) {
            builder = builder.startScript(startScript);
        }
        return builder;
    }

    /**
     * Get a map with hard-coded default argument options.
     *
     * @return map of argument options
     */
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
     * @param propertiesFile properties file
     * @return builder with loaded settings
     */
    public static DbSetupBuilder loadFromPropertiesFile(File propertiesFile) {
        DbSetupBuilder builder = new DbSetupBuilder();
        if (null != propertiesFile && propertiesFile.exists()) {
            Properties props = new Properties();
            try (FileReader fr = new FileReader(propertiesFile)) {
                props.load(fr);
            } catch (IOException ex) {
                Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
            }
            Map<String, String> argsMap = getDefaultArgsMap();
            for (String propName : props.stringPropertyNames()) {
                String propValue = props.getProperty(propName);
                if (null != propValue) {
                    argsMap.put(propName, propValue);
                }
            }
            return builder.updateFromArgs(argsMap);
        }
        return builder;
    }

    /**
     * Save the given setup to a properties file.
     *
     * @param propertiesFile properties file
     * @param setup database setup object
     */
    public static void savePropertiesFile(File propertiesFile, DbSetup setup) {
        savePropertiesFile(propertiesFile, setup, setup.getDbType(), setup.getHost(), setup.getPort());
    }

    /**
     * Save the given setup to a properties file, possibly replacing some setup
     * values with the passed arguments.
     *
     * @param propertiesFile properties file
     * @param setup database setup object
     * @param dbtype database type
     * @param host database host
     * @param port database port
     */
    private static void savePropertiesFile(File propertiesFile, DbSetup setup, DbType dbtype, String host, int port) {
        if (null == propertiesFile) {
            throw new IllegalArgumentException("propertiesFile == null");
        }
        if (dbtype == null || dbtype == DbType.NONE) {
            throw new IllegalArgumentException("dbtype = " + dbtype);
        }
        if (null == setup) {
            throw new IllegalArgumentException("setup == null");
        }
        try {
            println("Saving " + propertiesFile.getCanonicalPath());
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "", ex);
        }
        File parentFile = propertiesFile.getParentFile();
        if (null != parentFile) {
            parentFile.mkdirs();
        }
        Properties props = new Properties();
        String startScript = setup.getStartScript();
        if (null != startScript) {
            props.put("startScript", startScript);
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
        Utils.saveProperties(propertiesFile, props);
    }

    /**
     * Setup a connection given the settings.
     *
     * The connection may take some time and will be completed asynchronously in
     * another thread after this method returns. The returned future can be used
     * to wait for the connection.
     *
     * @param setup database setup object
     * @return future for new connection
     */
    public static XFuture<Connection> connect(DbSetup setup) {
        return setupConnection(setup.getDbType(), setup.getHost(), setup.getPort(), setup.getDbName(), setup.getDbUser(), new String(setup.getDbPassword()), setup.isDebug(), setup.getLoginTimeout());
    }

    /**
     * Default login timeout in seconds
     */
    public static final int DEFAULT_LOGIN_TIMEOUT = 5; // in seconds 

    /**
     * Setup a connection given the settings.
     *
     * The connection may take some time and will be completed asynchronously in
     * another thread after this method returns. The returned future can be used
     * to wait for the connection.
     *
     * @param dbtype database type
     * @param host database host
     * @param port database port
     * @param db database name
     * @param username user's name
     * @param password user's password
     * @param debug enable debugging
     * @param loginTimeout timeout for login
     * @return future for new connection
     */
    public static XFuture<Connection> setupConnection(DbType dbtype, String host, int port, String db, String username, String password, boolean debug, int loginTimeout) {
        if (dbtype == null || dbtype == DbType.NONE) {
            throw new IllegalArgumentException("dbtype = " + dbtype);
        }
        XFuture<Connection> ret = new XFuture<>("setupConnection");
        new Thread(() -> {
            Connection conn;
            try {
                ret.complete(setupConnectionPriv(dbtype, host, port, db, username, password, debug, loginTimeout));
            } catch (Exception ex) {
                ret.completeExceptionally(ex);
                LOGGER.log(Level.SEVERE, "", ex);
            }
        }, "dbConnectionSetupThread:" + host + ":" + port).start();
        return ret;
    }

    private static Connection setupConnectionPriv(DbType dbtype, String host, int port, String db, String username, String password, boolean debug, int loginTimeout) throws SQLException {

        try {
            switch (dbtype) {
                case MYSQL:
                    Class<?> mysqlDriverClass = Class.forName("com.mysql.jdbc.Driver");
                    println("driverClass = " + mysqlDriverClass);
                    String mysql_url = "jdbc:mysql://" + host + ":" + port + "/" + db;
                    if (debug) {
                        println("Connection url = " + mysql_url);
                    }
                    if (loginTimeout > 0) {
                        DriverManager.setLoginTimeout(loginTimeout);
                    }
                    return DriverManager.getConnection(mysql_url, username, password);

                case NEO4J:
                    @SuppressWarnings("unused") Class<?> neo4jDriverClass;
                    try {
                        neo4jDriverClass = Class.forName("org.neo4j.jdbc.Driver");
                        //println(" dynamic neo4jDriverClass = " + neo4jDriverClass);
                    } catch (ClassNotFoundException ex) {
                        LOGGER.log(Level.SEVERE, "", ex);
                    }

                    Properties properties = new Properties();
                    properties.put("user", username);
                    properties.put("password", password);
                    String neo4j_url = "jdbc:neo4j:http://" + host + ":" + port;
                    if (debug) {
                        LOGGER.log(Level.INFO, "neo4j_url = {0}", neo4j_url);
                        LOGGER.log(Level.INFO, "Connection url = {0}", neo4j_url);
                        try {
                            Class<?> neo4JDriverClass = Class.forName("org.neo4j.jdbc.Driver");
                            LOGGER.log(Level.INFO, "neo4JDriverClass = {0}", neo4JDriverClass);
                            ProtectionDomain neo4jDriverClassProtectionDomain = neo4JDriverClass.getProtectionDomain();
                            LOGGER.log(Level.INFO, "neo4jDriverClassProdectionDomain = {0}", neo4jDriverClassProtectionDomain);
                        } catch (ClassNotFoundException classNotFoundException) {
                            classNotFoundException.printStackTrace();
                        }
                    }
                    try {
                        if (loginTimeout > 0) {
                            DriverManager.setLoginTimeout(loginTimeout);
                        }
                        return DriverManager.getConnection(neo4j_url, properties);
                    } catch (Exception ex) {
                        System.err.println("dbtype="+dbtype+",host="+host+",port="+port+",db="+db+",username="+username);
                        Logger.getLogger(DbSetupBuilder.class.getName()).log(Level.SEVERE, "", ex);
                        throw new RuntimeException(ex);
                    }

                case NEO4J_BOLT:
                    throw new RuntimeException("Neo4J BOLT driver not supported.");
                default:
                    throw new IllegalArgumentException("Unsupported dbtype =" + dbtype);

            }

        } catch (ClassNotFoundException ex) { 
            Logger.getLogger(DbSetupBuilder.class.getName()).log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }
    private static final Logger LOGGER = Logger.getLogger(DbSetupBuilder.class.getName());
}
