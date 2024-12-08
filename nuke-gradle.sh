#!/bin/bash

find . \( -name ".gradle" -o -name "build" \) -type d -exec rm -rf {} \;
