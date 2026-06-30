package sp.phone.common;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.List;

import gov.anzong.androidnga.base.util.ContextUtils;
import gov.anzong.androidnga.common.PreferenceKey;
import sp.phone.mvp.model.entity.ThreadPageInfo;

/**
 * Created by Justwen on 2018/1/17.
 */

public class TopicHistoryManager {

    private Context mContext;

    private List<ThreadPageInfo> mTopicList;

    private static final int MAX_HISTORY_TOPIC_COUNT = 500;

    private static class SingleTonHolder {

        private static TopicHistoryManager sInstance = new TopicHistoryManager();
    }

    public static TopicHistoryManager getInstance() {
        return SingleTonHolder.sInstance;
    }

    private TopicHistoryManager() {
        mContext = ContextUtils.getContext();
        String topicStr = PreferenceManager.getDefaultSharedPreferences(mContext).getString(PreferenceKey.KEY_TOPIC_HISTORY, null);
        if (topicStr != null) {
            mTopicList = JSON.parseArray(topicStr, ThreadPageInfo.class);
        }
        if (mTopicList == null) {
            mTopicList = new ArrayList<>();
        }
    }

    public void addTopicHistory(ThreadPageInfo topic) {
        if (mTopicList.contains(topic)) {
            mTopicList.remove(topic);
        } else if (mTopicList.size() >= MAX_HISTORY_TOPIC_COUNT){
            mTopicList.remove(mTopicList.size() - 1);
        }
        mTopicList.add(0,topic);
        commit();
    }

    public ThreadPageInfo findTopicHistory(int tid) {
        for (ThreadPageInfo topic : mTopicList) {
            if (topic.getTid() == tid && topic.getPid() == 0) {
                return topic;
            }
        }
        return null;
    }

    public void updateTopicPage(int tid, int page) {
        if (tid == 0 || page <= 0) {
            return;
        }
        ThreadPageInfo topic = findTopicHistory(tid);
        if (topic != null && topic.getPage() != page) {
            topic.setPage(page);
            commit();
        }
    }

    public List<ThreadPageInfo> searchTopicHistory(String query) {
        if (TextUtils.isEmpty(query)) {
            return mTopicList;
        }
        String lowerQuery = query.toLowerCase();
        List<ThreadPageInfo> result = new ArrayList<>();
        for (ThreadPageInfo topic : mTopicList) {
            if (containsIgnoreCase(topic.getSubject(), lowerQuery)
                    || containsIgnoreCase(topic.getAuthor(), lowerQuery)
                    || containsIgnoreCase(topic.getLastPoster(), lowerQuery)
                    || String.valueOf(topic.getTid()).contains(query)) {
                result.add(topic);
            }
        }
        return result;
    }

    private boolean containsIgnoreCase(String value, String lowerQuery) {
        return value != null && value.toLowerCase().contains(lowerQuery);
    }

    public void removeTopicHistory(ThreadPageInfo topic) {
        if (mTopicList.contains(topic)) {
            mTopicList.remove(topic);
            commit();
        }
    }

    public void removeTopicHistory(int index) {
        mTopicList.remove(index);
        commit();
    }

    public List<ThreadPageInfo> getTopicHistoryList() {
        return mTopicList;
    }

    public void removeAllTopicHistory() {
        mTopicList.clear();
        commit();
    }

    private void commit() {
        String topicStr = JSON.toJSONString(mTopicList);
        PreferenceManager.getDefaultSharedPreferences(mContext)
                .edit()
                .putString(PreferenceKey.KEY_TOPIC_HISTORY,topicStr)
                .apply();
    }

}
