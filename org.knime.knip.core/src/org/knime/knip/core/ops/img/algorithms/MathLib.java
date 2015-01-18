package org.knime.knip.core.ops.img.algorithms;

/**
 * @deprecated
 */
@Deprecated
public class MathLib {

	public static int pow( final int a, final int b )
	{
		if (b == 0) {
            return 1;
        } else if (b == 1) {
            return a;
        } else
		{
			int result = a;

			for (int i = 1; i < b; i++) {
                result *= a;
            }

			return result;
		}
	}

	public static boolean[][] getRecursiveCoordinates( final int numDimensions )
	{
		boolean[][] positions = new boolean[ MathLib.pow( 2, numDimensions ) ][ numDimensions ];

		setCoordinateRecursive( numDimensions - 1, numDimensions, new int[ numDimensions ], positions );

		return positions;
	}

	/**
	 * recursively get coordinates covering all binary combinations for the given dimensionality
	 *
	 * example for 3d:
	 *
	 * x y z index
	 * 0 0 0 [0]
	 * 1 0 0 [1]
	 * 0 1 0 [2]
	 * 1 1 0 [3]
	 * 0 0 1 [4]
	 * 1 0 1 [5]
	 * 0 1 1 [6]
	 * 1 1 1 [7]
	 *
	 * All typical call will look like that:
	 *
	 * boolean[][] positions = new boolean[ MathLib.pow( 2, numDimensions ) ][ numDimensions ];
	 * MathLib.setCoordinateRecursive( numDimensions - 1, numDimensions, new int[ numDimensions ], positions );
	 *
	 * @param dimension - recusively changed current dimension, init with numDimensions - 1
	 * @param numDimensions - the number of dimensions
	 * @param location - recursively changed current state, init with new int[ numDimensions ]
	 * @param result - where the result will be stored when finished, needes a boolean[ MathLib.pow( 2, numDimensions ) ][ numDimensions ]
	 */
	public static void setCoordinateRecursive( final int dimension, final int numDimensions, final int[] location, final boolean[][] result )
	{
		final int[] newLocation0 = new int[ numDimensions ];
		final int[] newLocation1 = new int[ numDimensions ];

		for ( int d = 0; d < numDimensions; d++ )
		{
			newLocation0[ d ] = location[ d ];
			newLocation1[ d ] = location[ d ];
		}

		newLocation0[ dimension ] = 0;
		newLocation1[ dimension ] = 1;

		if ( dimension == 0 )
		{
			// compute the index in the result array ( binary to decimal conversion )
			int index0 = 0, index1 = 0;

			for ( int d = 0; d < numDimensions; d++ )
			{
				index0 += newLocation0[ d ] * pow( 2, d );
				index1 += newLocation1[ d ] * pow( 2, d );
			}

			// fill the result array
			for ( int d = 0; d < numDimensions; d++ )
			{
				result[ index0 ][ d ] = (newLocation0[ d ] == 1);
				result[ index1 ][ d ] = (newLocation1[ d ] == 1);
			}
		}
		else
		{
			setCoordinateRecursive( dimension - 1, numDimensions, newLocation0, result );
			setCoordinateRecursive( dimension - 1, numDimensions, newLocation1, result );
		}

	}
}
