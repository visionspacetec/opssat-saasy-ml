#!/bin/sh
echo "Running EM session tests"

mkdir -p "$1"/"$2"/subscribe
mkdir -p "$1"/"$2"/train
mkdir -p "$1"/"$2"/inference

# Train data fetch
{ ./exec_test_1_train_data_fetch.sh "$3" ; } 2> "$1"/"$2"/subscribe/results_time_train_data_fetch.txt

read -rsn1 -p"Press any key to continue";echo

# Download train data
{ ./exec_test_2_download_train_data.sh "$1" "$2" "$3" ; } 2> "$1"/"$2"/subscribe/results_time_download.txt

read -rsn1 -p"Press any key to continue";echo

# Train classifier models with no thread
{ ./exec_test_3_train_classifier_models.sh "$3" ; } 2> "$1"/"$2"/train/results_time_train_classifier_models.txt

read -rsn1 -p"Press any key to continue";echo

# Get classifier models with no thread
{ ./exec_test_5_get_classifier_models.sh "$1" "$2" "$3" ; } 2> "$1"/"$2"/train/results_time_get_classifier_models.txt

read -rsn1 -p"Press any key to continue";echo

# Train classifier models with thread
{ ./exec_test_4_train_classifier_models_thread.sh "$3" ; } 2> "$1"/"$2"/train/results_time_train_classifier_models_thread.txt

read -rsn1 -p"Press any key to continue";echo

# Get classifier models with thread
{ ./exec_test_5_get_classifier_models.sh "$1" "$2" "$3" "-thread"; } 2> "$1"/"$2"/train/results_time_get_classifier_models_thread.txt

read -rsn1 -p"Press any key to continue with the inference";echo

# Inference
{ ./exec_test_6_inference.sh "$1" "$2" "$3" ; } 2> "$1"/"$2"/inference/results_time_inference.txt
