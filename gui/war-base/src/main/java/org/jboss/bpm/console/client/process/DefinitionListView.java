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

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.mvc4g.client.Controller;
import com.mvc4g.client.Event;
import com.mvc4g.client.ViewInterface;
import org.gwt.mosaic.ui.client.ListBox;
import org.gwt.mosaic.ui.client.ToolBar;
import org.gwt.mosaic.ui.client.event.RowSelectionEvent;
import org.gwt.mosaic.ui.client.event.RowSelectionHandler;
import org.gwt.mosaic.ui.client.layout.BoxLayout;
import org.gwt.mosaic.ui.client.layout.BoxLayoutData;
import org.gwt.mosaic.ui.client.layout.MosaicPanel;
import org.gwt.mosaic.ui.client.list.DefaultListModel;
import org.gwt.mosaic.ui.client.table.AbstractScrollTable;
import org.jboss.bpm.console.client.common.*;
import org.jboss.bpm.console.client.model.ProcessDefinitionRef;
import org.jboss.errai.bus.client.ErraiBus;
import org.jboss.errai.bus.client.api.Message;
import org.jboss.errai.bus.client.api.MessageCallback;
import org.jboss.errai.workspaces.client.api.ProvisioningCallback;
import org.jboss.errai.workspaces.client.api.WidgetProvider;
import org.jboss.errai.workspaces.client.framework.Registry;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
//@Deprecated still used by jBPM - un-deprecating
public class DefinitionListView implements WidgetProvider, ViewInterface, DataDriven
{
  public final static String ID = DefinitionListView.class.getName();

  private Controller controller;

  private MosaicPanel definitionList = null;

  private ListBox<ProcessDefinitionRef> listBox;

  private boolean isInitialized;

  private int FILTER_NONE       = 10;
  private int FILTER_ACTIVE     = 20;
  private int FILTER_SUSPENDED  = 30;
  private int currentFilter = FILTER_NONE;

  private List<ProcessDefinitionRef> definitions = null;
  private PagingPanel pagingPanel;

  private MosaicPanel panel;

