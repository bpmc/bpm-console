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
package org.jboss.bpm.console.client.process;

import com.google.gwt.user.client.ui.Widget;
import com.mvc4g.client.Controller;
import org.gwt.mosaic.ui.client.layout.*;
import org.jboss.bpm.console.client.util.ConsoleLog;
import org.jboss.errai.workspaces.client.api.ProvisioningCallback;
import org.jboss.errai.workspaces.client.api.WidgetProvider;
import org.jboss.errai.workspaces.client.framework.Registry;

/**
 * Combined view of process and instance data in a single screen
 */
public class MergedProcessView implements WidgetProvider
{
  MosaicPanel panel;

  DefinitionListView definitionView;
  InstanceListView instanceView;

  public void provideWidget(ProvisioningCallback callback)
  {
    Controller controller = Registry.get(Controller.class);
    
    panel = new MosaicPanel();
    panel.setPadding(0);    
    
    definitionView = new DefinitionListView();
    instanceView = new InstanceListView();

    final MosaicPanel splitPanel = new MosaicPanel(new ColumnLayout());
    splitPanel.setPadding(0);

    definitionView.provideWidget(new ProvisioningCallback()
    {
      public void onSuccess(Widget instance)
      {
        splitPanel.add(instance, new ColumnLayoutData("250 px"));
      }

      public void onUnavailable()
      {
        ConsoleLog.error("Failed to load DefinitionListView.class");
      }
    });

    instanceView.provideWidget(
        new ProvisioningCallback()
        {
          public void onSuccess(Widget instance)
          {
            splitPanel.add(instance);
          }

          public void onUnavailable()
          {
            ConsoleLog.error("Failed to load DefinitionListView.class");
          }
        }
    );

    panel.add(splitPanel);
    
    callback.onSuccess(panel);
  }
}
