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
package org.jboss.bpm.console.client.util;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.gwt.mosaic.ui.client.Caption;
import org.gwt.mosaic.ui.client.ImageButton;
import org.gwt.mosaic.ui.client.WindowPanel;


/**
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
public class WindowUtil
{
  /**
   *
   * @param windowPanel
   */
  public static void addMaximizeButton(final WindowPanel windowPanel,
                                       Caption.CaptionRegion captionRegion) {
    final ImageButton maximizeBtn = new ImageButton(
        Caption.IMAGES.windowMaximize());
    maximizeBtn.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        if (windowPanel.getWindowState() == WindowPanel.WindowState.MAXIMIZED) {
          windowPanel.setWindowState(WindowPanel.WindowState.NORMAL);
        } else {
          windowPanel.setWindowState(WindowPanel.WindowState.MAXIMIZED);
        }
      }
    });
    windowPanel.addWindowStateListener(new WindowPanel.WindowStateListener() {

      public void onWindowStateChange(WindowPanel sender, WindowPanel.WindowState windowState, WindowPanel.WindowState windowState1)
      {
        if (sender.getWindowState() == WindowPanel.WindowState.MAXIMIZED) {
          maximizeBtn.setImage(Caption.IMAGES.windowRestore().createImage());
        } else {
          maximizeBtn.setImage(Caption.IMAGES.windowMaximize().createImage());
        }

      }
    });
    windowPanel.getHeader().add(maximizeBtn, captionRegion);
  }

  /**
   *
   * @param windowPanel
   */
  public static  void addMinimizeButton(final WindowPanel windowPanel,
                                        Caption.CaptionRegion captionRegion) {
    final ImageButton minimizeBtn = new ImageButton(
        Caption.IMAGES.windowMinimize());
    minimizeBtn.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        windowPanel.setWindowState(WindowPanel.WindowState.NORMAL);
      }
    });
    windowPanel.getHeader().add(minimizeBtn, captionRegion);
  }

}
