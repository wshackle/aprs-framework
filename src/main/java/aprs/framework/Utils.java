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

import crcl.base.CRCLCommandType;
import crcl.ui.XFuture;
import crcl.utils.CRCLSocket;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class Utils {

    private Utils() {
    }

    public static interface RunnableWithThrow {

        public void run() throws Exception;
    }

    public static class SwingFuture<T> extends XFuture<T> {

        public SwingFuture(String name) {
            super(name);
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            if (SwingUtilities.isEventDispatchThread()) {
                throw new IllegalStateException("One can not get a swing future result on the EventDispatchThread. (getNow can still be used.)");
            }
            return super.get();
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            if (SwingUtilities.isEventDispatchThread()) {
                throw new IllegalStateException("One can not get a swing future result on the EventDispatchThread. (getNow can still be used.)");
            }
            return super.get(timeout, unit);
        }

    }

    private static String createAssertErrorString(CRCLCommandType cmd, long id) {
        return "command id being reduced id="+id+", cmd="+CRCLSocket.cmdToString(cmd);
    }
    public static void setCommandID(CRCLCommandType cmd, long id) {
        assert cmd.getCommandID() <= id:
                createAssertErrorString(cmd,id);
        cmd.setCommandID(id);
    }
    
    private static class LogFileDirGetter {

        private static File logFileDir = createLogFileDir();

        private static File createLogFileDir() {
            try {
                File tmpTest = File.createTempFile("temp_test", "txt");
                File logFileDir = new File(tmpTest.getParentFile(), "aprs_logs_" + getDateTimeString());
                logFileDir.mkdirs();
                tmpTest.delete();
                return logFileDir;
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return null;
        }

        public File getLogFileDir() {
            return logFileDir;
        }
    }

    public static File getlogFileDir() {
        return new LogFileDirGetter().getLogFileDir();
    }

    
    private static String cleanAndLimitFilePrefix(String prefix_in) {
        if(prefix_in.length() > 80) {
            prefix_in = prefix_in.substring(0, 79);
        }
        String prefixOut = prefix_in.replaceAll("[ \t:;-]+", "_").replace('\\', '_').replace('/', '_');
        if(prefixOut.length() > 80) {
            prefixOut = prefixOut.substring(0, 79);
        }
        if(!prefixOut.endsWith("_")) {
            prefixOut = prefixOut + "_";
        }
        return prefixOut;
    }
    
    public static File createTempFile(String prefix, String suffix) throws IOException {
        return File.createTempFile(cleanAndLimitFilePrefix(Utils.getTimeString() + "_" + prefix), suffix, getlogFileDir());
    }

    public static File createTempFile(String prefix, String suffix, File dir) throws IOException {
        return File.createTempFile(cleanAndLimitFilePrefix(Utils.getTimeString() + "_" + prefix), suffix, dir);
    }
    
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HHmmss.SSS");
    
    public static String getDateTimeString() {
        Date date = new Date();
        return dateFormat.format(date);
    }

    private static final DateFormat timeFormat = new SimpleDateFormat("HHmmss.SSS");
    
    public static String getTimeString() {
        Date date = new Date();
        return timeFormat.format(date);
    }

    
    public static SwingFuture<Void> runOnDispatchThreadWithCatch(final RunnableWithThrow r) {
        SwingFuture<Void> ret = new SwingFuture<>("runOnDispatchThreadWithCatch");
        try {
            if (javax.swing.SwingUtilities.isEventDispatchThread()) {
                r.run();
                ret.complete(null);
                return ret;
            } else {

                javax.swing.SwingUtilities.invokeLater(() -> {
                    try {
                        r.run();
                        ret.complete(null);
                    } catch (Exception ex) {
                        ret.completeExceptionally(ex);
                        Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
                return ret;
            }
        } catch (Throwable exception) {

            ret.completeExceptionally(exception);
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, exception);
            return ret;
        }
    }

    public static SwingFuture<Void> runOnDispatchThread(final Runnable r) {
        return runOnDispatchThread("runOnDispatchThread", r);
    }

    public static SwingFuture<Void> runOnDispatchThread(String name, final Runnable r) {
        SwingFuture<Void> ret = new SwingFuture<>(name);
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            try {
                r.run();
                ret.complete(null);
            } catch (Exception e) {
                e.printStackTrace();
                ret.completeExceptionally(e);
            }

            return ret;
        } else {
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    r.run();
                    ret.complete(null);
                } catch (Exception e) {
                    e.printStackTrace();
                    ret.completeExceptionally(e);
                }
            });
            return ret;
        }
    }

    public static <R> SwingFuture<R> supplyOnDispatchThread(final Supplier<R> s) {
        SwingFuture<R> ret = new SwingFuture<>("supplyOnDispatchThread");
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            ret.complete(s.get());
            return ret;
        } else {
            javax.swing.SwingUtilities.invokeLater(() -> ret.complete(s.get()));
            return ret;
        }
    }

    private static <R> R unwrap(XFuture<R> f) {
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static <R> XFuture<R> composeOnDispatchThread(final Supplier<XFuture<R>> s) {
        XFuture<XFuture<R>> ret = new SwingFuture<>("composeOnDispatchThread");
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            return s.get();
        } else {
            javax.swing.SwingUtilities.invokeLater(() -> ret.complete(s.get()));
            return ret.thenApply(f -> unwrap(f));
        }
    }

    public static void autoResizeTableColWidths(JTable table) {

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

    static public void autoResizeTableRowHeights(JTable table) {
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

    public static void saveProperties(File file, Properties props) {
        List<String> names = new ArrayList<String>();
        for (Object key : props.keySet()) {
            names.add(key.toString());
        }
        Collections.sort(names);
        StackTraceElement ste[] = Thread.currentThread().getStackTrace();
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            if (ste.length > 2) {
                pw.println("#  Automatically saved by " + ste[2].getClassName() + "." + ste[2].getMethodName() + "() at " + ste[2].getFileName() + ":" + ste[2].getLineNumber());
            }
            for (int i = 0; i < names.size(); i++) {
                String name = names.get(i);
                String value = props.getProperty(name);
                value = value.replaceAll("\\\\", Matcher.quoteReplacement("\\\\"));
                pw.println(name + "=" + value);
            }
//            props.store(pw, "");
        } catch (IOException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void saveJTable(File f, JTable jtable) throws IOException {
        try (CSVPrinter printer = new CSVPrinter(new PrintStream(new FileOutputStream(f)), CSVFormat.DEFAULT)) {
            TableModel tm = jtable.getModel();
            List<String> colNameList = new ArrayList<>();
            for (int i = 0; i < tm.getColumnCount(); i++) {
                colNameList.add(tm.getColumnName(i));
            }
            printer.printRecord(colNameList);
            for (int i = 0; i < tm.getRowCount(); i++) {
                List<Object> l = new ArrayList<>();
                for (int j = 0; j < tm.getColumnCount(); j++) {
                    Object o = tm.getValueAt(i, j);
                    if (o instanceof File) {
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
                printer.printRecord(l);
            }
        }
    }
    
    static public String runTimeToString(long runningTimeMillis) {
        long runningTimeSecondsTotal = runningTimeMillis / 1000;
        long runningTimeHours = runningTimeSecondsTotal / 3600;
        long runningTimeMinutes = (runningTimeSecondsTotal % 3600) / 60;
        long runningTimeSeconds = (runningTimeSecondsTotal % 60);
        String s = String.format("%02d:%02d:%02d", runningTimeHours, runningTimeMinutes, runningTimeSeconds) + " (" + runningTimeSecondsTotal + " Total Seconds)";
        return s;
    }

//    public static void main(String[] args) {
//
//        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
//        //get current date time with Date()
//        Date date = new Date();
//        System.out.println(dateFormat.format(date));
//
//        //get current date time with Calendar()
//        Calendar cal = Calendar.getInstance();
//        System.out.println(dateFormat.format(cal.getTime()));
//    }
}
