/*
 * Copyright 2009 JBoss, a divison Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.bpm.console.client.monitor;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.mvc4g.client.Controller;
import com.mvc4g.client.Event;
import com.mvc4g.client.ViewInterface;
import org.gwt.mosaic.ui.client.LayoutPopupPanel;
import org.gwt.mosaic.ui.client.PopupMenu;
import org.gwt.mosaic.ui.client.ToolBar;
import org.gwt.mosaic.ui.client.ToolButton;
import org.gwt.mosaic.ui.client.layout.*;
import org.gwt.mosaic.ui.client.list.DefaultListModel;
import org.gwt.mosaic.ui.client.util.ResizableWidget;
import org.gwt.mosaic.ui.client.util.ResizableWidgetCollection;
import org.jboss.bpm.console.client.common.LoadingOverlay;
import org.jboss.bpm.console.client.model.ProcessDefinitionRef;
import org.jboss.bpm.console.client.model.StringRef;
import org.jboss.bpm.console.client.util.ConsoleLog;
import org.jboss.bpm.console.client.util.SimpleDateFormat;
import org.jboss.bpm.monitor.gui.client.ChronoscopeFactory;
import org.jboss.bpm.monitor.gui.client.JSOModel;
import org.jboss.bpm.monitor.gui.client.TimespanValues;
import org.jboss.errai.bus.client.ErraiBus;
import org.jboss.errai.bus.client.api.Message;
import org.jboss.errai.bus.client.api.MessageCallback;
import org.jboss.errai.workspaces.client.api.ProvisioningCallback;
import org.jboss.errai.workspaces.client.api.WidgetProvider;
import org.jboss.errai.workspaces.client.api.annotations.LoadTool;
import org.jboss.errai.workspaces.client.framework.Registry;
import org.timepedia.chronoscope.client.Dataset;
import org.timepedia.chronoscope.client.Datasets;
import org.timepedia.chronoscope.client.Overlay;
import org.timepedia.chronoscope.client.XYPlot;
import org.timepedia.chronoscope.client.browser.ChartPanel;
import org.timepedia.chronoscope.client.browser.Chronoscope;
import org.timepedia.chronoscope.client.browser.json.GwtJsonDataset;
import org.timepedia.chronoscope.client.canvas.View;
import org.timepedia.chronoscope.client.canvas.ViewReadyCallback;
import org.timepedia.chronoscope.client.data.tuple.Tuple2D;
import org.timepedia.chronoscope.client.event.PlotFocusEvent;
import org.timepedia.chronoscope.client.event.PlotFocusHandler;
import org.timepedia.chronoscope.client.io.DatasetReader;
import org.timepedia.chronoscope.client.util.date.ChronoDate;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Heiko Braun <hbraun@redhat.com>
 * @author: Jeff Yu <cyu@redhat.com>
 * @date: Mar 11, 2010
 */                                                                                                          
@LoadTool(name="Execution History", group = "Processes")
public class ExecutionHistoryView implements WidgetProvider, ViewInterface
{

    public static final String ID = ExecutionHistoryView.class.getName();

    private static final String TIMEPEDIA_FONTBOOK_SERVICE = "http://api.timepedia.org/fr";

    private static volatile double GOLDEN__RATIO = 1.618;

    private ChartPanel chartPanel;
    private ToolButton menuButton;
    private ToolButton timespanButton;
    private HTML title;
    private HTML timespan;
    private LayoutPanel chartArea;
    private LayoutPanel timespanPanel;
    private Map<Long, Overlay> overlayMapping = new HashMap<Long, Overlay>();

    private SimpleDateFormat dateFormat = new SimpleDateFormat();

    private String currentProcDef;
    private TimespanValues currentTimespan;

    private LayoutPanel instancePanel;
    private org.gwt.mosaic.ui.client.ListBox<String> listBox;
    private LayoutPanel buttonPanel;
    private CheckBox includeFailed;

    private Controller controller;

    private final static int DATASET_COMPLETED = 0;
    private final static int DATASET_FAILED = 1;
    private final static int DATASET_TERMINATED = 2;

    private List<ProcessDefinitionRef> processDefinitions;
    
