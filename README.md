# BitBox
Project for COMP90015, Distributed Systems

Explore the amazing world of the BitBox file system!

Please note that BitBox requires Java 1.8.

## Contents
This archive contains:
 - This README.md
 - A compiled BitBox peer, that can be run as `java -cp bitbox.jar unimelb.bitbox.Peer`
 - Full source code for the BitBox peer
 - A report analysing the distributed system aspects of the project
 - A sample configuration.properties file
 - An empty share directory
 
## Compiling
The easiest way to compile is via maven:
```
$ mvn package
```
Which will give you a target/bitbox-*.jar file that can be run:
```
$ java -cp target/bitbox-0.0.1-SNAPSHOT-jar-with-dependencies.jar unimelb.bitbox.Peer
```