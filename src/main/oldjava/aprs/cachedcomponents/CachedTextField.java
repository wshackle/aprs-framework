package aprs.cachedcomponents;

import aprs.misc.Utils;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.guieffect.qual.UIType;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CachedTextField {

    private final JTextField jTextField;
    private volatile  String text;

    @UIType
    private class CachedTextFieldActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            text = jTextField.getText();
        }
    }

    public CachedTextField(JTextField textComponent) {
        this.jTextField = textComponent;
        this.text = textComponent.getText();
        textComponent.addActionListener(new CachedTextFieldActionListener());
    }

    public String getText() {
        return text;
    }

    public void setText(String newText) {
        this.text = newText;
        Utils.runOnDispatchThread(() -> setTextComponentText(newText));
    }

    @UIEffect
    private void setTextComponentText(String newText) {
        jTextField.setText(newText);
    }

}
