package barqsoft.footballscores.widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;
import barqsoft.footballscores.scoresAdapter;

/**
 * RemoteViewsService controlling the data being shown in the scrollable scores collection widget
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class CollectionWidgetRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();
                data = getContentResolver().query(DatabaseContract.scores_table.buildScoreWithDate(),
                        null, null, new String[]{Utilies.getDate(0)}, null);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_collection_item);

                String homeName = data.getString(scoresAdapter.COL_HOME);
                String t = data.getString(scoresAdapter.COL_MATCHTIME);
                String awayName = data.getString(scoresAdapter.COL_AWAY);
                String score = Utilies.getScores(data.getInt(scoresAdapter.COL_HOME_GOALS), data.getInt(scoresAdapter.COL_AWAY_GOALS));

                // Add the data to the RemoteViews
                views.setImageViewResource(R.id.home_crest, Utilies.getTeamCrestByTeamName(homeName));
                views.setImageViewResource(R.id.away_crest, Utilies.getTeamCrestByTeamName(awayName));

                // Content Descriptions for RemoteViews were only added in ICS MR1
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    setRemoteContentDescription(views, R.id.home_crest, homeName);
                    setRemoteContentDescription(views, R.id.away_crest, awayName);
                }

                views.setTextViewText(R.id.score_textview, score);
                views.setTextViewText(R.id.data_textview, t);

                final Intent fillInIntent = new Intent();
                Uri scoresUri = DatabaseContract.scores_table.buildScoreWithDate();
                fillInIntent.setData(scoresUri);
                views.setOnClickFillInIntent(R.id.widget, fillInIntent);
                return views;
            }

            @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
            private void setRemoteContentDescription(RemoteViews views, int resourceId, String description) {
                views.setContentDescription(resourceId, description);
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_collection_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(scoresAdapter.COL_ID);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
