# Java Online Voting System

This is a complete online voting system built only with Java. It does not use HTML, CSS, JavaScript, PHP, database servers, or any other tech stack. The interface is made with Java Swing.

## Features

- Admin and voter login
- Voter registration
- Admin voter deletion before voting
- Election title, notice, and open/close voting control
- Voting start time and end time control
- Candidate add/remove management
- One vote per voter
- Live result table for admin and voters
- Winner/current leader display with tie handling
- Registered voter status table
- File-based data persistence using Java serialization
- Result export to a text file
- Seed accounts and candidates for quick testing

## Default Login

Admin:

```text
User ID: admin
Password: admin123
Role: Admin
```

On the login screen, make sure you select `Admin` in the Role dropdown.

Sample voters:

```text
User ID: voter1
Password: vote123
Role: Voter

User ID: voter2
Password: vote123
Role: Voter
```

## Run

Compile:

```bash
javac -d out src/votingsystem/OnlineVotingSystem.java
```

Run:

```bash
java -cp out votingsystem.OnlineVotingSystem
```

The application creates `voting-data.ser` in the project folder after saving data.

## Timeline And Winner

Admin can open the **Election** tab and set:

```text
Voting Start Time: yyyy-MM-dd HH:mm
Voting End Time: yyyy-MM-dd HH:mm
```

Example:

```text
2026-05-08 19:30
```

Voters can vote only between the start and end time while voting is open. The **Results** tab shows the winner after the end time, or the current leader while voting is still active.

                                                                               © 2026 Asfiya Anjum
