package com.example.finalpj;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import androidx.appcompat.widget.AppCompatImageView;

import java.util.ArrayList;
import java.util.List;

public class OverlayImageView extends AppCompatImageView {
    private List<String> overlayTexts = new ArrayList<>();
    private List<PointF> textPositions = new ArrayList<>();
    private boolean isTextVisible = false;
    private Paint textPaint;

    // 상수 선언
    private static final int TEXT_SIZE = 50;
    private static final int TEXT_COLOR = Color.RED;
    private static final int ANIMATION_DURATION = 300;

    public OverlayImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Paint 설정
        textPaint = new Paint();
        textPaint.setColor(TEXT_COLOR); // 텍스트 색상
        textPaint.setTextSize(TEXT_SIZE); // 텍스트 크기
        textPaint.setTypeface(Typeface.DEFAULT_BOLD); // 텍스트 스타일
        textPaint.setAntiAlias(true); // 부드럽게 렌더링

        // 클릭 이벤트 활성화
        setClickable(true);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                isTextVisible = !isTextVisible;
                startFadeAnimation();
            }
        });
    }

    // 좌표와 텍스트 리스트 설정 메서드
    public void setOverlayTexts(List<String> texts, List<PointF> positions) {
        this.overlayTexts = texts;
        this.textPositions = positions;
        this.isTextVisible = true;
        invalidate(); // 화면 다시 그리기
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 현재 View의 크기
        float viewWidth = getWidth();
        float viewHeight = getHeight();

        // 원본 이미지의 크기
        Drawable drawable = getDrawable();
        if (drawable == null) return;
        float imageWidth = drawable.getIntrinsicWidth();
        float imageHeight = drawable.getIntrinsicHeight();

        // 이미지가 View에 그려지는 비율 계산
        float widthRatio = viewWidth / imageWidth;
        float heightRatio = viewHeight / imageHeight;

        // 텍스트 오버레이 위치 계산 및 그리기
        if (isTextVisible && overlayTexts.size() == textPositions.size()) {
            for (int i = 0; i < overlayTexts.size(); i++) {
                String text = overlayTexts.get(i);
                PointF pos = textPositions.get(i);

                // 좌표 보정: 이미지 비율에 맞게 조정
                float adjustedX = pos.x * widthRatio;
                float adjustedY = pos.y * heightRatio;

                canvas.drawText(text, adjustedX, adjustedY, textPaint);
            }
        }
    }


    // 애니메이션 추가 (페이드 인/아웃)
    private void startFadeAnimation() {
        AlphaAnimation fadeAnimation = isTextVisible
                ? new AlphaAnimation(0.0f, 1.0f)  // 페이드 인
                : new AlphaAnimation(1.0f, 0.0f); // 페이드 아웃
        fadeAnimation.setDuration(ANIMATION_DURATION);
        fadeAnimation.setFillAfter(true);
        startAnimation(fadeAnimation);
    }
}
