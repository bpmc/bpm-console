package org.jboss.bpm.console.client.process;

import org.gwt.mosaic.ui.client.layout.ColumnLayout;
import org.gwt.mosaic.ui.client.layout.ColumnLayoutData;
import org.gwt.mosaic.ui.client.layout.MosaicPanel;
import org.jboss.bpm.console.client.util.ConsoleLog;
import org.jboss.errai.workspaces.client.api.ProvisioningCallback;
import org.jboss.errai.workspaces.client.api.WidgetProvider;
import org.jboss.errai.workspaces.client.framework.Registry;

import com.google.gwt.user.client.ui.Widget;
import com.mvc4g.client.Controller;


/**
 * @author Maciej Swiderski <swiderski.maciej@gmail.com>
 */
public class MergedProcessHistoryView  implements WidgetProvider
{
  MosaicPanel panel;

  DefinitionHistoryListView definitionView;
  HistoryInstanceListView instanceView;

  public void provideWidget(ProvisioningCallback callback)
  {
    Controller controller = Registry.get(Controller.class);
    
    panel = new MosaicPanel();
    panel.setPadding(0);    
    
    definitionView = new DefinitionHistoryListView();
    instanceView = new HistoryInstanceListView();

    final MosaicPanel splitPanel = new MosaicPanel(new ColumnLayout());
    splitPanel.setPadding(0);

    definitionView.provideWidget(new ProvisioningCallback()
    {
      public void onSuccess(Widget instance)
      {
        splitPanel.add(instance, new ColumnLayoutData("250 px"));
      }

      public void onUnavailable()
      {
        ConsoleLog.error("Failed to load DefinitionListView.class");
      }
    });

    instanceView.provideWidget(
        new ProvisioningCallback()
        {
          public void onSuccess(Widget instance)
          {
            splitPanel.add(instance);
          }

          public void onUnavailable()
          {
            ConsoleLog.error("Failed to load DefinitionListView.class");
          }
        }
    );

    panel.add(splitPanel);
    
    callback.onSuccess(panel);
  }
}
