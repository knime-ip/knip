package org.knime.knip.ops;

import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;

public final class ExampleOp< A extends RealType< A >, B extends IntegerType< B > > implements Converter< A, B >
{
	private final double min;
	private final long qmin;
	private final long qmax;
	private final double quantizeScale;

	public ExampleOp( final double min, final double max, final long quantizedMin, final long quantizedMax )
	{
		this.min = min;
		this.qmin = quantizedMin;
		this.qmax = quantizedMax;
		this.quantizeScale = ( quantizedMax - quantizedMin + 1 ) / ( max - min );
	}

	private final long quantize( final double value )
	{
		final long x = ( long ) Math.floor( (value - min ) * quantizeScale ) + qmin;
		return x > qmax ? qmax : ( x < qmin ? qmin : x );
	}

	@Override
	public void convert( final A input, final B output )
	{
		output.setInteger( quantize( input.getRealDouble() ) );
	}
}
