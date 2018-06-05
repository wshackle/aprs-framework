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
package aprs.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class SocketLineReader {

    private SocketLineReader() {
    }

    public interface CallBack {

        public void call(String line, PrintStream ps);
    }
    @Nullable
    private volatile ServerSocket serverSocket = null;

    public boolean isConnected() {
        if (null != socket) {
            return socket.isConnected();
        } else {
            return null != serverSocket && serverSocket.isBound() && !serverSocket.isClosed();
        }
    }

    private class Clnt {

        @Nullable
        volatile Socket socket;
        @Nullable
        volatile Thread thread;
//        @Nullable volatile BufferedReader br;
//        @Nullable volatile PrintStream ps;

        void close() {
            try {
                if (null != thread) {
                    thread.interrupt();
                    thread.join(200);
                }
            } catch (Exception exception) {
            }
//            try {
//                if (null != br) {
//                    br.close();
//                }
//            } catch (Exception exception) {
//            }
            try {
                if (null != socket) {
                    socket.close();
                }
            } catch (Exception exception) {
            }
//            br = null;
            socket = null;
            thread = null;
        }

        @SuppressWarnings("deprecation")
        @Override
        protected void finalize() {
            close();
        }

        @Override
        public String toString() {
            return "Clnt{" + "socket=" + socket + ", thread=" + thread + '}';
        }
    }
    @Nullable
    private volatile List<Clnt> als = null;
    @Nullable
    private volatile Socket socket = null;
    @Nullable
    private volatile Thread thread = null;
//    @Nullable private volatile BufferedReader br = null;
//    @Nullable private volatile PrintStream ps;
    private SocketLineReader.@Nullable CallBack cb;

    public int getPort() {
        if (socket == null) {
            return port;
        }
        int socketPort = socket.getPort();
        this.port = socketPort;
        return socketPort;
    }

    @Nullable
    public String getHost() {
        return host;
    }

    @Nullable
    private String host = null;

    private int port = -1;

    private SocketLineReader privateStart(boolean isClient,
            @Nullable String host, int port, final String threadname,
            SocketLineReader.CallBack _cb) throws IOException {
        cb = _cb;
        this.port = port;
        if (isClient) {
            if (null == host) {
                throw new IllegalArgumentException("host is null and isClient is true");
            }
            if (null == _cb) {
                throw new IllegalArgumentException("callback is null and isClient is true");
            }
            socket = new Socket();
            this.host = host;
            socket.connect(new InetSocketAddress(host, port), 500);
            socket.setReuseAddress(true);

            final Socket finalSocket = socket;
//            br = brl;
//            PrintStream psl = new PrintStream(socket.getOutputStream());
//            ps = psl;
            thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try (BufferedReader brl = new BufferedReader(new InputStreamReader(finalSocket.getInputStream()));
                            PrintStream psl = new PrintStream(finalSocket.getOutputStream())) {
                        String line = null;
                        while (null != (line = brl.readLine()) && !Thread.currentThread().isInterrupted()) {
                            _cb.call(line, psl);
                        }
                    } catch (Exception exception) {
                        if (!closing) {
                            exception.printStackTrace();
                        }
                    }
                }
            }, threadname);
            thread.start();
        } else {
            ServerSocket lss = new ServerSocket(port);
            serverSocket = lss;
            lss.setReuseAddress(true);
            ArrayList<Clnt> lcals = new ArrayList<Clnt>();
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
                                        String line = null;
                                        while (null != (line = lcbr.readLine()) && !Thread.currentThread().isInterrupted() && !closing) {
                                            _cb.call(line, lcps);
                                        }
                                    } catch (SocketException exception) {
                                        if (null != c && (null != c.socket || null != c.thread)) {
                                            System.err.println("Closing client socket " + c);
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
                            exception.printStackTrace();
                        }
                    }
                }
            }, threadname + "Listener");
            thread.setDaemon(true);
            thread.start();
        }
        return this;
    }

    public static SocketLineReader startServer(int port, final String threadname,
            SocketLineReader.CallBack _cb) throws IOException {
        SocketLineReader slr = new SocketLineReader();
        return slr.privateStart(false, null, port, threadname, _cb);
    }

    public static SocketLineReader startClient(
            String host, int port, final String threadname,
            SocketLineReader.CallBack _cb) throws IOException {
        SocketLineReader slr = new SocketLineReader();
        return slr.privateStart(true, host, port, threadname, _cb);
    }

    private volatile boolean closing = false;

    public void close() {
        closing = true;
        try {
            if (null != thread) {
                thread.interrupt();
                thread.join(200);
            }
        } catch (Exception e) {
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
        } catch (Exception e) {
        }

        try {
            if (null != serverSocket) {
                serverSocket.close();
            }
        } catch (Exception e) {
        }

        try {
            List<Clnt> lals = als;
            if (null != lals) {
                for (int i = 0; i < lals.size(); i++) {
                    Clnt c = lals.get(i);
                    c.close();
                }
            }
        } catch (Exception e) {
        }
        socket = null;
        thread = null;
        als = null;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void finalize() {
        close();
    }

    @Override
    public String toString() {
        return "SocketLineReader{" + "serverSocket=" + serverSocket + ", socket=" + socket + ", thread=" + thread + ", host=" + host + ", port=" + port + ", closing=" + closing + '}';
    }

}
