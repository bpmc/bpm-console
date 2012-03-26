package org.jboss.bpm.console.client.monitor;

import org.jboss.bpm.monitor.gui.client.TimespanValues;

import java.util.Date;

/**
 * User: Jeff Yu
 * Date: 12/04/11
 */
public class LoadChartProcessInstanceEvent {

    public final static int DATASET_COMPLETED = 0;

    public final static int DATASET_FAILED = 1;

    public final static int DATASET_TERMINATED = 2;

    private String definitionId;

    private int datasetType;

    private Date date;

    private TimespanValues timespan;

    public String getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(String definitionId) {
        this.definitionId = definitionId;
    }

    public int getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(int datasetType) {
        this.datasetType = datasetType;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public TimespanValues getTimespan() {
        return timespan;
    }

    public void setTimespan(TimespanValues timespan) {
        this.timespan = timespan;
    }
}
