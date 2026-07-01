package gov.anzong.androidnga.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.alibaba.android.arouter.facade.annotation.Route;

import gov.anzong.androidnga.arouter.ARouterConstants;
import sp.phone.common.TopicHistoryManager;
import sp.phone.mvp.model.entity.ThreadPageInfo;
import sp.phone.param.ArticleListParam;
import sp.phone.param.ParamKey;
import sp.phone.ui.fragment.ArticleSearchFragment;
import sp.phone.ui.fragment.ArticleTabFragment;
import sp.phone.util.StringUtils;

/**
 * 帖子详情页, 是否MD都用这个
 */
@Route(path = ARouterConstants.ACTIVITY_TOPIC_CONTENT)
public class ArticleListActivity extends BaseActivity {

    private ArticleListParam mRequestParam;

    private void setupFragment() {
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(android.R.id.content);

        if (fragment == null) {
            if (mRequestParam.searchPost == 0) {
                fragment = new ArticleTabFragment();
            } else {
                fragment = new ArticleSearchFragment();
            }
            fragment.setHasOptionsMenu(true);
            Bundle bundle = new Bundle();
            bundle.putParcelable(ParamKey.KEY_PARAM, mRequestParam);
            fragment.setArguments(bundle);
            fm.beginTransaction().replace(android.R.id.content, fragment).commit();
        } else {
            fragment.setHasOptionsMenu(true);
        }
    }

    private ArticleListParam getArticleListParam() {

        Bundle bundle = getIntent().getExtras();
        String url = getIntent().getDataString();
        ArticleListParam param = null;
        if (url != null) {
            param = new ArticleListParam();
            param.tid = StringUtils.getUrlParameter(url, "tid");
            param.pid = StringUtils.getUrlParameter(url, "pid");
            param.authorId = StringUtils.getUrlParameter(url, "authorid");
            param.page = StringUtils.getUrlParameter(url, "page");
            param.searchPost = StringUtils.getUrlParameter(url,ParamKey.KEY_SEARCH_POST);
        } else if (bundle != null) {
            param = bundle.getParcelable(ParamKey.KEY_PARAM);
            if (param == null) {
                param = new ArticleListParam();
                param.tid = bundle.getInt(ParamKey.KEY_TID, 0);
                param.pid = bundle.getInt(ParamKey.KEY_PID, 0);
                param.authorId = bundle.getInt(ParamKey.KEY_AUTHOR_ID, 0);
                param.searchPost = bundle.getInt(ParamKey.KEY_SEARCH_POST, 0);
                param.title = bundle.getString(ParamKey.KEY_TITLE);
            }
        }

        if (param != null) {
            applyHistoryPage(param, url);
        }
        return param;
    }

    private void applyHistoryPage(ArticleListParam param, String url) {
        if (param.tid == 0 || param.pid != 0 || param.authorId != 0 || param.searchPost != 0) {
            return;
        }
        boolean hasExplicitPage = url != null && url.contains("page=");
        if (hasExplicitPage || param.page > 1) {
            return;
        }
        ThreadPageInfo history = TopicHistoryManager.getInstance().findTopicHistory(param.tid);
        if (history != null && history.getPage() > 1) {
            param.page = history.getPage();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setToolbarEnabled(true);
        mRequestParam = getArticleListParam();
        super.onCreate(savedInstanceState);
        if (mRequestParam == null) {
            finish();
            return;
        }
        setupFragment();
        if (mRequestParam.title != null) {
            setTitle(mRequestParam.title);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        getSupportFragmentManager().findFragmentById(android.R.id.content).onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }
}
