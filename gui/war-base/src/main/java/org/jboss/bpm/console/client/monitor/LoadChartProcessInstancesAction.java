package org.jboss.bpm.console.client.monitor;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.Response;
import com.mvc4g.client.Controller;
import org.jboss.bpm.console.client.URLBuilder;
import org.jboss.bpm.console.client.common.AbstractRESTAction;
import org.jboss.bpm.console.client.model.JSOModel;
import org.jboss.bpm.console.client.model.JSOParser;
import org.jboss.bpm.console.client.model.StringRef;
import org.jboss.bpm.console.client.util.ConsoleLog;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: Jeff Yu
 * Date: 12/04/11
 */
public class LoadChartProcessInstancesAction extends AbstractRESTAction{

    public static final String ID = LoadChartProcessInstancesAction.class.getName();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getUrl(Object event) {

        LoadChartProcessInstanceEvent theEvent = (LoadChartProcessInstanceEvent) event;

        if (LoadChartProcessInstanceEvent.DATASET_COMPLETED == theEvent.getDatasetType()) {
            return URLBuilder.getInstance().getProcessHistoryCompletedInstancesURL(theEvent.getDefinitionId(),
                    String.valueOf(theEvent.getDate().getTime()), theEvent.getTimespan().getCanonicalName());
        } else if (LoadChartProcessInstanceEvent.DATASET_TERMINATED == theEvent.getDatasetType()) {
            return URLBuilder.getInstance().getProcessHistoryTerminatedInstanceURL(theEvent.getDefinitionId(),
                    String.valueOf(theEvent.getDate().getTime()), theEvent.getTimespan().getCanonicalName());
        } else if (LoadChartProcessInstanceEvent.DATASET_FAILED == theEvent.getDatasetType()) {
            return URLBuilder.getInstance().getProcessHistoryFailedInstanceURL(theEvent.getDefinitionId(),
                    String.valueOf(theEvent.getDate().getTime()), theEvent.getTimespan().getCanonicalName());
        }

        throw new RuntimeException("couldn't find an appropriate URL for the type of " + theEvent.getDatasetType());
    }

    @Override
    public RequestBuilder.Method getRequestMethod() {
        return RequestBuilder.GET;
    }

    @Override
    public void handleSuccessfulResponse(Controller controller, Object event, Response response) {
        ExecutionHistoryView view = (ExecutionHistoryView)controller.getView(ExecutionHistoryView.ID);

        List<StringRef> data =JSOParser.parseStringRef(response.getText());

        view.updateProcessInstances(data);

        ConsoleLog.debug("loaded chart data process instances : " + response.getText());
    }
}
