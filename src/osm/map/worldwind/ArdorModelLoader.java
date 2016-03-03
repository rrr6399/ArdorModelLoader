package osm.map.worldwind;

import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;

import java.io.File;
import java.net.URL;

import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.md2.Md2Importer;
import com.ardor3d.extension.model.obj.ObjImporter;
import com.ardor3d.image.util.awt.AWTImageLoader;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.jogl.JoglContextCapabilities;
import com.ardor3d.renderer.jogl.JoglRenderContext;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.geom.jogl.DirectNioBuffersSet;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;
import java.util.HashMap;

public class ArdorModelLoader {

	private static HashMap<Object, String> modelCache = new HashMap<Object, String>();

	public static String createModel(String source) {
		String model = modelCache.get(source);
		if (model == null) {
			model = source;
			modelCache.put(source, model);
		}
		return model;
	}

	public static Node loadModel(String modelFileStr) throws Exception {
		final Node root = new Node("rootNode");
		String modelDirStr = new File(modelFileStr).getParent();
		String modelNameStr = new File(modelFileStr).getName();
		File modelDir = new File(modelDirStr);
		modelDirStr = modelDir.getAbsolutePath();
		SimpleResourceLocator modelLocator = new SimpleResourceLocator(new URL("file:" + modelDirStr));
		SimpleResourceLocator textureLocator = new SimpleResourceLocator(new URL("file:" + modelDirStr));
		ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_MODEL, modelLocator);
		ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, textureLocator);
		if (modelFileStr.toLowerCase().endsWith("dae")) {
			ColladaImporter importer = new ColladaImporter();
			importer.setModelLocator(modelLocator);
			importer.setTextureLocator(textureLocator);
			root.attachChild(importer.load(modelNameStr).getScene()
			);
		} else if (modelFileStr.toLowerCase().endsWith(".obj")) {
			ObjImporter importer = new ObjImporter();
			importer.setModelLocator(modelLocator);
			importer.setTextureLocator(textureLocator);
			root.attachChild(importer.load(modelNameStr).getScene()
			);
		} else if (modelFileStr.toLowerCase().endsWith(".md2")) {
			Md2Importer importer = new Md2Importer();
			importer.setModelLocator(modelLocator);
			importer.setTextureLocator(textureLocator);
			root.attachChild(importer.load(modelNameStr).getScene()
			);
		}
		root.updateGeometricState(0);
		return root;
	}

	public static void initializeArdorSystem(final DrawContext dc) {
		if (ContextManager.getContextForKey("HACKED CONTEXT") != null) {
			RenderContext rc = ContextManager.switchContext("HACKED CONTEXT");
			return;
		}

		Logging.logger().info("ARDOR INITIALIZER -->> initializeArdorSystem");
		DirectNioBuffersSet dnbs = new DirectNioBuffersSet();
		final JoglContextCapabilities caps = new JoglContextCapabilities(dc.getGL(), dnbs);
		final JoglRenderContext rc = new JoglRenderContext(dc.getGLContext(), caps, dnbs);

		ContextManager.addContext("HACKED CONTEXT", rc);
		ContextManager.switchContext("HACKED CONTEXT");
		Camera cam = new Camera() {
			@Override
			public FrustumIntersect contains(BoundingVolume bound) {
				return FrustumIntersect.Inside;
			}
		};
		ContextManager.getCurrentContext().setCurrentCamera(cam);
		AWTImageLoader.registerLoader();
	}

}
