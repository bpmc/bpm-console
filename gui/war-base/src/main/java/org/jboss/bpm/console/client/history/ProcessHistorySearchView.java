/**
 * 
 */
package org.jboss.bpm.console.client.history;

import java.util.Date;
import java.util.List;

import org.gwt.mosaic.ui.client.DecoratedTabLayoutPanel;
import org.gwt.mosaic.ui.client.ToolBar;
import org.gwt.mosaic.ui.client.layout.BoxLayout;
import org.gwt.mosaic.ui.client.layout.BoxLayoutData;
import org.gwt.mosaic.ui.client.layout.LayoutPanel;
import org.gwt.mosaic.ui.client.layout.MosaicPanel;
import org.jboss.bpm.console.client.model.ProcessDefinitionRef;
import org.jboss.errai.workspaces.client.api.ProvisioningCallback;
import org.jboss.errai.workspaces.client.api.WidgetProvider;
import org.jboss.errai.workspaces.client.framework.Registry;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DateBox;
import com.mvc4g.client.Controller;
import com.mvc4g.client.Event;
import com.mvc4g.client.ViewInterface;

/**
 * @author Jeff Yu
 * @date: Mar 02, 2011
 */
public class ProcessHistorySearchView implements WidgetProvider, ViewInterface {
	
	public static final String ID = ProcessHistorySearchView.class.getName();
	
	private Controller controller;
	
	private ListBox processStatusList;
	
	private ListBox definitionList;
	
	private TextBox correlationKey;
	
	private DateBox startTime;
	
	private DateBox endTime;

    private ProvisioningCallback callback;
			
	public ProcessHistorySearchView() {
		this.controller = Registry.get(Controller.class);

        this.controller.addView(ID, this);
		this.controller.addAction(LoadProcessHistoryAction.ID, new LoadProcessHistoryAction());
        this.controller.addAction(LoadProcessDefinitionsAction.ID, new LoadProcessDefinitionsAction());
	}
	
	@Override
	public void provideWidget(final ProvisioningCallback callback) {

        controller.handleEvent(new Event(LoadProcessDefinitionsAction.ID, null));

		this.callback = callback;
		
	}

	public void initialize(final List<ProcessDefinitionRef> processDefinitions) {

		final LayoutPanel panel = new LayoutPanel(new BoxLayout(BoxLayout.Orientation.VERTICAL));
		panel.setPadding(0);
		panel.setWidgetSpacing(5);

		final ToolBar toolbar = new ToolBar();
		panel.add(toolbar, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));
		
		toolbar.add(new Button("Search", new ClickHandler() {

			@Override
			public void onClick(ClickEvent clickEvent) {

                if (definitionList.getItemCount() < 1) {
                    return;
                }

				String proDef = definitionList.getValue(definitionList.getSelectedIndex());

                String definitionId = null;

                for (ProcessDefinitionRef ref : processDefinitions) {
                    if (proDef.equals(ref.getName())) {
                        definitionId = ref.getId();
                    }
                }

				String theStatus = processStatusList.getValue(processStatusList.getSelectedIndex());
				Date theDate = startTime.getValue();
				if (theDate == null) {
					theDate = new Date(103,1,1);
				}
				Date edate = endTime.getValue();
				if (edate == null) {
					edate = new Date();
				}
				String ckey = correlationKey.getValue();
				
				ProcessSearchEvent event = new ProcessSearchEvent();
				event.setDefinitionKey(definitionId);
				event.setStatus(theStatus);
				event.setStartTime(theDate.getTime());
				event.setEndTime(edate.getTime());
				event.setKey(ckey);
				
				controller.handleEvent(new Event(LoadProcessHistoryAction.ID, event));
			}
			
		}) );
		
		final MosaicPanel formPanel = new MosaicPanel(new BoxLayout(BoxLayout.Orientation.VERTICAL));
		panel.add(formPanel, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));

		BoxLayoutData bld1 = new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL);
        bld1.setPreferredWidth("130px");
        
        final MosaicPanel processDefBox = new MosaicPanel(new BoxLayout());
		processDefBox.add(new Label("Process Definition: "), bld1);
		
		definitionList = new ListBox();
		for (ProcessDefinitionRef ref : processDefinitions) {
			definitionList.addItem(ref.getName());
		}
		processDefBox.add(definitionList);
				
        formPanel.add(processDefBox);       
        formPanel.add(createProcessStatusListBox(bld1));		
        formPanel.add(createCorrelationKeyTextBox(bld1));		
        formPanel.add(createStartTimeDateBox(bld1));        
        formPanel.add(createEndTimeDateBox(bld1));

		
		ProcessHistoryInstanceListView listview = new ProcessHistoryInstanceListView();
		final DecoratedTabLayoutPanel tabPanel = new DecoratedTabLayoutPanel(false);
		listview.provideWidget(new ProvisioningCallback(){

			@Override
			public void onSuccess(Widget instance) {
				tabPanel.add(instance, "History Instances");			
			}

			@Override
			public void onUnavailable() {
				
			}
			
		});
		
		panel.add(tabPanel, new BoxLayoutData(BoxLayoutData.FillStyle.BOTH));

        callback.onSuccess(panel);
	}

	
	private MosaicPanel createEndTimeDateBox(BoxLayoutData bld1) {
		MosaicPanel box4 = new MosaicPanel(new BoxLayout());
		endTime = new DateBox();
		endTime.setWidth("550px");
		box4.add(new Label("End Time: "), bld1);
		box4.add(endTime);
		return box4;
	}


	private MosaicPanel createStartTimeDateBox(BoxLayoutData bld1) {
		MosaicPanel box3 = new MosaicPanel(new BoxLayout());
		startTime = new DateBox();
		startTime.setWidth("550px");
		box3.add(new Label("Start Time: "), bld1);
		box3.add(startTime);
		return box3;
	}


	private MosaicPanel createCorrelationKeyTextBox(BoxLayoutData bld1) {
		MosaicPanel box2 = new MosaicPanel(new BoxLayout());
		correlationKey = new TextBox();
		correlationKey.setWidth("550px");
		box2.add(new Label("Correlation Key: "), bld1);
		box2.add(correlationKey);
		box2.add(new Label(" format: correlation name = [correlation value], e.g Session=[1]"));
		return box2;
	}


	private MosaicPanel createProcessStatusListBox(BoxLayoutData bld1) {
		MosaicPanel box1 = new MosaicPanel(new BoxLayout());
		processStatusList = new ListBox();
		processStatusList.addItem("COMPLETED");
		processStatusList.addItem("FAILED");
		processStatusList.addItem("TERMINATED");		
		box1.add(new Label("Process Status: "), bld1);
		box1.add(processStatusList);
		return box1;
	}


	@Override
	public void setController(Controller controller) {
		this.controller = controller;
	}

}
