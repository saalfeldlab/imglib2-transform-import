package io.nii;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import loci.formats.FormatException;
import loci.formats.in.DefaultMetadataOptions;
import loci.formats.in.MetadataLevel;
import loci.formats.in.NiftiReader;
import loci.plugins.util.ImageProcessorReader;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.util.Util;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

public class NiftiIo
{
	public static final String KEY_XRES = "Voxel width";
	public static final String KEY_YRES = "Voxel height";
	public static final String KEY_ZRES = "Slice thickness";
	
	public static ImagePlus readNifti( File f ) throws FormatException, IOException
	{
		AntsDefNiftiReader reader = new AntsDefNiftiReader();
		reader.setMetadataOptions( new DefaultMetadataOptions( MetadataLevel.MINIMUM ));
		reader.setId( f.getAbsolutePath() );

		int width = reader.getSizeX();
		int height = reader.getSizeY();
		int depth = reader.getSizeZ();
		int timepts = reader.getSizeT();
		int channels = reader.getSizeC();
		int imgCount = reader.getImageCount();
		System.out.println( "img count" + imgCount );
		System.out.println("timepts " + timepts  );
		System.out.println("depth " + depth  );
		System.out.println("channels " + channels  );
		
		if( timepts > 1 && channels == 1 )
		{
			channels = timepts;
		}
		
		double[] res = new double[]{ 1.0, 1.0, 1.0 };
		Hashtable< String, Object > meta = reader.getGlobalMetadata();

		// Get image resolutions from file
		if( meta.keySet().contains( KEY_XRES ))
		{
//			System.out.println( "resx ");
			res[ 0 ] = (Double)(meta.get( KEY_XRES ));
		}
		
		if( meta.keySet().contains( KEY_YRES ))
		{
//			System.out.println( "resy");
			res[ 1 ] = (Double)(meta.get( KEY_YRES ));
		}
		
		if( meta.keySet().contains( KEY_ZRES ))
		{
//			System.out.println( "resz ");
			res[ 2 ] = (Double)(meta.get( KEY_ZRES ));
		}
		
		ImageProcessorReader ipr = new ImageProcessorReader( reader );
		ImageStack stack = new ImageStack( width, height );
		for ( int z = 0; z < imgCount; z++ )
		{
			ImageProcessor[] processors = ipr.openProcessors( z );
			stack.addSlice( processors[ 0 ] );
		}

		reader.close();
		ipr.close();

		ImagePlus ip = new ImagePlus( f.getName(), stack );
		//switching channels and frames for imglib2 reasons
		ip.setDimensions( 1, depth, channels );

		ip.getCalibration().pixelWidth  = res[ 0 ];
		ip.getCalibration().pixelHeight = res[ 1 ];
		ip.getCalibration().pixelDepth  = res[ 2 ];

		return ip;
	
	}
	
	public static void main( String[] args ) throws FormatException, IOException
	{
//		String niipath = "/groups/saalfeld/home/bogovicj/projects/flyChemStainAtlas/ants_groundTruth/groupwise_all_884/ALL-F-A1_TileConfiguration_lensrepaired.nii.gz";
		//String niipath = "/groups/saalfeld/home/bogovicj/projects/flyChemStainAtlas/ants_groundTruth/NEW_groupwise_all-flip_884/ALLF-M-A4_TileConfiguration_lensWarp-mipav.nii.gz";
		//String niipath = "/nrs/saalfeld/john/projects/flyChemStainAtlas/take5_groupwise_template_442/all-flip/ALLF-M-I2_TileConfiguration_lens_registered_down442Warp.nii.gz";
		String niipath = "/data-ssd/john/ALLF-F-A1_TileConfiguration_lens.registered_down_flipHRWarp.nii";
		ImagePlus ip = readNifti( new File( niipath ));
//		ImagePlus ip = myReadNifti( new File( niipath ));
		System.out.println( ip );
		IJ.save( ip, "/data-ssd/john/ALLF-F-A1.tif" );
		
		Img<?> img = ImageJFunctions.wrap( ip );
		
		System.out.println( Util.printInterval( img ));
		
//		ImagePlus[] ip = BF.openImagePlus( niipath );
//		System.out.println( ip );
//		System.out.println( ip.length );
	}
}
