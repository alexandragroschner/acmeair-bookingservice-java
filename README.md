
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

## User-added Docs

Getting bloat via id from bloat service and saving it in the database:
-> make sure to add bloat in bloatService first (described in bloatService README)
```
# expected response: new ID of bloat added to booking db
ID=<idOBloatInBloatDB>
curl http://localhost/booking/bloatbyidandwrite/$ID
```
