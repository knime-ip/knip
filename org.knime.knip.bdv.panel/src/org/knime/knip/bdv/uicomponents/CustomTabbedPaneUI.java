package org.knime.knip.bdv.uicomponents;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.plaf.basic.BasicTabbedPaneUI;

/**
 * 
 * A custom colored tabbed pane UI.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class CustomTabbedPaneUI extends BasicTabbedPaneUI {

	@Override
	protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
		super.paintContentBorder(g, tabPlacement, selectedIndex);
	}

	@Override
	protected void installDefaults() {
		super.installDefaults();
		highlight = Color.lightGray;
		lightHighlight = Color.lightGray;
		shadow = Color.lightGray;
		darkShadow = Color.white;
		focus = Color.white;

	}

	@Override
	protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h,
			boolean isSelected) {
		g.setColor(Color.white);
		switch (tabPlacement) {
		case LEFT:
			g.fillRect(x + 1, y + 1, w - 1, h - 3);
			break;
		case RIGHT:
			g.fillRect(x, y + 1, w - 2, h - 3);
			break;
		case BOTTOM:
			g.fillRect(x + 1, y, w - 3, h - 1);
			break;
		case TOP:
		default:
			g.fillRect(x + 1, y + 1, w - 3, h - 1);
		}
	}

}
