#!/bin/sh

# radio-browser-query.sh
#
# A script to query the Internet radio station list maintained at
# radio-browser.info.
#
# Example: radio-browser-query.sh bbc radio 3 
#
# Copyright (c)2021 Kevin Boone, GPL v3.0

# SERVER_DNS is the advertised DNS record for radio-browser.info. We
#  will later get the list of API servers from this DNS record.

SERVER_DNS=_api._tcp.radio-browser.info

# Let's concatenate all the command-line arguments into a single string,
#  so the user doesn't have to put quotation marks around strings that
#  contain spaces. We can only do this because we don't take any other
#  command line arguments or switches.

ARGS="$*"

if [ "$ARGS" = "" ] ; then
  echo "Usage: $0 [partial station name]"
  exit 1
fi

# Call the concatenated arguments "QUERY", since they form the search term.

QUERY=$ARGS

# Select a radio-browser server randomly by querying the DNS
#  SRV record, randomly shuffling the results, then picking
#  the item at the top of the shuffled list

SERVER=$(dig  +short $SERVER_DNS SRV | shuf | head -1 | cut -f 4 -d ' ')

# This is a pretty crude URI encode -- just replace space with
#  %20. Any other punctuation will fail. Handling punctuation is
#  left as an exercise :)

ENC_QUERY=$(echo $QUERY | sed -e s/\\s/%20/g)

# Form the query string for the radio-browser.info API call, using the
#  server name we found from 'dig', the advertised REST URL, and the
#  query term we got from the command line. Note that a limit on the number
#  of results is hard-coded in this string.

API="https://$SERVER/xml/stations/byname/$ENC_QUERY?order=votes&offset=0&limit=100&hidebroken=true"

# We'll make the API request using curl or wget, one of which needs to be
#  present

if which curl > /dev/null; then
  API_RESPONSE=$(curl --silent $API)
elif which wget > /dev/null; then
  API_RESPONSE=$(wget --quiet --output-document=- $API)
else
  echo "Error: script requires curl or wget installed."
  exit 1
fi

# Use xmllint to extract from the XML response the fields we actually want. 
# Note that stdout is redirected because we don't want ugly error messages
#  from xmllint if there are no matches

API_RESPONSE=$(echo $API_RESPONSE | xmllint --xpath result/station/@name\|result/station/@url_resolved - 2> /dev/null) 

# An empty response indicates no matches, so print an error message

if [ "$API_RESPONSE" = "" ] ; then
  echo "$0: no matches"
  exit 1
fi

# Now use a bunch of 'sed' invocations to tidy up the response into a 
#  form suitable for display

echo $API_RESPONSE | \
  sed -e s/name/@@@/ | \
  sed -e s/name=/\\n\\n/g | \
  sed -e s/url_resolved=/\\n/g | \
  sed -e s/@@@=// | \
  tr -d '"' 

