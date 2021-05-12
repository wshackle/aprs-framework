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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author will
 */
public class DbCsvBackup {

    public static final boolean debug = false;

    static public ResultSet executeQuery(@Nullable PreparedStatement preparedStatement,@Nullable String simQuery, String name, String taskName, boolean replace) throws SQLException, IOException {
        File homeDir = new File(System.getProperty("user.home"));
        File queriesDir = new File(homeDir, "aprsQueries");
        File sysQueriesDir = new File(queriesDir, taskName.replace(' ', '_'));
        File dir = new File(sysQueriesDir, name);
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        final File resultsFile;
        final File queryFile;
        final File metaFile;
        if (null == simQuery || simQuery.length() < 2) {
            File fa[] = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith("_query.txt");
                }
            });
            if (fa != null && fa.length == 1 && fa[0].exists()) {
                queryFile = fa[0];
                String prefix = queryFile.getName().substring(0, queryFile.getName().length() - "_query.txt".length());
                resultsFile = new File(dir, prefix + "_results.csv");
                metaFile = new File(dir, prefix + "_meta.csv");
            } else {
                throw new RuntimeException("simQuery=" + simQuery + ",dir=" + dir);
            }
        } else {
            final int simQueryHashCode = simQuery.hashCode();
            resultsFile
                    = new File(dir, simQueryHashCode + "_results.csv");
            queryFile
                    = new File(dir, simQueryHashCode + "_query.txt");
            metaFile
                    = new File(dir, simQueryHashCode + "_meta.csv");
        }

        if (debug) {
            System.out.println("");
            System.out.flush();
            System.err.println("");
            System.err.flush();
            Thread.dumpStack();
            System.out.println("");
            System.out.flush();
            System.err.println("");
            System.err.flush();
            System.out.println("resultsFile = " + resultsFile + ", exists = " + resultsFile.exists());
            System.out.println("queryFile = " + queryFile + ", exists = " + queryFile.exists());
            System.out.println("metaFile = " + metaFile + ", exists = " + metaFile.exists());
        }
        if (!resultsFile.exists() || !queryFile.exists() || !metaFile.exists()) {
            System.out.println("!resultsFile.exists() || !queryFile.exists() || !metaFile.exists()");
            if (null == preparedStatement) {
                throw new RuntimeException("preparedStatement=" + preparedStatement + ",resultsFile=" + resultsFile + ",resultsFile.exists()=" + resultsFile.exists() + ",queryFile=" + queryFile + ",queryFile.exists()=" + queryFile.exists() + ",metaFile=" + metaFile + ",metaFile.exists()=" + metaFile.exists());
            }
            final String simQuery2 = simQuery;
            if(null == simQuery2) {
                throw new NullPointerException("simQuery2 can only be null if resultsFile="+resultsFile+", queryFile="+queryFile+", and metaFile="+metaFile+" exist. : name="+name+", taskName="+taskName);
            }
            return executeAndSaveQuery(preparedStatement, resultsFile, simQuery2, queryFile, metaFile);
        } else if (replace) {
            return readResultsSetCsv(resultsFile, metaFile);
        } else {
            final String simQuery3 = simQuery;
            if(null == simQuery3) {
                throw new NullPointerException("simQuery3 can only be null when replace is true : name="+name+", taskName="+taskName);
            }
            try (BufferedReader br = new BufferedReader(new FileReader(queryFile))) {
                String line;
                StringBuilder fileBuf = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    final String lineTrim = line.trim();
                    if (lineTrim.length() > 0) {
                        fileBuf.append(lineTrim).append("\n");
                    }
                }
                StringBuilder simBuf = new StringBuilder();
                String simLines[] = simQuery3.split("[\r\n]+");
                for (int i = 0; i < simLines.length; i++) {
                    String simLine = simLines[i];
                    final String simLineTrim = simLine.trim();
                    if (simLineTrim.length() > 0) {
                        simBuf.append(simLineTrim).append("\n");
                    }
                }
                final String trimmedSim = simBuf.toString().trim();
                final String trimmedFileLine = fileBuf.toString().trim();
                if (!trimmedFileLine.equals(trimmedSim)) {
                    if (debug) {
                        System.out.println("!trimmedFileLine.equals(trimmedSim)");
                        System.out.println("line = " + line);
                        System.out.println("simQuery = " + simQuery3);
                        System.out.println("trimmedFileLine = " + trimmedFileLine);
                        System.out.println("trimmedSim = " + trimmedSim);
                    }
                    if (null == preparedStatement) {
                        throw new RuntimeException("preparedStatement=" + preparedStatement + ",resultsFile=" + resultsFile + ",queryFile=" + queryFile + ",metaFile=" + metaFile + ",trimmedFileLine=" + trimmedFileLine + ",trimmedSim=" + trimmedSim);
                    }
                    return executeAndSaveQuery(preparedStatement, resultsFile, simQuery3, queryFile, metaFile);
                } else {
                    if (null == preparedStatement) {
                        throw new RuntimeException("preparedStatement=" + preparedStatement + ",resultsFile=" + resultsFile + ",queryFile=" + queryFile + ",metaFile=" + metaFile + ",trimmedFileLine=" + trimmedFileLine + ",trimmedSim=" + trimmedSim);
                    }
                    return preparedStatement.executeQuery();
                }
            } finally {
                if (debug) {
                    System.out.println("");
                    System.out.flush();
                    System.err.println("");
                    System.err.flush();
                }
            }
        }
    }

    private static ResultSet executeAndSaveQuery(PreparedStatement preparedStatement, File resultsFile, String simQuery, File queryFile, File metaFile) throws IOException, SQLException {
        try (PrintWriter pw = new PrintWriter(queryFile)) {
            pw.println(simQuery.trim());
        }
        ResultSet rs = preparedStatement.executeQuery();
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(resultsFile), CSVFormat.DEFAULT.withHeader(rs))) {
            printer.printRecords(rs);
        }
        String metaHeaders[] = new String[]{"index", "name", "type"};
        final ResultSetMetaData metaData = rs.getMetaData();
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(metaFile), CSVFormat.DEFAULT.withHeader(metaHeaders))) {
            if (metaData.getColumnCount() < 1) {
                throw new SQLException("metaData=" + metaData + ", metaData.getColumnCount()=" + metaData.getColumnCount());
            }
            for (int i = 0; i < metaData.getColumnCount(); i++) {
                printer.printRecord(new Object[]{i + 1, metaData.getColumnName(i + 1), metaData.getColumnTypeName(i + 1)});
            }
        }
        rs = preparedStatement.executeQuery();
        return rs;
    }

    private static ResultSet readResultsSetCsv(File resultsFile, File metaFile) throws IOException {
        if (debug) {
            System.out.println(" readResultsSetCsv metaFile = " + metaFile);
        }
        final ResultSetMetaData metaData;
        try (CSVParser parser = new CSVParser(new FileReader(metaFile), CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            if (debug) {
                System.out.println("parser = " + parser);
                System.out.println("parser.getHeaderMap() = " + parser.getHeaderMap());
            }
            final List<CSVRecord> parserRecords = parser.getRecords();
            if (debug) {
                System.out.println("parserRecords = " + parserRecords);
                System.out.println("parserRecords.size() = " + parserRecords.size());
            }
            metaData = new DbCsvBackupResultSetMetaData(parserRecords);
        }
        System.out.println(" readResultsSetCsv resultsFile = " + resultsFile);
        try (CSVParser parser = new CSVParser(new FileReader(resultsFile), CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            if (debug) {
                System.out.println("parser = " + parser);
                System.out.println("parser.getHeaderMap() = " + parser.getHeaderMap());
            }
            final List<CSVRecord> parserRecords = parser.getRecords();
            ResultSet rs
                    = new DbCsvBackupResultSet(parserRecords, metaData);
            return rs;
        }
    }

}
