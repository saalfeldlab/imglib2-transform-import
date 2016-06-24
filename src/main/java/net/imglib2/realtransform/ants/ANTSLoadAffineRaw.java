package net.imglib2.realtransform.ants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;

/**
 * Loads an affine transform generated by ANTS as 
 * an AffineTransform3d. 
 * 
 * @author John Bogovic <bogovicj@janelia.hhmi.org>
 *
 */
public class ANTSLoadAffineRaw {

	// ConvertTransformFile 3 --hm <sourcefile> <destfile.mat>
	public static AffineTransform3D loadAffine( String filePath ) throws IOException
	{
		if( !filePath.endsWith( "mat" ))
		{
			System.err.println("Must be a homogenous matrix, try running:");
			System.err.println("ConvertTransformFile 3 " + filePath + " " +
					filePath + ".mat --hm");
		}
		
		BufferedReader reader = new BufferedReader( new FileReader( new File( filePath )));
		String         line = null;
		
		double[] mtxDat = new double[ 12 ];
		
		int i = 0;
		int r = 0;
		while( ( line = reader.readLine() ) != null )
		{
			if( r == 3 ) 
				break;
			
			double[] row = parameters( line );
		
			for( int c = 0; c < row.length; c++ )
				mtxDat[ i++ ] = row[ c ];
			
			r++;
		}
		reader.close();
		
//		double eps = 0.0000001;
//		
//		System.out.println( "detmtx: " + det( mtxDat ));
//		if( det( mtxDat ) < 0.0001 )
//		{
//			mtxDat[  0 ] = eps;
//			mtxDat[  5 ] = eps;
//			mtxDat[ 10 ] = eps;
//		}
		
		AffineTransform3D affine = new AffineTransform3D();
		affine.set( mtxDat );
		
		return affine;
	}
	
	public static double det( double[] m )
	{
		return
				m[0]* m[5] * m[10] +
				m[4] * m[9] * m[2] +
				m[8] * m[1] * m[6] -
				m[2] * m[5] * m[8] -
				m[6] * m[9] * m[0] -
				m[10]* m[1] * m[4];
	}
	
	public static AffineTransform3D loadAffineRaw( String filePath ) throws IOException
	{
		AffineTransform3D affine = new AffineTransform3D();
		
		BufferedReader reader = new BufferedReader( new FileReader( new File( filePath )));
		String         line = null;
		double[] fixed = null;
		
		AntsXfmParams params = new AntsXfmParams();
		
		while( ( line = reader.readLine() ) != null )
		{
			if( line.startsWith( "Parameters:") )
			{
				//affine.set( parameters( line ));
				params.fromLine( parametersNamed( line ));
				
			}
			else if( line.startsWith( "FixedParameters:") )
			{
				params.setCenter( parametersNamed( line ));
			}
		}
		
		reader.close();
		
		params.computeMatrix();
		params.computeOffset();
		
		return params.getTransorm();
	}
	
	private static double[] parameters( String paramLine )
	{
		String[] splitLine = paramLine.split(" ");
		double[] out = new double[ splitLine.length ];
		
		for( int i = 0; i < splitLine.length; i++ )
			out[ i ] = Double.parseDouble( splitLine[ i ] );
		
		return out;
	}
	
	private static double[] parametersNamed( String paramLine )
	{
		String[] splitLine = paramLine.split(" ");
		//System.out.println( splitLine[ 0 ]);
		
		double[] out = new double[ splitLine.length - 1 ];
		
		for( int i = 1; i < splitLine.length; i++ )
			out[ i-1 ] = Double.parseDouble( splitLine[ i ] );
		
		return out;
	}
	
	private static ArrayList<double[]> loadPts( String fn ) throws IOException
	{
		BufferedReader reader = new BufferedReader( new FileReader( new File( fn )));
		String line = null;
		
		ArrayList<double[]> out = new ArrayList<double[]>(); 
		 
		while( ( line = reader.readLine() ) != null )
			if( !line.startsWith( "x") )
				out.add( loadLine( line ));
		
		reader.close();
		return out;
	}
	
	private static double[] loadLine( String line )
	{
		String[] splitLine = line.split(",");
		double[] out = new double[ splitLine.length ];
		for( int i = 0; i < splitLine.length; i++ )
			out[ i ] = Double.parseDouble( splitLine[ i ] );
		
		return out;
	}
	
	public static void conjugateQuaternionToR( double[] q, double[][] R )
	{
		LinAlgHelpers.quaternionToR( conjugateQuaternion( q ), R );
	}
	
	public static DenseMatrix64F conjugateQuaternionToR( double[] q )
	{
		double[][] Rarray = new double[ 3 ][ 3 ];
		LinAlgHelpers.quaternionToR( conjugateQuaternion( q ), Rarray );
		return new DenseMatrix64F( Rarray );
	}
	
	public static double[] conjugateQuaternion( double[] q )
	{
		return new double[]{ q[ 0 ], -q[ 1 ], -q[ 2 ], -q[ 3 ] };
	}

