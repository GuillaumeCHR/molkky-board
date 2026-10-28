# Molkky Board — closed test plan (14 days)

Thank you for testing **Molkky Board**, a scorekeeper for Mölkky games: you tap the pins that fell, the app applies
the rules, keeps the score and the history. Several phones on the same field can follow and enter the same game,
directly between them, with no account and no internet.

This plan takes about **10 to 20 minutes a day, on 8 of the 14 days**. You do not need to follow it to the letter:
play when you can, and answer the questions you could test. A real game with real pins is the best test, but a game
played on the sofa is fine too.

> Version under test: **0.8.1 (build 9)** or later. Plan written on 2026-10-29.

---

## What we ask of you

1. **Stay enrolled for the whole 14 days**, and keep the app installed. Google only opens the app to the public if at
   least 12 testers stay enrolled for 14 days in a row: if you leave early, the count starts again from zero.
2. **Open the app now and then**, ideally on the days listed below.
3. **Send your answers and your bugs** with the in-app button (see « How to send feedback »).

## Before you start

- **Join the test** with the link you received: [opt-in link to be added], then install Molkky Board from the
  Play Store with the same Google account.
- Android 8 or later. To play on **several phones**: the guest phone needs Android 10 or later, and the organiser's
  phone needs Android 13 or later to create its own hotspot (otherwise, both phones use the same Wi-Fi).
- The app is in English if your phone is in English, and in French otherwise. You can switch at any time in
  **Settings › Language**.

## How to send feedback

**Settings › About › Send feedback** opens an email to yogistudiofr@gmail.com. It is pre-filled with the app
version and the phone model, nothing else. Write your answers in this form, one per line:

```
Q4: B — the « OK » button was hidden by the keyboard
Q7: Not tested
```

- One email per day or one at the end: as you prefer.
- **A bug?** Say what you did, what you expected, what happened, and attach a screenshot if you can.
- **The app crashed?** On the next launch, the app shows the crash report (technical details only, no game data)
  and offers to send it by email. Nothing is sent without your tap.

## Your privacy during the test

Molkky Board has no ads, no analytics and no trackers, test version included. Your games stay on your phone; when
several phones play together, they talk directly to each other on the local network, encrypted, never through the
internet. We only receive what you choose to email us.

---

## Day 1 — First launch

Steps: open the app (watch the intro, or tap to skip it), then **Home › New game**. Follow the help bubbles.

- **Q1** The intro (pins flying, Yogi Studio, then the logo):
  A. Nice · B. Too long · C. I skipped it · D. Not tested
- **Q2** Without any explanation, starting a game and entering the first throw was:
  A. Obvious · B. A bit confusing (where?) · C. I got stuck (where?) · D. Not tested
- **Q3** The 4 help bubbles (add players, tap the pins, OK, undo):
  A. Helpful · B. Annoying · C. I did not see them · D. Not tested

## Day 2 — A solo game

Steps: **New game**, 3 players or more, **Official** rules, **One set**. Play to the end.

- **Q4** Entering a throw (tap the pins, check the preview « +7 → 38 », then **OK**):
  A. Fast and clear · B. Too many taps · C. I entered a wrong throw by mistake (how?) · D. Not tested
- **Q5** **Miss** then **OK** for a throw where nothing fell:
  A. Clear · B. I expected Miss to save straight away · C. Not tested
- **Q6** The big score, the round badge (« Round 3 ») and the misses (three dots):
  A. Easy to read · B. Something is hard to read (what?) · C. Not tested
- **Q7** Going over 50 (back to 25) and three misses in a row (out of the game), if it happened:
  A. Clear animations · B. Confusing · C. It did not happen · D. Not tested

## Day 4 — A team game

Steps: **New game › In teams**, 2 teams of 2. Try the dice for a random team name, change the throwing order by
dragging, then play.

- **Q8** Creating the teams (names, colours, emblems, order):
  A. Easy · B. Fiddly (what?) · C. Not tested
- **Q9** During the game, it was clear which player of which team had to throw:
  A. Yes · B. Not always · C. Not tested
- **Q10** The scoreboard (podium icon on the left of the score strip):
  A. Easy to find and read · B. Hard to find · C. Not tested

## Day 5 — Mistakes and changes

