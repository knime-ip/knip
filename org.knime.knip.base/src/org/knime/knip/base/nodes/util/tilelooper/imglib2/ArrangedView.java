/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2016 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
 * John Bogovic, Albert Cardona, Barry DeZonia, Christian Dietz, Jan Funke,
 * Aivar Grislis, Jonathan Hale, Grant Harris, Stefan Helfrich, Mark Hiner,
 * Martin Horn, Steffen Jaensch, Lee Kamentsky, Larry Lindsey, Melissa Linkert,
 * Mark Longair, Brian Northan, Nick Perry, Curtis Rueden, Johannes Schindelin,
 * Jean-Yves Tinevez and Michael Zinsmaier.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package org.knime.knip.base.nodes.util.tilelooper.imglib2;

import java.util.Arrays;
import java.util.List;

import net.imglib2.AbstractCursor;
import net.imglib2.AbstractInterval;
import net.imglib2.FlatIterationOrder;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.View;
import net.imglib2.util.IntervalIndexer;

/**
 * THIS WILL GO INTO IMGLIB2 THIS IS JUST HERE UNTIL IT IS THERE!
 *
 * Arranges a flat list of {@link RandomAccessibleInterval}s in an <em>n</em>
 * -dimensional {@link RandomAccessibleInterval} of
 * {@link RandomAccessibleInterval}s, i.e. in an <em>n</em>-dimensional grid
 * whose cells are {@link RandomAccessibleInterval}s.
 *
 * @param <T>
 *            the pixel type
 *
 * @author Marcel Wiedenmann (University of Konstanz)
 * @author Christian Dietz (University of Konstanz)
 */
public class ArrangedView< T > extends AbstractInterval implements RandomAccessibleInterval< T >, IterableInterval< T >, View
{
	public static < T > ArrangedView< T > arrangeAlongAxis( final List< ? extends T > source, final int axis )
	{
		final long[] grid = new long[ axis + 1 ];
		Arrays.fill( grid, 0, axis, 1 );
		grid[ axis ] = source.size();
		return new ArrangedView<>( source, grid );
	}

	private final T[] source;

	private final long[] grid;

	@SuppressWarnings( "unchecked" )
	public ArrangedView( final List< ? extends T > source, final long... grid )
	{
		super( grid );
		this.source = ( T[] ) source.toArray( new Object[ source.size() ] );
		this.grid = grid;
	}

	public List< T > getSource()
	{
		return Arrays.asList( source );
	}

	@Override
	public ArrangedViewRandomAccess< T > randomAccess()
	{
		return new ArrangedViewRandomAccess<>( source, grid );
	}

	@Override
	public ArrangedViewRandomAccess< T > randomAccess( final Interval interval )
	{
		return randomAccess();
	}

	@Override
	public long size()
	{
		return source.length;
	}

	@Override
	public T firstElement()
	{
		return source[ 0 ];
	}

	@Override
	public Object iterationOrder()
	{
		return new FlatIterationOrder( this );
	}

	@Override
	public ArrangedViewCursor< T > iterator()
	{
		return cursor();
	}

	@Override
	public ArrangedViewCursor< T > cursor()
	{
		return new ArrangedViewCursor<>( source, grid );
	}

	@Override
	public ArrangedViewCursor< T > localizingCursor()
	{
		return cursor();
	}

	public static class ArrangedViewRandomAccess< T > extends Point implements RandomAccess< T >
	{
		private final T[] source;

		private final long[] grid;

		public ArrangedViewRandomAccess( final T[] source, final long[] grid )
		{
			super( grid.length );
			this.source = source;
			this.grid = grid;
		}

		private ArrangedViewRandomAccess( final ArrangedViewRandomAccess< T > ra )
		{
			super( ra.position, true );
			source = ra.source;
			grid = ra.grid;
		}

		public T get( final int flatIndex )
		{
			return source[ flatIndex ];
		}

		@Override
		public T get()
		{
			final int i = ( int ) IntervalIndexer.positionToIndex( position, grid );
			return source[ i ];
		}

		@Override
		public ArrangedViewRandomAccess< T > copy()
		{
			return new ArrangedViewRandomAccess<>( this );
		}

		@Override
		public ArrangedViewRandomAccess< T > copyRandomAccess()
		{
			return copy();
		}
	}

	public static class ArrangedViewCursor< T > extends AbstractCursor< T >
	{
		private final T[] source;

		private final long[] grid;

		private final int maxIndex;

		private int i;

		public ArrangedViewCursor( final T[] source, final long[] grid )
		{
			super( grid.length );
			this.source = source;
			this.grid = grid;
			this.maxIndex = source.length - 1;
			reset();
		}

		private ArrangedViewCursor( final ArrangedViewCursor< T > cursor )
		{
			super( cursor.n );
			this.source = cursor.source;
			this.grid = cursor.grid;
			this.maxIndex = cursor.maxIndex;
			this.i = cursor.i;
		}

		@Override
		public T get()
		{
			return source[ i ];
		}

		@Override
		public void fwd()
		{
			++i;
		}

		@Override
		public void jumpFwd( final long steps )
		{
			i += steps;
		}

		@Override
		public void reset()
		{
			i = -1;
		}

		@Override
		public boolean hasNext()
		{
			return i < maxIndex;
		}

		@Override
		public void localize( final long[] position )
		{
			IntervalIndexer.indexToPosition( i, grid, position );
		}

		@Override
		public long getLongPosition( final int d )
		{
			return IntervalIndexer.indexToPosition( i, grid, d );
		}

		@Override
		public ArrangedViewCursor< T > copy()
		{
			return new ArrangedViewCursor<>( this );
		}

		@Override
		public ArrangedViewCursor< T > copyCursor()
		{
			return copy();
		}
	}
}
