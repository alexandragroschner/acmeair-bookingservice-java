
## Acme Air Booking Service - Java/Liberty

An implementation of the Acme Air Booking Service for Java/Liberty. The primary task of the booking service is to store, update, and retrieve booking data.

This implementation can support running on a variety of runtime platforms including standalone bare metal system, Virtual Machines, docker containers, IBM Bluemix, IBM Bluemix Container Service, and IBM Spectrum CFC.

## Build Instructions
* Instructions for [setting up and building the codebase](Build_Instructions.md)


## Docker Instructions

See Documentation for the [Main Service](https://github.com/blueperf/acmeair-mainservice-java)


## IBM Container Instructions

See Documentation for the [Main Service](https://github.com/blueperf/acmeair-mainservice-java)


## Istio Instructions

See Documentation for the [Main Service](https://github.com/blueperf/acmeair-mainservice-java)

## USER COMMENTS:
To book a flight (with a car) via CLI (curl) a bearer token cookie is required to authenticate the user (COMMENTED OUT IN CODE FOR NOW).
This token is generated per session and can be found by inspecting (browser tool) the call after booking a flight via the GUI.
If user authentication is turned off in the code, just remove the -b option and its String parameter from the curl command.

Note that flight IDs are also generated randomly at startup, so these will have to be either looked up in the database over the monosh or copied from the browser inspect tool as well.
```
Expected return: the booking as json
curl -X POST -b "Bearer=$BEARERTOKEN;loggedinuser=$USERID" http://localhost/booking/bookflightsandcar?userid=$USERID&toFlightId=$FLIGHTID&toFlightSegId=$FLIGHTSEGID&retFlightId=$RETFLIGHTID&retFlightSegId=$RETFLIGHTSEGID&oneWayFlight=$ISONEWAY&carname=$CARNAME

example with bearer token
curl -X POST -b "Bearer=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwOi8vYWNtZWFpci1tcyIsImV4cCI6MTcwODg1ODQ5MCwianRpIjoiNGs1NW5naHpXTVI5V3A2OGM2bGJkdyIsImlhdCI6MTcwODg1NDg5MCwic3ViIjoidWlkMEBlbWFpbC5jb20iLCJ1cG4iOiJ1aWQwQGVtYWlsLmNvbSIsImdyb3VwcyI6WyJ1c2VyIl19.JVOGuOtc8S0tEZjsLAakDumrUCSamyAoMN0QZnUjNKDCY40lI3iTTos_tsBAVwqm6khlTbwFekctI3UsRTFEUjXHC_CxKxoijF0I2GhiFr3OX8a00swLxIyCTCBCB3rE7k7k67MYqcZyEzpq3jydEbtUfQm-5Kvz_Xtko22jZ6Hm4A-YD25RBxvB_9-Js5mZbBaypHDAM9PRiikUEK6VGnddPio_q4ss6CayUxzUWnDWI8h7-8ROj_1IxeRnA0lWEQw6NMRRlc9rrjIGojuo8XNm7_H_pPDMAMPtI1A-JmGE2zWuhY85yVVngAqBBd8lGKoVeQ_PmVlbDiqZ0ZnV5g;loggedinuser=uid0@email.com" "http://localhost/booking/bookflightsandcar?userid=uid0@email.com&toFlightId=69c21fab-e1dc-4680-a061-2aa8a47d12e6&toFlightSegId=AA120&retFlightId=b53f5660-9e01-4b18-9716-34788c8a3024&retFlightSegId=AA331&oneWayFlight=false&carname=trabant"

example without bearer token
curl -X POST "http://localhost/booking/bookflightsandcar?userid=uid0@email.com&toFlightId=69c21fab-e1dc-4680-a061-2aa8a47d12e6&toFlightSegId=AA120&retFlightId=b53f5660-9e01-4b18-9716-34788c8a3024&retFlightSegId=AA331&oneWayFlight=false&carname=trabant"

example one way without bearer token:
curl -X POST "http://localhost/booking/bookflightsandcar?userid=uid0@email.com&toFlightId=35d5d444-e260-4249-95e5-9cf5e4a8f2b0&toFlightSegId=AA120&oneWayFlight=true&carname=trabant"
```