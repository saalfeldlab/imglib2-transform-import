package io.dfield;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public abstract class AbstractDfieldWriter
{
	protected double[] pixel_spacing;
	protected String unit;
	protected boolean little_endian = true;
	
	public void setPixelSpacing( final double[] pixel_spacing )
	{
		this.pixel_spacing = pixel_spacing;
	}
	
	public void setLittleEndian( final boolean little_endian )
	{
		this.little_endian = little_endian;
	}
	
	public void setUnit( final String unit )
	{
		this.unit = unit;
	}

	public <T extends RealType<T>> int write( File f, RandomAccessibleInterval<T> dfield ) throws IOException
	{
		return write( new FileOutputStream( f ), dfield );
	}
	
	public abstract <T extends RealType<T>> int write( OutputStream out, RandomAccessibleInterval<T> dfield ) throws IOException;

	/**
	 * Returning a -1 indicates that the last dimension holds vector offsets.
	 * 
	 * @return the dimension in which the vector should be stored
	 */
	public abstract int vectorDimension();
	
	public boolean validate( RandomAccessibleInterval<?> dfield )
	{
		int vecdim = vectorDimension();
		if( vecdim < 0 )
			return dfield.dimension( dfield.numDimensions() - 2 ) == ( dfield.numDimensions() - 1 );
		else
			return dfield.dimension( vecdim ) == ( dfield.numDimensions() - 1 );
	}

	/**
	 * 
	 * @param out the OutputStream
	 * @param dfield the displacement field
	 * @return the number of bytes written
	 * @throws IOException 
	 */
	public static <T extends RealType<T>> int writeRaw( OutputStream out, RandomAccessibleInterval<T> dfield ) throws IOException
	{
		DataOutputStream dout = new DataOutputStream( out );
		Cursor<T> c = Views.flatIterable( dfield ).cursor();
		while( c.hasNext() )
		{
			c.fwd();
			dout.writeFloat( c.get().getRealFloat() );
		}
		dout.flush();
		dout.close();

		return dout.size();
	}
	
}
