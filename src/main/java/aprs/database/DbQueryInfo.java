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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Class of database query information needed to make a particular query through
 * the DatabasePoseUpdater or QuerySet taken from text resource files. These
 * text resource files allow us to change the underlying database without
 * needing to change the client java code.
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class DbQueryInfo {

    private final String query;
    private final DbParamTypeEnum params[];
    private final Map<DbParamTypeEnum, String> results;
    private Map<DbParamTypeEnum, Integer> paramPosMap;
    private final String origText;
    
    private final @Nullable
    String resourceName;

    /**
     * Constructor to create instance from data taken from text resource file.
     *
     * @param query query template taken from resource file.
     * @param params parameter array info parsed from resource file
     * @param results results map parsed from resource file
     * @param origText complete original text from the file (for display only)
     * @param resourceName resource name where info was read
     */
    public DbQueryInfo(String query,
            DbParamTypeEnum[] params,
            Map<DbParamTypeEnum, String> results,
            String origText,
            @Nullable String  resourceName) {
        this.query = query;
        this.params = params;
        this.results = results;
        this.origText = origText;
        paramPosMap = new EnumMap<>(DbParamTypeEnum.class);
        for (int i = 0; i < params.length; i++) {
            paramPosMap.put(params[i], i + 1);
        }
        this.resourceName = resourceName;
    }

    /**
     * Get the original text from the corresponding text resource file with the
     * query template and parameter and result info. Used only for display.
     *
     * @return original text
     */
    public String getOrigText() {
        return origText;
    }

    /**
     * Gets a mapping from the APRS specific but database and query independent
     * parameter types to the names/indexes in the results of a particular query
     * for the current database.
     *
     * @return map of parameter types to name/index strings.
     */
    public Map<DbParamTypeEnum, String> getResults() {
        return results;
    }

    /**
     * Gets a mapping from the APRS specific but database and query independent
     * parameter types to the indexes in the parameters of a particular query
     * for the current database.
     *
     * @return map of parameter types to indexes.
     */
    public Map<DbParamTypeEnum, Integer> getParamPosMap() {
        if (null == paramPosMap && null != params) {
            paramPosMap = new EnumMap<>(DbParamTypeEnum.class);
            for (int i = 0; i < params.length; i++) {
                paramPosMap.put(params[i], i + 1);
            }
        }
        return paramPosMap;
    }

    /**
     * Query template string with the query to be sent to the database.
     * Parameter values are not filled in but use '?' or "{1}","{2}" etc
     * appropriate to the current database.
     *
     * @return query template string.
     */
    public String getQuery() {
        return query;
    }

    /**
     * Get array of parameter types needed for this query.
     *
     * @return array of parameter types.
     */
    public DbParamTypeEnum[] getParams() {
        return params;
    }

    /**
     * Parse a string taken from a text resource file and create a new
     * DbQueryInfo instance.
     *
     * @param s string from resource file.
     * @return new instance;
     */
    public static DbQueryInfo parse(String s, @Nullable String  resourceName) {
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
        if (null == results) {
            results = Collections.emptyMap();
        }
        return new DbQueryInfo(sb.toString(), ta, results, s, resourceName);
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
        for (String aPa : pa) {
            int eqindex = aPa.indexOf('=');
            if (eqindex < 1) {
                continue;
            }
            String name = aPa.substring(0, eqindex).trim();
            String value = aPa.substring(eqindex + 1).trim();
            DbParamTypeEnum type = DbParamTypeEnum.valueOf(name);
            results.put(type, value);
        }
        return results;
    }

    public @Nullable String getResourceName() {
        return resourceName;
    }

    
    @Override
    public String toString() {
        return "\nDbQueryInfo{\n" + "\t\tquery=" + query + ",\n\t\tparams=" + Arrays.toString(params) + ",\n\t\tresults=" + results + ",\n\t\tparamPosMap=" + paramPosMap + ",\n\t\torigText=" + origText + ",\n\t\tresourceName=" + resourceName + "\n}\n";
    }

}
