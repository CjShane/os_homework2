public class SingleThread {
    // Default grid size (can be overridden via command-line args)
    private static final int DEFAULT_SIZE = 500;

    // Fixed boundary temperatures (based on the example in the assignment)
    private static final double TOP_TEMP = 30.0;
    private static final double BOTTOM_TEMP = 75.0;
    private static final double LEFT_TEMP = 15.0;
    private static final double RIGHT_TEMP = 72.0;

    // Convergence threshold for total grid error
    private static final double ERROR_THRESHOLD = 5.0;

    public static void main(String[] args) {
        int size = DEFAULT_SIZE;
        if (args.length > 0) {
            size = Integer.parseInt(args[0]);
        }

        double[][] cur = new double[size][size];
        double[][] next = new double[size][size];
        boolean[][] isFixed = new boolean[size][size];

        initializeGrid(cur, next, isFixed);

        long startTime = System.nanoTime();
        Result result = relax(cur, next, isFixed, ERROR_THRESHOLD);
        long endTime = System.nanoTime();

        double elapsedSeconds = (endTime - startTime) / 1_000_000_000.0;

        // Required output line
        System.out.printf(
                "Type: Single thread; Size=%d; Average Grid Value=%.2f; Total Error=%.2f%n",
                size, result.average, result.totalError
        );

        // Helpful extra stats
        System.out.printf("Iterations=%d; Time=%.3f seconds%n",
                result.iterations, elapsedSeconds);
    }

    /**
     * Initialize the grid with fixed boundary temperatures and
     * initial values for interior points (0.0 by default).
     */
    private static void initializeGrid(double[][] cur, double[][] next, boolean[][] isFixed) {
        int n = cur.length;

        // Top and bottom rows
        for (int j = 0; j < n; j++) {
            cur[0][j] = TOP_TEMP;
            next[0][j] = TOP_TEMP;
            isFixed[0][j] = true;

            cur[n - 1][j] = BOTTOM_TEMP;
            next[n - 1][j] = BOTTOM_TEMP;
            isFixed[n - 1][j] = true;
        }

        // Left and right columns (excluding corners, already set above)
        for (int i = 1; i < n - 1; i++) {
            cur[i][0] = LEFT_TEMP;
            next[i][0] = LEFT_TEMP;
            isFixed[i][0] = true;

            cur[i][n - 1] = RIGHT_TEMP;
            next[i][n - 1] = RIGHT_TEMP;
            isFixed[i][n - 1] = true;
        }

        // Interior points start at 0.0 (default for double[][]),
        // and are not fixed. Nothing else to do here.
    }

    /**
     * Perform the iterative relaxation until the total error is below the threshold.
     * Returns final error, average temperature, and iteration count.
     */
    private static Result relax(double[][] cur,
                                double[][] next,
                                boolean[][] isFixed,
                                double errorThreshold) {

        int n = cur.length;
        double totalError;
        int iterations = 0;

        do {
            totalError = 0.0;

            // Update only interior, non-fixed cells
            for (int i = 1; i < n - 1; i++) {
                for (int j = 1; j < n - 1; j++) {
                    if (isFixed[i][j]) {
                        // Heaters keep their value
                        next[i][j] = cur[i][j];
                        continue;
                    }

                    double newVal = (cur[i - 1][j] + cur[i + 1][j] +
                            cur[i][j - 1] + cur[i][j + 1]) * 0.25;

                    totalError += Math.abs(newVal - cur[i][j]);
                    next[i][j] = newVal;
                }
            }

            // Swap current and next grids (no need to copy values)
            double[][] tmp = cur;
            cur = next;
            next = tmp;

            iterations++;

        } while (totalError >= errorThreshold);

        // Compute final average temperature of the grid
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                sum += cur[i][j];
            }
        }
        double avg = sum / (n * (double) n);

        return new Result(totalError, avg, iterations);
    }

    private static class Result {
        final double totalError;
        final double average;
        final int iterations;

        Result(double totalError, double average, int iterations) {
            this.totalError = totalError;
            this.average = average;
            this.iterations = iterations;
        }
    }
}
