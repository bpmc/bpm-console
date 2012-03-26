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

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.mvc4g.client.Controller;
import com.mvc4g.client.ViewInterface;
import org.gwt.mosaic.ui.client.layout.BoxLayout;
import org.gwt.mosaic.ui.client.layout.BoxLayoutData;
import org.gwt.mosaic.ui.client.layout.GridLayout;
import org.gwt.mosaic.ui.client.layout.MosaicPanel;
import org.jboss.bpm.console.client.common.HeaderLabel;
import org.jboss.bpm.console.client.model.PluginInfo;
import org.jboss.bpm.console.client.model.ServerStatus;
import org.jboss.errai.workspaces.client.api.WidgetProvider;
import org.jboss.errai.workspaces.client.api.ProvisioningCallback;
import org.jboss.errai.workspaces.client.framework.Registry;

/**
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
public class ServerStatusView
    implements ViewInterface, LazyPanel, WidgetProvider
{

  public final static String ID = ServerStatusView.class.getName();

  private Controller controller;

  private ApplicationContext appContext;

  private boolean initialized;

  MosaicPanel layoutPanel;
  MosaicPanel pluginPanel;

  public ServerStatusView()
  {
    appContext = Registry.get(ApplicationContext.class);
    controller = Registry.get(Controller.class);
  }

  @Override
  public void provideWidget(ProvisioningCallback callback)
  {
    layoutPanel = new MosaicPanel(new BoxLayout(BoxLayout.Orientation.VERTICAL));

    // console info
    HeaderLabel console = new HeaderLabel("Console Info");
    layoutPanel.add(console, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));

    MosaicPanel layout1 = new MosaicPanel(new GridLayout(2,1));
    layout1.add(new HTML("Version:"));
    layout1.add(new HTML(Version.VERSION));

    layoutPanel.add(layout1, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));

    // server info
    HeaderLabel server = new HeaderLabel("Server Info");
    layoutPanel.add(server, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));

    MosaicPanel layout2 = new MosaicPanel(new GridLayout(2,2));
    layout2.add(new HTML("Host:"));
    layout2.add(new HTML(Registry.get(ApplicationContext.class).getConfig().getConsoleServerUrl()));

    pluginPanel = new MosaicPanel();
    layout2.add(new Label("Plugins:"));
    layout2.add(pluginPanel);

    layoutPanel.add(layout2, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));

    // ---

    controller.addView(ServerStatusView.ID, this);

    update(ServerPlugins.getStatus());

    callback.onSuccess(layoutPanel);
  }

  public void setController(Controller controller)
  {
    this.controller = controller;
  }


  public boolean isInitialized()
  {
    return initialized;
  }

  public void initialize()
  {
    if(!initialized)
    {
      update(ServerPlugins.getStatus());
      initialized = true;
    }
  }

  private void update(ServerStatus status)
  {
    pluginPanel.clear();

    Grid g = new Grid(status.getPlugins().size(), 2);
    g.setWidth("100%");

    for (int row = 0; row < status.getPlugins().size(); ++row)
    {
      PluginInfo p = status.getPlugins().get(row);
      String type = p.getType().substring(
          p.getType().lastIndexOf(".")+1, p.getType().length()
      );

      g.setText(row, 0, type);

      final Image img = p.isAvailable() ?
          new Image("images/icons/confirm_small.png"):
          new Image("images/icons/deny_small.png");

      g.setWidget(row, 1, img );
    }

    pluginPanel.add(g);

    pluginPanel.layout();

  }

}
