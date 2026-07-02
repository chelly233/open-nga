package sp.phone.mvp.model;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.justwen.androidnga.cloud.CloudServerManager;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.anzong.androidnga.base.util.ContextUtils;
import gov.anzong.androidnga.base.util.ThreadUtils;
import gov.anzong.androidnga.http.OnHttpCallBack;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import sp.phone.http.retrofit.RetrofitHelper;
import sp.phone.http.retrofit.RetrofitService;
import sp.phone.mvp.contract.TopicListContract;
import sp.phone.mvp.model.convert.ErrorConvertFactory;
import sp.phone.mvp.model.convert.TopicConvertFactory;
import sp.phone.mvp.model.entity.ThreadPageInfo;
import sp.phone.mvp.model.entity.TopicListInfo;
import sp.phone.common.UserManagerImpl;
import sp.phone.param.TopicListParam;
import sp.phone.rxjava.BaseSubscriber;
import sp.phone.util.MD5Util;
import sp.phone.util.StringUtils;

/**
 * Created by Justwen on 2017/11/21.
 */

public class TopicListModel extends BaseModel implements TopicListContract.Model {

    private RetrofitService mService;

    private TopicConvertFactory mConvertFactory;

    public TopicListModel() {
        mService = (RetrofitService) RetrofitHelper.getInstance().getService(RetrofitService.class);
        mConvertFactory = new TopicConvertFactory();
    }

