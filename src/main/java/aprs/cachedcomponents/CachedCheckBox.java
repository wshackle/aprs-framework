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

import crcl.ui.XFutureVoid;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class CachedCheckBox extends CachedComponentBase {

    // NOTE AbstractButton is a convenient superclass of both
    // JCheckBox and JCheckBoxMenuItem with all the methods needed.
    @Nullable
    private final AbstractButton abstractButton;
    private volatile boolean selected;
    private volatile boolean enabled;

    private final AtomicInteger actionCount = new AtomicInteger();

    private final ActionListener actionListener = new ActionListener() {
        @Override
        @UIEffect
        public void actionPerformed(ActionEvent e) {
            actionCount.incrementAndGet();
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
    public CachedCheckBox(JCheckBox checkBox) {
        this.abstractButton = checkBox;
        selected = checkBox.isSelected();
        enabled = checkBox.isEnabled();
        checkBox.addActionListener(actionListener);
    }

    @UIEffect
    public CachedCheckBox(JCheckBoxMenuItem checkBoxMenuItem) {
        this.abstractButton = checkBoxMenuItem;
        selected = checkBoxMenuItem.isSelected();
        enabled = checkBoxMenuItem.isEnabled();
        checkBoxMenuItem.addActionListener(actionListener);
    }

//    @SuppressWarnings("guieffect")
//    private void checkMatch() {
//        if (null != abstractButton) {
//            int dc = getDispatchCount();
//            int sc = getStartCount();
//            int fc = getFinishCount();
//            int ac = actionCount.get();
//            boolean abstractButtonIsSelected = abstractButton.isSelected();
//            if (selected != abstractButtonIsSelected && (dc <= fc)) {
//                throw new IllegalStateException("selected=" + selected + ", abstractButtonIsSelected=" + abstractButtonIsSelected + ",dispatchCount=" + dc + ", startCount=" + sc + ", finishCount=" + fc + ", actionCount=" + ac);
//            }
//        }
//    }

    public boolean isSelected() {
//        checkMatch();
        return selected;
    }

    public XFutureVoid setSelected(boolean newSelectedVal) {
        boolean oldSelectedVal = this.selected;
        this.selected = newSelectedVal;
        if (null != abstractButton && newSelectedVal != oldSelectedVal) {
            return runOnDispatchThread(() -> setSelectedOnDisplay(newSelectedVal));
        } else {
            return XFutureVoid.completedFuture();
        }
    }

    @UIEffect
    private void setSelectedOnDisplay(boolean selected) {
        if (selected != this.selected) {
            int dc = getDispatchCount();
            int sc = getStartCount();
            int fc = getFinishCount();
            int ac = actionCount.get();
            throw new IllegalStateException("selected=" + selected + ", this.selected=" + this.selected + ",dispatchCount=" + dc + ", startCount=" + sc + ", finishCount=" + fc + ", actionCount=" + ac);
        }
        if (null != abstractButton && selected != abstractButton.isSelected()) {
            abstractButton.setSelected(selected);
        }
//        checkMatch();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (null != abstractButton && enabled != this.enabled) {
            runOnDispatchThread(() -> setEnabledOnDisplay(enabled));
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
