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
package aprs.framework.tomcat;

import com.bsb.common.vaadin.embed.EmbedVaadinServer;
import com.bsb.common.vaadin.embed.support.EmbedVaadin;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This was an experimental effort to embed launch the vaadin webapp from within
 * aprs-framework. Currently on-hold.
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class CRCLWebAppRunner implements Runnable {

//    public static void main(String[] args) throws LifecycleException {
//        Tomcat tomcat = new Tomcat();
//        tomcat.setPort(8080);
//
//        Context ctx = tomcat.addContext("/", new File(".").getAbsolutePath());
//
//        Tomcat.addServlet(ctx, "hello", new HttpServlet() {
//            protected void service(HttpServletRequest req, HttpServletResponse resp)
//                    throws ServletException, IOException {
//                Writer w = resp.getWriter();
//                w.write("Hello, World!");
//                w.flush();
//            }
//        });
//        ctx.addServletMapping("/*", "hello");
//        
//        Tomcat.addServlet(ctx, "hello", new crcl.vaadin.webapp.CrclClientUI().getUI());
//        ctx.addServletMapping("/*", "hello");
//
//        tomcat.start();
//        tomcat.getServer().await();
//    }
    public static void main(String[] args) {
        new CRCLWebAppRunner().run();
    }

    public void stop() {
        if (null != svr) {
            svr.stop();
            svr = null;
        }
        if(null != crclWebServerThread) {
            crclWebServerThread.interrupt();
            crclWebServerThread = null;
        }
    }

    private int httpPort = 8081;

    /**
     * Get the value of httpPort
     *
     * @return the value of httpPort
     */
    public int getHttpPort() {
        return httpPort;
    }

    /**
     * Set the value of httpPort
     *
     * @param httpPort new value of httpPort
     */
    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    private EmbedVaadinServer svr = null;
    
    private Thread crclWebServerThread=null;
    
    public void start() {
        stop();
        crclWebServerThread = new Thread(this,"crclWebServerThread");
        crclWebServerThread.start();
    }
    
    @Override
    public void run() {
        System.out.println("Creating instance of rcl.vaadin.webapp.CrclClientUI and starting it with EmbedVaadin.");
        svr = EmbedVaadin.forComponent((new crcl.vaadin.webapp.CrclClientUI()).getUI())
                .withTheme("default_theme")
                .withHttpPort(httpPort)
                .openBrowser(true)
                .start();
        System.out.println("EmbedVaadin svr created :"+svr);
    }

}
