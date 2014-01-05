package com.fhx.bitcoin.miner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static com.fhx.bitcoin.miner.HexUtil.decode;
import static com.fhx.bitcoin.miner.HexUtil.encode;

/**
 * Created by George on 1/2/14.
 */
public class ScanHash {
    private static final Logger log = LoggerFactory.getLogger(ScanHash.class);

    protected long hashed;

    public long getCount() {
        long cnt = hashed;
        hashed = 0;
        return cnt;
    }

    public boolean scan(Work work, int start, int count) {
        log.info("xxxx: work: start= " + start + ", count=" + count);
        //log.info("timeeeeeeeeeeeeeeeeeeee: " + Utils.df.format(new Date()));
        SHA256 sha256 = new SHA256();

        int[] _data = decode(new int[16], work.dataText.substring(128));
        int[] _midstate = decode(decode(new int[16], work.hash1Text), work.midstateText);

        log.info("xxxx data: " + Arrays.toString(_data));
        log.info("xxxx midstate: " + Arrays.toString(_midstate));

        int[] __state, __data, __hash1, __hash;
        int mee = 0;
        for (int nonce = start; nonce < start + count; nonce++) {
            _data[3] = nonce; // NONCE is _data[3]
            hashed++;

            __state = new int[_midstate.length];
            System.arraycopy(_midstate, 0, __state, 0, _midstate.length);
            __data = _data;

            sha256.processBlock(__state, __data);
            __hash1 = __state;
            // double hashing
            __state = SHA256.initState();
            sha256.processBlock(__state, __hash1);
            __hash = __state;

            //log.info("xxxx hash[7]: " + __hash[7]);
            mee++;

            if (__hash[7] == 0) {
                work.dataText = work.dataText.substring(0, 128) + encode(__data);
                return true;
            }
        }

        log.info("meeeeeeeeeeeeeee or nonce: " + mee);

        return false;
    }
}