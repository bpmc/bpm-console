/*
 * Copyright 2009 JBoss, a divison Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.bpm.console.client.util;

import org.gwt.mosaic.ui.client.layout.LayoutPanel;

/**
 * @author: Heiko Braun <hbraun@redhat.com>
 * @date: Oct 15, 2010
 */
public class Debug {
    public static void frame(LayoutPanel panel)
    {
        panel.getElement().setAttribute("style", "border: 1px dashed blue;");
    }
}
