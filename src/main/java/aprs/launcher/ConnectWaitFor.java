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

import crcl.ui.XFuture;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author shackle
 */
public class ConnectWaitFor {

    private final String host;
    private final int port;
    private final int max_tries;
    private final int timeout;
    private final SocketAddress endpoint;
    @Nullable private Throwable lastException = null;
    private final XFuture<Socket> socketFuture;
    private final long delay;
    private final Thread thread;
    private volatile int tries;
    private final long startTime;
    
    
    @SuppressWarnings("initialization")
    public ConnectWaitFor(String host, int port, int max_tries, int timeout, long delay) {
        if(host == null) {
            throw new NullPointerException("host");
        }
        if(port < 1) {
            throw new IllegalArgumentException("port ="+port);
        }
        startTime = System.currentTimeMillis();
        this.host = host;
        this.port = port;
        this.max_tries = max_tries;
        this.timeout = timeout;
        this.endpoint = new InetSocketAddress(host, port);
        this.delay = delay;
        socketFuture = new XFuture<Socket>("ConnectWaitFor(" + host + "," + port + "," + max_tries + "," + timeout + ","+delay+")");
        thread = new Thread(this::run, socketFuture.getName());
        thread.start();
    }
    
    private void run() {
        try {
            tryConnect();
            while (!socketFuture.isDone()) {
                Thread.sleep(delay);
                if(max_tries > 0 && tries >= max_tries) {
                    throw new IllegalStateException("max_tries exceeded host="+host+",port="+port+",tries="+tries+", max_tries="+max_tries);
                }
                if(Thread.currentThread().isInterrupted()) {
                    throw new IllegalStateException("interrupted");
                }
                if(socketFuture.isCancelled()) {
                    throw new IllegalStateException("socketFuture cancelled.");
                }
                tryConnect();
            }
        } catch (Exception exception) {
            Logger.getLogger(LaunchFileRunner.class.getName()).log(Level.SEVERE, "exception", exception);
            Logger.getLogger(LaunchFileRunner.class.getName()).log(Level.SEVERE, "lastException", lastException);
            System.err.println("ConnectWaitFor.run : time diff = "+(System.currentTimeMillis()-startTime));
            System.err.println("ConnectWaitFor.run : expected full timeout = "+(max_tries*(delay+timeout)));
            Thread.dumpStack();
            System.err.println("ConnectWaitFor.run : cancelling socketFuture="+socketFuture);
            socketFuture.cancelAll(false);
            // interrupted so quit
        }
    }
    
    private void tryConnect() {
        try {
            tries++;
            if (!socketFuture.isDone()) {
                Socket socket = new Socket();
                socket.connect(endpoint, timeout);
                socketFuture.complete(socket);
                System.out.println("Connected to  "+host+":"+port+" after "+(System.currentTimeMillis()-startTime)+" ms");
            }
        } catch (Exception exception) {
            lastException = exception;
        }
    }

    public XFuture<Socket> getSocketFuture() {
        return socketFuture;
    }
    
    public void cancel() {
        if(!socketFuture.isDone()) {
            Thread.dumpStack();
            System.err.println("ConnectWaitFor.cancel : cancelling socketFuture="+socketFuture);
        socketFuture.cancelAll(false);
        }
        thread.interrupt();
    }

}
