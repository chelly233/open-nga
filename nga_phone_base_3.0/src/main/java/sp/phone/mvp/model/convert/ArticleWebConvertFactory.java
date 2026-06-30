package sp.phone.mvp.model.convert;

import android.text.Html;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sp.phone.http.bean.ThreadData;
import sp.phone.http.bean.ThreadRowInfo;
import sp.phone.mvp.model.entity.ThreadPageInfo;
import sp.phone.util.NLog;

public class ArticleWebConvertFactory {

    private static final String TAG = ArticleWebConvertFactory.class.getSimpleName();
    private static final String POST_PROC_MARKER = "commonui.postArg.proc(";

    public static ThreadData getArticleInfo(String html) {
        try {
            if (TextUtils.isEmpty(html)) {
                return null;
            }
            int tid = findInt(html, "__CURRENT_TID\\s*=\\s*(-?\\d+)", 0);
            int fid = findInt(html, "__CURRENT_FID\\s*=\\s*(-?\\d+)", 0);
            int pageRows = findInt(html, "__CURRENT_PAGE_POSTS\\s*=\\s*(-?\\d+)", 0);
            TopicDefaults topicDefaults = parseTopicDefaults(html);
            int replies = topicDefaults == null ? Math.max(0, pageRows - 1) : topicDefaults.replies;
            int totalRows = replies + 1;
            String subject = firstText(html, "<h1>\\s*<a[^>]*>(.*?)</a>\\s*</h1>");
            Map<Integer, JSONObject> users = parseUsers(html);

            List<ThreadRowInfo> rowList = new ArrayList<>();
            for (List<String> args : parsePostArgs(html)) {
                if (args.size() < 20) {
                    continue;
                }
                int floor = parseInt(normalizeArg(args.get(0)), -1);
                if (floor < 0) {
                    continue;
                }
                ThreadRowInfo row = new ThreadRowInfo();
                row.setLou(floor);
                row.setTid(tid);
                row.setFid(fid);
                row.setPid(parseInt(normalizeArg(args.get(10)), 0));
                row.setAuthorid(parseInt(normalizeArg(args.get(13)), 0));
                row.setPostdate(firstText(html, "<span id=['\"]postdate" + floor + "['\"][^>]*>(.*?)</span>"));
                row.setSubject(firstText(html, "<h3 id=['\"]postsubject" + floor + "['\"][^>]*>(.*?)</h3>"));
                row.setContent(firstText(html, "<(?:span|p) id=['\"]postcontent" + floor + "['\"][^>]*>(.*?)</(?:span|p)>"));
                row.setFromClient(normalizeArg(args.get(19)));
                int[] scores = parseScores(normalizeArg(args.get(15)));
                row.setScore(scores[0]);
                row.setScore_2(scores[1]);
                fillUserInfo(row, users.get(row.getAuthorid()));
                ArticleConvertFactory.buildRowContent(row);
                rowList.add(row);
            }
            if (rowList.isEmpty()) {
                return null;
            }

            ThreadPageInfo threadInfo = new ThreadPageInfo();
            threadInfo.setTid(tid);
            threadInfo.setFid(fid);
            threadInfo.setSubject(TextUtils.isEmpty(subject) ? rowList.get(0).getSubject() : subject);
            threadInfo.setAuthorId(rowList.get(0).getAuthorid());
            threadInfo.setAuthor(rowList.get(0).getAuthor());
            threadInfo.setReplies(replies);

            ThreadData data = new ThreadData();
            data.setRawData(html);
            data.setThreadInfo(threadInfo);
            data.setRowList(rowList);
            data.set__ROWS(totalRows <= 0 ? rowList.size() : totalRows);
            data.setRowNum(rowList.size());
            return data;
        } catch (Exception e) {
            NLog.e(TAG, "can not parse web html: " + e.getClass().getSimpleName() + " " + e.getMessage());
            return null;
        }
    }

    private static void fillUserInfo(ThreadRowInfo row, JSONObject user) {
        if (user == null) {
            row.setAuthor(String.valueOf(row.getAuthorid()));
            return;
        }
        row.setAuthor(user.getString("username"));
        row.setJs_escap_avatar(user.getString("avatar"));
        row.setYz(user.getString("yz"));
        row.setMuteTime(user.getString("mute_time"));
        row.setSignature(user.getString("signature"));
        row.setPostCount(user.getString("postnum"));
        row.setAurvrc(parseInt(user.getString("rvrc"), 0));
        row.setReputation(parseInt(user.getString("rvrc"), 0) / 10.0f);
    }

