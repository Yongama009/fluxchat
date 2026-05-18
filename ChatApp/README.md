# FluxChat

FluxChat is a small Java chat application built with sockets. It has a server
that accepts multiple clients and broadcasts each message to everyone connected.

The project currently includes:

- A console chat client that can send and receive messages
- A socket server that listens for incoming clients
- A basic Swing GUI window, which is not connected to the chat server yet

## Project Structure

```text
ChatApp/
  src/
    client/
      Client.java       # Console client
      ChatGUI.java      # Basic Swing window
    server/
      Server.java       # Starts the chat server
      ClientHandler.java # Handles one connected client
```

## Requirements

You need Java installed. You can check with:

```bash
java -version
javac -version
```

## Compile

From the project root:

```bash
cd /home/wtc27/IdeaProjects/fluxchat
javac -d /tmp/fluxchat-classes ChatApp/src/server/*.java ChatApp/src/client/*.java
```

This compiles the app into `/tmp/fluxchat-classes`.

## Run Locally

Open one terminal and start the server:

```bash
java -cp /tmp/fluxchat-classes server.Server
```

Open another terminal and start a client:

```bash
java -cp /tmp/fluxchat-classes client.Client
```

Open a third terminal and start another client:

```bash
java -cp /tmp/fluxchat-classes client.Client
```

Now type a message in either client and press Enter. The message should appear
in both client terminals.

## Run Over a Real Network

To test between two computers, run the server on one computer and connect to it
from another computer on the same Wi-Fi or LAN.

On the server computer:

```bash
java -cp /tmp/fluxchat-classes server.Server 5000
```

Find the server computer's local IP address:

```bash
ip addr
```

Look for an address like `192.168.x.x` or `10.x.x.x`.

On another computer, start the client using the server computer's IP address:

```bash
java -cp /tmp/fluxchat-classes client.Client 192.168.x.x 5000
```

Replace `192.168.x.x` with the real IP address of the server computer.

If the connection fails, the firewall may be blocking the port. On Linux with
UFW, allow the chat port with:

```bash
sudo ufw allow 5000/tcp
```

## Stop the Server

In the terminal where the server is running, press:

```bash
Ctrl + C
```

If you lost the terminal, find the Java process:

```bash
jps
```

Then stop the server process:

```bash
kill <pid>
```

## Current Limitations

This is an early learning project, so it keeps things simple:

- No usernames yet
- No message history
- No private messages
- No encryption
- No login system
- The GUI is only a window for now and is not wired to the server
- The server broadcasts messages to all connected clients

## Next Ideas

Good next improvements would be:

- Ask each user for a name before joining
- Show when users connect or disconnect
- Connect the Swing GUI to the server
- Add a cleaner shutdown for clients
- Remove disconnected clients from the server list
- Add timestamps to messages
