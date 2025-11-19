import java.util.concurrent.CyclicBarrier;

public class Main {

    // You can adjust this for testing or final large runs
    private static final int GRID_SIZE = 500;
    private static final double ERROR_THRESHOLD = 5.0;

    public static void main(String[] args) throws InterruptedException {

        // Check command-line arguments for execution mode
        if (args.length > 0) {
            String mode = args[0].toLowerCase();
            switch (mode) {
                case "single":
                    runSingleThread();
                    return;
                case "barrier":
                    runMultiThreadBarrier();
                    return;
                case "test":
                    runPerformanceTests();
                    return;
                default:
                    System.out.println("Unknown mode: " + mode);
                    System.out.println("Usage: java Main [single|barrier|test]");
                    System.out.println("  (no args): Run multi-thread without barrier");
                    System.out.println("  single:    Run single-threaded version");
                    System.out.println("  barrier:   Run multi-thread with barrier synchronization");
                    System.out.println("  test:      Run comprehensive performance tests");
                    return;
            }
        }

        runMultiThread();
    }

    /**
     * Run the single-threaded version of the heat distribution solver.
     */
    private static void runSingleThread() {
        // 1. Initialize the grid
        double[][] cur = new double[GRID_SIZE][GRID_SIZE];
        double[][] next = new double[GRID_SIZE][GRID_SIZE];

        // Set boundary heat source values
        initializeGrid(cur);
        initializeGrid(next);

        long startTime = System.currentTimeMillis();

        int iterations = 0;
        double totalError;

        // 2. Perform iterative relaxation
        do {
            totalError = 0.0;

            // Update only interior cells
            for (int i = 1; i < GRID_SIZE - 1; i++) {
                for (int j = 1; j < GRID_SIZE - 1; j++) {
                    double newVal = (cur[i - 1][j] + cur[i + 1][j] +
                                   cur[i][j - 1] + cur[i][j + 1]) * 0.25;

                    totalError += Math.abs(newVal - cur[i][j]);
                    next[i][j] = newVal;
                }
            }

            // Swap current and next grids
            double[][] tmp = cur;
            cur = next;
            next = tmp;

            iterations++;

        } while (totalError >= ERROR_THRESHOLD);

        long endTime = System.currentTimeMillis();

        // 3. Compute final average temperature
        double sum = 0.0;
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                sum += cur[i][j];
            }
        }
        double avgTemp = sum / (GRID_SIZE * GRID_SIZE);

        // 4. Output results
        System.out.println("Type: Single thread; Size=" + GRID_SIZE
                + "; Average Grid Value=" + avgTemp
                + "; Total Error=" + totalError);

        System.out.println("Execution Time: " + (endTime - startTime) + " ms");
        System.out.println("Iterations: " + iterations);
    }

    /**
     * Run the multi-threaded version of the heat distribution solver.
     */
    private static void runMultiThread() throws InterruptedException {

        // 1. Initialize the grid
        double[][] grid = new double[GRID_SIZE][GRID_SIZE];

        // Set boundary heat source values
        initializeGrid(grid);

        Matrix matrix = new Matrix(grid);

        // 2. Split the work into 4 equal row chunks
        int quarter = GRID_SIZE / 4;

        WorkerThread t1 = new WorkerThread(matrix, 0, quarter);
        WorkerThread t2 = new WorkerThread(matrix, quarter, 2 * quarter);
        WorkerThread t3 = new WorkerThread(matrix, 2 * quarter, 3 * quarter);
        WorkerThread t4 = new WorkerThread(matrix, 3 * quarter, GRID_SIZE);

        // 3. Start threads
        long startTime = System.currentTimeMillis();

        t1.start();
        t2.start();
        t3.start();
        t4.start();

        // 4. Controller loop: keep checking the combined error
        while (true) {
            double totalError = t1.getLastError()
                    + t2.getLastError()
                    + t3.getLastError()
                    + t4.getLastError();

            if (totalError < 5.0) {
                // FIX: Interrupt specific workers instead of the whole group.
                // Interrupting the group interrupts Main as well, causing join() to fail.
                t1.interrupt();
                t2.interrupt();
                t3.interrupt();
                t4.interrupt();
                break;
            }

            // Prevent 100% CPU usage
            Thread.sleep(5);
        }

        // 5. Join worker threads and wait for them to finish
        t1.join();
        t2.join();
        t3.join();
        t4.join();

        long endTime = System.currentTimeMillis();

        double finalError = t1.getLastError() + t2.getLastError() + t3.getLastError() + t4.getLastError();
        double avgTemp = matrix.computeAverageTemperature();

        // 6. Output results in the required assignment format
        System.out.println("Type: Multithread (no barrier); Size=" + GRID_SIZE
                + "; Average Grid Value=" + avgTemp
                + "; Total Error=" + finalError);

        System.out.println("Execution Time: " + (endTime - startTime) + " ms");

        System.out.println("Iterations per thread:");
        System.out.println("  Thread 1: " + t1.getIterations());
        System.out.println("  Thread 2: " + t2.getIterations());
        System.out.println("  Thread 3: " + t3.getIterations());
        System.out.println("  Thread 4: " + t4.getIterations());
    }

    /**
     * Run the multi-threaded version with barrier synchronization.
     * All threads wait at a barrier after each iteration, ensuring synchronized updates.
     */
    private static void runMultiThreadBarrier() throws InterruptedException {

        // 1. Initialize the grid
        double[][] grid = new double[GRID_SIZE][GRID_SIZE];

        // Set boundary heat source values
        initializeGrid(grid);

        Matrix matrix = new Matrix(grid);

        // 2. Split the work into 4 equal row chunks
        int quarter = GRID_SIZE / 4;

        // 3. Create a barrier for 4 threads
        CyclicBarrier barrier = new CyclicBarrier(4);

        BarrierWorkerThread t1 = new BarrierWorkerThread(matrix, 0, quarter, barrier);
        BarrierWorkerThread t2 = new BarrierWorkerThread(matrix, quarter, 2 * quarter, barrier);
        BarrierWorkerThread t3 = new BarrierWorkerThread(matrix, 2 * quarter, 3 * quarter, barrier);
        BarrierWorkerThread t4 = new BarrierWorkerThread(matrix, 3 * quarter, GRID_SIZE, barrier);

        // 4. Start threads
        long startTime = System.currentTimeMillis();

        t1.start();
        t2.start();
        t3.start();
        t4.start();

        // 5. Controller loop: keep checking the combined error
        while (true) {
            double totalError = t1.getLastError()
                    + t2.getLastError()
                    + t3.getLastError()
                    + t4.getLastError();

            if (totalError < ERROR_THRESHOLD) {
                // Signal workers to stop and interrupt them
                t1.stopWorker();
                t2.stopWorker();
                t3.stopWorker();
                t4.stopWorker();

                // Interrupt to break out of barrier.await()
                t1.interrupt();
                t2.interrupt();
                t3.interrupt();
                t4.interrupt();
                break;
            }

            // Prevent 100% CPU usage
            Thread.sleep(5);
        }

        // 6. Join worker threads and wait for them to finish
        t1.join();
        t2.join();
        t3.join();
        t4.join();

        long endTime = System.currentTimeMillis();

        double finalError = t1.getLastError() + t2.getLastError() + t3.getLastError() + t4.getLastError();
        double avgTemp = matrix.computeAverageTemperature();

        // 7. Output results in the required assignment format
        System.out.println("Type: Multithread (with barrier); Size=" + GRID_SIZE
                + "; Average Grid Value=" + avgTemp
                + "; Total Error=" + finalError);

        System.out.println("Execution Time: " + (endTime - startTime) + " ms");

        System.out.println("Iterations per thread:");
        System.out.println("  Thread 1: " + t1.getIterations());
        System.out.println("  Thread 2: " + t2.getIterations());
        System.out.println("  Thread 3: " + t3.getIterations());
        System.out.println("  Thread 4: " + t4.getIterations());
    }

    /**
     * Run comprehensive performance tests comparing different implementations.
     * This implements Step 4 of the assignment.
     */
    private static void runPerformanceTests() throws InterruptedException {
        System.out.println("=".repeat(80));
        System.out.println("PERFORMANCE COMPARISON TESTS");
        System.out.println("=".repeat(80));
        System.out.println();

        // Test different grid sizes to see where algorithm bogs down
        int[] gridSizes = {100, 250, 500, 1000};

        for (int size : gridSizes) {
            System.out.println("-".repeat(80));
            System.out.println("Testing with Grid Size: " + size);
            System.out.println("-".repeat(80));

            // Run each test multiple times and average the results
            int numRuns = 3;

            long singleThreadTotal = 0;
            long multiThreadNoBarrierTotal = 0;
            long multiThreadBarrierTotal = 0;

            int singleIterations = 0;
            int[] multiNoBarrierIterations = new int[4];
            int[] multiBarrierIterations = new int[4];

            for (int run = 0; run < numRuns; run++) {
                // Test 1: Single Thread
                long singleTime = testSingleThread(size);
                singleThreadTotal += singleTime;

                // Test 2: Multi-thread without barrier
                long multiNoBarrierTime = testMultiThreadNoBarrier(size);
                multiThreadNoBarrierTotal += multiNoBarrierTime;

                // Test 3: Multi-thread with barrier
                long multiBarrierTime = testMultiThreadBarrier(size);
                multiThreadBarrierTotal += multiBarrierTime;

                // Small delay between runs
                Thread.sleep(100);
            }

            // Calculate averages
            long avgSingle = singleThreadTotal / numRuns;
            long avgMultiNoBarrier = multiThreadNoBarrierTotal / numRuns;
            long avgMultiBarrier = multiThreadBarrierTotal / numRuns;

            System.out.println("\nResults (average of " + numRuns + " runs):");
            System.out.println("  Single Thread:              " + avgSingle + " ms");
            System.out.println("  Multi-thread (no barrier):  " + avgMultiNoBarrier + " ms");
            System.out.println("  Multi-thread (with barrier): " + avgMultiBarrier + " ms");

            // Calculate speedup
            double speedupNoBarrier = (double) avgSingle / avgMultiNoBarrier;
            double speedupBarrier = (double) avgSingle / avgMultiBarrier;

            System.out.println("\nSpeedup compared to single thread:");
            System.out.println("  No barrier:  " + String.format("%.2fx", speedupNoBarrier));
            System.out.println("  With barrier: " + String.format("%.2fx", speedupBarrier));
            System.out.println();
        }

        System.out.println("=".repeat(80));
        System.out.println("ANALYSIS");
        System.out.println("=".repeat(80));
        System.out.println("1. Grid Size Impact: Larger grids show more benefit from multi-threading");
        System.out.println("2. Barrier Synchronization: Ensures all threads progress equally");
        System.out.println("3. Accuracy: Barrier version produces more consistent results across threads");
        System.out.println("4. Performance Trade-off: Barrier adds overhead but improves correctness");
        System.out.println("=".repeat(80));
    }

    /**
     * Test single-threaded implementation with specified grid size.
     */
    private static long testSingleThread(int size) {
        double[][] cur = new double[size][size];
        double[][] next = new double[size][size];

        initializeGridWithSize(cur, size);
        initializeGridWithSize(next, size);

        long startTime = System.currentTimeMillis();

        int iterations = 0;
        double totalError;

        do {
            totalError = 0.0;

            for (int i = 1; i < size - 1; i++) {
                for (int j = 1; j < size - 1; j++) {
                    double newVal = (cur[i - 1][j] + cur[i + 1][j] +
                            cur[i][j - 1] + cur[i][j + 1]) * 0.25;

                    totalError += Math.abs(newVal - cur[i][j]);
                    next[i][j] = newVal;
                }
            }

            double[][] tmp = cur;
            cur = next;
            next = tmp;

            iterations++;

        } while (totalError >= ERROR_THRESHOLD);

        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    /**
     * Test multi-threaded implementation without barrier.
     */
    private static long testMultiThreadNoBarrier(int size) throws InterruptedException {
        double[][] grid = new double[size][size];
        initializeGridWithSize(grid, size);

        Matrix matrix = new Matrix(grid);
        int quarter = size / 4;

        WorkerThread t1 = new WorkerThread(matrix, 0, quarter);
        WorkerThread t2 = new WorkerThread(matrix, quarter, 2 * quarter);
        WorkerThread t3 = new WorkerThread(matrix, 2 * quarter, 3 * quarter);
        WorkerThread t4 = new WorkerThread(matrix, 3 * quarter, size);

        long startTime = System.currentTimeMillis();

        t1.start();
        t2.start();
        t3.start();
        t4.start();

        while (true) {
            double totalError = t1.getLastError() + t2.getLastError() +
                    t3.getLastError() + t4.getLastError();

            if (totalError < ERROR_THRESHOLD) {
                t1.interrupt();
                t2.interrupt();
                t3.interrupt();
                t4.interrupt();
                break;
            }

            Thread.sleep(5);
        }

        t1.join();
        t2.join();
        t3.join();
        t4.join();

        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    /**
     * Test multi-threaded implementation with barrier.
     */
    private static long testMultiThreadBarrier(int size) throws InterruptedException {
        double[][] grid = new double[size][size];
        initializeGridWithSize(grid, size);

        Matrix matrix = new Matrix(grid);
        int quarter = size / 4;

        CyclicBarrier barrier = new CyclicBarrier(4);

        BarrierWorkerThread t1 = new BarrierWorkerThread(matrix, 0, quarter, barrier);
        BarrierWorkerThread t2 = new BarrierWorkerThread(matrix, quarter, 2 * quarter, barrier);
        BarrierWorkerThread t3 = new BarrierWorkerThread(matrix, 2 * quarter, 3 * quarter, barrier);
        BarrierWorkerThread t4 = new BarrierWorkerThread(matrix, 3 * quarter, size, barrier);

        long startTime = System.currentTimeMillis();

        t1.start();
        t2.start();
        t3.start();
        t4.start();

        while (true) {
            double totalError = t1.getLastError() + t2.getLastError() +
                    t3.getLastError() + t4.getLastError();

            if (totalError < ERROR_THRESHOLD) {
                t1.stopWorker();
                t2.stopWorker();
                t3.stopWorker();
                t4.stopWorker();

                t1.interrupt();
                t2.interrupt();
                t3.interrupt();
                t4.interrupt();
                break;
            }

            Thread.sleep(5);
        }

        t1.join();
        t2.join();
        t3.join();
        t4.join();

        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    /**
     * Initialize a grid with a specific size.
     */
    private static void initializeGridWithSize(double[][] grid, int n) {
        // Top row fixed to 30
        for (int j = 0; j < n; j++) grid[0][j] = 30;

        // Bottom row fixed to 75
        for (int j = 0; j < n; j++) grid[n - 1][j] = 75;

        // Left column fixed to 15
        for (int i = 0; i < n; i++) grid[i][0] = 15;

        // Right column fixed to 72
        for (int i = 0; i < n; i++) grid[i][n - 1] = 72;

        // Interior cells start at 0
        for (int i = 1; i < n - 1; i++) {
            for (int j = 1; j < n - 1; j++) {
                grid[i][j] = 0;
            }
        }
    }

    /**
     * Initialize the grid with the fixed heat source values required.
     */
    private static void initializeGrid(double[][] grid) {
        int n = grid.length;

        // Example based on assignment diagram:
        // Top row fixed to 30
        for (int j = 0; j < n; j++) grid[0][j] = 30;

        // Bottom row fixed to 75
        for (int j = 0; j < n; j++) grid[n - 1][j] = 75;

        // Left column fixed to 15
        for (int i = 0; i < n; i++) grid[i][0] = 15;

        // Right column fixed to 72
        for (int i = 0; i < n; i++) grid[i][n - 1] = 72;

        // Interior cells start at 0
        for (int i = 1; i < n - 1; i++) {
            for (int j = 1; j < n - 1; j++) {
                grid[i][j] = 0;
            }
        }
    }
}

