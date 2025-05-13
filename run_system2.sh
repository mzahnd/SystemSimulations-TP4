#!/bin/bash

# Simulation parameters
seed=1743645648280 
output_directory=./output 
amplitude=0.01
deltaT=0.001
simulation_time=5
mass=0.00021
spring_constant=102.3
gamma=0.0003

# Clear the terminal
clear

# Build the project
gradle clean build

# Run the simulation
gradle run --no-build-cache --rerun-tasks --args="\
  coupled-oscillator \
  -m $mass \
  -k $spring_constant \
  -y $gamma \
  -A $amplitude \
  -t $simulation_time \
  -dt $deltaT \
  --output-directory $output_directory \
  -s $seed"