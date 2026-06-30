package sp.phone.common;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import sp.phone.mvp.model.entity.ThreadPageInfo;

@Entity(tableName = "topic_history")
public class TopicHistoryInfo {

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "tid")
    public int tid;

    @ColumnInfo(name = "author")
    public String author;

    @ColumnInfo(name = "fid")
    public int fid;

    @ColumnInfo(name = "author_id")
    public int authorId;

    @ColumnInfo(name = "last_poster")
    public String lastPoster;

    @ColumnInfo(name = "replies")
    public int replies;

    @ColumnInfo(name = "subject")
    public String subject;

    @ColumnInfo(name = "title_font")
    public String titleFont;

    @ColumnInfo(name = "type")
    public int type;

    @ColumnInfo(name = "topic_misc")
    public String topicMisc;

    @ColumnInfo(name = "page")
    public int page;

    @ColumnInfo(name = "post_date")
    public int postDate;

    @ColumnInfo(name = "board")
    public String board;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public static TopicHistoryInfo fromThreadPageInfo(ThreadPageInfo info) {
        TopicHistoryInfo history = new TopicHistoryInfo();
        history.tid = info.getTid();
        history.author = info.getAuthor();
        history.fid = info.getFid();
        history.authorId = info.getAuthorId();
        history.lastPoster = info.getLastPoster();
        history.replies = info.getReplies();
        history.subject = info.getSubject();
        history.titleFont = info.getTitleFont();
        history.type = info.getType();
        history.topicMisc = info.getTopicMisc();
        history.page = info.getPage();
        history.postDate = info.getPostDate();
        history.board = info.getBoard();
        history.updatedAt = System.currentTimeMillis();
        return history;
    }

    public ThreadPageInfo toThreadPageInfo() {
        ThreadPageInfo info = new ThreadPageInfo();
        info.setTid(tid);
        info.setAuthor(author);
        info.setFid(fid);
        info.setAuthorId(authorId);
        info.setLastPoster(lastPoster);
        info.setReplies(replies);
        info.setSubject(subject);
        info.setTitleFont(titleFont);
        info.setType(type);
        info.setTopicMisc(topicMisc);
        info.setPage(page);
        info.setPostDate(postDate);
        info.setBoard(board);
        return info;
    }
}
