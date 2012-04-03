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
package org.jboss.bpm.console.client.common;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowCloseListener;
import com.google.gwt.user.client.ui.Frame;
import org.gwt.mosaic.core.client.Dimension;
import org.gwt.mosaic.ui.client.Caption;
import org.gwt.mosaic.ui.client.ScrollLayoutPanel;
import org.gwt.mosaic.ui.client.WindowPanel;
import org.gwt.mosaic.ui.client.layout.BoxLayout;
import org.gwt.mosaic.ui.client.layout.BoxLayoutData;
import org.jboss.bpm.console.client.util.ConsoleLog;
import org.jboss.bpm.console.client.util.WindowUtil;

import java.util.Date;

/**
 * A window panel that embeds an iframe.<br>
 * It resizes autmatically, if the iframe.window.name property
 * is set to the contents size.<p>
 * I.e.
 * <code>
 *  window.name="320,240";
 * </code>
 * <p/>
 * In case the property is not set, is resizes according to the current
 * window dimension.
 *
 * @see org.jboss.bpm.console.client.common.IFrameWindowCallback
 * 
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
public class IFrameWindowPanel
{
  private WindowPanel windowPanel = null;
  private Frame frame = null;

  private String url;
  private String title;

  private IFrameWindowCallback callback = null;

  public IFrameWindowPanel(String url, String title)
  {
    this.url = url;
    this.title = title;
  }

  private void createWindow()
  {
    windowPanel = new WindowPanel();
    windowPanel.setAnimationEnabled(true);    

    ScrollLayoutPanel layout = new ScrollLayoutPanel(new BoxLayout(BoxLayout.Orientation.VERTICAL));
    layout.setStyleName("bpm-window-layout");
    layout.setPadding(5);
    // info
    HeaderLabel header = new HeaderLabel(title, true);

    layout.add(header, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));
  
    windowPanel.addWindowCloseListener(new WindowCloseListener() {
      public void onWindowClosed() {
        if(getCallback()!=null)
          getCallback().onWindowClosed();

        windowPanel = null;
        frame = null;
      }

      public String onWindowClosing() {
        return null;
      }
    });

    // iframe
    frame = new Frame()
    {
      /*public void onBrowserEvent(com.google.gwt.user.client.Event event)
      {
        ConsoleLog.debug("Browser Event: "+ DOM.eventGetTypeString(event));

        final Element iframe = getFrame().getElement();
        String size = getContents(IFrameElement.as(iframe));
        if(size!=null && size.indexOf(",")!=-1)
        {
          ConsoleLog.debug("Frame content size: "+ size);
          String[] wh = size.split(",");
          getWindowPanel().setContentSize(
              new Dimension(
                  Integer.valueOf(wh[0]),
                  Integer.valueOf(wh[1])+100
              )
          );

        }
        else
        {
          ConsoleLog.debug("Unable to retrieve frame content size: "+size);

          final int width = Window.getClientWidth()-200;
          final int height = Window.getClientHeight()-100;

          getWindowPanel().setContentSize(
              new Dimension(width,height)
          );

        }

        windowPanel.layout();
        windowPanel.center();
        windowPanel.setVisible(true);
      }  */
    };

    //frame.sinkEvents(com.google.gwt.user.client.Event.ONLOAD);

    DOM.setStyleAttribute(frame.getElement(), "border", "none");

    // https://jira.jboss.org/jira/browse/JBPM-2244
    frame.getElement().setId(
        String.valueOf( new Date().getTime())
    );

    frame.setUrl(this.url);

    layout.add(frame, new BoxLayoutData(BoxLayoutData.FillStyle.BOTH));
    windowPanel.setWidget(layout);

    WindowUtil.addMaximizeButton(windowPanel, Caption.CaptionRegion.RIGHT);
    WindowUtil.addMinimizeButton(windowPanel, Caption.CaptionRegion.RIGHT);

    // show window
    final int width = Window.getClientWidth()-200;
    final int height = Window.getClientHeight()-100;
    windowPanel.setContentSize(new Dimension(width, height));
    windowPanel.center();
  }

  private void destroyWindow()
  {
    this.windowPanel.hide();
  }

  public Frame getFrame()
  {
    return frame;
  }

  public WindowPanel getWindowPanel()
  {
    return windowPanel;
  }

  public void setCallback(IFrameWindowCallback callback)
  {
    this.callback = callback;
  }

  private IFrameWindowCallback getCallback()
  {
    return callback;
  }

  public native String getContents(Element iframe) /*-{
       try {
         // Make sure the iframe's window & document are loaded.
         if (!iframe.contentWindow || !iframe.contentWindow.document)
           return "no set";

         // Get the contents from the window.name property.
         return iframe.contentWindow.name;
       } catch (e) {
         return "Error: "+e;
       }
     }-*/;


  public void show()
  {
    createWindow();    
  }
}
