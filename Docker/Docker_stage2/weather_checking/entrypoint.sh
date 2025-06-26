#! /bin/bash

echo "Timestamp:$(date)"
echo "Pinging: $@"
ping "$@"
