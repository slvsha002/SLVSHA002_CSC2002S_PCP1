package MonteCarloMiniParallel;

import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import MonteCarloMiniParallel.TerrainArea;
import MonteCarloMiniParallel.SearchParallel;

//Parallize the serial program

class MonteCarloMinimizationParallel {
    static final boolean DEBUG=false;
	
	static long startTime = 0;
	static long endTime = 0;

	//timers - note milliseconds
	private static void tick(){
		startTime = System.currentTimeMillis();
	}
	private static void tock(){
		endTime=System.currentTimeMillis(); 
	}

	static int[] tracker = initializeTracker();
	public static int[] initializeTracker() {
		int[] tracker = new int[2];
    	tracker[0] = Integer.MAX_VALUE;
    	tracker[1] = -1;
    	return tracker;
	}

	static class MinLocator extends RecursiveTask<int[]> {
		int lowest;
		int highest;
		SearchParallel[] searchArray;
		int glob_min =Integer.MAX_VALUE;
		int local_min = Integer.MAX_VALUE;
		private final int SEQUENTIAL_CUTOFF=300;

		public MinLocator(SearchParallel[] arr, int low, int high) {
			this.searchArray = arr;
			this.lowest = low;
			this.highest = high;
		}

		protected int[] compute() { //make this method an array so it can return the min and co ords?
			if((highest - lowest) < SEQUENTIAL_CUTOFF){
				int[] localTracker = new int[2];
				for(int i=lowest; i<highest; i++){
					local_min = searchArray[i].find_valleys();
					if((!searchArray[i].isStopped()) && (local_min <= tracker[0])) {
						localTracker[0] = local_min;
						localTracker[1] = i;
						tracker = localTracker;
					}
				}
			}
			else{
				MinLocator left = new MinLocator(searchArray, lowest, ((highest+lowest)/2));
				MinLocator right = new MinLocator(searchArray, ((highest+lowest)/2), highest);
				left.fork();
				int [] trackerRight = right.compute();
				int [] trackerLeft = left.join();
				if(trackerRight[0] <= trackerLeft[0]){
					tracker = trackerRight;
				}
				else{
					tracker = trackerLeft;
				}
			}
			return tracker;
		}
	}

    //Initialize the fork join
    static final ForkJoinPool fjPool = new ForkJoinPool();
    static int[] minimum(SearchParallel searchArr[]) {
        return fjPool.invoke(new MinLocator(searchArr,0,searchArr.length));
    }

    public static void main(String[] args)  {

    	int rows, columns; //grid size
    	double xmin, xmax, ymin, ymax; //x and y terrain limits
    	TerrainArea terrain;  //object to store the heights and grid points visited by searches
    	double searches_density;	// Density - number of Monte Carlo  searches per grid position - usually less than 1!

     	int num_searches;		// Number of searches
    	Random rand = new Random();  //the random number generator
        SearchParallel [] searchesParallel;		// Array of searches
    	
    	if (args.length!=7) {  
    		System.out.println("Incorrect number of command line arguments provided.");   	
    		System.exit(0);
    	}
    	/* Read argument values */
    	rows =Integer.parseInt( args[0] );
    	columns = Integer.parseInt( args[1] );
    	xmin = Double.parseDouble(args[2] );
    	xmax = Double.parseDouble(args[3] );
    	ymin = Double.parseDouble(args[4] );
    	ymax = Double.parseDouble(args[5] );
    	searches_density = Double.parseDouble(args[6] );
  
    	if(DEBUG) {
    		/* Print arguments */
    		System.out.printf("Arguments, Rows: %d, Columns: %d\n", rows, columns);
    		System.out.printf("Arguments, x_range: ( %f, %f ), y_range( %f, %f )\n", xmin, xmax, ymin, ymax );
    		System.out.printf("Arguments, searches_density: %f\n", searches_density );
    		System.out.printf("\n");
    	}

        //Initilize
        terrain = new TerrainArea(rows, columns, xmin,xmax,ymin,ymax);
        num_searches = (int)( rows * columns * searches_density );
        searchesParallel= new SearchParallel [num_searches];
        
        for (int i=0;i<num_searches;i++) 
    		searchesParallel[i]=new SearchParallel(i+1, rand.nextInt(rows),rand.nextInt(columns),terrain);
    	
      	if(DEBUG) {
    		/* Print initial values */
    		System.out.printf("Number searches: %d\n", num_searches);
    		//terrain.print_heights();
    	}

        //start timer
        tick();

		int[] min = minimum(searchesParallel);
		/*ForkJoinPool fjPool = new ForkJoinPool();
		MinLocator miniLoc = new MinLocator(searchesParallel,0,searchesParallel.length);
		int[] min = fjPool.invoke(miniLoc);*/

		if(DEBUG) System.out.println("Search "+searchesParallel[min[1]].getID()+" finished at  "+min[0] + " in " +searchesParallel[min[0]].getSteps());
        //end timer
        tock();

        if(DEBUG) {
    		/* print final state */
    		terrain.print_heights();
    		terrain.print_visited();
    	}
		System.out.printf("Run parameters\n");
		System.out.printf("\t Rows: %d, Columns: %d\n", rows, columns);
		System.out.printf("\t x: [%f, %f], y: [%f, %f]\n", xmin, xmax, ymin, ymax );
		System.out.printf("\t Search density: %f (%d searches)\n", searches_density,num_searches );

		/*  Total computation time */
		System.out.printf("Time: %d ms\n",endTime - startTime );
		int tmp=terrain.getGrid_points_visited();
		System.out.printf("Grid points visited: %d  (%2.0f%s)\n",tmp,(tmp/(rows*columns*1.0))*100.0, "%");
		tmp=terrain.getGrid_points_evaluated();
		System.out.printf("Grid points evaluated: %d  (%2.0f%s)\n",tmp,(tmp/(rows*columns*1.0))*100.0, "%");
	
		/* Results*/
		System.out.printf("Global minimum: %d at x=%.1f y=%.1f\n\n", min[0], terrain.getXcoord(searchesParallel[min[1]].getPos_row()), terrain.getYcoord(searchesParallel[min[1]].getPos_col()) );
    }
}
