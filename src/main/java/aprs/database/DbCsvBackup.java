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

/**
 *
 * @author will
 */
public class DbCsvBackup {

    public static boolean debug = false;

    static public ResultSet executeQuery(java.sql.PreparedStatement preparedStatement, String simQuery, String name, String taskName, boolean replace) throws SQLException, IOException {
        File homeDir = new File(System.getProperty("user.home"));
        File queriesDir = new File(homeDir, "aprsQueries");
        File sysQueriesDir = new File(queriesDir, taskName.replace(' ', '_'));
        File dir = new File(sysQueriesDir, name);
        dir.mkdirs();
        File resultsFile = new File(dir, simQuery.hashCode() + "_results.csv");
        File queryFile = new File(dir, simQuery.hashCode() + "_query.txt");
        File metaFile = new File(dir, simQuery.hashCode() + "_meta.csv");
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
//        if (resultsFile.exists()) {
//            return readResultsSetCsv(resultsFile);
//        }
        if (!resultsFile.exists() || !queryFile.exists() || !metaFile.exists()) {
            System.out.println("!resultsFile.exists() || !queryFile.exists() || !metaFile.exists()");
            return executeAndSaveQuery(preparedStatement, resultsFile, simQuery, queryFile, metaFile);
        } else {
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
                String simLines[] = simQuery.split("[\r\n]+");
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
                        System.out.println("simQuery = " + simQuery);
                        System.out.println("trimmedFileLine = " + trimmedFileLine);
                        System.out.println("trimmedSim = " + trimmedSim);
                    }
                    return executeAndSaveQuery(preparedStatement, resultsFile, simQuery, queryFile, metaFile);
                } else if (replace) {
                    return readResultsSetCsv(resultsFile, metaFile);
                } else {
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
