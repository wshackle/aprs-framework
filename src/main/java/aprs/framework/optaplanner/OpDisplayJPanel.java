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
import aprs.framework.optaplanner.actionmodel.OpActionType;
import static aprs.framework.optaplanner.actionmodel.OpActionType.DROPOFF;
import static aprs.framework.optaplanner.actionmodel.OpActionType.END;
import static aprs.framework.optaplanner.actionmodel.OpActionType.FAKE_DROPOFF;
import static aprs.framework.optaplanner.actionmodel.OpActionType.PICKUP;
import static aprs.framework.optaplanner.actionmodel.OpActionType.START;
import aprs.framework.optaplanner.actionmodel.OpEndAction;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;

/**
 * Class for displaying either a solved or initial OpActionPlan, plotting the 2D
 * route for the robot to pickup and dropoff some set of parts at the given
 * destinations.
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class OpDisplayJPanel extends JPanel {

    /**
     * Show a window showing the
     *
     * @param plan plan to show
     * @param title title of new window
     */
    public static void showPlan(OpActionPlan plan, String title) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            JFrame frm = new JFrame();
            frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frm.add(new OpDisplayJPanel(plan));
            frm.pack();
            frm.setSize(new Dimension(600, 600));
            frm.setTitle(title);
            frm.setVisible(true);
        });
    }

    /**
     * Show a default test plan for testing.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        showPlan(createTestInitPlan(), "testInit");
    }

    private final MouseMotionListener mml = new MouseMotionListener() {
        @Override
        public void mouseDragged(MouseEvent e) {
            OpDisplayJPanel.this.mouseDragged(e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            OpDisplayJPanel.this.mouseMoved(e);
        }
    };

    /**
     * Create OpDisplayJPanel from an existing plan.
     *
     * @param opActionPlan plan to show
     */
    public OpDisplayJPanel(OpActionPlan opActionPlan) {
        this.opActionPlan = opActionPlan;
        privateInit();
    }

    private void privateInit() {
        this.setBackground(Color.white);
        ToolTipManager.sharedInstance().registerComponent(this);
        this.addMouseMotionListener(mml);
    }

    /**
     * Create a OpDisplayJPanel showing a default test plan.
     *
     */
    public OpDisplayJPanel() {
        OpActionPlan ap = createTestInitPlan();
        this.opActionPlan = ap;
        privateInit();
    }

    static private OpActionPlan createTestInitPlan() {
        List<OpAction> initList = Arrays.asList(
                new OpAction("pickup A3", 5.1, 0.2, PICKUP, "A"),
                new OpAction("dropoff A3", 6.3, 0.4, DROPOFF, "A"),
                new OpAction("Start", 0, 0, START, "START"),
                new OpAction("pickup A1", 1.5, 0.6, PICKUP, "A"),
                new OpAction("dropoff A1", 2.7, 0.8, DROPOFF, "A"),
                new OpAction("pickup A2", 3.9, 0.1, PICKUP, "A"),
                new OpAction("dropoff A2", 4.2, 0.3, DROPOFF, "A"),
                new OpAction("pickup B3", 5.4, 1.5, PICKUP, "B"),
                new OpAction("dropoff B3", 6.6, 1.7, DROPOFF, "B"),
                new OpAction("pickup B1", 1.8, 1.9, PICKUP, "B"),
                new OpAction("dropoff B1", 2.1, 1.2, DROPOFF, "B"),
                new OpAction("pickup B2", 3.3, 1.4, PICKUP, "B"),
                new OpAction("dropoff B2", 4.5, 1.6, DROPOFF, "B")
        );
        OpActionPlan ap = new OpActionPlan();
        ap.setActions(initList);
        ap.getEndAction().setLocation(new Point2D.Double(7, 0));
        ap.initNextActions();
        return ap;
    }

    /**
     * Clear the map associating parts carried with colors.
     */
    public static void clearColorMap() {
        partsColorsMap.clear();
    }

    private static final ConcurrentHashMap<String, Color> partsColorsMap = new ConcurrentHashMap<>();

    private static Color[] colors = new Color[]{
        Color.BLUE,
        Color.CYAN,
        Color.GREEN,
        Color.MAGENTA,
        Color.PINK,
        Color.ORANGE,
        Color.RED,
        Color.YELLOW
    };

    private static Color getColor(String partName) {
        return partsColorsMap.computeIfAbsent(partName, (k) -> colors[partsColorsMap.size() % colors.length]);
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
        if (null != opActionPlan) {
            this.repaint();
        }
    }

    private double minX, minY, maxX, maxY, xdiff, ydiff;

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        super.paintComponent(g);

        if (null != label && null != labelPos) {
            Font origFont = g.getFont();
            if (null != labelFont) {
                g.setFont(labelFont);
            }
            g.drawString(label, labelPos.x, labelPos.y);
            g.setFont(origFont);
        }
        if (null == opActionPlan
                || null == opActionPlan.getActions()
                || opActionPlan.getActions().isEmpty()) {
            return;
        }
        MutableMap<OpActionType, Integer> typeCountMap
                = Lists.adapt(opActionPlan.getActions())
                        .countBy(OpAction::getActionType)
                        .toMapOfItemToCount();

        Dimension dim = this.getSize();
        if (dim.width < 1 || dim.height < 1) {
            System.err.println("bad dim =" + dim.width + " x " + dim.height);
            return;
        }
        if (null != opActionPlan) {
            OpEndAction endAction = opActionPlan.getEndAction();
            List<OpAction> actions = opActionPlan.getActions();
            if (null != endAction && null != actions) {
                minX = Double.POSITIVE_INFINITY;
                minY = Double.POSITIVE_INFINITY;
                maxX = Double.NEGATIVE_INFINITY;
                maxY = Double.NEGATIVE_INFINITY;
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
                xdiff = (maxX - minX);
                ydiff = (maxY - minY);

                OpActionInterface prevAction = null;
                OpActionInterface action = opActionPlan.findStartAction();
                int prevx = -1;
                int prevy = -1;
                int ly = 0;
                int h = dim.height;
                int w = keyVisible ? (dim.width - keyWidth) : dim.width;
                if (labelPos != null) {
                    ly = labelPos.y;
                    h = dim.height - ly;
                }
                Set<OpAction> numLabeledItems = new HashSet<>();
                for (OpAction actionToPaintBackground : opActionPlan.getActions()) {
                    int x = keyWidth + (int) ((0.9 * (actionToPaintBackground.getLocation().x - minX) / xdiff) * w + 0.05 * w);
                    int y = ly + (int) ((0.9 * (actionToPaintBackground.getLocation().y - minY) / ydiff) * h + 0.05 * h);
                    paintBackgroundSymbol(g2d, x, y, actionToPaintBackground);
                    if (!numLabeledItems.contains(actionToPaintBackground)) {
                        List<OpAction> closeItems = findCloseActions(x, y);
                        if (closeItems.size() > 1) {
                            numLabeledItems.add(actionToPaintBackground);
                            numLabeledItems.addAll(closeItems);
                            g2d.drawString(""+closeItems.size(), x+15, y+15);
                        }
                    }
                }
                while (action != null && action != prevAction) {
                    if (action.getActionType() == OpActionType.FAKE_DROPOFF) {
                        action = action.getNext();
                        continue;
                    }
                    int x = keyWidth + (int) ((0.9 * (action.getLocation().x - minX) / xdiff) * w + 0.05 * w);
                    int y = ly + (int) ((0.9 * (action.getLocation().y - minY) / ydiff) * h + 0.05 * h);
                    double l = Math.hypot(x - prevx, y - prevy);
                    double dx = 25 * (x - prevx) / l;
                    double dy = 25 * (y - prevy) / l;
                    int idx = (int) dx;
                    int idy = (int) dy;
                    if (actionNamesVisible) {
                        g2d.drawString(action.getName(), x + 10, y + 10);
                    } else {
                        paintActionSymbol(g2d, x, y, action.getActionType());
                    }
                    if (action.getNext().getActionType() == OpActionType.FAKE_DROPOFF) {
                        action = action.getNext();
                        continue;
                    }
                    if (null != prevAction && prevx > 0 && prevy > 0) {
                        Color origColor = g2d.getColor();
                        Stroke origStroke = g2d.getStroke();
                        if (action.getActionType() == DROPOFF) {
                            g2d.setColor(getColor(action.getPartType()));
                            if (null != carryStroke) {
                                g2d.setStroke(carryStroke);
                            }
                        }

                        g2d.drawLine(x - idx, y - idy, prevx + idx, prevy + idy);

                        AffineTransform origTransform = g2d.getTransform();

                        g2d.translate(x - idx, y - idy);
                        g2d.rotate(Math.atan2(prevx - x, y - prevy));

                        g2d.fill(arrowHead);

//                        g2d.draw(actionTypeShapeMap.get(action.getActionType()));
                        g2d.setTransform(origTransform);
                        g2d.setColor(origColor);
                        g2d.setStroke(origStroke);
                    }
                    prevAction = action;
                    prevx = x;
                    prevy = y;
                    action = action.getNext();
                }
            }
        }
        
        if (keyVisible) {
            int keyY = 25;
            g2d.drawLine(10, 10, 30, 10);
            g2d.drawString("(empty)", 40, 10);
            for (Map.Entry<String, Color> entry : partsColorsMap.entrySet()) {
                Color origColor = g2d.getColor();
                Stroke origStroke = g2d.getStroke();
                g2d.setColor(entry.getValue());
                g2d.drawLine(10, keyY, 30, keyY);
                g2d.setColor(origColor);
                g2d.setStroke(origStroke);
                g2d.drawString(entry.getKey(), 40, keyY);
                keyY += 15;
            }
            if (!actionNamesVisible) {
                keyY += 25;
                for (OpActionType type : OpActionType.values()) {
                    Integer typeCount = typeCountMap.get(type);
                    if (typeCount == null || typeCount == 0) {
                        if (type == END) {
                            typeCount = 1;
                        } else {
                            continue;
                        }
                    }
                    if (type == FAKE_DROPOFF) {
                        continue;
                    }
                    int x = 10;
                    paintActionSymbol(g2d, x, keyY, type);
                    g2d.drawString(type.toString(), 40, keyY);
                    g2d.drawString(": " + typeCount, 100, keyY);
                    keyY += 25;
                }
            }
        }
    }

    private boolean keyVisible = true;

    /**
     * Get the value of keyVisible
     *
     * @return the value of keyVisible
     */
    public boolean isKeyVisible() {
        return keyVisible;
    }

    /**
     * Set the value of keyVisible
     *
     * @param keyVisible new value of keyVisible
     */
    public void setKeyVisible(boolean keyVisible) {
        this.keyVisible = keyVisible;
    }

    private int keyWidth = 110;

    /**
     * Get the value of keyWidth
     *
     * @return the value of keyWidth
     */
    public int getKeyWidth() {
        return keyWidth;
    }

    /**
     * Set the value of keyWidth
     *
     * @param keyWidth new value of keyWidth
     */
    public void setKeyWidth(int keyWidth) {
        this.keyWidth = keyWidth;
    }

    private void paintBackgroundSymbol(Graphics2D g2d, int x, int y, OpAction action) {
        if (null == action.getPartType() || action.getPartType().length() < 1) {
            return;
        }

        switch (action.getActionType()) {
            case FAKE_DROPOFF:
                return;
            case START:
                return;
            case END:
                return;
            default:
                break;
        }

        AffineTransform origTransform = g2d.getTransform();
        g2d.translate(x - 7, y - 15);
        Color origColor = g2d.getColor();
        g2d.setColor(OpDisplayJPanel.getColor(action.getPartType()));
        g2d.fill(circleLetterShape);
        g2d.setColor(origColor);
        g2d.setTransform(origTransform);
    }

    private void paintActionSymbol(Graphics2D g2d, int x, int y, OpActionType type) {
        AffineTransform origTransform = g2d.getTransform();
        g2d.translate(x - 7, y - 15);
        g2d.draw(circleLetterShape);
        g2d.setTransform(origTransform);
        g2d.drawString(type.name().substring(0, 1), x, y);
    }

    private Shape arrowHead = new Polygon(new int[]{-10, 0, 10}, new int[]{0, 10, 0}, 3);

    private final Arc2D.Double circleLetterShape
            = new Arc2D.Double(0, 0, 20, 20, 0, 360, Arc2D.OPEN);

    private boolean actionNamesVisible = false;

    /**
     * Get the value of actionNamesVisible
     *
     * @return the value of actionNamesVisible
     */
    public boolean isActionNamesVisible() {
        return actionNamesVisible;
    }

    /**
     * Set the value of actionNamesVisible
     *
     * @param actionNamesVisible new value of actionNamesVisible
     */
    public void setActionNamesVisible(boolean actionNamesVisible) {
        this.actionNamesVisible = actionNamesVisible;
    }

    private Stroke carryStroke = new BasicStroke(3f);

    /**
     * Get the value of carryStroke
     *
     * @return the value of carryStroke
     */
    public Stroke getCarryStroke() {
        return carryStroke;
    }

    /**
     * Set the value of carryStroke
     *
     * @param carryStroke new value of carryStroke
     */
    public void setCarryStroke(Stroke carryStroke) {
        this.carryStroke = carryStroke;
    }

    private String label;

    /**
     * Get the value of label
     *
     * @return the value of label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Set the value of label
     *
     * @param label new value of label
     */
    public void setLabel(String label) {
        this.label = label;
        this.repaint();
    }

    private Point labelPos;

    /**
     * Get the value of labelPos
     *
     * @return the value of labelPos
     */
    public Point getLabelPos() {
        return labelPos;
    }

    /**
     * Set the value of labelPos
     *
     * @param labelPos new value of labelPos
     */
    public void setLabelPos(Point labelPos) {
        this.labelPos = labelPos;
    }

    private Font labelFont;

    /**
     * Get the value of labelFont
     *
     * @return the value of labelFont
     */
    public Font getLabelFont() {
        return labelFont;
    }

    /**
     * Set the value of labelFont
     *
     * @param labelFont new value of labelFont
     */
    public void setLabelFont(Font labelFont) {
        this.labelFont = labelFont;
    }

    private List<OpAction> findCloseActions(int x, int y) {
        Dimension dim = this.getSize();
        if (dim.width < 1 || dim.height < 1 || null == opActionPlan || null == opActionPlan.getActions()) {
            return Collections.emptyList();
        }
        int ly = 0;
        int h = dim.height;
        int w = keyVisible ? (dim.width - keyWidth) : dim.width;
        if (labelPos != null) {
            ly = labelPos.y;
            h = dim.height - ly;
        }
        List<OpAction> closeActions = new ArrayList<>();
        for (OpAction actionToCheck : opActionPlan.getActions()) {
            if(actionToCheck.getActionType() == FAKE_DROPOFF) {
                continue;
            }
            int actionX = keyWidth + (int) ((0.9 * (actionToCheck.getLocation().x - minX) / xdiff) * w + 0.05 * w);
            int actionY = ly + (int) ((0.9 * (actionToCheck.getLocation().y - minY) / ydiff) * h + 0.05 * h);
            if (Math.abs(actionX - x) < 30 && Math.abs(actionY - y) < 30) {
                closeActions.add(actionToCheck);
            }
        }
        return closeActions;
    }

    private void mouseDragged(MouseEvent e) {
        this.setToolTipText(findCloseActions(e.getX(), e.getY()).toString());
    }

    private void mouseMoved(MouseEvent e) {
        this.setToolTipText(findCloseActions(e.getX(), e.getY()).toString());
    }

}
