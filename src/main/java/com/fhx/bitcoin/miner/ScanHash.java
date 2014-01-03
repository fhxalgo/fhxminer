package com.fhx.bitcoin.miner;

import static com.fhx.bitcoin.miner.HexUtil.decode;
import static com.fhx.bitcoin.miner.HexUtil.encode;

/**
 * Created by George on 1/2/14.
 */
public class ScanHash {
    protected long hashed;

    public long getCount() {
        long cnt = hashed;
        hashed = 0;
        return cnt;
    }

    public boolean scan(Work work, int start, int count) {
        SHA256 sha256 = new SHA256();

        int[] _data = decode(new int[16], work.dataText.substring(128));
        int[] _midstate = decode(decode(new int[16], work.hash1Text), work.midstateText);

        int[] __state, __data, __hash1, __hash;
        for (int nonce = start; nonce < start + count; nonce++) {
            _data[3] = nonce; // NONCE is _data[3]
            hashed++;

            __state = new int[_midstate.length];
            System.arraycopy(_midstate, 0, __state, 0, _midstate.length);
            __data = _data;

            sha256.processBlock(__state, __data);
            __hash1 = __state;

            __state = SHA256.initState();
            sha256.processBlock(__state, __hash1);
            __hash = __state;

            if (__hash[7] == 0) {
                work.dataText = work.dataText.substring(0, 128) + encode(__data);
                return true;
            }
        }

        return false;
    }
}