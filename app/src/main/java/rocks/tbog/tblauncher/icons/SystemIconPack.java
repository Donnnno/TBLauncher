package rocks.tbog.tblauncher.icons;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.List;

import rocks.tbog.tblauncher.utils.DrawableUtils;
import rocks.tbog.tblauncher.utils.UserHandleCompat;

public class SystemIconPack implements IconPack<Void> {

    private static final String TAG = SystemIconPack.class.getSimpleName();

    @NonNull
    @Override
    public String getPackPackageName() {
        return "default";
    }

    @Override
    public void load(PackageManager packageManager) {
    }

    @Nullable
    @Override
    public Drawable getComponentDrawable(String componentName) {
        return null;
    }

    @Nullable
    @Override
    public Drawable getComponentDrawable(@NonNull Context ctx, @NonNull ComponentName componentName, @NonNull UserHandleCompat userHandle) {
        Drawable drawable = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                LauncherApps launcher = (LauncherApps) ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                List<LauncherActivityInfo> icons = launcher.getActivityList(componentName.getPackageName(), userHandle.getRealHandle());
                for (LauncherActivityInfo info : icons) {
                    if (info.getComponentName().equals(componentName)) {
                        drawable = info.getBadgedIcon(0);
                        break;
                    }
                }

                // This should never happen, let's just return the first icon
                if (drawable == null)
                    drawable = icons.get(0).getBadgedIcon(0);
            } else {
                drawable = ctx.getPackageManager().getActivityIcon(componentName);
            }
        } catch (PackageManager.NameNotFoundException | IndexOutOfBoundsException e) {
            Log.e(TAG, "Unable to find component " + componentName.toString() + e);
        }
        return drawable;
    }

    @NonNull
    @Override
    public BitmapDrawable applyBackgroundAndMask(@NonNull Context ctx, @NonNull Drawable icon) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int shape = DrawableUtils.SHAPE_TEARDROP;

            Bitmap outputBitmap;
            Canvas outputCanvas;
            Paint outputPaint;

            if (icon instanceof AdaptiveIconDrawable) {
                AdaptiveIconDrawable adaptiveIcon = (AdaptiveIconDrawable) icon;
                Drawable bgDrawable = adaptiveIcon.getBackground();
                Drawable fgDrawable = adaptiveIcon.getForeground();

                int layerSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 108f, ctx.getResources().getDisplayMetrics()));
                int iconSize = Math.round(layerSize / (1 + 2 * AdaptiveIconDrawable.getExtraInsetFraction()));
                int layerOffset = (layerSize - iconSize) / 2;

                // Create a bitmap of the icon to use it as the shader of the outputBitmap
                Bitmap iconBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
                Canvas iconCanvas = new Canvas(iconBitmap);

                // Stretch adaptive layers because they are 108dp and the icon size is 48dp
                bgDrawable.setBounds(-layerOffset, -layerOffset, iconSize + layerOffset, iconSize + layerOffset);
                bgDrawable.draw(iconCanvas);

                fgDrawable.setBounds(-layerOffset, -layerOffset, iconSize + layerOffset, iconSize + layerOffset);
                fgDrawable.draw(iconCanvas);

                outputBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
                outputCanvas = new Canvas(outputBitmap);
                outputPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                outputPaint.setShader(new BitmapShader(iconBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));

                DrawableUtils.setIconShape(outputCanvas, outputPaint, shape);
            }
            // If icon is not adaptive, put it in a white canvas to make it have a unified shape
            else {
                int iconSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, ctx.getResources().getDisplayMetrics()));

                outputBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
                outputCanvas = new Canvas(outputBitmap);
                outputPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                outputPaint.setColor(Color.WHITE);

                // Shrink icon to 70% of its size so that it fits the shape
                int topLeftCorner = Math.round(0.15f * iconSize);
                int bottomRightCorner = Math.round(0.85f * iconSize);
                icon.setBounds(topLeftCorner, topLeftCorner, bottomRightCorner, bottomRightCorner);

                DrawableUtils.setIconShape(outputCanvas, outputPaint, shape);
                icon.draw(outputCanvas);
            }
            return new BitmapDrawable(ctx.getResources(), outputBitmap);

        }

        if (icon instanceof BitmapDrawable)
            return (BitmapDrawable) icon;

        return new BitmapDrawable(ctx.getResources(), DrawableUtils.drawableToBitmap(icon));
    }

    @Nullable
    @Override
    public Collection<Void> getDrawableList() {
        return null;
    }

    @Nullable
    @Override
    public Drawable getDrawable(@NonNull Void aVoid) {
        return null;
    }
}
