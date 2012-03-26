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
public class OpenTasksView extends AbstractTaskList implements WidgetProvider, DataDriven
{

  public final static String ID = OpenTasksView.class.getName();

  private TaskDetailView detailsView;

  private ApplicationContext appContext;

  private SimpleDateFormat dateFormat = new SimpleDateFormat();

  private PagingPanel pagingPanel;

  private MosaicPanel panel;

  private Controller controller;

  private static boolean actionSetup = false;

  public static void registerCommonActions(Controller controller)
  {
    if(!actionSetup)
    {
      // create and register actions
      controller.addAction(LoadTasksAction.ID, new LoadTasksAction());
      controller.addAction(LoadTasksParticipationAction.ID, new LoadTasksParticipationAction());
      controller.addAction(ClaimTaskAction.ID, new ClaimTaskAction());
      controller.addAction(ReleaseTaskAction.ID, new ReleaseTaskAction());
      controller.addAction(UpdateDetailsAction.ID, new UpdateDetailsAction());
      controller.addAction(AssignTaskAction.ID, new AssignTaskAction());
      controller.addAction(ReloadAllTaskListsAction.ID, new ReloadAllTaskListsAction());

      actionSetup = true;
    }
  }

  public void provideWidget(ProvisioningCallback callback)
  {
    panel = new MosaicPanel(new BorderLayout());

    controller = Registry.get(Controller.class);
    appContext = Registry.get(ApplicationContext.class);

    initialize();

    registerCommonActions(controller);

    // ----

    /*TaskDetailView assignedDetailView = new TaskDetailView(false);
controller.addView("AssignedDetailView", assignedDetailView);
assignedDetailView.initialize();
registerView(controller, tabPanel, AssignedTasksView.ID, new AssignedTasksView(appContext, assignedDetailView));*/

    controller.addView(OpenTasksView.ID, this);

    // ----

    panel.add(taskList, new BorderLayoutData(BorderLayout.Region.CENTER));
    panel.add(detailsView, new BorderLayoutData(BorderLayout.Region.SOUTH, 10,200));

    callback.onSuccess(panel);
  }

  public void initialize()
  {
    if(!isInitialized)
    {
      taskList = new MosaicPanel( new BoxLayout(BoxLayout.Orientation.VERTICAL));
      taskList.setPadding(0);
      taskList.setWidgetSpacing(0);

      listBox =
          new ListBox<TaskRef>(
              new String[] {
                  "Priority", "Process", "Task Name", "Status", "Due Date"}
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
              listBox.setText(row, column, String.valueOf(item.getCurrentState()));
              break;
            case 4:
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
                    new Event(UpdateDetailsAction.ID, new DetailViewEvent("OpenDetailView", task))
                );
              }
            }
          }
      );

      // toolbar
      final MosaicPanel toolBox = new MosaicPanel();
      toolBox.setPadding(0);
      toolBox.setWidgetSpacing(5);

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

      toolBar.add(
          new Button("Claim", new ClickHandler() {
            public void onClick(ClickEvent clickEvent)
            {
              TaskRef selection = getSelection();

              if(selection!=null)
              {
                controller.handleEvent(
                    new Event(
                        ClaimTaskAction.ID,
                        new TaskIdentityEvent(appContext.getAuthentication().getUsername(), selection)
                    )
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


      // ----


      // create and register views
      detailsView = new TaskDetailView(true);
      controller.addView("OpenDetailView", detailsView);
      detailsView.initialize();


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
        new Event(LoadTasksParticipationAction.ID, getAssignedIdentity())
    );
  }


  public void reset()
  {
    final DefaultListModel<TaskRef> model =
        (DefaultListModel<TaskRef>) listBox.getModel();

    model.clear();

    // clear details
    controller.handleEvent(
        new Event(UpdateDetailsAction.ID, new DetailViewEvent("OpenDetailView", null))
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
      if(TaskRef.STATE.OPEN ==task.getCurrentState())
        model.add(task);
    }
  }

}
