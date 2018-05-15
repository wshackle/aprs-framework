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
package aprs.framework.database;

import aprs.framework.system.AprsSystemInterface;
import aprs.framework.spvision.VisionToDBJPanel;
import aprs.framework.DisplayInterface;
import aprs.framework.Utils;
import static aprs.framework.Utils.autoResizeTableColWidths;
import java.awt.Component;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
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
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class DbSetupJPanel extends javax.swing.JPanel implements DbSetupPublisher, DisplayInterface {

    /**
     * Creates new form DbSetupJPanel
     */
    @SuppressWarnings("initialization")
    public DbSetupJPanel() {
        initComponents();
        editTableArea = new JTextArea();
//        viewAreas = new ArrayList<>();
        setupMultiLineTable(jTableQueries, 1, editTableArea);
        jTextFieldDBLoginTimeout.setText(Integer.toString(DbSetupBuilder.DEFAULT_LOGIN_TIMEOUT));
        ((DefaultTableModel) jTableQueries.getModel()).addTableModelListener(queriesTableModelListener);
    }

    @SuppressWarnings("initialization")
    private final TableModelListener queriesTableModelListener = this::setMapUpToDate;

    private void setMapUpToDate(TableModelEvent evt) {
        mapUpToDate = false;
    }

//    private static List<String> getClasspathEntriesByPath(String path) throws IOException {
//        InputStream is = DbMain.class.getClassLoader().getResourceAsStream(path);
//        if(null == is) {
//            return null;
//        }
//        StringBuilder sb = new StringBuilder();
//        while (is.available() > 0) {
//            byte[] buffer = new byte[1024];
//            sb.append(new String(buffer, Charset.defaultCharset()));
//        }
//
//        return Arrays
//                .asList(sb.toString().split("\n")) // Convert StringBuilder to individual lines
//                .stream() // Stream the list
//                .filter(line -> line.trim().length() > 0) // Filter out empty lines
//                .collect(Collectors.toList());              // Collect remaining lines into a List again
//    }
    private JTextArea editTableArea;
//    private List<JTextArea> viewAreas;

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
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

        jComboBoxResourceDir.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "none", "mysql", "mysql_simple", "neo4j/v1", "neo4j/v2", " " }));
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

    private void updateSettingsFileName() {
        if (null != propertiesFile && propertiesFile.exists()) {
            return;
        }
        String settingsFileStart = jComboBoxDbType.getSelectedItem().toString();
        if (!propertiesFile.getName().startsWith(settingsFileStart)) {
            if (Objects.toString(jComboBoxPropertiesFiles.getSelectedItem()).startsWith(settingsFileStart)) {
                return;
            }
            for (int i = 0; i < jComboBoxPropertiesFiles.getItemCount(); i++) {
                String fname = Objects.toString(jComboBoxPropertiesFiles.getItemAt(i));
                if (fname.startsWith(settingsFileStart)) {
                    jComboBoxPropertiesFiles.setSelectedIndex(i);
                    return;
                }
            }
        }
        setPropertiesFile(new File(propertiesFile.getParentFile(), settingsFileStart + ".dbsettings.txt"));
    }


    private void jComboBoxDbTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxDbTypeActionPerformed
        updateSettingsFileName();
        if (!updatingFromDbSetup) {
            notifyAllDbSetupListeners();
        }
    }//GEN-LAST:event_jComboBoxDbTypeActionPerformed

    @MonotonicNonNull
    private AprsSystemInterface aprsJFrame = null;

    /**
     * Get the value of aprsJFrame
     *
     * @return the value of aprsJFrame
     */
    @Nullable
    public AprsSystemInterface getAprsSystemInterface() {
        return aprsJFrame;
    }

    /**
     * Set the value of aprsJFrame
     *
     * @param aprsJFrame new value of aprsJFrame
     */
    public void setAprsSystemInterface(AprsSystemInterface aprsJFrame) {
        this.aprsJFrame = aprsJFrame;
    }

    private void jButtonConnectDBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonConnectDBActionPerformed
        try {
            AprsSystemInterface parentFrame = this.aprsJFrame;
            if (null == parentFrame) {
                System.err.println(" this.aprsJFrame == null");
                return;
            }
            AprsSystemInterface checkedParentFrame = parentFrame;
            connected = true;
            DbSetup setup = this.getDbSetup();
            final StackTraceElement stackTraceElemArray[] = Thread.currentThread().getStackTrace();
            DbSetupBuilder.connect(setup)
                    .handle(
                            "jButtonConnectDBActionPerformed.handleDbConnect",
                            (Connection c, Throwable e) -> Utils.runOnDispatchThread(() -> {
                                if (null != e) {
                                    connected = false;
                                    Logger.getLogger(DbSetupJPanel.class.getName()).log(Level.SEVERE, null, e);
                                    System.err.println("Called from :");
                                    for (int i = 0; i < stackTraceElemArray.length; i++) {
                                        System.err.println(stackTraceElemArray[i]);
                                    }
                                    System.err.println("Exception handled at ");
                                    if (checkedParentFrame.isEnableDebugDumpstacks()) {
                                        Thread.dumpStack();
                                    }
                                    checkedParentFrame.setTitleErrorString("Database error: " + e.toString());
                                }
                                notifyAllDbSetupListeners();
                                if (null != c) {
                                    jTextAreaConnectErrors.setText("Connected to database of type " + setup.getDbType() + "\n as user " + setup.getDbUser() + " on host " + setup.getHost() + "\n with port " + setup.getPort() + "\n using queries from " + setup.getQueriesDir());
                                }
                            }));
        } catch (Exception ex) {
            jTextAreaConnectErrors.setText(ex + "\nCaused by :\n" + ex.getCause());
            Logger.getLogger(DbSetupJPanel.class.getName()).log(Level.SEVERE, null, ex);
            connected = false;
        }
    }//GEN-LAST:event_jButtonConnectDBActionPerformed

    private void jButtonDisconnectDBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDisconnectDBActionPerformed
