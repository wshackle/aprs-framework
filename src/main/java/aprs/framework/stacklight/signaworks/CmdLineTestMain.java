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
package aprs.framework.stacklight.signaworks;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class CmdLineTestMain {

    public static void main(String[] args) throws SocketException, IOException {
        Socket socket = new Socket("192.168.1.77", 20000);
//        socket.setSoTimeout(20);
        byte readreqbuf[] = new byte[10];
        byte writereqbuf[] = new byte[10];
        byte ackbuff[] = new byte[10];

        // Protocol notes. Always sending and recieving 10 bytes
        // the first byte is either 'R' or 'W' to either set the lights ('W') or get
        // gurrent state ('R').
        // For a read the other 9 bytes seem to not matter so 
        // I just leave them 0. 
        readreqbuf[0] = 'R';

        socket.getOutputStream().write(readreqbuf);
        System.out.println("socket.getOutputStream().write(" + Arrays.toString(readreqbuf) + ") returned.");

        System.out.println("Calling read() on socket. (It may wait indefinitely)  ...");
        socket.getInputStream().read(ackbuff);
        System.out.println("Recieved ackbuff = " + Arrays.toString(ackbuff));

        writereqbuf[0] = 'W';
        writereqbuf[1] = 0; // 0 - 5 indicate diffent sound groups (only matters if sound is turned on with buf[7]
        writereqbuf[2] = 1; // RED(top): 0 = off , 1 = on , 2 = blink 
        writereqbuf[3] = 2; // ORANGE: 0 = off , 1 = on , 2 = blink 
        writereqbuf[4] = 1; // GREEN: 0 = off , 1 = on , 2 = blink 
        writereqbuf[5] = 2; // BLUE: 0 = off , 1 = on , 2 = blink 
        writereqbuf[6] = 1; // CLEAR(bottom): 0 = off , 1 = on , 2 = blink 
        writereqbuf[7] = 0; // SOUND 0=off , 1-5 different sout
        writereqbuf[8] = 0; // Spare
        writereqbuf[9] = 0; // Spare

        socket.getOutputStream().write(writereqbuf);
        System.out.println("socket.getOutputStream().write(" + Arrays.toString(writereqbuf) + ") returned.");

        socket.getOutputStream().write(readreqbuf);
        System.out.println("socket.getOutputStream().write(" + Arrays.toString(readreqbuf) + ") returned.");

        System.out.println("Calling read() on socket. (It may wait indefinitely)  ...");
        socket.getInputStream().read(ackbuff);
        System.out.println("Receved ackbuf = " + Arrays.toString(ackbuff));
    }
}
