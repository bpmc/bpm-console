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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.mvc4g.client.Controller;
import com.mvc4g.client.Event;
import com.mvc4g.client.ViewInterface;
import org.gwt.mosaic.ui.client.ListBox;
import org.gwt.mosaic.ui.client.MessageBox;
import org.gwt.mosaic.ui.client.ToolBar;
import org.gwt.mosaic.ui.client.event.RowSelectionEvent;
import org.gwt.mosaic.ui.client.event.RowSelectionHandler;
import org.gwt.mosaic.ui.client.layout.*;
import org.gwt.mosaic.ui.client.list.DefaultListModel;
import org.jboss.bpm.console.client.ApplicationContext;
import org.jboss.bpm.console.client.ConsoleConfig;
import org.jboss.bpm.console.client.common.DataDriven;
import org.jboss.bpm.console.client.common.LoadingOverlay;
import org.jboss.bpm.console.client.model.DeploymentRef;
import org.jboss.bpm.console.client.util.SimpleDateFormat;
import org.jboss.errai.workspaces.client.api.ProvisioningCallback;
import org.jboss.errai.workspaces.client.api.WidgetProvider;
import org.jboss.errai.workspaces.client.framework.Registry;

import java.util.List;

/**
 * List of deployments
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
public class DeploymentListView implements ViewInterface, WidgetProvider, DataDriven
{
  public final static String ID = DeploymentListView.class.getName();

  private Controller controller;

  private boolean initialized;

  private MosaicPanel deploymentList = null;

  private ListBox<DeploymentRef> listBox;

  private DeploymentRef selection = null;

  private SimpleDateFormat dateFormat = new SimpleDateFormat();

  private int FILTER_NONE       = 10;
  private int FILTER_ACTIVE     = 20;
  private int FILTER_SUSPENDED  = 30;
  private int currentFilter = FILTER_NONE;

  private List<DeploymentRef> deployments = null;

  private DeploymentDetailView detailView;

  MosaicPanel panel;

  private boolean isRiftsawInstance = false;

  public DeploymentListView()
  {
    controller = Registry.get(Controller.class);

    // riftsaw?
    ConsoleConfig config = Registry.get(ApplicationContext.class).getConfig();
    isRiftsawInstance = config.getProfileName().equals("BPEL Console");
  }

  public void provideWidget(ProvisioningCallback callback)
  {
    panel = new MosaicPanel(new BorderLayout());
    listBox = createListBox();

    initialize();

    panel.add(deploymentList, new BorderLayoutData(BorderLayout.Region.CENTER));
    panel.add(detailView, new BorderLayoutData(BorderLayout.Region.SOUTH,200));

    // create and register actions
    controller.addAction(UpdateDeploymentsAction.ID, new UpdateDeploymentsAction());
    controller.addAction(UpdateDeploymentDetailAction.ID, new UpdateDeploymentDetailAction());
    controller.addAction(DeleteDeploymentAction.ID, new DeleteDeploymentAction());
    controller.addAction(SuspendDeploymentAction.ID, new SuspendDeploymentAction());
    controller.addAction(ResumeDeploymentAction.ID, new ResumeDeploymentAction());    
    //controller.addAction(ViewDeploymentAction.ID, new ViewDeploymentAction());    

    controller.addView(DeploymentListView.ID, this);


    callback.onSuccess(panel);
    
  }

  private ListBox createListBox()
  {
    final ListBox<DeploymentRef> listBox =
        new ListBox<DeploymentRef>(
            new String[] {
                "Deployment", "Status"}
        );


    listBox.setCellRenderer(new ListBox.CellRenderer<DeploymentRef>() {
      public void renderCell(ListBox<DeploymentRef> listBox, int row, int column,
                             DeploymentRef item) {

        String color= item.isSuspended() ? "#CCCCCC" : "#000000";

        switch (column) {
          case 0:
            String text = "<div style=\"color:"+color+"\">"+ item.getName() +"</div>";
            listBox.setWidget(row, column, new HTML(text));
            break;
          case 1:
            String status = item.isSuspended() ? "retired" : "active";
            String s = "<div style=\"color:"+color+"\">"+ status +"</div>";
            listBox.setWidget(row, column, new HTML(status));
            break;
          default:
            throw new RuntimeException("Unexpected column size");
        }
      }
    });

    listBox.addRowSelectionHandler(
        new RowSelectionHandler()
        {
          public void onRowSelection(RowSelectionEvent rowSelectionEvent)
          {
            int index = listBox.getSelectedIndex();
            if(index!=-1)
            {
              DeploymentRef item = listBox.getItem(index);

              controller.handleEvent(
                  new Event(UpdateDeploymentDetailAction.ID, item)
              );
            }
          }
        }
    );

    return listBox;
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
      deploymentList = new MosaicPanel( new BoxLayout(BoxLayout.Orientation.VERTICAL));
      deploymentList.setPadding(0);
      deploymentList.setWidgetSpacing(0);

      // toolbar

      final MosaicPanel toolBox = new MosaicPanel();
      toolBox.setPadding(0);
      toolBox.setWidgetSpacing(0);
      toolBox.setLayout(new BoxLayout(BoxLayout.Orientation.HORIZONTAL));

      final ToolBar toolBar = new ToolBar();
      toolBar.add(
          new Button("Refresh", new ClickHandler() {
            public void onClick(ClickEvent clickEvent)
            {
              reset();
              
              // force loading
              controller.handleEvent(
                  new Event(UpdateDeploymentsAction.ID, null)
              );
            }
          }
          )
      );


      Button deleteBtn = new Button("Delete", new ClickHandler()
      {
        public void onClick(ClickEvent clickEvent)
        {
          DeploymentRef deploymentRef = getSelection();
          if (deploymentRef != null)
          {
            MessageBox.confirm("Delete deployment",
                "Do you want to delete this deployment? Any related data will be removed.",
                new MessageBox.ConfirmationCallback()
                {
                  public void onResult(boolean doIt)
                  {
                    if (doIt)
                    {
                      controller.handleEvent(
                          new Event(
                              DeleteDeploymentAction.ID,
                              getSelection().getId()
                          )
                      );
                    }
                  }
                });
          }
          else
          {
            MessageBox.alert("Missing selection", "Please select a deployment");
          }
        }
      }
      );

      if(!isRiftsawInstance)
        toolBar.add(deleteBtn);

      toolBox.add(toolBar, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));

      // filter
      MosaicPanel filterPanel = new MosaicPanel(new BoxLayout(BoxLayout.Orientation.VERTICAL));
      filterPanel.setStyleName("mosaic-ToolBar");
      final com.google.gwt.user.client.ui.ListBox dropBox = new com.google.gwt.user.client.ui.ListBox(false);
      dropBox.setStyleName("bpm-operation-ui");
      dropBox.addItem("All");
      dropBox.addItem("Active");
      dropBox.addItem("Retired");

      dropBox.addChangeListener(new ChangeListener() {
        public void onChange(Widget sender) {
          switch (dropBox.getSelectedIndex())
          {
            case 0:
              currentFilter = FILTER_NONE;
              break;
            case 1:
              currentFilter = FILTER_ACTIVE;
              break;
            case 2:
              currentFilter = FILTER_SUSPENDED;
              break;
            default:
              throw new IllegalArgumentException("No such index");
          }

          renderFiltered();
        }
      });
      filterPanel.add(dropBox);

      toolBox.add(filterPanel, new BoxLayoutData(BoxLayoutData.FillStyle.VERTICAL));

      this.deploymentList.add(toolBox, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));
      this.deploymentList.add(listBox, new BoxLayoutData(BoxLayoutData.FillStyle.BOTH));

      // details
      // detail panel
      detailView = new DeploymentDetailView();
      controller.addView(DeploymentDetailView.ID, detailView);

      Timer t = new Timer()
      {
        @Override
        public void run()
        {
          controller.handleEvent(
              new Event(UpdateDeploymentsAction.ID, null)
          );
        }
      };

      t.schedule(500);

      initialized = true;
    }
  }

  public DeploymentRef getSelection()
  {
    DeploymentRef selection = null;
    if(isInitialized() && listBox.getSelectedIndex()!=-1)
      selection = listBox.getItem( listBox.getSelectedIndex());
    return selection;
  }

  public void reset()
  {
    final DefaultListModel<DeploymentRef> model =
        (DefaultListModel<DeploymentRef>) listBox.getModel();

    model.clear();

    // clear details
    controller.handleEvent(
        new Event(UpdateDeploymentDetailAction.ID, null)
    );
  }

  public void update(Object... data)
  {
    this.deployments = (List<DeploymentRef>)data[0];

    renderFiltered();
  }

  public void setLoading(boolean isLoading)
  {
    LoadingOverlay.on(deploymentList, isLoading);
  }

  private void renderFiltered()
  {
    if(this.deployments!=null)
    {
      reset();

      final DefaultListModel<DeploymentRef> model =
          (DefaultListModel<DeploymentRef>) listBox.getModel();

      for(DeploymentRef dpl : deployments)
      {
        if(FILTER_NONE==currentFilter)
        {
          model.add(dpl);
        }
        else
        {
          boolean showSuspended = (FILTER_SUSPENDED==currentFilter);
          if(dpl.isSuspended()==showSuspended)
            model.add(dpl);
        }
      }

      if(listBox.getSelectedIndex()!=-1)
        listBox.setItemSelected(listBox.getSelectedIndex(), false);

    }
  }

  public void select(String deploymentId)
  {
    final DefaultListModel<DeploymentRef> model =
        (DefaultListModel<DeploymentRef>) listBox.getModel();

    for(int i=0; i<model.getSize(); i++)
    {
      DeploymentRef ref = model.getElementAt(i);
      if(ref.getId().equals(deploymentId))
      {
        listBox.setSelectedIndex(i);
        break;
      }
    }

  }
}
