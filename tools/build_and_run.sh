#! /usr/bin/env bash

ant clean
ant debug
./deploy.sh
./log.sh
