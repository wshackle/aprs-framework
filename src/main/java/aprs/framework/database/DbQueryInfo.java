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

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class DbQueryInfo {

    private final String query;
    private final DbParamTypeEnum params[];
    private final Map<DbParamTypeEnum, String> results;
    private @Nullable Map<DbParamTypeEnum, Integer> paramPosMap = null;
    private final String origText;

    public String getOrigText() {
        return origText;
    }

    public Map<DbParamTypeEnum, String> getResults() {
        return results;
    }

    @Nullable public Map<DbParamTypeEnum, Integer> getParamPosMap() {
        if (null == paramPosMap && null != params) {
            paramPosMap = new EnumMap<>(DbParamTypeEnum.class);
            for (int i = 0; i < params.length; i++) {
                paramPosMap.put(params[i], i + 1);
            }
        }
        return paramPosMap;
    }

    public DbQueryInfo(String query,
            DbParamTypeEnum[] params,
            Map<DbParamTypeEnum, String> results,
            String origText) {
        this.query = query;
        this.params = params;
        this.results = results;
        this.origText = origText;
    }

    public String getQuery() {
        return query;
    }

    public DbParamTypeEnum[] getParams() {
        return params;
    }

    public static DbQueryInfo parse(String s) {
        String lines[] = s.split("[\r\n]+");
        StringBuilder sb = new StringBuilder();
        DbParamTypeEnum[] ta = null;
        Map<DbParamTypeEnum, String> results = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().startsWith(PARAMS_PREFIX) && i == 0) {
                ta = parseParamLine(line);
            } else if (line.trim().startsWith(RESULTS_PREFIX) && i < 2) {
                results = parseResultsLine(line);
            } else {
                sb.append(line);
                sb.append('\n');
            }
        }
        if (null == ta) {
            ta = new DbParamTypeEnum[0];
        }
        if(null == results) {
            results = Collections.emptyMap();
        }
        return new DbQueryInfo(sb.toString(), ta, results, s);
    }
    private static final String PARAMS_PREFIX = "#params=";
    private static final String RESULTS_PREFIX = "#results=";

    private static DbParamTypeEnum[] parseParamLine(String paramLine) {
        int braceindex = paramLine.indexOf('[');
        int startindex;
        if (braceindex < PARAMS_PREFIX.length()) {
            startindex = PARAMS_PREFIX.length();
        } else {
            startindex = braceindex + 1;
        }
        int endindex = paramLine.lastIndexOf(']');
        if (endindex < startindex) {
            endindex = paramLine.length();
        }
        String parmString = paramLine.substring(startindex, endindex).trim();
        if (parmString.length() < 1) {
            return new DbParamTypeEnum[0];
        }
        parmString = parmString.replaceAll("[(][0-9]*[)]", "");
        parmString = parmString.replaceAll("[{][0-9]*[}]", "");
        String pa[] = parmString.split("[ \t,]+");
        DbParamTypeEnum ta[] = new DbParamTypeEnum[pa.length];
        for (int i = 0; i < pa.length; i++) {
            ta[i] = DbParamTypeEnum.valueOf(pa[i]);
        }
        return ta;
    }

    private static Map<DbParamTypeEnum, String> parseResultsLine(String line) {
        int braceindex = line.indexOf('{');
        int startindex;
        if (braceindex < RESULTS_PREFIX.length()) {
            startindex = RESULTS_PREFIX.length();
        } else {
            startindex = braceindex + 1;
        }
        int endindex = line.lastIndexOf('}');
        if (endindex < startindex) {
            endindex = line.length();
        }
        String parmString = line.substring(startindex, endindex);
        Map<DbParamTypeEnum, String> results = new EnumMap<>(DbParamTypeEnum.class);
        String pa[] = parmString.split(",");
        for (int i = 0; i < pa.length; i++) {
            int eqindex = pa[i].indexOf('=');
            if (eqindex < 1) {
                continue;
            }
            String name = pa[i].substring(0, eqindex).trim();
            String value = pa[i].substring(eqindex + 1).trim();
            DbParamTypeEnum type = DbParamTypeEnum.valueOf(name);
            results.put(type, value);
        }
//        System.out.println("results = " + results);
        return results;
    }
}
