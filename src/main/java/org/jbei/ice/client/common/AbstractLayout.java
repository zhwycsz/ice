package org.jbei.ice.client.common;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public abstract class AbstractLayout extends Composite {

    public AbstractLayout() {
        FlexTable layout = new FlexTable();
        layout.setWidth("100%");
        layout.setHeight("100%");
        layout.setCellSpacing(0);
        layout.setCellPadding(0);
        initWidget(layout);

        initComponents();

        layout.setWidget(0, 0, createHeader());
        layout.setWidget(1, 0, createContents());
        layout.getCellFormatter().setVerticalAlignment(1, 0, HasVerticalAlignment.ALIGN_TOP);
        layout.getCellFormatter().setHeight(1, 0, "100%");
        layout.setWidget(2, 0, createFooter());
    }

    protected abstract Widget createContents();

    protected void initComponents() {
    }

    protected Widget createHeader() {
        VerticalPanel panel = new VerticalPanel();
        panel.setWidth("100%");
        panel.add(new HeaderView());
        panel.add(new HeaderMenu());
        return panel;
    }

    protected Widget createFooter() {
        return Footer.getInstance();
    }
}
