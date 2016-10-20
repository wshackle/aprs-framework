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
package aprs.framework.pddl.executor;

import aprs.framework.AprsJFrame;
import aprs.framework.PddlAction;
import aprs.framework.database.DbSetup;
import aprs.framework.database.DbSetupBuilder;
import aprs.framework.database.DbSetupListener;
import aprs.framework.database.DbSetupPublisher;
import aprs.framework.spvision.VisionToDBJPanel;
import crcl.base.CRCLCommandInstanceType;
import crcl.base.CRCLProgramType;
import crcl.base.CRCLStatusType;
import crcl.base.EndCanonType;
import crcl.base.InitCanonType;
import crcl.base.MiddleCommandType;
import crcl.ui.client.PendantClientJPanel;
import crcl.utils.CRCLSocket;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.xml.bind.JAXBException;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class PddlExecutorJPanel extends javax.swing.JPanel implements PddlExecutorDisplayInterface, DbSetupListener, PendantClientJPanel.ProgramLineListener {

    /**
     * Creates new form ActionsToCrclJPanel
     */
    public PddlExecutorJPanel() {
        initComponents();
        jTableTraySlotDesign.getModel().addTableModelListener(traySlotModelListener);
    }

    private static Object[] getTableRow(JTable table, int row) {
        final int colCount = table.getModel().getColumnCount();
        if (colCount < 1) {
            return null;
        }
        Object out[] = new Object[table.getModel().getColumnCount()];
        for (int i = 0; i < colCount; i++) {
            out[i] = table.getModel().getValueAt(row, i);
        }
        return out;
    }

    private final TableModelListener traySlotModelListener = new TableModelListener() {
        @Override
        public void tableChanged(TableModelEvent e) {
            System.out.println("tableChanged : e = " + e);
            for (int i = e.getFirstRow(); i <= e.getLastRow(); i++) {
                if (updatingTraySlotTable) {
                    return;
                }
                commitTraySlotDesign(getTableRow(jTableTraySlotDesign, i));
            }
        }
    };

    private void commitTraySlotDesign(Object[] data) {
        try {
            TraySlotDesign tsd = new TraySlotDesign((int) data[0]);
            tsd.setTrayDesignName((String) data[1]);
            tsd.setPartDesignName((String) data[2]);
            tsd.setX_OFFSET((double) data[3]);
            tsd.setY_OFFSET((double) data[4]);
            pddlActionToCrclGenerator.setSingleTraySlotDesign(tsd);
        } catch (SQLException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTablePddlOutput = new javax.swing.JTable();
        jButtonLoadPddlActionsFromFile = new javax.swing.JButton();
        jTextFieldPddlOutputActions = new javax.swing.JTextField();
        jButtonLoad = new javax.swing.JButton();
        jButtonDbSetup = new javax.swing.JButton();
        jButtonGenerateCRCL = new javax.swing.JButton();
        jButtonPddlOutputViewEdit = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableCrclProgram = new javax.swing.JTable();
        jCheckBoxAutoStartCrcl = new javax.swing.JCheckBox();
        jCheckBoxNeedLookFor = new javax.swing.JCheckBox();
        jTextFieldIndex = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jCheckBoxReplan = new javax.swing.JCheckBox();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jScrollPaneOptions = new javax.swing.JScrollPane();
        jTableOptions = new javax.swing.JTable();
        jScrollPaneTraySlotDesign = new javax.swing.JScrollPane();
        jTableTraySlotDesign = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jTextFieldManualObjectName = new javax.swing.JTextField();
        jButtonTake = new javax.swing.JButton();
        jButtonLookFor = new javax.swing.JButton();
        jButtonReturn = new javax.swing.JButton();
        jTextFieldTakeCount = new javax.swing.JTextField();
        jTextFieldLookForCount = new javax.swing.JTextField();
        jTextFieldReturnCount = new javax.swing.JTextField();
        jButtonClear = new javax.swing.JButton();

        jLabel6.setText("Pddl Output Actions");

        jLabel7.setText("CRCL");

        jTablePddlOutput.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "CRCLIndex", "Label", "Type", "Args", "Cost"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane4.setViewportView(jTablePddlOutput);

        jButtonLoadPddlActionsFromFile.setText("Browse");
        jButtonLoadPddlActionsFromFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLoadPddlActionsFromFileActionPerformed(evt);
            }
        });

        jTextFieldPddlOutputActions.setEditable(false);
        jTextFieldPddlOutputActions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldPddlOutputActionsActionPerformed(evt);
            }
        });

        jButtonLoad.setText("Load");
        jButtonLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLoadActionPerformed(evt);
            }
        });

        jButtonDbSetup.setText("Database Setup");
        jButtonDbSetup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDbSetupActionPerformed(evt);
            }
        });

        jButtonGenerateCRCL.setText("Generate");
        jButtonGenerateCRCL.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGenerateCRCLActionPerformed(evt);
            }
        });

        jButtonPddlOutputViewEdit.setText("View/Edit");
        jButtonPddlOutputViewEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPddlOutputViewEditActionPerformed(evt);
            }
        });

        jTableCrclProgram.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                { new Integer(1), null},
                { new Integer(2), null},
                { new Integer(3), null},
                { new Integer(4), null}
            },
            new String [] {
                "ID", "Text"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTableCrclProgram);

        jCheckBoxAutoStartCrcl.setText("Automatically Start CRCL programs");

        jCheckBoxNeedLookFor.setText("Skip LookFor");

        jTextFieldIndex.setText("0");
        jTextFieldIndex.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldIndexActionPerformed(evt);
            }
        });

        jLabel2.setText("Index");

        jCheckBoxReplan.setText("Continue");

        jTableOptions.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"lookForXYZ", "0.0,0.0,0.0"},
                {null, null}
            },
            new String [] {
                "Name", "Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPaneOptions.setViewportView(jTableOptions);

        jTabbedPane1.addTab("Options", jScrollPaneOptions);

        jTableTraySlotDesign.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "ID", "TrayDesignName", "PartDesignName", "X_OFFSET", "Y_OFFSET"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTableTraySlotDesign.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jTableTraySlotDesignMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jTableTraySlotDesignMouseReleased(evt);
            }
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTableTraySlotDesignMouseClicked(evt);
            }
        });
        jScrollPaneTraySlotDesign.setViewportView(jTableTraySlotDesign);

        jTabbedPane1.addTab("Tray Slot Desgn", jScrollPaneTraySlotDesign);

        jLabel1.setText("Object:");

        jTextFieldManualObjectName.setText("sku_part_large_gear");
        jTextFieldManualObjectName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldManualObjectNameActionPerformed(evt);
            }
        });

        jButtonTake.setText("Take");
        jButtonTake.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonTakeActionPerformed(evt);
            }
        });

        jButtonLookFor.setText("Look-For");
        jButtonLookFor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLookForActionPerformed(evt);
            }
        });

        jButtonReturn.setText("Return");
        jButtonReturn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonReturnActionPerformed(evt);
            }
        });

        jTextFieldTakeCount.setEditable(false);
        jTextFieldTakeCount.setText("0   ");

        jTextFieldLookForCount.setEditable(false);
        jTextFieldLookForCount.setText("0   ");

        jTextFieldReturnCount.setEditable(false);
        jTextFieldReturnCount.setText("0   ");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldManualObjectName, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonTake)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldTakeCount, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonLookFor)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldLookForCount, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonReturn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldReturnCount, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(161, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextFieldManualObjectName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonTake)
                    .addComponent(jButtonLookFor)
                    .addComponent(jButtonReturn)
                    .addComponent(jTextFieldTakeCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextFieldLookForCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextFieldReturnCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(83, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Manual Pickup Return", jPanel1);

        jButtonClear.setText("Clear");
        jButtonClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonClearActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jScrollPane4)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldPddlOutputActions, javax.swing.GroupLayout.PREFERRED_SIZE, 325, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonLoad)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonLoadPddlActionsFromFile)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonPddlOutputViewEdit)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonClear)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jCheckBoxReplan)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldIndex, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxNeedLookFor)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jCheckBoxAutoStartCrcl)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonGenerateCRCL)
                        .addGap(11, 11, 11)
                        .addComponent(jButtonDbSetup)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jButtonLoadPddlActionsFromFile)
                    .addComponent(jTextFieldPddlOutputActions, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonLoad)
                    .addComponent(jButtonPddlOutputViewEdit)
                    .addComponent(jButtonClear))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 145, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 145, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jCheckBoxAutoStartCrcl)
                        .addComponent(jButtonGenerateCRCL)
                        .addComponent(jButtonDbSetup)
                        .addComponent(jCheckBoxNeedLookFor)
                        .addComponent(jTextFieldIndex, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel2)
                        .addComponent(jCheckBoxReplan)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 145, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    public void browseActionsFile() throws IOException {
        JFileChooser chooser = new JFileChooser(new File(jTextFieldPddlOutputActions.getText()).getParent());
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            jTextFieldPddlOutputActions.setText(f.getCanonicalPath());
            saveProperties();
        }
    }

    private final PddlActionToCrclGenerator pddlActionToCrclGenerator = new PddlActionToCrclGenerator();

    public PddlActionToCrclGenerator getPddlActionToCrclGenerator() {
        return pddlActionToCrclGenerator;
    }

