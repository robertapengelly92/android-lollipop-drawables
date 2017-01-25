package robertapengelly.support.graphics.drawable;

import  android.annotation.TargetApi;
import  android.content.res.ColorStateList;
import  android.content.res.Resources;
import  android.content.res.Resources.Theme;
import  android.content.res.TypedArray;
import  android.graphics.Canvas;
import  android.graphics.ColorFilter;
import  android.graphics.Outline;
import  android.graphics.Paint;
import  android.graphics.PixelFormat;
import  android.graphics.PorterDuff;
import  android.graphics.PorterDuff.Mode;
import  android.graphics.PorterDuffColorFilter;
import  android.graphics.Rect;
import  android.graphics.Shader;
import  android.graphics.drawable.Drawable;
import  android.graphics.drawable.shapes.Shape;
import  android.os.Build;
import  android.util.AttributeSet;
import  android.util.TypedValue;

import  java.io.IOException;

import  org.xmlpull.v1.XmlPullParser;
import  org.xmlpull.v1.XmlPullParserException;

import  robertapengelly.support.lollipopdrawables.R;

/**
 * A Drawable object that draws primitive shapes. A ShapeDrawable takes a
 * {@link android.graphics.drawable.shapes.Shape} object and manages its
 * presence on the screen. If no Shape is given, then the ShapeDrawable will
 * default to a {@link android.graphics.drawable.shapes.RectShape}.
 *
 * <p>This object can be defined in an XML file with the <code>&lt;shape></code>
 * element.</p>
 *
 * <div class="special reference"> <h3>Developer Guides</h3>
 *
 * <p>For more information about how to use ShapeDrawable, read the <a
 * href="{@docRoot}guide/topics/graphics/2d-graphics.html#shape-drawable">
 * Canvas and Drawables</a> document. For more information about defining a
 * ShapeDrawable in XML, read the <a href="{@docRoot}
 * guide/topics/resources/drawable-resource.html#Shape">Drawable Resources</a>
 * document.</p>
 *
 * </div>
 */
public class ShapeDrawable extends LollipopDrawable {

    private boolean mMutated;
    
    private PorterDuffColorFilter mTintFilter;
    private ShapeState mShapeState;
    
    /** ShapeDrawable constructor. */
    public ShapeDrawable() {
        this(new ShapeState(null), null, null);
    }
    
    /**
     * Creates a ShapeDrawable with a specified Shape.
     *
     * @param s the Shape that this ShapeDrawable should be
     */
    public ShapeDrawable(Shape s) {
        this(new ShapeState(null), null, null);
        
        mShapeState.mShape = s;
    
    }
    
    /**
     * The one constructor to rule them all. This is called by all public
     * constructors to set the state and initialize local properties.
     */
    private ShapeDrawable(ShapeState state, Resources res, Theme theme) {
    
        if ((theme != null) && state.canApplyTheme()) {
        
            mShapeState = new ShapeState(state);
            applyTheme(theme);
        
        } else
            mShapeState = state;
        
        initializeWithState(state, res);
    
    }
    
    @Override
    public void applyTheme(Theme t) {
        super.applyTheme(t);

        final ShapeState state = mShapeState;
        
        if ((state == null) || (state.mThemeAttrs == null))
            return;
        
        try {
            updateStateFromTypedArray(t, null, mShapeState.mThemeAttrs);
        } catch (XmlPullParserException e) {
            throw new RuntimeException(e);
        }
        
        if (Build.VERSION.SDK_INT >= 21)
            // Update local properties.
            initializeWithState(state, t.getResources());
    
    }
    
    @Override
    public void draw(Canvas canvas) {
    
        final Rect r = getBounds();
        final ShapeState state = mShapeState;
        
        final Paint paint = state.mPaint;
        
        final int prevAlpha = paint.getAlpha();
        paint.setAlpha(modulateAlpha(prevAlpha, state.mAlpha));
        
        // only draw shape if it may affect output
        if ((paint.getAlpha() != 0) || (paint.getXfermode() != null)) {
        
            final boolean clearColorFilter;
            
            if (mTintFilter != null && paint.getColorFilter() == null) {
            
                paint.setColorFilter(mTintFilter);
                clearColorFilter = true;
            
            } else
                clearColorFilter = false;
            
            if (state.mShape != null) {
            
                // need the save both for the translate, and for the (unknown) Shape
                final int count = canvas.save();
                canvas.translate(r.left, r.top);
                
                onDraw(state.mShape, canvas, paint);
                canvas.restoreToCount(count);
            
            } else
                canvas.drawRect(r, paint);
            
            if (clearColorFilter)
                paint.setColorFilter(null);
        
        }
        
        // restore
        paint.setAlpha(prevAlpha);
    
    }
    
