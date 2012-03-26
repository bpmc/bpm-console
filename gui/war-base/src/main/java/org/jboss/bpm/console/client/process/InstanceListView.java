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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.*;
import org.gwt.mosaic.ui.client.ListBox;
import org.gwt.mosaic.ui.client.MessageBox;
import org.gwt.mosaic.ui.client.ToolBar;
import org.gwt.mosaic.ui.client.event.RowSelectionEvent;
import org.gwt.mosaic.ui.client.event.RowSelectionHandler;
import org.gwt.mosaic.ui.client.layout.BorderLayout;
import org.gwt.mosaic.ui.client.layout.BorderLayoutData;
import org.gwt.mosaic.ui.client.layout.BoxLayout;
import org.gwt.mosaic.ui.client.layout.BoxLayoutData;
import org.gwt.mosaic.ui.client.layout.MosaicPanel;
import org.gwt.mosaic.ui.client.list.DefaultListModel;
import org.jboss.bpm.console.client.ApplicationContext;
import org.jboss.bpm.console.client.common.DataDriven;
import org.jboss.bpm.console.client.common.IFrameWindowCallback;
import org.jboss.bpm.console.client.common.IFrameWindowPanel;
import org.jboss.bpm.console.client.common.LoadingOverlay;
import org.jboss.bpm.console.client.common.WidgetWindowPanel;
import org.jboss.bpm.console.client.icons.ConsoleIconBundle;
import org.jboss.bpm.console.client.model.ProcessDefinitionRef;
import org.jboss.bpm.console.client.model.ProcessInstanceRef;
import org.jboss.bpm.console.client.model.TokenReference;
import org.jboss.bpm.console.client.process.events.InstanceEvent;
import org.jboss.bpm.console.client.process.events.SignalInstanceEvent;
import org.jboss.bpm.console.client.util.SimpleDateFormat;
import org.jboss.errai.workspaces.client.api.ProvisioningCallback;
import org.jboss.errai.workspaces.client.api.WidgetProvider;
import org.jboss.errai.workspaces.client.framework.Registry;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.mvc4g.client.Controller;
import com.mvc4g.client.Event;
import com.mvc4g.client.ViewInterface;

