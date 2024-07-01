#!/bin/bash
awslocal s3api head-bucket --bucket "laa-ccms-bucket" &>/dev/null || not_exist=true
if [ $not_exist ]
then
  awslocal s3api create-bucket --bucket laa-ccms-bucket --region eu-west-2 \
  --create-bucket-configuration LocationConstraint=eu-west-2 1>/dev/null
  echo "Bucket 'laa-ccms-bucket' created."
else
  echo "Bucket 'laa-ccms-bucket' already exists."
fi