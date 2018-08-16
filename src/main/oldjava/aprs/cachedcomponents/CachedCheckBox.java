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
package aprs.cachedcomponents;

import aprs.misc.Utils;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractButton;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.guieffect.qual.UIType;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class CachedCheckBox {

    @Nullable
    private final AbstractButton abstractButton;
    private volatile boolean selected;
    private volatile boolean enabled;

    private final ActionListener actionListener = new ActionListener() {
        @Override
        @UIEffect
        public void actionPerformed(ActionEvent e) {
            if (null != abstractButton) {
                selected = abstractButton.isSelected();
            }
        }
    };


    public CachedCheckBox() {
        this.abstractButton = null;
        selected = false;
        enabled = true;
    }

    @UIEffect
    public CachedCheckBox(AbstractButton abstractButton) {
        this.abstractButton = abstractButton;
        selected = abstractButton.isSelected();
        enabled = abstractButton.isEnabled();
        abstractButton.addActionListener(actionListener);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        if (null != abstractButton && selected != this.selected) {
            Utils.runOnDispatchThread(() -> setSelectedOnDisplay(selected));
        }
        this.selected = selected;
    }

    @UIEffect
    private void setSelectedOnDisplay(boolean selected) {
        this.selected = selected;
        if (null != abstractButton && selected != abstractButton.isSelected()) {
            abstractButton.setSelected(selected);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (null != abstractButton && enabled != this.enabled) {
            Utils.runOnDispatchThread(() -> setEnabledOnDisplay(enabled));
        }
        this.enabled = enabled;
    }

    @UIEffect
    private void setEnabledOnDisplay(boolean enabled) {
        this.enabled = enabled;
        if (null != abstractButton) {
            abstractButton.setEnabled(enabled);
        }
    }
}
