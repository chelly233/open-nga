package sp.phone.mvp.model;

import android.text.TextUtils;

import com.trello.rxlifecycle2.android.FragmentEvent;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import gov.anzong.androidnga.base.util.ContextUtils;
import gov.anzong.androidnga.base.util.ThreadUtils;
import gov.anzong.androidnga.base.util.ToastUtils;
import gov.anzong.androidnga.http.OnHttpCallBack;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import sp.phone.common.UserManagerImpl;
import sp.phone.http.bean.ThreadData;
import sp.phone.http.bean.ThreadRowInfo;
import sp.phone.http.retrofit.RetrofitHelper;
import sp.phone.http.retrofit.RetrofitService;
import sp.phone.mvp.contract.ArticleListContract;
import sp.phone.mvp.model.convert.ArticleConvertFactory;
import sp.phone.mvp.model.convert.ArticleWebConvertFactory;
import sp.phone.mvp.model.convert.ArticleXmlConvertFactory;
import sp.phone.mvp.model.convert.ErrorConvertFactory;
import sp.phone.param.ArticleListParam;
import sp.phone.rxjava.BaseSubscriber;
import sp.phone.util.NLog;

/**
 * 加载帖子内容
 * Created by Justwen on 2017/7/10.
 */

class ArticleListModel : BaseModel(), ArticleListContract.Model {

    private val TAG = ArticleListModel::class.simpleName
    private val WEB_USER_AGENT = "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 Mobile Safari/537.36"

    private lateinit var mService: RetrofitService