//    public void setPddlActionToCrclGenerator(PddlActionToCrclGenerator pddlActionToCrclGenerator) {
//        this.pddlActionToCrclGenerator = pddlActionToCrclGenerator;
//    }
    private List<PddlAction> actionsList;

    /**
     * Get the value of actionsList
     *
     * @return the value of actionsList
     */
    public List<PddlAction> getActionsList() {
        return actionsList;
    }

    /**
     * Set the value of actionsList
     *
     * @param actionsList new value of actionsList
     */
    public void setActionsList(List<PddlAction> actionsList) {
        this.actionsList = actionsList;
        DefaultTableModel model = (DefaultTableModel) jTablePddlOutput.getModel();
        model.setNumRows(0);
    }

    private File propertiesFile;

    /**
     * Get the value of propertiesFile
     *
     * @return the value of propertiesFile
     */
    public File getPropertiesFile() {
        return propertiesFile;
    }

    /**
     * Set the value of propertiesFile
     *
     * @param propertiesFile new value of propertiesFile
     */
    public void setPropertiesFile(File propertiesFile) {
        this.propertiesFile = propertiesFile;
    }

    private static final String PDDLOUTPUT = "pddl.output";
    private static final String PDDLCRCLAUTOSTART = "pddl.crcl.autostart";

    public void saveProperties() throws IOException {
        Map<String, String> propsMap = new HashMap<>();
        propsMap.put(PDDLOUTPUT, jTextFieldPddlOutputActions.getText());
        propsMap.put(PDDLCRCLAUTOSTART, Boolean.toString(jCheckBoxAutoStartCrcl.isSelected()));
        Properties props = new Properties();
        props.putAll(propsMap);
        props.putAll(getTableOptions());
        try (FileWriter fw = new FileWriter(propertiesFile)) {
            props.store(fw, "");
        }
    }

    public void autoResizeTableColWidths(JTable table) {

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        int fullsize = 0;
        Container parent = table.getParent();
        if (null != parent) {
            fullsize = Math.max(parent.getPreferredSize().width, parent.getSize().width);
        }
        int sumWidths = 0;
        for (int i = 0; i < table.getColumnCount(); i++) {
            DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
            TableColumn col = colModel.getColumn(i);
            int width = 0;

            TableCellRenderer renderer = col.getHeaderRenderer();
            if (renderer == null) {
                renderer = table.getTableHeader().getDefaultRenderer();
            }
            Component headerComp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(),
                    false, false, 0, i);
            width = Math.max(width, headerComp.getPreferredSize().width);
            for (int r = 0; r < table.getRowCount(); r++) {
                renderer = table.getCellRenderer(r, i);
                Component comp = renderer.getTableCellRendererComponent(table, table.getValueAt(r, i),
                        false, false, r, i);
                width = Math.max(width, comp.getPreferredSize().width);
            }
            if (i == table.getColumnCount() - 1) {
                if (width < fullsize - sumWidths) {
                    width = fullsize - sumWidths;
                }
            }
            col.setPreferredWidth(width + 2);
            sumWidths += width + 2;
        }
    }

    public void autoResizeTableRowHeights(JTable table) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        for (int rowIndex = 0; rowIndex < table.getRowCount(); rowIndex++) {
            int height = 0;
            for (int colIndex = 0; colIndex < table.getColumnCount(); colIndex++) {
                DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
                TableColumn col = colModel.getColumn(colIndex);
                TableCellRenderer renderer = table.getCellRenderer(rowIndex, colIndex);
                Object value = table.getValueAt(rowIndex, colIndex);
                Component comp = renderer.getTableCellRendererComponent(table, value,
                        false, false, rowIndex, colIndex);
                Dimension compSize = comp.getPreferredSize();
                int thisCompHeight = compSize.height;
                height = Math.max(height, thisCompHeight);
            }
            if (height > 0) {
                table.setRowHeight(rowIndex, height);
            }
        }
    }

    public void addAction(PddlAction action) {
        if (null != action) {
            this.getActionsList().add(action);
            DefaultTableModel model = (DefaultTableModel) jTablePddlOutput.getModel();
            model.addRow(new Object[]{-1, action.getLabel(), action.getType(), Arrays.toString(action.getArgs()), action.getCost()});
        }
    }

    public void processActions() {
        try {
            generateCrcl();
        } catch (IOException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void loadActionsFile(File f) throws IOException {
        if (null != f && f.canRead()) {
            this.setActionsList(new ArrayList<>());
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while (null != (line = br.readLine())) {
                    addAction(PddlAction.parse(line));
                }
            }
            autoResizeTableColWidthsPddlOutput();
            setLoadEnabled(true);
            jCheckBoxReplan.setSelected(false);
            setReplanFromIndex(0);
            jTextFieldIndex.setText("0");
        }
    }

    public void autoResizeTableColWidthsPddlOutput() {
        autoResizeTableColWidths(jTablePddlOutput);
    }

    private void jButtonLoadPddlActionsFromFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLoadPddlActionsFromFileActionPerformed
        try {
            browseActionsFile();
            loadActionsFile(new File(jTextFieldPddlOutputActions.getText()));

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonLoadPddlActionsFromFileActionPerformed

    private void jButtonLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLoadActionPerformed
        try {
            loadActionsFile(new File(jTextFieldPddlOutputActions.getText()));

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonLoadActionPerformed

    private void jTextFieldPddlOutputActionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldPddlOutputActionsActionPerformed
        try {
            loadActionsFile(new File(jTextFieldPddlOutputActions.getText()));

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jTextFieldPddlOutputActionsActionPerformed

    private DbSetupPublisher dbSetupPublisher;

    private Callable<DbSetupPublisher> dbSetupSupplier = null;

    public Callable<DbSetupPublisher> getDbSetupSupplier() {
        return dbSetupSupplier;
    }

    public void setDbSetupSupplier(Callable<DbSetupPublisher> dbSetupSupplier) {
        this.dbSetupSupplier = dbSetupSupplier;
        try {
            dbSetupPublisher = dbSetupSupplier.call();
            dbSetupPublisher.addDbSetupListener(this);

        } catch (Exception ex) {
            Logger.getLogger(VisionToDBJPanel.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void jButtonDbSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDbSetupActionPerformed
        if (null != dbSetupSupplier) {
            try {
                dbSetupPublisher = dbSetupSupplier.call();
                dbSetupPublisher.addDbSetupListener(this);

            } catch (Exception ex) {
                Logger.getLogger(VisionToDBJPanel.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jButtonDbSetupActionPerformed

    private CRCLProgramType createEmptyProgram() {
        CRCLProgramType program = new CRCLProgramType();
        InitCanonType initCmd = program.getInitCanon();
        if (null == initCmd) {
            initCmd = new InitCanonType();
            if (null == initCmd.getCommandID()) {
                initCmd.setCommandID(BigInteger.ONE);
            }
        }
        program.setInitCanon(initCmd);
        EndCanonType endCmd = program.getEndCanon();
        if (null == endCmd) {
            endCmd = new EndCanonType();
            if (null == endCmd.getCommandID()) {
                endCmd.setCommandID(BigInteger.ONE);
            }
        }
        program.setEndCanon(endCmd);
        return program;
    }

    private CRCLProgramType crclProgram;

    /**
     * Get the value of crclProgram
     *
     * @return the value of crclProgram
     */
    public CRCLProgramType getCrclProgram() {
        return crclProgram;
    }

    List<JTextArea> crclAreas = new ArrayList<>();
    JTextArea editTableArea = new JTextArea();

    private String trimXml(String in) {
        int endHeaderIndex = in.indexOf("?>");
        if (endHeaderIndex > 0) {
            return in.substring(endHeaderIndex + 2).trim();
        } else {
            return in.trim();
        }
    }

    private void loadProgramToTable(CRCLProgramType crclProgram) {
        DefaultTableModel model = (DefaultTableModel) jTableCrclProgram.getModel();
        jTableCrclProgram.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                while (crclAreas.size() <= row) {
                    JTextArea area = new JTextArea();
                    area.setOpaque(true);
                    area.setVisible(true);
                    crclAreas.add(area);
                }
                crclAreas.get(row).setText(value.toString());
                return crclAreas.get(row);
            }

        });
        jTableCrclProgram.getColumnModel().getColumn(1).setCellEditor(new TableCellEditor() {

            private List<CellEditorListener> listeners = new ArrayList<>();

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                editTableArea.setOpaque(true);
                editTableArea.setVisible(true);
                editTableArea.setText(value.toString());
                return editTableArea;
            }

            @Override
            public Object getCellEditorValue() {
                return editTableArea.getText();
            }

            @Override
            public boolean isCellEditable(EventObject anEvent) {
                return true;
            }

            @Override
            public boolean shouldSelectCell(EventObject anEvent) {
                return true;
            }

            @Override
            public boolean stopCellEditing() {
                for (int i = 0; i < listeners.size(); i++) {
                    CellEditorListener l = listeners.get(i);
                    if (null != l) {
                        l.editingStopped(new ChangeEvent(jTableCrclProgram));
                    }
                }
                return true;
            }

            @Override
            public void cancelCellEditing() {
                for (int i = 0; i < listeners.size(); i++) {
                    CellEditorListener l = listeners.get(i);
                    if (null != l) {
                        l.editingCanceled(new ChangeEvent(jTableCrclProgram));
                    }
                }
            }

            @Override
            public void addCellEditorListener(CellEditorListener l) {
                listeners.add(l);
            }

            @Override
            public void removeCellEditorListener(CellEditorListener l) {
                listeners.remove(l);
            }
        });
        model.setRowCount(0);
        CRCLSocket crclSocket = CRCLSocket.getUtilSocket();
        CRCLCommandInstanceType instance = new CRCLCommandInstanceType();
        InitCanonType initCanon = crclProgram.getInitCanon();
        if (null != initCanon) {
            try {
                instance.setCRCLCommand(initCanon);
                model.addRow(new Object[]{initCanon.getCommandID().intValue(),
                    trimXml(crclSocket.commandInstanceToPrettyString(instance, false))
                });

            } catch (JAXBException ex) {
                Logger.getLogger(PddlExecutorJPanel.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        for (MiddleCommandType midCmd : crclProgram.getMiddleCommand()) {
            if (null != midCmd) {
                try {
                    instance.setCRCLCommand(midCmd);
                    model.addRow(new Object[]{midCmd.getCommandID().intValue(),
                        trimXml(crclSocket.commandInstanceToPrettyString(instance, false))
                    });

                } catch (JAXBException ex) {
                    Logger.getLogger(PddlExecutorJPanel.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        EndCanonType endCanon = crclProgram.getEndCanon();
        if (null != endCanon) {
            try {
                instance.setCRCLCommand(endCanon);
                model.addRow(new Object[]{endCanon.getCommandID().intValue(),
                    trimXml(crclSocket.commandInstanceToPrettyString(instance, false))
                });

            } catch (JAXBException ex) {
                Logger.getLogger(PddlExecutorJPanel.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        autoResizeTableRowHeights(jTableCrclProgram);
        autoResizeTableColWidths(jTableCrclProgram);
    }

    /**
     * Set the value of crclProgram
     *
     * @param crclProgram new value of crclProgram
     */
    public void setCrclProgram(CRCLProgramType crclProgram) {
        try {
            this.crclProgram = crclProgram;
            AprsJFrame aprsJframe = AprsJFrame.getCurrentAprsJFrame();
            if (null != aprsJframe) {

                aprsJframe.setCRCLProgram(crclProgram, this.jCheckBoxAutoStartCrcl.isSelected());
                aprsJframe.addProgramLineListener(this);
            }
            if (javax.swing.SwingUtilities.isEventDispatchThread()) {
                this.loadProgramToTable(crclProgram);
            } else {
                javax.swing.SwingUtilities.invokeLater(() -> loadProgramToTable(crclProgram));
            }
//            String programText = CRCLSocket.getUtilSocket().programToPrettyString(crclProgram, true);
//            jTextAreaCrcl.setText(programText);

        } catch (Exception ex) {
            Logger.getLogger(PddlExecutorJPanel.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void jButtonGenerateCRCLActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGenerateCRCLActionPerformed

        try {
            generateCrcl();
        } catch (IOException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonGenerateCRCLActionPerformed

    private void jButtonPddlOutputViewEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPddlOutputViewEditActionPerformed
        try {
            Desktop.getDesktop().open(new File(jTextFieldPddlOutputActions.getText()));

        } catch (IOException ex) {
            Logger.getLogger(PddlExecutorJPanel.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonPddlOutputViewEditActionPerformed

    public int getReplanFromIndex() {
        return replanFromIndex;
    }

    public void setReplanFromIndex(int replanFromIndex) {
        this.replanFromIndex = replanFromIndex;
        jTablePddlOutput.getSelectionModel().setSelectionInterval(replanFromIndex, replanFromIndex);
        jTablePddlOutput.scrollRectToVisible(new Rectangle(jTablePddlOutput.getCellRect(replanFromIndex, 0, true)));
    }


    private void jTextFieldIndexActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldIndexActionPerformed
        setReplanFromIndex(Integer.valueOf(jTextFieldIndex.getText()));
    }//GEN-LAST:event_jTextFieldIndexActionPerformed

    private boolean updatingTraySlotTable = false;

    private void updateTraySlotTable() {
        try {
            updatingTraySlotTable = true;
            jTableTraySlotDesign.getModel().removeTableModelListener(traySlotModelListener);
            checkDbSupplierPublisher();
            List<TraySlotDesign> designs = pddlActionToCrclGenerator.getAllTraySlotDesigns();
            DefaultTableModel model = (DefaultTableModel) jTableTraySlotDesign.getModel();
            model.setRowCount(0);
            for (TraySlotDesign d : designs) {
                model.addRow(new Object[]{d.getID(), d.getPartDesignName(), d.getTrayDesignName(), d.getX_OFFSET(), d.getY_OFFSET()});
            }
        } catch (SQLException | IOException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        jTableTraySlotDesign.getModel().addTableModelListener(traySlotModelListener);
        updatingTraySlotTable = false;
    }

    private void newTraySlotTable() {
        try {
            updatingTraySlotTable = true;
            jTableTraySlotDesign.getModel().removeTableModelListener(traySlotModelListener);
            checkDbSupplierPublisher();
            TraySlotDesign tsd = new TraySlotDesign((-99)); // ID ignored on new operation
            tsd.setPartDesignName("partDesignName");
            tsd.setTrayDesignName("trayDesignName");
            tsd.setX_OFFSET(0.0);
            tsd.setY_OFFSET(0.0);
            pddlActionToCrclGenerator.newSingleTraySlotDesign(tsd);
            List<TraySlotDesign> designs = pddlActionToCrclGenerator.getAllTraySlotDesigns();
            DefaultTableModel model = (DefaultTableModel) jTableTraySlotDesign.getModel();
            model.setRowCount(0);
            for (TraySlotDesign d : designs) {
                model.addRow(new Object[]{d.getID(), d.getPartDesignName(), d.getTrayDesignName(), d.getX_OFFSET(), d.getY_OFFSET()});
            }
        } catch (SQLException | IOException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        jTableTraySlotDesign.getModel().addTableModelListener(traySlotModelListener);
        updatingTraySlotTable = false;
    }

    private JPopupMenu traySlotPopup = new JPopupMenu();

    {
        JMenuItem updateMenuItem = new JMenuItem("Update");
        updateMenuItem.addActionListener(e -> {
            traySlotPopup.setVisible(false);
            updateTraySlotTable();
        });
        traySlotPopup.add(updateMenuItem);
        JMenuItem newRowMenuItem = new JMenuItem("New");
        newRowMenuItem.addActionListener(e -> {
            traySlotPopup.setVisible(false);
            newTraySlotTable();
        });
        traySlotPopup.add(newRowMenuItem);
    }

    private void showTraySlotPopup(Component comp, int x, int y) {
        traySlotPopup.show(comp, x, y);
    }
    private void jTableTraySlotDesignMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTableTraySlotDesignMouseClicked
        if (evt.isPopupTrigger()) {
            showTraySlotPopup(evt.getComponent(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_jTableTraySlotDesignMouseClicked

    private void jTableTraySlotDesignMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTableTraySlotDesignMousePressed
        if (evt.isPopupTrigger()) {
            showTraySlotPopup(evt.getComponent(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_jTableTraySlotDesignMousePressed

    private void jTableTraySlotDesignMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTableTraySlotDesignMouseReleased
        if (evt.isPopupTrigger()) {
            showTraySlotPopup(evt.getComponent(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_jTableTraySlotDesignMouseReleased

    private void jButtonClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearActionPerformed
        clearAll();
    }//GEN-LAST:event_jButtonClearActionPerformed

    public void clearAll() {
        this.setActionsList(new ArrayList<>());
        DefaultTableModel model = (DefaultTableModel) jTableCrclProgram.getModel();
        model.setRowCount(0);
        replanFromIndex = 0;
        jTextFieldIndex.setText(Integer.toString(replanFromIndex));
    }

    private int takePartCount = 0;
    private void jButtonTakeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonTakeActionPerformed
        try {
            takePartCount++;
            this.jTextFieldTakeCount.setText(Integer.toString(takePartCount));
            this.takePart(jTextFieldManualObjectName.getText());
        } catch (IOException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonTakeActionPerformed

    private int lookForCount = 0;
    private void jButtonLookForActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLookForActionPerformed
        try {
            lookForCount++;
            this.jTextFieldLookForCount.setText(Integer.toString(lookForCount));
            this.lookFor(jTextFieldManualObjectName.getText());
        } catch (IOException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonLookForActionPerformed

    private int returnCount = 0;
    private void jButtonReturnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonReturnActionPerformed
        try {
            returnCount++;
            this.jTextFieldReturnCount.setText(Integer.toString(returnCount));
            this.returnPart(jTextFieldManualObjectName.getText());
        } catch (IOException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonReturnActionPerformed

    private void jTextFieldManualObjectNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldManualObjectNameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldManualObjectNameActionPerformed

    public void setCrclIndexes(int indexes[]) {
        DefaultTableModel model = (DefaultTableModel) jTablePddlOutput.getModel();
        for (int i = 0; i < indexes.length; i++) {
            if (i >= model.getRowCount()) {
                break;
            }
            model.setValueAt(indexes[i], i, 0);
        }
        this.autoResizeTableColWidths(jTablePddlOutput);
    }

    public void setPddlLabelss(String labels[]) {
        DefaultTableModel model = (DefaultTableModel) jTablePddlOutput.getModel();
        for (int i = 0; i < labels.length; i++) {
            if (i >= model.getRowCount()) {
                break;
            }
            if (null != labels[i]) {
                model.setValueAt(labels[i], i, 1);
            }
        }
        this.autoResizeTableColWidths(jTablePddlOutput);
    }

    private void generateCrcl() throws IOException {
        checkDbSupplierPublisher();
        Map<String, String> options = getTableOptions();
        if (replanFromIndex < 0 || replanFromIndex > actionsList.size()) {
            replanFromIndex = 0;
        }
        List<MiddleCommandType> cmds = pddlActionToCrclGenerator.generate(actionsList, this.replanFromIndex, options);
        int indexes[] = pddlActionToCrclGenerator.getActionToCrclIndexes();
        indexes = Arrays.copyOf(indexes, indexes.length);
        setCrclIndexes(indexes);
        setPddlLabelss(pddlActionToCrclGenerator.getActionToCrclLabels());
        CRCLProgramType program = createEmptyProgram();
        if (pddlActionToCrclGenerator.getLastIndex() < actionsList.size()) {
            jCheckBoxReplan.setSelected(true);
            setReplanFromIndex(pddlActionToCrclGenerator.getLastIndex() + 1);
        } else {
            jCheckBoxReplan.setSelected(false);
            setReplanFromIndex(0);
        }
        jTextFieldIndex.setText(Integer.toString(replanFromIndex));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        program.getEndCanon().setCommandID(BigInteger.valueOf(cmds.size() + 2));
        setCrclProgram(program);
        replanStarted = false;
    }

    public void takePart(String part) throws IOException {
        clearAll();
        Map<String, String> options = getTableOptions();
        replanFromIndex = 0;
        List<PddlAction> takePartActionsList = new ArrayList<>();
        PddlAction takePartAction = new PddlAction("", "take-part",
                new String[]{"", part}, "cost");
        takePartActionsList.add(takePartAction);
        List<MiddleCommandType> cmds = pddlActionToCrclGenerator.generate(takePartActionsList, this.replanFromIndex, options);
        int indexes[] = pddlActionToCrclGenerator.getActionToCrclIndexes();
        indexes = Arrays.copyOf(indexes, indexes.length);
        setCrclIndexes(indexes);
        setPddlLabelss(pddlActionToCrclGenerator.getActionToCrclLabels());
        CRCLProgramType program = createEmptyProgram();
        if (pddlActionToCrclGenerator.getLastIndex() < actionsList.size()) {
            jCheckBoxReplan.setSelected(true);
            setReplanFromIndex(pddlActionToCrclGenerator.getLastIndex() + 1);
        } else {
            jCheckBoxReplan.setSelected(false);
            setReplanFromIndex(0);
        }
        jTextFieldIndex.setText(Integer.toString(replanFromIndex));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        program.getEndCanon().setCommandID(BigInteger.valueOf(cmds.size() + 2));
        setCrclProgram(program);
        replanStarted = false;
    }

    public void returnPart(String part) throws IOException {
        clearAll();
        Map<String, String> options = getTableOptions();
        replanFromIndex = 0;
        List<MiddleCommandType> cmds = new ArrayList<>();
        pddlActionToCrclGenerator.setOptions(options);
        pddlActionToCrclGenerator.returnPart(part, cmds);
        int indexes[] = pddlActionToCrclGenerator.getActionToCrclIndexes();
        indexes = Arrays.copyOf(indexes, indexes.length);
        setCrclIndexes(indexes);
        setPddlLabelss(pddlActionToCrclGenerator.getActionToCrclLabels());
        CRCLProgramType program = createEmptyProgram();
        if (pddlActionToCrclGenerator.getLastIndex() < actionsList.size()) {
            jCheckBoxReplan.setSelected(true);
            setReplanFromIndex(pddlActionToCrclGenerator.getLastIndex() + 1);
        } else {
            jCheckBoxReplan.setSelected(false);
            setReplanFromIndex(0);
        }
        jTextFieldIndex.setText(Integer.toString(replanFromIndex));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        program.getEndCanon().setCommandID(BigInteger.valueOf(cmds.size() + 2));
        setCrclProgram(program);
        replanStarted = false;
    }

    public void lookFor(String part) throws IOException {
        clearAll();
        Map<String, String> options = getTableOptions();
        replanFromIndex = 0;
        List<PddlAction> lookForActionsList = new ArrayList<>();
        PddlAction takePartAction = new PddlAction("", "look-for-part",
                new String[]{"", part}, "cost");
        lookForActionsList.add(takePartAction);
        List<MiddleCommandType> cmds = pddlActionToCrclGenerator.generate(lookForActionsList, this.replanFromIndex, options);
        int indexes[] = pddlActionToCrclGenerator.getActionToCrclIndexes();
        indexes = Arrays.copyOf(indexes, indexes.length);
        setCrclIndexes(indexes);
        setPddlLabelss(pddlActionToCrclGenerator.getActionToCrclLabels());
        CRCLProgramType program = createEmptyProgram();
        if (pddlActionToCrclGenerator.getLastIndex() < actionsList.size()) {
            jCheckBoxReplan.setSelected(true);
            setReplanFromIndex(pddlActionToCrclGenerator.getLastIndex() + 1);
        } else {
            jCheckBoxReplan.setSelected(false);
            setReplanFromIndex(0);
        }
        jTextFieldIndex.setText(Integer.toString(replanFromIndex));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        program.getEndCanon().setCommandID(BigInteger.valueOf(cmds.size() + 2));
        setCrclProgram(program);
        replanStarted = false;
    }

    private void checkDbSupplierPublisher() throws IOException {
        if (null != this.pddlActionToCrclGenerator && pddlActionToCrclGenerator.isConnected()) {
            return;
        }
        if (null != dbSetupSupplier) {
            try {
                dbSetupPublisher = dbSetupSupplier.call();
                dbSetupPublisher.addDbSetupListener(this);

            } catch (Exception ex) {
                Logger.getLogger(VisionToDBJPanel.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (null != dbSetupPublisher) {
            dbSetupPublisher.setDbSetup(new DbSetupBuilder().setup(dbSetupPublisher.getDbSetup()).connected(true).build());
            List<Future<?>> futures = dbSetupPublisher.notifyAllDbSetupListeners();
//            for (Future<?> f : futures) {
//                if (!f.isDone() && !f.isCancelled()) {
//                    try {
//                        f.get();
//
//                    } catch (InterruptedException ex) {
//                        Logger.getLogger(PddlExecutorJPanel.class
//                                .getName()).log(Level.SEVERE, null, ex);
//
//                    } catch (ExecutionException ex) {
//                        Logger.getLogger(PddlExecutorJPanel.class
//                                .getName()).log(Level.SEVERE, null, ex);
//                    }
//                }
//            }
        } else {
            System.err.println("dbSetupPublisher == null");
        }
    }

    public Map<String, String> getTableOptions() {
        Map<String, String> options = new HashMap<>();
        TableModel model = jTableOptions.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            Object key = model.getValueAt(i, 0);
            Object val = model.getValueAt(i, 1);
            if (null != key && null != val) {
                options.put(key.toString(), val.toString());
            }
        }
        return options;
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonClear;
    private javax.swing.JButton jButtonDbSetup;
    private javax.swing.JButton jButtonGenerateCRCL;
    private javax.swing.JButton jButtonLoad;
    private javax.swing.JButton jButtonLoadPddlActionsFromFile;
    private javax.swing.JButton jButtonLookFor;
    private javax.swing.JButton jButtonPddlOutputViewEdit;
    private javax.swing.JButton jButtonReturn;
    private javax.swing.JButton jButtonTake;
    private javax.swing.JCheckBox jCheckBoxAutoStartCrcl;
    private javax.swing.JCheckBox jCheckBoxNeedLookFor;
    private javax.swing.JCheckBox jCheckBoxReplan;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPaneOptions;
    private javax.swing.JScrollPane jScrollPaneTraySlotDesign;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTableCrclProgram;
    private javax.swing.JTable jTableOptions;
    private javax.swing.JTable jTablePddlOutput;
    private javax.swing.JTable jTableTraySlotDesign;
    private javax.swing.JTextField jTextFieldIndex;
    private javax.swing.JTextField jTextFieldLookForCount;
    private javax.swing.JTextField jTextFieldManualObjectName;
    private javax.swing.JTextField jTextFieldPddlOutputActions;
    private javax.swing.JTextField jTextFieldReturnCount;
    private javax.swing.JTextField jTextFieldTakeCount;
    // End of variables declaration//GEN-END:variables

    @Override
    public void loadProperties() throws IOException {
        if (null != propertiesFile && propertiesFile.canRead()) {
            Properties props = new Properties();
            try (FileReader fr = new FileReader(propertiesFile)) {
                props.load(fr);
            }
            String output = props.getProperty(PDDLOUTPUT);
            if (null != output) {
                jTextFieldPddlOutputActions.setText(output);
                File f = new File(output);
                if (f.canRead()) {
                    loadActionsFile(f);
                }
            }
            String autostart = props.getProperty(PDDLCRCLAUTOSTART);
            if (null != autostart) {
                this.jCheckBoxAutoStartCrcl.setSelected(Boolean.valueOf(autostart));
            }
            for (String name : props.stringPropertyNames()) {
                if (!name.equals(PDDLCRCLAUTOSTART) && !name.equals(PDDLOUTPUT)) {
                    DefaultTableModel model = (DefaultTableModel) jTableOptions.getModel();
                    boolean foundit = false;
                    for (int i = 0; i < model.getRowCount(); i++) {
                        String nameFromTable = model.getValueAt(i, 0).toString();
                        if (nameFromTable.equals(name)) {
                            model.setValueAt(props.getProperty(name), i, 1);
                            foundit = true;
                            break;
                        }
                    }
                    if (!foundit) {
                        model.addRow(new Object[]{name, props.getProperty(name)});
                    }
                }
            }
        }
    }

    private boolean loadEnabled = false;

    @Override
    public boolean isLoadEnabled() {
        return loadEnabled;
    }

    @Override
    public void setLoadEnabled(boolean enable) {
        this.jTextFieldPddlOutputActions.setEnabled(enable);
        this.jButtonLoad.setEnabled(enable);
        loadEnabled = enable;
    }

    @Override
    public void close() throws Exception {
    }

    @Override
    public void accept(DbSetup setup) {
        if (null != pddlActionToCrclGenerator) {
            pddlActionToCrclGenerator.accept(setup);
        }
    }

    private boolean needReplan = false;
    private int replanFromIndex = -1;
    private boolean replanStarted = false;

    @Override
    public void accept(PendantClientJPanel panel, int line) {
        CRCLStatusType status = panel.getStatus();
        CRCLProgramType program = panel.getProgram();
        if (line >= program.getMiddleCommand().size()
                && jCheckBoxReplan.isSelected()
                && !replanStarted) {
            replanStarted = true;
            javax.swing.Timer tmr
                    = new javax.swing.Timer(200, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            try {
                                generateCrcl();
                            } catch (IOException ex) {
                                Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });
            tmr.setRepeats(false);
            tmr.start();
        }
    }
}
