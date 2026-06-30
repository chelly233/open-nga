package sp.phone.ui.adapter;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import gov.anzong.androidnga.R;
import sp.phone.common.NoteManangerImpl;
import sp.phone.common.PhoneConfiguration;
import sp.phone.common.TopicHistoryManager;
import sp.phone.mvp.model.entity.ThreadPageInfo;
import sp.phone.param.TopicTitleHelper;
import sp.phone.rxjava.RxUtils;
import sp.phone.theme.ThemeManager;

public class TopicListAdapter extends BaseAppendableAdapter<ThreadPageInfo, TopicListAdapter.TopicViewHolder> {

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

    public class TopicViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.num)
        public TextView num;

        @BindView(R.id.title)
        public TextView title;

        @BindView(R.id.history_page)
        public TextView historyPage;

        @BindView(R.id.author)
        public TextView author;

        @BindView(R.id.last_reply)
        public TextView lastReply;

        public TopicViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
