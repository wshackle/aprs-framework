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
package aprs.remote;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class AprsRemoteConsoleServerSocket implements AutoCloseable , Runnable {

    private final ServerSocket ss;
    private final List<Thread> threadsList = new ArrayList<>();
    private final List<Socket> socketsList = new ArrayList<>();
     private Map<String, Scriptable<?>> scriptablesMap = new HashMap<>();
     
    public AprsRemoteConsoleServerSocket(int port,Map<String, Scriptable<?>> map) throws IOException {
        this(port);
        scriptablesMap.putAll(map);
    }

    public AprsRemoteConsoleServerSocket(int port) throws IOException {
        ss = new ServerSocket(port);
    }
    
    @Override
    public void run() {
        try {
            while (!ss.isClosed() && !Thread.currentThread().isInterrupted()) {
                Socket socket = ss.accept();
                socketsList.add(socket);
                final AprsRemoteConsoleRunner aprsRemoteConsoleRunner 
                        = new AprsRemoteConsoleRunner(socket.getInputStream(), socket.getOutputStream(),scriptablesMap);
                Thread thread = new Thread(aprsRemoteConsoleRunner,
                        "AprsRemoteConsoleService.port=" + ss.getLocalPort() + ".socket=" + socket.getInetAddress());
                thread.start();
                threadsList.add(thread);
            }
        } catch (Exception ex) {
            if(ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public void close() throws Exception {
        ss.close();
        for(Socket socket : socketsList) {
            socket.close();
        }
        for(Thread thread : threadsList) {
            thread.interrupt();
        }
    }
}
