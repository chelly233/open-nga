package sp.phone.http.bean;

import java.util.List;
import java.util.Map;

import sp.phone.mvp.model.entity.ThreadPageInfo;

public class ThreadData {
    public static final String SOURCE_JSON = "JSON";
    public static final String SOURCE_XML = "XML";
    public static final String SOURCE_COMPACT_XML = "COMPACT_XML";
    public static final String SOURCE_WEB_HTML = "WEB_HTML";
    public static final String SOURCE_CACHE = "CACHE";

    private List<ThreadRowInfo> rowList;
    private ThreadPageInfo threadInfo;
    private Map<String, String> __F;
    private int __ROWS;
    private int rowNum;

    /**
     * 从服务端获取的原始数据
     */
    private String mRawData;

    private String mSource;

    private String mRequestDomain;

    private String mFallbackMessage;

    public List<ThreadRowInfo> getRowList() {
        return rowList;
    }

    public void setRowList(List<ThreadRowInfo> rowList) {
        this.rowList = rowList;
    }

    public ThreadPageInfo getThreadInfo() {
        return threadInfo;
    }

    public void setThreadInfo(ThreadPageInfo threadInfo) {
        this.threadInfo = threadInfo;
    }

    public Map<String, String> get__F() {
        return __F;
    }

    public void set__F(Map<String, String> __F) {
        this.__F = __F;
    }

    public int get__ROWS() {
        return __ROWS;
    }

    public void set__ROWS(int __ROWS) {
        this.__ROWS = __ROWS;
    }

    public int getRowNum() {
        return rowNum;
    }

    public void setRowNum(int rowNum) {
        this.rowNum = rowNum;
    }

    public String getRawData() {
        return mRawData;
    }

    public void setRawData(String rawData) {
        mRawData = rawData;
    }

    public String getSource() {
        return mSource;
    }

    public void setSource(String source) {
        mSource = source;
    }

    public String getRequestDomain() {
        return mRequestDomain;
    }

    public void setRequestDomain(String requestDomain) {
        mRequestDomain = requestDomain;
    }

    public String getFallbackMessage() {
        return mFallbackMessage;
    }

    public void setFallbackMessage(String fallbackMessage) {
        mFallbackMessage = fallbackMessage;
    }
}
