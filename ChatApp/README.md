# FluxChat Opportunities

FluxChat is being reshaped from a basic chat app into a lightweight career
network for job opportunities. The app still uses Java sockets, but the server
now supports professional profiles, job posts, job listings, applications, and
public networking messages.

## Current Features

- Console client for networking messages and opportunity commands
- Swing desktop client connected to the server
- Multi-client socket server
- User display names
- In-memory user directory
- In-memory job posts
- In-memory application messages
- Public announcements when people join, leave, add users, update profiles, post jobs, or apply

## Commands

After connecting, use these commands from the console client or Swing message
box:

```text
/profile Role | skills | location
/adduser Name | role | skills | location
/users
/post Job title | company | location | description
/jobs
/apply JobId Short application message
/help
```

Any other text is sent as a public networking message.

Example flow:

```text
/profile Junior Java Developer | Java, SQL, Swing | Johannesburg
/adduser Thabo | Recruiter | Hiring, interviews | Cape Town
/users
/post Support Engineer | Acme | Remote | Help customers troubleshoot accounts
/jobs
/apply 1 I have Java support experience and can start immediately.
```

## Project Structure

```text
ChatApp/
  src/
    client/
      Client.java        # Console client
      ChatGUI.java       # Swing desktop opportunity client
    server/
      Server.java        # Starts the opportunity server
      ClientHandler.java # Handles users, commands, jobs, and applications
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

## Run Locally

Open one terminal and start the server:

```bash
java -cp /tmp/fluxchat-classes server.Server
```

Open another terminal and start the console client:

```bash
java -cp /tmp/fluxchat-classes client.Client
```

Or start the Swing desktop client:

```bash
java -cp /tmp/fluxchat-classes client.ChatGUI
```

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
UFW, allow the app port with:

```bash
sudo ufw allow 5000/tcp
```

## Current Limitations

This is still an early MVP:

- No database yet, so jobs disappear when the server stops
- User profiles are still in memory only
- No login system or identity verification
- No private recruiter-to-candidate messages
- No saved resumes, CV uploads, or company pages
- No search or filters yet
- No moderation tools yet

## Next Product Steps

Good next improvements would be:

- Save users and jobs to a database or file
- Add private messages between recruiters and candidates
- Add job categories, locations, salary ranges, and filters
- Add separate recruiter and job seeker roles
- Add a cleaner profile format with experience, education, and links
- Add moderation for spam and fake opportunities
