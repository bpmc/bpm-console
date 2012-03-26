/**
 * 
 */
package org.jboss.bpm.console.client.history;

import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.mvc4g.client.Controller;
import com.mvc4g.client.Event;
import com.mvc4g.client.ViewInterface;
import org.gwt.mosaic.ui.client.DecoratedTabLayoutPanel;
import org.gwt.mosaic.ui.client.ListBox;
import org.gwt.mosaic.ui.client.ListBox.CellRenderer;
import org.gwt.mosaic.ui.client.ScrollLayoutPanel;
import org.gwt.mosaic.ui.client.layout.BoxLayout;
import org.gwt.mosaic.ui.client.layout.BoxLayoutData;
import org.gwt.mosaic.ui.client.layout.MosaicPanel;
import org.gwt.mosaic.ui.client.list.DefaultListModel;
import org.jboss.bpm.console.client.common.DataDriven;
import org.jboss.bpm.console.client.common.LoadingOverlay;
import org.jboss.bpm.console.client.common.WidgetWindowPanel;
import org.jboss.bpm.console.client.model.HistoryProcessInstanceRef;
import org.jboss.bpm.console.client.model.StringRef;
import org.jboss.bpm.console.client.util.SimpleDateFormat;
import org.jboss.errai.workspaces.client.api.ProvisioningCallback;
import org.jboss.errai.workspaces.client.api.WidgetProvider;
import org.jboss.errai.workspaces.client.framework.Registry;

import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Jeff Yu
 * @date Mar 18, 2011
 */
public class ProcessHistoryInstanceListView implements ViewInterface, WidgetProvider, DataDriven {
	
	public static final String ID = ProcessHistoryInstanceListView.class.getName();
	
	private Controller controller;
	
	private MosaicPanel panel;
	
	private MosaicPanel instanceList;
	
	private ListBox<HistoryProcessInstanceRef> listbox;
	
	private SimpleDateFormat dateFormat = new SimpleDateFormat();
	
	private WidgetWindowPanel processEventsWindow;
		
	private ListBox<String> processEvents;

    private String selectedProcessInstanceId;
		
	@Override
	public void provideWidget(ProvisioningCallback callback) {
		
		controller = Registry.get(Controller.class);
		controller.addView(ID, this);
        controller.addAction(LoadProcessInstanceEventsAction.ID, new LoadProcessInstanceEventsAction());
		
		panel = new MosaicPanel();
		panel.setPadding(0);
		panel.setWidgetSpacing(5);
		
		instanceList = new MosaicPanel(new BoxLayout(BoxLayout.Orientation.VERTICAL));
		instanceList.setPadding(0);
		instanceList.setWidgetSpacing(5);
		
		listbox = new org.gwt.mosaic.ui.client.ListBox<HistoryProcessInstanceRef>(new String[]{"Instance Id", "Correlation Key", "Status", "Start Time", "Finish Time"});
		listbox.setCellRenderer(new org.gwt.mosaic.ui.client.ListBox.CellRenderer<HistoryProcessInstanceRef>(){

			@Override
			public void renderCell(org.gwt.mosaic.ui.client.ListBox<HistoryProcessInstanceRef> listBox, int row, int column, HistoryProcessInstanceRef item) {
				switch(column) {
				case 0:
					listBox.setText(row, column, item.getProcessInstanceId());
					break;
				case 1:
					listBox.setText(row, column, item.getKey());
					break;
				case 2:
					listBox.setText(row, column, item.getState());
					break;
				case 3:
					listBox.setText(row, column, dateFormat.format(item.getStartTime()));
					break;
				case 4:
					listBox.setText(row, column, dateFormat.format(item.getEndTime()));
					break;
				default:
					throw new RuntimeException("Should not happen!");
				}				
			}
			
		});
		
		listbox.addDoubleClickHandler(new DoubleClickHandler(){

			@Override
			public void onDoubleClick(DoubleClickEvent event) {
				int index = listbox.getSelectedIndex();
				if (index != -1) {
					HistoryProcessInstanceRef historyInstance = listbox.getItem(index);
                    selectedProcessInstanceId = historyInstance.getProcessInstanceId();
                    createHistoryInstanceDetailWindow();
                    //controller.handleEvent(new Event(LoadProcessInstanceEventsAction.ID, selectedProcessInstanceId));
				}
			}
			
		});
				
        instanceList.add(listbox, new BoxLayoutData(BoxLayoutData.FillStyle.BOTH));
        
        panel.add(instanceList);
		callback.onSuccess(panel);

	}	

	
	@Override
	public void setController(Controller controller) {
		this.controller = controller;
	}




	@Override
	public void reset() {
		
	}


	@Override
	public void update(Object... data) {
		List<HistoryProcessInstanceRef> result = (List<HistoryProcessInstanceRef>)data[0];
		DefaultListModel<HistoryProcessInstanceRef> model = (DefaultListModel<HistoryProcessInstanceRef>)listbox.getModel();
		model.clear();
		
		for (HistoryProcessInstanceRef ref : result) {
			model.add(ref);
		}
		panel.invalidate();		
		
	}

	@Override
	public void setLoading(boolean isLoading) {
		LoadingOverlay.on(instanceList, isLoading);
	}
	
	
	public void createHistoryInstanceDetailWindow() {
		
		org.gwt.mosaic.ui.client.layout.LayoutPanel layout = new ScrollLayoutPanel();
        layout.setStyleName("bpm-window-layout");
        layout.setPadding(5);

        Label header = new Label("Instance: "+ selectedProcessInstanceId);
        header.setStyleName("bpm-label-header");
        layout.add(header, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));
        
        final DecoratedTabLayoutPanel tabPanel = new DecoratedTabLayoutPanel(false);
        tabPanel.setPadding(5);
        
        processEvents = new ListBox<String>(new String[]{"Process Events"});
        processEvents.setCellRenderer(new CellRenderer<String>(){

			@Override
			public void renderCell(ListBox<String> listBox, int row, int column, String item) {
				switch(column) {
				case 0:
					listBox.setWidget(row, column, new HTML(item));
					break;
				default:
					throw new RuntimeException("Should not happen!");
				}				
			}
        });

        MosaicPanel sourcePanel = new MosaicPanel();
        sourcePanel.add(processEvents, new BoxLayoutData(BoxLayoutData.FillStyle.VERTICAL));        
        tabPanel.add(sourcePanel, "Activity Events");
        
        tabPanel.selectTab(0);
        
        layout.add(tabPanel, new BoxLayoutData(BoxLayoutData.FillStyle.BOTH));
        
        processEventsWindow = new WidgetWindowPanel("History Instance Activity", layout, true);

        controller.handleEvent(new Event(LoadProcessInstanceEventsAction.ID, selectedProcessInstanceId));

	}



    public void populateInstanceEvents(List<StringRef> refs) {
        final DefaultListModel<String> model = (DefaultListModel<String>)processEvents.getModel();
        model.clear();
        for (StringRef value : refs) {
            model.add(formatResult(value.getValue()));
        }
    }

	
    private String formatResult(String value) {
    	StringBuffer sbuffer = new StringBuffer();
    	StringTokenizer st = new StringTokenizer(value, "~");
    	sbuffer.append(st.nextToken() + " : ");
    	
    	while (st.hasMoreTokens()) {
    		sbuffer.append("<br/>");
    		sbuffer.append(st.nextToken());
    	}
    	
    	return sbuffer.toString();
    }

}
