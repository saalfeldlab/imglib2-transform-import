package forFFC;

import java.io.File;
import java.io.IOException;

import bdv.img.TpsTransformWrapper;
import bigwarp.landmarks.LandmarkTableModel;
import calibratedRai.CalibratedRai3d;
import ij.IJ;
import ij.ImagePlus;
import io.nii.NiftiIo;
import loci.formats.FormatException;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransformSequence;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.ants.ANTSDeformationField;
import net.imglib2.realtransform.ants.ANTSLoadAffine;
import net.imglib2.type.numeric.real.FloatType;
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
	
	public static void main( String[] args ) throws FormatException, IOException
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


		/* 
		 * READ THE IMAGE
		 */
		ImagePlus ip = read( srcF );
		ImagePlus itvl = read( itvlF );
		double[] res = new double[] {
				itvl.getCalibration().pixelWidth,
				itvl.getCalibration().pixelHeight,
				itvl.getCalibration().pixelDepth
		};
	
		AffineTransform3D outputCalibration = new AffineTransform3D();
		outputCalibration.set( res[ 0 ], 0, 0 );
		outputCalibration.set( res[ 1 ], 1, 1 );
		outputCalibration.set( res[ 2 ], 2, 2 );
		System.out.println( "output calibration: " + outputCalibration );
	
		Img<?> interval = ImageJFunctions.wrap( itvl );

		CalibratedRai3d<FloatType> crai = new CalibratedRai3d<FloatType>( ip, new FloatType() );
		RealRandomAccessible< FloatType > imgrra = crai.getRealRandomAccessible();
	
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
		IntervalView< FloatType > output = Views.interval( Views.raster( 
				 RealViews.transform( imgrra, totalXfm )), 
				 interval);
		
		System.out.println( "writing" );
		ImagePlus ipout = ImageJFunctions.wrap( output, "xfm" );
		ipout.getCalibration().pixelWidth = itvl.getCalibration().pixelWidth;
		ipout.getCalibration().pixelHeight = itvl.getCalibration().pixelHeight;
		ipout.getCalibration().pixelDepth = itvl.getCalibration().pixelDepth;
		IJ.save( ipout, newdirfile);

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
	
}