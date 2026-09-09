# Capstone Clan Bingo

A collaborative RuneLite bingo board for clan events.

## Features

- Shared team bingo boards using a team code
- Automatic RuneScape display-name detection
- Claim individual bingo tiles
- Mark tiles complete
- Track percentage or X/Y progress
- Request aid from teammates
- Join another player's aid request
- Display multiple participants on a tile
- Automatic shared-board synchronization
- Manual refresh button
- Persistent team-code selection
- Draggable borderless bingo board that can be moved to another monitor

## How it works

Players receive a six-character team code from the event organizer.

Enter the code in the Capstone Clan Bingo RuneLite sidebar and click **Join Team**.

Once connected, open the bingo board. Changes made by members of the same team are synchronized through the Capstone Clan Bingo server.

## Controls

### Left click

Cycles a tile through:

Open → Claimed → Complete → Open

### Right click

Provides additional controls including:

- Claim tile
- Mark open
- Mark complete
- Set progress
- Request/cancel aid
- Join/leave aid

## Third-party server

This plugin connects to a third-party server operated for Capstone Clan Bingo synchronization.

The plugin transmits:

- RuneScape display name
- Bingo team code
- Bingo tile ownership
- Bingo progress
- Completion state
- Aid-request and participation information

An IP address is necessarily transmitted as part of connecting to the server.

The plugin does not transmit RuneScape passwords, Jagex credentials, bank contents, inventory contents, chat messages, or any other game data.

The synchronization server is not controlled or verified by the RuneLite Developers.

## Privacy

Data is used only to provide shared bingo-board functionality for participating teams.

## Support

Please report bugs or feature requests through this repository's GitHub issue tracker.