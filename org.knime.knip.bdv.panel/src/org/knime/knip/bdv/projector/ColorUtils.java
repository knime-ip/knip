package org.knime.knip.bdv.projector;

import net.imglib2.type.numeric.ARGBType;

/**
 * Utility methods regarding color mixing.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class ColorUtils {

	/**
	 * Alpha blend colors.
	 * 
	 * @param a
	 * @param b
	 * @return blended color
	 */
	public static int combineAlphaColors(int a, int b) {
		final int rA = ARGBType.red(a);
		final int rB = ARGBType.red(b);
		final int gA = ARGBType.green(a);
		final int gB = ARGBType.green(b);
		final int bA = ARGBType.blue(a);
		final int bB = ARGBType.blue(b);

		final double aA = ARGBType.alpha(a) / 255.0;
		final double aB = ARGBType.alpha(b) / 255.0;

		final int aTarget = (int) ((aA + aB - aA * aB) * 255);
		final int rTarget = (int) ((rA * aA) + (rB * aB * (1.0 - aA)));
		final int gTarget = (int) ((gA * aA) + (gB * aB * (1.0 - aA)));
		final int bTarget = (int) ((bA * aA) + (bB * aB * (1.0 - aA)));
		return ARGBType.rgba(rTarget, gTarget, bTarget, aTarget);
	}

	/**
	 * Add colors component wise.
	 * 
	 * Note: This is not a proper color-mix.
	 * 
	 * @param a
	 * @param b
	 * @return combined color.
	 */
	public static int combineColors(int a, int b) {
		final int rTarget = Math.min(255, (ARGBType.red(a) + ARGBType.red(b)));
		final int gTarget = Math.min(255, (ARGBType.green(a) + ARGBType.green(b)));
		final int bTarget = Math.min(255, (ARGBType.blue(a) + ARGBType.blue(b)));
		final int aTarget = Math.min(255, ARGBType.alpha(a) + ARGBType.alpha(b));
		return ARGBType.rgba(rTarget, gTarget, bTarget, aTarget);
	}

	/**
	 * Add color b on top of color a. If b is not fully opaque color a will shimmer
	 * through.
	 * 
	 * @param a
	 * @param b
	 * @return blended color
	 */
	public static int blendAlphaColors(int a, int b) {
		final double rA = ARGBType.red(a);
		final double rB = ARGBType.red(b);
		final double gA = ARGBType.green(a);
		final double gB = ARGBType.green(b);
		final double bA = ARGBType.blue(a);
		final double bB = ARGBType.blue(b);

		final double aA = ARGBType.alpha(a);
		final double aB = ARGBType.alpha(b);
		final double fac = aB / 255.0;

		final int rTarget = (int) Math.min(255, rA * (1.0 - fac) + rB * fac);
		final int gTarget = (int) Math.min(255, gA * (1.0 - fac) + gB * fac);
		final int bTarget = (int) Math.min(255, bA * (1.0 - fac) + bB * fac);
		final int aTarget = (int) Math.min(255, aA * (1.0 - fac) + aB * fac);
		return ARGBType.rgba(rTarget, gTarget, bTarget, aTarget);
	}
}
