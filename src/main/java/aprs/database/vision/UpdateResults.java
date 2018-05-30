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
package aprs.database.vision;

import aprs.database.PhysicalItem;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class UpdateResults {

    final String name;
    private @Nullable String updateStringFilled;
    private int updateCount;
    private int totalUpdateCount;
    private int statementExecutionCount;
    private @Nullable PhysicalItem lastDetectedItem;
    private double x;
    private double y;
    private double rotation;
    private @Nullable List<Map<String, String>> lastResultSetMapList;
    private @Nullable Exception exception;
    private boolean returnedResultSet;

    private @Nullable List<Map<String, String>> lastVerificationResultSetListMap;

    private @Nullable String verificationQueryStringFilled;

    /**
     * Get the value of verificationQueryStringFilled
     *
     * @return the value of verificationQueryStringFilled
     */
    @Nullable public String getVerificationQueryStringFilled() {
        return verificationQueryStringFilled;
    }

    /**
     * Set the value of verificationQueryStringFilled
     *
     * @param verificationQueryStringFilled new value of
     * verificationQueryStringFilled
     */
    public void setVerificationQueryStringFilled(String verificationQueryStringFilled) {
        this.verificationQueryStringFilled = verificationQueryStringFilled;
    }

    /**
     * Get the value of lastVerificationResultSetListMap
     *
     * @return the value of lastVerificationResultSetListMap
     */
    @Nullable public List<Map<String, String>> getLastVerificationResultSetListMap() {
        return lastVerificationResultSetListMap;
    }

    /**
     * Set the value of lastVerificationResultSetListMap
     *
     * @param lastVerificationResultSetListMap new value of
     * lastVerificationResultSetListMap
     */
    public void setLastVerificationResultSetListMap(List<Map<String, String>> lastVerificationResultSetListMap) {
        this.lastVerificationResultSetListMap = lastVerificationResultSetListMap;
    }

    private boolean verified;

    /**
     * Get the value of verified
     *
     * @return the value of verified
     */
    public boolean isVerified() {
        return verified;
    }

    /**
     * Set the value of verified
     *
     * @param verified new value of verified
     */
    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public UpdateResults(String name) {
        this.name = name;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getRotation() {
        return rotation;
    }

    /**
     * @return the updateStringFilled
     */
    @Nullable public String getUpdateStringFilled() {
        return updateStringFilled;
    }

    /**
     * @param updateStringFilled the updateStringFilled to set
     */
    public void setUpdateStringFilled(String updateStringFilled) {
        this.updateStringFilled = updateStringFilled;
    }

    /**
     * @return the updateCount
     */
    public int getUpdateCount() {
        return updateCount;
    }

    /**
     * @param updateCount the updateCount to set
     */
    public void setUpdateCount(int updateCount) {
        this.updateCount = updateCount;
    }

    /**
     * @return the totalUpdateCount
     */
    public int getTotalUpdateCount() {
        return totalUpdateCount;
    }

    /**
     * @param totalUpdateCount the totalUpdateCount to set
     */
    public void setTotalUpdateCount(int totalUpdateCount) {
        this.totalUpdateCount = totalUpdateCount;
    }

    /**
     * @return the statementExecutionCount
     */
    public int getStatementExecutionCount() {
        return statementExecutionCount;
    }

    public int incrementStatementExecutionCount() {
        statementExecutionCount++;
        return statementExecutionCount;
    }

    /**
     * @param statementExecutionCount the statementExecutionCount to set
     */
    public void setStatementExecutionCount(int statementExecutionCount) {
        this.statementExecutionCount = statementExecutionCount;
    }

    /**
     * @return the lastDetectedItem
     */
    @Nullable public PhysicalItem getLastDetectedItem() {
        return lastDetectedItem;
    }

    /**
     * @param lastDetectedItem the lastDetectedItem to set
     */
    public void setLastDetectedItem(PhysicalItem lastDetectedItem) {
        this.lastDetectedItem = lastDetectedItem;
        x = lastDetectedItem.x;
        y = lastDetectedItem.y;
        rotation = lastDetectedItem.getRotation();
    }

    /**
     * @return the lastResultSetMapList
     */
    @Nullable public List<Map<String, String>> getLastResultSetMapList() {
        return lastResultSetMapList;
    }

    /**
     * @param lastResultSetMapList the lastResultSetMapList to set
     */
    public void setLastResultSetMapList(List<Map<String, String>> lastResultSetMapList) {
        this.lastResultSetMapList = lastResultSetMapList;
    }

    @Override
    public String toString() {
        return "UpdateResults{"
                + "name= " + name
                + ",\n    updateStringFilled=\n" + updateStringFilled
                + "\n\nupdateCount=" + updateCount
                + ",\n    totalUpdateCount=" + totalUpdateCount
                + ",\n    statementExecutionCount=" + statementExecutionCount
                + ",\n    lastDetectedItem=" + lastDetectedItem
                + ",\n    x=" + x
                + ",\n    y=" + y
                + ",\n    rotation=" + rotation
                + ",\n    lastResultSetMapList=" + lastResultSetMapList
                + ",\n    exception=" + exception + ((null != exception && null != exception.getCause()) ? "\n caused by \n" + exception.getCause() + "\n" : "")
                + ",\n    returnedResultSet=" + returnedResultSet
                + ",\n    verificationQueryStringFilled=\n" + verificationQueryStringFilled
                + "\n\n    "+getVerifyExceptionString()
                + ",\n    verified=" + verified
                + ",\n    lastVerificationResultSetListMap=" + lastVerificationResultSetListMap
                + "\n}";
    }

    private String getVerifyExceptionString() {
        if(null == verifyException) {
           return "";
        }
        Throwable cause = verifyException.getCause();
        if(null == cause) {
            return "verifyException=" + verifyException;
        }
        return "verifyException=" + verifyException +  "\n caused by \n" + cause + "\n";
    }
    
    @Nullable private Exception verifyException = null;

    /**
     * Get the value of verifyException
     *
     * @return the value of verifyException
     */
    @Nullable public Exception getVerifyException() {
        return verifyException;
    }

    /**
     * Set the value of verifyException
     *
     * @param verifyException new value of verifyException
     */
    public void setVerifyException(@Nullable Exception verifyException) {
        this.verifyException = verifyException;
    }

    /**
     * @return the exception
     */
    @Nullable public Exception getException() {
        return exception;
    }

    /**
     * @param exception the exception to set
     */
    public void setException(@Nullable Exception exception) {
        this.exception = exception;
    }

    /**
     * @return the returnedResultSet
     */
    public boolean isReturnedResultSet() {
        return returnedResultSet;
    }

    /**
     * @param returnedResultSet the returnedResultSet to set
     */
    public void setReturnedResultSet(boolean returnedResultSet) {
        this.returnedResultSet = returnedResultSet;
    }

}
