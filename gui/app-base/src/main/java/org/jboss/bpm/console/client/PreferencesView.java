/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.bpm.console.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
import com.mvc4g.client.Controller;
import com.mvc4g.client.ViewInterface;
import org.gwt.mosaic.ui.client.layout.BoxLayout;
import org.gwt.mosaic.ui.client.layout.BoxLayoutData;
import org.gwt.mosaic.ui.client.layout.GridLayout;
import org.gwt.mosaic.ui.client.layout.MosaicPanel;
import org.jboss.bpm.console.client.common.HeaderLabel;
import org.jboss.errai.workspaces.client.api.WidgetProvider;
import org.jboss.errai.workspaces.client.api.ProvisioningCallback;
import org.jboss.errai.workspaces.client.framework.Preferences;
import org.jboss.errai.workspaces.client.framework.Registry;
import org.jboss.errai.workspaces.client.api.ToolSet;

import java.util.List;

/**
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
public class PreferencesView implements ViewInterface, WidgetProvider
{

  public final static String ID = PreferencesView.class.getName();

  private Controller controller;

  private ApplicationContext appContext;

  MosaicPanel panel;

  public void setController(Controller controller)
  {
    this.controller = controller;
  }
  
  public void provideWidget(ProvisioningCallback callback)
  {
    panel = new MosaicPanel(new BoxLayout(BoxLayout.Orientation.VERTICAL));

    this.appContext = Registry.get(ApplicationContext.class);
    panel.add(new HeaderLabel("User Preferences"), new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));

    MosaicPanel layout = new MosaicPanel(new GridLayout(2,1));
    layout.add(
        new HTML("<b>Default Tool</b><br>" +
            "Select the tool that should be loaded upon login.")
    );

    final List<ToolSet> toolsets =
        org.jboss.errai.workspaces.client.Workspace.getInstance().getToolsets();
    final ListBox multiBox = new ListBox();
    multiBox.setVisibleItemCount(5);
    layout.add(multiBox);

    // init
    final Preferences prefs = GWT.create(Preferences.class);
    String prefEditor = prefs.get(Preferences.DEFAULT_TOOL);
    for(ToolSet ts : toolsets)
    {
      multiBox.addItem(ts.getToolSetName());
      if(ts.getToolSetName().equals(prefEditor))
        multiBox.setItemSelected(multiBox.getItemCount()-1, true);
    }

    multiBox.addClickHandler(
        new ClickHandler()
        {
          public void onClick(ClickEvent clickEvent)
          {
            String title = multiBox.getItemText(multiBox.getSelectedIndex());
            for(ToolSet ts: toolsets)
            {
              if(ts.getToolSetName().equals(title))
              {
                prefs.set(Preferences.DEFAULT_TOOL, ts.getToolSetName());
              }
            }
          }
        }
    );

    panel.add(layout, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));

    callback.onSuccess(panel);
  }

}
