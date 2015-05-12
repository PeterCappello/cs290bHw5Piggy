/*
 * The MIT License
 *
 * Copyright 2015 peter.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package applications.mandelbrotset;
import api.ReturnSubtasks;
import api.ReturnValue;
import api.Task;
import api.TaskRecursive;
import static clients.ClientMandelbrotSet.BLOCK_SIZE;
import static clients.ClientMandelbrotSet.EDGE_LENGTH;
import static clients.ClientMandelbrotSet.ITERATION_LIMIT;
import static clients.ClientMandelbrotSet.LOWER_LEFT_X;
import static clients.ClientMandelbrotSet.LOWER_LEFT_Y;
import static clients.ClientMandelbrotSet.N_PIXELS;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Peter Cappello
 */
public class TaskMandelbrotSet extends TaskRecursive<IterationCounts>
{
    static final private int MAX_NUM_PIXELS = 256;
    
    final private double lowerLeftX;
    final private double lowerLeftY;
    final private double edgeLength;
    final private int numPixels;
    final private int iterationLimit;
    final private int blockRow;
    final private int blockCol;
            
    public TaskMandelbrotSet( double lowerLeftX, double lowerLeftY, double edgeLength, int numPixels, int iterationLimit, int blockRow, int blockCol )
    {
        this.lowerLeftX = lowerLeftX;
        this.lowerLeftY = lowerLeftY;
        this.edgeLength = edgeLength;
        this.numPixels = numPixels;
        this.iterationLimit = iterationLimit;
        this.blockRow = blockRow;
        this.blockCol = blockCol;
    }
    
    @Override
    public boolean isAtomic() { return MAX_NUM_PIXELS < numPixels; }

    @Override
    public ReturnValue<IterationCounts> solve() 
    {
        final Integer[][] counts = new Integer[numPixels][numPixels];
        final double delta = edgeLength / numPixels;
        for ( int row = 0; row < numPixels; row++ )
            for ( int col = 0; col < numPixels; col++ )
            {
                counts[row][col] = getIterationCount( row, col, delta );
            }
        return new ReturnValue<>( this, new IterationCounts( counts, 0, 0 ) );
    }

    @Override
    public ReturnSubtasks decompose() 
    {
        final List<Task> subtasks = new  LinkedList<>();
        final int numBlocks = N_PIXELS / BLOCK_SIZE;
        double subTaskEdgeLength = EDGE_LENGTH / numBlocks;
        for ( int subTaskBlockRow = 0; subTaskBlockRow < numBlocks; subTaskBlockRow++ )
        {
            for ( int subTaskBlockCol = 0; subTaskBlockCol < numBlocks; subTaskBlockCol++ )
            {
                final double subTaskLowerLeftX = LOWER_LEFT_X + subTaskEdgeLength * subTaskBlockRow;
                final double subTaskLowerLeftY = LOWER_LEFT_Y + subTaskEdgeLength * subTaskBlockCol ;
                Task task = new TaskMandelbrotSet( subTaskLowerLeftX, subTaskLowerLeftY, subTaskEdgeLength , BLOCK_SIZE, ITERATION_LIMIT, subTaskBlockRow, subTaskBlockCol );
                subtasks.add( task );
            }
        }
        return new ReturnSubtasks( new AddBlocks(), subtasks );
    }
    
    @Override
    public String toString()
    {
        return String.format( "%s \n\t x: %e \n\t y: %e \n\t length: %e \n\t pixels: %d \n\t iteration limit: %d \n\t blockRow: %d \n\t blockCol: %d\n", 
                getClass(), lowerLeftX, lowerLeftY, edgeLength, numPixels, iterationLimit, blockRow, blockCol );
    }
    
    private int getIterationCount( int row, int col, double delta )
    {
        final double x0 = lowerLeftX + row * delta;
        final double y0 = lowerLeftY + col * delta;
        int iteration = 0;
        for ( double x = x0, y = y0; x*x + y*y <= 4.0 && iteration < iterationLimit; iteration++ )
        {
            double xtemp = x*x - y*y + x0;
            y = 2*x*y + y0;
            x = xtemp;
        }
        return iteration;
    }
}
