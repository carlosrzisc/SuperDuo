package barqsoft.footballscores.widget;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.widget.RemoteViews;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.MainActivity;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;
import barqsoft.footballscores.scoresAdapter;

/**
 * Updates single widget
 */
public class TodayMatchIntentService extends IntentService {
//    private static final String[] SCORES_COLUMNS = {
//            DatabaseContract.scores_table._ID,
//            DatabaseContract.scores_table.HOME_COL,
//            DatabaseContract.scores_table.HOME_GOALS_COL,
//            DatabaseContract.scores_table.TIME_COL,
//            DatabaseContract.scores_table.AWAY_COL,
//            DatabaseContract.scores_table.AWAY_GOALS_COL
//    };
//
//    private static final int INDEX_SCORE_ID = 0;
//    private static final int INDEX_HOME_NAME = 1;
//    private static final int INDEX_HOME_GOALS = 2;
//    private static final int INDEX_TIME = 3;
//    private static final int INDEX_AWAY_NAME = 4;
//    private static final int INDEX_AWAY_GOALS = 5;


    public TodayMatchIntentService() {
        super(TodayMatchIntentService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Retrieve all of the Today widget ids: these are the widgets we need to update
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,
                TodayMatchProvider.class));

        // Get today's data from the ContentProvider
        Cursor data = getContentResolver().query(DatabaseContract.scores_table.buildScoreWithDate(),
                null, null, new String[]{Utilies.getDate(0)}, null);
        if (data == null) {
            return;
        }
        if (!data.moveToFirst()) {
            data.close();
            return;
        }

        // Extract the scores data from the Cursor
        String homeName = data.getString(scoresAdapter.COL_HOME);
        String t = data.getString(scoresAdapter.COL_MATCHTIME);
        String awayName = data.getString(scoresAdapter.COL_AWAY);
        String score = Utilies.getScores(data.getInt(scoresAdapter.COL_HOME_GOALS), data.getInt(scoresAdapter.COL_AWAY_GOALS));

        data.close();

        // Perform this loop procedure for each Today widget
        for (int appWidgetId : appWidgetIds) {
            int layoutId = R.layout.widget_today;
            RemoteViews views = new RemoteViews(getPackageName(), layoutId);

            // Add the data to the RemoteViews
            views.setImageViewResource(R.id.home_crest, Utilies.getTeamCrestByTeamName("Everton FC"));
            views.setImageViewResource(R.id.away_crest, Utilies.getTeamCrestByTeamName("Stoke City FC"));

            // Content Descriptions for RemoteViews were only added in ICS MR1
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                setRemoteContentDescription(views, R.id.home_crest, homeName);
                setRemoteContentDescription(views, R.id.away_crest, awayName);
            }

            views.setTextViewText(R.id.score_textview, score);
            views.setTextViewText(R.id.data_textview, t);

            // Create an Intent to launch MainActivity
            Intent launchIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, 0);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void setRemoteContentDescription(RemoteViews views, int resourceId, String description) {
        views.setContentDescription(resourceId, description);
    }
}
