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
package aprs.framework.process.launcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author shackle
 */
public class Neo4JKiller {

    
    
    public static String getJpsCommand() throws IOException {
        String jpsCommandProperty = System.getProperty("jps.command");
        if(null != jpsCommandProperty && jpsCommandProperty.length() > 0) {
            return jpsCommandProperty;
        }
        File jpsCommandFile = new File(System.getProperty("user.home"),"jpsCommand.txt");
        if(jpsCommandFile.exists()) {
            try(BufferedReader br = new BufferedReader(new FileReader(jpsCommandFile))) {
                String line;
                while(null != (line = br.readLine())) {
                    String trimmed = line.trim();
                    if(trimmed.length() > 0) {
                        return trimmed;
                    }
                }
            }
        }
        
        String jpsCmd = "jps";
        if(isWindowsOs()) {
            jpsCmd = DEFAULT_WINDOWS_JPS_COMMAND;
        }
        System.out.println("Using command \""+jpsCmd+"\" to run jps to find the neo4j processes, if you need to use a different command on your system put the text for that command in:");
        System.out.println(jpsCommandFile.getCanonicalPath());
        System.out.println(" or set the property jps.command");
        return jpsCmd;
    }
    
    public static List<Integer> getNeo4JPIDs() throws IOException {

        ProcessBuilder pb = new ProcessBuilder(getJpsCommand(), "-l");
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        Process p = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        List<Integer> pids = new ArrayList<>();
        while (null != (line = br.readLine())) {
            String words[] = line.split("[ \t]+");
            if (words.length == 2) {
                if (words[1].equals("org.neo4j.server.CommunityBootstrapper")) {
                    pids.add(Integer.valueOf(words[0]));
                }
            }
        }
        return pids;
    }
    private static final String DEFAULT_WINDOWS_JPS_COMMAND = "c:\\Program Files\\Java\\jdk1.8.0_92\\bin\\jps";

    private static void killPIDsWindows(Iterable<Integer> pids) throws IOException {
        for(Integer pid : pids) {
            Runtime.getRuntime().exec(new String[]{"taskkill","/T","/F","/PID",""+pid});
        }
    }
    
    private static void killPIDsLinux(Iterable<Integer> pids) throws IOException {
        for(Integer pid : pids) {
            Runtime.getRuntime().exec(new String[]{"kill","-KILL",""+pid});
        }
    }
    
    public static void killPIDs(Iterable<Integer> pids) throws IOException {
        if(isWindowsOs()) {
            killPIDsWindows(pids);
        } else {
            killPIDsLinux(pids);
        }
    }

    private static boolean isWindowsOs() {
        return System.getProperty("os.name").startsWith("Windows");
    }
    
    public static void killNeo4J() throws IOException {
        List<Integer> pids = getNeo4JPIDs();
        killPIDs(pids);
    }
    
    public static void main(String[] args) throws IOException {
        killNeo4J();
    }
}
