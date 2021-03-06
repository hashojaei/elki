/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.visualization.visualizers.parallel.selection;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.result.RangeSelection;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ParallelPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.parallel.AbstractParallelVisualization;

/**
 * Visualizer for generating an SVG-Element representing the selected range.
 *
 * @author Robert Rödler
 * @since 0.5.0
 *
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class SelectionAxisRangeVisualization extends AbstractVisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME = "Selection Axis Range";

  /**
   * Constructor.
   */
  public SelectionAxisRangeVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    Hierarchy.Iter<ParallelPlotProjector<?>> it = VisualizationTree.filter(context, start, ParallelPlotProjector.class);
    for(; it.valid(); it.advance()) {
      ParallelPlotProjector<?> p = it.get();
      Relation<?> rel = p.getRelation();
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        continue;
      }
      final VisualizationTask task = new VisualizationTask(NAME, context, context.getSelectionResult(), rel, SelectionAxisRangeVisualization.this);
      task.level = VisualizationTask.LEVEL_DATA - 1;
      task.addUpdateFlags(VisualizationTask.ON_SELECTION);
      context.addVis(context.getSelectionResult(), task);
      context.addVis(p, task);
    }
  }

  /**
   * Instance
   *
   * @author Robert Rödler
   *
   * @apiviz.has RangeSelection oneway - - visualizes
   */
  public class Instance extends AbstractParallelVisualization<NumberVector> {
    /**
     * CSS Class for the range marker
     */
    public static final String MARKER = "selectionAxisRange";

    /**
     * Constructor.
     *
     * @param task Visualization task
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     * @param proj Projection
     */
    public Instance(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
      super(task, plot, width, height, proj);
      addListeners();
    }

    /**
     * Adds the required CSS-Classes
     *
     * @param svgp SVG-Plot
     */
    private void addCSSClasses(SVGPlot svgp) {
      final StyleLibrary style = context.getStyleLibrary();
      // Class for the cube
      if(!svgp.getCSSClassManager().contains(MARKER)) {
        CSSClass cls = new CSSClass(this, MARKER);
        cls.setStatement(SVGConstants.CSS_STROKE_VALUE, style.getColor(StyleLibrary.SELECTION));
        cls.setStatement(SVGConstants.CSS_STROKE_OPACITY_PROPERTY, style.getOpacity(StyleLibrary.SELECTION));
        cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT));
        cls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
        cls.setStatement(SVGConstants.CSS_STROKE_LINEJOIN_PROPERTY, SVGConstants.CSS_ROUND_VALUE);

        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, style.getColor(StyleLibrary.SELECTION));
        cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, style.getOpacity(StyleLibrary.SELECTION));

        svgp.addCSSClassOrLogError(cls);
      }
    }

    @Override
    public void fullRedraw() {
      super.fullRedraw();
      addCSSClasses(svgp);
      DBIDSelection selContext = context.getSelection();
      if(!(selContext instanceof RangeSelection)) {
        return;
      }
      HyperBoundingBox range = ((RangeSelection) selContext).getRanges();
      if(range == null) {
        return;
      }

      // Project:
      final int dims = range.getDimensionality();
      double[] min = new double[dims];
      double[] max = new double[dims];
      for(int d = 0; d < dims; d++) {
        min[d] = range.getMin(d);
        max[d] = range.getMax(d);
      }
      min = proj.fastProjectDataToRenderSpace(min);
      max = proj.fastProjectDataToRenderSpace(max);

      final int vdim = proj.getVisibleDimensions();
      for(int vd = 0; vd < vdim; vd++) {
        final int ad = proj.getDimForVisibleAxis(vd);
        final double amin = Math.min(min[ad], max[ad]);
        final double amax = Math.max(min[ad], max[ad]);
        if(amin > Double.MIN_VALUE && amax < Double.MAX_VALUE) {
          Element rect = svgp.svgRect(getVisibleAxisX(vd) - (0.01 * StyleLibrary.SCALE), amin, 0.02 * StyleLibrary.SCALE, amax - amin);
          SVGUtil.addCSSClass(rect, MARKER);
          layer.appendChild(rect);
        }
      }
    }
  }
}