  public void provideWidget(ProvisioningCallback callback)
  {

    panel = new MosaicPanel();
    panel.setWidgetSpacing(0);
    panel.setPadding(0);
    
    listBox = createListBox();
    final Controller controller = Registry.get(Controller.class);
    controller.addView(ID, this);

    controller.addAction(UpdateInstancesAction.ID, new UpdateInstancesAction());
    controller.addAction(StartNewInstanceAction.ID, new StartNewInstanceAction());
    controller.addAction(StateChangeAction.ID, new StateChangeAction());
    controller.addAction(DeleteDefinitionAction.ID, new DeleteDefinitionAction());
    controller.addAction(DeleteInstanceAction.ID, new DeleteInstanceAction());
    controller.addAction(UpdateDefinitionsAction.ID, new UpdateDefinitionsAction());

    initialize();

    Timer t = new Timer()
    {
      @Override
      public void run()
      {
        controller.handleEvent(
            new Event(UpdateDefinitionsAction.ID, null)
        );
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

      definitionList = new MosaicPanel( new BoxLayout(BoxLayout.Orientation.VERTICAL));
      definitionList.setPadding(0);
      definitionList.setWidgetSpacing(0);

      // toolbar

      final MosaicPanel toolBox = new MosaicPanel();
      toolBox.setPadding(0);
      toolBox.setWidgetSpacing(0);
      toolBox.setLayout(new BoxLayout(BoxLayout.Orientation.HORIZONTAL));

      // toolbar
      final ToolBar toolBar = new ToolBar();      
      ClickHandler clickHandler = new ClickHandler()
      {
        public void onClick(ClickEvent clickEvent)
        {
          reload();
        }
      };
      
      toolBar.add(
          new Button("Refresh", clickHandler
          )
      );

      toolBox.add(toolBar, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));

      // filter
      MosaicPanel filterPanel = new MosaicPanel(new BoxLayout(BoxLayout.Orientation.VERTICAL));
      filterPanel.setStyleName("mosaic-ToolBar");
      final com.google.gwt.user.client.ui.ListBox dropBox = new com.google.gwt.user.client.ui.ListBox(false);
      dropBox.setStyleName("bpm-operation-ui");
      dropBox.addItem("All");
      dropBox.addItem("Active");
      dropBox.addItem("Retired");

      dropBox.addChangeHandler(new ChangeHandler() {

        public void onChange(ChangeEvent changeEvent)
        {          
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

      definitionList.add(toolBox, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));
      definitionList.add(listBox, new BoxLayoutData(BoxLayoutData.FillStyle.BOTH));
      pagingPanel = new PagingPanel(
          new PagingCallback()
          {
            public void rev()
            {
              renderFiltered();
            }

            public void ffw()
            {
              renderFiltered();
            }
          }
      );
      definitionList.add(pagingPanel,new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));

      // layout
      //MosaicPanel layout = new MosaicPanel(new BorderLayout());
      //layout.add(definitionList, new BorderLayoutData(BorderLayout.Region.CENTER));

      // details
      /*ProcessDetailView detailsView = new ProcessDetailView();
      controller.addView(ProcessDetailView.ID, detailsView);
      controller.addAction(UpdateProcessDetailAction.ID, new UpdateProcessDetailAction());
      layout.add(detailsView, new BorderLayoutData(BorderLayout.Region.SOUTH, 10,200));*/

      //panel.add(layout);

      panel.add(definitionList);

      // deployments model listener
      ErraiBus.get().subscribe(Model.SUBJECT,
          new MessageCallback()
      {
        public void callback(Message message)
        {
          switch (ModelCommands.valueOf(message.getCommandType()))
          {
            case HAS_BEEN_UPDATED:
              if(message.get(String.class, ModelParts.CLASS).equals(Model.DEPLOYMENT_MODEL))
                reload();
              break;
          }
        }
      });

      isInitialized = true;
    }
  }

  private void reload()
  {
    DeferredCommand.addCommand(
        new Command()
        {
          public void execute()
          {
            final DefaultListModel<ProcessDefinitionRef> model =
                (DefaultListModel<ProcessDefinitionRef>) listBox.getModel();
            model.clear();

            // force loading
            controller.handleEvent(
                new Event(UpdateDefinitionsAction.ID, null)
            );
          }
        }
    );
  }

  private ListBox createListBox()
  {
    final ListBox<ProcessDefinitionRef> listBox =
        new ListBox<ProcessDefinitionRef>(
            new String[] {
                "<b>Process</b>", "v."//, "Version", "Suspended"
            }
        );
    
    listBox.setFocus(true);    

    listBox.setCellRenderer(new ListBox.CellRenderer<ProcessDefinitionRef>() {
      public void renderCell(ListBox<ProcessDefinitionRef> listBox, int row, int column,
                             ProcessDefinitionRef item) {
        switch (column) {
          case 0:
            
            String name = item.getName();
            String s = name.indexOf("}") > 0 ?
                name.substring(name.lastIndexOf("}")+1, name.length()) : name;

            String color= item.isSuspended() ? "#CCCCCC" : "#000000";
            String text = "<div style=\"color:"+color+"\">"+ s +"</div>";            

            listBox.setWidget(row, column, new HTML(text));
            break;
          case 1:
            listBox.setText(row, column, String.valueOf(item.getVersion()));
            break;
          case 2:
            listBox.setText(row, column, String.valueOf(item.isSuspended()));
            break;
          default:
            throw new RuntimeException("Unexpected column size");
        }
      }
    });

    listBox.setMinimumColumnWidth(0, 190);
    listBox.setColumnResizePolicy(AbstractScrollTable.ColumnResizePolicy.MULTI_CELL);
    
    listBox.addRowSelectionHandler(
        new RowSelectionHandler()
        {
          public void onRowSelection(RowSelectionEvent rowSelectionEvent)
          {
            int index = listBox.getSelectedIndex();
            if(index!=-1)
            {
              ProcessDefinitionRef item = listBox.getItem(index);


              // update details
              /*controller.handleEvent(
                  new Event(UpdateProcessDetailAction.ID, item)
              );*/

              // load instances
              controller.handleEvent(
                  new Event(
                      UpdateInstancesAction.ID,
                      item
                  )
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

  public void reset()
  {
    final DefaultListModel<ProcessDefinitionRef> model =
        (DefaultListModel<ProcessDefinitionRef>) listBox.getModel();

    model.clear();

    // clear instance panel
    controller.handleEvent(new Event(ClearInstancesAction.ID, null));
    
    // clear details
    /*controller.handleEvent(
        new Event(UpdateProcessDetailAction.ID, null)
    );*/

  }

  public void update(Object... data)
  {
    this.definitions = (List<ProcessDefinitionRef>)data[0];
    pagingPanel.reset();
    renderFiltered();
  }

  public void setLoading(boolean isLoading)
  {
    LoadingOverlay.on(panel, isLoading);
  }

  private void renderFiltered()
  {
    if(this.definitions!=null)
    {
      reset();

      final DefaultListModel<ProcessDefinitionRef> model =
          (DefaultListModel<ProcessDefinitionRef>) listBox.getModel();      

      List<ProcessDefinitionRef> tmp = new ArrayList<ProcessDefinitionRef>();
      for(ProcessDefinitionRef def : definitions)
      {
        if(FILTER_NONE==currentFilter)
        {
          tmp.add(def);
        }
        else
        {
          boolean showSuspended = (FILTER_SUSPENDED==currentFilter);
          if(def.isSuspended()==showSuspended)
            tmp.add(def);
        }
      }

      for(ProcessDefinitionRef def : (List<ProcessDefinitionRef>)pagingPanel.trim(tmp) )
        model.add(def);

      if(listBox.getSelectedIndex()!=-1)
        listBox.setItemSelected(listBox.getSelectedIndex(), false);

    }
  }

  public ProcessDefinitionRef getSelection()
  {
    ProcessDefinitionRef selection = null;
    if(isInitialized() && listBox.getSelectedIndex()!=-1)
      selection = listBox.getItem( listBox.getSelectedIndex());
    return selection;
  }

}
