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

import crcl.utils.CRCLUtils;
import crcl.utils.XFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class SocketLineReader {

    private SocketLineReader() {
    }

    public interface CallBack {

        @SuppressWarnings("unused")
        public void call(String line, @Nullable PrintStream ps);
    }
    private volatile @Nullable
    ServerSocket serverSocket = null;

    public boolean isConnected() {
        if (null != socket) {
            return socket.isConnected();
        } else {
            return null != serverSocket && serverSocket.isBound() && !serverSocket.isClosed();
        }
    }

    private static class Clnt {

        volatile @Nullable
        Socket socket;

        volatile @Nullable
        Thread thread;

        void close() {
            try {
                if (null != thread) {
                    thread.interrupt();
                    thread.join(200);
                }
            } catch (Exception ignored) {
            }
            try {
                if (null != socket) {
                    socket.close();
                }
            } catch (Exception ignored) {
            }
            socket = null;
            thread = null;
        }

        @Override
        protected void finalize() {
            close();
        }

        @Override
        public String toString() {
            return "Clnt{" + "socket=" + socket + ", thread=" + thread + '}';
        }
    }
    private volatile @Nullable
    List<Clnt> als = null;
    private volatile @Nullable
    Socket socket = null;
    private volatile @Nullable
    Thread thread = null;

    private SocketLineReader.@Nullable CallBack cb;

    public int getPort() {
        if (socket == null) {
            return port;
        }
        int socketPort = socket.getPort();
        this.port = socketPort;
        return socketPort;
    }

    public @Nullable
    String getHost() {
        return host;
    }

    private @Nullable
    String host = null;

    private int port = -1;

    private static final ConcurrentHashMap<Integer, StackTraceElement[]> startersMap = new ConcurrentHashMap<>();

    private static void printStartersMap() {
        System.out.println("startersMap.size() = " + startersMap.size());
        System.out.println("Begin StartersMap : ");
        for (Map.Entry<Integer, StackTraceElement[]> entry : startersMap.entrySet()) {
            System.out.println("entry.getKey() = " + entry.getKey());
            System.out.println("entry.getValue() = " + XFuture.traceToString(entry.getValue()));
        }
        System.out.println("End StartersMap");

    }

    private void privateStartServer(
            int portParam,
            final String threadname,
            CallBack _cb)
            throws IOException {

        try {
            this.cb = _cb;
            this.port = portParam;
            ServerSocket lss;
            try {
                lss = new ServerSocket(portParam);
                serverSocket = lss;
                lss.setReuseAddress(true);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                Runtime.getRuntime().exec("netstat -naptee");
                System.out.println("portParam = " + portParam);
                printStartersMap();
                try (Socket socket = new Socket("localhost", portParam)) {
                    socket.getOutputStream().write("!!!quit!!!\n".getBytes());
                    socket.getOutputStream().flush();
                }
                throw ioe;
            }
            startersMap.put(portParam, Thread.currentThread().getStackTrace());
            ArrayList<Clnt> lcals = new ArrayList<>();
            als = lcals;
            thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        while (!Thread.currentThread().isInterrupted() && !closing) {
                            final Clnt c = new Clnt();
                            Socket lcs = lss.accept();
                            c.socket = lcs;
                            Thread lct = new Thread(new Runnable() {

                                @Override
                                public void run() {
                                    try (BufferedReader lcbr = new BufferedReader(new InputStreamReader(lcs.getInputStream()));
                                            PrintStream lcps = new PrintStream(lcs.getOutputStream())) {
                                        String line;
                                        while (null != (line = lcbr.readLine()) && !Thread.currentThread().isInterrupted() && !closing) {
                                            if (line.contains("!!!quit!!!")) {
                                                throw new RuntimeException("!!!quit!!! recieved");
                                            }
                                            _cb.call(line, lcps);
                                        }
                                    } catch (SocketException exception) {
                                        if (null != c && (null != c.socket || null != c.thread)) {
                                            if (!closing) {
                                                System.err.println("Closing client socket " + c);
                                            }
                                        }
                                        try {
                                            if (null != socket) {
                                                socket.close();
                                            }
                                        } catch (Exception ex) {
                                            // ignore
                                        }
                                    } catch (Exception exception) {
                                        if (!closing) {
                                            printStartersMap();
                                            exception.printStackTrace();
                                        }
                                    }
                                }

                            }, threadname);
                            lct.setDaemon(true);
                            c.thread = lct;
                            lcals.add(c);
                            lct.start();
                        }
                    } catch (Exception exception) {
                        if (!closing) {
                            printStartersMap();
                            exception.printStackTrace();
                        }
                    }
                }
            }, threadname + "Listener");
            thread.setDaemon(true);
            thread.start();
        } catch (Exception exception) {
            if (!closing) {
                printStartersMap();
                exception.printStackTrace();
            }
            if (exception instanceof IOException) {
                throw (IOException) exception;
            }
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            } else {
                throw new RuntimeException(exception);
            }
        }
    }

    private void privateStartClient(
            String host1,
            int portParam,
            final String threadname,
            CallBack _cb,
            int connectTimeOut,
            int readSoTimeOut)
            throws IllegalArgumentException, IOException {
        StackTraceElement trace[] = Thread.currentThread().getStackTrace();
        this.cb = _cb;
        this.port = portParam;
        if (null == host1) {
            throw new IllegalArgumentException("host is null and isClient is true");
        }
        if (null == _cb) {
            throw new IllegalArgumentException("callback is null and isClient is true");
        }
        socket = new Socket();
        if (readSoTimeOut > 0) {
            socket.setSoTimeout(readSoTimeOut);
        }
        this.host = host1;
        InetSocketAddress svrInetSocketAddress = new InetSocketAddress(host1, portParam);
        if (connectTimeOut > 0) {
            socket.connect(svrInetSocketAddress, connectTimeOut);
        } else {
            socket.connect(svrInetSocketAddress);
        }
        socket.setReuseAddress(true);
        final Socket finalSocket = socket;

        thread = new Thread(new Runnable() {

            @Override
            public void run() {
                long t1 = System.currentTimeMillis();
                long t0 = t1;
                int count = 0;
                String lastLine = null;
                try (BufferedReader brl = new BufferedReader(new InputStreamReader(finalSocket.getInputStream()));
                        PrintStream psl = new PrintStream(finalSocket.getOutputStream())) {
                    String line;
                    t1 = System.currentTimeMillis();
                    while (null != (line = brl.readLine()) && !Thread.currentThread().isInterrupted()) {
                        _cb.call(line, psl);
                        t1 = System.currentTimeMillis();
                        count++;
                        lastLine = line;
                    }
                } catch (Exception exception) {
                    final Logger logger = Logger.getLogger(SocketLineReader.class.getName());
                    if (!closing) {
                        long t2 = System.currentTimeMillis();
                        long timeDiff = t2 - t1;
                        long time0Diff = t2 - t0;
                        logger.severe("trace = " + CRCLUtils.traceToString(trace));
                        logger.severe("readSoTimeOut = " + readSoTimeOut);
                        final Socket socketLocal = socket;
                        logger.severe("socket = " + socketLocal);
                        if (null != socketLocal) {
                            final InputStream inputStream;
                            try {
                                if (!socketLocal.isClosed()) {
                                    logger.severe("socket.getSoTimeout() = " + socketLocal.getSoTimeout());
                                    inputStream = socketLocal.getInputStream();
                                    if (null != inputStream) {
                                        logger.severe("inputStream.available() = " + inputStream.available());
                                    }
                                }
                            } catch (IOException ex) {
                                logger.log(Level.SEVERE, "", ex);
                            }
                        }

                        logger.severe("SocketLineReader error: timeDiff = " + timeDiff + ", count=" + count + ", time0Diff=" + time0Diff);
                        logger.severe("SocketLineReader error: lastLine=" + lastLine);
                        logger.log(SEVERE,
                                "isClient=true, host=" + host + ",portParam=" + portParam + ", threadname=" + threadname + ", cb=" + _cb + ",readSoTimeOut=" + readSoTimeOut,
                                exception);
                        _cb.call("EXCEPTION:" + exception, null);
                        throw new RuntimeException(exception);
                    } else {
                        logger.log(WARNING,
                                () -> {
                                    return "aprs.database.SocketLineReader: closing=true, isClient=true, host=" + host
                                    + ",portParam=" + portParam
                                    + ", threadname=" + threadname
                                    + ", cb=" + _cb
                                    + ",readSoTimeOut=" + readSoTimeOut
                                    + ",exception.getMessage()=" + exception.getMessage();
                                });
                    }
                }
            }
        }, threadname);
        thread.start();
    }

    public static SocketLineReader startServer(
            int portParam,
            final String threadname,
            SocketLineReader.CallBack _cb) throws IOException {
        SocketLineReader slr = new SocketLineReader();
        try {
            slr.privateStartServer(portParam, threadname, _cb);
            return slr;
        } catch (RuntimeException runtimeException) {
            Logger.getLogger(SocketLineReader.class.getName())
                    .log(SEVERE,
                            "isClient=false, portParam=" + portParam + ", threadname=" + threadname + ", cb=" + _cb,
                            runtimeException);
            throw new RuntimeException(runtimeException);
        } catch (IOException iOException) {
            Logger.getLogger(SocketLineReader.class.getName())
                    .log(SEVERE,
                            "isClient=false, portParam=" + portParam + ", threadname=" + threadname + ", cb=" + _cb,
                            iOException);
            throw iOException;
        }
    }

    public static SocketLineReader startClient(
            String host, int portParam, final String threadname,
            SocketLineReader.CallBack _cb,
            int connectTimeOut,
            int readSoTimeOut
    ) throws IOException {
        SocketLineReader slr = new SocketLineReader();
        try {
            slr.privateStartClient(host, portParam, threadname, _cb, connectTimeOut, readSoTimeOut);
            return slr;
        } catch (RuntimeException runtimeException) {
            Logger.getLogger(SocketLineReader.class.getName())
                    .log(SEVERE,
                            "isClient=true, host=" + host + ",portParam=" + portParam + ", threadname=" + threadname + ", cb=" + _cb + ",connectTimeOut=" + connectTimeOut,
                            runtimeException);
            throw new RuntimeException(runtimeException);
        } catch (IOException iOException) {
            Logger.getLogger(SocketLineReader.class.getName())
                    .log(SEVERE,
                            "isClient=true, host=" + host + ",portParam=" + portParam + ", threadname=" + threadname + ", cb=" + _cb + ",connectTimeOut=" + connectTimeOut,
                            iOException);
            throw iOException;
        }
    }

    private volatile boolean closing = false;

    public void close() {
        closing = true;
        try {
            if (null != thread) {
                thread.interrupt();
                thread.join(200);
            }
        } catch (Exception ignored) {
        }
//        try {
//            if (null != br) {
//                br.close();
//            }
//        } catch (Exception e) {
//        }
//        try {
//            if (null != ps) {
//                ps.close();
//            }
//        } catch (Exception e) {
//        }
        try {
            if (null != socket) {
                socket.close();
            }
        } catch (Exception ignored) {
        }

        //noinspection CatchMayIgnoreException
        try {
            if (null != serverSocket) {
                serverSocket.close();
            }
        } catch (Exception e) {
        }

        //noinspection CatchMayIgnoreException
        try {
            List<Clnt> lals = als;
            if (null != lals) {
                for (Clnt c : lals) {
                    c.close();
                }
            }
        } catch (Exception e) {
        }
        socket = null;
        thread = null;
        als = null;
    }

    @Override
    protected void finalize() {
        close();
    }

    @Override
    public String toString() {
        return "SocketLineReader{" + "serverSocket=" + serverSocket + ", socket=" + socket + ", thread=" + thread + ", host=" + host + ", port=" + port + ", closing=" + closing + '}';
    }

}
