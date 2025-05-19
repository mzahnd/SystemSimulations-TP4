import os
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from matplotlib.animation import FuncAnimation
import matplotlib.patches as patches
import argparse
from typing import Union
import logging

plt.rcParams.update({
    'font.size': 20,
    'axes.titlesize': 22,
    'axes.labelsize': 22,
    'xtick.labelsize': 20,
    'ytick.labelsize': 20,
    'legend.fontsize': 20
})

parser = argparse.ArgumentParser(
    description="Parse Kotlin output file and generate animations and plots."
)
parser.add_argument(
    "-f", "--output_file", type=str, required=True, help="Output file to animate"
)

args = parser.parse_args()

MAX_DESIRED_FPS = 60

PARTICLE_RADIUS = 0.00005
BOARD_LEN = 1
L0 = 0.001
output_file = args.output_file

df = pd.read_csv(
    f"./output/{output_file}",
    sep=",",  # separator (default is comma)
    header=0,  # use first row as header
    index_col=None,  # don't use any column as index
    skiprows=2,
)  # number of rows to skip

# Set Time as index
df.set_index("time", inplace=True)

print(df)


# Get unique times
times = df.index.unique()

# Calculate interval to make animation last exactly 15 seconds
TOTAL_DURATION = 12  # seconds
interval = (TOTAL_DURATION * 1000) / len(times)  # convert to milliseconds

# Create figure and axis
fig, ax = plt.subplots(figsize=(12, 10))
fig.subplots_adjust(left=0.15)
ax.set_xlim(-0.1, BOARD_LEN + 0.1)
ax.set_ylim(-1.1e-2, 1.1e-2)
ax.set_xlabel("X Position [m]")
ax.set_ylabel("Y Position [m]")


# Initialize list to store circle patches
circles = []
board_circle = None
obstacle_circle = None


def init():
    # Clear any existing circles
    for circle in circles:
        circle.remove()
    circles.clear()
    return


def update(frame):
    # Get data for current time
    current_time = times[frame]
    time_data = df.loc[current_time]

    # Clear existing particle circles
    for circle in circles:
        circle.remove()
    circles.clear()

    # Create new circles for each particle
    for _, particle in time_data.iterrows():
        circle = patches.Circle(
            (particle["id"]*L0, particle["r"]),
            radius=PARTICLE_RADIUS,
            fill=True,
            color="blue",
            alpha=0.6,
        )
        ax.add_patch(circle)
        circles.append(circle)

    # Update title with current time
    ax.set_title(f"Time: {current_time:.2f}")

    return circles


# Create animation
ani = FuncAnimation(
    fig, update, frames=len(times), init_func=init, blit=False, interval=interval
)

# Save the animation
print("Saving animation...")
os.makedirs("./animations", exist_ok=True)
ani.save(
    f"./animations/{output_file}-simulation.mp4",
    writer="ffmpeg",
    fps=len(times) / TOTAL_DURATION,
    dpi=100,
    extra_args=["-crf", "26", "-preset", "veryfast"],
)  # smaller file, faster encode
print("Animation saved successfully!")

# Close the figure to free memory
plt.close(fig)
