import argparse
import os
from dataclasses import dataclass

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns

DT_FIXED = 0.1

# ------------------------------

CUSTOM_PALETTE = [
    "#508fbe",  # blue
    "#f37120",  # orange
    "#4baf4e",  # green
    "#f2cb31",  # yellow
    "#c178ce",  # purple
    "#cd4745",  # red
    "#9ef231",  # light green
    "#50beaa",  # green + blue
    "#8050be",  # violet
    "#cf1f51",  # magenta
]
GREY = "#6f6f6f"
LIGHT_GREY = "#bfbfbf"

PLT_THEME = {
    "axes.prop_cycle": plt.cycler(color=CUSTOM_PALETTE),  # Set palette
    "axes.spines.top": False,  # Remove spine (frame)
    "axes.spines.right": False,
    "axes.spines.left": True,
    "axes.spines.bottom": True,
    "axes.edgecolor": LIGHT_GREY,
    "axes.titleweight": "normal",  # Optional: ensure title weight is normal (not bold)
    "axes.titlelocation": "center",  # Center the title by default
    "axes.titlecolor": GREY,  # Set title color
    "axes.labelcolor": GREY,  # Set labels color
    "axes.labelpad": 12,
    "axes.titlesize": 10,
    "xtick.bottom": False,  # Remove ticks on the X axis
    "ytick.labelcolor": GREY,  # Set Y ticks color
    "ytick.color": GREY,  # Set Y label color
    "savefig.dpi": 128,
    "legend.frameon": False,
    "legend.labelcolor": GREY,
    "figure.titlesize": 16,  # Set suptitle size
}
plt.style.use(PLT_THEME)
sns.set_palette(CUSTOM_PALETTE)
sns.set_style(PLT_THEME)

DPI = 100
FIGSIZE = (1920 / DPI, 1080 / DPI)


# ------------------------------


@dataclass
class SimulationParameters:
    mass: float
    k: int
    gamma: int
    r0: float
    v0: float
    amplitude: int
    seed: int


def format_power_of_10(x):
    if x == 0:
        return "0"

    # Get the base and exponent
    base, exp = f"{x:.2e}".split("e")
    base = float(base)
    exp = int(exp)

    # Check if the decimal part is zero (e.g. 1.00 → 1)
    if round(base % 1, 2) == 0:
        base_str = f"{int(base)}"
    else:
        base_str = f"{base:.2f}"

    return f"{base_str}x10^{exp}"


def y_fmt(x, pos):
    """Format number as power of 10"""
    return format_power_of_10(x)


def plot_pressure(pressure_df: pd.DataFrame, output_dir: str):
    plot_df = pressure_df.reset_index().rename(
        columns={"pressure_container": "container", "pressure_obstacle": "obstacle"}
    )
    unified_plot_df = pressure_df.reset_index().melt(  # index to column
        id_vars="time",
        value_vars=["pressure_container", "pressure_obstacle"],
        var_name="boundary",
        value_name="pressure_Pa",
    )

    # Unified plot
    plt.figure(figsize=FIGSIZE)
    sns.lineplot(
        data=unified_plot_df,
        x="time",
        y="pressure_Pa",
        hue="boundary",
        style="boundary",
    )
    plt.xlabel("Tiempo (t)", fontsize=14)
    plt.ylabel("Presión (Pa)", fontsize=14)
    plt.grid(True)
    # plt.show()
    plt.savefig(f"./{output_dir}/pressure.png")
    plt.clf()
    plt.close()

    # Container only
    plt.figure(figsize=FIGSIZE)
    sns.lineplot(data=plot_df, x="time", y="container")
    plt.xlabel("Tiempo (t)", fontsize=14)
    plt.ylabel("Presión (N/m²)", fontsize=14)
    plt.grid(True)
    # plt.show()
    plt.savefig(f"./{output_dir}/pressure_container.png")
    plt.clf()
    plt.close()

    # Obstacle only
    plt.figure(figsize=FIGSIZE)
    sns.lineplot(data=plot_df, x="time", y="obstacle")
    plt.xlabel("Tiempo (t)", fontsize=14)
    plt.ylabel("Presión (N/m²)", fontsize=14)
    plt.grid(True)
    # plt.show()
    plt.savefig(f"./{output_dir}/pressure_obstacle.png")
    plt.clf()
    plt.close()


def plot_verlet(
    simulation_params: SimulationParameters,
    df: pd.DataFrame,
    output_dir: str,
):
    plt.figure(figsize=FIGSIZE)
    sns.lineplot(df.index, df["r"], markers=True)
    plt.xlabel("Tiempo (s)")
    plt.ylabel("Posición (m)")
    plt.grid(True)
    # plt.show()
    plt.savefig(f"./{output_dir}/verlet.png")
    plt.clf()
    plt.close()


def read_csv(filepath: str):
    config_df = pd.read_csv(filepath, nrows=1, header=0, keep_default_na=False)
    config = SimulationParameters(
        mass=float(config_df["m"][0]),
        k=int(config_df["k"][0]),
        gamma=int(config_df["y"][0]),
        r0=float(config_df["r0"][0]),
        v0=float(config_df["v0"][0]),
        amplitude=float(config_df["A"][0]),
        seed=int(config_df["seed"][0]),
    )

    df = pd.read_csv(
        filepath,
        sep=",",  # separator (default is comma)
        header=0,  # use first row as header
        index_col=None,  # don't use any column as index
        skiprows=2,  # number of rows to skip
    )
    df.set_index("time", inplace=True)

    return config, df


def main(output_file: str):
    input_dir = "./output"
    output_base_dir = "./graphics"
    os.makedirs(output_base_dir, exist_ok=True)

    simulation_params, df = read_csv(f"{input_dir}/{output_file}")
    print(df)

    plot_verlet(simulation_params, df, output_base_dir)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Parse Kotlin output file and generate animations and plots."
    )
    parser.add_argument(
        "-f", "--output_file", type=str, required=True, help="Output file to animate"
    )

    # args = parser.parse_args()

    main(
        # output_file=args.output_file,
        output_file="mass-70_0_k-10000_y-100_t-5_0_v0--0_7142857142857143_r0-1_0_seed-1743645648280.csv",
    )
