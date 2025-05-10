import os
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from matplotlib.animation import FuncAnimation
import matplotlib.patches as patches
import argparse
from typing import Union
import logging

PARTICLE_RADIUS = 0.0005
BOARD_LEN = 1
NUMBER_OF_PARTICLES = 1000
PLOTS_DIR = "./graphics"



"""
This function should plot the amplitude of the system over time

"""
def plot_amplitudes(df: pd.DataFrame):
    # Set Time as index
    df.set_index("time", inplace=True)

    # Get unique times
    times = df.index.unique()

    amplitudes = []
    for t in times:
        time_data = df.loc[t]
        
        # Calculate max and min y positions
        y_max = time_data['y'].max()
        y_min = time_data['y'].min()
        
        # Calculate amplitude and adjust it
        amplitude = (y_max - y_min) / 2
        amplitudes.append(amplitude)
    
    # Create the plot
    plt.figure(figsize=(10, 6))
    plt.plot(times, amplitudes, 'b-', label='System Amplitude')
    plt.xlabel('Time')
    plt.ylabel('Amplitude')
    plt.title('System Amplitude Over Time')
    plt.grid(True)
    plt.legend()
    os.makedirs(PLOTS_DIR, exist_ok=True)
    plt.savefig(f"{PLOTS_DIR}/amplitudes_2.png")
    plt.close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Parse Kotlin output file and generate animations and plots."
    )
    parser.add_argument(
        "-f", "--output_file", type=str, required=True, help="Output file to animate"
    )

    args = parser.parse_args()

    output_file = args.output_file


    df = pd.read_csv(
        f"./output/{output_file}",
        sep=",",  # separator (default is comma)
        header=0,  # use first row as header
        index_col=None,  # don't use any column as index
        skiprows=0,
    )  # number of rows to skip

    print(df)

    # Plot the amplitudes
    plot_amplitudes(df)