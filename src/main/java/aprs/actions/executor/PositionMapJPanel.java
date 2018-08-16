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

import aprs.cachedcomponents.CachedTable;
import aprs.misc.Utils;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.guieffect.qual.UIType;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("CanBeFinal")
public class PositionMapJPanel extends javax.swing.JPanel {

    /**
     * Creates new form PositionMapJPanel
     */
    @SuppressWarnings("initialization")
    @UIEffect
    public PositionMapJPanel() {
        initComponents();
        defaultBackgroundColor = this.getBackground();
        defaultForegroundColor = this.getForeground();
        Object spinnerIndexValueObject = jSpinnerIndex.getValue();
        if (!(spinnerIndexValueObject instanceof Integer)) {
            throw new IllegalStateException("jSpinnerIndex.getValue() returned " + spinnerIndexValueObject);
        }
        spinnerIndexValue = (int) spinnerIndexValueObject;
        positionMapFile = new File(jTextFieldErrorMapFilename.getText());
        jTextFieldErrorMapFilename.addActionListener(e -> positionMapFile = new File(jTextFieldErrorMapFilename.getText()));
        posMapCachedTable = new CachedTable(jTablePosMap);
    }

    private final Color defaultBackgroundColor;
    private final Color defaultForegroundColor;

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
    @UIEffect
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel13 = new javax.swing.JLabel();
        jTextFieldErrorMapFilename = new javax.swing.JTextField();
        jButtonErrorMapFileBrowse = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTablePosMap = new javax.swing.JTable();
        jButtonSave = new javax.swing.JButton();
        jSpinnerIndex = new javax.swing.JSpinner();
        jButtonClear = new javax.swing.JButton();
        jLabelSize = new javax.swing.JLabel();

        jLabel13.setText("File name:");

        jTextFieldErrorMapFilename.setText("errors.csv");

