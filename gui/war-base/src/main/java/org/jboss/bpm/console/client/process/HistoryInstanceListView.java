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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
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
import org.jboss.bpm.console.client.common.*;
import org.jboss.bpm.console.client.model.HistoryActivityInstanceRef;
import org.jboss.bpm.console.client.model.HistoryProcessInstanceRef;
import org.jboss.bpm.console.client.model.ProcessDefinitionRef;
import org.jboss.bpm.console.client.process.events.HistoryActivityDiagramEvent;
import org.jboss.bpm.console.client.util.SimpleDateFormat;
import org.jboss.errai.workspaces.client.api.ProvisioningCallback;
import org.jboss.errai.workspaces.client.api.WidgetProvider;
import org.jboss.errai.workspaces.client.framework.Registry;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Maciej Swiderski <swiderski.maciej@gmail.com>
 */
public class HistoryInstanceListView implements WidgetProvider, ViewInterface, DataDriven
{
  public final static String ID = HistoryInstanceListView.class.getName();

  private Controller controller;

  private MosaicPanel instanceList = null;

  private ListBox<HistoryProcessInstanceRef> listBoxHistory;

  private ListBox<HistoryActivityInstanceRef> listBoxInstanceActivity;

  private ProcessDefinitionRef currentDefinition;

  private boolean isInitialized;

  private List<HistoryProcessInstanceRef> cachedInstances = null;

  private List<HistoryActivityInstanceRef> cachedInstancesActivity = null;

  private List<String> executedActivities = null;

  private SimpleDateFormat dateFormat = new SimpleDateFormat();

  private ApplicationContext appContext;

  private PagingPanel pagingPanel;

  MosaicPanel panel;

  private Button diagramBtn;

  private WidgetWindowPanel diagramWindowPanel;

  private ActivityDiagramView diagramView;

  public void provideWidget(ProvisioningCallback callback)
  {

    this.appContext = Registry.get(ApplicationContext.class);
    
    panel = new MosaicPanel();
    panel.setPadding(0);

    Registry.get(Controller.class).addView(ID, this);
    initialize();

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
      instanceList = new MosaicPanel( new BoxLayout(BoxLayout.Orientation.VERTICAL));
      instanceList.setPadding(0);
      instanceList.setWidgetSpacing(0);

      // create history list box elements
      listBoxHistory = createHistoryListBox();
      // create list of activities executed for currently selected history process instance
      this.listBoxInstanceActivity = createHistoryActivitiesListBox();

      // toolbar
      final MosaicPanel toolBox = new MosaicPanel();

      toolBox.setPadding(0);
      toolBox.setWidgetSpacing(5);
      toolBox.setLayout(new BoxLayout(BoxLayout.Orientation.HORIZONTAL));

      final ToolBar toolBar = new ToolBar();
      toolBar.add(
          new Button("Refresh", new ClickHandler() {

            public void onClick(ClickEvent clickEvent)
            {

              controller.handleEvent(
                  new Event(
                      UpdateHistoryDefinitionAction.ID,
                      getCurrentDefinition()
                  )
              );
            }
          }
          )
      );

      diagramBtn = new Button("Diagram", new ClickHandler()
      {
        public void onClick(ClickEvent clickEvent)
        {
          String diagramUrl = currentDefinition.getDiagramUrl();
          if (currentDefinition != null && executedActivities != null) {
            HistoryActivityDiagramEvent eventData = new HistoryActivityDiagramEvent(currentDefinition, executedActivities);
            if(diagramUrl !=null && !diagramUrl.equals(""))
            {
              createDiagramWindow();
              controller.handleEvent(
                  new Event(LoadHistoryDiagramAction.ID, eventData)
              );

            }
            else
            {
              MessageBox.alert("Incomplete deployment", "No diagram associated with process");
            }
          }
        }
      }
      );



      // terminate works on any BPM Engine
      toolBar.add(diagramBtn);
      diagramBtn.setEnabled(false);

      toolBox.add(toolBar, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));


