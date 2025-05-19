import os
import argparse
from dataclasses import dataclass
from typing import Dict, List, Optional
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter, LogLocator
from decimal import Decimal, getcontext, localcontext
from math import factorial
import seaborn as sns
import numpy as np

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
BLACK = "#1a1a1a"
GREY = "#6f6f6f"
LIGHT_GREY = "#bfbfbf"

PLT_THEME = {
    "axes.prop_cycle": plt.cycler(color=CUSTOM_PALETTE),  # Set palette
    "axes.spines.top": False,  # Remove spine (frame)
    "axes.spines.right": False,
    "axes.spines.left": True,
    "axes.spines.bottom": True,
    "axes.edgecolor": BLACK,
    "axes.titleweight": "normal",  # Optional: ensure title weight is normal (not bold)
    "axes.titlelocation": "center",  # Center the title by default
    "axes.titlecolor": GREY,  # Set title color
    "axes.labelcolor": GREY,  # Set labels color
    "axes.labelpad": 12,
    "xtick.bottom": False,  # Remove ticks on the X axis
    "xtick.labelcolor": BLACK,  # Set Y ticks color
    "ytick.labelcolor": BLACK,  # Set Y ticks color
    "ytick.color": GREY,  # Set Y label color
    "savefig.dpi": 128,
    "legend.frameon": False,
    "legend.labelcolor": GREY,
    "figure.titlesize": 16,  # Set suptitle size
    "font.size": 20,
    "axes.titlesize": 22,
    "axes.labelsize": 22,
    "xtick.labelsize": 20,
    "ytick.labelsize": 20,
    "legend.fontsize": 20,
}
plt.style.use(PLT_THEME)
sns.set_palette(CUSTOM_PALETTE)
sns.set_style(PLT_THEME)

getcontext().prec = 40

DPI = 100
FIGSIZE = (1920 / DPI, 1080 / DPI)


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


@dataclass
class Output:
    params: SimulationParameters
    values: List[Instant]
    dt: float


def _pow10_fmt(y, _):
    """Return labels like 10^{-8} for log-scaled axis."""
    if y == 0:
        return "0"
    exp = int(np.log10(y))
    return rf"$10^{{{exp}}}$"


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


# Coseno con Decimal usando serie de Taylor
def cos_decimal(x: Decimal, terms: int = 30) -> Decimal:
    x = x % (2 * Decimal("3.141592653589793238462643383279"))
    result = Decimal(1)
    num = Decimal(1)
    denom = Decimal(1)
    sign = -1
    for n in range(1, terms):
        num *= x * x
        denom *= Decimal((2 * n - 1) * (2 * n))
        result += sign * (num / denom)
        sign *= -1
    return result


# Raíz cuadrada para Decimal
def sqrt_decimal(x: Decimal) -> Decimal:
    with localcontext() as ctx:
        ctx.prec += 10
        return x.sqrt()


def read_csv(filepath: str) -> Output:
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

    print(filepath)

    df = pd.read_csv(filepath, sep=",", header=0, index_col=None, skiprows=2)
    df = df.sort_values("time")

    # Calcular dt como la diferencia promedio entre tiempos
    times = df["time"].values
    if len(times) > 1:
        dt = round(np.mean(np.diff(times)), 8)
    else:
        dt = 0.0

    values = [
        Instant(t=row.time, r=row.r, v=row.v) for row in df.itertuples(index=False)
    ]

    return Output(params=config, values=values, dt=dt)


def calculate_oscilator(simulation_output: Output):
    params = simulation_output.params

    gamma_over_2m = Decimal(str(params.gamma)) / (2 * Decimal(str(params.mass)))
    k_over_m = Decimal(str(params.k)) / Decimal(str(params.mass))
    gamma_square_over_4m2 = (Decimal(str(params.gamma)) ** 2) / (
        4 * Decimal(str(params.mass)) ** 2
    )
    cos_constant = sqrt_decimal(k_over_m - gamma_square_over_4m2)
    amplitude = Decimal(str(params.amplitude))

    def r_t(t: float):
        t_dec = Decimal(str(t))
        exp_part = (-gamma_over_2m * t_dec).exp()
        cos_arg = cos_constant * t_dec
        cos_part = cos_decimal(cos_arg)
        return amplitude * exp_part * cos_part

    return [
        Instant(t=out.t, r=float(r_t(out.t)), v=0.0) for out in simulation_output.values
    ]


def calculate_mse(output: Output) -> float:
    analytic_vals = calculate_oscilator(output)
    df_analytic = pd.DataFrame(analytic_vals)
    df = pd.DataFrame(output.values)

    df_merged = pd.merge(df, df_analytic, on="t", suffixes=("", "_analytic"))
    mse = ((df_merged["r"] - df_merged["r_analytic"]) ** 2).mean()
    print(f"MSE of {output.dt} is {mse:.30e}")
    return mse


