#!/bin/bash

# Simulation parameters
seed=1743645648280
output_directory=./output
amplitude=0.01
deltaT=0.001
simulation_time=15
mass=0.00021
spring_constant=102.3
gamma=0.0003

# Define the array of angular frequencies
angular_frequencies=(
  1.737 1.842 1.947
  2.053 2.158 2.263
)

# Join frequencies with commas
frequencies_str=$(IFS=,; echo "${angular_frequencies[*]}")

# Clear the terminal
clear

# Build the project
gradle clean build

# Run the simulation with all frequencies
gradle run --no-build-cache --rerun-tasks --args="\
  coupled-oscillator \
  -m $mass \
  -k $spring_constant \
  -y $gamma \
  -A $amplitude \
  -t $simulation_time \
  -dt $deltaT \
  -w $frequencies_str \
  --output-directory $output_directory \
  -s $seed \
  --sweep-k"