    private static Map<Integer, JSONObject> parseUsers(String html) {
        Map<Integer, JSONObject> users = new HashMap<>();
        int start = html.indexOf("commonui.userInfo.setAll(");
        if (start < 0) {
            return users;
        }
        start = html.indexOf('{', start);
        int end = findMatchingBrace(html, start);
        if (start < 0 || end <= start) {
            return users;
        }
        try {
            JSONObject object = JSON.parseObject(html.substring(start, end + 1));
            for (String key : object.keySet()) {
                Object value = object.get(key);
                if (value instanceof JSONObject) {
                    users.put(parseInt(key, 0), (JSONObject) value);
                }
            }
        } catch (RuntimeException e) {
            NLog.e(TAG, "can not parse web users: " + e.getMessage());
        }
        return users;
    }

    private static TopicDefaults parseTopicDefaults(String html) {
        int index = html.indexOf("commonui.postArg.setDefault(");
        if (index < 0) {
            return null;
        }
        int start = index + "commonui.postArg.setDefault(".length();
        int end = findMatchingParen(html, start - 1);
        if (end <= start) {
            return null;
        }
        List<String> args = splitArguments(html.substring(start, end));
        if (args.size() < 14) {
            return null;
        }
        TopicDefaults defaults = new TopicDefaults();
        defaults.replies = parseInt(normalizeArg(args.get(11)), 0);
        defaults.rowsPerPage = parseInt(normalizeArg(args.get(13)), 20);
        return defaults;
    }

    private static List<List<String>> parsePostArgs(String html) {
        List<List<String>> result = new ArrayList<>();
        int index = 0;
        while ((index = html.indexOf(POST_PROC_MARKER, index)) >= 0) {
            int start = index + POST_PROC_MARKER.length();
            int end = findMatchingParen(html, start - 1);
            if (end > start) {
                result.add(splitArguments(html.substring(start, end)));
            }
            index = Math.max(start, end + 1);
        }
        return result;
    }

    private static List<String> splitArguments(String raw) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        int depth = 0;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (quote != 0) {
                current.append(c);
                if (c == quote && (i == 0 || raw.charAt(i - 1) != '\\')) {
                    quote = 0;
                }
                continue;
            }
            if (c == '\'' || c == '"') {
                quote = c;
                current.append(c);
            } else if (c == '(') {
                depth++;
                current.append(c);
            } else if (c == ')') {
                depth--;
                current.append(c);
            } else if (c == ',' && depth == 0) {
                args.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        args.add(current.toString().trim());
        return args;
    }

    private static int findMatchingParen(String text, int openIndex) {
        char quote = 0;
        int depth = 0;
        for (int i = openIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote != 0) {
                if (c == quote && text.charAt(i - 1) != '\\') {
                    quote = 0;
                }
                continue;
            }
            if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '(') {
                depth++;
            } else if (c == ')' && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    private static int findMatchingBrace(String text, int openIndex) {
        char quote = 0;
        int depth = 0;
        for (int i = openIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote != 0) {
                if (c == quote && text.charAt(i - 1) != '\\') {
                    quote = 0;
                }
                continue;
            }
            if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    private static String firstText(String source, String regex) {
        Matcher matcher = Pattern.compile(regex, Pattern.DOTALL).matcher(source);
        if (!matcher.find()) {
            return "";
        }
        return Html.fromHtml(matcher.group(1).replaceAll("(?i)<br\\s*/?>", "<br>")).toString().trim();
    }

    private static int findInt(String source, String regex, int defaultValue) {
        Matcher matcher = Pattern.compile(regex).matcher(source);
        return matcher.find() ? parseInt(matcher.group(1), defaultValue) : defaultValue;
    }

    private static String normalizeArg(String value) {
        value = value.trim();
        if (value.length() >= 2 && ((value.startsWith("'") && value.endsWith("'"))
                || (value.startsWith("\"") && value.endsWith("\"")))) {
            return value.substring(1, value.length() - 1);
        }
        return "null".equals(value) ? "" : value;
    }

    private static int[] parseScores(String raw) {
        String[] parts = raw.split(",");
        return new int[]{
                parts.length > 0 ? parseInt(parts[0], 0) : 0,
                parts.length > 1 ? parseInt(parts[1], 0) : 0
        };
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return TextUtils.isEmpty(value) ? defaultValue : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static class TopicDefaults {
        int replies;
        int rowsPerPage;
    }
}
