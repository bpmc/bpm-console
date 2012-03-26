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
package org.jboss.bpm.console.client.engine;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import org.jboss.bpm.console.client.util.ConsoleLog;
import org.jboss.errai.bus.server.annotations.security.RequireRoles;
import org.jboss.errai.workspaces.client.api.ProvisioningCallback;
import org.jboss.errai.workspaces.client.api.WidgetProvider;
import org.jboss.errai.workspaces.client.api.annotations.LoadTool;

@LoadTool(name = "Deployments", group = "Runtime", icon="deploymentIcon")
public class DeploymentModule implements WidgetProvider
{
  static DeploymentListView instance = null;

  public void provideWidget(final ProvisioningCallback callback)
  {
    GWT.runAsync(
        new RunAsyncCallback()
        {
          public void onFailure(Throwable err)
          {
            ConsoleLog.error("Failed to load tool", err);
          }

          public void onSuccess()
          {
            if (instance == null) {
              instance = new DeploymentListView();
            }
            instance.provideWidget(callback);
          }
        }

    );
  }
}
