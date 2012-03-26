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
package org.jboss.bpm.console.client.task;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.mvc4g.client.Controller;
import com.mvc4g.client.Event;
import org.gwt.mosaic.ui.client.ListBox;
import org.gwt.mosaic.ui.client.MessageBox;
import org.gwt.mosaic.ui.client.ToolBar;
import org.gwt.mosaic.ui.client.event.RowSelectionEvent;
import org.gwt.mosaic.ui.client.event.RowSelectionHandler;
import org.gwt.mosaic.ui.client.layout.*;
import org.gwt.mosaic.ui.client.list.DefaultListModel;
import org.jboss.bpm.console.client.ApplicationContext;
import org.jboss.bpm.console.client.ServerPlugins;
import org.jboss.bpm.console.client.common.*;
import org.jboss.bpm.console.client.model.TaskRef;
import org.jboss.bpm.console.client.task.events.DetailViewEvent;
import org.jboss.bpm.console.client.task.events.TaskIdentityEvent;
import org.jboss.bpm.console.client.util.SimpleDateFormat;
import org.jboss.errai.bus.client.ErraiBus;
import org.jboss.errai.bus.client.api.Message;
import org.jboss.errai.bus.client.api.MessageCallback;
import org.jboss.errai.workspaces.client.api.WidgetProvider;
import org.jboss.errai.workspaces.client.api.ProvisioningCallback;
import org.jboss.errai.workspaces.client.framework.Registry;

import java.util.List;

