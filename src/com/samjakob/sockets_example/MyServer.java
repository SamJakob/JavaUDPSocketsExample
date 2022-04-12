package com.samjakob.sockets_example;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class MyServer {

    /**
     * The server datagram socket that accepts incoming datagrams.
     */
    DatagramSocket serverSocket;

    /**
     * Trivial main method that creates a new {@link MyServer} and calls
     * 'start' on it.
     */
    public static void main(String[] args) {
        var server = new MyServer();
        server.start();
    }

    /**
     * Start the server.
     * Once this is called, the server will constantly wait for new messages,
     * until it receives a message of "exit".
     */
    public void start() {

        // If the server socket has already been initialized, do nothing.
        if (serverSocket != null) return;

        // Otherwise, initialize the server socket.

        try {

            // Initialize the server datagram socket and bind to the port for
            // our protocol.
            serverSocket = new DatagramSocket(ScuffedProtocol.PORT);

            System.out.println(
                    "Now listening on port " + ScuffedProtocol.PORT + "!"
            );

            // We create a buffer to receive data from the datagram socket.
            // In this case, our buffer is 256 bytes, which means we can
            // receive up to 256 bytes for any given message.
            //
            // You can freely adjust this, but obviously a bigger buffer means
            // more memory is required.
            byte[] buffer = new byte[256];

            while (!serverSocket.isClosed()) {

                try {

                    // Attempt to receive a new packet of length up to the
                    // specified buffer.
                    //
                    // If the message we're receiving is longer than the buffer
                    // length, it will be truncated.
                    //
                    // This method 'blocks' execution until a message is
                    // received, so you may wish to introduce delegates as in
                    // the TCP example and pass them messages per-client to
                    // process.
                    //
                    // Doing this isn't as trivial as it is in the TCP example
                    // though, because UDP is 'connection-less' and therefore
                    // stateless, there is no connection setup, and this means
                    // any logic to route messages based on who sent them needs
                    // to be done manually instead of with the Java library or
                    // at a protocol level.
                    //
                    // https://stackoverflow.com/a/26390150/2872279
                    // The above StackOverflow example has a detailed
                    // commentary on this.
                    var incomingPacket = new DatagramPacket(buffer, buffer.length);
                    serverSocket.receive(incomingPacket);

                    // Get the details of who sent the message.
                    var clientAddress = incomingPacket.getAddress();
                    var clientPort = incomingPacket.getPort();

                    // Convert the raw bytes into a String.
                    var message = new String(
                        // Create the string using bytes from the incoming packet.
                        incomingPacket.getData(),
                        // Offset (starting point) = 0
                        0,
                        // Length = length of incoming packet
                        incomingPacket.getLength(),
                        // Character Set = UTF-8
                        StandardCharsets.UTF_8
                    );

                    // Process the message.
                    // If it is "exit", stop the server, otherwise print the
                    // message and send it back to the client.
                    if (message.equalsIgnoreCase("exit")) {
                        serverSocket.close();
                        serverSocket = null;
                        break;
                    }

                    System.out.println("Client: " + message);

                    // As noted below, we re-use the buffer that we received.
                    // If you wanted to encode a new message buffer you could
                    // do it as follows:

//                    var myMessage = "hello, world!";
//                    var myMessageBuffer = myMessage.getBytes(StandardCharsets.UTF_8);

                    // Send the message back to the client.
                    // Notice we use the same DatagramPacket constructor as we
                    // did for the incoming packet, however this time we also
                    // specify the destination address and port for the packet.
                    var outgoingPacket = new DatagramPacket(
                        // In this case, we want to send back the message we
                        // received, so we simply re-use the buffer from the
                        // incoming message. This is naturally more memory
                        // efficient than re-creating the buffer.
                        //
                        // IMPORTANT!!!
                        // Note the use of incomingPacket.getLength() here
                        // instead of buffer.length. We only want to send back
                        // the bytes in the message, not the entire buffer.
                        buffer, incomingPacket.getLength(),
                        // The destination address and port for the packet.
                        // In this case, it's the client that sent us the
                        // incomingPacket, so we re-use the clientAddress and
                        // clientPort variables we retrieved from the incoming
                        // packet.
                        clientAddress, clientPort
                    );

                    // Send the packet.
                    serverSocket.send(outgoingPacket);

                }  catch (IOException ex) {
                    // NOTE: contrary to the TCP example, we don't receive
                    // messages per connection, because UDP is not a
                    // 'session'-based protocol – i.e., you only get a stream
                    // of messages from various different hosts, rather than
                    // one stream of messages per connection or 'session'.
                    System.err.println(
                            "Communication error. " +
                                    "Is there a problem with the client?"
                    );
                }

            }

        } catch (SocketException ex) {
            System.err.println(
                    "Failed to start the server. " +
                            "Is the port already taken?"
            );
            ex.printStackTrace();
        }

    }

}
