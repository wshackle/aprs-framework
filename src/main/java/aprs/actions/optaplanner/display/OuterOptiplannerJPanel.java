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
import aprs.actions.optaplanner.actionmodel.OpEndAction;
import aprs.misc.Utils;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
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
                if(selectedIndex < 0 || selectedIndex > jTable1.getRowCount()) {
                    opDisplayJPanel1.setCloseActions(Collections.emptyList());
                    return;
                }
//                int viewIndex = jTable1.convertRowIndexToView(selectedIndex);
                int modelIndex = jTable1.convertRowIndexToModel(selectedIndex);
                if (modelIndex >= 0 && modelIndex < actions.size()) {
                    OpAction selectedAction = actions.get(modelIndex);
                    opDisplayJPanel1.setCloseActions(Collections.singletonList(selectedAction));
                }
//                DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
////                if (firstIndex >= 0 && firstIndex < actions.size()) {
////                    OpAction selectedAction = actions.get(firstIndex);
////                    opDisplayJPanel1.setCloseActions(Collections.singletonList(selectedAction));
////                }
//                Object firstIndexData = model.getValueAt(firstIndex, 0);
//                Object viewIndexData = model.getValueAt(viewIndex, 0);
//                Object modelIndexData = model.getValueAt(modelIndex, 0);
//                if (modelIndexData instanceof Integer) {
//                    int listIndex = (int) modelIndexData;
//                    if (listIndex >= 0 && listIndex < actions.size()) {
//                        OpAction selectedAction = actions.get(listIndex);
//                        opDisplayJPanel1.setCloseActions(Collections.singletonList(selectedAction));
//                    }
//                }
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    @UIEffect
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        opDisplayJPanel1 = new aprs.actions.optaplanner.display.OpDisplayJPanel();

        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jTable1.setAutoCreateRowSorter(true);
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Index", "Id", "Name", "Type", "PartType", "X", "Y", "Cost", "Required", "Skipped", "NextId", "ExecType", "Args", "PossibleNexts"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
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
            .addGap(0, 287, Short.MAX_VALUE)
        );
        opDisplayJPanel1Layout.setVerticalGroup(
            opDisplayJPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jSplitPane1.setBottomComponent(opDisplayJPanel1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 287, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 251, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    @UIEffect
    private void opDisplayJPanel1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_opDisplayJPanel1MouseClicked
        opDisplayJPanel1.setCloseActionsFromMouseEvent(evt);
        List<OpAction> closeActions = opDisplayJPanel1.getCloseActions();
        if (null != closeActions && !closeActions.isEmpty()) {
            final OpActionPlan opActionPlan = opDisplayJPanel1.getOpActionPlan();
            if(null == opActionPlan) {
                return;
            }
            List<OpAction> actions = opActionPlan.getOrderedList(true);
            if(null == actions) {
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
    @Nullable
    @SafeEffect
    public OpActionPlan getOpActionPlan() {
        return opDisplayJPanel1.getOpActionPlan();
    }

    /**
     * Set the value of opActionPlan
     *
     * @param opActionPlan new value of opActionPlan
     */
    public void setOpActionPlan(@Nullable OpActionPlan opActionPlan) {
        opDisplayJPanel1.setOpActionPlan(opActionPlan);
        if(null == opActionPlan) {
            return;
        }
        internalShowOpActionPlan(opActionPlan);
    }

    private void internalShowOpActionPlan(OpActionPlan opActionPlan) {
        Utils.runOnDispatchThread(() -> internalShowOpActionPlanOnDisplay(opActionPlan));
    }

    @UIEffect
    private void internalShowOpActionPlanOnDisplay(OpActionPlan opActionPlan) {
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
            model.addRow(new Object[]{i, action.getId(), action.getName(), action.getOpActionType(), action.getPartType(), action.getLocation().x, action.getLocation().y, action.cost(opActionPlan), action.isRequired(), skipped, (null != next) ? next.getId() : -1, action.getExecutorActionType(),Arrays.toString(action.getExecutorArgs()),possibleNextIds});
            prevAction = action;
        }
        OpEndAction endAction = opActionPlan.getEndAction();
        model.addRow(new Object[]{actions.size(), endAction.getId(), endAction.getName(), endAction.getOpActionType(), null, endAction.getLocation().x, endAction.getLocation().y, 0, endAction.isRequired(), false, -1, Collections.emptyList()});

        Utils.autoResizeTableColWidths(jTable1);
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
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTable jTable1;
    private aprs.actions.optaplanner.display.OpDisplayJPanel opDisplayJPanel1;
    // End of variables declaration//GEN-END:variables
}
