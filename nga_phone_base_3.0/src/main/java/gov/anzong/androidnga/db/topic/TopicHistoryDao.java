package gov.anzong.androidnga.db.topic;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import sp.phone.common.TopicHistoryInfo;

@Dao
public interface TopicHistoryDao {

    @Query("SELECT * FROM topic_history ORDER BY updated_at DESC LIMIT :limit")
    List<TopicHistoryInfo> loadTopicHistory(int limit);

    @Query("SELECT * FROM topic_history WHERE tid = :tid LIMIT 1")
    TopicHistoryInfo findTopicHistory(int tid);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveTopicHistory(TopicHistoryInfo history);

    @Query("UPDATE topic_history SET page = :page, updated_at = :updatedAt WHERE tid = :tid")
    void updateTopicPage(int tid, int page, long updatedAt);

    @Query("DELETE FROM topic_history WHERE tid = :tid")
    void deleteTopicHistory(int tid);

    @Query("DELETE FROM topic_history")
    void deleteAllTopicHistory();

    @Query("DELETE FROM topic_history WHERE tid NOT IN (SELECT tid FROM topic_history ORDER BY updated_at DESC LIMIT :limit)")
    void trimTopicHistory(int limit);

    @Query("SELECT * FROM topic_history WHERE subject LIKE :query OR author LIKE :query OR last_poster LIKE :query OR CAST(tid AS TEXT) LIKE :query ORDER BY updated_at DESC LIMIT :limit")
    List<TopicHistoryInfo> searchTopicHistory(String query, int limit);
}
