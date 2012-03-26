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
package org.jboss.bpm.console.client.report;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import org.gwt.mosaic.ui.client.layout.GridLayout;
import org.gwt.mosaic.ui.client.layout.GridLayoutData;
import org.gwt.mosaic.ui.client.layout.MosaicPanel;
import org.jboss.bpm.report.model.ReportParameter;
import org.jboss.bpm.report.model.ReportReference;
import org.jboss.errai.workspaces.client.framework.Preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Report parameter input.
 *
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
public class ReportParameterForm extends MosaicPanel
{
  private List<InputField> fields = new ArrayList<InputField>();

  private Preferences prefs;
  public ReportParameterForm(ReportReference reportReference, ReportParamCallback callback)
  {
    this.prefs = GWT.create(Preferences.class);
    this.add(getFormPanel(reportReference, callback));
  }

  private Widget getFormPanel(final ReportReference reportRef, final ReportParamCallback callback)
  {
    MosaicPanel p = new MosaicPanel();
    p.setPadding(5);
    p.add(createForm(reportRef, callback));    
    return p;
  }

  private MosaicPanel createForm(final ReportReference reportRef, final ReportParamCallback callback)
  {
    boolean hasParameters = reportRef.getParameterMetaData().size()>0;
    int numRows = hasParameters ?
        reportRef.getParameterMetaData().size() + 1 : 2;

    MosaicPanel form = new MosaicPanel(new GridLayout(2, numRows));

    final Button createBtn = new Button("Create Report",
        new ClickHandler()
        {
          public void onClick(ClickEvent clickEvent)
          {
            Map<String, String> values = new HashMap<String, String>();
            for (InputField field : fields)
            {
              values.put(field.id, field.getValue());
            }

            if(!values.isEmpty())
            {
              writePrefs(values, reportRef);
            }

            callback.onSumbit(values);
          }
        });    

    Map<String,String> preferenceValues = readPrefs(reportRef);

    for(final ReportParameter reportParam : reportRef.getParameterMetaData())
    {
      String promptText = reportParam.getPromptText() != null ? reportParam.getPromptText() : reportParam.getName();
      String helpText = reportParam.getHelptext() != null ? reportParam.getHelptext() : "";

      final TextBox textBox = new TextBox();
      String prefValue = preferenceValues.get(reportParam.getName());
      if(prefValue !=null)
        textBox.setText(prefValue);      

      // retain reference to values
      final InputField field = new InputField()
      {
        String getValue()
        {
          return textBox.getText();
        }

        {
          widget = textBox;
          id = reportParam.getName();

        }
      };

      fields.add(field);

      form.add(new HTML("<b>"+promptText+"</b><br/>"+helpText));           
      form.add(textBox);
    }

    // fallback
    if(!hasParameters)
    {
      form.add(new HTML("This report doesn't require any paramters.")
      , new GridLayoutData(2,1, HasHorizontalAlignment.ALIGN_CENTER, HasVerticalAlignment.ALIGN_TOP));      
    }

    // submit
    form.add(new HTML(""));
    form.add(createBtn, new GridLayoutData(HasHorizontalAlignment.ALIGN_RIGHT, HasVerticalAlignment.ALIGN_BOTTOM));

    return form;
  }

  private void writePrefs(Map<String, String> values, ReportReference reportRef)
  {
    String name = reportRef.getTitle().replaceAll(" ","_");
    String prefKey = "bpm-form-"+name;
    StringBuffer sb = new StringBuffer();
    int i = 1;
    for(String key : values.keySet())
    {
      sb.append(key).append("=").append(values.get(key));
      if(i<values.keySet().size())
        sb.append(",");
      i++;

    }

    prefs.set(prefKey, sb.toString());
  }

  private Map<String, String> readPrefs(ReportReference reportRef)
  {
    Map<String,String> values = new HashMap<String,String>();
    String name = reportRef.getTitle().replaceAll(" ","_");
    String prefKey = "bpm-form-"+name;
    
    if(prefs.has(prefKey))
    {
      String prefValue = prefs.get(prefKey);
      String[] tokens = prefValue.split(",");
      for(int i=0; i<tokens.length; i++)
      {
        String[] tuple = tokens[i].split("=");
        values.put(tuple[0], tuple[1]);
      }
    }

    return values;
  }

  private class InputField
  {
    Widget widget;
    String id;

    String getValue()
    {
      throw new IllegalArgumentException("Override this method");
    }
  }
}
