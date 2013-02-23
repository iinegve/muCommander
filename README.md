muCommander
===========

www.mucommander.com fork of v0.9.0

Current repository contains also sources of all required libraries:
* collections
* conf
* file
* io
* runtime
* util

Key difference from v0.9.0:
* supports HDFS 2.0.0

===========
Sources could be built right after git clone.
To get runnable version the one must
* download everything
* run mvn clean install
* in ./manager/target directory run manager-{version}.jar (it uses target/lib folder)
