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


@dataclass(frozen=True, eq=True)
class SimulationParameters:
    mass: float
    k: int
    gamma: int
    r0: float
    v0: float
    amplitude: int
    seed: int


@dataclass(frozen=True, eq=True)
class Instant:
    t: float
    r: float
    v: float
    a: float


@dataclass
class Output:
    params: SimulationParameters
    values: list[Instant]


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


def calculate_oscilator(simulation_output: Output):
    params = simulation_output.params

    gamma_over_2m = params.gamma / (2 * params.mass)
    k_over_m = params.k / params.mass
    gamma_square_over_4m2 = (params.gamma * params.gamma) / (
        4 * params.mass * params.mass
    )
    cos_constant = np.sqrt(k_over_m - gamma_square_over_4m2)

    def r_t(t: float):
        return (
            params.amplitude * np.exp(t * gamma_over_2m * -1) * np.cos(t * cos_constant)
        )

    return [
        Instant(t=out.t, r=r_t(out.t), v=0.0, a=0.0) for out in simulation_output.values
    ]


def plot_algorithms(outputs: dict[str, Output], output_dir: str, zoom: bool = False):
    # df_plot = pd.DataFrame(simulation_output.values)

    # oscilator = calculate_oscilator(simulation_output)
    # df_plot_oscilator = pd.DataFrame(oscilator)
    # print(df_plot_oscilator)

    # plt.figure(figsize=FIGSIZE)

    # ax = sns.lineplot(data=df_plot, x="t", y="r", markers=True, label="Estimado")
    # sns.lineplot(
    #     data=df_plot_oscilator, x="t", y="r", ax=ax, label="Solución Analítica"
    # )

    # plt.xlabel("Tiempo (s)")
    # plt.ylabel("Posición (m)")
    # plt.grid(True)
    # # plt.show()
    # plt.savefig(f"./{output_dir}/verlet.png")
    # plt.clf()
    # plt.close()

    plt.figure(figsize=FIGSIZE)

    for label, out in outputs.items():
        df = pd.DataFrame(out.values)
        sns.lineplot(data=df, x="t", y="r", label=label)

    # analytic curve – extracted from the first dataset (all share params)
    analytic_vals = calculate_oscilator(next(iter(outputs.values())))
    df_analytic = pd.DataFrame(analytic_vals)
    sns.lineplot(
        data=df_analytic,
        x="t",
        y="r",
        label="Solución Analítica",
        color="black",
        linestyle="--",
    )

    plt.xlabel("Tiempo (s)")
    plt.ylabel("Posición (m)")
    plt.grid(True)

    if zoom:
        # Zoom into the last 10 % of the simulated time to highlight divergence
        t_min, t_max = df_analytic["t"].min(), df_analytic["t"].max()
        span = t_max - t_min
        left, right = t_max - 0.05 * span, t_max
        plt.xlim(left, right)

        mask = (df_analytic["t"] >= left) & (df_analytic["t"] <= right)
        y_min, y_max = (
            df_analytic.loc[mask, "r"].min() - 0.01,
            df_analytic.loc[mask, "r"].max() + 0.01,
        )
        padding = 0.05 * abs(y_max - y_min)
        plt.ylim(y_min - padding, y_max + padding)

        filename = os.path.join(output_dir, "algorithms_zoom.png")
    else:
        filename = os.path.join(output_dir, "algorithms.png")

    plt.legend()
    plt.tight_layout()
    plt.savefig(filename, dpi=DPI)
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
    df = df.sort_values("time")
    values = [
        Instant(t=row.time, r=row.r, v=row.v, a=row.a)
        for row in df.itertuples(index=False)
    ]

    return Output(params=config, values=values)


def main(
    euler_path: str | None,
    verlet_path: str | None,
    beeman_path: str | None,
    gpc_path: str | None,
):
    input_dir = "./output"
    output_base_dir = "./graphics"
    os.makedirs(output_base_dir, exist_ok=True)

    outputs: dict[str, Output] = {}
    if euler_path:
        outputs["Euler"] = read_csv(
            euler_path
            if os.path.isabs(euler_path)
            else os.path.join(input_dir, euler_path)
        )
    if verlet_path:
        outputs["Verlet"] = read_csv(
            verlet_path
            if os.path.isabs(verlet_path)
            else os.path.join(input_dir, verlet_path)
        )
    if beeman_path:
        outputs["Beeman"] = read_csv(
            beeman_path
            if os.path.isabs(beeman_path)
            else os.path.join(input_dir, beeman_path)
        )
    if gpc_path:
        outputs["GPC"] = read_csv(
            gpc_path if os.path.isabs(gpc_path) else os.path.join(input_dir, gpc_path)
        )

    if not outputs:
        raise ValueError("Debes proporcionar al menos un archivo de algoritmo.")

    plot_algorithms(outputs, output_base_dir, zoom=False)
    plot_algorithms(outputs, output_base_dir, zoom=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Parse Kotlin output file and generate animations and plots."
    )
    parser.add_argument("--euler", type=str, help="CSV de la integración de Euler")
    parser.add_argument("--verlet", type=str, help="CSV de la integración de Verlet")
    parser.add_argument("--beeman", type=str, help="CSV de la integración de Beeman")
    parser.add_argument("--gpc", type=str, help="CSV de Gear Predictor-Corrector")

    args = parser.parse_args()
    main(args.euler, args.verlet, args.beeman, args.gpc)
