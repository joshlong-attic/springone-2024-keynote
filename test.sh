#!/usr/bin/env bash

curl -d'{"name":"Cora"}' -XPOST -H"content-type: application/json" http://localhost:8080/dogs/45/adoptions
