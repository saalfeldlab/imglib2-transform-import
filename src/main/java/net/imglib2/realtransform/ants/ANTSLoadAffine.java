package net.imglib2.realtransform.ants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import net.imglib2.realtransform.AffineTransform3D;

/**
 * Loads an affine transform generated by ANTS as an AffineTransform3d.
 * 
 * @author John Bogovic &lt;bogovicj@janelia.hhmi.org&gt;
 *
 */
public class ANTSLoadAffine
{

	// ConvertTransformFile 3 --hm <sourcefile> <destfile.mat>
	public static AffineTransform3D loadAffine( String filePath ) throws IOException
	{
		if ( !filePath.endsWith( "mat" ) )
		{
			System.err.println( "Must be a homogenous matrix, try running:" );
			System.err.println( "ConvertTransformFile 3 " + filePath + " " + filePath
					+ ".mat --hm" );
		}

		BufferedReader reader = new BufferedReader( new FileReader( new File( filePath ) ) );
		String line = null;

		double[] mtxDat = new double[ 12 ];

		int i = 0;
		int r = 0;
		while ( (line = reader.readLine()) != null )
		{
			if ( r == 3 )
				break;

			double[] row = parameters( line );

			for ( int c = 0; c < row.length; c++ )
				mtxDat[ i++ ] = row[ c ];

			r++;
		}
		reader.close();


		AffineTransform3D affine = new AffineTransform3D();
		affine.set( mtxDat );

		return affine;
	}

	private static double[] parameters( String paramLine )
	{
		String[] splitLine = paramLine.split( " " );
		double[] out = new double[ splitLine.length ];

		for ( int i = 0; i < splitLine.length; i++ )
			out[ i ] = Double.parseDouble( splitLine[ i ] );

		return out;
	}

}
