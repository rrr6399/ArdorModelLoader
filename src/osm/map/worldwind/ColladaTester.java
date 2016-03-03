/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package osm.map.worldwind;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.awt.WorldWindowGLJPanel;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.ogc.collada.ColladaRoot;
import gov.nasa.worldwind.ogc.collada.impl.ColladaController;
import java.io.IOException;
import javafx.application.Application;
import javafx.embed.swing.SwingNode;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import jdk.internal.util.xml.XMLStreamException;
import static javafx.application.Application.launch;

/**
 *
 * @author Rob
 */
public class ColladaTester extends Application {

	private static final double HOME_LAT = 44.72;
	private static final double HOME_LON = -0.5;
	private static final double CAMERA_ALT = 10_000.0;
	private static final double AIRCRAFT_ALT = 3_000.0;

	private final WorldWindowGLJPanel _ww = new WorldWindowGLJPanel();
	private final ColladaRoot _aircraft;
	private /* */ double _scale = 1.0;

	public ColladaTester() throws IOException, XMLStreamException, javax.xml.stream.XMLStreamException {
		_aircraft = ColladaRoot.createAndParse(this.getClass().getResource("spit.dae"));
		_aircraft.setAltitudeMode(WorldWind.ABSOLUTE);
		_aircraft.setModelScale(new Vec4(_scale));
		_aircraft.setPosition(
			Position.fromDegrees(HOME_LAT, HOME_LON, AIRCRAFT_ALT));
		_ww.setModel(new BasicModel());
		_ww.getView().setEyePosition(
			Position.fromDegrees(HOME_LAT, HOME_LON, CAMERA_ALT));
		final RenderableLayer layer = new RenderableLayer();
		layer.addRenderable(new ColladaController(_aircraft));
		_ww.getModel().getLayers().add(layer);
	}

	private void scaleIt() {
		_scale *= 2.0;
		_aircraft.setModelScale(new Vec4(_scale));
		_ww.redraw();
		System.err.println(_scale);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		final SwingNode rootPane = new SwingNode();
		rootPane.setContent(_ww);
		final Button scaleIt = new Button("Scale Aircraft");
//		final Scene scene = new Scene( new BorderPane(rootPane, scaleIt, null, null, null));
		scaleIt.setMaxWidth(Double.MAX_VALUE);
		scaleIt.setOnAction(e -> scaleIt());
//		primaryStage.setScene(scene);
		primaryStage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}

}
