package gov.anzong.androidnga.gallery;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.reactivestreams.Subscription;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import gov.anzong.androidnga.base.util.ContextUtils;
import gov.anzong.androidnga.base.util.DeviceUtils;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import sp.phone.http.OnSimpleHttpCallBack;
import sp.phone.rxjava.BaseSubscriber;
import sp.phone.util.ActivityUtils;

public class SaveImageTask {

    private static final String RELATIVE_DIR = Environment.DIRECTORY_PICTURES + "/nga_open_source";

    private Context mContext;

    private int mDownloadCount;

    /** Android 9 及以下保存的传统公共相册目录 */
    private static final String PATH_IMAGES = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/nga_open_source/";

    private Subscription mSubscription;

    public SaveImageTask() {
        mContext = ContextUtils.getContext();
    }

    public static class DownloadResult {

        File file;

        String url;

        public DownloadResult(File file, String url) {
            this.file = file;
            this.url = url;
        }
    }

    public void execute(OnSimpleHttpCallBack<DownloadResult> callBack, String... urls) {

        if (isRunning()) {
            ActivityUtils.showToast("图片正在下载，防止风怒！！");
            return;
        }

        mDownloadCount = 0;
        Observable.fromArray(urls)
                .observeOn(Schedulers.io())
                .map(url -> {
                    File file = Glide
                            .with(mContext)
                            .load(url)
                            .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                            .get();
                    return new DownloadResult(file, url);
                })
                .map(result -> {
                    // 把下载到的缓存文件保存进系统相册。result.file 仍保留为可分享的缓存文件。
                    saveToGallery(result.file, buildDisplayName(result.url));
                    return result;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new BaseSubscriber<DownloadResult>() {
                    @Override
                    public void onNext(DownloadResult result) {
                        mDownloadCount++;
                        if (mDownloadCount == urls.length) {
                            ActivityUtils.showToast(urls.length > 1 ? "所有图片已保存到相册" : "已保存到相册");
                        }
                        callBack.onResult(result);
                    }

                    @Override
                    public void onComplete() {
                        mSubscription = null;
                    }

                    @Override
                    public void onSubscribe(Subscription subscription) {
                        super.onSubscribe(subscription);
                        mSubscription = subscription;
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        mSubscription = null;
                        ActivityUtils.showToast("下载失败");
                    }

                });
    }

    private static String buildDisplayName(String url) {
        String suffix = url.substring(url.lastIndexOf('.'));
        int questionMarkPosition = suffix.lastIndexOf('?');
        if (questionMarkPosition != -1) {
            suffix = suffix.substring(0, questionMarkPosition);
        }
        return System.currentTimeMillis() + suffix;
    }

    private static String guessMimeType(String displayName) {
        String lower = displayName.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        } else if (lower.endsWith(".gif")) {
            return "image/gif";
        } else if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    /**
     * 保存图片到系统相册。
     * Android 10+（scoped storage）通过 MediaStore 写入，无需存储权限；
     * Android 9 及以下沿用传统公共目录 + 媒体扫描。
     */
    private void saveToGallery(File source, String displayName) throws Exception {
        if (DeviceUtils.isGreaterEqual_10_0()) {
            ContentResolver resolver = mContext.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, guessMimeType(displayName));
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_DIR);
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);

            Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            Uri item = resolver.insert(collection, values);
            if (item == null) {
                throw new Exception("MediaStore insert failed");
            }
            try (InputStream in = new java.io.FileInputStream(source);
                 OutputStream out = resolver.openOutputStream(item)) {
                IOUtils.copy(in, out);
            }
            values.clear();
            values.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(item, values, null, null);
        } else {
            File target = new File(PATH_IMAGES, displayName);
            FileUtils.copyFile(source, target);
            mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(target)));
        }
    }

    private boolean isRunning() {
        return mSubscription != null;
    }


}
