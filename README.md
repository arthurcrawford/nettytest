nettytest
=========

Netty test server with alternate return channel and multiplexed protocols.

This is based on the netty documentation and examples such as the echo server.  I wrote it as an example usage of the
netty framework and how to implement a protocol that allows for return messages to be sent back on an alternate
connection when the incoming channel breaks.  Also, it allows for the multiplexing of both a binary byte-level protocol
and a text-based test protocol.

To build the server.

    $ mvn clean install

To run.

    $ java -jar target/nettytest-1.0-jar-with-dependencies.jar

To test.

    $ cat TestMessage - | nc localhost 9797 ...

    Client connected successfully
    1385120339562973000: 48656C6C6F3A2D29
