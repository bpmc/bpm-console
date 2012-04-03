/**
 * 
 */
package org.jboss.bpm.console.client.history;

import org.jboss.bpm.console.client.util.ConsoleLog;
import org.jboss.errai.workspaces.client.api.ProvisioningCallback;
import org.jboss.errai.workspaces.client.api.WidgetProvider;
import org.jboss.errai.workspaces.client.api.annotations.LoadTool;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;

/**
 * @author Jeff Yu
 * @date Mar 18, 2011
 */
@LoadTool(name="History Query", group = "Processes", icon = "historySearchIcon", priority= 3 )
public class ProcessHistorySearchModule implements WidgetProvider {
	
	private ProcessHistorySearchView instance;
	
	
	@Override
	public void provideWidget(final ProvisioningCallback callback) {
	    GWT.runAsync(
	            new RunAsyncCallback()
	            {
	              public void onFailure(Throwable err)
	              {
	                ConsoleLog.error("Failed to load tool", err);
	              }

	              public void onSuccess()
	              {
	                if (instance == null) {
	                  instance = new ProcessHistorySearchView();
	                }
	                instance.provideWidget(callback);
	              }
	            }
	        );

	}

}
