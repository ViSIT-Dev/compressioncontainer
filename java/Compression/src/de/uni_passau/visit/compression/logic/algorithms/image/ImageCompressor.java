package de.uni_passau.visit.compression.logic.algorithms.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_passau.visit.compression.data.ImageCompressionLevel;
import de.uni_passau.visit.compression.exceptions.ImageCompressionException;
import de.uni_passau.visit.compression.exceptions.TextureCompressionException;
import de.uni_passau.visit.compression.models.AbstractCompressionLevelFilter;

/**
 * Instances of this class can be used to compress image or the texture of
 * 3D-models. It uses ImageMagick as backend, which is called as a command line
 * process. On Windows-systems the folder containing the magick.exe file has to
 * be added to the PATH-variable. ImageMagick version has to be at least 7.0.8.
 * 
 * @author Florian Schlenker
 *
 */
public class ImageCompressor {

	private static final Logger log = LogManager.getLogger(ImageCompressor.class);

	/**
	 * This method creates one or several compression versions of a given texture
	 * file. For each desired version one can specify the maximum dimension. The
	 * resulting image(s) then will have at most this size in width and in height.
	 * If the dimension of the original texture file is smaller than a specified
	 * upper bound, the texture will not be scaled up. Instead the original file
	 * will just be copied to the output filename.
	 * 
	 * @param basePath
	 *            The absolute or relative path to the folder containing the input
	 *            texture file
	 * @param inputFilename
	 *            The filename of the texture file that shall be compressed (with
	 *            extension)
	 * @param outputFilenames
	 *            An array containing the filenames for each of the compressed
	 *            versions generated by this method. Its length has to be equal to
	 *            the length of the {@code upperBounds}-array.
	 * @param upperBounds
	 *            An array containing the upper bounds for each of the compressed
	 *            versions that shall be generated by this method
	 * @throws TextureCompressionException
	 *             Will be thrown, if an error in the ImageMagick-backend occurs
	 *             during the compression of the texture file
	 */
	public void compressTextureFile(File inputFilename, File[] outputFilenames, int[] upperBounds)
			throws TextureCompressionException {

		for (int i = 0; i < upperBounds.length; ++i) {
			try {
				executeResize(inputFilename, outputFilenames[i], upperBounds[i], upperBounds[i]);
			} catch (IOException | InterruptedException ex) {
				throw new TextureCompressionException(ex);
			}
		}
	}

	/**
	 * This method creates one or several compressed versions of a given image file.
	 * For each desired version one can specify the maximum dimension. The resulting
	 * image(s) then will have at most this size in width and in height. If the
	 * original file is smaller than the dimensions specified in a compression
	 * level, the original image will not be scaled up and no file for this
	 * compression level will be created.
	 * 
	 * @param inputFilename
	 *            The filename of the image file that shall be compressed (with
	 *            extension) including the path to that file
	 * @param outputFilenames
	 *            An array containing the filenames including the paths for each of
	 *            the compressed versions generated by this method. Its length has
	 *            to be equal to the length of the {@code levels}-array.
	 * @param levels
	 *            An array of @see ImageCompressionLevel objects determining the
	 *            desired compression levels regarding width, height and title of
	 *            each level.
	 * @param filter
	 *            This filter is called for each compression level. A file for a
	 *            level will only be created, if this filter function returns
	 *            {@code true}.
	 * @throws ImageCompressionException
	 *             Will be thrown, if the image file could not be read or if an
	 *             error in the ImageMagick-backend occurs during the compression of
	 *             the image file
	 */
	public void compressImageFile(File inputFilename, File[] outputFilenames, ImageCompressionLevel[] levels,
			AbstractCompressionLevelFilter filter) throws ImageCompressionException {
		try {
			BufferedImage bimg = ImageIO.read(inputFilename);
			int width = bimg.getWidth();
			int height = bimg.getHeight();
			for (int i = 0; i < levels.length; ++i) {
				if (filter.filterCompressionLevel(levels[i].getTitle())) {
					if (width > levels[i].getMaxWidth() || height > levels[i].getMaxHeight()) {
						try {
							executeResize(inputFilename, outputFilenames[i], levels[i].getMaxWidth(),
									levels[i].getMaxHeight());
						} catch (IOException | InterruptedException ex) {
							throw new ImageCompressionException(ex);
						}
					}
				}
			}
		} catch (IOException ex) {
			throw new ImageCompressionException(ex);
		}
	}

	private void executeResize(File inputPath, File outputPath, int sizeXUpperBound, int sizeYUpperBound)
			throws IOException, InterruptedException {
		Runtime rt = Runtime.getRuntime();
		Process pr;

		if (SystemUtils.IS_OS_WINDOWS) {
			log.debug("magick.exe convert " + inputPath + " -resize " + sizeXUpperBound + "x" + sizeYUpperBound + "> "
					+ outputPath);
			pr = rt.exec("magick.exe convert " + inputPath + " -resize " + sizeXUpperBound + "x" + sizeYUpperBound
					+ "> " + outputPath);

		} else {
			log.debug(
					"convert " + inputPath + " -resize " + sizeXUpperBound + "x" + sizeYUpperBound + "> " + outputPath);
			pr = rt.exec(
					"convert " + inputPath + " -resize " + sizeXUpperBound + "x" + sizeYUpperBound + "> " + outputPath);
		}

		pr.waitFor();
	}
}
