/**
 * WorkerThread is responsible for processing a contiguous range of rows in the
 * shared {@link Matrix} for the unsynchronized multi-threaded implementation.
 * <p>
 * Each worker repeatedly:
 * <ul>
 *   <li>Copies its portion of the grid into a local buffer,</li>
 *   <li>Recomputes the new temperatures for the plates in its row range using
 *       the {@code Matrix.computerAvg(int, int)} method,</li>
 *   <li>Accumulates a local error for its portion of the grid.</li>
 * </ul>
 * The thread continues iterating until it is interrupted. The expectation is
 * that some controlling code will monitor the per-thread errors and, once the
 * overall grid error is sufficiently small, will interrupt the entire thread
 * group using {@code Thread.currentThread().getThreadGroup().interrupt()}.
 */
public class WorkerThread extends Thread {

    /** Shared matrix that all worker threads update. */
    private final Matrix matrix;

    /** First row (inclusive) this worker is responsible for. */
    private final int startRow;

    /** One past the last row (exclusive) this worker is responsible for. */
    private final int endRow;

    /** The last computed local error for this worker's row range. */
    private double lastError;

    /** Number of iterations this worker has completed. */
    private int iterations;

    /**
     * Creates a new worker responsible for a contiguous subset of rows.
     *
     * @param matrix   the shared Matrix to read from and write to
     * @param startRow the first row index (inclusive) this worker will update
     * @param endRow   one past the last row index (exclusive) this worker will update
     */
    public WorkerThread(Matrix matrix, int startRow, int endRow) {
        this.matrix = matrix;
        this.startRow = startRow;
        this.endRow = endRow;
        this.lastError = 0.0;
        this.iterations = 0;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                lastError = performIteration();
                iterations++;
            }
        } catch (Exception e) {
            // For this homework, we simply exit the thread on any exception.
            // In production code, you would likely log or propagate this.
        }
    }

    /**
     * Performs a single iteration over this worker's row range.
     * <p>
     * For each interior plate in the assigned rows, this method computes a new
     * temperature using the four-neighbor average and updates the shared
     * matrix. A local error for this worker's region is computed as the sum of
     * absolute differences between the previous and new values.
     *
     * @return the local error accumulated over this worker's row range
     */
    private double performIteration() {
        int rowCount = matrix.getRowCount();
        int colCount = matrix.getColCount();

        // Local buffer to hold the previous values for this worker's rows.
        double[][] previousLocal = new double[endRow - startRow][colCount];

        // Copy current values into the local buffer.
        for (int i = startRow; i < endRow; i++) {
            for (int j = 0; j < colCount; j++) {
                previousLocal[i - startRow][j] = matrix.getValue(i, j);
            }
        }

        double error = 0.0;

        // Update only interior cells; boundary cells are treated as fixed.
        for (int i = startRow; i < endRow; i++) {
            for (int j = 0; j < colCount; j++) {
                if (i <= 0 || i >= rowCount - 1 || j <= 0 || j >= colCount - 1) {
                    // Skip boundaries (fixed heat sources).
                    continue;
                }

                double newValue = matrix.computerAvg(i, j);
                double oldValue = previousLocal[i - startRow][j];

                matrix.setValue(i, j, newValue);
                error += Math.abs(newValue - oldValue);
            }
        }

        return error;
    }

    /**
     * Returns the last computed local error for this worker's row range.
     * This can be used by a controller to approximate the overall grid error
     * by summing the errors from all workers.
     *
     * @return the last local error value
     */
    public double getLastError() {
        return lastError;
    }

    /**
     * Returns the number of iterations this worker has completed.
     *
     * @return the iteration count
     */
    public int getIterations() {
        return iterations;
    }
}