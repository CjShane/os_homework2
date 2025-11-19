public class Main {

    // You can adjust this for testing or final large runs
    private static final int GRID_SIZE = 1000;

    public static void main(String[] args) throws InterruptedException {

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
