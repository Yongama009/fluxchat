# FluxChat Opportunities

FluxChat is being reshaped from a basic chat app into a lightweight career
network for job opportunities. The app still uses Java sockets, but the server
now supports professional profiles, job posts, job listings, applications, and
public networking messages.

## Current Features

- Console client for networking messages and opportunity commands
- Swing desktop client with forms for account, profile, jobs, applications, users, and messages
- Multi-client socket server
- User display names and optional password registration
- File-backed user directory
- File-backed job posts
- File-backed application messages
- Public announcements when people join, leave, add users, update profiles, post jobs, or apply
- Simple run scripts for local development

## Commands

After connecting, use these commands from the console client or Swing message
box:

```text
/register password
/login password
/profile Role | skills | location
/adduser Name | role | skills | location
/users
/post Job title | company | location | description
/jobs
/apply JobId Short application message
/applications JobId
/help
```

Any other text is sent as a public networking message.

Example flow:

```text
/register strongpass
/profile Junior Java Developer | Java, SQL, Swing | Johannesburg
/adduser Thabo | Recruiter | Hiring, interviews | Cape Town
/users
/post Support Engineer | Acme | Remote | Help customers troubleshoot accounts
/jobs
/apply 1 I have Java support experience and can start immediately.
/applications 1
```

If a name has been registered, future connections using that name must run
`/login password` before changing profile data, posting jobs, or applying.

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
      store/             # File-backed repository, models, and password hashing
  scripts/
    compile.sh
    run-server.sh
    run-client.sh
    run-gui.sh
  data/
    fluxchat.db          # Created at runtime; ignored by git
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
ChatApp/scripts/compile.sh
```

Manual compile command:

```bash
javac -d /tmp/fluxchat-classes ChatApp/src/server/store/*.java ChatApp/src/server/*.java ChatApp/src/client/*.java
```

## Run Locally

Open one terminal and start the server:

```bash
ChatApp/scripts/run-server.sh
```

Open another terminal and start the console client:

```bash
ChatApp/scripts/run-client.sh
```

Or start the Swing desktop client:

```bash
ChatApp/scripts/run-gui.sh
```

The Swing app has action tabs on the right:

- Account: register or log in with the selected display name
- Profile: update your own role, skills, and location
- Jobs: post a job or refresh the job list
- Apply: apply to a job or view applications for a job
- Users: list users, list jobs, or show help

The message box at the bottom still accepts raw commands and normal public
networking messages.

The server stores data in `ChatApp/data/fluxchat.db` by default. You can pass a
custom port and data file:

```bash
ChatApp/scripts/run-server.sh 5001 /tmp/fluxchat-dev.db
```

## Run Over a Real Network

To test between two computers, run the server on one computer and connect to it
from another computer on the same Wi-Fi or LAN.

On the server computer:

```bash
ChatApp/scripts/run-server.sh 5000
```

Find the server computer's local IP address:

```bash
ip addr
```

Look for an address like `192.168.x.x` or `10.x.x.x`.

On another computer, start the client using the server computer's IP address:

```bash
ChatApp/scripts/run-client.sh 192.168.x.x 5000
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
- Persistence is file-based, not a production database yet
- Authentication is basic username/password, not a full session or role system yet
- No private recruiter-to-candidate messages
- No saved resumes, CV uploads, or company pages
- No search or filters yet
- No moderation tools yet

## Next Product Steps

Good next improvements would be:

- Add private messages between recruiters and candidates
- Add job categories, locations, salary ranges, and filters
- Add separate recruiter and job seeker roles
- Add a cleaner profile format with experience, education, and links
- Replace socket commands with a backend API and web/mobile frontend
- Move from file persistence to SQLite or PostgreSQL
- Add moderation for spam and fake opportunities
