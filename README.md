Know-the-flow is a daemon, written in Clojure, providing a REST API for tracking the dispensation of liquids, typically from a keg as you would find for coldbrew coffee, as is the case in our office, or beer.

Paired with a flow meter, connected via an Arduino, know-the-flow will surface basic details on consumption and remaining capacity.  KTF can be run on any host to which the Arduino can connect, and a RaspberryPi works well.  NOTE: the Arduino driver code, and flow meter selection, comes from the [RaspberryPints project](http://raspberrypints.com/).


## HTTP API
Using an embedded Jetty httpd, KTF provides a simple REST API.

### GET /cask - How much is remaining?
Returns the configured and remaining capacity, in milliliters.
```
{"capacity":18927,"remaining":18182}
```

### GET /cask/{since} - How has been consumed since timestamp?
{since} is a timestamp in Unix epoch seconds.
Returns the requested timestamp and amount consumed, in milliliters
```
{"ts":1500910483,"consumed":200}
```

### PUT /cask/{vol} - Administrative interface for adjusting cask state
This endpoint is intended as an administrative interface, for adjusting the state of the cask, as needed.
{vol} is a positive (optionally unsigned) or negative value to add or remove from the cask.
Returns 200 "OK" on success.
