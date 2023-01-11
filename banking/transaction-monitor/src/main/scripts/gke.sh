#!/bin/bash

echo ============================================================
echo Attempts to do all steps for Google Cloud
echo ============================================================

ls kubernetes*yaml | grep -v kubernetes-5-optional-hazelcast.yaml | while read -r ALINE
do
 echo $ALINE
done

