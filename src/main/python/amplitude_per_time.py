import os
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import argparse

PARTICLE_RADIUS = 0.0005
BOARD_LEN = 1
NUMBER_OF_PARTICLES = 1000
PLOTS_DIR = "./graphics"

"""
This function should plot the amplitude of the system over time

"""
def plot_amplitudes(df: pd.DataFrame):
    df.set_index("time", inplace=True)

    times = df.index.unique()

    amplitudes = []
    max_amplitude = -np.inf

    for t in times:
        time_data = df.loc[t]

        r_max = time_data['r'].max()
        r_min = time_data['r'].min()
        amplitude = r_max - r_min
        amplitudes.append(amplitude)

        if amplitude > max_amplitude:
            max_amplitude = amplitude

    plt.figure(figsize=(10, 6))
    plt.plot(times, amplitudes, 'b-', label='System amplitude')
    plt.xlabel('Time')
    plt.ylabel('Amplitude')
    plt.title('System amplitude along the time')
    plt.grid(True)
    plt.legend()
    os.makedirs(PLOTS_DIR, exist_ok=True)
    plt.savefig(f"{PLOTS_DIR}/max_amplitudes_per_time.png")
    plt.close()

    print(f"Max amplitude registry: {max_amplitude:.6f}")

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
        sep=",",
        header=0,  # use first row as header
        index_col=None,  # don't use any column as index
        skiprows=2,
    )  # number of rows to skip

    print(df)

    # Plot the amplitudes
    plot_amplitudes(df)