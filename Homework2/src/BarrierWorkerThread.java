import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * BarrierWorkerThread is responsible for processing a contiguous range of rows in the
 * shared {@link Matrix} with synchronized iterations using a {@link CyclicBarrier}.
 * <p>
 * Unlike {@link WorkerThread}, this implementation ensures that all worker threads
 * complete each iteration before any thread proceeds to the next iteration. This
 * guarantees that all parts of the grid are equally updated at each step, preventing
 * some regions from being more accurate than others.
 * <p>
 * Each worker repeatedly:
 * <ul>
 *   <li>Copies its portion of the grid into a local buffer,</li>
 *   <li>Recomputes the new temperatures for the plates in its row range using
 *       the {@code Matrix.computerAvg(int, int)} method,</li>
 *   <li>Accumulates a local error for its portion of the grid,</li>
 *   <li>Waits at the barrier for all other threads to complete their iteration.</li>
 * </ul>
 */
public class BarrierWorkerThread extends Thread {

    /** Shared matrix that all worker threads update. */
    private final Matrix matrix;

    /** First row (inclusive) this worker is responsible for. */
    private final int startRow;

    /** One past the last row (exclusive) this worker is responsible for. */
    private final int endRow;

    /** Barrier to synchronize all worker threads at the end of each iteration. */
    private final CyclicBarrier barrier;

    /** The last computed local error for this worker's row range. */
    private volatile double lastError;

    /** Number of iterations this worker has completed. */
    private int iterations;

    /** Flag to signal the thread to stop. */
    private volatile boolean shouldStop;

    /**
     * Creates a new barrier-synchronized worker responsible for a contiguous subset of rows.
     *
     * @param matrix   the shared Matrix to read from and write to
     * @param startRow the first row index (inclusive) this worker will update
     * @param endRow   one past the last row index (exclusive) this worker will update
     * @param barrier  the CyclicBarrier used to synchronize all workers
     */
    public BarrierWorkerThread(Matrix matrix, int startRow, int endRow, CyclicBarrier barrier) {
        this.matrix = matrix;
        this.startRow = startRow;
        this.endRow = endRow;
        this.barrier = barrier;
        this.lastError = 0.0;
        this.iterations = 0;
        this.shouldStop = false;
    }

    @Override
    public void run() {
        try {
            while (!shouldStop && !Thread.currentThread().isInterrupted()) {
                lastError = performIteration();
                iterations++;

                // Wait for all threads to complete this iteration
                // Check shouldStop before waiting to avoid deadlock
                if (!shouldStop) {
                    barrier.await();
                } else {
                    break;
                }
            }
        } catch (InterruptedException e) {
            // Thread was interrupted, exit gracefully
            Thread.currentThread().interrupt();
        } catch (BrokenBarrierException e) {
            // Barrier was broken, likely due to another thread being interrupted
            // Exit gracefully
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
     * Signals this worker thread to stop after completing its current iteration.
     */
    public void stopWorker() {
        shouldStop = true;
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
