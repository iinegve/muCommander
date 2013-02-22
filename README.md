muCommander
===========

muCommander fork of version 0.9.0. Initially it lives at www.mucommander.com, 
but for a while those guys stop updated it.

Current fork contains also sources of required libraries (collections, conf, etc).

Important thing is the file module has been updated to support new version of HDFS - 
currently 2.0.0 - but probably it will work with newer ones. 

===========
Sources could be built right after git clone.
To get runnable version the one must
1. run mvn clean install
2. in ./manager/target directory run manager-{version}.jar (it uses target/lib folder)
