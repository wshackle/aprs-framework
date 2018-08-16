package aprs.cachedcomponents;

import aprs.misc.Utils;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.Array;
import java.util.Objects;

public class CachedComboBox <E>  {

    private  final JComboBox<E> comboBox;
    private  final Class<E> eClass;

    private final ListDataListener listDataListener = new ListDataListener() {
        @Override
        @UIEffect
        public void intervalAdded(ListDataEvent e) {
            syncUiToCache();
        }

        @Override
        @UIEffect
        public void intervalRemoved(ListDataEvent e) {
            syncUiToCache();
        }

        @Override
        @UIEffect
        public void contentsChanged(ListDataEvent e) {
            syncUiToCache();
        }
    };

    private  final ItemListener itemListener = new ItemListener() {

        @Override
        @UIEffect
        public void itemStateChanged(ItemEvent e) {
            switch (e.getStateChange()) {
                case ItemEvent.SELECTED:
                    selectedIndex = comboBox.getSelectedIndex();
                    break;

                case ItemEvent.DESELECTED:
                    selectedIndex = -1;
            }
        }
    };

    private volatile  E items[];
    private volatile  int selectedIndex;

    @UIEffect
    @SuppressWarnings("unchecked")
    private synchronized void syncUiToCache() {
        ComboBoxModel<E> model = comboBox.getModel();
        int size = model.getSize();
        E newItems[] = (E[]) Array.newInstance(eClass,size);
        for (int i = 0; i < size; i++) {
            newItems[i] = model.getElementAt(i);
        }
        this.items = newItems;
        this.selectedIndex = comboBox.getSelectedIndex();
    }

    @UIEffect
    @SuppressWarnings("initialized")
    public CachedComboBox(Class<E> eClass, JComboBox<E> comboBox) {
        this.comboBox = comboBox;
        this.eClass = eClass;
        syncUiToCache();
        ComboBoxModel<E> model = comboBox.getModel();
        model.addListDataListener(listDataListener);
        comboBox.addItemListener(itemListener);
    }

    public E[] getItems() {
        return  items;
    }

    @SuppressWarnings("unchecked")
    public void setItems(E[] inItems) {
        E newItems[] = (E[]) Array.newInstance(eClass,inItems.length);
        System.arraycopy(inItems,0,newItems,0,inItems.length);
        synchronized (this) {
            this.items = newItems;
            this.selectedIndex = -1;
        }
        Utils.runOnDispatchThread(() -> combobBoxSetItems(inItems));
    }

    @UIEffect
    private void combobBoxSetItems(E[] inItems) {
        comboBox.removeAllItems();
        for (int i = 0; i < inItems.length; i++) {
            comboBox.addItem(inItems[i]);
        }
    }

    public void setSelectedItem(@Nullable  E item) {
        if(null == item) {
            selectedIndex=-1;
        } else {
            synchronized (this) {
                for (int i = 0; i < items.length; i++) {
                    if(Objects.equals(items[i],item)) {
                        selectedIndex = i;
                        break;
                    }
                }
            }
        }
        Utils.runOnDispatchThread(() -> comboBoxSetSelectedItem(item));
    }

    public E getElementAt(int index) {
        return items[index];
    }

    @SuppressWarnings("unchecked")
    public void insertElementAt(E element, int index) {
        synchronized (this) {
            if(index < 0 || index > items.length) {
                throw new IllegalArgumentException("index = "+index+", items.length="+items.length);
            }
            E newItems[] = (E[]) Array.newInstance(eClass,items.length+1);
            for (int i = 0; i < index && i < items.length ; i++) {
                newItems[i] = items[i];
            }
            newItems[index] = element;
            for (int i = index+1; i < newItems.length; i++) {
                newItems[i] = items[i-1];
            }
            if(selectedIndex > index) {
                selectedIndex++;
            }
            this.items = newItems;
        }
        Utils.runOnDispatchThread(() -> comboBoxInsertItemAt(element,index));
    }

    @SuppressWarnings("unchecked")
    public void addElement(E element) {
        synchronized (this) {
            E newItems[] = (E[]) Array.newInstance(eClass,items.length+1);
            for (int i = 0;  i < items.length ; i++) {
                newItems[i] = items[i];
            }
            newItems[items.length] = element;
            this.items = newItems;
        }
        Utils.runOnDispatchThread(() -> comboBoxAddElment(element));
    }

    @UIEffect
    private void comboBoxAddElment(E element) {
        comboBox.addItem(element);
    }

    @UIEffect
    private void comboBoxInsertItemAt(E element, int index) {
        comboBox.insertItemAt(element,index);
    }

    public  int getSize() {
        return items.length;
    }

    @SuppressWarnings("unchecked")
    public void removeAllElements() {
        E newItems[] = (E[]) Array.newInstance(eClass,0);
        synchronized (this) {
            this.items = newItems;
            this.selectedIndex = -1;
        }
        Utils.runOnDispatchThread(this::comboBoxRemoveAllElements);
    }


    @UIEffect
    private void comboBoxRemoveAllElements() {
        comboBox.removeAllItems();
    }

    @UIEffect
    private  void comboBoxSetSelectedItem(@Nullable  E item) {
        if(null != item) {
            comboBox.setSelectedItem(item);
        }
    }

    @Nullable
    public E getSelectedItem() {
        if(selectedIndex < 0 || selectedIndex > items.length) {
            return null;
        }
        return items[selectedIndex];
    }

}