/**
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
public class AssignedTasksView extends AbstractTaskList implements WidgetProvider, DataDriven
{

  public final static String ID = AssignedTasksView.class.getName();

  private final ApplicationContext appContext;

  private IFrameWindowPanel iframeWindow = null;

  private TaskDetailView detailsView;

  private SimpleDateFormat dateFormat = new SimpleDateFormat();

  private boolean hasDispatcherPlugin;

  private PagingPanel pagingPanel;

  private MosaicPanel panel;

  public AssignedTasksView()
  {
    controller = Registry.get(Controller.class);
    appContext = Registry.get(ApplicationContext.class);
  }

  public void provideWidget(ProvisioningCallback callback)
  {
    panel = new MosaicPanel(new BorderLayout());

    initialize();

    panel.add(taskList, new BorderLayoutData(BorderLayout.Region.CENTER));
    panel.add(detailsView, new BorderLayoutData(BorderLayout.Region.SOUTH, 10 , 200));
    
    controller.addView(AssignedTasksView.ID, this);

    callback.onSuccess(panel);
  }

  public void initialize()
  {
    if(!isInitialized)
    {
      // workaround
      OpenTasksView.registerCommonActions(controller);
      
      taskList = new MosaicPanel( new BoxLayout(BoxLayout.Orientation.VERTICAL));
      taskList.setPadding(0);
      taskList.setWidgetSpacing(0);

      listBox =
          new ListBox<TaskRef>(
              new String[] {
                  "Priority", "Process", "Task Name", "Due Date"
              }
          );


      listBox.setCellRenderer(new ListBox.CellRenderer<TaskRef>() {
        public void renderCell(ListBox<TaskRef> listBox, int row, int column,
                               TaskRef item) {
          switch (column) {
            case 0:
              listBox.setText(row, column, String.valueOf(item.getPriority()));
              break;
            case 1:
              listBox.setText(row, column, item.getProcessId());
              break;
            case 2:
              listBox.setText(row, column, item.getName());
              break;
            case 3:
              String d = item.getDueDate() != null ? dateFormat.format(item.getDueDate()):"";
              listBox.setText(row, column, d);
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
              TaskRef task = getSelection(); // first call always null?
              if(task!=null)
              {
                controller.handleEvent(
                    new Event(UpdateDetailsAction.ID, new DetailViewEvent("AssignedDetailView", task))
                );
              }

            }
          }
      );

      // toolbar
      final MosaicPanel toolBox = new MosaicPanel();
      toolBox.setPadding(0);
      toolBox.setWidgetSpacing(5);
      //toolBox.setLayout(new BoxLayout(BoxLayout.Orientation.VERTICAL));

      final ToolBar toolBar = new ToolBar();
      toolBar.add(
          new Button("Refresh", new ClickHandler() {
            public void onClick(ClickEvent clickEvent)
            {
              reload();

            }
          }
          )
      );


      Button viewBtn = new Button("View", new ClickHandler()
      {

        public void onClick(ClickEvent clickEvent)
        {
          TaskRef selection = getSelection();

          if (selection != null)
          {
            if (selection.getUrl() != null && !selection.getUrl().equals(""))
            {
              iframeWindow = new IFrameWindowPanel(
                  selection.getUrl(), "Task Form: "+selection.getName()
              );

              iframeWindow.setCallback(
                  new IFrameWindowCallback()
                  {
                    public void onWindowClosed()
                    {
                      reload();
                    }
                  }
              );

              iframeWindow.show();
            }
            else
            {
              MessageBox.alert("Invalid operation", "The task doesn't provide a UI");
            }
          }
          else
          {
            MessageBox.alert("Missing selection", "Please select a task");
          }
        }
      }
      );
      toolBar.add(viewBtn);

      toolBar.add(
          new Button("Release", new ClickHandler() {
            public void onClick(ClickEvent clickEvent)
            {
              TaskRef selection = getSelection();

              if(selection!=null)
              {
                TaskIdentityEvent payload = new TaskIdentityEvent(
                    null, selection
                );

                controller.handleEvent(
                    new Event(ReleaseTaskAction.ID, payload)
                );
              }
              else
              {
                MessageBox.alert("Missing selection", "Please select a task");
              }
            }
          }
          )
      );


      toolBox.add(toolBar, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));

      this.taskList.add(toolBox, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));
      this.taskList.add(listBox, new BoxLayoutData(BoxLayoutData.FillStyle.BOTH));

      pagingPanel = new PagingPanel(
          new PagingCallback()
          {
            public void rev()
            {
              renderUpdate();
            }

            public void ffw()
            {
              renderUpdate();
            }
          }
      );

      this.taskList.add(pagingPanel, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));
    
      detailsView = new TaskDetailView(false);
      controller.addView("AssignedDetailView", detailsView);
      detailsView.initialize();


      // plugin availability
      this.hasDispatcherPlugin =
          ServerPlugins.has("org.jboss.bpm.console.server.plugin.FormDispatcherPlugin");
      viewBtn.setEnabled(hasDispatcherPlugin);


      // deployments model listener
      ErraiBus.get().subscribe(Model.SUBJECT,
          new MessageCallback()
      {
        public void callback(Message message)
        {
          switch (ModelCommands.valueOf(message.getCommandType()))
          {
            case HAS_BEEN_UPDATED:
              if(message.get(String.class, ModelParts.CLASS).equals(Model.PROCESS_MODEL))
                reload();
              break;
          }
        }
      });

      Timer t = new Timer()
      {
        @Override
        public void run()
        {
          // force loading
          reload();
        }
      };

      t.schedule(500);
      
      isInitialized = true;
    }
  }

  private void reload()
  {
    // force loading
    controller.handleEvent(
        new Event(LoadTasksAction.ID, appContext.getAuthentication().getUsername())
    );
  }

  public void reset()
  {
     final DefaultListModel<TaskRef> model =
        (DefaultListModel<TaskRef>) listBox.getModel();

    model.clear();

     // clear details
    controller.handleEvent(
        new Event(UpdateDetailsAction.ID, new DetailViewEvent("AssignedDetailView", null))
    );
  }

  public void update(Object... data)
  {
    this.identity = (String)data[0];
    this.cachedTasks = (List<TaskRef>)data[1];
    pagingPanel.reset();
    renderUpdate();
  }

  public void setLoading(boolean isLoading)
  {
    if(panel.isVisible())
      LoadingOverlay.on(taskList, isLoading);
  }

  private void renderUpdate()
  {
    // lazy init
    initialize();

    reset();

    final DefaultListModel<TaskRef> model =
        (DefaultListModel<TaskRef>) listBox.getModel();

    List<TaskRef> trimmed = pagingPanel.trim(cachedTasks);
    for(TaskRef task : trimmed)
    {
      if(TaskRef.STATE.ASSIGNED ==task.getCurrentState())
        model.add(task);
    }

  }

}
