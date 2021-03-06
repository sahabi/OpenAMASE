// ===============================================================================
// Authors: AFRL/RQQD
// Organization: Air Force Research Laboratory, Aerospace Systems Directorate, Power and Control Division
// 
// Copyright (c) 2017 Government of the United State of America, as represented by
// the Secretary of the Air Force.  No copyright is claimed in the United States under
// Title 17, U.S. Code.  All Other Rights Reserved.
// ===============================================================================

/*
 * GRAL: GRAphing Library for Java(R)
 *
 * (C) Copyright 2009-2013 Erich Seifert <dev[at]erichseifert.de>,
 * Michael Seifert <mseifert[at]error-reports.org>
 *
 * This file is part of GRAL.
 *
 * GRAL is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GRAL is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GRAL.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.erichseifert.gral.plots.points;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.text.Format;
import java.text.NumberFormat;

import de.erichseifert.gral.data.Row;
import de.erichseifert.gral.graphics.AbstractDrawable;
import de.erichseifert.gral.graphics.Drawable;
import de.erichseifert.gral.graphics.DrawableContainer;
import de.erichseifert.gral.graphics.DrawingContext;
import de.erichseifert.gral.graphics.OuterEdgeLayout;
import de.erichseifert.gral.plots.Label;
import de.erichseifert.gral.plots.axes.Axis;
import de.erichseifert.gral.plots.axes.AxisRenderer;
import de.erichseifert.gral.plots.colors.ColorMapper;
import de.erichseifert.gral.util.GraphicsUtils;
import de.erichseifert.gral.util.Location;
import de.erichseifert.gral.util.MathUtils;
import de.erichseifert.gral.util.PointND;

/**
 * Class that creates {@code Drawable}s for a row of data.
 */
public class DefaultPointRenderer2D extends AbstractPointRenderer {
	/** Version id for serialization. */
	private static final long serialVersionUID = -895832597380598383L;

	/**
	 * Returns the graphical representation to be drawn for the specified data
	 * value.
	 * @param data Information on axes, renderers, and values.
	 * @param shape Outline that describes the point's shape.
	 * @return Component that can be used to draw the point
	 */
	public Drawable getPoint(final PointData data, final Shape shape) {
		Drawable drawable = new AbstractDrawable() {
			/** Version id for serialization. */
			private static final long serialVersionUID = 1915778739867091906L;

			public void draw(DrawingContext context) {
				PointRenderer renderer = DefaultPointRenderer2D.this;

				Axis axisY = data.axes.get(1);
				AxisRenderer axisRendererY = data.axisRenderers.get(1);
				Row row = data.row;
				int col = data.col;

				ColorMapper colors = getColor();
				Paint paint = colors.get(row.getIndex());

				GraphicsUtils.fillPaintedShape(
					context.getGraphics(), shape, paint, null);

				if (renderer.isErrorVisible()) {
					int colErrorTop = renderer.getErrorColumnTop();
					int colErrorBottom = renderer.getErrorColumnBottom();
					drawErrorBars(context, shape,
						row, col, colErrorTop, colErrorBottom,
						axisY, axisRendererY);
				}
			}
		};

		return drawable;
	}

	/**
	 * Draws the specified value label for the specified shape.
	 * @param context Environment used for drawing.
	 * @param point Point shape used to layout the label.
	 * @param row Data row containing the point.
	 * @param col Index of the column that will be projected on the axis.
	 */
	protected void drawValueLabel(DrawingContext context,
			Shape point, Row row, int col) {
		Comparable<?> value = row.get(col);

		// Formatting
		Format format = getValueFormat();
		if ((format == null) && row.isColumnNumeric(col)) {
			format = NumberFormat.getInstance();
		}

		// Text to display
		String text = (format != null) ? format.format(value) : value.toString();

		// Visual settings
		ColorMapper colors = getValueColor();
		Paint paint = colors.get(row.getIndex());
		Font font = getValueFont();
		double fontSize = font.getSize2D();

		// Layout settings
		Location location = getValueLocation();
		double alignX = getValueAlignmentX();
		double alignY = getValueAlignmentY();
		double rotation = getValueRotation();
		double distance = getValueDistance();
		if (MathUtils.isCalculatable(distance)) {
			distance *= fontSize;
		} else {
			distance = 0.0;
		}

		// Create a label with the settings
		Label label = new Label(text);
		label.setAlignmentX(alignX);
		label.setAlignmentY(alignY);
		label.setRotation(rotation);
		label.setColor(paint);
		label.setFont(font);

		Rectangle2D boundsPoint = point.getBounds2D();
		DrawableContainer labelContainer =
			new DrawableContainer(new OuterEdgeLayout(distance));
		labelContainer.add(label, location);

		labelContainer.setBounds(boundsPoint);
		labelContainer.draw(context);
	}

