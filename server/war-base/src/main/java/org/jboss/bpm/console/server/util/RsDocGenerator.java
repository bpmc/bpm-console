package org.jboss.bpm.console.server.util;

import org.jboss.bpm.console.server.InfoFacade;

import java.io.*;

/**
 * User: Jeff Yu
 * Date: 31/03/11
 */
public class RsDocGenerator {

    private File output;

    public RsDocGenerator(String outputDir) throws Exception {
       try {
            this.output = new File(outputDir);
            if(!this.output.exists())
                this.output.mkdirs();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


    public void generate(String contextPath, String project, String type) {

        RsDocBuilder builder = new RsDocBuilder(contextPath, InfoFacade.getRSResources());

        String filename = this.output.getAbsolutePath() + "/" + project + "_restful_service." + type;

        String result = null;

        if ("html".equalsIgnoreCase(type)) {
            result = builder.build2HTML(project).toString();
        } else if ("xml".equalsIgnoreCase(type)) {
            result = builder.build2Docbook(project).toString();
        }

        Writer out = null;
        try{
           out = new OutputStreamWriter(new FileOutputStream(filename), "UTF-8");
           out.write(result);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
           if (out != null) {
               try {
                   out.close();
               } catch (IOException ie) {
                   throw new RuntimeException("Error in closing IO.", ie);
               }
           }

        }

    }


    public static void main(String[] args) throws Exception{

      String dir = args[0];

      RsDocGenerator generator = new RsDocGenerator(dir);
      generator.generate("/bpel-console-server/rs", "riftsaw", "html");
      generator.generate("/bpel-console-server/rs", "riftsaw", "xml");

      generator.generate("/gwt-console-server/rs", "jbpm", "html");
      generator.generate("/gwt-console-server/rs", "jbpm", "xml");
    }

}
