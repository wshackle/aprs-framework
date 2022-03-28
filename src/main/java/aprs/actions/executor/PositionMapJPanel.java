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

import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import aprs.cachedcomponents.CachedTable;
import aprs.misc.Utils;
import aprs.system.AprsSystem;
import javax.swing.SwingUtilities;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("serial")
public class PositionMapJPanel extends javax.swing.JPanel {

    /**
     * Creates new form PositionMapJPanel
     */
    @SuppressWarnings({"nullness", "initialization"})
    @UIEffect
    public PositionMapJPanel() {
        try {
            initComponents();
            defaultBackgroundColor = this.getBackground();
            defaultForegroundColor = this.getForeground();
            Object spinnerIndexValueObject = jSpinnerIndex.getValue();
            if (!(spinnerIndexValueObject instanceof Integer)) {
                throw new IllegalStateException("jSpinnerIndex.getValue() returned " + spinnerIndexValueObject);
            }
            spinnerIndexValue = (int) spinnerIndexValueObject;
            handleErrorMapFilenameAction();
            jTextFieldErrorMapFilename.addActionListener(e -> handleErrorMapFilenameAction());
            posMapCachedTable = new CachedTable(jTablePosMap);
        } catch (Exception ex) {
            Logger.getLogger(PositionMapJPanel.class.getName()).log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }

    private File handleErrorMapFilenameAction() {
        try {
            return positionMapFile = Utils.file(jTextFieldErrorMapFilename.getText());
        } catch (Exception ex) {
            Logger.getLogger(PositionMapJPanel.class.getName()).log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }

    private final Color defaultBackgroundColor;
    private final Color defaultForegroundColor;
    private AprsSystem aprsSystem;

    public AprsSystem getAprsSystem() {
        return aprsSystem;
    }

    public void setAprsSystem(AprsSystem aprsSystem) {
        this.aprsSystem = aprsSystem;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "rawtypes", "nullness"})
    @UIEffect
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTextFieldErrorMapFilename = new javax.swing.JTextField();
        jButtonErrorMapFileBrowse = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTablePosMap = new JTable(  )
        {
            //  Returning the Class of each column will allow different
            //  renderers to be used based on Class

            public Class getColumnClass(int column)
            {
                return getValueAt(0, column).getClass();
            }

            public Component prepareRenderer(
                TableCellRenderer renderer, int row, int column)
            {
                Component c = super.prepareRenderer(renderer, row, column);
                JComponent jc = (JComponent)c;

                //  Alternate row color

                if (!isRowSelected(row))
                c.setBackground(row % 2 == 0 ? getBackground() : Color.LIGHT_GRAY);

                return c;
            }
        };
        jButtonSave = new javax.swing.JButton();
        jSpinnerIndex = new javax.swing.JSpinner();
        jButtonClear = new javax.swing.JButton();
        jLabelSize = new javax.swing.JLabel();
        jButtonPlot = new javax.swing.JButton();

        jTextFieldErrorMapFilename.setText("errors.csv");

        jButtonErrorMapFileBrowse.setText("Browse");
        jButtonErrorMapFileBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonErrorMapFileBrowseActionPerformed(evt);
            }
        });

        jTablePosMap.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Input_X", "Input_Y", "Input_Z", "Output_X", "Output_Y", "Output_Z", "Offset_X", "Offset_Y", "Offset_Z", "Label"
            }
        ) {
            final Class[] types = new Class [] {
                java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane3.setViewportView(jTablePosMap);

        jButtonSave.setText("Save");
        jButtonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSaveActionPerformed(evt);
            }
        });

        jSpinnerIndex.setModel(new javax.swing.SpinnerNumberModel(0, 0, 0, 1));
        jSpinnerIndex.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerIndexStateChanged(evt);
            }
        });

        jButtonClear.setText("Clear");
        jButtonClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonClearActionPerformed(evt);
            }
        });

        jLabelSize.setText("/1         ");

        jButtonPlot.setText("Plot");
        jButtonPlot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPlotActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 544, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jSpinnerIndex, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelSize)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonPlot)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonErrorMapFileBrowse)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonSave)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonClear))
                    .addComponent(jTextFieldErrorMapFilename))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSpinnerIndex, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jButtonErrorMapFileBrowse)
                        .addComponent(jButtonSave)
                        .addComponent(jButtonClear)
                        .addComponent(jLabelSize)
                        .addComponent(jButtonPlot)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldErrorMapFilename, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 123, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    final private List<PositionMap> positionMaps = new ArrayList<>();

    @SafeEffect
    public final List<PositionMap> getPositionMaps() {
        return Collections.unmodifiableList(positionMaps);
    }

    private @Nullable
    List<PositionMap> reversePositionMaps = new ArrayList<>();

    public final List<PositionMap> getReversePositionMaps() {
        if (null == reversePositionMaps) {
            List<PositionMap> l = new ArrayList<>();
            for (int i = positionMaps.size() - 1; i >= 0; i--) {
                PositionMap pm = positionMaps.get(i);
                if (null != pm && pm.getErrmapList() != null && pm.getErrmapList().size() > 0) {
                    PositionMap rpm = pm.reverse();
                    l.add(rpm);
                }
            }
            reversePositionMaps = l;
        }
        if (null == reversePositionMaps) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(reversePositionMaps);
    }

    private @Nullable
    PositionMap getPositionMap(int index) {
        return positionMaps.get(index);
    }

    @UIEffect
    private void setPositionMapOnDisplay(int index, PositionMap positionMap) throws IOException {
        assert SwingUtilities.isEventDispatchThread();
        reversePositionMaps = null;
        while (positionMaps.size() < index) {
            positionMaps.add(new PositionMap(Collections.emptyList()));
        }
        if (positionMaps.size() == index) {
            positionMaps.add(positionMap);
        } else {
            positionMaps.set(index, positionMap);
        }
        if (index == spinnerIndexValue) {
            if (null != positionMap) {
                loadPositionMapToTable(positionMap);
            }
        }
        this.updatePositionMapInfoOnDisplay();
    }

    @UIEffect
    private void updatePositionMapInfoOnDisplay() {
        jSpinnerIndex.setModel(new SpinnerNumberModel(spinnerIndexValue, 0, positionMaps.size(), 1));
        jLabelSize.setText("/" + positionMaps.size() + "   ");
    }

    @UIEffect
    public void addPositionMapOnDisplay(PositionMap positionMap) throws IOException {
        assert SwingUtilities.isEventDispatchThread();
        reversePositionMaps = null;
        for (int i = positionMaps.size() - 1; i >= 0; i--) {
            PositionMap pm = positionMaps.get(i);
            if (pm == null || pm.getErrmapList() == null || pm.getErrmapList().size() < 1) {
                positionMaps.remove(i);
            } else {
                break;
            }
            int spinVal = spinnerIndexValue;
            if (spinVal >= positionMaps.size()) {
                spinnerIndexValue = Math.max(0, positionMaps.size() - 1);
                jSpinnerIndex.setValue(spinnerIndexValue);
            }
        }
        this.positionMaps.add(positionMap);
        if (positionMaps.size() == 1) {
            loadPositionMapToTable(positionMap);
        }
        this.updatePositionMapInfoOnDisplay();
    }

    public void removePositionMap(PositionMap positionMap) throws IOException {
        reversePositionMaps = null;
        int spinVal = spinnerIndexValue;
        String pfn = positionMap.getFileName();
        this.positionMaps.remove(positionMap);
        if (pfn != null && pfn.length() > 0) {
            for (int i = 0; i < positionMaps.size(); i++) {
                PositionMap pm = positionMaps.get(i);
                if (null == pm || null == pm.getFileName() || Objects.equals(pm.getFileName(), pfn)) {
                    positionMaps.remove(i);
                    i--;
                }
            }
        }
        if (spinVal < 0 || spinVal >= positionMaps.size()) {
            spinVal = 0;
        }
        if (positionMaps.size() < 1) {
            addPositionMapOnDisplay(PositionMap.emptyPositionMap());
        }
        PositionMap spinValPositionMap = getPositionMap(spinVal);
        if (null != spinValPositionMap) {
            loadPositionMapToTable(spinValPositionMap);
        }
        aprsSystem.runOnDispatchThread(this::updatePositionMapInfoOnDisplay);
    }

    private boolean selected;

    /**
     * Get the value of selected
     *
     * @return the value of selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Set the value of selected
     *
     * @param selected new value of selected
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
        aprsSystem.runOnDispatchThread(() -> setSelectedOnDisplay(selected));
    }

    @UIEffect
    private void setSelectedOnDisplay(boolean selected) {
        if (selected) {
            this.setBackground(defaultBackgroundColor.darker());
            this.setForeground(this.defaultForegroundColor.brighter());
        } else {
            this.setBackground(defaultBackgroundColor);
            this.setForeground(this.defaultForegroundColor);
        }
    }

    volatile private File positionMapFile;

    public File getPositionMapFile() {
        return positionMapFile;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public @Nullable
    Object @Nullable [] getSelectedRowData() {
        TableModel model = jTablePosMap.getModel();
        if (model instanceof DefaultTableModel) {
            DefaultTableModel dtm = (DefaultTableModel) model;
            int selectedRow = jTablePosMap.getSelectedRow();
            Vector<Vector> vector = dtm.getDataVector();
            if (selectedRow >= 0 && selectedRow < vector.size()) {
                Vector vdata = vector.get(selectedRow);
                return vdata.toArray();
            }
        }
        return null;
    }

    public void setSelectedRowData(Object[] data) throws IOException {
        int selectedRow = jTablePosMap.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < jTablePosMap.getRowCount()) {
            PositionMap positionMap = getPositionMap(jSpinnerIndexValue());
            if (null == positionMap) {
                return;
            }
            final PositionMapEntry newPointPairLabelEntry
                    = PositionMapEntry.pointPairLabelEntry((double) data[0], (double) data[1], (double) data[2], (double) data[3], (double) data[4], (double) data[5], (String) data[9]);
            positionMap.getErrmapList().set(selectedRow, newPointPairLabelEntry);
            reversePositionMaps = null;
            List<PositionMap> newReversePositionMaps = getReversePositionMaps();
            loadPositionMapToTable(positionMap);
            updatePositionMapInfoOnDisplay();
        }
    }

    @SafeEffect
    public String[] getPositionMapFileNames() {
        String fa[] = new String[positionMaps.size()];
        for (int i = 0; i < fa.length; i++) {
            PositionMap pmI = positionMaps.get(i);
            if (null != pmI) {
                String filename = pmI.getFileName();
                if (null != filename) {
                    fa[i] = filename;
                }
            }
        }
        return fa;
    }

    private final CachedTable posMapCachedTable;

    private void loadPositionMapToTable(PositionMap positionMap) throws IOException {
        posMapCachedTable.setRowCount(0);
        String filename = positionMap.getFileName();
        if (null != filename) {
            positionMapFile = Utils.file(filename);
            jTextFieldErrorMapFilename.setText(filename);
        }
        for (Object a[] : positionMap.getTableIterable()) {
            posMapCachedTable.addRow(a);
        }
    }

    @UIEffect
    public void addPositionMapFileOnDisplay(File f) throws IOException, PositionMap.BadErrorMapFormatException {
        assert SwingUtilities.isEventDispatchThread();
        addPositionMapOnDisplay(new PositionMap(f));
    }

    private File startingDirectory;

    /**
     * Get the value of startingDirectory
     *
     * @return the value of startingDirectory
     */
    public File getStartingDirectory() {
        return startingDirectory;
    }

    /**
     * Set the value of startingDirectory
     *
     * @param startingDirectory new value of startingDirectory
     */
    @SafeEffect
    public void setStartingDirectory(File startingDirectory) {
        this.startingDirectory = startingDirectory;
    }

    @SuppressWarnings("nullness")
    private int jSpinnerIndexValue() {
        return (int) jSpinnerIndex.getValue();
    }

    @UIEffect
    private void jButtonErrorMapFileBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonErrorMapFileBrowseActionPerformed
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter csvExtensionFilter = new FileNameExtensionFilter("csv", "csv");
        chooser.addChoosableFileFilter(csvExtensionFilter);
        chooser.setFileFilter(csvExtensionFilter);
        if (null != startingDirectory) {
            chooser.setCurrentDirectory(startingDirectory);
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File selectedFile = chooser.getSelectedFile();
                if (null != selectedFile) {
                    setPositionMapOnDisplay(jSpinnerIndexValue(), new PositionMap(selectedFile));
                }
            } catch (IOException | PositionMap.BadErrorMapFormatException ex) {
                Logger.getLogger(PositionMapJPanel.class.getName()).log(Level.SEVERE, "", ex);
            }
        }
    }//GEN-LAST:event_jButtonErrorMapFileBrowseActionPerformed

    @UIEffect
    private void jButtonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSaveActionPerformed
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter csvExtensionFilter = new FileNameExtensionFilter("csv", "csv");
        chooser.addChoosableFileFilter(csvExtensionFilter);
        chooser.setFileFilter(csvExtensionFilter);
        if (null != startingDirectory) {
            chooser.setCurrentDirectory(startingDirectory);
        }
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File selectedFile = chooser.getSelectedFile();
                if (null != selectedFile) {
                    PositionMap positionMap = getPositionMap(jSpinnerIndexValue());
                    if (null == positionMap) {
                        positionMap = PositionMap.emptyPositionMap();
                    }
                    positionMap.saveFile(selectedFile);
                }
            } catch (IOException ex) {
                Logger.getLogger(PositionMapJPanel.class.getName()).log(Level.SEVERE, "", ex);
            }
        }
    }//GEN-LAST:event_jButtonSaveActionPerformed

    private volatile int spinnerIndexValue;

    @UIEffect
    private void jSpinnerIndexStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinnerIndexStateChanged
        try {
            Object spinnerIndexValueObject = jSpinnerIndex.getValue();
            if (!(spinnerIndexValueObject instanceof Integer)) {
                throw new IllegalStateException("jSpinnerIndex.getValue() returned " + spinnerIndexValueObject);
            }
            spinnerIndexValue = (int) spinnerIndexValueObject;
            final int spinVal = (int) spinnerIndexValueObject;
            if (getPositionMaps().size() == spinVal) {
                setPositionMapOnDisplay(spinVal, PositionMap.emptyPositionMap());
            }
            PositionMap spinValPositionMap = getPositionMap(spinVal);
            if (null != spinValPositionMap) {
                loadPositionMapToTable(spinValPositionMap);
            } else {
                loadPositionMapToTable(PositionMap.emptyPositionMap());
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }//GEN-LAST:event_jSpinnerIndexStateChanged

    @UIEffect
    private void jButtonClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearActionPerformed
        try {
            clearCurrentMapOnDisplay();
        } catch (Exception ex) {
            Logger.getLogger(PositionMapJPanel.class.getName()).log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }//GEN-LAST:event_jButtonClearActionPerformed

    @UIEffect
    @SuppressWarnings({"nullness"})
    private void jButtonPlotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPlotActionPerformed
        getPositionMap(spinnerIndexValue).plot();
    }//GEN-LAST:event_jButtonPlotActionPerformed

    @UIEffect
    public void clearCurrentMapOnDisplay() throws IOException {
        assert SwingUtilities.isEventDispatchThread();
        final int spinVal = spinnerIndexValue;
        setPositionMapOnDisplay(spinVal, PositionMap.emptyPositionMap());
        PositionMap spinValPositionMap = getPositionMap(spinVal);
        if (null != spinValPositionMap) {
            loadPositionMapToTable(spinValPositionMap);
        } else {
            loadPositionMapToTable(PositionMap.emptyPositionMap());
        }
    }

    @SafeEffect
    public void clearAllMaps() {
        try {
            positionMaps.clear();
            if (null != reversePositionMaps) {
                reversePositionMaps.clear();
            }
            clearCurrentMapOnDisplay();
            aprsSystem.runOnDispatchThread(this::clearAllMapsOnDisplay);
        } catch (Exception ex) {
            Logger.getLogger(PositionMapJPanel.class.getName()).log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }

    @UIEffect
    private void clearAllMapsOnDisplay() {
        jSpinnerIndex.setModel(new SpinnerNumberModel(0, 0, 0, 1));
        jLabelSize.setText("/1   ");
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonClear;
    private javax.swing.JButton jButtonErrorMapFileBrowse;
    private javax.swing.JButton jButtonPlot;
    private javax.swing.JButton jButtonSave;
    private javax.swing.JLabel jLabelSize;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSpinner jSpinnerIndex;
    private javax.swing.JTable jTablePosMap;
    private javax.swing.JTextField jTextFieldErrorMapFilename;
    // End of variables declaration//GEN-END:variables
}
