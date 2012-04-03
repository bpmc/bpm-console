Special node for executing GWT tests under linux:
The GWT-maven plugin executes any test prefixed "GwtTest"
(See http://gwt-maven.googlecode.com/svn/docs/maven-googlewebtoolkit2-plugin/testing.html)
It reads stderr in order to figure wether or not the test has been successful.
On fedora (or any other linux) it happens that that mozilla components issue the following 
INFO message to stderr, which causes the test to fail:

> Gtk-WARNING **: Unable to locate theme engine in module_path: "nodoka"

This is caused by missing nodoka (theme) package; please install it as follow:
yum install gtk-nodoka-engine (on fedora)

