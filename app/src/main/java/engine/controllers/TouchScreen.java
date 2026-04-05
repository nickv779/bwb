package engine.controllers;

import android.view.MotionEvent;
import android.view.View;

import engine.models.GameObject;
import engine.J4Q;
import engine.activities.GameEngineScene;

public class TouchScreen implements View.OnTouchListener{
    public int fingers_down=0;
    public float[] touch_x=new float[10];
    public float[] touch_y=new float[10];

    public int[] id=new int[10];

    private ObjectPicker objectPicker;

    public void setup(int width, int height){
        if(objectPicker==null)objectPicker=new ObjectPicker();
        objectPicker.setSize(width,height);
    }

    public void capture(GameEngineScene scene){
        objectPicker.begin();
        scene.root.draw(objectPicker.shader);
        for(int i=0;i<J4Q.touchScreen.fingers_down;i++) {
            id[i]=objectPicker.pick((int)touch_x[i],(int)touch_y[i]);
        }
        objectPicker.end();
    }

    public int pick(int slot){
        return id[slot];
    }

    public GameObject pickObject(int slot){
        return J4Q.getObject(id[slot]);
    }


    @Override
    public boolean onTouch(View view, MotionEvent event) {

        //get the object ID

        //getObject(ID).setShader(highlightobject_shader);
        //getObject(ID).transform.scale(1.2);

        TouchScreen t=J4Q.touchScreen;

        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                t.fingers_down=1;
                for(int i=0;i<t.touch_x.length;i++){
                    t.touch_x[i]=0;
                    t.touch_y[i]=0;
                }
                t.touch_x[0]=event.getX();
                t.touch_y[0]=event.getY();
                //processTouchEvent(t.fingers_down, t.touch_x,t.touch_y);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                t.fingers_down=event.getPointerCount();
                for(int i=0;i<t.touch_x.length;i++){
                    t.touch_x[i]=0;
                    t.touch_y[i]=0;
                }
                for(int i=0;i<event.getPointerCount()&& i<t.touch_x.length;i++){
                    t.touch_x[i] = event.getX(i);
                    t.touch_y[i] = event.getY(i);
                }
                //processTouchEvent(t.fingers_down, t.touch_x,t.touch_y);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                t.fingers_down=event.getPointerCount()-1;
                int pointerIndex = event.getActionIndex();
                for(int i=0;i<t.touch_x.length;i++){
                    t.touch_x[i]=0;
                    t.touch_y[i]=0;
                }
                for(int i=0;i<event.getPointerCount()&& i<t.touch_x.length;i++){
                    if(i<pointerIndex) {
                        t.touch_x[i] = event.getX(i);
                        t.touch_y[i] = event.getY(i);
                    }else if(i>pointerIndex){
                        t.touch_x[i-1] = event.getX(i);
                        t.touch_y[i-1] = event.getY(i);
                    }
                }
                //processTouchEvent(t.fingers_down, t.touch_x,t.touch_y);
                break;
            case MotionEvent.ACTION_UP:
                t.fingers_down=0;
                for(int i=0;i<t.touch_x.length;i++){
                    t.touch_x[i]=0;
                    t.touch_y[i]=0;
                }
                //processTouchEvent(t.fingers_down, t.touch_x,t.touch_y);
                break;
            case MotionEvent.ACTION_CANCEL:
                t.fingers_down=0;
                for(int i=0;i<t.touch_x.length;i++){
                    t.touch_x[i]=0;
                    t.touch_y[i]=0;
                }
                //processTouchEvent(t.fingers_down, t.touch_x,t.touch_y);
                break;
        }

        return true;
    }
}
