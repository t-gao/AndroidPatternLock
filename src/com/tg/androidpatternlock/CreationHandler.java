package com.tg.androidpatternlock;

import java.util.ArrayList;

import com.tg.androidpatternlock.Encrypter.HashGenerationException;

import android.os.AsyncTask;

public class CreationHandler {

    private ArrayList<Integer> mPatternCache;// = new ArrayList<Integer>();;
    private State mState = State.None;

    private PatternCreatingListener mPatternCreatingListener;

    public void completeInput(ArrayList<Integer> pattern) {
        if (mState == State.None) {
            mPatternCache = new ArrayList<Integer>(pattern);
            // mPatternCache = (ArrayList<Integer>) pattern.clone();
            mState = State.CompleteOnce;
            if (mPatternCreatingListener != null) {
                mPatternCreatingListener.onInputOnce();
            }
        } else if (mState == State.CompleteOnce) {
            new CompleteCreationTask().execute(pattern);
        }
    }

    class CompleteCreationTask extends AsyncTask<ArrayList<Integer>, Void, CreationResult> {

        @Override
        protected CreationResult doInBackground(ArrayList<Integer>... params) {
            CreationResult result = new CreationResult();
            result.match = false;
            result.encryptedPattern = null;
            if (params == null || params.length == 0) {
                return result;
            }

            ArrayList<Integer> pattern = params[0];
            result.match = PatternComparoter.compare(mPatternCache, pattern);
            if (result.match && mPatternCache != null) {
               
                try {
                    result.encryptedPattern = Encrypter.generateSHA256(Encrypter.convertIntegerArrayToString(mPatternCache));
                } catch (HashGenerationException e) {
                    result.encryptedPattern = null;
                }
            }
            
            return result;
        }

        @Override
        protected void onPostExecute(CreationResult result) {
            boolean match = result.match;
            if (!match) {
                reset();
            }
            if (mPatternCreatingListener != null) {
                mPatternCreatingListener.onComplete(match, result.encryptedPattern);
            }
        }
        
    }

    private class CreationResult {
        boolean match;
        String encryptedPattern;
    }

    public void reset() {
        mState = State.None;
        mPatternCache = null;
    }

    public void setPatternCreatingListener(PatternCreatingListener li) {
        mPatternCreatingListener = li;
    }

    public interface PatternCreatingListener {
        public void onInputOnce();

        public void onComplete(boolean match, String encryptedPatternStr);
    }

    private enum State {
        None, CompleteOnce
    }
}
