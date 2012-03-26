package org.jboss.bpm.console.client.monitor;

import org.jboss.bpm.monitor.gui.client.TimespanValues;

/**
 * User: Jeff Yu
 * Date: 12/04/11
 */
public class LoadDatasetEvent {

    private String definitionId;

    private TimespanValues timespan;

    private boolean includedFailed;

    public String getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(String definitionId) {
        this.definitionId = definitionId;
    }

    public TimespanValues getTimespan() {
        return timespan;
    }

    public void setTimespan(TimespanValues timespan) {
        this.timespan = timespan;
    }

    public boolean isIncludedFailed() {
        return includedFailed;
    }

    public void setIncludedFailed(boolean includedFailed) {
        this.includedFailed = includedFailed;
    }
}