	private static class AntsXfmParams
	{
		
		double[] q;
		double S1;
		double S2;
		double S3;
		double K1;
		double K2;
		double K3;
		double[] translation;
		
		AffineTransform3D matrix;
		double[] center;
		double[] offset;
		
		public AntsXfmParams()
		{
			q = new double[]{ 0, 0, 0, 1};
			S1 = 1;
			S2 = 1;
			S3 = 1;
			K1 = 0;
			K2 = 0;
			K3 = 0;
		}
		
		public void fromLine( double[] paramVector )
		{
			q = new double[ 4 ];
			translation = new double[ 3 ]; // TODO generalize to arbitrary dims
			
			int i = 0;
			while( i < 4 )
				q[ i ] = paramVector[ i++ ];
			
			S1 = paramVector[ i++ ];
			S2 = paramVector[ i++ ];
			S3 = paramVector[ i++ ];
			K1 = paramVector[ i++ ];
			K2 = paramVector[ i++ ];
			K3 = paramVector[ i++ ];
			
			int j = 0;
//			while( j < 3 )
			while( j < 2 )
				translation[ j++ ] = paramVector[ i++ ];
			
			System.out.println( "q: " + q[0] + " " + q[1] + " " + q[2] + " " + q[3]);
			System.out.println( "S: " + S1 + " " + S2 + " " + S3 );
			System.out.println( "K: " + K1 + " " + K2 + " " + K3 );
			System.out.println( "t: " + translation[0] + " " + translation[1] + " " + translation[2]);
		}
		
		public void setCenter( double[] center )
		{
			this.center = center;
		}
		
		public void computeMatrix( )
		{
			
			DenseMatrix64F R = ANTSLoadAffineRaw.conjugateQuaternionToR( q );
			
			DenseMatrix64F S = new DenseMatrix64F( 3, 3, true, 
					new double[]{ S1, 0, 0,
								  0, S2, 0,
								  0, 0, S3 });
			
			DenseMatrix64F K = new DenseMatrix64F( 3, 3, true, 
					new double[]{ 1, K1, K2,
								  0,  1, K3,
								  0,  0,  1 });
			
//			System.out.println( R );
//			System.out.println( S );
//			System.out.println( K );
			
			DenseMatrix64F tmp = new DenseMatrix64F( 3, 3 );
			
			
			CommonOps.mult( R, S, tmp );
			
			// re-use matrix R ( but re-name it for clarity )
			DenseMatrix64F out = R;
			
			// make a new matrix for the output
//			DenseMatrix64F out= new DenseMatrix64F( 3, 3 );
//			CommonOps.mult( tmp, K, out );
			
			System.out.println( out );
			
			/* 
			 * enter elements of matrix in a loop
			 * since the out DenseMatrix64F is 3x3
			 * whereas matrix is 4x4  
			 */
			matrix = new AffineTransform3D(); 
			for( int i = 0; i < 3; i++ ) for( int j = 0; j < 3; j++ )
				matrix.set( out.get(i, j), i, j);
			
			
			System.out.println( matrix );
		}
		
		public void computeOffset()
		{
			offset = new double[ 3 ];
			
			for( int i = 0; i < 3; i++ )
			{
				offset[i] = translation[i] + center[i];
				for( int j = 0; j < 3; j++ )
				{
					offset[ i ] -= matrix.get( i, j ) * center[ j ];
				}
			}
			
			for( int i = 0; i < 3; i++ )
				matrix.set( offset[ i ], i, 3 );
		}
		
		public AffineTransform3D getTransorm()
		{
			return matrix;
		}
	}
	
	public static void main(String[] args) throws IOException {
		
//		String affinePath = "/groups/saalfeld/home/bogovicj/projects/wong_reg/seg_flyc/exps/saved_sh_exp/exp0019_test_warps.sh_20150915101728/TESTAffine.txt";
		String affinePath = "/groups/saalfeld/home/bogovicj/projects/wong_reg/seg_flyc/exps/saved_sh_exp/exp0019_test_warps.sh_20150915101728/TESTAffine.mat";
//		String affinePath = "/Users/bogovicj/Documents/tests/ants/TESTAffine.mat";
		
		String ptPath = "/groups/saalfeld/home/bogovicj/projects/wong_reg/seg_flyc/exps/saved_sh_exp/exp0019_test_warps.sh_20150915101728/pts.csv";
//		String ptPath = "/Users/bogovicj/Documents/tests/ants/pts.csv";
		AffineTransform3D xfm = loadAffine( affinePath );
		System.out.println( xfm );
		
		ArrayList<double[]> pts = loadPts( ptPath );
		//System.out.println( " " + XfmUtils.printArray( pts.get( 1 )));
		
		double[] xfmPt = new double[ 3 ];
		for( int i = 0; i < pts.size(); i++ )
		{
			xfm.apply( pts.get( i ), xfmPt );
			//System.out.println( XfmUtils.printArray( xfmPt ));
		}
		
	}
}
