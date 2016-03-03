package osm.map.worldwind;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.util.Logging;

import java.util.concurrent.atomic.AtomicReference;

import com.ardor3d.framework.Scene;
import com.ardor3d.framework.jogl.JoglCanvasRenderer;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.math.Ray3;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRendererFactory;
import com.ardor3d.renderer.jogl.JoglRenderer;
import com.ardor3d.renderer.jogl.JoglTextureRendererProvider;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.TextureStateRecord;
import com.ardor3d.renderer.state.record.TextureUnitRecord;
import com.ardor3d.scenegraph.Node;
import com.jogamp.opengl.GL2;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Renderable;

/**
 * http://forum.worldwindcentral.com/showthread.php?45896-Collada-models-with-lighting&p=124893#post124893 
 * @author robotfire
 */
public class ArdorModel implements Renderable, Scene {

	private Position position;
	private String modelSource;
	private double yaw = 0.0;
	private double roll = 0.0;
	private double pitch = 0.0;
	private boolean keepConstantSize = true;
	private double size = 1;
	public boolean clamp = false;
	boolean useLighting;
	boolean useTexture;
	boolean renderPicker;

	private final AtomicReference<Node> nodeRef = new AtomicReference<>();
	private JoglCanvasRenderer renderer;

	private boolean visible = true;

	public ArdorModel(String path, Position pos) {
		try {
			this.modelSource = ArdorModelLoader.createModel(path);
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.setPosition(pos);
		this.useLighting = true;
		this.useTexture = true;
	}

	public void clamp() {
		this.clamp = true;
	}

	@Override
	public void render(DrawContext dc) {
		if (dc == null) {
			String message = Logging.getMessage("nullValue.DrawContextIsNull");
			Logging.logger().severe(message);
			throw new IllegalStateException(message);
		}

		if (!this.isVisible()) {
			return;
		}

		try {
			beginDraw(dc);
			if (dc.isPickingMode()) {
				this.renderPicker = true;
			} else {
				this.renderPicker = false;
			}
			draw(dc);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			endDraw(dc);
		}
	}

	protected void draw(DrawContext dc) {
		GL2 gl = dc.getGL().getGL2();

		Vec4 loc;
		if (clamp) {
			loc = dc.computeTerrainPoint(position.latitude, position.longitude, 0);
		} else {
			loc = dc.getGlobe().computePointFromPosition(position, position.elevation * dc.getVerticalExaggeration()
			);
		}
		double localSize = this.computeSize(dc, loc);

		if (dc.getView().getFrustumInModelCoordinates().contains(loc)) {
			dc.getView().pushReferenceCenter(dc, loc);
			gl.glRotated(position.getLongitude().degrees, 0, 1, 0);
			gl.glRotated(-position.getLatitude().degrees, 1, 0, 0);

//			gl.glRotated(yaw, 0, 0, 1);
//			gl.glRotated(roll, 0, 1, 0);
//			gl.glRotated(pitch, 1, 0, 0);

			gl.glRotated(yaw, 0, 0, 1);
			gl.glRotated(pitch, 1, 0, 0);
			gl.glRotated(roll, 0, 1, 0);

			gl.glScaled(localSize, localSize, localSize);

			drawArdor(dc);
			dc.getView().popReferenceCenter(dc);
		}
	}

	private boolean requestedLoad = false;

	private void drawArdor(DrawContext dc) {
		ArdorModelLoader.initializeArdorSystem(dc);
		Node node = this.nodeRef.get();

		if (node == null && !requestedLoad) {
			if (!WorldWind.getTaskService().isFull()) {
				initialize(dc, modelSource); //set the local variable node
				WorldWind.getTaskService().addTask(new LoadModelTask(modelSource));
				requestedLoad = true;
			} else {
				System.err.println("Task queue is full, delay model load");
			}
		}

		if (node != null) {
			GL2 gl = dc.getGL().getGL2();
			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glPushMatrix();
			gl.glPushAttrib(GL2.GL_TEXTURE_BIT | GL2.GL_LIGHTING_BIT);

			if (useTexture) {
				gl.glEnable(GL2.GL_TEXTURE_2D);
				gl.glEnable(GL2.GL_BLEND);
				gl.glEnable(GL2.GL_RESCALE_NORMAL);
			} else {
				gl.glDisable(GL2.GL_TEXTURE_2D);
				gl.glDisable(GL2.GL_BLEND);
			}

			final RenderContext context = ContextManager.getCurrentContext();
			final ContextCapabilities caps = context.getCapabilities();

			{
				final TextureStateRecord record = (TextureStateRecord) context.getStateRecord(StateType.Texture);
				for (int i = 0; i < caps.getNumberOfTotalTextureUnits(); i++) {
					TextureUnitRecord unitRecord = record.units[i];
					unitRecord.invalidate();
				}
			}

			node.draw(renderer.getRenderer());
			renderer.getRenderer().renderBuckets();

			gl.glPopAttrib();
			gl.glPopMatrix();

		}
	}

	private class LoadModelTask
		implements Runnable {

		private final String source;

		LoadModelTask(String source) {
			this.source = source;
		}

		@Override
		public void run() {
			try {
				Node node = ArdorModelLoader.loadModel(modelSource);
				nodeRef.set(node);
			} catch (Exception e) {
				System.err.println("Failed to load model: " + e);
			}
		}
	}

	private void initialize(DrawContext dc, String model) {
		try {
			renderer = new JoglCanvasRenderer(this) {
				JoglRenderer joglRenderer = new JoglRenderer();

				@Override
				public Renderer getRenderer() {
					return joglRenderer;
				}
			};

			TextureRendererFactory.INSTANCE.setProvider(new JoglTextureRendererProvider());
		} catch (Exception e) {
			System.err.println("Failed to load model: " + e);
		}
	}

// puts opengl in the correct state for this layer
	protected void beginDraw(DrawContext dc) {
		GL2 gl = dc.getGL().getGL2();
		Vec4 cameraPosition = dc.getView().getEyePoint();
		gl.glPushAttrib(
			GL2.GL_TEXTURE_BIT
			| GL2.GL_COLOR_BUFFER_BIT
			| GL2.GL_DEPTH_BUFFER_BIT
			| GL2.GL_HINT_BIT
			| GL2.GL_POLYGON_BIT
			| GL2.GL_ENABLE_BIT
			| GL2.GL_CURRENT_BIT
			| GL2.GL_LIGHTING_BIT
			| GL2.GL_TRANSFORM_BIT
			| GL2.GL_CLIENT_VERTEX_ARRAY_BIT);
//float[] lightPosition = {0F, 100000000f, 0f, 0f};
		float[] light1Position = {(float) (cameraPosition.x + 1000), (float) (cameraPosition.y + 1000), (float) (cameraPosition.z + 1000), 1.0f};
//		float[] light1Position = {(float) (cameraPosition.x ), (float) (cameraPosition.y), (float) (cameraPosition.z + 1000), 1.0f};
		float[] light2Position = {(float) (cameraPosition.x + 1000), (float) (cameraPosition.y + 1000), (float) (cameraPosition.z - 1000), 1.0f};
		/**
		 * Ambient light array
		 */
		float[] lightAmbient = {0.4f, 0.4f, 0.4f, 0.4f};
		/**
		 * Diffuse light array
		 */
		float[] lightDiffuse = {1.0f, 1.0f, 1.0f, 1.0f};
		/**
		 * Specular light array
		 */
		float[] lightSpecular = {1.0f, 1.0f, 1.0f, 1.0f};
		float[] model_ambient = {0.5f, 0.5f, 0.5f, 1.0f};
		gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, model_ambient, 0);
		gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_POSITION, light1Position, 0);
		gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_POSITION, light2Position, 0);
		gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_DIFFUSE, lightDiffuse, 0);
		gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_AMBIENT, lightAmbient, 0);
		gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_SPECULAR, lightSpecular, 0);
