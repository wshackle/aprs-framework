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
package aprs.framework.spvision;

import aprs.framework.database.DetectedItem;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class VisionSocketServer implements AutoCloseable {

    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private ExecutorService publishService;
    private final boolean shutdownServiceOnClose;

    public int getPort() {
        return serverSocket != null? serverSocket.getLocalPort(): -1;
    }
    
    
    private static class DaemonThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        }

    }
    private static DaemonThreadFactory daemonThreadFactory = new DaemonThreadFactory();

    public VisionSocketServer(ServerSocket serverSocket, ExecutorService executorService, boolean shutdownServiceOnClose) {
        this.serverSocket = serverSocket;
        this.executorService = executorService;
        this.shutdownServiceOnClose = shutdownServiceOnClose;
        start();
    }

    public VisionSocketServer(int port, int backlog, InetAddress bindAddr) throws IOException {
        try {
            this.shutdownServiceOnClose = true;
            serverSocket = new ServerSocket(port, backlog, bindAddr);
            this.executorService = Executors.newCachedThreadPool(daemonThreadFactory);
            this.publishService = Executors.newSingleThreadExecutor(daemonThreadFactory);
            
            start();
        } catch (IOException iOException) {
            throw new IOException("Can't bind to port=" + port+" with bindAddr="+bindAddr, iOException);
        }
    }

    public VisionSocketServer(int port) throws IOException {
        try {
            this.shutdownServiceOnClose = true;
            serverSocket = new ServerSocket(port);
            this.executorService = Executors.newCachedThreadPool(daemonThreadFactory);
            this.publishService = Executors.newSingleThreadExecutor(daemonThreadFactory);
            start();
        } catch (IOException iOException) {
            throw new IOException("Can't bind to port=" + port, iOException);
        }
    }

    private List<Socket> clients = new ArrayList<Socket>();

    private void start() {
        this.executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!closing && !Thread.currentThread().isInterrupted()) {
                        Socket clientSocket = serverSocket.accept();
                        if (debug) {
                            System.out.println("clientSocket = " + clientSocket);
                        }
                        if (null != bytesToSend) {
                            clientSocket.getOutputStream().write(bytesToSend);
                        }
                        synchronized (clients) {
                            clients.add(clientSocket);
                        }
                    }
                } catch (IOException ex) {
                    Logger.getLogger(VisionSocketServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    private volatile boolean closing = false;
    private byte bytesToSend[] = null;

    public static String listToLine(List<DetectedItem> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            DetectedItem item = list.get(i);
            if (null != item && item.name != null && item.name.length() > 0) {
                sb.append(item.name);
                sb.append(',');
                sb.append(item.rotation);
                sb.append(',');
                sb.append(item.x);
                sb.append(',');
                sb.append(item.y);
                sb.append(',');
                sb.append(item.score);
                sb.append(',');
                sb.append(item.type);
                sb.append(',');
            }
        }
        sb.append('\n');
        return sb.toString();
    }

    private boolean debug = false;

    /**
     * Get the value of debug
     *
     * @return the value of debug
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Set the value of debug
     *
     * @param debug new value of debug
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void publishList(List<DetectedItem> list) {
        bytesToSend = listToLine(list).getBytes();
        publishService.submit(new Runnable() {
            @Override
            public void run() {
                synchronized (clients) {
                    for (int i = 0; i < clients.size() && !closing; i++) {
                        Socket client = clients.get(i);
                        if (null != client) {
                            try {
                                if (debug) {
                                    System.out.println(String.format("Sending %d bytes to %s:%d : %s",
                                            bytesToSend.length,
                                            ((InetSocketAddress) client.getRemoteSocketAddress()).getHostString(),
                                            ((InetSocketAddress) client.getRemoteSocketAddress()).getPort(),
                                            new String(bytesToSend)));
                                }
                                client.getOutputStream().write(bytesToSend);
                            } catch (IOException ex) {
                                Logger.getLogger(VisionSocketServer.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public void close() {
        closing = true;
        bytesToSend = null;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ex) {
                Logger.getLogger(VisionSocketServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            serverSocket = null;
        }
        if (null != executorService) {
            if (shutdownServiceOnClose) {
                executorService.shutdownNow();
                try {
                    executorService.awaitTermination(100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ex) {
                    Logger.getLogger(VisionSocketServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            executorService = null;
        }
        if (null != publishService) {
            publishService.shutdownNow();
            try {
                publishService.awaitTermination(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Logger.getLogger(VisionSocketServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            publishService = null;
        }
        synchronized (clients) {
            for (int i = 0; i < clients.size(); i++) {
                Socket client = clients.get(i);
                if (null != client) {
                    try {
                        client.close();
                    } catch (IOException ex) {
                        Logger.getLogger(VisionSocketServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            clients.clear();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

}
