
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.triangulate.IncrementalDelaunayTriangulator;
import com.vividsolutions.jts.triangulate.quadedge.QuadEdge;
import com.vividsolutions.jts.triangulate.quadedge.QuadEdgeSubdivision;
import com.vividsolutions.jts.triangulate.quadedge.TriangleVisitor;
import com.vividsolutions.jts.triangulate.quadedge.Vertex;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import javax.swing.JFrame;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.opengis.referencing.operation.TransformException;
import org.thema.common.Config;
import org.thema.common.JTS;
import org.thema.data.IOImage;
import org.thema.data.feature.DefaultFeature;
import org.thema.drawshape.image.RasterShape;
import org.thema.drawshape.layer.DefaultGroupLayer;
import org.thema.drawshape.layer.FeatureLayer;
import org.thema.drawshape.layer.RasterLayer;
import org.thema.drawshape.style.RasterStyle;
import org.thema.drawshape.ui.MapViewer;
import org.thema.parallel.AbstractParallelTask;
import org.thema.parallel.ExecutorService;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author gvuidel
 */
public class TIN {
    
    public static final class Err {
        int bestInd = -1;
        double max = -Double.MAX_VALUE;
        double sum = 0;
    }
    
    public static void main(String [] args) throws TransformException, IOException {
        Config.setNodeClass(TIN.class);
        Config.setParallelProc(8);
        long start = System.currentTimeMillis();
//        GridCoverage2D cov = IOImage.loadCoverage(new File("/home/gvuidel/data_PImage/Fichiers_IDF/Petite_zone/mnt.tif"));
        GridCoverage2D cov = IOImage.loadCoverage(new File("/home/gvuidel/data_PImage/Fichiers_IDF/Zone_complete/mnt.tif"));
        
        //GridCoverage2D cov = IOImage.loadCoverage(new File("/home/gvuidel/Documents/PImage/carre.asc"));
        Raster r = cov.getRenderedImage().getData();
        final float[] demBuf = ((DataBufferFloat)r.getDataBuffer()).getData();
        
        GridCoverage2D mne = IOImage.loadCoverage(new File("/home/gvuidel/data_PImage/Fichiers_IDF/Zone_complete/mne.tif"));
        final float[] mneBuf = ((DataBufferFloat)mne.getRenderedImage().getData().getDataBuffer()).getData();
        for(int i = 0; i < demBuf.length; i++)
            demBuf[i] += mneBuf[i];
        
        GridGeometry2D grid = cov.getGridGeometry();
        Envelope env = JTS.rectToEnv(grid.getGridRange2D());
        //env.expandBy(100);
        QuadEdgeSubdivision div = new QuadEdgeSubdivision(env, 0.1);
        IncrementalDelaunayTriangulator tri = new IncrementalDelaunayTriangulator(div);
        
        
        tri.insertSite(new Vertex(0, r.getHeight()-1, r.getSampleDouble(0, r.getHeight()-1, 0)));
        tri.insertSite(new Vertex(0, 0, r.getSampleDouble(0, 0, 0)));
        tri.insertSite(new Vertex(r.getWidth()-1, 0, r.getSampleDouble(r.getWidth()-1, 0, 0)));
        tri.insertSite(new Vertex(r.getWidth()-1, r.getHeight()-1, r.getSampleDouble(r.getWidth()-1, r.getHeight()-1, 0)));
        
        WritableRaster estim = r.createCompatibleWritableRaster();
        float[] estimBuf = ((DataBufferFloat)estim.getDataBuffer()).getData();
        final float [] errBuf = new float[estimBuf.length];
        int w = estim.getWidth();
        Envelope envUpdate = env;
        double errSum = Double.MAX_VALUE;
        double errMax = Double.MAX_VALUE;
        while(errMax > 2) {
            long t1 = System.currentTimeMillis();
            errSum = 0;
            GridCoordinates2D best = new GridCoordinates2D();
            GridCoordinates2D best2 = new GridCoordinates2D();
            double errMax2 = -Double.MAX_VALUE;
            final int x2 = (int) envUpdate.getMaxX();
            final int y2 = (int) envUpdate.getMaxY();
            for(int y = (int) envUpdate.getMinY(); y < y2; y++)
                for(int x = (int) envUpdate.getMinX(); x < x2; x++) {
                    double z = demBuf[y*w+x];
                    QuadEdge edge = div.locate(new Coordinate(x, y));

                    Vertex v1 = edge.orig();
                    Vertex v2 = edge.dest();
                    Vertex v3 = edge.lPrev().orig();
                    if(Double.isNaN(v3.getZ()))
                        v3.setZ(0);
                    double zEstim = new Vertex(x, y, z).interpolateZValue(v1, v2, v3);
                    if(zEstim > v1.getZ() && zEstim > v2.getZ() && zEstim > v3.getZ() ||
                           zEstim < v1.getZ() && zEstim < v2.getZ() && zEstim < v3.getZ())
                        throw new RuntimeException();
//                        System.err.println("err point en dehors du triangle");
                    estimBuf[y*w+x] = (float) zEstim;
                    double err = Math.abs(z-zEstim);
                    errBuf[y*w+x] = (float) err;
                    
                    if(err > errMax2) {
                        errMax2 = err;
                        best2.x = x;
                        best2.y = y;
                    }
                }
            long t2 = System.currentTimeMillis();
            if(errMax2 >= errMax) {
                errMax = errMax2;
                best.x = best2.x;
                best.y = best2.y;
            } else {
                AbstractParallelTask<Err, Err> task = new AbstractParallelTask<Err, Err>() {
                    Err result = new Err();
                    @Override
                    public int getSplitRange() {
                        return errBuf.length;
                    }
                    @Override
                    public Err execute(int start, int end) {
                        Err res = new Err();
                        res.max = result.max;
                        for(int i = start; i < end; i++) {
                            final double err = errBuf[i];
                            if(err > res.max) {
                                res.max = err;
                                res.bestInd = i;
                            }
                            res.sum += err;
                        }
                        return res;
                    }
                    @Override
                    public void gather(Err res) {
                        if(res.max > result.max) {
                            result.max = res.max;
                            result.bestInd = res.bestInd;
                        }
                        result.sum += res.sum;
                    }
                    @Override
                    public Err getResult() {
                        return result;
                    }
                };
                ExecutorService.execute(task);
                errMax = task.getResult().max;
                errSum = task.getResult().sum;
                best.x = task.getResult().bestInd % w;
                best.y = task.getResult().bestInd / w;
//                errMax = -Double.MAX_VALUE;
//                for(int i = 0; i < demBuf.length; i++) {
//                    final double err = errBuf[i];
//                    if(err > errMax) {
//                        errMax = err;
//                        best.x = i%w;
//                        best.y = i/w;
//                    }
//                    errSum += err;
//                }
            }
            System.out.println("ErrMax : " +errMax + " - Sum : " + errSum);
            double z = r.getSampleDouble(best.x, best.y, 0);
            Vertex v = new Vertex(best.x, best.y, z);
            QuadEdge edge = tri.insertSite(v);
            if(edge.dest().equals(v))
                edge = edge.sym();
            envUpdate = new Envelope(edge.toLineSegment().p0, edge.toLineSegment().p1);
            QuadEdge e = edge.oNext();
            while(e != edge) {
                envUpdate.expandToInclude(e.dest().getX(), e.dest().getY());
                e = e.oNext();
            }
            
            envUpdate.expandBy(1);
            envUpdate = envUpdate.intersection(env);
            System.out.println("Update : " + (t2-t1) + " - Search/Add : " + (System.currentTimeMillis()-t2));
        }
        
        Geometry triangulation = div.getTriangles(new GeometryFactory());
        System.out.println("NB triangles : " + triangulation.getNumGeometries());
        System.out.println("Temps total : " + (System.currentTimeMillis()-start)/1000);
        
        TriangleCoordinatesVisitor visitor = new TriangleCoordinatesVisitor();
        visitTriangles(div, visitor, div.locate(env.centre()), triangulation.getNumGeometries());
	
        List triPtsList = visitor.getTriangles();
        Polygon[] tris = new Polygon[triPtsList.size()];
        GeometryFactory geomFact = new GeometryFactory();
        int i = 0;
        for (Iterator it = triPtsList.iterator(); it.hasNext();) {
                Coordinate[] triPt = (Coordinate[]) it.next();
                tris[i++] = geomFact
                                .createPolygon(geomFact.createLinearRing(triPt), null);
        }
        triangulation = geomFact.createGeometryCollection(tris);
        triangulation = org.geotools.geometry.jts.JTS.transform(triangulation, grid.getGridToCRS2D());
        
        
        List<DefaultFeature> triangles = new ArrayList<>();
        for(i = 0; i < triangulation.getNumGeometries(); i++)
            triangles.add(new DefaultFeature(i, triangulation.getGeometryN(i)));
        MapViewer mapViewer = new MapViewer();
        DefaultGroupLayer gl = new DefaultGroupLayer("", true);
        gl.addLayerFirst(new RasterLayer("Estim", new RasterShape(estim, cov.getEnvelope2D(), new RasterStyle(), true)));
        //gl.addLayerFirst(new GeometryLayer("Tri", triangulation));
        gl.addLayerFirst(new FeatureLayer("Tri", triangles));
        mapViewer.setRootLayer(gl);
        JFrame frm = new JFrame();
        frm.getContentPane().add(mapViewer);
        frm.setVisible(true);
        frm.pack();
        
    }
    
    
    
