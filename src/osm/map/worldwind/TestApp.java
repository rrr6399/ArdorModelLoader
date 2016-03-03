package osm.map.worldwind;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwindx.examples.ApplicationTemplate;
import static gov.nasa.worldwindx.examples.ApplicationTemplate.insertBeforeCompass;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;

public class TestApp extends ApplicationTemplate {

	public static class AppFrame extends ApplicationTemplate.AppFrame {
		ArdorModel model;
		Position pos;
		RenderableLayer layer;
		Timer timer;
		double alt = 300;

		public AppFrame() {

			String model550="C:/RaptorX/projects/models/djif550mitGoPro/model.dae";
			String model950="C:/RaptorX/projects/WorldWind-Ardor3D/ArdorModelLoader/models/obj/Hexa950modOBJ/Hexa950mod.obj";
			layer = new RenderableLayer();
//			pos = Position.fromDegrees(30, -100, alt);
			pos = Position.fromDegrees(35.77704,-120.80598,alt);
			this.model = new ArdorModel(
				model950,
				pos
			);
			model.setSize(5);
			model.setPitch(90);
			layer.addRenderable(model);
			insertBeforeCompass(getWwd(), layer);

			this.timer = new Timer(1000, new ActionListener() {
				boolean first = true;
				@Override
				public void actionPerformed(ActionEvent e) {
					if(first) {
						gotoPos();											
						first =false;
					} else {
						updatePosition();
					}
				}
			});
			timer.start();
		}

		private void gotoPos() {
			this.getWwd().getView().goTo(pos, alt);
		}

		private void updatePosition() {
//			pos = pos.add(Position.fromDegrees(.01, .01));
			model.setPosition(pos);
//			model.setYaw(model.getYaw()+45); // roll
//			model.setRoll(model.getRoll()+45);
//			model.setPitch(model.getPitch()+45);
//			model.setPitch(model.getPitch()+(Math.random()-.5)*5);
			layer.firePropertyChange(AVKey.LAYER,null,this);
			//this.getWwd().redraw();
//			timer.stop();
		}


		protected RenderableLayer getLayer() {
			for (Layer layer : getWwd().getModel().getLayers()) {
				if (layer.getName().contains("Renderable")) {
					return (RenderableLayer) layer;
				}
			}

			return null;
		}

	}

	public static void main(String[] args) {
		ApplicationTemplate.start("World Wind Cones", AppFrame.class);
	}
}
