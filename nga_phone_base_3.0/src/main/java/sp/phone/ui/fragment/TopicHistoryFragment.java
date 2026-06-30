package sp.phone.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;

import java.util.List;

import gov.anzong.androidnga.R;
import gov.anzong.androidnga.base.util.ContextUtils;
import gov.anzong.androidnga.base.widget.DividerItemDecorationEx;
import gov.anzong.androidnga.common.ui.dialog.ConfirmDialog;
import sp.phone.common.PhoneConfiguration;
import sp.phone.common.TopicHistoryManager;
import sp.phone.mvp.model.entity.ThreadPageInfo;
import sp.phone.mvp.model.entity.TopicListInfo;
import sp.phone.param.ArticleListParam;
import sp.phone.param.ParamKey;
import sp.phone.ui.adapter.TopicListAdapter;
import sp.phone.view.RecyclerViewEx;

/**
 * Created by Justwen on 2018/1/17.
 */

public class TopicHistoryFragment extends BaseFragment implements View.OnClickListener {

    private TopicListAdapter mTopicListAdapter;

    private RecyclerViewEx mListView;

    private TopicHistoryManager mTopicHistoryManager;

    private String mQuery = "";

    private TextView mEmptyView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        mTopicHistoryManager = TopicHistoryManager.getInstance();
        setTitle(R.string.label_activity_topic_history);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_topic_history, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mTopicListAdapter = new TopicListAdapter(getContext());
        mTopicListAdapter.setShowHistoryPage(true);
        mTopicListAdapter.setOnClickListener(this);

        mListView = view.findViewById(R.id.list);
        mListView.setLayoutManager(new LinearLayoutManager(getContext()));
        mListView.setEmptyView(view.findViewById(R.id.empty_view));
        mEmptyView = view.findViewById(R.id.tv_empty);
        mListView.setAdapter(mTopicListAdapter);
        mListView.addItemDecoration(new DividerItemDecorationEx(view.getContext(), ContextUtils.getDimension(R.dimen.topic_list_item_padding), DividerItemDecoration.VERTICAL));

        super.onViewCreated(view, savedInstanceState);
        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position >= 0) {
                    ThreadPageInfo topic = mTopicListAdapter.getItem(position);
                    mTopicHistoryManager.removeTopicHistory(topic);
                    refreshData();
                }

            }
        });
        //将recycleView和ItemTouchHelper绑定
        touchHelper.attachToRecyclerView(mListView);
        refreshData();
    }

    private void setData(List<ThreadPageInfo> topicLIst) {
        TopicListInfo listInfo = new TopicListInfo();
        listInfo.setThreadPageList(topicLIst);
        mTopicListAdapter.setHighlightKeyword(mQuery);
        mTopicListAdapter.clear();
        mTopicListAdapter.setData(listInfo.getThreadPageList());
    }

    private void refreshData() {
        if (mEmptyView != null) {
            mEmptyView.setText(TextUtils.isEmpty(mQuery) ? R.string.topic_history_empty : R.string.topic_history_search_empty);
        }
        setData(mTopicHistoryManager.searchTopicHistory(mQuery));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.settings_black_list_option_menu, menu);
        inflater.inflate(R.menu.menu_search, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(getString(R.string.search));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filter(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                filter("");
                return true;
            }
        });
    }

    private void filter(String query) {
        mQuery = TextUtils.isEmpty(query) ? "" : query.trim();
        refreshData();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_delete_all) {
            ConfirmDialog.Companion.showConfirmDialog(getActivity(), "确认删除所有浏览历史吗", () -> {
                mTopicHistoryManager.removeAllTopicHistory();
                filter("");
            });
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View view) {
        ThreadPageInfo info = (ThreadPageInfo) view.getTag();
        ArticleListParam param = new ArticleListParam();
        param.tid = info.getTid();
        param.page = info.getPage();
        param.title = info.getSubject();
        param.topicInfo = JSON.toJSONString(info);
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ParamKey.KEY_PARAM, param);
        intent.putExtras(bundle);
        intent.setClass(getContext(), PhoneConfiguration.getInstance().articleActivityClass);
        startActivity(intent);
    }
}