    @Override
    public int getAlpha() {
        return mShapeState.mAlpha;
    }
    
    @Override
    public int getChangingConfigurations() {
        return (super.getChangingConfigurations() | mShapeState.mChangingConfigurations);
    }
    
    @Override
    public ConstantState getConstantState() {
    
        mShapeState.mChangingConfigurations = getChangingConfigurations();
        return mShapeState;
    
    }
    
    @Override
    public int getIntrinsicHeight() {
        return mShapeState.mIntrinsicHeight;
    }
    
    @Override
    public int getIntrinsicWidth() {
        return mShapeState.mIntrinsicWidth;
    }
    
    @Override
    public int getOpacity() {
    
        if (mShapeState.mShape == null) {
        
            final Paint p = mShapeState.mPaint;
            
            if (p.getXfermode() == null) {
            
                final int alpha = p.getAlpha();
                
                if (alpha == 0)
                    return PixelFormat.TRANSPARENT;
                
                if (alpha == 255)
                    return PixelFormat.OPAQUE;
            
            }
        
        }
        
        // not sure, so be safe
        return PixelFormat.TRANSLUCENT;
    
    }
    
    @Override
    @TargetApi(21)
    public void getOutline(Outline outline) {
    
        if (mShapeState.mShape != null) {
        
            mShapeState.mShape.getOutline(outline);
            outline.setAlpha(getAlpha() / 255.0f);
        
        }
    
    }
    
    @Override
    public boolean getPadding(Rect padding) {
    
        if (mShapeState.mPadding != null) {
        
            padding.set(mShapeState.mPadding);
            return true;
        
        } else
            return super.getPadding(padding);
    
    }
    
    /** Returns the Paint used to draw the shape. */
    public Paint getPaint() {
        return mShapeState.mPaint;
    }
    
    /**
     * Returns the ShaderFactory used by this ShapeDrawable for requesting a
     * {@link android.graphics.Shader}.
     */
    public ShaderFactory getShaderFactory() {
        return mShapeState.mShaderFactory;
    }
    
