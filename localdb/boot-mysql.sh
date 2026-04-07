#!/bin/bash

cd `dirname $0`

docker compose up -d
echo "MySQL container started on port 43376"