Steps: during a game, try each of these:
**Undo** the last throw; **⋮ › Game log**, edit a throw from an earlier round, then delete another one;
**⋮ › Manage players**, add a late player, then remove a player.

- **Q11** Undo, edit and delete a throw (scores, back-to-25 and outs recalculated):
  A. Works as expected · B. A result looked wrong (which?) · C. Not tested
- **Q12** Adding and removing a player during the game:
  A. Easy · B. Confusing · C. Not tested
- **Q13** Close the app in the middle of a game (swipe it away), then open it again:
  A. The game came back exactly where I left it · B. Something was lost (what?) · C. Not tested

## Days 6 to 8 — Several phones (needs 2 phones or more)

Steps on the **organiser's** phone: start a game, then **⋮ › Manage phones**, choose **Hotspot** or **Same Wi-Fi**,
**Share the game**. On the **guest** phone: **Home › Join**, scan the QR code, accept the « Connect to network »
window if Android shows it. The organiser accepts (« It's Léa », « New player » or « Referee »).
Then enter throws on both phones. Try both network modes if you can.

- **Q14** Time to join, from tapping « Join » to seeing the game:
  A. Under 10 s · B. 10 to 30 s · C. More than 30 s · D. It failed (at which step?) · E. Not tested
- **Q15** Network mode that worked:
  A. Hotspot · B. Same Wi-Fi · C. Both · D. Neither (error message?) · E. Not tested
- **Q16** Throws and corrections showed up on every phone:
  A. Instantly · B. With a delay · C. Some were missing · D. Not tested
- **Q17** Move the guest phone away until it disconnects, then come back:
  A. It reconnected on its own · B. I had to scan again · C. It did not come back · D. Not tested
- **Q18** Permissions asked (camera, nearby devices, local network, notifications) and their explanations:
  A. Clear · B. Worrying or unclear (which?) · C. Not tested

## Day 9 — End of game and memory

Steps: finish a game, look at the end screen, play a **Rematch**, then open **History** and **Hall of Fame**.

- **Q19** The victory screen (ranking, stats, series of games):
  A. Clear · B. Too much information · C. Something is missing (what?) · D. Not tested
- **Q20** History and Hall of Fame:
  A. Clear and correct · B. A game or a record is missing or wrong · C. Not tested

## Day 10 — Sharing

Steps: on the end screen, **Share this moment**. Try a picture, the photo of the moment, the short video and the GIF,
and share one of them to a messaging app (or just save it).

- **Q21** The share pictures:
  A. Nice enough to post · B. Plain · C. Something is wrong (cut names, wrong score…) · D. Not tested
- **Q22** Video and GIF creation:
  A. Fast (under 10 s) · B. Slow (how long?) · C. It failed · D. Not tested

## Day 11 — Outdoors and settings

Steps: play a few throws outside, in the sun if possible. In **Settings**, try **Bright sunlight**, dark mode (from
the phone's settings), **Voice announcements**, **Sounds** and the **Language**.

- **Q23** Readability outside, with **Bright sunlight** on:
  A. Readable in full sun · B. Still hard to read · C. Not tested
- **Q24** Voice announcements and sounds:
  A. Useful · B. Annoying · C. They did not play (phone on silent or vibrate?) · D. Not tested
- **Q25** Large text (phone settings › font size at the maximum): nothing cut or overlapping?
  A. Everything readable · B. Something is cut (which screen?) · C. Not tested

## Day 12 — Rules and setting up the pins

Steps: **Home › Game rules** and **Home › Set up the pins**.

- **Q26** Could someone who has never played set up and play a correct game with the app alone?
  A. Yes · B. Almost (what is missing?) · C. No · D. Not tested

## Day 13 — Your data

Steps: **Settings › Data › Back up my data**, save the file, then **Restore a backup** with that same file.

- **Q27** Backup and restore:
  A. Worked, no duplicate games · B. Something went wrong · C. Not tested

## Day 14 — Overall

- **Q28** Did the app ever crash, freeze or lag?
  A. Never · B. Once or twice (when?) · C. Often
- **Q29** Would you use Molkky Board for your next real game?
  A. Yes · B. Maybe · C. No (why?)
- **Q30** One thing to improve before the public release, and one thing you liked (free text).

Thanks a lot! Please stay enrolled until the end of day 14, even if you have finished the plan.
