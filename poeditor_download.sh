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
    JSON=`curl -s -X POST https://api.poeditor.com/v2/projects/export -d api_token="$1" -d id="403165" -d language="$i" -d type="android_strings" -d filters="translated"`
    echo "Export URL of $i: $JSON"
    URL=`echo $JSON | jq -r .result.url`
    mkdir -p app/src/main/res/values-$i
    curl -s $URL --output app/src/main/res/values-$i/strings.xml
    echo "Saved to app/src/main/res/values-$i/strings.xml"
    echo ""
  fi
done

echo ""
