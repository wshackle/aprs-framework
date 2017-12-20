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
package aprs.framework.posmatch;

import aprs.framework.Utils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import rcs.posemath.PmCartesian;

/**
 *
 * @author shackle
 */
public class PosMatchJPanel extends javax.swing.JPanel {

    /**
     * Creates new form PosMatchJPanel
     */
    public PosMatchJPanel() {
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

        jPanel1 = new javax.swing.JPanel();
        jTextFieldFilename1 = new javax.swing.JTextField();
        jButtonSelectFile1 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jTextFieldFilename2 = new javax.swing.JTextField();
        jButtonSelectFile2 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableMatches = new javax.swing.JTable();
        jButtonSaveMatchesCsvFile = new javax.swing.JButton();

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("File 1"));

        jButtonSelectFile1.setText("Select File ");
        jButtonSelectFile1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSelectFile1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTextFieldFilename1, javax.swing.GroupLayout.PREFERRED_SIZE, 218, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonSelectFile1)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldFilename1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonSelectFile1))
                .addContainerGap())
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("File 2"));

        jButtonSelectFile2.setText("Select File ");
        jButtonSelectFile2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSelectFile2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTextFieldFilename2, javax.swing.GroupLayout.PREFERRED_SIZE, 218, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonSelectFile2)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldFilename2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonSelectFile2))
                .addContainerGap())
        );

        jTableMatches.setAutoCreateRowSorter(true);
        jTableMatches.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "X1", "Y1", "X2", "Y2", "X_Offset", "Y_Offset"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, true, true, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTableMatches);

        jButtonSaveMatchesCsvFile.setText("Save Matches CSV File");
        jButtonSaveMatchesCsvFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSaveMatchesCsvFileActionPerformed(evt);
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
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButtonSaveMatchesCsvFile)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 381, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonSaveMatchesCsvFile))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonSelectFile1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSelectFile1ActionPerformed
        browseOpenFile(0);
    }//GEN-LAST:event_jButtonSelectFile1ActionPerformed

    private void jButtonSelectFile2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSelectFile2ActionPerformed
        browseOpenFile(1);
    }//GEN-LAST:event_jButtonSelectFile2ActionPerformed

    private void jButtonSaveMatchesCsvFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSaveMatchesCsvFileActionPerformed
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                saveJTable(chooser.getSelectedFile(), jTableMatches);
            } catch (IOException ex) {
                Logger.getLogger(PosMatchJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jButtonSaveMatchesCsvFileActionPerformed

    public void browseOpenFile(int index) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadFile(chooser.getSelectedFile(), index);
        }
    }

    final Map<String, List<PmCartesian>> maps[] = new Map[2];

    Map<String, List<PmCartesian>> parseFile(File f) {
        Map<String, List<PmCartesian>> map = new TreeMap<>();
        try {
            System.out.println("Loading setup file :" + f.getCanonicalPath());

            try (CSVParser parser = CSVParser.parse(f, Charset.defaultCharset(), Utils.preferredCsvFormat())) {
                for (CSVRecord csvRecord : parser) {
                    String name = csvRecord.get("name");
                    String xString = csvRecord.get("x");
                    String yString = csvRecord.get("y");
                    double x = Double.parseDouble(xString);
                    double y = Double.parseDouble(yString);
                    PmCartesian cart = new PmCartesian(x, y, 0);
                    map.compute(name, (String s, List<PmCartesian> l) -> {
                        if (null == l) {
                            l = new ArrayList<>();
                        }
                        l.add(cart);
                        return l;
                    });
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(PosMatchJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        return map;
    }

    public void loadFile(File f, int index) {
        if (index < 0 || index >= maps.length) {
            throw new IllegalArgumentException("index  of " + index + " must be > 0 && < " + maps.length);
        }
        maps[index] = parseFile(f);
        List<PosMatch> l = combineMaps(maps);
        loadMatchListToTable(l);
        if (index == 0) {
            try {
                jTextFieldFilename1.setText(f.getCanonicalPath());
            } catch (IOException ex) {
                Logger.getLogger(PosMatchJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (index == 1) {
            try {
                jTextFieldFilename2.setText(f.getCanonicalPath());
            } catch (IOException ex) {
                Logger.getLogger(PosMatchJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void loadMatchListToTable(List<PosMatch> l) {
        DefaultTableModel model = (DefaultTableModel) jTableMatches.getModel();
        model.setRowCount(0);
        for (PosMatch match : l) {
            model.addRow(new Object[]{match.name, match.cart1.x, match.cart1.y, match.cart2.x, match.cart2.y, match.cart1.x - match.cart2.x, match.cart1.y - match.cart2.y});
        }
    }

    public static class PosMatch {

        public final String name;
        public final PmCartesian cart1;
        public final PmCartesian cart2;

        public PosMatch(String name, PmCartesian cart1, PmCartesian cart2) {
            this.name = name;
            this.cart1 = cart1;
            this.cart2 = cart2;
        }
    }

    private void saveJTable(File f, JTable jtable) throws IOException {
        try (CSVPrinter printer = new CSVPrinter(new PrintStream(new FileOutputStream(f)), Utils.preferredCsvFormat())) {
            TableModel tm = jtable.getModel();
            List<Object> l = new ArrayList<>();
            for (int j = 0; j < tm.getColumnCount(); j++) {
                l.add(tm.getColumnName(j));
            }
            printer.printRecord(l);
            for (int i = 0; i < tm.getRowCount(); i++) {
                l = new ArrayList<>();
                boolean bad_record = false;
                for (int j = 0; j < tm.getColumnCount(); j++) {
                    Object o = tm.getValueAt(i, j);
                    if(o == null) {
                        bad_record = true;
                        break;
                    } else if(o instanceof Double) {
                        if(!Double.isFinite((Double)o)) {
                            bad_record = true;
                            break;
                        }
                        l.add(o);
                    } else if (o instanceof File) {
                        Path rel = f.getParentFile().toPath().toRealPath().relativize(Paths.get(((File) o).getCanonicalPath())).normalize();
                        if (rel.toString().length() < ((File) o).getCanonicalPath().length()) {
                            l.add(rel);
                        } else {
                            l.add(o);
                        }
                    } else {
                        l.add(o);
                    }
                }
                if(!bad_record) {
                    printer.printRecord(l);
                }
            }
        }
    }

    public static PosMatch bestMatch(String name, List<PmCartesian> posList1, List<PmCartesian> posList2, double maxDist) {
        int bestL1Index = -1;
        int bestL2Index = -1;
        double minDist = Double.POSITIVE_INFINITY;
        for (int i = 0; i < posList1.size(); i++) {
            for (int j = 0; j < posList2.size(); j++) {
                double dist = posList1.get(i).distFromXY(posList2.get(j));
                if (dist < minDist) {
                    bestL1Index = i;
                    bestL2Index = j;
                    minDist = dist;
                }
            }
        }
        if (bestL1Index >= 0 && bestL2Index >= 0 && minDist < maxDist) {
            return new PosMatch(name, posList1.remove(bestL1Index), posList2.remove(bestL2Index));
        }
        return null;
    }

    public static List<PosMatch> match(String name, List<PmCartesian> posList1, List<PmCartesian> posList2) {
        List<PosMatch> matchList = new ArrayList<>();
        List<PmCartesian> tempList1 = new ArrayList<>();
        if (null != posList1) {
            tempList1.addAll(posList1);
        }
        List<PmCartesian> tempList2 = new ArrayList<>();
        if (null != posList2) {
            tempList2.addAll(posList2);
        }
        if (null != posList1 && null != posList2) {
            while (tempList1.size() > 0 && tempList2.size() > 0) {
                PosMatch match = bestMatch(name, tempList1, tempList2, 100.0);
                if (null != match) {
                    matchList.add(match);
                } else {
                    break;
                }
            }
        }
        if (null != posList1) {
            for (PmCartesian cart : tempList1) {
                matchList.add(new PosMatch(name, cart, new PmCartesian(Double.NaN, Double.NaN, Double.NaN)));
            }
        }
        if (null != posList2) {
            for (PmCartesian cart : tempList2) {
                matchList.add(new PosMatch(name, new PmCartesian(Double.NaN, Double.NaN, Double.NaN), cart));
            }
        }
        return matchList;
    }

    public static List<PosMatch> combineMaps(Map<String, List<PmCartesian>> maps[]) {
        List<PosMatch> matchList = new ArrayList<>();
        Set<String> allNames = new TreeSet<>();
        if (null == maps || maps.length != 2) {
            throw new IllegalArgumentException("maps[] must have length 2");
        }
        if (null != maps[0]) {
            allNames.addAll(maps[0].keySet());
        }
        if (null != maps[1]) {
            allNames.addAll(maps[1].keySet());
        }
        for (String name : allNames) {

            List<PmCartesian> posList1 = (maps[0] != null) ? maps[0].get(name) : null;
            List<PmCartesian> posList2 = (maps[1] != null) ? maps[1].get(name) : null;
            List<PosMatch> thisNameMatches = match(name, posList1, posList2);
            matchList.addAll(thisNameMatches);
        }
        return matchList;
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonSaveMatchesCsvFile;
    private javax.swing.JButton jButtonSelectFile1;
    private javax.swing.JButton jButtonSelectFile2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTableMatches;
    private javax.swing.JTextField jTextFieldFilename1;
    private javax.swing.JTextField jTextFieldFilename2;
    // End of variables declaration//GEN-END:variables
}