//		gl.glDisable(GL2.GL_LIGHT0);
		gl.glEnable(GL2.GL_LIGHT0);

		gl.glEnable(GL2.GL_LIGHT1);
		gl.glEnable(GL2.GL_LIGHTING);
		gl.glEnable(GL2.GL_NORMALIZE);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPushMatrix();
	}

// resets opengl state
	protected void endDraw(DrawContext dc) {
		GL2 gl = dc.getGL().getGL2();
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPopMatrix();
		gl.glPopAttrib();
	}

	private double computeSize(DrawContext dc, Vec4 loc) {
		if (this.keepConstantSize) {
			return size;
		}
		if (loc == null) {
			System.err.println("Null location when computing size of model");
			return 1;
		}
		double d = loc.distanceTo3(dc.getView().getEyePoint());
		double newSize = 60 * dc.getView().computePixelSizeAtDistance(d);
		if (newSize < 2) {
			newSize = 2;
		}
		return newSize;
	}

	/*protected Vec4 computeReferenceCenter(DrawContext dc) {
return this.computeTerrainPoint(dc,
this.getPosition().getLatitude(), this.getPosition().getLongitude());
}*/
	protected final Vec4 computeTerrainPoint(DrawContext dc, Angle lat, Angle lon) {
		Vec4 p = dc.getSurfaceGeometry().getSurfacePoint(lat, lon);
		if (p == null) {
			p = dc.getGlobe().computePointFromPosition(lat, lon,
				dc.getGlobe().getElevation(lat, lon) * dc.getVerticalExaggeration()
			);
		}
		return p;
	}

	public boolean isConstantSize() {
		return keepConstantSize;
	}

	public void setKeepConstantSize(boolean val) {
		this.keepConstantSize = val;
	}

	public double getSize() {
		return size;
	}

	public void setSize(double size) {
		this.size = size;
	}

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	public String getModel() {
		return modelSource;
	}

	public double getYaw() {
		return yaw;
	}

	public void setYaw(double val) {
		this.yaw = val;
	}

	public double getRoll() {
		return roll;
	}

	public void setRoll(double val) {
		this.roll = val;
	}

	public double getPitch() {
		return pitch;
	}

	public void setPitch(double val) {
		this.pitch = val;
	}

	public boolean isVisible() {
		return this.visible;
	}

	public void setVisible(boolean v) {
		this.visible = v;;
	}

	@Override
	public boolean renderUnto(Renderer renderer) {
// TODO Auto-generated method stub
		return false;
	}

	@Override
	public PickResults doPick(Ray3 pickRay) {
// TODO Auto-generated method stub
		return null;
	}

}
