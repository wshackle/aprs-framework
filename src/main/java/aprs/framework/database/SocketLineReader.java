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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

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
    private ServerSocket ss = null;

    private class Clnt {

        Socket s;
        Thread t;
        BufferedReader br;
        PrintStream ps;
        
        public void close() {
            try {
                if (null != t) {
                    t.interrupt();
                    t.join(200);
                }
            } catch (Exception exception) {
            }
            try {
                if (null != br) {
                    br.close();
                }
            } catch (Exception exception) {
            }
            try {
                if (null != s) {
                    s.close();
                }
            } catch (Exception exception) {
            }
            br = null;
            s = null;
            t = null;
        }
        
        protected void finalize() {
            close();
        }
    }
    private ArrayList<Clnt> als = null;
    private Socket s = null;
    private Thread t = null;
    private BufferedReader br = null;
    private PrintStream ps;
    private SocketLineReader.CallBack cb;

    private SocketLineReader privateStart(boolean isClient,
            String host, int port, final String threadname,
            SocketLineReader.CallBack _cb) throws IOException {
        cb = _cb;
        if (isClient) {
            s = new Socket(host, port);
            s.setReuseAddress(true);
            br = new BufferedReader(new InputStreamReader(s.getInputStream()));
            ps = new PrintStream(s.getOutputStream());
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        String line = null;
                        while (null != (line = br.readLine()) && !Thread.interrupted()) {
                            cb.call(line, ps);
                        }
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }, threadname);
            t.start();
        } else {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            als = new ArrayList<Clnt>();
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        while (!Thread.interrupted()) {
                            final Clnt c = new Clnt();
                            c.s = ss.accept();
                            c.br = new BufferedReader(new InputStreamReader(c.s.getInputStream()));
                            c.ps = new PrintStream(c.s.getOutputStream());
                            c.t = new Thread(new Runnable() {

                                @Override
                                public void run() {
                                    try {
                                        String line = null;
                                        while (null != (line = c.br.readLine()) && !Thread.interrupted()) {
                                            cb.call(line, c.ps);
                                        }
                                    } catch (Exception exception) {
                                        exception.printStackTrace();
                                    }
                                }
                            }, threadname);
                            als.add(c);
                            c.t.start();
                        }
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }, threadname + "Listener");
            t.start();
        }
        return this;
    }

    public static SocketLineReader start(boolean isClnt,
            String host, int port, final String threadname,
            SocketLineReader.CallBack _cb) throws IOException {
        SocketLineReader slr = new SocketLineReader();
        return slr.privateStart(isClnt, host, port, threadname, _cb);
    }

    public void close() {
        try {
            if (null != t) {
                t.interrupt();
                t.join(200);
            }
        } catch (Exception e) {
        }
//        try {
//            if (null != br) {
//                br.close();
//            }
//        } catch (Exception e) {
//        }
        try {
            if (null != ps) {
                ps.close();
            }
        } catch (Exception e) {
        }
        try {
            if (null != s) {
                s.close();
            }
        } catch (Exception e) {
        }
        
        
        try {
            if (null != als) {
                for(int i = 0; i < als.size(); i++) {
                    Clnt c = als.get(i);
                    c.close();
                }
            }
        } catch (Exception e) {
        }
        s = null;
        t = null;
        br = null;
        ps = null;
        als = null;
    }

    protected void finalize() {
        close();
    }

}
