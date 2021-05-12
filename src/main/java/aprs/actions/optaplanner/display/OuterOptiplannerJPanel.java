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
import aprs.actions.optaplanner.actionmodel.OpEndAction;
import aprs.actions.optaplanner.actionmodel.score.EasyOpActionPlanScoreCalculator;
import aprs.misc.Utils;
import crcl.utils.XFutureVoid;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("serial")
public class OuterOptiplannerJPanel extends javax.swing.JPanel {

    /**
     * Creates new form OuterOptiplannerJPanel
     */
    @SuppressWarnings({"initialization"})
    @UIEffect
    public OuterOptiplannerJPanel() {
        initComponents();
        jTable1.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {

                OpActionPlan opActionPlan = opDisplayJPanel1.getOpActionPlan();
                List<OpAction> actions = opActionPlan.getOrderedList(true);
//                int firstIndex = e.getFirstIndex();
//                int lastIndex = e.getLastIndex();
                int selectedIndex = jTable1.getSelectedRow();
                if (selectedIndex < 0 || selectedIndex > jTable1.getRowCount()) {
                    opDisplayJPanel1.setCloseActions(Collections.emptyList());
                    jButtonDown.setEnabled(false);
                    jButtonUp.setEnabled(false);
                    return;
                }
//                int viewIndex = jTable1.convertRowIndexToView(selectedIndex);
                int modelIndex = jTable1.convertRowIndexToModel(selectedIndex);
                if (modelIndex >= 0 && modelIndex < actions.size()) {
                    OpAction selectedAction = actions.get(modelIndex);
                    jButtonDown.setEnabled(true);
                    jButtonUp.setEnabled(true);
                    opDisplayJPanel1.setCloseActions(Collections.singletonList(selectedAction));
                } else {
                    jButtonDown.setEnabled(false);
                    jButtonUp.setEnabled(false);
                }
            }
        });
        jLabel1.setText(label + ": " + valueString);
        opDisplayJPanel1.addActionsModifiedListener(() -> setOpActionPlan(opDisplayJPanel1.getOpActionPlan()));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @UIEffect
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        opDisplayJPanel1 = new aprs.actions.optaplanner.display.OpDisplayJPanel();
        jLabel1 = new javax.swing.JLabel();
        jButtonDown = new javax.swing.JButton();
        jButtonUp = new javax.swing.JButton();

        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        jSplitPane1.setDividerLocation(50);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jTable1.setAutoCreateRowSorter(true);
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Index", "Id", "Name", "Type", "PartType", "X", "Y", "Cost", "Required", "Skipped", "NextId", "ExecType", "Args", "PossibleNexts"
            }
        ) {
            final Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            final boolean[] canEdit = new boolean [] {
                false, false, true, true, true, true, true, true, false, false, false, true, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTable1);

        jSplitPane1.setLeftComponent(jScrollPane1);

        opDisplayJPanel1.setPreferredSize(new java.awt.Dimension(413, 200));
        opDisplayJPanel1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                opDisplayJPanel1MouseClicked(evt);
            }
        });

        javax.swing.GroupLayout opDisplayJPanel1Layout = new javax.swing.GroupLayout(opDisplayJPanel1);
        opDisplayJPanel1.setLayout(opDisplayJPanel1Layout);
        opDisplayJPanel1Layout.setHorizontalGroup(
            opDisplayJPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 366, Short.MAX_VALUE)
        );
        opDisplayJPanel1Layout.setVerticalGroup(
            opDisplayJPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jSplitPane1.setBottomComponent(opDisplayJPanel1);

        jLabel1.setText("label:  value  ");

        jButtonDown.setText("Down");
        jButtonDown.setEnabled(false);
        jButtonDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDownActionPerformed(evt);
            }
        });

        jButtonUp.setText("Up");
        jButtonUp.setEnabled(false);
        jButtonUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonUpActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 366, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonUp)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonDown)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jButtonDown)
                    .addComponent(jButtonUp))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 331, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    @UIEffect
    private void opDisplayJPanel1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_opDisplayJPanel1MouseClicked
        opDisplayJPanel1.setCloseActionsFromMouseEvent(evt);
        List<OpAction> closeActions = opDisplayJPanel1.getCloseActions();
        if (null != closeActions && !closeActions.isEmpty()) {
            final OpActionPlan opActionPlan = opDisplayJPanel1.getOpActionPlan();
            if (null == opActionPlan) {
                return;
            }
            List<OpAction> actions = opActionPlan.getOrderedList(true);
            if (null == actions) {
                return;
            }
            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
            OpAction closeAction0 = closeActions.get(0);
            for (int i = 0; i < model.getRowCount(); i++) {
                final Object valueAtI1 = model.getValueAt(i, 1);
                if (valueAtI1 != null
                        && closeAction0.getId() == Integer.parseInt(valueAtI1.toString())
                        && closeAction0.getName().equals(model.getValueAt(i, 2))
                        && closeAction0.getOpActionType().equals(model.getValueAt(i, 3))
                        && closeAction0.getPartType().equals(model.getValueAt(i, 4))) {
                    jTable1.getSelectionModel().setSelectionInterval(i, i);
                    scrollToVisible(jTable1, i, 0);
                    return;
                }
            }
        }
    }//GEN-LAST:event_opDisplayJPanel1MouseClicked

    @UIEffect
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        jSplitPane1.setDividerLocation(0.3);
    }//GEN-LAST:event_formComponentResized

    @UIEffect
    private void jButtonUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonUpActionPerformed
        int selectedIndex = jTable1.getSelectedRow();
        OpActionPlan opActionPlan = Objects.requireNonNull(opDisplayJPanel1.getOpActionPlan(), "opDisplayJPanel1.getOpActionPlan()");
        List<OpAction> actions = opActionPlan.getOrderedList(true);
        int modelIndex = jTable1.convertRowIndexToModel(selectedIndex);
        System.out.println("modelIndex = " + modelIndex);
        if (modelIndex > 0 && modelIndex < actions.size()) {
            OpAction selectedAction = actions.get(modelIndex);
            System.out.println("selectedAction = " + selectedAction);
            OpAction selectedActionM1 = actions.get(modelIndex - 1);
            for (int i = modelIndex - 1; i > 0; i--) {
                OpAction actionI = actions.get(i);
                if (selectedAction.getOpActionType() == actionI.getOpActionType() && selectedAction.getPartType().equals(actionI.getPartType())) {
                    System.out.println("actionI = " + actionI);
                    System.out.println("i = " + i);
                    OpAction actionIM1 = actions.get(i - 1);
                    selectedActionM1.setNext(actionI);
                    OpActionInterface selectedActionNext = selectedAction.getNext();
                    OpActionInterface actionINext = actionI.getNext();
                    actionIM1.setNext(selectedAction);
                    selectedAction.setNext(actionINext);
                    actionI.setNext(selectedActionNext);
                    List<OpAction> newOrderedActionsList = opActionPlan.getOrderedList(true);
                    System.out.println("newOrderedActionsList.get(i) = " + newOrderedActionsList.get(i));
                    System.out.println("newOrderedActionsList.get(modelIndex) = " + newOrderedActionsList.get(modelIndex));
                    OpActionPlan newOpActionPlan = new OpActionPlanCloner().cloneSolution(opActionPlan);
                    setOpActionPlan(newOpActionPlan);
                    return;
                }
            }
        }
    }//GEN-LAST:event_jButtonUpActionPerformed

    @UIEffect
    private void jButtonDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDownActionPerformed
        int selectedIndex = jTable1.getSelectedRow();
        OpActionPlan opActionPlan = opDisplayJPanel1.getOpActionPlan();
        if (null != opActionPlan) {
            List<OpAction> actions = opActionPlan.getOrderedList(true);
            int modelIndex = jTable1.convertRowIndexToModel(selectedIndex);
            System.out.println("modelIndex = " + modelIndex);
            if (modelIndex > 0 && modelIndex < actions.size()) {
                OpAction selectedAction = actions.get(modelIndex);
                System.out.println("selectedAction = " + selectedAction);
                OpAction selectedActionM1 = actions.get(modelIndex - 1);
                for (int i = modelIndex + 1; i < actions.size(); i++) {
                    OpAction actionI = actions.get(i);
                    if (selectedAction.getOpActionType() == actionI.getOpActionType() && selectedAction.getPartType().equals(actionI.getPartType())) {
                        System.out.println("actionI = " + actionI);
                        System.out.println("i = " + i);
                        OpAction actionIM1 = actions.get(i - 1);
                        selectedActionM1.setNext(actionI);
                        OpActionInterface selectedActionNext = selectedAction.getNext();
                        OpActionInterface actionINext = actionI.getNext();
                        actionIM1.setNext(selectedAction);
                        selectedAction.setNext(actionINext);
                        actionI.setNext(selectedActionNext);
                        List<OpAction> newOrderedActionsList = opActionPlan.getOrderedList(true);
                        System.out.println("newOrderedActionsList.get(i) = " + newOrderedActionsList.get(i));
                        System.out.println("newOrderedActionsList.get(modelIndex) = " + newOrderedActionsList.get(modelIndex));
                        OpActionPlan newOpActionPlan = new OpActionPlanCloner().cloneSolution(opActionPlan);
                        setOpActionPlan(newOpActionPlan);
                        return;
                    }
                }
            }
        }
    }//GEN-LAST:event_jButtonDownActionPerformed

    @UIEffect
    private static void scrollToVisible(JTable table, int rowIndex, int vColIndex) {
        Container container = table.getParent();
        if (container instanceof JViewport) {
            JViewport viewport = (JViewport) container;
            Rectangle rect = table.getCellRect(rowIndex, vColIndex, true);
            Point pt = viewport.getViewPosition();
            rect.setLocation(rect.x - pt.x, rect.y - pt.y);
            viewport.scrollRectToVisible(rect);
        } else {
            throw new IllegalStateException("Tables parent " + container + " needs to be a JViewPort");
        }
    }

    /**
     * Get the value of opActionPlan
     *
     * @return the value of opActionPlan
     */
    @SafeEffect
    public @Nullable
    OpActionPlan getOpActionPlan() {
        return opDisplayJPanel1.getOpActionPlan();
    }

    public void addActionsModifiedListener(Runnable r) {
        opDisplayJPanel1.addActionsModifiedListener(r);
    }

    public void removeActionsModifiedListener(Runnable r) {
        opDisplayJPanel1.removeActionsModifiedListener(r);
    }

    /**
     * Set the value of opActionPlan
     *
     * @param opActionPlan new value of opActionPlan
     * @return future for determining when action is complete
     */
    public XFutureVoid setOpActionPlan(@Nullable OpActionPlan opActionPlan) {
        opDisplayJPanel1.setOpActionPlan(opActionPlan);
        if (null == opActionPlan) {
            return XFutureVoid.completedFutureWithName("null == opActionPlan");
        }
        return internalShowOpActionPlan(opActionPlan);
    }

    private XFutureVoid internalShowOpActionPlan(OpActionPlan opActionPlan) {
        return Utils.runOnDispatchThread(() -> internalShowOpActionPlanOnDisplay(opActionPlan));
    }

    private String label = "label";

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
        jLabel1.setText(label + ": " + valueString);
    }

    private String valueString = "value";

    /**
     * Get the value of valueString
     *
     * @return the value of valueString
     */
    public String getValueString() {
        return valueString;
    }

    /**
     * Set the value of valueString
     *
     * @param valueString new value of valueString
     */
    public void setValueString(String valueString) {
        this.valueString = valueString;
        jLabel1.setText(label + ": " + valueString);
    }

    @UIEffect
    private void internalShowOpActionPlanOnDisplay(OpActionPlan opActionPlan) {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            throw new RuntimeException("thread=" + Thread.currentThread());
        }
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        List<OpAction> actions = opActionPlan.getOrderedList(true);
        OpActionInterface prevAction = null;
        for (int i = 0; i < actions.size(); i++) {
            OpAction action = actions.get(i);
            List<OpActionInterface> possibleNexts
                    = action.getPossibleNextActions();
            List<Integer> possibleNextIds = new ArrayList<>();
            for (int j = 0; j < possibleNexts.size(); j++) {
                OpActionInterface possibleNext
                        = possibleNexts.get(j);
                possibleNextIds.add(possibleNext.getId());
                Collections.sort(possibleNextIds);
            }
            final OpActionInterface next = action.getNext();
            boolean skipped = OpActionPlan.isSkippedAction(action, prevAction);
            addNullableRow(model, new Object[]{i, action.getId(), action.getName(), action.getOpActionType(), action.getPartType(), action.getLocation().x, action.getLocation().y, action.cost(opActionPlan), action.isRequired(), skipped, (null != next) ? next.getId() : -1, action.getExecutorActionType(), Arrays.toString(action.getExecutorArgs()), possibleNextIds});
            prevAction = action;
        }
        OpEndAction endAction = opActionPlan.getEndAction();
        addNullableRow(model, new Object[]{actions.size(), endAction.getId(), endAction.getName(), endAction.getOpActionType(), null, endAction.getLocation().x, endAction.getLocation().y, 0, endAction.isRequired(), false, -1, Collections.emptyList()});

        Utils.autoResizeTableColWidths(jTable1);
        EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
        HardSoftLongScore score = calculator.calculateScore(opActionPlan);
        opActionPlan.setScore(score);
        setValueString(score.toShortString());
    }

    @SuppressWarnings("nullness")
    private void addNullableRow(DefaultTableModel model, final @Nullable Object[] rowData) {
        model.addRow(rowData);
    }

    /**
     * Get the value of showSkippedActions
     *
     * @return the value of showSkippedActions
     */
    public boolean isShowSkippedActions() {
        return opDisplayJPanel1.isShowSkippedActions();
    }

    /**
     * Set the value of showSkippedActions
     *
     * @param showSkippedActions new value of showSkippedActions
     */
    public void setShowSkippedActions(boolean showSkippedActions) {
        this.opDisplayJPanel1.setShowSkippedActions(showSkippedActions);
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonDown;
    private javax.swing.JButton jButtonUp;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTable jTable1;
    private aprs.actions.optaplanner.display.OpDisplayJPanel opDisplayJPanel1;
    // End of variables declaration//GEN-END:variables
}
