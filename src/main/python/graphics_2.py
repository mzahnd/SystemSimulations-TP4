import os
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import re
import scipy.optimize
import matplotlib.ticker as mticker

PARTICLE_RADIUS = 0.0005
BOARD_LEN = 1
NUMBER_OF_PARTICLES = 1000
PLOTS_DIR = "./graphics"
OUTPUT_DIR = "./output"

def extract_w(filename: str) -> float:
    match = re.search(r"w-(\d+_\d+)", filename)
    if match:
        return float(match.group(1).replace("_", "."))
    raise ValueError(f"Could not extract 'w' from filename: {filename}")

def extract_k(filename: str) -> float:
    match = re.search(r"k-(\d+_\d+)", filename)
    if match:
        return float(match.group(1).replace("_", "."))
    raise ValueError(f"Could not extract 'w' from filename: {filename}")

def compute_amplitudes(df: pd.DataFrame) -> pd.Series:
    df.set_index("time", inplace=True)
    amplitudes = []

    for t in df.index.unique():
        time_data = df.loc[t]
        y_max = time_data['y'].max()
        y_min = time_data['y'].min()
        amplitude = (y_max - y_min) / 2
        amplitudes.append((t, amplitude))

    return pd.Series(dict(amplitudes)).sort_index()

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
    plt.plot(times, amplitudes, 'b-', label='System Amplitude')
    plt.xlabel('Time')
    plt.ylabel('Amplitude')
    plt.title('System Amplitude Over Time')
    plt.grid(True)
    plt.legend()
    os.makedirs(PLOTS_DIR, exist_ok=True)
    plt.savefig(f"{PLOTS_DIR}/amplitudes_max_over_time.png")
    plt.close()

    print(f"Maximum amplitude recorded: {max_amplitude:.6f}")

def plot_amplitudes_comparison():
    plt.figure(figsize=(12, 7))

    for file in os.listdir(OUTPUT_DIR):
        if file.endswith(".csv") and "Beeman" in file:
            try:
                w_value = extract_w(file)
                df = pd.read_csv(
                    os.path.join(OUTPUT_DIR, file),
                    sep=",",
                    header=0,
                    skiprows=2,
                    low_memory=False
                )

                # Rename column if needed
                if 'y' not in df.columns and 'r' in df.columns:
                    df.rename(columns={'r': 'y'}, inplace=True)

                amplitudes = compute_amplitudes(df)
                plt.plot(amplitudes.index, amplitudes.values, label=f"w = {w_value}")

            except Exception as e:
                print(f"Error processing {file}: {e}")

    plt.xlabel("Time [s]")
    plt.ylabel("Amplitude [m]")
    plt.grid(True)
    plt.legend()
    os.makedirs(PLOTS_DIR, exist_ok=True)
    plt.savefig(f"{PLOTS_DIR}/amplitudes_comparison_w.png")
    plt.close()

def plot_steady_amplitude_vs_w(folder: str):
    amplitudes_by_w = {}

    for file in os.listdir(folder):
        if file.endswith(".csv") and "w-" in file:
            try:
                w = extract_w(file)
                df = pd.read_csv(
                    os.path.join(folder, file),
                    sep=",",
                    header=0,
                    skiprows=2,
                    low_memory=False
                )

                if 'r' not in df.columns:
                    continue

                r = df['r']
                max_r = r.max()
                min_r = r.min()
                amplitude = (max_r - min_r) / 2  # Half peak-to-peak
                amplitudes_by_w[w] = amplitude

            except Exception as e:
                print(f"Error processing {file}: {e}")

    # Sort by w
    ws = sorted(amplitudes_by_w.keys())
    amplitudes = [amplitudes_by_w[w] for w in ws]

    # Plot
    plt.figure(figsize=(10, 6))
    plt.plot(ws, amplitudes, marker='o', linestyle='-', color='green')
    plt.xlabel('w [rad/s]')
    plt.ylabel('Max amplitude [m]')
    plt.grid(True)
    os.makedirs(PLOTS_DIR, exist_ok=True)
    plt.savefig(f'{PLOTS_DIR}/steady_amplitude_vs_w.png')
    plt.show()

