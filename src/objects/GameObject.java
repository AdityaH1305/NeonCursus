package objects;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class GameObject {
    private Vector3f position;
    private Vector3f rotation;
    private float scale;

    public GameObject() {
        position = new Vector3f(0.0f, 0.0f, 0.0f);
        rotation = new Vector3f(0.0f, 0.0f, 0.0f);
        scale = 1.0f;
    }

    // --- GETTERS & SETTERS ---

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
    }

    public Vector3f getRotation() {
        return rotation;
    }

    public void setRotation(float x, float y, float z) {
        this.rotation.set(x, y, z);
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    // --- THE GRAPHICS MATH ---

    /**
     * Calculates the Transformation Matrix for this object.
     * Order of operations is critical in OpenGL: Translate, then Rotate, then
     * Scale.
     */
    public Matrix4f getModelMatrix() {
        Matrix4f model = new Matrix4f().identity();

        // 1. Translate (Move to position in 3D space)
        model.translate(position);

        // 2. Rotate (Convert degrees to radians for JOML)
        model.rotateX((float) Math.toRadians(rotation.x));
        model.rotateY((float) Math.toRadians(rotation.y));
        model.rotateZ((float) Math.toRadians(rotation.z));

        // 3. Scale (Size the object)
        model.scale(scale);

        return model;
    }
}