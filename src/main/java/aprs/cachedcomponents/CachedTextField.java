package aprs.cachedcomponents;

import aprs.misc.Utils;
import static aprs.misc.Utils.isEventDispatchThread;
import crcl.ui.XFutureVoid;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.guieffect.qual.UIType;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.checkerframework.checker.guieffect.qual.UI;

public class CachedTextField extends CachedComponentBase {

    private final JTextField jTextField;
    private volatile String text;

    private final AtomicInteger actionCount = new AtomicInteger();

    @UIType
    private class CachedTextFieldActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            actionCount.incrementAndGet();
            syncText();
        }
    }

    private final CachedTextFieldActionListener actionListener
            = new CachedTextFieldActionListener();
    
    
    @UIEffect
    public final void syncText() {
        text = jTextField.getText();
    }

//    @SuppressWarnings("guieffect")
//    private void checkMatch() {
//        String textCopy = text;
//        String jTextFieldCopy = jTextField.getText();
//        int dc = getDispatchCount();
//        int sc = getStartCount();
//        int fc = getFinishCount();
//        int ac = actionCount.get();
//        if (!Objects.equals(textCopy, jTextFieldCopy) && (dc <= fc)) {
//            throw new IllegalStateException("text=" + textCopy + ", jTextField.getText()=" + jTextFieldCopy + ",dispatchCount=" + dc + ", startCount=" + sc + ", finishCount=" + fc + ", actionCount=" + ac);
//        }
//    }

    @UIEffect
    public CachedTextField(JTextField textField) {
        this.jTextField = textField;
        this.text = textField.getText();
        textField.addActionListener(actionListener);
    }

    public String getText() {
//        checkMatch();
        return text;
    }

    public XFutureVoid setText(String newText) {
        this.text = newText;
        return runOnDispatchThread(() -> setTextComponentText(newText));
    }

    private static final Logger LOGGER = Logger.getLogger(CachedTextField.class.getName());

    @UIEffect
    private void setTextComponentText(String newText) {
        jTextField.setText(newText);
//        checkMatch();
    }

}
