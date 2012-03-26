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
package org.jboss.bpm.console.rebind;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.*;

/**
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
public class WorkspaceGenerator extends Generator
{
  /**
   * Simple name of class to be generated
   */
  private String className = null;
  /**
   * Package name of class to be generated
   */
  private String packageName = null;
  /**
   * Fully qualified class name passed into GWT.create()
   */
  private String typeName = null;

  public static final String WORKSPACE_CONFIG_DEFAULT = "org/jboss/bpm/console/workspace-default.cfg";
  public static final String WORKSPACE_CONFIG = "org/jboss/bpm/console/workspace.cfg";

  public String generate(TreeLogger logger, GeneratorContext context, String typeName)
      throws UnableToCompleteException
  {
    this.typeName = typeName;
    TypeOracle typeOracle = context.getTypeOracle();

    try
    {
      // get classType and save instance variables
      JClassType classType = typeOracle.getType(typeName);
      packageName = classType.getPackage().getName();
      className = classType.getSimpleSourceName() + "Wrapper";

      // Generate class source code
      generateClass(logger, context);

    }
    catch (Exception e)
    {
      // record to logger that Map generation threw an exception
      e.printStackTrace(System.out);
      logger.log(TreeLogger.ERROR, "Failed to generate workspace launcher", e);
    }

    // return the fully qualifed name of the class generated
    return packageName + "." + className;
  }

  /**
   * Generate source code for new class. Class extends
   * <code>HashMap</code>.
   *
   * @param logger  Logger object
   * @param context Generator context
   */
  private void generateClass(TreeLogger logger, GeneratorContext
      context)
  {

    // get print writer that receives the source code
    PrintWriter printWriter = null;
    printWriter = context.tryCreate(logger, packageName, className);

    // print writer if null, source code has ALREADY been generated, return
    if (printWriter == null) return;

    // init composer, set class properties, create source writer
    ClassSourceFileComposerFactory composerFactory =
        new ClassSourceFileComposerFactory(packageName, className);

    // Imports
    //composerFactory.addImport("org.jboss.bpm.console.client.ApplicationContext");
    composerFactory.addImport("org.jboss.bpm.console.client.Workspace");
    composerFactory.addImport("org.jboss.bpm.console.client.WorkspaceLauncher");

    // Interfaces
    composerFactory.addImplementedInterface("org.jboss.bpm.console.client.WorkspaceLauncher");

    // SourceWriter
    SourceWriter sourceWriter = composerFactory.createSourceWriter(context, printWriter);

    // Methods
    generateMethods(sourceWriter);

    // close generated class
    sourceWriter.outdent();
    sourceWriter.println("}");

    // commit generated class
    context.commit(logger, printWriter);
  }

  private void generateMethods(SourceWriter sourceWriter)
  {
    // start constructor source generation
    sourceWriter.println("public void launch(Workspace workspace) { ");
    sourceWriter.indent();

    InputStream in = getClass().getClassLoader().getResourceAsStream(WORKSPACE_CONFIG);

    if (null == in)
    {
      in = getClass().getClassLoader().getResourceAsStream(WORKSPACE_CONFIG_DEFAULT);
      if(null==in)
        throw new RuntimeException("Cannot find '"+WORKSPACE_CONFIG+"' or '"+WORKSPACE_CONFIG_DEFAULT+"'");
    }

    try
    {
      //use buffering, reading one line at a time
      //FileReader always assumes default encoding is OK!
      BufferedReader input = new BufferedReader(new InputStreamReader(in));
      try
      {
        String line = null;
        while ((line = input.readLine()) != null)
        {

          // ignore comments and empty lines
          if (line.equals("") || line.startsWith("#"))
            continue;

          sourceWriter.println("workspace.addEditor( new " + line + "(), false );");
        }
      }
      finally
      {
        input.close();
      }
    }
    catch (IOException ex)
    {
      throw new RuntimeException("Error reading '"+WORKSPACE_CONFIG+"'");
    }

    // end constructor source generation
    sourceWriter.outdent();
    sourceWriter.println("}");
  }

  /*private void generateConstructor(SourceWriter sourceWriter)
  {
    // start constructor source generation
    sourceWriter.println("public " + className + "() { ");
    sourceWriter.indent();
    sourceWriter.println("super();");
    // end constructor source generation 
    sourceWriter.outdent();
    sourceWriter.println("}");
  }*/

}
