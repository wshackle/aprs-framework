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
package aprs.framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class AprsMulitSupervisorJFrame extends javax.swing.JFrame {

    /**
     * Creates new form AprsMulitSupervisorJFrame
     */
    public AprsMulitSupervisorJFrame() {

        initComponents();
        try {
            loadSetupFile(new File(readFirstLine(lastSetupFileFile)));
        } catch (IOException ex) {
            try {
                closeAllAprsSystems();
            } catch (IOException ex1) {
                // ignore
            }
        }
    }

    private String readFirstLine(File f) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            return br.readLine();
        }
    }

    private final File lastSetupFileFile = new File(System.getProperty("aprsLastMultiSystemSetupFile", System.getProperty("user.home") + File.separator + ".lastAprsSetupFile.txt"));

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanelTasks = new javax.swing.JPanel();
        jScrollPaneTasks = new javax.swing.JScrollPane();
        jTableTasks = new javax.swing.JTable();
        jPanelRobots = new javax.swing.JPanel();
        jScrollPaneRobots = new javax.swing.JScrollPane();
        jTableRobots = new javax.swing.JTable();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItemSaveSetupAs = new javax.swing.JMenuItem();
        jMenuItemLoadSetup = new javax.swing.JMenuItem();
        jMenuItemAddSystem = new javax.swing.JMenuItem();
        jMenuItemDeleteSelectedSystem = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanelTasks.setBorder(javax.swing.BorderFactory.createTitledBorder("Tasks"));

        jTableTasks.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                { new Integer(1), "2 2l2s Kits", "Motoman", null},
                { new Integer(2), "2  1l2m Kits", "Fanuc", null}
            },
            new String [] {
                "Priority", "Task", "Robot", "PropertiesFile"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPaneTasks.setViewportView(jTableTasks);

        javax.swing.GroupLayout jPanelTasksLayout = new javax.swing.GroupLayout(jPanelTasks);
        jPanelTasks.setLayout(jPanelTasksLayout);
        jPanelTasksLayout.setHorizontalGroup(
            jPanelTasksLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTasksLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPaneTasks)
                .addContainerGap())
        );
        jPanelTasksLayout.setVerticalGroup(
            jPanelTasksLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPaneTasks, javax.swing.GroupLayout.DEFAULT_SIZE, 220, Short.MAX_VALUE)
        );

        jPanelRobots.setBorder(javax.swing.BorderFactory.createTitledBorder("Robots"));

        jTableRobots.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Motoman",  new Boolean(true)},
                {"Fanuc",  new Boolean(true)}
            },
            new String [] {
                "Robot", "Enabled"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Boolean.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPaneRobots.setViewportView(jTableRobots);

        javax.swing.GroupLayout jPanelRobotsLayout = new javax.swing.GroupLayout(jPanelRobots);
        jPanelRobots.setLayout(jPanelRobotsLayout);
        jPanelRobotsLayout.setHorizontalGroup(
            jPanelRobotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelRobotsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPaneRobots, javax.swing.GroupLayout.DEFAULT_SIZE, 593, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelRobotsLayout.setVerticalGroup(
            jPanelRobotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelRobotsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPaneRobots, javax.swing.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE)
                .addContainerGap())
        );

        jMenu1.setText("File");

        jMenuItemSaveSetupAs.setText("Save Setup As ...");
        jMenuItemSaveSetupAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveSetupAsActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemSaveSetupAs);

        jMenuItemLoadSetup.setText("Load Setup ...");
        jMenuItemLoadSetup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemLoadSetupActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemLoadSetup);

        jMenuItemAddSystem.setText("Add System ...");
        jMenuItemAddSystem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemAddSystemActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemAddSystem);

        jMenuItemDeleteSelectedSystem.setText("Delete Selected System");
        jMenuItemDeleteSelectedSystem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDeleteSelectedSystemActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemDeleteSelectedSystem);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");
        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanelTasks, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jPanelRobots, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelTasks, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelRobots, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItemSaveSetupAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveSetupAsActionPerformed
        JFileChooser chooser = new JFileChooser(System.getProperty("user.home"));
        if (JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(this)) {
            try {
                saveSetupFile(chooser.getSelectedFile());
            } catch (IOException ex) {
                Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jMenuItemSaveSetupAsActionPerformed

    private void jMenuItemLoadSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemLoadSetupActionPerformed
        JFileChooser chooser = new JFileChooser(System.getProperty("user.home"));
        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(this)) {
            try {
                loadSetupFile(chooser.getSelectedFile());
            } catch (IOException ex) {
                Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jMenuItemLoadSetupActionPerformed

    private void jMenuItemAddSystemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemAddSystemActionPerformed
        JFileChooser chooser  = new JFileChooser();
        if(JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(this)) {
            try {
                File propertiesFile = chooser.getSelectedFile();
                AprsJFrame aj = new AprsJFrame(propertiesFile);
                aj.setPropertiesFile(propertiesFile);
                aj.setPriority(aprsSystems.size()+1);
                aj.setVisible(true);
                aj.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                aprsSystems.add(aj);
                updateTasksTable();
                updateRobotsTable();
            } catch (IOException ex) {
                Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jMenuItemAddSystemActionPerformed

    private void jMenuItemDeleteSelectedSystemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemDeleteSelectedSystemActionPerformed
        int selectedIndex = jTableTasks.getSelectedRow();
        if(selectedIndex >=0 && selectedIndex < aprsSystems.size()) {
            try {
                AprsJFrame aj = aprsSystems.remove(selectedIndex);
                try {
                    aj.close();
                } catch (Exception ex) {
                    Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
                updateTasksTable();
                updateRobotsTable();
            } catch (IOException ex) {
                Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jMenuItemDeleteSelectedSystemActionPerformed

    public void saveSetupFile(File f) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            TableModel tm = jTableTasks.getModel();
            for (int i = 0; i < tm.getRowCount(); i++) {
                pw.println(tm.getValueAt(i, 0) + "," + tm.getValueAt(i, 1) + "," + tm.getValueAt(i, 2)+","+tm.getValueAt(i, 3));
            }
        }
        saveLastSetupFile(f);
    }

    private void saveLastSetupFile(File f) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(lastSetupFileFile))) {
            pw.println(f.getCanonicalPath());
        }
    }

    private final List<AprsJFrame> aprsSystems = new ArrayList<>();

    /**
     * Get the value of aprsSystems
     *
     * @return the value of aprsSystems
     */
    public List<AprsJFrame> getAprsSystems() {
        return Collections.unmodifiableList(aprsSystems);
    }

    public void closeAllAprsSystems() throws IOException {
        for (AprsJFrame aprsJframe : aprsSystems) {
            try {
                aprsJframe.close();
            } catch (Exception ex) {
                Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        aprsSystems.clear();
        updateTasksTable();
        updateRobotsTable();
    }

    public final void loadSetupFile(File f) throws IOException {
        closeAllAprsSystems();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            DefaultTableModel tm = (DefaultTableModel) jTableTasks.getModel();
            String line = null;
            tm.setRowCount(0);
            int linecount = 0;
            while (null != (line = br.readLine())) {
                String fields[] = line.split("[ \t,]+");
                linecount++;
                if(fields.length != 4) {
                    System.err.println("Bad line :"+linecount +" in "+f+"  --> "+line);
                    System.err.println("fields="+Arrays.toString(fields));
                    System.err.println("fields.length="+fields.length);
                    System.err.println("fields.length must equal 4");
                    System.out.println("");
                    break;
                }
//                tm.addRow(fields);
                int priority = Integer.valueOf(fields[0]);
                File propertiesFile = new File(fields[3]);
                AprsJFrame aj = new AprsJFrame(propertiesFile);
                aj.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                aj.setPriority(priority);
                aj.setTaskName(fields[1]);
                aj.setRobotName(fields[2]);
                aj.setPropertiesFile(propertiesFile);
                aj.loadProperties();
                aj.setVisible(true);
                aprsSystems.add(aj);
            }
        }
        Collections.sort(aprsSystems, new Comparator<AprsJFrame>() {
            @Override
            public int compare(AprsJFrame o1, AprsJFrame o2) {
                return Integer.compare(o1.getPriority(), o2.getPriority());
            }
        });
        updateTasksTable();
        updateRobotsTable();
        saveLastSetupFile(f);
    }

    public void updateTasksTable() throws IOException {
        DefaultTableModel tm = (DefaultTableModel) jTableTasks.getModel();
        tm.setRowCount(0);
        for (AprsJFrame aprsJframe : aprsSystems) {
            tm.addRow(new Object[]{aprsJframe.getPriority(), aprsJframe.getTaskName(), aprsJframe.getRobotName(), aprsJframe.getPropertiesFile().getCanonicalPath()});
        }
    }

    public void updateRobotsTable() throws IOException {
        Set<String> robotSet = new TreeSet<>();

        DefaultTableModel tm = (DefaultTableModel) jTableRobots.getModel();
        tm.setRowCount(0);
        for (AprsJFrame aprsJframe : aprsSystems) {
            robotSet.add(aprsJframe.getRobotName());
        }
        for (String robot : robotSet) {
            tm.addRow(new Object[]{robot, true});
        }
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
            java.util.logging.Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new AprsMulitSupervisorJFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItemAddSystem;
    private javax.swing.JMenuItem jMenuItemDeleteSelectedSystem;
    private javax.swing.JMenuItem jMenuItemLoadSetup;
    private javax.swing.JMenuItem jMenuItemSaveSetupAs;
    private javax.swing.JPanel jPanelRobots;
    private javax.swing.JPanel jPanelTasks;
    private javax.swing.JScrollPane jScrollPaneRobots;
    private javax.swing.JScrollPane jScrollPaneTasks;
    private javax.swing.JTable jTableRobots;
    private javax.swing.JTable jTableTasks;
    // End of variables declaration//GEN-END:variables
}
