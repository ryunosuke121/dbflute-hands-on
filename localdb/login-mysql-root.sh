#!/bin/bash

cd `dirname $0`

docker compose exec mysql mysql --user=root --default-character-set=utf8mb4
