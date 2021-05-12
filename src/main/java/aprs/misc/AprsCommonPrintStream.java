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
package aprs.misc;

import java.io.PrintStream;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

/**
 *
 *@author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class AprsCommonPrintStream extends PrintStream {

    final private PrintStream ps;
    final private ConcurrentLinkedDeque<Consumer<String>> stringConsumers;

    AprsCommonPrintStream(PrintStream ps, ConcurrentLinkedDeque<Consumer<String>> lineConsumers) {
        super(ps, true);
        this.ps = ps;
        this.stringConsumers = lineConsumers;
    }

    private StringBuffer sb = new StringBuffer();

    @Override
    public void write(byte[] buf, int off, int len) {
        super.write(buf, off, len);
        final String s = new String(buf, off, len);
        sb.append(s);
        if (s.contains("\n")) {
            String fullString = sb.toString();
            for(Consumer<String> stringConsumer : stringConsumers) {
                stringConsumer.accept(fullString);
            }
            sb = new StringBuffer();
        }
    }
}
