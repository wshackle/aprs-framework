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
package aprs.framework.optaplanner;

import aprs.framework.optaplanner.actionmodel.OpAction;
import aprs.framework.optaplanner.actionmodel.OpActionInterface;
import aprs.framework.optaplanner.actionmodel.OpActionPlan;
import static aprs.framework.optaplanner.actionmodel.OpActionType.DROPOFF;
import static aprs.framework.optaplanner.actionmodel.OpActionType.PICKUP;
import static aprs.framework.optaplanner.actionmodel.OpActionType.START;
import aprs.framework.optaplanner.actionmodel.OpEndAction;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class OpDisplayJPanel extends JPanel {

    
    
    public static void showPlan(OpActionPlan p, String title) {
        javax.swing.SwingUtilities.invokeLater(()-> {
            JFrame frm = new JFrame();
            frm.add(new OpDisplayJPanel(p));
            frm.pack();
            frm.setSize(new Dimension(600,600));
            frm.setTitle(title);
            frm.setVisible(true);
        });
    }
    
    public static void main(String[] args) {
        showPlan(createTestInitPlan(),"testInit");
    }
    
    public OpDisplayJPanel(OpActionPlan opActionPlan) {
        this.opActionPlan = opActionPlan;
    }

    public OpDisplayJPanel() {
        OpActionPlan ap = createTestInitPlan();
        this.opActionPlan = ap;
    }

    static private OpActionPlan createTestInitPlan() {
        List<OpAction> initList = Arrays.asList(
                new OpAction("pickup A3", 5, 0, PICKUP, "A"),
                new OpAction("dropoff A3", 6, 0, DROPOFF, "A"),
                new OpAction("Start", 0, 0, START, "START"),
                new OpAction("pickup A1", 1, 0, PICKUP, "A"),
                new OpAction("dropoff A1", 2, 0, DROPOFF, "A"),
                new OpAction("pickup A2", 3, 0, PICKUP, "A"),
                new OpAction("dropoff A2", 4, 0, DROPOFF, "A"),
                new OpAction("pickup B3", 5, 1, PICKUP, "B"),
                new OpAction("dropoff B3", 6, 1, DROPOFF, "B"),
                new OpAction("pickup B1", 1, 1, PICKUP, "B"),
                new OpAction("dropoff B1", 2, 1, DROPOFF, "B"),
                new OpAction("pickup B2", 3, 1, PICKUP, "B"),
                new OpAction("dropoff B2", 4, 1, DROPOFF, "B")
        );
        OpActionPlan ap = new OpActionPlan();
        ap.setActions(initList);
        ap.getEndAction().setLocation(new Point2D.Double(7, 0));
        ap.initNextActions();
        return ap;
    }

    
    
    private OpActionPlan opActionPlan;

    /**
     * Get the value of opActionPlan
     *
     * @return the value of opActionPlan
     */
    public OpActionPlan getOpActionPlan() {
        return opActionPlan;
    }

    /**
     * Set the value of opActionPlan
     *
     * @param opActionPlan new value of opActionPlan
     */
    public void setOpActionPlan(OpActionPlan opActionPlan) {
        this.opActionPlan = opActionPlan;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Dimension dim = this.getSize();
        if (dim.width < 1 || dim.height < 1) {
            System.err.println("bad dim =" + dim.width + " x " + dim.height);
            return;
        }
        if (null != opActionPlan) {
            OpEndAction endAction = opActionPlan.getEndAction();
            List<OpAction> actions = opActionPlan.getActions();
            if (null != endAction && null != actions) {
                double minX = Double.POSITIVE_INFINITY;
                double minY = Double.POSITIVE_INFINITY;
                double maxX = Double.NEGATIVE_INFINITY;
                double maxY = Double.NEGATIVE_INFINITY;
                if (null != endAction.getLocation()) {
                    Point2D.Double endloc = endAction.getLocation();
                    if (minX > endloc.x) {
                        minX = endloc.x;
                    }
                    if (minY > endloc.y) {
                        minY = endloc.y;
                    }
                    if (maxX < endloc.x) {
                        maxX = endloc.x;
                    }
                    if (maxY < endloc.y) {
                        maxY = endloc.y;
                    }
                }
                for (OpAction action : actions) {
                    Point2D.Double loc = action.getLocation();
                    if (minX > loc.x) {
                        minX = loc.x;
                    }
                    if (minY > loc.y) {
                        minY = loc.y;
                    }
                    if (maxX < loc.x) {
                        maxX = loc.x;
                    }
                    if (maxY < loc.y) {
                        maxY = loc.y;
                    }
                }
                double xdiff = (maxX - minX);
                double ydiff = (maxY - minY);
                Graphics2D g2d = (Graphics2D) g;
                OpActionInterface prevAction = null;
                OpActionInterface action = opActionPlan.findStartAction();
                int prevx = -1;
                int prevy= -1;
                while (action != null && action != prevAction) {
                    int x = (int) ((0.9 * (action.getLocation().x - minX) / xdiff) * dim.width + 0.05 * dim.width);
                    int y = (int) ((0.9 * (action.getLocation().y - minY) / ydiff) * dim.height + 0.05 * dim.height);
                    g2d.drawString(action.getName(), x+10, y+10);
                    if(null != prevAction && prevx > 0 && prevy > 0) {
                        g2d.drawLine(x, y, prevx, prevy);
                    }
                    prevAction = action;
                    prevx = x;
                    prevy = y;
                    action = action.getNext();
                }
            }
        }
    }

}