    private val mWebPageCache = object : LinkedHashMap<String, ThreadData>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ThreadData>?): Boolean {
            return size > 8
        }
    }

    init {
        mService =
            RetrofitHelper.getInstance().getService(RetrofitService::class.java) as RetrofitService
    }

    fun getUrl(param: ArticleListParam): String {
        return getReadUrl(param, "&__output=8&noprefix&v2")
    }

    private fun getXmlUrl(param: ArticleListParam): String {
        return getReadUrl(param, "&lite=xml&noprefix&v2")
    }

    private fun getCompactXmlUrl(param: ArticleListParam): String {
        return getReadUrl(param, "&__output=10&noprefix&v2")
    }

    private fun getWebUrl(param: ArticleListParam): String {
        var page = param.page;
        var tid = param.tid;
        var pid = param.pid;
        var authorId = param.authorId;
        var url = getAvailableDomain() + "/read.php?" + "&page=" + page;
        if (tid != 0) {
            url = url + "&tid=" + tid;
        }
        if (pid != 0) {
            url = url + "&pid=" + pid;
        }
        if (authorId != 0) {
            url = url + "&authorid=" + authorId;
        }
        return url;
    }

    private fun getWebCacheKey(param: ArticleListParam): String {
        return "tid=${param.tid};pid=${param.pid};author=${param.authorId};page=${param.page}"
    }

    private fun getReadUrl(param: ArticleListParam, output: String): String {
        var page = param.page;
        var tid = param.tid;
        var pid = param.pid;
        var authorId = param.authorId;
        var url =
            getAvailableDomain() + "/read.php?" + "&page=" + page + output + "&__inchst=UTF8";
        if (tid != 0) {
            url = url + "&tid=" + tid;
        }
        if (pid != 0) {
            url = url + "&pid=" + pid;
        }

        if (authorId != 0) {
            url = url + "&authorid=" + authorId;
        }

        return url;

    }


    override fun loadPage(param: ArticleListParam, callBack: OnHttpCallBack<ThreadData>) {
        loadPage(param, null, callBack);
    }

    override fun loadPage(
        param: ArticleListParam,
        header: MutableMap<String, String>?,
        callBack: OnHttpCallBack<ThreadData>
    ) {
        val fallbackMessages = mutableListOf<String>()
        requestJsonPage(getUrl(param), header)
            .onErrorResumeNext { throwable: Throwable ->
                if (throwable is ServerException) {
                    recordFallback(fallbackMessages, ThreadData.SOURCE_JSON, throwable)
                    requestXmlPage(getXmlUrl(param), header)
                        .onErrorResumeNext { xmlThrowable: Throwable ->
                            if (xmlThrowable is ServerException) {
                                recordFallback(fallbackMessages, ThreadData.SOURCE_XML, xmlThrowable)
                                requestXmlPage(getCompactXmlUrl(param), header)
                                    .onErrorResumeNext { compactXmlThrowable: Throwable ->
                                        if (compactXmlThrowable is ServerException) {
                                            recordFallback(fallbackMessages, ThreadData.SOURCE_COMPACT_XML, compactXmlThrowable)
                                            requestWebPage(param, header, getWebCacheKey(param))
                                        } else {
                                            Observable.error(compactXmlThrowable)
                                        }
                                    }
                            } else {
                                Observable.error(xmlThrowable)
                            }
                        }
                } else {
                    Observable.error(throwable)
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .compose(getLifecycleProvider().bindUntilEvent(FragmentEvent.DETACH))
            .subscribe(object : BaseSubscriber<ThreadData>() {
                override fun onNext(threadData: ThreadData) {
                    fillDebugInfo(param, threadData, fallbackMessages)
                    callBack.onSuccess(threadData)
                    UserManagerImpl.getInstance().putAvatarUrl(threadData)
                    if (threadData.source == ThreadData.SOURCE_WEB_HTML) {
                        prefetchNextWebPage(param, header)
                    }
                }

                override fun onError(throwable: Throwable) {
                    recordFallback(fallbackMessages, ThreadData.SOURCE_WEB_HTML, throwable)
                    NLog.e(TAG, "article parse failed tid=${param.tid}, page=${param.page}, domain=${getAvailableDomain()}, fallback=${fallbackMessages.joinToString(" -> ")}")
                    callBack.onError(ErrorConvertFactory.getErrorMessage(throwable), throwable)
                }
            })
    }

    private fun recordFallback(messages: MutableList<String>, source: String, throwable: Throwable) {
        val message = throwable.message ?: throwable.javaClass.simpleName
        messages.add("$source: $message")
    }

    private fun fillDebugInfo(param: ArticleListParam, data: ThreadData, fallbackMessages: List<String>) {
        data.requestDomain = getAvailableDomain()
        data.fallbackMessage = fallbackMessages.joinToString(" -> ")
        NLog.d(TAG, "article source=${data.source}, tid=${param.tid}, page=${param.page}, domain=${data.requestDomain}, rows=${data.rowNum}/${data.get__ROWS()}, fallback=${data.fallbackMessage}")
    }

    private fun requestJsonPage(
        url: String,
        header: MutableMap<String, String>?
    ): Observable<ThreadData> {
        return mService.post(url, header, HashMap<String, String>())
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.newThread())
            .compose(getLifecycleProvider().bindUntilEvent(FragmentEvent.DETACH))
            .map { s ->
                val time = System.currentTimeMillis()
                val data = ArticleConvertFactory.getArticleInfo(s)
                NLog.e(TAG, "time = ${System.currentTimeMillis() - time}")
                if (data == null) {
                    val errorMsg = ErrorConvertFactory.getErrorMessage(s)
                    if (errorMsg != null) {
                        throw Exception(errorMsg)
                    } else {
                        throw ServerException("NGA后台抽风了，请尝试右上角菜单中的使用内置浏览器打开")
                    }
                } else {
                    data.source = ThreadData.SOURCE_JSON
                    data
                }
            }
    }

    private fun requestXmlPage(
        url: String,
        header: MutableMap<String, String>?
    ): Observable<ThreadData> {
        return mService.post(url, header, HashMap<String, String>())
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.newThread())
            .compose(getLifecycleProvider().bindUntilEvent(FragmentEvent.DETACH))
            .map { s ->
                val time = System.currentTimeMillis()
                val data = ArticleXmlConvertFactory.getArticleInfo(s)
                NLog.e(TAG, "xml time = ${System.currentTimeMillis() - time}")
                if (data == null) {
                    val errorMsg = ErrorConvertFactory.getErrorMessage(s)
                    if (errorMsg != null) {
                        throw Exception(errorMsg)
                    } else {
                        throw ServerException("NGA后台抽风了，请尝试右上角菜单中的使用内置浏览器打开")
                    }
                } else {
                    data.source = if (url.contains("__output=10")) ThreadData.SOURCE_COMPACT_XML else ThreadData.SOURCE_XML
                    data
                }
            }
    }

    private fun requestWebPage(
        param: ArticleListParam,
        header: MutableMap<String, String>?
    ): Observable<ThreadData> {
        return requestWebPage(param, header, null)
    }

    private fun requestWebPage(
        param: ArticleListParam,
        header: MutableMap<String, String>?,
        cacheKey: String?
    ): Observable<ThreadData> {
        if (cacheKey != null) {
            synchronized(mWebPageCache) {
                val cachedData = mWebPageCache.remove(cacheKey)
                if (cachedData != null) {
                    return Observable.just(cachedData)
                }
            }
        }
        val webHeader = HashMap<String, String>()
        if (header != null) {
            webHeader.putAll(header)
        }
        webHeader["User-Agent"] = WEB_USER_AGENT
        return mService.get(getWebUrl(param), webHeader)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.newThread())
            .compose(getLifecycleProvider().bindUntilEvent(FragmentEvent.DETACH))
            .map { s ->
                val time = System.currentTimeMillis()
                val data = ArticleWebConvertFactory.getArticleInfo(s)
                NLog.e(TAG, "web time = ${System.currentTimeMillis() - time}")
                if (data == null) {
                    throw ServerException("NGA后台抽风了，请尝试右上角菜单中的使用内置浏览器打开")
                } else if (!isValidWebPage(param, data)) {
                    throw ServerException("HTML解析结果异常，请稍后重试")
                } else {
                    data.source = ThreadData.SOURCE_WEB_HTML
                    data
                }
            }
    }

    private fun prefetchNextWebPage(param: ArticleListParam, header: MutableMap<String, String>?) {
        if (param.tid == 0 || param.pid != 0 || param.authorId != 0 || param.searchPost != 0) {
            return
        }
        val nextParam = param.clone() as ArticleListParam
        nextParam.page = param.page + 1
        val cacheKey = getWebCacheKey(nextParam)
        synchronized(mWebPageCache) {
            if (mWebPageCache.containsKey(cacheKey)) {
                return
            }
        }
        requestWebPage(nextParam, header, null)
            .subscribe(object : BaseSubscriber<ThreadData>() {
                override fun onNext(threadData: ThreadData) {
                    fillDebugInfo(nextParam, threadData, emptyList())
                    synchronized(mWebPageCache) {
                        mWebPageCache[cacheKey] = threadData
                    }
                    NLog.d(TAG, "prefetched web page tid=${nextParam.tid} page=${nextParam.page}")
                }

                override fun onError(throwable: Throwable) {
                    NLog.d(TAG, "prefetch web page failed tid=${nextParam.tid} page=${nextParam.page}")
                }
            })
    }

    private fun isValidWebPage(param: ArticleListParam, data: ThreadData): Boolean {
        val threadInfo = data.threadInfo ?: return false
        val rowList = data.rowList ?: return false
        if (rowList.isEmpty()) {
            NLog.e(TAG, "invalid web page: empty row list")
            return false
        }
        if (param.tid != 0 && threadInfo.tid != 0 && param.tid != threadInfo.tid) {
            NLog.e(TAG, "invalid web page: tid mismatch request=${param.tid}, actual=${threadInfo.tid}")
            return false
        }
        if (threadInfo.page > 0 && param.page > 0 && threadInfo.page != param.page) {
            NLog.e(TAG, "invalid web page: page mismatch request=${param.page}, actual=${threadInfo.page}")
            return false
        }

        var minFloor = Int.MAX_VALUE
        var maxFloor = Int.MIN_VALUE
        for (row: ThreadRowInfo in rowList) {
            if (row.tid != 0 && param.tid != 0 && row.tid != param.tid) {
                NLog.e(TAG, "invalid web page: row tid mismatch request=${param.tid}, actual=${row.tid}")
                return false
            }
            minFloor = minOf(minFloor, row.lou)
            maxFloor = maxOf(maxFloor, row.lou)
        }

        val totalRows = data.get__ROWS()
        if (totalRows > 0 && maxFloor >= totalRows) {
            NLog.e(TAG, "invalid web page: floor exceeds total rows maxFloor=$maxFloor, totalRows=$totalRows")
            return false
        }
        if (param.page > 0) {
            val expectedMinFloor = (param.page - 1) * 20
            val expectedMaxFloor = expectedMinFloor + 19
            if (maxFloor < expectedMinFloor || minFloor > expectedMaxFloor) {
                NLog.e(TAG, "invalid web page: floor range mismatch page=${param.page}, actual=$minFloor-$maxFloor, expected=$expectedMinFloor-$expectedMaxFloor")
                return false
            }
        }
        return true
    }


    override fun cachePage(param: ArticleListParam, rawData: String) {

        if (TextUtils.isEmpty(param.topicInfo)) {
            ToastUtils.error("缓存失败！");
            return;
        }
        ThreadUtils.postOnSubThread {
            try {
                val path = ContextUtils.getContext().filesDir.absolutePath + "/cache/${param.tid}"
                val describeFile = File(path, "${param.tid}.json")
                FileUtils.write(describeFile, param.topicInfo)
                val rawDataFile = File(path, "${param.page}.json")
                FileUtils.write(rawDataFile, rawData)
                ToastUtils.success("缓存成功！")
            } catch (e: IOException) {
                ToastUtils.error("缓存失败！")
                e.printStackTrace()
            }
        }
    }

    override fun loadCachePage(param: ArticleListParam, callBack: OnHttpCallBack<ThreadData>) {
        Observable.create<ThreadData> { emitter ->
            val cachePath =
                "${ContextUtils.getContext().filesDir.absolutePath}/cache/${param.tid}/${param.page}.json"
            val cacheFile = File(cachePath)
            val rawData = FileUtils.readFileToString(cacheFile)
            val threadData = ArticleConvertFactory.getArticleInfo(rawData)
            if (threadData != null) {
                threadData.source = ThreadData.SOURCE_CACHE
                emitter.onNext(threadData)
            } else {
                emitter.onError(Exception())
            }
            emitter.onComplete()
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : BaseSubscriber<ThreadData>() {
                override fun onNext(threadData: ThreadData) {
                    callBack.onSuccess(threadData)
                }

                override fun onError(throwable: Throwable) {
                    callBack.onError("读取缓存失败！")
                }
            })
    }

    class ServerException(message: String) : Exception(message)

}