def plot_steady_amplitude_vs_w_and_k(folder: str):
    amplitudes_by_w_and_k = {}
    max_amplitudes = {}  # Store max amplitude and corresponding w for each k

    for file in os.listdir(folder):
        if file.endswith(".csv") and "w-" in file:
            try:
                w = extract_w(file)
                k = extract_k(file)
                df = pd.read_csv(
                    os.path.join(folder, file),
                    sep=",",
                    header=0,
                    skiprows=2,
                    low_memory=False
                )

                if 'r' not in df.columns:
                    continue

                r = df['r']
                max_r = r.max()
                min_r = r.min()
                amplitude = (max_r - min_r) / 2  # Half peak-to-peak
                
                if k not in amplitudes_by_w_and_k:
                    amplitudes_by_w_and_k[k] = {}
                amplitudes_by_w_and_k[k][w] = amplitude

                # Update max amplitude for this k if needed
                if k not in max_amplitudes or amplitude > max_amplitudes[k][1]:
                    max_amplitudes[k] = (w, amplitude)

            except Exception as e:
                print(f"Error processing {file}: {e}")

    # Sort by k and w
    ks = sorted(amplitudes_by_w_and_k.keys())
    
    # Create plot with different colors for each k
    plt.figure(figsize=(12, 7))
    
    # Set font sizes
    plt.rcParams.update({
        'font.size': 20,
        'axes.titlesize': 22,
        'axes.labelsize': 22,
        'xtick.labelsize': 20,
        'ytick.labelsize': 20,
        'legend.fontsize': 20
    })

    # Use a colormap for different k values
    colors = plt.cm.viridis(np.linspace(0, 1, len(ks)))
    
    yticks = []
    for k, color in zip(ks, colors):
        ws = sorted(amplitudes_by_w_and_k[k].keys())
        amplitudes = [amplitudes_by_w_and_k[k][w] for w in ws]
        w0s = np.array([max_amplitudes[k][0] for k in ks])  # w values that give max amplitude
        amax = np.array([max_amplitudes[k][1] for k in ks])
        plt.plot(ws, amplitudes, linestyle='-', marker='o', color=color)
        plt.scatter(w0s, amax, color='red', s=120, marker='*', zorder=10)
        plt.xticks(ws, [f"{w:.2f}" for w in ws], rotation=45)

    # Overlay the (k, w0) points as red stars
    """
    # Compute (k, w0) points (where w0 is the frequency that gives max amplitude for each k)
    ws_list = []
    a_max_list = []
    for (w, amplitude) in max_amplitudes.values():
        ws_list.append(w)
        a_max_list.append(amplitude)

    # Overlay the (k, w0) points as red stars
    plt.scatter(ws_list, a_max_list, color='red', s=120, marker='*', label=r'$(k,\,A_{max})$ (max amplitude)', zorder=10)

    # Annotate each red star with its value in scientific notation (LaTeX, superscript)
    yticks = []
    for x, y in zip(ws_list, a_max_list):
        exponent = int(np.floor(np.log10(abs(y)))) if y != 0 else 0
        coeff = y / 10**exponent if y != 0 else 0
        label = fr'${coeff:.2f} \times 10^{{{exponent}}}$'
        yticks.append(label)

    ax = plt.gca()
    formatter = mticker.ScalarFormatter(useMathText=True)
    formatter.set_scientific(True)
    formatter.set_powerlimits((-2, 2))
    ax.yaxis.set_major_formatter(formatter)
    plt.yticks(a_max_list, yticks)
    plt.xticks(ws_list, [f"{w:.2f}" for w in ws_list], rotation=30)
    """

    plt.xlabel(r'$\omega$ [rad/s]')
    plt.ylabel(r'$A_{max}$ [m]')
    plt.grid(True)
    plt.legend(loc='upper left')
    plt.tight_layout()
    
    os.makedirs(PLOTS_DIR, exist_ok=True)
    plt.savefig(f'{PLOTS_DIR}/steady_amplitude_vs_w_and_k.png', bbox_inches='tight', dpi=300)
    plt.show()

def plot_w0_vs_k(folder: str):
    amplitudes_by_w_and_k = {}
    max_amplitudes = {}  # Store max amplitude and corresponding w for each k

    for file in os.listdir(folder):
        if file.endswith(".csv") and "w-" in file:
            try:
                w = extract_w(file)
                k = extract_k(file)
                df = pd.read_csv(
                    os.path.join(folder, file),
                    sep=",",
                    header=0,
                    skiprows=2,
                    low_memory=False
                )

                if 'r' not in df.columns:
                    continue

                r = df['r']
                max_r = r.max()
                min_r = r.min()
                amplitude = (max_r - min_r) / 2  # Half peak-to-peak
                
                if k not in amplitudes_by_w_and_k:
                    amplitudes_by_w_and_k[k] = {}
                amplitudes_by_w_and_k[k][w] = amplitude

                # Update max amplitude for this k if needed
                if k not in max_amplitudes or amplitude > max_amplitudes[k][1]:
                    max_amplitudes[k] = (w, amplitude)

            except Exception as e:
                print(f"Error processing {file}: {e}")

    # Sort by k
    ks = np.array(sorted(max_amplitudes.keys()))
    w0s = np.array([max_amplitudes[k][0] for k in ks])  # w values that give max amplitude

    # Fit to w0 = a * sqrt(k)
    def sqrt_law(k, a):
        return a * np.sqrt(k)
    popt, pcov = scipy.optimize.curve_fit(sqrt_law, ks, w0s)
    a_fit = popt[0]

    # Create plot
    plt.figure(figsize=(12, 7))
    
    # Set font sizes
    plt.rcParams.update({
        'font.size': 20,
        'axes.titlesize': 22,
        'axes.labelsize': 22,
        'xtick.labelsize': 20,
        'ytick.labelsize': 20,
        'legend.fontsize': 20
    })
    
    # Plot data points
    plt.plot(
        ks, w0s,
        marker='o', linestyle='',
        color='royalblue',
        markersize=10,
        markeredgecolor='black',
        zorder=10
    )
    # Plot the smooth fit curve
    k_dense = np.linspace(ks.min(), ks.max(), 300)
    w0s_fit_dense = sqrt_law(k_dense, a_fit)
    plt.plot(
        k_dense, w0s_fit_dense,
        color='orange',
        linewidth=4,
        linestyle='-',
        label=fr'Fit: $a\ \sqrt{{k}}$, $a$={a_fit:.3f}'
    )
    
    plt.xlabel('k [N/m]')
    plt.ylabel(r'$\omega_0$ [rad/s]')
    plt.xscale('linear')
    plt.grid(True, which='both', ls='--')
    plt.legend()
    plt.tight_layout()
    
    # Set x-ticks to the ks values
    plt.xticks(ks, [f"{k:.2f}" for k in ks], rotation=30)

    
    os.makedirs(PLOTS_DIR, exist_ok=True)
    plt.savefig(f'{PLOTS_DIR}/w0_vs_k.png', bbox_inches='tight', dpi=300)
    plt.show()

if __name__ == "__main__":
    plot_steady_amplitude_vs_w_and_k("./output")
    #plot_w0_vs_k("./output")
    #plot_amplitudes_comparison()
