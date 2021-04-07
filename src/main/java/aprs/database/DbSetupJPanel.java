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
package aprs.database;

import aprs.cachedcomponents.CachedCheckBox;
import aprs.cachedcomponents.CachedComboBox;
import aprs.cachedcomponents.CachedTextField;
import aprs.system.AprsSystem;
import aprs.database.vision.VisionToDBJPanel;
import static aprs.misc.AprsCommonLogger.println;
import aprs.misc.Utils;
import java.awt.Component;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EventObject;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import crcl.utils.XFutureVoid;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import static aprs.misc.Utils.autoResizeTableColWidths;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("serial")
public class DbSetupJPanel extends javax.swing.JPanel implements DbSetupPublisher {

     /**
     * Creates new form DbSetupJPanel
     */
    @SuppressWarnings({"initialization","nullness"})
    @UIEffect
    public DbSetupJPanel() {
        this(null);
    }
    /**
     * Creates new form DbSetupJPanel
     * @param aprsSystem1 system panel will be connected to
     */
    @SuppressWarnings({"nullness","initialization"})
    @UIEffect
    public DbSetupJPanel(AprsSystem aprsSystem1) {
        this.aprsSystem = aprsSystem1;
        initComponents();
        editTableArea = new JTextArea();
//        viewAreas = new ArrayList<>();
        setupMultiLineTable(jTableQueries, 1, editTableArea);
        jTextFieldDBLoginTimeout.setText(Integer.toString(DbSetupBuilder.DEFAULT_LOGIN_TIMEOUT));
        jTableQueries.getModel().addTableModelListener(queriesTableModelListener);
        debugCachedCheckBox = new CachedCheckBox(jCheckBoxDebug);
        dbTypeCachedComboBox = new CachedComboBox<>(aprs.database.DbType.class, jComboBoxDbType);
        dbHostCachedTextField = new CachedTextField(jTextFieldDBHost);
        queriesDirectoryCachedTextField = new CachedTextField(jTextFieldQueriesDirectory);
        queriesMap = getQueriesMapInternal();
        queriesDir = getQueriesDirInternal();
        jRadioButtonResourceDir.addActionListener(e -> updateQueriesDirInternal());
        jRadioButtonResourceDir.addItemListener(e -> updateQueriesDirInternal());
        jComboBoxResourceDir.addActionListener(e -> updateQueriesDirInternal());
        jTextFieldQueriesDirectory.addActionListener(e -> updateQueriesDirInternal());
        passwd = jPasswordFieldDBPassword.getPassword();
        jPasswordFieldDBPassword.addActionListener(e -> passwd = jPasswordFieldDBPassword.getPassword());
        resourceDirSelected = jRadioButtonResourceDir.isSelected();
        dbNameCachedTextField = new CachedTextField(jTextFieldDBName);
        dbUserCachedTextField = new CachedTextField(jTextFieldDBUser);
        dbPortCachedTextField = new CachedTextField(jTextFieldDBPort);
        dbLoginTimeoutCachedTextField = new CachedTextField(jTextFieldDBLoginTimeout);
        startScriptCachedTextField = new CachedTextField(jTextFieldStartScript);
        propertiesFilesCachedComboBox = new CachedComboBox<>(String.class, jComboBoxPropertiesFiles);
        resourceDirCachedComboBox = new CachedComboBox<>(String.class, jComboBoxResourceDir);

    }

    @SuppressWarnings({"nullness","initialization"})
    private final TableModelListener queriesTableModelListener = this::handleQueriesTableEvent;

    @UIEffect
    private void handleQueriesTableEvent(TableModelEvent evt) {
        updateQueriesMapInternal();
    }

    private final CachedComboBox<String> resourceDirCachedComboBox;

    private final JTextArea editTableArea;

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @UIEffect
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroupQueryDirType = new javax.swing.ButtonGroup();
        jTextFieldDBPort = new javax.swing.JTextField();
        jComboBoxDbType = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        jButtonConnectDB = new javax.swing.JButton();
        jButtonDisconnectDB = new javax.swing.JButton();
        jPasswordFieldDBPassword = new javax.swing.JPasswordField();
        jLabel3 = new javax.swing.JLabel();
        jTextFieldDBName = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jTextFieldDBUser = new javax.swing.JTextField();
        jTextFieldDBHost = new javax.swing.JTextField();
        jComboBoxPropertiesFiles = new javax.swing.JComboBox<>();
        jButtonBrowse = new javax.swing.JButton();
        jButtonLoad = new javax.swing.JButton();
        jButtonSave = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableQueries = new javax.swing.JTable();
        jRadioButtonResourceDir = new javax.swing.JRadioButton();
        jComboBoxResourceDir = new javax.swing.JComboBox<>();
        jRadioButtonExternDir = new javax.swing.JRadioButton();
        jTextFieldQueriesDirectory = new javax.swing.JTextField();
        jButtonBrowseExternalDirectory = new javax.swing.JButton();
        jButtonLoadExternalDirectory = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextAreaConnectErrors = new javax.swing.JTextArea();
        jCheckBoxDebug = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        jTextFieldDBLoginTimeout = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jTextFieldStartScript = new javax.swing.JTextField();

        jTextFieldDBPort.setText("-99");

