package org.jbei.ice.client.common.table.cell;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public abstract class UrlCell<T> extends AbstractCell<T> {

    private static final String MOUSEOVER_EVENT_NAME = "mouseover";
    private static final String MOUSEOUT_EVENT_NAME = "mouseout";
    private static final String MOUSEOVER_STYLE = "mouseover_color";
    private static final String MOUSE_CLICK = "click";

    public UrlCell() {
        super(MOUSEOVER_EVENT_NAME, MOUSEOUT_EVENT_NAME, MOUSE_CLICK);
    }

    @Override
    public void render(Context context, T value, SafeHtmlBuilder sb) {

        if (value == null)
            return;

        sb.appendHtmlConstant("<a class=\"cell_mouseover\">" + getCellValue(value) + "</a>");
    }

    @Override
    public void onBrowserEvent(Context context, Element parent, T value, NativeEvent event,
            ValueUpdater<T> valueUpdater) {

        super.onBrowserEvent(context, parent, value, event, valueUpdater);

        final String eventType = event.getType();

        //        if (MOUSEOVER_EVENT_NAME.equalsIgnoreCase(eventType)) {
        //            if (withinBounds(parent, event)) {
        //                parent.setClassName(MOUSEOVER_STYLE);
        //                Utils.showPointerCursor(parent);
        //            } else {
        //                parent.removeClassName(MOUSEOVER_STYLE);
        //                Utils.showDefaultCursor(parent);
        //            }
        //        }
        //
        //        if (MOUSEOUT_EVENT_NAME.equalsIgnoreCase(eventType)) {
        //            parent.removeClassName(MOUSEOVER_STYLE);
        //            Utils.showDefaultCursor(parent);
        //        }

        if (MOUSE_CLICK.equalsIgnoreCase(eventType)) {
            if (withinBounds(parent, event))
                onClick(value);
        }
    }

    protected boolean withinBounds(Element parent, NativeEvent event) {

        if (event.getClientY() < ((Element) parent.getFirstChild()).getAbsoluteTop()) {
            return false;
        }

        if (event.getClientY() > ((Element) parent.getFirstChild()).getAbsoluteBottom()) {
            return false;
        }

        if (event.getClientX() < ((Element) parent.getFirstChild()).getAbsoluteLeft()) {
            return false;
        }

        if (event.getClientX() > ((Element) parent.getFirstChild()).getAbsoluteRight()) {
            return false;
        }

        return true;
    }

    /**
     * @return the value to be displayed as a link in the cell
     */
    protected abstract String getCellValue(T object);

    /**
     * Action to be performed in the event of a click. To actual go to the url,
     * example code is
     * 
     * History.newItem(Page.ENTRY_VIEW.getLink() + ";id=" + recordId);
     */
    protected abstract void onClick(T object);
}
