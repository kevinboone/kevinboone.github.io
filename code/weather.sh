#!/bin/sh

# A simple script to retrieve a 3-day weather forecast from the BBC's RSS
#  feed, and format it for display in a Linux terminal. This script
#  writes ANSI escapes to set the terminal colours, so won't be suitable
#  for anything except a terminal.
#
# See https://kevinboone.me/clh_weather.html
#
# Copyright (c)2021 Kevin Boone, GPL v3.0

# LOC is the location code. Look at the BBC weather website for a particular
#   location, and find the code in the URL
LOC=2643743

# Request the RSS feed, and use xmllint to parse the title and \
#  description fields \
curl -s https://weather-broker-cdn.api.bbci.co.uk/en/forecast/rss/3day/$LOC \
| xmllint --xpath rss/channel/item/title\|rss/channel/item/description - \
| sed -e 's/<[^>]*>//g' \
| tr '\n' '#' \
| sed -e 's/#/\n\n/g' \
| sed -e 's/ (.*F)//g' \
| sed -e 's/Minimum Temperature/Min/g' \
| sed -e 's/Maximum Temperature/Max/g' \
| sed -e 's/UV Risk/UV/g' \
| sed -e 's/Tonight\|Today\|Monday\|Tuesday\|Wednesday\
\|Thursday\|Friday\|Saturday\|Sunday/\\m[red]\0\\m[]/g' \
| sed -e 's/: [^,]*/\\fB\0\\fR/g' \
| sed -e 's/^$/###\n/' \
| sed -e 's/: / /g' \
| groff -k -Tutf8 -man \
| sed '/^$/d' \
| sed 's/###/\n/g'

