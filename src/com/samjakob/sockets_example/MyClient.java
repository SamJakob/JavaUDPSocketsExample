package com.samjakob.sockets_example;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class MyClient {

    /**
     * The hostname of the server to connect to.
     */
    public static final String SERVER_HOSTNAME = "localhost";

    /**
     * The client datagram socket that connects to the server datagram socket.
     */
    DatagramSocket clientSocket;

    /** Trivial main method that creates a new {@link MyClient} and calls
     * 'start' on it. */
    public static void main(String[] args) {
        var client = new MyClient();
        client.start();
    }

    /**
     * Start the client.
     * Once this is called, the client will constantly wait for user input in
     * the terminal, and for every line entered into the terminal will send it
     * to the server and print a response from the server.
     */
    public void start() {

        var scanner = new Scanner(System.in);

        // Initialize the client socket.
        try {

            // Specifying no parameters to DatagramSocket causes Java to
            // request a free port from the OS for the socket, it binds to the
            // wildcard address (meaning any IP address on the system).
            clientSocket = new DatagramSocket();

        } catch(SocketException ex) {
            System.err.println(
                    "Failed to initialize the client socket. " +
                            "Is there a free port?"
            );
            ex.printStackTrace();
        }

        // Attempt to identify the server address and set it if it was
        // successfully identified.
        final InetAddress serverAddress;
        try {
            serverAddress = InetAddress.getByName(SERVER_HOSTNAME);
        } catch (UnknownHostException ex) {
            System.err.println("Unknown host: " + SERVER_HOSTNAME);
            ex.printStackTrace();
            return;
        }

        // Print the prompt for the user to write a message.
        System.out.print("> ");

        // Likewise to the server, we create a buffer to receive data from the
        // datagram socket.
        //
        // In this case, our buffer is 256 bytes, which means we can
        // receive up to 256 bytes for any given message.
        //
        // You can freely adjust this, but obviously a bigger buffer means
        // more memory is required.
        byte[] buffer = new byte[256];

        // While we're connected, read a new line and if it's not "exit", send
        // it to the server and print the result.
        while (!clientSocket.isClosed()) {

            try {
                // TODO: when should a user be able to send messages?
                //  Should every outgoing message expect a response before
                //  another message can be sent?

                // If our System.in has some bytes ready, which would imply
                // that our Scanner has a next line (i.e., someone has
                // typed something into the console) then read the input.
                //
                // We can't just use Scanner.hasNext here because hasNext
                // is blocking and doing that would stop us from being able
                // to check for incoming messages from the server whilst we
                // wait for input.
                if (System.in.available() > 0) {
                    String message = scanner.nextLine();

                    // If the command is exit, close the socket and break
                    // out of the loop.
                    if (message.equalsIgnoreCase("exit")) {
                        // Send exit to the server to tell it that the
                        // client connection is closing.
                        var exitBuffer = message.getBytes(StandardCharsets.UTF_8);
                        clientSocket.send(new DatagramPacket(
                            exitBuffer,
                            exitBuffer.length,
                            serverAddress,
                            ScuffedProtocol.PORT
                        ));

                        // TODO: how should you tell the server that your
                        //  client is disconnecting? Should you introduce
                        //  a mechanism for reconnecting automatically if
                        //  the connection drops?

                        clientSocket.close();
                        break;
                    }

                    // Otherwise, send the message.
                    var messageBuffer = message.getBytes(StandardCharsets.UTF_8);
                    clientSocket.send(new DatagramPacket(
                        messageBuffer,
                        messageBuffer.length,
                        serverAddress,
                        ScuffedProtocol.PORT
                    ));

                    // Contrary to the TCP example, we attempt to receive the
                    // message from the server right after we've sent it.
                    //
                    // This is done to get a trivial working proof-of-concept,
                    // however this is hardly robust and isn't really useful
                    // for anything other than a simple echo server.
                    //
                    // You might want to use a Java Thread or asynchronous
                    // Runnable to accept data from clients simultaneously with
                    // accepting input from the terminal.
                    var incomingPacket = new DatagramPacket(
                        buffer,
                        buffer.length,
                        serverAddress,
                        ScuffedProtocol.PORT
                    );
                    clientSocket.receive(incomingPacket);

                    // Convert the raw bytes into a String.
                    // See the server for more details on this.
                    var messageResponse = new String(
                        incomingPacket.getData(), 0, incomingPacket.getLength(),
                        StandardCharsets.UTF_8
                    );

                    System.out.println("Server: " + messageResponse);

                    System.out.print("> ");

                }

            } catch (IOException ex) {
                // If we encounter an IOException, it means there was a
                // problem communicating (IO = Input/Output) so we'll log
                // the error.
                System.err.println(
                        "A communication error occurred with the server."
                );
                ex.printStackTrace();
                break;
            }

        }

    }

}
