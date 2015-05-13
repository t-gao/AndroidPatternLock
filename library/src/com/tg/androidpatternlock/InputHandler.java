package com.tg.androidpatternlock;

import java.util.ArrayList;

import com.tg.androidpatternlock.Encrypter.HashGenerationException;
import com.tg.androidpatternlock.LockView.PatternPasswordStorageFetcher;

public class InputHandler {

    private String mPatternCache = null;

    public InputHandler() {
    }

    //TODO: asynchronously
    public boolean check(PatternPasswordStorageFetcher fetcher,
            ArrayList<Integer> pattern) {
        String input = null;
        if (mPatternCache == null && fetcher != null) {
            mPatternCache = fetcher.fetchEncrypted();
        }
        try {
            input = Encrypter.generateSHA256(Encrypter
                    .convertIntegerArrayToString(pattern));
        } catch (HashGenerationException e) {
            e.printStackTrace();
            return false;
        }
        return input != null && input.equals(mPatternCache);
    }

    public void reset() {
        mPatternCache = null;
    }
}
