# FluxChat Opportunities

FluxChat is being reshaped from a basic chat app into a lightweight career
network for job opportunities. The app still uses Java sockets, but the server
now supports professional profiles, job posts, job listings, applications, and
public networking messages.

## Current Features

- Console client for networking messages and opportunity commands
- Swing desktop client that starts with Register/Login only, then opens job tools after authentication
- Multi-client socket server
- Required CV-style registration for job applicants
- Strong password policy: uppercase, lowercase, digit, special character, and 8+ characters
- South African ID number format, birth date, and checksum validation
- Duplicate ID number blocking
- Automatic job matching after registration or login
- Optional source links for jobs from LinkedIn or another external job board
- Local job safety checks for suspicious wording, invalid links, shortened links, and company/link mismatch
- File-backed user directory
- File-backed job posts
- File-backed application messages
- Public announcements when people join, leave, add users, update profiles, post jobs, or apply
- Simple run scripts for local development

## Commands

After connecting, use these commands from the console client or Swing message
box:

```text
/registercv First name | Last name | ID number | email | phone | location | role | skills | education | experience | password
/loginid IDNumber password
/register password
/login password
/profile Role | skills | location
/adduser Name | role | skills | location
/users
/post Job title | company | location | description | optional source URL
/jobs
/matches
/apply JobId Short application message
/applications JobId
/help
```

Any other text is sent as a public networking message.

Example flow:

```text
/registercv Alice | Mokoena | 8001015009087 | alice@example.com | 0712345678 | Johannesburg | Junior Java Developer | Java, SQL, Swing | Diploma in IT | 2 years support experience | Strongpass1!
/adduser Thabo | Recruiter | Hiring, interviews | Cape Town
/users
/post Support Engineer | Acme | Remote | Help customers troubleshoot accounts | https://www.linkedin.com/jobs/view/example
/jobs
/matches
/apply 1 I have Java support experience and can start immediately.
/applications 1
```

If a name has been registered, future connections using that name must run
`/login password` before changing profile data, posting jobs, or applying.

For the stronger CV flow, use `/loginid IDNumber password`. Job applications
are only accepted from users who completed registration with a validated CV
profile. Each application stores the applicant's CV summary with the message.

Passwords must be at least 8 characters and include uppercase, lowercase, a
digit, and a special character.

Important identity limitation: FluxChat validates South African ID number
structure locally, including date and checksum. It cannot prove that a first
name and last name legally match that ID number without integrating a real KYC
or government identity verification service.

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

When the Swing app first opens, it shows only two choices:

- Register: creates the account and CV profile using first name, last name, ID
  number, contact details, role, skills, education, experience, and password
- Login: signs in with ID number and password

After registration or login succeeds, the main app opens with action tabs:

- Jobs: post a job, attach a source link, refresh jobs, or show your matches
- Apply: apply to a job with the saved CV profile or view applications for a job
- Users: list users, list jobs, show matches, or show help

The message box at the bottom still accepts raw commands and normal public
networking messages.

## Job Matching

FluxChat matches jobs against the signed-in user's CV profile:

- skills are matched against the job title and description
- target role words are matched against the job title and description
- location is matched against the job location, with `Remote` treated as a match

After successful registration or login, the server automatically sends matched
opportunities. You can also click `My Matches` or type:

```text
/matches
```

## LinkedIn Integration

FluxChat can store a LinkedIn job URL as the job source link, but it does not
scrape LinkedIn.

LinkedIn's official Job Posting API is restricted to approved Talent
Solutions/ATS partners, and current documentation says access requires approval.
The realistic path is:

- use the `Source Link` field now for manually captured LinkedIn job links
- apply for LinkedIn Apply Connect / Talent Solutions partner access later
- once approved, replace manual source links with an authorized API sync

Do not build this by scraping LinkedIn pages; that is brittle and likely to
violate platform rules.

## Job Safety Checks

FluxChat rejects job posts locally when they contain obvious scam signals:

- invalid source links
- shortened links such as `bit.ly` or `tinyurl.com`
- source links whose domain does not match the company name, unless it is a
  known job board such as LinkedIn
- suspicious phrases such as application fees, upfront payment, guaranteed jobs,
  WhatsApp-only recruitment, Telegram, bank details, or no interview
- descriptions that are too short to provide basic job detail

This does not prove a job is legitimate. It reduces obvious risk. A production
version should add moderator approval, company verification, report/block tools,
and live URL checks through trusted services.

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
- Authentication is basic ID/password, not a full session or role system yet
- Local ID validation does not replace external KYC/name matching
- Job safety checks are local heuristics, not guaranteed fraud detection
- LinkedIn job syncing requires approved LinkedIn API access
- No private recruiter-to-candidate messages
- No saved resumes, CV uploads, or company pages
- No search or filters yet
- No moderation tools yet

## Next Product Steps

Good next improvements would be:

- Add private messages between recruiters and candidates
- Add job categories, locations, salary ranges, and filters
- Add separate recruiter and job seeker roles
- Add CV links, document uploads, and recruiter-only application viewing
- Integrate a KYC provider to verify that legal names match ID numbers
- Replace socket commands with a backend API and web/mobile frontend
- Move from file persistence to SQLite or PostgreSQL
- Add moderation for spam and fake opportunities
