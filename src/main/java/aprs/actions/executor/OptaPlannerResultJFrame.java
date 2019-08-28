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
package aprs.actions.executor;

import aprs.actions.optaplanner.actionmodel.OpActionPlan;
import aprs.actions.optaplanner.display.OptiplannerDisplayJFrame;
import aprs.misc.Utils;
import aprs.simview.Object2DOuterDialogPanel;
import java.awt.Desktop;
import java.awt.HeadlessException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import static javax.swing.JOptionPane.QUESTION_MESSAGE;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.collections.impl.block.factory.Comparators;

/**
 *
 *@author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class OptaPlannerResultJFrame extends javax.swing.JFrame {

    /**
     * Creates new form OptaPlannerResultJFrame
     */
    public OptaPlannerResultJFrame() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jButtonItemsImage = new javax.swing.JButton();
        jButtonShowPlans = new javax.swing.JButton();
        jButtonItemsSimView = new javax.swing.JButton();
        jButtonInputActions = new javax.swing.JButton();
        jButtonOutputActions = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenuFile = new javax.swing.JMenu();
        jMenuItemFileOpen = new javax.swing.JMenuItem();
        jMenuItemLast = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        jButtonItemsImage.setText("Items Image");
        jButtonItemsImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonItemsImageActionPerformed(evt);
            }
        });

        jButtonShowPlans.setText("Show Plans");
        jButtonShowPlans.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonShowPlansActionPerformed(evt);
            }
        });

        jButtonItemsSimView.setText("Items SimView");
        jButtonItemsSimView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonItemsSimViewActionPerformed(evt);
            }
        });

        jButtonInputActions.setText("Input Actions");
        jButtonInputActions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonInputActionsActionPerformed(evt);
            }
        });

        jButtonOutputActions.setText("Output Actions");
        jButtonOutputActions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOutputActionsActionPerformed(evt);
            }
        });

        jMenuFile.setText("File");

        jMenuItemFileOpen.setText("Open ...");
        jMenuItemFileOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemFileOpenActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemFileOpen);

        jMenuItemLast.setText("Last");
        jMenuItemLast.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemLastActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemLast);

        jMenuBar1.add(jMenuFile);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(13, 13, 13)
                .addComponent(jScrollPane1)
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButtonItemsImage)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonItemsSimView)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonShowPlans)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButtonInputActions)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonOutputActions)
                .addContainerGap(103, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 238, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jButtonItemsImage)
                        .addComponent(jButtonItemsSimView)
                        .addComponent(jButtonInputActions)
                        .addComponent(jButtonOutputActions))
                    .addComponent(jButtonShowPlans))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private final FileFilter optoCsvFileFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return !pathname.isDirectory() && pathname.getName().contains("Opta") && pathname.getName().endsWith(".csv");
        }
    };

    private void jMenuItemLastActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemLastActionPerformed
        showLastRunResults();
    }//GEN-LAST:event_jMenuItemLastActionPerformed

    public void showLastRunResults() throws HeadlessException {
        try {
            File lastDir = lastAprsLogDir();
            File optaResultsFiles[] = lastDir.listFiles(optoCsvFileFilter);
            File selectedCsvFile = (File) JOptionPane.showInputDialog(this,
                    "OptaResultsFile",
                    "Select OptaPlanner Results Log",
                    QUESTION_MESSAGE,
                    null,
                    optaResultsFiles,
                    optaResultsFiles[0]);
            loadResultsCsvFile(selectedCsvFile);
        } catch (IOException ex) {
            Logger.getLogger(OptaPlannerResultJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void loadResultsCsvFile(File selectedCsvFile) throws IOException {
        System.out.println("selectedCsvFile = " + selectedCsvFile);
        Utils.readCsvFileToTable(true, jTable1, selectedCsvFile);
        setTitle(selectedCsvFile.getCanonicalPath());
    }

    private void jMenuItemFileOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemFileOpenActionPerformed
        try {
            File lastDir = lastAprsLogDir();
            JFileChooser chooser = new JFileChooser(lastDir);
            chooser.addChoosableFileFilter(CSV_SWING_FILENAME_FILTER);
            chooser.setFileFilter(CSV_SWING_FILENAME_FILTER);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                loadResultsCsvFile(chooser.getSelectedFile());
            }
        } catch (IOException ex) {
            Logger.getLogger(OptaPlannerResultJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemFileOpenActionPerformed

    private volatile @Nullable
    OptiplannerDisplayJFrame lastDisplayFrame = null;
    private void jButtonShowPlansActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonShowPlansActionPerformed
        try {
            int selectedRow = jTable1.getSelectedRow();
            if (selectedRow < 0 || selectedRow > jTable1.getRowCount()) {
                return;
            }
            int inputPlanIndex = findColIndex("inputPlan");
            int outputPlanIndex = findColIndex("outputPlan");
            if (inputPlanIndex < 0 || inputPlanIndex > jTable1.getColumnCount()) {
                return;
            }
            if (outputPlanIndex < 0 || outputPlanIndex > jTable1.getColumnCount()) {
                return;
            }
            final File inputPlanFile = new File(jTable1.getValueAt(selectedRow, inputPlanIndex).toString());
            OpActionPlan inputPlan = OpActionPlan.loadActionList(inputPlanFile);

            OptiplannerDisplayJFrame displayJFrame = new OptiplannerDisplayJFrame();
            displayJFrame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            displayJFrame.setInputOpActionPlan(inputPlan);
            final File outputPlanFile = new File(jTable1.getValueAt(selectedRow, outputPlanIndex).toString());
            OpActionPlan outputPlan = OpActionPlan.loadActionList(outputPlanFile);
            displayJFrame.setOutputOpActionPlan(outputPlan);
            displayJFrame.setVisible(true);
            if (lastDisplayFrame != null) {
                lastDisplayFrame.setVisible(false);
                lastDisplayFrame.dispose();
            }
            lastDisplayFrame = displayJFrame;
        } catch (IOException ex) {
            Logger.getLogger(OptaPlannerResultJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonShowPlansActionPerformed

    private int findColIndex(String colNameToFind) {
        for (int i = 0; i < jTable1.getColumnCount(); i++) {
            String colName = jTable1.getColumnName(i);
            if (colName.trim().equals(colNameToFind)) {
                return i;
            }
        }
        return -1;
    }
    private void jButtonItemsImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonItemsImageActionPerformed
        try {
            int selectedRow = jTable1.getSelectedRow();
            if (selectedRow < 0 || selectedRow >= jTable1.getRowCount()) {
                return;
            }
            int itemImageIndex = findColIndex("itemsImage");
            if (itemImageIndex < 0 || itemImageIndex >= jTable1.getColumnCount()) {
                return;
            }
            File itemImageFile = new File(jTable1.getValueAt(selectedRow, itemImageIndex).toString());
            Desktop desktop = java.awt.Desktop.getDesktop();
            desktop.open(itemImageFile);
        } catch (IOException ex) {
            Logger.getLogger(OptaPlannerResultJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonItemsImageActionPerformed

    private void jButtonItemsSimViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonItemsSimViewActionPerformed
        try {
            int selectedRow = jTable1.getSelectedRow();
            if (selectedRow < 0 || selectedRow >= jTable1.getRowCount()) {
                return;
            }
            int itemCsvIndex = findColIndex("itemsCsv");
            if (itemCsvIndex < 0 || itemCsvIndex >= jTable1.getColumnCount()) {
                return;
            }
            File object2DPropertiesFile = null;
            File object2DLogLinesFile = null;
            int object2DPropertiesIndex = findColIndex("object2DProperties");
            if (object2DPropertiesIndex < 0 || object2DPropertiesIndex >= jTable1.getColumnCount()) {
                Object o = jTable1.getValueAt(selectedRow, object2DPropertiesIndex);
                if (o != null) {
                    String oString = o.toString();
                    if (oString.length() > 0 && !oString.equals("null")) {
                        object2DPropertiesFile = new File(oString);
                    }
                }
            }

            int object2DLogLinesIndex = findColIndex("object2DLogLines");
            if (object2DLogLinesIndex < 0 || object2DLogLinesIndex >= jTable1.getColumnCount()) {
                Object o = jTable1.getValueAt(selectedRow, object2DLogLinesIndex);
                if (o != null) {
                    String oString = o.toString();
                    if (oString.length() > 0 && !oString.equals("null")) {
                        object2DLogLinesFile = new File(oString);
                    }
                }
            }
            final String csvFileName = jTable1.getValueAt(selectedRow, itemCsvIndex).toString();
            File itemCsvFile = new File(csvFileName);
            Object2DOuterDialogPanel.showObject2DDialog(this, csvFileName, true, object2DPropertiesFile, itemCsvFile, object2DLogLinesFile);
        } catch (Exception ex) {
            Logger.getLogger(OptaPlannerResultJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonItemsSimViewActionPerformed

    private void jButtonInputActionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonInputActionsActionPerformed
        try {
            int selectedRow = jTable1.getSelectedRow();
            if (selectedRow < 0 || selectedRow >= jTable1.getRowCount()) {
                return;
            }
            int actionsInIndex = findColIndex("actionsIn");
            if (actionsInIndex < 0 || actionsInIndex >= jTable1.getColumnCount()) {
                return;
            }
            final String actionsInFilename = jTable1.getValueAt(selectedRow, actionsInIndex).toString();
            File actionsInFile = new File(actionsInFilename);
            Desktop desktop = Desktop.getDesktop();
            desktop.open(actionsInFile);
        } catch (Exception ex) {
            Logger.getLogger(OptaPlannerResultJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonInputActionsActionPerformed

    private void jButtonOutputActionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOutputActionsActionPerformed
        try {
            int selectedRow = jTable1.getSelectedRow();
            if (selectedRow < 0 || selectedRow >= jTable1.getRowCount()) {
                return;
            }
            int actionsOutIndex = findColIndex("actionsOut");
            if (actionsOutIndex < 0 || actionsOutIndex >= jTable1.getColumnCount()) {
                actionsOutIndex = findColIndex("actionOut");
            }
            if (actionsOutIndex < 0 || actionsOutIndex >= jTable1.getColumnCount()) {
                return;
            }
            final String actionsOutFilename = jTable1.getValueAt(selectedRow, actionsOutIndex).toString();
            File actionsOutFile = new File(actionsOutFilename);
            Desktop desktop = Desktop.getDesktop();
            desktop.open(actionsOutFile);
        } catch (Exception ex) {
            Logger.getLogger(OptaPlannerResultJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonOutputActionsActionPerformed
    private static final FileNameExtensionFilter CSV_SWING_FILENAME_FILTER = new FileNameExtensionFilter("CSV", ".csv");

    private File lastAprsLogDir() throws IOException {
        File dir = Utils.getlogFileDir();
        File parent = dir.getParentFile();
        System.out.println("parent = " + parent);
        File dirs[] = parent.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() && pathname.getName().startsWith("aprs_");
            }
        });
        Arrays.sort(dirs, Comparators.byLongFunction(File::lastModified));
        int lastDirIndex = dirs.length - 1;
        File lastDir = dirs[lastDirIndex];
        File csvFiles[] = lastDir.listFiles(optoCsvFileFilter);
        while (csvFiles.length < 1 && lastDirIndex > 0) {
            lastDirIndex--;
            if (lastDir.listFiles().length < 1 && lastDir.canWrite() && lastDir.getParentFile().canWrite()) {
                lastDir.delete();
            }
            lastDir = dirs[lastDirIndex];
            csvFiles = lastDir.listFiles(optoCsvFileFilter);
        }
        return lastDir;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(OptaPlannerResultJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(OptaPlannerResultJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(OptaPlannerResultJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(OptaPlannerResultJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new OptaPlannerResultJFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonInputActions;
    private javax.swing.JButton jButtonItemsImage;
    private javax.swing.JButton jButtonItemsSimView;
    private javax.swing.JButton jButtonOutputActions;
    private javax.swing.JButton jButtonShowPlans;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenu jMenuFile;
    private javax.swing.JMenuItem jMenuItemFileOpen;
    private javax.swing.JMenuItem jMenuItemLast;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
