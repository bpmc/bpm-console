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
package org.jboss.bpm.console.client.report;

import com.google.gwt.user.client.Timer;
import com.mvc4g.client.Controller;
import com.mvc4g.client.Event;
import com.mvc4g.client.ViewInterface;
import org.gwt.mosaic.ui.client.layout.FillLayout;
import org.gwt.mosaic.ui.client.layout.MosaicPanel;
import org.jboss.bpm.console.client.common.WidgetWindowPanel;
import org.jboss.bpm.console.client.search.UpdateSearchDefinitionsAction;
import org.jboss.bpm.report.model.ReportReference;
import org.jboss.errai.workspaces.client.api.ProvisioningCallback;
import org.jboss.errai.workspaces.client.api.WidgetProvider;
import org.jboss.errai.workspaces.client.framework.Registry;

import java.util.List;

/**
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
public class ReportView implements ViewInterface, WidgetProvider
{
  public final static String ID = ReportView.class.getName();

  private Controller controller;
  private boolean isInitialized; 
  private ReportLaunchPadView coverpanel;

  private MosaicPanel panel;

  public void provideWidget(ProvisioningCallback callback)
  {
    panel = new MosaicPanel(new FillLayout());
    panel.setPadding(0);
    controller = Registry.get(Controller.class);

    initialize();

    controller.addView(ReportView.ID, this);
    controller.addAction(UpdateReportConfigAction.ID, new UpdateReportConfigAction());
    
    // ----

    Timer t = new Timer()
    {
      @Override
      public void run()
      {
        controller.handleEvent(new Event(UpdateReportConfigAction.ID, null));
      }
    };

    t.schedule(500);

    callback.onSuccess(panel);
  }

  public boolean isInitialized()
  {
    return isInitialized;
  }

  public void initialize()
  {
    if(!isInitialized)
    {
      // cover
      coverpanel = new ReportLaunchPadView();
      panel.add(coverpanel);

      // views and actions      
      controller.addView(ReportLaunchPadView.ID, coverpanel);

      controller.addAction(UpdateSearchDefinitionsAction.ID, new UpdateSearchDefinitionsAction());
      controller.addAction(RenderReportAction.ID, new RenderReportAction());

      this.isInitialized = true;
    }
  }

  public void setController(Controller controller)
  {
    this.controller = controller;
  }

  public void configure(List<ReportReference> reports)
  {
    // update coverview
    coverpanel.update(reports);    
  }

  public void displayReport(String title, String dispatchUrl)
  {
    ReportFrame reportFrame = new ReportFrame();
    reportFrame.setFrameUrl(dispatchUrl);
    new WidgetWindowPanel(title, reportFrame, true);
  }
}