    public void provideWidget(ProvisioningCallback callback)
    {

        this.controller = Registry.get(Controller.class);

        controller.addView(ID, this);
        controller.addAction(GetProcessDefinitionsAction.ID, new GetProcessDefinitionsAction());
        controller.addAction(LoadDatasetsAction.ID, new LoadDatasetsAction());
        controller.addAction(LoadChartProcessInstancesAction.ID, new LoadChartProcessInstancesAction());

        LayoutPanel panel = new LayoutPanel(new BoxLayout(BoxLayout.Orientation.VERTICAL));
                
        final ToolBar toolBar = new ToolBar();
        panel.add(toolBar, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));

        // -----

        menuButton = new ToolButton("Open", new ClickHandler()
        {
            public void onClick(ClickEvent clickEvent) {
                controller.handleEvent(new Event(GetProcessDefinitionsAction.ID, null));
            }
        });        
        toolBar.add(menuButton);


        // -----

        title = new HTML();
        title.getElement().setAttribute("style", "font-size:24px; font-weight:BOLD");

        // ------------

        BoxLayout boxLayout = new BoxLayout(BoxLayout.Orientation.HORIZONTAL);
        timespanPanel = new LayoutPanel(boxLayout);
        timespanPanel.setPadding(0);

        timespan = new HTML();
        timespan.getElement().setAttribute("style", "padding-left:10px;padding-top:2px; color:#C8C8C8;font-size:16px;text-align:left;");
        timespanButton = new ToolButton();

        timespanButton.setStyle(ToolButton.ToolButtonStyle.MENU);
        timespanButton.getElement().setAttribute("style", "padding-right:0px;background-image:none;");
        timespanButton.setVisible(false);

        final PopupMenu timeBtnMenu = new PopupMenu();

        for(final TimespanValues ts : TimespanValues.values())
        {
            timeBtnMenu.addItem(ts.getCanonicalName(), new Command()
            {
                public void execute()
                {

                    LoadDatasetEvent theEvent = new LoadDatasetEvent();
                    String theDefinitionId = getDefinitionId(currentProcDef);
                    if (theDefinitionId == null) {
                        return ;
                    }
                    theEvent.setDefinitionId(theDefinitionId);
                    theEvent.setTimespan(ts);
                    currentTimespan = ts;
                    if (includeFailed.getValue()) {
                        theEvent.setIncludedFailed(true);
                    } else {
                        theEvent.setIncludedFailed(false);
                    }

                    LoadingOverlay.on(chartArea, true);
                    controller.handleEvent(new Event(LoadDatasetsAction.ID, theEvent));
                }
            });
        };

        timespanButton.setMenu(timeBtnMenu);

