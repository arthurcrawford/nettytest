nettytest
=========

Netty test server with alternate return channel and multiplexed protocols.

This is based on the netty documentation and examples.  It was developed to understand how to use the netty framework
and how to implement a protocol that allows for return messages to be sent back on an alternate connection when the
incoming channel breaks.  Also, it allows for the multiplexing of both a binary byte-level protocol and a text-based
test protocol.