package org.jboss.bpm.console.client.process;

import org.jboss.bpm.console.client.util.ConsoleLog;
import org.jboss.errai.workspaces.client.api.ProvisioningCallback;
import org.jboss.errai.workspaces.client.api.WidgetProvider;
import org.jboss.errai.workspaces.client.api.annotations.LoadTool;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;


/**
 * @author Maciej Swiderski <swiderski.maciej@gmail.com>
 */
@LoadTool(name = "Execution History", group = "Processes", icon = "databaseIcon", priority = 2)
public class ProcessHistoryModule implements WidgetProvider
{
  static MergedProcessHistoryView instance = null;

  public void provideWidget(final ProvisioningCallback callback)
  {
    ProcessHistoryModule.createInstance(callback);
  }

  public static void createInstance(final ProvisioningCallback callback)
  {
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
              instance = new MergedProcessHistoryView();
            }
            instance.provideWidget(callback);
          }
        }

    );

  }
}
