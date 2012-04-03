Running in hosted mode:
----------------------

Start the GWT shell with 

	mvn gwt:<run|debug>

When the hosted browser is started, it's enough to hit the 'refresh' button to recompile
and verfiy changes.

NOTE: Really quick turnaround through 

	mvn -Dhosted gwt:<run|debug>

(Will run "mvn -o -Dgoogle.webtoolkit.compileSkip=true gwt:<run|debug>")


Running in web mode:
-------------------

mvn package 

Produces a war file in target/gwt-console.war,
which can be deployed to a running jboss instance.

Problems?
---------
Please post any questions to the gwt-console developer forum.
(http://www.jboss.org/index.html?module=bb&op=viewforum&f=295)

Have fun.



