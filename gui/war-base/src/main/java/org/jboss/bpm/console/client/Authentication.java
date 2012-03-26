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

import com.google.gwt.http.client.*;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import org.gwt.mosaic.ui.client.MessageBox;
import org.jboss.bpm.console.client.util.ConsoleLog;
import org.jboss.bpm.console.client.util.JSONWalk;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Authentication
{
  private AuthCallback callback;

  private List<String> rolesAssigned = new ArrayList<String>();

  private String sid;
  private String username;
  private String password;

  private ConsoleConfig config;
  private String rolesUrl;
  private Date loggedInSince;

  public Authentication(ConsoleConfig config, String sessionID, String rolesUrl)
  {
    this.config = config;
    this.sid = sessionID;
    this.rolesUrl = rolesUrl;
    this.loggedInSince = new Date();
  }

  public String getSid()
  {
    return sid;
  }

  public void login(String user, String pass)
  {
    this.username = user;
    this.password = pass;

    String formAction = config.getConsoleServerUrl() + "/rs/identity/secure/j_security_check";
    RequestBuilder rb = new RequestBuilder(RequestBuilder.POST, formAction);
    rb.setHeader("Content-Type", "application/x-www-form-urlencoded");

    try
    {
      rb.sendRequest("j_username="+user+"&j_password="+pass,
          new RequestCallback()
          {

            public void onResponseReceived(Request request, Response response)
            {
              ConsoleLog.debug("postLoginCredentials() HTTP "+response.getStatusCode());

              if(response.getText().indexOf("HTTP 401")!=-1) // HACK
              {
                if (callback != null)
                  callback.onLoginFailed(request, new Exception("Authentication failed"));
                else
                  throw new RuntimeException("Unknown exception upon login attempt");
              }
              else if(response.getStatusCode()==200) // it's always 200, even when the authentication fails
              {
                DeferredCommand.addCommand(
                    new Command()
                    {

                      public void execute()
                      {
                        requestAssignedRoles();
                      }
                    }
                );
              }
            }

            public void onError(Request request, Throwable t)
            {
              if (callback != null)
                callback.onLoginFailed(request, new Exception("Authentication failed"));
              else
                throw new RuntimeException("Unknown exception upon login attempt");
            }
          }
      );
    }
    catch (RequestException e)
    {
      ConsoleLog.error("Request error", e);
    }
  }

  public Date getLoggedInSince()
  {
    return loggedInSince;
  }

  /**
   * Login using specific credentials.
   * This delegates to {@link com.google.gwt.http.client.RequestBuilder#setUser(String)}
   * and {@link com.google.gwt.http.client.RequestBuilder#setPassword(String)}
   */
  private void requestAssignedRoles()
  {
    RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, rolesUrl );

    ConsoleLog.debug("Request roles: " + rb.getUrl());

    /*if (user != null && pass != null)
    {
      rb.setUser(user);
      rb.setPassword(pass);

      if (!GWT.isScript()) // hosted mode only
      {
        rb.setHeader("xtest-user", user);
        rb.setHeader("xtest-pass", pass); // NOTE: This is plaintext, use for testing only
      }
    }*/

    try
    {
      rb.sendRequest(null,
          new RequestCallback()
          {

            public void onResponseReceived(Request request, Response response)
            {
              ConsoleLog.debug("requestAssignedRoles() HTTP "+response.getStatusCode());

              // parse roles
              if (200 == response.getStatusCode())
              {
                rolesAssigned = Authentication.parseRolesAssigned(response.getText());
                if (callback != null) callback.onLoginSuccess(request, response);
              }
              else
              {
                onError(request, new Exception(response.getText()));
              }
            }

            public void onError(Request request, Throwable t)
            {
              // auth failed
              // Couldn't connect to server (could be timeout, SOP violation, etc.)
              if (callback != null)
                callback.onLoginFailed(request, t);
              else
                throw new RuntimeException("Unknown exception upon login attempt", t);
            }
          });
    }

    catch (RequestException e1)
    {
      // Couldn't connect to server
      throw new RuntimeException("Unknown error upon login attempt", e1);
    }
  }


  public void setCallback(AuthCallback callback)
  {
    this.callback = callback;
  }

  private native void reload() /*-{
       $wnd.location.reload();
     }-*/;

  public static void logout(final ConsoleConfig conf)
  {
    RequestBuilder rb = new RequestBuilder(
        RequestBuilder.POST,
        conf.getConsoleServerUrl()+"/rs/identity/sid/invalidate"
    );

    try
    {
      rb.sendRequest(null, new RequestCallback()
      {
        public void onResponseReceived(Request request, Response response)
        {
          ConsoleLog.debug("logout() HTTP "+response.getStatusCode());

          if(response.getStatusCode()!=200)
          {
            ConsoleLog.error(response.getText());  
          }
        }

        public void onError(Request request, Throwable t)
        {
          ConsoleLog.error("Failed to invalidate session", t);
        }
      });
    }
    catch (RequestException e)
    {
      ConsoleLog.error("Request error", e);
    }
  }

  public void logoutAndReload()
  {
    RequestBuilder rb = new RequestBuilder(
        RequestBuilder.POST,
        config.getConsoleServerUrl()+"/rs/identity/sid/invalidate"
    );

    try
    {
      rb.sendRequest(null, new RequestCallback()
      {
        public void onResponseReceived(Request request, Response response)
        {
          ConsoleLog.debug("logoutAndReload() HTTP "+response.getStatusCode());
          resetState();
          reload();
        }

        public void onError(Request request, Throwable t)
        {
          ConsoleLog.error("Failed to invalidate session", t);
        }
      });
    }
    catch (RequestException e)
    {
      ConsoleLog.error("Request error", e);
    }
  }

  private void resetState()
  {
    sid = null;
    username = null;
    password = null;
    rolesAssigned = new ArrayList<String>();
    loggedInSince = null;
  }

  public void handleSessionTimeout()
  {
    MessageBox.confirm("Session expired", "Please login again",
        new MessageBox.ConfirmationCallback()
        {
          public void onResult(boolean b)
          {
            // regardless of the choice, force login
            logoutAndReload();
          }
        }
    );
  }


  public interface AuthCallback
  {
    void onLoginSuccess(Request request, Response response);

    void onLoginFailed(Request request, Throwable t);
  }


  public List<String> getRolesAssigned()
  {
    return rolesAssigned;
  }

  public String getUsername()
  {
    return username;
  }

  public String getPassword()
  {
    return password;
  }

  public static List<String> parseRolesAssigned(String json)
  {
    // parse roles
    List<String> roles = new ArrayList<String>();

    JSONValue root = JSONParser.parse(json);
    JSONArray array = JSONWalk.on(root).next("roles").asArray();

    for (int i = 0; i < array.size(); ++i)
    {
      JSONObject item = array.get(i).isObject();
      boolean assigned = JSONWalk.on(item).next("assigned").asBool();
      String roleName = JSONWalk.on(item).next("role").asString();

      if (assigned)
      {
        roles.add(roleName);
      }
    }

    return roles;
  }
}
