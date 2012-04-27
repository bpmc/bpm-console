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
import com.google.gwt.user.client.ui.Widget;
import com.mvc4g.client.Controller;
import com.mvc4g.client.Event;
import com.mvc4g.client.ViewInterface;
import org.gwt.mosaic.ui.client.ListBox;
import org.gwt.mosaic.ui.client.MessageBox;
import org.gwt.mosaic.ui.client.ToolBar;
import org.gwt.mosaic.ui.client.event.RowSelectionEvent;
import org.gwt.mosaic.ui.client.event.RowSelectionHandler;
import org.gwt.mosaic.ui.client.layout.BoxLayout;
import org.gwt.mosaic.ui.client.layout.BoxLayoutData;
import org.gwt.mosaic.ui.client.layout.MosaicPanel;
import org.gwt.mosaic.ui.client.list.DefaultListModel;
import org.jboss.bpm.console.client.common.DataDriven;
import org.jboss.bpm.console.client.common.LoadingOverlay;
import org.jboss.bpm.console.client.model.JobRef;
import org.jboss.bpm.console.client.util.SimpleDateFormat;
import org.jboss.errai.workspaces.client.api.ProvisioningCallback;
import org.jboss.errai.workspaces.client.api.WidgetProvider;
import org.jboss.errai.workspaces.client.framework.Registry;

import java.util.Date;
import java.util.List;