    public static void visitTriangles(QuadEdgeSubdivision div, TriangleVisitor triVisitor, QuadEdge startingEdge, int nbTri) {

            // visited flag is used to record visited edges of triangles
            // setVisitedAll(false);
            Stack<QuadEdge> edgeStack = new Stack<>();
            edgeStack.push(startingEdge);

            Set<QuadEdge> visitedEdges = new HashSet<>();
            int nb = 0;
            while (nb < nbTri && !edgeStack.empty()) {
                    QuadEdge edge = (QuadEdge) edgeStack.pop();
                    if (! visitedEdges.contains(edge)) {
                            QuadEdge[] triEdges = fetchTriangleToVisit(div, edge, edgeStack, visitedEdges);
                            if (triEdges != null){
                                    triVisitor.visit(triEdges);
                                    nb++;
                            }
                    }
            }
    }

	/**
	 * The quadedges forming a single triangle.
   * Only one visitor is allowed to be active at a
	 * time, so this is safe.
	 */
	private static QuadEdge[] triEdges = new QuadEdge[3];

	/**
	 * Stores the edges for a visited triangle. Also pushes sym (neighbour) edges
	 * on stack to visit later.
	 * 
	 * @param edge
	 * @param edgeStack
	 * @param includeFrame
	 * @return the visited triangle edges
	 * or null if the triangle should not be visited (for instance, if it is
	 *         outer)
	 */
	private static QuadEdge[] fetchTriangleToVisit(QuadEdgeSubdivision div, QuadEdge edge, Stack<QuadEdge> edgeStack, Set<QuadEdge> visitedEdges) {
		QuadEdge curr = edge;
		int edgeCount = 0;
		boolean isFrame = false;
		do {
			triEdges[edgeCount] = curr;

			if (div.isFrameEdge(curr))
				isFrame = true;
			
			// push sym edges to visit next
			QuadEdge sym = curr.sym();
			if (! visitedEdges.contains(sym) && !div.isFrameEdge(curr))
				edgeStack.push(sym);
			
			// mark this edge as visited
                        if (!div.isFrameEdge(curr))
                            visitedEdges.add(curr);
			
			edgeCount++;
			curr = curr.lNext();
		} while (curr != edge);

		if (isFrame)
			return null;
		return triEdges;
	}
        
        
        private static class TriangleCoordinatesVisitor implements TriangleVisitor {
		private CoordinateList coordList = new CoordinateList();

		private List triCoords = new ArrayList();

		public TriangleCoordinatesVisitor() {
		}

		public void visit(QuadEdge[] triEdges) {
			coordList.clear();
			for (int i = 0; i < 3; i++) {
				Vertex v = triEdges[i].orig();
				coordList.add(v.getCoordinate());
			}
			if (coordList.size() > 0) {
				coordList.closeRing();
				Coordinate[] pts = coordList.toCoordinateArray();
				if (pts.length != 4) {
					//checkTriangleSize(pts);
					return;
				}

				triCoords.add(pts);
			}
		}

		
		public List getTriangles() {
			return triCoords;
		}
	}
}
