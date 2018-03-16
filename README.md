# Akka Streams Chirper Client

This project is a demonstration project that introduces some of the features of
[Akka Streams](https://doc.akka.io/docs/akka/current/stream/index.html?language=java)
and [Akka HTTP](https://doc.akka.io/docs/akka-http/current/index.html?language=java).

There are three runnable source files,
`ChirperClientSimulator`, `ChirperHistoryWebSocket`, and `ChirperLiveWebSocket`.
* `ChirperClientSimulator` Simulates the activities of a Chirper application client.
It creates users, add user friends, and randomly creates chirps.
* `ChirperHistoryWebSocket` Reads all of the current chirp history via a web-socket.
* `ChirperLiveWebSocket` Opens a web-socket and reads chirps as they are created.

This project requires that the Chirper application is running in the background when you run any one or all of these
sample programs.

The Chirper application is a chat app. Users can register with the app, add friends to their network,
and create chirps - chat messages.

## Installation

Clone this project and the Chirper project.
~~~~
git clone https://github.com/lagom/lagom-java-chirper-example.git
git clone https://github.com/mckeeh3/akka-streams-chirper-client.git
~~~~

## Execution

These steps are done via command windows and assume that you are running a bash compatible shell. You may want to open
4 command windows so that you can run all three executables in parallel.

Run the Chirper application.
~~~~
cd lagom-java-chirper-example
mvn lagom:runAll
~~~~

Once the Chirper application is running run the `ChirperClientSimulator`.
~~~~
cd akka-streams-chirper-client
mvn compile exec:java@client
~~~~
This will create some Chirper users, add some friends to each user, and then it will create a continuous series of random user chirps.

You may stop or continue to run `ChirperClientSimulator` before the next step. However, the Chirper app must be running in the
background.

__Note:__ `ChirperClientSimulator` will continue to run until you hit the Enter key in the terminal window.

Now that some users, friends, and chirps have been created you can run `ChirperHistoryWebSocket` and `ChirperLiveWebSocket`.

Run `ChirperHistoryWebSocket`.
~~~~
cd akka-streams-chirper-client
mvn compile exec:java@history
~~~~
Run `ChirperLiveWebSocket`.
~~~~
cd akka-streams-chirper-client
mvn compile exec:java@live
~~~~

__Note:__ `ChirperLiveWebSocket` will continue to run until you hit the Enter key in the terminal window.