/**
 * Display a list of jobs waiting for execution.<br/>
 * I.e. pending Timers and Messages.
 *
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
public class JobListView implements ViewInterface, WidgetProvider, DataDriven
{
  public final static String ID = JobListView.class.getName();

  private Controller controller;

  private MosaicPanel jobList = null;

  private ListBox<JobRef> listBox;

  private JobRef selection = null;

  private SimpleDateFormat dateFormat = new SimpleDateFormat();

  private int FILTER_NONE       = 10;
  private int FILTER_TIMER     = 20;
  private int FILTER_MESSAGE  = 30;
  private int currentFilter = FILTER_NONE;

  private List<JobRef> jobs = null;

  MosaicPanel panel;

  private boolean initialized;

  public JobListView()
  {
    controller = Registry.get(Controller.class);
  }
  
  public void provideWidget(ProvisioningCallback callback)
  {
    panel = new MosaicPanel();

    listBox = createListBox();

    initialize();

    panel.add(jobList);

    controller.addView(JobListView.ID, this);
    controller.addAction(ExecuteJobAction.ID, new ExecuteJobAction());
    callback.onSuccess(panel);
  }

  private ListBox createListBox()
  {
    final ListBox<JobRef> listBox =
        new ListBox<JobRef>(
            new String[] {
                "ID", "Due Date", "Type"}
        );


    listBox.setCellRenderer(new ListBox.CellRenderer<JobRef>() {
      public void renderCell(ListBox<JobRef> listBox, int row, int column,
                             JobRef item) {
        switch (column) {
          case 0:
            listBox.setText(row, column, item.getId());
            break;
          case 1:
            long ts = item.getTimestamp();
            String ds = ts > 0 ?  dateFormat.format(new Date(ts)) : "";
            listBox.setText(row, column, ds);
            break;
          case 2:
            listBox.setText(row, column, item.getType());
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
              JobRef item = listBox.getItem(index);

              /*controller.handleEvent(
                  new Event(UpdateJobDetailAction.ID, item)
              );*/
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
      jobList = new MosaicPanel( new BoxLayout(BoxLayout.Orientation.VERTICAL));
      jobList.setPadding(0);
      jobList.setWidgetSpacing(0);

      // toolbar

      final MosaicPanel toolBox = new MosaicPanel();
      toolBox.setPadding(0);
      toolBox.setWidgetSpacing(0);
      toolBox.setLayout(new BoxLayout(BoxLayout.Orientation.HORIZONTAL));

      // toolbar
      final ToolBar toolBar = new ToolBar();
      toolBar.add(
          new Button("Refresh", new ClickHandler() {
            public void onClick(ClickEvent clickEvent)
            {              
              // force loading
              controller.handleEvent(
                  new Event(UpdateJobsAction.ID, null)
              );
            }
          }
          )
      );

      toolBar.add(
          new Button("Execute", new ClickHandler() {

            public void onClick(ClickEvent clickEvent)
            {              
              JobRef selection = getSelection();
              if(null==selection)
              {
                MessageBox.alert("Missing selection", "Please select a job!");
              }
              else
              {
                controller.handleEvent(
                    new Event(ExecuteJobAction.ID, selection.getId())
                );
              }
            }
          }
          )
      );

      toolBox.add(toolBar, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));

      // filter
      MosaicPanel filterPanel = new MosaicPanel(new BoxLayout(BoxLayout.Orientation.VERTICAL));
      filterPanel.setStyleName("mosaic-ToolBar");
      final com.google.gwt.user.client.ui.ListBox dropBox = new com.google.gwt.user.client.ui.ListBox(false);
      dropBox.setStyleName("bpm-operation-ui");
      dropBox.addItem("All");
      dropBox.addItem("Timers");
      dropBox.addItem("Messages");

      dropBox.addChangeListener(new ChangeListener() {
        public void onChange(Widget sender) {
          switch (dropBox.getSelectedIndex())
          {
            case 0:
              currentFilter = FILTER_NONE;
              break;
            case 1:
              currentFilter = FILTER_TIMER;
              break;
            case 2:
              currentFilter = FILTER_MESSAGE;
              break;
            default:
              throw new IllegalArgumentException("No such index");
          }

          renderFiltered();
        }
      });
      filterPanel.add(dropBox);

      toolBox.add(filterPanel, new BoxLayoutData(BoxLayoutData.FillStyle.VERTICAL));

      this.jobList.add(toolBox, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));
      this.jobList.add(listBox, new BoxLayoutData(BoxLayoutData.FillStyle.BOTH));

      // details
      /*JobDetailView detailsView = new JobDetailView();
      controller.addView(JobDetailView.ID, detailsView);
      controller.addAction(UpdateJobDetailAction.ID, new UpdateJobDetailAction());
      layout.add(detailsView, new BorderLayoutData(BorderLayout.Region.SOUTH, 10,200));
      */

      Timer t = new Timer()
      {
        @Override
        public void run()
        {
          controller.handleEvent(new Event(UpdateJobsAction.ID, null));
        }
      };

      t.schedule(500);
      
      controller.addAction(UpdateJobsAction.ID, new UpdateJobsAction());

      this.initialized = true;
    }
  }

  public void reset()
  {
    final DefaultListModel<JobRef> model =
        (DefaultListModel<JobRef>) listBox.getModel();

    model.clear();

  }

  public void update(Object... data)
  {
    this.jobs = (List<JobRef>)data[0];
    renderFiltered();
  }

  public void setLoading(boolean isLoading)
  {
    LoadingOverlay.on(jobList, isLoading);
  }

  private void renderFiltered()
  {
    if(this.jobs!=null)
    {
      reset();
      
      final DefaultListModel<JobRef> model =
          (DefaultListModel<JobRef>) listBox.getModel();

      for(JobRef def : jobs)
      {
        if(FILTER_NONE==currentFilter)
        {
          model.add(def);
        }
        else
        {
          if(FILTER_TIMER==currentFilter
              && def.getType().equals("timer"))
          {
            model.add(def);
          }
          else if(FILTER_MESSAGE==currentFilter
              && def.getType().equals("message"))
          {
            model.add(def);
          }
        }
      }

      if(listBox.getSelectedIndex()!=-1)
        listBox.setItemSelected(listBox.getSelectedIndex(), false);

      // clear details
      /* controller.handleEvent(
         new Event(UpdateJobDetailAction.ID, null)
     ); */
    }
  }

  public JobRef getSelection()
  {
    JobRef selection = null;
    if(isInitialized() && listBox.getSelectedIndex()!=-1)
      selection = listBox.getItem( listBox.getSelectedIndex());
    return selection;
  }
}
