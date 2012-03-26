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
package org.jboss.bpm.console.client.task;

import com.mvc4g.client.Controller;
import com.mvc4g.client.ViewInterface;
import org.gwt.mosaic.ui.client.ListBox;
import org.gwt.mosaic.ui.client.layout.MosaicPanel;
import org.jboss.bpm.console.client.Authentication;
import org.jboss.bpm.console.client.model.TaskRef;
import org.jboss.errai.workspaces.client.framework.Registry;

import java.util.List;

/**
 * Base class for task lists.
 * 
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
public abstract class AbstractTaskList implements ViewInterface
{  
  protected Controller controller;
  protected MosaicPanel taskList = null;
  protected ListBox<TaskRef> listBox;
  protected boolean isInitialized;
  protected String identity;
  protected List<TaskRef> cachedTasks;

  public AbstractTaskList()
  {
    super();
  }

  public boolean isInitialized()
  {
    return isInitialized;
  }

  public void setController(Controller controller)
  {
    this.controller = controller;
  }

  public TaskRef getSelection()
  {
    TaskRef selection = null;
    if(isInitialized() && listBox.getSelectedIndex()!=-1)
    {
      selection = listBox.getItem( listBox.getSelectedIndex());      
    }
    return selection;
  }

  public String getAssignedIdentity()
  {
    return Registry.get(Authentication.class).getUsername();
  }
}
