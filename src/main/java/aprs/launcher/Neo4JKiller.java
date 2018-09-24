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
package aprs.launcher;

import com.github.wshackle.fanuccrclservermain.FanucCRCLMain;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
class Neo4JKiller {

    
    private static final Logger LOGGER = Logger.getLogger(Neo4JKiller.class.getName());
    
    private static boolean debug = Boolean.getBoolean("Neo4JKiller.debug");
    
    private static void logDebug(String string)  {
        if(debug) {
            LOGGER.log(Level.INFO, string);
        }
    }
    
    public static boolean isDebug() {
        return debug;
    }
    
    public static void setDebug(boolean newDebugVal) {
        debug = newDebugVal;
    }
    
    @MonotonicNonNull
    private static File jpsCommandFile;

    public static void setJpsCommandFile(File f) {
        jpsCommandFile = f;
    }

    @Nullable
    static File getJpsCommandFile() {
        return jpsCommandFile;
    }

    static final String JPS_COMMAND_FILENAME_STRING = "jpsCommand.txt";
    
    private static String getJpsCommand() throws IOException {
        String jpsCommandProperty = System.getProperty("jps.command");
        if (null != jpsCommandProperty && jpsCommandProperty.length() > 0) {
            return jpsCommandProperty;
        }
        if (null != jpsCommandFile) {
            if (jpsCommandFile.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(jpsCommandFile))) {
                    String line;
                    while (null != (line = br.readLine())) {
                        String trimmed = line.trim();
                        if (trimmed.length() > 0) {
                            return trimmed;
                        }
                    }
                }
            }
        }

        String jpsCmd = "jps";
        if (isWindowsOs()) {
            jpsCmd = DEFAULT_WINDOWS_JPS_COMMAND;
        }
        logDebug("Using command \"" + jpsCmd + "\" to run jps to find the neo4j processes, if you need to use a different command on your system put the text for that command in:");
        if(null != jpsCommandFile) {
            logDebug(jpsCommandFile.getCanonicalPath());
        } else {
            logDebug(JPS_COMMAND_FILENAME_STRING+" in the directory with the launch.txt file");
        }
        logDebug(" or set the property jps.command");
        return jpsCmd;
    }

    private static List<Integer> getNeo4JPIDs() throws IOException {

        ProcessBuilder pb = new ProcessBuilder(getJpsCommand(), "-l");
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        Process p = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        List<Integer> pids = new ArrayList<>();
        while (null != (line = br.readLine())) {
            logDebug("getNeo4JPIDS: line = " + line);
            String words[] = line.split("[ \t]+");
            if (words.length == 2) {
                if (words[1].equals("org.neo4j.server.CommunityBootstrapper")) {
                    pids.add(Integer.valueOf(words[0]));
                } else {
                    logDebug("words = " + Arrays.toString(words));
                }
            }
        }
        return pids;
    }
    private static final String DEFAULT_WINDOWS_JPS_COMMAND = "C:\\Program Files\\Java\\jdk1.8.0_92\\bin\\jps";

    private static void killPIDsWindows(Iterable<Integer> pids) throws IOException {
        List<Process> processes = new ArrayList<>();
        for (Integer pid : pids) {
            Process p = Runtime.getRuntime().exec(new String[]{"taskkill", "/T", "/F", "/PID", "" + pid});
            processes.add(p);
        }
        for (Process p : processes) {
            try {
                if (!p.waitFor(500, TimeUnit.MILLISECONDS)) {
                    p.destroyForcibly();
                }
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, "", ex);
                p.destroyForcibly();
            }
        }
    }

    private static void killPIDsLinux(Iterable<Integer> pids) throws IOException {
        List<Process> processes = new ArrayList<>();
        for (Integer pid : pids) {
            Process p = Runtime.getRuntime().exec(new String[]{"kill", "-KILL", "" + pid});
            processes.add(p);
        }
        for (Process p : processes) {
            try {
                if (!p.waitFor(500, TimeUnit.MILLISECONDS)) {
                    p.destroyForcibly();
                }
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, "", ex);
                p.destroyForcibly();
            }
        }
    }

    private static void killPIDs(Iterable<Integer> pids) throws IOException {
        if (isWindowsOs()) {
            killPIDsWindows(pids);
        } else {
            killPIDsLinux(pids);
        }
    }

    private static boolean isWindowsOs() {
        return System.getProperty("os.name").startsWith("Windows");
    }

    static void killNeo4J() throws IOException {
        List<Integer> pids = getNeo4JPIDs();
        logDebug("killNeo4J: pids = " + pids);
        killPIDs(pids);
        List<Integer> newpids = getNeo4JPIDs();
        logDebug("killNeo4J: newpids = " + newpids);
    }

    public static void main(String[] args) throws IOException {
        killNeo4J();
    }
}
