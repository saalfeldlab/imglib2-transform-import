package forFFC;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import bdv.img.TpsTransformWrapper;
import bigwarp.landmarks.LandmarkTableModel;
import calibratedRai.CalibratedRai3d;
import ij.IJ;
import ij.ImagePlus;
import io.nii.NiftiIo;
import loci.formats.FormatException;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccessible;
import net.imglib2.RealRandomAccessibleRealInterval;
import net.imglib2.exception.ImgLibException;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ByteImagePlus;
import net.imglib2.img.imageplus.FloatImagePlus;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.InvertibleRealTransformSequence;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.ants.ANTSDeformationField;
import net.imglib2.realtransform.ants.ANTSLoadAffine;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class reverseAntsBigWarpTransformMultiThreaded
{
 
	// Args to test the template by itself:
	 /* 
		/nobackup/saalfeld/john/wong_alignment/brains_2016Mar/groupwiseRegSpacing/MYtemplate.tif
		/nobackup/saalfeld/john/wong_alignment/data/frulexAaligned-C3.tif
		/groups/saalfeld/home/bogovicj/projects/wong_reg/antsTemplate2FruTemplateBigwarp/landmarks_20160701.csv
	 
	*/
	/* Args to test a flye example:
	 * 
		/nobackup/saalfeld/john/wong_alignment/brains_2016Mar/groupwiseRegSpacing/MY20160301_r3_ss2527lc10_frup65_rnai_flye_00002_baseline_spacing.nii
		/nobackup/saalfeld/john/wong_alignment/data/frulexAaligned-C3.tif
		/groups/saalfeld/home/bogovicj/projects/wong_reg/antsTemplate2FruTemplateBigwarp/landmarks_20160701.csv
		/nobackup/saalfeld/john/wong_alignment/brains_2016Mar/groupwiseRegSpacing/MY20160301_r3_ss2527lc10_frup65_rnai_flye_00002_baseline_spacingAffine.mat
		/nobackup/saalfeld/john/wong_alignment/brains_2016Mar/groupwiseRegSpacing/MY20160301_r3_ss2527lc10_frup65_rnai_flye_00002_baseline_spacingWarp.nii
	 */
	/* Args to test a flya example:
	 * 
		/nobackup/saalfeld/john/wong_alignment/brains_2016Mar/groupwiseRegSpacing/MY20160301_r3_ss2527lc10_frup65_flya_00001_baseline_spacing.nii
		/nobackup/saalfeld/john/wong_alignment/data/frulexAaligned-C3.tif
		/groups/saalfeld/home/bogovicj/projects/wong_reg/antsTemplate2FruTemplateBigwarp/landmarks_20160701.csv
		/nobackup/saalfeld/john/wong_alignment/brains_2016Mar/groupwiseRegSpacing/MY20160301_r3_ss2527lc10_frup65_flya_00001_baseline_spacingAffine.mat
		/nobackup/saalfeld/john/wong_alignment/brains_2016Mar/groupwiseRegSpacing/MY20160301_r3_ss2527lc10_frup65_flya_00001_baseline_spacingWarp_mipav.nii
	 */
	/*
	 * /data/JData/A/A39_reverseBigWarpTransform/SampleData/Rois/0.tif
	 * /data/JData/A/A39_reverseBigWarpTransform/SampleData/20161117_r3_nsyb_LC4_RNAi_flyd_L_00001_gpuregression1480523880/20161117_r3_nsyb_LC4_RNAi_flyd_L_00001_baseline.tif
	 * LandMarks =/data/JData/A/A39_reverseBigWarpTransform/SampleData/LandMarksLC17_LR-flya-Attempt3.csv
	 * Affine = /data/JData/A/A39_reverseBigWarpTransform/SampleData/ANTs/T_20161117_r3_nsyb_LC4_RNAi_flyd_L_00001_baseline120GenericAffine.mat
	 * Warp = /data/JData/A/A39_reverseBigWarpTransform/SampleData/ANTs/T_20161117_r3_nsyb_LC4_RNAi_flyd_L_00001_baseline121Warp.nii
	 */
	public static void main( String[] args ) throws FormatException, IOException, ImgLibException
	{
		System.out.println("Started reverseAntsBigWarpTransformMultiThreaded");
		File srcF  = new File( args[0] ); //source file
		File itvlF = new File( args[1] ); //target space
		File tpsF = new File( args[2] ); //landmarks
		String newdirfile = new String(args[3]); //output

		File affineF = null;
		File defF = null;

		if( args.length > 4 )
			affineF = new File( args[4] );

		if( args.length > 5 )
			defF = new File( args[5] ); //warp file

		/* 
		 * READ THE IMAGE
		 */
		ImagePlus ip = read( srcF );    // roi
		ImagePlus itvl = read( itvlF ); // source space
		 
		double[] resIn = new double[] {
				itvl.getCalibration().pixelWidth,
				itvl.getCalibration().pixelHeight,
				itvl.getCalibration().pixelDepth
		};
		
		double[] resOut = new double[] {
				ip.getCalibration().pixelWidth,
				ip.getCalibration().pixelHeight,
				ip.getCalibration().pixelDepth
		};
		
		// 'target space'
		AffineTransform3D roiCalibration = new AffineTransform3D();
		roiCalibration.set( resOut[ 0 ], 0, 0 );
		roiCalibration.set( resOut[ 1 ], 1, 1 );
		roiCalibration.set( resOut[ 2 ], 2, 2 );
		System.out.println( "roi calibration: " + roiCalibration );
	
		AffineTransform3D targetCalibration = new AffineTransform3D();
		targetCalibration.set( resIn[ 0 ], 0, 0 );
		targetCalibration.set( resIn[ 1 ], 1, 1 );
		targetCalibration.set( resIn[ 2 ], 2, 2 );
		System.out.println( "target calibration: " + targetCalibration );
		
		Img<FloatType> mask = ImageJFunctions.convertFloat( ip );
		Img<?> interval = ImageJFunctions.wrap( itvl );

		
		/* 
		 * READ THE TRANSFORM
		 */
		// The affine part
		AffineTransform3D affine = null;
		if( affineF != null )
			affine = ANTSLoadAffine.loadAffine( affineF.getAbsolutePath() );
		
		// the deformation
		ANTSDeformationField df = null;
		if( defF != null)
			df = new ANTSDeformationField( defF );
		
		// the TPS
		LandmarkTableModel ltm = new LandmarkTableModel( 3 );
		ltm.load( tpsF );
		TpsTransformWrapper tpsInvXfm = new TpsTransformWrapper( 3 );
		tpsInvXfm.setTps( ltm.getTransform() );
		
		// Concatenate all the transforms
		InvertibleRealTransformSequence totalXfm = new InvertibleRealTransformSequence();
		
		totalXfm.add( targetCalibration );
		if( affine != null )
			totalXfm.add( affine.inverse() );

		if( df != null )	
			totalXfm.add( df );

		totalXfm.add( tpsInvXfm );
		totalXfm.add( roiCalibration.inverse() );
		
		System.out.println( "totalXfm sz " + totalXfm );

		/*
		 * Render the image
		 */
		ByteImagePlus<ByteType> ipimg = ImagePlusImgs.bytes( Intervals.dimensionsAsLongArray( interval ) );

		transferMask(ipimg, totalXfm, mask, mask );
		System.out.println( "writing" );
		ImagePlus ipout = ipimg.getImagePlus();
		ipout.getCalibration().pixelWidth = itvl.getCalibration().pixelWidth;
		ipout.getCalibration().pixelHeight = itvl.getCalibration().pixelHeight;
		ipout.getCalibration().pixelDepth = itvl.getCalibration().pixelDepth;
		
		IJ.save( ipout, newdirfile);

		System.out.println("done");
	}

	public static <T extends RealType<T>, S extends RealType<S>> void transferMask(RandomAccessibleInterval<T> result, 
			InvertibleRealTransform totalxfm, RandomAccessible<S> mask, Interval itvl  )
	{
		System.out.println( "itvl: " + Util.printInterval(itvl));
		
		Cursor<S> c = Views.interval( mask, itvl ).cursor();
		RandomAccess<T> ra = result.randomAccess();
		RealPoint xfmPt = new RealPoint( mask.numDimensions() );
		while( c.hasNext() )
		{
			S s = c.next();
			if( s.getRealDouble() > 0 )
			{
				totalxfm.applyInverse( xfmPt, c );
				
				if( isOutside( xfmPt, result ))
					continue;
				
				System.out.println( "inside" );

				for( int d = 0; d < xfmPt.numDimensions(); d++ )
					ra.setPosition( (int)(Math.round(xfmPt.getDoublePosition(d))), d );

				ra.get().setOne();
			}
		}
	}
	
	public static boolean isOutside( RealPoint pt, Interval itvl )
	{
		for( int d = 0; d < itvl.numDimensions(); d++ )
		{
			if( pt.getDoublePosition(d) < itvl.min( d ) )
				return true;
			
			if( pt.getDoublePosition(d) > itvl.max( d ) )
				return true;
		}
		return false;
	}
	
	public static ImagePlus read( File f ) throws FormatException, IOException
	{
		ImagePlus ip;
		if( f.getName().endsWith( "nii" ))
			ip = NiftiIo.readNifti( f );
		else
			ip = IJ.openImage( f.getAbsolutePath() );
		
		return ip;
	}

	public static FinalInterval getSubInterval( Interval interval, int d, long start, long end )
	{
		int nd = interval.numDimensions();
		long[] min = new long[ nd ];
		long[] max = new long[ nd ];
		for( int i = 0; i < nd; i++ )
		{
			if( i == d )
			{
				min[ i ] = start;
				max[ i ] = end - 1;
			}
			else
			{
				min[ i ] = interval.min( i );
				max[ i ] = interval.max( i );
			}
		}
		return new FinalInterval( min, max );
	}

}
