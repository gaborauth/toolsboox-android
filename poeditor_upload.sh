#!/bin/bash

curl -X POST https://api.poeditor.com/v2/projects/upload \
  -F api_token="$1" \
  -F id="403165" \
  -F language="en" \
  -F overwrite="1" \
  -F updating="terms_translations" \
  -F sync_terms="1" \
  -F file=@"app/src/main/res/values/strings.xml"

echo ""
echo ""
echo ""