    @Override
    public void loadCache(OnHttpCallBack<TopicListInfo> callBack) {
        Observable.create((ObservableOnSubscribe<TopicListInfo>) emitter -> {
            String path = ContextUtils.getContext().getFilesDir().getAbsolutePath() + "/cache/";
            File[] cacheDirs = new File(path).listFiles();

            if (cacheDirs == null) {
                emitter.onError(new Exception());
            } else {
                TopicListInfo listInfo = new TopicListInfo();
                for (File dir : cacheDirs) {
                    File infoFile = new File(dir, dir.getName() + ".json");
                    if (!infoFile.exists()) {
                        continue;
                    }
                    String rawData = FileUtils.readFileToString(infoFile);
                    ThreadPageInfo pageInfo = JSON.parseObject(rawData, ThreadPageInfo.class);
                    if (pageInfo == null) {
                        CloudServerManager.putCrashData(ContextUtils.getContext(),"rawData", rawData);
                    } else {
                        listInfo.addThreadPage(JSON.parseObject(rawData, ThreadPageInfo.class));
                    }
                }
                emitter.onNext(listInfo);
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new BaseSubscriber<TopicListInfo>() {
                    @Override
                    public void onNext(TopicListInfo topicListInfo) {
                        callBack.onSuccess(topicListInfo);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        callBack.onError("读取缓存失败！");
                    }
                });
    }

    @Override
    public void removeTopic(ThreadPageInfo info, final OnHttpCallBack<String> callBack) {
        Map<String, String> fieldMap = new HashMap<>();
        fieldMap.put("__lib", "topic_favor");
        fieldMap.put("__act", "topic_favor");
        fieldMap.put("__output", "8");
        fieldMap.put("action", "del");
        fieldMap.put("page", String.valueOf(info.getPage()));
        String tidArray = String.valueOf(info.getTid());
        if (info.getPid() != 0) {
            tidArray = tidArray +  "_" + info.getPid();
        }
        fieldMap.put("tidarray", tidArray);
        mService.post(fieldMap)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new BaseSubscriber<String>() {
                    @Override
                    public void onNext(@NonNull String s) {
                        if (s.contains("操作成功")) {
                            callBack.onSuccess("操作成功！");
                        } else {
                            callBack.onError("操作失败!");
                        }
                    }
                });
    }

    @Override
    public void loadTopicList(final int page, TopicListParam param, final OnHttpCallBack<TopicListInfo> callBack) {
        String url = getUrl(page, param);
        mService.get(url)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .map(new Function<String, TopicListInfo>() {
                    @Override
                    public TopicListInfo apply(@NonNull String js) throws Exception {
                        TopicListInfo result = mConvertFactory.getTopicListInfo(js, page);
                        if (result != null) {
                            return result;
                        } else {
                            throw new Exception(ErrorConvertFactory.getErrorMessage(js));
                        }
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new BaseSubscriber<TopicListInfo>() {
                    @Override
                    public void onNext(@NonNull TopicListInfo topicListInfo) {
                        callBack.onSuccess(topicListInfo);
                    }

                    @Override
                    public void onError(@NonNull Throwable throwable) {
                        callBack.onError(ErrorConvertFactory.getErrorMessage(throwable));
                    }
                });
    }

    @Override
    public void loadTwentyFourList(TopicListParam param, final OnHttpCallBack<TopicListInfo> callBack, int totalPage) {
        List<Observable<String>> obsList = new ArrayList<Observable<String>>();
        for (int i = 1; i <= totalPage; i++) {
            obsList.add(mService.get(getUrl(i, param)));
        }
        Observable.concat(obsList).subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .map(new Function<String, TopicListInfo>() {
                    @Override
                    public TopicListInfo apply(@NonNull String js) throws Exception {
                        TopicListInfo result = mConvertFactory.getTopicListInfo(js, 0);
                        if (result != null) {
                            return result;
                        } else {
                            throw new Exception(ErrorConvertFactory.getErrorMessage(js));
                        }
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new BaseSubscriber<TopicListInfo>() {
                    @Override
                    public void onNext(@NonNull TopicListInfo topicListInfo) {
                        callBack.onSuccess(topicListInfo);
                    }

                    @Override
                    public void onError(@NonNull Throwable throwable) {
                        callBack.onError(ErrorConvertFactory.getErrorMessage(throwable));
                    }
                });
    }

    @Override
    public void removeCacheTopic(ThreadPageInfo info, OnHttpCallBack<String> callBack) {
        ThreadUtils.postOnSubThread(() -> {
            String path = ContextUtils.getContext().getFilesDir().getAbsolutePath() + "/cache/";
            File[] cacheDirs = new File(path).listFiles();
            if (cacheDirs == null) {
                callBack.onError(null);
                return;
            }
            try {
                for (File dir : cacheDirs) {
                    if (dir.getName().equals(String.valueOf(info.getTid()))) {
                        FileUtils.deleteDirectory(dir);
                        callBack.onSuccess(null);
                        return;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            callBack.onError(null);
        });
    }

    private String getUrl(int page, TopicListParam requestInfo) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("page", String.valueOf(page));
        if (0 != requestInfo.authorId) {
            params.put("authorid", String.valueOf(requestInfo.authorId));
        }
        if (requestInfo.searchPost != 0) {
            params.put("searchpost", String.valueOf(requestInfo.searchPost));
        }
        if (requestInfo.favor != 0) {
            params.put("favor", String.valueOf(requestInfo.favor));
        }
        if (requestInfo.content != 0) {
            params.put("content", String.valueOf(requestInfo.content));
        }
        if (!StringUtils.isEmpty(requestInfo.author)) {
            try {
                if (requestInfo.author.endsWith("&searchpost=1")) {
                    params.put("author", URLEncoder.encode(
                            requestInfo.author.substring(0, requestInfo.author.length() - 13),
                            "GBK"));
                    params.put("searchpost", "1");
                } else {
                    params.put("author", URLEncoder.encode(requestInfo.author, "GBK"));
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            if (requestInfo.stid != 0) {
                params.put("stid", String.valueOf(requestInfo.stid));
            } else if (0 != requestInfo.fid) {
                params.put("fid", String.valueOf(requestInfo.fid));
            }
            if (!StringUtils.isEmpty(requestInfo.key)) {
                params.put("key", StringUtils.encodeUrl(requestInfo.key, "UTF-8"));
            }
            if (!StringUtils.isEmpty(requestInfo.fidGroup)) {
                params.put("fidgroup", requestInfo.fidGroup);
            }
        }
        if (requestInfo.recommend == 1) {
            params.put("recommend", "1");
            params.put("order_by", "postdatedesc");
        }
        params.put("app_id", "1010");
        params.put("t", String.valueOf(System.currentTimeMillis() / 1000L));

        String accessUid = UserManagerImpl.getInstance().getUserId();
        String accessToken = UserManagerImpl.getInstance().getCid();
        params.put("access_uid", StringUtils.isEmpty(accessUid) ? "" : accessUid);
        params.put("access_token", StringUtils.isEmpty(accessToken) ? "" : accessToken);
        params.put("sign", buildSign(params));

        StringBuilder jsonUri = new StringBuilder(getAvailableDomain()).append("/thread.php?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            jsonUri.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        jsonUri.append("lite=js&noprefix");
        return jsonUri.toString();
    }

    private String buildSign(Map<String, String> params) {
        StringBuilder builder = new StringBuilder();
        builder.append(params.get("app_id"));
        builder.append(params.get("access_uid"));
        builder.append(params.get("access_token"));
        if (params.containsKey("authorid")) {
            builder.append("authorid=").append(params.get("authorid")).append("&");
        }
        if (params.containsKey("searchpost")) {
            builder.append("searchpost=").append(params.get("searchpost")).append("&");
        }
        if (params.containsKey("favor")) {
            builder.append("favor=").append(params.get("favor")).append("&");
        }
        if (params.containsKey("content")) {
            builder.append("content=").append(params.get("content")).append("&");
        }
        if (params.containsKey("author")) {
            builder.append("author=").append(params.get("author")).append("&");
        }
        if (params.containsKey("stid")) {
            builder.append("stid=").append(params.get("stid")).append("&");
        } else if (params.containsKey("fid")) {
            builder.append("fid=").append(params.get("fid")).append("&");
        }
        if (params.containsKey("key")) {
            builder.append("key=").append(params.get("key")).append("&");
        }
        if (params.containsKey("fidgroup")) {
            builder.append("fidgroup=").append(params.get("fidgroup")).append("&");
        }
        if (params.containsKey("recommend")) {
            builder.append("recommend=").append(params.get("recommend")).append("&");
        }
        if (params.containsKey("order_by")) {
            builder.append("order_by=").append(params.get("order_by")).append("&");
        }
        builder.append("page=").append(params.get("page"));
        builder.append("392e916a6d1d8b7523e2701470000c30bc2165a1");
        builder.append(params.get("t"));
        return MD5Util.MD5(builder.toString());
    }
}
