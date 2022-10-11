#!/bin/sh
echo "Running EM session tests"

mkdir -p "$1"/"$2"/subscribe
mkdir -p "$1"/"$2"/train

# Train data fetch
{ ./exec_test_train_data_fetch.sh "$3" ; } 2> "$1"/"$2"/subscribe/results_time_train_data_fetch.txt

sleep 10

# Download train data
{ ./exec_test_download_train_data.sh "$1" "$2" "$3" ; } 2> "$1"/"$2"/subscribe/results_time_download.txt

sleep 10

# Train classifier models with no thread
{ ./exec_test_train_classifier_models.sh "$3" ; } 2> "$1"/"$2"/train/results_time_train_classifier_models.txt

sleep 10

# Get classifier models with no thread
{ ./exec_test_get_classifier_models.sh "$1" "$2" "$3" ; } 2> "$1"/"$2"/train/results_time_get_classifier_models.txt

sleep 10

# Train classifier models with thread
{ ./exec_test_train_classifier_models_thread.sh "$3" ; } 2> "$1"/"$2"/train/results_time_train_classifier_models_thread.txt

sleep 10

# Get classifier models with thread
{ ./exec_test_get_classifier_models.sh "$1" "$2" "$3" ; } 2> "$1"/"$2"/train/results_time_get_classifier_models_thread.txt
