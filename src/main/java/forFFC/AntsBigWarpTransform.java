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
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.exception.ImgLibException;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ByteImagePlus;
import net.imglib2.img.imageplus.FloatImagePlus;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.img.imageplus.ShortImagePlus;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransformSequence;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.ants.ANTSDeformationField;
import net.imglib2.realtransform.ants.ANTSLoadAffine;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class AntsBigWarpTransform
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
	
	public static void main( String[] args ) throws FormatException, IOException, ImgLibException
	{
		System.out.println("Started AntsBigWarpTransform111");
		File srcF  = new File( args[0] );
		File itvlF = new File( args[1] );
		File tpsF = new File( args[2] );
		String newdirfile = new String(args[3]);

		File affineF = null;
		File defF = null;

		if( args.length > 4 )
			affineF = new File( args[4] );

		if( args.length > 5 )
			defF = new File( args[5] );

		int nThreads = 1;
		if( args.length > 6 )
			nThreads = Integer.parseInt( args[6] );

		/* 
		 * READ THE IMAGE
		 */
		ImagePlus ip = read( srcF );
		ImagePlus itvl = read( itvlF );


		if( ip.getBitDepth() == 8 )
		{
			System.out.println( "BYTE" );
			CalibratedRai3d< ByteType > crai = new CalibratedRai3d< ByteType >( ip, new ByteType() );
			ImagePlusImg< ByteType, ? > impi = ImagePlusImgs.bytes( Intervals.dimensionsAsLongArray( ImageJFunctions.wrap( itvl ) ) );
			execute( crai, impi, itvl, affineF, defF, tpsF, newdirfile, nThreads );
		}
		else if( ip.getBitDepth() == 16 )
		{
			System.out.println( "SHORT" );
			CalibratedRai3d< ShortType > crai = new CalibratedRai3d< ShortType >( ip, new ShortType() );
			ShortImagePlus< ShortType > impi = ImagePlusImgs.shorts( Intervals.dimensionsAsLongArray( ImageJFunctions.wrap( itvl ) ) );
			execute( crai, impi, itvl, affineF, defF, tpsF, newdirfile, nThreads );
		}
		else if( ip.getBitDepth() == 32 )
		{
			System.out.println( "FLOAT" );
			CalibratedRai3d< FloatType > crai = new CalibratedRai3d< FloatType >( ip, new FloatType() );
			FloatImagePlus< FloatType > impi = ImagePlusImgs.floats( Intervals.dimensionsAsLongArray( ImageJFunctions.wrap( itvl ) ) );
			execute( crai, impi, itvl, affineF, defF, tpsF, newdirfile, nThreads );
		}
		else
		{
			System.err.println( "unrecognized type");
			return;
		}

		System.out.println("done");
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

	public static < T extends RealType< T > & NativeType<T>> void execute( 
			CalibratedRai3d< T > crai,
			ImagePlusImg< T, ? > ipimg,
			ImagePlus itvl,
			File affineF, File defF, File tpsF,
			String newdirfile,
			int nThreads ) throws IOException, FormatException, ImgLibException
	{

		RealRandomAccessible< T > imgrra = crai.getRealRandomAccessible();

		double[] res = new double[] {
				itvl.getCalibration().pixelWidth,
				itvl.getCalibration().pixelHeight,
				itvl.getCalibration().pixelDepth
		};
	
		AffineTransform3D outputCalibration = new AffineTransform3D();
		outputCalibration.set( res[ 0 ], 0, 0 );
		outputCalibration.set( res[ 1 ], 1, 1 );
		outputCalibration.set( res[ 2 ], 2, 2 );

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
		if( affine != null )
			totalXfm.add( affine.inverse() );

		if( df != null )
			totalXfm.add( df );

		totalXfm.add( tpsInvXfm );
		totalXfm.add( outputCalibration.inverse() );
		
		/*
		 * Render the image
		 */
		IntervalView< T > output = Views.interval( Views.raster( 
				 RealViews.transform( imgrra, totalXfm )), 
				 ipimg);


//		ImagePlusImg< T, ? > ipimg = getOutputIpi( ip, interval );
		System.out.println("copyting with " + nThreads + " threads.");
		copyToImageStack( output, ipimg, nThreads );
		
		System.out.println( "writing" );
		ImagePlus ipout = ipimg.getImagePlus();
		ipout.getCalibration().pixelWidth = itvl.getCalibration().pixelWidth;
		ipout.getCalibration().pixelHeight = itvl.getCalibration().pixelHeight;
		ipout.getCalibration().pixelDepth = itvl.getCalibration().pixelDepth;
		IJ.save( ipout, newdirfile);
	}

	public static < T extends NumericType<T> > RandomAccessibleInterval<T> copyToImageStack( 
			final RandomAccessible< T > ra,
			final RandomAccessibleInterval<T> target,
			final int nThreads )
	{
		// TODO I wish I didn't have to do this inside this method
//		MixedTransformView< T > raible = Views.permute( ra, 2, 3 );
		RandomAccessible< T > raible = ra;

		// what dimension should we split across?
		int nd = raible.numDimensions();
		int tmp = nd - 1;
		while( tmp >= 0 )
		{
			if( target.dimension( tmp ) > 1 )
				break;
			else
				tmp--;
		}
		final int dim2split = tmp;

		final long[] splitPoints = new long[ nThreads + 1 ];
		long N = target.dimension( dim2split );
		long del = ( long )( N / nThreads ); 
		splitPoints[ 0 ] = target.min( dim2split );
		splitPoints[ nThreads ] = target.max( dim2split ) + 1;
		for( int i = 1; i < nThreads; i++ )
		{
			splitPoints[ i ] = splitPoints[ i - 1 ] + del;
			System.out.println( "splitPoints[i]: " + splitPoints[ i ] ); 
		}
//		System.out.println( "dim2split: " + dim2split );
//		System.out.println( "split points: " + XfmUtils.printArray( splitPoints ));

		ExecutorService threadPool = Executors.newFixedThreadPool( nThreads );

		LinkedList<Callable<Boolean>> jobs = new LinkedList<Callable<Boolean>>();
		for( int i = 0; i < nThreads; i++ )
		{
			final long start = splitPoints[ i ];
			final long end   = splitPoints[ i+1 ];

			jobs.add( new Callable<Boolean>()
			{
				public Boolean call()
				{
					try
					{
						final FinalInterval subItvl = getSubInterval( target, dim2split, start, end );
						final IntervalView< T > subTgt = Views.interval( target, subItvl );
						final Cursor< T > c = subTgt.cursor();
						final RandomAccess< T > ra = raible.randomAccess();
						while ( c.hasNext() )
						{
							c.fwd();
							ra.setPosition( c );
							c.get().set( ra.get() );
						}
						return true;
					}
					catch( Exception e )
					{
						e.printStackTrace();
					}
					return false;
				}
			});
		}
		try
		{
			List< Future< Boolean > > futures = threadPool.invokeAll( jobs );
			threadPool.shutdown(); // wait for all jobs to finish

		}
		catch ( InterruptedException e1 )
		{
			e1.printStackTrace();
		}

		IJ.showProgress( 1.1 );
		return target;
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
