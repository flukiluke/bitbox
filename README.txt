BitBox, featuring UDP and client extensions
-------------------------------------------
Produced for the subject COMP90015 Distributed Systems at The University of Melbourne
Original concept and protocol design: Aaron Harwood
This implementation: Luke Ceddia, Bosco Feng, Rainer Selby, Brody Taylor


BitBox is a distributed file system client, capable of synchronising a directory
between many peers in a decentralised network.

This package comes with:
 - A configuration.properties file.
 - A client's private key bitboxclient_rsa which corresponds to the public key already in the configuration.properties file.
 - All Java sources and a pom.xml for building with Maven.

The public/private keypair is setup with the identity name 'brodyt'.

To run the BitBox peer:

$ java -cp bitbox.jar unimelb.bitbox.Peer

To run the client:

$ java -cp bitbox.jar unimelb.bitbox.Client -i brodyt -c list_peers -s ourpeer.com:9000

or

$ java -cp bitbox.jar unimelb.bitbox.Client -i brodyt -c connect_peer -s ourpeer.com:9000 -p remotepeer.com:8400


If you want to compile from scratch, you will require Java 1.8. Compile using maven:

$ mvn package

Which produces target/bitbox-0.0.1-SNAPSHOT-jar-with-dependencies.jar.