      instanceList.add(toolBox, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));
      instanceList.add(listBoxHistory, new BoxLayoutData(BoxLayoutData.FillStyle.BOTH));

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
      instanceList.add(pagingPanel, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));
      instanceList.add(listBoxInstanceActivity, new BoxLayoutData(BoxLayoutData.FillStyle.BOTH));


      // cached data?
      if(this.cachedInstances!=null)
        bindData(this.cachedInstances);

      // layout
      MosaicPanel layout = new MosaicPanel(new BorderLayout());
      layout.setPadding(0);
      layout.add(instanceList, new BorderLayoutData(BorderLayout.Region.CENTER));


      panel.add(layout);

      isInitialized = true;

      this.executedActivities = new ArrayList<String>();

    }
  }

  public HistoryProcessInstanceRef getSelection()
  {
    HistoryProcessInstanceRef selection = null;
    if(listBoxHistory.getSelectedIndex()!=-1)
      selection = listBoxHistory.getItem( listBoxHistory.getSelectedIndex());
    return selection;
  }

  public ProcessDefinitionRef getCurrentDefinition()
  {
    return this.currentDefinition;
  }

  public void setController(Controller controller)
  {
    this.controller = controller;

    this.diagramView = new ActivityDiagramView();

    controller.addView(ActivityDiagramView.ID, diagramView);
  }

  public void reset()
  {
    this.currentDefinition = null;
    this.cachedInstances = new ArrayList<HistoryProcessInstanceRef>();
    renderUpdate();

    diagramBtn.setEnabled(false);
  }

  public void update(Object... data)
  {
    if (data[0] instanceof ProcessDefinitionRef) {
      // fill in list box for finished process instances for current definition
      this.currentDefinition = (ProcessDefinitionRef)data[0];
      this.cachedInstances = (List<HistoryProcessInstanceRef>)data[1];

      //if(isInitialized()) pagingPanel.reset();
      renderUpdate();

      //clear activity list box
      final DefaultListModel<HistoryActivityInstanceRef> model =
          (DefaultListModel<HistoryActivityInstanceRef>) listBoxInstanceActivity.getModel();

      model.clear();
      diagramBtn.setEnabled(false);
    }
    else
    {
      // fill in list box of activities executed for currently selected process instance
      this.cachedInstancesActivity = (List<HistoryActivityInstanceRef>) data[0];

      renderHistoryActivityList();
    }
  }

  public void setLoading(boolean isLoading)
  {
    LoadingOverlay.on(instanceList, isLoading);
  }

  private void renderUpdate()
  {
    if(isInitialized())
    {
      bindData(this.cachedInstances);


    }
  }

  private void bindData(List<HistoryProcessInstanceRef> instances)
  {
    final DefaultListModel<HistoryProcessInstanceRef> model =
        (DefaultListModel<HistoryProcessInstanceRef>) listBoxHistory.getModel();
    model.clear();

    List<HistoryProcessInstanceRef> list = pagingPanel.trim(instances);
    for(HistoryProcessInstanceRef inst : list)
    {
      model.add(inst);
    }

    // layout again
    panel.invalidate();
  }

  private void renderHistoryActivityList()
  {

    if(this.cachedInstancesActivity!=null)
    {

      final DefaultListModel<HistoryActivityInstanceRef> model =
          (DefaultListModel<HistoryActivityInstanceRef>) listBoxInstanceActivity.getModel();

      model.clear();
      this.executedActivities.clear();

      for(HistoryActivityInstanceRef def : cachedInstancesActivity)
      {

        model.add(def);
        this.executedActivities.add(def.getActivityName());

      }

      if(listBoxInstanceActivity.getSelectedIndex()!=-1)
        listBoxInstanceActivity.setItemSelected(listBoxInstanceActivity.getSelectedIndex(), false);

    }
  }


  protected ListBox<HistoryProcessInstanceRef> createHistoryListBox() {
    listBoxHistory = new ListBox<HistoryProcessInstanceRef>(new String[] { "<b>Instance</b>", "State", "Start Date", "End Date", "Duration" });

    listBoxHistory.setCellRenderer(new ListBox.CellRenderer<HistoryProcessInstanceRef>() {

      public void renderCell(ListBox<HistoryProcessInstanceRef> listBox, int row, int column, HistoryProcessInstanceRef item) {
        switch (column) {
          case 0:
            listBox.setText(row, column, item.getProcessInstanceId());
            break;
          case 1:
            listBox.setText(row, column, item.getState().toString());
            break;
          case 2:
            String d = item.getStartTime() != null ? dateFormat.format(item.getStartTime()) : "";
            listBox.setText(row, column, d);
            break;
          case 3:
            String de = item.getEndTime() != null ? dateFormat.format(item.getEndTime()) : "";
            listBox.setText(row, column, de);
            break;
          case 4:
            listBox.setText(row, column, String.valueOf(item.getDuration()));
            break;
          default:
            throw new RuntimeException("Unexpected column size");
        }
      }
    });

    listBoxHistory.addRowSelectionHandler(new RowSelectionHandler() {

      public void onRowSelection(RowSelectionEvent rowSelectionEvent) {
        int index = listBoxHistory.getSelectedIndex();
        if (index != -1) {
          HistoryProcessInstanceRef item = listBoxHistory.getItem(index);

          // update details
          controller.handleEvent(new Event(UpdateHistoryInstanceAction.ID,  item.getProcessInstanceId()));

          diagramBtn.setEnabled(true);
        }
      }
    });

    return listBoxHistory;
  }

  private ListBox<HistoryActivityInstanceRef> createHistoryActivitiesListBox()
  {
    final ListBox<HistoryActivityInstanceRef> listBox =
        new ListBox<HistoryActivityInstanceRef>(
            new String[] {
                "ActivityName", "StartTime", "EndTime", "Duration"
            }
        );
    
    listBox.setCellRenderer(new ListBox.CellRenderer<HistoryActivityInstanceRef>() {
      public void renderCell(ListBox<HistoryActivityInstanceRef> listBox, int row, int column,
                             HistoryActivityInstanceRef item) {
        switch (column) {
          case 0:
            listBox.setText(row, column, item.getActivityName());
            break;
          case 1:
            String dateString = item.getStartTime()!=null ? dateFormat.format(item.getStartTime()) : "";
            listBox.setText(row, column, dateString);
            break;
          case 2:
            dateString = item.getEndTime()!=null ? dateFormat.format(item.getEndTime()) : "";
            listBox.setText(row, column, dateString);
            break;
          case 3:
            listBox.setText(row, column, String.valueOf(item.getDuration()));
            break;
          default:
            throw new RuntimeException("Unexpected column size");
        }
      }
    });



    return listBox;
  }

  private void createDiagramWindow()
  {

    MosaicPanel layout = new MosaicPanel(new BoxLayout(BoxLayout.Orientation.VERTICAL));
    layout.setStyleName("bpm-window-layout");
    layout.setPadding(5);

    Label header = new Label("Instance: ");
    header.setStyleName("bpm-label-header");
    layout.add(header, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));

    layout.add(diagramView, new BoxLayoutData(BoxLayoutData.FillStyle.BOTH));

    diagramWindowPanel = new WidgetWindowPanel(
        "Process Instance Activity",
        layout, true
    );

  }
}