/**
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
public class InstanceListView implements WidgetProvider, ViewInterface, DataDriven
{
    public final static String ID = InstanceListView.class.getName();

    private Controller controller;

    private MosaicPanel instanceList = null;

    private ListBox<ProcessInstanceRef> listBox;

    private ProcessDefinitionRef currentDefinition;

    private boolean isInitialized;

    private List<ProcessInstanceRef> cachedInstances = null;

    private SimpleDateFormat dateFormat = new SimpleDateFormat();

    private ApplicationContext appContext;

    private IFrameWindowPanel iframeWindow = null;

    private boolean isRiftsawInstance;

    //private PagingPanel pagingPanel;

    MosaicPanel panel;

    private Button startBtn, terminateBtn, deleteBtn, signalBtn, refreshBtn;

    // elements needed to signal waiting execution
    private List<TokenReference> tokensToSignal = null;

    private WidgetWindowPanel signalWindowPanel;

    private ListBox<TokenReference> listBoxTokens = null;

    private List<TextBox> signalTextBoxes = null;

    private ListBox<String> listBoxTokenSignals;

    private ImageResource greenIcon;

    public void provideWidget(ProvisioningCallback callback)
    {

        this.appContext = Registry.get(ApplicationContext.class);
        // riftsaw?
        isRiftsawInstance = appContext.getConfig().getProfileName().equals("BPEL Console");

        panel = new MosaicPanel();
        panel.setPadding(0);

        Registry.get(Controller.class).addView(ID, this);
        initialize();

        ConsoleIconBundle imageBundle = (ConsoleIconBundle)GWT.create(ConsoleIconBundle.class);
        greenIcon = imageBundle.greenIcon();

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

            listBox =
                    new ListBox<ProcessInstanceRef>(
                            new String[] {
                                    "<b>Instance</b>", "State", "Start Date"}
                    );

            listBox.setCellRenderer(new ListBox.CellRenderer<ProcessInstanceRef>() {
                public void renderCell(ListBox<ProcessInstanceRef> listBox, int row, int column,
                                       ProcessInstanceRef item) {
                    switch (column) {
                        case 0:
                            listBox.setText(row, column, item.getId());
                            break;
                        case 1:
                            listBox.setText(row, column, item.getState().toString());
                            break;
                        case 2:
                            String d = item.getStartDate() != null ? dateFormat.format(item.getStartDate()) : "";
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
                            int index = listBox.getSelectedIndex();
                            if(index!=-1)
                            {
                                ProcessInstanceRef item = listBox.getItem(index);

                                // enable or disable signal button depending on current activity
                                if (isSignalable(item)) {
                                    signalBtn.setEnabled(true);
                                } else {
                                    signalBtn.setEnabled(false);
                                }

                                terminateBtn.setEnabled(true);

                                // update details
                                controller.handleEvent(
                                        new Event(UpdateInstanceDetailAction.ID,
                                                new InstanceEvent(currentDefinition, item)
                                        )
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
            refreshBtn = new Button("Refresh", new ClickHandler() {

                public void onClick(ClickEvent clickEvent) {
                    controller.handleEvent(
                            new Event(
                                    UpdateInstancesAction.ID,
                                    getCurrentDefinition()
                            )
                    );
                }
            }
            );
            toolBar.add(refreshBtn);
            refreshBtn.setEnabled(false);
            toolBar.addSeparator();

            startBtn = new Button("Start", new ClickHandler()
            {
                public void onClick(ClickEvent clickEvent)
                {
                    MessageBox.confirm("Start new execution",
                            "Do you want to start a new execution of this process?",
                            new MessageBox.ConfirmationCallback()
                            {
                                public void onResult(boolean doIt)
                                {
                                    if (doIt)
                                    {
                                        String url = getCurrentDefinition().getFormUrl();
                                        boolean hasForm = (url != null && !url.equals(""));
                                        if (hasForm)
                                        {
                                            ProcessDefinitionRef definition = getCurrentDefinition();
                                            iframeWindow = new IFrameWindowPanel(
                                                    definition.getFormUrl(), "New Process Instance: " + definition.getId()
                                            );

                                            iframeWindow.setCallback(
                                                    new IFrameWindowCallback()
                                                    {
                                                        public void onWindowClosed()
                                                        {
                                                            controller.handleEvent(
                                                                    new Event(UpdateInstancesAction.ID, getCurrentDefinition())
                                                            );
                                                        }
                                                    }
                                            );

                                            iframeWindow.show();
                                        }
                                        else
                                        {
                                            controller.handleEvent(
                                                    new Event(
                                                            StartNewInstanceAction.ID,
                                                            getCurrentDefinition()
                                                    )
                                            );
                                        }
                                    }

                                }
                            });

                }
            }
            );


            terminateBtn = new Button("Terminate", new ClickHandler()
            {
                public void onClick(ClickEvent clickEvent)
                {
                    if (getSelection() != null)
                    {

                        MessageBox.confirm("Terminate instance",
                                "Terminating this instance will stop further execution.",
                                new MessageBox.ConfirmationCallback()
                                {
                                    public void onResult(boolean doIt)
                                    {
                                        if (doIt)
                                        {
                                            ProcessInstanceRef selection = getSelection();
                                            selection.setState(ProcessInstanceRef.STATE.ENDED);
                                            selection.setEndResult(ProcessInstanceRef.RESULT.OBSOLETE);
                                            controller.handleEvent(
                                                    new Event(
                                                            StateChangeAction.ID,
                                                            selection
                                                    )
                                            );
                                        }
                                    }
                                });
                    }
                    else
                    {
                        MessageBox.alert("Missing selection", "Please select an instance");
                    }
                }
            }
            );            


            deleteBtn = new Button("Delete", new ClickHandler()
            {
                public void onClick(ClickEvent clickEvent)
                {
                    if (getSelection() != null)
                    {
                        MessageBox.confirm("Delete instance",
                                "Deleting this instance will remove any history information and associated tasks as well.",
                                new MessageBox.ConfirmationCallback()
                                {
                                    public void onResult(boolean doIt)
                                    {

                                        if (doIt)
                                        {
                                            ProcessInstanceRef selection = getSelection();
                                            selection.setState(ProcessInstanceRef.STATE.ENDED);

                                            controller.handleEvent(
                                                    new Event(
                                                            DeleteInstanceAction.ID,
                                                            selection
                                                    )
                                            );
                                        }
                                    }
                                });

                    }
                    else
                    {
                        MessageBox.alert("Missing selection", "Please select an instance");
                    }
                }
            }
            );

            signalBtn = new Button("Signal", new ClickHandler()
            {
                public void onClick(ClickEvent clickEvent)
                {
                    createSignalWindow();
                }
            }
            );

            if(!isRiftsawInstance)  // riftsaw doesn't support instance operations
            {
                toolBar.add(startBtn);
                toolBar.add(signalBtn);
                toolBar.add(deleteBtn);

                startBtn.setEnabled(false);
                deleteBtn.setEnabled(false);
                signalBtn.setEnabled(false);
            }

            // terminate works on any BPM Engine
            toolBar.add(terminateBtn);
            terminateBtn.setEnabled(false);

            toolBox.add(toolBar, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));


            instanceList.add(toolBox, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));
            instanceList.add(listBox, new BoxLayoutData(BoxLayoutData.FillStyle.BOTH));

            /*pagingPanel = new PagingPanel(
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
          instanceList.add(pagingPanel, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));*/

            // cached data?
            if(this.cachedInstances!=null)
                bindData(this.cachedInstances);

            // layout
            MosaicPanel layout = new MosaicPanel(new BorderLayout());
            layout.setPadding(0);
            layout.add(instanceList, new BorderLayoutData(BorderLayout.Region.CENTER));

            // details
            InstanceDetailView detailsView = new InstanceDetailView();
            controller.addView(InstanceDetailView.ID, detailsView);
            controller.addAction(UpdateInstanceDetailAction.ID, new UpdateInstanceDetailAction());
            controller.addAction(ClearInstancesAction.ID, new ClearInstancesAction());
            controller.addAction(SignalExecutionAction.ID, new SignalExecutionAction());
            layout.add(detailsView, new BorderLayoutData(BorderLayout.Region.SOUTH,10,200));

            panel.add(layout);

            isInitialized = true;

        }
    }

    public ProcessInstanceRef getSelection()
    {
        ProcessInstanceRef selection = null;
        if(listBox.getSelectedIndex()!=-1)
            selection = listBox.getItem( listBox.getSelectedIndex());
        return selection;
    }

    public ProcessDefinitionRef getCurrentDefinition()
    {
        return this.currentDefinition;
    }

    public void setController(Controller controller)
    {
        this.controller = controller;
    }

    public void reset()
    {
        this.currentDefinition = null;
        this.cachedInstances = new ArrayList<ProcessInstanceRef>();
        renderUpdate();

        startBtn.setEnabled(false);
        terminateBtn.setEnabled(false);
        deleteBtn.setEnabled(false);
        signalBtn.setEnabled(false);
        refreshBtn.setEnabled(false);
    }

    public void update(Object... data)
    {
        this.currentDefinition = (ProcessDefinitionRef)data[0];
        this.cachedInstances = (List<ProcessInstanceRef>)data[1];

        //if(isInitialized()) pagingPanel.reset();
        renderUpdate();
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

            // clear details
            controller.handleEvent(
                    new Event(UpdateInstanceDetailAction.ID,
                            new InstanceEvent(this.currentDefinition, null)
                    )
            );

            startBtn.setEnabled(true);            
            deleteBtn.setEnabled(true);
            refreshBtn.setEnabled(true);
        }
    }

    private void bindData(List<ProcessInstanceRef> instances)
    {
        final DefaultListModel<ProcessInstanceRef> model =
                (DefaultListModel<ProcessInstanceRef>) listBox.getModel();
        model.clear();

        List<ProcessInstanceRef> list = instances;//pagingPanel.trim(instances);
        for(ProcessInstanceRef inst : list)
        {
        	model.add(inst);
        }

        // layout again
        panel.invalidate();
    }

    private boolean isSignalable(ProcessInstanceRef processInstance) {

        tokensToSignal = new ArrayList<TokenReference>();

        // first check if the parent execution is signalable
        if (processInstance.getRootToken() != null && processInstance.getRootToken().canBeSignaled()) {
            tokensToSignal.add(processInstance.getRootToken());

        } else if (processInstance.getRootToken() != null && processInstance.getRootToken().getChildren() != null) {
            // next verify children
            collectSignalableTokens(processInstance.getRootToken(), tokensToSignal);
        }

        if (tokensToSignal.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    private void collectSignalableTokens(TokenReference tokenParent, List<TokenReference> tokensToSignal) {
        if (tokenParent.getChildren() != null) {
            for (TokenReference token : tokenParent.getChildren()) {
                if (token.canBeSignaled()) {
                    tokensToSignal.add(token);
                }

                collectSignalableTokens(token, tokensToSignal);
            }
        }
    }

    private void createSignalWindow()
    {
        signalTextBoxes = new ArrayList<TextBox>();

        MosaicPanel layout = new MosaicPanel(new BoxLayout(BoxLayout.Orientation.VERTICAL));
        layout.setStyleName("bpm-window-layout");
        layout.setPadding(5);
        // toolbar
        final MosaicPanel toolBox = new MosaicPanel();

        toolBox.setPadding(0);
        toolBox.setWidgetSpacing(5);
        toolBox.setLayout(new BoxLayout(BoxLayout.Orientation.HORIZONTAL));

        final ToolBar toolBar = new ToolBar();
        toolBar.add(
                new Button("Signal", new ClickHandler() {

                    public void onClick(ClickEvent clickEvent)
                    {
                        int selectedToken = listBoxTokens.getSelectedIndex();
                        int selectedSignal = listBoxTokenSignals.getSelectedIndex();
                        if (selectedToken != -1 && selectedSignal != -1) {

                            controller.handleEvent(
                                    new Event(SignalExecutionAction.ID,
                                            new SignalInstanceEvent(getCurrentDefinition(), getSelection(), listBoxTokens.getItem(selectedToken), listBoxTokenSignals.getItem(selectedSignal), selectedToken)));

                        } else {
                            MessageBox.alert("Incomplete selection", "Please select both token and signal name");
                        }


                    }
                }
                )
        );

        toolBar.add(
                new Button("Cancel", new ClickHandler() {

                    public void onClick(ClickEvent clickEvent)
                    {

                        signalWindowPanel.close();
                    }
                }
                )
        );

        Label header = new Label("Available tokens to signal: ");
        header.setStyleName("bpm-label-header");
        layout.add(header, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));

        toolBox.add(toolBar, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));

        layout.add(toolBox, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));

        listBoxTokens = new ListBox<TokenReference>(new String[] { "Id", "Name" });


        listBoxTokens.setCellRenderer(new ListBox.CellRenderer<TokenReference>() {

            public void renderCell(ListBox<TokenReference> listBox, int row, int column, TokenReference item) {
                switch (column) {
                    case 0:
                        listBox.setText(row, column, item.getId());
                        break;
                    case 1:
                        listBox.setText(row, column, item.getName() == null ? item.getCurrentNodeName() : item.getName());
                        break;
                    default:
                        throw new RuntimeException("Unexpected column size");
                }
            }
        });

        listBoxTokens.addRowSelectionHandler(
                new RowSelectionHandler()
                {
                    public void onRowSelection(RowSelectionEvent rowSelectionEvent)
                    {
                        int index = listBoxTokens.getSelectedIndex();
                        if(index!=-1)
                        {
                            TokenReference item = listBoxTokens.getItem(index);
                            renderAvailableSignals(item);

                        }
                    }
                }
        );

        renderSignalListBox(-1);
        layout.add(listBoxTokens, new BoxLayoutData(BoxLayoutData.FillStyle.BOTH));

        Label headerSignals = new Label("Available signal names");
        headerSignals.setStyleName("bpm-label-header");
        layout.add(headerSignals, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));

        listBoxTokenSignals = new ListBox<String>(new String[] { "Signal name" });


        listBoxTokenSignals.setCellRenderer(new ListBox.CellRenderer<String>() {

            public void renderCell(ListBox<String> listBox, int row, int column, String item) {
                switch (column) {
                    case 0:
                        listBox.setText(row, column, item);
                        break;

                    default:
                        throw new RuntimeException("Unexpected column size");
                }
            }
        });



        layout.add(listBoxTokenSignals, new BoxLayoutData(BoxLayoutData.FillStyle.BOTH));

        signalWindowPanel = new WidgetWindowPanel(
                "Signal proces from wait state",
                layout, true
        );

    }

    public void renderSignalListBox(int i) {
        // remove currently signaled token
        if (i > -1) {
            tokensToSignal.remove(i);
        }

        // if available token list is empty close window
        if (tokensToSignal.isEmpty()) {
            signalWindowPanel.close();
        }

        // display all remaining token possible to signal
        ((DefaultListModel<TokenReference>)listBoxTokens.getModel()).clear();
        for (TokenReference token : tokensToSignal) {
            ((DefaultListModel<TokenReference>)listBoxTokens.getModel()).add(token);
        }

        // clear available signal list box
        if (listBoxTokenSignals != null) {
            ((DefaultListModel<String>)listBoxTokenSignals.getModel()).clear();
        }
    }

    private void renderAvailableSignals(TokenReference item) {
        ((DefaultListModel<String>)listBoxTokenSignals.getModel()).clear();
        for (String signal : item.getAvailableSignals()) {
            ((DefaultListModel<String>)listBoxTokenSignals.getModel()).add(signal);
        }
    }

}
