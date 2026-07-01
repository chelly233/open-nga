package sp.phone.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import gov.anzong.androidnga.R;
import gov.anzong.androidnga.core.util.ImagePreviewUtils;
import gov.anzong.androidnga.gallery.ImageZoomActivity;
import gov.anzong.androidnga.util.GlideApp;
import sp.phone.common.NoteManangerImpl;
import sp.phone.common.PhoneConfiguration;
import sp.phone.common.TopicHistoryManager;
import sp.phone.mvp.model.entity.ThreadPageInfo;
import sp.phone.param.TopicTitleHelper;
import sp.phone.rxjava.RxUtils;
import sp.phone.theme.ThemeManager;

public class TopicListAdapter extends BaseAppendableAdapter<ThreadPageInfo, TopicListAdapter.TopicViewHolder> {

    private static final int PREVIEW_IMAGE_COUNT = 3;

    private static final int PREVIEW_IMAGE_CORNER_DP = 4;

    private static final Pattern NGA_IMG_PATTERN = Pattern.compile("(?is)\\[img(?:=[^\\]]*)?\\](.*?)\\[/img\\]");

    private static final Pattern HTML_IMG_PATTERN = Pattern.compile("(?is)<img\\b[^>]*\\bsrc=[\"']?([^\"'\\s>]+)");

    private static final Pattern DIRECT_IMAGE_PATTERN = Pattern.compile("(?i)(?:https?:)?//\\S+?\\.(?:jpg|jpeg|png|webp|gif)(?:\\.medium\\.jpg|\\.thumb\\.jpg|\\?\\S*)?|(?:\\./|/)?attachments/\\S+?\\.(?:jpg|jpeg|png|webp|gif)(?:\\.medium\\.jpg|\\.thumb\\.jpg|\\?\\S*)?");

    private static final Pattern IMAGE_FILE_PATTERN = Pattern.compile("(?i)\\S+\\.(?:jpg|jpeg|png|webp|gif)(?:\\.medium\\.jpg|\\.thumb\\.jpg|\\?\\S*)?");

    private boolean mShowHistoryPage;

    private String mHighlightKeyword;

    public TopicListAdapter(Context context) {
        super(context);
    }

    public void setShowHistoryPage(boolean showHistoryPage) {
        mShowHistoryPage = showHistoryPage;
    }

    public void setHighlightKeyword(String highlightKeyword) {
        mHighlightKeyword = highlightKeyword;
    }

