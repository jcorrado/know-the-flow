Know-the-flow is a daemon, written in Clojure, providing a REST API for tracking the dispensation of liquids, typically from a keg as you would find for coldbrew coffee, as is the case in our office, or beer.

Paired with a flow meter, connected via an Arduino, know-the-flow will surface basic details on consumption and remaining capacity.  KTF can be run on any host to which the Arduino can connect, and a RaspberryPi works well.  NOTE: the Arduino driver code, and flow meter selection, comes from the [RaspberryPints project](http://raspberrypints.com/).


## HTTP API
Using an embedded Jetty httpd, KTF provides a simple REST API.

### GET /cask - How much is remaining?
* Returns the configured and remaining capacity, in milliliters.
```
{"capacity":18927,"remaining":18182}
```

### GET /cask/{since} - How much has been consumed since time-stamp?
* ``{since}`` is a time-stamp in Unix epoch seconds.
* Returns the requested time-stamp and amount consumed, in milliliters
```
{"ts":1500910483,"consumed":200}
```

### PUT /cask/{vol} - Administrative interface for adjusting cask state
This endpoint is intended as an administrative interface, for adjusting the state of the cask, as needed.
* ``{vol}`` is a positive (optionally unsigned) or negative value to add or remove from the cask.
* Returns 200 "OK" on success.


## Design Considerations

### Determining when a cask is kicked
We define the starting capacity of our cask, and decrement capacity based on dispensation events, but this is *not* actually how we determine when the cask is empty.  Instead, our flow meter driver provides a "kicked" event.  If the system is incorrectly calibrated, we will continue to decrement our cask's capacity, past zero.  We make no attempt to handle this, instead logging events so that re-calibration can be investigated.  It is expected that API consumers will provide a strategy for sensibly surfacing this state to users.  Simple messaging like "Just about empty" would make sense.

### Determining when a cask has been replaced
One of our design goals is a system that doesn't require operator intervention, so we have to infer when a cask has been replaced.  Any simple heuristic is complicated by the fact that kick events are often followed by "ghost" dispensation events of small amounts.

Our strategy is, upon kick, to *immediately* reset a cask's capacity to full.  However, our API will continue to report the cask as empty until ``new-cask-threshold`` (presently 300 ml) has been dispensed.  This helps avoid reporting a new cask, until it has truly been replaced, but also means that there is no announcement of a new cask until someone takes the first cup.


## Operations Notes

### Running The Daemon
For now, the daemon should be run by hand.  Once packaged, a proper init solution will be provider.  

Run the uberjar, passing the serial pty and HTTP listening port.
```
pi@flow0:~ $ java -jar know-the-flow-0.2.0-standalone.jar /dev/ttyACM0 8080
-07-24 03:54:21 flow0 DEBUG [org.eclipse.jetty.util.log:176] - Logging to com.github.fzakaria.slf4j.timbre.TimbreLoggerAdapter@cc3423 via org.eclipse.jetty.util.log.Slf4j
g
-07-24 03:54:22 flow0 INFO [org.eclipse.jetty.util.log:186] - Logging initialized @88141ms
17-07-24 03:55:09 +0000 flow0 INFO [know-the-flow.core:44] - Starting know-the-flow server
17-07-24 03:55:10 +0000 flow0 INFO [org.eclipse.jetty.server.Server:327] - jetty-9.2.z-SNAPSHOT
17-07-24 03:55:11 +0000 flow0 INFO [org.eclipse.jetty.server.ServerConnector:266] - Started ServerConnector@1ed5a1c{HTTP/1.1}{0.0.0.0:8080}
17-07-24 03:55:11 +0000 flow0 INFO [org.eclipse.jetty.server.Server:379] - Started @136870ms
```

### Log Files
* KTF log level is defined as ``info`` but can be make configurable if needed.  Logging is to STDOUT and ``know-the-flow.log`` in CWD from which the daemon was launched.
* KTF also writes out ``know-the-flow.txn``, an EDN file used for state tracking between restarts of the daemon.

### Considerations On Restarting
The state file ``know-the-flow.txn`` is written, but not currently used; a daemon restart means all state is lost.  For now, use the admin API to set state to the last capacity, as extracted from ``know-the-flow.log``.
