package com.tg.androidpatternlock;

import java.util.ArrayList;

import android.os.AsyncTask;

public class PatternComparoter {

    private static final PatternComparoter mInstance = new PatternComparoter();

    private PatternComparoter() {

    }

    public static PatternComparoter getInstance() {
        return mInstance;
    }

    public static boolean compare(ArrayList<Integer> first, ArrayList<Integer> second) {
        if (first == null || second == null) {
            return false;
        }
        int size = first.size();
        if (size != second.size()) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (first.get(i).intValue() != second.get(i).intValue()) {
                return false;
            }
        }

        return true;
    }

    public void compare(ArrayList<Integer> first, ArrayList<Integer> second,
            CompareCallback callback) {
        new CompareTask(callback).execute(first, second);
    }

    public interface CompareCallback {
        public void onResult(boolean match);
    }

    class CompareTask extends AsyncTask<ArrayList<Integer>, Void, Boolean> {

        private CompareCallback mCampareCallback;

        public CompareTask(CompareCallback callback) {
            mCampareCallback = callback;
        }

        @Override
        protected Boolean doInBackground(ArrayList<Integer>... params) {
            if (params == null || params.length < 2) {
                return false;
            }

            ArrayList<Integer> first = params[0];
            ArrayList<Integer> second = params[1];
            return compare(first, second);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (mCampareCallback != null) {
                mCampareCallback.onResult(result);
            }
        }

    }
}
