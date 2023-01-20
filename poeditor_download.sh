#!/bin/bash

JSON=`curl -s -X POST https://api.poeditor.com/v2/languages/list -d api_token="$1" -d id="403165"`
echo "List of languages JSON: $JSON"
echo ""

LIST_LANGUAGES=`echo $JSON | jq -r .result.languages[].code`
for i in $LIST_LANGUAGES
do
  if [ "$i" == "en" ]
  then
    echo "Skip \"en\"..."
  else
    VALUESDIRNAME=`echo $i | sed -e 's/\-\(.*\)/'-r'\U\1/'`
    JSON=`curl -s -X POST https://api.poeditor.com/v2/projects/export -d api_token="$1" -d id="403165" -d language="$i" -d type="android_strings" -d filters="translated"`
    echo "Export URL of $i: $JSON"
    URL=`echo $JSON | jq -r .result.url`
    echo "Export URL: $URL"
    mkdir -p app/src/main/res/values-$VALUESDIRNAME
    curl -s $URL --output app/src/main/res/values-$VALUESDIRNAME/strings.xml
    echo "Saved to app/src/main/res/values-$VALUESDIRNAME/strings.xml"
    echo ""
  fi
done

echo ""