    @Override
    public TopicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TopicViewHolder viewHolder = new TopicViewHolder(LayoutInflater.from(mContext).inflate(R.layout.list_topic, parent, false));
        viewHolder.title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, PhoneConfiguration.getInstance().getTopicTitleSize());
        RxUtils.clicks(viewHolder.itemView, mOnClickListener);
        viewHolder.itemView.setOnLongClickListener(mOnLongClickListener);
        return viewHolder;
    }

    @Override
    public void setData(List<ThreadPageInfo> dataList) {
        if (dataList == null) {
            super.setData(null);
        } else {
            super.appendData(dataList);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull TopicViewHolder holder, int position) {

        ThreadPageInfo info = getItem(position);
        info.setPosition(position);
        holder.itemView.setTag(info);

        handleJsonList(holder, info);
        if (!PhoneConfiguration.getInstance().useSolidColorBackground()) {
            holder.itemView.setBackgroundResource(ThemeManager.getInstance().getBackgroundColor(position));
        }
    }

    private void handleJsonList(TopicViewHolder holder, ThreadPageInfo entry) {
        if (entry == null) {
            return;
        }
        String userNote = NoteManangerImpl.Companion.getInstance().getNoteFromListByName(entry.getAuthor());
        String authorName = entry.getAuthor();
        if (userNote != null){
            authorName += "(" + userNote + ")";
        }
        holder.author.setText(authorName);
        holder.lastReply.setText(entry.getLastPoster());
        holder.num.setText(String.valueOf(entry.getReplies()));
        holder.title.setText(TopicTitleHelper.handleTitleFormat(entry, mHighlightKeyword));
        bindPreviewImages(holder, entry);
        if (mShowHistoryPage) {
            holder.historyPage.setVisibility(View.VISIBLE);
            ThreadPageInfo history = TopicHistoryManager.getInstance().findTopicHistory(entry.getTid());
            int page = history == null ? entry.getPage() : history.getPage();
            entry.setPage(Math.max(page, 1));
            holder.historyPage.setText(mContext.getString(R.string.topic_history_last_page, entry.getPage()));
        } else {
            holder.historyPage.setVisibility(View.GONE);
        }
    }

    private void bindPreviewImages(TopicViewHolder holder, ThreadPageInfo entry) {
        if (!PhoneConfiguration.getInstance().isTopicListPreviewImageEnabled()) {
            clearPreviewImages(holder);
            return;
        }
        List<String> imageUrls = findImageUrls(entry);
        if (imageUrls.isEmpty()) {
            clearPreviewImages(holder);
            return;
        }
        holder.previewContainer.setVisibility(View.VISIBLE);
        String[] allUrls = imageUrls.toArray(new String[0]);
        for (int i = 0; i < holder.previewImages.length; i++) {
            ImageView imageView = holder.previewImages[i];
            if (i >= imageUrls.size() || i >= PREVIEW_IMAGE_COUNT) {
                imageView.setVisibility(View.GONE);
                GlideApp.with(mContext).clear(imageView);
                imageView.setOnClickListener(null);
                continue;
            }
            String imageUrl = imageUrls.get(i);
            imageView.setVisibility(View.VISIBLE);
            GlideApp.with(mContext)
                    .load(ImagePreviewUtils.previewUrl(imageUrl))
                    .transform(new CenterCrop(), new RoundedCorners(dpToPx(PREVIEW_IMAGE_CORNER_DP)))
                    .into(imageView);
            imageView.setOnClickListener(v -> openImageViewer(allUrls, imageUrl));
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                mContext.getResources().getDisplayMetrics());
    }

    private void clearPreviewImages(TopicViewHolder holder) {
        holder.previewContainer.setVisibility(View.GONE);
        for (ImageView imageView : holder.previewImages) {
            imageView.setVisibility(View.GONE);
            imageView.setOnClickListener(null);
            GlideApp.with(mContext).clear(imageView);
        }
    }

    private void openImageViewer(String[] imageUrls, String currentUrl) {
        Intent intent = new Intent(mContext, ImageZoomActivity.class);
        intent.putExtra(ImageZoomActivity.KEY_GALLERY_URLS, imageUrls);
        intent.putExtra(ImageZoomActivity.KEY_GALLERY_CUR_URL, currentUrl);
        mContext.startActivity(intent);
    }

    private List<String> findImageUrls(ThreadPageInfo entry) {
        List<String> imageUrls = new ArrayList<>();
        List<String> previewImages = entry.getPreviewImages();
        if (previewImages != null) {
            for (String previewImage : previewImages) {
                String imageUrl = normalizeImageUrl(previewImage);
                if (imageUrl != null && !imageUrls.contains(imageUrl)) {
                    imageUrls.add(imageUrl);
                }
            }
        }
        ThreadPageInfo.ReplyInfo replyInfo = entry.getReplyInfo();
        if (replyInfo == null || replyInfo.getContent() == null) {
            return imageUrls;
        }
        String content = replyInfo.getContent();
        collectImageUrls(imageUrls, NGA_IMG_PATTERN.matcher(content), 1);
        collectImageUrls(imageUrls, HTML_IMG_PATTERN.matcher(content), 1);
        collectImageUrls(imageUrls, DIRECT_IMAGE_PATTERN.matcher(content), 0);
        return imageUrls;
    }

    private void collectImageUrls(List<String> imageUrls, Matcher matcher, int group) {
        while (matcher.find()) {
            String raw = group == 0 ? matcher.group() : matcher.group(group);
            String imageUrl = normalizeImageUrl(raw);
            if (imageUrl != null && !imageUrls.contains(imageUrl)) {
                imageUrls.add(imageUrl);
            }
        }
    }

    private String normalizeImageUrl(String raw) {
        if (raw == null) {
            return null;
        }
        raw = raw.trim().replace("&amp;", "&");
        if (raw.isEmpty()) {
            return null;
        }
        if (raw.startsWith("//")) {
            return "https:" + raw;
        }
        if (raw.startsWith("./")) {
            return "https://img.nga.178.com/attachments" + raw.substring(1);
        }
        if (raw.startsWith(".")) {
            return "https://img.nga.178.com/attachments" + raw;
        }
        if (raw.startsWith("/attachments/")) {
            return "https://img.nga.178.com" + raw;
        }
        if (raw.startsWith("/")) {
            return "https://img.nga.178.com/attachments" + raw;
        }
        if (raw.startsWith("attachments/")) {
            return "https://img.nga.178.com/" + raw;
        }
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            return raw;
        }
        if (IMAGE_FILE_PATTERN.matcher(raw).matches()) {
            return "https://img.nga.178.com/attachments/" + raw;
        }
        return null;
    }

    public class TopicViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.num)
        public TextView num;

        @BindView(R.id.title)
        public TextView title;

        @BindView(R.id.history_page)
        public TextView historyPage;

        @BindView(R.id.topic_preview_container)
        public LinearLayout previewContainer;

        @BindView(R.id.topic_preview_image_1)
        public ImageView previewImage1;

        @BindView(R.id.topic_preview_image_2)
        public ImageView previewImage2;

        @BindView(R.id.topic_preview_image_3)
        public ImageView previewImage3;

        public ImageView[] previewImages;

        @BindView(R.id.author)
        public TextView author;

        @BindView(R.id.last_reply)
        public TextView lastReply;

        public TopicViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            previewImages = new ImageView[]{previewImage1, previewImage2, previewImage3};
        }
    }
}