//        this.closeDB();
        connected = false;
        setDbSetup(getDbSetup());
        notifyAllDbSetupListeners();
        System.out.println("Disconnected from database.");
        jTextAreaConnectErrors.setText("Disconnected from database.");
    }//GEN-LAST:event_jButtonDisconnectDBActionPerformed

    private void jTextFieldDBNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldDBNameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldDBNameActionPerformed

    private void jTextFieldDBUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldDBUserActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldDBUserActionPerformed

    private void jButtonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSaveActionPerformed
        JFileChooser chooser = new JFileChooser();
        File parentFile = propertiesFile.getParentFile();
        if (null != parentFile) {
            chooser.setCurrentDirectory(parentFile);
        }
        chooser.setSelectedFile(propertiesFile);
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            this.setPropertiesFile(chooser.getSelectedFile());
        }
        DbSetupBuilder.savePropertiesFile(propertiesFile, getDbSetup());
        this.notifyAllDbSetupListeners();
    }//GEN-LAST:event_jButtonSaveActionPerformed

    private void jButtonLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLoadActionPerformed
        this.setPropertiesFile(new File(jComboBoxPropertiesFiles.getSelectedItem().toString()));
        DbSetup newSetup = DbSetupBuilder.loadFromPropertiesFile(propertiesFile).build();
        this.setDbSetup(newSetup);
        this.notifyAllDbSetupListeners();
    }//GEN-LAST:event_jButtonLoadActionPerformed

    private void jButtonBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonBrowseActionPerformed
        JFileChooser chooser = new JFileChooser();
        File parentFile = propertiesFile.getParentFile();
        if (null != parentFile) {
            chooser.setCurrentDirectory(parentFile);
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            this.setPropertiesFile(chooser.getSelectedFile());
        }
    }//GEN-LAST:event_jButtonBrowseActionPerformed

    @MonotonicNonNull
    private volatile String lastResourceDirSet = null;

    private void jComboBoxResourceDirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxResourceDirActionPerformed
        try {
            Object item = this.jComboBoxResourceDir.getSelectedItem();
            if (null != item && !item.equals(lastResourceDirSet)) {
                String resDirSuffix = item.toString();
                if (null != resDirSuffix && !"none".equals(resDirSuffix)) {
                    this.lastResourceDirSet = resDirSuffix;
                    jComboBoxResourceDir.setSelectedItem(resDirSuffix);
                    updateResDirSuffix(resDirSuffix, () -> {
                        if (!updatingFromDbSetup) {
                            notifyAllDbSetupListeners();
                        }
                    });
                }
            }
        } catch (IOException iOException) {
            Logger.getLogger(DbSetupJPanel.class.getName()).log(Level.SEVERE, null, iOException);
        }
    }//GEN-LAST:event_jComboBoxResourceDirActionPerformed

    private void updateResDirSuffix(String resDirSuffix, @Nullable Runnable r) throws IOException {
        mapUpToDate = false;
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
        map
                = DbSetupBuilder.readResourceQueriesDirectory(resDir);
        mapUpToDate = true;
        loadQueriesMap(map, r);
    }


    private void jButtonBrowseExternalDirectoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonBrowseExternalDirectoryActionPerformed
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            loadExternalQueriesDirectory(f);
        }
    }//GEN-LAST:event_jButtonBrowseExternalDirectoryActionPerformed

    private void jButtonLoadExternalDirectoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLoadExternalDirectoryActionPerformed
        loadExternalQueriesDirectory(new File(jTextFieldQueriesDirectory.getText()));
    }//GEN-LAST:event_jButtonLoadExternalDirectoryActionPerformed

    private void jTextFieldQueriesDirectoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldQueriesDirectoryActionPerformed
        loadExternalQueriesDirectory(new File(jTextFieldQueriesDirectory.getText()));
    }//GEN-LAST:event_jTextFieldQueriesDirectoryActionPerformed

    private void loadExternalQueriesDirectory(File f) {
        try {
            if (!jTextFieldQueriesDirectory.getText().equals(f.getCanonicalPath())) {
                jTextFieldQueriesDirectory.setText(f.getCanonicalPath());
            }
            jRadioButtonExternDir.setSelected(true);
            Map<DbQueryEnum, DbQueryInfo> queriesMap
                    = DbSetupBuilder.readQueriesDirectory(f.getAbsolutePath());
            loadQueriesMap(queriesMap, null);
        } catch (IOException ex) {
            Logger.getLogger(DbSetupJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private boolean debug = false;

    private boolean connected = false;
    private volatile boolean updatingFromDbSetup = false;

    /**
     * Display options according to the given setup object.
     *
     * @param setup object to read properties from
     */
    @Override
    public void setDbSetup(DbSetup setup) {
        try {
            if (null == setup) {
                return;
            }
            if (setup.isConnected() && (null == setup.getDbType() || setup.getDbType() == DbType.NONE)) {
                this.connected = false;
                throw new IllegalArgumentException("setup.getDbType() == " + setup.getDbType());
            }
            jCheckBoxDebug.setSelected(setup.isDebug());
            updatingFromDbSetup = true;
            DbType dbtype = setup.getDbType();
            if (!Objects.equals(dbtype, this.jComboBoxDbType.getSelectedItem())) {
                this.jComboBoxDbType.setSelectedItem(dbtype);
            }
            String host = setup.getHost();
            if (!Objects.equals(host, jTextFieldDBHost.getText())) {
                this.jTextFieldDBHost.setText(setup.getHost());
            }
            int port = setup.getPort();
            int curport = -99;
            try {
                curport = Integer.parseInt(jTextFieldDBPort.getText());
            } catch (Exception e) {
                // deliberately ignored
            }
            if (curport != port) {
                this.jTextFieldDBPort.setText(Integer.toString(port));
            }
            int loginTimeout = setup.getLoginTimeout();
            int curLoginTimeout = -99;
            try {
                curLoginTimeout = Integer.parseInt(jTextFieldDBPort.getText());
            } catch (Exception e) {
                // deliberately ignored
            }
            if (curLoginTimeout != loginTimeout) {
                this.jTextFieldDBLoginTimeout.setText(Integer.toString(loginTimeout));
            }
            char curpasswd[] = jPasswordFieldDBPassword.getPassword();
            char newpasswd[] = setup.getDbPassword();
            if (!Arrays.equals(curpasswd, newpasswd)) {
                this.jPasswordFieldDBPassword.setText(new String(newpasswd));
            }
            String user = setup.getDbUser();
            if (user != null && !Objects.equals(user, jTextFieldDBUser.getText())) {
                this.jTextFieldDBUser.setText(user);
            }
            String dbname = setup.getDbName();
            if (dbname != null && !Objects.equals(user, jTextFieldDBName.getText())) {
                this.jTextFieldDBName.setText(dbname);
            }
            this.connected = setup.isConnected();

            if (jButtonConnectDB.isEnabled() != (!connected)) {
                this.jButtonConnectDB.setEnabled(!connected);
            }
            if (jButtonDisconnectDB.isEnabled() != connected) {
                this.jButtonDisconnectDB.setEnabled(connected);
            }
            boolean internal = setup.isInternalQueriesResourceDir();
            if (internal != this.jRadioButtonResourceDir.isSelected()) {
                jRadioButtonResourceDir.setSelected(internal);
            }
            if (internal == this.jRadioButtonExternDir.isSelected()) {
                jRadioButtonExternDir.setSelected(!internal);
            }
            String queryDir = setup.getQueriesDir();
            boolean queriesMapReloaded = false;
            if (null != queryDir) {
                if (internal) {
                    lastResourceDirSet = queryDir;
                    jComboBoxResourceDir.setSelectedItem(queryDir);
                    updateResDirSuffix(queryDir, null);
                    queriesMapReloaded = true;
                } else if (!Objects.equals(queryDir, jTextFieldQueriesDirectory.getText())) {
                    loadExternalQueriesDirectory(new File(queryDir));
                    queriesMapReloaded = true;
                }
            }
            String startScript = setup.getStartScript();
            if (startScript != null && startScript.length() > 0) {
                jTextFieldStartScript.setText(startScript);
            }
            if (!queriesMapReloaded) {
                Map<DbQueryEnum, DbQueryInfo> queriesMap = setup.getQueriesMap();
                if (null != queriesMap) {
                    loadQueriesMap(queriesMap, null);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(DbSetupJPanel.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            updatingFromDbSetup = false;
        }
    }

    private void loadQueriesMapInternal(Map<DbQueryEnum, DbQueryInfo> queriesMap, @Nullable Runnable r) {
        DefaultTableModel model = (DefaultTableModel) jTableQueries.getModel();
        model.setRowCount(0);
        for (Map.Entry<DbQueryEnum, DbQueryInfo> entry : queriesMap.entrySet()) {
            model.addRow(new Object[]{entry.getKey(), entry.getValue().getOrigText()});
        }
        autoResizeTableColWidths(jTableQueries);
        autoResizeTableRowHeights(jTableQueries);
        if (null != r) {
            r.run();
        }
    }

    private void loadQueriesMap(Map<DbQueryEnum, DbQueryInfo> queriesMap, @Nullable Runnable r) {
        Utils.runOnDispatchThread(() -> loadQueriesMapInternal(queriesMap, r));
    }

    private void setupMultiLineTable(JTable jTable,
            int multiLineColumnIndex,
            JTextArea editTableArea) {
        jTable.getColumnModel().getColumn(multiLineColumnIndex).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, @Nullable Object value, boolean isSelected, boolean hasFocus, int row, int column) {
//                while (viewAreas.size() <= row) {
//                    JTextArea area = new JTextArea();
//                    area.setOpaque(true);
//                    area.setVisible(true);
//                    viewAreas.add(area);
//                }
//                if (null == viewAreas.get(row)) {
//                    JTextArea area = new JTextArea();
//                    area.setOpaque(true);
//                    area.setVisible(true);
//                    viewAreas.set(row, area);
//                }

                JTextArea area = new JTextArea();
                area.setOpaque(true);
                area.setVisible(true);
                area.setText(Objects.toString(value));
                return area;
            }

        });
        jTable.getColumnModel().getColumn(multiLineColumnIndex).setCellEditor(new TableCellEditor() {

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
                        l.editingStopped(new ChangeEvent(jTable));
                    }
                }
                return true;
            }

            @Override
            public void cancelCellEditing() {
                for (int i = 0; i < listeners.size(); i++) {
                    CellEditorListener l = listeners.get(i);
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

    @Nullable
    private volatile Map<DbQueryEnum, DbQueryInfo> map = null;
    private volatile boolean mapUpToDate = false;

    private volatile boolean disconnecting = false;

    @Override
    public void disconnect() {
        boolean was_disconnecting = disconnecting;
        try {
            disconnecting = true;
            setDbSetup(new DbSetupBuilder().setup(this.getDbSetup()).connected(false).build());
        } finally {
            disconnecting = was_disconnecting;
        }
    }

    public Map<DbQueryEnum, DbQueryInfo> getQueriesMap() {
        if (disconnecting) {
            if (map == null) {
                return Collections.emptyMap();
            }
            return map;
        }
        if (null != map && mapUpToDate) {
            return map;
        }
        mapUpToDate = false;
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            map = getQueriesMapInternal();
            mapUpToDate = true;
        } else {
            try {
                javax.swing.SwingUtilities.invokeAndWait(() -> {
                    if (null != map && mapUpToDate) {
                        return;
                    }
                    map = getQueriesMapInternal();
                    mapUpToDate = true;
                });
            } catch (InterruptedException | InvocationTargetException ex) {
                if (!disconnecting) {
                    Logger.getLogger(DbSetupJPanel.class.getName()).log(Level.SEVERE, null, ex);
                    throw new RuntimeException(ex);
                }
            }
        }
        Map<DbQueryEnum, DbQueryInfo> ret = map;
        if (null == ret) {
            throw new IllegalStateException("couldn't initialize query to info map: map == null");
        }
        return ret;
    }

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
                    DbQueryInfo.parse(Objects.toString(queryObject)));
        }
        return newMap;
    }

    /**
     * Get the name of the directory where query info text files were read from
     * or should be read from.
     *
     * @return queries directory
     */
    public String getQueriesDir() {
        if (jRadioButtonResourceDir.isSelected()) {
            return jComboBoxResourceDir.getSelectedItem().toString();
        }
        return jTextFieldQueriesDirectory.getText();
    }

    private static int parseIntOr(String string, int defaultValue) {
        try {
            return Integer.parseInt(string);
        } catch (Exception ex) {

        }
        return defaultValue;
    }

    /**
     * Get the user's currently chosen/entered options in a setup object.
     *
     * @return setup
     */
    @Override
    public DbSetup getDbSetup() {
        DbType dbtype = (DbType) jComboBoxDbType.getSelectedItem();
        if (null == dbtype || dbtype == DbType.NONE) {
            connected = false;
        }
        return new DbSetupBuilder()
                .connected(connected)
                .type((DbType) jComboBoxDbType.getSelectedItem())
                .host(jTextFieldDBHost.getText())
                .passwd(jPasswordFieldDBPassword.getPassword())
                .dbname(jTextFieldDBName.getText())
                .user(jTextFieldDBUser.getText())
                .port(Integer.parseInt(jTextFieldDBPort.getText()))
                .loginTimeout(parseIntOr(jTextFieldDBLoginTimeout.getText(), 5))
                .queriesMap(getQueriesMap())
                .internalQueriesResourceDir(jRadioButtonResourceDir.isSelected())
                .queriesDir(getQueriesDir())
                .debug(jCheckBoxDebug.isSelected())
                .startScript(jTextFieldStartScript.getText())
                .build();
    }

    private ExecutorService notifyService = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "dbSetupNotifyThread." + this.toString());
            thread.setDaemon(true);
            return thread;
        }
    });

    @Nullable
    volatile private List<Future<?>> futures = null;

    public void shutDownNotifyService() {
        try {
            notifyService.shutdownNow();
            notifyService.awaitTermination(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Logger.getLogger(DbSetupJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Call the accept method of all registered listeners with the current setup
     * object.
     *
     * Typically forcing multiple modules to reconnect to the database with new
     * options.
     *
     * @return list of futures for determining when all the listeners have been
     * notified.
     */
    @Override
    public List<Future<?>> notifyAllDbSetupListeners() {
        boolean cancelWarnGiven = false;
        AprsSystemInterface parentFrame = this.aprsJFrame;
        if (null == parentFrame) {
            throw new IllegalStateException("this.aprsJFrame == null");
        }
        AprsSystemInterface checkedParentFrame = parentFrame;
        List<Future<?>> origFutures = this.futures;
        if (null != origFutures) {
            for (Future<?> f : origFutures) {
                if (!f.isDone() && !f.isCancelled()) {
                    if (!cancelWarnGiven && !parentFrame.isClosing()) {
                        cancelWarnGiven = true;
                        System.err.println("Cancelling a dbSetup notification");
                        if (checkedParentFrame.isEnableDebugDumpstacks()) {
                            Thread.dumpStack();
                        }
                    }
                    f.cancel(false);
                }
            }
        }
        List<Future<?>> newFutures = new ArrayList<>();
        this.futures = newFutures;
        if (notifyService != null && !parentFrame.isClosing()) {
            final DbSetup thisDbSetup = DbSetupJPanel.this.getDbSetup();
//            System.out.println("thisDbSetup = " + thisDbSetup);
//            System.out.println("thisDbSetup.getQueriesMap() = " + thisDbSetup.getQueriesMap());
            Future<?> future
                    = notifyService.submit(new Runnable() {
                        @Override
                        public void run() {
                            for (DbSetupListener listener : dbSetupListeners) {
                                if (null != listener) {
                                    listener.accept(thisDbSetup);
                                }
                            }
                        }
                    });
            newFutures.add(future);
        }
        return newFutures;
    }

    private void addComboItemUnique(String item) {
        for (int i = 0; i < jComboBoxPropertiesFiles.getItemCount(); i++) {
            if (Objects.equals(jComboBoxPropertiesFiles.getItemAt(i), item)) {
                return;
            }
        }
        jComboBoxPropertiesFiles.addItem(item);
    }

    /**
     * Load the most recent settings file.
     */
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
                jComboBoxPropertiesFiles.removeAllItems();
                List<File> files = new ArrayList<>();
                files.addAll(set.stream().map(File::new)
                        .filter(File::exists)
                        .sorted(Comparator.comparing(File::lastModified).reversed())
                        .limit(3)
                        .collect(Collectors.toList()));
                try (PrintWriter pw = new PrintWriter(new FileWriter(recentSettingsFile))) {
                    for (File f : files) {
                        String p = f.getCanonicalPath();
                        pw.println(p);
                        addComboItemUnique(p);
                    }
                }
            }
        } catch (IOException iOException) {
            Logger.getLogger(DbSetupJPanel.class.getName()).log(Level.SEVERE, null, iOException);
        }
    }
    private File recentSettingsFile = new File(System.getProperty("user.home"), ".dbsetup_recent.txt");
    private File propertiesFile = new File(System.getProperty("user.home"), ".dbsetup_properties.txt");

    @Override
    public void setPropertiesFile(File f) {
        try {
            propertiesFile = f;
            String newPath = f.getCanonicalPath();
            addComboItemUnique(newPath);
            jComboBoxPropertiesFiles.setSelectedItem(newPath);
            saveRecent(newPath);
        } catch (IOException iOException) {
            Logger.getLogger(DbSetupJPanel.class.getName()).log(Level.SEVERE, null, iOException);
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
    public File getPropertiesFile() {
        return propertiesFile;
    }
    private volatile boolean savingProperties = false;

    @Override
    public void saveProperties() {
        DbSetup setup = this.getDbSetup();
        saveProperties(setup.getDbType(), setup.getHost(), setup.getPort());
    }

    private void saveProperties(DbType dbtype, String host, int port) {
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
                    Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
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
            props.put("useQueriesResource", jRadioButtonResourceDir.isSelected());
            props.put("resDir", jComboBoxResourceDir.getSelectedItem());
            props.put("queryDir", jTextFieldQueriesDirectory.getText());
            props.put("startScript", jTextFieldStartScript.getText());
            Utils.saveProperties(propertiesFile, props);
        } finally {
            savingProperties = false;
        }
    }

    @Override
    public String toString() {
        return "DbSetupJPanel{" + "aprsJFrame=" + aprsJFrame + ", connected=" + connected + '}';
    }

    private volatile boolean restoringProperties = false;

    public Map<String, String> updateArgsMap() {
        return updateArgsMap(this.getDbSetup().getDbType());
    }

    private final Map<String, String> argsMap = DbSetupBuilder.getDefaultArgsMap();

    public Map<String, String> updateArgsMap(DbType dbtype) {
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

//    public final void restoreProperties(DbType dbtype, String host, int port) {
//        try {
//            restoringProperties = true;
//            if (null != propertiesFile && propertiesFile.exists()) {
//                Properties props = new Properties();
//                try (FileReader fr = new FileReader(propertiesFile)) {
//                    props.load(fr);
//                } catch (IOException ex) {
//                    Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
//                }
//                for (String propName : props.stringPropertyNames()) {
//                    argsMap.put(propName, props.getProperty(propName));
//                }
//                updateFromArgs(argsMap, dbtype, host, port, null);
//            }
//        } catch (Exception e) {
//            addLogMessage(e);
//        } finally {
//            restoringProperties = false;
//        }
//    }
    public void addLogMessage(Exception e) {
        e.printStackTrace();
    }

    public void addLogMessage(String msg) {
        System.err.println(msg);
    }

    public DbType getDbType() {
        return (DbType) jComboBoxDbType.getSelectedItem();
    }

    private volatile boolean updatingFromArgs = false;

    private static String checkedArgsMapGet(Map<String, String> _argsMap, String key) {
        String value = _argsMap.get(key);
        if (null == value) {
            throw new IllegalStateException("map does not contain required key=" + key + ", map=" + _argsMap);
        }
        return value;
    }

    private void updateFromArgs(Map<String, String> _argsMap, DbType dbtype, String host, int port, @Nullable DbSetup curSetup) {
        try {
            updatingFromArgs = true;
            DbSetupBuilder builder = new DbSetupBuilder()
                    .type(dbtype)
                    .host(host)
                    .port(port)
                    .dbname(checkedArgsMapGet(_argsMap, "--dbname"))
                    .user(checkedArgsMapGet(_argsMap, "--dbuser"))
                    .passwd(_argsMap.getOrDefault("--dbpasswd", "").toCharArray());
            if (null == host) {
                String dbSpecificHost = _argsMap.get(dbtype + ".host");
                if (null != dbSpecificHost) {
                    builder = builder.host(dbSpecificHost);
                    host = dbSpecificHost;
                }
                if (host == null) {
                    if (null == curSetup) {
                        curSetup = this.getDbSetup();
                    }
                    host = curSetup.getHost();
                }
                String dbSpecificPort = _argsMap.get(this.getDbType() + "." + host + ".port");
                if (null != dbSpecificPort) {

                    port = Integer.parseInt(dbSpecificPort);
                    builder = builder.port(port);
                }
            }
            if (port < 1) {
                if (host == null) {
                    if (null == curSetup) {
                        curSetup = this.getDbSetup();
                    }
                    host = curSetup.getHost();
                }
                String dbSpecificPort = _argsMap.get(this.getDbType() + "." + host + ".port");
                if (null != dbSpecificPort) {
//                    this.jTextFieldDBPort.setText(dbSpecificPort);
                    port = Integer.parseInt(dbSpecificPort);
                    builder = builder.port(port);
                }
            }
            String dbHostPort = String.format("%s.%s_%d", dbtype.toString(), host, port);
            String dbSpecificName = _argsMap.get(dbHostPort + ".name");
            if (null != dbSpecificName) {
                builder = builder.dbname(dbSpecificName);
            }
            String dbSpecificUser = _argsMap.get(dbHostPort + ".user");
            if (null != dbSpecificUser) {
                builder = builder.user(dbSpecificUser);
            }
            String dbSpecificPasswd = _argsMap.get(dbHostPort + ".passwd");
            if (null != dbSpecificPasswd) {
                builder = builder.passwd(dbSpecificPasswd.toCharArray());
            }
            this.setDbSetup(builder.build());
        } finally {
            updatingFromArgs = false;
        }
    }

    final String RESOURCE_BASE = "aprs/framework/database";

    public void updateFromArgs(Map<String, String> _argsMap) {
        try {
            updatingFromArgs = true;
            String host = checkedArgsMapGet(_argsMap, "--dbhost");
            int port = Integer.parseInt(checkedArgsMapGet(_argsMap, "--dbport"));
            String argsMapDbTypeString = checkedArgsMapGet(_argsMap, "--dbtype");
            DbSetup curSetup = null;
            DbType dbtype = null;
            if (argsMapDbTypeString == null || argsMapDbTypeString.length() < 1) {
                curSetup = this.getDbSetup();
                dbtype = curSetup.getDbType();
            } else {
                dbtype = DbType.valueOf(argsMapDbTypeString);
            }
            DbSetupBuilder builder = new DbSetupBuilder()
                    .type(dbtype)
                    .host(host)
                    .port(port)
                    .dbname(checkedArgsMapGet(_argsMap, "--dbname"))
                    .user(checkedArgsMapGet(_argsMap, "--dbuser"))
                    .passwd(_argsMap.getOrDefault("--dbpasswd", "").toCharArray());
            String dbSpecificHost = _argsMap.get(dbtype + ".host");
            if (null != dbSpecificHost) {
                builder = builder.host(dbSpecificHost);
                host = dbSpecificHost;
            }
            String dbSpecificPort = _argsMap.get(dbtype + "." + host + ".port");
            if (null != dbSpecificPort) {
                port = Integer.parseInt(dbSpecificPort);
                builder = builder.port(port);
            }
            String useQueriesResourceString = _argsMap.get("useQueriesResource");
            boolean useQueriesResource = true;
            if (null != useQueriesResourceString) {
                useQueriesResource = Boolean.valueOf(useQueriesResourceString);
            }
            String resDirSuffix = _argsMap.get("resDir");
            if (null != resDirSuffix) {
                this.lastResourceDirSet = resDirSuffix;
                jComboBoxResourceDir.setSelectedItem(resDirSuffix);
            }
            String startScriptString = _argsMap.get("startScript");
            if (null != startScriptString && startScriptString.length() > 0) {
                jTextFieldStartScript.setText(startScriptString);
            }
            if (useQueriesResource) {
                jRadioButtonExternDir.setSelected(false);
                jRadioButtonResourceDir.setSelected(true);
                try {
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
                    Map<DbQueryEnum, DbQueryInfo> queriesMap
                            = DbSetupBuilder.readResourceQueriesDirectory(resDir);
                    loadQueriesMap(queriesMap, null);
                    builder = builder.queriesMap(queriesMap);
                } catch (IOException ex) {
                    Logger.getLogger(DbSetupJPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                jRadioButtonExternDir.setSelected(true);
                jRadioButtonResourceDir.setSelected(false);
                String queryDir = _argsMap.get("queryDir");
                if (null != queryDir) {
                    try {
                        jTextFieldQueriesDirectory.setText(queryDir);
                        Map<DbQueryEnum, DbQueryInfo> queriesMap
                                = DbSetupBuilder.readQueriesDirectory(queryDir);
                        loadQueriesMap(queriesMap, null);
                        builder = builder.queriesMap(queriesMap);
                    } catch (IOException ex) {
                        Logger.getLogger(DbSetupJPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            updateFromArgs(_argsMap, dbtype, host, port, curSetup);

        } finally {
            updatingFromArgs = false;
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

    /**
     * Get name of a script file to execute to start the database server if
     * available
     *
     * @return name of start script file
     */
    public String getStartScript() {
        return jTextFieldStartScript.getText();
    }

    @Override
    public final void loadProperties() {
        try {
            restoringProperties = true;
            if (null != propertiesFile && propertiesFile.exists()) {
                Properties props = new Properties();
                try (FileReader fr = new FileReader(propertiesFile)) {
                    props.load(fr);
                } catch (IOException ex) {
                    Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
                for (String propName : props.stringPropertyNames()) {
                    String propValue = props.getProperty(propName);
                    if (null != propValue) {
                        argsMap.put(propName, propValue);
                    }
                }
                updateFromArgs(argsMap);
            }
        } catch (Exception e) {
            addLogMessage(e);
        } finally {
            restoringProperties = false;
        }
    }

    @Override
    public void close() throws Exception {
    }

    Set<DbSetupListener> dbSetupListeners = new HashSet<>();

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

    public DefaultComboBoxModel<DbType> getDbTypeComboModel() {
        return dbTypeComboModel;
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
    private javax.swing.JComboBox<aprs.framework.database.DbType> jComboBoxDbType;
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
