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
package aprs.actions.optaplanner.display;

import aprs.actions.optaplanner.actionmodel.OpAction;
import aprs.actions.optaplanner.actionmodel.OpActionInterface;
import aprs.actions.optaplanner.actionmodel.OpActionPlan;
import aprs.actions.optaplanner.actionmodel.OpActionPlanCloner;
import aprs.actions.optaplanner.actionmodel.OpActionType;
import static aprs.actions.optaplanner.actionmodel.OpActionType.DROPOFF;
import static aprs.actions.optaplanner.actionmodel.OpActionType.END;
import static aprs.actions.optaplanner.actionmodel.OpActionType.FAKE_DROPOFF;
import static aprs.actions.optaplanner.actionmodel.OpActionType.FAKE_PICKUP;
import static aprs.actions.optaplanner.actionmodel.OpActionType.PICKUP;
import static aprs.actions.optaplanner.actionmodel.OpActionType.START;
import aprs.actions.optaplanner.actionmodel.OpEndAction;
import aprs.actions.optaplanner.actionmodel.score.EasyOpActionPlanScoreCalculator;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;

/**
 * Class for displaying either a solved or initial OpActionPlan, plotting the 2D
 * route for the robot to pickup and dropoff some set of parts at the given
 * destinations.
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings({"guieffect", "serial"})
public class OpDisplayJPanel extends JPanel {

    /**
     * Show a window showing the
     *
     * @param plan plan to show
     * @param title title of new window
     * @param defaultCloseOperation default close operation for displayed frame
     */
    public static void showPlan(
            OpActionPlan plan,
            String title,
            int defaultCloseOperation) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            JFrame frm = new JFrame();
            frm.setDefaultCloseOperation(defaultCloseOperation);
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
        showPlan(createTestInitPlan(), "testInit", JFrame.EXIT_ON_CLOSE);
    }

    private volatile @Nullable
    Point mouseDownPoint = null;

    private final ConcurrentLinkedDeque<Runnable> actionsModifiedListeners
            = new ConcurrentLinkedDeque<>();

    private void notifyActionsModifiedListeners() {
        for (Runnable r : actionsModifiedListeners) {
            r.run();
        }
    }

    public void addActionsModifiedListener(Runnable r) {
        actionsModifiedListeners.add(r);
    }

    public void removeActionsModifiedListener(Runnable r) {
        actionsModifiedListeners.remove(r);
    }

    private final MouseMotionListener mml = new MouseMotionListener() {
        @Override
        public void mouseDragged(MouseEvent e) {
            final Point mouseDownPointFinal = OpDisplayJPanel.this.mouseDownPoint;
            if (null != mouseDownPointFinal) {
                Dimension dim = getSize();
                int h = dim.height;
                int w = keyVisible ? (dim.width - keyWidth) : dim.width;
                final List<OpAction> closeActionsFinal = OpDisplayJPanel.this.closeActions;
                if (null != closeActionsFinal && !closeActionsFinal.isEmpty()) {
                    for (OpAction action : closeActionsFinal) {
                        action.getLocation().x += (maxX - minX) * ((e.getPoint().x - mouseDownPointFinal.x) / ((double) w));
                        action.getLocation().y += (maxY - minY) * ((e.getPoint().y - mouseDownPointFinal.y) / ((double) h));
                    }
                    notifyActionsModifiedListeners();
                }
                mouseDownPoint = e.getPoint();
            }
            repaint();
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            mouseDownPoint = null;
        }
    };
    private final MouseListener mouseListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (!e.isPopupTrigger() && (popupMenu == null || !popupMenu.isVisible())) {
                setCloseActionsFromMouseEvent(e);
            } else {
                checkPopup(e);
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (!e.isPopupTrigger() && (popupMenu == null || !popupMenu.isVisible())) {
                mouseDownPoint = e.getPoint();
                setCloseActionsFromMouseEvent(e);
            } else {
                checkPopup(e);
                mouseDownPoint = null;
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            checkPopup(e);
        }
    };

    private @MonotonicNonNull
    JPopupMenu popupMenu = null;

    /**
     * Get the value of showSkippedActions
     *
     * @return the value of showSkippedActions
     */
    public boolean isShowSkippedActions() {
        return showSkippedActionsMenuItem.isSelected();
    }

    /**
     * Set the value of showSkippedActions
     *
     * @param showSkippedActions new value of showSkippedActions
     */
    public void setShowSkippedActions(boolean showSkippedActions) {
        this.showSkippedActionsMenuItem.setSelected(showSkippedActions);
    }

    private void replan() {
        if (null != opActionPlan) {
            List<OpAction> origActions = opActionPlan.getActions();
            if (null != origActions) {
                OpActionPlan newOpActionPlan = new OpActionPlanCloner().cloneSolution(opActionPlan);
                List<OpAction> newActions = new ArrayList<>();
                for (OpAction act : origActions) {
                    switch (act.getOpActionType()) {
                        case PICKUP:
                        case DROPOFF:
                        case START:
                            newActions.add(new OpAction(act.getName(), act.getLocation().x, act.getLocation().y, act.getOpActionType(), act.getPartType(), act.isRequired()));
                            break;

                        default:
                            break;
                    }
                }
                Collections.shuffle(newActions);
                newOpActionPlan.setActions(newActions);
                newOpActionPlan.initNextActions();
                if (null == solver) {
                    if (null == solverFactory) {
                        solverFactory = OpActionPlan.createSolverFactory();
                    }
                    solver = solverFactory.buildSolver();
                }
                OpActionPlan newSolution = solver.solve(newOpActionPlan);
                EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
                HardSoftLongScore score = calculator.calculateScore(newSolution);
                showPlan(newSolution, "Replanned:" + score.toShortString(), JFrame.DISPOSE_ON_CLOSE);
            }
        }
    }

    public @Nullable
    SolverFactory<OpActionPlan> getSolverFactory() {
        return solverFactory;
    }

    public void setSolverFactory(SolverFactory<OpActionPlan> solverFactory) {
        this.solverFactory = solverFactory;
    }

    private volatile @MonotonicNonNull
    SolverFactory<OpActionPlan> solverFactory = null;

    private volatile @MonotonicNonNull
    Solver<OpActionPlan> solver = null;

    private JPopupMenu createPopupMenu() {
        JPopupMenu newPopupMenu = new JPopupMenu("Popup or Plan Display " + label);
        JMenuItem newWindowMenuItem = new JMenuItem("New Window");
        newWindowMenuItem.addActionListener((ActionEvent evt) -> {
            if (null != opActionPlan) {
                if (null != label) {
                    showPlan(opActionPlan, label, JFrame.DISPOSE_ON_CLOSE);
                } else {
                    showPlan(opActionPlan, "", JFrame.DISPOSE_ON_CLOSE);
                }
            }
            newPopupMenu.setVisible(false);
        });
        newPopupMenu.add(newWindowMenuItem);
        JMenuItem replanMenuItem = new JMenuItem("Replan");
        replanMenuItem.addActionListener((ActionEvent evt) -> {
            replan();
            newPopupMenu.setVisible(false);
            repaint();
        });
        newPopupMenu.add(replanMenuItem);
        clearActionListeners(showSkippableNextMenuItem);
        showSkippableNextMenuItem.addActionListener((ActionEvent evt) -> {
            newPopupMenu.setVisible(false);
            repaint();
        });
        newPopupMenu.add(showSkippableNextMenuItem);
        clearActionListeners(showPossibleNextMenuItem);
        showPossibleNextMenuItem.addActionListener((ActionEvent evt) -> {
            newPopupMenu.setVisible(false);
            repaint();
        });
        newPopupMenu.add(showPossibleNextMenuItem);

        clearActionListeners(showFakeActionsMenuItem);
        showFakeActionsMenuItem.addActionListener((ActionEvent evt) -> {
            newPopupMenu.setVisible(false);
            if (showFakeActionsMenuItem.isSelected()) {
                setKeyWidth(150);
            } else {
                setKeyWidth(110);
            }
            repaint();
        });
        showSkippedActionsMenuItem.addActionListener((ActionEvent evt) -> {
            newPopupMenu.setVisible(false);
            repaint();
        });
        newPopupMenu.add(showFakeActionsMenuItem);
        newPopupMenu.add(showSkippedActionsMenuItem);
        JMenuItem neverMindMenuItem = new JMenuItem("Never Mind");
        neverMindMenuItem.addActionListener((ActionEvent evt) -> {
            newPopupMenu.setVisible(false);
            repaint();
        });
        newPopupMenu.add(neverMindMenuItem);
        return newPopupMenu;
    }

    private void clearActionListeners(JCheckBoxMenuItem cbmi) {
        for (ActionListener l : cbmi.getActionListeners()) {
            cbmi.removeActionListener(l);
        }
    }

    private void checkPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            if (null == popupMenu) {
                popupMenu = createPopupMenu();
            }
            popupMenu.setLocation(e.getXOnScreen(), e.getYOnScreen());
            popupMenu.setVisible(true);
        }
    }

    /**
     * Create OpDisplayJPanel from an existing plan.
     *
     * @param opActionPlan plan to show
     */
    @SuppressWarnings({"nullness", "initialization"})
    @UIEffect
    private OpDisplayJPanel(OpActionPlan opActionPlan) {
        this.opActionPlan = opActionPlan;
        super.setBackground(Color.white);
        super.addMouseMotionListener(mml);
        super.addMouseListener(mouseListener);
//        ToolTipManager.sharedInstance().registerComponent(this);
    }

    /**
     * Create a OpDisplayJPanel showing a default test plan.
     *
     */
    public OpDisplayJPanel() {
        this(createEmptyInitPlan());
    }

    static private OpActionPlan createTestInitPlan() {
        List<OpAction> initList = Arrays.asList(
                new OpAction("pickup A3", 5.1, 0.2, PICKUP, "A", true),
                new OpAction("dropoff A3", 6.3, 0.4, DROPOFF, "A", true),
                new OpAction("Start", 0, 0, START, "START", true),
                new OpAction("pickup A1", 1.5, 0.6, PICKUP, "A", true),
                new OpAction("dropoff A1", 2.7, 0.8, DROPOFF, "A", true),
                new OpAction("pickup A2", 3.9, 0.1, PICKUP, "A", true),
                new OpAction("dropoff A2", 4.2, 0.3, DROPOFF, "A", true),
                new OpAction("pickup B3", 5.4, 1.5, PICKUP, "B", true),
                new OpAction("dropoff B3", 6.6, 1.7, DROPOFF, "B", true),
                new OpAction("pickup B1", 1.8, 1.9, PICKUP, "B", true),
                new OpAction("dropoff B1", 2.1, 1.2, DROPOFF, "B", true),
                new OpAction("pickup B2", 3.3, 1.4, PICKUP, "B", true),
                new OpAction("dropoff B2", 4.5, 1.6, DROPOFF, "B", true)
        );
        OpActionPlan ap = new OpActionPlan();
        ap.setActions(initList);
        ap.getEndAction().setLocation(new Point2D.Double(7, 0));
        ap.initNextActions();
        return ap;
    }

    static private OpActionPlan createEmptyInitPlan() {
        List<OpAction> initList = Arrays.asList(
                new OpAction("Start", 0, 0, START, "START", true)
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

    private static final Color[] colors = new Color[]{
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

    private @Nullable
    OpActionPlan opActionPlan;

    /**
     * Get the value of opActionPlan
     *
     * @return the value of opActionPlan
     */
    @SafeEffect
    public @Nullable
    OpActionPlan getOpActionPlan() {
        return opActionPlan;
    }

    /**
     * Set the value of opActionPlan
     *
     * @param opActionPlan new value of opActionPlan
     */
    @SafeEffect
    public void setOpActionPlan(@Nullable OpActionPlan opActionPlan) {
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
        String thisLabel = this.label;
        if (null != thisLabel && null != labelPos) {
            Font origFont = g.getFont();
            if (null != labelFont) {
                g.setFont(labelFont);
            }
            g.drawString(thisLabel, labelPos.x, labelPos.y);
            g.setFont(origFont);
        }
        if (null == opActionPlan) {
            return;
        }
        List<OpAction> actionsList = opActionPlan.getActions();
        if (null == actionsList
                || actionsList.isEmpty()) {
            return;
        }
        MutableMap<OpActionType, Integer> typeCountMap
                = Lists.adapt(actionsList)
                        .countBy(OpAction::getOpActionType)
                        .toMapOfItemToCount();

        Dimension dim = this.getSize();
        if (dim.width < 1 || dim.height < 1) {
            System.err.println("bad dim =" + dim.width + " x " + dim.height);
            return;
        }
        OpActionPlan plan = this.opActionPlan;
        if (null != plan) {
            paintOpActionPlan(plan, dim, g2d);
        }

        if (keyVisible) {
            paintKey(g2d, typeCountMap);
        }
    }

    private void paintKey(Graphics2D g2d, MutableMap<OpActionType, Integer> typeCountMap) {
        int keyY = 25;
        g2d.drawLine(10, 10, 30, 10);
        g2d.drawString("(empty)", 40, 10);
        for (Map.Entry<String, Color> entry : partsColorsMap.entrySet()) {
            Color origColor = g2d.getColor();
            Stroke origStroke = g2d.getStroke();
            if (null != carryStroke) {
                g2d.setStroke(carryStroke);
            }
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
                int x = 10;
                paintActionSymbol(g2d, x, keyY, type, false);
                g2d.drawString(type.toString(), 40, keyY);
                g2d.drawString(": " + typeCount, 140, keyY);
                keyY += 25;
            }
            int x = 10;
            paintActionSymbol(g2d, x, keyY, " ", true);
            g2d.drawString("required", 40, keyY);
            if (null != opActionPlan) {
                List<OpAction> acts = opActionPlan.getActions();
                if (null != acts) {
                    g2d.drawString(": " + acts.stream().filter(OpAction::isRequired).count(), 100, keyY);
                }
            }
        }
        if (null != closeActions) {
            for (OpAction action : closeActions) {
                keyY += 25;
                g2d.drawString(action.getName(), 10, keyY);
            }
        }
    }

    private final JCheckBoxMenuItem showPossibleNextMenuItem = new JCheckBoxMenuItem("Show Possible Next(s)", false);
    private final JCheckBoxMenuItem showSkippableNextMenuItem = new JCheckBoxMenuItem("Show Skippable Next(s)", false);
    private final JCheckBoxMenuItem showFakeActionsMenuItem = new JCheckBoxMenuItem("Show Fake Actions(s)", false);
    private final JCheckBoxMenuItem showSkippedActionsMenuItem = new JCheckBoxMenuItem("Show Skipped Actions(s)", true);
    private final ConcurrentHashMap<String, Point2D.Double> fakeLocationsMap = new ConcurrentHashMap<>();
    private final Random locRandom = new Random();

    private Point2D.Double getActionLocation(OpActionInterface act, double minX, double maxX, double minY, double maxY) {
        if (!showFakeActionsMenuItem.isSelected()) {
            return act.getLocation();
        }
        if (act.getOpActionType() != FAKE_DROPOFF && act.getOpActionType() != FAKE_PICKUP) {
            return act.getLocation();
        }
        return fakeLocationsMap.computeIfAbsent(act.getName(), (String key)
                -> {
            return new Point2D.Double(
                    locRandom.nextDouble() * (maxX - minX) + minX,
                    locRandom.nextDouble() * (maxY - minY) + minY);
        });
    }

    private void paintOpActionPlan(OpActionPlan plan, Dimension dim, Graphics2D g2d) {
        OpEndAction endAction = plan.getEndAction();
        List<OpAction> actions = plan.getOrderedList(true);
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
                if (!showFakeActionsMenuItem.isSelected()) {
                    if (action.getOpActionType() == FAKE_DROPOFF || action.getOpActionType() == FAKE_PICKUP) {
                        continue;
                    }
                }
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

            int ly = 0;
            int h = dim.height;
            int w = keyVisible ? (dim.width - keyWidth) : dim.width;
            if (labelPos != null) {
                ly = labelPos.y;
                h = dim.height - ly;
            }

            Set<OpAction> numLabeledItems = new HashSet<>();
            if (null != closeActions) {
                for (OpAction action : closeActions) {
                    if (!showFakeActionsMenuItem.isSelected()) {
                        if (action.getOpActionType() == FAKE_DROPOFF || action.getOpActionType() == FAKE_PICKUP) {
                            continue;
                        }
                    }
                    OpActionType actionType = action.getOpActionType();
                    Point2D.Double location = getActionLocation(action, minX, maxX, minY, maxY);
                    int x = keyWidth + (int) ((0.9 * (location.x - minX) / xdiff) * w + 0.05 * w);
                    int y = ly + (int) ((0.9 * (location.y - minY) / ydiff) * h + 0.05 * h);
                    AffineTransform origTransform = g2d.getTransform();
                    Color origColor = g2d.getColor();
                    g2d.translate(x - 3, y - 12);
                    g2d.setColor(Color.black);
                    g2d.fill(selectedCircleLetterShape);
                    g2d.setTransform(origTransform);
                    g2d.translate(x - 3, y - 12);
                    g2d.setColor(this.getBackground());
                    g2d.translate(x - 7, y - 15);
                    g2d.fill(requiredCircleLetterShape);
                    g2d.setTransform(origTransform);
                    g2d.setColor(origColor);
                }
            }
            OpActionInterface prevAction = null;
            for (OpAction action : actions) {
                if (!showFakeActionsMenuItem.isSelected()) {
                    if (action.getOpActionType() == FAKE_DROPOFF || action.getOpActionType() == FAKE_PICKUP) {
                        prevAction = action;
                        continue;
                    }
                }
                boolean skipped = OpActionPlan.isSkippedAction(action, prevAction);

                if (!showSkippedActionsMenuItem.isSelected()) {
                    if (skipped) {
                        prevAction = action;
                        continue;
                    }
                }
                prevAction = action;
                OpActionType actionType = action.getOpActionType();
                Point2D.Double location = getActionLocation(action, minX, maxX, minY, maxY);
                int x = keyWidth + (int) ((0.9 * (location.x - minX) / xdiff) * w + 0.05 * w);
                int y = ly + (int) ((0.9 * (location.y - minY) / ydiff) * h + 0.05 * h);
                if (showSkippableNextMenuItem.isSelected()) {
                    OpActionInterface nextAction = action.getNext();
                    if (actionType == PICKUP || actionType == DROPOFF || actionType == START || showFakeActionsMenuItem.isSelected()) {
                        if (null != nextAction) {
                            OpActionType nextActionType = nextAction.getOpActionType();
                            if (nextActionType == PICKUP || nextActionType == DROPOFF || nextActionType == END || showFakeActionsMenuItem.isSelected()) {
                                Point2D.Double nextLocation = getActionLocation(nextAction, minX, maxX, minY, maxY);
                                int nx = keyWidth + (int) ((0.9 * (nextLocation.x - minX) / xdiff) * w + 0.05 * w);
                                int ny = ly + (int) ((0.9 * (nextLocation.y - minY) / ydiff) * h + 0.05 * h);
                                double l = Math.hypot(nx - x, ny - y);
                                double dx = 25 * (nx - x) / l;
                                double dy = 25 * (ny - y) / l;
                                int idx = (int) dx;
                                int idy = (int) dy;
                                Color origColor = g2d.getColor();
                                Stroke origStroke = g2d.getStroke();

                                g2d.setColor(Color.darkGray);
                                if (null != nextStroke) {
                                    g2d.setStroke(nextStroke);
                                }
                                g2d.drawLine(nx - idx, ny - idy, x + idx, y + idy);
                                g2d.setColor(origColor);
                                g2d.setStroke(origStroke);
                            }
                        }
                    }
                    if (showPossibleNextMenuItem.isSelected()) {
                        for (OpActionInterface possibleNextAction : action.getPossibleNextActions()) {
                            if (null != possibleNextAction) {
                                OpActionType nextActionType = possibleNextAction.getOpActionType();
                                if (nextActionType == PICKUP || nextActionType == DROPOFF || nextActionType == END || showFakeActionsMenuItem.isSelected()) {
                                    Point2D.Double nextLocation = getActionLocation(possibleNextAction, minX, maxX, minY, maxY);
                                    int nx = keyWidth + (int) ((0.9 * (nextLocation.x - minX) / xdiff) * w + 0.05 * w);
                                    int ny = ly + (int) ((0.9 * (nextLocation.y - minY) / ydiff) * h + 0.05 * h);
                                    double l = Math.hypot(nx - x, ny - y);
                                    double dx = 25 * (nx - x) / l;
                                    double dy = 25 * (ny - y) / l;
                                    int idx = (int) dx;
                                    int idy = (int) dy;
                                    Color origColor = g2d.getColor();
                                    Stroke origStroke = g2d.getStroke();

                                    g2d.setColor(Color.lightGray);
                                    if (null != possibleNextStroke) {
                                        g2d.setStroke(possibleNextStroke);
                                    }
                                    g2d.drawLine(nx - idx, ny - idy, x + idx, y + idy);
                                    g2d.setColor(origColor);
                                    g2d.setStroke(origStroke);
                                }
                            }
                        }
                    }
                }
                paintBackgroundSymbol(g2d, x, y, action);
                paintActionSymbol(g2d, x, y, action.getOpActionType(), action.isRequired());
                if (!numLabeledItems.contains(action)) {
                    List<OpAction> closeItems = findCloseActions(x, y);
                    if (closeItems.size() > 1) {
                        numLabeledItems.add(action);
                        numLabeledItems.addAll(closeItems);
                        g2d.drawString("" + closeItems.size(), x + 15, y + 15);
                    }
                }
            }
            prevAction = null;
            OpActionInterface action = plan.findStartAction();
            int prevx = -1;
            int prevy = -1;
            while (action != null && action != prevAction) {
                if (action.getOpActionType() == OpActionType.FAKE_DROPOFF) {
                    action = action.getNext();
                    continue;
                }
                if (action.getOpActionType() == OpActionType.FAKE_PICKUP) {
                    action = action.getNext();
                    if (null == action) {
                        throw new IllegalStateException("action of type FAKE_PICKUP has null next: prevAction=" + prevAction);
                    }
                    action = action.getNext();
                    continue;
                }

                Point2D.Double location = getActionLocation(action, minX, maxX, minY, maxY);
                int x = keyWidth + (int) ((0.9 * (location.x - minX) / xdiff) * w + 0.05 * w);
                int y = ly + (int) ((0.9 * (location.y - minY) / ydiff) * h + 0.05 * h);
                double l = Math.hypot(x - prevx, y - prevy);
                double dx = 25 * (x - prevx) / l;
                double dy = 25 * (y - prevy) / l;
                int idx = (int) dx;
                int idy = (int) dy;
                if (actionNamesVisible) {
                    g2d.drawString(action.getName(), x + 10, y + 10);
                } else {
                    paintActionSymbol(g2d, x, y, action.getOpActionType(), action.isRequired());
                }
                OpActionInterface nextActon = action.getNext();
                if (null == nextActon) {
                    continue;
                }
                if (nextActon.getOpActionType() == OpActionType.FAKE_DROPOFF) {
                    action = action.getNext();
                    continue;
                }
                if (null != prevAction && prevx > 0 && prevy > 0) {
                    Color origColor = g2d.getColor();
                    Stroke origStroke = g2d.getStroke();
                    if (action.getOpActionType() == DROPOFF) {
                        g2d.setColor(getColor(action.getPartType()));
                        if (null != carryStroke) {
                            g2d.setStroke(carryStroke);
                        }
                    } else {
                        g2d.setColor(Color.black);
                        if (null != emptyStroke) {
                            g2d.setStroke(emptyStroke);
                        }
                    }

                    g2d.drawLine(x - idx, y - idy, prevx + idx, prevy + idy);

                    AffineTransform origTransform = g2d.getTransform();

                    g2d.translate(x - idx, y - idy);
                    g2d.rotate(Math.atan2(prevx - x, y - prevy));

                    g2d.fill(arrowHead);
                    g2d.setTransform(origTransform);
                    g2d.setColor(origColor);
                    g2d.setStroke(origStroke);
                }
                prevAction = action;
                prevx = x;
                prevy = y;
                action = action.effectiveNext(true);
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

    private int keyWidth = 150;

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
    private void setKeyWidth(int keyWidth) {
        this.keyWidth = keyWidth;
    }

    private void paintBackgroundSymbol(Graphics2D g2d, int x, int y, OpAction action) {
        if (null == action.getPartType() || action.getPartType().length() < 1) {
            return;
        }

        switch (action.getOpActionType()) {
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
        g2d.setColor(Color.white);
        g2d.fill(centerLetterShape);
        g2d.setColor(origColor);
        g2d.setTransform(origTransform);
    }

    private void paintActionSymbol(Graphics2D g2d, int x, int y, OpActionType type, boolean required) {
        String typeLetterString = type.name().substring(0, 1);
        paintActionSymbol(g2d, x, y, typeLetterString, required);
    }

    private void paintActionSymbol(Graphics2D g2d, int x, int y, String typeLetterString, boolean required) {
        if (typeLetterString.equals("F")
                && !showFakeActionsMenuItem.isSelected()) {
            return;
        }
        AffineTransform origTransform = g2d.getTransform();
        g2d.translate(x - 7, y - 15);
        g2d.draw(circleLetterShape);
        if (required) {
            g2d.draw(requiredCircleLetterShape);
        }
        g2d.setTransform(origTransform);
        g2d.drawString(typeLetterString, x, y);
    }

    private final Shape arrowHead = new Polygon(new int[]{-10, 0, 10}, new int[]{0, 10, 0}, 3);

    private final Arc2D.Double centerLetterShape
            = new Arc2D.Double(4, 4, 12, 12, 0, 360, Arc2D.OPEN);
    private final Arc2D.Double circleLetterShape
            = new Arc2D.Double(0, 0, 20, 20, 0, 360, Arc2D.OPEN);
    private final Arc2D.Double requiredCircleLetterShape
            = new Arc2D.Double(-2, -2, 24, 24, 0, 360, Arc2D.OPEN);

    private final Arc2D.Double selectedCircleLetterShape
            = new Arc2D.Double(-8, -8, 30, 30, 0, 360, Arc2D.OPEN);

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

    private Stroke carryStroke = new BasicStroke(4f);

    private final Stroke emptyStroke = new BasicStroke(3f);

    private final Stroke nextStroke = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{0.5f, 0.5f}, 0f);

    private final Stroke possibleNextStroke = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{0.5f, 0.5f}, 0f);

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

    private @Nullable
    String label;

    /**
     * Get the value of label
     *
     * @return the value of label
     */
    @SafeEffect
    public @Nullable
    String getLabel() {
        return label;
    }

    /**
     * Set the value of label
     *
     * @param label new value of label
     */
    @SafeEffect
    public void setLabel(String label) {
        this.label = label;
        this.repaint();
    }

    private @MonotonicNonNull
    Point labelPos;

    /**
     * Get the value of labelPos
     *
     * @return the value of labelPos
     */
    public @Nullable
    Point getLabelPos() {
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

    private @Nullable
    Font labelFont;

    /**
     * Get the value of labelFont
     *
     * @return the value of labelFont
     */
    public @Nullable
    Font getLabelFont() {
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
        List<OpAction> actonsList = opActionPlan.getActions();
        if (null == actonsList) {
            return Collections.emptyList();
        }
        List<OpAction> newCloseActionsList = new ArrayList<>();
        for (OpAction actionToCheck : actonsList) {
            if (!showFakeActionsMenuItem.isSelected()) {
                if (actionToCheck.getOpActionType() == FAKE_DROPOFF || actionToCheck.getOpActionType() == FAKE_PICKUP) {
                    continue;
                }
            }
            Point2D.Double location = getActionLocation(actionToCheck, minX, maxX, minY, maxY);
            int actionX = keyWidth + (int) ((0.9 * (location.x - minX) / xdiff) * w + 0.05 * w);
            int actionY = ly + (int) ((0.9 * (location.y - minY) / ydiff) * h + 0.05 * h);
            if (Math.abs(actionX - x) < 30 && Math.abs(actionY - y) < 30) {
                newCloseActionsList.add(actionToCheck);
            }
        }
        return newCloseActionsList;
    }

//    private void mouseDragged(MouseEvent e) {
//        setCloseActionsToolTip(e);
//    }
//
    private @Nullable
    List<OpAction> closeActions = null;

    public List<OpAction> getCloseActions() {
        if (null == closeActions) {
            return Collections.emptyList();
        }
        return closeActions;
    }

    public void setCloseActions(List<OpAction> closeActions) {
        this.closeActions = closeActions;
        System.out.println("closeActions = " + closeActions);
        this.repaint();
    }

    public void setCloseActionsFromMouseEvent(MouseEvent e) {
        closeActions
                = findCloseActions(e.getX(), e.getY());
        System.out.println("closeActions = " + closeActions);
        repaint();
    }

}
