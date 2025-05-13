#!/bin/bash

# Simulation parameters
seed=1743645648280 
output_directory=./output 
amplitude=1.0
deltaT=0.00001
simulation_time=5

# Clear the terminal
clear

# Build the project
gradle clean build

# Run the simulation
gradle run --no-build-cache --rerun-tasks --args="\
  damped-oscillator \
  -A $amplitude \
  -t $simulation_time \
  -dt $deltaT \
  --output-directory $output_directory \
  -s $seed"
