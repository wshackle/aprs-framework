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
package aprs.database.vision;

import aprs.database.PhysicalItem;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class VisionSocketServer implements AutoCloseable {

    private volatile ServerSocket serverSocket;
    private @Nullable ExecutorService executorService;
    private @Nullable ExecutorService publishService;
    private final boolean shutdownServiceOnClose;

    public int getPort() {
        return serverSocket != null ? serverSocket.getLocalPort() : -1;
    }

    private static class DaemonThreadFactory implements ThreadFactory {

        private static final AtomicInteger tnum = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("VisionSocketServer_" + tnum.incrementAndGet());
            return thread;
        }

    }
    private static final DaemonThreadFactory daemonThreadFactory = new DaemonThreadFactory();

    private VisionSocketServer(ServerSocket serverSocket, ExecutorService executorService, boolean shutdownServiceOnClose) {
        this.serverSocket = serverSocket;
        this.executorService = executorService;
        this.shutdownServiceOnClose = shutdownServiceOnClose;
        this.publishService = Executors.newSingleThreadExecutor(daemonThreadFactory);
        start();
    }

    @SuppressWarnings("WeakerAccess")
    public VisionSocketServer(int port, int backlog, InetAddress bindAddr) throws IOException {
        try {
            this.shutdownServiceOnClose = true;
            serverSocket = new ServerSocket(port, backlog, bindAddr);
            this.executorService = Executors.newCachedThreadPool(daemonThreadFactory);
            this.publishService = Executors.newSingleThreadExecutor(daemonThreadFactory);

            start();
        } catch (IOException iOException) {
            throw new IOException("Can't bind to port=" + port + " with bindAddr=" + bindAddr, iOException);
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

    private final List<Socket> clients = Collections.synchronizedList(new ArrayList<>());

    @SuppressWarnings("DefaultAnnotationParam")
    private void start(
         
         
         
         
        @UnknownInitialization(Object.class) VisionSocketServer this) {
        if (null == executorService) {
            throw new IllegalStateException("exectorService is null, VisionSocketServer not fully initialized.");
        }
        if (null == serverSocket) {
            throw new IllegalStateException("serverSocket is null, VisionSocketServer not fully initialized.");
        }
        if (null == clients) {
            throw new IllegalStateException("clients is null, VisionSocketServer not fully initialized.");
        }
        this.executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (null == serverSocket) {
                    throw new IllegalStateException("serverSocket is null, VisionSocketServer not fully initialized.");
                }
                List<Socket> localClients = clients;
                if (null == localClients) {
                    throw new IllegalStateException("clients is null, VisionSocketServer not fully initialized.");
                }
                String origThreadName = Thread.currentThread().getName();
                try {
                    Thread.currentThread().setName("VisionSocketServer_accepting_for_port_" + serverSocket.getLocalPort());
                    while (!closing && !Thread.currentThread().isInterrupted()) {
                        Socket clientSocket = serverSocket.accept();
                        if (debug) {
                            System.out.println("clientSocket = " + clientSocket);
                        }
                        byte bytes[] = bytesToSend;
                        if (null != bytes) {
                            clientSocket.getOutputStream().write(bytes);
                        }
                        localClients.add(clientSocket);
                    }
                } catch (IOException ex) {
                    if (!closing) {
                        Logger.getLogger(VisionSocketServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } finally {
                    Thread.currentThread().setName(origThreadName);
                }
            }
        });
    }

    private volatile boolean closing = false;
    private byte bytesToSend @Nullable []  = null;

    public static String listToLine(List<PhysicalItem> list) {
        StringBuilder sb = new StringBuilder();
        for (PhysicalItem item : list) {
            if (null != item && item.getName() != null && item.getName().length() > 0) {
                sb.append(item.getName());
                sb.append(',');
                sb.append(item.getRotation());
                sb.append(',');
                sb.append(item.x);
                sb.append(',');
                sb.append(item.y);
                sb.append(',');
                sb.append(item.getScore());
                sb.append(',');
                sb.append(item.getType());
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

    private volatile long lastPublishListTime;
    private final AtomicInteger publishCount = new AtomicInteger();
    
    public long getLastPublishTime() {
        return lastPublishListTime;
    }
    
    public int getPublishCount() {
        return publishCount.get();
    }
    
    public void publishList(List<PhysicalItem> list) {
        String line = listToLine(list);
        byte ba[] = line.getBytes();
        this.bytesToSend = ba;
        if (null != publishService) {
            publishService.submit(new Runnable() {
                @Override
                public void run() {
                    Thread.currentThread().setName("VisionSocketServer.publishList.serverSocket=" + serverSocket + ".clients.size=" + clients.size());
                    for (int i = 0; i < clients.size() && !closing; i++) {
                        Socket client = clients.get(i);
                        if (Thread.currentThread().isInterrupted()) {
                            return;
                        }
                        if (null != client) {
                            try {
                                if (debug) {
                                    InetSocketAddress remoteAddress = (InetSocketAddress) client.getRemoteSocketAddress();
                                    if (null != remoteAddress) {
                                        System.out.println(String.format("Sending %d bytes to %s:%d : %s",
                                                ba.length,
                                                remoteAddress.getHostString(),
                                                remoteAddress.getPort(),
                                                line));
                                    }
                                }
                                client.getOutputStream().write(ba);
                            } catch (IOException ex) {
                                try {
                                    client.close();
                                } catch (IOException ex1) {
                                    Logger.getLogger(VisionSocketServer.class.getName()).log(Level.SEVERE, null, ex1);
                                }
                                clients.remove(i);
                                Logger.getLogger(VisionSocketServer.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                    lastPublishListTime = System.currentTimeMillis();
                    publishCount.incrementAndGet();
                }
            });
        }
    }

    @Override
    public void close() {
        closing = true;
        bytesToSend = null;
        try {
            serverSocket.close();
        } catch (IOException ex) {
            Logger.getLogger(VisionSocketServer.class.getName()).log(Level.SEVERE, null, ex);
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

        for (Socket client : clients) {
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

    @Override
    @SuppressWarnings("deprecation")
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

}