    /** Returns the Shape of this ShapeDrawable. */
    public Shape getShape() {
        return mShapeState.mShape;
    }
    
    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Theme theme)
        throws XmlPullParserException, IOException {
        super.inflate(r, parser, attrs, theme);
        
        final TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.ShapeDrawable);
        updateStateFromTypedArray(null, a, null);
        a.recycle();
        
        int type;
        
        final int outerDepth = parser.getDepth();
        
        while (((type = parser.next()) != XmlPullParser.END_DOCUMENT)
            && ((type != XmlPullParser.END_TAG) || (parser.getDepth() > outerDepth))) {
            
            if (type != XmlPullParser.START_TAG)
                continue;
            
            final String name = parser.getName();
            
            // call our subclass
            if (!inflateTag(name, r, parser, attrs))
                android.util.Log.w("drawable", "Unknown element: " + name + " for ShapeDrawable " + this);
        
        }
        
        // Update local properties.
        initializeWithState(mShapeState, r);
    
    }
    
    /**
     * Subclasses override this to parse custom subelements. If you handle it,
     * return true, else return <em>super.inflateTag(...)</em>.
     */
    protected boolean inflateTag(String name, Resources r, XmlPullParser parser, AttributeSet attrs) {
    
        if ("padding".equals(name)) {
        
            TypedArray a = r.obtainAttributes(attrs, R.styleable.ShapeDrawablePadding);
            setPadding(
                    a.getDimensionPixelOffset(R.styleable.ShapeDrawablePadding_android_left, 0),
                    a.getDimensionPixelOffset(R.styleable.ShapeDrawablePadding_android_top, 0),
                    a.getDimensionPixelOffset(R.styleable.ShapeDrawablePadding_android_right, 0),
                    a.getDimensionPixelOffset(R.styleable.ShapeDrawablePadding_android_bottom, 0));
            a.recycle();
            
            return true;
        
        }
        
        return false;
    
    }
    
    /**
     * Initializes local dynamic properties from state. This should be called
     * after significant state changes, e.g. from the One True Constructor and
     * after inflating or applying a theme.
     */
    private void initializeWithState(ShapeState state, Resources res) {
        mTintFilter = updateTintFilter(mTintFilter, state.mTint, state.mTintMode);
    }
    
    @Override
    public boolean isStateful() {
    
        final ShapeState s = mShapeState;
        return (super.isStateful() || ((s.mTint != null) && s.mTint.isStateful()));
    
    }
    
    private static int modulateAlpha(int paintAlpha, int alpha) {
        int scale = alpha + (alpha >>> 7); // convert to 0..256
        return paintAlpha * scale >>> 8;
    }
    
    @Override
    public Drawable mutate() {
    
        if (!mMutated && (super.mutate() == this)) {
        
            if (mShapeState.mPaint != null)
                mShapeState.mPaint = new Paint(mShapeState.mPaint);
            else
                mShapeState.mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            
            if (mShapeState.mPadding != null)
                mShapeState.mPadding = new Rect(mShapeState.mPadding);
            else
                mShapeState.mPadding = new Rect();
            
            try {
                mShapeState.mShape = mShapeState.mShape.clone();
            } catch (CloneNotSupportedException e) {
                return null;
            }
            
            mMutated = true;
        
        }
        
        return this;
    
    }
    
    @Override
    protected void onBoundsChange(Rect bounds) {
    
        super.onBoundsChange(bounds);
        updateShape();
    
    }
    
    @Override
    protected boolean onStateChange(int[] stateSet) {
    
        final ShapeState state = mShapeState;
        
        if (state.mTint != null && state.mTintMode != null) {
        
            mTintFilter = updateTintFilter(mTintFilter, state.mTint, state.mTintMode);
            return true;
        
        }
        
        return false;
    
    }
    
    /**
     * Called from the drawable's draw() method after the canvas has been set to
     * draw the shape at (0,0). Subclasses can override for special effects such
     * as multiple layers, stroking, etc.
     */
    protected void onDraw(Shape shape, Canvas canvas, Paint paint) {
        shape.draw(canvas, paint);
    }
    
    /**
     * Set the alpha level for this drawable [0..255]. Note that this drawable
     * also has a color in its paint, which has an alpha as well. These two
     * values are automatically combined during drawing. Thus if the color's
     * alpha is 75% (i.e. 192) and the drawable's alpha is 50% (i.e. 128), then
     * the combined alpha that will be used during drawing will be 37.5% (i.e.
     * 96).
     */
    @Override
    public void setAlpha(int alpha) {
    
        mShapeState.mAlpha = alpha;
        invalidateSelf();
    
    }
    
    @Override
    public void setColorFilter(ColorFilter cf) {
    
        mShapeState.mPaint.setColorFilter(cf);
        invalidateSelf();
    
    }
    
    @Override
    public void setDither(boolean dither) {
    
        mShapeState.mPaint.setDither(dither);
        invalidateSelf();
    
    }
    
    /**
     * Sets the intrinsic (default) height for this shape.
     *
     * @param height the intrinsic height (in pixels)
     */
    public void setIntrinsicHeight(int height) {
    
        mShapeState.mIntrinsicHeight = height;
        invalidateSelf();
    
    }
    
    /**
     * Sets the intrinsic (default) width for this shape.
     *
     * @param width the intrinsic width (in pixels)
     */
    public void setIntrinsicWidth(int width) {
    
        mShapeState.mIntrinsicWidth = width;
        invalidateSelf();
    
    }
    
    /**
     * Sets padding for this shape, defined by a Rect object. Define the padding
     * in the Rect object as: left, top, right, bottom.
     */
    public void setPadding(Rect padding) {
    
        if (padding == null)
            mShapeState.mPadding = null;
        else {
        
            if (mShapeState.mPadding == null)
                mShapeState.mPadding = new Rect();
            
            mShapeState.mPadding.set(padding);
        
        }
        
        invalidateSelf();
    
    }
    
    /**
     * Sets padding for the shape.
     *
     * @param left   padding for the left side (in pixels)
     * @param top    padding for the top (in pixels)
     * @param right  padding for the right side (in pixels)
     * @param bottom padding for the bottom (in pixels)
     */
    public void setPadding(int left, int top, int right, int bottom) {
    
        if ((bottom | left | right | top) == 0)
            mShapeState.mPadding = null;
        else {
        
            if (mShapeState.mPadding == null)
                mShapeState.mPadding = new Rect();
            
            mShapeState.mPadding.set(left, top, right, bottom);
        
        }
        
        invalidateSelf();
    
    }
    
    /**
     * Sets a ShaderFactory to which requests for a
     * {@link android.graphics.Shader} object will be made.
     *
     * @param fact an instance of your ShaderFactory implementation
     */
    public void setShaderFactory(ShaderFactory fact) {
        mShapeState.mShaderFactory = fact;
    }
    
    /** Sets the Shape of this ShapeDrawable. */
    public void setShape(Shape s) {
    
        mShapeState.mShape = s;
        updateShape();
    
    }
    
    @Override
    public void setTintList(ColorStateList tint) {
    
        mShapeState.mTint = tint;
        mTintFilter = updateTintFilter(mTintFilter, tint, mShapeState.mTintMode);
        
        invalidateSelf();
    
    }
    
    @Override
    public void setTintMode(PorterDuff.Mode tintMode) {
    
        mShapeState.mTintMode = tintMode;
        mTintFilter = updateTintFilter(mTintFilter, mShapeState.mTint, tintMode);
        
        invalidateSelf();
    
    }
    
    private void updateShape() {
    
        if (mShapeState.mShape != null) {
        
            final Rect r = getBounds();
            
            final int h = r.height();
            final int w = r.width();
            mShapeState.mShape.resize(w, h);
            
            if (mShapeState.mShaderFactory != null)
                mShapeState.mPaint.setShader(mShapeState.mShaderFactory.resize(w, h));
        
        }
        
        invalidateSelf();
    
    }
    
    /** Initializes the constant state from the values in the typed array. */
    private void updateStateFromTypedArray(Theme theme, TypedArray a, TypedValue[] values) throws XmlPullParserException {
    
        final ShapeState state = mShapeState;
        final Paint paint = state.mPaint;
        
        // Account for any configuration changes.
        state.mChangingConfigurations |= TypedArrayCompat.getChangingConfigurations(a);
        
        // Extract the theme attributes, if any.
        state.mThemeAttrs = TypedArrayCompat.extractThemeAttrs(a);
        
        int color = paint.getColor();
        color = a.getColor(R.styleable.ShapeDrawable_android_color, color);
        paint.setColor(color);
        
        boolean dither = paint.isDither();
        dither = a.getBoolean(R.styleable.ShapeDrawable_android_dither, dither);
        paint.setDither(dither);
        
        setIntrinsicHeight((int) a.getDimension(R.styleable.ShapeDrawable_android_height, state.mIntrinsicHeight));
        setIntrinsicWidth((int) a.getDimension(R.styleable.ShapeDrawable_android_width, state.mIntrinsicWidth));
        
        final int tintMode = a.getInt(R.styleable.ShapeDrawable_android_tintMode, -1);
        
        if (tintMode != -1)
            state.mTintMode = LollipopDrawable.parseTintMode(tintMode, Mode.SRC_IN);
        
        final ColorStateList tint = TypedArrayCompat.getColorStateList(theme, a, values,
            R.styleable.ShapeDrawable_android_tint);
        
        if (tint != null)
            state.mTint = tint;
    
    }
    
    /**
     * Base class defines a factory object that is called each time the drawable
     * is resized (has a new width or height). Its resize() method returns a
     * corresponding shader, or null. Implement this class if you'd like your
     * ShapeDrawable to use a special {@link android.graphics.Shader}, such as a
     * {@link android.graphics.LinearGradient}.
     */
    public static abstract class ShaderFactory {
    
        /**
         * Returns the Shader to be drawn when a Drawable is drawn. The
         * dimensions of the Drawable are passed because they may be needed to
         * adjust how the Shader is configured for drawing. This is called by
         * ShapeDrawable.setShape().
         *
         * @param width  the width of the Drawable being drawn
         * @param height the heigh of the Drawable being drawn
         * @return the Shader to be drawn
         */
        public abstract Shader resize(int width, int height);
    
    }
    
    /**
     * Defines the intrinsic properties of this ShapeDrawable's Shape.
     */
    final static class ShapeState extends ConstantState {
    
        int mAlpha = 255;
        int mChangingConfigurations;
        int mIntrinsicHeight;
        int mIntrinsicWidth;
        
        ColorStateList mTint = null;
        Mode mTintMode = DEFAULT_TINT_MODE;
        Paint mPaint;
        Rect mPadding;
        Shape mShape;
        ShaderFactory mShaderFactory;
        TypedValue[] mThemeAttrs;
        
        ShapeState(ShapeState orig) {
        
            if (orig != null) {
            
                mAlpha = orig.mAlpha;
                mIntrinsicHeight = orig.mIntrinsicHeight;
                mIntrinsicWidth = orig.mIntrinsicWidth;
                mPadding = orig.mPadding;
                mPaint = orig.mPaint;
                mShape = orig.mShape;
                mShaderFactory = orig.mShaderFactory;
                mThemeAttrs = orig.mThemeAttrs;
                mTint = orig.mTint;
                mTintMode = orig.mTintMode;
            
            } else
                mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        }
        
        @Override
        public boolean canApplyTheme() {
            return (mThemeAttrs != null);
        }
        
        @Override
        public int getChangingConfigurations() {
            return mChangingConfigurations;
        }
        
        @Override
        public Drawable newDrawable() {
            return new ShapeDrawable(this, null, null);
        }
        
        @Override
        public Drawable newDrawable(Resources res) {
            return new ShapeDrawable(this, res, null);
        }
        
        @Override
        public Drawable newDrawable(Resources res, Theme theme) {
            return new ShapeDrawable(this, res, theme);
        }
    
    }

}