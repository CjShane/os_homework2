/**
 * The Matrix class represents a 2D grid of temperature values used for
 * simulating heat distribution in the OS Homework 2 thermal averaging assignment.
 * <p>
 * Each cell represents a plate whose temperature is either fixed (boundary plates)
 * or computed iteratively as the average of its four neighboring plates.
 * The class stores both the current temperature grid and the grid from the
 * previous iteration, which allows for calculating total error between iterations.
 */
public class Matrix {
    private double [][] matrix;
    private double[][] previous;
    private int iterations;

    /**
     * Constructs a Matrix object with an initial grid of temperature values.
     * Creates a second internal grid to store the previous iteration's values.
     *
     * @param matrix the initial 2D array representing temperature values
     */
    public Matrix(double [][] matrix) {
        this.matrix = matrix;
        this.previous = new double[matrix.length][matrix[0].length];
        copyGrid(matrix, this.previous);
    }


    /**
     * Computes the average temperature for a single plate at position (i, j).
     * <p>
     * Interior plates are averaged using their four orthogonal neighbors:
     * up, down, left, and right. Boundary plates are treated as fixed heat
     * sources and thus simply return their current stored temperature value.
     *
     * @param i the row index of the plate
     * @param j the column index of the plate
     * @return the computed average temperature for the plate
     */
    double computerAvg(int i, int j){
        if (i <= 0 || i >= matrix.length - 1 || j <= 0 || j >= matrix[0].length - 1) {
            return matrix[i][j];
        }

        double up = matrix[i - 1][j];
        double down = matrix[i + 1][j];
        double left = matrix[i][j - 1];
        double right = matrix[i][j + 1];

        return (up + down + left + right) / 4.0;
    }

    /**
     * Copies all values from the source grid into the destination grid.
     * This is used to store the current iteration's state into the
     * previous-iteration grid before computing a new iteration.
     *
     * @param src the source 2D array
     * @param dst the destination 2D array
     */
    void copyGrid(double[][] src, double[][] dst){
        for (int i = 0; i < src.length; i++) {
            for (int j = 0; j < src[i].length; j++) {
                dst[i][j] = src[i][j];
            }
        }
    }

    /**
     * Saves the current matrix values into the previous matrix buffer.
     * This should be called once per iteration before new values are computed.
     */
    void saveCurrentToPrevious() {
        copyGrid(matrix, previous);
    }

    /**
     * Computes the total grid error for the current iteration.
     * <p>
     * Grid error is defined as the sum of the absolute differences between
     * each plate's current temperature and its temperature from the previous
     * iteration.
     *
     * @return the total accumulated grid error
     */
    double computeGridError(){
        double totalError = 0.0;
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                totalError += Math.abs(matrix[i][j] - previous[i][j]);
            }
        }
        return totalError;
    }

    /**
     * Computes the average temperature of all plates in the grid.
     *
     * @return the average temperature across the entire matrix
     */
    double computeAverageTemperature(){
        double sum = 0.0;
        int count = 0;
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                sum += matrix[i][j];
                count++;
            }
        }
        return sum / count;
    }
}
