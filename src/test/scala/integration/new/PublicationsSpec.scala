package integration.`new`

import integration.helper.VercorsSpec

class PublicationsSpec extends VercorsSpec {
  vercors should verify using silicon example "publications/2016/layers/LFQ.java"
  vercors should verify using silicon example "publications/2016/layers/LFQHist.java"
}