def plot_mse_by_dt(outputs_by_method: Dict[str, List[Output]], output_dir: str):
    Y_MIN_EXP = -35  # lower exponent
    Y_MAX_EXP = -2  # upper exponent
    Y_MIN = 10**Y_MIN_EXP
    Y_MAX = 10**Y_MAX_EXP
    plt.figure(figsize=FIGSIZE)

    # ── reshape data ────────────────────────────────────────────────
    mse_data = []
    for method, outputs in outputs_by_method.items():
        for output in outputs:
            print(f"Calculate MSE of {method}")
            mse_data.append(
                {
                    "Method": method,
                    "dt": output.dt,
                    "MSE": calculate_mse(output),
                }
            )

    df = pd.DataFrame(mse_data).sort_values("dt")
    print(df)

    # ── main line plot ──────────────────────────────────────────────
    ax = sns.lineplot(
        data=df,
        x="dt",
        y="MSE",
        hue="Method",
        marker="o",
        style="Method",
        markersize=8,
        linewidth=2,
    )

    # ── Y axis: logarithmic and nicely formatted ───────────────────
    ax.set_yscale("log")
    ax.set_ylim(Y_MIN, Y_MAX)
    ax.yaxis.set_major_locator(LogLocator(base=10))
    ax.yaxis.set_major_formatter(FuncFormatter(_pow10_fmt))

    # ── X axis: show every dt value ────────────────────────────────
    # ── X axis: logarithmic and nicely formatted ───────────────────
    ax.set_xscale("log")
    x_ticks = sorted(df["dt"].unique())
    ax.set_xticks(x_ticks)
    ax.get_xaxis().set_major_formatter(FuncFormatter(_pow10_fmt))

    # ── labels, grid, legend ───────────────────────────────────────
    plt.xlabel("Time step (dt) [s]")
    plt.ylabel("Mean Squared Error (MSE)")
    plt.grid(True, which="both", linestyle="--", linewidth=0.5, alpha=0.6)
    plt.legend(title="Method")
    plt.tight_layout()

    # ── save figure ────────────────────────────────────────────────
    output_path = os.path.join(output_dir, "mse_vs_dt.png")
    plt.savefig(output_path, dpi=DPI)
    plt.clf()
    plt.close()

    print(f"Gráfico de MSE vs dt guardado en: {output_path}")


def plot_mse_by_dt_2(outputs_by_method: Dict[str, List[Output]], output_dir: str):
    plt.figure(figsize=FIGSIZE)

    mse_data = []
    for method, outputs in outputs_by_method.items():
        for output in outputs:
            mse = calculate_mse(output)
            mse_data.append({"Method": method, "dt": output.dt, "MSE": mse})

    df = pd.DataFrame(mse_data)
    df = df.sort_values("dt")
    print(df)

    sns.lineplot(
        data=df,
        x="dt",
        y="MSE",
        hue="Method",
        marker="o",
        style="Method",
        markersize=8,
        linewidth=2,
    )

    plt.xlabel("Time step (dt) [s]")
    plt.ylabel("Mean Squared Error (MSE)")
    plt.grid(True)

    unique_dts = sorted(df["dt"].unique())
    plt.xticks(unique_dts)

    plt.legend(title="Method")
    plt.tight_layout()

    output_path = os.path.join(output_dir, "mse_vs_dt.png")
    plt.savefig(output_path, dpi=DPI)
    plt.clf()
    plt.close()

    print(f"Gráfico de MSE vs dt guardado en: {output_path}")


def main(
    euler_paths: Optional[List[str]],
    verlet_paths: Optional[List[str]],
    beeman_paths: Optional[List[str]],
    gpc_paths: Optional[List[str]],
):
    input_dir = "./output"
    output_base_dir = "./graphics"
    os.makedirs(output_base_dir, exist_ok=True)

    outputs_by_method: Dict[str, List[Output]] = {}

    def process_paths(method: str, paths: Optional[List[str]]):
        if paths:
            outputs_by_method[method] = [
                read_csv(path if os.path.isabs(path) else os.path.join(input_dir, path))
                for path in paths
            ]

    # process_paths("Euler", euler_paths)  # Do not print Euler
    process_paths("Verlet", verlet_paths)
    process_paths("Beeman", beeman_paths)
    process_paths("GPC", gpc_paths)

    if not outputs_by_method:
        raise ValueError("Debes proporcionar al menos un archivo de algoritmo.")

    plot_mse_by_dt(outputs_by_method, output_base_dir)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Comparar múltiples simulaciones y graficar MSE en función de dt."
    )
    parser.add_argument("--euler", type=str, nargs="+", help="CSVs de Euler")
    parser.add_argument("--verlet", type=str, nargs="+", help="CSVs de Verlet")
    parser.add_argument("--beeman", type=str, nargs="+", help="CSVs de Beeman")
    parser.add_argument(
        "--gpc", type=str, nargs="+", help="CSVs de Gear Predictor-Corrector"
    )

    args = parser.parse_args()
    main(args.euler, args.verlet, args.beeman, args.gpc)
