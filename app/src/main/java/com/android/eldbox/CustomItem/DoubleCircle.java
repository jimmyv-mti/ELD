package com.android.eldbox.CustomItem;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.android.eldbox.R;

public class DoubleCircle extends View
{
    private Paint rpmCirclePaint;
    private Paint outSideCirclePaint;
    private Paint backGroundPaint;
    private Paint rpmPointPaint;
    private Paint vssPointPaint;
    private Paint shadowPaint;
    private Paint textPaint;
    private Resources resources;
    private Typeface typeface;
    private Bitmap shadowImg;
    private Bitmap backGroundImg;
    private Bitmap smallPointImg;
    private Bitmap bigPointImg;
    private RectF rectFRpm;
    private RectF rectFVss;
    private Rect destRectRpm;
    private Rect destRectVss;
    private Rect destRectBackGround;
    private Rect destRectShadow;
    private float vss = 0;
    private float rpm = 0;
    private final float maxVss = 220;
    private final float maxRpm = 11000;

    public DoubleCircle(Context context)
    {
        super(context);
    }

    public DoubleCircle(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
    }

    public DoubleCircle(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    /*
      这里的神奇常数843，399，是这张图中⚪的圆心，154.8、298.9则分别是两个⚪的半径
      100、134.2是宽度，因为使用这种画圆弧的方法矩形不是外切，矩形的边在弧线的中心位置，因而需要一些调整
      背景图两个圆圈的一些数据,单位为px，其中圆心为公共圆心，第一点、第二点等指的是从圆心到最外层与大小圆弧内切、外切的距离
      圆心 843 398
      第一点半径51.7
      第二点半径154.8
      第三点半径164.8
      第四点半径298.9
      内圈宽度：103.1
      外圈宽度：134.1
      下同
       Center 843 398
        The first point radius is 51.7
        The second point radius is 154.8
        The third point radius is 164.8
        Fourth point radius 298.9
        Inner ring width: 103.1
        Outer ring width: 134.1
        The same below
     */
    private void init()
    {
        typeface = Typeface.createFromAsset(getContext().getAssets(), "BreuerText-Bold.ttf");
        rpmCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rpmCirclePaint.setColor(Color.parseColor("#D86000"));
        rpmCirclePaint.setStrokeWidth((float) 100);
        rpmCirclePaint.setStyle(Paint.Style.STROKE);
        outSideCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outSideCirclePaint.setColor(Color.parseColor("#0060F0"));
        outSideCirclePaint.setStrokeWidth((float) 138);
        outSideCirclePaint.setStyle(Paint.Style.STROKE);
        backGroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backGroundPaint.setDither(true);
        backGroundPaint.setFilterBitmap(true);
        rpmPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rpmPointPaint.setFilterBitmap(true);
        rpmPointPaint.setDither(true);
        vssPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        vssPointPaint.setFilterBitmap(true);
        vssPointPaint.setDither(true);
        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setFilterBitmap(true);
        shadowPaint.setDither(true);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTextSize(60);
        textPaint.setTypeface(typeface);//设置字体 Set font
        resources = getResources();
        smallPointImg = ((BitmapDrawable) resources.getDrawable(R.drawable.small, null)).getBitmap();
        Matrix matrix = new Matrix();
        matrix.postScale(1f, 1f);
        matrix.postRotate(3);
        //旋转3°使指针能够和圆圈完美贴合 Rotate 3° to make the pointer fit perfectly with the circle
        smallPointImg = Bitmap.createBitmap(smallPointImg, 0, 0, smallPointImg.getWidth(), smallPointImg.getHeight(), matrix, true);
        bigPointImg = ((BitmapDrawable) resources.getDrawable(R.drawable.big, null)).getBitmap();
        matrix.postScale(1f, 1f);
        matrix.postRotate(3);
        //旋转3°使指针能够和圆圈完美贴合 Rotate 3° to make the pointer fit perfectly with the circle
        bigPointImg = Bitmap.createBitmap(bigPointImg, 0, 0, bigPointImg.getWidth(), bigPointImg.getHeight(), matrix, true);
        shadowImg = ((BitmapDrawable) resources.getDrawable(R.drawable.shadow, null)).getBitmap();
        matrix = new Matrix();
        matrix.postScale(1f, 1f);
        matrix.postRotate(0);
        shadowImg = Bitmap.createBitmap(shadowImg, 0, 0, shadowImg.getWidth(), shadowImg.getHeight(), matrix, true);
        backGroundImg = ((BitmapDrawable) resources.getDrawable(R.drawable.b1280x8004, null)).getBitmap();
        //设置bitmap绘制的位置，通过一点一点微调出来的，因此都是些magic number
        //Set the position of the bitmap drawing, fine-tuned by little by little, so they are all magic numbers
        rectFRpm = new RectF((float) (843 - 154.8 + 100 / 2), (float) (399 - 154.8 + 100 / 2), (float) (843 + 154.8 - 100 / 2), (float) (399 + 154.8 - 100 / 2));
        rectFVss = new RectF((float) (843 - 298.9 + 134.1 / 2), (float) (399 - 298.9 + 134.1 / 2), (float) (843 + 298.3 - 134.1 / 2), (float) (399 + 298.9 - 134.1 / 2));
        destRectRpm = new Rect(0 - 49 - 45, 0 - 32, 0 - 45, 64 - 32);
        destRectVss = new Rect(0 - 193, 0 - 32, 39 - 193, 64 - 32);
        destRectBackGround = new Rect(0, 0, 1280, 800);
        destRectShadow = new Rect(0 - 309, 0 - 312, 417 - 309, 624 - 312);

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(MeasureSpec.makeMeasureSpec(1280, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(800, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        //首先绘制橙色和蓝色这两个表示数值的圆弧，然后在其上绘制引用，再绘制指针，再绘制背景图，最终绘制文字
        //First draw one orange and one blue arcs representing the values,Then draw a reference on it, draw the pointer, draw the background image, and finally draw the text
        drawRpmCircle(canvas);
        drawVssCircle(canvas);
        drawShadow(canvas);
        drawRpmPoint(canvas);
        drawVssPoint(canvas);
        drawBackground(canvas);
        drawText(canvas);
    }

    private void drawRpmCircle(Canvas canvas)
    {
        float temp;
        temp = rpm > maxRpm ? maxRpm : rpm;
        canvas.drawArc(rectFRpm, 72, (198 * temp / maxRpm), false, rpmCirclePaint);
    }

    private void drawVssCircle(Canvas canvas)
    {
        float temp;
        temp = vss > maxVss ? maxVss : vss;
        canvas.drawArc(rectFVss, 72, (float) (198.5 * temp / maxVss), false, outSideCirclePaint);
    }

    /**
     * 绘制指针，使用和水平方向数值的差值来计算画布的旋转角度，由此实现指针旋转
     *Draw a pointer and use the difference from the horizontal value to calculate the rotation angle of the canvas, thereby implementing pointer rotation
     */
    private void drawRpmPoint(Canvas canvas)
    {
        float temp;
        temp = rpm > maxRpm ? maxRpm : rpm;
        float rotate = (((temp - 6000) / 1000) * 18 - 3);//-3是为了使指针对准 -3 is to align the pointer
        canvas.translate(843, 399);
        canvas.rotate(rotate);
        canvas.drawBitmap(smallPointImg, null, destRectRpm, rpmPointPaint);
        canvas.rotate(-rotate);
        canvas.translate(-843, -399);
    }

    private void drawVssPoint(Canvas canvas)
    {
        float temp;
        temp = vss > maxVss ? maxVss : vss;
        float rotate = (float) (((temp - 120) / 10) * 9 - 0.8);//-0.8是为了使指针对准  -0.8 is to align the pointer
        canvas.translate(843, 399);
        canvas.rotate(rotate);
        canvas.drawBitmap(bigPointImg, null, destRectVss, vssPointPaint);
        canvas.rotate(-rotate);
        canvas.translate(-843, -399);
    }

    private void drawBackground(Canvas canvas)
    {
        canvas.drawBitmap(backGroundImg, null, destRectBackGround, backGroundPaint);
    }

    /**
     * 绘制阴影遮挡层，用以实现颜色渐变之效果  Draw a shadow occlusion layer to achieve the effect of color gradation
     */
    private void drawShadow(Canvas canvas)
    {
        //设置bitmap绘制的位置，通过一点一点微调出来的，因此都是些magic number
        // Set the position of the bitmap drawing, fine-tuned by little by little, so they are all magic numbers
        canvas.translate(843, 399);
        canvas.drawBitmap(shadowImg, null, destRectShadow, vssPointPaint);
        canvas.translate(-843, -399);
    }

    /**
     * 绘制文字 Drawing text
     *
     * @param canvas
     */
    private void drawText(Canvas canvas)
    {
        textPaint.setColor(Color.parseColor("#0060D8"));
        canvas.drawText(Integer.toString((int) vss), 1138, 365, textPaint);
        textPaint.setColor(Color.parseColor("#D86000"));
        canvas.drawText(Integer.toString((int) rpm), 1138, 440, textPaint);
    }

    public void setVss(int vss)
    {
        if (this.vss == vss) return;//如果数值没有改变，则不触发动画 If the value has not changed, the animation is not triggered
        ValueAnimator valueAnimator = ValueAnimator.ofInt((int) this.vss, vss);
        valueAnimator.setDuration(300);
       /*
        如果接收到同一数据的间隔小于duration，上个动画尚未结束，便开始新的动画，会造成卡顿等问题，所以duration应当小于两次数据的发送间隔。
        因为通常而言相同数据的发送间隔会大于500ms，所以在正常的使用中duration可以更大一些。
        If the interval of receiving the same data is less than duration, then the last animation has not ended, and a new
        animation is started, which causes problems such as catastrophic, so the duration should be less than the interval between two data transmissions.
        Since the same data transmission interval is usually greater than 500ms, the duration can be larger in normal use.
        */
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                int a = (Integer) animation.getAnimatedValue();
                DoubleCircle.this.vss = (float) a;
                DoubleCircle.this.invalidate();
            }
        });
        valueAnimator.start();

    }

    public void setRpm(int rpm)
    {
        if (this.rpm == rpm) return;//如果数值没有改变，则不触发动画 If the value has not changed, the animation is not triggered
        ValueAnimator valueAnimator = ValueAnimator.ofInt((int) this.rpm, rpm);
        valueAnimator.setDuration(300);
        /*
        如果接收到同一数据的间隔小于duration，上个动画尚未结束，便开始新的动画，会造成卡顿等问题，所以duration应当小于两次数据的发送间隔。
        因为通常而言相同数据的发送间隔会大于500ms，所以在正常的使用中duration可以更大一些。
        If the interval of receiving the same data is less than duration, then the last animation has not ended, and a new
        animation is started, which causes problems such as catastrophic, so the duration should be less than the interval between two data transmissions.
        Since the same data transmission interval is usually greater than 500ms, the duration can be larger in normal use.
        */
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                int a = (Integer) animation.getAnimatedValue();
                DoubleCircle.this.rpm = (float) a;
                DoubleCircle.this.invalidate();
            }
        });
        valueAnimator.start();

    }

}