        timespanPanel.add(timespanButton, new BoxLayoutData("20px", "20px"));
        timespanPanel.add(timespan, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));

        // ------------

        final LayoutPanel contents = new LayoutPanel(new RowLayout());

        LayoutPanel headerPanel = new LayoutPanel(new ColumnLayout());
        headerPanel.setPadding(0);
        headerPanel.add(title, new ColumnLayoutData("55%"));
        headerPanel.add(timespanPanel, new ColumnLayoutData("45%"));

        // ------------

        chartArea = new LayoutPanel();
        chartArea.setPadding(15);
        chartArea.setLayout(new BorderLayout());

        instancePanel = new LayoutPanel();
        listBox = new org.gwt.mosaic.ui.client.ListBox(new String[]{"Process Instance"});
        instancePanel.add(listBox);
        contents.add(headerPanel, new RowLayoutData("100"));
        contents.add(chartArea, new RowLayoutData(true));

        // ------------

        includeFailed = new CheckBox("Incl. failed / terminated?");
        includeFailed.setValue(false);
        includeFailed.addValueChangeHandler(new ValueChangeHandler<Boolean>()
        {
            public void onValueChange(ValueChangeEvent<Boolean> isEnabled) {

                LoadDatasetEvent theEvent = new LoadDatasetEvent();
                String theDefinitionId = getDefinitionId(currentProcDef);
                if (theDefinitionId == null) {
                    return ;
                }
                theEvent.setDefinitionId(theDefinitionId);
                theEvent.setTimespan(currentTimespan);
                if (includeFailed.getValue()) {
                    theEvent.setIncludedFailed(true);
                } else {
                    theEvent.setIncludedFailed(false);
                }
                LoadingOverlay.on(chartArea, true);
                controller.handleEvent(new Event(LoadDatasetsAction.ID, theEvent));
            }
        });

        buttonPanel = new LayoutPanel(new BoxLayout(BoxLayout.Orientation.VERTICAL));
        buttonPanel.add(includeFailed);
        
        // ------------
        panel.add(contents, new BoxLayoutData(BoxLayoutData.FillStyle.BOTH));

        ErraiBus.get().subscribe("process.execution.history", new MessageCallback()
        {
            public void callback(Message message) {

                String processName = message.get(String.class, "processName");
                String processDefinitionId = message.get(String.class, "processDefinitionId");
                update(processName, processDefinitionId);

            }
        });

        callback.onSuccess(panel);
    }


    private String getDefinitionId(String currentProcessDefinition) {
        String definitionId = null;

        if (processDefinitions == null || processDefinitions.size() < 1) {
            return null;
        }

        for (ProcessDefinitionRef ref : processDefinitions) {
            if (currentProcessDefinition.equals(ref.getName())) {
                definitionId = ref.getId();
            }
        }

        return definitionId;
    }


    public void selectDefinition(List<ProcessDefinitionRef> processDefinitions)
    {

        this.processDefinitions = processDefinitions;

        final LayoutPopupPanel popup = new LayoutPopupPanel(true);
        popup.addStyleName("soa-PopupPanel");

        final com.google.gwt.user.client.ui.ListBox listBox =
                new com.google.gwt.user.client.ui.ListBox();
        listBox.addItem("");

        for(ProcessDefinitionRef ref : processDefinitions)
        {
            listBox.addItem(ref.getName());
        }

        // show dialogue
        LayoutPanel p = new LayoutPanel(new BoxLayout(BoxLayout.Orientation.VERTICAL));
        p.add(new HTML("Please select a process:"));
        p.add(listBox);

        // -----

        LayoutPanel p2 = new LayoutPanel(new BoxLayout(BoxLayout.Orientation.HORIZONTAL));
        p2.add(new Button("Done", new ClickHandler() {
            public void onClick(ClickEvent clickEvent)
            {
                if(listBox.getSelectedIndex()>0)
                {
                    popup.hide();
                    String procDef = listBox.getItemText(listBox.getSelectedIndex());
                    update(procDef, getDefinitionId(procDef));
                }
            }
        }));

        // -----

        HTML html = new HTML("Cancel");
        html.addClickHandler(new ClickHandler(){
            public void onClick(ClickEvent clickEvent)
            {
                popup.hide();
            }
        });
        p2.add(html, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));
        p.add(p2);

        // -----

        popup.setPopupPosition(menuButton.getAbsoluteLeft()-5, menuButton.getAbsoluteTop()+30);
        popup.setWidget(p);
        popup.pack();
        popup.show();


    }

    private void update(String procDef, String processDefinitionId) {
        currentProcDef = procDef;

        String name = currentProcDef; // riftsaw name juggling
        String subtitle = "";
        if(currentProcDef.indexOf("}")!=-1)
        {

            String[] qname = currentProcDef.split("}");
            name = qname[1];
            subtitle = qname[0].substring(1, qname[0].length());
        }

        title.setHTML(name + "<br/><div style='color:#C8C8C8;font-size:12px;text-align:left;'>"+subtitle+"</div>");
        TimespanValues ts = currentTimespan == null ? TimespanValues.LAST_7_DAYS : currentTimespan;


        LoadingOverlay.on(chartArea, true);

        LoadDatasetEvent theEvent = new LoadDatasetEvent();
        theEvent.setDefinitionId(processDefinitionId);
        theEvent.setTimespan(ts);
        if (includeFailed.getValue()) {
            theEvent.setIncludedFailed(true);
        } else {
            theEvent.setIncludedFailed(false);
        }

        currentTimespan = ts;

        controller.handleEvent(new Event(LoadDatasetsAction.ID, theEvent));

    }


    public void updateChart(String chartData) {
        ((DefaultListModel)listBox.getModel()).clear();

        LoadingOverlay.on(chartArea, false);

        timespanButton.setVisible(true);

        // feed chronoscope ...
        final Datasets<Tuple2D> datasets = new Datasets<Tuple2D>();
        DatasetReader datasetReader = ChronoscopeFactory.getInstance().getDatasetReader();
        JSOModel jsoModel = JSOModel.fromJson(chartData);

        if(chartData.startsWith("["))
        {
            for(int i=0; i<jsoModel.length(); i++)
            {
                datasets.add(datasetReader.createDatasetFromJson(
                        new GwtJsonDataset(jsoModel.get(i)))
                );
            }
        }
        else
        {
            datasets.add(datasetReader.createDatasetFromJson(
                    new GwtJsonDataset(jsoModel))
            );
        }


        renderChart(datasets);
        timespanPanel.layout();

    }

    private void renderChart(Datasets<Tuple2D> datasets)
    {
        try
        {
            Dataset[] dsArray = datasets.toArray();

            // if exists remove. I don't know how to update at this point
            if(chartPanel!=null)
            {
                //chartArea.remove(chartPanel);
                chartPanel.replaceDatasets(dsArray);
                overlayMapping.clear();
            }
            else
            {
                initChartPanel(dsArray);
            }

            timespan.setText("Executions " +currentTimespan.getCanonicalName());
            chartArea.layout();
        }
        catch (Exception e)
        {
            ConsoleLog.error("Failed to create chart", e);
        }
    }

    private void initChartPanel(Dataset[] datasets)
    {
        int[] dim = calcChartDimension();

        // ------
        chartPanel = Chronoscope.createTimeseriesChart(datasets, dim[0], dim[1]);

        // marker
        final XYPlot plot = chartPanel.getChart().getPlot();

        plot.addPlotFocusHandler(new PlotFocusHandler(){
            public void onFocus(final PlotFocusEvent event)
            {
                if(event.getFocusDataset()>=0) // zooming
                {
                    ChronoDate chronoDate = ChronoDate.get(event.getDomain());
                    Date date = new Date();
                    date.setTime((long) chronoDate.getTime());

                    LoadChartProcessInstanceEvent theEvent = new LoadChartProcessInstanceEvent();
                    String theDefinitionId = getDefinitionId(currentProcDef);
                    if (theDefinitionId == null) {
                        return ;
                    }
                    theEvent.setDefinitionId(theDefinitionId);
                    theEvent.setDate(date);
                    theEvent.setDatasetType(event.getFocusDataset());
                    theEvent.setTimespan(currentTimespan);
                    controller.handleEvent(new Event(LoadChartProcessInstancesAction.ID, theEvent));
                }
            }
        });

        // ------        

        final ViewReadyCallback callback = new ViewReadyCallback() {
            public void onViewReady(View view) {
                resizeChartArea(view);
            }
        };

        chartPanel.setViewReadyCallback(callback);

        final LayoutPanel wrapper = new LayoutPanel();
        wrapper.setLayout(new ColumnLayout());
        wrapper.add(chartPanel, new ColumnLayoutData("70%"));
        wrapper.add(new Label("More..."), new ColumnLayoutData("30%"));

        chartArea.add(chartPanel);
        chartArea.add(buttonPanel, new BorderLayoutData(BorderLayout.Region.EAST, "150px"));

        instancePanel.getElement().setAttribute("style", "margin-top:15px");
        chartArea.add(instancePanel, new BorderLayoutData(BorderLayout.Region.SOUTH, "150px"));

        // ------

        ResizableWidgetCollection.get().add(new ResizableWidget() {
            public Element getElement() {
                return chartPanel.getElement();
            }

            public boolean isAttached() {
                return chartPanel.isAttached();
            }

            public void onResize(int width, int height)
            {
                View view = resizeChartView();
            }
        });
    }


    public void updateProcessInstances(List<StringRef> instances) {
        DefaultListModel<String> listModel = (DefaultListModel)listBox.getModel();
        listModel.clear();
        for(StringRef instance : instances) {
            listModel.add(instance.getValue());
        }
    }

    private int[] calcChartDimension()
    {
       // int w = (int) (chartArea.getOffsetWidth() * 0.50);
       // int h = (int) (w / GOLDEN__RATIO);

        return new int[] {460, 200};
    }

    private View resizeChartView()
    {
        int[] dim = calcChartDimension();

        // Resizing the chart once displayed currently unsupported
        final View view = chartPanel.getChart().getView();
        if(view!=null)
            view.resize(dim[0], dim[1]);

        resizeChartArea(view);

        return view;
    }

    private void resizeChartArea(View view)
    {
        /*int resizeTo= Integer.valueOf(view.getHeight()) + 75;
        chartArea.setHeight(resizeTo+"px");
        chartArea.layout();*/
        chartArea.layout();
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

}
