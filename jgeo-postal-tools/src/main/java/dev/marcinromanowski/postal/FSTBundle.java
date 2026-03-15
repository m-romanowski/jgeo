package dev.marcinromanowski.postal;

import org.apache.lucene.util.fst.FST;

record FSTBundle(
    FST<Long> cityFST,
    FST<Long> cityPostalFST,
    FST<Long> countryPostalFST
) {

}
