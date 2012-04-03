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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.mvc4g.client.Controller;
import com.mvc4g.client.ViewInterface;
import org.gwt.mosaic.ui.client.Caption;
import org.gwt.mosaic.ui.client.MessageBox;
import org.gwt.mosaic.ui.client.WindowPanel;
import org.gwt.mosaic.ui.client.layout.BoxLayout;
import org.gwt.mosaic.ui.client.layout.BoxLayoutData;
import org.gwt.mosaic.ui.client.layout.MosaicPanel;
import org.jboss.bpm.console.client.util.ConsoleLog;
import org.jboss.errai.bus.client.ErraiBus;
import org.jboss.errai.bus.client.api.base.MessageBuilder;
import org.jboss.errai.bus.client.protocols.SecurityParts;
import org.jboss.errai.bus.client.security.SecurityService;
import org.jboss.errai.workspaces.client.framework.Registry;

import java.util.List;

/**
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
public class LoginView implements ViewInterface
{
    public final static String NAME = "loginView";

    private ConsoleConfig config;
    private Authentication auth;

    private WindowPanel window = null;
    private TextBox usernameInput;
    private PasswordTextBox passwordInput;

    public final static String[] KNOWN_ROLES = {"admin", "manager", "user"};

    private HTML messagePanel = new HTML("Authentication required");

    public LoginView()
    {
        this.config = Registry.get(ApplicationContext.class).getConfig();
    }

    public void setController(Controller controller)
    {

    }

    public void display()
    {
        // force session invalidation, required to catch browser reload
        Authentication.logout(config);

        // start new session
        requestSessionID();
    }

    private void requestSessionID()
    {
        RequestBuilder rb = new RequestBuilder(
                RequestBuilder.GET,
                config.getConsoleServerUrl()+"/rs/identity/sid"
        );

        try
        {
            rb.sendRequest(null, new RequestCallback()
            {

                public void onResponseReceived(Request request, Response response)
                {
                    ConsoleLog.debug("SID: "+ response.getText());
                    ConsoleLog.debug("Cookies: "+ Cookies.getCookieNames());
                    final String sid = response.getText();

                    auth = new Authentication(
                            config,
                            sid,
                            URLBuilder.getInstance().getUserInRoleURL(KNOWN_ROLES)
                    );

                    auth.setCallback(
                            new Authentication.AuthCallback()
                            {

                                public void onLoginSuccess(Request request, Response response)
                                {
                                    // clear the form
                                    usernameInput.setText("");
                                    passwordInput.setText("");

                                    // display main console
                                    window.hide();

                                    // assemble main layout
                                    DeferredCommand.addCommand(
                                            new Command()
                                            {
                                                public void execute()
                                                {
                                                    // move the loading div to foreground
                                                    DOM.getElementById("splash").getStyle().setProperty("zIndex", "1000");
                                                    DOM.getElementById("ui_loading").getStyle().setProperty("visibility", "visible");

                                                    // launch workspace
                                                    GWT.runAsync(
                                                            new RunAsyncCallback()
                                                            {
                                                                public void onFailure(Throwable throwable)
                                                                {
                                                                    GWT.log("Code splitting failed", throwable);
                                                                    MessageBox.error("Code splitting failed", throwable.getMessage());
                                                                }

                                                                public void onSuccess()
                                                                {

                                                                    List<String> roles = auth.getRolesAssigned();
                                                                    StringBuilder roleString = new StringBuilder();
                                                                    int index = 1;
                                                                    for(String s : roles)
                                                                    {
                                                                        roleString.append(s);
                                                                        if(index<roles.size())
                                                                            roleString.append(",");
                                                                        index++;

                                                                    }

                                                                    // populate authentication context
                                                                    // this will trigger the AuthenticaionModule
                                                                    // and finally initialize the workspace UI

                                                                    Registry.get(SecurityService.class).setDeferredNotification(false);

                                                                    MessageBuilder.createMessage()
                                                                            .toSubject("AuthorizationListener")
                                                                            .signalling()
                                                                            .with(SecurityParts.Name, auth.getUsername())
                                                                            .with(SecurityParts.Roles, roleString.toString())
                                                                            .noErrorHandling()
                                                                            .sendNowWith(ErraiBus.get()
                                                                            );

                                                                    Timer t = new Timer() {

                                                                        public void run()
                                                                        {
                                                                            // hide the loading div
                                                                            DeferredCommand.addCommand(
                                                                                    new Command()
                                                                                    {
                                                                                        public void execute()
                                                                                        {
                                                                                            DOM.getElementById("ui_loading").getStyle().setProperty("visibility", "hidden");
                                                                                            DOM.getElementById("splash").getStyle().setProperty("visibility", "hidden");
                                                                                        }
                                                                                    });

                                                                        }
                                                                    };
                                                                    t.schedule(2000);

                                                                }
                                                            }
                                                    );

                                                }
                                            });

                                    window = null;
                                }

                                public void onLoginFailed(Request request, Throwable t)
                                {
                                    usernameInput.setText("");
                                    passwordInput.setText("");
                                    messagePanel.setHTML("<div style='color:#CC0000;'>Authentication failed. Please try again:</div>");
                                }
                            }
                    );

                    Registry.set(Authentication.class, auth);

                    createLayoutWindowPanel();
                    window.pack();
                    window.center();

                    // focus
                    usernameInput.setFocus(true);

                }

                public void onError(Request request, Throwable t)
                {
                    ConsoleLog.error("Failed to initiate session", t);
                }
            });
        }
        catch (RequestException e)
        {
            ConsoleLog.error("Request error", e);
        }
    }

    /**
     * The 'layout' window panel.
     */
    private void createLayoutWindowPanel() {

        window = new WindowPanel(config.getProfileName(), false);
        Widget closeBtn = window.getHeader().getWidget(0, Caption.CaptionRegion.RIGHT);
        closeBtn.setVisible(false);
        window.setAnimationEnabled(false);


        MosaicPanel panel = new MosaicPanel();
        panel.addStyleName("bpm-login");

        createLayoutContent(panel);
        window.setWidget(panel);
    }

    /**
     * Create content for layout.
     */
    private void createLayoutContent(MosaicPanel layoutPanel) {

        layoutPanel.setLayout(new BoxLayout(BoxLayout.Orientation.VERTICAL));
        layoutPanel.setPadding(10);


        Widget form = createForm();

        final Button submit = new Button("Login");
        submit.addClickHandler(new ClickHandler()
        {

            public void onClick(ClickEvent clickEvent)
            {
                engageLogin();
            }
        });


        HTML html = new HTML("Version: " + Version.VERSION);
        html.setStyleName("bpm-login-info");

        MosaicPanel btnPanel = new MosaicPanel(new BoxLayout());
        btnPanel.add(html, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));
        btnPanel.add(submit);

        layoutPanel.add(messagePanel, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));
        layoutPanel.add(form, new BoxLayoutData(BoxLayoutData.FillStyle.BOTH));
        layoutPanel.add(btnPanel, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));

    }

    private Widget createForm()
    {
        MosaicPanel panel = new MosaicPanel(new BoxLayout(BoxLayout.Orientation.VERTICAL));
        panel.setPadding(0);

        MosaicPanel box1 = new MosaicPanel(new BoxLayout());
        box1.setPadding(0);        
        MosaicPanel box2 = new MosaicPanel(new BoxLayout());
        box2.setPadding(0);

        usernameInput = new TextBox();
        passwordInput = new PasswordTextBox();


        BoxLayoutData bld1 = new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL);
        bld1.setPreferredWidth("70px");

        box1.add( new Label("Username:"), bld1);
        box1.add(usernameInput);

        box2.add(new Label("Password:"), bld1);
        box2.add(passwordInput);

        passwordInput.addKeyboardListener(
                new KeyboardListener()
                {

                    public void onKeyDown(Widget widget, char c, int i)
                    {
                    }

                    public void onKeyPress(Widget widget, char c, int i)
                    {
                    }

                    public void onKeyUp(Widget widget, char c, int i)
                    {
                        if(c == KeyboardListener.KEY_ENTER)
                        {
                            engageLogin();
                        }
                    }
                }
        );

        panel.add(box1);
        panel.add(box2);
        return panel;
    }


    //  -----------------------------------


    private void engageLogin()
    {
        requestProtectedResource();
    }

    /**
     * The j_security_check URL is a kind of temporary resource that only exists
     * if tomcat decided that the login page should be shown.
     */
    private void requestProtectedResource()
    {
        RequestBuilder rb = new RequestBuilder(
                RequestBuilder.GET,
                config.getConsoleServerUrl()+"/rs/identity/secure/sid"
        );

        try
        {
            rb.sendRequest(null, new RequestCallback()
            {

                public void onResponseReceived(Request request, Response response)
                {
                    ConsoleLog.debug("requestProtectedResource() HTTP "+response.getStatusCode());
                    auth.login( getUsername(), getPassword() );
                }

                public void onError(Request request, Throwable t)
                {
                    ConsoleLog.error("Failed to request protected resource", t);
                }
            });
        }
        catch (RequestException e)
        {
            ConsoleLog.error("Request error", e);
        }
    }

    private String getUsername()
    {
        return usernameInput.getText();
    }

    private String getPassword()
    {
        return passwordInput.getText();
    }

}
