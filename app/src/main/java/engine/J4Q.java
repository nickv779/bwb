package engine;

import android.content.Context;
import java.util.HashMap;

import engine.activities.GameEngineScene;
import engine.controllers.J4QLeftController;
import engine.controllers.J4QRightController;
import engine.controllers.TouchScreen;
import engine.models.GameObject;
import engine.models.Mesh;
import engine.physics.PhysicsEngine;

public class J4Q {

    private static int objectID=0;

    private static HashMap<Integer,Mesh> objects=new HashMap<>();
    public static GameObject getObject(int ID){Mesh m=objects.get(ID);
        if(m==null) return null; else{
            return m.gameObject;
        }
    }

    public static TouchScreen touchScreen;
    public static int newObjectID(Mesh mesh){objectID+=1;objects.put(objectID,mesh);return objectID;}
    public static PhysicsEngine physicsEngine;

    public static Context activity;
    public static GameEngineScene scene;
    public static float perSec(){return scene.perSec();}

    public static J4QLeftController leftController=new J4QLeftController();
    public static J4QRightController rightController=new J4QRightController();
    public static native long stopHapticFeedbackLeft();
    public static native long stopHapticFeedbackRight();
    public static native long applyHapticFeedbackLeft(float amplitude, float seconds, int frequency);
    public static native long applyHapticFeedbackRight(float amplitude, float seconds, int frequency);
}
