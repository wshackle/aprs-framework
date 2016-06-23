/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aprs.framework;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public interface ActionsToCrclDisplayInterface extends DisplayInterface {

    public void browseActionsFile() throws IOException;

    public List<PddlAction> getActionsList();

    public void setActionsList(List<PddlAction> actionsList);

    public void addAction(PddlAction action);

    public void autoResizeTableColWidthsPddlOutput();

    public boolean isLoadEnabled();

    public void setLoadEnabled(boolean enable);

}
