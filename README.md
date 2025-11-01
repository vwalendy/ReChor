# ReChor — Public Transit Route Planner

ReChor is a Swiss public transport route planner that finds and displays optimal journeys between two stations.  
It was developed as part of my Computer Science studies at EPFL.

The app computes multiple good routes (not just "the fastest") and shows a detailed breakdown of each trip: departures, arrivals, intermediate stops, and transfers. The UI lets you search, compare, and inspect each journey.

---

## Features

### 🧠 Multi-criteria routing
ReChor computes a Pareto front of journeys. That means it keeps only the "interesting" trips:
- minimal travel time
- minimal number of changes
- no dominated routes

So instead of giving you one answer, it shows you the best trade-offs.

### 🗺 Journey details
For each proposed journey you get:
- which train/bus you take
- from which stop/platform
- departure and arrival times
- all segments chained cleanly

### 💻 JavaFX UI
Graphical interface with:
- origin & destination search fields
- list of best journeys
- detail panel for the selected journey
- (optional) map panel placeholder

### 🚉 Real timetable data
Uses real Swiss public transport timetable snapshots.

---

## Tech stack

- **Language:** Java
- **UI:** JavaFX
- **Algorithms / data structures:** custom public transport routing with Pareto dominance
- **IDE:** IntelliJ IDEA
- **Build:** Standard IntelliJ project (no Maven/Gradle). Run `Main.java` directly.

---

## How to run

1. Clone the repo:
   ```bash
   git clone https://github.com/vwalendy/ReChor.git
   cd ReChor
