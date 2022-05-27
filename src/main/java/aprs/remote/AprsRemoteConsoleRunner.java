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
package aprs.remote;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class AprsRemoteConsoleRunner implements AutoCloseable, Runnable {

    private final InputStream in;
    private final OutputStream out;
    private volatile boolean closing = false;
    private Map<String, Scriptable<?>> scriptablesMap = new HashMap<>();

    public AprsRemoteConsoleRunner(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }

    public AprsRemoteConsoleRunner(InputStream in, OutputStream out, Map<String, Scriptable<?>> mapIn) {
        this(in, out);
        this.scriptablesMap.putAll(mapIn);
    }

    @Override
    public void run() {

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out));
        pw.print("\r\nRunning:  " + this.getClass() + " from "+ this.getClass().getProtectionDomain()+"\r\n\r\n");
        pw.print("Commands are of the form:\r\n");
        pw.print("newvariable = oldvariable method arg1 arg2  ...\r\n");
        pw.print("\r\nOR omit \"newvariable=\" and variable name will be automatically generated. :\r\n");
        pw.print("oldvariable method arg1 arg2  ...\r\n");
        pw.print("\r\nUse \".\" as an alias for the last variable set.\r\n");
        pw.print("\r\n");
        pw.print("\r\n");
        pw.print("!!END START PREFIX\r\n");
        pw.print("----------------------------------------------------------\r\n");
        pw.flush();
        String line = null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))){
            int lineNo = 0;
            String lastVar = null;
            String origLine = "";
            while (null != (line = br.readLine()) && !closing && !Thread.currentThread().isInterrupted()) {
                try {
//                    System.out.println("line = " + line);

                    lineNo++;
//                    pw.print("line = " + line + "\r\n");
                    line = line.trim();
                    origLine = line;
                    int eqIndex = line.indexOf('=');
                    final String varToSet;
                    if (eqIndex > 0 && eqIndex < line.length()) {
                        varToSet = line.substring(0, eqIndex).trim();
                        if (varToSet.startsWith(".")) {
                            pw.print("Variable name " + varToSet + " not allowed.\r\n");
                            continue;
                        }
                        line = line.substring(eqIndex + 1).trim();
                    } else {
                        varToSet = "var" + lineNo;
                    }
                    if (line.startsWith(".") && null != lastVar) {
                        String lastVarBase = lastVar;
                        int uindex = lastVarBase.lastIndexOf("_");
                        if (uindex > 0) {
                            lastVarBase = lastVarBase.substring(0, uindex);
                        }
                        line = lastVar + " " + line.substring(1).trim();
                    }
                    String qsplit[] = line.split("\"");
                    List<String> fieldsList = new ArrayList<>();
                    for (int i = 0; i < qsplit.length; i++) {
                        String string = qsplit[i].trim();
                        if (i % 2 == 0) {
                            String fields[] = string.split("[ ,\t\r\n]+", 6);
//                            pw.print("fields.length = " + fields.length + "\r\n");
//                            pw.print("fields=" + Arrays.toString(fields) + "\r\n");
                            fieldsList.addAll(Arrays.asList(fields));
                        } else {
                            fieldsList.add(string);
                        }
                    }
//                    pw.print("varToSet = " + varToSet + "\r\n");
                    String fields[] = fieldsList.toArray(new String[0]);
//                    pw.print("fields.length = " + fields.length + "\r\n");
//                    pw.print("fields=" + Arrays.toString(fields) + "\r\n");
                    if (fields.length < 1) {
                        List<String> keyList = new ArrayList<>(scriptablesMap.keySet());
                        Collections.sort(keyList);
                        pw.print("Try print or list and then one of these :  " + keyList + "\r\n");
                    } else if (fields[0].equalsIgnoreCase("print")) {
                        for (int i = 1; i < fields.length; i++) {
                            String field = fields[i];
                            if (field.equals(".") && null != lastVar) {
                                field = lastVar;
                            }
                            pw.print(field + " = " + scriptablesMap.get(field) + "\r\n");
                        }
                        if (fields.length < 2) {
                            List<String> keyList = new ArrayList<>(scriptablesMap.keySet());
                            Collections.sort(keyList);
                            for (String key : keyList) {
                                pw.print(key + " = " + scriptablesMap.get(key) + "\r\n");
                            }
                        }
                    } else if (fields[0].equalsIgnoreCase("list")) {
                        for (int i = 1; i < fields.length; i++) {
                            String field = fields[i];
                            if (field.equals(".") && null != lastVar) {
                                field = lastVar;
                            }
                            final Scriptable<?> scriptable = scriptablesMap.get(field);
                            if (null != scriptable) {
                                pw.print(field + " = " + scriptable.toVerboseString() + "\r\n");
                            }
                        }
                        if (fields.length < 2) {
                            List<String> keyList = new ArrayList<>(scriptablesMap.keySet());
                            Collections.sort(keyList);
                            for (String key : keyList) {
                                final Scriptable<?> scriptable = scriptablesMap.get(key);
                                if (null != scriptable) {
                                    pw.print(key + " = " + scriptable.toVerboseString() + "\r\n");
                                }
                            }
                        }
                    } else {
                        Scriptable<?> scriptable = scriptablesMap.get(fields[0]);
                        if (null == scriptable) {
                            pw.print("\"" + fields[0] + "\" is not defined\r\n");
                            List<String> keyList = new ArrayList<>(scriptablesMap.keySet());
                            Collections.sort(keyList);
                            pw.print("Try one of these:  " + keyList + "\r\n");
                        } else {
                            if (fields.length < 2) {
                                pw.print(fields[0] + " = " + scriptable + "\r\n");
                                pw.print("Try \"list " + fields[0] + "\" to see commands for this object. \r\n");
                            } else {
                                int feilds_to_remove = 2;
                                final String[] remainingFields
                                        = removeFields(fields, feilds_to_remove);
                                final @Nullable Object[] replacedFields = new Object[remainingFields.length];
                                for (int i = 0; i < remainingFields.length; i++) {
                                    String remainingField = remainingFields[i];
                                    if (remainingField.equals(".") && lastVar != null) {
                                        remainingField = lastVar;
                                    }
                                    if (scriptablesMap.containsKey(remainingField)) {
                                        replacedFields[i] = scriptablesMap.get(remainingField).getObj();
                                    } else {
                                        replacedFields[i] = remainingField;
                                    }
                                }
                                if (null == varToSet || scriptable.hasAction(fields[1])) {
                                    scriptable.action(fields[1], replacedFields, pw);
                                } else {
                                    Scriptable<?> varToSetValue = scriptable.function(fields[1], replacedFields, pw);
                                    if (null != varToSetValue) {
                                        this.scriptablesMap.put(varToSet, varToSetValue);
                                        pw.print(varToSet + " = " + varToSetValue + "\r\n");
                                        lastVar = varToSet;
                                    } else {
                                        pw.print("Null value returned by " + fields[1] + "\r\n");
                                        if (null != lastVar) {
                                            pw.print("LastVar: " + lastVar + " = " + this.scriptablesMap.get(lastVar) + "\r\n");
                                        } else {
                                            pw.print("LastVar: null"); 
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    Logger.getLogger(AprsRemoteConsoleRunner.class.getName()).log(Level.SEVERE, null, ex);
                    pw.print("Exception : " + ex + "\r\n");
                } finally {
                    pw.print("\r\n");
                    pw.print("!!END RESPONSE TO line number=" + lineNo + " line=" + origLine + "\r\n");
                    pw.print("----------------------------------------------------------\r\n");
                    pw.flush();
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(AprsRemoteConsoleRunner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @SuppressWarnings("nullness")
    private static String[] removeFields(String[] fields, int feilds_to_remove) {
        return (fields.length < (feilds_to_remove + 1))
                ? new String[]{}
                : Arrays.copyOfRange(fields, feilds_to_remove, fields.length);
    }

    @Override
    public void close() throws Exception {
        closing = true;
        in.close();
        out.close();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("AprsRemoteConsoleService.main: args.length = " + args.length);
        System.out.println("AprsRemoteConsoleService.main: args = " + Arrays.toString(args));
        if (args.length < 1) {
            try (AprsRemoteConsoleRunner svc = new AprsRemoteConsoleRunner(System.in, System.out)) {
                svc.run();
            }
        } else {
            int port = Integer.parseInt(args[0]);
            try (AprsRemoteConsoleServerSocket socket = new AprsRemoteConsoleServerSocket(port)) {
                socket.run();
            }
        }
    }

}