        jComboBoxDbType.setModel(getDbTypeComboModel());
        jComboBoxDbType.setSelectedIndex(getDbTypeComboDefaultIndex());
        jComboBoxDbType.setSelectedItem(getDbTypeComboDefaultITem());
        jComboBoxDbType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxDbTypeActionPerformed(evt);
            }
        });

        jLabel4.setText("Database Username: ");

        jButtonConnectDB.setText("Connect To Database");
        jButtonConnectDB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonConnectDBActionPerformed(evt);
            }
        });

        jButtonDisconnectDB.setText("Disconnect From Database");
        jButtonDisconnectDB.setEnabled(false);
        jButtonDisconnectDB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDisconnectDBActionPerformed(evt);
            }
        });

        jPasswordFieldDBPassword.setText("password");

        jLabel3.setText("Database Name: ");

        jTextFieldDBName.setText("neo4j");
        jTextFieldDBName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldDBNameActionPerformed(evt);
            }
        });

        jLabel1.setText("Database Host: ");

        jLabel2.setText("Database Port: ");

        jLabel16.setText("Database Type: ");

        jLabel5.setText("Database Password: ");

        jTextFieldDBUser.setText("neo4j");
        jTextFieldDBUser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldDBUserActionPerformed(evt);
            }
        });

        jTextFieldDBHost.setText("localhost");

        jComboBoxPropertiesFiles.setEditable(true);

        jButtonBrowse.setText("Browse");
        jButtonBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonBrowseActionPerformed(evt);
            }
        });

        jButtonLoad.setText("Load");
        jButtonLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLoadActionPerformed(evt);
            }
        });

        jButtonSave.setText("Save");
        jButtonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSaveActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Queries"));

        jTableQueries.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Type", "Text"
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
        jScrollPane1.setViewportView(jTableQueries);

        buttonGroupQueryDirType.add(jRadioButtonResourceDir);
        jRadioButtonResourceDir.setSelected(true);
        jRadioButtonResourceDir.setText("Resource Directory: ");

        jComboBoxResourceDir.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "none", "neo4j/v2", " " }));
        jComboBoxResourceDir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxResourceDirActionPerformed(evt);
            }
        });

        buttonGroupQueryDirType.add(jRadioButtonExternDir);
        jRadioButtonExternDir.setText("External Directory: ");

        jTextFieldQueriesDirectory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldQueriesDirectoryActionPerformed(evt);
            }
        });

        jButtonBrowseExternalDirectory.setText("Browse");
        jButtonBrowseExternalDirectory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonBrowseExternalDirectoryActionPerformed(evt);
            }
        });

        jButtonLoadExternalDirectory.setText("Load");
        jButtonLoadExternalDirectory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLoadExternalDirectoryActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jRadioButtonResourceDir)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBoxResourceDir, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jRadioButtonExternDir)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldQueriesDirectory, javax.swing.GroupLayout.DEFAULT_SIZE, 135, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonLoadExternalDirectory)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonBrowseExternalDirectory)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 192, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jRadioButtonResourceDir)
                    .addComponent(jComboBoxResourceDir, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jRadioButtonExternDir)
                    .addComponent(jTextFieldQueriesDirectory, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonBrowseExternalDirectory)
                    .addComponent(jButtonLoadExternalDirectory))
                .addContainerGap())
        );

        jTextAreaConnectErrors.setColumns(20);
        jTextAreaConnectErrors.setRows(5);
        jScrollPane2.setViewportView(jTextAreaConnectErrors);

        jCheckBoxDebug.setText("Debug");

        jLabel6.setText("Database Login Timeout: ");

        jTextFieldDBLoginTimeout.setText("5");

        jLabel7.setText("Start Script:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldStartScript))
                    .addComponent(jScrollPane2)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jComboBoxPropertiesFiles, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButtonBrowse)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonLoad)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonSave)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jCheckBoxDebug))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jButtonConnectDB, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel6)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel16, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(12, 12, 12)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jTextFieldDBHost, javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jTextFieldDBPort)
                                    .addComponent(jComboBoxDbType, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jTextFieldDBLoginTimeout))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel3)
                                    .addComponent(jLabel4)
                                    .addComponent(jLabel5))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jTextFieldDBUser)
                                    .addComponent(jPasswordFieldDBPassword)
                                    .addComponent(jTextFieldDBName)))
                            .addComponent(jButtonDisconnectDB, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel16)
                    .addComponent(jComboBoxDbType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(jTextFieldDBName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(5, 5, 5)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel4)
                    .addComponent(jTextFieldDBUser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextFieldDBHost, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextFieldDBPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPasswordFieldDBPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextFieldDBLoginTimeout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonConnectDB)
                    .addComponent(jButtonDisconnectDB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jTextFieldStartScript, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jComboBoxPropertiesFiles, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonBrowse)
                    .addComponent(jButtonLoad)
                    .addComponent(jButtonSave)
                    .addComponent(jCheckBoxDebug))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    @UIEffect
    private void jComboBoxDbTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxDbTypeActionPerformed
        if (!updatingFromDbSetup) {
            notifyAllDbSetupListeners(null);
        }
    }//GEN-LAST:event_jComboBoxDbTypeActionPerformed

    private final AprsSystem aprsSystem;

    /**
     * Get the value of aprsSystemInterface
     *
     * @return the value of aprsSystemInterface
     */
     public AprsSystem getAprsSystem() {
        return aprsSystem;
    }


    @UIEffect
    private void jButtonConnectDBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonConnectDBActionPerformed
        try {
            AprsSystem parentFrame = this.aprsSystem;
            if (null == parentFrame) {
                System.err.println(" this.aprsSystemInterface == null");
                return;
            }
            AprsSystem checkedParentFrame = parentFrame;
            connected = true;
            DbSetup setup = this.getDbSetup();
            final StackTraceElement stackTraceElemArray[] = Thread.currentThread().getStackTrace();
            DbSetupBuilder.connect(setup)
                    .handle(
                            "jButtonConnectDBActionPerformed.handleDbConnect",
                            (Connection c, Throwable e) -> checkedParentFrame.runOnDispatchThread(() -> {
                                if (null != e) {
                                    connected = false;
                                    Logger.getLogger(DbSetupJPanel.class.getName()).log(Level.SEVERE, "", e);
                                    System.err.println("Called from :");
                                    for (StackTraceElement aStackTraceElemArray : stackTraceElemArray) {
                                        System.err.println(aStackTraceElemArray);
                                    }
                                    System.err.println("Exception handled at ");
                                    if (checkedParentFrame.isEnableDebugDumpStacks()) {
                                        Thread.dumpStack();
                                    }
                                    checkedParentFrame.setTitleErrorString("Database error: " + e.toString());
                                }
                                notifyAllDbSetupListeners(null);
                                if (null != c) {
                                    jTextAreaConnectErrors.setText("Connected to database of type " + setup.getDbType() + "\n as user " + setup.getDbUser() + " on host " + setup.getHost() + "\n with port " + setup.getPort() + "\n using queries from " + setup.getQueriesDir());
                                }
                            }));
        } catch (Exception ex) {
            jTextAreaConnectErrors.setText(ex + "\nCaused by :\n" + ex.getCause());
            Logger.getLogger(DbSetupJPanel.class.getName()).log(Level.SEVERE, "", ex);
            connected = false;
        }
    }//GEN-LAST:event_jButtonConnectDBActionPerformed

    @UIEffect
    private void jButtonDisconnectDBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDisconnectDBActionPerformed
        connected = false;
        setDbSetup(getDbSetup())
                .thenRun(() -> {
                    aprsSystem.runOnDispatchThread(() -> {
                        notifyAllDbSetupListeners(null);
                        println("Disconnected from database.");
                        jTextAreaConnectErrors.setText("Disconnected from database.");
                    });
                });
    }//GEN-LAST:event_jButtonDisconnectDBActionPerformed

    @UIEffect
    private void jTextFieldDBNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldDBNameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldDBNameActionPerformed

    @UIEffect
    private void jTextFieldDBUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldDBUserActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldDBUserActionPerformed

    @UIEffect
    private void jButtonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSaveActionPerformed
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter txtExtensionFilter = new FileNameExtensionFilter("txt", "txt");
        chooser.addChoosableFileFilter(txtExtensionFilter);
        chooser.setFileFilter(txtExtensionFilter);
        if (null == propertiesFile) {
            throw new IllegalStateException("null == propertiesFile");
        }
        File parentFile = propertiesFile.getParentFile();
        if (null != parentFile) {
            chooser.setCurrentDirectory(parentFile);
        }
        chooser.setSelectedFile(propertiesFile);
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            this.setPropertiesFile(chooser.getSelectedFile());
        }
        DbSetupBuilder.savePropertiesFile(propertiesFile, getDbSetup());
        this.notifyAllDbSetupListeners(null);
    }//GEN-LAST:event_jButtonSaveActionPerformed

    @UIEffect
    private void jButtonLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLoadActionPerformed
        if (null == propertiesFile) {
            throw new IllegalStateException("null == propertiesFile");
        }
        this.setPropertiesFile(new File(jComboBoxPropertiesFiles.getSelectedItem().toString()));
        DbSetup newSetup = DbSetupBuilder.loadFromPropertiesFile(propertiesFile).build();
        this.setDbSetup(newSetup);
        this.notifyAllDbSetupListeners(null);
    }//GEN-LAST:event_jButtonLoadActionPerformed

    @UIEffect
    private void jButtonBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonBrowseActionPerformed
        if (null == propertiesFile) {
            throw new IllegalStateException("null == propertiesFile");
        }
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter txtExtensionFilter = new FileNameExtensionFilter("txt", "txt");
        chooser.addChoosableFileFilter(txtExtensionFilter);
        chooser.setFileFilter(txtExtensionFilter);
        File parentFile = propertiesFile.getParentFile();
        if (null != parentFile) {
            chooser.setCurrentDirectory(parentFile);
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            this.setPropertiesFile(chooser.getSelectedFile());
        }
    }//GEN-LAST:event_jButtonBrowseActionPerformed

     private volatile @MonotonicNonNull  String lastResourceDirSet = null;

    @UIEffect
    private void jComboBoxResourceDirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxResourceDirActionPerformed
        try {
            Object item = this.jComboBoxResourceDir.getSelectedItem();
            if (null != item && !item.equals(lastResourceDirSet)) {
                String resDirSuffix = item.toString();
                if (null != resDirSuffix && !"none".equals(resDirSuffix)) {
                    this.lastResourceDirSet = resDirSuffix;
                    resourceDirCachedComboBox.setSelectedItem(resDirSuffix);
                    updateResDirSuffixOnDisplay(resDirSuffix);
                    if (!updatingFromDbSetup) {
                        notifyAllDbSetupListeners(null);
                    }
                }
            }
        } catch (IOException iOException) {
            Logger.getLogger(DbSetupJPanel.class.getName()).log(Level.SEVERE, "", iOException);
        }
    }//GEN-LAST:event_jComboBoxResourceDirActionPerformed

    private XFutureVoid updateResDirSuffix(String resDirSuffix) throws IOException {
        if (resDirSuffix == null) {
            resDirSuffix = "neo4j/v1/";
        }
        if (!resDirSuffix.startsWith("/")) {
            resDirSuffix = "/" + resDirSuffix;
        }
        if (!resDirSuffix.endsWith("/")) {
            resDirSuffix = resDirSuffix + "/";
        }
        String resDir = RESOURCE_BASE + resDirSuffix;
        queriesMap
                = DbSetupBuilder.readResourceQueriesDirectory(resDir);
        return loadQueriesMap(queriesMap, false, resDir);
    }

    @UIEffect
    private void updateResDirSuffixOnDisplay(String resDirSuffix) throws IOException {
        if (resDirSuffix == null) {
            resDirSuffix = "neo4j/v1/";
        }
        if (!resDirSuffix.startsWith("/")) {
            resDirSuffix = "/" + resDirSuffix;
        }
        if (!resDirSuffix.endsWith("/")) {
            resDirSuffix = resDirSuffix + "/";
        }
        String resDir = RESOURCE_BASE + resDirSuffix;
        queriesMap
                = DbSetupBuilder.readResourceQueriesDirectory(resDir);
        loadQueriesMapOnDisplay(queriesMap, false, resDir);
    }

    @UIEffect
    private void jButtonBrowseExternalDirectoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonBrowseExternalDirectoryActionPerformed
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            loadExternalQueriesDirectory(f);
        }
    }//GEN-LAST:event_jButtonBrowseExternalDirectoryActionPerformed

    @UIEffect
    private void jButtonLoadExternalDirectoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLoadExternalDirectoryActionPerformed
        loadExternalQueriesDirectory(new File(jTextFieldQueriesDirectory.getText()));
    }//GEN-LAST:event_jButtonLoadExternalDirectoryActionPerformed

    @UIEffect
    private void jTextFieldQueriesDirectoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldQueriesDirectoryActionPerformed
        loadExternalQueriesDirectory(new File(jTextFieldQueriesDirectory.getText()));
    }//GEN-LAST:event_jTextFieldQueriesDirectoryActionPerformed

    private XFutureVoid loadExternalQueriesDirectory(File f) {
        try {
            String filename = f.getCanonicalPath();
            queriesMap
                    = DbSetupBuilder.readQueriesDirectory(f.getAbsolutePath());
            return loadQueriesMap(queriesMap, true, filename);
        } catch (IOException ex) {
            Logger.getLogger(DbSetupJPanel.class.getName()).log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }

    private boolean connected = false;
    private volatile boolean updatingFromDbSetup = false;
    private final CachedCheckBox debugCachedCheckBox;
    private final CachedComboBox<aprs.database.DbType> dbTypeCachedComboBox;
    private final CachedTextField dbHostCachedTextField;
    private final CachedTextField queriesDirectoryCachedTextField;

    /**
     * Display options according to the given setup object.
     *
     * @param setup object to read properties from
     * @return future for determining when setup is complete
     */
    @SuppressWarnings("WeakerAccess")
    @Override
    public XFutureVoid setDbSetup(DbSetup setup) {
        List<XFutureVoid> localFutures = new ArrayList<>();
        try {
            if (null == setup) {
                throw new IllegalArgumentException("setup == null");
            }
            if (setup.isConnected() && (null == setup.getDbType() || setup.getDbType() == DbType.NONE)) {
                this.connected = false;
                throw new IllegalArgumentException("setup.getDbType() == " + setup.getDbType());
            }
            localFutures.add(debugCachedCheckBox.setSelected(setup.isDebug()));
            updatingFromDbSetup = true;
            DbType dbtype = setup.getDbType();
            if (!Objects.equals(dbtype, dbTypeCachedComboBox.getSelectedItem())) {
                localFutures.add(dbTypeCachedComboBox.setSelectedItem(dbtype));
            }
            String host = setup.getHost();
            if (!Objects.equals(host, dbHostCachedTextField.getText())) {
                localFutures.add(dbHostCachedTextField.setText(setup.getHost()));
            }
            int port = setup.getPort();
            localFutures.add(dbPortCachedTextField.setText(Integer.toString(port)));
            int loginTimeout = setup.getLoginTimeout();
            localFutures.add(setLoginTimeout(loginTimeout));
            char newpasswd[] = setup.getDbPassword();
            localFutures.add(setDbPassword(newpasswd));
            String user = setup.getDbUser();
            localFutures.add(dbUserCachedTextField.setText(user));
            String dbname = setup.getDbName();
            localFutures.add(dbNameCachedTextField.setText(dbname));
            boolean newConnectedState = setup.isConnected();
            this.connected = newConnectedState;

            localFutures.add(setDbConnectedState(newConnectedState));
            boolean internal = setup.isInternalQueriesResourceDir();
            localFutures.add(setInternalQueriesDir(internal));
            String queryDir = setup.getQueriesDir();
            boolean queriesMapReloaded = false;
            if (null != queryDir) {
                if (internal) {
                    lastResourceDirSet = queryDir;
                    localFutures.add(resourceDirCachedComboBox.setSelectedItem(queryDir));
                    updateResDirSuffix(queryDir);
                    localFutures.add(updateQueriesDir());
                    queriesMapReloaded = true;
                } else if (!Objects.equals(queryDir, queriesDirectoryCachedTextField.getText())) {
                    localFutures.add(loadExternalQueriesDirectory(new File(queryDir)));
                    queriesMapReloaded = true;
                }
            }
            String startScript = setup.getStartScript();
            if (startScript != null && startScript.length() > 0) {
                localFutures.add(startScriptCachedTextField.setText(startScript));
            }
            if (!queriesMapReloaded) {
                Map<DbQueryEnum, DbQueryInfo> localQueriesMap = setup.getQueriesMap();
                if (null != localQueriesMap) {
                    localFutures.add(loadQueriesMap(localQueriesMap, false, ""));
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(DbSetupJPanel.class.getName()).log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        } finally {
            updatingFromDbSetup = false;
        }
        return XFutureVoid.allOf(localFutures);
    }

    @SafeEffect
    private XFutureVoid setInternalQueriesDir(boolean internal) {
        resourceDirSelected = internal;
        return aprsSystem.runOnDispatchThread(() -> setInternalQueriesDirOnDisplay(internal));
    }

    @UIEffect
    private void setInternalQueriesDirOnDisplay(boolean internal) {
        if (internal != this.jRadioButtonResourceDir.isSelected()) {
            jRadioButtonResourceDir.setSelected(internal);
        }
        if (internal == this.jRadioButtonExternDir.isSelected()) {
            jRadioButtonExternDir.setSelected(!internal);
        }
    }

    @SafeEffect
    private XFutureVoid setDbConnectedState(boolean newConnectedState) {
        return aprsSystem.runOnDispatchThread(() -> setDbConnectedStateOnDisplay(newConnectedState));
    }

    @UIEffect
    private void setDbConnectedStateOnDisplay(boolean newConnectedState) {
        //noinspection DoubleNegation
        if (jButtonConnectDB.isEnabled() != (!newConnectedState)) {
            this.jButtonConnectDB.setEnabled(!newConnectedState);
        }
        if (jButtonDisconnectDB.isEnabled() != newConnectedState) {
            this.jButtonDisconnectDB.setEnabled(newConnectedState);
        }
    }

    @SafeEffect
    private synchronized XFutureVoid setDbPassword(char[] newpasswd) {
        passwd = newpasswd;
        return aprsSystem.runOnDispatchThread(() -> setDbPasswordOnDisplay(newpasswd));
    }

    @UIEffect
    private void setDbPasswordOnDisplay(char[] newpasswd) {
        char curpasswd[] = jPasswordFieldDBPassword.getPassword();
        if (!Arrays.equals(curpasswd, newpasswd)) {
            this.jPasswordFieldDBPassword.setText(new String(newpasswd));
        }
    }

    @SafeEffect
    private XFutureVoid setLoginTimeout(int loginTimeout) {
        return aprsSystem.runOnDispatchThread(() -> setLoginTimeoutOnDisplay(loginTimeout));
    }

    @UIEffect
    private void setLoginTimeoutOnDisplay(int loginTimeout) {
        int curLoginTimeout = -99;
        try {
            curLoginTimeout = Integer.parseInt(jTextFieldDBLoginTimeout.getText());
        } catch (Exception e) {
            // deliberately ignored
        }
        if (curLoginTimeout != loginTimeout) {
            this.jTextFieldDBLoginTimeout.setText(Integer.toString(loginTimeout));
        }
    }

    @UIEffect
    private void loadQueriesMapOnDisplay(Map<DbQueryEnum, DbQueryInfo> queriesMap, boolean externDir, String filename) {
        if (!queriesDirectoryCachedTextField.getText().equals(filename)) {
            queriesDirectoryCachedTextField.setText(filename);
        }
        jRadioButtonExternDir.setSelected(externDir);
        DefaultTableModel model = (DefaultTableModel) jTableQueries.getModel();
        model.setRowCount(0);
        for (Map.Entry<DbQueryEnum, DbQueryInfo> entry : queriesMap.entrySet()) {
            model.addRow(new Object[]{entry.getKey(), entry.getValue().getOrigText()});
        }
        autoResizeTableColWidths(jTableQueries);
        autoResizeTableRowHeights(jTableQueries);
    }

    private XFutureVoid loadQueriesMap(Map<DbQueryEnum, DbQueryInfo> queriesMap, boolean externDir, String filename) {
        return aprsSystem.runOnDispatchThread(() -> loadQueriesMapOnDisplay(queriesMap, externDir, filename));
    }

    @UIEffect
    @SuppressWarnings("guieffect")
    private void setupMultiLineTable(JTable jTable,
            int multiLineColumnIndex,
            JTextArea editTableArea) {
        jTable.getColumnModel().getColumn(multiLineColumnIndex).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            @UIEffect
            public Component getTableCellRendererComponent(JTable table, @Nullable Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JTextArea area = new JTextArea();
                area.setOpaque(true);
                area.setVisible(true);
                area.setText(Objects.toString(value));
                return area;
            }

        });
        jTable.getColumnModel().getColumn(multiLineColumnIndex).setCellEditor(new TableCellEditor() {

            private final List<CellEditorListener> listeners = new ArrayList<>();

            @Override
            @UIEffect
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                editTableArea.setOpaque(true);
                editTableArea.setVisible(true);
                editTableArea.setText(value.toString());
                return editTableArea;
            }

            @Override
            @UIEffect
            public Object getCellEditorValue() {
                return editTableArea.getText();
            }

            @Override
            @UIEffect
            public boolean isCellEditable(EventObject anEvent) {
                return true;
            }

            @Override
            public boolean shouldSelectCell(EventObject anEvent) {
                return true;
            }

            @Override
            @UIEffect
            public boolean stopCellEditing() {
                for (CellEditorListener l : listeners) {
                    if (null != l) {
                        l.editingStopped(new ChangeEvent(jTable));
                    }
                }
                return true;
            }

            @Override
            @UIEffect
            public void cancelCellEditing() {
                for (CellEditorListener l : listeners) {
                    if (null != l) {
                        l.editingCanceled(new ChangeEvent(jTable));
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
    }

    private volatile Map<DbQueryEnum, DbQueryInfo> queriesMap;

    private volatile boolean disconnecting = false;

    @Override
    public XFutureVoid disconnect() {
        boolean was_disconnecting = disconnecting;
        disconnecting = true;
        if(!this.connected) {
            return XFutureVoid.completedFuture();
        }
        return setDbSetup(new DbSetupBuilder().setup(this.getDbSetup()).connected(false).build())
                .thenRun(() -> disconnecting = was_disconnecting);
    }

    private Map<DbQueryEnum, DbQueryInfo> getQueriesMap() {
        return queriesMap;
    }

//        if (disconnecting) {
//            if (queriesMap == null) {
//                return Collections.emptyMap();
//            }
//            return queriesMap;
//        }
//        if (null != queriesMap && mapUpToDate) {
//            return queriesMap;
//        }
//        mapUpToDate = false;
//        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
//            queriesMap = getQueriesMapInternal();
//            mapUpToDate = true;
//        } else {
//            try {
//                javax.swing.SwingUtilities.invokeAndWait(() -> {
//                    if (null != queriesMap && mapUpToDate) {
//                        return;
//                    }
//                    queriesMap = getQueriesMapInternal();
//                    mapUpToDate = true;
//                });
//            } catch (InterruptedException | InvocationTargetException ex) {
//                if (!disconnecting) {
//                    Logger.getLogger(DbSetupJPanel.class.getName()).log(Level.SEVERE, "", ex);
//                    throw new RuntimeException(ex);
//                }
//            }
//        }
//        Map<DbQueryEnum, DbQueryInfo> ret = queriesMap;
//        if (null == ret) {
//            throw new IllegalStateException("couldn't initialize query to info map: map == null");
//        }
//        return ret;
//    }
    private XFutureVoid updateQueriesMap() {
        return aprsSystem.runOnDispatchThread(this::updateQueriesMapInternal);
    }

    @UIEffect
    private void updateQueriesMapInternal() {
        queriesMap = getQueriesMapInternal();
    }

    @UIEffect
    private synchronized Map<DbQueryEnum, DbQueryInfo> getQueriesMapInternal() {
        Map<DbQueryEnum, DbQueryInfo> newMap = new EnumMap<>(DbQueryEnum.class);
        DefaultTableModel model = (DefaultTableModel) jTableQueries.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            Object keyObject = model.getValueAt(i, 0);
            if (null == keyObject) {
                throw new IllegalStateException("Null keyObject in table on row " + i);
            }
            if (!(keyObject instanceof DbQueryEnum)) {
                throw new IllegalStateException("Bad keyObject in table on row " + i);
            }
            Object queryObject = model.getValueAt(i, 1);
            if (null == queryObject) {
                throw new IllegalStateException("Null queryObject in table on row " + i);
            }
            newMap.put((DbQueryEnum) keyObject,
                    DbQueryInfo.parse(Objects.toString(queryObject),"jTableQueries.getValueAt("+i+",1)"));
        }
        return newMap;
    }

    private volatile String queriesDir;

    /**
     * Get the name of the directory where query info text files were read from
     * or should be read from.
     *
     * @return queries directory
     */
    private String getQueriesDir() {
        return queriesDir;
    }

    @SafeEffect
    private XFutureVoid updateQueriesDir() {
        return aprsSystem.runOnDispatchThread(this::updateQueriesDirInternal);
    }

    @UIEffect
    private void updateQueriesDirInternal() {
        queriesDir = getQueriesDirInternal();
        resourceDirSelected = jRadioButtonResourceDir.isSelected();
    }
    private volatile boolean resourceDirSelected;

    @UIEffect
    private String getQueriesDirInternal() {
        if (jRadioButtonResourceDir.isSelected()) {
            return jComboBoxResourceDir.getSelectedItem().toString();
        }
        return queriesDirectoryCachedTextField.getText();
    }

    private static int parseIntOr(String string, int defaultValue) {
        try {
            return Integer.parseInt(string);
        } catch (Exception exception) {
            Logger.getLogger(DbSetupJPanel.class
                    .getName()).log(Level.SEVERE, "", exception);
        }
        return defaultValue;
    }

    private volatile char[] passwd;

    private final CachedTextField dbNameCachedTextField;
    private final CachedTextField dbUserCachedTextField;
    private final CachedTextField dbPortCachedTextField;
    private final CachedTextField dbLoginTimeoutCachedTextField;
    private final CachedTextField startScriptCachedTextField;

    /**
     * Get the user's currently chosen/entered options in a setup object.
     *
     * @return setup
     */
    @SuppressWarnings("WeakerAccess")
    @Override
    public DbSetup getDbSetup() {
        DbType dbtype = dbTypeCachedComboBox.getSelectedItem();
        if (null == dbtype || dbtype == DbType.NONE) {
            connected = false;
        }
        return new DbSetupBuilder()
                .connected(connected)
                .type(dbTypeCachedComboBox.getSelectedItem())
                .host(dbHostCachedTextField.getText())
                .passwd(passwd)
                .dbname(dbNameCachedTextField.getText())
                .user(dbUserCachedTextField.getText())
                .port(Integer.parseInt(dbPortCachedTextField.getText()))
                .loginTimeout(parseIntOr(dbLoginTimeoutCachedTextField.getText(), 5))
                .queriesMap(getQueriesMap())
                .internalQueriesResourceDir(resourceDirSelected)
                .queriesDir(getQueriesDir())
                .debug(debugCachedCheckBox.isSelected())
                .startScript(startScriptCachedTextField.getText())
                .build();
    }

    private volatile @Nullable List<XFutureVoid> futures = null;

    /**
     * Notify all dbSetupListeners of the new setup.
     *
     * @param notifyService optional service where listeners are called.
     * @return list of futures for determining when all the listeners have been
     * notified.
     */
    @SuppressWarnings("WeakerAccess")
    @Override
    public List<XFutureVoid> notifyAllDbSetupListeners(@Nullable ExecutorService notifyService) {
        boolean cancelWarnGiven = false;
        AprsSystem parentFrame = this.aprsSystem;
        if (null == parentFrame) {
            throw new IllegalStateException("this.aprsSystemInterface == null");
        }
        AprsSystem checkedParentFrame = parentFrame;
        List<XFutureVoid> origFutures = this.futures;
        if (null != origFutures) {
            for (Future<?> f : origFutures) {
                if (!f.isDone() && !f.isCancelled()) {
                    if (!cancelWarnGiven && !parentFrame.isClosing()) {
                        cancelWarnGiven = true;
                        System.err.println("Cancelling a dbSetup notification");
                        if (checkedParentFrame.isEnableDebugDumpStacks()) {
                            Thread.dumpStack();
                        }
                    }
                    f.cancel(false);
                }
            }
        }
        if (parentFrame.isClosing()) {
            return Collections.emptyList();
        }
        List<XFutureVoid> newFutures = new ArrayList<>();
        this.futures = newFutures;
        final DbSetup thisDbSetup = DbSetupJPanel.this.getDbSetup();
        if (notifyService != null) {
            XFutureVoid future
                    = XFutureVoid.runAsync("broadcastDbSetup",
                            () -> {
                                broadcastDbSetup(thisDbSetup);
                            },
                            notifyService);
            newFutures.add(future);
            return newFutures;
        } else {
            broadcastDbSetup(thisDbSetup);
            return Collections.emptyList();
        }
    }

    private synchronized void broadcastDbSetup(DbSetup thisDbSetup) {
        for (DbSetupListener listener : dbSetupListeners) {
            if (null != listener) {
                listener.accept(thisDbSetup);
            }
        }
    }

    @SafeEffect
    private XFutureVoid addComboItemUnique(String item) {
        return aprsSystem.runOnDispatchThread(() -> addComboItemUniqueOnDisplay(item));
    }

    @UIEffect
    private void addComboItemUniqueOnDisplay(String item) {
        for (int i = 0; i < propertiesFilesCachedComboBox.getItemCount(); i++) {
            if (Objects.equals(propertiesFilesCachedComboBox.getItemAt(i), item)) {
                return;
            }
        }
        propertiesFilesCachedComboBox.addItem(item);
    }

    /**
     * Load the most recent settings file.
     */
    @UIEffect
    public void loadRecentSettings() {
        try {
            if (null != recentSettingsFile && recentSettingsFile.exists()) {
                TreeSet<String> set = new TreeSet<>();
                try (BufferedReader br = new BufferedReader(new FileReader(recentSettingsFile))) {
                    String line;
                    while (null != (line = br.readLine())) {
                        set.add(line.trim());
                    }
                }
                propertiesFilesCachedComboBox.removeAllItems();
                List<File> files = set.stream()
                        .map(File::new)
                        .filter(File::exists)
                        .sorted(Comparator.comparing(File::lastModified).reversed())
                        .limit(3)
                        .collect(Collectors.toList());
                try (PrintWriter pw = new PrintWriter(new FileWriter(recentSettingsFile))) {
                    for (File f : files) {
                        String p = f.getCanonicalPath();
                        pw.println(p);
                        addComboItemUniqueOnDisplay(p);
                    }
                }
            }
        } catch (IOException iOException) {
            Logger.getLogger(DbSetupJPanel.class.getName()).log(Level.SEVERE, "", iOException);
        }
    }
    private final File recentSettingsFile = new File(Utils.getAprsUserHomeDir(), ".dbsetup_recent.txt");
    private @MonotonicNonNull File propertiesFile = null;

    private final CachedComboBox<String> propertiesFilesCachedComboBox;

    public void setPropertiesFile(File f) {
        try {
            propertiesFile = f;
            String newPath = f.getCanonicalPath();
            addComboItemUnique(newPath);
            propertiesFilesCachedComboBox.setSelectedItem(newPath);
            saveRecent(newPath);
        } catch (IOException iOException) {
            Logger.getLogger(DbSetupJPanel.class.getName()).log(Level.SEVERE, "", iOException);
        }
    }

    private void saveRecent(String newPath) throws IOException {
        File f = recentSettingsFile;
        if (null != f) {
            File parentFile = f.getParentFile();
            if (null != parentFile) {
                parentFile.mkdirs();
            }
            try (PrintWriter fw = new PrintWriter(new FileWriter(f, true))) {
                fw.println(newPath);
            }
        }
    }

    /**
     * Get the properties file.
     *
     * @return properties file
     */
     public  @Nullable  File getPropertiesFile() {
        return propertiesFile;
    }
    private volatile boolean savingProperties = false;

    public void saveProperties() {
        DbSetup setup = this.getDbSetup();
        saveProperties(setup.getDbType(), setup.getHost(), setup.getPort());
    }

    private void saveProperties(DbType dbtype, String host, int port) {
        if (null == propertiesFile) {
            throw new IllegalStateException("null == propertiesFile");
        }
        try {
            savingProperties = true;
            File parentFile = propertiesFile.getParentFile();
            if (null != parentFile) {
                parentFile.mkdirs();
            }
            Properties props = new Properties();
            if (propertiesFile.exists()) {
                try (FileReader fr = new FileReader(propertiesFile)) {
                    props.load(fr);
                } catch (IOException ex) {
                    Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
                }
            }
            props.putAll(updateArgsMap(dbtype));
            DbSetup setup = this.getDbSetup();
            if (host == null) {
                host = setup.getHost();
            }
            if (port < 1) {
                port = setup.getPort();
            }
            props.put(dbtype + ".host", host);
            props.put(dbtype + "." + host + ".port", Integer.toString(port));
            String dbHostPort = String.format("%s.%s_%d", dbtype.toString(), host, port);
            props.put(dbHostPort + ".name", setup.getDbName());
            props.put(dbHostPort + ".user", setup.getDbUser());
            props.put(dbHostPort + ".passwd", new String(setup.getDbPassword()));
            props.put("useQueriesResource", resourceDirSelected);
            String resDir = resourceDirCachedComboBox.getSelectedItem();
            if (null != resDir) {
                props.put("resDir", resDir);
            }
            props.put("queryDir", queriesDirectoryCachedTextField.getText());
            props.put("startScript", startScriptCachedTextField.getText());
            Utils.saveProperties(propertiesFile, props);
        } finally {
            savingProperties = false;
        }
    }

    private volatile boolean restoringProperties = false;

    public Map<String, String> updateArgsMap() {
        return updateArgsMap(this.getDbSetup().getDbType());
    }

    private final Map<String, String> argsMap = DbSetupBuilder.getDefaultArgsMap();

    private Map<String, String> updateArgsMap(DbType dbtype) {
        DbSetup curSetup = this.getDbSetup();
        argsMap.put("--dbhost", curSetup.getHost());
        argsMap.put("--dbport", Integer.toString(curSetup.getPort()));
        argsMap.put("--dbname", curSetup.getDbName());
        argsMap.put("--dbuser", curSetup.getDbUser());
        argsMap.put("--dbpasswd",
                new String(curSetup.getDbPassword()));
        argsMap.put("--dbtype", curSetup.getDbType().toString());
        return argsMap;
    }

    private final String RESOURCE_BASE = "aprs/database";

    @UIEffect
    private void autoResizeTableRowHeights(JTable table) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        for (int rowIndex = 0; rowIndex < table.getRowCount(); rowIndex++) {
            int height = 0;
            for (int colIndex = 0; colIndex < table.getColumnCount(); colIndex++) {
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

    /**
     * Get name of a script file to execute to start the database server if
     * available
     *
     * @return name of start script file
     */
    public String getStartScript() {
        return startScriptCachedTextField.getText();
    }

//    public final XFutureVoid loadProperties() {
//        try {
//            if (null != propertiesFile && propertiesFile.exists()) {
//                restoringProperties = true;
//                Properties props = new Properties();
//                try (FileReader fr = new FileReader(propertiesFile)) {
//                    props.load(fr);
//                } catch (IOException ex) {
//                    Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
//                }
//                for (String propName : props.stringPropertyNames()) {
//                    String propValue = props.getProperty(propName);
//                    if (null != propValue) {
//                        argsMap.put(propName, propValue);
//                    }
//                }
//                return updateFromArgs(argsMap)
//                        .thenRun(() -> restoringProperties = false);
//            } else {
//                return XFutureVoid.completedFuture();
//            }
//        } catch (Exception e) {
//            addLogMessage(e);
//            throw new RuntimeException(e);
//        }
//    }
    private final Set<DbSetupListener> dbSetupListeners = new HashSet<>();

    @Override
    public void addDbSetupListener(DbSetupListener listener) {
        dbSetupListeners.add(listener);
    }

    @Override
    public void removeDbSetupListener(DbSetupListener listener) {
        dbSetupListeners.remove(listener);
    }

    @Override
    public void removeAllDbSetupListeners() {
        dbSetupListeners.clear();
    }

    private final DefaultComboBoxModel<DbType> dbTypeComboModel = new DefaultComboBoxModel<>(DbType.values());

    private DefaultComboBoxModel<DbType> getDbTypeComboModel() {
        return dbTypeComboModel;
    }

    private DbType getDbTypeComboDefaultITem() {
        return DbType.NEO4J;
    }

    private int getDbTypeComboDefaultIndex() {
        return DbType.NEO4J.ordinal();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroupQueryDirType;
    private javax.swing.JButton jButtonBrowse;
    private javax.swing.JButton jButtonBrowseExternalDirectory;
    private javax.swing.JButton jButtonConnectDB;
    private javax.swing.JButton jButtonDisconnectDB;
    private javax.swing.JButton jButtonLoad;
    private javax.swing.JButton jButtonLoadExternalDirectory;
    private javax.swing.JButton jButtonSave;
    private javax.swing.JCheckBox jCheckBoxDebug;
    private javax.swing.JComboBox<aprs.database.DbType> jComboBoxDbType;
    private javax.swing.JComboBox<String> jComboBoxPropertiesFiles;
    private javax.swing.JComboBox<String> jComboBoxResourceDir;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPasswordField jPasswordFieldDBPassword;
    private javax.swing.JRadioButton jRadioButtonExternDir;
    private javax.swing.JRadioButton jRadioButtonResourceDir;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTableQueries;
    private javax.swing.JTextArea jTextAreaConnectErrors;
    private javax.swing.JTextField jTextFieldDBHost;
    private javax.swing.JTextField jTextFieldDBLoginTimeout;
    private javax.swing.JTextField jTextFieldDBName;
    private javax.swing.JTextField jTextFieldDBPort;
    private javax.swing.JTextField jTextFieldDBUser;
    private javax.swing.JTextField jTextFieldQueriesDirectory;
    private javax.swing.JTextField jTextFieldStartScript;
    // End of variables declaration//GEN-END:variables
}
