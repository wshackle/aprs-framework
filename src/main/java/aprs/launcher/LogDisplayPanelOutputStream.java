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

import aprs.logdisplay.LogDisplayJPanel;
import aprs.misc.Utils;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author shackle
 */
public class LogDisplayPanelOutputStream extends OutputStream {

    final private LogDisplayJPanel logDisplayJPanel;

    LogDisplayPanelOutputStream(LogDisplayJPanel logDisplayJInternalFrame, List<LineConsumer> lineConsumers) {
        this.logDisplayJPanel = logDisplayJInternalFrame;
        if (null == logDisplayJInternalFrame) {
            throw new IllegalArgumentException("logDisplayJInteralFrame may not be null");
        }
        this.lineConsumers = new ArrayList<>(lineConsumers);
    }

    private StringBuffer sb = new StringBuffer();

    private final List<LineConsumer> lineConsumers;

    @Override
    public String toString() {
        return "LogDisplayPanelOutputStream{" + "logDisplayJPanel=" + logDisplayJPanel + ", sb=" + sb + ", lineConsumers=" + lineConsumers + '}';
    }

    private void notifiyLineConsumers(String line) {
//            System.out.println("line = " + line);
//            System.out.println("lineConsumers = " + lineConsumers);
        for (int i = 0; i < lineConsumers.size(); i++) {
            LineConsumer consumer = lineConsumers.get(i);
            if (consumer.isFinished()) {
                lineConsumers.remove(consumer);
            }
        }
        for (LineConsumer consumer : lineConsumers) {
            consumer.accept(line);
        }
        for (int i = 0; i < lineConsumers.size(); i++) {
            LineConsumer consumer = lineConsumers.get(i);
            if (consumer.isFinished()) {
                lineConsumers.remove(consumer);
            }
        }
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        if (null != logDisplayJPanel) {
            final String s = new String(buf, off, len);
            sb.append(s);
            if (s.contains("\n")) {
                String fullString = sb.toString();
                notifiyLineConsumers(fullString);
                Utils.runOnDispatchThread(() -> {
                    logDisplayJPanel.appendText(fullString);
                });
                sb = new StringBuffer();
            }
        }
    }

    @Override
    public void write(int b) throws IOException {
        if (b < 0 || b > 255) {
            throw new IOException("bad byte = " + b);
        }
        byte buf[] = new byte[1];
        buf[0] = (byte) b;
        this.write(buf, 0, 1);
    }
}
