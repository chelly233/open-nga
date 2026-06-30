package sp.phone.common;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.List;

import gov.anzong.androidnga.base.util.ContextUtils;
import gov.anzong.androidnga.common.PreferenceKey;
import gov.anzong.androidnga.db.AppDatabase;
import gov.anzong.androidnga.db.topic.TopicHistoryDao;
import sp.phone.mvp.model.entity.ThreadPageInfo;

/**
 * Created by Justwen on 2018/1/17.
 */

public class TopicHistoryManager {

    private Context mContext;

    private List<ThreadPageInfo> mTopicList;

    private TopicHistoryDao mTopicHistoryDao;

    private static final int MAX_HISTORY_TOPIC_COUNT = 500;

    private static final long UPDATE_PAGE_INTERVAL_MS = 1000;

    private int mLastUpdatedTid;

    private int mLastUpdatedPage;

    private long mLastUpdatePageTime;

    private static class SingleTonHolder {

        private static TopicHistoryManager sInstance = new TopicHistoryManager();
    }

    public static TopicHistoryManager getInstance() {
        return SingleTonHolder.sInstance;
    }

    private TopicHistoryManager() {
        mContext = ContextUtils.getContext();
        mTopicHistoryDao = AppDatabase.getInstance().topicHistoryDao();
        importLegacyTopicHistory();
        reloadTopicHistory();
    }

    private void importLegacyTopicHistory() {
        String topicStr = PreferenceManager.getDefaultSharedPreferences(mContext).getString(PreferenceKey.KEY_TOPIC_HISTORY, null);
        if (TextUtils.isEmpty(topicStr)) {
            return;
        }
        List<ThreadPageInfo> legacyTopicList = JSON.parseArray(topicStr, ThreadPageInfo.class);
        if (legacyTopicList != null) {
            long updatedAt = System.currentTimeMillis();
            for (ThreadPageInfo topic : legacyTopicList) {
                TopicHistoryInfo history = TopicHistoryInfo.fromThreadPageInfo(topic);
                history.updatedAt = updatedAt--;
                mTopicHistoryDao.saveTopicHistory(history);
            }
            mTopicHistoryDao.trimTopicHistory(MAX_HISTORY_TOPIC_COUNT);
        }
        PreferenceManager.getDefaultSharedPreferences(mContext)
                .edit()
                .remove(PreferenceKey.KEY_TOPIC_HISTORY)
                .apply();
    }

    private void reloadTopicHistory() {
        mTopicList = toThreadPageInfoList(mTopicHistoryDao.loadTopicHistory(MAX_HISTORY_TOPIC_COUNT));
    }

    public void addTopicHistory(ThreadPageInfo topic) {
        if (topic == null || topic.getTid() == 0 || topic.getPid() != 0) {
            return;
        }
        mTopicHistoryDao.saveTopicHistory(TopicHistoryInfo.fromThreadPageInfo(topic));
        mTopicHistoryDao.trimTopicHistory(MAX_HISTORY_TOPIC_COUNT);
        reloadTopicHistory();
    }

    public ThreadPageInfo findTopicHistory(int tid) {
        TopicHistoryInfo history = mTopicHistoryDao.findTopicHistory(tid);
        return history == null ? null : history.toThreadPageInfo();
    }

    public void updateTopicPage(int tid, int page) {
        if (tid == 0 || page <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        if (mLastUpdatedTid == tid
                && mLastUpdatedPage == page
                && now - mLastUpdatePageTime < UPDATE_PAGE_INTERVAL_MS) {
            return;
        }
        mLastUpdatedTid = tid;
        mLastUpdatedPage = page;
        mLastUpdatePageTime = now;
        mTopicHistoryDao.updateTopicPage(tid, page, System.currentTimeMillis());
        reloadTopicHistory();
    }

    public List<ThreadPageInfo> searchTopicHistory(String query) {
        if (TextUtils.isEmpty(query)) {
            return mTopicList;
        }
        String likeQuery = "%" + query + "%";
        return toThreadPageInfoList(mTopicHistoryDao.searchTopicHistory(likeQuery, MAX_HISTORY_TOPIC_COUNT));
    }

    public void removeTopicHistory(ThreadPageInfo topic) {
        if (topic != null) {
            mTopicHistoryDao.deleteTopicHistory(topic.getTid());
            reloadTopicHistory();
        }
    }

    public void removeTopicHistory(int index) {
        if (index >= 0 && index < mTopicList.size()) {
            removeTopicHistory(mTopicList.get(index));
        }
    }

    public List<ThreadPageInfo> getTopicHistoryList() {
        reloadTopicHistory();
        return mTopicList;
    }

    public void removeAllTopicHistory() {
        mTopicHistoryDao.deleteAllTopicHistory();
        reloadTopicHistory();
    }

    private List<ThreadPageInfo> toThreadPageInfoList(List<TopicHistoryInfo> historyList) {
        List<ThreadPageInfo> topicList = new ArrayList<>();
        for (TopicHistoryInfo history : historyList) {
            topicList.add(history.toThreadPageInfo());
        }
        return topicList;
    }

}
