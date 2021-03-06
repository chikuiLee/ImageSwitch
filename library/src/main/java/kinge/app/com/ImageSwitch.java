package kinge.app.com;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by chikuilee on 16-4-10.
 *
 */
public class ImageSwitch extends FrameLayout {
    // Timer use to control how long to change the item
    private Timer mTimer;
    private Handler mHandler;
    private Adapter adapter;
    private View childs[];
    private int animationDuration=500;
    private int l,t,r,b;
    //child item width
    int width;
    private int curentPosition;
    float x,y;
    float downX,downY;
    private boolean isAnimation;
    private boolean isTouching;
    private float mVx;
    private VelocityTracker mVelocityTracker;
    public ImageSwitch(Context context) {
        super(context);
        init();
    }
    public ImageSwitch(Context context, AttributeSet attrs){
        super(context,attrs);
        init();
    }
    public ImageSwitch(Context context,AttributeSet attrs,int defAttr){
        super(context,attrs,defAttr);
        init();
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ImageSwitch(Context context, AttributeSet attrs, int defAttr, int defStyle){
        super(context,attrs,defAttr,defStyle);
        init();
    }
    private void init(){
        childs=new View[3];
        curentPosition=0;
    }
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        System.out.println("l "+l+" r "+r);
        ViewGroup parent= (ViewGroup) getParent();
        this.l=0+getPaddingLeft();
        this.t=0+getPaddingTop();
        this.r=r-l-getPaddingRight();
        this.b=b-getPaddingBottom();
        int ll=this.l;
        width=this.r-this.l;
        for(int i=0;i<getChildCount();++i){
            //layout the child in right position
            View child=getChildAt(i);
            if(childs[0].equals(child)){
                //The child should be  put int left;
                child.layout(-width,this.t,0,this.b);
            }
            if(childs[1].equals(child)){
                //The child should be put int middle
                child.layout(this.l,this.t,this.r,this.b);
            }
            if(childs[2].equals(child)){
                child.layout(getWidth(),this.t,getWidth()+width,this.b);
            }
        }
    }
    //start the task to change the child view auto
    public void start(int period){
        System.out.println("start ");
        mTimer=new Timer();
        mHandler =new Handler(this);
        if(adapter !=null&& adapter.getCount()>0){


            adapter.bindData(childs[1],curentPosition);
            //pre load the next item
            if(adapter.getCount()>1){
                int np=curentPosition+1;
                np=np% adapter.getCount();
                adapter.bindData(childs[2],np);
                adapter.bindData(childs[0], adapter.getCount()-1);
                //only the content count bigger than 1 then you need to exchange them
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        mHandler.sendEmptyMessage(1);
                    }
                },period,period);
            }
        }
    }
    // stop the task
    public void stop(){
        if(mTimer!=null){
            mTimer.cancel();
            mTimer.purge();
            mTimer=null;
        }
    }
    public void setAdapter(Adapter loadNext){
        this.adapter =loadNext;
        View child= adapter.createItem();
        childs[0]=child;
        addView(child);
        child= adapter.createItem();
        childs[1]=child;
        addView(child);
        child= adapter.createItem();
        childs[2]=child;
        addView(child);
    }
    // set the duration for exchange animation
    public void setAnimationDuration(int duration){
        this.animationDuration=duration;
    }
    private void changeToNext(){
        if(isAnimation ||isTouching){
            return;
        }
        isAnimation =true;
//        System.out.println("################  before #######################");
        int translation0=childs[1].getRight();
        int translation1=childs[2].getLeft()-childs[1].getLeft();
//        System.out.println(childs[1].hashCode()+": left "+childs[1].getLeft()+" right "+childs[1].getRight()+" tx "+childs[1].getTranslationX());
//        System.out.println(childs[2].hashCode()+": left "+childs[2].getLeft()+" right "+childs[2].getRight()+" tx "+childs[2].getTranslationX());
//        System.out.println("==================  before =======================");
        PropertyValuesHolder translationHolder=PropertyValuesHolder.ofFloat("translationX",-translation0);
        PropertyValuesHolder alphaHolder=PropertyValuesHolder.ofFloat("alpha",0.6f);
        ObjectAnimator dismissAnimator=ObjectAnimator.ofPropertyValuesHolder(childs[1],translationHolder,alphaHolder);
        dismissAnimator.setDuration(animationDuration).start();
        ObjectAnimator occurAnimator=ObjectAnimator.ofFloat(childs[2],"translationX",-translation1);
        occurAnimator.setDuration(animationDuration);
        occurAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
//                System.out.println("*************** after *************");
//                System.out.println(childs[1].hashCode()+": left "+childs[1].getLeft()+" right "+childs[1].getRight()+" tx "+childs[1].getTranslationX());
//                System.out.println(childs[2].hashCode()+": left "+childs[2].getLeft()+" right "+childs[2].getRight()+" tx "+childs[2].getTranslationX());
//                System.out.println("$$$$$$$$$$$$$$$$ after $$$$$$$$$$$$$$");
                reLayoutRight();

                ++curentPosition;
                curentPosition=curentPosition% adapter.getCount();
                adapter.onItemChanged(curentPosition);
                int np=(curentPosition+1)% adapter.getCount();
                adapter.bindData(childs[2],np);
//                np=curentPosition-1;
//                np=np<0? adapter.getCount()-1:np;
//                adapter.bindData(childs[0],np);
                isAnimation =false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        occurAnimator.start();
    }

    /**
     *  if you swipe from right to left
     *  then move the item in the childs array
     */
    private void reLayoutRight(){
        View temp=childs[0];
        childs[0]=childs[1];
        childs[1]=childs[2];
        childs[2]=temp;

        childs[0].setAlpha(1);
       reLayout();
    }
    /*
    * if you swipe for left to right
    * them move the item  in childs array
    * */
    private void reLayoutLeft(){
        View temp=childs[2];
        childs[2]=childs[1];
        childs[1]=childs[0];
        childs[0]=temp;
        reLayout();
    }
    /*
    * relayout the child in childs array
    * */
    private void reLayout(){
        childs[0].layout(-width,t,0,b);
        childs[0].setTranslationX(0);
        childs[1].layout(l,t,r,b);
        childs[1].setTranslationX(0);
        childs[2].layout(getWidth(),t,getWidth()+width,b);
        childs[2].setTranslationX(0);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean r0=handleMotionEvent(ev);
        boolean r1=super.dispatchTouchEvent(ev);
        return r0||r1;
    }
    /*
    * notification the data set had changed
    * */
    public void notificationDataSetChanged(){
        if(adapter!=null){
            int np=curentPosition-1;
            np=np<0?adapter.getCount()-1:np;
            adapter.bindData(childs[0],np);
            adapter.bindData(childs[1],curentPosition);
            np=curentPosition+1;
            np=np>=adapter.getCount()?0:np;
            adapter.bindData(childs[2],np);
        }
    }
    /*
        * override the onToucheEvent method to move the child ,when you swipe in this view
        * */
    public boolean handleMotionEvent(MotionEvent event) {
        if(isAnimation){
            return true;
        }

//        return super.onTouchEvent(event);
        boolean dealResult=false;
        int w=getWidth();
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                downY=event.getY();
                downX=event.getX();
                x=downX;
                y=downY;
                dealResult=true;
                isTouching=true;
                mVx=0;
                   /*
                    * use VeloctyTracker to compute the v fo event;
                    * */
                if(mVelocityTracker==null){
                    mVelocityTracker=VelocityTracker.obtain();
                }
                else{
                    mVelocityTracker.clear();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                isTouching=true;
                float dx=event.getX()-x;
                float dy=event.getY()-y;
                mVelocityTracker.addMovement(event);
                mVelocityTracker.computeCurrentVelocity(1000);
                mVx=mVelocityTracker.getXVelocity();
                if(Math.abs(dx)>Math.abs(dy)&& adapter !=null&& adapter.getCount()>1){
                    if(dx>0){
                        dealResult=true;
                        for(int i=0;i<3;++i){
                            childs[i].setTranslationX(dx);
                            event.setAction(MotionEvent.ACTION_CANCEL);
                        }
                    }
                    if(dx<0){
                        dealResult=true;
                        for(int i=0;i<3;++i){
                            childs[i].setTranslationX(dx);
                            event.setAction(MotionEvent.ACTION_CANCEL);
                        }
                    }
                }

                break;
            case MotionEvent.ACTION_UP:
                float upx=event.getX();
                dx=upx-downX;
                if((Math.abs(mVx)>1000||Math.abs(dx)>width/4)&& adapter !=null&& adapter.getCount()>1){

                    if(dx>0){
                        move2Left(w);
                    }
                    if(dx<0){
                        move2Right(w);
                    }
                }
                else{
                    isAnimation =true;
                    animationScroll(0);
                }
                isTouching=false;
                break;
        }

//        event.recycle();
        return dealResult;
    }
    private void move2Right(int w){
        isAnimation =true;
        ++curentPosition;
        if(curentPosition>= adapter.getCount()){
            curentPosition=0;
        }
        animationScroll(-w);
    }
    private void move2Left(int w){
        isAnimation =true;
        --curentPosition;
        if(curentPosition<0){
            curentPosition= adapter.getCount()-1;
        }
        animationScroll(w);
    }
    /*
    * use the ObjectAnimator to Scroll the child to right position
    * */
    private void animationScroll(final float w){
        ObjectAnimator animator=ObjectAnimator.ofFloat(childs[0],"translationX",w).setDuration(200);
        animator.start();
        ObjectAnimator animator1=ObjectAnimator.ofFloat(childs[1],"translationX",w).setDuration(200);
        animator1.start();
        ObjectAnimator animator2=ObjectAnimator.ofFloat(childs[2],"translationX",w).setDuration(200);
        animator2.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimation =false;
                if(w>0) {
                    reLayoutLeft();
                    int pn=curentPosition-1;
                    pn=pn<0? adapter.getCount()-1:pn;
                    //load pre item data
                    adapter.bindData(childs[0],pn);
                }
                else if(w<0){
                    reLayoutRight();
                    int np=curentPosition+1;
                    //load next item data
                    adapter.bindData(childs[2],np% adapter.getCount());
                }
                adapter.onItemChanged(curentPosition);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animator2.start();
    }
    private static class Handler extends android.os.Handler{
        WeakReference <ImageSwitch>weakReference;
        public Handler(ImageSwitch imageSwitch){
            weakReference=new WeakReference<ImageSwitch>(imageSwitch);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ImageSwitch imageSwitch=weakReference.get();
            if(imageSwitch!=null){
                imageSwitch.changeToNext();
            }
        }
    }
    /*
    * This interface is used to create the child and bindData for the child for ImageSwitch
    * */
    public interface Adapter {
        /*
        * use to create the item
        * return : return the item it created
        * */
        View createItem();
        /*
        * bindData for the view
        * @param
        * view :target
        * position: position of the view
        * */
        void bindData(View view, int position);
        /*
        * get the count of child
        * return :
        * return the count for the child;
        * */
        int getCount();
        void onItemChanged(int currentItem);
    }
}