        jButtonErrorMapFileBrowse.setText("Browse");
        jButtonErrorMapFileBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonErrorMapFileBrowseActionPerformed(evt);
            }
        });

        jTablePosMap.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                { new Double(0.0),  new Double(0.0),  new Double(0.0),  new Double(0.0),  new Double(0.0),  new Double(0.0)},
                { new Double(1.0),  new Double(0.0),  new Double(0.0),  new Double(1.0),  new Double(0.0),  new Double(0.0)},
                { new Double(0.0),  new Double(1.0),  new Double(0.0),  new Double(0.0),  new Double(1.0),  new Double(0.0)},
                { new Double(0.0),  new Double(0.0),  new Double(1.0),  new Double(0.0),  new Double(0.0),  new Double(1.0)}
            },
            new String [] {
                "X", "Y", "Z", "Offset_X", "Offset_Y", "Offset_Z"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class
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
                        .addGap(10, 10, 10)
                        .addComponent(jLabel13)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldErrorMapFilename)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonErrorMapFileBrowse)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonSave)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonClear)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSpinnerIndex, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel13)
                        .addComponent(jTextFieldErrorMapFilename, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jButtonErrorMapFileBrowse)
                        .addComponent(jButtonSave)
                        .addComponent(jButtonClear)
                        .addComponent(jLabelSize)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 118, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    final private List<PositionMap> positionMaps = new ArrayList<>();

    @SafeEffect
    public final List<PositionMap> getPositionMaps() {
        return Collections.unmodifiableList(positionMaps);
    }
    @Nullable private List<PositionMap> reversePositionMaps = new ArrayList<>();

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

    @Nullable
    private PositionMap getPositionMap(int index) {
        return positionMaps.get(index);
    }

    private void setPositionMap(int index, PositionMap positionMap) {
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
        Utils.runOnDispatchThread(this::updatePositionMapInfoOnDisplay);
    }

    @UIEffect
    private void updatePositionMapInfoOnDisplay() {
        jSpinnerIndex.setModel(new SpinnerNumberModel(spinnerIndexValue, 0, positionMaps.size(), 1));
        jLabelSize.setText("/" + positionMaps.size() + "   ");
    }

    public void addPositionMap(PositionMap positionMap) {
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
                Utils.runOnDispatchThread(() -> jSpinnerIndex.setValue(spinnerIndexValue));
            }
        }
        this.positionMaps.add(positionMap);
        if (positionMaps.size() == 1) {
            loadPositionMapToTable(positionMap);
        }
        Utils.runOnDispatchThread(this::updatePositionMapInfoOnDisplay);
    }

    public void removePositionMap(PositionMap positionMap) {
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
            addPositionMap(PositionMap.emptyPositionMap());
        }
        PositionMap spinValPositionMap = getPositionMap(spinVal);
        if (null != spinValPositionMap) {
            loadPositionMapToTable(spinValPositionMap);
        }
        Utils.runOnDispatchThread(this::updatePositionMapInfoOnDisplay);
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
        Utils.runOnDispatchThread(() -> setSelectedOnDisplay(selected));
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
    
    private void loadPositionMapToTable(PositionMap positionMap) {
        posMapCachedTable.setRowCount(0);
        String filename = positionMap.getFileName();
        if (null != filename) {
            positionMapFile = new File(filename);
            jTextFieldErrorMapFilename.setText(filename);
        }
        for (Object a[] : positionMap.getTableIterable()) {
            posMapCachedTable.addRow(a);
        }
    }

    public void addPositionMapFile(File f) throws IOException, PositionMap.BadErrorMapFormatException {
        addPositionMap(new PositionMap(f));
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

    @UIEffect
    private void jButtonErrorMapFileBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonErrorMapFileBrowseActionPerformed
        JFileChooser chooser = new JFileChooser();
        if (null != startingDirectory) {
            chooser.setCurrentDirectory(startingDirectory);
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                setPositionMap((int) jSpinnerIndex.getValue(), new PositionMap(chooser.getSelectedFile()));
            } catch (IOException | PositionMap.BadErrorMapFormatException ex) {
                Logger.getLogger(PositionMapJPanel.class.getName()).log(Level.SEVERE, "", ex);
            }
        }
    }//GEN-LAST:event_jButtonErrorMapFileBrowseActionPerformed

    @UIEffect
    private void jButtonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSaveActionPerformed
        JFileChooser chooser = new JFileChooser();
        if (null != startingDirectory) {
            chooser.setCurrentDirectory(startingDirectory);
        }
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                PositionMap positionMap = getPositionMap((int) jSpinnerIndex.getValue());
                File f = chooser.getSelectedFile();
                if (null == positionMap) {
                    positionMap = PositionMap.emptyPositionMap();
                }
                positionMap.saveFile(f);
            } catch (IOException ex) {
                Logger.getLogger(PositionMapJPanel.class.getName()).log(Level.SEVERE, "", ex);
            }
        }
    }//GEN-LAST:event_jButtonSaveActionPerformed

    private volatile int spinnerIndexValue;

    @UIEffect
    private void jSpinnerIndexStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinnerIndexStateChanged
        Object spinnerIndexValueObject = jSpinnerIndex.getValue();
        if (!(spinnerIndexValueObject instanceof Integer)) {
            throw new IllegalStateException("jSpinnerIndex.getValue() returned " + spinnerIndexValueObject);
        }
        spinnerIndexValue = (int) spinnerIndexValueObject;
        final int spinVal = (int) spinnerIndexValueObject;
        if (getPositionMaps().size() == spinVal) {
            setPositionMap(spinVal, PositionMap.emptyPositionMap());
        }
        PositionMap spinValPositionMap = getPositionMap(spinVal);
        if (null != spinValPositionMap) {
            loadPositionMapToTable(spinValPositionMap);
        } else {
            loadPositionMapToTable(PositionMap.emptyPositionMap());
        }
    }//GEN-LAST:event_jSpinnerIndexStateChanged

    @UIEffect
    private void jButtonClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearActionPerformed
        clearCurrentMap();
    }//GEN-LAST:event_jButtonClearActionPerformed

    public void clearCurrentMap() {
        final int spinVal = spinnerIndexValue;
        setPositionMap(spinVal, PositionMap.emptyPositionMap());
        PositionMap spinValPositionMap = getPositionMap(spinVal);
        if (null != spinValPositionMap) {
            loadPositionMapToTable(spinValPositionMap);
        } else {
            loadPositionMapToTable(PositionMap.emptyPositionMap());
        }
    }

    @SafeEffect
    public void clearAllMaps() {
        positionMaps.clear();
        if (null != reversePositionMaps) {
            reversePositionMaps.clear();
        }
        clearCurrentMap();
        Utils.runOnDispatchThread(this::clearAllMapsOnDisplay);
    }

    @UIEffect
    private void clearAllMapsOnDisplay() {
        jSpinnerIndex.setModel(new SpinnerNumberModel(0, 0, 0, 1));
        jLabelSize.setText("/1   ");
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonClear;
    private javax.swing.JButton jButtonErrorMapFileBrowse;
    private javax.swing.JButton jButtonSave;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabelSize;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSpinner jSpinnerIndex;
    private javax.swing.JTable jTablePosMap;
    private javax.swing.JTextField jTextFieldErrorMapFilename;
    // End of variables declaration//GEN-END:variables
}
