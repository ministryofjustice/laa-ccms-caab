#!/bin/bash
awslocal s3api head-bucket --bucket "laa-ccms-documents" &>/dev/null || not_exist=true
if [ $not_exist ]
then
  awslocal s3api create-bucket --bucket laa-ccms-documents --region eu-west-2 \
  --create-bucket-configuration LocationConstraint=eu-west-2 1>/dev/null
  echo "Bucket 'laa-ccms-documents' created."
else
  echo "Bucket 'laa-ccms-documents' already exists."
fi