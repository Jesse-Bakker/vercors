package vct.test.integration.examples

import vct.test.integration.helper.VercorsSpec

class VerifyThisSpec extends VercorsSpec {
  vercors should verify using silicon example "verifythis/prefix.pvl"
  vercors should verify using silicon example "verifythis/lcp.pvl"
  vercors should verify using silicon example "verifythis/2019/challenge1.pvl"
}
