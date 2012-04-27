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
package org.jboss.bpm.console.client;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.mvc4g.client.Controller;
import com.mvc4g.client.Event;
import org.gwt.mosaic.ui.client.MessageBox;
import org.jboss.bpm.console.client.common.Model;
import org.jboss.bpm.console.client.common.ModelCommands;
import org.jboss.bpm.console.client.common.ModelParts;
import org.jboss.bpm.console.client.icons.ConsoleIconBundle;
import org.jboss.bpm.console.client.util.ConsoleLog;
import org.jboss.errai.bus.client.api.Message;
import org.jboss.errai.bus.client.api.MessageCallback;
import org.jboss.errai.bus.client.api.SubscribeListener;
import org.jboss.errai.bus.client.framework.ClientMessageBus;
import org.jboss.errai.bus.client.ErraiBus;
import org.jboss.errai.bus.client.framework.SubscriptionEvent;
import org.jboss.errai.bus.client.security.SecurityService;
import org.jboss.errai.workspaces.client.framework.Registry;
import org.jboss.errai.workspaces.client.api.annotations.DefaultBundle;
import org.jboss.errai.workspaces.client.api.annotations.GroupOrder;

/**
 * Main entry point for the BPM console module
 */
@GroupOrder("Tasks, Processes, Reporting, Runtime, Settings")
@DefaultBundle(ConsoleIconBundle.class)
public class ErraiApplication implements EntryPoint, SubscribeListener
{
    public void onModuleLoad() {
        Log.setUncaughtExceptionHandler();

        DeferredCommand.addCommand(new Command() {
            public void execute() {

                // hide splash image
                // move the loading div to background
                DOM.getElementById("splash_loading").getStyle().setProperty("display", "none");
                DOM.getElementById("splash").getStyle().setProperty("zIndex", "-1");

                onModuleLoad2();
            }
        });

        ErraiBus.get().subscribe(Model.SUBJECT,
                new MessageCallback()
                {
                    public void callback(Message message)
                    {
                        Log.debug("Data model: " + message.getCommandType());
                    }
                });
    }

    public void onModuleLoad2()
    {
        final ClientMessageBus bus = (ClientMessageBus) ErraiBus.get();

        bus.addPostInitTask(
                new Runnable() {
                    public void run()
                    {
                        Registry.get(SecurityService.class).setDeferredNotification(true);
                    }
                }
        );

        bus.addSubscribeListener(this);
        
        Controller mainController = new com.mvc4g.client.Controller();
        Registry.set(Controller.class, mainController);

        // ------

        // setup base urls
        String proxyUrl = null;
        if (!GWT.isScript())
        {
            proxyUrl = GWT.getModuleBaseURL() + "xhp";
        }

        final ConsoleConfig config = new ConsoleConfig(proxyUrl);
        ConsoleLog.debug("Console server: " + config.getConsoleServerUrl());

        URLBuilder.configureInstance(config);

        // ------

        ApplicationContext appContext = new ApplicationContext()
        {

            public void displayMessage(String message, boolean isError)
            {
                if(isError)
                    MessageBox.error("Error", message);
                else
                    MessageBox.alert("Warn", message);
            }


            public Authentication getAuthentication()
            {
                return Registry.get(Authentication.class); // set in login view
            }


            public ConsoleConfig getConfig()
            {
                return config;
            }


            public void refreshView()
            {

            }
        };

        Registry.set(ApplicationContext.class, appContext);

        // ------

        registerGlobalViewsAndActions(mainController);

        mainController.addAction("login", new LoginAction());
        mainController.addAction(BootstrapAction.ID, new BootstrapAction());
        mainController.addView("loginView", new LoginView());

        // bootstrap and login
        mainController.handleEvent(
                new com.mvc4g.client.Event(BootstrapAction.ID, Boolean.TRUE)
        );

        mainController.handleEvent(new Event("login", null));
    }

    /**
     * Views and actions accessible from any component
     */
    private void registerGlobalViewsAndActions(Controller controller)
    {
        // register global views and actions, available across editors
        //controller.addView(Header.ID, header);
        controller.addAction(LoadingStatusAction.ID, new LoadingStatusAction());
        controller.addAction(BootstrapAction.ID, new BootstrapAction());
    }

    public void onSubscribe(SubscriptionEvent event) {
        ConsoleLog.debug("New Subscription: "+event.getSubject());
    }
}