	/**
	 * Draws error bars.
	 * @param context Environment used for drawing.
	 * @param point Shape of the point.
	 * @param row Data row containing the point.
	 * @param col Index of the column that will be projected on the axis.
	 * @param colErrorTop Index of the column that contains the upper error value.
	 * @param colErrorBottom Index of the column that contains the lower error value.
	 * @param axis Axis.
	 * @param axisRenderer Axis renderer.
	 */
	protected void drawErrorBars(DrawingContext context, Shape point,
			Row row, int col, int colErrorTop, int colErrorBottom,
			Axis axis, AxisRenderer axisRenderer) {
		if (axisRenderer == null) {
			return;
		}

		if (colErrorTop < 0 || colErrorTop >= row.size() ||
				!row.isColumnNumeric(colErrorTop) ||
				colErrorBottom < 0 || colErrorBottom >= row.size() ||
				!row.isColumnNumeric(colErrorBottom)) {
			return;
		}

		Number value = (Number) row.get(col);
		Number errorTop = (Number) row.get(colErrorTop);
		Number errorBottom = (Number) row.get(colErrorBottom);
		if (!MathUtils.isCalculatable(value) ||
				!MathUtils.isCalculatable(errorTop) ||
				!MathUtils.isCalculatable(errorBottom)) {
			return;
		}

		Graphics2D graphics = context.getGraphics();
		AffineTransform txOld = graphics.getTransform();

		// Calculate positions
		PointND<Double> pointValue = axisRenderer.getPosition(axis,
			value, true, false);
		PointND<Double> pointTop = axisRenderer.getPosition(axis,
				value.doubleValue() + errorTop.doubleValue(), true, false);
			PointND<Double> pointBottom = axisRenderer.getPosition(axis,
					value.doubleValue() - errorBottom.doubleValue(), true, false);
		if (pointValue == null || pointTop == null || pointBottom == null) {
			return;
		}
		double posY = pointValue.get(PointND.Y);
		double posYTop = pointTop.get(PointND.Y) - posY;
		double posYBottom = pointBottom.get(PointND.Y) - posY;

		// Draw the error bar
		Line2D errorBar = new Line2D.Double(0.0, posYTop, 0.0, posYBottom);
		ColorMapper colors = getErrorColor();
		Paint errorPaint = colors.get(row.getIndex());
		Stroke errorStroke = getErrorStroke();
		GraphicsUtils.drawPaintedShape(
			graphics, errorBar, errorPaint, null, errorStroke);

		// Draw the shapes at the end of the error bars
		Shape endShape = getErrorShape();
		graphics.translate(0.0, posYTop);
		Stroke endShapeStroke = new BasicStroke(1f);
		GraphicsUtils.drawPaintedShape(
			graphics, endShape, errorPaint, null, endShapeStroke);
		graphics.setTransform(txOld);
		graphics.translate(0.0, posYBottom);
		GraphicsUtils.drawPaintedShape(
			graphics, endShape, errorPaint, null, endShapeStroke);
		graphics.setTransform(txOld);
	}

	/**
	 * Returns a {@code Shape} instance that can be used for further
	 * calculations.
	 * @param data Information on axes, renderers, and values.
	 * @return Outline that describes the point's shape.
	 */
	public Shape getPointShape(PointData data) {
		return getShape();
	}

	/**
	 * Returns a graphical representation of the value label to be drawn for
	 * the specified data value.
	 * @param data Information on axes, renderers, and values.
	 * @param shape Outline that describes the bounds for the value label.
	 * @return Component that can be used to draw the value label.
	 */
	public Drawable getValue(final PointData data, final Shape shape) {
		Drawable drawable = new AbstractDrawable() {
			/** Version id for serialization. */
			private static final long serialVersionUID = -2568531344817590175L;

			public void draw(DrawingContext context) {
				PointRenderer renderer = DefaultPointRenderer2D.this;
				Row row = data.row;

				if (renderer.isValueVisible()) {
					int colValue = renderer.getValueColumn();
					drawValueLabel(context, shape, row, colValue);
				}
			}
		};
		return drawable;
	}
}
