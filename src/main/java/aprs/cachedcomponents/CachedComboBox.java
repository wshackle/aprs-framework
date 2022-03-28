package aprs.cachedcomponents;

import aprs.misc.Utils;
import crcl.utils.XFutureVoid;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.Array;
import java.util.Objects;

public class CachedComboBox<E> {

    private final JComboBox<E> comboBox;
    private final Class<E> eClass;

    private final ListDataListener listDataListener;

    private final ItemListener itemListener = new ItemListener() {

        @Override
        @UIEffect
        @SuppressWarnings("nullness")
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

    private volatile E items[];
    private volatile int selectedIndex;

    public int getItemCount() {
        return items.length;
    }

    public E getItemAt(int index) {
        return items[index];
    }

    @UIEffect
    @SuppressWarnings({"unchecked", "initialization","nullness"})
    private synchronized void syncUiToCache(@Nullable ComboBoxModel<E> model) {
        assert model != null;
        int size = model.getSize();
        E newItems[] = (E[]) Array.newInstance(eClass, size);
        for (int i = 0; i < size; i++) {
            newItems[i] = model.getElementAt(i);
        }
        this.items = newItems;
        this.selectedIndex = comboBox.getSelectedIndex();
    }

    @UIEffect
    @SuppressWarnings({"nullness", "initialization"})
    public CachedComboBox(Class<E> eClass, JComboBox<E> comboBox) {
        this.comboBox = comboBox;
        this.eClass = eClass;
        ComboBoxModel<E> model = comboBox.getModel();
        assert model != null;
        syncUiToCache(model);
        listDataListener = new ListDataListener() {
            @Override
            @UIEffect
            public void intervalAdded(ListDataEvent e) {
                syncUiToCache(comboBox.getModel());
            }

            @Override
            @UIEffect
            public void intervalRemoved(ListDataEvent e) {
                syncUiToCache(comboBox.getModel());
            }

            @Override
            @UIEffect
            public void contentsChanged(ListDataEvent e) {
                syncUiToCache(comboBox.getModel());
            }
        };
        model.addListDataListener(listDataListener);
        comboBox.addItemListener(itemListener);

    }

    public E[] getItems() {
        return items;
    }

    @SuppressWarnings("unchecked")
    public void setItems(E[] inItems) {
        E newItems[] = (E[]) Array.newInstance(eClass, inItems.length);
        System.arraycopy(inItems, 0, newItems, 0, inItems.length);
        synchronized (this) {
            this.items = newItems;
            this.selectedIndex = -1;
        }
        Utils.runOnDispatchThread(() -> combobBoxSetItems(inItems));
    }

    @UIEffect
    public void setItemsOnDisplay(E[] inItems) {
        assert SwingUtilities.isEventDispatchThread();
        E newItems[] = (E[]) Array.newInstance(eClass, inItems.length);
        System.arraycopy(inItems, 0, newItems, 0, inItems.length);
        synchronized (this) {
            this.items = newItems;
            this.selectedIndex = -1;
        }
        combobBoxSetItems(inItems);
    }

    @UIEffect
    private void combobBoxSetItems(E[] inItems) {
        comboBox.removeAllItems();
        for (int i = 0; i < inItems.length; i++) {
            comboBox.addItem(inItems[i]);
        }
    }

    public XFutureVoid setSelectedItem(@Nullable E item) {
        if (null == item) {
            selectedIndex = -1;
        } else {
            synchronized (this) {
                for (int i = 0; i < items.length; i++) {
                    if (Objects.equals(items[i], item)) {
                        selectedIndex = i;
                        break;
                    }
                }
            }
        }
        return Utils.runOnDispatchThread(() -> comboBoxSetSelectedItem(item));
    }

    @UIEffect
    public void setSelectedItemOnDisplay(@Nullable E item) {
        assert SwingUtilities.isEventDispatchThread();
        if (null == item) {
            selectedIndex = -1;
        } else {
            synchronized (this) {
                for (int i = 0; i < items.length; i++) {
                    if (Objects.equals(items[i], item)) {
                        selectedIndex = i;
                        break;
                    }
                }
            }
        }
        comboBoxSetSelectedItem(item);
    }

    public E getElementAt(int index) {
        return items[index];
    }

    public void addItem(E itemToAdd) {
        addElement(itemToAdd);
    }

    @SuppressWarnings("unchecked")
    public void insertElementAt(E element, int index) {
        synchronized (this) {
            if (index < 0 || index > items.length) {
                throw new IllegalArgumentException("index = " + index + ", items.length=" + items.length);
            }
            E newItems[] = (E[]) Array.newInstance(eClass, items.length + 1);
            for (int i = 0; i < index && i < items.length; i++) {
                newItems[i] = items[i];
            }
            newItems[index] = element;
            if (newItems.length - (index + 1) >= 0) {
                System.arraycopy(items, index + 1 - 1, newItems, index + 1, newItems.length - (index + 1));
            }
            if (selectedIndex > index) {
                selectedIndex++;
            }
            this.items = newItems;
        }
        Utils.runOnDispatchThread(() -> comboBoxInsertItemAt(element, index));
    }

    @SuppressWarnings("unchecked")
    public void addElement(E element) {
        synchronized (this) {
            E newItems[] = (E[]) Array.newInstance(eClass, items.length + 1);
            System.arraycopy(items, 0, newItems, 0, items.length);
            newItems[items.length] = element;
            this.items = newItems;
        }
        Utils.runOnDispatchThread(() -> comboBoxAddElment(element));
    }

    @SuppressWarnings("unchecked")
    @UIEffect
    public void addElementOnDisplay(E element) {
        assert SwingUtilities.isEventDispatchThread();
        synchronized (this) {
            E newItems[] = (E[]) Array.newInstance(eClass, items.length + 1);
            System.arraycopy(items, 0, newItems, 0, items.length);
            newItems[items.length] = element;
            this.items = newItems;
        }
        comboBoxAddElment(element);
    }

    @UIEffect
    private void comboBoxAddElment(E element) {
        comboBox.addItem(element);
    }

    @UIEffect
    private void comboBoxInsertItemAt(E element, int index) {
        comboBox.insertItemAt(element, index);
    }

    public int getSize() {
        return items.length;
    }

    public void removeAllItems() {
        removeAllElements();
    }

    @SuppressWarnings("unchecked")
    public void removeAllElements() {
        E newItems[] = (E[]) Array.newInstance(eClass, 0);
        synchronized (this) {
            this.items = newItems;
            this.selectedIndex = -1;
        }
        Utils.runOnDispatchThread(this::comboBoxRemoveAllElements);
    }

    @UIEffect
    public void removeAllElementsOnDisplay() {
        assert SwingUtilities.isEventDispatchThread();
        E newItems[] = (E[]) Array.newInstance(eClass, 0);
        synchronized (this) {
            this.items = newItems;
            this.selectedIndex = -1;
        }
        comboBoxRemoveAllElements();
    }

    @UIEffect
    private void comboBoxRemoveAllElements() {
        comboBox.removeAllItems();
    }

    @UIEffect
    private void comboBoxSetSelectedItem(@Nullable E item) {
        if (null != item) {
            comboBox.setSelectedItem(item);
        }
    }

    public @Nullable
    E getSelectedItem() {
        if (selectedIndex < 0 || selectedIndex > items.length) {
            return null;
        }
        return items[selectedIndex];
    }

}
